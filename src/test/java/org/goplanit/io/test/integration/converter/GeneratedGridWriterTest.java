package org.goplanit.io.test.integration.converter;

import org.goplanit.io.converter.network.PlanitNetworkWriterFactory;
import org.goplanit.io.converter.zoning.PlanitZoningWriterFactory;
import org.goplanit.io.test.util.PlanItIOTestHelper;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetworkUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.goplanit.utils.zoning.connectoid.ZoneConnectoidType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the writer implementation separately and combine it with verifying the memory model Simple Grid creator
 * works as expected
 * 
 * @author markr
 *
 */
public class GeneratedGridWriterTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path TEST_CASE_PATH = Path.of("src","test","resources","testcases");
  private static final Path GRID_WRITER_OUTPUT_PATH = Path.of(TEST_CASE_PATH.toString(),"grid10x10");

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(GeneratedGridWriterTest.class);
    }
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }


  /**
   * This test should verify that reading a network leads to a network that is exactly the same as writing this network
   * back to disk and reading it again 
   */
  @Test
  public void simple10x10GridWriterTest() {
    try {
      var testToken = IdGenerator.createIdGroupingToken("simple10x10GridWriterTest");
      var network = MacroscopicNetworkUtils.createSimpleGrid(testToken, 10, 10);

      // add custom input property to test functionality as part of I/O
      network.getTransportLayers().getFirst().getLinks().getFirst().addInputProperty("dummyKey","dummyValue");

      var writer = PlanitNetworkWriterFactory.create(GRID_WRITER_OUTPUT_PATH.toAbsolutePath().toString());
      writer.write(network);

      var referenceNetwork =
          Path.of(GRID_WRITER_OUTPUT_PATH.toAbsolutePath().toString(), "reference","network.xml");
      var createdNetworkPath =
          Path.of(GRID_WRITER_OUTPUT_PATH.toAbsolutePath().toString(), "network.xml");

      var networkLayer = network.getTransportLayers().getFirst();
      var zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.getOdZones().getFactory().registerNew().setXmlId("A");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A`");
      zoning.setCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);

      zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
          zoning.getOdZones().getByXmlId("A"), networkLayer.getNodes().get(0), ZONE_ACCESS_EGRESS, 0).setXmlId("cA");
      zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(
          zoning.getOdZones().getByXmlId("A`"), networkLayer.getNodes().get(99), ZONE_ACCESS_EGRESS,  0).setXmlId("cA`");

      var zoningWriter =
          PlanitZoningWriterFactory.create(GRID_WRITER_OUTPUT_PATH.toAbsolutePath().toString(), network);
      zoningWriter.write(zoning);

      var referenceZoning =
          Path.of(GRID_WRITER_OUTPUT_PATH.toAbsolutePath().toString(), "reference","zoning.xml");
      var createdZoningPath =
          Path.of(GRID_WRITER_OUTPUT_PATH.toAbsolutePath().toString(), "zoning.xml");

      assert(PlanItIOTestHelper.compareFiles(
              referenceNetwork.toString(), createdNetworkPath.toString(), true));

      assert(PlanItIOTestHelper.compareFiles(
          referenceZoning.toString(), createdZoningPath.toString(), true));

    }catch(Exception e){
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }

}
