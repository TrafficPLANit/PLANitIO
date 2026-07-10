package org.goplanit.io.converter.demands;

import org.goplanit.converter.demands.DiscreteDemandsWriter;
import org.goplanit.converter.idmapping.DiscreteDemandsIdMapper;
import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.demands.discrete.person.Person;
import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.demands.discrete.trip.Trip;
import org.goplanit.io.converter.PlanitWriterImpl;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.xml.generated.v2.*;
import org.goplanit.zoning.Zoning;

import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

/**
 * A class that takes a PLANit discrete demands and persists it to file in the PLANit native XML format.
 * 
 * @author markr
 *
 */
public class PlanitDiscreteDemandsWriter extends PlanitWriterImpl<DiscreteDemands> implements DiscreteDemandsWriter {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitDiscreteDemandsWriter.class.getCanonicalName());

  /** settings to use */
  private final PlanitDiscreteDemandsWriterSettings settings;

  /** the reference zoning to use */
  private Zoning referenceZoning;

  /** the reference network to use */
  private MacroscopicNetwork referenceNetwork;

  /** the XML to populate */
  private final XMLElementDiscreteDemand xmlRawDiscreteDemand;


  /** Populate the XML id of the XML demands element
   *
   * @param discreteDemand to extract XML id from
   */
  private void populateXmlId(DiscreteDemands discreteDemand) {
    /* XML id */

    if(getIdMapperType() == IdMapperType.ID){
      xmlRawDiscreteDemand.setId(String.valueOf(discreteDemand.getId()));
    }else if(getIdMapperType() == IdMapperType.XML) {
      String xmlId = discreteDemand.getXmlId();
      if (StringUtils.isNullOrBlank(xmlId)) {
        LOGGER.warning(String.format("DiscreteDemands has no XML id defined, adopting internally" +
                " generated id %d instead",
            discreteDemand.getId()));
        xmlId = String.valueOf(discreteDemand.getId());
      }
      xmlRawDiscreteDemand.setId(xmlId);
    }else if(getIdMapperType() == IdMapperType.EXTERNAL_ID){
      String externalId = discreteDemand.getExternalId();
      if (StringUtils.isNullOrBlank(externalId)) {
        LOGGER.warning(String.format("DiscreteDemands has no external id defined, adopting internally" +
                " generated id %d instead",
            discreteDemand.getId()));
        externalId = String.valueOf(discreteDemand.getId());
      }
      xmlRawDiscreteDemand.setId(externalId);
    }
  }

  /**
   * Verify if setup is valid before commencing persist
   *
   * @return true when valid, false otherwise
   */
  private boolean validateSettings() {
    if(!getSettings().validate()){
      return false;
    }

    boolean valid = true;
    if(getReferenceZoning() == null){
      LOGGER.severe("Unable to persist PLANit discrete demands without reference zoning," +
          " please provide before persisting");
      valid = false;
    }
    return valid;
  }

  /** Populate the demands configuration's time periods
   *
   * @param discreteDemands to populate XML with
   */
  private void populateTimePeriods(DiscreteDemands discreteDemands) {
    if(discreteDemands.getTimePeriods() == null || discreteDemands.getTimePeriods().isEmpty()) {
      LOGGER.severe(String.format("No time periods available on discreteDemands (%s), this shouldn't happen",
          discreteDemands.getIdsAsString()));
      return;
    }

    var xmlTimePeriods = new TimePeriodsContainerType();
    xmlRawDiscreteDemand.setTimeperiods(xmlTimePeriods);
    discreteDemands.getTimePeriods().streamSortedBy(
        getPrimaryIdMapper().getTimePeriodIdMapper()).forEach(timePeriod -> {
      TimePeriodXmlUtils.addTimePeriod(
          xmlTimePeriods, timePeriod, getPrimaryIdMapper().getTimePeriodIdMapper());
    });
  }

  /**
   * Populate households
   * @param discreteDemands to use
   */
  private void populateHouseholds(DiscreteDemands discreteDemands) {

    // Get or initialize the top-level <households> container element
    var householdsElement = xmlRawDiscreteDemand.getHouseholds();
    if (householdsElement == null) {
      householdsElement = new XMLElementHouseholds();
      xmlRawDiscreteDemand.setHouseholds(householdsElement);
    }
    List<XMLElementHousehold> xmlHouseholds =  householdsElement.getHouseholds();

    // Iterate over internal domain model and map to JAXB elements
    for (var domainHousehold : discreteDemands.getHouseholds()) {
      var xmlHousehold = new XMLElementHousehold();

      var mappedId = getPrimaryIdMapper().getHouseholdClassIdMapper().apply(domainHousehold);
      if(mappedId == null){
        LOGGER.severe(String.format(
            "Mapped id for households (%s) is null, make sure the chosen id mapper is valid, skipped",
            domainHousehold.getIdsAsString()));
        continue;
      }

      xmlHousehold.setId(getPrimaryIdMapper().getHouseholdClassIdMapper().apply(domainHousehold));

      if (domainHousehold.hasExternalId()) {
        xmlHousehold.setExternalid(domainHousehold.getExternalId());
      }

      var zoneRef = getComponentIdMappers().getZoningIdMappers().getZoneIdMapper().apply(domainHousehold.getZone());
      if(StringUtils.isNullOrBlank(zoneRef)){
        LOGGER.severe(String.format(
            "Household (%s) has no zone, not allowed, skipping",domainHousehold.getIdsAsString()));
        continue;
      }
      if (domainHousehold.getZone() != null) {
        xmlHousehold.setZoneref(zoneRef);
      }

      // Append directly to the live JAXB-managed array list
      xmlHouseholds.add(xmlHousehold);
    }
  }

  /**
   * Populate persons
   * @param discreteDemands to use
   */
  private void populatePersons(DiscreteDemands discreteDemands) {

    // Get or initialize the top-level <persons> container element
    var personsElement = xmlRawDiscreteDemand.getPersons();
    if (personsElement == null) {
      personsElement = new PersonsType();
      xmlRawDiscreteDemand.setPersons(personsElement);
    }
    List<PersonType> xmlPersons = personsElement.getPersons();

    // Iterate over internal domain model and map to JAXB elements
    for (var domainPerson : discreteDemands.getPersons()) {
      var xmlPerson = new PersonType();

      // Map and validate Person Primary ID
      var mappedId = getPrimaryIdMapper().getPersonClassIdMapper().apply(domainPerson);
      if (mappedId == null) {
        LOGGER.severe(String.format(
            "Mapped id for person (%s) is null, make sure the chosen id mapper is valid, skipped",
            domainPerson.getIdsAsString()));
        continue;
      }
      xmlPerson.setId(mappedId);

      // Map External ID if present
      if (domainPerson.hasExternalId()) {
        xmlPerson.setExternalid(domainPerson.getExternalId());
      }

      // Map and validate the Household Reference
      if (domainPerson.getHousehold() == null) {
        LOGGER.severe(String.format(
            "Person (%s) has no associated household, not allowed, skipping", domainPerson.getIdsAsString()));
        continue;
      }
      var householdRef = getPrimaryIdMapper().getHouseholdClassIdMapper().apply(domainPerson.getHousehold());
      if (StringUtils.isNullOrBlank(householdRef)) {
        LOGGER.severe(String.format(
            "Mapped household reference id for person (%s) is blank/null, skipping", domainPerson.getIdsAsString()));
        continue;
      }
      xmlPerson.setHhref(householdRef);

      // Delegate schedule processing cleanly
      addXmlActivitySchedule(domainPerson, xmlPerson);

      // Append directly to the live JAXB-managed array list
      xmlPersons.add(xmlPerson);
    }
  }

  /**
   * Populate and attach the schedule to a specific person element
   * @param domainPerson source domain data
   * @param xmlPerson target JAXB person element
   */
  private void addXmlActivitySchedule(Person domainPerson, PersonType xmlPerson) {
    if (domainPerson.getSchedule() == null) {
      return; // No schedule to map for this person, which can happen if they do not travel
    }
    var domainSchedule = domainPerson.getSchedule();
    var xmlSchedule = new ActivityScheduleType();

    // We put initial purpose on initial schedule and then never again in nested structure
    var initialPurpose = domainPerson.getInitialPurpose();
    if(!StringUtils.isNullOrBlank(initialPurpose)){
      xmlSchedule.setInit(initialPurpose);
    }

    //Map the live polymorphic collection list
    var xmlElements = xmlSchedule.getTourrevesAndTripreves();
    for (var element : domainSchedule) {
      if (element == null) continue;

      // Use reflection/type verification to identify the strategy type
      if (element instanceof Tour) {

        var xmlTourRef = new Tourref();
        var tourId = getPrimaryIdMapper().getTourClassIdMapper().apply((Tour)element);
        if (StringUtils.isNullOrBlank(tourId)) {
          LOGGER.severe(String.format("Tour reference ID is null for person (%s), skipping tour entry",
              domainPerson.getIdsAsString()));
          continue;
        }

        xmlTourRef.setRef(tourId);
        // xmlTourRef.setDescr(element.getDescription()); // Optional: map description if available
        xmlElements.add(xmlTourRef);

      } else if (element instanceof Trip) {

        var xmlTripRef = new Tripref();
        var tripId = getPrimaryIdMapper().getTripClassIdMapper().apply((Trip)element);
        if (StringUtils.isNullOrBlank(tripId)) {
          LOGGER.severe(String.format("Trip reference ID is null for person (%s), skipping trip entry",
              domainPerson.getIdsAsString()));
          continue;
        }

        xmlTripRef.setRef(tripId);
        // xmlTripRef.setDescr(element.getDescription()); // Optional
        xmlElements.add(xmlTripRef);

      } else {
        LOGGER.warning(String.format("Unknown schedule element type encountered (%s) for person (%s), skipped",
            element.getClass().getSimpleName(), domainPerson.getIdsAsString()));
      }
    }

    // Attach the populated schedule block to the person node
    xmlPerson.setSchedule(xmlSchedule);
  }

  /**
   * Populate master tours
   * @param discreteDemands to use
   */
  private void populateTours(DiscreteDemands discreteDemands) {

    // Get or initialize the top-level <tours> container element
    var toursElement = xmlRawDiscreteDemand.getTours();
    if (toursElement == null) {
      toursElement = new XMLElementTours();
      xmlRawDiscreteDemand.setTours(toursElement);
    }
    List<XMLElementTour> xmlTours = toursElement.getTours();

    // Iterate over internal domain model and map to JAXB elements
    var zoneIdMapper = getComponentIdMappers().getZoningIdMappers().getZoneIdMapper();
    for (var domainTour : discreteDemands.getTours()) {
      var xmlTour = new XMLElementTour();

      // Map and validate Tour Primary ID
      var mappedId = getPrimaryIdMapper().getTourClassIdMapper().apply(domainTour);
      if (mappedId == null) {
        LOGGER.severe(String.format(
            "Mapped id for tour (%s) is null, make sure the chosen id mapper is valid, skipped",
            domainTour.getIdsAsString()));
        continue;
      }
      xmlTour.setId(mappedId);

      // Map optional attributes
      if (domainTour.hasExternalId()) {
        xmlTour.setExternalid(domainTour.getExternalId());
      }

      //todo when we have time
      //xmlTour.setDescr(todo);

      // Map core travel attributes (Purpose, Origin, Destination)
      xmlTour.setPurp(domainTour.getPurpose());

      // Map and validate Origin Zone
      var originZoneRef = zoneIdMapper.apply(domainTour.getOrigin());
      if (StringUtils.isNullOrBlank(originZoneRef)) {
        LOGGER.severe(String.format(
            "Tour (%s) has an invalid or missing origin zone reference, skipping",
            domainTour.getIdsAsString()));
        continue;
      }
      xmlTour.setO(originZoneRef);
      // destination
      var destZoneRef = zoneIdMapper.apply(domainTour.getDestination());
      if (StringUtils.isNullOrBlank(destZoneRef)) {
        LOGGER.severe(String.format(
            "Tour (%s) has an invalid or missing destination zone reference, skipping",
            domainTour.getIdsAsString()));
        continue;
      }
      xmlTour.setD(destZoneRef);

      // Map optional temporal boundaries
      if (domainTour.getStartTime() != null) {
        xmlTour.setStartTime(domainTour.getStartTime()); // Assuming a local XML time utility
      }
      if (domainTour.getEndTime() != null) {
        xmlTour.setEndTime(domainTour.getEndTime());
      }

      // Map parent reference if this represents a child sub-tour block
      if (domainTour.getParentTour() != null) {
        var parentId = getPrimaryIdMapper().getTourClassIdMapper().apply(domainTour.getParentTour());
        if (!StringUtils.isNullOrBlank(parentId)) {
          xmlTour.setParentref(parentId);
        }
      }

      //xmlTour.setDescr(); todo

      // =========================================================================
      // Validation: A tour must contain structural elements (Trips or Sub-Tours)
      // =========================================================================
      if (domainTour.getSchedule() == null || domainTour.getSchedule().isEmpty()) {
        LOGGER.severe(String.format(
            "Tour (%s) contains an empty or missing internal schedule. A valid tour must " +
                "contain nested trips or sub-tours. Skipping tour generation.",
            domainTour.getIdsAsString()));
        continue;
      }
      boolean scheduleValid = addXmlActivitySchedule(domainTour, xmlTour);
      if (!scheduleValid) {
        LOGGER.severe(String.format(
            "Tour (%s) could not be safely generated due to a corrupt internal schedule structure. Skipping entire tour.",
            domainTour.getIdsAsString()));
        continue;
      }

      // Append directly to the live JAXB-managed array list
      xmlTours.add(xmlTour);
    }
  }

  /**
   * Populate master trips
   * @param discreteDemands to use
   */
  private void populateTrips(DiscreteDemands discreteDemands) {

    // Get or initialize the top-level <trips> container element
    var tripsElement = xmlRawDiscreteDemand.getTrips();
    if (tripsElement == null) {
      tripsElement = new XMLElementTrips();
      xmlRawDiscreteDemand.setTrips(tripsElement);
    }
    List<XMLElementTrip> xmlTrips = tripsElement.getTrips();

    // Iterate over internal domain model and map to JAXB elements
    for (var domainTrip : discreteDemands.getTrips()) {
      var xmlTrip = new XMLElementTrip();

      // Map and validate Trip Primary ID
      var mappedId = getPrimaryIdMapper().getTripClassIdMapper().apply(domainTrip);
      if (mappedId == null) {
        LOGGER.severe(String.format(
            "Mapped id for trip (%s) is null, make sure the chosen id mapper is valid, skipped",
            domainTrip.getIdsAsString()));
        continue;
      }
      xmlTrip.setId(mappedId);

      // Map and validate Parent Tour Reference (Required by XSD)
      if (domainTrip.getTour() == null) {
        LOGGER.severe(String.format(
            "Trip (%s) is missing its parent Tour object reference. Skipping invalid trip structure.",
            domainTrip.getIdsAsString()));
        continue;
      }
      var tourRefId = getPrimaryIdMapper().getTourClassIdMapper().apply(domainTrip.getTour());
      if (StringUtils.isNullOrBlank(tourRefId)) {
        LOGGER.severe(String.format(
            "Parent Tour ID mapping failed for Trip (%s). Reference is mandatory, skipping.",
            domainTrip.getIdsAsString()));
        continue;
      }
      xmlTrip.setTourref(tourRefId);

      // 3. Map and validate Purpose
      if (StringUtils.isNullOrBlank(domainTrip.getPurpose())) {
        xmlTrip.setPurp(domainTrip.derivePurposeFromDirectionAndTour());
      }else {
        xmlTrip.setPurp(domainTrip.getPurpose());
      }

      // Map and validate Mode
      if (domainTrip.getMode() == null) {
        LOGGER.severe(String.format("Trip (%s) is missing a mandatory transport mode, skipping",
            domainTrip.getIdsAsString()));
        continue;
      }
      var modeRef = getXmlModeReference(
          domainTrip.getMode(), getComponentIdMappers().getNetworkIdMappers().getModeIdMapper());
      if (StringUtils.isNullOrBlank(modeRef)) {
        LOGGER.severe(String.format("Trip (%s) is missing a valid mapped mode id for mode (%s), skipping",
            domainTrip.getIdsAsString(), domainTrip.getMode().getIdsAsString()));
        continue;
      }
      xmlTrip.setMode(modeRef);

      // Map optional metadata attributes
      if (domainTrip.hasExternalId()) {
        xmlTrip.setExternalid(domainTrip.getExternalId());
      }

      //xmlTrip.setDescr(domainTrip.getDescription()); todo

      // Map departure/start time directly using Java 11 LocalTime
      if (domainTrip.getStartTime() != null) {
        xmlTrip.setStartTime(domainTrip.getStartTime());
      }

      // Map Direction (Outbound vs Inbound)
      if (domainTrip.getDirection() != null) {
        // Assuming domainTrip.getDirection() maps cleanly to your TripDirectionType enum values
        try {
          var xmlDirection = TripDirectionType.fromValue(domainTrip.getDirection().name().toLowerCase());
          xmlTrip.setDirection(xmlDirection);
        } catch (IllegalArgumentException e) {
          LOGGER.warning(String.format("Unknown direction type (%s) on Trip (%s), falling back to default.",
              domainTrip.getDirection(), domainTrip.getIdsAsString()));
        }
      }

      // Append directly to the live JAXB list only if 100% structurally sound
      xmlTrips.add(xmlTrip);
    }
  }

  /**
   * Populate and validate the polymorphic list of sub-tours and tour-trips (Java 11 compatible)
   * @param domainTour source domain tour data
   * @param xmlTour target JAXB tour element
   * @return true if successfully mapped; false if a structural validation fails
   */
  private boolean addXmlActivitySchedule(Tour domainTour, XMLElementTour xmlTour) {
    var domainSchedule = domainTour.getSchedule();
    var xmlElements = xmlTour.getSubtoursAndTourtrips();

    for (var element : domainSchedule) {
      if (element == null) continue;

      if (element instanceof Tour) {
        Tour subTour = (Tour) element;
        var xmlTourRef = new Subtour();
        var tourId = getPrimaryIdMapper().getTourClassIdMapper().apply(subTour);

        if (StringUtils.isNullOrBlank(tourId)) {
          LOGGER.severe(String.format(
              "Nested Sub-Tour reference ID missing within schedule of parent Tour (%s).",
              domainTour.getIdsAsString()));
          return false;
        }

        xmlTourRef.setRef(tourId);
        xmlElements.add(xmlTourRef);

      } else if (element instanceof Trip) {
        Trip subTrip = (Trip) element;
        var xmlTourtripRef = new Tourtrip();
        var tripId = getPrimaryIdMapper().getTripClassIdMapper().apply(subTrip);

        if (StringUtils.isNullOrBlank(tripId)) {
          LOGGER.severe(String.format(
              "Nested Trip reference ID missing within schedule of parent Tour (%s).",
              domainTour.getIdsAsString()));
          return false;
        }

        xmlTourtripRef.setRef(tripId);
        xmlElements.add(xmlTourtripRef);

      } else {
        LOGGER.warning(String.format("Unknown schedule element type (%s) in Tour (%s), skipped element.",
            element.getClass().getSimpleName(), domainTour.getIdsAsString()));
      }
    }

    return true;
  }

  /** Constructor
   *
   * @param settings to use
   * @param xmlRawDiscreteDemand to populate and persist
   */
  protected PlanitDiscreteDemandsWriter(
          final PlanitDiscreteDemandsWriterSettings settings,
          final XMLElementDiscreteDemand xmlRawDiscreteDemand) {
    this(settings, null, xmlRawDiscreteDemand);
  }

  /** Constructor
   *
   * @param settings to use
   * @param referenceZoning to use
   * @param xmlRawDiscreteDemand to populate and persist
   */
  protected PlanitDiscreteDemandsWriter(
          final PlanitDiscreteDemandsWriterSettings settings,
          final Zoning referenceZoning,
          final XMLElementDiscreteDemand xmlRawDiscreteDemand) {
    super(IdMapperType.XML);
    this.settings = settings;
    this.referenceZoning = referenceZoning;
    this.xmlRawDiscreteDemand = xmlRawDiscreteDemand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DiscreteDemandsIdMapper getPrimaryIdMapper() {
    return getComponentIdMappers().getDiscreteDemandsIdMapper();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final DiscreteDemands discreteDemands){
    PlanItRunTimeException.throwIfNull(discreteDemands,
        "DiscreteDemands is null cannot write to PLANit native format");

    if(!validateSettings()){
      LOGGER.severe("Unable to continue PLANit writing of discrete demands, settings invalid");
      return;
    }
    
    /* initialise */
    {
      getComponentIdMappers().populateMissingIdMappers(getIdMapperType());
      LOGGER.info(String.format("Persisting PLANit discrete demands to: %s",
          Paths.get(getSettings().getOutputDirectory(), getSettings().getFileName())));
    }

    LOGGER.info(String.format("Id mapper set to: %s", getIdMapperType()));
    getSettings().logSettings();
    
    /* xml id */
    populateXmlId(discreteDemands);

    populateTimePeriods(discreteDemands);

    populateHouseholds(discreteDemands);

    populatePersons(discreteDemands);

    populateTours(discreteDemands);

    populateTrips(discreteDemands);
        
    /* persist */
    super.persist(xmlRawDiscreteDemand, XMLElementDiscreteDemand.class, PlanitSchema.DISCRETE_DEMAND_XSD);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void reset() {
    xmlRawDiscreteDemand.setId(null);
    xmlRawDiscreteDemand.setPersons(null);
    xmlRawDiscreteDemand.setHouseholds(null);
    xmlRawDiscreteDemand.setTours(null);
    xmlRawDiscreteDemand.setTrips(null);
    xmlRawDiscreteDemand.setTimeperiods(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitDiscreteDemandsWriterSettings getSettings() {
    return this.settings;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void setReferenceZoning(Zoning referenceZoning) {
    this.referenceZoning = referenceZoning;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setReferenceNetwork(MacroscopicNetwork referenceNetwork) {
    this.referenceNetwork = referenceNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetwork getReferenceNetwork() {
    return this.referenceNetwork = referenceNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Zoning getReferenceZoning() {
    return this.referenceZoning;
  }
}
