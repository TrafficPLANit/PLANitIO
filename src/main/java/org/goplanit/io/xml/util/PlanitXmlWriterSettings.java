package org.goplanit.io.xml.util;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import org.goplanit.converter.FileBasedConverterWriterSettings;
import org.goplanit.converter.SingleFileBasedConverterWriterSettings;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CharacterUtils;

/**
 * Settings relevant for persisting Planit Xml output
 * 
 * @author markr
 *
 */
public class PlanitXmlWriterSettings extends SingleFileBasedConverterWriterSettings {
  
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
    super( outputPathDirectory);
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
  public void logSettings() {
    LOGGER.info(String.format("Decimal fidelity set to %s", decimalFormat.getMaximumFractionDigits()));
    
    super.logSettings();
  }  

  /** Collect number of decimals used in writing double values
   * 
   * @return number of decimals used
   */
  public DecimalFormat getDecimalFormat() {
    return decimalFormat;
  }

  /** Set number of decimals used in writing double values 
   * 
   * @param decimalFormat number of decimals
   */
  public void setDecimalFormat(DecimalFormat decimalFormat) {
    this.decimalFormat = decimalFormat;
  }
  
  /** Separator to use for tuples of coordinates that are being persisted
   * 
   * @return separator
   */
  public Character getTupleSeparator() {
    return tupleSeparator;
  }

  /**
   *  Set separator 
   *  
   * @param tupleSeparator to use
   */
  public void setTupleSeparator(Character tupleSeparator) {
    this.tupleSeparator = tupleSeparator;
  }
  
  /** Separator to use for separating x,y entries of a single coordinate that is being persisted
   * 
   * @return separator
   */  
  public Character getCommaSeparator() {
    return commaSeparator;
  }

  /** Set separator
   * 
   * @param commaSeparator to use
   */
  public void setCommaSeparator(Character commaSeparator) {
    this.commaSeparator = commaSeparator;
  }
  
  /** Separator to use for separating decimals from unit changes
   * 
   * @return separator
   */   
  public Character getDecimalSeparator() {
    return decimalSeparator;
  }

  /**
   * Set separator
   * 
   * @param decimalSeparator to use
   */
  public void setDecimalSeparator(Character decimalSeparator) {
    this.decimalSeparator = decimalSeparator;
  }
  

  /**
   * Reset content
   */
  public void reset() {
    super.reset();
  }  
    
}
