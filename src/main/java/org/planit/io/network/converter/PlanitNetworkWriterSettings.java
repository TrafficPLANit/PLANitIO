package org.planit.io.network.converter;

import java.text.DecimalFormat;
import org.planit.utils.math.Precision;
import org.planit.utils.misc.CharacterUtils;

/**
 * configurable settings for the PLANit network writer
 * 
 * @author markr
 *
 */
public class PlanitNetworkWriterSettings {
  
  /**
   * number of decimals to use, default is {@link Precision.DEFAULT_DECIMAL_FORMAT}
   */
  protected DecimalFormat decimalFormat = Precision.DEFAULT_DECIMAL_FORMAT;
  
  /** tuple separator, default is {@Link CharacterUtils.SPACE} */
  protected Character tupleSeparator = CharacterUtils.SPACE;

  /** tuple separator, default is {@Link CharacterUtils.COMMA} */
  protected Character commaSeparator = CharacterUtils.COMMA;
  
  /** decimal separator, default is {@Link CharacterUtils.DECIMAL_POINT} */
  protected Character decimalSeparator = CharacterUtils.DECIMAL_POINT;  

  /** collect number of decimals used in writing coordinates
   * @return number of decimals used
   */
  public DecimalFormat getDecimalFormat() {
    return decimalFormat;
  }

  /** set number of decimals used in writing coordinates
   * 
   * @param coordinateDecimals number of decimals
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
  
}
