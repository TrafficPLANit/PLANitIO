package org.goplanit.io.converter.pricing;

import java.util.logging.Logger;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.converter.PlanitXmlWriterSettings;
import org.goplanit.utils.misc.LoggingUtils;

/**
 * Configurable settings for the PLANit network pricing writer
 * <p>
 * The pricing writer takes only the keys every PLANit writer shares, with its own default file name. It has settings
 * of its own so a settings file names the writer it is for, and so its settings are logged under its own header. A
 * pricing holds no geometry, so no country is used.
 * </p>
 *
 * @author markr
 */
public class PlanitNetworkPricingWriterSettings extends PlanitXmlWriterSettings implements ConverterWriterSettings {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkPricingWriterSettings.class.getCanonicalName());

  /** default network pricing file name to use */
  public static final String DEFAULT_NETWORK_PRICING_XML = "network_pricing.xml";

  /**
   * Constructor
   */
  public PlanitNetworkPricingWriterSettings() {
    super();
    setFileName(DEFAULT_NETWORK_PRICING_XML);
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   */
  public PlanitNetworkPricingWriterSettings(final String outputPathDirectory) {
    super(outputPathDirectory, DEFAULT_NETWORK_PRICING_XML, null /* no country used for a pricing */);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
    setFileName(DEFAULT_NETWORK_PRICING_XML);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings(int level) {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Network Pricing Writer Settings"));
    super.logSettings(level);
  }
}
