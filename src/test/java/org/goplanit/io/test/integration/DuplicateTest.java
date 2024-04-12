package org.goplanit.io.test.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.goplanit.io.test.util.PlanItIoTestRunner;
import org.goplanit.logging.Logging;
import org.goplanit.utils.id.IdGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * JUnit test cases for duplicate tests for TraditionalStaticAssignment
 * 
 * @author gman6028, markr
 *
 */
public class DuplicateTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path testCasePath = Path.of("src","test","resources","testcases");

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(DuplicateTest.class);
    } 
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }
  
  /**
   * Test that a duplicate XML Id for a Link Segment Type is flagged as a error
   */
  @Test
  public void test_duplicate_link_segment_type_xml_id() {
    try {
      final String projectPath = Path.of(testCasePath.toString(),"duplicate_tests", "xml","duplicateLinkSegmentTypeXmlId").toString();
      String description = "testDuplicateLinkSegmentType";
      
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setupAndExecuteDefaultAssignment();
      fail("Exception for duplicate link segment type XML Id was not thrown");
    } catch (Exception e) {
      LOGGER.severe("EXCEPTION=CORRECT");
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate XML Id for a Link Segment is flagged as a error
   */
  @Test
  public void test_duplicate_link_segment_xml_id() {
    try {
      final String projectPath = Path.of(testCasePath.toString(),"duplicate_tests", "xml","duplicateLinkSegmentXmlId").toString();
      String description = "testDuplicateLinkSegment";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setupAndExecuteDefaultAssignment();
      
      LOGGER.setLevel(oldLevel);
      fail("Exception for duplicate link segment XML Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate XML Id for a Node is flagged as a error
   */
  @Test
  public void test_duplicate_node_xml_id() {
    try {
      final String projectPath = Path.of(testCasePath.toString(),"duplicate_tests", "xml","duplicateNodeXmlId").toString();
      String description = "testDuplicateNode";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setupAndExecuteDefaultAssignment();
      LOGGER.setLevel(oldLevel);
      fail("Exception for duplicate node XML Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate XML Id for a Mode is flagged as a error
   */
  @Test
  public void test_duplicate_mode_xml_id() {
    try {
      final String projectPath = Path.of(testCasePath.toString(),"duplicate_tests", "xml","duplicateModeXmlId").toString();
      String description = "testDuplicateMode";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setupAndExecuteDefaultAssignment();
      LOGGER.setLevel(oldLevel);
      fail("Exception for duplicate mode XML Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate XML Id for a Zone is flagged as a error
   */
  @Test
  public void test_duplicate_zone_xml_id() {
    try {
      final String projectPath = Path.of(testCasePath.toString(),"duplicate_tests", "xml","duplicateZoneXmlId").toString();
      String description = "testDuplicateZone";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setupAndExecuteDefaultAssignment();
      LOGGER.setLevel(oldLevel);
      fail("Exception for duplicate zone XML Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate XML Id for a Time Period is flagged as a error
   */
  @Test
  public void test_duplicate_time_period_xml_id() {
    try {
      final String projectPath = Path.of(testCasePath.toString(),"duplicate_tests", "xml","duplicateTimePeriodXmlId").toString();
      String description = "testDuplicateTimePeriod";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setupAndExecuteDefaultAssignment();
      LOGGER.setLevel(oldLevel);
      fail("Exception for duplicate time period XML Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate XML Id for a User class is flagged as a error
   */
  @Test
  public void test_duplicate_user_class_xml_id() {
    try {
      final String projectPath = Path.of(testCasePath.toString(),"duplicate_tests", "xml","duplicateUserClassXmlId").toString();
      String description = "testDuplicateUserClass";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setupAndExecuteDefaultAssignment();
      LOGGER.setLevel(oldLevel);
      fail("Exception for duplicate user class XML Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }
}