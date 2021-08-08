package org.planit.io.converter.service;

import org.planit.converter.ConverterReaderSettings;
import org.planit.io.xml.util.PlanitXmlReaderSettings;
import org.planit.network.MacroscopicNetwork;

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
   * Constructor
   * 
   * @param parentNetwork to use 
   */
  public PlanitServiceNetworkReaderSettings(final MacroscopicNetwork parentNetwork) {
    super();
    this.parentNetwork = parentNetwork;
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
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
  } 

  /** Collect the parent network for this service network
   * 
   * @return parent network
   */
  public MacroscopicNetwork getParentNetwork() {
    return parentNetwork;
  } 
  
}
