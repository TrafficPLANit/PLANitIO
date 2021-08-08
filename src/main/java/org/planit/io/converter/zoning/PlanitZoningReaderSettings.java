package org.planit.io.converter.zoning;

import org.planit.converter.ConverterReaderSettings;
import org.planit.io.xml.util.PlanitXmlReaderSettings;

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
   *  @param xmlFileExtension to use
   */
  public PlanitZoningReaderSettings(final String inputPathDirectory, final String xmlFileExtension) {
    super(inputPathDirectory, xmlFileExtension);
  }  
    

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
  }
  
  // GETTERS/SETTERS
     
}
