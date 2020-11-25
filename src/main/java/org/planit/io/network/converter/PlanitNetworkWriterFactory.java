package org.planit.io.network.converter;

import org.planit.network.physical.macroscopic.MacroscopicNetwork;
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
   * @return created network writer 
   */
  public static PlanitNetworkWriter createWriter(String networkPath) {
    return new PlanitNetworkWriter(networkPath, new XMLElementMacroscopicNetwork());    
  }
  
  /** Create a PLANitNetworkWriter which can persist a PLANit network in the native PLANit XML format. By providing the XML memory model instance to populate
   * we make it possible for the writer to embed the persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   * 
   * @param networkPath the file to use for persisting
   * @param xmlRawNetwork, use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @return created network writer 
   */
  public static PlanitNetworkWriter createWriter(String networkPath, XMLElementMacroscopicNetwork xmlRawNetwork) {
    return new PlanitNetworkWriter(networkPath, xmlRawNetwork);    
  }    
     
     
}
