package org.goplanit.io.converter.network;

import org.goplanit.xml.generated.XMLElementMacroscopicNetwork;

/**
 * Factory for creating PLANit Network writers
 * 
 * @author markr
 *
 */
public class PlanitNetworkWriterFactory {
  
  /** Create a PLANitNetworkWriter which can persist a PLANit network in the native PLANit XML format witha ll defaults. It is expected the user sets the required
   * minimum configuration afterwards to be able to persist
   * 
   * @return created PLANit network writer
   */
  public static PlanitNetworkWriter create() {
    return new PlanitNetworkWriter(new XMLElementMacroscopicNetwork());    
  }  
  
  /** Create a PLANitNetworkWriter which can persist a PLANit network in the native PLANit XML format
   * 
   * @param networkPath the path to use for persisting
   * @return created PLANit network writer 
   */
  public static PlanitNetworkWriter create(String networkPath) {
    return create(networkPath, null);    
  }  
  
  /** Create a PLANitNetworkWriter which can persist a PLANit network in the native PLANit XML format
   * 
   * @param networkPath the path to use for persisting
   * @param countryName the country to base the projection method on if available
   * @return created PLANit network writer 
   */
  public static PlanitNetworkWriter create(String networkPath, String countryName) {
    return create(networkPath, countryName, new XMLElementMacroscopicNetwork());    
  }
  
  /** Create a PLANitNetworkWriter which can persist a PLANit network in the native PLANit XML format. By providing the XML memory model instance to populate
   * we make it possible for the writer to embed the persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   * 
   * @param networkPath the file to use for persisting
   * @param countryName the country to base the projection method on if available
   * @param xmlRawNetwork, use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @return created PLANit network writer 
   */
  public static PlanitNetworkWriter create(String networkPath, String countryName, XMLElementMacroscopicNetwork xmlRawNetwork) {
    return new PlanitNetworkWriter(networkPath, countryName, xmlRawNetwork);    
  }    
     
     
}
