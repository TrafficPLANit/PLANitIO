package org.goplanit.io.test.integration.converter;

import org.goplanit.io.test.integration.TestBase;
import org.goplanit.io.xml.util.PlanitXmlCrsInspector;
import org.goplanit.io.xml.util.PlanitXmlNetworkSpatialInspector;
import org.goplanit.io.xml.util.PlanitXmlVersion;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for lightweight PLANit XML CRS inspection.
 */
public class PlanitXmlCrsInspectorTest extends TestBase {

  /**
   * Verify PLANit XML version and CRS lookup on v2 network XML.
   */
  @Test
  public void test_peek_v2_network_crs() {
    final String inputDirectory = Path.of(TEST_CASE_PATH.toString(), "converter_test", "input").toString();
    final String inputFile = Path.of(inputDirectory, "network.xml").toString();

    var xmlVersion = PlanitXmlCrsInspector.peekPlanitXmlVersion(inputFile);
    assertTrue(xmlVersion.isPresent());
    assertEquals(PlanitXmlVersion.V2, xmlVersion.get());

    var srsName = PlanitXmlCrsInspector.peekNetworkSrsName(inputDirectory);
    assertTrue(srsName.isPresent());
    assertEquals("EPSG:3112", srsName.get());

    var versionedSrsName = PlanitXmlCrsInspector.peekNetworkSrsName(inputDirectory, PlanitXmlVersion.V2);
    assertTrue(versionedSrsName.isPresent());
    assertEquals("EPSG:3112", versionedSrsName.get());
  }

  /**
   * Verify PLANit XML version and CRS lookup on v1 network XML.
   */
  @Test
  public void test_peek_v1_network_crs() {
    final String inputDirectory = Path.of(TEST_CASE_PATH.toString(), "v1v2converter_test", "v1input").toString();
    final String inputFile = Path.of(inputDirectory, "network.xml").toString();

    var xmlVersion = PlanitXmlCrsInspector.peekPlanitXmlVersion(inputFile);
    assertTrue(xmlVersion.isPresent());
    assertEquals(PlanitXmlVersion.V1, xmlVersion.get());

    var srsName = PlanitXmlCrsInspector.peekNetworkSrsName(inputDirectory);
    assertTrue(srsName.isPresent());
    assertEquals("EPSG:3112", srsName.get());

    var versionedSrsName = PlanitXmlCrsInspector.peekNetworkSrsName(inputDirectory, PlanitXmlVersion.V1);
    assertTrue(versionedSrsName.isPresent());
    assertEquals("EPSG:3112", versionedSrsName.get());
  }

  /**
   * Verify PLANit XML version and CRS lookup on v2 zoning XML.
   */
  @Test
  public void test_peek_v2_zoning_crs() {
    final String inputDirectory = Path.of(TEST_CASE_PATH.toString(), "converter_test", "input").toString();
    final String inputFile = Path.of(inputDirectory, "zoning.xml").toString();

    var xmlVersion = PlanitXmlCrsInspector.peekPlanitXmlVersion(inputFile);
    assertTrue(xmlVersion.isPresent());
    assertEquals(PlanitXmlVersion.V2, xmlVersion.get());

    var srsName = PlanitXmlCrsInspector.peekZoningSrsName(inputDirectory);
    assertTrue(srsName.isPresent());
    assertEquals("EPSG:3112", srsName.get());

    var versionedSrsName = PlanitXmlCrsInspector.peekZoningSrsName(inputDirectory, PlanitXmlVersion.V2);
    assertTrue(versionedSrsName.isPresent());
    assertEquals("EPSG:3112", versionedSrsName.get());
  }

  /**
   * Verify PLANit XML version and CRS lookup on v1 zoning XML.
   */
  @Test
  public void test_peek_v1_zoning_crs() {
    final String inputDirectory = Path.of(TEST_CASE_PATH.toString(), "v1v2converter_test", "v1input").toString();
    final String inputFile = Path.of(inputDirectory, "zoning.xml").toString();

    var xmlVersion = PlanitXmlCrsInspector.peekPlanitXmlVersion(inputFile);
    assertTrue(xmlVersion.isPresent());
    assertEquals(PlanitXmlVersion.V1, xmlVersion.get());

    var srsName = PlanitXmlCrsInspector.peekZoningSrsName(inputDirectory);
    assertTrue(srsName.isPresent());
    assertEquals("EPSG:3112", srsName.get());

    var versionedSrsName = PlanitXmlCrsInspector.peekZoningSrsName(inputDirectory, PlanitXmlVersion.V1);
    assertTrue(versionedSrsName.isPresent());
    assertEquals("EPSG:3112", versionedSrsName.get());
  }

  /**
   * Verify representative network coordinate can be inspected from XML without a full JAXB network read.
   */
  @Test
  public void test_peek_v1_network_reference_coordinate() {
    final String inputDirectory = Path.of(TEST_CASE_PATH.toString(), "grid10x10","reference").toString();

    var referenceCoordinate = PlanitXmlNetworkSpatialInspector.peekNetworkReferenceCoordinate(inputDirectory);
    assertTrue(referenceCoordinate.isPresent());
    assertNotNull(referenceCoordinate.get());
    assertEquals(0.0, referenceCoordinate.get().x);
    assertEquals(0.0, referenceCoordinate.get().y);
  }
}
