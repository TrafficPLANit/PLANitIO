package org.goplanit.io.converter.network;

import java.util.logging.Logger;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.converter.PlanitXmlWriterSettings;
import org.goplanit.utils.misc.LoggingUtils;

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

  /** default network file name to use */
  public static final String DEFAULT_NETWORK_XML = "network.xml";
      
  /**
   * Default constructor
   */
  public PlanitNetworkWriterSettings() {
    super();
    setFileName(DEFAULT_NETWORK_XML);
  }
  
  /**
   * Constructor with default file name
   * 
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  public PlanitNetworkWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, DEFAULT_NETWORK_XML, countryName);
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
    setFileName(DEFAULT_NETWORK_XML);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings(int level) {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Network Writer Settings"));
    super.logSettings(level);
  }
  
}
