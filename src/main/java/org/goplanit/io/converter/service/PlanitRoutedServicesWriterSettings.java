package org.goplanit.io.converter.service;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;
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

  /**
   * Default constructor
   */
  public PlanitRoutedServicesWriterSettings() {
    super();
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param countryName to use (not used as long as service network has no explicit geo locations embedded)
   */
  public PlanitRoutedServicesWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, countryName);
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
  public void logSettings(){
    super.logSettings();
    LOGGER.info(String.format("Trip frequency time unit set to %s", getTripFrequencyTimeUnit()));
  }

  // ************* getters/setters ******************

  public TimeUnit getTripFrequencyTimeUnit() {
    return frequencyTimeUnit;
  }

  public void setTripFrequencyTimeUnit(TimeUnit frequencyTimeUnit) {
    this.frequencyTimeUnit = frequencyTimeUnit;
  }

  public boolean isLogServicesWithoutTrips() {
    return logServicesWithoutTrips;
  }

  public void setLogServicesWithoutTrips(boolean logServicesWithoutTrips) {
    this.logServicesWithoutTrips = logServicesWithoutTrips;
  }
}
