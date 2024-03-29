package org.goplanit.io.converter.service;

import java.time.LocalTime;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.goplanit.converter.BaseReaderImpl;
import org.goplanit.converter.service.RoutedServicesReader;
import org.goplanit.io.xml.util.xmlEnumConversionUtil;
import org.goplanit.io.xml.util.PlanitXmlJaxbParser;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.service.routed.*;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.service.routed.RoutedTripScheduleImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.CharacterUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.time.ExtendedLocalTime;
import org.goplanit.utils.unit.Unit;
import org.goplanit.xml.generated.TimeUnit;
import org.goplanit.xml.generated.XMLElementDepartures;
import org.goplanit.xml.generated.XMLElementRelativeTimings;
import org.goplanit.xml.generated.XMLElementRoutedServices;
import org.goplanit.xml.generated.XMLElementRoutedServicesLayer;
import org.goplanit.xml.generated.XMLElementRoutedTrip;
import org.goplanit.xml.generated.XMLElementRoutedTrips;
import org.goplanit.xml.generated.XMLElementService;
import org.goplanit.xml.generated.XMLElementServices;
import org.goplanit.xml.generated.XMLElementRoutedServices.Servicelayers;
import org.goplanit.xml.generated.XMLElementRoutedTrip.Frequency;
import org.goplanit.xml.generated.XMLElementRoutedTrip.Schedule;

/**
 * Implementation of a routed service reader in the PLANit XML native format
 * 
 * @author markr
 *
 */
public class PlanitRoutedServicesReader extends BaseReaderImpl<RoutedServices> implements RoutedServicesReader {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitRoutedServicesReader.class.getCanonicalName());            
  
  /** the settings for this reader */
  private final PlanitRoutedServicesReaderSettings settings;
  
  /** parses the XML content in JAXB memory format */
  private final PlanitXmlJaxbParser<XMLElementRoutedServices> xmlParser;

  /** the routed services to populate */
  private final RoutedServices routedServices;
  
  /**
   * initialise the XML id trackers and populate them for the parent PLANit references, 
   * so we can lay indices on the XML id as well for quick lookups
   * 
   */
  private void initialiseParentXmlIdTrackers() {
    initialiseSourceIdMap(ServiceLegSegment.class, ServiceLegSegment::getXmlId);
    routedServices.getParentNetwork().getTransportLayers().forEach( layer -> getSourceIdContainer(ServiceLegSegment.class).addAll(layer.getLegSegments()));
  }    
  
  /** Parse a schedule based trip for the given routed service
   * 
   * @param xmlSchedule to extract from
   * @param routedTrip to populate
   * @param routedServicesLayer to use
   * @throws PlanItException thrown if error
   */  
  private void parseScheduleBasedTrip(final Schedule xmlSchedule, final RoutedTripSchedule routedTrip, final RoutedServicesLayer routedServicesLayer) throws PlanItException {

    /* XML departures */
    XMLElementDepartures xmlDepartures = xmlSchedule.getDepartures();
    if(xmlDepartures==null || xmlDepartures.getDeparture()==null || xmlDepartures.getDeparture().isEmpty()) {
      LOGGER.warning(String.format("IGNORE: Schedule based trip %s has no departures defined",routedTrip.getXmlId()));
      return;
    }
    
    /* departures */
    RoutedTripDepartures routedTripDepartures = routedTrip.getDepartures();
    for(XMLElementDepartures.Departure xmlDeparture : xmlDepartures.getDeparture()) {
      /* departure */
      
      /* XML id */
      String xmlId = xmlDeparture.getId();
      if(StringUtils.isNullOrBlank(xmlId)) {
        LOGGER.warning(String.format("IGNORE: A routed trip %s has no XML id defined for a departure, departure removed", routedTrip.getXmlId()));
        continue;
      }
      
      /* departure time */
      String extendedDepartureTime = xmlDeparture.getTime();
      if(extendedDepartureTime==null) {
        LOGGER.warning(String.format("IGNORE: A routed trip %s has no departure time defined for its departure element, departure removed", routedTrip.getXmlId()));
        continue;        
      }
      var parsedDepartureTime = ExtendedLocalTime.of(extendedDepartureTime);
      /* instance */
      RoutedTripDeparture departure = routedTripDepartures.getFactory().registerNew(parsedDepartureTime);
      departure.setXmlId(xmlId);
      
      /* external id*/
      if(!StringUtils.isNullOrBlank(xmlDeparture.getExternalid())) {
        departure.setExternalId(xmlDeparture.getExternalid());
      }   
    }
    
    /* XML relative leg timings */
    XMLElementRelativeTimings xmlRelativeLegTimings = xmlSchedule.getReltimings();
    if(xmlRelativeLegTimings==null || xmlRelativeLegTimings.getLeg()==null || xmlRelativeLegTimings.getLeg().isEmpty()) {
      LOGGER.warning(String.format("IGNORE: Schedule based trip %s has no relative timings (reltimings=) for its legs defined",routedTrip.getXmlId()));
      return;
    }    
    
    /* default dwell time */
    //TODO: for some reason the xsd's default dwell time of 00:00:00 is not populated by JAXB so we do it programmatically here for now
    LocalTime defaultDwellTime = LocalTime.MIN;
    if(xmlRelativeLegTimings.getDwelltime()!=null) {
      defaultDwellTime = xmlRelativeLegTimings.getDwelltime();
    }
    /* set on implementation so it can be used for persistence later on if required, not used in memory model */
    ((RoutedTripScheduleImpl)routedTrip).setDefaultDwellTime(defaultDwellTime);
    
    /* relative leg timings */
    boolean validTimings = true;   
    for( XMLElementRelativeTimings.Leg xmlRelativeTimingLeg : xmlRelativeLegTimings.getLeg()) {
      /* leg (segment) timing */
      String xmlLegSegmentRef = xmlRelativeTimingLeg.getLsref();
      if(StringUtils.isNullOrBlank(xmlLegSegmentRef)) {
        LOGGER.warning(String.format("IGNORE: Schedule based trip %s has relative timing for leg (segment) without reference to service leg segment, attribute lsref= missing",routedTrip.getXmlId()));
        validTimings = false;
        break;
      }

      /* leg reference */
      ServiceLegSegment parentLegSegment = getBySourceId(ServiceLegSegment.class, xmlLegSegmentRef);
      if(parentLegSegment==null) {
        LOGGER.warning(String.format("IGNORE: Unavailable leg segment referenced lsref=%s in scheduled trip %s leg timing ",xmlLegSegmentRef, routedTrip.getXmlId()));
        validTimings = false;
        break;
      }
      xmlRelativeTimingLeg.getDuration();
      
      /* scheduled duration of leg */
      var scheduledLegDuration = xmlRelativeTimingLeg.getDuration();
      if(scheduledLegDuration == null) {
        LOGGER.warning(String.format("IGNORE: A scheduled trip %s its directional leg timing %s has no valid duration", routedTrip.getXmlId(), parentLegSegment.getXmlId()));
        validTimings = false;
        break;        
      }
      
      /* scheduled dwell time of leg */
      var scheduledDwellTime= xmlRelativeTimingLeg.getDwelltime();
      if(scheduledDwellTime==null) {
        scheduledDwellTime = defaultDwellTime;
      }             
      
      /* instance on schedule */
      routedTrip.addRelativeLegSegmentTiming(parentLegSegment, scheduledLegDuration, scheduledDwellTime);
    }
    
    if(!validTimings) {
      routedTrip.clearRelativeLegTimings();
    }
  }

  /** Parse a frequency based trip for the given routed service
   * 
   * @param xmlFrequency to extract from
   * @param routedTrip to populate
   * @param routedServicesLayer to use
   * @throws PlanItException thrown if error
   */    
  private void parseFrequencyBasedTrip(final Frequency xmlFrequency, final RoutedTripFrequency routedTrip, final RoutedServicesLayer routedServicesLayer) throws PlanItException {
    /* leg segment references */
    String xmlLegRefs = xmlFrequency.getLsrefs();
    if(StringUtils.isNullOrBlank(xmlLegRefs)) {
      LOGGER.warning(String.format("IGNORE: Frequency based trip %s has no references to underlying service leg segments",routedTrip.getXmlId()));
      return;
    }

    /* add legs to trip */    
    String[] xmlLegRefsArray = xmlLegRefs.split(CharacterUtils.COMMA.toString());
    for(int index=0;index<xmlLegRefsArray.length;++index) {
      
      ServiceLegSegment parentLegSegment = getBySourceId(ServiceLegSegment.class, xmlLegRefsArray[index].trim());
      if(parentLegSegment==null) {
        LOGGER.warning(String.format("IGNORE: Unavailable directed leg referenced %s in trip %s",xmlLegRefsArray[index], routedTrip.getXmlId()));
        routedTrip.clearLegs();
      }
      routedTrip.addLegSegment(parentLegSegment);
    }
    
    /* unit of frequency */
    TimeUnit xmlTimeUnit = xmlFrequency.getUnit();
    if(xmlTimeUnit == null){
      throw new PlanItRunTimeException("Unavailable time unit for frequency in trip %s",routedTrip.getXmlId());
    }
    org.goplanit.utils.unit.TimeUnit planitFrequencyTimeUnit = xmlEnumConversionUtil.xmlToPlanit(xmlTimeUnit);
    
    /* XML frequency */
    double xmlNonNormalisedFrequency = xmlFrequency.getValue();
    if(xmlNonNormalisedFrequency<=0) {
      LOGGER.warning(String.format("IGNORE: Invalid or absent frequency for trip %s, please specify a valid frequency (>0)",routedTrip.getXmlId()));
      return;
    }
    
    /* apply conversion in opposite direction since frequency is the inverse of a "normal" time value, e.g. 1 per hour, should become 1/3600 per second and not 3600 */ 
    double frequencyPerHour = Unit.HOUR.convertTo(planitFrequencyTimeUnit, xmlNonNormalisedFrequency);
    
    /* frequency */
    routedTrip.setFrequencyPerHour(frequencyPerHour);
  }

  /** Parse the various trips (frequency or schedule based) for the given routed service
   * 
   * @param xmlTrip to populate
   * @param routedService to extract from
   * @param routedServicesLayer to use
   * @throws PlanItException thrown if error
   */
  private void parseRoutedTripInfo(final XMLElementRoutedTrip xmlTrip, final RoutedService routedService, final RoutedServicesLayer routedServicesLayer) throws PlanItException {
    /* populate trip information on tripInfo */
    RoutedServiceTripInfo tripInfo = routedService.getTripInfo();
        
    /* XML id */
    String xmlId = xmlTrip.getId();
    if(StringUtils.isNullOrBlank(xmlId)) {
      LOGGER.warning(String.format("IGNORE: A trip on routed service (%s) has no XML id defined", routedService.getXmlId()));
      return;
    }
    
    RoutedTrip routedTrip = null;
    if(xmlTrip.getFrequency()!=null) {
      routedTrip = tripInfo.getFrequencyBasedTrips().getFactory().registerNew();
      routedTrip.setXmlId(xmlId);
      parseFrequencyBasedTrip(xmlTrip.getFrequency(), (RoutedTripFrequency) routedTrip, routedServicesLayer);
    }else if(xmlTrip.getSchedule() != null) {
      routedTrip = tripInfo.getScheduleBasedTrips().getFactory().registerNew();
      routedTrip.setXmlId(xmlId);
      parseScheduleBasedTrip(xmlTrip.getSchedule(), (RoutedTripSchedule) routedTrip, routedServicesLayer);      
    }else {
      LOGGER.warning(String.format("IGNORE: Trip (%s) on routed service (%s) has neither schedule nor frequency defined", xmlId, routedService.getXmlId()));
      return;      
    }
              
    /* external id*/
    if(!StringUtils.isNullOrBlank(xmlTrip.getExternalid())) {
      routedTrip.setExternalId(xmlTrip.getExternalid());
    }     
  }

  /** Parse the services for a single supported mode on the given routed services layer defined in this XML file
   * 
   * @param xmlServices to extract services from
   * @param servicesByMode the memory model version to populate
   * @param routedServicesLayer to use
   * @throws PlanItException thrown if error
   */  
  private void parseRoutedModeServicesWithinLayer(final XMLElementServices xmlServices, final RoutedModeServices servicesByMode, final RoutedServicesLayer routedServicesLayer) throws PlanItException {
    /* services */
    List<XMLElementService> xmlServicesList = xmlServices.getService();
    if(xmlServicesList == null || xmlServicesList.isEmpty()) {
      LOGGER.severe(String.format("IGNORE: No routed services within mode (%s) specific services defined",servicesByMode.getMode().getXmlId()));
      return;
    }
    
    /* routed service definitions*/
    for(XMLElementService xmlRoutedService : xmlServicesList) {
      /* XML id */
      String xmlId = xmlRoutedService.getId();
      if(StringUtils.isNullOrBlank(xmlId)) {
        LOGGER.warning(String.format("IGNORE: A routed Service for mode %s has no XML id defined", servicesByMode.getMode().getXmlId()));
        continue;
      }
            
      /* instance */
      RoutedService routedService = servicesByMode.getFactory().registerNew();
      routedService.setXmlId(xmlId);
      
      /* external id*/
      if(!StringUtils.isNullOrBlank(xmlRoutedService.getExternalid())) {
        routedService.setExternalId(xmlRoutedService.getExternalid());
      }   
      
      /* name*/
      if(!StringUtils.isNullOrBlank(xmlRoutedService.getName())) {
        routedService.setName(xmlRoutedService.getName());
      }    
      
      /* name description */
      if(!StringUtils.isNullOrBlank(xmlRoutedService.getNamedescription())) {
        routedService.setNameDescription(xmlRoutedService.getNamedescription());
      }   
      
      /* service description */
      if(!StringUtils.isNullOrBlank(xmlRoutedService.getServicedescription())) {
        routedService.setServiceDescription(xmlRoutedService.getServicedescription());
      }       
      
      /* trips definitions*/
      XMLElementRoutedTrips xmlTripsList = xmlRoutedService.getTrips();
      if(xmlTripsList==null) {
        LOGGER.warning(String.format("Routed service %s has no trips defined", routedService.getXmlId()));
        return;
      }
            
      /* trip definitions*/
      for(XMLElementRoutedTrip xmlTrip : xmlTripsList.getTrip()) {
        /* routed trip info */
        parseRoutedTripInfo(xmlTrip, routedService, routedServicesLayer);
      }      
    }
  }

  /** Parse the services per mode on the given routed services layer defined in this XML file
   * 
   * @param xmlRoutedServicesLayer to extract services (per mode for)
   * @param routedServicesLayer the memory model version to populate
   * @throws PlanItException thrown if error
   */
  private void parseRoutedServicesWithinLayer(final XMLElementRoutedServicesLayer xmlRoutedServicesLayer, final RoutedServicesLayer routedServicesLayer) throws PlanItException {
    /* services (by mode) */
    List<XMLElementServices> xmlModeServicesList = xmlRoutedServicesLayer.getServices();
    if(xmlModeServicesList == null || xmlModeServicesList.isEmpty()) {
      LOGGER.severe(String.format("IGNORE: Routed services layer has no services defined"));
      return;
    }
    
    /** routed services per mode */
    for(XMLElementServices xmlModeServices : xmlModeServicesList) {
      
      /* mode ref */
      String modeXmlRef = xmlModeServices.getModeref();
      if(StringUtils.isNullOrBlank(modeXmlRef)) {
        if(routedServicesLayer.getParentLayer().getSupportedModes().size()!=1) {
          LOGGER.severe(String.format("IGNORE: routed services layer %s services element has no supported mode specified",routedServicesLayer.getXmlId()));
          return;
        }else {
          /* only single mode on layer, so use that */          
          modeXmlRef = routedServicesLayer.getParentLayer().getFirstSupportedMode().getXmlId();
          LOGGER.info(String.format("routed services layer %s has no explicit supported mode specified, using only available mode %s instead",routedServicesLayer.getXmlId(), modeXmlRef));
        }
      } 
      
      /* memory model mode and compatibility */
      final String finalModeXmlRef = modeXmlRef;
      Mode supportedMode = routedServicesLayer.getParentLayer().getParentNetworkLayer().getSupportedModes().stream().filter(
          mode -> finalModeXmlRef.equals(mode.getXmlId())).findFirst().orElseThrow(
              () -> new PlanItException("Invalid mode %s referenced by routed service layer %s",finalModeXmlRef, routedServicesLayer.getXmlId()));
      if(!routedServicesLayer.getParentLayer().supports(supportedMode)) {
        LOGGER.severe(String.format("DISCARD: routed services defines a mode %s that is not supported on the parent service layer %s of the routed services",finalModeXmlRef, routedServicesLayer.getParentLayer().getXmlId()));
        return;      
      }
      
      /* container for services for mode */
      RoutedModeServices servicesByMode = routedServicesLayer.getServicesByMode(supportedMode);
      parseRoutedModeServicesWithinLayer(xmlModeServices, servicesByMode, routedServicesLayer);
    }

  }
      
  /** Parse a single routed services layer defined in this XML file
   * 
   * @param xmlRoutedServicesLayer to extract from
   * @throws PlanItException thrown if error
   */  
  private void parseRoutedServicesLayer(final XMLElementRoutedServicesLayer xmlRoutedServicesLayer) throws PlanItException {
    /* XML id */
    String layerXmlId = xmlRoutedServicesLayer.getId();
    if(StringUtils.isNullOrBlank(layerXmlId)) {
      LOGGER.severe(String.format("IGNORE: Routed services service layer has no XML id"));
      return;
    }
    
    /* parent layer (XML) */
    String xmlParentServiceNetworkLayerRef = xmlRoutedServicesLayer.getServicelayerref();
    if(StringUtils.isNullOrBlank(xmlParentServiceNetworkLayerRef)) {
      LOGGER.severe(String.format("IGNORE: routed services service layer %s has no reference to parent service network layer",layerXmlId));
      return;
    }    
    
    /* parent layer (memory) */
    ServiceNetworkLayer networkLayer = routedServices.getParentNetwork().getTransportLayers().getByXmlId(xmlParentServiceNetworkLayerRef);
    if(networkLayer == null) {
      LOGGER.severe(String.format("IGNORE: routed services layer %s references parent service network layer %s that is not available (yet)",layerXmlId, xmlParentServiceNetworkLayerRef));
      return;      
    }
    
    /* instance */
    RoutedServicesLayer routedServicesLayer = routedServices.getLayers().getFactory().registerNew(networkLayer);
    routedServicesLayer.setXmlId(layerXmlId);
    
    /* external id */
    if(!StringUtils.isNullOrBlank(xmlRoutedServicesLayer.getExternalid())) {
      routedServicesLayer.setExternalId(xmlRoutedServicesLayer.getExternalid());
    }
    
    /* services */
    parseRoutedServicesWithinLayer(xmlRoutedServicesLayer, routedServicesLayer);
  }

  /** Parse the available routed services layers defined in this XML file
   * 
   * @throws PlanItException thrown if error
   */
  private void parseRoutedServiceLayers() throws PlanItException{
    Servicelayers xmlServiceLayers = xmlParser.getXmlRootElement().getServicelayers();
    if(xmlServiceLayers==null) {
      LOGGER.warning(String.format("IGNORE: No service layers present in routed services file"));
      return;
    }
    
    //TODO: in future the reference should lead to a lookup of this network instead of it already locked in since this is too restrictive
    //      in more general use cases
    String parentNetworkXmlId = xmlServiceLayers.getServicenetworkref();
    PlanItException.throwIfNull(routedServices.getParentNetwork(), "No parent service network available on routed services %s memory model", routedServices.getXmlId());
    if(StringUtils.isNullOrBlank(parentNetworkXmlId) || !parentNetworkXmlId.equals(routedServices.getParentNetwork().getXmlId())){
      LOGGER.severe(String.format("IGNORE: routed services service layers different (or no) reference to parent service network than memory model (%s vs %s)",parentNetworkXmlId, routedServices.getParentNetwork().getXmlId()));
    }
    
    List<XMLElementRoutedServicesLayer> xmlServiceLayersList = xmlServiceLayers.getServicelayer();
        
    /* layers */
    for(XMLElementRoutedServicesLayer xmlServiceLayer : xmlServiceLayersList) {      
      /*layer */
      parseRoutedServicesLayer(xmlServiceLayer);
    } 
  }

  /** Constructor where settings are directly provided such that input information can be extracted from it
   * 
   * @param idToken to use for the service network to populate
   * @param parentServiceNetwork to use
   * @param settings to use
   * @throws PlanItException  thrown if error
   */
  protected PlanitRoutedServicesReader(final IdGroupingToken idToken, final ServiceNetwork parentServiceNetwork, final PlanitRoutedServicesReaderSettings settings) throws PlanItException{
    this(settings, new RoutedServices(idToken, parentServiceNetwork));
  }  
  
  /** Constructor where settings and routed services to populate are directly provided
   * 
   * @param settings to use
   * @param routedServices to populate
   */
  protected PlanitRoutedServicesReader(final PlanitRoutedServicesReaderSettings settings, final RoutedServices routedServices) {
    this.xmlParser = new PlanitXmlJaxbParser<>(XMLElementRoutedServices.class);
    this.settings = settings;
    this.routedServices = routedServices;
    if(routedServices.getParentNetwork() == null) {
      LOGGER.severe("parent service network not set on routed services, this is not allowed");
    }
  }  
    
  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param populatedXmlRawRoutedServices to extract from
   * @param routedServices to populate
   */
  protected PlanitRoutedServicesReader(final XMLElementRoutedServices populatedXmlRawRoutedServices, final RoutedServices routedServices){
    this(populatedXmlRawRoutedServices, new PlanitRoutedServicesReaderSettings(), routedServices);
  }

  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   *
   * @param populatedXmlRawRoutedServices to extract from
   * @param settings to use
   * @param routedServices to populate
   */
  protected PlanitRoutedServicesReader(final XMLElementRoutedServices populatedXmlRawRoutedServices, final PlanitRoutedServicesReaderSettings settings, final RoutedServices routedServices) {
    this.xmlParser = new PlanitXmlJaxbParser<>(populatedXmlRawRoutedServices);
    this.settings = settings;
    this.routedServices = routedServices;
  }
  
  /** Constructor
   * 
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   * @param routedServices to populate
   */
  protected PlanitRoutedServicesReader(final String inputPathDirectory, final String xmlFileExtension, final RoutedServices routedServices){
    this.xmlParser = new PlanitXmlJaxbParser<>(XMLElementRoutedServices.class);
    this.settings = new PlanitRoutedServicesReaderSettings(routedServices.getParentNetwork(), inputPathDirectory, xmlFileExtension);
    this.routedServices = routedServices;
  }  
  
  /** Default XSD files used to validate input XML files against, TODO: move to properties file */
  public static final String ROUTED_SERVICES_XSD_FILE = "https://trafficplanit.github.io/PLANitManual/xsd/routedservicesinput.xsd";  

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServices read(){
        
    /* parse the XML raw network to extract PLANit network from */   
    xmlParser.initialiseAndParseXmlRootElement(getSettings().getInputDirectory(), getSettings().getXmlFileExtension());
    PlanItRunTimeException.throwIfNull(xmlParser.getXmlRootElement(), "No valid PLANit XML routed services could be parsed into memory, abort");
    
    /* XML id */
    String xmlId = xmlParser.getXmlRootElement().getId();
    if(StringUtils.isNullOrBlank(xmlId)) {
      LOGGER.warning(String.format("Routed services has no XML id defined, adopting internally generated id %d instead", routedServices.getId()));
      xmlId = String.valueOf(routedServices.getId());
    }
    routedServices.setXmlId(xmlId);    
                  
    try {
      
      /* initialise the indices used, if needed */
      initialiseParentXmlIdTrackers();

      /* parse content */
      parseRoutedServiceLayers();

      /* log stats */
      routedServices.logInfo(LoggingUtils.routedServicesPrefix(routedServices.getId()));
      
      /* free XML content after parsing */
      xmlParser.clearXmlContent();           
      
    } catch (PlanItException e) {
      throw new PlanItRunTimeException(e);
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException(String.format("Error while populating routed services %s in PLANitIO", routedServices.getXmlId()),e);
    }    
    
    return routedServices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitRoutedServicesReaderSettings getSettings() {
    return settings;
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
  }

}
