package org.goplanit.io.converter.pricing;

import java.math.BigDecimal;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.converter.BaseReaderImpl;
import org.goplanit.converter.pricing.NetworkPricingReader;
import org.goplanit.io.converter.network.ModeXmlUtils;
import org.goplanit.io.xml.util.PlanitXmlJaxbParser;
import org.goplanit.io.xml.util.XmlEnumConversionUtil;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.pricing.*;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.exceptions.ExceptionUtils;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.goplanit.utils.pricing.*;
import org.goplanit.utils.problem.Problems;
import org.goplanit.xml.generated.v2.XMLElementCap;
import org.goplanit.xml.generated.v2.XMLElementCaps;
import org.goplanit.xml.generated.v2.XMLElementDistanceBandOverride;
import org.goplanit.xml.generated.v2.XMLElementEntryExitScheme;
import org.goplanit.xml.generated.v2.XMLElementExclude;
import org.goplanit.xml.generated.v2.XMLElementGatePairPrice;
import org.goplanit.xml.generated.v2.XMLElementGatePairPriceOverride;
import org.goplanit.xml.generated.v2.XMLElementGatePairPricing;
import org.goplanit.xml.generated.v2.XMLElementGates;
import org.goplanit.xml.generated.v2.XMLElementInclude;
import org.goplanit.xml.generated.v2.XMLElementLocationScheme;
import org.goplanit.xml.generated.v2.XMLElementMinimum;
import org.goplanit.xml.generated.v2.XMLElementModeScheme;
import org.goplanit.xml.generated.v2.XMLElementNetworkPricing;
import org.goplanit.xml.generated.v2.XMLElementSections;
import org.goplanit.xml.generated.v2.XMLElementStartWithPerKmPrice;
import org.goplanit.xml.generated.v2.XMLElementStartWithPerKmPriceOverride;
import org.goplanit.xml.generated.v2.XMLElementStartWithPerKmPricing;
import org.goplanit.xml.generated.v2.XMLElementTimeBandEntry;

import static org.goplanit.utils.math.TypeConversionUtil.toDoubleOrNull;

/**
 * Reads a network pricing in the native PLANit XML format onto a network already in memory
 * <p>
 * A pricing file names its network in {@code <parentnetwork>} and refers to that network's layers, link segment types,
 * link segments, modes and toll roads by XML id. The reader looks each one up, builds the sections, gates and pricing
 * of each scheme, and hands the scheme to its factory, which checks it and registers it unless a problem refuses it;
 * caps follow their schemes. What the network cannot resolve, such as an unknown id, is the reader's to report, with
 * one warning each, as that is where it is dropped. The problems a factory finds are logged once per scheme or cap,
 * under the XML id the user wrote.
 * </p>
 *
 * @author markr
 */
public class PlanitNetworkPricingReader extends BaseReaderImpl<NetworkPricing> implements NetworkPricingReader {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkPricingReader.class.getCanonicalName());

  /**
   * The kind, XML id and name of a scheme, as a warning names it, such as "mode scheme a (Truck charge)"
   *
   * @param scheme to describe
   * @return its kind and XML id, and its name when it has one
   */
  private static String describe(PricingScheme scheme) {
    var kind = scheme instanceof ModePricingScheme ? "mode scheme" :
        scheme instanceof LocationPricingScheme ? "location scheme" : "entry-exit scheme";
    var kindAndId = kind + " " + scheme.getXmlId();
    return scheme.getName() == null ? kindAndId : kindAndId + " (" + scheme.getName() + ")";
  }

  /**
   * Parse the section price rules of a {@code <price>}; one naming a section its scheme does not have is ignored
   * with a single
   * warning
   *
   * @param subject the scheme, as the warnings name it
   * @param xmlPrice to parse
   * @param sections of the scheme, by XML id
   * @return the section price rules
   */
  private static List<SectionPriceRule> parseSectionPriceRules(
      String subject,
      XMLElementStartWithPerKmPrice xmlPrice,
      Map<String, PricingSchemeSection> sections) {
    var sectionPriceRules = new ArrayList<SectionPriceRule>();
    for (var xmlSectionPrice : xmlPrice.getSectionprices()) {
      var section = sections.get(xmlSectionPrice.getSectionref());
      if (section == null) {
        LOGGER.warning(String.format("IGNORE: %s lists unknown section %s", subject, xmlSectionPrice.getSectionref()));
        continue;
      }
      sectionPriceRules.add(new SectionPriceRuleImpl(section, toDoubleOrNull(xmlSectionPrice.getStart()),
          toDoubleOrNull(xmlSectionPrice.getPerkm())));
    }
    return sectionPriceRules;
  }

  /**
   * Parse the gate from-to amounts of a {@code <price>}; one naming a gate its scheme does not have is ignored with a
   * single warning
   *
   * @param subject the scheme, as the warnings name it
   * @param xmlPrice to parse
   * @param gates of the scheme, by XML id
   * @return the gate from-to amounts
   */
  private static List<GateFromToAmount> parseGateFromToAmounts(
      String subject,
      XMLElementGatePairPrice xmlPrice,
      Map<String, PricingSchemeGate> gates) {
    var gatePairs = new ArrayList<GateFromToAmount>();
    for (var xmlPair : xmlPrice.getPairs()) {
      var fromGate = gates.get(xmlPair.getFromgateref());
      var toGate = gates.get(xmlPair.getTogateref());
      if (fromGate == null || toGate == null) {
        LOGGER.warning(String.format("IGNORE: %s lists unknown gate %s", subject,
            fromGate == null ? xmlPair.getFromgateref() : xmlPair.getTogateref()));
        continue;
      }
      gatePairs.add(new GateFromToAmountImpl(fromGate, toGate, xmlPair.getAmount().doubleValue()));
    }
    return gatePairs;
  }

  /**
   * Parse the price rule of a mode or location scheme
   *
   * @param subject the scheme, as the warnings name it
   * @param xmlPrice to parse, may be null; an override is one too
   * @param sections of the scheme, by XML id
   * @return the price rule, null when none is given
   */
  private static StartWithPerKmPriceRule parseStartWithPerKmPriceRule(
      String subject,
      XMLElementStartWithPerKmPrice xmlPrice,
      Map<String, PricingSchemeSection> sections) {
    return xmlPrice == null ? null : new StartWithPerKmPriceRuleImpl(toDoubleOrNull(xmlPrice.getStart()),
        toDoubleOrNull(xmlPrice.getPerkm()), parseSectionPriceRules(subject, xmlPrice, sections));
  }

  /**
   * Parse the price rule of an entry-exit scheme
   *
   * @param subject the scheme, as the warnings name it
   * @param xmlPrice to parse, may be null; an override is one too
   * @param gates of the scheme, by XML id
   * @return the price rule, null when none is given
   */
  private static FromToGatesPriceRule parseFromToGatesPriceRule(
      String subject,
      XMLElementGatePairPrice xmlPrice,
      Map<String, PricingSchemeGate> gates) {
    return xmlPrice == null ? null : new FromToGatesPriceRuleImpl(parseGateFromToAmounts(subject, xmlPrice, gates));
  }

  /**
   * Parse a time band entry: its start and duration in minutes, keeping the unit it is written in, so writing it back
   * gives what was read
   * <p>
   * The time attributes are shared with the counts, whose unit may be a year, so the reader refuses a year here
   * itself, before the entry is made, to report it as such.
   * </p>
   *
   * @param xmlEntry to parse
   * @return the entry
   * @throws IllegalArgumentException when it is given in years, lasts longer than a day, or starts a day or more after
   *                                  midnight
   */
  private static PricingTimeBandEntry parseTimeBandEntry(XMLElementTimeBandEntry xmlEntry) {
    var unit = XmlEnumConversionUtil.xmlToPlanit(xmlEntry.getTemporalunit());
    ExceptionUtils.throwIllegalArgumentIf(unit == ChronoUnit.YEARS,
        "Time band entry in years refused, it is given in minutes, hours or days");
    var days = xmlEntry.getDays().stream().map(XmlEnumConversionUtil::xmlToPlanit).collect(Collectors.toList());
    int unitMinutes = (int) unit.getDuration().toMinutes();
    return new PricingTimeBandEntryImpl(days, XmlEnumConversionUtil.xmlToPlanit(xmlEntry.getPublicholidays()),
        XmlEnumConversionUtil.xmlToPlanit(xmlEntry.getSchoolholidays()), xmlEntry.getTemporaloffset() * unitMinutes,
        xmlEntry.getTemporalunitmultiplier() * unitMinutes, unit);
  }

  /**
   * Parse the distance bands of a pricing of either kind
   *
   * @param xmlBands to parse
   * @return the distance bands
   * @throws IllegalArgumentException when a band is per hour, or starts at or beyond its end
   */
  private static List<PricingDistanceBand> parseDistanceBands(List<XMLElementDistanceBandOverride> xmlBands) {
    return xmlBands.stream().map(xmlBand -> (PricingDistanceBand) new PricingDistanceBandImpl(
        XmlEnumConversionUtil.xmlToPlanit(xmlBand.getPer()), toDoubleOrNull(xmlBand.getFromkm()),
        toDoubleOrNull(xmlBand.getTokm()), xmlBand.getFactor().doubleValue())).collect(Collectors.toList());
  }

  /**
   * Parse a time band of a pricing of either kind
   *
   * @param <P> the kind of price rule
   * @param xmlId of the band
   * @param xmlEntries of the band
   * @param priceRule of the band, null when it gives none
   * @param priceOverrides of the band
   * @return the band
   * @throws IllegalArgumentException when one of its entries makes no sense on its own, or it gives neither a price
   *                                  rule nor overrides, naming the band
   */
  private static <P extends PriceRule<P>> PricingTimeBand<P> parseTimeBand(
      String xmlId,
      List<XMLElementTimeBandEntry> xmlEntries,
      P priceRule,
      List<PriceOverride<P>> priceOverrides) {
    try {
      var entries = xmlEntries.stream().map(PlanitNetworkPricingReader::parseTimeBandEntry)
          .collect(Collectors.toList());
      return new PricingTimeBandImpl<>(xmlId, entries, priceRule, priceOverrides);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(String.format("time band %s: %s", xmlId, e.getMessage()), e);
    }
  }

  /** parses the XML file, which has no legacy version */
  private final PlanitXmlJaxbParser<XMLElementNetworkPricing, XMLElementNetworkPricing> xmlParser;

  /** the settings to use */
  private final PlanitNetworkPricingReaderSettings settings;

  /** the network pricing to populate */
  private final NetworkPricing networkPricing;

  /**
   * The modes that pay as given, see {@link ModeXmlUtils#findPricedModeSelection}, found among the modes read, warning
   * on this reader's logger
   *
   * @param subject        the entity the modes are of, as the warnings name it
   * @param pricedModeRefs the modes named, null or blank when not given
   * @param exemptModeRefs the modes exempted, null or blank when not given
   * @return the modes as given, null when the subject is not to be read
   */
  private PricedModeSelection findPricedModeSelection(String subject, String pricedModeRefs, String exemptModeRefs) {
    return ModeXmlUtils.findPricedModeSelection(subject, pricedModeRefs, exemptModeRefs,
        (refs, unknownRef) -> findBySourceIds(Mode.class, refs, unknownRef), LOGGER);
  }

  /**
   * Stop reading when the file names no parent network, or one other than the network the pricing is read onto
   *
   * @param parentNetworkXmlId as the file gives it
   */
  private void checkParentNetwork(String parentNetworkXmlId) {
    var network = networkPricing.getParentNetwork();
    if (StringUtils.isNullOrBlank(parentNetworkXmlId)) {
      throw new PlanItRunTimeException("Network pricing %s has no parent network defined", networkPricing.getXmlId());
    }
    if (!parentNetworkXmlId.equals(network.getXmlId())) {
      throw new PlanItRunTimeException(
          "Network pricing %s names parent network %s, while the network in memory is %s",
          networkPricing.getXmlId(), parentNetworkXmlId, network.getXmlId());
    }
  }

  /**
   * Build the lookups by XML id of the network entities the file refers to
   *
   * @param network the pricing refers to
   */
  private void initialiseParentXmlIdTrackers(MacroscopicNetwork network) {
    var layers = network.getTransportLayers();
    initialiseSourceIdMap(MacroscopicNetworkLayer.class, MacroscopicNetworkLayer::getXmlId, layers);
    initialiseSourceIdMap(Mode.class, Mode::getXmlId, network.getModes());
    initialiseSourceIdMap(MacroscopicLinkSegmentType.class, MacroscopicLinkSegmentType::getXmlId, layers,
        MacroscopicNetworkLayer::getLinkSegmentTypes);
    initialiseSourceIdMap(MacroscopicLinkSegment.class, MacroscopicLinkSegment::getXmlId, layers,
        MacroscopicNetworkLayer::getLinkSegments);
    initialiseSourceIdMap(TollRoad.class, TollRoad::getXmlId, layers, MacroscopicNetworkLayer::getTollRoads);
  }

  /**
   * Build the lookups by XML id of the pricing's own entities, filled as they are read, so a duplicate id is found
   * before it is registered
   */
  private void initialiseXmlIdTrackers() {
    initialiseSourceIdMap(PricingScheme.class, PricingScheme::getXmlId);
    initialiseSourceIdMap(PricingSchemeSection.class, PricingSchemeSection::getXmlId);
    initialiseSourceIdMap(PricingSchemeGate.class, PricingSchemeGate::getXmlId);
    initialiseSourceIdMap(PriceCap.class, PriceCap::getXmlId);
  }

  /**
   * Find the entities a comma-separated list of XML ids refers to, ignoring each unknown id with a single warning
   *
   * @param <T> type of entity
   * @param type of entity
   * @param refs to find, may be null or blank
   * @param subject listing them, as the warning names it
   * @param kind of entity, as the warning names it
   * @return the entities found, in the order listed
   */
  private <T> List<T> findBySourceIdsIgnoringUnknown(Class<T> type, String refs, String subject, String kind) {
    return findBySourceIds(type, refs, ref -> LOGGER.warning(String.format(
        "IGNORE: %s lists unknown %s %s", subject, kind, ref)));
  }

  /**
   * The scheme read earlier that holds a section or gate
   *
   * @param sectionOrGate to look for
   * @return the scheme, null when none
   */
  private PricingScheme findSchemeHolding(Object sectionOrGate) {
    return networkPricing.streamSchemes().filter(scheme ->
        (scheme instanceof ModePricingScheme &&
            ListUtils.containsByIdentity(((ModePricingScheme) scheme).getSections(), sectionOrGate)) ||
        (scheme instanceof LocationPricingScheme &&
            ListUtils.containsByIdentity(((LocationPricingScheme) scheme).getSections(), sectionOrGate)) ||
        (scheme instanceof EntryExitPricingScheme &&
            ListUtils.containsByIdentity(((EntryExitPricingScheme) scheme).getGates(), sectionOrGate)))
        .findFirst().orElse(null);
  }

  /**
   * Check an id is not used by a section or gate read before, in this scheme or one read earlier, with a single
   * warning naming both schemes when it is
   *
   * @param <T> section or gate
   * @param type section or gate
   * @param xmlId of the section or gate
   * @param subject the section or gate, as the warning names it
   * @param schemeSubject its scheme, as the warning names it
   * @param readInThisScheme the sections or gates of this scheme read so far, by XML id
   * @return true when the id is new
   */
  private <T> boolean isNewSectionOrGateId(
      Class<T> type,
      String xmlId,
      String subject,
      String schemeSubject,
      Map<String, T> readInThisScheme) {
    String earlierScheme = null;
    if (readInThisScheme.containsKey(xmlId)) {
      earlierScheme = schemeSubject;
    } else if (getBySourceId(type, xmlId) != null) {
      var holder = findSchemeHolding(getBySourceId(type, xmlId));
      earlierScheme = holder == null ? "a scheme" : describe(holder);
    }
    if (earlierScheme != null) {
      LOGGER.warning(String.format("DISCARD: %s of %s has the id of a %s of %s read earlier", subject, schemeSubject,
          type == PricingSchemeSection.class ? "section" : "gate", earlierScheme));
    }
    return earlierScheme == null;
  }

  /**
   * Check a scheme's id is not used by a scheme of any kind read before, with a single warning naming both when it is
   *
   * @param xmlId of the scheme
   * @param subject the scheme, as the warning names it
   * @return true when the id is new
   */
  private boolean isNewSchemeId(String xmlId, String subject) {
    var earlier = getBySourceId(PricingScheme.class, xmlId);
    if (earlier != null) {
      LOGGER.warning(String.format("DISCARD: %s has the id of %s read earlier", subject, describe(earlier)));
    }
    return earlier == null;
  }

  /**
   * Parse the priced link segments of a mode scheme: what its include gives, less what its exclude gives, the whole
   * network when it has no include. An unknown reference is ignored with a single warning; an include listing no known
   * reference at all leaves the scheme not read, with a single warning, as reading it as the whole network would change
   * where it charges
   *
   * @param subject the scheme, as the warnings name it
   * @param xmlInclude to parse, may be null
   * @param xmlExclude to parse, may be null
   * @return the priced link segments, null when the scheme is not to be read
   */
  private ModePricingSchemeLinkSegmentCoverage parseLinkSegmentCoverage(
      String subject,
      XMLElementInclude xmlInclude,
      XMLElementExclude xmlExclude) {
    var coverage = new ModePricingSchemeLinkSegmentCoverage();
    if (xmlInclude != null) {
      var layers = findBySourceIdsIgnoringUnknown(
          MacroscopicNetworkLayer.class, xmlInclude.getLayerrefs(), subject, "layer");
      var types = findBySourceIdsIgnoringUnknown(
          MacroscopicLinkSegmentType.class, xmlInclude.getTyperefs(), subject, "link segment type");
      var linkSegments = findBySourceIdsIgnoringUnknown(
          MacroscopicLinkSegment.class, xmlInclude.getLsrefs(), subject, "link segment");
      boolean listsAny = !StringUtils.isNullOrBlank(xmlInclude.getLayerrefs()) ||
          !StringUtils.isNullOrBlank(xmlInclude.getTyperefs()) || !StringUtils.isNullOrBlank(xmlInclude.getLsrefs());
      if (listsAny && layers.isEmpty() && types.isEmpty() && linkSegments.isEmpty()) {
        LOGGER.warning(String.format("DISCARD: %s includes no known layer, link segment type or link segment",
            subject));
        return null;
      }
      coverage.considerLayers(layers);
      coverage.includeLinkSegmentTypes(types);
      coverage.includeLinkSegments(linkSegments);
    }
    if (xmlExclude != null) {
      coverage.excludeLinkSegmentTypes(findBySourceIdsIgnoringUnknown(
          MacroscopicLinkSegmentType.class, xmlExclude.getTyperefs(), subject, "link segment type"));
      coverage.excludeLinkSegments(findBySourceIdsIgnoringUnknown(
          MacroscopicLinkSegment.class, xmlExclude.getLsrefs(), subject, "link segment"));
    }
    return coverage;
  }

  /**
   * Parse the toll roads a location or entry-exit scheme charges on. When one is unknown the scheme is not read, with a
   * single warning, as charging on fewer roads than listed would change its prices
   *
   * @param subject the scheme, as the warning names it
   * @param tollRoadRefs comma-separated toll road ids
   * @return the toll roads, null when the scheme is not to be read
   */
  private List<TollRoad> parseTollRoads(String subject, String tollRoadRefs) {
    var unknown = new ArrayList<String>();
    var tollRoads = findBySourceIds(TollRoad.class, tollRoadRefs, unknown::add);
    if (!unknown.isEmpty()) {
      LOGGER.warning(String.format("DISCARD: %s lists unknown toll roads %s", subject, String.join(",", unknown)));
      return null;
    }
    return tollRoads;
  }

  /**
   * Parse and register the sections of a scheme. A section with an id used before is not read, nor one listing no
   * known link segment or type, each with a single warning; an unknown link segment or type is ignored with a single
   * warning
   *
   * @param schemeSubject the scheme, as the warnings name it
   * @param xmlSections to parse, may be null
   * @return the sections registered, by XML id, in the order given
   */
  private Map<String, PricingSchemeSection> parseSections(String schemeSubject, XMLElementSections xmlSections) {
    var sections = new LinkedHashMap<String, PricingSchemeSection>();
    if (xmlSections == null) {
      return sections;
    }
    for (var xmlSection : xmlSections.getSections()) {
      var subject = "Section " + xmlSection.getId();
      if (!isNewSectionOrGateId(PricingSchemeSection.class, xmlSection.getId(), subject, schemeSubject, sections)) {
        continue;
      }
      var listedBy = subject + " of " + schemeSubject;
      var linkSegments = findBySourceIdsIgnoringUnknown(
          MacroscopicLinkSegment.class, xmlSection.getLsrefs(), listedBy, "link segment");
      var types = findBySourceIdsIgnoringUnknown(
          MacroscopicLinkSegmentType.class, xmlSection.getTyperefs(), listedBy, "link segment type");
      if (linkSegments.isEmpty() && types.isEmpty()) {
        LOGGER.warning(String.format("DISCARD: %s lists no known link segment or link segment type", listedBy));
        continue;
      }
      var section = networkPricing.getSections().getFactory().registerNew(linkSegments, types);
      section.setXmlId(xmlSection.getId());
      section.setExternalId(xmlSection.getExternalid());
      section.setName(xmlSection.getName());
      sections.put(xmlSection.getId(), section);
    }
    return sections;
  }

  /**
   * Parse and register the gates of a scheme. A gate with an id used before is not read, nor one listing no known
   * entry or exit, each with a single warning; an unknown link segment is ignored with a single warning
   *
   * @param schemeSubject the scheme, as the warnings name it
   * @param xmlGates to parse, may be null
   * @return the gates registered, by XML id, in the order given
   */
  private Map<String, PricingSchemeGate> parseGates(String schemeSubject, XMLElementGates xmlGates) {
    var gates = new LinkedHashMap<String, PricingSchemeGate>();
    if (xmlGates == null) {
      return gates;
    }
    for (var xmlGate : xmlGates.getGates()) {
      var subject = "Gate " + xmlGate.getId();
      if (!isNewSectionOrGateId(PricingSchemeGate.class, xmlGate.getId(), subject, schemeSubject, gates)) {
        continue;
      }
      var listedBy = subject + " of " + schemeSubject;
      var entries = findBySourceIdsIgnoringUnknown(
          MacroscopicLinkSegment.class, xmlGate.getEntrylsrefs(), listedBy, "link segment");
      var exits = findBySourceIdsIgnoringUnknown(
          MacroscopicLinkSegment.class, xmlGate.getExitlsrefs(), listedBy, "link segment");
      if (entries.isEmpty() && exits.isEmpty()) {
        LOGGER.warning(String.format("DISCARD: %s lists no known entry or exit", listedBy));
        continue;
      }
      var gate = networkPricing.getGates().getFactory().registerNew(entries, exits);
      gate.setXmlId(xmlGate.getId());
      gate.setExternalId(xmlGate.getExternalid());
      gate.setName(xmlGate.getName());
      gates.put(xmlGate.getId(), gate);
    }
    return gates;
  }

  /**
   * Parse price overrides of either kind; one naming no known mode is not read, with a single warning. An override
   * giving a factor only reads as a price rule giving no amount, which the override counts as none
   *
   * @param <X> the XML override of the kind of price rule
   * @param <P> the kind of price rule
   * @param subject the scheme, as the warnings name it
   * @param xmlOverrides to parse
   * @param modeRefs gives the modes an override names
   * @param factor gives an override's factor, null when it gives amounts
   * @param parsePriceRule parses the amounts an override gives as a price rule of its kind
   * @return the overrides
   * @throws IllegalArgumentException when an override gives both amounts and a factor, or neither
   */
  private <X, P extends PriceRule<P>> List<PriceOverride<P>> parsePriceOverrides(
      String subject,
      List<X> xmlOverrides,
      Function<X, String> modeRefs,
      Function<X, BigDecimal> factor,
      Function<X, P> parsePriceRule) {
    var overrides = new ArrayList<PriceOverride<P>>();
    for (var xmlOverride : xmlOverrides) {
      var modes = findPricedModeSelection("Price override of " + subject, modeRefs.apply(xmlOverride), null);
      if (modes != null) {
        overrides.add(new PriceOverrideImpl<>(modes, parsePriceRule.apply(xmlOverride),
            toDoubleOrNull(factor.apply(xmlOverride))));
      }
    }
    return overrides;
  }

  /**
   * Parse the minimums of a pricing of either kind; one naming no known mode is not read, with a single warning
   *
   * @param subject the scheme, as the warnings name it
   * @param xmlMinimums to parse
   * @return the minimums
   */
  private List<ModesMinimumPrice> parseMinimums(String subject, List<XMLElementMinimum> xmlMinimums) {
    var minimums = new ArrayList<ModesMinimumPrice>();
    for (var xmlMinimum : xmlMinimums) {
      var modes = findPricedModeSelection("Minimum of " + subject, xmlMinimum.getModerefs(), null);
      if (modes != null) {
        minimums.add(new ModesMinimumPriceImpl(xmlMinimum.getAmount().doubleValue(), modes));
      }
    }
    return minimums;
  }

  /**
   * Parse the pricing of a mode or location scheme; its caps are registered once the scheme is
   * <p>
   * The file gives a scheme's price rules as a base price rule with overrides for some modes, time bands and distance
   * bands that change it, and minimums per use. PLANit holds them in one pricing of the scheme's kind of price rule,
   * which works out the price rule for a mode. Building it is where a part making no sense on its own is refused, as
   * its constructor throws, such as a time band entry in years or an override giving both amounts and a factor; the
   * whole scheme is then not read, as reading it without that part would change its prices.
   * </p>
   *
   * @param subject the scheme, as the warnings name it
   * @param xmlPricing to parse
   * @param sections of the scheme, by XML id
   * @return the pricing
   * @throws IllegalArgumentException when one of its parts makes no sense on its own
   */
  private SchemePricing<StartWithPerKmPriceRule> parseStartWithPerKmPricing(
      String subject,
      XMLElementStartWithPerKmPricing xmlPricing,
      Map<String, PricingSchemeSection> sections) {
    Function<XMLElementStartWithPerKmPrice, StartWithPerKmPriceRule> parsePriceRule =
        xmlPrice -> parseStartWithPerKmPriceRule(subject, xmlPrice, sections);
    var timeBands = xmlPricing.getTimebandoverrides().stream().map(xmlBand -> parseTimeBand(xmlBand.getId(),
        xmlBand.getTimebandentries(), parsePriceRule.apply(xmlBand.getPrice()), parsePriceOverrides(subject,
            xmlBand.getPriceoverrides(), XMLElementStartWithPerKmPriceOverride::getModerefs,
            XMLElementStartWithPerKmPriceOverride::getFactor, parsePriceRule::apply))).collect(Collectors.toList());
    return new StartWithPerKmSchemePricingImpl(parsePriceRule.apply(xmlPricing.getPrice()),
        parsePriceOverrides(subject, xmlPricing.getPriceoverrides(),
            XMLElementStartWithPerKmPriceOverride::getModerefs, XMLElementStartWithPerKmPriceOverride::getFactor,
            parsePriceRule::apply), timeBands,
        parseDistanceBands(xmlPricing.getDistancebandoverrides()), parseMinimums(subject, xmlPricing.getMinima()));
  }

  /**
   * Parse the pricing of an entry-exit scheme, as {@link #parseStartWithPerKmPricing} does for the other kind; its caps
   * are registered once the scheme is
   *
   * @param subject the scheme, as the warnings name it
   * @param xmlPricing to parse
   * @param gates of the scheme, by XML id
   * @return the pricing
   * @throws IllegalArgumentException when one of its parts makes no sense on its own
   */
  private SchemePricing<FromToGatesPriceRule> parseFromToGatesPricing(
      String subject,
      XMLElementGatePairPricing xmlPricing,
      Map<String, PricingSchemeGate> gates) {
    Function<XMLElementGatePairPrice, FromToGatesPriceRule> parsePriceRule =
        xmlPrice -> parseFromToGatesPriceRule(subject, xmlPrice, gates);
    var timeBands = xmlPricing.getTimebandoverrides().stream().map(xmlBand -> parseTimeBand(xmlBand.getId(),
        xmlBand.getTimebandentries(), parsePriceRule.apply(xmlBand.getPrice()), parsePriceOverrides(subject,
            xmlBand.getPriceoverrides(), XMLElementGatePairPriceOverride::getModerefs,
            XMLElementGatePairPriceOverride::getFactor, parsePriceRule::apply))).collect(Collectors.toList());
    return new FromToGatesSchemePricingImpl(parsePriceRule.apply(xmlPricing.getPrice()),
        parsePriceOverrides(subject, xmlPricing.getPriceoverrides(), XMLElementGatePairPriceOverride::getModerefs,
            XMLElementGatePairPriceOverride::getFactor, parsePriceRule::apply), timeBands,
        parseDistanceBands(xmlPricing.getDistancebandoverrides()), parseMinimums(subject, xmlPricing.getMinima()));
  }

  /**
   * Parse and register a cap. A cap with an id used before, naming no known mode, or making no sense on its own, such
   * as one giving both an amount and a count, is not read, with a single warning; a cap without an id gets its
   * internal id
   *
   * @param xmlCap to parse
   * @param schemes it covers
   * @param subject the cap, as the warnings name it
   */
  private void parseCap(XMLElementCap xmlCap, List<PricingScheme> schemes, String subject) {
    var xmlId = xmlCap.getId();
    if (xmlId != null && getBySourceId(PriceCap.class, xmlId) != null) {
      LOGGER.warning(String.format("DISCARD: %s has an id used before", subject));
      return;
    }
    var modes = findPricedModeSelection(subject, xmlCap.getModerefs(), null);
    if (modes == null) {
      return;
    }
    var amountInCurrency = toDoubleOrNull(xmlCap.getAmount());
    var problems = new Problems<PricingProblemKind>();
    var cappedModes = PriceCapUtils.limitToChargedModes(schemes, modes, problems);
    if (cappedModes != null) {
      problems.addAll(PriceCapUtils.findMinimumsAboveAmount(schemes, amountInCurrency, cappedModes));
    }
    problems.logIfAny(LOGGER, subject);
    if (cappedModes == null || problems.has(PricingProblemKind.CAP_BELOW_MINIMUM)) {
      return;
    }
    PriceCap cap;
    try {
      cap = networkPricing.getCaps().getFactory().registerNew(schemes, amountInCurrency,
          xmlCap.getActivateabovecount(), XmlEnumConversionUtil.xmlToPlanit(xmlCap.getPer()), cappedModes);
    } catch (IllegalArgumentException e) {
      LOGGER.warning(String.format("DISCARD: %s not read: %s", subject, e.getMessage()));
      return;
    }
    cap.setXmlId(xmlId == null ? String.valueOf(cap.getId()) : xmlId);
    registerBySourceId(PriceCap.class, cap);
  }

  /**
   * Parse and register the caps covering several schemes. One naming fewer than two known schemes is not read, with a
   * single warning, as a cap of one scheme belongs in that scheme's pricing; an unknown scheme is ignored with a single
   * warning
   *
   * @param xmlCaps to parse, may be null
   */
  private void parseCapsAcrossSchemes(XMLElementCaps xmlCaps) {
    if (xmlCaps == null) {
      return;
    }
    for (var xmlCap : xmlCaps.getCaps()) {
      var subject = xmlCap.getId() == null ? "Cap" : "Cap " + xmlCap.getId();
      var schemes = findBySourceIdsIgnoringUnknown(PricingScheme.class, xmlCap.getSchemerefs(), subject, "scheme");
      if (schemes.size() < 2) {
        LOGGER.warning(String.format("DISCARD: %s names fewer than two known schemes, a cap of one scheme belongs in "
            + "that scheme's pricing", subject));
        continue;
      }
      parseCap(xmlCap, schemes, subject);
    }
  }

  /**
   * Build a scheme's pricing, hand the scheme to its factory, and log the problems found once, under its XML id. A
   * scheme whose parts make no sense on their own, such as a time band entry in years, is not read, with a single
   * warning, and its sections and gates are removed. A scheme registered keeps its XML id and description, its
   * sections and gates still held theirs, and its caps follow
   *
   * @param xmlId of the scheme
   * @param description of the scheme, may be null
   * @param xmlCaps of the scheme's pricing, its own caps
   * @param subject the scheme, as the warnings name it
   * @param sections of the scheme registered, by XML id
   * @param gates of the scheme registered, by XML id
   * @param parseAndRegister parses the scheme's pricing of its kind and registers the scheme through its kind's factory
   * @return the scheme registered, null when it is not
   */
  private PricingScheme registerScheme(
      String xmlId,
      String description,
      List<XMLElementCap> xmlCaps,
      String subject,
      Map<String, PricingSchemeSection> sections,
      Map<String, PricingSchemeGate> gates,
      Supplier<Pair<? extends PricingScheme, Problems<PricingProblemKind>>> parseAndRegister) {
    Pair<? extends PricingScheme, Problems<PricingProblemKind>> registered;
    try {
      registered = parseAndRegister.get();
    } catch (IllegalArgumentException e) {
      LOGGER.warning(String.format("DISCARD: %s not read: %s", subject, e.getMessage()));
      sections.values().forEach(networkPricing.getSections()::remove);
      gates.values().forEach(networkPricing.getGates()::remove);
      return null;
    }
    registered.second().logIfAny(LOGGER, subject);
    PricingScheme scheme = registered.first();
    if (scheme == null) {
      return null;
    }
    scheme.setXmlId(xmlId);
    scheme.setDescription(description);
    registerBySourceId(PricingScheme.class, scheme);
    sections.values().stream().filter(section -> networkPricing.getSections().get(section.getId()) == section)
        .forEach(section -> registerBySourceId(PricingSchemeSection.class, section));
    gates.values().stream().filter(gate -> networkPricing.getGates().get(gate.getId()) == gate)
        .forEach(gate -> registerBySourceId(PricingSchemeGate.class, gate));
    for (var xmlCap : xmlCaps) {
      parseCap(xmlCap, List.of(scheme), (xmlCap.getId() == null ? "Cap" : "Cap " + xmlCap.getId()) + " of " + subject);
    }
    return scheme;
  }

  /**
   * Parse and register a mode scheme
   *
   * @param xmlScheme to parse
   */
  private void parseModeScheme(XMLElementModeScheme xmlScheme) {
    var subject = "Mode scheme " + xmlScheme.getId();
    if (!isNewSchemeId(xmlScheme.getId(), subject)) {
      return;
    }
    var modes = findPricedModeSelection(subject, xmlScheme.getModerefs(), xmlScheme.getExemptmoderefs());
    var pricedLinkSegments = modes == null ? null :
        parseLinkSegmentCoverage(subject, xmlScheme.getInclude(), xmlScheme.getExclude());
    if (pricedLinkSegments == null) {
      return;
    }
    var sections = parseSections(subject, xmlScheme.getSections());
    var scheme = registerScheme(xmlScheme.getId(), xmlScheme.getDescription(), xmlScheme.getPricing().getCaps(),
        subject, sections, Map.of(), () -> networkPricing.getModeSchemes().getFactory().registerNew(
            xmlScheme.getName(), modes, pricedLinkSegments, List.copyOf(sections.values()),
            parseStartWithPerKmPricing(subject, xmlScheme.getPricing(), sections)));
    if (scheme != null) {
      scheme.setExternalId(xmlScheme.getExternalid());
    }
  }

  /**
   * Parse and register a location scheme
   *
   * @param xmlScheme to parse
   */
  private void parseLocationScheme(XMLElementLocationScheme xmlScheme) {
    var subject = "Location scheme " + xmlScheme.getId();
    if (!isNewSchemeId(xmlScheme.getId(), subject)) {
      return;
    }
    var modes = findPricedModeSelection(subject, xmlScheme.getModerefs(), xmlScheme.getExemptmoderefs());
    var tollRoads = modes == null ? null : parseTollRoads(subject, xmlScheme.getTollroadrefs());
    if (tollRoads == null) {
      return;
    }
    var sections = parseSections(subject, xmlScheme.getSections());
    var scheme = registerScheme(xmlScheme.getId(), xmlScheme.getDescription(), xmlScheme.getPricing().getCaps(),
        subject, sections, Map.of(), () -> networkPricing.getLocationSchemes().getFactory().registerNew(
            xmlScheme.getName(), modes, tollRoads, List.copyOf(sections.values()),
            parseStartWithPerKmPricing(subject, xmlScheme.getPricing(), sections)));
    if (scheme != null) {
      scheme.setExternalId(xmlScheme.getExternalid());
    }
  }

  /**
   * Parse and register an entry-exit scheme
   *
   * @param xmlScheme to parse
   */
  private void parseEntryExitScheme(XMLElementEntryExitScheme xmlScheme) {
    var subject = "Entry-exit scheme " + xmlScheme.getId();
    if (!isNewSchemeId(xmlScheme.getId(), subject)) {
      return;
    }
    var modes = findPricedModeSelection(subject, xmlScheme.getModerefs(), xmlScheme.getExemptmoderefs());
    var tollRoads = modes == null ? null : parseTollRoads(subject, xmlScheme.getTollroadrefs());
    if (tollRoads == null) {
      return;
    }
    var gates = parseGates(subject, xmlScheme.getGates());
    var scheme = registerScheme(xmlScheme.getId(), xmlScheme.getDescription(), xmlScheme.getPricing().getCaps(),
        subject, Map.of(), gates, () -> networkPricing.getEntryExitSchemes().getFactory().registerNew(
            xmlScheme.getName(), modes, tollRoads, List.copyOf(gates.values()),
            parseFromToGatesPricing(subject, xmlScheme.getPricing(), gates)));
    if (scheme != null) {
      scheme.setExternalId(xmlScheme.getExternalid());
    }
  }

  /**
   * Make the XML ids of the pricing's schemes, sections, gates and caps their internal ids, recreated so they are
   * contiguous, so new entities added while editing cannot get an XML id already used
   */
  private void syncXmlIdsToIds() {
    LOGGER.info("Syncing PLANit network pricing XML ids to internally generated ids, overwriting original XML ids");
    networkPricing.getModifier().recreatePricingSchemeIds();
    networkPricing.getSections().recreateIds(true);
    networkPricing.getGates().recreateIds(true);
    networkPricing.getCaps().recreateIds(true);
    networkPricing.streamSchemes().forEach(scheme -> scheme.setXmlId(String.valueOf(scheme.getId())));
    networkPricing.getSections().forEach(section -> section.setXmlId(String.valueOf(section.getId())));
    networkPricing.getGates().forEach(gate -> gate.setXmlId(String.valueOf(gate.getId())));
    networkPricing.getCaps().forEach(cap -> cap.setXmlId(String.valueOf(cap.getId())));
  }

  /**
   * Constructor
   *
   * @param settings to use
   * @param networkPricing to populate, made on the network the file refers to
   */
  protected PlanitNetworkPricingReader(PlanitNetworkPricingReaderSettings settings, NetworkPricing networkPricing) {
    this.xmlParser = new PlanitXmlJaxbParser<>(
        XMLElementNetworkPricing.class,
        XMLElementNetworkPricing.class); // no legacy version yet
    this.settings = settings;
    this.networkPricing = networkPricing;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NetworkPricing read() {
    getSettings().logSettings();
    xmlParser.initialiseAndParseXmlRootElement(getSettings().getInputSource(), getSettings().getXmlFileExtension());
    var xmlPricing = xmlParser.getXmlRootElement();
    PlanItRunTimeException.throwIfNull(xmlPricing, "No valid PLANit XML network pricing could be parsed into memory");

    var xmlId = xmlPricing.getId();
    if (StringUtils.isNullOrBlank(xmlId)) {
      LOGGER.warning(String.format("Network pricing has no XML id defined, adopting internally generated id %d " +
          "instead", networkPricing.getId()));
      xmlId = String.valueOf(networkPricing.getId());
    }
    networkPricing.setXmlId(xmlId);
    networkPricing.setExternalId(xmlPricing.getExternalid());
    checkParentNetwork(xmlPricing.getParentnetwork());
    networkPricing.setCurrency(Currency.getInstance(xmlPricing.getCurrency()));
    networkPricing.setYear(Year.of(xmlPricing.getYear().getYear()));

    initialiseParentXmlIdTrackers(networkPricing.getParentNetwork());
    initialiseXmlIdTrackers();
    if (xmlPricing.getModeschemes() != null) {
      xmlPricing.getModeschemes().getModeschemes().forEach(this::parseModeScheme);
    }
    if (xmlPricing.getLocationschemes() != null) {
      xmlPricing.getLocationschemes().getLocationschemes().forEach(this::parseLocationScheme);
    }
    if (xmlPricing.getEntryexitschemes() != null) {
      xmlPricing.getEntryexitschemes().getEntryexitschemes().forEach(this::parseEntryExitScheme);
    }
    parseCapsAcrossSchemes(xmlPricing.getCaps());

    if (getSettings().isSyncXmlIdsToIds()) {
      syncXmlIdsToIds();
    }
    networkPricing.logInfo(LoggingUtils.networkPricingPrefix(networkPricing.getId()));
    xmlParser.clearXmlContent();
    return networkPricing;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitNetworkPricingReaderSettings getSettings() {
    return settings;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
    xmlParser.clearXmlContent();
  }
}
