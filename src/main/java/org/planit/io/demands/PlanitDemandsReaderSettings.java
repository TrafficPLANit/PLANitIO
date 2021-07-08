package org.planit.io.demands;

import java.util.Map;

import org.planit.utils.mode.Mode;
import org.planit.utils.zoning.Zone;
import org.planit.zoning.Zoning;
import org.planit.converter.ConverterReaderSettings;
import org.planit.io.xml.util.PlanitXmlReaderSettings;
import org.planit.network.MacroscopicNetwork;

/**
 * Settings for the PLANit zoning reader
 * 
 * @author markr
 *
 */
public class PlanitDemandsReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {

  /**
   * Reference network to use when demand relate to network entities
   */
  protected MacroscopicNetwork referenceNetwork;
  
  /**
   * Reference zoning to use when demands relate to zoning entities
   */  
  protected Zoning referenceZoning;
    
  /**
   * Map which stores references to modes by XML Id
   */  
  protected Map<String, Mode> xmlIdModeMap;
  
  /**
   * Map which stores references to zones by XML Id
   */  
   protected Map<String, Zone> xmlIdZoneMap;  
      
  /** Map to zones by XML id when parsing
   * 
   * @return xmlIdZoneMap
   */
  protected Map<String, Zone> getMapToIndexZoneByXmlIds() {
    return this.xmlIdZoneMap;
  }  
  
  /** Map to modes by XML id when parsing
   * 
   * @return xmlIdModeMap
   */
  protected Map<String, Mode> getMapToIndexModeByXmlIds() {
    return this.xmlIdModeMap;
  }    
  
  /** Collect reference network used
   * 
   * @return reference network
   */
  protected MacroscopicNetwork getReferenceNetwork() {
    return referenceNetwork;
  }
  
  /** Collect reference zoning used
   * 
   * @return reference zoning
   */  
  protected Zoning getReferenceZoning() {
    return referenceZoning;
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // TODO     
  }    
    
  /** Use provided map to index zones by XML id when parsing
   * 
   * @param xmlIdZoneMap to use
   */
  public void setMapToIndexZoneByXmlIds(final Map<String, Zone> xmlIdZoneMap) {
    this.xmlIdZoneMap = xmlIdZoneMap;
  }
  
  /** Use provided map to index modes by xml id when parsing
   * 
   * @param xmlIdModeMap to use
   */
  public void setMapToIndexModeByXmlIds(final Map<String, Mode> xmlIdModeMap) {
    this.xmlIdModeMap = xmlIdModeMap;
  }  
   
  /** Set reference network to use
   * 
   * @param referenceNetwork to use
   */
  public void setReferenceNetwork(final MacroscopicNetwork referenceNetwork) {
    this.referenceNetwork = referenceNetwork;
  }

  /** Set reference zoning to use
   * 
   * @param referenceZoning to use
   */
  public void setReferenceZoning(final Zoning referenceZoning) {
    this.referenceZoning = referenceZoning;
  }
  


   
}
