package org.goplanit.io.converter.intermodal;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.converter.network.PlanitNetworkWriterSettings;
import org.goplanit.io.converter.service.PlanitRoutedServicesWriterSettings;
import org.goplanit.io.converter.service.PlanitServiceNetworkWriterSettings;
import org.goplanit.io.converter.zoning.PlanitZoningWriterSettings;
import org.goplanit.io.converter.PlanitXmlWriterSettings;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.settings.SettingsSchema;
import org.goplanit.utils.settings.YamlWriteSettingsSupport;
import org.goplanit.utils.settings.annotations.DelegatedToSyncedInternalSettingsKeys;
import org.goplanit.utils.settings.annotations.SettingsFileReference;
import org.goplanit.utils.settings.annotations.SettingsKeyLevel;

import java.util.logging.Logger;

/**
 * Settings for PLANit intermodal writer
 * <p>
 * Its settings file names the settings files of the writers it holds, each relative to it, and holds none of their
 * keys, such as
 * </p>
 * <pre>
 * planitIoWriterSettings: 1
 * network: network_writer.yaml
 * zoning: zoning_writer.yaml
 * </pre>
 * <p>
 * Each writer is made from its file as it would be on its own, so a value every writer shares, such as the output
 * directory, is given in each file; a writer whose file is not named keeps its defaults. Written out, it writes its
 * file and one file per writer next to it, named after the key.
 * </p>
 *
 * @author markr
 *
 */
public class PlanitIntermodalWriterSettings implements ConverterWriterSettings, YamlWriteSettingsSupport {

  private static final Logger LOGGER = Logger.getLogger(PlanitIntermodalWriterSettings.class.getCanonicalName());

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

  /**
   * {@inheritDoc}
   */
  public void logSettings(int level) {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Intermodal Writer Settings"));
    getNetworkSettings().logSettings(level);
    getZoningSettings().logSettings(level);
    getServiceNetworkSettings().logSettings(level);
    getRoutedServicesSettings().logSettings(level);
  }

  /** Collect zoning settings
   * 
   * @return zoning settings
   */
  @SettingsFileReference(key = "zoning", level = SettingsKeyLevel.STANDARD,
      description = "The settings file of the zoning writer, relative to this settings file")
  public PlanitZoningWriterSettings getZoningSettings() {
    return zoningSettings;
  }

  /** Collect network settings
   *
   * @return network settings
   */
  @SettingsFileReference(key = "network", level = SettingsKeyLevel.STANDARD,
      description = "The settings file of the network writer, relative to this settings file")
  public  PlanitNetworkWriterSettings getNetworkSettings() {
    return networkSettings;
  }

  /** Collect service network settings
   *
   * @return service network settings
   */
  @SettingsFileReference(key = "serviceNetwork", level = SettingsKeyLevel.UNCOMMON,
      description = "The settings file of the service network writer, relative to this settings file")
  public  PlanitServiceNetworkWriterSettings getServiceNetworkSettings() {
    return serviceNetworkSettings;
  }

  /** Collect routed services settings
   *
   * @return routed services settings
   */
  @SettingsFileReference(key = "routedServices", level = SettingsKeyLevel.UNCOMMON,
      description = "The settings file of the routed services writer, relative to this settings file")
  public  PlanitRoutedServicesWriterSettings getRoutedServicesSettings() {
    return routedServicesSettings;
  }

  /** Set the outputPathDirectory used on both zoning and (service) network settings
   *
   * @param outputDirectory to use
   */
  @DelegatedToSyncedInternalSettingsKeys("outputDirectory")
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
  @DelegatedToSyncedInternalSettingsKeys("country")
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
  @DelegatedToSyncedInternalSettingsKeys("destinationCrs")
  public void setDestinationCoordinateReferenceSystem(CoordinateReferenceSystem destinationCoordinateReferenceSystem) {
    getZoningSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
    getNetworkSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
    getServiceNetworkSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
    getRoutedServicesSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SettingsSchema getSettingsSchema() {
    return PlanitXmlWriterSettings.SCHEMA;
  }

}
