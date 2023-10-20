package org.goplanit.io.converter.zoning;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;

/**
 * Settings for the PLANit zoning writer
 * 
 * @author markr
 *
 */
public class PlanitZoningWriterSettings extends PlanitXmlWriterSettings implements ConverterWriterSettings {


  /** default zoning file name to use */
  public static final String DEFAULT_ZONING_XML = "zoning.xml";

  /**
   * Default constructor
   */
  public PlanitZoningWriterSettings() {
    super();
    setFileName(DEFAULT_ZONING_XML);
  }
  
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   */
  public PlanitZoningWriterSettings(final String outputPathDirectory) {
    super(outputPathDirectory);
    setFileName(DEFAULT_ZONING_XML);
  }
  
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   * @param countryName to use
   */
  public PlanitZoningWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, DEFAULT_ZONING_XML, countryName);
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
