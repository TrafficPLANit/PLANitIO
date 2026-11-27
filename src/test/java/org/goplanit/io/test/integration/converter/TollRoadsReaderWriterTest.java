package org.goplanit.io.test.integration.converter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.io.converter.network.PlanitNetworkWriterFactory;
import org.goplanit.io.test.util.PlanItIOTestHelper;
import org.goplanit.logging.Logging;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests reading and writing toll roads in the native PLANit network format: who pays as given, what the reader skips
 * or discards with one warning each, the round trip, and a layer without toll roads
 *
 * @author markr
 */
public class TollRoadsReaderWriterTest extends NetworkReaderWriterTestBase {

  /** the logger */
  private static Logger LOGGER = null;

  /** location of the toll roads test cases */
  private static final Path TOLL_ROADS_PATH = Path.of(TEST_CASE_PATH.toString(), "tollroads");

  /**
   * The XML ids of a toll road's link segments, in order
   *
   * @param tollRoad to ask
   * @return the XML ids
   */
  private static List<String> linkSegmentIds(TollRoad tollRoad) {
    return tollRoad.getLinkSegments().stream().map(MacroscopicLinkSegment::getXmlId).collect(Collectors.toList());
  }

  /**
   * The written {@code <tollroad>} element with the given id, up to its end
   *
   * @param written network file text
   * @param xmlId of the toll road
   * @return the element's text
   */
  private static String writtenTollRoad(String written, String xmlId) {
    var element = written.substring(written.indexOf("<tollroad id=\"" + xmlId + "\""));
    return element.substring(0, element.indexOf("/>"));
  }

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(TollRoadsReaderWriterTest.class);
    }
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }

  // READING

  /** A network without toll roads reads as before, with no toll roads ("Existing input without toll roads") */
  @Test
  public void existingInputWithoutTollRoads() {
    var layer = readLayer(Path.of(TEST_CASE_PATH.toString(), "grid10x10", "reference"));
    assertFalse(layer.getLinkSegments().isEmpty());
    assertTrue(layer.getTollRoads().isEmpty());
    assertEquals(0, readerWarnings.getCount());
  }

  /**
   * Toll roads are read with their link segments, name and external id, and who pays as given: by default, heavy goods
   * named, or motorcycles exempt ("Toll road read")
   */
  @Test
  public void tollRoadsAreRead() {
    var layer = readLayer(TOLL_ROADS_PATH.resolve("input"));

    assertEquals(3, layer.getTollRoads().size());
    var m4 = byXmlId(layer.getTollRoads(), "m4");
    assertEquals(List.of("0", "1"), linkSegmentIds(m4));
    assertEquals("M4", m4.getName());
    assertEquals("way 7", m4.getExternalId());
    assertTrue(m4.getPricedModeSelection().isUnspecified());
    assertEquals(Set.of("car", "hgv", "motor_bike"), xmlIds(m4.findPricedModes(layer.getSupportedModes())));
    assertEquals(Set.of("hgv"),
        xmlIds(byXmlId(layer.getTollRoads(), "trucks").getPricedModeSelection().getPricedModes()));
    var ramp = byXmlId(layer.getTollRoads(), "ramp");
    assertEquals(Set.of("motor_bike"), xmlIds(ramp.getPricedModeSelection().getExemptedModes()));
    assertEquals(Set.of("car", "hgv"), xmlIds(ramp.findPricedModes(layer.getSupportedModes())));
    assertEquals(0, readerWarnings.getCount());
  }

  /**
   * What the reader cannot use is skipped or discarded with one warning each: an unknown link segment is skipped
   * ("Unknown link segment"), a second toll road with an id used before is not read ("Toll road id used twice"), nor
   * one giving both priced and priced exempt modes ("Both priced and priced exempt modes"), an unknown mode is skipped
   * ("Unknown mode"), a toll road whose every link segment is unknown is not read ("Toll road left without link
   * segments"), nor one whose every link segment is already on another toll road ("Link segment already on another
   * toll road")
   */
  @Test
  public void whatCannotBeUsedIsSkippedWithOneWarningEach() {
    var layer = new MacroscopicNetworkLayer[] {readLayer(TOLL_ROADS_PATH.resolve("lenient"))};

    assertEquals(2, layer[0].getTollRoads().size());
    assertEquals(List.of("0"), linkSegmentIds(byXmlId(layer[0].getTollRoads(), "a")));
    assertEquals(Set.of("hgv"),
        xmlIds(byXmlId(layer[0].getTollRoads(), "unknownmode").getPricedModeSelection().getPricedModes()));
    assertNull(byXmlId(layer[0].getTollRoads(), "both"));
    assertNull(byXmlId(layer[0].getTollRoads(), "nothing"));
    assertNull(byXmlId(layer[0].getTollRoads(), "taken"));
    assertEquals(6, readerWarnings.getCount());
  }

  // WRITING

  /**
   * Toll roads written and read back keep their link segments, name, external id and priced modes as given, and a
   * toll road naming no modes is written with neither attribute ("Round trip", "Default priced modes written")
   */
  @Test
  public void roundTripKeepsTollRoads() throws Exception {
    var originalNetwork = PlanItIOTestHelper.readNetwork(TOLL_ROADS_PATH.resolve("input"));
    var original = originalNetwork.getTransportLayers().getFirst();

    PlanitNetworkWriterFactory.create(TOLL_ROADS_PATH.toAbsolutePath().toString()).write(originalNetwork);
    IdGenerator.reset();
    try {
      var readBack = readLayer(TOLL_ROADS_PATH);
      assertEquals(original.getTollRoads().size(), readBack.getTollRoads().size());
      for (var written : original.getTollRoads()) {
        var read = byXmlId(readBack.getTollRoads(), written.getXmlId());
        assertNotNull(read, written.getXmlId());
        assertEquals(linkSegmentIds(written), linkSegmentIds(read));
        assertEquals(written.getName(), read.getName());
        assertEquals(written.getExternalId(), read.getExternalId());
        assertEquals(xmlIds(written.getPricedModeSelection().getPricedModes()),
            xmlIds(read.getPricedModeSelection().getPricedModes()));
        assertEquals(xmlIds(written.getPricedModeSelection().getExemptedModes()),
            xmlIds(read.getPricedModeSelection().getExemptedModes()));
      }
      var m4 = writtenTollRoad(Files.readString(TOLL_ROADS_PATH.resolve("network.xml")), "m4");
      assertFalse(m4.contains("pricedmoderefs"));
      assertFalse(m4.contains("pricedexemptmoderefs"));
      assertEquals(0, readerWarnings.getCount());
    } finally {
      PlanItIOTestHelper.deleteFile(TOLL_ROADS_PATH.resolve("network.xml").toAbsolutePath().toString());
    }
  }

  /** A layer without toll roads is written without a toll roads element ("Layer without toll roads") */
  @Test
  public void layerWithoutTollRoadsIsWrittenWithoutSection() throws Exception {
    var network = PlanItIOTestHelper.readNetwork(Path.of(TEST_CASE_PATH.toString(), "grid10x10", "reference"));

    PlanitNetworkWriterFactory.create(TOLL_ROADS_PATH.toAbsolutePath().toString()).write(network);

    try {
      assertFalse(Files.readString(TOLL_ROADS_PATH.resolve("network.xml")).contains("tollroads"));
    } finally {
      PlanItIOTestHelper.deleteFile(TOLL_ROADS_PATH.resolve("network.xml").toAbsolutePath().toString());
    }
  }
}
