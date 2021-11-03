package org.goplanit.io.converter.network;

import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.io.xml.util.PlanitXmlReaderSettings;

/**
 * Configurable settings for the PLANit network reader
 * 
 * @author markr
 *
 */
public class PlanitNetworkReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {
    

  
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
  


  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // TODO    
  } 
    
  
}
