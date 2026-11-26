package org.goplanit.io.converter.intermodal;

import org.goplanit.converter.ConverterReaderSettings;
import org.goplanit.io.converter.network.PlanitNetworkReaderSettings;
import org.goplanit.io.converter.service.PlanitRoutedServicesReaderSettings;
import org.goplanit.io.converter.service.PlanitServiceNetworkReader;
import org.goplanit.io.converter.service.PlanitServiceNetworkReaderSettings;
import org.goplanit.io.converter.zoning.PlanitZoningReaderSettings;
import org.goplanit.io.converter.PlanitXmlReaderSettingsSchema;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.settings.SettingsSchema;
import org.goplanit.utils.settings.YamlWriteSettingsSupport;
import org.goplanit.utils.settings.annotations.DelegatedToSyncedInternalSettingsKeys;
import org.goplanit.utils.settings.annotations.SettingsFileReference;
import org.goplanit.utils.settings.annotations.SettingsKeyLevel;

import java.util.List;
import java.util.logging.Logger;

/**
 * Settings of PLANit intermodal reader
 * <p>
 * Its settings file names the settings files of the readers it holds, each relative to it, and holds none of their
 * keys, such as
 * </p>
 * <pre>
 * planitIoReaderSettings: 1
 * network: network.yaml
 * zoning: zoning.yaml
 * </pre>
 * <p>
 * Each reader is made from its file as it would be on its own, so a zoning reader made on its own reads the same
 * file; a reader whose file is not named keeps its defaults.
 * </p>
 *
 * @author markr
 *
 */
public class PlanitIntermodalReaderSettings implements ConverterReaderSettings, YamlWriteSettingsSupport {

  private static final Logger LOGGER = Logger.getLogger(PlanitIntermodalReaderSettings.class.getCanonicalName());

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
    this(
        new PlanitNetworkReaderSettings(inputPathDirectory, xmlFileExtension),
        new PlanitZoningReaderSettings(inputPathDirectory, xmlFileExtension));
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
        new PlanitServiceNetworkReaderSettings(),
        new PlanitRoutedServicesReaderSettings());
    for (var partSettings : List.of(serviceNetworkSettings, routedServicesSettings)) {
      partSettings.setInputSource(networkSettings.getInputSource());
      partSettings.setXmlFileExtension(networkSettings.getXmlFileExtension());
    }
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings(int level) {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Intermodal Reader Settings"));
    getNetworkSettings().logSettings(level);
    getZoningSettings().logSettings(level);
    getServiceNetworkSettings().logSettings(level);
    getRoutedServicesSettings().logSettings(level);
  }

  /** provide access to the network reader settings
   * @return network reader settings
   */
  @SettingsFileReference(key = "network", level = SettingsKeyLevel.STANDARD,
      description = "The settings file of the network reader, relative to this settings file")
  public PlanitNetworkReaderSettings getNetworkSettings() {
    return networkSettings;
  }

  /** provide access to the zoning reader settings
   * @return zoning reader settings
   */
  @SettingsFileReference(key = "zoning", level = SettingsKeyLevel.STANDARD,
      description = "The settings file of the zoning reader, relative to this settings file")
  public PlanitZoningReaderSettings getZoningSettings() {
    return zoningSettings;
  }

  /** provide access to the routed services settings
   * @return routed services settings
   */
  @SettingsFileReference(key = "routedServices", level = SettingsKeyLevel.UNCOMMON,
      description = "The settings file of the routed services reader, relative to this settings file")
  public PlanitRoutedServicesReaderSettings getRoutedServicesSettings() {
    return routedServicesSettings;
  }

  /** provide access to the servicesNetworkSettings
   * @return servicesNetworkSettings
   */
  @SettingsFileReference(key = "serviceNetwork", level = SettingsKeyLevel.UNCOMMON,
      description = "The settings file of the service network reader, relative to this settings file")
  public PlanitServiceNetworkReaderSettings getServiceNetworkSettings() {
    return serviceNetworkSettings;
  }

  /** set the input path directory used for all underlying component settings
   * @param inputDirectory to use
   */
  @DelegatedToSyncedInternalSettingsKeys("inputSource")
  public void setInputDirectory(String inputDirectory) {
    getNetworkSettings().setInputDirectory(inputDirectory);
    getZoningSettings().setInputDirectory(inputDirectory);
    getServiceNetworkSettings().setInputDirectory(inputDirectory);
    getRoutedServicesSettings().setInputDirectory(inputDirectory);

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SettingsSchema getSettingsSchema() {
    return PlanitXmlReaderSettingsSchema.SCHEMA;
  }

}
