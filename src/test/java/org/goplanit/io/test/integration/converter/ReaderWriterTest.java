package org.goplanit.io.test.integration.converter;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.logging.Logger;

import org.goplanit.logging.Logging;
import org.goplanit.utils.id.IdGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test the reader writer implementation separately
 * 
 * @author markr
 *
 */
public class ReaderWriterTest {

  /** the logger */
  private static Logger LOGGER = null;

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(ReaderWriterTest.class);
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
  public void readerWriterTest() {
    try {
      //TODO
      // populate with Sydney planit network -> write to disk as planit network, use that to store.
      // Then apply this for the read/write test

      // todo: do the same for discrete demands read a file, then write, then read and compare
    }catch(Exception e){
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }

}
