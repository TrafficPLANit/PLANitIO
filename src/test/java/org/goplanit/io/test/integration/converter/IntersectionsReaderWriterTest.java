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
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicBannedTurn;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.NonSpatialIntersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.xml.utils.JAXBUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests reading and writing intersections in the native PLANit network format: the round trip, a layer without
 * intersections, and what the reader skips, discards or rejects. Also that removing infrastructure takes the banned
 * turns on it along, so the network is still written and read back without errors
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

  /** a movement as the XML ids of its from and to segments, its length, external id and direct ban with its modes */
  private static String asText(SpatialMovement movement) {
    return String.join("|", movement.getLinkSegmentFrom().getXmlId(), movement.getLinkSegmentTo().getXmlId(),
        String.valueOf(movement.getLengthKm()), String.valueOf(movement.getExternalId()),
        movement.hasDirectBan() ? "direct" : "none", movement.getExplicitlyDirectlyBannedModes().stream()
            .map(Mode::getXmlId).sorted().collect(Collectors.joining(",")));
  }

  /** each movement of the intersection, see {@link #asText(SpatialMovement)}, an empty set for a non-spatial one */
  private static Set<String> movements(Intersection intersection) {
    return intersection instanceof SpatialIntersection ? ((SpatialIntersection) intersection).getMovements().stream()
        .map(IntersectionsReaderWriterTest::asText).collect(Collectors.toSet()) : Set.of();
  }

  /** the internal segments with a signal of the intersection, an empty list for a non-spatial one */
  private static List<MacroscopicLinkSegment> internalSegmentsWithSignal(Intersection intersection) {
    return intersection instanceof SpatialIntersection ?
        ((SpatialIntersection) intersection).getInternalSegmentsWithSignal() : List.of();
  }

  /** each banned turn of the layer as the XML ids of its from and to segments and the modes it names */
  private static Set<String> bannedTurns(MacroscopicNetworkLayer layer) {
    return layer.getBannedTurns().stream().map(MacroscopicBannedTurn.class::cast).map(ban -> String.join("|",
        ban.getLinkSegmentFrom().getXmlId(), ban.getLinkSegmentTo().getXmlId(), ban.getExplicitlyBannedModes()
            .stream().map(Mode::getXmlId).sorted().collect(Collectors.joining(",")))).collect(Collectors.toSet());
  }

  /** the spatial movements of the layer that belong to no intersection, see {@link #asText(SpatialMovement)} */
  private static Set<String> independentMovements(MacroscopicNetworkLayer layer) {
    var ofIntersections = layer.getIntersections().stream().flatMap(i -> movements(i).stream())
        .collect(Collectors.toSet());
    return layer.getSpatialMovements().stream().map(IntersectionsReaderWriterTest::asText)
        .filter(movement -> !ofIntersections.contains(movement)).collect(Collectors.toSet());
  }

  /** the approaches of the intersection with a signal of their own */
  private static List<MacroscopicLinkSegment> approachesWithOwnSignal(Intersection intersection) {
    return intersection.getApproachSegments().stream().filter(intersection::hasOwnSignal).collect(Collectors.toList());
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

  // READING

  /** A v2 network without intersections reads as before, with an empty intersections container */
  @Test
  public void inputWithoutIntersectionsReadsNone() {
    var layer = readLayer(Path.of(TEST_CASE_PATH.toString(), "grid10x10", "reference"));

    assertFalse(layer.getNodes().isEmpty());
    assertTrue(layer.getIntersections().isEmpty());
  }

  /** Every property of the intersections, banned turns and movements in the input is read */
  @Test
  public void intersectionsAreRead() {
    var layer = readLayer(INTERSECTIONS_PATH.resolve("roundtrip").resolve("input"));

    assertEquals(3, layer.getIntersections().size());
    var i1 = byXmlId(layer, "i1");
    assertTrue(i1 instanceof NonSpatialIntersection);
    assertEquals("ext-1", i1.getExternalId());
    assertEquals(IntersectionControlType.SIGNALISED, i1.getControlType());
    assertEquals(EnumSet.of(IntersectionType.JUNCTION, IntersectionType.CROSSING), i1.getTypes());
    assertEquals(Set.of("0"), nodeIds(i1));
    assertEquals(Set.of("0", "2", "4", "7"), segmentIds(i1.getApproachSegments()));
    assertEquals(Set.of("0", "2", "7"), segmentIds(approachesWithOwnSignal(i1)));

    var i2 = byXmlId(layer, "i2");
    assertTrue(i2 instanceof NonSpatialIntersection);
    assertFalse(i2.hasExternalId());
    assertEquals(IntersectionControlType.UNSIGNALISED, i2.getControlType());
    assertEquals(EnumSet.of(IntersectionType.CROSSING), i2.getTypes());
    assertEquals(Set.of("9"), segmentIds(approachesWithOwnSignal(i2)));

    var i3 = (SpatialIntersection) byXmlId(layer, "i3");
    assertEquals(Set.of("5", "6"), nodeIds(i3));
    assertEquals(Set.of("8", "12", "14"), segmentIds(i3.getApproachSegments()));
    assertEquals(Set.of("10", "11"), segmentIds(i3.getInternalSegments()));
    assertEquals(Set.of("11"), segmentIds(i3.getInternalSegmentsWithSignal()));
    assertSame(i3, layer.getIntersections().getByMemberNode(i3.getMemberNodes().get(1)));
    /* banned modes imply a direct ban, as for m3 */
    assertEquals(Set.of("8|15|0.02|ext-m1|direct|", "12|9|0.0|null|none|", "14|9|0.02|null|direct|car"),
        movements(i3));
    assertEquals(Set.of("0|8|0.2|null|direct|"), independentMovements(layer));
    assertEquals(4, layer.getSpatialMovements().size());
    assertEquals(Set.of("0|5|", "2|6|car"), bannedTurns(layer));
    assertEquals(0, readerWarnings.count);
  }

  /**
   * An intersection with an unknown node, or a node read earlier for another intersection, is not read; an unknown or
   * refused segment is skipped; an intersection without approaches is read as such; a movement with an unknown segment
   * or mode, or not starting on an approach, is skipped; an independent movement without a direct ban is skipped; a
   * banned turn with an unknown mode is skipped. Each loss is warned once
   */
  @Test
  public void invalidReferencesAreSkippedWithOneWarningEach() {
    var layer = readLayer(INTERSECTIONS_PATH.resolve("lenient"));

    assertEquals(2, layer.getIntersections().size());
    assertNull(byXmlId(layer, "i2"));
    assertNull(byXmlId(layer, "i3"));

    var i1 = byXmlId(layer, "i1");
    assertEquals(Set.of("0", "2"), segmentIds(i1.getApproachSegments()));

    var i4 = (SpatialIntersection) byXmlId(layer, "i4");
    assertTrue(i4.getApproachSegments().isEmpty());
    assertEquals(Set.of("10"), segmentIds(i4.getInternalSegments()));
    assertTrue(i4.getMovements().isEmpty());
    assertTrue(layer.getSpatialMovements().isEmpty());
    assertEquals(Set.of("0|5|"), bannedTurns(layer));
    assertEquals(11, readerWarnings.count);
  }

  /** A segment listed both as a plain internal segment and as one with a signal is kept with a signal, warned once */
  @Test
  public void internalSegmentListedTwiceIsKeptWithSignal() {
    var i3 = (SpatialIntersection) byXmlId(readLayer(INTERSECTIONS_PATH.resolve("listed_twice")), "i3");

    assertEquals(Set.of("10", "11"), segmentIds(i3.getInternalSegments()));
    assertEquals(Set.of("10"), segmentIds(i3.getInternalSegmentsWithSignal()));
    assertEquals(1, readerWarnings.count);
  }

  /** An intersection still using the replaced segments lists makes reading fail, rather than reading it without its
   * segments */
  @Test
  public void replacedSegmentsListFailsReading() {
    var failure = assertThrows(
        PlanItRunTimeException.class, () -> readLayer(INTERSECTIONS_PATH.resolve("replaced_segments")));
    assertTrue(failure.getCause().getCause() instanceof JAXBUtils.ElementRemovedFromSchemaException);
  }

  /** A banned turn written before banned turns named their link segments as fromlsref and tolsref makes reading fail,
   * rather than reading the turn without its segments */
  @Test
  public void banWrittenBeforeLinkSegmentRefsFailsReading() {
    assertThrows(PlanItRunTimeException.class, () -> readLayer(INTERSECTIONS_PATH.resolve("ban_before_lsrefs")));
  }

  // WRITING

  /** Intersections, banned turns and movements written and read back are equivalent to those written */
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
        assertEquals(written instanceof SpatialIntersection, read instanceof SpatialIntersection);
        assertEquals(written.getExternalId(), read.getExternalId());
        assertEquals(written.getControlType(), read.getControlType());
        assertEquals(written.getTypes(), read.getTypes());
        assertEquals(nodeIds(written), nodeIds(read));
        assertEquals(segmentIds(written.getApproachSegments()), segmentIds(read.getApproachSegments()));
        assertEquals(segmentIds(approachesWithOwnSignal(written)), segmentIds(approachesWithOwnSignal(read)));
        assertEquals(segmentIds(IntersectionUtils.getInternalSegmentsOf(written)),
            segmentIds(IntersectionUtils.getInternalSegmentsOf(read)));
        assertEquals(segmentIds(internalSegmentsWithSignal(written)), segmentIds(internalSegmentsWithSignal(read)));
        assertEquals(movements(written), movements(read));
      }
      assertEquals(original.getSpatialMovements().size(), readBack.getSpatialMovements().size());
      assertEquals(independentMovements(original), independentMovements(readBack));
      assertEquals(bannedTurns(original), bannedTurns(readBack));

      /* empty lists, signals following the control type and absent external ids are left out */
      var written = Files.readString(projectPath.resolve("network.xml"));
      assertEquals(1, written.split("<internalsegments ", -1).length - 1);
      assertEquals(1, written.split("<internalsignalsegments>", -1).length - 1);
      assertEquals(2, written.split("signal=\"", -1).length - 1);
      assertEquals(3, written.split("externalid=\"ext-", -1).length - 1);
      /* a direct ban is written explicitly, its modes only when it does not ban every mode, as for banned turns */
      assertEquals(1, written.split("<spatialintersection ", -1).length - 1);
      /* each intersection is written with its id first */
      assertEquals(written.split("<intersection ", -1).length - 1, written.split("<intersection id=\"", -1).length - 1);
      assertEquals(1, written.split("<spatialintersection id=\"", -1).length - 1);
      assertEquals(3, written.split("directban=\"true\"", -1).length - 1);
      assertEquals(1, written.split("bannedmoderefs=\"", -1).length - 1);
      assertEquals(1, written.split("tolsref=\"6\" moderefs=\"car\"/>", -1).length - 1);
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
    assertEquals(2, layer.getBannedTurns().size());

    var ban = layer.getBannedTurns().stream().filter(b -> "0".equals(b.getXmlId())).findFirst().orElseThrow();
    ((MacroscopicLinkSegment) ban.getSegmentTo()).setLinkSegmentType(
        layer.getLinkSegmentTypes().getFactory().registerNew("no access"));
    layer.getLayerModifier().removeInfrastructureWithoutModeAccess();
    assertEquals(Set.of("2|6|car"), bannedTurns(layer));

    PlanitNetworkWriterFactory.create(outputPath.toAbsolutePath().toString()).write(network);
    IdGenerator.reset();
    readerWarnings.count = 0;
    try {
      var readBack = readLayer(outputPath);
      assertEquals(Set.of("2|6|car"), bannedTurns(readBack));
      assertEquals(0, readerWarnings.count);
    } finally {
      PlanItIOTestHelper.deleteFile(outputPath.resolve("network.xml").toAbsolutePath().toString());
    }
  }
}
