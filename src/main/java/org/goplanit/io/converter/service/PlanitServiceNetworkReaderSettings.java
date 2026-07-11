package org.goplanit.io.converter.service;

import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.io.xml.util.PlanitXmlReaderSettings;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.misc.LoggingUtils;

import java.util.logging.Logger;

/**
 * Configurable settings for the PLANit service network reader
 * 
 * @author markr
 *
 */
public class PlanitServiceNetworkReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitServiceNetworkReaderSettings.class.getCanonicalName());

  /**
   * Constructor.
   *
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   */
  public PlanitServiceNetworkReaderSettings(final String inputPathDirectory, final String xmlFileExtension) {
    super(inputPathDirectory, xmlFileExtension);
  }

  /**
   * Constructor
   */
  public PlanitServiceNetworkReaderSettings() {
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings() {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Service Network Reader Settings"));
    super.logSettings();
  }


}
