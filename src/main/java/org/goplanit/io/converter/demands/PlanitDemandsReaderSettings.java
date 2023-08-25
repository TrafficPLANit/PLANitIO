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
   * {@inheritDoc}
   */
  public PlanitDemandsReaderSettings(){
    super();
  }

  /**
   * {@inheritDoc}
   */
  public PlanitDemandsReaderSettings(final String inputDirectory){
    super(inputDirectory);
  }

  /**
   * {@inheritDoc}
   */
  public PlanitDemandsReaderSettings(final String inputDirectory, final String xmlFileExtension) {
    super(inputDirectory, xmlFileExtension);
  }

}
