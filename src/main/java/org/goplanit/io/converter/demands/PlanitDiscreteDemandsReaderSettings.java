package org.goplanit.io.converter.demands;

import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.io.xml.util.PlanitXmlReaderSettings;
import org.goplanit.utils.misc.LoggingUtils;

import java.util.logging.Logger;

/**
 * Settings for the PLANit discrete demands reader
 * 
 * @author markr
 *
 */
public class PlanitDiscreteDemandsReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitDiscreteDemandsReaderSettings.class.getCanonicalName());

  /**
   * Constructor
   */
  public PlanitDiscreteDemandsReaderSettings(){
    super();
  }

  /**
   * Constructor with input dir
   *
   * @param inputDirectory to use
   */
  public PlanitDiscreteDemandsReaderSettings(final String inputDirectory){
    super(inputDirectory);
  }

  /**
   * Constructor with input dir and file ext
   *
   * @param inputDirectory to use
   * @param xmlFileExtension to use
   */
  public PlanitDiscreteDemandsReaderSettings(final String inputDirectory, final String xmlFileExtension) {
    super(inputDirectory, xmlFileExtension);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings() {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Discrete Demands Reader Settings"));
    super.logSettings();
  }

}
