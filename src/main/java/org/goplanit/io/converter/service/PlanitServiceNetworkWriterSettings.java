package org.goplanit.io.converter.service;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.converter.PlanitXmlWriterSettings;
import org.goplanit.utils.misc.LoggingUtils;

import java.util.logging.Logger;

/**
 * configurable settings for the PLANit service network writer
 * 
 * @author markr
 *
 */
public class PlanitServiceNetworkWriterSettings extends PlanitXmlWriterSettings implements ConverterWriterSettings {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PlanitServiceNetworkWriterSettings.class.getCanonicalName());

  /** default network file name to use */
  public static final String DEFAULT_SERVICE_NETWORK_XML = "service_network.xml";

  /**
   * Default constructor
   */
  public PlanitServiceNetworkWriterSettings() {
    super();
    setFileName(DEFAULT_SERVICE_NETWORK_XML);
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param countryName to use (not used as long as service network has no explicit geo locations embedded)
   */
  public PlanitServiceNetworkWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, DEFAULT_SERVICE_NETWORK_XML, countryName);
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param fileName to use
   * @param countryName to use (not used as long as service network has no explicit geo locations embedded)
   */
  public PlanitServiceNetworkWriterSettings(String outputPathDirectory, final String fileName, String countryName) {
    super(outputPathDirectory, fileName, countryName);
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
    setFileName(DEFAULT_SERVICE_NETWORK_XML);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings(int level) {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Service Network Writer Settings"));
    super.logSettings(level);
  }
  
}
