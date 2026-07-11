package org.goplanit.io.converter.zoning;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;
import org.goplanit.utils.misc.LoggingUtils;

import java.util.logging.Logger;

/**
 * Settings for the PLANit zoning writer
 * 
 * @author markr
 *
 */
public class PlanitZoningWriterSettings extends PlanitXmlWriterSettings implements ConverterWriterSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitZoningWriterSettings.class.getCanonicalName());


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
  public PlanitZoningWriterSettings(
      final String outputPathDirectory, final String outputFileName, final String countryName) {
    super(outputPathDirectory, outputFileName, countryName);
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
  public void logSettings() {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Zoning Writer Settings"));
    super.logSettings();
  }
}
