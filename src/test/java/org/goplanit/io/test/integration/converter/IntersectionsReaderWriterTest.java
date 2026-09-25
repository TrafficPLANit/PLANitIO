package org.goplanit.io.test.integration.converter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.io.converter.network.PlanitNetworkReader;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.io.converter.network.PlanitNetworkWriterFactory;
import org.goplanit.io.test.integration.TestBase;
import org.goplanit.io.test.util.PlanItIOTestHelper;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerUtils;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.physical.Node;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests reading and writing intersections in the native PLANit network format: the round trip, a layer without
 * intersections, and what the reader skips, discards or rejects. Also that removing infrastructure takes the banned
 * movements on it along, so the network is still written and read back without errors
 *
 * @author markr
 */
public class IntersectionsReaderWriterTest extends TestBase {

  /** the logger */
  private static Logger LOGGER = null;

  /** location of the intersections test cases */
  private static final Path INTERSECTIONS_PATH = Path.of(TEST_CASE_PATH.toString(), "intersections");

  /** the network reader's logger, held here so the recorder stays attached to the instance the reader uses */
  private static final Logger READER_LOGGER = Logger.getLogger(PlanitNetworkReader.class.getCanonicalName());

  /** records the warnings the network reader logs while a test runs */
  private WarningRecorder readerWarnings;

  /** counts warnings and more severe records */
  private static class WarningRecorder extends Handler {

    /** number of records counted */
    int count = 0;

    @Override public void publish(LogRecord record) {
      if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
        ++count;
      }
    }

    @Override public void flush() {}

    @Override public void close() {}
  }

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(IntersectionsReaderWriterTest.class);
    }
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }

  @BeforeEach
  public void recordReaderWarnings() {
    IdGenerator.reset();
    readerWarnings = new WarningRecorder();
    READER_LOGGER.addHandler(readerWarnings);
  }

  @AfterEach
  public void stopRecordingReaderWarnings() {
    READER_LOGGER.removeHandler(readerWarnings);
  }

  /** read the network in the given directory */
  private static MacroscopicNetwork readNetwork(Path directory) {
    return PlanitNetworkReaderFactory.create(directory.toAbsolutePath().toString()).read();
  }

  /** read the network in the given directory, and return its only layer */
  private static MacroscopicNetworkLayer readLayer(Path directory) {
    return readNetwork(directory).getTransportLayers().getFirst();
  }

  /** the intersection of the layer with the given XML id, null when none */
  private static Intersection byXmlId(MacroscopicNetworkLayer layer, String xmlId) {
    return layer.getIntersections().stream().filter(i -> xmlId.equals(i.getXmlId())).findFirst().orElse(null);
  }

  /** XML ids of the member nodes */
  private static Set<String> nodeIds(Intersection intersection) {
    return intersection.getMemberNodes().stream().map(Node::getXmlId).collect(Collectors.toSet());
  }

  /** XML ids of the given segments */
  private static Set<String> segmentIds(List<MacroscopicLinkSegment> segments) {
    return segments.stream().map(MacroscopicLinkSegment::getXmlId).collect(Collectors.toSet());
  }

  // READING

  /** A v2 network without intersections reads as before, with an empty intersections container */
  @Test
  public void inputWithoutIntersectionsReadsNone() {
    var layer = readLayer(Path.of(TEST_CASE_PATH.toString(), "grid10x10", "reference"));

    assertFalse(layer.getNodes().isEmpty());
    assertTrue(layer.getIntersections().isEmpty());
  }

  /** Every property of the intersections in the input is read */
  @Test
  public void intersectionsAreRead() {
    var layer = readLayer(INTERSECTIONS_PATH.resolve("roundtrip").resolve("input"));

    assertEquals(3, layer.getIntersections().size());
    var i1 = byXmlId(layer, "i1");
    assertEquals("ext-1", i1.getExternalId());
    assertEquals(IntersectionControlType.SIGNALISED, i1.getControlType());
    assertEquals(EnumSet.of(IntersectionType.JUNCTION, IntersectionType.CROSSING), i1.getTypes());
    assertEquals(Set.of("0"), nodeIds(i1));
    assertEquals(Set.of("0", "2", "4", "7"), segmentIds(i1.getApproachSegments()));
    assertTrue(i1.getInternalSegments().isEmpty());

    var i2 = byXmlId(layer, "i2");
    assertFalse(i2.hasExternalId());
    assertEquals(IntersectionControlType.UNSIGNALISED, i2.getControlType());
    assertEquals(EnumSet.of(IntersectionType.CROSSING), i2.getTypes());

    var i3 = byXmlId(layer, "i3");
    assertEquals(Set.of("5", "6"), nodeIds(i3));
    assertEquals(Set.of("8", "12", "14"), segmentIds(i3.getApproachSegments()));
    assertEquals(Set.of("10", "11"), segmentIds(i3.getInternalSegments()));
    assertSame(i3, layer.getIntersections().getByMemberNode(i3.getMemberNodes().get(1)));
    assertEquals(0, readerWarnings.count);
  }

  /**
   * An intersection with an unknown node, or a node read earlier for another intersection, is not read; an unknown or
   * refused segment is skipped; an intersection without approaches is read as such. Each loss is warned once
   */
  @Test
  public void invalidReferencesAreSkippedWithOneWarningEach() {
    var layer = readLayer(INTERSECTIONS_PATH.resolve("lenient"));

    assertEquals(2, layer.getIntersections().size());
    assertNull(byXmlId(layer, "i2"));
    assertNull(byXmlId(layer, "i3"));

    var i1 = byXmlId(layer, "i1");
    assertEquals(Set.of("0", "2"), segmentIds(i1.getApproachSegments()));

    var i4 = byXmlId(layer, "i4");
    assertTrue(i4.getApproachSegments().isEmpty());
    assertEquals(Set.of("10"), segmentIds(i4.getInternalSegments()));
    assertEquals(5, readerWarnings.count);
  }

  /** An intersection listing a segments role twice makes reading fail */
  @Test
  public void repeatedSegmentsRoleFailsReading() {
    assertThrows(PlanItRunTimeException.class, () -> readLayer(INTERSECTIONS_PATH.resolve("duplicate_role")));
  }

  // WRITING

  /** Intersections written and read back are equivalent to those written */
  @Test
  public void roundTripKeepsIntersections() throws Exception {
    var projectPath = INTERSECTIONS_PATH.resolve("roundtrip");
    var originalNetwork = readNetwork(projectPath.resolve("input"));
    var original = originalNetwork.getTransportLayers().getFirst();

    PlanitNetworkWriterFactory.create(projectPath.toAbsolutePath().toString()).write(originalNetwork);
    IdGenerator.reset();
    var readBack = readLayer(projectPath);

    try {
      assertEquals(original.getIntersections().size(), readBack.getIntersections().size());
      for (var written : original.getIntersections()) {
        var read = byXmlId(readBack, written.getXmlId());
        assertNotNull(read, written.getXmlId());
        assertEquals(written.getExternalId(), read.getExternalId());
        assertEquals(written.getControlType(), read.getControlType());
        assertEquals(written.getTypes(), read.getTypes());
        assertEquals(nodeIds(written), nodeIds(read));
        assertEquals(segmentIds(written.getApproachSegments()), segmentIds(read.getApproachSegments()));
        assertEquals(segmentIds(written.getInternalSegments()), segmentIds(read.getInternalSegments()));
      }

      /* roles without segments and absent external ids are left out */
      var written = Files.readString(projectPath.resolve("network.xml"));
      assertEquals(1, written.split("type=\"internal\"", -1).length - 1);
      assertEquals(2, written.split("externalid=\"ext-", -1).length - 1);
    } finally {
      PlanItIOTestHelper.deleteFile(projectPath.resolve("network.xml").toAbsolutePath().toString());
    }
  }

  /** A layer without intersections is written without an intersections element */
  @Test
  public void layerWithoutIntersectionsIsWrittenWithoutSection() throws Exception {
    var outputPath = INTERSECTIONS_PATH;
    var network = readNetwork(Path.of(TEST_CASE_PATH.toString(), "grid10x10", "reference"));

    PlanitNetworkWriterFactory.create(outputPath.toAbsolutePath().toString()).write(network);

    try {
      assertFalse(Files.readString(outputPath.resolve("network.xml")).contains("intersections"));
    } finally {
      PlanItIOTestHelper.deleteFile(outputPath.resolve("network.xml").toAbsolutePath().toString());
    }
  }

  // REMOVING INFRASTRUCTURE

  /**
   * A ban on a link segment removed for lacking mode access is removed with it, so the network is written and read
   * back without warnings and without the ban
   */
  @Test
  public void banOnSegmentRemovedForModeAccessIsGoneAfterRoundTrip() throws Exception {
    var outputPath = INTERSECTIONS_PATH;
    var network = readNetwork(INTERSECTIONS_PATH.resolve("roundtrip").resolve("input"));
    var layer = network.getTransportLayers().getFirst();
    assertEquals(1, layer.getBannedMovements().size());

    var ban = layer.getBannedMovements().iterator().next();
    ((MacroscopicLinkSegment) ban.getSegmentTo()).setLinkSegmentType(
        layer.getLinkSegmentTypes().getFactory().registerNew("no access"));
    MacroscopicNetworkLayerUtils.removeInfrastructureWithoutModeAccess(layer);
    assertTrue(layer.getBannedMovements().isEmpty());

    PlanitNetworkWriterFactory.create(outputPath.toAbsolutePath().toString()).write(network);
    IdGenerator.reset();
    readerWarnings.count = 0;
    try {
      var readBack = readLayer(outputPath);
      assertTrue(readBack.getBannedMovements().isEmpty());
      assertEquals(0, readerWarnings.count);
    } finally {
      PlanItIOTestHelper.deleteFile(outputPath.resolve("network.xml").toAbsolutePath().toString());
    }
  }
}
