package org.planit.io.demands;

import java.util.HashMap;
import java.util.Map;

import org.planit.utils.mode.Mode;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.zoning.Zone;
import org.planit.zoning.Zoning;
import org.planit.converter.ConverterReaderSettings;
import org.planit.io.xml.util.PlanitXmlReaderSettings;
import org.planit.network.macroscopic.MacroscopicNetwork;
import org.planit.userclass.TravelerType;
import org.planit.userclass.UserClass;

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
   * Map to store travel types by XML Id
   */
  protected Map<String, TravelerType> xmlIdTravelerTypeMap = new HashMap<String, TravelerType>();
  
  /**
   * Map to store user class by XML Id
   */
  protected Map<String, UserClass> xmlIdUserClassMap = new HashMap<String, UserClass>();;  
  
  /**
   * Map which stores time periods by XML Id
   */
  protected Map<String, TimePeriod> xmlIdTimePeriodMap = new HashMap<String, TimePeriod>();  
  
  /**
   * Map which stores modes by XML Id
   */  
  protected Map<String, Mode> xmlIdModeMap;
  
  /**
   * Map which stores zones by XML Id
   */  
   protected Map<String, Zone> xmlIdZoneMap;  
  
  
  /** Map to index traveler types by XML id when parsing
   * 
   * @return xmlIdTravelerTypeMap 
   */
  protected Map<String, TravelerType> getMapToIndexTravelerTypeByXmlIds() {
    return this.xmlIdTravelerTypeMap;
  }     
  
  /** Map to index user classes by XML id when parsing
   * 
   * @return xmlIdUserClassMap
   */
  protected Map<String, UserClass> getMapToIndexUserClassByXmlIds() {
    return this.xmlIdUserClassMap;
  }    
  
  /** Map to index time periods  by XML id when parsing
   * 
   * @return xmlIdTimePeriodMap
   */
  protected Map<String, TimePeriod> getMapToIndexTimePeriodByXmlIds() {
    return this.xmlIdTimePeriodMap;
  } 
  
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
  
  /** Use provided map to index traveler types by XML id when parsing
   * 
   * @param xmlIdTravelerTypeMap to use
   */
  public void setMapToIndexTravelerTypeByXmlIds(final Map<String, TravelerType> xmlIdTravelerTypeMap) {
    this.xmlIdTravelerTypeMap = xmlIdTravelerTypeMap;
  }     
  
  /** Use provided map to index user classes by XML id when parsing
   * 
   * @param xmlIdUserClassMap to use
   */
  public void setMapToIndexUserClassByXmlIds(final Map<String, UserClass> xmlIdUserClassMap) {
    this.xmlIdUserClassMap = xmlIdUserClassMap;
  }  
       
  /** Use provided map to index time periods by XML id when parsing
   * 
   * @param xmlIdTimePeriodMap to use
   */
  public void setMapToIndexTimePeriodByXmlIds(final Map<String, TimePeriod> xmlIdTimePeriodMap) {
    this.xmlIdTimePeriodMap = xmlIdTimePeriodMap;
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
