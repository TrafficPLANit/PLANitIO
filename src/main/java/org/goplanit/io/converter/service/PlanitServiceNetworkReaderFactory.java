package org.goplanit.io.converter.service;

import java.util.logging.Logger;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.xml.generated.XMLElementServiceNetwork;


/**
 * Factory for creating PlanitServiceNetworkReaders
 * 
 * @author markr
 *
 */
public class PlanitServiceNetworkReaderFactory {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitServiceNetworkReaderFactory.class.getCanonicalName());
  
  /** Create a PLANitServiceNetworkReader
   * 
   * @param parentNetwork the parent network the service network is assumed to be built upon
   * @return created service network reader
   */
  public static PlanitServiceNetworkReader create(MacroscopicNetwork parentNetwork) {
    return create(IdGroupingToken.collectGlobalToken(), parentNetwork);
  }   
  
  /** Create a PLANitServiceNetworkReader based on custom id token
   * 
   * @param idToken to use for service network id generation
   * @param parentNetwork the network the service network is assumed to be built upon  
   * @return created service network reader
   */
  public static PlanitServiceNetworkReader create(final IdGroupingToken idToken, MacroscopicNetwork parentNetwork) {
    return create(new PlanitServiceNetworkReaderSettings(parentNetwork), new ServiceNetwork(idToken, parentNetwork));    
  }
  
  /** Create a PLANitServiceNetworkReader sourced from given input directory
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
  
  /** Create a PLANitServiceNetworkReader based on given settings which in turn contain information on location and parent network to use
   * 
   * @param settings to use
   * @return created service network reader
   */
  public static PlanitServiceNetworkReader create(final PlanitServiceNetworkReaderSettings settings) {
    return create(settings, new ServiceNetwork(IdGroupingToken.collectGlobalToken(), settings.getParentNetwork()));
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
  public static PlanitServiceNetworkReader create(final String inputDirectory, final String xmlFileExtension, final ServiceNetwork serviceNetwork) {
    try {
      return new PlanitServiceNetworkReader(inputDirectory, xmlFileExtension, serviceNetwork);
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
  public static PlanitServiceNetworkReader create(final XMLElementServiceNetwork xmlRawServiceNetwork, final ServiceNetwork serviceNetwork) {
    try {
      return new PlanitServiceNetworkReader(xmlRawServiceNetwork, serviceNetwork);
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
    }  
    return null;    
  }    
     
}
