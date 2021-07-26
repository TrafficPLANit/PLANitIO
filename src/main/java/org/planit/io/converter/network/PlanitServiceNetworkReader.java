package org.planit.io.converter.network;

import java.util.logging.Logger;
import org.planit.converter.network.NetworkReaderBase;
import org.planit.io.xml.util.PlanitXmlJaxbParser;
import org.planit.network.ServiceNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.ServiceNetworkLayer;
import org.planit.xml.generated.XMLElementServiceNetwork;

/**
 * Implementation of a service network reader in the PLANit XML native format
 * 
 * @author markr
 *
 */
public class PlanitServiceNetworkReader extends NetworkReaderBase {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitServiceNetworkReader.class.getCanonicalName());            
  
  /** the settings for this reader */
  private final PlanitServiceNetworkReaderSettings settings;
  
  /** parses the XML content in JAXB memory format */
  private final PlanitXmlJaxbParser<XMLElementServiceNetwork> xmlParser;
  
  /** the service network to populate */
  private final ServiceNetwork serviceNetwork;
      
  /**
   * parse the network layer
   * 
   * @param xmlLayer layer to extract from
   * @param jtsUtils to use
   * @return parsed network layer
   * @throws PlanItException thrown if error
   *
   */
  private ServiceNetworkLayer parseServiceNetworkLayer( /* XMLElementInfrastructureLayer xmlLayer, PlanitJtsCrsUtils jtsUtils */ ) throws PlanItException {
    //TODO
    return null;
  }  
  
  /** Parse the various network layers
   * 
   * @throws PlanItException thrown if error
   */
  private void parseServiceNetworkLayers() throws PlanItException {
    //TODO:
  }    
  
  /** Constructor where settings are directly provided such that input information can be extracted from it
   * 
   * @param idToken to use for the service network to populate
   * @param settings to use
   * @throws PlanItException  thrown if error
   */
  protected PlanitServiceNetworkReader(IdGroupingToken idToken, PlanitServiceNetworkReaderSettings settings) throws PlanItException{
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementServiceNetwork>(XMLElementServiceNetwork.class);
    this.settings = settings;
    this.serviceNetwork = new ServiceNetwork(idToken, settings.getParentNetwork());
  }  
  
  /** Constructor where settings and service network are directly provided
   * 
   * @param settings to use
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitServiceNetworkReader(PlanitServiceNetworkReaderSettings settings, ServiceNetwork serviceNetwork) throws PlanItException{
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementServiceNetwork>(XMLElementServiceNetwork.class);
    this.settings = settings;
    this.serviceNetwork = serviceNetwork;
    if(!settings.getParentNetwork().equals(serviceNetwork.getParentNetwork())) {
      LOGGER.severe("parent network in service network reader settings does not match the parent network in the provided service network for the PLANit service network reader");
    }
  }  
    
  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param externalXmlRawNetwork to extract from
   * @param serviceNetwork to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitServiceNetworkReader(XMLElementServiceNetwork externalXmlRawServiceNetwork, ServiceNetwork serviceNetwork) throws PlanItException{
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementServiceNetwork>(externalXmlRawServiceNetwork);
    this.settings = new PlanitServiceNetworkReaderSettings(serviceNetwork.getParentNetwork());
    this.serviceNetwork = serviceNetwork;
  }
  
  /** Constructor
   * 
   * @param networkPathDirectory to use
   * @param xmlFileExtension to use
   * @param serviceNetwork to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitServiceNetworkReader(String networkPathDirectory, String xmlFileExtension, ServiceNetwork serviceNetwork) throws PlanItException{   
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementServiceNetwork>(XMLElementServiceNetwork.class);
    this.settings = new PlanitServiceNetworkReaderSettings(serviceNetwork.getParentNetwork(), networkPathDirectory, xmlFileExtension);
    this.serviceNetwork = serviceNetwork;
  }  
  
  /** Default XSD files used to validate input XML files against, TODO: move to properties file */
  public static final String SERVICE_NETWORK_XSD_FILE = "https://trafficplanit.github.io/PLANitManual/xsd/servicenetworkinput.xsd";  

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetwork read() throws PlanItException {
        
    /* parse the XML raw network to extract PLANit network from */   
    xmlParser.initialiseAndParseXmlRootElement(getSettings().getInputDirectory(), getSettings().getXmlFileExtension());
    
//    /* xml id */
//    String networkXmlId = xmlParser.getXmlRootElement().getId();
//    if(StringUtils.isNullOrBlank(networkXmlId)) {
//      LOGGER.warning(String.format("Network has no XML id defined, adopting internally generated id %d instead",network.getId()));
//      networkXmlId = String.valueOf(network.getId());
//    }
//    network.setXmlId(networkXmlId);
//            
//    /* defaults */
//    injectMissingDefaultsToRawXmlNetwork();       
//    
//    try {
//      
//      /* parse modes*/
//      parseModes();
//
//      /* parse layers */
//      parseNetworkLayers();
//      
//      /* free xml content */
//      xmlParser.clearXmlContent();
//      
//    } catch (PlanItException e) {
//      throw e;
//    } catch (final Exception e) {
//      LOGGER.severe(e.getMessage());
//      throw new PlanItException("Error while populating physical network in PLANitIO",e);
//    }    
//    
    return serviceNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitServiceNetworkReaderSettings getSettings() {
    return settings;
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // TODO Auto-generated method stub    
  }
  
  


}
