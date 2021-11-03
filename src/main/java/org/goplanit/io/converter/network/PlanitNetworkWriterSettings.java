package org.goplanit.io.converter.network;

import java.util.logging.Logger;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;

/**
 * configurable settings for the PLANit writer
 * 
 * @author markr
 *
 */
public class PlanitNetworkWriterSettings extends PlanitXmlWriterSettings implements ConverterWriterSettings {
  
  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkWriterSettings.class.getCanonicalName());
      
  /**
   * Default constructor
   */
  public PlanitNetworkWriterSettings() {
    super();
  }
  
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  public PlanitNetworkWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, countryName);
  }  
    
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   * @param fileName to use
   * @param countryName to use
   */
  public PlanitNetworkWriterSettings(String outputPathDirectory, final String fileName, String countryName) {
    super(outputPathDirectory, fileName, countryName);
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
  }  
  
}
