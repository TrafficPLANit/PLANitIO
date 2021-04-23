package org.planit.io.converter.zoning;

import java.util.HashMap;
import java.util.Map;

import org.planit.converter.ConverterReaderSettings;
import org.planit.io.xml.util.PlanitXmlReaderSettings;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.Zone;

/**
 * Settings for the PLANit zoning reader
 * 
 * @author markr
 *
 */
public class PlanitZoningReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {
  
  /**
   * Map to stores zones by xml Id
   */
  protected Map<String, Zone> xmlIdZoneMap = new HashMap<String, Zone>();
  
  /**
   * Map to stores connectoids by xml Id
   */
  protected Map<String, Connectoid> xmlIdConnectoidMap = new HashMap<String, Connectoid>();  
  
  /** map to index nodes by xml id when parsing
   * @return zoneXmlIdToNodeMap to use
   */
  protected Map<String, Zone> getMapToIndexZoneByXmlIds() {
    return this.xmlIdZoneMap;
  }     
  
  /**
   * Default constructor
   */
  public PlanitZoningReaderSettings() {
    super();
  }
  
  /**
   * Constructor
   * 
   *  @param inputPathDirectory to use
   *  @param xmlFileExtension to use
   */
  public PlanitZoningReaderSettings(final String inputPathDirectory, final String xmlFileExtension) {
    super(inputPathDirectory, xmlFileExtension);
  }  
  
  /** Use provided map to index zones by xml id when parsing
   * @param xmlIdZoneMap to use
   */
  public void setMapToIndexZoneByXmlIds(Map<String, Zone> xmlIdZoneMap) {
    this.xmlIdZoneMap = xmlIdZoneMap;
  } 
  
  /** map to index connectoids by xml id when parsing
   * @return zoneXmlIdToNodeMap to use
   */
  protected Map<String, Connectoid> getMapToIndexConnectoidsByXmlIds() {
    return this.xmlIdConnectoidMap;
  }     
  
  /** Use provided map to index connectoids by xml id when parsing
   * @param xmlIdConnectoidMap to use
   */
  public void setMapToIndexConnectoidsByXmlIds(Map<String, Connectoid> xmlIdConnectoidMap) {
    this.xmlIdConnectoidMap = xmlIdConnectoidMap;
  }   
  
  /**
   * reset 
   */
  public void reset() {
    xmlIdZoneMap.clear();
    xmlIdConnectoidMap.clear();
  }
   
}
