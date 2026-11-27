package org.goplanit.io.converter.pricing;

import java.util.logging.Logger;

import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.io.converter.PlanitXmlReaderSettings;
import org.goplanit.utils.misc.LoggingUtils;

/**
 * Configurable settings for the PLANit network pricing reader
 * <p>
 * The pricing reader takes only the keys every PLANit reader shares: where to read from, the file extension, and
 * whether to sync XML ids to ids. It has settings of its own so a settings file names the reader it is for, and so
 * its settings are logged under its own header.
 * </p>
 *
 * @author markr
 */
public class PlanitNetworkPricingReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkPricingReaderSettings.class.getCanonicalName());

  /**
   * Constructor
   */
  public PlanitNetworkPricingReaderSettings() {
    super();
  }

  /**
   * Constructor
   *
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   */
  public PlanitNetworkPricingReaderSettings(final String inputPathDirectory, final String xmlFileExtension) {
    super(inputPathDirectory, xmlFileExtension);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings(int level) {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Network Pricing Reader Settings"));
    super.logSettings(level);
  }
}
