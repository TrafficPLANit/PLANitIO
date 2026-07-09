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
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;
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

      var zone = getBySourceId(Zone.class,xmlHousehold.getZoneref());
      if (zone == null) {
        LOGGER.severe(String.format(
            "Household (%s) references zone ID '%s' which cannot be found in the registered network zoning. Skipping.",
            xmlHousehold.getId(), xmlHousehold.getZoneref()));
        continue;
      }

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
      if (startTime.isAfter(endTime)) {
        LOGGER.severe(String.format(
            "Tour (%s) has an invalid temporal layout: start_time (%s) occurs after end_time (%s). " +
                "Skipping corrupt tour structure.",
            xmlTour.getId(), startTime, endTime));
        continue;
      }

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

    var tripsContainer = discreteDemands.getTrips();
    for (var xmlTrip : xmlTrips) {
      // 1. Structural Guard Checks
      if (StringUtils.isNullOrBlank(xmlTrip.getId())) {
        LOGGER.severe("Encountered a trip with a missing or blank XML ID. Skipping entry.");
        continue;
      }

      // todo: we could derive the purpose using functionality on trip given its tour is present, so this check
      //  has to move lower for that
      if (StringUtils.isNullOrBlank(xmlTrip.getPurp())) {
        LOGGER.severe(String.format("Trip (%s) is missing its travel purpose ('purp'). Skipping.",
            xmlTrip.getId()));
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

      // Resolve Mandatory Parent Tour Link
      var parentTour = getBySourceId(Tour.class, xmlTrip.getTourref());
      if (parentTour == null) {
        LOGGER.severe(String.format(
            "Trip (%s) references parent tour ID '%s' which cannot be found in the registered tours. Skipping.",
            xmlTrip.getId(), xmlTrip.getTourref()));
        continue;
      }

      var tripStartTime = xmlTrip.getStartTime();
      if (tripStartTime != null) {
        if (parentTour.getStartTime() != null && tripStartTime.isBefore(parentTour.getStartTime())) {
          LOGGER.severe(String.format(
              "Trip (%s) departs at %s, which occurs BEFORE its parent Tour (%s) starts (%s). Skipping corrupt schedule link.",
              xmlTrip.getId(), tripStartTime, parentTour.getXmlId(), parentTour.getStartTime()));
          continue;
        }
        if (parentTour.getEndTime() != null && tripStartTime.isAfter(parentTour.getEndTime())) {
          LOGGER.severe(String.format(
              "Trip (%s) departs at %s, which occurs AFTER its parent Tour (%s) ends (%s). Skipping corrupt schedule link.",
              xmlTrip.getId(), tripStartTime, parentTour.getXmlId(), parentTour.getEndTime()));
          continue;
        }
      }

      if (xmlTrip.getDirection() == null) {
        LOGGER.severe(String.format(
            "Trip (%s) is missing a valid direction attribute (must exactly match 'outbound' or 'inbound'). Skipping.",
            xmlTrip.getId()));
        continue;
      }
      var direction = xmlTrip.getDirection();

      var trip = tripsContainer.getFactory().registerNew(parentTour, direction, false);
      trip.setXmlId(xmlTrip.getId());

      // 4. Map optional metadata attributes
      if (!StringUtils.isNullOrBlank(xmlTrip.getExternalid())) {
        trip.setExternalId(xmlTrip.getExternalid());
      }

      //trip.setDescription(xmlTrip.getDescr());



      //Map Direction Attribute (Enum handling)
      if (xmlTrip.getDirection() != null) {
        // Assuming your domain model uses a clean equivalent enum or accepts the string/JAXB enum directly
        //trip.setDirection(xmlTrip.getDirection());
      }

      // 7. Register identity mapping for the final schedule assembly pass
      registerBySourceId(Trip.class, trip);
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
