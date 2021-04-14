package org.planit.io.converter.network;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.math.Precision;
import org.planit.utils.misc.CharacterUtils;

/**
 * configurable settings for the PLANit writer
 * 
 * @author markr
 *
 */
public class PlanitWriterSettings {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitWriterSettings.class.getCanonicalName());
  
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
  
  /** the country */
  protected String countryName = null;  
  
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
  
  /**
   * set the country name to optimise projection if possible
   * 
   * @param countryName to use
   */
  public void setCountryName(String countryName) {
    this.countryName = countryName;
  }  

  /**
   * collect the country name set
   * 
   * @return used country
   */
  public String getCountryName() {
    return countryName;
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
  
}
