package org.planit.io.converter.network;

import java.util.Map;

import org.planit.converter.ConverterReaderSettings;
import org.planit.io.xml.util.PlanitXmlReaderSettings;
import org.planit.network.MacroscopicNetwork;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.Node;

/**
 * Configurable settings for the PLANit service network reader
 * 
 * @author markr
 *
 */
public class PlanitServiceNetworkReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {
  
  /** the parent network used */
  protected final MacroscopicNetwork parentNetwork;
  
  /**
   * mapping of parent nodes by XML id for quick lookups (optionally externally set)
   */
  private Map<String, Node> parentNodesByXmlId;
  
  /**
   * mapping of parent links by XML id for quick lookups (optionally externally set)
   */
  private Map<String, Link> parentLinksByXmlId;  
  
  /**
   * Collect the parent nodes indexed by their XML id
   */
  protected Map<String, Node> getParentNodesByXmlId() {
    return this.parentNodesByXmlId;
  }   
  
  /**
   * Collect the parent links indexed by their XML id
   */
  protected Map<String, Link> getParentLinksByXmlId() {
    return this.parentLinksByXmlId;
  }      
        
  /**
   * Constructor
   * 
   * @param parentNetwork to use 
   */
  public PlanitServiceNetworkReaderSettings(final MacroscopicNetwork parentNetwork) {
    super();
    this.parentNetwork = parentNetwork;
    this.parentNodesByXmlId = null;
    this.parentLinksByXmlId = null;
  }  
  
  /**
   * Constructor.
   * 
   * @param parentNetwork to use 
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   */
  public PlanitServiceNetworkReaderSettings(final MacroscopicNetwork parentNetwork, final String inputPathDirectory, final String xmlFileExtension) {
    super(inputPathDirectory, xmlFileExtension);
    this.parentNetwork = parentNetwork;
    this.parentNodesByXmlId = null;
    this.parentLinksByXmlId = null;
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    parentNodesByXmlId = null;
    parentLinksByXmlId = null;
  } 

  /** Collect the parent network for this service network
   * 
   * @return parent network
   */
  public MacroscopicNetwork getParentNetwork() {
    return parentNetwork;
  }
  
  /** Allow user to override the map for XML id to parent node mapping. If so, the reader will not create its own mapping but instead uses the provided mapping
   * 
   * @param parentNodesByXmlId to use
   */
  public void setParentNodesByXmlId(final Map<String, Node> parentNodesByXmlId) {
    this.parentNodesByXmlId = parentNodesByXmlId;
  }  
  
  /** Allow user to override the map for XML id to parent link mapping. If so, the reader will not create its own mapping but instead uses the provided mapping
   * 
   * @param parentLinksByXmlId to use
   */
  public void setParentLinksByXmlId(final Map<String, Link> parentLinksByXmlId) {
    this.parentLinksByXmlId = parentLinksByXmlId;
  }   
 
  
}
