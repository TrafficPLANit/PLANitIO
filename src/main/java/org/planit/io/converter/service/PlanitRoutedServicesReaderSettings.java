package org.planit.io.converter.service;

import java.util.HashMap;
import java.util.Map;

import org.planit.converter.ConverterReaderSettings;
import org.planit.io.xml.util.PlanitXmlReaderSettings;
import org.planit.network.ServiceNetwork;
import org.planit.utils.network.layer.ServiceNetworkLayer;
import org.planit.utils.network.layer.service.ServiceLeg;

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
   * mapping of parent legs by XML id for quick lookups (optionally externally set)
   */
  private final Map<ServiceNetworkLayer, Map<String, ServiceLeg>> parentLegsByXmlId;
  
  /**
   * Check if parent legs by XML id are present (for any layer) 
   * 
   * @param true when at least one is available, false otherwise
   */
  protected boolean hasParentLegsByXmlId() {
    return parentLegsByXmlId.isEmpty();
  }   
  
  /**
   * Collect the parent legs indexed by their XML id
   * 
   * @param layer for which we collect the alternatively indexed entities
   */
  protected Map<String, ServiceLeg> getParentLegsByXmlId(final ServiceNetworkLayer layer) {
    return parentLegsByXmlId.get(layer);
  }      
        
  /**
   * Constructor
   * 
   * @param parentNetwork to use 
   */
  public PlanitRoutedServicesReaderSettings(final ServiceNetwork parentNetwork) {
    super();
    this.parentNetwork = parentNetwork;
    this.parentLegsByXmlId = new HashMap<ServiceNetworkLayer,Map<String, ServiceLeg>>();
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
    this.parentLegsByXmlId = null;
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    parentLegsByXmlId.clear();
  } 

  /** Collect the parent service network for these routed services
   * 
   * @return parent network
   */
  public ServiceNetwork getParentNetwork() {
    return parentNetwork;
  }
  
  /** Allow user to override the map for XML id to parent leg mapping. If so, the reader will not create its own mapping but instead uses the provided mapping
   * @param layer 
   * 
   * @param parentLegsByXmlId to use
   */
  public void setParentLegsByXmlId(ServiceNetworkLayer layer, final Map<String, ServiceLeg> parentLegsByXmlId) {
    this.parentLegsByXmlId.put(layer,parentLegsByXmlId);
  }     
  
}
