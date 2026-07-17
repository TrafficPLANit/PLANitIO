package org.goplanit.io.converter.demands;

import org.goplanit.converter.BaseReaderImpl;
import org.goplanit.converter.demands.DiscreteDemandsReader;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.DiscreteDemandsModifierUtils;
import org.goplanit.demands.discrete.household.Household;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.io.converter.zoning.PlanitZoningReader;
import org.goplanit.io.xml.util.PlanitXmlJaxbParser;
import org.goplanit.io.xml.util.XmlEnumConversionUtil;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.wrapper.MapWrapperImpl;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.xml.generated.v2.*;
import org.goplanit.zoning.Zoning;

import java.time.LocalTime;
import java.util.logging.Logger;

import static org.goplanit.io.converter.demands.TimePeriodXmlUtils.parseTimePeriod;

/**
 * Reader to parse PLANit discrete demands from native XML format
 * 
 * @author markr
 *
 */
public class PlanitDiscreteDemandsReader extends BaseReaderImpl<DiscreteDemands> implements DiscreteDemandsReader {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitDiscreteDemandsReader.class.getCanonicalName());

  /** parses the xml content in JAXB memory format */
  private final PlanitXmlJaxbParser<XMLElementDiscreteDemand,?> xmlParser;

  /** track start time as local time for quick comparisons */
  private LocalTime timePeriodStartTimeAsLocalTime;

  /** track end time as local time for quick comparisons, if exceeds midnight, it wraps around (does not go beyond) */
  private LocalTime timePeriodEndTimeAsLocalTime;

  /**
   * Check if start occurs before end taking wrap around into account if it exists
   *
   * @param startTime to check
   * @param endTime to check
   * @return flag
   */
  private boolean isInvalidOrder(LocalTime startTime, LocalTime endTime){
    if(timePeriodStartTimeAsLocalTime.isBefore(timePeriodEndTimeAsLocalTime)){
      return startTime.isAfter(endTime); // no wrap around --> normal check
    }else{
      // case 1: start time > end time but end time has not wrapped around --> invalid
      // case 2: start time has wrapped round, but end time < start time --> invalid
      return (startTime.isAfter(endTime) && endTime.isAfter(timePeriodStartTimeAsLocalTime)) ||
          (startTime.isBefore(timePeriodStartTimeAsLocalTime) &&  endTime.isBefore(startTime));
    }

  }

  /**
   * Initialise event listeners in case we want to make changes to the XML ids after parsing is complete, e.g.,
   * if the parsed demands is going to be modified and saved to disk afterwards, then it is advisable to sync
   * all XML ids to the internal ids upon parsing because this avoids the risk of generating duplicate XML ids
   * during editing of the network (when XML ids are chosen to be synced to internal ids)
   */
  private void syncXmlIdsToIds() {
    LOGGER.info("Syncing PLANit discrete demands XML ids to internally generated ids, " +
        "overwriting original XML ids");
    DiscreteDemandsModifierUtils.syncManagedIdEntitiesContainerXmlIdsToIds(discreteDemands);
  }

  /**
   * initialise the XML id trackers and populate them for the network and or zoning references,
   * so we can lay indices on the XML id as well for quick lookups
   *
   * @param network to use
   * @param zoning to use
   */
  private void initialiseParentXmlIdTrackers(LayeredNetwork<?,?> network, Zoning zoning) {

    // mode xml index
    initialiseSourceIdMap(Mode.class, Mode::getXmlId, network.getModes());

    // zone XML index
    initialiseSourceIdMap(Zone.class, Zone::getXmlId);
    getSourceIdContainer(Zone.class).addAll(zoning.getOdZones());
    getSourceIdContainer(Zone.class).addAll(zoning.getTransferZones());
  }

  /**
   * initialise the XML id trackers of generated PLANit entity types, so we can lay indices on the XML id as well
   * for quick lookups
   *
   */
  private void initialiseXmlIdTrackers() {
    initialiseSourceIdMap(TimePeriod.class, TimePeriod::getXmlId);
    initialiseSourceIdMap(Household.class, Household::getXmlId);
    initialiseSourceIdMap(Person.class, Person::getXmlId);
    initialiseSourceIdMap(Tour.class, Tour::getXmlId);
    initialiseSourceIdMap(Trip.class, Trip::getXmlId);
  }

  /**
   * Check if all required settings are indeed set by the user
   *
   */
  private void validate() {
    PlanItRunTimeException.throwIfNull(getReferenceNetwork(),
            "Reference network is null for PLANit discrete demands reader");
    PlanItRunTimeException.throwIfNull(getReferenceZoning(),
            "Reference zoning is null for PLANit discrete demands reader");

  }

  /**
   * parse the time periods
   *
   */
  private void parseTimePeriods() {
    var xmlDiscreteDemands = xmlParser.getXmlRootElement();

    /* XML time periods */
    var xmlTimePeriods = xmlDiscreteDemands.getTimeperiods();

    LocalTime defaultStartTime = LocalTime.MIN;

    /* time period */
    for (var xmlTimePeriod : xmlTimePeriods.getTimeperiods()) {
      var timePeriod = parseTimePeriod(xmlTimePeriod, defaultStartTime, discreteDemands.getTimePeriods());
      registerBySourceId(TimePeriod.class, timePeriod);
    }

    // we currently only allow a single time period to keep things simple. It is needed to have the one so we can
    // determine if start and end times of activities are moving forward in time since the start point as the period
    // may wrap around a day
    if(discreteDemands.getTimePeriods().size() > 1){
      LOGGER.severe(String.format(
          "Only single time period per discrete demand file is currently support, but found %d, abort",
          discreteDemands.getTimePeriods().size()));
      throw new PlanItRunTimeException("Invalid time period specification");
    }

    this.timePeriodStartTimeAsLocalTime =
        LocalTime.ofSecondOfDay(discreteDemands.getTimePeriods().getFirst().getStartTimeSeconds());
    this.timePeriodEndTimeAsLocalTime =
        LocalTime.ofSecondOfDay( (discreteDemands.getTimePeriods().getFirst().getStartTimeSeconds() +
        discreteDemands.getTimePeriods().getFirst().getDurationSeconds()) % LocalTime.MAX.toSecondOfDay());

    // in case of exactly 1 day, we subtract 1 second to ensure we can distinguish start from end
    if(timePeriodStartTimeAsLocalTime.equals(timePeriodEndTimeAsLocalTime) &&
        discreteDemands.getTimePeriods().getFirst().getDurationSeconds() > 0){
      this.timePeriodEndTimeAsLocalTime = timePeriodEndTimeAsLocalTime.minusSeconds(1);
    }
  }

  /**
   * Parse households
   */
  private void parseHouseholds() {
    var xmlDiscreteDemands = xmlParser.getXmlRootElement();
    var xmlHouseholdsElement = xmlDiscreteDemands.getHouseholds();

    if (xmlHouseholdsElement == null) {
      throw new PlanItRunTimeException(
          "Discrete demands input is missing the mandatory <households> container element. " +
          "Aborting parsing.");
    }

    var householdsContainer = discreteDemands.getHouseholds();
    for (var xmlHousehold : xmlHouseholdsElement.getHouseholds()) {

      // Mandatory Validation Checks
      if (StringUtils.isNullOrBlank(xmlHousehold.getId())) {
        LOGGER.severe("Encountered household with missing or blank XML ID. Skipping entry.");
        continue;
      }
      if (StringUtils.isNullOrBlank(xmlHousehold.getZoneref())) {
        LOGGER.severe(String.format("Household (%s) is missing its mandatory 'zoneref' attribute. Skipping.",
            xmlHousehold.getId()));
        continue;
      }

      // Instantiate via PLANit factory layout
      var household = householdsContainer.getFactory().registerNew();

      // Map foundational properties
      household.setXmlId(xmlHousehold.getId());
      if (!StringUtils.isNullOrBlank(xmlHousehold.getExternalid())) {
        household.setExternalId(xmlHousehold.getExternalid());
      }

      var zone = (OdZone) getBySourceId(Zone.class,xmlHousehold.getZoneref());
      if (zone == null) {
        LOGGER.severe(String.format(
            "Household (%s) references zone ID '%s' which cannot be found in the registered network zoning. Skipping.",
            xmlHousehold.getId(), xmlHousehold.getZoneref()));
        continue;
      }
      household.setZone(zone);

      //Register plumbing registry tracking
      registerBySourceId(Household.class, household);
    }
  }

  /**
   * Parse persons but delay filling out their schedule as we lack info for that at this point
   */
  private void parsePersonsWithoutSchedule() {
    var xmlDiscreteDemands = xmlParser.getXmlRootElement();
    var xmlPersonsElement = xmlDiscreteDemands.getPersons();

    if (xmlPersonsElement == null) {
      LOGGER.severe("Discrete demands input is missing the mandatory <persons> container element. " +
          "Aborting parsing.");
      return;
    }

    var xmlPersons = xmlPersonsElement.getPersons();
    if (xmlPersons == null || xmlPersons.isEmpty()) {
      LOGGER.severe("The <persons> element is empty. Demand configuration must contain at least one " +
          "person. Aborting parsing.");
      return;
    }

    var personsContainer = discreteDemands.getPersons();
    for (var xmlPerson : xmlPersons) {
      if (StringUtils.isNullOrBlank(xmlPerson.getId())) {
        LOGGER.severe("Encountered a person with a missing or blank XML ID. Skipping entry.");
        continue;
      }
      if (StringUtils.isNullOrBlank(xmlPerson.getHhref())) {
        LOGGER.severe(String.format(
            "Person (%s) is missing its mandatory 'hhref' attribute. Persons must belong to a household. Skipping.",
            xmlPerson.getId()));
        continue;
      }

      // Resolve the parent Household dependency on the fly
      var household = getBySourceId(Household.class, xmlPerson.getHhref());
      if (household == null) {
        LOGGER.severe(String.format(
            "Person (%s) references household ID '%s' which cannot be found. Skipping.",
            xmlPerson.getId(), xmlPerson.getHhref()));
        continue;
      }

      //Validate Schedule Block Presence and Initial Purpose
      var xmlSchedule = xmlPerson.getSchedule();
      if (xmlSchedule == null) {
        LOGGER.severe(String.format(
            "Person (%s) is missing its mandatory <schedule> block configuration. Skipping.",
            xmlPerson.getId()));
        continue;
      }

      var person = personsContainer.getFactory().registerNew(household);

      person.setXmlId(xmlPerson.getId());
      if (!StringUtils.isNullOrBlank(xmlPerson.getExternalid())) {
        person.setExternalId(xmlPerson.getExternalid());
      }

      if (!StringUtils.isNullOrBlank(xmlSchedule.getInit())) {
        person.setInitialPurpose(xmlSchedule.getInit());
      }

      //Register identity tracking
      registerBySourceId(Person.class, person);
    }
  }

  /** settings for the reader */
  protected final PlanitDiscreteDemandsReaderSettings settings;

  /** the discrete demands to populate */
  protected DiscreteDemands discreteDemands;

  /**
   * Reference network to use
   */
  protected LayeredNetwork<?, ?> referenceNetwork;

  /**
   * Reference zoning to use
   */
  protected Zoning referenceZoning;

  /** zoning reader provides alternative way to obtain reference zoning and reference network in case not available upon
   * construction. When using a reader, reference zoning and network are expected to remain null.
   */
  protected PlanitZoningReader zoningReader;

  /** Set the demands to populate
   *
   * @param discreteDemands to populate
   */
  protected void setDiscreteDemands(final DiscreteDemands discreteDemands) {
    this.discreteDemands = discreteDemands;
  }

  /**
   * Parses the discrete demand contents of the XML
   */
  protected void populateDiscreteDemandContents() {

    parseTimePeriods();

    parseHouseholds();

    parsePersonsWithoutSchedule();

    populateToursWithoutSchedule();

    populateTrips();

    // now we can populate the person schedules with all information parsed ...
    populatePersonSchedules();
    // ... and the trips on the tour schedules
    populateTourSchedules();
  }


  /**
   * Populate tours without schedule, we do that later
   */
  private void populateToursWithoutSchedule() {
    var xmlDiscreteDemands = xmlParser.getXmlRootElement();
    var xmlToursElement = xmlDiscreteDemands.getTours();

    if (xmlToursElement == null) {
      LOGGER.warning("Discrete demands input does not contain a <tours> block.");
      return;
    }

    var xmlTours = xmlToursElement.getTours();
    if (xmlTours == null || xmlTours.isEmpty()) {
      return;
    }

    var toursContainer = discreteDemands.getTours();
    for (var xmlTour : xmlTours) {
      if (StringUtils.isNullOrBlank(xmlTour.getId())) {
        LOGGER.severe("Encountered a tour with a missing or blank XML ID. Skipping entry.");
        continue;
      }
      if (StringUtils.isNullOrBlank(xmlTour.getPurp())) {
        LOGGER.severe(String.format("Tour (%s) is missing its mandatory 'purp' (purpose) attribute. " +
            "Skipping.", xmlTour.getId()));
        continue;
      }
      if (StringUtils.isNullOrBlank(xmlTour.getO()) || StringUtils.isNullOrBlank(xmlTour.getD())) {
        LOGGER.severe(String.format("Tour (%s) must specify both an origin ('o') and destination ('d') " +
            "zone reference. Skipping.", xmlTour.getId()));
        continue;
      }

      // Spatial Reference Resolution (Origin & Destination Zones)
      var originZone = (OdZone) getBySourceId(Zone.class, xmlTour.getO());
      if (originZone == null) {
        LOGGER.severe(String.format("Tour (%s) references origin zone '%s' which cannot be found. " +
            "Skipping.", xmlTour.getId(), xmlTour.getO()));
        continue;
      }
      var destinationZone = (OdZone) getBySourceId(Zone.class, xmlTour.getD());
      if (destinationZone == null) {
        LOGGER.severe(String.format("Tour (%s) references destination zone '%s' which cannot " +
            "be found. Skipping.", xmlTour.getId(), xmlTour.getD()));
        continue;
      }

      // Instantiate via PLANit factory layout
      var tour = toursContainer.getFactory().registerNew();
      tour.setXmlId(xmlTour.getId());
      tour.setPurpose(xmlTour.getPurp());
      tour.setOrigin(originZone);
      tour.setDestination(destinationZone);
      // currently we can only determine the person of a tour through nested tour in person schedule
      // parent tour we can set

      var startTime = xmlTour.getStartTime();
      var endTime = xmlTour.getEndTime();
      if (startTime == null || endTime == null) {
        LOGGER.severe(String.format(
            "Tour (%s) is missing temporal bounds. Both 'start_time' and 'end_time' are mandatory for " +
                "scheduling simulation execution. Skipping.",
            xmlTour.getId()));
        continue;
      }
      if (isInvalidOrder(startTime, endTime)) {
        LOGGER.severe(String.format(
            "Tour (%s) has an invalid temporal layout: start_time (%s) occurs after end_time (%s). " +
                "Skipping corrupt tour structure.",
            xmlTour.getId(), startTime, endTime));
        isInvalidOrder(startTime, endTime);
        continue;
      }
      tour.setStartEndTime(startTime, endTime);

      // we do post loop for parent-tours, since they may not all be parsed yet

      registerBySourceId(Tour.class, tour);
    }

    // Resolve and stitch self-referential parent sub-tours
    for (var xmlTour : xmlTours) {
      if (StringUtils.isNullOrBlank(xmlTour.getParentref())) {
        continue; // Standard top-level tour, no parent hierarchy to resolve
      }

      var planitTour = getBySourceId(Tour.class, xmlTour.getId());
      if (planitTour == null) {
        continue; // Skip if the instance itself failed validation in Pass 1
      }

      var parentTour = getBySourceId(Tour.class, xmlTour.getParentref());
      if (parentTour == null) {
        LOGGER.severe(String.format(
            "Sub-tour (%s) references parent tour ID '%s' which does not exist anywhere in the dataset. " +
                "Clearing corrupt sub-tour mapping.",
            xmlTour.getId(), xmlTour.getParentref()));
        continue;
      }

      planitTour.setParentTour(parentTour);
    }
  }

  /**
   * Populate trips
   */
  private void populateTrips() {
    var xmlDiscreteDemands = xmlParser.getXmlRootElement();
    var xmlTripsElement = xmlDiscreteDemands.getTrips();

    if (xmlTripsElement == null) {
      LOGGER.warning("Discrete demands input does not contain a <trips> block.");
      return;
    }

    var xmlTrips = xmlTripsElement.getTrips();
    if (xmlTrips == null || xmlTrips.isEmpty()) {
      return;
    }

    @SuppressWarnings("unchecked")
    MapWrapperImpl<String,Mode> modesByXmlId = (MapWrapperImpl<String,Mode>) getSourceIdContainer(Mode.class);
    var tripsContainer = discreteDemands.getTrips();
    for (var xmlTrip : xmlTrips) {
      if (StringUtils.isNullOrBlank(xmlTrip.getId())) {
        LOGGER.severe("Encountered a trip with a missing or blank XML ID. Skipping entry.");
        continue;
      }

      if (StringUtils.isNullOrBlank(xmlTrip.getMode())) {
        LOGGER.severe(String.format("Trip (%s) is missing its mandatory transport mode. Skipping.", xmlTrip.getId()));
        continue;
      }
      if (StringUtils.isNullOrBlank(xmlTrip.getTourref())) {
        LOGGER.severe(String.format("Trip (%s) is missing its mandatory parent tour reference " +
            "link ('tourref'). Skipping.", xmlTrip.getId()));
        continue;
      }

      // Mandatory Parent Tour Link
      var parentTour = getBySourceId(Tour.class, xmlTrip.getTourref());
      if (parentTour == null) {
        LOGGER.severe(String.format(
            "Trip (%s) references parent tour ID '%s' which cannot be found in the registered tours. Skipping.",
            xmlTrip.getId(), xmlTrip.getTourref()));
        continue;
      }

      // start end time
      var tripStartTime = xmlTrip.getStartTime();
      if (tripStartTime != null) {
        if (parentTour.getStartTime() != null && tripStartTime.isBefore(parentTour.getStartTime())) {
          LOGGER.severe(String.format(
              "Trip (%s) departs at %s, which occurs BEFORE its parent Tour (%s) starts (%s). " +
                  "Skipping corrupt schedule link.",
              xmlTrip.getId(), tripStartTime, parentTour.getXmlId(), parentTour.getStartTime()));
          continue;
        }
        if (parentTour.getEndTime() != null && tripStartTime.isAfter(parentTour.getEndTime())) {
          LOGGER.severe(String.format(
              "Trip (%s) departs at %s, which occurs AFTER its parent Tour (%s) ends (%s)." +
                  " Skipping corrupt schedule link.",
              xmlTrip.getId(), tripStartTime, parentTour.getXmlId(), parentTour.getEndTime()));
          continue;
        }
      }

      // direction
      if (xmlTrip.getDirection() == null) {
        LOGGER.severe(String.format(
            "Trip (%s) is missing a valid direction attribute (must exactly match 'outbound' or 'inbound'). Skipping.",
            xmlTrip.getId()));
        continue;
      }
      var direction = XmlEnumConversionUtil.xmlToPlanit(xmlTrip.getDirection());

      // object and ids
      var trip = tripsContainer.getFactory().registerNew(parentTour, direction, false);
      trip.setXmlId(xmlTrip.getId());
      if (!StringUtils.isNullOrBlank(xmlTrip.getExternalid())) {
        trip.setExternalId(xmlTrip.getExternalid());
      }

      // purpose
      if (StringUtils.isNullOrBlank(xmlTrip.getPurp())) {
        LOGGER.severe(String.format("Trip (%s) is missing its travel purpose ('purp'). " +
                "Deriving from trip and direction instead",
            xmlTrip.getId()));
        trip.derivePurposeFromDirectionAndTour();
      }else{
        trip.setPurpose(xmlTrip.getPurp());
      }

      // Resolve via modes, xml indexed, and assumed writer has accounted for predefined modes being mapped to their
      // names as XML id
      // todo: using xml id with contextual information should be phased out, deal with predefined modes in
      //  IO differently
      var mode = modesByXmlId.get(xmlTrip.getMode());
      if (mode == null) {
        LOGGER.severe(String.format(
            "Trip (%s) references mode '%s' which cannot be found in the active network infrastructure layers. Skipping.",
            xmlTrip.getId(), xmlTrip.getMode()));
        continue;
      }
      trip.setMode(mode);

      //trip.setDescription(xmlTrip.getDescr());

      registerBySourceId(Trip.class, trip);
    }
  }

  /**
   * Second pass: Reconstructs internal chronological activity schedules
   * for all parsed individuals by anchoring registered tours and trips.
   */
  private void populatePersonSchedules() {
    if(discreteDemands.getPersons().isEmpty()){
      return;
    }

    var xmlDiscreteDemands = xmlParser.getXmlRootElement();
    var xmlPersonsElement = xmlDiscreteDemands.getPersons();
    for (var xmlPerson : xmlPersonsElement.getPersons()) {
      var person = getBySourceId(Person.class, xmlPerson.getId());
      if (person == null) {
        continue;
      }

      // Individual has no explicit plan/schedule registered --> dismiss
      var xmlSchedule = xmlPerson.getSchedule();
      if (xmlSchedule == null) {
        continue;
      }
      var xmlScheduleChoices = xmlSchedule.getTourrevesAndTripreves();
      if (xmlScheduleChoices == null || xmlScheduleChoices.isEmpty()) {
        continue;
      }

      // process schedule in order as that is how it is supposed to be registered on the person
      var domainSchedule = person.getSchedule();
      for (Object xmlChoice : xmlScheduleChoices) {

        // Case A: The entry is a reference to a Tour (<tourref ref="..." />)
        if (xmlChoice instanceof Tourref) {
          var tourRef = (Tourref) xmlChoice;
          var tour = getBySourceId(Tour.class, tourRef.getRef());
          if (tour == null) {
            LOGGER.severe(String.format(
                "Person (%s) schedule references Tour ID '%s' which cannot be found. Skipping schedule leg.",
                xmlPerson.getId(), tourRef.getRef()));
            continue;
          }
          // Append the configured master tour into the person's plan timeline
          domainSchedule.add(tour);
          tour.setPerson(person);
        }
        // Case B: The entry is a direct reference to a standalone Trip (<tripref ref="..." />) --> not allowed at top
        //         level
        else if (xmlChoice instanceof Tripref) {
          var tripRef = (Tripref) xmlChoice;
          var trip = getBySourceId(Trip.class, tripRef.getRef());

          LOGGER.severe(String.format("Person schedule can only have tour's as top-level entries, found a trip (%s) " +
              "for person (%s), this is not yet supported, skip", person.getIdsAsString(), trip.getIdsAsString()));
        }else {
          LOGGER.warning(String.format("Unrecognized JAXB polymorphic schedule type '%s' for Person (%s).",
              xmlChoice.getClass().getSimpleName(), xmlPerson.getId()));
        }
      }
    }
  }

  /**
   * Second pass: Reconstructs internal chronological timelines for all individual
   * master tours by registering their trip segment and nested sub-tour references.
   */
  private void populateTourSchedules() {
    if (discreteDemands.getTours().isEmpty()) {
      return;
    }

    var xmlDiscreteDemands = xmlParser.getXmlRootElement();
    var xmlToursElement = xmlDiscreteDemands.getTours();
    if (xmlToursElement == null || xmlToursElement.getTours() == null) {
      return;
    }

    for (var xmlTour : xmlToursElement.getTours()) {
      var currTour = getBySourceId(Tour.class, xmlTour.getId());
      if (currTour == null) {
        continue;
      }

      var xmlTourChoices = xmlTour.getSubtoursAndTourtrips();
      if (xmlTourChoices == null || xmlTourChoices.isEmpty()) {
        continue;
      }
      var tourSchedule = currTour.getSchedule();
      for (Object xmlChoice : xmlTourChoices) {

        // Case A: Element is a leaf-node trip segment reference (<tourtrip ref="..." />)
        if (xmlChoice instanceof Tourtrip) {
          var tourTripRef = (Tourtrip) xmlChoice;
          var childTrip = getBySourceId(Trip.class, tourTripRef.getRef());

          if (childTrip == null) {
            LOGGER.severe(String.format(
                "Tour (%s) references a Trip ID '%s' that cannot be found. Skipping leg reference.",
                xmlTour.getId(), tourTripRef.getRef()));
            continue;
          }

          // Anchor the trip into this tour's internal sequence
          tourSchedule.add(childTrip);
        }

        // Case B: Element is a nested sub-tour reference (<subtour ref="..." />)
        else if (xmlChoice instanceof Subtour) {
          var subTourRef = (Subtour) xmlChoice;
          var childTour = getBySourceId(Tour.class, subTourRef.getRef());

          if (childTour == null) {
            LOGGER.severe(String.format(
                "Tour (%s) references a nested Sub-tour ID '%s' that cannot be found. Skipping leg reference.",
                xmlTour.getId(), subTourRef.getRef()));
            continue;
          }

          // Establish the nested structural parent link
          childTour.setParentTour(currTour);

          // Anchor the nested sub-tour into this tour's internal sequence
          tourSchedule.add(childTour);
        }

        else {
          LOGGER.warning(String.format("Unrecognized JAXB polymorphic internal tour type '%s' inside Tour (%s).",
              xmlChoice.getClass().getSimpleName(), xmlTour.getId()));
        }
      }
    }
  }

  /** Reference to demand schema location TODO: move to properties file*/
  public static final String DISCRETE_DEMAND_XSD_FILE =
          "https://trafficplanit.github.io/PLANitManual/xsd/discretedemandinput.xsd";


  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to
   * PLANit memory model
   *
   * @param xmlDiscreteDemands to extract from
   * @param network reference network for the demands to read
   * @param zoning reference zoning for the demands to read
   * @param discreteDemandsToPopulate to populate
   */
  public PlanitDiscreteDemandsReader(
      final XMLElementDiscreteDemand xmlDiscreteDemands,
      final LayeredNetwork<?, ?> network,
      final Zoning zoning,
      final DiscreteDemands discreteDemandsToPopulate){
    this(new PlanitDiscreteDemandsReaderSettings(), network, zoning, discreteDemandsToPopulate);
    this.xmlParser.setXmlRootElement(xmlDiscreteDemands);
  }

  /** Constructor where parsing will be based upon the settings and already present compatible network and zoning
   *
   * @param settings to use
   * @param network reference network for the demands to read
   * @param zoning reference zoning for the demands to read
   * @param discreteDemandsToPopulate to populate
   */
  public PlanitDiscreteDemandsReader(
          final PlanitDiscreteDemandsReaderSettings settings,
          final LayeredNetwork<?, ?> network,
          final Zoning zoning,
          final DiscreteDemands discreteDemandsToPopulate){
    this.xmlParser = new PlanitXmlJaxbParser<>(
        XMLElementDiscreteDemand.class,
        XMLElementDiscreteDemand.class); // no legacy version yet

    this.settings = settings;

    setDiscreteDemands(discreteDemandsToPopulate);

    this.zoningReader = null;
    this.referenceNetwork = network;
    this.referenceZoning = zoning;
  }

  /** Constructor where parsing will be based upon the settings and zoning reader provides zoning (and network)
   *
   * @param settings to use
   * @param zoningReader to construct zoning (and network) from
   */
  public PlanitDiscreteDemandsReader(
          final PlanitDiscreteDemandsReaderSettings settings,
          final PlanitZoningReader zoningReader){
    this(settings, null, null,null);
    setDiscreteDemands(null);
    this.zoningReader = zoningReader;
  }

  /** Parse the XMLand populate the demands memory model
   *
   */
  @Override
  public DiscreteDemands read() {
    
    try {

      /* prep reference network and zoning to populate based on network reader if that is what we use */
      if(zoningReader != null){
        PlanItRunTimeException.throwIf(referenceNetwork!=null, "Expected reference network to be " +
                "null when using zoning reader on PLANit demands reader");
        PlanItRunTimeException.throwIf(referenceZoning!=null, "Expected reference zoning to be" +
                " null when using zoning reader on PLANit demands reader");
        LOGGER.info("Parsing zoning using zoning reader to prepare Demands reader run");
        this.referenceZoning = zoningReader.read();
        this.referenceNetwork = zoningReader.getReferenceNetwork();
        setDiscreteDemands(new DiscreteDemands(getReferenceNetwork().getNetworkGroupingTokenId()));
      }

      /* verify completeness of inputs */
      validate();
            
      initialiseParentXmlIdTrackers(getReferenceNetwork(), getReferenceZoning());
      initialiseXmlIdTrackers();
      
      xmlParser.initialiseAndParseXmlRootElement(settings.getInputDirectory(), settings.getXmlFileExtension());
      var xmlDiscreteDemands = xmlParser.getXmlRootElement();
      
      /* xml id */
      String demandsXmlId = xmlDiscreteDemands.getId();
      if(StringUtils.isNullOrBlank(demandsXmlId)) {
        LOGGER.warning(String.format("Demands has no XML id defined, adopting internally generated id %d instead",
                discreteDemands.getId()));
        demandsXmlId = String.valueOf(discreteDemands.getId());
      }
      discreteDemands.setXmlId(demandsXmlId);

      var externalId = xmlDiscreteDemands.getExternalid();
      if(!StringUtils.isNullOrBlank(externalId)){
        discreteDemands.setExternalId(externalId);
      }

      /* discrete demands */
      populateDiscreteDemandContents();

      if(getSettings().isSyncXmlIdsToIds()){
        syncXmlIdsToIds();
      }

      /* log stats */
      discreteDemands.logInfo(LoggingUtils.discreteDemandsPrefix(discreteDemands.getId()));
      
      /* free */
      xmlParser.clearXmlContent();           

    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException("Error when populating discrete demands in PLANitIO",e);
    }
    
    return discreteDemands;
  } 
  

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitDiscreteDemandsReaderSettings getSettings() {
    return settings;
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void reset() {
  }

  /**
   * each reader is expected to ensure that it relates to a zoning
   * this reference zoning can be obtained (after reading is complete). the converter uses this to avoid the user
   * having to manually transfer this zoning to the writer which also requires this same zoning consistency
   * This is what this method enables
   */
  @Override
  public Zoning getReferenceZoning() {
    return this.referenceZoning;
  }

  /** Collect reference network used
   *
   * @return reference network
   */
  public LayeredNetwork<?, ?> getReferenceNetwork() {
    return referenceNetwork;
  }


  /** Set reference network to use
   *
   * @param referenceNetwork to use
   */
  public void setReferenceNetwork(final MacroscopicNetwork referenceNetwork) {
    this.referenceNetwork = referenceNetwork;
  }

  /** Set reference zoning to use
   *
   * @param referenceZoning to use
   */
  public void setReferenceZoning(final Zoning referenceZoning) {
    this.referenceZoning = referenceZoning;
  }
}
