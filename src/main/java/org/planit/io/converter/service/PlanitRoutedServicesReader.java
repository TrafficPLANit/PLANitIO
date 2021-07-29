package org.planit.io.converter.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.converter.service.RoutedServicesReader;
import org.planit.io.xml.util.PlanitXmlJaxbParser;
import org.planit.service.routed.RoutedModeServices;
import org.planit.service.routed.RoutedService;
import org.planit.service.routed.RoutedServiceTripInfo;
import org.planit.service.routed.RoutedServices;
import org.planit.service.routed.RoutedServicesLayer;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.StringUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.ServiceNetworkLayer;
import org.planit.utils.network.layer.service.ServiceLeg;
import org.planit.xml.generated.XMLElementRoutedServices;
import org.planit.xml.generated.XMLElementRoutedServices.Servicelayers;
import org.planit.xml.generated.XMLElementRoutedServicesLayer;
import org.planit.xml.generated.XMLElementRoutedTrip;
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
  
  /** Parse the trips definition for a given routed service in this XML file
   * 
   * @param xmlTrips to extract trips from
   * @param routedService the memory model version to populate
   */  
  private void parseRoutedTripsForService(XMLElementRoutedTrips xmlTrips, RoutedService routedService) {
    List<XMLElementRoutedTrip> xmlTripsList = xmlTrips.getTrip();
    if(xmlTripsList==null || xmlTripsList.isEmpty()) {
      LOGGER.warning(String.format("Routed service %s has no trip entries defined", routedService.getXmlId()));
      return;
    }  
    
    /* trip definitions*/
    for(XMLElementRoutedTrip xmlTrip : xmlTripsList) {
                  
      /* instance */
      RoutedServiceTripInfo tripInfo = routedService.getTrips().getFactory().registerNew();
      
      /* XML id */
      String xmlId = xmlTrip.getId();
      if(StringUtils.isNullOrBlank(xmlId)) {
        LOGGER.warning(String.format("Routed Service %s has trip without XML id defined, use generated id instead", routedService.getXmlId()));
        xmlId = String.valueOf(tripInfo.getId());
      }
      tripInfo.setXmlId(xmlId);      
      
      /* external id*/
      if(!StringUtils.isNullOrBlank(xmlTrip.getExternalid())) {
        tripInfo.setExternalId(xmlTrip.getExternalid());
      }   
    }
  }

  /** Parse the services for a single supported mode on the given routed services layer defined in this XML file
   * 
   * @param xmlServices to extract services from
   * @param servicesByMode the memory model version to populate
   */  
  private void parseRoutedModeServicesWithinLayer(XMLElementServices xmlServices, RoutedModeServices servicesByMode) {
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
      List<XMLElementRoutedTrips> xmlTripsList = xmlRoutedService.getTrips();
      if(xmlTripsList==null || xmlTripsList.isEmpty()) {
        LOGGER.warning(String.format("Routed service %s has no trips defined", routedService.getXmlId()));
        return;
      }
      
      /* trips */
      for(XMLElementRoutedTrips xmlTrips : xmlTripsList) {
        parseRoutedTripsForService(xmlTrips, routedService);
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
      parseRoutedModeServicesWithinLayer(xmlModeServices, servicesByMode);
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
    if(!settings.hasParentLegsByXmlId()) {      
      for(ServiceNetworkLayer layer : routedServices.getParentNetwork().getTransportLayers()) {
        settings.setParentLegsByXmlId(layer, new HashMap<String, ServiceLeg>());        
        Map<String,ServiceLeg> legsByXmlId = settings.getParentLegsByXmlId(layer);
        layer.getLegs().forEach( leg -> legsByXmlId.put(leg.getXmlId(), leg));
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
