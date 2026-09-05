package org.goplanit.io.test;

import org.goplanit.io.converter.network.PlanitNetworkWriterSettings;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for PLANit assertion utilities
 */
public class PlanitAssertionUtilsTest {

  /**
   * Create a directory containing a PLANit network XML file with the provided GML position
   * @param baseDirectory base directory
   * @param directoryName directory name
   * @param gmlPosition GML position value
   * @return created directory
   * @throws IOException when unable to write
   */
  private Path createNetworkXmlDirectory(Path baseDirectory, String directoryName, String gmlPosition)
      throws IOException {
    Path directory = baseDirectory.resolve(directoryName);
    Files.createDirectories(directory);
    Files.writeString(
        directory.resolve(PlanitNetworkWriterSettings.DEFAULT_NETWORK_XML),
        "<network xmlns:gml=\"http://www.opengis.net/gml\"><gml:Point><gml:pos>"
            + gmlPosition
            + "</gml:pos></gml:Point></network>");
    return directory;
  }

  /**
   * Verify GML coordinate tolerance is explicit and bounded
   * @throws IOException when unable to write test files
   */
  @Test
  public void testNetworkXmlComparisonWithGmlCoordinateTolerance() throws IOException {
    Path baseDirectory = Path.of("target", "planit-assertion-utils-test", Long.toString(System.nanoTime()));
    Path referenceDirectory = createNetworkXmlDirectory(baseDirectory, "reference", "1.0 2.0");
    Path smallDifferenceDirectory = createNetworkXmlDirectory(
        baseDirectory, "small_difference", "1.0000001 2.0000001");
    Path largeDifferenceDirectory = createNetworkXmlDirectory(baseDirectory, "large_difference", "1.001 2.001");

    assertThrows(
        AssertionError.class,
        () -> PlanitAssertionUtils.assertNetworkFilesSimilar(smallDifferenceDirectory, referenceDirectory));

    assertDoesNotThrow(
        () -> PlanitAssertionUtils.assertNetworkFilesSimilarWithGmlCoordinateTolerance(
            smallDifferenceDirectory, referenceDirectory));

    assertThrows(
        AssertionError.class,
        () -> PlanitAssertionUtils.assertNetworkFilesSimilarWithGmlCoordinateTolerance(
            largeDifferenceDirectory, referenceDirectory));
  }
}
