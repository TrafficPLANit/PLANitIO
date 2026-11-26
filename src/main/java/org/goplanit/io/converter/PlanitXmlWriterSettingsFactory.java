package org.goplanit.io.converter;

import java.util.List;

import org.goplanit.io.converter.demands.PlanitDemandsWriterSettings;
import org.goplanit.io.converter.demands.PlanitDiscreteDemandsWriterSettings;
import org.goplanit.io.converter.intermodal.PlanitIntermodalWriterSettings;
import org.goplanit.io.converter.network.PlanitNetworkWriterSettings;
import org.goplanit.io.converter.service.PlanitRoutedServicesWriterSettings;
import org.goplanit.io.converter.service.PlanitServiceNetworkWriterSettings;
import org.goplanit.io.converter.zoning.PlanitZoningWriterSettings;
import org.goplanit.utils.exceptions.SettingsProblemsException;
import org.goplanit.utils.settings.SettingsSource;
import org.goplanit.utils.settings.SettingsYamlLoader;

/**
 * Makes the settings of each PLANit writer from YAML settings files and text
 * <p>
 * For example, zoning writer settings made from a file, then a writer given them:
 * </p>
 * <pre>
 * var settings = PlanitXmlWriterSettingsFactory.createZoningWriterSettingsFromYaml(
 *     List.of(SettingsSource.ofFile(Path.of("zoning_writer.yaml"))));
 * var writer = PlanitZoningWriterFactory.create(settings, network);
 * </pre>
 * <p>
 * It is the one place to find how PLANit writer settings are made from files. The sources follow
 * {@link PlanitXmlWriterSettingsSchema} and apply in order, a later source winning for a replacing key. Each writer has
 * a file of its own; the intermodal writer's file names the files of the writers it holds. Every problem in the
 * sources is reported together, see {@link SettingsProblemsException}. The settings made are ordinary settings, so
 * nothing that takes settings changes, and code changing them afterwards wins. Settings made in code keep using the
 * constructors of the settings and the writers' own factories.
 * </p>
 *
 * @author markr
 */
public final class PlanitXmlWriterSettingsFactory {

  /** Not instantiable, everything is static */
  private PlanitXmlWriterSettingsFactory() {
  }

  /**
   * Network writer settings made from YAML sources
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources
   */
  public static PlanitNetworkWriterSettings createNetworkWriterSettingsFromYaml(List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlWriterSettingsSchema.SCHEMA, sources)
        .create(PlanitNetworkWriterSettings.class);
  }

  /**
   * Zoning writer settings made from YAML sources
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources
   */
  public static PlanitZoningWriterSettings createZoningWriterSettingsFromYaml(List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlWriterSettingsSchema.SCHEMA, sources)
        .create(PlanitZoningWriterSettings.class);
  }

  /**
   * Demands writer settings made from YAML sources
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources
   */
  public static PlanitDemandsWriterSettings createDemandsWriterSettingsFromYaml(List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlWriterSettingsSchema.SCHEMA, sources)
        .create(PlanitDemandsWriterSettings.class);
  }

  /**
   * Discrete demands writer settings made from YAML sources
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources
   */
  public static PlanitDiscreteDemandsWriterSettings createDiscreteDemandsWriterSettingsFromYaml(
      List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlWriterSettingsSchema.SCHEMA, sources)
        .create(PlanitDiscreteDemandsWriterSettings.class);
  }

  /**
   * Service network writer settings made from YAML sources
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources
   */
  public static PlanitServiceNetworkWriterSettings createServiceNetworkWriterSettingsFromYaml(
      List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlWriterSettingsSchema.SCHEMA, sources)
        .create(PlanitServiceNetworkWriterSettings.class);
  }

  /**
   * Routed services writer settings made from YAML sources
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources
   */
  public static PlanitRoutedServicesWriterSettings createRoutedServicesWriterSettingsFromYaml(
      List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlWriterSettingsSchema.SCHEMA, sources)
        .create(PlanitRoutedServicesWriterSettings.class);
  }

  /**
   * Intermodal writer settings made from YAML sources, each writer it holds made from the file a source names for it
   *
   * @param sources in the order they apply
   * @return the settings
   * @throws SettingsProblemsException holding every problem found in the sources and the files they name
   */
  public static PlanitIntermodalWriterSettings createIntermodalWriterSettingsFromYaml(List<SettingsSource> sources) {
    return new SettingsYamlLoader(PlanitXmlWriterSettingsSchema.SCHEMA, sources)
        .create(PlanitIntermodalWriterSettings.class);
  }
}
