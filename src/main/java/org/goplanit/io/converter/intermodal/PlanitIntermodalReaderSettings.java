package org.goplanit.io.converter.intermodal;

import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.io.converter.network.PlanitNetworkReaderSettings;
import org.goplanit.io.converter.service.PlanitRoutedServicesReaderSettings;
import org.goplanit.io.converter.service.PlanitServiceNetworkReader;
import org.goplanit.io.converter.service.PlanitServiceNetworkReaderSettings;
import org.goplanit.io.converter.zoning.PlanitZoningReaderSettings;

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

  /** the service settings to use, only relevant when reading with services */
  protected final PlanitServiceNetworkReaderSettings serviceNetworkSettings;

  /** the service settings to use, only relevant when reading with services */
  protected final PlanitRoutedServicesReaderSettings routedServicesSettings;

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
   */
  public PlanitIntermodalReaderSettings(final String inputPathDirectory) {
    this(new PlanitNetworkReaderSettings(inputPathDirectory), new PlanitZoningReaderSettings(inputPathDirectory));
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
  public PlanitIntermodalReaderSettings(
      final PlanitNetworkReaderSettings networkSettings,
      final PlanitZoningReaderSettings zoningSettings) {
    this(
        networkSettings,
        zoningSettings,
        new PlanitServiceNetworkReaderSettings(networkSettings.getInputDirectory(), networkSettings.getXmlFileExtension()),
        new PlanitRoutedServicesReaderSettings(networkSettings.getInputDirectory(), networkSettings.getXmlFileExtension()));
  }

  /**
   * Constructor
   *
   * @param networkSettings to use
   * @param zoningSettings to use
   * @param serviceNetworkSettings to use
   * @param routedServicesSettings to use
   */
  public PlanitIntermodalReaderSettings(
      final PlanitNetworkReaderSettings networkSettings,
      final PlanitZoningReaderSettings zoningSettings,
      final PlanitServiceNetworkReaderSettings serviceNetworkSettings,
      final PlanitRoutedServicesReaderSettings routedServicesSettings) {
    this.networkSettings = networkSettings;
    this.zoningSettings = zoningSettings;
    this.serviceNetworkSettings = serviceNetworkSettings;
    this.routedServicesSettings = routedServicesSettings;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    getNetworkSettings().reset();
    getZoningSettings().reset();
    getServiceNetworkSettings().reset();
    getRoutedServicesSettings().reset();
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

  /** provide access to the routed services settings
   * @return routed services settings
   */
  public PlanitRoutedServicesReaderSettings getRoutedServicesSettings() {
    return routedServicesSettings;
  }

  /** provide access to the servicesNetworkSettings
   * @return servicesNetworkSettings
   */
  public PlanitServiceNetworkReaderSettings getServiceNetworkSettings() {
    return serviceNetworkSettings;
  }
  
  /** set the input path directory used for all underlying component settings
   * @param inputDirectory to use
   */
  public void setInputDirectory(String inputDirectory) {
    getNetworkSettings().setInputDirectory(inputDirectory);
    getZoningSettings().setInputDirectory(inputDirectory);
    getServiceNetworkSettings().setInputDirectory(inputDirectory);
    getRoutedServicesSettings().setInputDirectory(inputDirectory);

  }    

 
}
