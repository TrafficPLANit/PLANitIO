package org.goplanit.io.converter.zoning;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.converter.PlanitXmlWriterSettings;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.settings.annotations.SettingsKeyLevel;
import org.goplanit.utils.settings.annotations.SettingsRead;
import org.goplanit.utils.settings.annotations.SettingsWrite;

import java.util.logging.Logger;

/**
 * Settings for the PLANit zoning writer
 * 
 * @author markr
 *
 */
public class PlanitZoningWriterSettings extends PlanitXmlWriterSettings implements ConverterWriterSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitZoningWriterSettings.class.getCanonicalName());

  private boolean removeDanglingZones = DEFAULT_REMOVE_DANGLING_ZONES;


  /** default zoning file name to use */
  public static final String DEFAULT_ZONING_XML = "zoning.xml";

  /** default we remove dangling zones (zones without a connctoid) */
  public static final boolean DEFAULT_REMOVE_DANGLING_ZONES = true;

  /**
   * Default constructor
   */
  public PlanitZoningWriterSettings() {
    super();
    setFileName(DEFAULT_ZONING_XML);
  }
  
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   */
  public PlanitZoningWriterSettings(final String outputPathDirectory) {
    super(outputPathDirectory);
    setFileName(DEFAULT_ZONING_XML);
  }
  
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  public PlanitZoningWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, DEFAULT_ZONING_XML, countryName);
  }  
  
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   * @param outputFileName to use
   * @param countryName to use
   */
  public PlanitZoningWriterSettings(
      final String outputPathDirectory, final String outputFileName, final String countryName) {
    super(outputPathDirectory, outputFileName, countryName);
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings(int level) {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Zoning Writer Settings"));
    super.logSettings(level);
    LOGGER.info(LoggingUtils.settingsValue("Remove dangling zones", isRemoveDanglingZones(), level));
  }

  /** flag on removal of dangling zones
   * Get the value of removeDanglingZones.
   *
   * @return value of removeDanglingZones
   */
  @SettingsRead(key = "removeDanglingZones")
  public boolean isRemoveDanglingZones() {
    return removeDanglingZones;
  }

  /**
   * Set the value of removeDanglingZones.
   *
   * @param removeDanglingZones value of removeDanglingZones
   */
  @SettingsWrite(key = "removeDanglingZones", level = SettingsKeyLevel.UNCOMMON,
      description = "Leave out the zones without a connectoid, logging each one")
  public void setRemoveDanglingZones(boolean removeDanglingZones) {
    this.removeDanglingZones = removeDanglingZones;
  }
}
