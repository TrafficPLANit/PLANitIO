package org.goplanit.io.converter.service;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;

import java.util.logging.Logger;

/**
 * configurable settings for the PLANit routed services writer
 * 
 * @author markr
 *
 */
public class PlanitRoutedServicesWriterSettings extends PlanitXmlWriterSettings implements ConverterWriterSettings {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PlanitRoutedServicesWriterSettings.class.getCanonicalName());

  /**
   * Default constructor
   */
  public PlanitRoutedServicesWriterSettings() {
    super();
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param countryName to use (not used as long as service network has no explicit geo locations embedded)
   */
  public PlanitRoutedServicesWriterSettings(final String outputPathDirectory, final String countryName) {
    super(outputPathDirectory, countryName);
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param fileName to use
   * @param countryName to use (not used as long as service network has no explicit geo locations embedded)
   */
  public PlanitRoutedServicesWriterSettings(String outputPathDirectory, final String fileName, String countryName) {
    super(outputPathDirectory, fileName, countryName);
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
  }  
  
}
