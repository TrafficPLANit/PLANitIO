package org.planit.io.intermodal;

import java.util.Map;
import java.util.logging.Logger;

import org.planit.io.xml.util.PlanitXmlReader;
import org.planit.network.InfrastructureNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.network.physical.Node;
import org.planit.xml.generated.XMLElementMacroscopicIntermodal;
import org.planit.zoning.Zoning;


/**
 * Class to parse intermodal components from native XML format. Since the transfers occur at transfer zones
 * which are closely related to regular (OD) zones, the intermodal memory model is closely related to the {@link Zoning}
 * class
 * 
 * @author markr
 *
 */

public class PlanitInterModalReader extends PlanitXmlReader<XMLElementMacroscopicIntermodal>{
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitInterModalReader.class.getCanonicalName());
  
  /** settings for the zoning reader */
  protected final PlanitInterModalReaderSettings settings = new PlanitInterModalReaderSettings();
  
  /** the zoning to populate */
  protected Zoning zoning;
  
  /** set the oning to populate
   * @param zoning to populate
   */
  protected void setZoning(Zoning zoning) {
    this.zoning = zoning;
  }
  
   
  /** constructor
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param zoning to populate
   * @throws PlanItException  thrown if error
   */
  public PlanitInterModalReader(String pathDirectory, String xmlFileExtension, Zoning zoning) throws PlanItException{   
    super(XMLElementMacroscopicIntermodal.class,pathDirectory, xmlFileExtension);    
    setZoning(zoning);
  }
  
  /** constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlElementMacroscopicIntermodal to extract from
   * @param zoning to populate
   * @throws PlanItException  thrown if error
   */
  public PlanitInterModalReader(XMLElementMacroscopicIntermodal xmlElementMacroscopicIntermodal, Zoning zoning) throws PlanItException{
    super(xmlElementMacroscopicIntermodal);    
    setZoning(zoning);
  }  

  /** read the zoning from disk
   * 
   * @param network this zoning is compatible with
   * @param nodesByXmlIds to identify mapping between zones and network
   * @return zoning parsed
   * @throws PlanItException thrown if error
   */
  public Zoning read(InfrastructureNetwork<?> network, Map<String, Node> nodesByXmlIds) throws PlanItException {  
    // create and register zones, centroids and connectoids
    try {
      
      /* popoulate Xml memory model */
      initialiseAndParseXmlRootElement();
      
      
      /* free */
      clearXmlContent();
      
    } catch (PlanItException e) {
      throw e;
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when populating zoning in PLANitIO",e);
    }
    
    return zoning;
  }
  

  /** settings for this reader
   * @return settings
   */
  public PlanitInterModalReaderSettings getSettings() {
    return settings;
  }
}
