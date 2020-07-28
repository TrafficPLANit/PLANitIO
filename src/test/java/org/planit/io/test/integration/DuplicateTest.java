package org.planit.io.test.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.io.test.util.PlanItIOTestHelper;
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
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateLinkSegmentTypeExternalId";
      String description = "testDuplicateLinkSegmentType";
      Integer maxIterations = 1;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
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
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateLinkSegmentExternalId";
      String description = "testDuplicateLinkSegment";
      Integer maxIterations = 1;

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
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
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateNodeExternalId";
      String description = "testDuplicateNode";
      Integer maxIterations = 1;

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
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
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateModeExternalId";
      String description = "testDuplicateMode";
      Integer maxIterations = 1;

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
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
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateZoneExternalId";
      String description = "testDuplicateZone";
      Integer maxIterations = 1;

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
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
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateTimePeriodExternalId";
      String description = "testDuplicateTimePeriod";
      Integer maxIterations = 1;

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
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
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateUserClassExternalId";
      String description = "testDuplicateUserClass";
      Integer maxIterations = 1;

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
      LOGGER.setLevel(oldLevel);
      fail("Exception for duplicate user class external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }
}