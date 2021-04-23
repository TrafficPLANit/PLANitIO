package org.planit.io.converter.network;

import java.util.HashMap;
import java.util.Map;

import org.planit.converter.ConverterReaderSettings;
import org.planit.io.xml.util.PlanitXmlReaderSettings;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;

/**
 * configurable settings for the PLANit network reader
 * 
 * @author markr
 *
 */
public class PlanitNetworkReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {
    
  /**
   * option to provide external map to populate with xml node Ids corresponding to parsed Nodes
   */
  private Map<String, Node> nodeXmlIdToNodeMap = new HashMap<String, Node>();;

  /**
   * option to provide external map to populate with link segments by xml Id
   */
  private Map<String, MacroscopicLinkSegment> linkSegmentXmlIdToLinkSegmentMap = new HashMap<String, MacroscopicLinkSegment>();

  /**
   * option to provide external map to populate with xml link segment type Ids corresponding to link segment types
   */
  private Map<String, MacroscopicLinkSegmentType> linkSegmentTypeXmlIdToLinkSegmentTypeMap = new HashMap<String, MacroscopicLinkSegmentType>();

  /**
   * option to provide external map to populate with xml Ids corresponding to Modes
   */
  private Map<String, Mode> modeXmlIdToModeMap = new HashMap<String, Mode>();
  
  /**
   * Default constructor. Expected that the user provides the input path at some point later
   */
  public PlanitNetworkReaderSettings() {
    super();
  }
  
  /**
   * Constructor.
   * 
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   */
  public PlanitNetworkReaderSettings(final String inputPathDirectory, final String xmlFileExtension) {
    super(inputPathDirectory, xmlFileExtension);
  }  
  
  /** Use provided map to index modes by xml id when parsing
   * @param modeXmlIdToModeMap to use
   */
  public void setMapToIndexModeByXmlIds(Map<String, Mode> modeXmlIdToModeMap) {
    this.modeXmlIdToModeMap = modeXmlIdToModeMap;
  }
  
  /** exogenous map to index modes by xml id when parsing
   * @return modeXmlIdToModeMap to use
   */
  public Map<String, Mode> getMapToIndexModeByXmlIds() {
    return this.modeXmlIdToModeMap;
  }  
  
  /** Use provided map to index nodes by xml id when parsing
   * @param nodeXmlIdToNodeMap to use
   */
  public void setMapToIndexNodeByXmlIds(Map<String, Node> nodeXmlIdToNodeMap) {
    this.nodeXmlIdToNodeMap = nodeXmlIdToNodeMap;
  } 
  
  /** exogenous map to index nodes by xml id when parsing (if any, can be null)
   * @return nodeXmlIdToNodeMap to use
   */
  public Map<String, Node> getMapToIndexNodeByXmlIds() {
    return this.nodeXmlIdToNodeMap;
  }    
  
  /** Use provided map to index link segments by xml id when parsing
   * @param linkSegmentXmlIdToLinkSegmentMap to use
   */
  public void setMapToIndexLinkSegmentByXmlIds(Map<String, MacroscopicLinkSegment> linkSegmentXmlIdToLinkSegmentMap) {
    this.linkSegmentXmlIdToLinkSegmentMap = linkSegmentXmlIdToLinkSegmentMap;
  }   
  
  /** exogenous map to index link segments by xml id when parsing (if any, can be null)
   * @return linkSegmentXmlIdToLinkSegmentMap to use
   */
  public Map<String, MacroscopicLinkSegment> getMapToIndexLinkSegmentByXmlIds() {
    return this.linkSegmentXmlIdToLinkSegmentMap;
  }   
  
  /** Use provided map to index link segment types by xml id when parsing
   * @param linkSegmentTypeXmlIdToLinkSegmentTypeMap to use
   */
  public void setMapToIndexLinkSegmentTypeByXmlIds(Map<String, MacroscopicLinkSegmentType> linkSegmentTypeXmlIdToLinkSegmentTypeMap) {
    this.linkSegmentTypeXmlIdToLinkSegmentTypeMap = linkSegmentTypeXmlIdToLinkSegmentTypeMap;
  }  
  
  /** exogenous map to index link segment types by xml id when parsing (if any, can be null)
   * @return linkSegmentTypeXmlIdToLinkSegmentTypeMap to use
   */
  public Map<String, MacroscopicLinkSegmentType> getMapToIndexLinkSegmentTypeByXmlIds() {
    return this.linkSegmentTypeXmlIdToLinkSegmentTypeMap;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // TODO    
  } 
    
  
}
