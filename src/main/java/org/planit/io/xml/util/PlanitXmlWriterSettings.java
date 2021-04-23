package org.planit.io.xml.util;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.locale.CountryNames;
import org.planit.utils.math.Precision;
import org.planit.utils.misc.CharacterUtils;

/**
 * Settings relevant for persisting Planit Xml output
 * 
 * @author markr
 *
 */
public class PlanitXmlWriterSettings {
  
  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitXmlWriterSettings.class.getCanonicalName());

  /** directory to persist to */
  private String outputPathDirectory = null; 
  
  /** destination country to persist for */
  private String countryName = DEFAULT_COUNTRY;  
  
  /** destination file name to persist to */
  private String fileName = null;   
  
  /** default destination country to use if none is set */
  public static String DEFAULT_COUNTRY = CountryNames.GLOBAL;
  
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
  
  /** the coordinate reference system used for writing entities of this network */
  protected CoordinateReferenceSystem destinationCoordinateReferenceSystem = null;    
  
  /**
   * Default constructor 
   */
  public PlanitXmlWriterSettings() {
  }
  
  /**
   * Constructor
   * 
   *  @param outputPathDirectory to use
   */
  public PlanitXmlWriterSettings(final String outputPathDirectory) {
    this.outputPathDirectory = outputPathDirectory;
  }
  
  /**
   * Constructor
   * 
   *  @param outputPathDirectory to use
   */
  public PlanitXmlWriterSettings(final String outputPathDirectory, final String fileName) {
    this.outputPathDirectory = outputPathDirectory;
    this.setFileName(fileName);
  }      
  
  /**
   * Constructor
   * 
   *  @param outputPathDirectory to use
   *  @param countryName to use
   */
  public PlanitXmlWriterSettings(final String outputPathDirectory, final String fileName, final String countryName) {
    this.outputPathDirectory = outputPathDirectory;
    this.countryName = countryName;
    this.setFileName(fileName);
  }  
  
  /** the outputPathDirectory used
   * @return directory used
   */
  public String getOutputPathDirectory() {
    return this.outputPathDirectory;
  }
  
  /** set the outputPathDirectory used
   * @param directory to use
   */
  public void setOutputPathDirectory(String outputPathDirectory) {
    this.outputPathDirectory = outputPathDirectory;
  }

  /** collect country name used
   * @return country name
   */
  public String getCountryName() {
    return countryName;
  }

  /** Set country name used
   * @param countryName to use
   */
  public void setCountryName(String countryName) {
    this.countryName = countryName;
  }

  /** collect the file name to use
   * @return file name to use
   */
  public String getFileName() {
    return fileName;
  }

  /** set the file name to use
   * @param fileName to use
   */
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }  
  
  /**
   * Convenience method to log all the current settings
   */
  public void logSettings() {
    LOGGER.info(String.format("Decimal fidelity set to %s", decimalFormat.getMaximumFractionDigits()));
    
    if(getDestinationCoordinateReferenceSystem() != null) {
      LOGGER.info(String.format("Destination Coordinate Reference System set to: %s", getDestinationCoordinateReferenceSystem().getName()));
    }
    
  }  

  /** collect number of decimals used in writing coordinates
   * @return number of decimals used
   */
  public DecimalFormat getDecimalFormat() {
    return decimalFormat;
  }

  /** set number of decimals used in writing coordinates
   * 
   * @param decimalFormat number of decimals
   */
  public void setDecimalFormat(DecimalFormat decimalFormat) {
    this.decimalFormat = decimalFormat;
  }
  
  /** separator to use for tuples of coordinates that are being persisted
   * 
   * @return separator
   */
  public Character getTupleSeparator() {
    return tupleSeparator;
  }

  /**
   *  set separator 
   *  
   * @param tupleSeparator to use
   */
  public void setTupleSeparator(Character tupleSeparator) {
    this.tupleSeparator = tupleSeparator;
  }
  
  /** separator to use for separating x,y entries of a single coordinate that is being persisted
   * 
   * @return separator
   */  
  public Character getCommaSeparator() {
    return commaSeparator;
  }

  /** set separator
   * 
   * @param commaSeparator to use
   */
  public void setCommaSeparator(Character commaSeparator) {
    this.commaSeparator = commaSeparator;
  }
  
  /** separator to use for separating decimals from unit changes
   * 
   * @return separator
   */   
  public Character getDecimalSeparator() {
    return decimalSeparator;
  }

  /**
   * set separator
   * 
   * @param decimalSeparator to use
   */
  public void setDecimalSeparator(Character decimalSeparator) {
    this.decimalSeparator = decimalSeparator;
  }
  
  
  /** collect the destination Crs
   * @return destination Crs
   */
  public CoordinateReferenceSystem getDestinationCoordinateReferenceSystem() {
    return destinationCoordinateReferenceSystem;
  }

  /** Set the destination Crs to use (if not set, network's native Crs will be used, unless the user has specified a
   * specific country for which we have a more appropriate Crs registered) 
   * 
   * @param destinationCoordinateReferenceSystem to use
   */
  public void setDestinationCoordinateReferenceSystem(CoordinateReferenceSystem destinationCoordinateReferenceSystem) {
    this.destinationCoordinateReferenceSystem = destinationCoordinateReferenceSystem;
  }

  /**
   * Reset content
   */
  public void reset() {
    //TODO
  }  
    
}
