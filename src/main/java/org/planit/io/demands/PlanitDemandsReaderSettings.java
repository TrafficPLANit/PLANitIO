package org.planit.io.demands;

import java.util.HashMap;
import java.util.Map;

import org.planit.time.TimePeriod;
import org.planit.userclass.TravelerType;
import org.planit.userclass.UserClass;

/**
 * Settings for the PLANit zoning reader
 * 
 * @author markr
 *
 */
public class PlanitDemandsReaderSettings {

  /**
   * Map to store travel types by xml Id
   */
  protected Map<String, TravelerType> xmlIdTravelerTypeMap = new HashMap<String, TravelerType>();
  
  /**
   * Map to store user class by xml Id
   */
  protected Map<String, UserClass> xmlIdUserClassMap = new HashMap<String, UserClass>();;  
  
  /**
   * Map which stores time periods by xml Id
   */
  protected Map<String, TimePeriod> xmlIdTimePeriodMap = new HashMap<String, TimePeriod>();;  
  
  
  /** map to index traveler types by xml id when parsing
   * @return xmlIdTravelerTypeMap 
   */
  protected Map<String, TravelerType> getMapToIndexTravelerTypeByXmlIds() {
    return this.xmlIdTravelerTypeMap;
  }     
  
  /** Use provided map to index traveler types by xml id when parsing
   * @param xmlIdTravelerTypeMap to use
   */
  public void setMapToIndexTravelerTypeByXmlIds(Map<String, TravelerType> xmlIdTravelerTypeMap) {
    this.xmlIdTravelerTypeMap = xmlIdTravelerTypeMap;
  }
  
  /** map to index user classes by xml id when parsing
   * @return xmlIdUserClassMap
   */
  protected Map<String, UserClass> getMapToIndexUserClassByXmlIds() {
    return this.xmlIdUserClassMap;
  }     
  
  /** Use provided map to index user classesby xml id when parsing
   * @param xmlIdUserClassMap to use
   */
  public void setMapToIndexUserClassByXmlIds(Map<String, UserClass> xmlIdUserClassMap) {
    this.xmlIdUserClassMap = xmlIdUserClassMap;
  }  
  
  /** map to index time periods  by xml id when parsing
   * @return xmlIdTimePeriodMap
   */
  protected Map<String, TimePeriod> getMapToIndexTimePeriodByXmlIds() {
    return this.xmlIdTimePeriodMap;
  }     
  
  /** Use provided map to index time periods by xml id when parsing
   * @param xmlIdTimePeriodMap to use
   */
  public void setMapToIndexTimePeriodByXmlIds(Map<String, TimePeriod> xmlIdTimePeriodMap) {
    this.xmlIdTimePeriodMap = xmlIdTimePeriodMap;
  }  
   
}
