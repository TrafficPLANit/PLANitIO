package org.goplanit.io.converter.service;

import org.goplanit.io.converter.network.PlanitNetworkWriter;
import org.goplanit.xml.generated.XMLElementMacroscopicNetwork;
import org.goplanit.xml.generated.XMLElementServiceNetwork;

/**
 * Factory for creating PLANit Service Network writers
 * 
 * @author markr
 *
 */
public class PlanitServiceNetworkWriterFactory {
  
  /** Create a PLANitServiceNetworkWriter which can persist a PLANit Service network in the native PLANit XML format with all defaults. It is expected the user sets the required
   * minimum configuration afterwards to be able to persist
   * 
   * @return created PLANit serviceNetwork writer
   */
  public static PlanitServiceNetworkWriter create() {
    return new PlanitServiceNetworkWriter(new XMLElementServiceNetwork());
  }  
  
  /** Create a PLANitServiceNetworkWriter which can persist a PLANit Service network in the native PLANit XML format
   * 
   * @param serviceNetworkPath the path to use for persisting
   * @return created PLANit serviceNetwork writer
   */
  public static PlanitServiceNetworkWriter create(String serviceNetworkPath) {
    return create(serviceNetworkPath, null);
  }  
  
  /** Create a PLANitServiceNetworkWriter which can persist a PLANit Service network in the native PLANit XML format
   * 
   * @param serviceNetworkPath the path to use for persisting
   * @param countryName the country to base the projection method on if available
   * @return created PLANit serviceNetwork writer
   */
  public static PlanitServiceNetworkWriter create(String serviceNetworkPath, String countryName) {
    return create(serviceNetworkPath, countryName, new XMLElementServiceNetwork());
  }
  
  /** Create a PLANitNetworkWriter which can persist a PLANit network in the native PLANit XML format. By providing the XML memory model instance to populate
   * we make it possible for the writer to embed the persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   * 
   * @param networkPath the file to use for persisting
   * @param countryName the country to base the projection method on if available
   * @param xmlRawServiceNetwork, use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @return created PLANit serviceNetwork writer
   */
  public static PlanitServiceNetworkWriter create(String networkPath, String countryName, XMLElementServiceNetwork xmlRawServiceNetwork) {
    return new PlanitServiceNetworkWriter(networkPath, countryName, xmlRawServiceNetwork);
  }    
     
     
}
