package org.goplanit.io.converter.service;

import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.converter.idmapping.PlanitComponentIdMapper;
import org.goplanit.converter.idmapping.RoutedServicesIdMapper;
import org.goplanit.converter.service.RoutedServicesWriter;
import org.goplanit.io.converter.PlanitWriterImpl;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.io.xml.util.xmlEnumConversionUtil;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.CharacterUtils;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.service.routed.*;
import org.goplanit.xml.generated.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.nio.file.Paths;
import java.util.*;
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
  private static DatatypeFactory xmlDataTypeFactory;
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

  /**
   * Add the id information to trip
   *
   * @param xmlTrip to add id info  to
   * @param trip to extract from
   */
  private void populateXmlTripIds(XMLElementRoutedTrip xmlTrip, RoutedTrip trip) {
    /* xml id*/
    xmlTrip.setId(getPrimaryIdMapper().getRoutedTripRefIdMapper().apply(trip));

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
  private void createAndPopulateXmlRoutedServiceTrip(XMLElementRoutedTrips xmlTrips, RoutedTripFrequency frequencyBasedTrip) {
    var xmlRoutedTripFrequency = new XMLElementRoutedTrip();

    /* xml id, external id */
    populateXmlTripIds(xmlRoutedTripFrequency, frequencyBasedTrip);

    /* frequency */
    {
      if(!frequencyBasedTrip.hasPositiveFrequency()){
        LOGGER.warning(String.format("Frequency based routed trip %s has no positive frequency specified, discarded", xmlRoutedTripFrequency.getId()));
      }
      var frequency = new XMLElementRoutedTrip.Frequency();

      /* unit */
      frequency.setUnit(xmlEnumConversionUtil.planitToXml(getSettings().getTripFrequencyTimeUnit()));

      /* frequency value */
      frequency.setValue((float) HOUR.convertTo(getSettings().getTripFrequencyTimeUnit(), frequencyBasedTrip.getFrequencyPerHour()));

      /* ls refs */
      var lsRefsList = new ArrayList<String>(frequencyBasedTrip.getNumberOfLegSegments());
      for(int index = 0; index < frequencyBasedTrip.getNumberOfLegSegments(); ++index){
        lsRefsList.add(getComponentIdMappers().getServiceNetworkIdMapper().getServiceLegSegmentIdMapper().apply(frequencyBasedTrip.getLegSegment(index)));
      }
      if(lsRefsList.isEmpty()){
        LOGGER.warning(String.format("No service leg segments present on frequency based trip (%s), discarded", frequencyBasedTrip.getXmlId()));
      }
      frequency.setLsrefs(lsRefsList.stream().collect(Collectors.joining(CharacterUtils.COMMA.toString())));

      xmlRoutedTripFrequency.setFrequency(frequency);
    }

    xmlTrips.getTrip().add(xmlRoutedTripFrequency);
  }

  /**
   * Populate a routed service trip based on a schedule as XML element
   *
   * @param xmlTrips to add trip to
   * @param scheduleBasedTrip to extract from
   * @return true when successful, false otherwise
   */
  private void createAndPopulateXmlRoutedServiceTrip(XMLElementRoutedTrips xmlTrips, RoutedTripSchedule scheduleBasedTrip) {
    var xmlRoutedTripSchedule = new XMLElementRoutedTrip();

    /* xml id, external id */
    populateXmlTripIds(xmlRoutedTripSchedule, scheduleBasedTrip);

    /* schedule */
    {
      var xmlSchedule = new XMLElementRoutedTrip.Schedule();

      /* departures */
      if(!scheduleBasedTrip.hasDepartures()){
        LOGGER.warning(String.format("No departures present on schedule based trip (%s), discarded", scheduleBasedTrip.getXmlId()));
        return;
      }
      var xmlDepartures = new XMLElementDepartures();
      xmlSchedule.setDepartures(xmlDepartures);

      /* departure - in ascending order by departure time*/
      var xmlDepartureList = xmlDepartures.getDeparture();
      scheduleBasedTrip.getDepartures().streamAscDepartureTime().forEach( departure -> {
        var xmlDeparture = new XMLElementDepartures.Departure();

        /* departure XML id */
        xmlDeparture.setId(getPrimaryIdMapper().getRoutedTripDepartureRefIdMapper().apply(departure));

        /* departure external id */
        if(departure.hasExternalId()){
          xmlDeparture.setExternalid(departure.getExternalId());
        }

        /* time (HH:mm:ss) */
        xmlDeparture.setTime(departure.getDepartureTime().toString());

        xmlDepartureList.add(xmlDeparture);
      });

      /* rel timings */
      if(!scheduleBasedTrip.hasRelativeLegTimings()){
        LOGGER.warning(String.format("No relative leg timings present on schedule based trip (%s), discarded", scheduleBasedTrip.getXmlId()));
        return;
      }

      var xmlRelTimings = new XMLElementRelativeTimings();
      xmlSchedule.setReltimings(xmlRelTimings);

      scheduleBasedTrip.updateDefaultDwellTimeToMostCommon();
      var defaultDwellTime = scheduleBasedTrip.getDefaultDwellTime();

      {
        /* rel timing */
        var xmlRelTimingLegList = xmlRelTimings.getLeg();
        for(var relLegTiming : scheduleBasedTrip) {
          var xmlReltimingLeg = new XMLElementRelativeTimings.Leg();

          /* duration */
          xmlReltimingLeg.setDuration(relLegTiming.getDuration());

          /* dwell time */
          if(!relLegTiming.getDwellTime().equals(defaultDwellTime)){
            xmlReltimingLeg.setDwelltime(relLegTiming.getDwellTime());
          }

          /* service leg segment reference */
          if(!relLegTiming.hasParentLegSegment()){
            LOGGER.warning(String.format("No service leg segment present on relative leg timing, discarded this trip (%s)", scheduleBasedTrip.getXmlId()));
            return;
          }
          xmlReltimingLeg.setLsref(getComponentIdMappers().getServiceNetworkIdMapper().getServiceLegSegmentIdMapper().apply(relLegTiming.getParentLegSegment()));

          xmlRelTimingLegList.add(xmlReltimingLeg);
        }
      }

      if(defaultDwellTime!=null){
        xmlRelTimings.setDwelltime(defaultDwellTime);
      }

      xmlRoutedTripSchedule.setSchedule(xmlSchedule);
    }

    xmlTrips.getTrip().add(xmlRoutedTripSchedule);
    return;
  }

  /**
   * Populate a routed service as XML element
   *
   * @param xmlServices to add service to
   * @param routedService to extract from
   * @return true when successful, false otherwise
   */
  private void createAndPopulateXmlRoutedServices(XMLElementServices xmlServices, RoutedService routedService) {
    XMLElementService xmlRoutedService = new XMLElementService();

    /* XML id */
    xmlRoutedService.setId(getPrimaryIdMapper().getRoutedServiceRefIdMapper().apply(routedService));

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
      if(getSettings().isLogServicesWithoutTrips()) {
        LOGGER.warning(String.format("Routed service (%s, name: %s - %s) without trips found, discarding",
            xmlRoutedService.getId(), routedService.getName(), routedService.getNameDescription()));
      }
      return;
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
  }


  /**
   * Populate all routed services grouped by mode as XML element
   *
   * @param xmlLayer to add mode specific routed services to
   * @param servicesForMode to extract information from
   * @return true when successful, false when error during persisting
   */
  private void createAndPopulateXmlRoutedServicesByMode(XMLElementRoutedServicesLayer xmlLayer, RoutedModeServices servicesForMode) {
    if(servicesForMode.isEmpty()){
      /* no services for given mode found, skip */
      return;
    }

    /* umbrella element to add to */
    var xmlServices = new XMLElementServices();
    xmlLayer.getServices().add(xmlServices);

    /* mode */
    xmlServices.setModeref(getComponentIdMappers().getNetworkIdMappers().getModeIdMapper().apply(servicesForMode.getMode()));

    /* for each service populate and XML element */
    for( var service : servicesForMode){
      createAndPopulateXmlRoutedServices(xmlServices, service);
    }

    var modePrefix = LoggingUtils.surroundwithBrackets(String.format("mode: %s", servicesForMode.getMode()));
    LOGGER.info(String.format("%s%s Routed services : %d", currLayerLogPrefix, modePrefix, servicesForMode.size()));
    LOGGER.info(String.format("%s%s (scheduled) trips : %d", currLayerLogPrefix, modePrefix,
        servicesForMode.stream().mapToInt( rs -> rs.getTripInfo().getScheduleBasedTrips().size()).sum()));
    LOGGER.info(String.format("%s%s (scheduled) trips departures: %d", currLayerLogPrefix, modePrefix,
        servicesForMode.stream().mapToInt( rs ->
            rs.getTripInfo().getScheduleBasedTrips().stream().mapToInt( t -> t.getDepartures().size()).sum()).sum()));
    LOGGER.info(String.format("%s%s (frequency) trips : %d", currLayerLogPrefix, modePrefix,
        servicesForMode.stream().mapToInt( rs -> rs.getTripInfo().getFrequencyBasedTrips().size()).sum()));
    LOGGER.info(String.format("%s%s (frequency) trips * freq : %.2f", currLayerLogPrefix, modePrefix,
        servicesForMode.stream().mapToDouble( rs -> rs.getTripInfo().getFrequencyBasedTrips().stream().mapToDouble( t -> t.getFrequencyPerHour()).sum()).sum()));
  }

  /**
   * Populate routed services layer in XML form based on provided PLANit memory model
   *
   * @param xmlLayer to populate
   * @param layer to extract information from
   * @param routedServices to use
   * @return true when successfully populated, false otherwise
   */
  private void populateXmlRoutedServiceLayer(XMLElementRoutedServicesLayer xmlLayer, RoutedServicesLayer layer, RoutedServices routedServices) {

    /* XML id */
    var xmlId = layer.getXmlId();
    if(layer.getXmlId() == null) {
      LOGGER.warning(String.format("Routed services layer has no XML id defined, adopting internally generated id %d instead", layer.getId()));
      xmlId = String.valueOf(layer.getId());
    }
    xmlLayer.setId(xmlId);
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
    for(var servicesForMode : layer){
      createAndPopulateXmlRoutedServicesByMode(xmlLayer, servicesForMode);
    }
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

      this.currLayerLogPrefix = LoggingUtils.surroundwithBrackets("rs-layer: "+layer.getXmlId());

      populateXmlRoutedServiceLayer(xmlLayer, layer, routedServices);
      if(!xmlLayer.getServices().isEmpty()) {
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
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesIdMapper getPrimaryIdMapper() {
    return getComponentIdMappers().getRoutedServicesIdMapper();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(RoutedServices routedServices) throws PlanItException {

    /* initialise */
    getComponentIdMappers().populateMissingIdMappers(getIdMapperType());
    LOGGER.info(String.format("Persisting PLANit routed services to: %s", Paths.get(getSettings().getOutputDirectory(), getSettings().getFileName())));
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

}
