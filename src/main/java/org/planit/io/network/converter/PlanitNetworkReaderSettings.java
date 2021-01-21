package org.planit.io.network.converter;

import java.util.HashMap;
import java.util.Map;

import org.planit.input.InputBuilderListener;
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
public class PlanitNetworkReaderSettings {
  
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
   * Flag to determine whether duplicate xml Id should be considered an error (defaults to {@link InputBuilderListener.DEFAULT_ERROR_ON_DUPLICATE_EXTERNAL_ID}
   */
  private boolean errorIfDuplicateXmlId = InputBuilderListener.DEFAULT_ERROR_ON_DUPLICATE_SOURCE_ID;  
  
  /** Use provided map to index modes by xml id when parsing
   * @param modeXmlIdToModeMap to use
   */
  public void setMapToIndexModeByXmlIds(Map<String, Mode> modeXmlIdToModeMap) {
    this.modeXmlIdToModeMap = modeXmlIdToModeMap;
  }
  
  /** exogenous map to index modes by xml id when parsing
   * @param modeXmlIdToModeMap to use
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
   * @param nodeXmlIdToNodeMap to use
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
   * @param linkSegmentXmlIdToLinkSegmentMap to use
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
   * @param linkSegmentTypeXmlIdToLinkSegmentTypeMap to use
   */
  public Map<String, MacroscopicLinkSegmentType> getMapToIndexLinkSegmentTypeByXmlIds() {
    return this.linkSegmentTypeXmlIdToLinkSegmentTypeMap;
  }

  /** check value of this flag
   * @return true when raising error on duplicate xml id
   */
  public boolean isErrorIfDuplicateXmlId() {
    return errorIfDuplicateXmlId;
  }

  /**
   * update flag on whether or not to raise an error on duplicate xml ids
   * 
   * @param errorIfDuplicateXmlId
   */
  public void setErrorIfDuplicateXmlId(boolean errorIfDuplicateXmlId) {
    this.errorIfDuplicateXmlId = errorIfDuplicateXmlId;
  }   
  
  
}
