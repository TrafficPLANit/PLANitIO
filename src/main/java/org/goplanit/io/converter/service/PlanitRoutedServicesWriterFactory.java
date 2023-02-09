package org.goplanit.io.converter.service;

import org.goplanit.xml.generated.XMLElementRoutedServices;

/**
 * Factory for creating PLANit Routed Services writers
 * 
 * @author markr
 *
 */
public class PlanitRoutedServicesWriterFactory {
  
  /** Create a PLANitRoutedServicesWriter which can persist a PLANit RoutedServices in the native PLANit XML format with all defaults. It is expected the user sets the required
   * minimum configuration afterwards to be able to persist
   * 
   * @return created PLANit RoutedServicesNetwork writer
   */
  public static PlanitRoutedServicesWriter create() {
    return new PlanitRoutedServicesWriter(new XMLElementRoutedServices());
  }  
  
  /** Create a PLANitRoutedServicesWriter which can persist a PLANit RoutedServices in the native PLANit XML format
   * 
   * @param serviceNetworkPath the path to use for persisting
   * @return created PLANit serviceNetwork writer
   */
  public static PlanitRoutedServicesWriter create(String serviceNetworkPath) {
    return create(serviceNetworkPath, null);
  }  
  
  /** Create a PLANitRoutedServicesWriter which can persist a PLANit RoutedServices in the native PLANit XML format
   * 
   * @param serviceNetworkPath the path to use for persisting
   * @param countryName the country to base the projection method on if available
   * @return created PLANit serviceNetwork writer
   */
  public static PlanitRoutedServicesWriter create(String serviceNetworkPath, String countryName) {
    return create(serviceNetworkPath, countryName, new XMLElementRoutedServices());
  }
  
  /** Create a PLANitRoutedServicesWriter which can persist a PLANit RoutedServices in the native PLANit XML format. By providing the XML memory model instance to populate
   * we make it possible for the writer to embed the persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   * 
   * @param networkPath the file to use for persisting
   * @param countryName the country to base the projection method on if available
   * @param xmlRawRoutedServices, use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @return created PLANit RoutedServices writer
   */
  public static PlanitRoutedServicesWriter create(String networkPath, String countryName, XMLElementRoutedServices xmlRawRoutedServices) {
    return new PlanitRoutedServicesWriter(networkPath, countryName, xmlRawRoutedServices);
  }    
     
     
}
