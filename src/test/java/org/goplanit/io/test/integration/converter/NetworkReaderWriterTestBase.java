package org.goplanit.io.test.integration.converter;

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.goplanit.io.converter.network.PlanitNetworkReader;
import org.goplanit.io.test.integration.TestBase;
import org.goplanit.io.test.util.PlanItIOTestHelper;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.test.LogRecorder;
import org.goplanit.utils.wrapper.MapWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base of the tests reading and writing what a network layer holds besides its graph, such as intersections and toll
 * roads, or what refers to it, such as a network pricing: reading a network's only layer, finding and listing what was
 * read by XML id, and counting the warnings the network reader logs while a test runs
 * <p>
 * Such tests check that the reader skips, discards or rejects what it cannot use, with one warning each, so each test
 * starts with fresh ids and an empty count.
 * </p>
 *
 * @author markr
 */
public abstract class NetworkReaderWriterTestBase extends TestBase {

  /** the network reader's logger, held here so the recorder stays attached to the instance the reader uses */
  private static final Logger READER_LOGGER = Logger.getLogger(PlanitNetworkReader.class.getCanonicalName());

  /** records the warnings the network reader logs while a test runs */
  protected LogRecorder readerWarnings;

  /**
   * The entity of a container with the given XML id
   *
   * @param <T> type of entity
   * @param container to look in
   * @param xmlId of the entity
   * @return the entity, null when none
   */
  protected static <T extends ExternalIdAble> T byXmlId(MapWrapper<?, T> container, String xmlId) {
    return container.firstMatch(entity -> xmlId.equals(entity.getXmlId()));
  }

  /**
   * The XML ids of entities, sorted
   *
   * @param entities to list
   * @return their XML ids
   */
  protected static Set<String> xmlIds(Iterable<? extends ExternalIdAble> entities) {
    var ids = new TreeSet<String>();
    entities.forEach(entity -> ids.add(entity.getXmlId()));
    return ids;
  }

  /**
   * Read the network in the given directory, and return its only layer
   *
   * @param directory holding the network file
   * @return the layer
   */
  protected static MacroscopicNetworkLayer readLayer(Path directory) {
    return PlanItIOTestHelper.readNetwork(directory).getTransportLayers().getFirst();
  }

  /** Start each test with fresh ids, recording the reader's warnings from zero */
  @BeforeEach
  public void recordReaderWarnings() {
    IdGenerator.reset();
    readerWarnings = new LogRecorder();
    READER_LOGGER.addHandler(readerWarnings);
  }

  /** Stop recording the reader's warnings */
  @AfterEach
  public void stopRecordingReaderWarnings() {
    READER_LOGGER.removeHandler(readerWarnings);
  }
}
