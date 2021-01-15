package org.planit.io.network.converter;

import org.planit.network.macroscopic.physical.MacroscopicNetwork;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;

/**
 * Factory for creating PLANitNetworkReaders
 * 
 * @author markr
 *
 */
public class PlanitNetworkReaderFactory {
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param networkPath to use
   * @param xmlFileExtension to consider
   * @param network to populate
   * @return created PLANit reader
   */
  public static PlanitNetworkReader createReader(String networkPath, String xmlFileExtension, MacroscopicNetwork network) {
    return new PlanitNetworkReader(networkPath, xmlFileExtension, network);    
  }  
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param xmlRawNetwork the raw network based on the JAXB parser
   * @param network to populate
   * @return created PLANit reader
   */
  public static PlanitNetworkReader createReader(XMLElementMacroscopicNetwork xmlRawNetwork, MacroscopicNetwork network) {
    return new PlanitNetworkReader(xmlRawNetwork, network);    
  }    
     
}
