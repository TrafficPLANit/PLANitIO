package org.planit.io.network.converter;

import java.util.logging.Logger;

import org.planit.network.InfrastructureNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;


/**
 * Factory for creating PLANitNetworkReaders
 * 
 * @author markr
 *
 */
public class PlanitNetworkReaderFactory {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkReaderFactory.class.getCanonicalName());
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param networkPath to use
   * @param xmlFileExtension to consider
   * @param network to populate
   * @return created PLANit reader
   */
  public static PlanitNetworkReader createReader(String networkPath, String xmlFileExtension, InfrastructureNetwork network) {
    try {
      return new PlanitNetworkReader(networkPath, xmlFileExtension, network);
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
    }    
    return null;
  }  
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param xmlRawNetwork the raw network based on the JAXB parser
   * @param network to populate
   * @return created PLANit reader
   */
  public static PlanitNetworkReader createReader(XMLElementMacroscopicNetwork xmlRawNetwork, InfrastructureNetwork network) {
    try {
      return new PlanitNetworkReader(xmlRawNetwork, network);
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
    }  
    return null;    
  }    
     
}
