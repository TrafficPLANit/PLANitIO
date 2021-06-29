package org.planit.io.converter.zoning;

import java.util.Map;

import org.planit.converter.ConverterReaderSettings;
import org.planit.io.xml.util.PlanitXmlReaderSettings;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.physical.Node;

/**
 * Settings for the PLANit zoning reader
 * 
 * @author markr
 *
 */
public class PlanitZoningReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {
  
  /**
   * mapping of nodes by XML id for quick lookups
   */
  protected Map<String, Node> nodesByXmlId = null; 

  /**
   * mapping of link segments by XML id for quick lookups
   */  
  protected Map<String, MacroscopicLinkSegment> linkSegmentsByXmlId = null;    
  
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
    

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    nodesByXmlId = null;
    linkSegmentsByXmlId = null;    
  }
  
  // GETTERS/SETTERS
  
  /** Allow user to override the map containing the xml id to node mapping. If so, it avoids creating a duplicate index within the class instance
   * if one already exists
   * 
   * @param nodesByXmlId to use
   */
  public void setNodesByXmlId(final Map<String, Node> nodesByXmlId) {
    this.nodesByXmlId = nodesByXmlId;
  }

  /** Allow user to override the map containing the xml id to link segment mapping. If so, it avoids creating a duplicate index within the class instance
   * if one already exists
   * 
   * @param linkSegmentsByXmlId to use
   */  
  public void setLinkSegmentsByXmlId(final Map<String, MacroscopicLinkSegment> linkSegmentsByXmlId) {
    this.linkSegmentsByXmlId = linkSegmentsByXmlId;
  }  
   
}
