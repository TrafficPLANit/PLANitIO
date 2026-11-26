package org.goplanit.io.converter;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import org.goplanit.converter.FileBasedConverterWriterSettings;
import org.goplanit.converter.SingleFileBasedConverterWriterSettings;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CharacterUtils;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.settings.SettingsSchema;
import org.goplanit.utils.settings.YamlWriteSettingsSupport;
import org.goplanit.utils.settings.annotations.SettingsHeader;
import org.goplanit.utils.settings.annotations.SettingsKeyLevel;
import org.goplanit.utils.settings.annotations.SettingsRead;
import org.goplanit.utils.settings.annotations.SettingsWrite;

/**
 * Settings relevant for persisting Planit Xml output
 * <p>
 * Each PLANit writer has a settings file of its own, headed {@code planitIoWriterSettings: 1}, holding every key at
 * the top level, those of every writer and its own, such as
 * </p>
 * <pre>
 * planitIoWriterSettings: 1
 * outputDirectory: out
 * country: Australia
 * destinationCrs: EPSG:28356
 * fileName: roads.xml
 * </pre>
 * <p>
 * The writers' files are apart from the readers', so a step reading another format and writing PLANit uses a whole
 * file for each. {@link PlanitXmlWriterSettingsFactory} makes the settings of each writer from such files.
 * </p>
 *
 * @author markr
 *
 */
@SettingsHeader(key = "planitIoWriterSettings", version = 1)
public class PlanitXmlWriterSettings extends SingleFileBasedConverterWriterSettings implements YamlWriteSettingsSupport {
  
  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitXmlWriterSettings.class.getCanonicalName());

  /**
   * number of decimals to use, default is Precision.DEFAULT_DECIMAL_FORMAT
   */
  protected DecimalFormat decimalFormat = Precision.DEFAULT_DECIMAL_FORMAT;
  
  /** tuple separator, default is CharacterUtils.SPACE */
  protected Character tupleSeparator = CharacterUtils.SPACE;

  /** tuple separator, default is CharacterUtils.COMMA */
  protected Character commaSeparator = CharacterUtils.COMMA;
  
  /** decimal separator, default is CharacterUtils.DECIMAL_POINT */
  protected Character decimalSeparator = CharacterUtils.DECIMAL_POINT;
  

  /** Validate the settings
   * 
   * @return true when valid, false otherwise
   */
  protected boolean validate() {
    /* other settings are not always mandatory */
    return super.validate();
  }    
  
  /**
   * Default constructor 
   */
  public PlanitXmlWriterSettings() {
    super();
  }
  
  /**
   * Constructor
   * 
   *  @param outputPathDirectory to use
   */
  public PlanitXmlWriterSettings(final String outputPathDirectory) {
    super(outputPathDirectory);
  }
  
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  public PlanitXmlWriterSettings(final String outputPathDirectory, final String countryName) {
    super( outputPathDirectory, countryName);
  }      
  
  /**
   * Constructor
   * 
   *  @param outputPathDirectory to use
   *  @param fileName to use
   *  @param countryName to use
   */
  public PlanitXmlWriterSettings(final String outputPathDirectory, final String fileName, final String countryName) {
    super( outputPathDirectory, fileName, countryName);
  }  

  /**
   * Convenience method to log all the current settings
   */
  @Override
  public void logSettings(int level) {
    super.logSettings(level);
    LOGGER.info(LoggingUtils.settingsValue("Decimal fidelity", decimalFormat.getMaximumFractionDigits(), level));
    LOGGER.info(LoggingUtils.settingsValue("Tuple separator", tupleSeparator, level));
    LOGGER.info(LoggingUtils.settingsValue("Comma separator", commaSeparator, level));
    LOGGER.info(LoggingUtils.settingsValue("Decimal separator", decimalSeparator, level));
  }  

  /** Collect number of decimals used in writing double values
   * 
   * @return number of decimals used
   */
  @SettingsRead(key = "decimalFormat")
  public DecimalFormat getDecimalFormat() {
    return decimalFormat;
  }

  /** Set number of decimals used in writing double values
   *
   * @param decimalFormat number of decimals
   */
  @SettingsWrite(key = "decimalFormat", level = SettingsKeyLevel.UNCOMMON,
      description = "The pattern numbers are written with, such as #.###, as Java's DecimalFormat reads it")
  public void setDecimalFormat(DecimalFormat decimalFormat) {
    this.decimalFormat = decimalFormat;
  }

  /** Separator to use for tuples of coordinates that are being persisted
   *
   * @return separator
   */
  @SettingsRead(key = "tupleSeparator")
  public Character getTupleSeparator() {
    return tupleSeparator;
  }

  /**
   *  Set separator
   *
   * @param tupleSeparator to use
   */
  @SettingsWrite(key = "tupleSeparator", level = SettingsKeyLevel.RARE,
      description = "The character between two coordinates of a line or polygon, such as a space")
  public void setTupleSeparator(Character tupleSeparator) {
    this.tupleSeparator = tupleSeparator;
  }

  /** Separator to use for separating x,y entries of a single coordinate that is being persisted
   *
   * @return separator
   */
  @SettingsRead(key = "commaSeparator")
  public Character getCommaSeparator() {
    return commaSeparator;
  }

  /** Set separator
   *
   * @param commaSeparator to use
   */
  @SettingsWrite(key = "commaSeparator", level = SettingsKeyLevel.RARE,
      description = "The character between the x and the y of one coordinate, such as a comma")
  public void setCommaSeparator(Character commaSeparator) {
    this.commaSeparator = commaSeparator;
  }

  /** Separator to use for separating decimals from unit changes
   *
   * @return separator
   */
  @SettingsRead(key = "decimalSeparator")
  public Character getDecimalSeparator() {
    return decimalSeparator;
  }

  /**
   * Set separator
   *
   * @param decimalSeparator to use
   */
  @SettingsWrite(key = "decimalSeparator", level = SettingsKeyLevel.RARE,
      description = "The character between the whole part and the decimals of a number, such as a point")
  public void setDecimalSeparator(Character decimalSeparator) {
    this.decimalSeparator = decimalSeparator;
  }


  /**
   * Reset content
   */
  public void reset() {
    super.reset();
    this.decimalFormat = Precision.DEFAULT_DECIMAL_FORMAT;
    this.tupleSeparator = CharacterUtils.SPACE;
    this.commaSeparator = CharacterUtils.COMMA;
    this.decimalSeparator = CharacterUtils.DECIMAL_POINT;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SettingsSchema getSettingsSchema() {
    return PlanitXmlWriterSettingsSchema.SCHEMA;
  }

}
