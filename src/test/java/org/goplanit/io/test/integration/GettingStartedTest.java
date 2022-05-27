package org.goplanit.io.test.integration;

import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.goplanit.io.demo.TraditionalStaticAssignmentProjectDemos;
import org.goplanit.logging.Logging;
import org.goplanit.utils.id.IdGenerator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the getting started demo example
 * 
 * @author markr
 *
 */
public class GettingStartedTest {

  /** the logger */
  private static Logger LOGGER = null;

  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(GettingStartedTest.class);
    } 
  }

  @AfterClass
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }

  /**
   * call getting started demo and make sure it at least runs. This makes sure the getting started demo on the website 
   * also runs. If any issues are found that require changing the demo, they should also be reflected in the getting started on the website 
   */
  @Test
  public void gettingStartedTest() {
    try {
      var path = Path.of("src","test","resources","testcases","getting_started","base");
      TraditionalStaticAssignmentProjectDemos.gettingStartedDemo(path.toString());
    }catch(Exception e){
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }

}
