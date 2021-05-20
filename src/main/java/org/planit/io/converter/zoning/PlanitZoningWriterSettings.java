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
   * Constructor
   * 
   * @param outputPathDirectory to use
   */
  public PlanitZoningWriterSettings(final String outputPathDirectory) {
    super(outputPathDirectory);
  }
  
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  public PlanitZoningWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, null, countryName);
  }  
  
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   * @param outputFileName to use
   * @param countryName to use
   */
  public PlanitZoningWriterSettings(final String outputPathDirectory, final String outputFileName, final String countryName) {
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
