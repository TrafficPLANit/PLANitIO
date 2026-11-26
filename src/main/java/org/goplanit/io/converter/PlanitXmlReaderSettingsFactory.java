package org.goplanit.io.converter;

import java.util.List;

import org.goplanit.io.converter.demands.PlanitDemandsReaderSettings;
import org.goplanit.io.converter.demands.PlanitDiscreteDemandsReaderSettings;
import org.goplanit.io.converter.intermodal.PlanitIntermodalReaderSettings;
import org.goplanit.io.converter.network.PlanitNetworkReaderSettings;
import org.goplanit.io.converter.service.PlanitRoutedServicesReaderSettings;
import org.goplanit.io.converter.service.PlanitServiceNetworkReaderSettings;
import org.goplanit.io.converter.zoning.PlanitZoningReaderSettings;
import org.goplanit.utils.exceptions.SettingsProblemsException;
import org.goplanit.utils.settings.SettingsSource;
import org.goplanit.utils.settings.SettingsYamlLoader;

/**
 * Makes the settings of each PLANit reader from YAML settings files and text
 * <p>
 * For example, network reader settings made from a file, then a reader given them:
 * </p>
 * <pre>
 * var settings = PlanitXmlReaderSettingsFactory.createNetworkReaderSettingsFromYaml(
 *     List.of(SettingsSource.ofFile(Path.of("network_reader.yaml"))));
 * var reader = PlanitNetworkReaderFactory.create(settings);
 * </pre>
 * <p>
 * It is the one place to find how PLANit reader settings are made from files. The sources follow
 * {@link PlanitXmlReaderSettings#SCHEMA} and apply in order, a later source winning for a replacing key. Each reader has
 * a file of its own; the intermodal reader's file names the files of the readers it holds. Every problem in the
 * sources is reported together, see {@link SettingsProblemsException}. The settings made are ordinary settings, so
 * nothing that takes settings changes, and code changing them afterwards wins. Settings made in code keep using the
 * constructors of the settings and the readers' own factories.
 * </p>
 *
 * @author markr
 */
public final class PlanitXmlReaderSettingsFactory {

  /** Not instantiable, everything is static */
  private PlanitXmlReaderSettingsFactory() {
  }

  /**
   * Network reader settings made from YAML sources
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources
   */
  public static PlanitNetworkReaderSettings createNetworkReaderSettingsFromYaml(List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlReaderSettings.SCHEMA, sources)
        .create(PlanitNetworkReaderSettings.class);
  }

  /**
   * Zoning reader settings made from YAML sources
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources
   */
  public static PlanitZoningReaderSettings createZoningReaderSettingsFromYaml(List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlReaderSettings.SCHEMA, sources)
        .create(PlanitZoningReaderSettings.class);
  }

  /**
   * Demands reader settings made from YAML sources
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources
   */
  public static PlanitDemandsReaderSettings createDemandsReaderSettingsFromYaml(List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlReaderSettings.SCHEMA, sources)
        .create(PlanitDemandsReaderSettings.class);
  }

  /**
   * Discrete demands reader settings made from YAML sources
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources
   */
  public static PlanitDiscreteDemandsReaderSettings createDiscreteDemandsReaderSettingsFromYaml(
      List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlReaderSettings.SCHEMA, sources)
        .create(PlanitDiscreteDemandsReaderSettings.class);
  }

  /**
   * Service network reader settings made from YAML sources
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources
   */
  public static PlanitServiceNetworkReaderSettings createServiceNetworkReaderSettingsFromYaml(
      List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlReaderSettings.SCHEMA, sources)
        .create(PlanitServiceNetworkReaderSettings.class);
  }

  /**
   * Routed services reader settings made from YAML sources
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources
   */
  public static PlanitRoutedServicesReaderSettings createRoutedServicesReaderSettingsFromYaml(
      List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlReaderSettings.SCHEMA, sources)
        .create(PlanitRoutedServicesReaderSettings.class);
  }

  /**
   * Intermodal reader settings made from YAML sources, each reader it holds made from the file a source names for it
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources and the files they name
   */
  public static PlanitIntermodalReaderSettings createIntermodalReaderSettingsFromYaml(List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlReaderSettings.SCHEMA, sources)
        .create(PlanitIntermodalReaderSettings.class);
  }
}
