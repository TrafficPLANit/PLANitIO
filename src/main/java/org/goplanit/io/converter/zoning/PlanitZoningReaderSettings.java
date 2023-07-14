package org.goplanit.io.converter.zoning;

import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.io.xml.util.PlanitXmlReaderSettings;

/**
 * Settings for the PLANit zoning reader
 * 
 * @author markr
 *
 */
public class PlanitZoningReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {
    
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

  // GETTERS/SETTERS
     
}
