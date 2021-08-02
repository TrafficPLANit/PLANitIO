package org.planit.io.converter.service;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.datatype.XMLGregorianCalendar;

import org.planit.converter.service.RoutedServicesReader;
import org.planit.io.xml.util.EnumConversionUtil;
import org.planit.io.xml.util.PlanitXmlJaxbParser;
import org.planit.service.routed.RoutedModeServices;
import org.planit.service.routed.RoutedService;
import org.planit.service.routed.RoutedServiceTripInfo;
import org.planit.service.routed.RoutedServices;
import org.planit.service.routed.RoutedServicesLayer;
import org.planit.service.routed.RoutedTrip;
import org.planit.service.routed.RoutedTripDeparture;
import org.planit.service.routed.RoutedTripDepartures;
import org.planit.service.routed.RoutedTripFrequency;
import org.planit.service.routed.RoutedTripSchedule;
import org.planit.service.routed.RoutedTripScheduleImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.CharacterUtils;
import org.planit.utils.misc.StringUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.ServiceNetworkLayer;
import org.planit.utils.network.layer.service.ServiceLegSegment;
import org.planit.utils.unit.UnitUtils;
import org.planit.utils.unit.Units;
import org.planit.xml.generated.TimeUnit;
import org.planit.xml.generated.XMLElementDepartures;
import org.planit.xml.generated.XMLElementRelativeTimings;
import org.planit.xml.generated.XMLElementRoutedServices;
import org.planit.xml.generated.XMLElementRoutedServices.Servicelayers;
import org.planit.xml.generated.XMLElementRoutedServicesLayer;
import org.planit.xml.generated.XMLElementRoutedTrip;
import org.planit.xml.generated.XMLElementRoutedTrip.Frequency;
import org.planit.xml.generated.XMLElementRoutedTrip.Schedule;
import org.planit.xml.generated.XMLElementRoutedTrips;
import org.planit.xml.generated.XMLElementService;
import org.planit.xml.generated.XMLElementServices;

/**
 * Implementation of a routed service reader in the PLANit XML native format
 * 
 * @author markr
 *
 */
public class PlanitRoutedServicesReader implements RoutedServicesReader {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitRoutedServicesReader.class.getCanonicalName());            
  
  /** the settings for this reader */
  private final PlanitRoutedServicesReaderSettings settings;
  
  /** parses the XML content in JAXB memory format */
  private final PlanitXmlJaxbParser<XMLElementRoutedServices> xmlParser;
  
  /** the routed services to populate */
  private final RoutedServices routedServices;
  
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
      XMLGregorianCalendar xmlDepartureTime = xmlDeparture.getTime();
      if(xmlDepartureTime==null) {
        LOGGER.warning(String.format("IGNORE: A routed trip %s has no departure time defined for its departure element, departure removed", routedTrip.getXmlId()));
        continue;        
      }
      LocalTime departureTime = xmlDepartureTime.toGregorianCalendar().toZonedDateTime().toLocalTime();            
      /* instance */
      RoutedTripDeparture departure = routedTripDepartures.getFactory().registerNew(departureTime);
      departure.setXmlId(xmlId);
      
      /* external id*/
      if(!StringUtils.isNullOrBlank(xmlDeparture.getExternalid())) {
        departure.setExternalId(xmlDeparture.getExternalid());
      }   
    }
    
    /* XML relative leg timings */
    XMLElementRelativeTimings xmlRelativeLegTimings = xmlSchedule.getReltimings();
    if(xmlRelativeLegTimings==null || xmlRelativeLegTimings.getLeg()==null || xmlRelativeLegTimings.getLeg().isEmpty()) {
      LOGGER.warning(String.format("IGNORE: Schedule based trip %s has no relative timings for its legs defined",routedTrip.getXmlId()));
      return;
    }    
    
    /* default dwell time */    
    final XMLGregorianCalendar xmlDefaultDwellTime= xmlRelativeLegTimings.getDwelltime();;
    PlanItException.throwIfNull(xmlDefaultDwellTime, "Default dwell time for scheduled routed service trips should be available but it is not");
    LocalTime defaultDwellTime = xmlDefaultDwellTime.toGregorianCalendar().toZonedDateTime().toLocalTime();
    /* set on implementation so it can be used for persistence later on if required, not used in memory model */
    ((RoutedTripScheduleImpl)routedTrip).setDefaultDwellTime(defaultDwellTime);
    
    /* relative leg timings */
    boolean validTimings = true;
    Map<String, ServiceLegSegment> parentLegSegmentsByXmlId = settings.getParentLegSegmentsByXmlId(routedServicesLayer.getParentLayer());    
    for( XMLElementRelativeTimings.Leg xmlRelativeTimingLeg : xmlRelativeLegTimings.getLeg()) {
      /* leg (segment) timing */
      String xmlLegSegmentRef = xmlRelativeTimingLeg.getLsref();
      if(StringUtils.isNullOrBlank(xmlLegSegmentRef)) {
        LOGGER.warning(String.format("IGNORE: Schedule based trip %s has relative timing for leg (segment) without reference to service leg segment",routedTrip.getXmlId()));
        validTimings = false;
        break;
      }

      /* leg reference */
      ServiceLegSegment parentLegSegment = parentLegSegmentsByXmlId.get(xmlLegSegmentRef);
      if(parentLegSegment==null) {
        LOGGER.warning(String.format("IGNORE: Unavailable leg segment referenced %s in scheduled trip %s leg timing ",xmlLegSegmentRef, routedTrip.getXmlId()));
        validTimings = false;
        break;
      }
      xmlRelativeTimingLeg.getDuration();
      
      /* scheduled duration of leg */
      final XMLGregorianCalendar xmlScheduledLegDuration = xmlRelativeTimingLeg.getDuration();
      if(xmlScheduledLegDuration==null) {
        LOGGER.warning(String.format("IGNORE: A scheduled trip %s its directional leg timing %s has no valid duration", routedTrip.getXmlId(), parentLegSegment.getXmlId()));
        validTimings = false;
        break;        
      }
      LocalTime duration = xmlScheduledLegDuration.toGregorianCalendar().toZonedDateTime().toLocalTime();    
      
      /* scheduled dwell time of leg */
      XMLGregorianCalendar xmlScheduledDwellTime= xmlRelativeTimingLeg.getDwelltime();
      if(xmlScheduledDwellTime==null) {
        xmlScheduledDwellTime = xmlDefaultDwellTime; 
      }
      LocalTime dwellTime = xmlScheduledDwellTime.toGregorianCalendar().toZonedDateTime().toLocalTime();       
      
      /* instance on schedule */
      routedTrip.addRelativeLegSegmentTiming(parentLegSegment, duration, dwellTime);
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
    Map<String, ServiceLegSegment> parentLegSegmentsByXmlId = settings.getParentLegSegmentsByXmlId(routedServicesLayer.getParentLayer());    
    String[] xmlLegRefsArray = xmlLegRefs.split(CharacterUtils.COMMA.toString());
    for(int index=0;index<xmlLegRefs.length();++index) {
      
      ServiceLegSegment parentLegSegment = parentLegSegmentsByXmlId.get(xmlLegRefsArray[index]);
      if(parentLegSegment==null) {
        LOGGER.warning(String.format("IGNORE: Unavailable directed leg referenced %s in trip %s",xmlLegRefsArray[index], routedTrip.getXmlId()));
        routedTrip.clearLegs();
      }
      routedTrip.addLegSegment(parentLegSegment);
    }
    
    /* unit of frequency */
    TimeUnit xmlTimeUnit = xmlFrequency.getUnit();    
    PlanItException.throwIfNull(xmlTimeUnit,"Unavailable time unit for frequency in trip %s",routedTrip.getXmlId());
    Units xmlFromUnit = EnumConversionUtil.xmlToPlanit(xmlTimeUnit);
    
    /* XML frequency */
    double xmlNonNormalisedFrequency = xmlFrequency.getValue();
    if(xmlNonNormalisedFrequency<=0) {
      LOGGER.warning(String.format("IGNORE: Invalid or absent frequency for trip %s, please specify a valid frequency (>0)",routedTrip.getXmlId()));
      return;
    }
    
    /* apply conversion in opposite direction since frequency is the inverse of a "normal" time value, e.g. 1 per hour, should become 1/3600 per second and not 3600 */ 
    double frequencyPerHour = UnitUtils.convert(Units.HOUR, xmlFromUnit, xmlNonNormalisedFrequency);
    
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

  /**
   * In XML files we use the XML ids for referencing parent network entities. In memory internal ids are used for indexing, therefore
   * we keep a separate indices by XML within the reader to be able to quickly find entities by XML id if needed
   */
  private void initialiseParentNetworkReferenceIndices() {
    if(!settings.hasParentLegSegmentsByXmlId()) {      
      for(ServiceNetworkLayer layer : routedServices.getParentNetwork().getTransportLayers()) {
        settings.setParentLegSegmentsByXmlId(layer, new HashMap<String, ServiceLegSegment>());        
        Map<String,ServiceLegSegment> legSegmentsByXmlId = settings.getParentLegSegmentsByXmlId(layer);
        layer.getLegSegments().forEach( legSegment -> legSegmentsByXmlId.put(legSegment.getXmlId(), legSegment));
      }
    }    
  }

  /** Constructor where settings are directly provided such that input information can be extracted from it
   * 
   * @param idToken to use for the service network to populate
   * @param settings to use
   * @throws PlanItException  thrown if error
   */
  protected PlanitRoutedServicesReader(final IdGroupingToken idToken, final PlanitRoutedServicesReaderSettings settings) throws PlanItException{
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementRoutedServices>(XMLElementRoutedServices.class);
    this.settings = settings;
    this.routedServices = new RoutedServices(idToken, settings.getParentNetwork());
  }  
  
  /** Constructor where settings and routed services to populate are directly provided
   * 
   * @param settings to use
   * @param routedServices to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitRoutedServicesReader(final PlanitRoutedServicesReaderSettings settings, final RoutedServices routedServices) throws PlanItException{
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementRoutedServices>(XMLElementRoutedServices.class);
    this.settings = settings;
    this.routedServices = routedServices;
    if(!settings.getParentNetwork().equals(routedServices.getParentNetwork())) {
      LOGGER.severe("parent network in settings instance does not match the parent network in the provided routed services instance for the PLANit routed services reader");
    }
  }  
    
  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param populatedXmlRawRoutedServices to extract from
   * @param routedServices to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitRoutedServicesReader(final XMLElementRoutedServices populatedXmlRawRoutedServices, final RoutedServices routedServices) throws PlanItException{
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementRoutedServices>(populatedXmlRawRoutedServices);
    this.settings = new PlanitRoutedServicesReaderSettings(routedServices.getParentNetwork());
    this.routedServices = routedServices;
  }
  
  /** Constructor
   * 
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   * @param routedServices to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitRoutedServicesReader(final String inputPathDirectory, final String xmlFileExtension, final RoutedServices routedServices) throws PlanItException{   
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementRoutedServices>(XMLElementRoutedServices.class);
    this.settings = new PlanitRoutedServicesReaderSettings(routedServices.getParentNetwork(), inputPathDirectory, xmlFileExtension);
    this.routedServices = routedServices;
  }  
  
  /** Default XSD files used to validate input XML files against, TODO: move to properties file */
  public static final String ROUTED_SERVICES_XSD_FILE = "https://trafficplanit.github.io/PLANitManual/xsd/routedservicesinput.xsd";  

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServices read() throws PlanItException {
        
    /* parse the XML raw network to extract PLANit network from */   
    xmlParser.initialiseAndParseXmlRootElement(getSettings().getInputDirectory(), getSettings().getXmlFileExtension());
    PlanItException.throwIfNull(xmlParser.getXmlRootElement(), "No valid PLANit XML routed services could be parsed into memory, abort");
    
    /* XML id */
    String xmlId = xmlParser.getXmlRootElement().getId();
    if(StringUtils.isNullOrBlank(xmlId)) {
      LOGGER.warning(String.format("Routed services has no XML id defined, adopting internally generated id %d instead", routedServices.getId()));
      xmlId = String.valueOf(routedServices.getId());
    }
    routedServices.setXmlId(xmlId);    
                  
    try {
      
      /* initialise the indices used, if needed */
      initialiseParentNetworkReferenceIndices();      

      /* parse content */
      parseRoutedServiceLayers();
      
      /* free XML content after parsing */
      xmlParser.clearXmlContent();           
      
    } catch (PlanItException e) {
      throw e;
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException(String.format("Error while populating routed services %s in PLANitIO", routedServices.getXmlId()),e);
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
    settings.reset();
  }

}
