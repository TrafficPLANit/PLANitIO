package org.goplanit.io.test.integration.converter;

import org.goplanit.io.converter.PlanitXmlReaderSettingsFactory;
import org.goplanit.io.converter.PlanitXmlWriterSettingsFactory;
import org.goplanit.io.converter.demands.PlanitDemandsReaderSettings;
import org.goplanit.io.converter.demands.PlanitDemandsWriterFactory;
import org.goplanit.io.converter.demands.PlanitDemandsWriterSettings;
import org.goplanit.io.converter.demands.PlanitDiscreteDemandsReaderSettings;
import org.goplanit.io.converter.demands.PlanitDiscreteDemandsWriterSettings;
import org.goplanit.io.converter.intermodal.PlanitIntermodalWriterSettings;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.io.converter.network.PlanitNetworkReaderSettings;
import org.goplanit.io.converter.network.PlanitNetworkWriterSettings;
import org.goplanit.io.converter.pricing.PlanitNetworkPricingReaderFactory;
import org.goplanit.io.converter.pricing.PlanitNetworkPricingReaderSettings;
import org.goplanit.io.converter.pricing.PlanitNetworkPricingWriterSettings;
import org.goplanit.io.converter.service.PlanitRoutedServicesReaderSettings;
import org.goplanit.io.converter.service.PlanitRoutedServicesWriterFactory;
import org.goplanit.io.converter.service.PlanitRoutedServicesWriterSettings;
import org.goplanit.io.converter.service.PlanitServiceNetworkReaderSettings;
import org.goplanit.io.converter.service.PlanitServiceNetworkWriterFactory;
import org.goplanit.io.converter.service.PlanitServiceNetworkWriterSettings;
import org.goplanit.io.converter.zoning.PlanitZoningReaderSettings;
import org.goplanit.io.converter.zoning.PlanitZoningWriterFactory;
import org.goplanit.io.converter.zoning.PlanitZoningWriterSettings;
import org.goplanit.io.test.util.PlanItIOTestHelper;
import org.goplanit.logging.Logging;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.settings.SettingsFileContentType;
import org.goplanit.utils.settings.SettingsSource;
import org.goplanit.utils.settings.YamlWriteSettingsSupport;
import org.goplanit.utils.settings.problem.SettingsProblemKind;
import org.goplanit.utils.test.SettingsFileAssertionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the settings files of the PLANit readers and writers: every key read from a file and written out again, the
 * global defaults written out, the readers' files apart from the writers', stacked files, and intermodal files naming
 * the files of their nested settings
 *
 * @author markr
 */
public class PlanitXmlSettingsFilesTest {

  /**
   * One kind of reader or writer settings, with its two files: every key not at its default, and the global defaults
   *
   * @param <S> the type of the settings
   */
  private static final class SettingsKind<S extends YamlWriteSettingsSupport> {

    /** makes the settings from sources */
    private final Function<List<SettingsSource>, S> fromYaml;

    /** makes the settings at their defaults */
    private final Supplier<S> atDefaults;

    /** resets the settings */
    private final Consumer<S> reset;

    /** the file setting every key to a value other than its default */
    private final Path everyKeyNotDefaultFile;

    /** the file of every key at its global default */
    private final Path globalDefaultsFile;

    /**
     * Constructor
     *
     * @param fromYaml makes the settings from sources
     * @param atDefaults makes the settings at their defaults
     * @param reset resets the settings
     * @param everyKeyNotDefaultPrefix of the file of every key not at its default in the settings folder
     * @param globalDefaultsPrefix of the file of the global defaults in the settings folder
     */
    private SettingsKind(Function<List<SettingsSource>, S> fromYaml, Supplier<S> atDefaults, Consumer<S> reset,
        String everyKeyNotDefaultPrefix, String globalDefaultsPrefix) {
      this.fromYaml = fromYaml;
      this.atDefaults = atDefaults;
      this.reset = reset;
      this.everyKeyNotDefaultFile = SETTINGS_DIR.resolve(everyKeyNotDefaultPrefix + "_every_key_not_default.yaml");
      this.globalDefaultsFile = SETTINGS_DIR.resolve(globalDefaultsPrefix + "_global_defaults.yaml");
    }

    /**
     * Verify that settings made from the file of every key not at its default, then reset, write every key as the
     * settings at their defaults do
     *
     * @param folder to write the files to
     * @throws IOException when a file cannot be written or read
     */
    private void assertResetGivesSettingsAsMadeNew(Path folder) throws IOException {
      SettingsFileAssertionUtils.assertResetGivesSettingsAsMadeNew(
          fromYaml, reset, everyKeyNotDefaultFile, atDefaults.get(), folder);
    }
  }

  /** the folder of the settings files of each kind, absolute so a relative output directory in them resolves to one */
  private static final Path SETTINGS_DIR = Path.of("src", "test", "resources", "settings").toAbsolutePath();

  /** a folder holding a network, a zoning, a service network and routed services, each in a file of its own */
  private static final Path CONVERTER_TEST_INPUT =
      Path.of("src", "test", "resources", "testcases", "converter_test", "input");

  /** the network and pricing of the road pricing research note's example */
  private static final Path NETWORK_PRICING_TEST_INPUT =
      Path.of("src", "test", "resources", "testcases", "networkpricing");

  /** the header of reader settings */
  private static final String READER_HEADER = "planitIoReaderSettings: 1\n";

  /** the header of writer settings */
  private static final String WRITER_HEADER = "planitIoWriterSettings: 1\n";

  /** prefix of the files of every reader, all taking the same keys and defaults */
  private static final String READER_FILES = "planit_io_reader";

  /** prefix of the file of every key not at its default, shared by the writers having the keys of every writer only */
  private static final String WRITER_FILES = "planit_io_writer";

  /**
   * each kind of reader and writer settings with its files, the readers sharing theirs, and the writers with the keys
   * of every writer only sharing their file of every key not at its default, their default file names differing
   */
  private static final List<SettingsKind<?>> SETTINGS_KINDS = List.of(
      new SettingsKind<>(PlanitXmlReaderSettingsFactory::createNetworkReaderSettingsFromYaml,
          PlanitNetworkReaderSettings::new, PlanitNetworkReaderSettings::reset, READER_FILES, READER_FILES),
      new SettingsKind<>(PlanitXmlReaderSettingsFactory::createZoningReaderSettingsFromYaml,
          PlanitZoningReaderSettings::new, PlanitZoningReaderSettings::reset, READER_FILES, READER_FILES),
      new SettingsKind<>(PlanitXmlReaderSettingsFactory::createDemandsReaderSettingsFromYaml,
          PlanitDemandsReaderSettings::new, PlanitDemandsReaderSettings::reset, READER_FILES, READER_FILES),
      new SettingsKind<>(PlanitXmlReaderSettingsFactory::createDiscreteDemandsReaderSettingsFromYaml,
          PlanitDiscreteDemandsReaderSettings::new, PlanitDiscreteDemandsReaderSettings::reset, READER_FILES,
          READER_FILES),
      new SettingsKind<>(PlanitXmlReaderSettingsFactory::createServiceNetworkReaderSettingsFromYaml,
          PlanitServiceNetworkReaderSettings::new, PlanitServiceNetworkReaderSettings::reset, READER_FILES,
          READER_FILES),
      new SettingsKind<>(PlanitXmlReaderSettingsFactory::createRoutedServicesReaderSettingsFromYaml,
          PlanitRoutedServicesReaderSettings::new, PlanitRoutedServicesReaderSettings::reset, READER_FILES,
          READER_FILES),
      new SettingsKind<>(PlanitXmlReaderSettingsFactory::createNetworkPricingReaderSettingsFromYaml,
          PlanitNetworkPricingReaderSettings::new, PlanitNetworkPricingReaderSettings::reset, READER_FILES,
          READER_FILES),
      new SettingsKind<>(PlanitXmlWriterSettingsFactory::createNetworkWriterSettingsFromYaml,
          PlanitNetworkWriterSettings::new, PlanitNetworkWriterSettings::reset, WRITER_FILES,
          "planit_io_network_writer"),
      new SettingsKind<>(PlanitXmlWriterSettingsFactory::createServiceNetworkWriterSettingsFromYaml,
          PlanitServiceNetworkWriterSettings::new, PlanitServiceNetworkWriterSettings::reset, WRITER_FILES,
          "planit_io_service_network_writer"),
      new SettingsKind<>(PlanitXmlWriterSettingsFactory::createDiscreteDemandsWriterSettingsFromYaml,
          PlanitDiscreteDemandsWriterSettings::new, PlanitDiscreteDemandsWriterSettings::reset, WRITER_FILES,
          "planit_io_discrete_demands_writer"),
      new SettingsKind<>(PlanitXmlWriterSettingsFactory::createNetworkPricingWriterSettingsFromYaml,
          PlanitNetworkPricingWriterSettings::new, PlanitNetworkPricingWriterSettings::reset, WRITER_FILES,
          "planit_io_network_pricing_writer"),
      new SettingsKind<>(PlanitXmlWriterSettingsFactory::createZoningWriterSettingsFromYaml,
          PlanitZoningWriterSettings::new, PlanitZoningWriterSettings::reset, "planit_io_zoning_writer",
          "planit_io_zoning_writer"),
      new SettingsKind<>(PlanitXmlWriterSettingsFactory::createDemandsWriterSettingsFromYaml,
          PlanitDemandsWriterSettings::new, PlanitDemandsWriterSettings::reset, "planit_io_demands_writer",
          "planit_io_demands_writer"),
      new SettingsKind<>(PlanitXmlWriterSettingsFactory::createRoutedServicesWriterSettingsFromYaml,
          PlanitRoutedServicesWriterSettings::new, PlanitRoutedServicesWriterSettings::reset,
          "planit_io_routed_services_writer", "planit_io_routed_services_writer"));

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  /**
   * A file written in the test folder
   *
   * @param relativePath of the file in the test folder
   * @param text of the file, its header included
   * @return the file
   * @throws IOException when the file cannot be written
   */
  private Path writeFile(String relativePath, String text) throws IOException {
    var file = tempDir.resolve(relativePath);
    Files.createDirectories(file.getParent());
    return Files.writeString(file, text);
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(PlanitXmlSettingsFilesTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }

  /** folder for the test files */
  @TempDir
  public Path tempDir;

  /**
   * For each reader and writer, settings made from the file of every key not at its default write that file again,
   * with every key, with the keys differing from the defaults, and when made again from a file without descriptions
   */
  @Test
  public void fileWrittenAgainBySettingsMadeFromIt() throws IOException {
    for (var kind : SETTINGS_KINDS) {
      SettingsFileAssertionUtils.assertFileWrittenAgainBySettingsMadeFromIt(
          kind.fromYaml, kind.everyKeyNotDefaultFile, tempDir);
    }
  }

  /**
   * For each reader and writer, the settings at their defaults write the file of the global defaults, and so do
   * settings made from that file
   */
  @Test
  public void fileWrittenBySettingsAtTheirDefaults() throws IOException {
    for (var kind : SETTINGS_KINDS) {
      SettingsFileAssertionUtils.assertFileWrittenBySettingsAtTheirDefaults(
          kind.fromYaml, kind.atDefaults.get(), kind.globalDefaultsFile, tempDir);
    }
  }

  /**
   * For each reader and writer, settings made from the file of every key not at its default and then reset write
   * every key as the settings at their defaults do: a reset puts every key back to its default
   */
  @Test
  public void resetGivesSettingsAsMadeNew() throws IOException {
    for (var kind : SETTINGS_KINDS) {
      kind.assertResetGivesSettingsAsMadeNew(tempDir);
    }
  }

  /** A writer's file given to a reader, and a reader's file given to a writer, are one problem each, of its header */
  @Test
  public void readerAndWriterFilesKeptApart() {
    SettingsFileAssertionUtils.assertThrowsOneSettingsProblem(SettingsProblemKind.WRONG_HEADER,
        () -> PlanitXmlReaderSettingsFactory.createNetworkReaderSettingsFromYaml(
            List.of(SettingsSource.ofText(WRITER_HEADER + "fileName: roads.xml\n"))));

    SettingsFileAssertionUtils.assertThrowsOneSettingsProblem(SettingsProblemKind.WRONG_HEADER,
        () -> PlanitXmlWriterSettingsFactory.createNetworkWriterSettingsFromYaml(
            List.of(SettingsSource.ofText(READER_HEADER + "syncXmlIdsToIds: true\n"))));
  }

  /** Sources stack in order, the last one giving a key winning */
  @Test
  public void stackedReaderFiles() {
    var settings = PlanitXmlReaderSettingsFactory.createNetworkReaderSettingsFromYaml(List.of(
        SettingsSource.ofText(READER_HEADER + "syncXmlIdsToIds: true\n"),
        SettingsSource.ofText(READER_HEADER + "syncXmlIdsToIds: false\n")));
    assertFalse(settings.isSyncXmlIdsToIds());
  }

  /** A key of another writer, the zoning writer's here, is a problem naming the key for the network writer */
  @Test
  public void keyOfAnotherWriterRefused() {
    var problem = SettingsFileAssertionUtils.assertThrowsOneSettingsProblem(SettingsProblemKind.UNKNOWN_KEY,
        () -> PlanitXmlWriterSettingsFactory.createNetworkWriterSettingsFromYaml(
            List.of(SettingsSource.ofText(WRITER_HEADER + "removeDanglingZones: false\n"))));
    assertEquals("removeDanglingZones", problem.getSubject());
  }

  /** The zoning writer made from a file of its own takes the keys of every writer and its own */
  @Test
  public void zoningWriterOnItsOwn() throws IOException {
    var file = writeFile("run/zoning_writer.yaml",
        WRITER_HEADER + "outputDirectory: out\nfileName: zones.xml\nremoveDanglingZones: false\n");
    var settings =
        PlanitXmlWriterSettingsFactory.createZoningWriterSettingsFromYaml(List.of(SettingsSource.ofFile(file)));

    assertEquals(tempDir.resolve("run/out"), Path.of(settings.getOutputDirectory()));
    assertEquals("zones.xml", settings.getFileName());
    assertFalse(settings.isRemoveDanglingZones());
  }

  /**
   * The network pricing reader made from a file of its own reads the pricing file from the folder the file names
   * ("Network pricing reader on its own")
   *
   * @throws IOException when a file cannot be written
   */
  @Test
  public void networkPricingReaderOnItsOwn() throws IOException {
    Files.createDirectories(tempDir.resolve("run/pricing"));
    Files.copy(NETWORK_PRICING_TEST_INPUT.resolve("example").resolve("networkpricing.xml"),
        tempDir.resolve("run/pricing/networkpricing.xml"));
    var file = writeFile("run/pricing_reader.yaml", READER_HEADER + "inputSource: pricing\n");
    var settings = PlanitXmlReaderSettingsFactory.createNetworkPricingReaderSettingsFromYaml(
        List.of(SettingsSource.ofFile(file)));

    var network = PlanItIOTestHelper.readNetwork(NETWORK_PRICING_TEST_INPUT.resolve("network"));
    var pricing = PlanitNetworkPricingReaderFactory.create(settings, network).read();
    assertEquals("sydney2025", pricing.getXmlId());
    assertEquals(10, pricing.streamSchemes().count());
  }

  /**
   * An intermodal reader file names the files of its nested settings, relative to itself, each of them made from its
   * file as on its own; the nested settings not named keep their defaults
   */
  @Test
  public void intermodalReaderNamingTheFilesOfItsNestedSettings() throws IOException, URISyntaxException {
    writeFile("run/network.yaml", READER_HEADER + "inputSource: planit_osm\n");
    writeFile("run/zoning.yaml", READER_HEADER + "inputSource: planit_gis\n");
    var file = writeFile("run/intermodal.yaml", READER_HEADER + "network: network.yaml\nzoning: zoning.yaml\n");

    var settings =
        PlanitXmlReaderSettingsFactory.createIntermodalReaderSettingsFromYaml(List.of(SettingsSource.ofFile(file)));

    assertEquals(tempDir.resolve("run/planit_osm"),
        Path.of(settings.getNetworkSettings().getInputSource().toURI()));
    assertEquals(tempDir.resolve("run/planit_gis"), Path.of(settings.getZoningSettings().getInputSource().toURI()));
    assertNull(settings.getServiceNetworkSettings().getInputSource());
    assertNull(settings.getRoutedServicesSettings().getInputSource());
  }

  /** A key of nested settings in an intermodal file is a problem naming the key */
  @Test
  public void keyOfNestedSettingsInTheIntermodalFileRefused() {
    var problem = SettingsFileAssertionUtils.assertThrowsOneSettingsProblem(SettingsProblemKind.UNKNOWN_KEY,
        () -> PlanitXmlWriterSettingsFactory.createIntermodalWriterSettingsFromYaml(
            List.of(SettingsSource.ofText(WRITER_HEADER + "outputDirectory: out\n"))));
    assertEquals("outputDirectory", problem.getSubject());
  }

  /** A file an intermodal file names that is not there is one problem naming that file, and no settings are made */
  @Test
  public void namedFileMissing() throws IOException {
    var file = writeFile("run/intermodal.yaml", READER_HEADER + "zoning: not_there.yaml\n");

    var problem = SettingsFileAssertionUtils.assertThrowsOneSettingsProblem(SettingsProblemKind.FILE_NOT_READ,
        () -> PlanitXmlReaderSettingsFactory.createIntermodalReaderSettingsFromYaml(
            List.of(SettingsSource.ofFile(file))));
    assertEquals(SettingsSource.ofFile(tempDir.resolve("run/not_there.yaml")).getDescription(), problem.getSource());
  }

  /**
   * Intermodal writer settings written out write a file naming a file for each of their nested settings, each written
   * next to it, named after it followed by the key, and read back give the same settings
   */
  @Test
  public void intermodalWriterWrittenOutWithItsNestedSettings() throws IOException {
    var settings = new PlanitIntermodalWriterSettings();
    settings.getNetworkSettings().setFileName("roads.xml");
    settings.getZoningSettings().setRemoveDanglingZones(false);
    var file = tempDir.resolve("run/intermodal.yaml");
    Files.createDirectories(file.getParent());

    settings.writeSettingsFile(file, SettingsFileContentType.KEYS_DIFFERING_FROM_DEFAULTS, false);

    var lines = Files.readAllLines(file);
    for (var key : List.of("network", "zoning", "serviceNetwork", "routedServices")) {
      var nestedSettingsFileName = "intermodal_" + key + ".yaml";
      assertTrue(lines.contains(key + ": " + nestedSettingsFileName), key);
      assertTrue(Files.exists(file.resolveSibling(nestedSettingsFileName)), key);
    }
    var readBack =
        PlanitXmlWriterSettingsFactory.createIntermodalWriterSettingsFromYaml(List.of(SettingsSource.ofFile(file)));
    assertEquals("roads.xml", readBack.getNetworkSettings().getFileName());
    assertFalse(readBack.getZoningSettings().isRemoveDanglingZones());
  }

  /** The service network and routed services writers, made from settings made from their files, keep those settings */
  @Test
  public void serviceWritersMadeFromTheirFiles() {
    var serviceNetworkSettings = PlanitXmlWriterSettingsFactory.createServiceNetworkWriterSettingsFromYaml(
        List.of(SettingsSource.ofText(WRITER_HEADER + "fileName: lines.xml\n")));
    var routedServicesSettings = PlanitXmlWriterSettingsFactory.createRoutedServicesWriterSettingsFromYaml(
        List.of(SettingsSource.ofText(WRITER_HEADER + "fileName: services.xml\ntripFrequencyTimeUnit: MINUTE\n")));

    assertSame(serviceNetworkSettings, PlanitServiceNetworkWriterFactory.create(serviceNetworkSettings).getSettings());
    assertSame(routedServicesSettings, PlanitRoutedServicesWriterFactory.create(routedServicesSettings).getSettings());
  }

  /**
   * The zoning and demands writers, made from settings made from their files, before anything is read, keep those
   * settings; the converter gives them the network and the zoning when converting
   */
  @Test
  public void zoningAndDemandsWritersMadeFromTheirFiles() {
    var zoningSettings = PlanitXmlWriterSettingsFactory.createZoningWriterSettingsFromYaml(
        List.of(SettingsSource.ofText(WRITER_HEADER + "fileName: zones.xml\nremoveDanglingZones: false\n")));
    var demandsSettings = PlanitXmlWriterSettingsFactory.createDemandsWriterSettingsFromYaml(
        List.of(SettingsSource.ofText(WRITER_HEADER + "fileName: trips.xml\n")));

    assertSame(zoningSettings, PlanitZoningWriterFactory.create(zoningSettings).getSettings());
    assertSame(demandsSettings, PlanitDemandsWriterFactory.create(demandsSettings).getSettings());
  }

  /** The input directory set in code is the input source, as if a file in the working folder had given it */
  @Test
  public void inputDirectorySetsTheInputSource() throws URISyntaxException {
    var settings = new PlanitNetworkReaderSettings();
    settings.setInputDirectory("planit_osm");

    var fromText = PlanitXmlReaderSettingsFactory.createNetworkReaderSettingsFromYaml(
        List.of(SettingsSource.ofText(READER_HEADER + "inputSource: planit_osm\n")));
    assertEquals(Path.of("planit_osm").toAbsolutePath(), Path.of(settings.getInputSource().toURI()));
    assertEquals(Path.of(fromText.getInputSource().toURI()), Path.of(settings.getInputSource().toURI()));
  }

  /** A reader reads the same network from a folder holding its file as from that one file */
  @Test
  public void inputSourceAsAFolderOrAsOneFile() {
    var fromFolder = new PlanitNetworkReaderSettings();
    fromFolder.setInputSource(CONVERTER_TEST_INPUT.toString());
    var fromFile = new PlanitNetworkReaderSettings();
    fromFile.setInputSource(CONVERTER_TEST_INPUT.resolve("network.xml").toString());

    var linksFromFolder =
        PlanitNetworkReaderFactory.create(fromFolder).read().getTransportLayers().getFirst().getLinks().size();
    var linksFromFile =
        PlanitNetworkReaderFactory.create(fromFile).read().getTransportLayers().getFirst().getLinks().size();
    assertTrue(linksFromFolder > 0);
    assertEquals(linksFromFolder, linksFromFile);
  }
}
