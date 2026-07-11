package org.goplanit.io.converter.service;

import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.io.xml.util.PlanitXmlReaderSettings;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.utils.misc.LoggingUtils;

import java.util.logging.Logger;

/**
 * Configurable settings for the PLANit routed services reader
 * 
 * @author markr
 *
 */
public class PlanitRoutedServicesReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitRoutedServicesReaderSettings.class.getCanonicalName());

  /**
   * Constructor
   *
   */
  public PlanitRoutedServicesReaderSettings() {
    this(".", DEFAULT_XML_EXTENSION);
  }

  /**
   * Constructor.
   *
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   */
  public PlanitRoutedServicesReaderSettings(final String inputPathDirectory, final String xmlFileExtension) {
    super(inputPathDirectory, xmlFileExtension);
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
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings() {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Routed Services Reader Settings"));
    super.logSettings();
  }

}
