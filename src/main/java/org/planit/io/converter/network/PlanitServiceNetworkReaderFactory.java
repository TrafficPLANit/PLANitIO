package org.planit.io.converter.network;

import java.util.logging.Logger;

import org.planit.network.MacroscopicNetwork;
import org.planit.network.ServiceNetwork;
import org.planit.network.TransportLayerNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;


/**
 * Factory for creating PplanitServiceNetworkReaders
 * 
 * @author markr
 *
 */
public class PlanitServiceNetworkReaderFactory {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitServiceNetworkReaderFactory.class.getCanonicalName());
  
  /** Create a PLANitNetworkReader which will create a service network
   * 
   * @param parentNetwork the parent network the service network is assumed to be built upon
   * @return created service network reader
   */
  public static PlanitServiceNetworkReader create(MacroscopicNetwork parentNetwork) {
    return create(IdGroupingToken.collectGlobalToken(), parentNetwork);
  }   
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param idToken to use for service network id generation
   * @param parentNetwork the network the service network is assumed to be built upon  
   * @return created service network reader
   */
  public static PlanitServiceNetworkReader create(final IdGroupingToken idToken, MacroscopicNetwork parentNetwork) {
    return create(new PlanitServiceNetworkReaderSettings(parentNetwork), new ServiceNetwork(idToken, parentNetwork));    
  }
  
  /** Create a ServiceNetworkReader which creates a service network based on provided location and parent network
   * 
   * @param inputDirectory to use (directory only, find first compatible file)
   * @param parentNetwork the network the service network is assumed to be built upon
   * @return created service network reader
   */
  public static PlanitServiceNetworkReader create(final String inputDirectory, MacroscopicNetwork parentNetwork) {
    PlanitServiceNetworkReader serviceNetworkReader = create(parentNetwork);
    serviceNetworkReader.getSettings().setInputDirectory(inputDirectory);
    return serviceNetworkReader;
  }  
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param settings to use
   * @return created service network reader
   */
  public static PlanitServiceNetworkReader create(final PlanitServiceNetworkReaderSettings settings) {
    return create(settings, new ServiceNetwork(IdGroupingToken.collectGlobalToken()));
  }   
  
  /** Create a Service Network Reader for given (empty) service network and given settings
   * 
   * @param settings to use
   * @param serviceNetwork to use
   * @return created service network reader
   */
  public static PlanitServiceNetworkReader create(final PlanitServiceNetworkReaderSettings settings, final ServiceNetwork serviceNetwork) {
    try {
      return new PlanitServiceNetworkReader(settings, serviceNetwork);
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
    }    
    return null;
  }  
   
  
  /** Create a Service Network Reader for location and service network to populate
   * 
   * @param inputDirectory to use (directory only, find first compatible file)
   * @param xmlFileExtension to consider
   * @param serviceNetwork to populate
   * @return created service network reader
   */
  public static PlanitNetworkReader create(final String inputDirectory, final String xmlFileExtension, final ServiceNetwork serviceNetwork) {
    try {
      return new PlanitNetworkReader(inputDirectory, xmlFileExtension, serviceNetwork);
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
    }    
    return null;
  }  
    
  
  /** Create a Service Network Reader for given XML root element and service network to populate
   * 
   * @param xmlRawServiceNetwork the raw network based on the JAXB parser
   * @param serviceNetwork to populate
   * @return created PLANit service network reader
   */
  public static PlanitServiceNetworkReader create(final XMLElementMacroscopicNetwork xmlRawServiceNetwork, final ServiceNetwork serviceNetwork) {
    try {
      return new PlanitServiceNetworkReader(xmlRawServiceNetwork, serviceNetwork);
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
    }  
    return null;    
  }    
     
}
