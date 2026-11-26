package org.goplanit.io.converter.service;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.converter.PlanitXmlWriterSettings;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.settings.annotations.SettingsKeyLevel;
import org.goplanit.utils.settings.annotations.SettingsRead;
import org.goplanit.utils.settings.annotations.SettingsWrite;
import org.goplanit.utils.unit.TimeUnit;

import java.util.logging.Logger;

/**
 * configurable settings for the PLANit routed services writer
 * 
 * @author markr
 *
 */
public class PlanitRoutedServicesWriterSettings extends PlanitXmlWriterSettings implements ConverterWriterSettings {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PlanitRoutedServicesWriterSettings.class.getCanonicalName());

  /** user configured time unit */
  private TimeUnit frequencyTimeUnit = DEFAULT_FREQUENCY_TIME_UNIT;

  /** user configured logging regarding discarded routed services because of no trips associated with them */
  private boolean logServicesWithoutTrips = DEFAULT_LOG_SERVICES_WITHOUT_TRIPS;

  /** default time unit to use for trip frequencies */
  public static final TimeUnit DEFAULT_FREQUENCY_TIME_UNIT = TimeUnit.HOUR;

  /** default time unit to use for trip frequencies */
  public static final boolean DEFAULT_LOG_SERVICES_WITHOUT_TRIPS = false;

  /** default routed services file name to use */
  public static final String DEFAULT_ROUTED_SERVICES_XML = "routed_services.xml";

  /**
   * Default constructor
   */
  public PlanitRoutedServicesWriterSettings() {
    super();
    setFileName(DEFAULT_ROUTED_SERVICES_XML);
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param countryName to use (not used as long as service network has no explicit geo locations embedded)
   */
  public PlanitRoutedServicesWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, DEFAULT_ROUTED_SERVICES_XML, countryName);
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param fileName to use
   * @param countryName to use (not used as long as service network has no explicit geo locations embedded)
   */
  public PlanitRoutedServicesWriterSettings(String outputPathDirectory, final String fileName, String countryName) {
    super(outputPathDirectory, fileName, countryName);
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
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Routed Services Writer Settings"));
    super.logSettings(level);
    LOGGER.info(LoggingUtils.settingsValue("Trip frequency time unit", getTripFrequencyTimeUnit(), level));
    LOGGER.info(LoggingUtils.settingsValue("Log services without trips", isLogServicesWithoutTrips(), level));
  }

  // ************* getters/setters ******************

  /**
   * time unit to use
   * @return time unit
   */
  @SettingsRead(key = "tripFrequencyTimeUnit")
  public TimeUnit getTripFrequencyTimeUnit() {
    return frequencyTimeUnit;
  }

  /**
   * set time unit to use
   * @param frequencyTimeUnit unit
   */
  @SettingsWrite(key = "tripFrequencyTimeUnit", level = SettingsKeyLevel.UNCOMMON,
      description = "The time unit trip frequencies are written per: HOUR, MINUTE, SECOND or MILLISECOND")
  public void setTripFrequencyTimeUnit(TimeUnit frequencyTimeUnit) {
    this.frequencyTimeUnit = frequencyTimeUnit;
  }

  /**
   * Check flag to log services without any trips
   * @return flag
   */
  @SettingsRead(key = "logServicesWithoutTrips")
  public boolean isLogServicesWithoutTrips() {
    return logServicesWithoutTrips;
  }

  /**
   * Flag to log services without any trips
   * @param logServicesWithoutTrips flag
   */
  @SettingsWrite(key = "logServicesWithoutTrips", level = SettingsKeyLevel.RARE,
      description = "Log each routed service left out for having no trips")
  public void setLogServicesWithoutTrips(boolean logServicesWithoutTrips) {
    this.logServicesWithoutTrips = logServicesWithoutTrips;
  }
}
