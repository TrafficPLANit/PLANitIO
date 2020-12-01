package org.planit.io.network.converter;

import org.planit.xml.generated.XMLElementMacroscopicNetwork;

/**
 * Factory for creating PLANitNetworkReaders
 * 
 * @author markr
 *
 */
public class PlanitNetworkWriterFactory {
  
  /** Create a PLANitNetworkWriter which can persist a PLANit network in the native PLANit XML format
   * 
   * @param networkPath the file to use for persisting
   * @param countryName the country to base the projection method on if available
   * @return created network writer 
   */
  public static PlanitNetworkWriter createWriter(String networkPath, String countryName) {
    return new PlanitNetworkWriter(networkPath, countryName, new XMLElementMacroscopicNetwork());    
  }
  
  /** Create a PLANitNetworkWriter which can persist a PLANit network in the native PLANit XML format. By providing the XML memory model instance to populate
   * we make it possible for the writer to embed the persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   * 
   * @param networkPath the file to use for persisting
   * @param countryName the country to base the projection method on if available
   * @param xmlRawNetwork, use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @return created network writer 
   */
  public static PlanitNetworkWriter createWriter(String networkPath, XMLElementMacroscopicNetwork xmlRawNetwork, String countryName) {
    return new PlanitNetworkWriter(networkPath, countryName, xmlRawNetwork);    
  }    
     
     
}
