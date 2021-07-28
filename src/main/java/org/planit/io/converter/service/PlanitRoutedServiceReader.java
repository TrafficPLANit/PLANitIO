package org.planit.io.converter.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.planit.converter.network.NetworkReaderImpl;
import org.planit.converter.service.RoutedServicesReader;
import org.planit.io.xml.util.PlanitXmlJaxbParser;
import org.planit.network.ServiceNetwork;
import org.planit.service.routed.RoutedServices;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.CharacterUtils;
import org.planit.utils.misc.StringUtils;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.ServiceNetworkLayer;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.layer.physical.Nodes;
import org.planit.utils.network.layer.service.ServiceLeg;
import org.planit.utils.network.layer.service.ServiceLegSegment;
import org.planit.utils.network.layer.service.ServiceNode;
import org.planit.utils.wrapper.MapWrapper;
import org.planit.utils.wrapper.MapWrapperImpl;
import org.planit.xml.generated.Direction;
import org.planit.xml.generated.XMLElementRoutedServices;
import org.planit.xml.generated.XMLElementServiceLeg;
import org.planit.xml.generated.XMLElementServiceLegs;
import org.planit.xml.generated.XMLElementServiceNetwork;
import org.planit.xml.generated.XMLElementServiceNetworkLayer;
import org.planit.xml.generated.XMLElementServiceNodes;

/**
 * Implementation of a routed service reader in the PLANit XML native format
 * 
 * @author markr
 *
 */
public class PlanitRoutedServiceReader implements RoutedServicesReader {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitRoutedServiceReader.class.getCanonicalName());            
  
  /** the settings for this reader */
  private final PlanitRoutedServicesReaderSettings settings;
  
  /** parses the XML content in JAXB memory format */
  private final PlanitXmlJaxbParser<XMLElementRoutedServices> xmlParser;
  
  /** the routed services to populate */
  private final RoutedServices routedServices;
      
  /**
   * In XML files we use the XML ids for referencing parent network entities. In memory internal ids are used for indexing, therefore
   * we keep a separate indices by XML within the reader to be able to quickly find entities by XML id if needed
   */
  private void initialiseParentNetworkReferenceIndices() {
    // TODO     
  }

  /** Constructor where settings are directly provided such that input information can be extracted from it
   * 
   * @param idToken to use for the service network to populate
   * @param settings to use
   * @throws PlanItException  thrown if error
   */
  protected PlanitRoutedServiceReader(IdGroupingToken idToken, PlanitRoutedServicesReaderSettings settings) throws PlanItException{
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
  protected PlanitRoutedServiceReader(PlanitRoutedServicesReaderSettings settings, RoutedServices routedServices) throws PlanItException{
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
  protected PlanitRoutedServiceReader(XMLElementRoutedServices populatedXmlRawRoutedServices, RoutedServices routedServices) throws PlanItException{
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
  protected PlanitRoutedServiceReader(String inputPathDirectory, String xmlFileExtension, RoutedServices routedServices) throws PlanItException{   
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
      //TODO
      
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
