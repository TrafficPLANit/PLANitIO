package org.goplanit.io.converter.service;

import java.util.logging.Logger;

import org.goplanit.network.ServiceNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.xml.generated.XMLElementRoutedServices;


/**
 * Factory for creating PlanitRoutedServicesReaders
 * 
 * @author markr
 *
 */
public class PlanitRoutedServicesReaderFactory {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitRoutedServicesReaderFactory.class.getCanonicalName());
  
  /** Create a PlanitRoutedServicesReader
   * 
   * @param parentNetwork the parent network the routed services are to be built upon
   * @return created routed service reader
   */
  public static PlanitRoutedServicesReader create(ServiceNetwork parentNetwork) {
    return create(IdGroupingToken.collectGlobalToken(), parentNetwork);
  }   
  
  /** Create a PlanitRoutedServicesReader based on custom id token
   * 
   * @param idToken to use for routed services id generation
   * @param parentNetwork the network the routed services are assumed to be built upon  
   * @return created routed service reader
   */
  public static PlanitRoutedServicesReader create(final IdGroupingToken idToken, ServiceNetwork parentNetwork) {
    return create(new PlanitRoutedServicesReaderSettings(parentNetwork), new RoutedServices(idToken, parentNetwork));    
  }
  
  /** Create a PlanitRoutedServicesReader sourced from given input directory
   * 
   * @param inputDirectory to use (directory only, find first compatible file)
   * @param parentNetwork the network the routed services are assumed to be built upon  
   * @return created routed service reader
   */
  public static PlanitRoutedServicesReader create(final String inputDirectory, ServiceNetwork parentNetwork) {
    PlanitRoutedServicesReader serviceNetworkReader = create(parentNetwork);
    serviceNetworkReader.getSettings().setInputDirectory(inputDirectory);
    return serviceNetworkReader;
  }  
  
  /** Create a PlanitRoutedServicesReader based on given settings which in turn contain information on location and parent network to use
   * 
   * @param settings to use
   * @return created routed service reader
   */
  public static PlanitRoutedServicesReader create(final PlanitRoutedServicesReaderSettings settings) {
    return create(settings, new RoutedServices(IdGroupingToken.collectGlobalToken(), settings.getParentNetwork()));
  }   
  
  /** Create a PlanitRoutedServicesReader for given (empty) routed services and given settings
   * 
   * @param settings to use
   * @param routedServices to use
   * @return created routed service reader
   */
  public static PlanitRoutedServicesReader create(final PlanitRoutedServicesReaderSettings settings, final RoutedServices routedServices) {
    try {
      return new PlanitRoutedServicesReader(settings, routedServices);
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
    }    
    return null;
  }  
   
  
  /** Create a PlanitRoutedServicesReader for location and routed services to populate
   * 
   * @param inputDirectory to use (directory only, find first compatible file)
   * @param xmlFileExtension to consider
   * @param routedServices to populate
   * @return created routed service reader
   */
  public static PlanitRoutedServicesReader create(final String inputDirectory, final String xmlFileExtension, final RoutedServices routedServices) {
    try {
      return new PlanitRoutedServicesReader(inputDirectory, xmlFileExtension, routedServices);
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
    }    
    return null;
  }  
    
  
  /** Create a PlanitRoutedServicesReader for given XML root element and routed services to populate
   * 
   * @param xmlRawRoutedServices the raw routed services based on the JAXB parser
   * @param routedServices to populate
   * @return created routed service reader
   */
  public static PlanitRoutedServicesReader create(final XMLElementRoutedServices xmlRawRoutedServices, final RoutedServices routedServices) {
    try {
      return new PlanitRoutedServicesReader(xmlRawRoutedServices, routedServices);
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
    }  
    return null;    
  }    
     
}
