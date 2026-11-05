package org.goplanit.io.test.integration.converter;

import org.goplanit.io.converter.network.PlanitNetworkReader;
import org.goplanit.io.converter.network.PlanitNetworkWriterFactory;
import org.goplanit.io.test.integration.TestBase;
import org.goplanit.io.test.util.PlanItIOTestHelper;
import org.goplanit.io.test.util.WarningRecorder;
import org.goplanit.mode.ModeFeaturesFactory;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.mode.DriverType;
import org.goplanit.utils.mode.MotorisationModeType;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.mode.TrackModeType;
import org.goplanit.utils.mode.UseOfModeType;
import org.goplanit.utils.mode.VehicularModeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests reading and writing the usability features of modes in the native PLANit network format: who drives a mode is
 * kept through writing and reading back, a predefined mode is written with its type's driver type, and a file whose
 * use or driver type differs from a predefined mode's type is warned about while the mode keeps its type's features
 *
 * @author markr
 */
public class ModeFeaturesReaderWriterTest extends TestBase {

  /** the network reader's logger, held here so the recorder stays attached to the instance the reader uses */
  private static final Logger READER_LOGGER = Logger.getLogger(PlanitNetworkReader.class.getCanonicalName());

  /** location of a small network whose only mode is the predefined car mode */
  private static final Path GRID_PATH = Path.of(TEST_CASE_PATH.toString(), "grid10x10", "reference");

  /** name of the network file the writer writes */
  private static final String NETWORK_FILE_NAME = "network.xml";

  /**
   * Write the grid network to the given directory, change its network file as given, and read it back
   *
   * @param directory to write to
   * @param original text of the network file to change
   * @param replacement for it
   * @return network read back
   * @throws IOException thrown if the network file cannot be changed
   */
  private static MacroscopicNetwork writeChangeAndReadBack(Path directory, String original, String replacement)
      throws IOException {
    PlanitNetworkWriterFactory.create(directory.toAbsolutePath().toString()).write(
        PlanItIOTestHelper.readNetwork(GRID_PATH));
    var networkFile = directory.resolve(NETWORK_FILE_NAME);
    var networkText = Files.readString(networkFile);
    assertTrue(networkText.contains(original));
    Files.writeString(networkFile, networkText.replace(original, replacement));
    IdGenerator.reset();
    return PlanItIOTestHelper.readNetwork(directory);
  }

  /** records the warnings the network reader logs while a test runs */
  private WarningRecorder readerWarnings;

  @BeforeEach
  public void recordReaderWarnings() {
    IdGenerator.reset();
    readerWarnings = new WarningRecorder();
    READER_LOGGER.addHandler(readerWarnings);
  }

  @AfterEach
  public void stopRecordingReaderWarnings() {
    READER_LOGGER.removeHandler(readerWarnings);
    IdGenerator.reset();
  }

  /**
   * Who drives a custom mode is kept when the network is written and read back, and so is not knowing it; the
   * predefined car mode has its type's driver type
   *
   * @param outputDir to write the network to
   */
  @Test
  public void driverTypeIsKeptThroughWritingAndReadingBack(@TempDir Path outputDir) {
    var network = PlanItIOTestHelper.readNetwork(GRID_PATH);
    var motorisedRoad = ModeFeaturesFactory.createPhysicalFeatures(
        VehicularModeType.VEHICLE, MotorisationModeType.MOTORISED, TrackModeType.ROAD);
    var van = network.getModes().getFactory().registerNewCustomMode("van", 100, 1.5, motorisedRoad,
        ModeFeaturesFactory.createUsabilityFeatures(UseOfModeType.PRIVATE, DriverType.TRAVELLER));
    van.setXmlId("van");
    var pod = network.getModes().getFactory().registerNewCustomMode("pod", 40, 1, motorisedRoad,
        ModeFeaturesFactory.createUsabilityFeatures(UseOfModeType.PRIVATE));
    pod.setXmlId("pod");

    PlanitNetworkWriterFactory.create(outputDir.toAbsolutePath().toString()).write(network);
    IdGenerator.reset();
    var readBack = PlanItIOTestHelper.readNetwork(outputDir);

    assertEquals(DriverType.TRAVELLER, readBack.getModes().getByXmlId("van").getUseFeatures().getDriverType());
    assertNull(readBack.getModes().getByXmlId("pod").getUseFeatures().getDriverType());
    assertEquals(DriverType.TRAVELLER,
        readBack.getModes().get(PredefinedModeType.CAR).getUseFeatures().getDriverType());
    assertEquals(0, readerWarnings.getCount());
  }

  /**
   * A predefined mode is written with the driver type of its type
   *
   * @param outputDir to write the network to
   * @throws IOException thrown if the written network cannot be read
   */
  @Test
  public void predefinedModeIsWrittenWithItsDriverType(@TempDir Path outputDir) throws IOException {
    PlanitNetworkWriterFactory.create(outputDir.toAbsolutePath().toString()).write(
        PlanItIOTestHelper.readNetwork(GRID_PATH));

    assertTrue(Files.readString(outputDir.resolve(NETWORK_FILE_NAME)).contains("<drivertype>traveller</drivertype>"));
  }

  /**
   * A file whose use and driver type of the predefined car mode differ from its type's gives one warning for each,
   * and the car mode keeps its type's features
   *
   * @param outputDir to write the network to
   * @throws IOException thrown if the network file cannot be changed
   */
  @Test
  public void predefinedModeKeepsItsTypesFeaturesWhenTheFileDiffers(@TempDir Path outputDir) throws IOException {
    var network = writeChangeAndReadBack(outputDir,
        "<usedtotype>private</usedtotype>\n                    <drivertype>traveller</drivertype>",
        "<usedtotype>public</usedtotype>\n                    <drivertype>operator</drivertype>");

    var car = network.getModes().get(PredefinedModeType.CAR);
    assertEquals(2, readerWarnings.getCount());
    assertEquals(UseOfModeType.PRIVATE, car.getUseFeatures().getUseOfType());
    assertEquals(DriverType.TRAVELLER, car.getUseFeatures().getDriverType());
  }

  /**
   * A file that leaves out who drives the predefined car mode, as every file written before it could be given does,
   * gives no warning, and the car mode has its type's driver type
   *
   * @param outputDir to write the network to
   * @throws IOException thrown if the network file cannot be changed
   */
  @Test
  public void predefinedModeWithoutDriverTypeInTheFileGivesNoWarning(@TempDir Path outputDir) throws IOException {
    var network = writeChangeAndReadBack(outputDir,
        "\n                    <drivertype>traveller</drivertype>", "");

    assertEquals(0, readerWarnings.getCount());
    assertEquals(DriverType.TRAVELLER,
        network.getModes().get(PredefinedModeType.CAR).getUseFeatures().getDriverType());
  }
}
