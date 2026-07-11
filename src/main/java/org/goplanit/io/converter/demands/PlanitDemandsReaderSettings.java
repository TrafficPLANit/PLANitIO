package org.goplanit.io.converter.demands;

import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.io.xml.util.PlanitXmlReaderSettings;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.zoning.Zoning;

import java.util.logging.Logger;

/**
 * Settings for the PLANit demands reader
 * 
 * @author markr
 *
 */
public class PlanitDemandsReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitDemandsReaderSettings.class.getCanonicalName());

  /**
   * Constructor
   */
  public PlanitDemandsReaderSettings(){
    super();
  }

  /**
   * Constructor with input dir
   *
   * @param inputDirectory to use
   */
  public PlanitDemandsReaderSettings(final String inputDirectory){
    super(inputDirectory);
  }

  /**
   * Constructor with input dir and file ext
   *
   * @param inputDirectory to use
   * @param xmlFileExtension to use
   */
  public PlanitDemandsReaderSettings(final String inputDirectory, final String xmlFileExtension) {
    super(inputDirectory, xmlFileExtension);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings() {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Demands Reader Settings"));
    super.logSettings();
  }

}
