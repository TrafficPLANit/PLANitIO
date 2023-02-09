package org.goplanit.io.converter.intermodal;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.converter.network.PlanitNetworkWriterSettings;
import org.goplanit.io.converter.service.PlanitRoutedServicesWriterSettings;
import org.goplanit.io.converter.service.PlanitServiceNetworkWriterSettings;
import org.goplanit.io.converter.zoning.PlanitZoningWriterSettings;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Settings for PLANit intermodal writer
 * 
 * @author markr
 *
 */
public class PlanitIntermodalWriterSettings implements ConverterWriterSettings {

  /** the network settings to use */
  protected final PlanitNetworkWriterSettings networkSettings;
  
  /** the zoning settings to use */
  protected final PlanitZoningWriterSettings zoningSettings;

  /** the service network settings to use */
  protected final PlanitServiceNetworkWriterSettings serviceNetworkSettings;

  /** the routed services settings to use */
  protected final PlanitRoutedServicesWriterSettings routedServicesSettings;

  /**
   * Default constructor
   */
  public PlanitIntermodalWriterSettings() {
    this( new PlanitNetworkWriterSettings(),
          new PlanitZoningWriterSettings(),
          new PlanitServiceNetworkWriterSettings(),
          new PlanitRoutedServicesWriterSettings());
  }
  
  /**
   * Constructor
   * 
   * @param outputDirectory to use
   * @param countryName to use
   */
  public PlanitIntermodalWriterSettings(final String outputDirectory, final String countryName) {
    this(
            new PlanitNetworkWriterSettings(outputDirectory, countryName),
            new PlanitZoningWriterSettings(outputDirectory, countryName),
            new PlanitServiceNetworkWriterSettings(outputDirectory, countryName),
            new PlanitRoutedServicesWriterSettings(outputDirectory, countryName));
  }      
  
  /**
   * Constructor
   * 
   * @param networkSettings to use
   * @param zoningSettings to use
   * @param serviceNetworkSettings to use
   * @param routedServicesSettings to use
   */
  public PlanitIntermodalWriterSettings(
          final PlanitNetworkWriterSettings networkSettings,
          final PlanitZoningWriterSettings zoningSettings,
          final PlanitServiceNetworkWriterSettings serviceNetworkSettings,
          final PlanitRoutedServicesWriterSettings routedServicesSettings) {
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

  /** Collect zoning settings
   * 
   * @return zoning settings
   */
  public PlanitZoningWriterSettings getZoningSettings() {
    return zoningSettings;
  }

  /** Collect network settings
   * 
   * @return network settings
   */
  public  PlanitNetworkWriterSettings getNetworkSettings() {
    return networkSettings;
  }

  /** Collect service network settings
   *
   * @return service network settings
   */
  public  PlanitServiceNetworkWriterSettings getServiceNetworkSettings() {
    return serviceNetworkSettings;
  }

  /** Collect routed services settings
   *
   * @return routed services settings
   */
  public  PlanitRoutedServicesWriterSettings getRoutedServicesSettings() {
    return routedServicesSettings;
  }

  /** Set the outputPathDirectory used on both zoning and (service) network settings
   * 
   * @param outputDirectory to use
   */
  public void setOutputDirectory(String outputDirectory) {
    getZoningSettings().setOutputDirectory(outputDirectory);
    getNetworkSettings().setOutputDirectory(outputDirectory);
    getServiceNetworkSettings().setOutputDirectory(outputDirectory);
    getRoutedServicesSettings().setOutputDirectory(outputDirectory);
  }

  /** Set country name used on both zoning and (service) network settings
   * 
   * @param countryName to use
   */
  public void setCountry(String countryName) {
    getZoningSettings().setCountry(countryName);
    getNetworkSettings().setCountry(countryName);
    getServiceNetworkSettings().setCountry(countryName);
    getRoutedServicesSettings().setCountry(countryName);
  }
  
  /** Set the destination Crs to use (if not set, network's native Crs will be used, unless the user has specified a
   * specific country for which we have a more appropriate Crs registered) 
   * 
   * @param destinationCoordinateReferenceSystem to use
   */
  public void setDestinationCoordinateReferenceSystem(CoordinateReferenceSystem destinationCoordinateReferenceSystem) {
    getZoningSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
    getNetworkSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
    getServiceNetworkSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
    getRoutedServicesSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
  }  
  
}
