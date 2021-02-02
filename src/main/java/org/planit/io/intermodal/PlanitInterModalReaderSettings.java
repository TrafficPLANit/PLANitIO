package org.planit.io.intermodal;

import java.util.HashMap;
import java.util.Map;

import org.planit.utils.zoning.Zone;

/**
 * Settings for the PLANit zoning reader
 * 
 * @author markr
 *
 */
public class PlanitInterModalReaderSettings {

  /**
   * Map to stores zones by xml Id
   */
  protected Map<String, Zone> xmlIdZoneMap = new HashMap<String, Zone>();
  
  /** map to index nodes by xml id when parsing
   * @return zoneXmlIdToNodeMap to use
   */
  protected Map<String, Zone> getMapToIndexZoneByXmlIds() {
    return this.xmlIdZoneMap;
  }     
  
  /** Use provided map to index zones by xml id when parsing
   * @param xmlIdZoneMap to use
   */
  public void setMapToIndexZoneByXmlIds(Map<String, Zone> xmlIdZoneMap) {
    this.xmlIdZoneMap = xmlIdZoneMap;
  } 
   
}
