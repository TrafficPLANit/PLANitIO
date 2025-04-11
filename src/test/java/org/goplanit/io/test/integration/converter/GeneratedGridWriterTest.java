package org.goplanit.io.test.integration.converter;

import org.goplanit.io.converter.network.PlanitNetworkWriterFactory;
import org.goplanit.io.test.util.PlanItIOTestHelper;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetworkUtils;
import org.goplanit.utils.id.IdGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.logging.Logger;

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

      var writer = PlanitNetworkWriterFactory.create(GRID_WRITER_OUTPUT_PATH.toAbsolutePath().toString());
      writer.write(network);

      var referenceNetwork =
          Path.of(GRID_WRITER_OUTPUT_PATH.toAbsolutePath().toString(), "reference","network.xml");
      var createdNetworkPath =
          Path.of(GRID_WRITER_OUTPUT_PATH.toAbsolutePath().toString(), "network.xml");
      PlanItIOTestHelper.compareFiles(referenceNetwork.toString(), createdNetworkPath.toString(), true);

    }catch(Exception e){
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }

}
