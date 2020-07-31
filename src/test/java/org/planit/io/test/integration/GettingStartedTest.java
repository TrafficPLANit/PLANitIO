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
 * Test the getting started demo example
 * 
 * @author mraa2518
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
  public void getting_started_example_test() {
    try {
      PLANitStaticAssignmentProjectDemos.gettingStartedDemo("src\\test\\resources\\testcases\\getting_started\\xml");
    }catch(Exception e){
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }

}
