package org.goplanit.io.converter.zoning;

import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.io.xml.util.PlanitXmlReaderSettings;
import org.goplanit.utils.misc.LoggingUtils;

import java.util.logging.Logger;

/**
 * Settings for the PLANit zoning reader
 * 
 * @author markr
 *
 */
public class PlanitZoningReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitZoningReaderSettings.class.getCanonicalName());
    
  /**
   * Default constructor
   */
  public PlanitZoningReaderSettings() {
    super();
  }

  /**
   * Constructor
   *
   *  @param inputPathDirectory to use
   */
  public PlanitZoningReaderSettings(final String inputPathDirectory) {
    super(inputPathDirectory);
  }

  /**
   * Constructor
   * 
   *  @param inputPathDirectory to use
   *  @param xmlFileExtension to use
   */
  public PlanitZoningReaderSettings(final String inputPathDirectory, final String xmlFileExtension) {
    super(inputPathDirectory, xmlFileExtension);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings() {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Zoning Reader Settings"));
    super.logSettings();
  }

  // GETTERS/SETTERS
     
}
