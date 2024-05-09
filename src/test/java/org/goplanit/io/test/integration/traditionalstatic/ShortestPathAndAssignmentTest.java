package org.goplanit.io.test.integration.traditionalstatic;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.goplanit.demands.Demands;
import org.goplanit.io.test.integration.TestBase;
import org.goplanit.io.test.util.PlanItIOTestHelper;
import org.goplanit.io.test.util.PlanItIoTestRunner;
import org.goplanit.io.test.util.PlanItInputBuilder4Testing;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.test.LinkSegmentExpectedResultsDto;
import org.goplanit.utils.test.TestOutputDto;
import org.goplanit.utils.time.TimePeriod;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JUnit test cases for shortest path (AON) tests for TraditionalStaticAssignment
 * 
 * @author gman6028, markr
 *
 */
public class ShortestPathAndAssignmentTest extends TestBase {

  //
  // The numbering of the nodes and links for the grid-like structure is as follows:
  //
  //          9
  //    5 > > > > > > > > > > > > > > > >25 (5,5)
  //    ^                                ^
  //  4 ^          ^                     ^ 40
  //    ^     7    ^                     ^
  //    3 -------- 8  >>                 ^
  //    |          |                     ^
  //  2 |          | 11                  ^
  //    |     6    |                     ^
  //    2 -------- 7 > >                 ^
  //    |          |                     ^
  //  1 |          | 10        ^         ^
  //    |     5    |           ^         ^
  //    1 -------- 6 --------- 11 > > > 21
  //   (0,0)
  //


  /** the logger */
  private static Logger LOGGER = null;

  // zones A to D

  // nodes 1 to 20

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(ShortestPathAndAssignmentTest.class);
    } 
  }
  
  @BeforeEach
  public void beforeTest() {
    pathMap = new TreeMap<>();
    odMap = new TreeMap<>();
    linkResults = new TreeMap<>();
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }
  
  /**
   * Test that PlanItProject reads in the values of one initial costs file
   * 
   * This test case uses the example from the course notes of ITLS6102 Strategic
   * Transport Planning, Lecture 1 (Overview), the example on Page 122 of the 2019
   * course notes.
   * 
   * Time_Period_1 uses route A to B in the example, which has a total route cost
   * of 85 (the fifth argument in the ResultDto constructor).
   */
  @Test
  public void test_basic_shortest_path_algorithm_a_to_b_one_initial_cost_file() {
    try {
      String projectPath = Path.of(TEST_CASE_PATH.toString(),"basicShortestPathAlgorithm","xml","AtoB").toString();
      String initialCostPath = Path.of(projectPath,"initial_link_segment_costs.csv").toString();
      String description = "testBasic1";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      runner.registerInitialLinkSegmentCost(initialCostPath);

      runner.setupAndExecuteDefaultAssignment();        

      /* compare results */      
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath,runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * Test that PlanItProject reads in the values of two initial costs files
   * 
   * This test case uses the example from the course notes of ITLS6102 Strategic
   * Transport Planning, Lecture 1 (Overview), the example on Page 122 of the 2019
   * course notes.
   * 
   * Time_Period_1 uses route A to B in the example, which has a total route cost
   * of 85.
   */
  @Test
  public void test_basic_shortest_path_algorithm_a_to_b_two_initial_cost_files() {
    try {
      String projectPath = Path.of(TEST_CASE_PATH.toString(),"basicShortestPathAlgorithm","xml","AtoB").toString();
      String initialCostPath = Path.of(projectPath,"initial_link_segment_costs.csv").toString();
      String initialCostPath1 = Path.of(projectPath,"initial_link_segment_costs1.csv").toString();
      String description = "testBasic1";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      /* two initial costs, second one is the one that should be used in the end */
      //runner.registerInitialLinkSegmentCost(initialCostPath);
      runner.registerInitialLinkSegmentCost(initialCostPath1);
      runner.setupAndExecuteDefaultAssignment();       
      
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * Test of results for TraditionalStaticAssignment for simple test case.
   * 
   * This test case uses the example from the course notes of ITLS6102 Strategic
   * Transport Planning, Lecture 1 (Overview), the example on Page 122 of the 2019
   * course notes.
   * 
   * This test case uses route A to C in the example, which has a total route cost
   * of 77 (the fifth argument in the ResultDto constructor).
   * 
   * This test sets the maximum speed (1) on each link segment.  The link segment type maximum speed is set to a value 
   * which is too high (2).  The code should take the lower of these values for each link, giving the correct result.
   */
 @Test
  public void test_basic_shortest_path_algorithm_a_to_c_with_link_segment_maximum_speed() {
    try {
      String projectPath = Path.of(TEST_CASE_PATH.toString(),"basicShortestPathAlgorithm","xml","AtoCLinkSegmentMaximumSpeed").toString();
      String description = "testBasic2";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();       

      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);

      /* compare results */
      var network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      var mode1 = network.getModes().getByXmlId("1");
      var demands = testOutputDto.getB().demands.getFirst();
      var timePeriod = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
      var memoryOutputFormatter = testOutputDto.getA();

      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node6XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,2000, 10, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node11XmlId, node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1, 12,2000, 12, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node12XmlId, node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1,8, 2000, 8, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node13XmlId, node12XmlId, new LinkSegmentExpectedResultsDto(12, 13, 1,47, 2000, 47, 1));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter,null, linkResults);

      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zoneAXmlId, zoneCXmlId,"[1,6,11,12,13]");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zoneCXmlId, zoneAXmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zoneCXmlId, zoneCXmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, null, pathMap);

      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zoneAXmlId, zoneAXmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zoneAXmlId, zoneCXmlId, 77.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zoneCXmlId, zoneAXmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zoneCXmlId, zoneCXmlId, 0.0);
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);

    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
  
  /**
   * Test of results for TraditionalStaticAssignment for simple test case.
   * 
   * This test case uses the example from the course notes of ITLS6102 Strategic
   * Transport Planning, Lecture 1 (Overview), the example on Page 122 of the 2019
   * course notes.
   * 
   * This test case uses route A to C in the example, which has a total route cost
   * of 77 (the fifth argument in the ResultDto constructor).
   */
  @Test
  public void test_basic_shortest_path_algorithm_a_to_c() {
    try {
      String projectPath = Path.of(TEST_CASE_PATH.toString(),"basicShortestPathAlgorithm","xml","AtoC").toString();
      String description = "testBasic2";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      var testOutputDto = runner.setupAndExecuteDefaultAssignment();

      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);

      /* compare results */
      var memoryOutputFormatter = testOutputDto.getA();
      var network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      var mode1 = network.getModes().getByXmlId("1");
      var demands = testOutputDto.getB().demands.getFirst();
      var timePeriod = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));

      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node6XmlId, node1XmlId,
              new LinkSegmentExpectedResultsDto(1, 6, 1, 10,2000, 10, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node11XmlId, node6XmlId,
              new LinkSegmentExpectedResultsDto(6, 11, 1, 12,2000, 12, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node12XmlId, node11XmlId,
              new LinkSegmentExpectedResultsDto(11, 12, 1,8, 2000, 8, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node13XmlId, node12XmlId,
              new LinkSegmentExpectedResultsDto(12, 13, 1,47, 2000, 47, 1));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter, null, linkResults);

      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zoneAXmlId, zoneCXmlId, "[1,6,11,12,13]");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zoneCXmlId, zoneAXmlId, "");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zoneCXmlId, zoneCXmlId, "");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, null, pathMap);

      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zoneAXmlId, zoneAXmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zoneAXmlId, zoneCXmlId, 77.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zoneCXmlId, zoneAXmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zoneCXmlId, zoneCXmlId, 0.0);
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * Identical to #ShortestPathTest.test_basic_shortest_path_algorithm_a_to_c() except that we write out the path geometries.
   */
  @Test
  public void test_basic_shortest_path_algorithm_a_to_c_geometry() {
    try {
      String projectPath = Path.of(TEST_CASE_PATH.toString(),"basicShortestPathAlgorithm","xml","AtoCWithPathGeometry").toString();
      String description = "testBasic2";
      String csvFileName = "Time_Period_1_2.csv";
      String xmlFileName = "Time_Period_1.xml";

      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);

      var ta = runner.getRawTrafficAssignmentConfigurator();
      var linkConf = ta.getOutputConfiguration().getOutputTypeConfiguration(OutputType.LINK);
      linkConf.removeAllProperties();
      linkConf.addProperties(
              OutputPropertyType.LINK_SEGMENT_GEOMETRY,
              OutputPropertyType.UPSTREAM_NODE_XML_ID,
              OutputPropertyType.DOWNSTREAM_NODE_XML_ID,
              OutputPropertyType.LINK_SEGMENT_XML_ID);

      // deactivate all but paths and attach path geometries
      ta.getOutputConfiguration().deregisterOutputTypeConfiguration(OutputType.OD);
      var pathConfiguration = ta.getOutputConfiguration().getOutputTypeConfiguration(OutputType.PATH);
      pathConfiguration.addProperty(OutputPropertyType.PATH_GEOMETRY);

      ta.getOutputConfiguration().setPersistZeroFlow(true);

      var testOutputDto = runner.setupAndExecuteDefaultAssignment();


      /* compare results */
      var memoryOutputFormatter = testOutputDto.getA();

      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);

    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * Test of results for TraditionalStaticAssignment for simple test case.
   * 
   * This test case uses the example from the course notes of ITLS6102 Strategic
   * Transport Planning, Lecture 1 (Overview), the example on Page 122 of the 2019
   * course notes.
   * 
   * This test case uses route A to D in the example, which has a total route cost
   * of 108 (the fifth argument in the ResultDto constructor).
   */
  @Test
  public void test_basic_shortest_path_algorithm_a_to_d() {
    try {
      String projectPath = Path.of(TEST_CASE_PATH.toString(),"basicShortestPathAlgorithm","xml","AtoD").toString();
      String description = "testBasic3";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();       

      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);

      /* compare results */
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = testOutputDto.getB().demands.getFirst();
      TimePeriod timePeriod = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));

      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node6XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,2000, 10, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node8XmlId, node7XmlId, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,2000, 12, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node9XmlId, node8XmlId, new LinkSegmentExpectedResultsDto(8, 9, 1, 20,2000, 20, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node11XmlId, node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1, 12,2000, 12, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node7XmlId, node12XmlId, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,2000, 5, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node14XmlId, node9XmlId, new LinkSegmentExpectedResultsDto(9, 14, 1, 10,2000, 10, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node12XmlId, node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1,8, 2000, 8, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node15XmlId, node14XmlId, new LinkSegmentExpectedResultsDto(14, 15, 1,10, 2000, 10, 1));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node20XmlId, node15XmlId, new LinkSegmentExpectedResultsDto(15, 20, 1,21, 2000, 21, 1));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter,null, linkResults);

      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zoneAXmlId, zoneDXmlId,"[1,6,11,12,7,8,9,14,15,20]");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zoneDXmlId, zoneAXmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zoneDXmlId, zoneDXmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, null, pathMap);

      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zoneAXmlId, zoneAXmlId,0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zoneAXmlId, zoneDXmlId,108.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zoneDXmlId, zoneAXmlId,0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zoneDXmlId, zoneDXmlId,0.0);

      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
  
  @Test
  public void test_basic_shortest_path_algorithm_three_time_periods_record_zero_flow() {
    try {
      String projectPath = Path.of(TEST_CASE_PATH.toString(),"basicShortestPathAlgorithm","xml","ThreeTimePeriodsRecordZeroFlow").toString();
      String description = "testBasic13";
      String csvFileName1 = "Time_Period_1_2.csv";
      String odCsvFileName1 = "Time_Period_1_1.csv";
      String csvFileName2 = "Time_Period_2_2.csv";
      String odCsvFileName2 = "Time_Period_2_1.csv";
      String csvFileName3 = "Time_Period_3_2.csv";
      String odCsvFileName3 = "Time_Period_3_1.csv";
      String xmlFileName1 = "Time_Period_1.xml";
      String xmlFileName2 = "Time_Period_2.xml";
      String xmlFileName3 = "Time_Period_3.xml";
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(
              projectPath, runIdDescription, csvFileName1, xmlFileName1, csvFileName2, xmlFileName2, csvFileName3, xmlFileName3);
      PlanItIOTestHelper.deleteOdFiles(
              projectPath, runIdDescription, odCsvFileName1, xmlFileName1, odCsvFileName2, xmlFileName2, odCsvFileName3, xmlFileName3);
      PlanItIOTestHelper.deletePathFiles(
              projectPath, runIdDescription, csvFileName1, xmlFileName1, csvFileName2, xmlFileName2, csvFileName3, xmlFileName3);

      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(true); // <- zero flow
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();       

      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName3, xmlFileName3);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName3, xmlFileName3);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName3, xmlFileName3);

      /* compare results */
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = testOutputDto.getB().demands.getFirst();

      TimePeriod timePeriod0 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
      TimePeriod timePeriod1 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("1"));
      TimePeriod timePeriod2 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("2"));

      // LINK RESULTS
      {
        //tp=0
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node3XmlId, node8XmlId, new LinkSegmentExpectedResultsDto(8, 3, 1, 8, 2000, 8, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node4XmlId, node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node5XmlId, node4XmlId, new LinkSegmentExpectedResultsDto(4, 5, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node6XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node7XmlId, node12XmlId, new LinkSegmentExpectedResultsDto(12, 7, 1, 5, 2000, 5, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node8XmlId, node7XmlId, new LinkSegmentExpectedResultsDto(7, 8, 1, 12, 2000, 12, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node10XmlId, node5XmlId, new LinkSegmentExpectedResultsDto(5, 10, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node11XmlId, node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1, 12, 2000, 12, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node12XmlId, node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1, 8, 2000, 8, 1));

        //tp=1
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod1, mode1, node6XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod1, mode1, node11XmlId, node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1, 12, 2000, 12, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod1, mode1, node12XmlId, node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1, 8, 2000, 8, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod1, mode1, node13XmlId, node12XmlId, new LinkSegmentExpectedResultsDto(12, 13, 1, 47, 2000, 47, 1));

        //tp=2
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node6XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node7XmlId, node12XmlId, new LinkSegmentExpectedResultsDto(12, 7, 1, 5, 2000, 5, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node8XmlId, node7XmlId, new LinkSegmentExpectedResultsDto(7, 8, 1, 12, 2000, 12, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node9XmlId, node8XmlId, new LinkSegmentExpectedResultsDto(8, 9, 1, 20, 2000, 20, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node11XmlId, node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1, 12, 2000, 12, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node12XmlId, node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1, 8, 2000, 8, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node14XmlId, node9XmlId, new LinkSegmentExpectedResultsDto(9, 14, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node15XmlId, node14XmlId, new LinkSegmentExpectedResultsDto(14, 15, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node20XmlId, node15XmlId, new LinkSegmentExpectedResultsDto(15, 20, 1, 21, 2000, 21, 1));
        PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter, null, linkResults);
      }

      // PATH RESULTS
      {
        //tp=0
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneAXmlId, zoneAXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneAXmlId, zoneBXmlId, "[1,6,11,12,7,8,3,4,5,10]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneAXmlId, zoneCXmlId, "[1,6,11,12,13]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneAXmlId, zoneDXmlId, "[1,6,11,12,7,8,9,14,15,20]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneBXmlId, zoneAXmlId, "[10,5,4,3,8,7,12,11,6,1]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneBXmlId, zoneBXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneBXmlId, zoneCXmlId, "[10,15,14,13]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneBXmlId, zoneDXmlId, "[10,15,20]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneCXmlId, zoneAXmlId, "[13,12,11,6,1]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneCXmlId, zoneBXmlId, "[13,14,15,10]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneCXmlId, zoneCXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneCXmlId, zoneDXmlId, "[13,14,15,20]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneDXmlId, zoneAXmlId, "[20,15,14,9,8,7,12,11,6,1]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneDXmlId, zoneBXmlId, "[20,15,10]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneDXmlId, zoneCXmlId, "[20,15,14,13]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneDXmlId, zoneDXmlId, "");

        //tp=1
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneAXmlId, zoneAXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneAXmlId, zoneBXmlId, "[1,6,11,12,7,8,3,4,5,10]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneAXmlId, zoneCXmlId, "[1,6,11,12,13]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneAXmlId, zoneDXmlId, "[1,6,11,12,7,8,9,14,15,20]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneBXmlId, zoneAXmlId, "[10,5,4,3,8,7,12,11,6,1]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneBXmlId, zoneBXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneBXmlId, zoneCXmlId, "[10,15,14,13]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneBXmlId, zoneDXmlId, "[10,15,20]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneCXmlId, zoneAXmlId, "[13,12,11,6,1]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneCXmlId, zoneBXmlId, "[13,14,15,10]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneCXmlId, zoneCXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneCXmlId, zoneDXmlId, "[13,14,15,20]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneDXmlId, zoneAXmlId, "[20,15,14,9,8,7,12,11,6,1]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneDXmlId, zoneBXmlId, "[20,15,10]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneDXmlId, zoneCXmlId, "[20,15,14,13]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneDXmlId, zoneDXmlId, "");

        //tp=2
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneAXmlId, zoneAXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneAXmlId, zoneBXmlId, "[1,6,11,12,7,8,3,4,5,10]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneAXmlId, zoneCXmlId, "[1,6,11,12,13]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneAXmlId, zoneDXmlId, "[1,6,11,12,7,8,9,14,15,20]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneBXmlId, zoneAXmlId, "[10,5,4,3,8,7,12,11,6,1]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneBXmlId, zoneBXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneBXmlId, zoneCXmlId, "[10,15,14,13]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneBXmlId, zoneDXmlId, "[10,15,20]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneCXmlId, zoneAXmlId, "[13,12,11,6,1]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneCXmlId, zoneBXmlId, "[13,14,15,10]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneCXmlId, zoneCXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneCXmlId, zoneDXmlId, "[13,14,15,20]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneDXmlId, zoneAXmlId, "[20,15,14,9,8,7,12,11,6,1]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneDXmlId, zoneBXmlId, "[20,15,10]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneDXmlId, zoneCXmlId, "[20,15,14,13]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneDXmlId, zoneDXmlId, "");

        PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, null, pathMap);
      }

      // OD RESULTS
      {
        //tp=0
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneAXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneAXmlId, zoneBXmlId, 85.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneAXmlId, zoneCXmlId, 77.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneAXmlId, zoneDXmlId, 108.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneBXmlId, zoneAXmlId, 85.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneBXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneBXmlId, zoneCXmlId, 18.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneBXmlId, zoneDXmlId, 24.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneCXmlId, zoneAXmlId, 77.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneCXmlId, zoneBXmlId, 18.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneCXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneCXmlId, zoneDXmlId, 36.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneDXmlId, zoneAXmlId, 108.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneDXmlId, zoneBXmlId, 24.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneDXmlId, zoneCXmlId, 36.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneDXmlId, zoneDXmlId, 0.0);

        //tp=1
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneAXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneAXmlId, zoneBXmlId, 85.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneAXmlId, zoneCXmlId, 77.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneAXmlId, zoneDXmlId, 108.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneBXmlId, zoneAXmlId, 85.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneBXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneBXmlId, zoneCXmlId, 18.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneBXmlId, zoneDXmlId, 24.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneCXmlId, zoneAXmlId, 77.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneCXmlId, zoneBXmlId, 18.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneCXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneCXmlId, zoneDXmlId, 36.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneDXmlId, zoneAXmlId, 108.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneDXmlId, zoneBXmlId, 24.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneDXmlId, zoneCXmlId, 36.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneDXmlId, zoneDXmlId, 0.0);

        //tp=2
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneAXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneAXmlId, zoneBXmlId, 85.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneAXmlId, zoneCXmlId, 77.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneAXmlId, zoneDXmlId, 108.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneBXmlId, zoneAXmlId, 85.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneBXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneBXmlId, zoneCXmlId, 18.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneBXmlId, zoneDXmlId, 24.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneCXmlId, zoneAXmlId, 77.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneCXmlId, zoneBXmlId, 18.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneCXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneCXmlId, zoneDXmlId, 36.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneDXmlId, zoneAXmlId, 108.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneDXmlId, zoneBXmlId, 24.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneDXmlId, zoneCXmlId, 36.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneDXmlId, zoneDXmlId, 0.0);
        PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);
      }
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }  
  }

  /**
   * Test of results for TraditionalStaticAssignment for simple test case using
   * three time periods.
   * 
   * This test case uses the example from the course notes of ITLS6102 Strategic
   * Transport Planning, Lecture 1 (Overview), the example on Page 122 of the 2019
   * course notes.
   * 
   * Time_Period_1 uses route A to B in the example, which has a total route cost
   * of 85 (the fifth argument in the ResultDto constructor). Time Period 2 uses
   * route A to C in the example, which has a total route cost of 77. Time Period
   * 3 uses route A to D in the example, which has a total route cost of 108.
   */
  @Test
  public void test_basic_shortest_path_algorithm_three_time_periods() {
    try {
      String projectPath = Path.of(TEST_CASE_PATH.toString(),"basicShortestPathAlgorithm","xml","ThreeTimePeriods").toString();
      String description = "testBasic13";
      String csvFileName1 = "Time_Period_1_2.csv";
      String odCsvFileName1 = "Time_Period_1_1.csv";
      String csvFileName2 = "Time_Period_2_2.csv";
      String odCsvFileName2 = "Time_Period_2_1.csv";
      String csvFileName3 = "Time_Period_3_2.csv";
      String odCsvFileName3 = "Time_Period_3_1.csv";
      String xmlFileName1 = "Time_Period_1.xml";
      String xmlFileName2 = "Time_Period_2.xml";
      String xmlFileName3 = "Time_Period_3.xml";
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(
              projectPath, runIdDescription, csvFileName1, xmlFileName1, csvFileName2, xmlFileName2, csvFileName3, xmlFileName3);
      PlanItIOTestHelper.deleteOdFiles(
              projectPath, runIdDescription, odCsvFileName1, xmlFileName1, odCsvFileName2, xmlFileName2, odCsvFileName3, xmlFileName3);
      PlanItIOTestHelper.deletePathFiles(
              projectPath, runIdDescription, csvFileName1, xmlFileName1, csvFileName2, xmlFileName2, csvFileName3, xmlFileName3);

      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();       

      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName3, xmlFileName3);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName3, xmlFileName3);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath,runIdDescription, csvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName3, xmlFileName3);

      /* compare results */
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = testOutputDto.getB().demands.getFirst();

      TimePeriod timePeriod0 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
      TimePeriod timePeriod1 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("1"));
      TimePeriod timePeriod2 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("2"));

      // LINK RESULTS
      {
        //tp=0
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node3XmlId, node8XmlId, new LinkSegmentExpectedResultsDto(8, 3, 1, 8, 2000, 8, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node4XmlId, node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node5XmlId, node4XmlId, new LinkSegmentExpectedResultsDto(4, 5, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node6XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node7XmlId, node12XmlId, new LinkSegmentExpectedResultsDto(12, 7, 1, 5, 2000, 5, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node8XmlId, node7XmlId, new LinkSegmentExpectedResultsDto(7, 8, 1, 12, 2000, 12, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node10XmlId, node5XmlId, new LinkSegmentExpectedResultsDto(5, 10, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node11XmlId, node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1, 12, 2000, 12, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod0, mode1, node12XmlId, node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1, 8, 2000, 8, 1));

        //tp=1
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod1, mode1, node6XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod1, mode1, node11XmlId, node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1, 12, 2000, 12, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod1, mode1, node12XmlId, node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1, 8, 2000, 8, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod1, mode1, node13XmlId, node12XmlId, new LinkSegmentExpectedResultsDto(12, 13, 1, 47, 2000, 47, 1));

        //tp2
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node6XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node7XmlId, node12XmlId, new LinkSegmentExpectedResultsDto(12, 7, 1, 5, 2000, 5, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node8XmlId, node7XmlId, new LinkSegmentExpectedResultsDto(7, 8, 1, 12, 2000, 12, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node9XmlId, node8XmlId, new LinkSegmentExpectedResultsDto(8, 9, 1, 20, 2000, 20, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node11XmlId, node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1, 12, 2000, 12, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node12XmlId, node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1, 8, 2000, 8, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node14XmlId, node9XmlId, new LinkSegmentExpectedResultsDto(9, 14, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node15XmlId, node14XmlId, new LinkSegmentExpectedResultsDto(14, 15, 1, 10, 2000, 10, 1));
        PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod2, mode1, node20XmlId, node15XmlId, new LinkSegmentExpectedResultsDto(15, 20, 1, 21, 2000, 21, 1));
        PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter, null, linkResults);
      }

      // PATH RESULTS
      {
        //tp=0
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneAXmlId, zoneAXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneAXmlId, zoneBXmlId, "[1,6,11,12,7,8,3,4,5,10]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneAXmlId, zoneCXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneAXmlId, zoneDXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneBXmlId, zoneAXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneBXmlId, zoneBXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneBXmlId, zoneCXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneBXmlId, zoneDXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneCXmlId, zoneAXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneCXmlId, zoneBXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneCXmlId, zoneCXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneCXmlId, zoneDXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneDXmlId, zoneAXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneDXmlId, zoneBXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneDXmlId, zoneCXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod0, mode1, zoneDXmlId, zoneDXmlId, "");

        //tp=1
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneAXmlId, zoneAXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneAXmlId, zoneBXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneAXmlId, zoneCXmlId, "[1,6,11,12,13]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneAXmlId, zoneDXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneBXmlId, zoneAXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneBXmlId, zoneBXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneBXmlId, zoneCXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneBXmlId, zoneDXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneCXmlId, zoneAXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneCXmlId, zoneBXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneCXmlId, zoneCXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneCXmlId, zoneDXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneDXmlId, zoneAXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneDXmlId, zoneBXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneDXmlId, zoneCXmlId, "");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod1, mode1, zoneDXmlId, zoneDXmlId, "");

        //tp=2
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneAXmlId, zoneAXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneAXmlId, zoneBXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneAXmlId, zoneCXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneAXmlId, zoneDXmlId,"[1,6,11,12,7,8,9,14,15,20]");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneBXmlId, zoneAXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneBXmlId, zoneBXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneBXmlId, zoneCXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneBXmlId, zoneDXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneCXmlId, zoneAXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneCXmlId, zoneBXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneCXmlId, zoneCXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneCXmlId, zoneDXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneDXmlId, zoneAXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneDXmlId, zoneBXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneDXmlId, zoneCXmlId,"");
        PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod2, mode1, zoneDXmlId, zoneDXmlId,"");
        PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, null, pathMap);
      }

      // OD RESULTS
      {
        //tp=0
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneAXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneAXmlId, zoneBXmlId, 85.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneAXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneAXmlId, zoneDXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneBXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneBXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneBXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneBXmlId, zoneDXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneCXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneCXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneCXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneCXmlId, zoneDXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneDXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneDXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneDXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod0, mode1, zoneDXmlId, zoneDXmlId, 0.0);

        //tp=1
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneAXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneAXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneAXmlId, zoneCXmlId, 77.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneAXmlId, zoneDXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneBXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneBXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneBXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneBXmlId, zoneDXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneCXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneCXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneCXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneCXmlId, zoneDXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneDXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneDXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneDXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod1, mode1, zoneDXmlId, zoneDXmlId, 0.0);

        //tp=2
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneAXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneAXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneAXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneAXmlId, zoneDXmlId, 108.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneBXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneBXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneBXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneBXmlId, zoneDXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneCXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneCXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneCXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneCXmlId, zoneDXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneDXmlId, zoneAXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneDXmlId, zoneBXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneDXmlId, zoneCXmlId, 0.0);
        PlanItIOTestHelper.addToNestedMap(odMap, timePeriod2, mode1, zoneDXmlId, zoneDXmlId, 0.0);
        PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);
      }

    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
}