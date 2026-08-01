package org.goplanit.io.test.integration.converter;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.tour.TourImpl;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.demands.discrete.trip.TripImpl;
import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.io.converter.demands.PlanitDiscreteDemandsReaderFactory;
import org.goplanit.io.converter.demands.PlanitDiscreteDemandsWriterFactory;
import org.goplanit.io.test.integration.TestBase;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.ZoningModifierUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.matchers.CompareMatcher;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

import static org.goplanit.io.test.util.PlanItIOTestHelper.deleteFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit test cases for converting discrete demands to PLANit XML
 *
 * @author markr
 *
 */
public class SimpleDiscreteDemandReaderWriterTest extends TestBase {

  /**
   * the logger
   */
  private static Logger LOGGER = null;

  private static final String PURPOSE_WORK = "work";
  private static final String PURPOSE_SHOPPING = "shopping";
  private static final String PURPOSE_GYM = "gym";

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(SimpleDiscreteDemandReaderWriterTest.class);
    }
  }

  /**
   * run garbage collection after each test as it apparently is not triggered properly within
   * Eclipse (or takes too long before being triggered)
   */
  @AfterEach
  public void afterTest() {
    IdGenerator.reset();
    System.gc();
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
  }


  /**
   * Test case which constructs a minimal in memory PLANit network and zoning, and the constructs an in memory
   * discrete demands setup and persists it as a PLANit xml
   */
  @Test
  public void testMemoryModelToPlanitXml() {

    final Path PLANIT_OUTPUT_DIR = Path.of(TEST_CASE_PATH.toString(), "discrete_demands_test");
    final Path PLANIT_REF_DIR = Path.of(TEST_CASE_PATH.toString(), "discrete_demands_test", "reference");

    try {

      var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
      var carMode = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
      var busMode = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
      var trainMode = network.getModes().getFactory().registerNew(PredefinedModeType.TRAIN);
      var walkMode = network.getModes().getFactory().registerNew(PredefinedModeType.PEDESTRIAN);
      network.getTransportLayers().getFactory().registerNew(network.getModes());

      var zoning = new Zoning(network.getIdGroupingToken(), network.getNetworkGroupingTokenId());
      var zone0 = zoning.getOdZones().getFactory().registerNew();
      var zone1 = zoning.getOdZones().getFactory().registerNew();
      var zone2 = zoning.getOdZones().getFactory().registerNew();
      var zone3 = zoning.getOdZones().getFactory().registerNew();
      var zone4 = zoning.getOdZones().getFactory().registerNew();

      var discreteDemands = new DiscreteDemands(network.getIdGroupingToken());

      // P0: (as a worked example)
      //      HOME (zone0)
      //          |
      //          +-- Trip OUTBOUND [car] 08:00
      //          |
      //          +-- TOUR: WORK
      //          |    zone0 -> zone1
      //          |    08:00 - 17:30
      //          |
      // |    WORK activity @ zone1
      // |    |
      // |    +-- Trip OUTBOUND [walk] 12:30
      //          |    |
      // |    +-- SUBTOUR: GYM
      //          |    |    zone1 -> zone2 -> zone1
      //          |    |    12:30 - 13:30
      //          |    |
      // |    +-- Trip INBOUND [walk] 13:10
      //          |    |
      // |    +-- Resume WORK activity @ zone1
      // |
      //      +-- Trip INBOUND [car] 17:00
      //          |
      //          HOME (zone0)
      //          |
      //          +-- Trip OUTBOUND [walk] 18:00 - SHOPPING1
      //          |
      //          +-- Trip OUTBOUND [walk] 18:30 - SHOPPING2
      //          |
      //          +-- TOUR: SHOPPING
      //          |    zone1 -> zone3
      //          |    18:00 - 20:30
      //          |
      //          +-- Trip INBOUND [walk] 20:20
      //          |
      //          END

      // time period
      discreteDemands.getTimePeriods().getFactory().registerNew(
          "all day", 0, 24 * 3600);

      // 2 households
      var household0 = discreteDemands.getHouseholds().getFactory().registerNew();
      household0.setZone(zone0);
      var household1 = discreteDemands.getHouseholds().getFactory().registerNew();
      household1.setZone(zone2);

      // 4 people, 2:2 split across households
      var person0 = discreteDemands.getPersons().getFactory().registerNew(household0);
      var person1 = discreteDemands.getPersons().getFactory().registerNew(household0);
      var person2 = discreteDemands.getPersons().getFactory().registerNew(household1);
      var person3 = discreteDemands.getPersons().getFactory().registerNew(household1);
      discreteDemands.getPersons().forEach(p -> p.setInitialPurpose("home"));

      // each person has a schedule with one or more (sequential) tours which in turn may contain nested tours
      // each tour origin == tour id, destination == tour id + 1
      boolean addToSchedule = true;
      var tour0_p0 = discreteDemands.getTours().getFactory().registerNew(
          person0, zone0, zone1, LocalTime.of(8, 0), LocalTime.of(17, 30), addToSchedule);
      tour0_p0.setPurpose(PURPOSE_WORK);
      var tour1_p1 = discreteDemands.getTours().getFactory().registerNew(
          person1, zone0, zone2, LocalTime.of(8, 0), LocalTime.of(17, 30), addToSchedule);
      tour1_p1.setPurpose(PURPOSE_WORK);
      var tour2_p2 = discreteDemands.getTours().getFactory().registerNew(
          person2, zone2, zone3, LocalTime.of(10, 0), LocalTime.of(13, 0), addToSchedule);
      tour2_p2.setPurpose(PURPOSE_SHOPPING);
      var tour3_p3 = discreteDemands.getTours().getFactory().registerNew(
          person3, zone2, zone4, LocalTime.of(15, 0), LocalTime.of(16, 0), addToSchedule);
      tour3_p3.setPurpose(PURPOSE_GYM);

      // outbound trips of main tour - always starting point
      var tour0_outboundTrip = discreteDemands.getTrips().getFactory().registerNew(
          tour0_p0, DirectionBound.OUTBOUND, addToSchedule);
      tour0_outboundTrip.setMode(carMode);
      tour0_outboundTrip.syncStartTimeToTourStartTime();
      var tour1_outboundTrip = discreteDemands.getTrips().getFactory().registerNew(
          tour1_p1, DirectionBound.OUTBOUND, addToSchedule);
      tour1_outboundTrip.setMode(trainMode);
      tour1_outboundTrip.syncStartTimeToTourStartTime();
      var tour2_outboundTrip = discreteDemands.getTrips().getFactory().registerNew(
          tour2_p2, DirectionBound.OUTBOUND, addToSchedule);
      tour2_outboundTrip.setMode(busMode);
      tour2_outboundTrip.syncStartTimeToTourStartTime();
      var tour3_outboundTrip = discreteDemands.getTrips().getFactory().registerNew(
          tour3_p3, DirectionBound.OUTBOUND, addToSchedule);
      tour3_outboundTrip.setMode(walkMode);
      tour3_outboundTrip.syncStartTimeToTourStartTime();

      // for tour0: add a sub-tour (so nested at destination of original tour)
      var tour0_subtour0_p0 = discreteDemands.getTours().getFactory().registerNew(
          tour0_p0, tour0_p0.getDestination(), zone2,
          LocalTime.of(12, 30), LocalTime.of(13, 30), addToSchedule);
      tour0_subtour0_p0.setPurpose(PURPOSE_GYM);
      //  with inbound + outbound trip for subtour
      {
        var tour0_subtour0_outbound = discreteDemands.getTrips().getFactory().registerNew(
            tour0_subtour0_p0, DirectionBound.OUTBOUND, addToSchedule);
        tour0_subtour0_outbound.setMode(walkMode);
        tour0_subtour0_outbound.syncStartTimeToTourStartTime();
        var tour0_subtour0_inbound = discreteDemands.getTrips().getFactory().registerNew(
            tour0_subtour0_p0, DirectionBound.INBOUND, addToSchedule);
        tour0_subtour0_inbound.setMode(walkMode);
        tour0_subtour0_inbound.syncStartTimeToTourEndWithNegativeOffset(Duration.of(20, ChronoUnit.MINUTES));
      }

      // inbound trips of main tour - back to origin
      var tour0_inboundTrip = discreteDemands.getTrips().getFactory().registerNew(
          tour0_p0, DirectionBound.INBOUND, addToSchedule);
      tour0_inboundTrip.setMode(carMode);
      tour0_inboundTrip.syncStartTimeToTourEndWithNegativeOffset(Duration.of(30, ChronoUnit.MINUTES));
      var tour1_inboundTrip = discreteDemands.getTrips().getFactory().registerNew(
          tour1_p1, DirectionBound.INBOUND, addToSchedule);
      tour1_inboundTrip.setMode(trainMode);
      tour1_inboundTrip.syncStartTimeToTourEndWithNegativeOffset(Duration.of(45, ChronoUnit.MINUTES));
      var tour2_inboundTrip = discreteDemands.getTrips().getFactory().registerNew(
          tour2_p2, DirectionBound.INBOUND, addToSchedule);
      tour2_inboundTrip.setMode(walkMode); // walk back, even though we took bus to the destination
      tour2_inboundTrip.syncStartTimeToTourEndWithNegativeOffset(Duration.of(20, ChronoUnit.MINUTES));
      var tour3_inboundTrip = discreteDemands.getTrips().getFactory().registerNew(
          tour3_p3, DirectionBound.INBOUND, addToSchedule);
      tour3_inboundTrip.setMode(walkMode);
      tour3_inboundTrip.syncStartTimeToTourEndWithNegativeOffset(Duration.of(20, ChronoUnit.MINUTES));

      // for tour0: add another sequential tour (so placed AFTER returning at origin from original tour)
      var tour_after_tour0_p0 = discreteDemands.getTours().getFactory().registerNew(
          person0, tour0_p0.getDestination(), zone3,
          LocalTime.of(18, 0), LocalTime.of(20, 30), addToSchedule);
      tour_after_tour0_p0.setPurpose(PURPOSE_SHOPPING);
      //  with 2xinbound + 1xoutbound trip for this next tour
      {
        // outbound trip 0: tour origin -> zone4
        var tour_after_tour0_outbound0 = discreteDemands.getTrips().getFactory().registerNew(
            tour_after_tour0_p0, DirectionBound.OUTBOUND, addToSchedule);
        tour_after_tour0_outbound0.setMode(walkMode);
        tour_after_tour0_outbound0.syncStartTimeToTourStartTime();
        tour_after_tour0_outbound0.setDestination(zone4);
        tour_after_tour0_outbound0.setPurpose("shopping1");
        // outbound trip 1: zone4 -> tour destination
        var tour_after_tour0_outbound1 = discreteDemands.getTrips().getFactory().registerNew(
            tour_after_tour0_p0, DirectionBound.OUTBOUND, addToSchedule);
        tour_after_tour0_outbound1.setMode(walkMode);
        tour_after_tour0_outbound1.setStartTime(LocalTime.of(18,30));
        tour_after_tour0_outbound1.setOrigin(zone4);
        tour_after_tour0_outbound1.setPurpose("shopping2");
        var tour_after_tour0_inbound = discreteDemands.getTrips().getFactory().registerNew(
            tour_after_tour0_p0, DirectionBound.INBOUND, addToSchedule);
        tour_after_tour0_inbound.setMode(walkMode);
        tour_after_tour0_inbound.syncStartTimeToTourEndWithNegativeOffset(Duration.of(10, ChronoUnit.MINUTES));
      }

      var writer =
          PlanitDiscreteDemandsWriterFactory.create(PLANIT_OUTPUT_DIR.toAbsolutePath().toString(), network, zoning);
      writer.setIdMapperType(IdMapperType.ID);
      // convert
      writer.write(discreteDemands);

      String theOutputFile = Path.of(PLANIT_OUTPUT_DIR.toString(), "discrete_demands.xml").toString();
      String theRefFile =Path.of(PLANIT_REF_DIR.toString(),"discrete_demands.xml").toString();
      org.hamcrest.MatcherAssert.assertThat(
          /* xml unit functionality comparing the two files */
          Input.fromFile(theOutputFile),
          CompareMatcher.isSimilarTo(Input.fromFile(theRefFile)));

      deleteFile(theOutputFile);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * Test case which takes a reference PLANit XML discrete demands setup,
   * parses it using the reader, and asserts the reconstructed in-memory
   * graph properties match the model specifications.
   */
  @Test
  public void testPlanitXmlToMemoryModel() {

    // we use the same setup as the one we wrote to disk above, so we can use its reference to parse.
    final Path PLANIT_REF_DIR = Path.of(TEST_CASE_PATH.toString(), "discrete_demands_test", "reference");

    try {
      var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
      var carMode = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
      var busMode = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
      var trainMode = network.getModes().getFactory().registerNew(PredefinedModeType.TRAIN);
      var walkMode = network.getModes().getFactory().registerNew(PredefinedModeType.PEDESTRIAN);
      network.getTransportLayers().getFactory().registerNew(network.getModes());

      var zoning = new Zoning(network.getIdGroupingToken(), network.getNetworkGroupingTokenId());

      // Instantiate zones programmatically
      var zone0 = zoning.getOdZones().getFactory().registerNew();
      var zone1 = zoning.getOdZones().getFactory().registerNew();
      var zone2 = zoning.getOdZones().getFactory().registerNew();
      var zone3 = zoning.getOdZones().getFactory().registerNew();
      var zone4 = zoning.getOdZones().getFactory().registerNew();
      ZoningModifierUtils.updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(zoning);

      var demandsReader = PlanitDiscreteDemandsReaderFactory.create(
          PLANIT_REF_DIR.toAbsolutePath().toString(), network, zoning);
      var discreteDemands = (DiscreteDemands) demandsReader.read();

      // --- DOMAIN MODEL VERIFICATION ASSERTIONS ---

      // Time Periods Assertion
      var timePeriods = discreteDemands.getTimePeriods();
      assertEquals(1, timePeriods.size(), "Should have exactly 1 global time period registered.");
      var globalPeriod = timePeriods.getFirst();
      assertNotNull(globalPeriod);
      assertEquals(0, globalPeriod.getStartTimeSeconds());
      assertEquals(24 * 3600, globalPeriod.getDurationSeconds());

      // Households Count & Integrity
      var households = discreteDemands.getHouseholds();
      assertEquals(2, households.size(), "Should parse exactly 2 unique households.");

      // Persons Count & Split Integrity
      var persons = discreteDemands.getPersons();
      assertEquals(4, persons.size(), "Should parse exactly 4 unique individual agents.");

      // Verify each person has initial purpose set to "home"
      persons.forEach(person ->
          assertEquals("home", person.getInitialPurpose(), "Initial purpose must decode to 'home'.")
      );

      // Deep Validation on Agent 0's complex schedule hierarchy (Person 0)
      var person0 = persons.stream()
          .filter(p -> "0".equals(p.getXmlId()))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Person with XML ID 'p0' was not found."));

      // --- SPATIAL MAPPING CHECKS: HOUSEHOLDS ---
      assertNotNull(person0.getHousehold());
      assertNotNull(person0.getHousehold().getZone(), "Household must have a validated spatial anchor.");
      assertEquals("0", person0.getHousehold().getZone().getXmlId(),
          "p0 household must anchor to zone0 mapping exactly.");

      // Tours verification
      var tours = discreteDemands.getTours();
      assertEquals(6, tours.size(),
          "Should find 6 total tours across all agents (4 main tours + 1 sub-tour + 1 sequential tour).");
      var trips = discreteDemands.getTrips();
      assertEquals(13, trips.size(),
          "Should find 13 total trips across all agents (4*2 main tour trips + 1*2 sub-tour trips + " +
              "1*2 sequential tour trips + 1 extra due to multi-outbound trips for sequential tour trip (shopping)).");

      // Pull main work tour of person 0
      var workTourP0 = tours.stream()
          .filter(t -> "0".equals(t.getXmlId()))
          .findFirst()
          .orElseThrow(() -> new AssertionError("Tour 'tour0_p0' was not registered."));

      // Verify Tour 1 Mode Mapping (Train)
      var workTourP1 = tours.stream()
          .filter(t -> "1".equals(t.getXmlId()))
          .findFirst()
          .orElseThrow();
      assertTrue(workTourP1.hasSchedule(), "Expected Tour 1 to have a schedule");
      var p1Outbound = (Trip) workTourP1.getSchedule().get(0);
      assertEquals(PredefinedModeType.TRAIN, p1Outbound.getMode().getPredefinedModeType(),
          "Tour 1 outbound trip should map exactly to TRAIN.");

      // Verify Tour 2 Mode Mapping (Bus to Destination, Walk Back)
      var shoppingTourP2 = tours.stream()
          .filter(t -> "2".equals(t.getXmlId()))
          .findFirst()
          .orElseThrow();
      var p2Outbound = (Trip) shoppingTourP2.getSchedule().get(0);
      var p2Inbound = (Trip) shoppingTourP2.getSchedule().get(1);
      assertEquals(PredefinedModeType.BUS, p2Outbound.getMode().getPredefinedModeType(),
          "Tour 2 outbound trip should map exactly to BUS.");
      assertEquals(PredefinedModeType.PEDESTRIAN, p2Inbound.getMode().getPredefinedModeType(),
          "Tour 2 inbound trip should map exactly to PEDESTRIAN (walk back).");

      assertEquals(PURPOSE_WORK, workTourP0.getPurpose());
      assertEquals(LocalTime.of(8, 0), workTourP0.getStartTime());
      assertEquals(LocalTime.of(17, 30), workTourP0.getEndTime());

      // --- SPATIAL MAPPING CHECKS: TOURS ---
      assertNotNull(workTourP0.getOrigin(), "Tour origin zone link cannot be null.");
      assertNotNull(workTourP0.getDestination(), "Tour destination zone link cannot be null.");
      assertEquals("0", workTourP0.getOrigin().getXmlId(), "Tour origin must map to zone0.");
      assertEquals("1", workTourP0.getDestination().getXmlId(), "Tour destination must map to zone1.");
      assertEquals(0, workTourP0.getOrigin().getId(), "Tour origin must map to zone0.");
      assertEquals(1, workTourP0.getDestination().getId(), "Tour destination must map to zone1.");

      // Verify Schedule Reconstruction Chronology for Person 0
      var scheduleP0 = workTourP0.getSchedule();
      assertNotNull(scheduleP0, "Tour schedule must be reassembled and populated.");
      assertEquals(3, scheduleP0.size(),
          "The work tour schedule for p0 should possess 3 chronological segments.");

      var firstSegment = scheduleP0.get(0);
      assertTrue(firstSegment instanceof TripImpl, "First segment must be a Trip.");
      var outboundTrip = (Trip) firstSegment;
      assertEquals(DirectionBound.OUTBOUND, outboundTrip.getDirection());
      assertNotNull(outboundTrip.getMode(), "Trip mode should not be null.");
      assertEquals(PredefinedModeType.CAR, outboundTrip.getMode().getPredefinedModeType(),
          "Outbound trip should map exactly to CAR mode.");

      var secondSegment = scheduleP0.get(1);
      assertTrue(secondSegment instanceof TourImpl, "Second segment must be the nested sub-tour.");
      var subTour = (Tour) secondSegment;
      assertEquals(PURPOSE_GYM, subTour.getPurpose());
      assertEquals(workTourP0, subTour.getParentTour(),
          "The sub-tour's parent tour link must be wired up correctly.");

      // Verify sub-tour's internal trips and their modes (Walk)
      var subTourSchedule = subTour.getSchedule();
      assertEquals(2, subTourSchedule.size(),
          "Sub-tour schedule should contain 2 trips (outbound + inbound).");

      var subTourOutbound = (Trip) subTourSchedule.get(0);
      assertEquals(PredefinedModeType.PEDESTRIAN, subTourOutbound.getMode().getPredefinedModeType(),
          "Sub-tour outbound trip should be a WALK/PEDESTRIAN mode.");

      var subTourInbound = (Trip) subTourSchedule.get(1);
      assertEquals(PredefinedModeType.PEDESTRIAN, subTourInbound.getMode().getPredefinedModeType(),
          "Sub-tour inbound trip should be a WALK/PEDESTRIAN mode.");

      var thirdSegment = scheduleP0.get(2);
      assertTrue(thirdSegment instanceof TripImpl, "Third segment must be a Trip.");
      var inboundTrip = (Trip) thirdSegment;
      assertEquals(DirectionBound.INBOUND, inboundTrip.getDirection());
      assertNotNull(inboundTrip.getMode(), "Inbound trip mode should not be null.");
      assertEquals(PredefinedModeType.CAR, inboundTrip.getMode().getPredefinedModeType(),
          "Inbound trip back home should map exactly to CAR mode.");

    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }

}
