package org.goplanit.io.converter.pricing;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.idmapping.NetworkPricingIdMapper;
import org.goplanit.converter.pricing.NetworkPricingWriter;
import org.goplanit.io.converter.PlanitWriterImpl;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.io.xml.util.XmlEnumConversionUtil;
import org.goplanit.pricing.NetworkPricing;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.goplanit.utils.pricing.FromToGatesPriceRule;
import org.goplanit.utils.pricing.ModePricingSchemeLinkSegmentCoverage;
import org.goplanit.utils.pricing.ModesMinimumPrice;
import org.goplanit.utils.pricing.PriceOverride;
import org.goplanit.utils.pricing.PricingDistanceBand;
import org.goplanit.utils.pricing.PricingScheme;
import org.goplanit.utils.pricing.PricingSchemeGate;
import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.pricing.PricingTimeBandEntry;
import org.goplanit.utils.pricing.SchemePricing;
import org.goplanit.utils.pricing.StartWithPerKmPriceRule;
import org.goplanit.xml.generated.v2.XMLElementCap;
import org.goplanit.xml.generated.v2.XMLElementCaps;
import org.goplanit.xml.generated.v2.XMLElementDistanceBandOverride;
import org.goplanit.xml.generated.v2.XMLElementEntryExitScheme;
import org.goplanit.xml.generated.v2.XMLElementEntryExitSchemes;
import org.goplanit.xml.generated.v2.XMLElementExclude;
import org.goplanit.xml.generated.v2.XMLElementGate;
import org.goplanit.xml.generated.v2.XMLElementGatePairPrice;
import org.goplanit.xml.generated.v2.XMLElementGatePairPriceOverride;
import org.goplanit.xml.generated.v2.XMLElementGatePairPricing;
import org.goplanit.xml.generated.v2.XMLElementGatePairTimeBandOverride;
import org.goplanit.xml.generated.v2.XMLElementGates;
import org.goplanit.xml.generated.v2.XMLElementInclude;
import org.goplanit.xml.generated.v2.XMLElementLocationScheme;
import org.goplanit.xml.generated.v2.XMLElementLocationSchemes;
import org.goplanit.xml.generated.v2.XMLElementMinimum;
import org.goplanit.xml.generated.v2.XMLElementModeScheme;
import org.goplanit.xml.generated.v2.XMLElementModeSchemes;
import org.goplanit.xml.generated.v2.XMLElementNetworkPricing;
import org.goplanit.xml.generated.v2.XMLElementPair;
import org.goplanit.xml.generated.v2.XMLElementSection;
import org.goplanit.xml.generated.v2.XMLElementSectionPrice;
import org.goplanit.xml.generated.v2.XMLElementSections;
import org.goplanit.xml.generated.v2.XMLElementTimeBandEntry;
import org.goplanit.xml.generated.v2.XMLElementStartWithPerKmPrice;
import org.goplanit.xml.generated.v2.XMLElementStartWithPerKmPriceOverride;
import org.goplanit.xml.generated.v2.XMLElementStartWithPerKmPricing;
import org.goplanit.xml.generated.v2.XMLElementStartWithPerKmTimeBandOverride;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.goplanit.utils.pricing.PriceRule;

import static org.goplanit.utils.math.TypeConversionUtil.toBigDecimalOrNull;

/**
 * Writes a network pricing in the native PLANit XML format
 * <p>
 * The file names the pricing's network in {@code <parentnetwork>} and refers to its layers, link segment types, link
 * segments, modes and toll roads by the ids the network writer writes, so the two files are read back together. It
 * writes everything the pricing holds and leaves out what is not set: a mode scheme's own priced link segments as
 * {@code <include>} and {@code <exclude>}, the toll roads of a location or entry-exit scheme as {@code tollroadrefs},
 * and each cap in the pricing of the one scheme it covers, or among the top-level caps when it covers several.
 * </p>
 *
 * @author markr
 */
public class PlanitNetworkPricingWriter extends PlanitWriterImpl<NetworkPricing> implements NetworkPricingWriter {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkPricingWriter.class.getCanonicalName());

  /**
   * The ids of entities, sorted and comma-separated
   *
   * @param <T> type of entity
   * @param entities to list
   * @param idMapper giving each entity's id
   * @return the ids, null when there are none so the attribute is left out
   */
  private static <T> String toRefs(Collection<? extends T> entities, Function<? super T, String> idMapper) {
    return entities.isEmpty() ? null : StringUtils.toSortedCommaSeparatedString(entities, idMapper::apply);
  }

  /**
   * Create the XML of a time band entry
   *
   * @param entry to write
   * @return the XML entry
   */
  private static XMLElementTimeBandEntry createXmlTimeBandEntry(PricingTimeBandEntry entry) {
    var xmlEntry = new XMLElementTimeBandEntry();
    entry.getDays().stream()
        .sorted()
        .map(XmlEnumConversionUtil::planitToXml)
        .forEach(xmlEntry.getDays()::add);
    xmlEntry.setPublicholidays(XmlEnumConversionUtil.planitToXmlOrNullWhenIncluded(entry.getPublicHolidays()));
    xmlEntry.setSchoolholidays(XmlEnumConversionUtil.planitToXmlOrNullWhenIncluded(entry.getSchoolHolidays()));
    var unit = entry.getPersistTemporalUnit();
    int unitMinutes = (int) unit.getDuration().toMinutes();
    xmlEntry.setTemporalunit(XmlEnumConversionUtil.planitToXml(unit));
    int multiplier = entry.getDurationMinutes() / unitMinutes;
    xmlEntry.setTemporalunitmultiplier(multiplier == 1 ? null : multiplier);
    xmlEntry.setTemporaloffset(entry.getStartMinuteOfDay() / unitMinutes);
    return xmlEntry;
  }

  /**
   * Create the XML of the distance bands of a pricing of either kind
   *
   * @param distanceBands to write
   * @return the XML distance bands
   */
  private static List<XMLElementDistanceBandOverride> createXmlDistanceBands(List<PricingDistanceBand> distanceBands) {
    var xmlBands = new ArrayList<XMLElementDistanceBandOverride>(distanceBands.size());
    for (var distanceBand : distanceBands) {
      var xmlBand = new XMLElementDistanceBandOverride();
      xmlBand.setPer(XmlEnumConversionUtil.planitToXmlDistanceBandPeriod(distanceBand.getPeriod()));
      double fromKm = distanceBand.getRange().getLowerBound();
      double toKm = distanceBand.getRange().getUpperBound();
      xmlBand.setFromkm(fromKm > 0 ? BigDecimal.valueOf(fromKm) : null);
      xmlBand.setTokm(Double.isInfinite(toKm) ? null : BigDecimal.valueOf(toKm));
      xmlBand.setFactor(BigDecimal.valueOf(distanceBand.getDistanceBandFactor()));
      xmlBands.add(xmlBand);
    }
    return xmlBands;
  }

  /** the settings to use */
  private final PlanitNetworkPricingWriterSettings settings;

  /** the XML memory model to populate and persist */
  private final XMLElementNetworkPricing xmlRawNetworkPricing;

  /**
   * The id mappers of the network the pricing refers to
   *
   * @return the network's id mappers
   */
  private NetworkIdMapper getNetworkIdMapper() {
    return getComponentIdMappers().getNetworkIdMappers();
  }

  /**
   * Write the modes that pay as given, named or exempted, leaving both out when neither is given
   *
   * @param modes to write
   * @param setModeRefs sets the named modes
   * @param setExemptModeRefs sets the exempted modes, null when the element takes named modes only
   */
  private void populateModeRefs(
      PricedModeSelection modes,
      Consumer<String> setModeRefs,
      Consumer<String> setExemptModeRefs) {
    var modeIdMapper = getNetworkIdMapper().getModeIdMapper();
    setModeRefs.accept(toRefs(modes.getPricedModes(), modeIdMapper));
    if (setExemptModeRefs != null) {
      setExemptModeRefs.accept(toRefs(modes.getExemptedModes(), modeIdMapper));
    }
  }

  /**
   * Populate the amounts of a mode or location scheme's price rule: its start, per km and section price rules
   *
   * @param xmlPrice to populate; an override is one too
   * @param priceRule to populate from
   */
  private void populateStartWithPerKmAmounts(
      XMLElementStartWithPerKmPrice xmlPrice,
      StartWithPerKmPriceRule priceRule) {
    xmlPrice.setStart(toBigDecimalOrNull(priceRule.getStartAmountInCurrency()));
    xmlPrice.setPerkm(toBigDecimalOrNull(priceRule.getPerKmAmountInCurrency()));
    var sectionIdMapper = getPrimaryIdMapper().getSectionIdMapper();
    for (var sectionPriceRule : priceRule.getSectionPriceRules()) {
      var xmlSectionPrice = new XMLElementSectionPrice();
      xmlSectionPrice.setSectionref(sectionIdMapper.apply(sectionPriceRule.getSection()));
      xmlSectionPrice.setStart(toBigDecimalOrNull(sectionPriceRule.getStartAmountInCurrency()));
      xmlSectionPrice.setPerkm(toBigDecimalOrNull(sectionPriceRule.getPerKmAmountInCurrency()));
      xmlPrice.getSectionprices().add(xmlSectionPrice);
    }
  }

  /**
   * Populate the amounts of an entry-exit scheme's price rule: its gate from-to amounts
   *
   * @param xmlPrice to populate; an override is one too
   * @param priceRule to populate from
   */
  private void populateGateFromToAmounts(XMLElementGatePairPrice xmlPrice, FromToGatesPriceRule priceRule) {
    var gateIdMapper = getPrimaryIdMapper().getGateIdMapper();
    for (var gatePair : priceRule.getPricedFromToGates()) {
      var xmlPair = new XMLElementPair();
      xmlPair.setFromgateref(gateIdMapper.apply(gatePair.getFromGate()));
      xmlPair.setTogateref(gateIdMapper.apply(gatePair.getToGate()));
      xmlPair.setAmount(BigDecimal.valueOf(gatePair.getAmountInCurrency()));
      xmlPrice.getPairs().add(xmlPair);
    }
  }

  /**
   * Create the XML of the minimums of a pricing of either kind
   *
   * @param minimums to write
   * @return the XML minimums
   */
  private List<XMLElementMinimum> createXmlMinimums(List<ModesMinimumPrice> minimums) {
    return minimums.stream().map(minimum -> {
      var xmlMinimum = new XMLElementMinimum();
      xmlMinimum.setAmount(BigDecimal.valueOf(minimum.getAmountInCurrency()));
      populateModeRefs(minimum.getPricedModeSelection(), xmlMinimum::setModerefs, null);
      return xmlMinimum;
    }).collect(Collectors.toList());
  }

  /**
   * Create the XML of a mode or location scheme's price rule
   *
   * @param priceRule to write, may be null
   * @return the XML price, null when there is no price rule
   */
  private XMLElementStartWithPerKmPrice createXmlStartWithPerKmPriceRule(StartWithPerKmPriceRule priceRule) {
    if (priceRule == null) {
      return null;
    }
    var xmlPrice = new XMLElementStartWithPerKmPrice();
    populateStartWithPerKmAmounts(xmlPrice, priceRule);
    return xmlPrice;
  }

  /**
   * Create the XML of a mode or location scheme's price overrides, each with its price rule's amounts or its factor
   *
   * @param priceOverrides to write
   * @return the XML price overrides
   */
  private List<XMLElementStartWithPerKmPriceOverride> createXmlStartWithPerKmPriceOverrides(
      List<PriceOverride<StartWithPerKmPriceRule>> priceOverrides) {
    return createXmlPriceOverrides(priceOverrides, XMLElementStartWithPerKmPriceOverride::new,
        this::populateStartWithPerKmAmounts, XMLElementStartWithPerKmPriceOverride::setModerefs,
        XMLElementStartWithPerKmPriceOverride::setFactor);
  }

  /**
   * Create the XML of price overrides of either kind, each with its price rule's amounts or its factor, as
   * {@code parsePriceOverrides} reads them
   *
   * @param <X> the XML override of the kind of price rule
   * @param <P> the kind of price rule
   * @param priceOverrides to write
   * @param createXmlOverride makes an empty XML override
   * @param populateAmounts writes a price rule's amounts into an XML override
   * @param setModeRefs sets an XML override's mode refs
   * @param setFactor sets an XML override's factor
   * @return the XML price overrides
   */
  private <X, P extends PriceRule<P>> List<X> createXmlPriceOverrides(
      List<PriceOverride<P>> priceOverrides,
      Supplier<X> createXmlOverride,
      BiConsumer<X, P> populateAmounts,
      BiConsumer<X, String> setModeRefs,
      BiConsumer<X, BigDecimal> setFactor) {
    return priceOverrides.stream().map(priceOverride -> {
      var xmlOverride = createXmlOverride.get();
      if (priceOverride.getPriceRule() != null) {
        populateAmounts.accept(xmlOverride, priceOverride.getPriceRule());
      }
      populateModeRefs(priceOverride.getPricedModeOverrides(), modeRefs -> setModeRefs.accept(xmlOverride, modeRefs),
          null);
      setFactor.accept(xmlOverride, toBigDecimalOrNull(priceOverride.getPriceOverrideFactor()));
      return xmlOverride;
    }).collect(Collectors.toList());
  }

  /**
   * Create the XML of a mode or location scheme's pricing, without its caps, which are placed once every scheme is
   * written
   *
   * @param pricing to write
   * @return the XML pricing
   */
  private XMLElementStartWithPerKmPricing createXmlStartWithPerKmPricing(
      SchemePricing<StartWithPerKmPriceRule> pricing) {
    var xmlPricing = new XMLElementStartWithPerKmPricing();
    xmlPricing.setPrice(createXmlStartWithPerKmPriceRule(pricing.getBasePriceRule()));
    xmlPricing.getPriceoverrides().addAll(createXmlStartWithPerKmPriceOverrides(pricing.getPriceOverrides()));
    for (var timeBand : pricing.getTimeBands()) {
      var xmlBand = new XMLElementStartWithPerKmTimeBandOverride();
      xmlBand.setId(timeBand.getXmlId());
      timeBand.getEntries().stream().map(PlanitNetworkPricingWriter::createXmlTimeBandEntry)
          .forEach(xmlBand.getTimebandentries()::add);
      xmlBand.setPrice(createXmlStartWithPerKmPriceRule(timeBand.getPriceRule()));
      xmlBand.getPriceoverrides().addAll(createXmlStartWithPerKmPriceOverrides(timeBand.getPriceOverrides()));
      xmlPricing.getTimebandoverrides().add(xmlBand);
    }
    xmlPricing.getDistancebandoverrides().addAll(createXmlDistanceBands(pricing.getDistanceBands()));
    xmlPricing.getMinima().addAll(createXmlMinimums(pricing.getModesMinimumPrices()));
    return xmlPricing;
  }

  /**
   * Create the XML of an entry-exit scheme's price rule
   *
   * @param priceRule to write, may be null
   * @return the XML price, null when there is no price rule
   */
  private XMLElementGatePairPrice createXmlFromToGatesPriceRule(FromToGatesPriceRule priceRule) {
    if (priceRule == null) {
      return null;
    }
    var xmlPrice = new XMLElementGatePairPrice();
    populateGateFromToAmounts(xmlPrice, priceRule);
    return xmlPrice;
  }

  /**
   * Create the XML of an entry-exit scheme's price overrides, each with its price rule's gate from-to amounts or its
   * factor
   *
   * @param priceOverrides to write
   * @return the XML price overrides
   */
  private List<XMLElementGatePairPriceOverride> createXmlFromToGatesPriceOverrides(
      List<PriceOverride<FromToGatesPriceRule>> priceOverrides) {
    return createXmlPriceOverrides(priceOverrides, XMLElementGatePairPriceOverride::new,
        this::populateGateFromToAmounts, XMLElementGatePairPriceOverride::setModerefs,
        XMLElementGatePairPriceOverride::setFactor);
  }

  /**
   * Create the XML of an entry-exit scheme's pricing, as {@link #createXmlStartWithPerKmPricing} does for the other
   * kind
   *
   * @param pricing to write
   * @return the XML pricing
   */
  private XMLElementGatePairPricing createXmlFromToGatesPricing(SchemePricing<FromToGatesPriceRule> pricing) {
    var xmlPricing = new XMLElementGatePairPricing();
    xmlPricing.setPrice(createXmlFromToGatesPriceRule(pricing.getBasePriceRule()));
    xmlPricing.getPriceoverrides().addAll(createXmlFromToGatesPriceOverrides(pricing.getPriceOverrides()));
    for (var timeBand : pricing.getTimeBands()) {
      var xmlBand = new XMLElementGatePairTimeBandOverride();
      xmlBand.setId(timeBand.getXmlId());
      timeBand.getEntries().stream().map(PlanitNetworkPricingWriter::createXmlTimeBandEntry)
          .forEach(xmlBand.getTimebandentries()::add);
      xmlBand.setPrice(createXmlFromToGatesPriceRule(timeBand.getPriceRule()));
      xmlBand.getPriceoverrides().addAll(createXmlFromToGatesPriceOverrides(timeBand.getPriceOverrides()));
      xmlPricing.getTimebandoverrides().add(xmlBand);
    }
    xmlPricing.getDistancebandoverrides().addAll(createXmlDistanceBands(pricing.getDistanceBands()));
    xmlPricing.getMinima().addAll(createXmlMinimums(pricing.getModesMinimumPrices()));
    return xmlPricing;
  }

  /**
   * Create the XML of the sections of a scheme
   *
   * @param sections to write
   * @return the XML sections, null when there are none
   */
  private XMLElementSections createXmlSections(List<PricingSchemeSection> sections) {
    if (sections.isEmpty()) {
      return null;
    }
    var xmlSections = new XMLElementSections();
    for (var section : sections) {
      var xmlSection = new XMLElementSection();
      xmlSection.setId(getPrimaryIdMapper().getSectionIdMapper().apply(section));
      xmlSection.setExternalid(section.hasExternalId() ? section.getExternalId() : null);
      xmlSection.setName(section.getName());
      xmlSection.setLsrefs(toRefs(section.getLinkSegments(), getNetworkIdMapper().getMacroscopicLinkSegmentIdMapper()));
      xmlSection.setTyperefs(toRefs(section.getLinkSegmentTypes(), getNetworkIdMapper().getLinkSegmentTypeIdMapper()));
      xmlSections.getSections().add(xmlSection);
    }
    return xmlSections;
  }

  /**
   * Create the XML of the gates of a scheme
   *
   * @param gates to write
   * @return the XML gates, null when there are none
   */
  private XMLElementGates createXmlGates(List<PricingSchemeGate> gates) {
    if (gates.isEmpty()) {
      return null;
    }
    var xmlGates = new XMLElementGates();
    var linkSegmentIdMapper = getNetworkIdMapper().getMacroscopicLinkSegmentIdMapper();
    for (var gate : gates) {
      var xmlGate = new XMLElementGate();
      xmlGate.setId(getPrimaryIdMapper().getGateIdMapper().apply(gate));
      xmlGate.setExternalid(gate.hasExternalId() ? gate.getExternalId() : null);
      xmlGate.setName(gate.getName());
      xmlGate.setEntrylsrefs(toRefs(gate.getEntryLinkSegments(), linkSegmentIdMapper));
      xmlGate.setExitlsrefs(toRefs(gate.getExitLinkSegments(), linkSegmentIdMapper));
      xmlGates.getGates().add(xmlGate);
    }
    return xmlGates;
  }

  /**
   * The toll roads of a location or entry-exit scheme, in the order it holds them, comma-separated
   *
   * @param tollRoads to list
   * @return the toll road ids
   */
  private String toTollRoadRefs(List<TollRoad> tollRoads) {
    return StringUtils.toCommaSeparatedString(tollRoads, getNetworkIdMapper().getTollRoadIdMapper());
  }

  /**
   * Populate a mode scheme's own priced link segments as include and exclude, leaving out what gives nothing
   *
   * @param xmlScheme to populate
   * @param coverage to write
   */
  private void populatePricedLinkSegments(
      XMLElementModeScheme xmlScheme,
      ModePricingSchemeLinkSegmentCoverage coverage) {
    var networkIdMapper = getNetworkIdMapper();
    if (!coverage.getConsideredLayers().isEmpty() || !coverage.getIncludedLinkSegmentTypes().isEmpty() ||
        !coverage.getIncludedLinkSegments().isEmpty()) {
      var xmlInclude = new XMLElementInclude();
      xmlInclude.setLayerrefs(toRefs(coverage.getConsideredLayers(), networkIdMapper.getNetworkLayerIdMapper()));
      xmlInclude.setTyperefs(
          toRefs(coverage.getIncludedLinkSegmentTypes(), networkIdMapper.getLinkSegmentTypeIdMapper()));
      xmlInclude.setLsrefs(
          toRefs(coverage.getIncludedLinkSegments(), networkIdMapper.getMacroscopicLinkSegmentIdMapper()));
      xmlScheme.setInclude(xmlInclude);
    }
    if (!coverage.getExcludedLinkSegmentTypes().isEmpty() || !coverage.getExcludedLinkSegments().isEmpty()) {
      var xmlExclude = new XMLElementExclude();
      xmlExclude.setTyperefs(
          toRefs(coverage.getExcludedLinkSegmentTypes(), networkIdMapper.getLinkSegmentTypeIdMapper()));
      xmlExclude.setLsrefs(
          toRefs(coverage.getExcludedLinkSegments(), networkIdMapper.getMacroscopicLinkSegmentIdMapper()));
      xmlScheme.setExclude(xmlExclude);
    }
  }

  /**
   * Populate the mode schemes, keeping the XML pricing of each so its caps can be placed in it
   *
   * @param networkPricing to write
   * @param xmlCapsOf the XML caps of each scheme written, to add to
   */
  private void populateXmlModeSchemes(
      NetworkPricing networkPricing,
      Map<PricingScheme, List<XMLElementCap>> xmlCapsOf) {
    if (networkPricing.getModeSchemes().isEmpty()) {
      return;
    }
    var xmlSchemes = new XMLElementModeSchemes();
    networkPricing.getModeSchemes().streamSortedBy(getPrimaryIdMapper().getSchemeIdMapper()).forEach(scheme -> {
      var xmlScheme = new XMLElementModeScheme();
      xmlScheme.setId(getPrimaryIdMapper().getSchemeIdMapper().apply(scheme));
      xmlScheme.setExternalid(scheme.hasExternalId() ? scheme.getExternalId() : null);
      xmlScheme.setName(scheme.getName());
      xmlScheme.setDescription(scheme.getDescription());
      populateModeRefs(scheme.getPricedModeSelection(), xmlScheme::setModerefs, xmlScheme::setExemptmoderefs);
      populatePricedLinkSegments(xmlScheme, scheme.getLinkSegmentCoverage());
      xmlScheme.setSections(createXmlSections(scheme.getSections()));
      xmlScheme.setPricing(createXmlStartWithPerKmPricing(scheme.getPricing()));
      xmlCapsOf.put(scheme, xmlScheme.getPricing().getCaps());
      xmlSchemes.getModeschemes().add(xmlScheme);
    });
    xmlRawNetworkPricing.setModeschemes(xmlSchemes);
  }

  /**
   * Populate the location schemes, keeping the XML pricing of each so its caps can be placed in it
   *
   * @param networkPricing to write
   * @param xmlCapsOf the XML caps of each scheme written, to add to
   */
  private void populateXmlLocationSchemes(
      NetworkPricing networkPricing,
      Map<PricingScheme, List<XMLElementCap>> xmlCapsOf) {
    if (networkPricing.getLocationSchemes().isEmpty()) {
      return;
    }
    var xmlSchemes = new XMLElementLocationSchemes();
    networkPricing.getLocationSchemes().streamSortedBy(getPrimaryIdMapper().getSchemeIdMapper()).forEach(scheme -> {
      var xmlScheme = new XMLElementLocationScheme();
      xmlScheme.setId(getPrimaryIdMapper().getSchemeIdMapper().apply(scheme));
      xmlScheme.setExternalid(scheme.hasExternalId() ? scheme.getExternalId() : null);
      xmlScheme.setName(scheme.getName());
      xmlScheme.setDescription(scheme.getDescription());
      populateModeRefs(scheme.getPricedModeSelection(), xmlScheme::setModerefs, xmlScheme::setExemptmoderefs);
      xmlScheme.setTollroadrefs(toTollRoadRefs(scheme.getTollRoads()));
      xmlScheme.setSections(createXmlSections(scheme.getSections()));
      xmlScheme.setPricing(createXmlStartWithPerKmPricing(scheme.getPricing()));
      xmlCapsOf.put(scheme, xmlScheme.getPricing().getCaps());
      xmlSchemes.getLocationschemes().add(xmlScheme);
    });
    xmlRawNetworkPricing.setLocationschemes(xmlSchemes);
  }

  /**
   * Populate the entry-exit schemes, keeping the XML pricing of each so its caps can be placed in it
   *
   * @param networkPricing to write
   * @param xmlCapsOf the XML caps of each scheme written, to add to
   */
  private void populateXmlEntryExitSchemes(
      NetworkPricing networkPricing,
      Map<PricingScheme, List<XMLElementCap>> xmlCapsOf) {
    if (networkPricing.getEntryExitSchemes().isEmpty()) {
      return;
    }
    var xmlSchemes = new XMLElementEntryExitSchemes();
    networkPricing.getEntryExitSchemes().streamSortedBy(getPrimaryIdMapper().getSchemeIdMapper()).forEach(scheme -> {
      var xmlScheme = new XMLElementEntryExitScheme();
      xmlScheme.setId(getPrimaryIdMapper().getSchemeIdMapper().apply(scheme));
      xmlScheme.setExternalid(scheme.hasExternalId() ? scheme.getExternalId() : null);
      xmlScheme.setName(scheme.getName());
      xmlScheme.setDescription(scheme.getDescription());
      populateModeRefs(scheme.getPricedModeSelection(), xmlScheme::setModerefs, xmlScheme::setExemptmoderefs);
      xmlScheme.setTollroadrefs(toTollRoadRefs(scheme.getTollRoads()));
      xmlScheme.setGates(createXmlGates(scheme.getGates()));
      xmlScheme.setPricing(createXmlFromToGatesPricing(scheme.getPricing()));
      xmlCapsOf.put(scheme, xmlScheme.getPricing().getCaps());
      xmlSchemes.getEntryexitschemes().add(xmlScheme);
    });
    xmlRawNetworkPricing.setEntryexitschemes(xmlSchemes);
  }

  /**
   * Populate the caps: each in the pricing of the one scheme it covers, or among the top-level caps, naming its
   * schemes, when it covers several
   *
   * @param networkPricing to write
   * @param xmlCapsOf the XML caps of each scheme written
   */
  private void populateXmlCaps(NetworkPricing networkPricing, Map<PricingScheme, List<XMLElementCap>> xmlCapsOf) {
    var xmlCaps = new XMLElementCaps();
    var capIdMapper = getPrimaryIdMapper().getCapIdMapper();
    networkPricing.getCaps().streamSortedBy(capIdMapper).forEach(cap -> {
      var xmlCap = new XMLElementCap();
      xmlCap.setId(capIdMapper.apply(cap));
      xmlCap.setAmount(toBigDecimalOrNull(cap.getAmountInCurrency()));
      xmlCap.setActivateabovecount(cap.getAmountInCurrency() == null ? cap.getActivateAboveCount() : null);
      xmlCap.setPer(XmlEnumConversionUtil.planitToXmlCapPeriod(cap.getPeriod()));
      populateModeRefs(cap.getPricedModeSelection(), xmlCap::setModerefs, null);
      if (cap.getSchemes().size() == 1) {
        xmlCapsOf.get(cap.getSchemes().get(0)).add(xmlCap);
      } else {
        xmlCap.setSchemerefs(toRefs(cap.getSchemes(), getPrimaryIdMapper().getSchemeIdMapper()));
        xmlCaps.getCaps().add(xmlCap);
      }
    });
    xmlRawNetworkPricing.setCaps(xmlCaps.getCaps().isEmpty() ? null : xmlCaps);
  }

  /**
   * Populate the top level of the file: the pricing's ids, its network, currency and year
   *
   * @param networkPricing to write
   */
  private void populateTopLevelElement(NetworkPricing networkPricing) {
    xmlRawNetworkPricing.setId(getPrimaryIdMapper().getNetworkPricingIdMapper().apply(networkPricing));
    xmlRawNetworkPricing.setExternalid(networkPricing.hasExternalId() ? networkPricing.getExternalId() : null);
    xmlRawNetworkPricing.setParentnetwork(
        getNetworkIdMapper().getNetworkIdMapper().apply(networkPricing.getParentNetwork()));
    xmlRawNetworkPricing.setCurrency(networkPricing.getCurrency().getCurrencyCode());
    try {
      xmlRawNetworkPricing.setYear(DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
          networkPricing.getYear().getValue(), DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED,
          DatatypeConstants.FIELD_UNDEFINED));
    } catch (DatatypeConfigurationException e) {
      throw new PlanItRunTimeException("Unable to write the year of network pricing %s", networkPricing.getXmlId());
    }
  }

  /**
   * Constructor
   *
   * @param settings to use
   * @param xmlRawNetworkPricing to populate and persist
   */
  protected PlanitNetworkPricingWriter(
      PlanitNetworkPricingWriterSettings settings,
      XMLElementNetworkPricing xmlRawNetworkPricing) {
    super(IdMapperType.XML);
    this.settings = settings;
    this.xmlRawNetworkPricing = xmlRawNetworkPricing;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NetworkPricingIdMapper getPrimaryIdMapper() {
    return getComponentIdMappers().getNetworkPricingIdMapper();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(NetworkPricing networkPricing) {
    PlanItRunTimeException.throwIfNull(networkPricing, "Network pricing is null, cannot write it in native format");
    getComponentIdMappers().populateMissingIdMappers(getIdMapperType());
    getSettings().logSettings();
    LOGGER.info(String.format("Persisting PLANit network pricing to: %s",
        Paths.get(getSettings().getOutputDirectory(), getSettings().getFileName())));

    populateTopLevelElement(networkPricing);
    Map<PricingScheme, List<XMLElementCap>> xmlCapsOf = new IdentityHashMap<>();
    populateXmlModeSchemes(networkPricing, xmlCapsOf);
    populateXmlLocationSchemes(networkPricing, xmlCapsOf);
    populateXmlEntryExitSchemes(networkPricing, xmlCapsOf);
    populateXmlCaps(networkPricing, xmlCapsOf);

    super.persist(xmlRawNetworkPricing, XMLElementNetworkPricing.class, PlanitSchema.NETWORK_PRICING_XSD);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    xmlRawNetworkPricing.setId(null);
    xmlRawNetworkPricing.setExternalid(null);
    xmlRawNetworkPricing.setModeschemes(null);
    xmlRawNetworkPricing.setLocationschemes(null);
    xmlRawNetworkPricing.setEntryexitschemes(null);
    xmlRawNetworkPricing.setCaps(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitNetworkPricingWriterSettings getSettings() {
    return settings;
  }
}
