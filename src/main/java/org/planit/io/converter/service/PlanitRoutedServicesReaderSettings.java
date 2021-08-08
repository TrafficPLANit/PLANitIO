package org.planit.io.converter.service;

import org.planit.converter.ConverterReaderSettings;
import org.planit.io.xml.util.PlanitXmlReaderSettings;
import org.planit.network.ServiceNetwork;

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
   * Constructor
   * 
   * @param parentNetwork to use 
   */
  public PlanitRoutedServicesReaderSettings(final ServiceNetwork parentNetwork) {
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
  public PlanitRoutedServicesReaderSettings(final ServiceNetwork parentNetwork, final String inputPathDirectory, final String xmlFileExtension) {
    super(inputPathDirectory, xmlFileExtension);
    this.parentNetwork = parentNetwork;
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
  } 

  /** Collect the parent service network for these routed services
   * 
   * @return parent network
   */
  public ServiceNetwork getParentNetwork() {
    return parentNetwork;
  }  
    
}
