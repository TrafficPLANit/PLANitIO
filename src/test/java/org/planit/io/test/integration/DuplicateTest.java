package org.planit.io.test.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.io.test.util.PlanItIOTestRunner;
import org.planit.logging.Logging;
import org.planit.utils.id.IdGenerator;

/**
 * JUnit test cases for duplicate tests for TraditionalStaticAssignment
 * 
 * @author gman6028, markr
 *
 */
public class DuplicateTest {

  /** the logger */
  private static Logger LOGGER = null;

  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(DuplicateTest.class);
    } 
  }

  @AfterClass
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }
  
  /**
   * Test that a duplicate external Id for a Link Segment Type is flagged as a error
   */
  @Test
  public void test_duplicate_link_segment_type_external_id() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateLinkSegmentTypeXmlId";
      String description = "testDuplicateLinkSegmentType";
      
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setupAndExecuteDefaultAssignment();
      fail("Exception for duplicate link segment type external Id was not thrown");
    } catch (Exception e) {
      LOGGER.severe("EXCEPTION=CORRECT");
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate external Id for a Link Segment is flagged as a error
   */
  @Test
  public void test_duplicate_link_segment_external_id() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateLinkSegmentXmlId";
      String description = "testDuplicateLinkSegment";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);        
      runner.setupAndExecuteDefaultAssignment();
      
      LOGGER.setLevel(oldLevel);
      fail("Exception for duplicate link segment external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate external Id for a Node is flagged as a error
   */
  @Test
  public void test_duplicate_node_external_id() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateNodeXmlId";
      String description = "testDuplicateNode";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setupAndExecuteDefaultAssignment();
      LOGGER.setLevel(oldLevel);
      fail("Exception for duplicate node external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate external Id for a Mode is flagged as a error
   */
  @Test
  public void test_duplicate_mode_external_id() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateModeXmlId";
      String description = "testDuplicateMode";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setupAndExecuteDefaultAssignment();
      LOGGER.setLevel(oldLevel);
      fail("Exception for duplicate mode external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate external Id for a Zone is flagged as a error
   */
  @Test
  public void test_duplicate_zone_external_id() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateZoneXmlId";
      String description = "testDuplicateZone";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setupAndExecuteDefaultAssignment();
      LOGGER.setLevel(oldLevel);
      fail("Exception for duplicate zone external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate external Id for a Time Period is flagged as a error
   */
  @Test
  public void test_duplicate_time_period_external_id() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateTimePeriodXmlId";
      String description = "testDuplicateTimePeriod";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setupAndExecuteDefaultAssignment();
      LOGGER.setLevel(oldLevel);
      fail("Exception for duplicate time period external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate external Id for a Time Period is flagged as a error
   */
  @Test
  public void test_duplicate_user_class_external_id() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateUserClassXmlId";
      String description = "testDuplicateUserClass";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setupAndExecuteDefaultAssignment();
      LOGGER.setLevel(oldLevel);
      fail("Exception for duplicate user class external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }
}