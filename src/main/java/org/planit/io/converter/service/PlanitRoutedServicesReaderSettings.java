package org.planit.io.converter.service;

import java.util.Map;

import org.planit.converter.ConverterReaderSettings;
import org.planit.io.xml.util.PlanitXmlReaderSettings;
import org.planit.network.ServiceNetwork;
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
  private Map<String, ServiceLeg> parentLegsByXmlId; 
  
  /**
   * Collect the parent legs indexed by their XML id
   */
  protected Map<String, ServiceLeg> getParentLegsByXmlId() {
    return this.parentLegsByXmlId;
  }      
        
  /**
   * Constructor
   * 
   * @param parentNetwork to use 
   */
  public PlanitRoutedServicesReaderSettings(final ServiceNetwork parentNetwork) {
    super();
    this.parentNetwork = parentNetwork;
    this.parentLegsByXmlId = null;
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
    parentLegsByXmlId = null;
  } 

  /** Collect the parent service network for these routed services
   * 
   * @return parent network
   */
  public ServiceNetwork getParentNetwork() {
    return parentNetwork;
  }
  
  /** Allow user to override the map for XML id to parent leg mapping. If so, the reader will not create its own mapping but instead uses the provided mapping
   * 
   * @param parentLegsByXmlId to use
   */
  public void setParentLegsByXmlId(final Map<String, ServiceLeg> parentLegsByXmlId) {
    this.parentLegsByXmlId = parentLegsByXmlId;
  }     
  
}
