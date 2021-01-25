package org.planit.io.zoning;

import java.util.HashMap;
import java.util.Map;

import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.Zone;

/**
 * Settings for the PLANit zoning reader
 * 
 * @author markr
 *
 */
public class PlanitZoningReaderSettings {

  /**
   * Map to stores zones by xml Id
   */
  protected Map<String, Zone> xmlIdZoneMap = new HashMap<String, Zone>();
  
  /**
   * Map to stores connectoids by xml Id
   */
  protected Map<String, Connectoid> xmlIdConnectoidMap = new HashMap<String, Connectoid>();  
  
  /** map to index nodes by xml id when parsing
   * @param zoneXmlIdToNodeMap to use
   */
  protected Map<String, Zone> getMapToIndexZoneByXmlIds() {
    return this.xmlIdZoneMap;
  }     
  
  /** Use provided map to index zones by xml id when parsing
   * @param zoneXmlIdToNodeMap to use
   */
  public void setMapToIndexZoneByXmlIds(Map<String, Zone> xmlIdZoneMap) {
    this.xmlIdZoneMap = xmlIdZoneMap;
  } 
  
  /** map to index nodes by xml id when parsing
   * @param zoneXmlIdToNodeMap to use
   */
  protected Map<String, Connectoid> getMapToIndexConnectoidsByXmlIds() {
    return this.xmlIdConnectoidMap;
  }     
  
  /** Use provided map to index zones by xml id when parsing
   * @param zoneXmlIdToNodeMap to use
   */
  public void setMapToIndexConnectoidsByXmlIds(Map<String, Connectoid> xmlIdConnectoidMap) {
    this.xmlIdConnectoidMap = xmlIdConnectoidMap;
  }   
   
}
