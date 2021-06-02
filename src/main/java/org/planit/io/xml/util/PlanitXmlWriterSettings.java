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
  private String outputDirectory = null; 
  
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
    this.outputDirectory = outputPathDirectory;
  }
  
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  public PlanitXmlWriterSettings(final String outputPathDirectory, final String countryName) {
    this.outputDirectory = outputPathDirectory;
    this.setCountry(countryName);
  }      
  
  /**
   * Constructor
   * 
   *  @param outputPathDirectory to use
   *  @param fileName to use
   *  @param countryName to use
   */
  public PlanitXmlWriterSettings(final String outputPathDirectory, final String fileName, final String countryName) {
    this.outputDirectory = outputPathDirectory;
    this.setCountry(countryName);
    this.setFileName(fileName);
  }  
  
  /** The outputPathDirectory used
   * 
   * @return directory used
   */
  public String getOutputPathDirectory() {
    return this.outputDirectory;
  }
  
  /** Set the outputDirectory used
   * 
   * @param outputDirectory to use
   */
  public void setOutputDirectory(String outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  /** Collect country name used
   * 
   * @return country name
   */
  public String getCountry() {
    return countryName;
  }

  /** Set country name used
   * 
   * @param countryName to use
   */
  public void setCountry(String countryName) {
    this.countryName = countryName;
  }

  /** Collect the file name to use
   * 
   * @return file name to use
   */
  public String getFileName() {
    return fileName;
  }

  /** Set the file name to use
   * 
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

  /** Collect number of decimals used in writing coordinates
   * 
   * @return number of decimals used
   */
  public DecimalFormat getDecimalFormat() {
    return decimalFormat;
  }

  /** Set number of decimals used in writing coordinates
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
  
  
  /** Collect the destination Crs
   * 
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
    this.outputDirectory = null;
    this.fileName = null;
    this.destinationCoordinateReferenceSystem = null;
  }  
    
}
