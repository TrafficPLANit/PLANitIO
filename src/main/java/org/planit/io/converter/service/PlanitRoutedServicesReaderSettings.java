package org.planit.io.converter.service;

import java.util.HashMap;
import java.util.Map;

import org.planit.converter.ConverterReaderSettings;
import org.planit.io.xml.util.PlanitXmlReaderSettings;
import org.planit.network.ServiceNetwork;
import org.planit.utils.network.layer.ServiceNetworkLayer;
import org.planit.utils.network.layer.service.ServiceLegSegment;

/**
 * Configurable settings for the PLANit routed services reader
 * 
 * @author markr
 *
 */
public class PlanitRoutedServicesReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {
  
  /** the parent service network used */
  protected final ServiceNetwork parentNetwork;
  
  /**
   * mapping of parent leg segments by XML id for quick lookups (optionally externally set)
   */
  private final Map<ServiceNetworkLayer, Map<String, ServiceLegSegment>> parentLegSegmentsByXmlId;  
  
  
  /**
   * Check if parent leg segments by XML id are present (for any layer) 
   * 
   * @param true when at least one is available, false otherwise
   */
  protected boolean hasParentLegSegmentsByXmlId() {
    return !parentLegSegmentsByXmlId.isEmpty();
  }    
   
  
  /**
   * Collect the parent leg segments indexed by their XML id
   * 
   * @param layer for which we collect the alternatively indexed entities
   */
  protected Map<String, ServiceLegSegment> getParentLegSegmentsByXmlId(final ServiceNetworkLayer layer) {
    return parentLegSegmentsByXmlId.get(layer);
  }     
        
  /**
   * Constructor
   * 
   * @param parentNetwork to use 
   */
  public PlanitRoutedServicesReaderSettings(final ServiceNetwork parentNetwork) {
    super();
    this.parentNetwork = parentNetwork;
    this.parentLegSegmentsByXmlId = new HashMap<ServiceNetworkLayer,Map<String, ServiceLegSegment>>();
  }  
  
  /**
   * Constructor.
   * 
   * @param parentNetwork to use 
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   */
  public PlanitRoutedServicesReaderSettings(final ServiceNetwork parentNetwork, final String inputPathDirectory, final String xmlFileExtension) {
    super(inputPathDirectory, xmlFileExtension);
    this.parentNetwork = parentNetwork;
    this.parentLegSegmentsByXmlId = null;
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    parentLegSegmentsByXmlId.clear();
  } 

  /** Collect the parent service network for these routed services
   * 
   * @return parent network
   */
  public ServiceNetwork getParentNetwork() {
    return parentNetwork;
  }  
  
  /** Allow user to override the map for XML id to parent leg segment mapping. If so, the reader will not create its own mapping but instead uses the provided mapping
   * @param layer 
   * 
   * @param parentLegsByXmlId to use
   */
  public void setParentLegSegmentsByXmlId(ServiceNetworkLayer layer, final Map<String, ServiceLegSegment> parentLegSegmentsByXmlId) {
    this.parentLegSegmentsByXmlId.put(layer,parentLegSegmentsByXmlId);
  }    
  
}
