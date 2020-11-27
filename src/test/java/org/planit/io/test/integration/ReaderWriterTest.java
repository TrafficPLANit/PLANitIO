package org.planit.io.test.integration;

import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.io.demo.PLANitStaticAssignmentProjectDemos;
import org.planit.logging.Logging;
import org.planit.utils.id.IdGenerator;

/**
 * Test the reader writer implementation separately
 * 
 * @author markr
 *
 */
public class ReaderWriterTest {

  /** the logger */
  private static Logger LOGGER = null;

  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(ReaderWriterTest.class);
    } 
  }

  @AfterClass
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }

  /**
   * This test should verify that reading a network leads to a network that is exactly the same as writing this network
   * back to disk and reading it again 
   */
  @Test
  public void reader_writer_test() {
    try {
      //TODO
      // populate with Sydney OSM network from the PLANitOSM repo -> write to disk as planit network, use that to store. Then apply this for the read/write test
    }catch(Exception e){
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }

}
