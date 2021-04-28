package org.planit.io.converter.zoning;

import org.planit.converter.ConverterWriterSettings;
import org.planit.io.xml.util.PlanitXmlWriterSettings;

/**
 * Settings for the planit zoning writer
 * 
 * @author markr
 *
 */
public class PlanitZoningWriterSettings extends PlanitXmlWriterSettings implements ConverterWriterSettings {

  /**
   * Default constructor
   */
  public PlanitZoningWriterSettings() {
    super();
  }
  
  /**
   * Default constructor
   * 
   * @param outputPathDirectory
   */
  public PlanitZoningWriterSettings(String outputPathDirectory) {
    super(outputPathDirectory);
  }
  
  /**
   * Default constructor
   * 
   * @param outputPathDirectory to use
   * @param outputFileName to use
   * @param countryName to use
   */
  public PlanitZoningWriterSettings(String outputPathDirectory, String outputFileName, String countryName) {
    super(outputPathDirectory, outputFileName, countryName);
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
  } 
}
