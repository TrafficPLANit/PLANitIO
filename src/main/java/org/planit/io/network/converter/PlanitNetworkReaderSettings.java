package org.planit.io.network.converter;

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
   * option to provide external map to populate with external node Ids corresponding to parsed Nodes
   */
  private Map<Object, Node> nodeExternalIdToNodeMap;

  /**
   * option to provide external map to populate with link segments by external Id
   */
  private Map<Object, MacroscopicLinkSegment> linkSegmentExternalIdToLinkSegmentMap;

  /**
   * option to provide external map to populate with external link segment type Ids corresponding to link segment types
   */
  private Map<Object, MacroscopicLinkSegmentType> linkSegmentTypeExternalIdToLinkSegmentTypeMap;

  /**
   * option to provide external map to populate with external Ids corresponding to Modes
   */
  private Map<Object, Mode> modeExternalIdToModeMap;
  
  /**
   * Flag to determine whether duplicate external Id should be considered an error (defaults to {@link InputBuilderListener.DEFAULT_ERROR_ON_DUPLICATE_EXTERNAL_ID}
   */
  private boolean errorIfDuplicateExternalId = InputBuilderListener.DEFAULT_ERROR_ON_DUPLICATE_EXTERNAL_ID;  
  
  /** Use provided map to index modes by external id when parsing
   * @param modeExternalIdToModeMap to use
   */
  public void setUseMapToIndexModeByExternalIds(Map<Object, Mode> modeExternalIdToModeMap) {
    this.modeExternalIdToModeMap = modeExternalIdToModeMap;
  }
  
  /** exogenous map to index modes by external id when parsing (if any, can be null)
   * @param modeExternalIdToModeMap to use
   */
  public Map<Object, Mode> getUseMapToIndexModeByExternalIds() {
    return this.modeExternalIdToModeMap;
  }  
  
  /** Use provided map to index nodes by external id when parsing
   * @param nodeExternalIdToNodeMap to use
   */
  public void setUseMapToIndexNodeByExternalIds(Map<Object, Node> nodeExternalIdToNodeMap) {
    this.nodeExternalIdToNodeMap = nodeExternalIdToNodeMap;
  } 
  
  /** exogenous map to index nodes by external id when parsing (if any, can be null)
   * @param nodeExternalIdToNodeMap to use
   */
  public Map<Object, Node> getUseMapToIndexNodeByExternalIds() {
    return this.nodeExternalIdToNodeMap;
  }    
  
  /** Use provided map to index link segments by external id when parsing
   * @param linkSegmentExternalIdToLinkSegmentMap to use
   */
  public void setUseMapToIndexLinkSegmentByExternalIds(Map<Object, MacroscopicLinkSegment> linkSegmentExternalIdToLinkSegmentMap) {
    this.linkSegmentExternalIdToLinkSegmentMap = linkSegmentExternalIdToLinkSegmentMap;
  }   
  
  /** exogenous map to index link segments by external id when parsing (if any, can be null)
   * @param linkSegmentExternalIdToLinkSegmentMap to use
   */
  public Map<Object, MacroscopicLinkSegment> getUseMapToIndexLinkSegmentByExternalIds() {
    return this.linkSegmentExternalIdToLinkSegmentMap;
  }   
  
  /** Use provided map to index link segment types by external id when parsing
   * @param linkSegmentTypeExternalIdToLinkSegmentTypeMap to use
   */
  public void setUseMapToIndexLinkSegmentTypeByExternalIds(Map<Object, MacroscopicLinkSegmentType> linkSegmentTypeExternalIdToLinkSegmentTypeMap) {
    this.linkSegmentTypeExternalIdToLinkSegmentTypeMap = linkSegmentTypeExternalIdToLinkSegmentTypeMap;
  }  
  
  /** exogenous map to index link segment types by external id when parsing (if any, can be null)
   * @param linkSegmentTypeExternalIdToLinkSegmentTypeMap to use
   */
  public Map<Object, MacroscopicLinkSegmentType> getUseMapToIndexLinkSegmentTypeByExternalIds() {
    return this.linkSegmentTypeExternalIdToLinkSegmentTypeMap;
  }

  /** check value of this flag
   * @return true when raising error on duplicate external id
   */
  public boolean isErrorIfDuplicateExternalId() {
    return errorIfDuplicateExternalId;
  }

  /**
   * update flag on whether or not to raise an error on duplicate external ids
   * 
   * @param errorIfDuplicateExternalId
   */
  public void setErrorIfDuplicatexExternalId(boolean errorIfDuplicateExternalId) {
    this.errorIfDuplicateExternalId = errorIfDuplicateExternalId;
  }   
  
  
}
