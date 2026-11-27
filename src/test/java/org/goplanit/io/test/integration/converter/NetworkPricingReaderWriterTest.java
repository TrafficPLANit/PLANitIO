package org.goplanit.io.test.integration.converter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.io.converter.pricing.PlanitNetworkPricingReader;
import org.goplanit.io.converter.pricing.PlanitNetworkPricingReaderFactory;
import org.goplanit.io.converter.pricing.PlanitNetworkPricingReaderSettings;
import org.goplanit.io.converter.pricing.PlanitNetworkPricingWriterFactory;
import org.goplanit.io.input.PlanItInputBuilder;
import org.goplanit.io.test.util.PlanItIOTestHelper;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.pricing.NetworkPricing;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.pricing.PriceRule;
import org.goplanit.utils.pricing.FromToGatesPriceRule;
import org.goplanit.utils.pricing.PriceCap;
import org.goplanit.utils.time.TimeInWeekWithDayType;
import org.goplanit.utils.pricing.PricingPeriod;
import org.goplanit.utils.pricing.PricingScheme;
import org.goplanit.utils.pricing.PricingSchemeSection;
import org.goplanit.utils.pricing.StartWithPerKmPriceRule;
import org.goplanit.utils.test.LogRecorder;
import org.goplanit.xml.generated.v2.TemporalUnitType;
import org.goplanit.xml.generated.v2.XMLElementNetworkPricing;
import org.goplanit.xml.utils.JAXBUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests reading and writing a network pricing in the native PLANit format, on the example network of the road pricing
 * research note: what the reader skips or discards with one warning each, the pricing read, the round trip, and the
 * worked cases of the research note's example
 * <p>
 * Each problem has a small pricing file of its own in {@code cases}. The checks the schema makes, an invalid currency,
 * a location scheme without toll roads, a network data file validating as before, and a {@code <price>} with the
 * amounts of the other kind of scheme (the two files in {@code schema}), are not made here: the Java validator times
 * out fetching the GML schemas the PLANit schemas import, so they are checked against the schemas with another
 * validator.
 * </p>
 *
 * @author markr
 */
public class NetworkPricingReaderWriterTest extends NetworkReaderWriterTestBase {

  /** the logger */
  private static Logger LOGGER = null;

  /** location of the network pricing test cases */
  private static final Path PRICING_PATH = Path.of(TEST_CASE_PATH.toString(), "networkpricing");

  /** tolerance on amounts */
  private static final double DELTA = 1e-9;

  /**
   * A moment that is no holiday
   *
   * @param day of the week
   * @param hour of the day
   * @param minute of the hour
   * @return the moment
   */
  private static TimeInWeekWithDayType at(DayOfWeek day, int hour, int minute) {
    return TimeInWeekWithDayType.of(day, hour * 60 + minute, false, false);
  }

  /**
   * The XML ids of the link segments a scheme charges on
   *
   * @param scheme to ask
   * @return their XML ids
   */
  private static Set<String> pricedLinkSegmentIds(PricingScheme scheme) {
    return xmlIds(scheme.streamPricedLinkSegments().collect(Collectors.toList()));
  }

  /**
   * The section of a list with the given XML id
   *
   * @param sections to look in
   * @param xmlId of the section
   * @return the section
   */
  private static PricingSchemeSection sectionOf(List<PricingSchemeSection> sections, String xmlId) {
    return sections.stream()
        .filter(section -> xmlId.equals(section.getXmlId()))
        .findFirst()
        .orElseThrow();
  }

  /**
   * Check two price rules of the same kind give the same amounts: start, rate and section price rules, or gate
   * from-to amounts
   *
   * @param expected price rule
   * @param actual price rule
   */
  private static void assertSamePriceRule(PriceRule<?> expected, PriceRule<?> actual) {
    assertEquals(expected == null, actual == null);
    if (expected instanceof StartWithPerKmPriceRule) {
      var expectedRule = (StartWithPerKmPriceRule) expected;
      var actualRule = (StartWithPerKmPriceRule) actual;
      assertEquals(expectedRule.getStartAmountInCurrency(), actualRule.getStartAmountInCurrency());
      assertEquals(expectedRule.getPerKmAmountInCurrency(), actualRule.getPerKmAmountInCurrency());
      assertEquals(expectedRule.getSectionPriceRules().size(), actualRule.getSectionPriceRules().size());
      for (int index = 0; index < expectedRule.getSectionPriceRules().size(); index++) {
        var expectedSectionPriceRule = expectedRule.getSectionPriceRules().get(index);
        var actualSectionPriceRule = actualRule.getSectionPriceRules().get(index);
        assertEquals(expectedSectionPriceRule.getSection().getXmlId(), actualSectionPriceRule.getSection().getXmlId());
        assertEquals(expectedSectionPriceRule.getStartAmountInCurrency(),
            actualSectionPriceRule.getStartAmountInCurrency());
        assertEquals(expectedSectionPriceRule.getPerKmAmountInCurrency(),
            actualSectionPriceRule.getPerKmAmountInCurrency());
      }
    } else if (expected instanceof FromToGatesPriceRule) {
      var expectedGateFromToAmounts = ((FromToGatesPriceRule) expected).getPricedFromToGates();
      var actualGateFromToAmounts = ((FromToGatesPriceRule) actual).getPricedFromToGates();
      assertEquals(expectedGateFromToAmounts.size(), actualGateFromToAmounts.size());
      for (int index = 0; index < expectedGateFromToAmounts.size(); index++) {
        var expectedGateFromToAmount = expectedGateFromToAmounts.get(index);
        var actualGateFromToAmount = actualGateFromToAmounts.get(index);
        assertEquals(expectedGateFromToAmount.getFromGate().getXmlId(),
            actualGateFromToAmount.getFromGate().getXmlId());
        assertEquals(expectedGateFromToAmount.getToGate().getXmlId(), actualGateFromToAmount.getToGate().getXmlId());
        assertEquals(expectedGateFromToAmount.getAmountInCurrency(), actualGateFromToAmount.getAmountInCurrency(),
            DELTA);
      }
    }
  }

  /**
   * Check a scheme read back is equivalent to the one written: its ids, name, description, modes, where it charges,
   * and the point price rule of each mode it charges at moments across the week and at distances inside and beyond the
   * distance bands
   *
   * @param written scheme
   * @param readBack scheme
   */
  private static void assertEquivalentSchemes(PricingScheme written, PricingScheme readBack) {
    assertNotNull(readBack, written.getXmlId());
    assertEquals(written.getName(), readBack.getName());
    assertEquals(written.getExternalId(), readBack.getExternalId());
    assertEquals(written.getDescription(), readBack.getDescription());
    assertEquals(xmlIds(written.getChargedModes()), xmlIds(readBack.getChargedModes()));
    assertEquals(pricedLinkSegmentIds(written), pricedLinkSegmentIds(readBack));
    var writtenPricing = written.getPricing();
    var readPricing = readBack.getPricing();
    assertEquals(writtenPricing.getPriceOverrides().size(), readPricing.getPriceOverrides().size());
    assertEquals(writtenPricing.getTimeBands().size(), readPricing.getTimeBands().size());
    assertEquals(writtenPricing.getDistanceBands().size(), readPricing.getDistanceBands().size());
    assertEquals(writtenPricing.getModesMinimumPrices().size(), readPricing.getModesMinimumPrices().size());
    for (var mode : written.getChargedModes()) {
      var readMode = readBack.getChargedModes().stream()
          .filter(candidate -> candidate.getXmlId().equals(mode.getXmlId()))
          .findFirst()
          .orElseThrow();
      for (var day : List.of(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY)) {
        for (int hour = 0; hour < 24; hour++) {
          for (double km : new double[] {5, 15, 250}) {
            assertSamePriceRule(writtenPricing.getPointPriceRule(mode, at(day, hour, 15), km),
                readPricing.getPointPriceRule(readMode, at(day, hour, 15), km));
          }
        }
      }
    }
  }

  /**
   * Check a cap read back is equivalent to the one written
   *
   * @param written cap
   * @param readBack cap
   */
  private static void assertEquivalentCaps(PriceCap written, PriceCap readBack) {
    assertNotNull(readBack, written.getXmlId());
    assertEquals(written.getAmountInCurrency(), readBack.getAmountInCurrency());
    assertEquals(written.getActivateAboveCount(), readBack.getActivateAboveCount());
    assertEquals(written.getPeriod(), readBack.getPeriod());
    assertEquals(xmlIds(written.getSchemes()), xmlIds(readBack.getSchemes()));
    assertEquals(xmlIds(written.getPricedModeSelection().getPricedModes()),
        xmlIds(readBack.getPricedModeSelection().getPricedModes()));
  }

  /** the example network, read anew for each test */
  private MacroscopicNetwork network;

  /**
   * The mode of the example network with the given XML id
   *
   * @param xmlId of the mode
   * @return the mode
   */
  private Mode mode(String xmlId) {
    return network.getModes().getByXmlId(xmlId);
  }

  /**
   * Read a pricing file onto the example network
   *
   * @param file to read
   * @return the pricing read
   */
  private NetworkPricing readPricing(Path file) {
    var settings = new PlanitNetworkPricingReaderSettings();
    settings.setInputSource(file.toAbsolutePath().toString());
    return PlanitNetworkPricingReaderFactory.create(settings, network).read();
  }

  /**
   * Read a scenario's pricing file onto the example network, counting the reader's warnings
   *
   * @param scenario the name of the file in the cases folder, without extension
   * @param expectedWarnings the number of warnings the reader should log
   * @return the pricing read
   */
  private NetworkPricing readCase(String scenario, int expectedWarnings) {
    var pricing = new NetworkPricing[1];
    assertEquals(expectedWarnings, LogRecorder.countWarnings(PlanitNetworkPricingReader.class,
        () -> pricing[0] = readPricing(PRICING_PATH.resolve("cases").resolve(scenario + ".xml"))));
    return pricing[0];
  }

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(NetworkPricingReaderWriterTest.class);
    }
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }

  /** folder for a project's input files */
  @TempDir
  public Path tempDir;

  /** Read the example network for each test, which reads without warnings */
  @BeforeEach
  public void readNetwork() {
    network = PlanItIOTestHelper.readNetwork(PRICING_PATH.resolve("network"));
    assertEquals(0, readerWarnings.getCount());
  }

  // READING

  /** A pricing file naming another parent network stops reading with an error ("Parent network does not match") */
  @Test
  public void parentNetworkDoesNotMatch() {
    assertThrows(PlanItRunTimeException.class, () -> readCase("parent_network_mismatch", 0));
  }

  /** A location scheme with the id of a mode scheme read earlier is not read ("Scheme id used twice") */
  @Test
  public void schemeIdUsedTwice() {
    var pricing = readCase("scheme_id_used_twice", 1);
    assertEquals(Set.of("a"), xmlIds(pricing.getModeSchemes()));
    assertTrue(pricing.getLocationSchemes().isEmpty());
  }

  /** The second section with an id used before is not read, its scheme is ("Section id used twice") */
  @Test
  public void sectionIdUsedTwice() {
    var pricing = readCase("section_id_used_twice", 1);
    assertEquals(1, byXmlId(pricing.getLocationSchemes(), "orbital").getSections().size());
    assertTrue(byXmlId(pricing.getLocationSchemes(), "westconnex").getSections().isEmpty());
    assertEquals(1, pricing.getSections().size());
  }

  /** A scheme listing an unknown toll road is not read, the rest of the file is ("Unknown toll road") */
  @Test
  public void unknownTollRoad() {
    var pricing = readCase("unknown_toll_road", 1);
    assertEquals(Set.of("harbour"), xmlIds(pricing.getLocationSchemes()));
  }

  /**
   * An unknown link segment type or link segment in an include, exclude, section or gate is ignored, the rest of each
   * kept ("Unknown reference in a layout")
   */
  @Test
  public void unknownReferenceInALayout() {
    var pricing = readCase("unknown_reference_in_layout", 4);
    var trucks = byXmlId(pricing.getModeSchemes(), "trucks");
    assertEquals(3, trucks.getLinkSegmentCoverage().getIncludedLinkSegmentTypes().size());
    assertTrue(trucks.getLinkSegmentCoverage().getExcludedLinkSegments().isEmpty());
    assertEquals(Set.of("5101"), xmlIds(sectionOf(trucks.getSections(), "urban").getLinkSegments()));
    var north = pricing.getGates().firstMatch(gate -> "north".equals(gate.getXmlId()));
    assertEquals(Set.of("601", "602"), xmlIds(north.getEntryLinkSegments()));
  }

  /** A section price and a pair naming what their schemes do not have are ignored ("Unknown section or gate") */
  @Test
  public void unknownSectionOrGate() {
    var pricing = readCase("unknown_section_or_gate", 2);
    assertEquals(2, byXmlId(pricing.getLocationSchemes(), "orbital").getPricing().getBasePriceRule()
        .getSectionPriceRules().size());
    assertEquals(6, byXmlId(pricing.getEntryExitSchemes(), "ticketroad").getPricing().getBasePriceRule()
        .getPricedFromToGates().size());
  }

  /**
   * A scheme with overlapping time bands, and one with an override giving both amounts and a factor, are not read, the
   * rest of the file is ("Scheme breaking a pricing rule")
   */
  @Test
  public void schemeBreakingAPricingRule() {
    var pricing = readCase("scheme_breaking_rule", 2);
    assertEquals(Set.of("orbital"), xmlIds(pricing.getLocationSchemes()));
  }

  /** A scheme naming car on the truck toll road is read without car ("Mode the toll road does not price") */
  @Test
  public void modeTheTollRoadDoesNotPrice() {
    var pricing = readCase("mode_not_priced", 1);
    assertEquals(Set.of("heavygoods"), xmlIds(byXmlId(pricing.getLocationSchemes(), "truckroad").getChargedModes()));
  }

  /** A time band entry in years leaves its scheme not read ("Year in a time band entry") */
  @Test
  public void yearInATimeBandEntry() {
    var pricing = readCase("year_in_band", 1);
    assertEquals(Set.of("orbital"), xmlIds(pricing.getLocationSchemes()));
  }

  /** A cap with both an amount and a count is not read, its scheme is ("Cap with both an amount and a count") */
  @Test
  public void capWithBothAnAmountAndACount() {
    var pricing = readCase("cap_with_amount_and_count", 1);
    assertEquals(Set.of("zone"), xmlIds(pricing.getLocationSchemes()));
    assertTrue(pricing.getCaps().isEmpty());
  }

  /** A top-level cap naming one scheme is not read ("Top-level cap naming one scheme") */
  @Test
  public void topLevelCapNamingOneScheme() {
    var pricing = readCase("top_level_cap_naming_one_scheme", 1);
    assertEquals(Set.of("harbour"), xmlIds(pricing.getLocationSchemes()));
    assertTrue(pricing.getCaps().isEmpty());
  }

  /**
   * A base price rule, a factor override for heavy goods, two time bands and two distance bands are read as given, and
   * the point price rule for a mode at a moment follows them ("Pricing read")
   */
  @Test
  public void pricingRead() {
    var pricing = readCase("pricing", 0);
    var westconnex = byXmlId(pricing.getLocationSchemes(), "westconnex");
    assertEquals(Set.of("101", "102", "103", "104", "105", "106"), pricedLinkSegmentIds(westconnex));
    var schemePricing = westconnex.getPricing();
    assertEquals(1, schemePricing.getPriceOverrides().size());
    assertEquals(List.of("night", "am"),
        schemePricing.getTimeBands().stream().map(band -> band.getXmlId()).collect(Collectors.toList()));
    assertEquals(2, schemePricing.getDistanceBands().size());

    var truckAtNoon = schemePricing.getPointPriceRule(mode("heavygoods"), at(DayOfWeek.TUESDAY, 12, 0), 5);
    assertEquals(5.19, truckAtNoon.getStartAmountInCurrency(), DELTA);
    assertEquals(1.92, truckAtNoon.getPerKmAmountInCurrency(), DELTA);
    var truckAtNight = schemePricing.getPointPriceRule(mode("heavygoods"), at(DayOfWeek.TUESDAY, 23, 0), 5);
    assertEquals(1.73, truckAtNight.getStartAmountInCurrency(), DELTA);
    assertEquals(0.64, truckAtNight.getPerKmAmountInCurrency(), DELTA);
    var carInThePeak = schemePricing.getPointPriceRule(mode("car"), at(DayOfWeek.TUESDAY, 8, 0), 5);
    assertEquals(1.73, carInThePeak.getStartAmountInCurrency(), DELTA);
    assertEquals(0.80, carInThePeak.getPerKmAmountInCurrency(), DELTA);
    assertEquals(0.40, schemePricing.getPointPriceRule(mode("car"), at(DayOfWeek.TUESDAY, 8, 0),
        15).getPerKmAmountInCurrency(), DELTA);
  }

  /**
   * The example of the research note reads without warnings, with the schemes, sections, gates and caps it holds; the
   * worked cases needing only the point price rule for a mode at a moment give the amounts listed with it ("The example
   * of the research note", "Scheme on a toll road", "Roads priced as one", "Morning peak entry")
   */
  @Test
  public void exampleOfTheResearchNote() {
    var pricing = new NetworkPricing[1];
    assertEquals(0, LogRecorder.countWarnings(PlanitNetworkPricingReader.class,
        () -> pricing[0] = readPricing(PRICING_PATH.resolve("example").resolve("networkpricing.xml"))));
    var example = pricing[0];
    assertEquals("AUD", example.getCurrency().getCurrencyCode());
    assertEquals(2025, example.getYear().getValue());
    assertEquals(Set.of("trucks", "light", "ev"), xmlIds(example.getModeSchemes()));
    assertEquals(Set.of("westconnex", "harbour", "truckroad", "orbital", "cordon", "zone"),
        xmlIds(example.getLocationSchemes()));
    assertEquals(Set.of("ticketroad"), xmlIds(example.getEntryExitSchemes()));
    assertEquals(4, example.getSections().size());
    assertEquals(3, example.getGates().size());
    assertEquals(7, example.getCaps().size());

    /* scheme on a toll road, with its two sections; roads priced as one */
    var orbital = byXmlId(example.getLocationSchemes(), "orbital");
    assertEquals(Set.of("z1w", "z1e"), xmlIds(orbital.getSections()));
    assertEquals(Set.of("801", "802", "803", "804"), pricedLinkSegmentIds(orbital));
    assertEquals(6, byXmlId(example.getLocationSchemes(), "westconnex").streamPricedLinkSegments().count());

    /* morning peak entry: weekdays 06:30 to 09:30 */
    var peakEntry = byXmlId(example.getLocationSchemes(), "harbour").getPricing().getTimeBands().get(1)
        .getEntries().get(0);
    assertTrue(peakEntry.covers(at(DayOfWeek.MONDAY, 6, 30)));
    assertTrue(peakEntry.covers(at(DayOfWeek.FRIDAY, 9, 29)));
    assertFalse(peakEntry.covers(at(DayOfWeek.MONDAY, 9, 30)));
    assertFalse(peakEntry.covers(at(DayOfWeek.SATURDAY, 7, 0)));

    /* a truck on an urban motorway, Tuesday 08:00: 0.30 per km in term time, 0.25 in the school holidays, 0.12 on a
     * primary road */
    var trucks = byXmlId(example.getModeSchemes(), "trucks");
    var truck = mode("heavygoods");
    var urban = sectionOf(trucks.getSections(), "urban");
    var primary = sectionOf(trucks.getSections(), "primary");
    var termTime = trucks.getPricing().getPointPriceRule(truck, at(DayOfWeek.TUESDAY, 8, 0), 10);
    assertEquals(0.30, termTime.findSectionPriceRule(urban).getPerKmAmountInCurrency(), DELTA);
    assertEquals(0.12, termTime.findSectionPriceRule(primary).getPerKmAmountInCurrency(), DELTA);
    var schoolHolidays = trucks.getPricing()
        .getPointPriceRule(truck, TimeInWeekWithDayType.of(DayOfWeek.TUESDAY, 8 * 60, false, true), 10);
    assertEquals(0.25, schoolHolidays.findSectionPriceRule(urban).getPerKmAmountInCurrency(), DELTA);

    /* a truck on the Harbour, Tuesday: 13.23 at 07:00, 9.90 in the shoulder */
    var harbour = byXmlId(example.getLocationSchemes(), "harbour").getPricing();
    assertEquals(13.23, harbour.getPointPriceRule(truck, at(DayOfWeek.TUESDAY, 7, 0), 1).getStartAmountInCurrency(),
        DELTA);
    assertEquals(9.90, harbour.getPointPriceRule(truck, at(DayOfWeek.TUESDAY, 11, 0), 1).getStartAmountInCurrency(),
        DELTA);

    /* a truck on WestConnex at 23:00 pays the car toll */
    var westconnexNight = byXmlId(example.getLocationSchemes(), "westconnex").getPricing()
        .getPointPriceRule(truck, at(DayOfWeek.TUESDAY, 23, 0), 5);
    assertEquals(1.73, westconnexNight.getStartAmountInCurrency(), DELTA);
    assertEquals(0.64, westconnexNight.getPerKmAmountInCurrency(), DELTA);

    /* a motorcycle on the orbital, zone 1 westbound, at 08:00: a start of 0.50 and 0.55 per km */
    var motorcycle = orbital.getPricing().getPointPriceRule(mode("motorcycle"), at(DayOfWeek.TUESDAY, 8, 0), 5);
    assertEquals(0.50, motorcycle.getStartAmountInCurrency(), DELTA);
    assertEquals(0.55, motorcycle.findSectionPriceRule(sectionOf(orbital.getSections(),
        "z1w")).getPerKmAmountInCurrency(), DELTA);

    /* a truck on the ticket road from south to north at 08:00 on a weekday: 18.30 */
    var gates = byXmlId(example.getEntryExitSchemes(), "ticketroad").getGates();
    var south = gates.stream()
        .filter(gate -> "south".equals(gate.getXmlId()))
        .findFirst()
        .orElseThrow();
    var north = gates.stream()
        .filter(gate -> "north".equals(gate.getXmlId()))
        .findFirst()
        .orElseThrow();
    var ticketRoad = byXmlId(example.getEntryExitSchemes(), "ticketroad").getPricing()
        .getPointPriceRule(truck, at(DayOfWeek.WEDNESDAY, 8, 0), 10);
    assertEquals(18.30, ticketRoad.findByFromToGate(south, north).getAmountInCurrency(), DELTA);

    /* a car's 250th km of the day on a motorway, in the light scheme: 0.075 per km */
    assertEquals(0.075, byXmlId(example.getModeSchemes(), "light").getPricing()
        .getPointPriceRule(mode("car"), at(DayOfWeek.TUESDAY, 12, 0), 250).getPerKmAmountInCurrency(), DELTA);

    /* a car on the truck toll road pays nothing there */
    assertFalse(byXmlId(example.getLocationSchemes(), "truckroad").getChargedModes().contains(mode("car")));

    /* the trip minimum: a car on 2 km of the orbital's zone 1 westbound at midday pays 1.00 + 2 x 0.40 = 1.80, lifted
     * to the minimum of 2.00; 5 km costs 3.00 */
    var car = mode("car");
    var midday = at(DayOfWeek.TUESDAY, 12, 0);
    var zone1West = sectionOf(orbital.getSections(), "z1w");
    assertEquals(2.00, example.computeCumulativePrice(orbital, car, midday, 0, 2, Map.of(zone1West, 2.0)), DELTA);
    assertEquals(3.00, example.computeCumulativePrice(orbital, car, midday, 0, 5, Map.of(zone1West, 5.0)), DELTA);

    /* the caps per use of WestConnex: 20 km at midday, 1.73 + 20 x 0.64 = 14.53 for a car, capped at 12.25, and three
     * times that for a truck, capped at 36.75 */
    var westconnex = byXmlId(example.getLocationSchemes(), "westconnex");
    assertEquals(12.25, example.computeCumulativePrice(westconnex, car, midday, 0, 20, Map.of()), DELTA);
    assertEquals(36.75, example.computeCumulativePrice(westconnex, truck, midday, 0, 20, Map.of()), DELTA);

    /* the cordon's caps, one charge an hour and 15 a day: a car crossing at 08:00 pays 5.00 and again at 08:30
     * nothing; then 3.00 an hour from 10:00, until the fifth charge of the day pays the 1.00 left of the 15 */
    var cordon = byXmlId(example.getLocationSchemes(), "cordon");
    var totals = example.getCaps().createPriceCapTotals();
    var paid = new ArrayList<Double>();
    for (var hourAndMinute : List.of(new int[] {8, 0}, new int[] {8, 30}, new int[] {10, 0}, new int[] {11, 0},
        new int[] {12, 0}, new int[] {13, 0})) {
      if (hourAndMinute[1] == 0 && hourAndMinute[0] > 8) {
        totals.startPeriod(PricingPeriod.HOUR);
      }
      double price = example.computeCumulativePrice(cordon, car, at(DayOfWeek.TUESDAY, hourAndMinute[0],
          hourAndMinute[1]), 0, 1, Map.of(), totals);
      totals.addToTotal(cordon, car, price, true);
      paid.add(price);
    }
    assertEquals(List.of(5.00, 0.0, 3.00, 3.00, 3.00, 1.00), paid);

    /* the congestion charge zone, once a day: 18 for the first entry, nothing for the second */
    var zone = byXmlId(example.getLocationSchemes(), "zone");
    var zoneTotals = example.getCaps().createPriceCapTotals();
    double first = example.computeCumulativePrice(zone, car, at(DayOfWeek.TUESDAY, 8, 0), 0, 1, Map.of(), zoneTotals);
    zoneTotals.addToTotal(zone, car, first, true);
    assertEquals(18, first, DELTA);
    assertEquals(0, example.computeCumulativePrice(zone, car, at(DayOfWeek.TUESDAY, 16, 0), 0, 1, Map.of(), zoneTotals),
        DELTA);
  }

  /**
   * A project whose input folder holds the network and its pricing creates and registers the pricing on its network,
   * populated by the input builder from the pricing file
   *
   * @throws Exception when a file cannot be copied or the project cannot be made
   */
  @Test
  public void projectCreatesAndRegistersThePricingOnItsNetwork() throws Exception {
    Files.copy(PRICING_PATH.resolve("network").resolve("network.xml"), tempDir.resolve("network.xml"));
    Files.copy(PRICING_PATH.resolve("project").resolve("zoning.xml"), tempDir.resolve("zoning.xml"));
    Files.copy(PRICING_PATH.resolve("example").resolve("networkpricing.xml"), tempDir.resolve("networkpricing.xml"));
    var project = new CustomPlanItProject(new PlanItInputBuilder(tempDir.toAbsolutePath().toString()));
    var projectNetwork = (MacroscopicNetwork) project.createAndRegisterInfrastructureNetwork(
        MacroscopicNetwork.class.getCanonicalName());

    var pricing = new NetworkPricing[1];
    assertEquals(0, LogRecorder.countWarnings(PlanitNetworkPricingReader.class,
        () -> {
          try {
            pricing[0] = project.createAndRegisterNetworkPricing(projectNetwork);
          } catch (PlanItException e) {
            throw new PlanItRunTimeException(e);
          }
        }));
    assertEquals("sydney2025", pricing[0].getXmlId());
    assertEquals(projectNetwork, pricing[0].getParentNetwork());
    assertEquals(10, pricing[0].streamSchemes().count());
  }

  // WRITING

  /**
   * The example written and read back onto the same network is equivalent to the one written; its caps, given without
   * an id, are written with one and read back the same ("Round trip", "Cap without an id")
   */
  @Test
  public void roundTrip() throws Exception {
    var original = readPricing(PRICING_PATH.resolve("example").resolve("networkpricing.xml"));
    PlanitNetworkPricingWriterFactory.create(PRICING_PATH.toAbsolutePath().toString()).write(original);
    var writtenFile = PRICING_PATH.resolve("network_pricing.xml");
    try {
      var written = Files.readString(writtenFile);
      assertEquals(7, written.split("<cap id=\"", -1).length - 1);
      assertFalse(written.contains("<cap amount") || written.contains("<cap activateabovecount"));

      var readBack = new NetworkPricing[1];
      assertEquals(0, LogRecorder.countWarnings(PlanitNetworkPricingReader.class,
          () -> readBack[0] = readPricing(writtenFile)));
      assertEquals(original.getXmlId(), readBack[0].getXmlId());
      assertEquals(original.getCurrency(), readBack[0].getCurrency());
      assertEquals(original.getYear(), readBack[0].getYear());
      assertEquals(original.streamSchemes().count(), readBack[0].streamSchemes().count());
      original.getModeSchemes().forEach(scheme ->
          assertEquivalentSchemes(scheme, byXmlId(readBack[0].getModeSchemes(), scheme.getXmlId())));
      original.getLocationSchemes().forEach(scheme ->
          assertEquivalentSchemes(scheme, byXmlId(readBack[0].getLocationSchemes(), scheme.getXmlId())));
      original.getEntryExitSchemes().forEach(scheme ->
          assertEquivalentSchemes(scheme, byXmlId(readBack[0].getEntryExitSchemes(), scheme.getXmlId())));
      assertEquals(xmlIds(original.getSections()), xmlIds(readBack[0].getSections()));
      assertEquals(xmlIds(original.getGates()), xmlIds(readBack[0].getGates()));
      assertEquals(original.getCaps().size(), readBack[0].getCaps().size());
      original.getCaps().forEach(cap -> assertEquivalentCaps(cap,
          readBack[0].getCaps().firstMatch(candidate -> cap.getXmlId().equals(candidate.getXmlId()))));
    } finally {
      PlanItIOTestHelper.deleteFile(writtenFile.toAbsolutePath().toString());
    }
  }

  /**
   * A time band entry in hours and one in minutes, read and written back, are each written in the unit, multiplier and
   * offset they were read in ("Time band entry written as read")
   */
  @Test
  public void timeBandEntryWrittenAsRead() throws Exception {
    var pricing = readCase("time_band_entries", 0);
    PlanitNetworkPricingWriterFactory.create(tempDir.toAbsolutePath().toString()).write(pricing);
    var written = JAXBUtils.generateInstanceFromXml(new File[] {tempDir.resolve("network_pricing.xml").toFile()},
        XMLElementNetworkPricing.class, XMLElementNetworkPricing.class);
    var locationSchemes = written.getLocationschemes().getLocationschemes();

    var hourEntry = locationSchemes.stream()
        .filter(scheme -> "westconnex".equals(scheme.getId()))
        .findFirst()
        .orElseThrow()
        .getPricing()
        .getTimebandoverrides()
        .get(0)
        .getTimebandentries()
        .get(0);
    assertEquals(TemporalUnitType.HOUR, hourEntry.getTemporalunit());
    assertEquals(3, hourEntry.getTemporalunitmultiplier());
    assertEquals(7, hourEntry.getTemporaloffset());

    var minuteEntry = locationSchemes.stream()
        .filter(scheme -> "harbour".equals(scheme.getId()))
        .findFirst()
        .orElseThrow()
        .getPricing()
        .getTimebandoverrides()
        .get(0)
        .getTimebandentries()
        .get(0);
    assertEquals(TemporalUnitType.MINUTE, minuteEntry.getTemporalunit());
    assertEquals(180, minuteEntry.getTemporalunitmultiplier());
    assertEquals(390, minuteEntry.getTemporaloffset());
  }

  /**
   * An entry excluding public holidays and one giving no holiday settings, read and written back: the first is written
   * with {@code publicholidays="excluded"}, the second without either holiday setting, as included is the schema's
   * default ("Holiday settings written as read")
   */
  @Test
  public void holidaySettingsWrittenAsRead() throws Exception {
    var pricing = readCase("time_band_entries", 0);
    PlanitNetworkPricingWriterFactory.create(tempDir.toAbsolutePath().toString()).write(pricing);
    var written = Files.readString(tempDir.resolve("network_pricing.xml"));
    assertEquals(1, written.split("publicholidays=\"excluded\"", -1).length - 1);
    assertFalse(written.contains("publicholidays=\"included\""));
    assertFalse(written.contains("schoolholidays"));
  }
}
