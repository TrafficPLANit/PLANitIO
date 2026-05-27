package org.goplanit.io.converter.demands;

import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.io.xml.util.PlanitXmlReaderSettings;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.zoning.Zoning;

/**
 * Settings for the PLANit demands reader
 * 
 * @author markr
 *
 */
public class PlanitDemandsReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {

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

}
