package org.goplanit.io.converter.network;

import java.util.logging.Logger;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.TransportLayerNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.xml.generated.XMLElementMacroscopicNetwork;


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
   * @return created PLANit network reader
   */
  public static PlanitNetworkReader create() {
    return create(IdGroupingToken.collectGlobalToken());
  }   
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param idToken to use for network
   * @return created PLANit network reader
   */
  public static PlanitNetworkReader create(final IdGroupingToken idToken) {
    return create(new PlanitNetworkReaderSettings(), new MacroscopicNetwork(idToken));    
  }
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific 
   *  defaults for any right hand driving country
   * 
   * @param inputDirectory to use (directory only, find first compatible file)
   * @return created PLANit network reader
   */
  public static PlanitNetworkReader create(final String inputDirectory) {
    PlanitNetworkReader networkReader = create();
    networkReader.getSettings().setInputDirectory(inputDirectory);
    return networkReader;
  }  
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param settings to use
   * @return created PLANit network reader
   */
  public static PlanitNetworkReader create(final PlanitNetworkReaderSettings settings) {
    return create(settings, new MacroscopicNetwork(IdGroupingToken.collectGlobalToken()));
  }   
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param settings to use
   * @param network to use
   * @return created PLANit network reader
   */
  public static PlanitNetworkReader create(final PlanitNetworkReaderSettings settings, final TransportLayerNetwork<?,?> network) {
    try {
      return new PlanitNetworkReader(settings, network);
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
    }    
    return null;
  }  
   
  
  /** Create a PLANitNetworkReader which will create its own macroscopic network and non-locale specific defaults for any right hand driving country
   * 
   * @param inputDirectory to use (directory only, find first compatible file)
   * @param xmlFileExtension to consider
   * @param network to populate
   * @return created PLANit reader
   */
  public static PlanitNetworkReader create(final String inputDirectory, final String xmlFileExtension, final TransportLayerNetwork<?,?> network) {
    try {
      return new PlanitNetworkReader(inputDirectory, xmlFileExtension, network);
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
  public static PlanitNetworkReader create(final XMLElementMacroscopicNetwork xmlRawNetwork, final TransportLayerNetwork<?,?> network) {
    try {
      return new PlanitNetworkReader(xmlRawNetwork, network);
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
    }  
    return null;    
  }    
     
}
