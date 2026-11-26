package org.goplanit.io.converter.demands;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.converter.PlanitXmlWriterSettings;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.settings.annotations.SettingsKeyLevel;
import org.goplanit.utils.settings.annotations.SettingsRead;
import org.goplanit.utils.settings.annotations.SettingsWrite;

/**
 * Configurable settings for the PLANit demands writer. Note that the default decimal format used is a maximum of
 * 4 digits to reduce the size of the OD matrices. This can however be altered by the user if desired.
 * 
 * @author markr
 *
 */
public class PlanitDemandsWriterSettings extends PlanitXmlWriterSettings implements ConverterWriterSettings {
  
  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PlanitDemandsWriterSettings.class.getCanonicalName());
  
  /** origin separator used in matrix values element */
  private String originSeparator = DEFAULT_ORIGIN_SEPARATOR;
  
  /** destination separator used in matrix values element */
  private String destinationSeparator = DEFAULT_DESTINATION_SEPARATOR;

  /**
   * Write the OD values with at most four fraction digits, more being of no use for demands
   */
  private void useFourFractionDigits() {
    var demandWriterDecimalFormat = (DecimalFormat)this.getDecimalFormat().clone();
    demandWriterDecimalFormat.setMaximumFractionDigits(4);
    setDecimalFormat(demandWriterDecimalFormat);
  }

  /** Validate the settings
   *
   * @return true when valid, false otherwise
   */
  protected boolean validate() {
    return super.validate();
  }

  /** default origin separator used in matrix values element */
  public static String DEFAULT_ORIGIN_SEPARATOR = " ";

  /** default origin separator used in matrix values element */
  public static String DEFAULT_DESTINATION_SEPARATOR = ",";

  /** default demands file name to use */
  public static final String DEFAULT_DEMANDS_XML = "demands.xml";
  
  /**
   * Default constructor
   */
  public PlanitDemandsWriterSettings() {
    super();
    setFileName(DEFAULT_DEMANDS_XML);
    useFourFractionDigits();
  }
  
  /**
   * Constructor, requires user to se file name
   * 
   * @param outputPathDirectory to use
   */
  public PlanitDemandsWriterSettings(final String outputPathDirectory) {
    this(outputPathDirectory, DEFAULT_DEMANDS_XML);
  }  
    
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   * @param fileName to use
   */
  public PlanitDemandsWriterSettings(String outputPathDirectory, final String fileName) {
    super(outputPathDirectory, fileName, null /* no country used for demands */);
    useFourFractionDigits();
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
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Demands Writer Settings"));
    super.logSettings(level);
    LOGGER.info(LoggingUtils.settingsValue("Origin separator", getOriginSeparator(), level));
    LOGGER.info(LoggingUtils.settingsValue("Destination separator", getDestinationSeparator(), level));
  }

  /**
   * separator used in between origins
   * @return sep string
   */
  @SettingsRead(key = "originSeparator")
  public String getOriginSeparator() {
    return originSeparator;
  }

  /**
   * set separator used in between origins
   * @param originSeparator string to use
   */
  @SettingsWrite(key = "originSeparator", level = SettingsKeyLevel.RARE,
      description = "The text between the rows of two origins in the demand matrix, such as a space")
  public void setOriginSeparator(String originSeparator) {
    this.originSeparator = originSeparator;
  }

  /**
   * separator used in between destinations
   * @return sep string
   */
  @SettingsRead(key = "destinationSeparator")
  public String getDestinationSeparator() {
    return destinationSeparator;
  }

  /**
   * set separator used in between destinations
   * @param destinationSeparator string to use
   */
  @SettingsWrite(key = "destinationSeparator", level = SettingsKeyLevel.RARE,
      description = "The text between the values of two destinations in a row of the demand matrix, such as a comma")
  public void setDestinationSeparator(String destinationSeparator) {
    this.destinationSeparator = destinationSeparator;
  }
  
}
