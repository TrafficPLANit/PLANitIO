package org.goplanit.io.converter.demands;

import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.io.xml.util.PlanitXmlReaderSettings;

/**
 * Settings for the PLANit discrete demands reader
 * 
 * @author markr
 *
 */
public class PlanitDiscreteDemandsReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {

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

}
