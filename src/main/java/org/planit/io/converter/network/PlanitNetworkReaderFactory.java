package org.planit.io.converter.network;

import java.util.logging.Logger;

import org.planit.network.InfrastructureNetwork;
import org.planit.network.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
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
   */
  public static PlanitNetworkReader create() {
    return create(IdGroupingToken.collectGlobalToken());
  }   
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param idToken to use for network
   */
  public static PlanitNetworkReader create(final IdGroupingToken idToken) {
    return create(new PlanitNetworkReaderSettings(), new MacroscopicNetwork(idToken));    
  }
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param settings to use
   */
  public static PlanitNetworkReader create(final PlanitNetworkReaderSettings settings) {
    return create(settings, new MacroscopicNetwork(IdGroupingToken.collectGlobalToken()));
  }   
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param settings to use
   * @param network to use
   */
  public static PlanitNetworkReader create(final PlanitNetworkReaderSettings settings, final InfrastructureNetwork<?,?> network) {
    try {
      return new PlanitNetworkReader(settings, network);
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
    }    
    return null;
  }  
   
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param inputPath to use (directory only, find first compatible file)
   * @param xmlFileExtension to consider
   * @param network to populate
   * @return created PLANit reader
   */
  public static PlanitNetworkReader create(final String inputPath, final String xmlFileExtension, final InfrastructureNetwork<?,?> network) {
    try {
      return new PlanitNetworkReader(inputPath, xmlFileExtension, network);
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
  public static PlanitNetworkReader create(final XMLElementMacroscopicNetwork xmlRawNetwork, final InfrastructureNetwork<?,?> network) {
    try {
      return new PlanitNetworkReader(xmlRawNetwork, network);
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
    }  
    return null;    
  }    
     
}
