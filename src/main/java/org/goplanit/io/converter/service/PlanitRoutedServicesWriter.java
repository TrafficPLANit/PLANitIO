package org.goplanit.io.converter.service;

import org.goplanit.converter.IdMapperFunctionFactory;
import org.goplanit.converter.IdMapperType;
import org.goplanit.converter.service.RoutedServicesWriter;
import org.goplanit.io.converter.PlanitWriterImpl;
import org.goplanit.io.converter.network.PlanitNetworkWriter;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.io.xml.util.xmlEnumConversionUtil;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.service.routed.RoutedTripScheduleImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.CharacterUtils;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.service.routed.*;
import org.goplanit.utils.time.LocalTimeUtils;
import org.goplanit.utils.unit.TimeUnit;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.xml.generated.*;
import org.locationtech.jts.awt.PointShapeFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.goplanit.utils.unit.Unit.HOUR;

/**
 * Writer to persist PLANit routed services to disk in the native PLANit format. By default the xml ids are used for writing out the ids in the XML.
 * 
 * @author markr
 *
 */
public class PlanitRoutedServicesWriter extends PlanitWriterImpl<RoutedServices> implements RoutedServicesWriter {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitRoutedServicesWriter.class.getCanonicalName());

  /** XML memory model equivalent of the PLANit memory model */
  private final XMLElementRoutedServices xmlRawRoutedServices;

  /** used to create instances of XML gregorian calendars */
  private static DatatypeFactory xmlDataTypeFactory = null;
  static {
    try {
      xmlDataTypeFactory = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      throw new IllegalStateException(
          "Exception while creating DatatypeFactory", e);
    }
  }

  /** routed services writer settings to use */
  private final PlanitRoutedServicesWriterSettings settings;

  /* track logging prefix for current layer */
  private String currLayerLogPrefix;

  /** id mappers for routed services entities */
  private Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> routedServicesIdMappers;

  /**
   * Add the id information to trip
   *
   * @param xmlTrip to add id info  to
   * @param trip to extract from
   */
  private void populateXmlTripIds(XMLElementRoutedTrip xmlTrip, RoutedTrip trip) {
    /* xml id*/
    xmlTrip.setId(getRoutedTripRefIdMapper().apply(trip));

    /* external id */
    if(trip.hasExternalId()){
      xmlTrip.setExternalid(trip.getExternalId());
    }
  }

  /**
   * Populate a routed service trip based on a frequency as XML element
   *
   * @param xmlTrips to add trip to
   * @param frequencyBasedTrip to extract from
   * @return true when successful, false otherwise
   */
  private boolean createAndPopulateXmlRoutedServiceTrip(XMLElementRoutedTrips xmlTrips, RoutedTripFrequency frequencyBasedTrip) {
    var xmlRoutedTripFrequency = new XMLElementRoutedTrip();

    /* xml id, external id */
    populateXmlTripIds(xmlRoutedTripFrequency, frequencyBasedTrip);

    /* frequency */
    {
      if(!frequencyBasedTrip.hasPositiveFrequency()){
        LOGGER.warning(String.format("Frequency based routed trip %s has no positive frequency specified, discarded", xmlRoutedTripFrequency.getId()));
        return false;
      }
      var frequency = new XMLElementRoutedTrip.Frequency();

      /* unit */
      frequency.setUnit(xmlEnumConversionUtil.planitToXml(getSettings().getTripFrequencyTimeUnit()));

      /* frequency value */
      frequency.setValue((float) HOUR.convertTo(getSettings().getTripFrequencyTimeUnit(), frequencyBasedTrip.getFrequencyPerHour()));

      /* ls refs */
      var lsRefsList = new ArrayList<String>(frequencyBasedTrip.getNumberOfLegSegments());
      for(int index = 0; index < frequencyBasedTrip.getNumberOfLegSegments(); ++index){
        lsRefsList.add(getParentServiceLegSegmentRefIdMapper().apply(frequencyBasedTrip.getLegSegment(index)));
      }
      if(lsRefsList.isEmpty()){
        LOGGER.warning(String.format("No service leg segments present on frequency based trip (%s), discarded", frequencyBasedTrip.getXmlId()));
        return false;
      }
      frequency.setLsrefs(lsRefsList.stream().collect(Collectors.joining(CharacterUtils.COMMA.toString())));

      xmlRoutedTripFrequency.setFrequency(frequency);
    }

    xmlTrips.getTrip().add(xmlRoutedTripFrequency);
    return true;
  }

  /**
   * Populate a routed service trip based on a schedule as XML element
   *
   * @param xmlTrips to add trip to
   * @param scheduleBasedTrip to extract from
   * @return true when successful, false otherwise
   */
  private boolean createAndPopulateXmlRoutedServiceTrip(XMLElementRoutedTrips xmlTrips, RoutedTripSchedule scheduleBasedTrip) {
    var xmlRoutedTripSchedule = new XMLElementRoutedTrip();

    /* xml id, external id */
    populateXmlTripIds(xmlRoutedTripSchedule, scheduleBasedTrip);

    /* schedule */
    {
      var xmlSchedule = new XMLElementRoutedTrip.Schedule();

      /* departures */
      if(!scheduleBasedTrip.hasDepartures()){
        LOGGER.warning(String.format("No departures present on schedule based trip (%s), discarded", scheduleBasedTrip.getXmlId()));
        return false;
      }
      var xmlDepartures = new XMLElementDepartures();
      xmlSchedule.setDepartures(xmlDepartures);

      /* departure */
      var xmlDepartureList = xmlDepartures.getDeparture();
      for(var departure : scheduleBasedTrip.getDepartures()){
        var xmlDeparture = new XMLElementDepartures.Departure();

        /* departure XML id */
        xmlDeparture.setId(getRoutedTripDepartureRefIdMapper().apply(departure));

        /* departure external id */
        if(departure.hasExternalId()){
          xmlDeparture.setExternalid(departure.getExternalId());
        }

        /* time (HH:mm:ss) */
        xmlDeparture.setTime(departure.getDepartureTime().toString());

        xmlDepartureList.add(xmlDeparture);
      }

      /* rel timings */
      if(!scheduleBasedTrip.hasRelativeLegTimings()){
        LOGGER.warning(String.format("No relative leg timings present on schedule based trip (%s), discarded", scheduleBasedTrip.getXmlId()));
        return false;
      }

      var xmlRelTimings = new XMLElementRelativeTimings();
      xmlSchedule.setReltimings(xmlRelTimings);

      var defaultDwellTime = ((RoutedTripScheduleImpl)scheduleBasedTrip).getDefaultDwellTime();
      if(defaultDwellTime != LocalTime.MIN){
        var xmlGregorianCalendarDefaultDwellTime =
            xmlDataTypeFactory.newXMLGregorianCalendar(LocalTimeUtils.toGregorianCalendar(defaultDwellTime));
        xmlRelTimings.setDwelltime(xmlGregorianCalendarDefaultDwellTime);
      }

      {
        /* rel timing */
        var xmlRelTimingLegList = xmlRelTimings.getLeg();
        for(var relLegTiming : scheduleBasedTrip) {
          var xmlReltimingLeg = new XMLElementRelativeTimings.Leg();

          /* duration */
          xmlReltimingLeg.setDuration(
              xmlDataTypeFactory.newXMLGregorianCalendar(LocalTimeUtils.toGregorianCalendar(relLegTiming.getDuration())));

          /* dwell time */
          if(!relLegTiming.getDwellTime().equals(defaultDwellTime)){
            xmlReltimingLeg.setDwelltime(
                xmlDataTypeFactory.newXMLGregorianCalendar(LocalTimeUtils.toGregorianCalendar(relLegTiming.getDwellTime())));
          }

          /* service leg segment reference */
          if(!relLegTiming.hasParentLegSegment()){
            LOGGER.warning(String.format("No service leg segment present on relative leg timing, discarded this trip (%s)", scheduleBasedTrip.getXmlId()));
            return false;
          }
          xmlReltimingLeg.setLsref(getParentServiceLegSegmentRefIdMapper().apply(relLegTiming.getParentLegSegment()));

          //TODO --> continue here, see if it will persist -> then continue with adding a test to see if we can read and write causing same result
          //         to do so we first need to create a sample via integration test, then add to PLANit IO

          xmlRelTimingLegList.add(xmlReltimingLeg);
        }
      }

      xmlRoutedTripSchedule.setSchedule(xmlSchedule);
    }

    xmlTrips.getTrip().add(xmlRoutedTripSchedule);
    return true;
  }

  /**
   * Populate a routed service as XML element
   *
   * @param xmlServices to add service to
   * @param routedService to extract from
   * @return true when successful, false otherwise
   */
  private boolean createAndPopulateXmlRoutedServices(XMLElementServices xmlServices, RoutedService routedService) {
    XMLElementService xmlRoutedService = new XMLElementService();

    /* XML id */
    routedService.setXmlId(getRoutedServiceRefIdMapper().apply(routedService));

    /* external id */
    if(routedService.hasExternalId()) {
      xmlRoutedService.setExternalid(routedService.getExternalId());
    }

    /* name */
    if(routedService.hasName()){
      xmlRoutedService.setName(routedService.getName());
    }

    /* name description */
    if(routedService.hasNameDescription()){
      xmlRoutedService.setNamedescription(routedService.getNameDescription());
    }

    /* service description */
    if(routedService.hasServiceDescription()){
      xmlRoutedService.setServicedescription(routedService.getServiceDescription());
    }

    /* discard if no trips registered */
    if(!(routedService.getTripInfo().hasFrequencyBasedTrips() || routedService.getTripInfo().hasScheduleBasedTrips())){
      return false;
    }

    /* trips */
    var xmlTrips = new XMLElementRoutedTrips();
    xmlRoutedService.setTrips(xmlTrips);

    /* frequency based trips */
    if(routedService.getTripInfo().hasFrequencyBasedTrips()) {
      for (var freqTrip : routedService.getTripInfo().getFrequencyBasedTrips()) {
        createAndPopulateXmlRoutedServiceTrip(xmlTrips, freqTrip);
      }
    }

    /* schedule based trips */
    if(routedService.getTripInfo().hasScheduleBasedTrips()) {
      for (var schedTrip : routedService.getTripInfo().getScheduleBasedTrips()) {
        createAndPopulateXmlRoutedServiceTrip(xmlTrips, schedTrip);
      }
    }

    xmlServices.getService().add(xmlRoutedService);
    return true;
  }


  /**
   * Populate all routed services grouped by mode as XML element
   *
   * @param xmlLayer to add mode specific routed services to
   * @param servicesForMode to extract information from
   * @return true when successful, false otherwise
   */
  private boolean createAndPopulateXmlRoutedServicesByMode(XMLElementRoutedServicesLayer xmlLayer, RoutedModeServices servicesForMode) {
    /* umbrella element to add to */
    var xmlServices = new XMLElementServices();
    xmlLayer.getServices().add(xmlServices);

    /* mode */
    xmlServices.setModeref(getParentModeRefIdMapper().apply(servicesForMode.getMode()));

    /* for each service populate and XML element */
    boolean sucess = true;
    for( var service : servicesForMode){
      sucess = createAndPopulateXmlRoutedServices(xmlServices, service);
      if(!sucess){
        break;
      }
    }
    return sucess;
  }

  /**
   * Populate routed services layer in XML form based on provided PLANit memory model
   *
   * @param xmlLayer to populate
   * @param layer to extract information from
   * @param routedServices to use
   * @return true when successfully populated, false otherwise
   */
  private boolean populateXmlRoutedServiceLayer(XMLElementRoutedServicesLayer xmlLayer, RoutedServicesLayer layer, RoutedServices routedServices) {

    /* XML id */
    var xmlId = layer.getXmlId();
    if(layer.getXmlId() == null) {
      LOGGER.warning(String.format("Routed services layer has no XML id defined, adopting internally generated id %d instead", layer.getId()));
      xmlId = String.valueOf(layer.getId());
    }
    layer.setXmlId(xmlId);
    this.currLayerLogPrefix = LoggingUtils.surroundwithBrackets("rs-layer: "+layer.getXmlId());

    /* external id */
    if(layer.hasExternalId()) {
      xmlLayer.setExternalid(layer.getExternalId());
    }

    /* parent layer ref */
    String parentLayerXmlId = layer.getParentLayer().getXmlId();
    if(StringUtils.isNullOrBlank(parentLayerXmlId)) {
      LOGGER.severe(String.format("Routed services layer's parent service layer has no XML id defined, assuming internally generated id %d as reference id instead, please verify this matches persisted parent network id",layer.getParentLayer().getId()));
      parentLayerXmlId = String.valueOf(layer.getParentLayer().getId());
    }
    xmlLayer.setServicelayerref(parentLayerXmlId);

    /* per mode all services in this layer */
    boolean success = true;
    for(var servicesForMode : layer){
      success = createAndPopulateXmlRoutedServicesByMode(xmlLayer, servicesForMode);
      if(!success){
        break;
      }
    }

    return success;
  }

  /** Populate the available routed services layers
   *
   * @param routedServices to extract layers from and populate xml
   */
  protected void populateXmlRoutedServicesLayers(RoutedServices routedServices) {

    var xmlServiceLayers = xmlRawRoutedServices.getServicelayers();
    if(xmlServiceLayers == null){
      xmlServiceLayers = new XMLElementRoutedServices.Servicelayers();
      xmlRawRoutedServices.setServicelayers(xmlServiceLayers);
    }

    /* service network ref */
    String parentNetworkXmlId = routedServices.getParentNetwork().getXmlId();
    if(StringUtils.isNullOrBlank(parentNetworkXmlId)) {
      LOGGER.severe(String.format("Routed services' parent network has no XML id defined, assuming internally generated id %d as reference id instead, please verify this matches persisted parent network id",routedServices.getParentNetwork().getId()));
      parentNetworkXmlId = String.valueOf(routedServices.getParentNetwork().getId());
    }
    xmlServiceLayers.setServicenetworkref(parentNetworkXmlId);

    LOGGER.info(String.format("Found %d routed services layers", routedServices.getLayers().size()));
    var xmlLayers = xmlServiceLayers.getServicelayer();
    for(var layer : routedServices.getLayers()){
      var xmlLayer = new XMLElementRoutedServicesLayer();

      boolean success = populateXmlRoutedServiceLayer(xmlLayer, layer, routedServices);
      if(success) {
        xmlLayers.add(xmlLayer);
      }
    }
  }

  /**
   * Populate the top level XML element for the routed services and include parent network reference. In case no XML id is set, attempt to salvage
   * with internal id's and log warnings for user verification of correctness.
   *
   * @param routedServices to use
   */
  private void populateTopLevelElement(RoutedServices routedServices) {
    /* xml id */
    if(!routedServices.hasXmlId()) {
      LOGGER.warning(String.format("Routed services has no XML id defined, adopting internally generated id %d instead",routedServices.getId()));
      routedServices.setXmlId(String.valueOf(routedServices.getId()));
    }
    xmlRawRoutedServices.setId(routedServices.getXmlId());

    /* external id */
    if(routedServices.hasExternalId()){
      xmlRawRoutedServices.setExternalid(routedServices.getExternalId());
    }
  }

  /**
   * Collect how parent node's refs were mapped to the XML ids when persisting the parent network, use this mapping for our references as well
   * by using this function
   *
   * @return mapping from parent node to string (XML id to persist)
   */
  protected Function<Vertex, String> getParentNodeRefIdMapper(){
    return (Function<Vertex, String>) getParentIdMapperTypes().get(Vertex.class);
  }

  /**
   * Collect how parent mode's refs were mapped to the XML ids when persisting the parent network, use this mapping for our references as well
   * by using this function
   *
   * @return mapping from parent mode to string (XML id to persist)
   */
  protected Function<Mode, String> getParentModeRefIdMapper(){
    return (Function<Mode, String>) getParentIdMapperTypes().get(Mode.class);
  }

  /**
   * Collect how parent service leg segment ids were mapped to the XML ids when persisting the parent network, use this mapping for our references as well
   * by using this function
   *
   * @return mapping from parent service leg segment to string (XML id to persist)
   */
  protected Function<ServiceLegSegment, String> getParentServiceLegSegmentRefIdMapper(){
    return (Function<ServiceLegSegment, String>) getParentIdMapperTypes().get(ServiceLegSegment.class);
  }

  /**
   * Collect how routed service leg ids are to be mapped to the XML ids when persisting
   *
   * @return mapping from routed service to string (XML id to persist)
   */
  private Function<RoutedService, String> getRoutedServiceRefIdMapper() {
    return (Function<RoutedService, String>) getParentIdMapperTypes().get(RoutedService.class);
  }

  /**
   * Collect how routed trip ids are to be mapped to the XML ids when persisting
   *
   * @return mapping from routed trip to string (XML id to persist)
   */
  private Function<RoutedTrip, String> getRoutedTripRefIdMapper() {
    return (Function<RoutedTrip, String>) getParentIdMapperTypes().get(RoutedTrip.class);
  }

  /**
   * Collect how RoutedTripDeparture ids are to be mapped to the XML ids when persisting
   *
   * @return mapping from RoutedTripDeparture to string (XML id to persist)
   */
  private Function<RoutedTripDeparture, String> getRoutedTripDepartureRefIdMapper() {
    return (Function<RoutedTripDeparture, String>) getParentIdMapperTypes().get(RoutedTripDeparture.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initialiseIdMappingFunctions() {
    this.routedServicesIdMappers = createPlanitRoutedServicesIdMappingTypes(getIdMapperType());

    if(!hasParentIdMapperTypes()){
      LOGGER.warning("id mapping from parent network unknown for routed services, generate mappings and assume same mapping approach as for routed services");
      var serviceNetworkMappings = PlanitServiceNetworkWriter.createPlanitServiceNetworkIdMappingTypes(getIdMapperType());
      setParentIdMapperTypes(serviceNetworkMappings);
    }
  }

  /** Constructor
   *
   * @param xmlRawRoutedServices to populate with PLANit routed services when persisting
   */
  protected PlanitRoutedServicesWriter(XMLElementRoutedServices xmlRawRoutedServices) {
    this(null, CountryNames.GLOBAL, xmlRawRoutedServices);
  }

  /** Constructor
   *
   * @param outputPath to persist in
   * @param xmlRawRoutedServices to populate with PLANit routed services when persisting
   */
  protected PlanitRoutedServicesWriter(String outputPath, XMLElementRoutedServices xmlRawRoutedServices) {
    this(outputPath, CountryNames.GLOBAL, xmlRawRoutedServices);
  }

  /** Constructor
   *
   * @param outputPath to persist in
   * @param countryName to optimise projection for (if available, otherwise ignore)
   * @param xmlRawRoutedServices to populate with PLANit routed services when persisting
   */
  protected PlanitRoutedServicesWriter(String outputPath, String countryName, XMLElementRoutedServices xmlRawRoutedServices) {
    super(IdMapperType.XML);
    this.settings = new PlanitRoutedServicesWriterSettings(outputPath, DEFAULT_ROUTED_SERVICES_XML, countryName);
    this.xmlRawRoutedServices = xmlRawRoutedServices;
  }

  /** default routed services file name to use */
  public static final String DEFAULT_ROUTED_SERVICES_XML = "routed_services.xml";

  /**
   * Create id mappers per type based on a given id mapping type
   *
   * @return newly created map with all zoning entity mappings
   */
  public static Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> createPlanitRoutedServicesIdMappingTypes(IdMapperType mappingType){
    var result = new HashMap<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>>();
    result.put(RoutedTrip.class, IdMapperFunctionFactory.createRoutedTripIdMappingFunction(mappingType));
    result.put(RoutedTripDeparture.class,  IdMapperFunctionFactory.createRoutedTripDepartureIdMappingFunction(mappingType));
    result.put(RoutedService.class, IdMapperFunctionFactory.createRoutedServiceIdMappingFunction(mappingType));
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(RoutedServices routedServices) throws PlanItException {

    /* initialise */
    initialiseIdMappingFunctions();
    LOGGER.info(String.format("Persisting PLANit routed services to: %s", Paths.get(getSettings().getOutputPathDirectory(), getSettings().getFileName())));
    getSettings().logSettings();
    
    /* xml id */
    populateTopLevelElement(routedServices);

    /* network layers */
    populateXmlRoutedServicesLayers(routedServices);
    
    /* persist */
    super.persist(xmlRawRoutedServices, XMLElementRoutedServices.class, PlanitSchema.ROUTED_SERVICES_XSD);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    currLayerLogPrefix = null;
    xmlRawRoutedServices.getServicelayers().getServicelayer().clear();
    xmlRawRoutedServices.setServicelayers(null);
    xmlRawRoutedServices.setId(null);
    xmlRawRoutedServices.setExternalid(null);
  }  
  
  // GETTERS/SETTERS
  
  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitRoutedServicesWriterSettings getSettings() {
    return this.settings;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> getIdMapperByType() {
    return new HashMap<>(this.routedServicesIdMappers);
  }

}
