package org.goplanit.io.converter.service;

import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.io.xml.util.PlanitXmlReaderSettings;
import org.goplanit.network.MacroscopicNetwork;

/**
 * Configurable settings for the PLANit service network reader
 * 
 * @author markr
 *
 */
public class PlanitServiceNetworkReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {

  /**
   * Constructor.
   *
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   */
  public PlanitServiceNetworkReaderSettings(final String inputPathDirectory, final String xmlFileExtension) {
    super(inputPathDirectory, xmlFileExtension);
  }

  /**
   * Constructor
   */
  public PlanitServiceNetworkReaderSettings() {
    super();
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
  } 

}
