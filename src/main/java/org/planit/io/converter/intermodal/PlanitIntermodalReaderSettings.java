package org.planit.io.converter.intermodal;

import org.planit.converter.ConverterReaderSettings;
import org.planit.io.converter.network.PlanitNetworkReaderSettings;
import org.planit.io.converter.zoning.PlanitZoningReaderSettings;

/**
 * Settings of PLANit intermodal reader
 * 
 * @author markr
 *
 */
public class PlanitIntermodalReaderSettings implements ConverterReaderSettings {
  
  /** the network settings to use */
  protected final PlanitNetworkReaderSettings networkSettings;
  
  /** the zoning settings to use */
  protected final PlanitZoningReaderSettings zoningSettings;  

  /**
   * Default constructor
   */
  public PlanitIntermodalReaderSettings() {
    this(new PlanitNetworkReaderSettings(),new PlanitZoningReaderSettings());
  }
  
  /**
   * Constructor
   * 
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   */
  public PlanitIntermodalReaderSettings(final String inputPathDirectory, final String xmlFileExtension) {
    this(new PlanitNetworkReaderSettings(inputPathDirectory, xmlFileExtension), new PlanitZoningReaderSettings(inputPathDirectory, xmlFileExtension));
  }      
  
  /**
   * Constructor
   * 
   * @param networkSettings to use
   * @param zoningSettings to use
   */
  public PlanitIntermodalReaderSettings(final PlanitNetworkReaderSettings networkSettings, final PlanitZoningReaderSettings zoningSettings) {
    this.networkSettings = networkSettings;
    this.zoningSettings = zoningSettings;
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    getNetworkSettings().reset();
    getZoningSettings().reset();
  }   
  
  /** provide access to the network reader settings
   * @return network reader settings
   */
  public PlanitNetworkReaderSettings getNetworkSettings() {
    return networkSettings;
  }
  
  /** provide access to the zoning reader settings
   * @return zoning reader settings
   */
  public PlanitZoningReaderSettings getZoningSettings() {
    return zoningSettings;
  }
  
  /** set the input path directory used for both zoning and network settings
   * @param inputDirectory to use
   */
  public void setInputDirectory(String inputDirectory) {
    getNetworkSettings().setInputDirectory(inputDirectory);
    getZoningSettings().setInputDirectory(inputDirectory);
  }    

 
}
