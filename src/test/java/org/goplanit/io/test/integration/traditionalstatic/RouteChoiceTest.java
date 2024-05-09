package org.goplanit.io.test.integration.traditionalstatic;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.cost.physical.BPRConfigurator;
import org.goplanit.cost.physical.PhysicalCostConfigurator;
import org.goplanit.demands.Demands;
import org.goplanit.io.test.integration.TestBase;
import org.goplanit.io.test.util.PlanItIOTestHelper;
import org.goplanit.io.test.util.PlanItIoTestRunner;
import org.goplanit.io.test.util.PlanItInputBuilder4Testing;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.output.configuration.LinkOutputTypeConfiguration;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.functionalinterface.TriConsumer;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.test.LinkSegmentExpectedResultsDto;
import org.goplanit.utils.test.TestOutputDto;
import org.goplanit.utils.time.TimePeriod;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JUnit test case for TraditionalStaticAssignment
 * 
 * Many of these unit tests use the ResultDto object to save standard results
 * against which the results of test runs are compared.
 * 
 * The arguments to the ResultDto constructor are:
 * 
 * startNodeId external id of start node (used to define the link segment)
 * endNodeId external id of end node (used to define the link segment) linkFlow
 * flow through link (output) linkCost cost (travel time) of link (output)
 * totalCostToEndNode cumulative travel time from start of output path to the
 * end of the current link (output) capacity capacity of the link (input) (no
 * lanes x capacity per lane)) length length of the link (input) speed travel
 * speed of the link (input)
 * 
 * 
 * @author gman6028
 *
 */
public class RouteChoiceTest extends TestBase {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path ROUTE_CHOICE_TEST_CASE_PATH = Path.of(TEST_CASE_PATH.toString(),"route_choice", "xml");

  private static final Path ROUTE_CHOICE_TEST_CASE_INPUT_PATH = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"_input");

  private static final Path ROUTE_CHOICE_TEST_CASE_INPUT_PATH_SIMO_MISO_ONE_TP =
          Path.of(ROUTE_CHOICE_TEST_CASE_INPUT_PATH.toString(),"SIMOMISO_one_time_period");

  private static final Path ROUTE_CHOICE_TEST_CASE_INPUT_PATH_SIMO_MISO_THREE_TP =
          Path.of(ROUTE_CHOICE_TEST_CASE_INPUT_PATH.toString(),"SIMOMISO_three_time_period");

  private static final Path ROUTE_CHOICE_TEST_CASE_INPUT_PATH_SIMO_MISO_TWO_MODES =
          Path.of(ROUTE_CHOICE_TEST_CASE_INPUT_PATH.toString(),"SIMOMISO_two_modes");

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(RouteChoiceTest.class);
    } 
  }
  
  @BeforeEach
  public void beforeTest() {
    pathMap = new TreeMap<>();
    odMap = new TreeMap<>();
    linkResults = new TreeMap<>();
    linkIdResultsMap = new TreeMap<>();
  }  

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }
  
  /**
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the first route choice example from the Traditional Static Assignment Route
   * Choice Equilibration Test cases.docx document.
   */
  @Test
  public void test_1_no_route_choice_single_mode() {
    try {
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"noRouteChoiceSingleMode").toString();
      String description = "testRouteChoice1";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();        

      /* compare results */        
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = testOutputDto.getB().demands.getFirst();
      TimePeriod timePeriod = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));

      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node1XmlId, node2XmlId, new LinkSegmentExpectedResultsDto(2, 1, 2000,0.015, 2000, 1, 66.6666667));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node1XmlId, node5XmlId, new LinkSegmentExpectedResultsDto(5, 1, 1000,0.0103125, 2000, 1, 96.969697));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node2XmlId, node3XmlId, new LinkSegmentExpectedResultsDto(3, 2, 2000,0.015, 2000, 1, 66.6666667));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node2XmlId, node4XmlId, new LinkSegmentExpectedResultsDto(4, 2, 1000,0.0103125, 2000, 1, 96.969697));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node3XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 3, 2000,0.09, 1000, 1, 11.1111111));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node3XmlId, node6XmlId, new LinkSegmentExpectedResultsDto(6, 3, 1000,0.0103125, 2000, 1, 96.969697));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node4XmlId, node13XmlId, new LinkSegmentExpectedResultsDto(13, 4, 1000,0.01, 20000, 1, 99.9996875));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node5XmlId, node15XmlId, new LinkSegmentExpectedResultsDto(15, 5, 1000,0.01, 20000, 1, 99.9996875));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node6XmlId, node12XmlId, new LinkSegmentExpectedResultsDto(12, 6, 1000,0.01, 20000, 1, 99.9996875));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node11XmlId, node2XmlId, new LinkSegmentExpectedResultsDto(2, 11, 1000,0.0103125, 2000, 1, 96.969697));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node14XmlId, node3XmlId, new LinkSegmentExpectedResultsDto(3, 14, 1000,0.0103125, 2000, 1, 96.969697));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node16XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 16, 1000,0.0103125, 2000, 1, 96.969697));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter,maxIterations, linkResults);

      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone1XmlId, zone1XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone1XmlId, zone2XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone1XmlId, zone3XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone1XmlId, zone4XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone1XmlId, zone5XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone1XmlId, zone6XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone2XmlId, zone1XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone2XmlId, zone2XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone2XmlId, zone3XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone2XmlId, zone4XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone2XmlId, zone5XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone2XmlId, zone6XmlId,"[12,6,3,2,1,16]");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone3XmlId, zone1XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone3XmlId, zone2XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone3XmlId, zone3XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone3XmlId, zone4XmlId,"[13,4,2,1,3,14]");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone3XmlId, zone5XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone3XmlId, zone6XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone4XmlId, zone1XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone4XmlId, zone2XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone4XmlId, zone3XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone4XmlId, zone4XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone4XmlId, zone5XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone4XmlId, zone6XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone5XmlId, zone1XmlId,"[15,5,1,3,2,11]");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone5XmlId, zone2XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone5XmlId, zone3XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone5XmlId, zone4XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone5XmlId, zone5XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone5XmlId, zone6XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone6XmlId, zone1XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone6XmlId, zone2XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone6XmlId, zone3XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone6XmlId, zone4XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone6XmlId, zone5XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone6XmlId, zone6XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);

      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone1XmlId, zone1XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone1XmlId, zone2XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone1XmlId, zone3XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone1XmlId, zone4XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone1XmlId, zone5XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone1XmlId, zone6XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone2XmlId, zone1XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone2XmlId, zone2XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone2XmlId, zone3XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone2XmlId, zone4XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone2XmlId, zone5XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone2XmlId, zone6XmlId, 0.060625);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone3XmlId, zone1XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone3XmlId, zone2XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone3XmlId, zone3XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone3XmlId, zone4XmlId, 0.135625);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone3XmlId, zone5XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone3XmlId, zone6XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone4XmlId, zone1XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone4XmlId, zone2XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone4XmlId, zone3XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone4XmlId, zone4XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone4XmlId, zone5XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone4XmlId, zone6XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone5XmlId, zone1XmlId, 0.135625);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone5XmlId, zone2XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone5XmlId, zone3XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone5XmlId, zone4XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone5XmlId, zone5XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone5XmlId, zone6XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone6XmlId, zone1XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone6XmlId, zone2XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone6XmlId, zone3XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone6XmlId, zone4XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone6XmlId, zone5XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone6XmlId, zone6XmlId, 0.0);
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
    } catch (final Exception e) {
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * /**
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the second route choice example from the Traditional Static Assignment Route
   * Choice Equilibration Test cases.docx document.
   */
  @Test
  public void test_2_SIMO_MISO_route_choice_single_mode() {
    try {
      final String inputPath = ROUTE_CHOICE_TEST_CASE_INPUT_PATH_SIMO_MISO_ONE_TP.toString();
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"SIMOMISOrouteChoiceSingleMode").toString();
      String description = "testRouteChoice2";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(inputPath, projectPath, description, TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();        

      /* compare results */        
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = (Demands)testOutputDto.getB().demands.getFirst();
      TimePeriod timePeriod = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));

      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node1XmlId, node11XmlId, new LinkSegmentExpectedResultsDto(11, 1, 3600,0.025, 3600, 1, 40));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node2XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 295.2,0.0333944, 1200, 2, 59.8903352));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node3XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 3, 1425.6,0.0332658, 1200, 1, 30.0609344));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node4XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 4, 1879.2,0.0667837, 1200, 1, 14.9737025));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node4XmlId, node2XmlId, new LinkSegmentExpectedResultsDto(2, 4, 295.2,0.0333944, 1200, 2, 59.8903352));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node4XmlId, node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 1425.6,0.0332658, 1200, 1, 30.0609344));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node12XmlId, node4XmlId, new LinkSegmentExpectedResultsDto(4, 12, 3600,0.025, 3600, 1, 40));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter, maxIterations, linkResults);
      
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone1XmlId,zone1XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone1XmlId,zone2XmlId,"[11,1,4,12]");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone2XmlId,zone1XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone2XmlId,zone2XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone1XmlId,zone1XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone1XmlId,zone2XmlId, 0.1164169);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone2XmlId,zone1XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone2XmlId,zone2XmlId, 0.0);
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
      
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
   * This test checks that PlanItProject reads the initial costs from a file
   * correctly, and outputs them after the first iteration.
   * 
   * The test input initial costs file uses Link Segment Id to identify link
   * segments
   */
  @Test
  public void test_2_SIMO_MISO_route_choice_single_mode_with_initial_costs_and_one_iteration() {
    try {

      final String inputPath = ROUTE_CHOICE_TEST_CASE_INPUT_PATH_SIMO_MISO_ONE_TP.toString();
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"SIMOMISOrouteChoiceSingleModeInitialCostsOneIteration").toString();

      String description = "testRouteChoice2initialCosts";
      String csvFileName = "Time_Period_1_1.csv";
      String odCsvFileName = "Time_Period_1_0.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 1;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(
              inputPath, projectPath, description, TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      runner.registerInitialLinkSegmentCost(Path.of(inputPath,"initial_link_segment_costs.csv").toString());
      runner.setupAndExecuteDefaultAssignment();        

      /* compare results */        
            
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
   * This test runs the same network using one iteration with different initial
   * costs for each time, checking that the results are different for each time
   * period.
   */
  @Test
  public void test_2_SIMO_MISO_route_choice_single_mode_with_initial_costs_and_one_iteration_and_three_time_periods() {
    try {
      final String inputPath = ROUTE_CHOICE_TEST_CASE_INPUT_PATH_SIMO_MISO_THREE_TP.toString();
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"SIMOMISOrouteChoiceInitialCostsOneIterationThreeTimePeriods").toString();
      String description = "test2initialCostsOneIterationThreeTimePeriods";
      String csvFileName1 = "Time_Period_1_1.csv";
      String odCsvFileName1 = "Time_Period_1_0.csv";
      String csvFileName2 = "Time_Period_2_1.csv";
      String odCsvFileName2 = "Time_Period_2_0.csv";
      String csvFileName3 = "Time_Period_3_1.csv";
      String odCsvFileName3 = "Time_Period_3_0.csv";
      String xmlFileName1 = "Time_Period_1.xml";
      String xmlFileName2 = "Time_Period_2.xml";
      String xmlFileName3 = "Time_Period_3.xml";
      
      Integer maxIterations = 1;
      
      //remove old output (if any) before run
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(
              projectPath, runIdDescription, csvFileName1, csvFileName2, csvFileName3, xmlFileName1, xmlFileName2, xmlFileName3);
      
      PlanItIOTestHelper.deleteOdFiles(
              projectPath, runIdDescription, odCsvFileName1, odCsvFileName2, odCsvFileName3, xmlFileName1, xmlFileName2, xmlFileName3);
      
      PlanItIOTestHelper.deletePathFiles(
              projectPath, runIdDescription, csvFileName1, csvFileName2, csvFileName3, xmlFileName1, xmlFileName2, xmlFileName3);

      /* run test */
      PlanItIoTestRunner runner =
              new PlanItIoTestRunner(inputPath, projectPath, description, TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      runner.registerInitialLinkSegmentCostByTimePeriod("0", Path.of(inputPath,"initial_link_segment_costs_time_period_1.csv").toString());
      runner.registerInitialLinkSegmentCostByTimePeriod("1", Path.of(inputPath,"initial_link_segment_costs_time_period_2.csv").toString());
      runner.registerInitialLinkSegmentCostByTimePeriod("2", Path.of(inputPath,"initial_link_segment_costs_time_period_3.csv").toString());
      runner.setupAndExecuteDefaultAssignment();        

      /* compare results */
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName3, xmlFileName3);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName3, xmlFileName3);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName3, xmlFileName3);
    } catch (final Exception e) {
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * This test checks that PlanItProject reads the initial costs from a file
   * correctly, and outputs them after the first iteration.
   * 
   * The test input initial costs file uses Link Segment External Id to identify
   * link segments
   *
   */
  @Test
  public void test_2_SIMO_MISO_route_choice_single_mode_with_initial_costs_and_one_iteration_using_link_segment_external_ids() {
    try {
      final String inputPath = ROUTE_CHOICE_TEST_CASE_INPUT_PATH_SIMO_MISO_ONE_TP.toString();
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"SIMOMISOrouteChoiceInitialCostsOneIterationExternalIds").toString();
      String description = "testRouteChoice2initialCosts";
      String csvFileName = "Time_Period_1_1.csv";
      String odCsvFileName = "Time_Period_1_0.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 1;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(inputPath, projectPath, description, TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      runner.registerInitialLinkSegmentCost( Path.of(inputPath,"initial_link_segment_costs.csv").toString());
      runner.setupAndExecuteDefaultAssignment();        

      /* compare results */        
      
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
   * This test checks that PlanItProject reads the initial costs from a file
   * correctly, and outputs them after 500 iterations.
   * 
   * The test input initial costs file uses Link Segment Id to identify link
   * segments
   */
  @Test
  public void test_2_SIMO_MISO_route_choice_single_mode_with_initial_costs_and_500_iterations() {
    try {
      final String inputPath = ROUTE_CHOICE_TEST_CASE_INPUT_PATH_SIMO_MISO_ONE_TP.toString();
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"SIMOMISOrouteChoiceSingleModeWithInitialCosts500Iterations").toString();
      String description = "testRouteChoice2initialCosts";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(inputPath, projectPath, description, TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      runner.registerInitialLinkSegmentCost(Path.of(inputPath,"initial_link_segment_costs.csv").toString());
      runner.setupAndExecuteDefaultAssignment();        

      /* compare results */   
      
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
      
    } catch (final Exception e) {
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * This test runs the same network with three time periods with different initial
   * costs for each, running the test for 500 iterations.
   */
  @Test
  public void test_2_SIMO_MISO_route_choice_single_mode_with_initial_costs_and_500_iterations_and_three_time_periods() {
    try {
      final String inputPath = ROUTE_CHOICE_TEST_CASE_INPUT_PATH_SIMO_MISO_THREE_TP.toString();
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"SIMOMISOrouteChoiceSingleModeWithInitialCosts500IterationsThreeTimePeriods").toString();
      String description = "test2initialCosts500IterationsThreeTimePeriods";
      String csvFileName1 = "Time_Period_1_500.csv";
      String odCsvFileName1 = "Time_Period_1_499.csv";
      String csvFileName2 = "Time_Period_2_500.csv";
      String odCsvFileName2 = "Time_Period_2_499.csv";
      String csvFileName3 = "Time_Period_3_500.csv";
      String odCsvFileName3 = "Time_Period_3_499.csv";
      String xmlFileName1 = "Time_Period_1.xml";
      String xmlFileName2 = "Time_Period_2.xml";
      String xmlFileName3 = "Time_Period_3.xml";
      Integer maxIterations = 500;

      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(
              projectPath, runIdDescription, csvFileName1, xmlFileName1, csvFileName2, xmlFileName2, csvFileName3, xmlFileName3);
      PlanItIOTestHelper.deleteOdFiles(
              projectPath, runIdDescription, odCsvFileName1, xmlFileName1, odCsvFileName2, xmlFileName2, odCsvFileName3, xmlFileName3);
      PlanItIOTestHelper.deletePathFiles(
              projectPath, runIdDescription, csvFileName1, xmlFileName1, csvFileName2, xmlFileName2, csvFileName3, xmlFileName3);
      
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(
              inputPath, projectPath, description, TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      runner.registerInitialLinkSegmentCostByTimePeriod("0", Path.of(inputPath,"initial_link_segment_costs_time_period_1.csv").toString());
      runner.registerInitialLinkSegmentCostByTimePeriod("1",Path.of(inputPath,"initial_link_segment_costs_time_period_2.csv").toString());
      runner.registerInitialLinkSegmentCostByTimePeriod("2",Path.of(inputPath,"initial_link_segment_costs_time_period_3.csv").toString());
      runner.setupAndExecuteDefaultAssignment();        

      /* compare results */        
            
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName3, xmlFileName3);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName3, xmlFileName3);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName3, xmlFileName3);
      
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * This test checks that PlanItProject reads the initial costs from a file
   * correctly, and outputs them after 500 iterations.
   * 
   * The test input initial costs file uses Link Segment External Id to identify
   * link segments.
   */
  @Test
  public void test_2_SIMO_MISO_route_choice_single_mode_with_initial_costs_and_500_iterations_using_link_segment_external_ids() {
    try {
      final String inputPath = ROUTE_CHOICE_TEST_CASE_INPUT_PATH_SIMO_MISO_ONE_TP.toString();
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"SIMOMISOrouteChoiceSingleModeWithInitialCosts500IterationsLinkSegmentExternalIds").toString();
      String description = "testRouteChoice2initialCosts";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(inputPath, projectPath, description, TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      runner.registerInitialLinkSegmentCost(Path.of(inputPath,"initial_link_segment_costs.csv").toString());
      runner.setupAndExecuteDefaultAssignment();        

      /* compare results */      
      
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
    } catch (final Exception e) {
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the third route choice example from the Traditional Static Assignment Route
   * Choice Equilibration Test cases.docx document.
   */
  @Test
  public void test_3_MIMO_route_choice_single_mode() {
    try {
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"MIMOrouteChoiceSingleMode").toString();
      String description = "testRouteChoice3";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto =
              runner.setupAndExecuteDefaultAssignment();

      /* compare results */
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = testOutputDto.getB().demands.getFirst();
      TimePeriod timePeriod = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
      
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node1XmlId, node11XmlId, new LinkSegmentExpectedResultsDto(11, 1, 8000,0.03, 8000, 2, 66.6666667));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node2XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 3952,0.0239029, 5000, 2, 83.6718462));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node3XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 3, 4048,0.0531495, 3000, 2, 37.6297041));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node3XmlId, node2XmlId, new LinkSegmentExpectedResultsDto(2, 3, 3952,0.0295286, 4000, 2, 67.7310119));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node4XmlId, node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 3856,0.0472937, 3000, 2, 42.2888931));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node5XmlId, node3XmlId, new LinkSegmentExpectedResultsDto(3, 5, 4144,0.2043143, 2000, 2, 9.7888406));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node5XmlId, node4XmlId, new LinkSegmentExpectedResultsDto(4, 5, 3856,0.1581746, 2000, 2, 12.6442576));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node12XmlId, node5XmlId, new LinkSegmentExpectedResultsDto(5, 12, 8000,2.58, 2000, 2, 0.7751938));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter, maxIterations, linkResults);
      
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone1XmlId, zone1XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone1XmlId, zone2XmlId,"[11,1,2,3,4,5,12]");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone2XmlId, zone1XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone2XmlId, zone2XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);          
      
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone1XmlId, zone1XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone1XmlId, zone2XmlId, 2.8673689);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone2XmlId, zone1XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone2XmlId, zone2XmlId, 0.0);
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

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
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the fourth route choice example from the Traditional Static Assignment Route
   * Choice Equilibration Test cases.docx document.
   * 
   * This test case uses the <odrowmatrix> method in the macroscopicinput.xml file
   * to define the OD demands input matrix.
   */
  @Test
  public void test_4_bi_directional_links_route_choice_single_mode() {
    try {
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"biDirectionalLinksRouteChoiceSingleMode").toString();
      String description = "testRouteChoice4";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();        

      /* compare results */      
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = testOutputDto.getB().demands.getFirst();
      TimePeriod timePeriod = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
      
      linkResults.put(timePeriod, new TreeMap<>());
      linkResults.get(timePeriod).put(mode1, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node1XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node2XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node3XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node4XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node5XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node6XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node7XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node8XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node9XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node10XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node11XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node12XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node13XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node14XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node15XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node16XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node21XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node22XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node23XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node24XmlId, new TreeMap<>());

      linkResults.get(timePeriod).get(mode1).get(node9XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 9, 0.6,
          0.029, 1500, 2.9, 100));
      linkResults.get(timePeriod).get(mode1).get(node12XmlId).put(node9XmlId, new LinkSegmentExpectedResultsDto(9, 12, 0.6,
          0.029,1500, 2.9, 100));
      linkResults.get(timePeriod).get(mode1).get(node11XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 11,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      linkResults.get(timePeriod).get(mode1).get(node12XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 12,
          482.4, 0.0301605,  1500, 3, 99.4679928));
      linkResults.get(timePeriod).get(mode1).get(node16XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 16, 483,
          0.0100538, 1500, 1, 99.4653552));
      linkResults.get(timePeriod).get(mode1).get(node12XmlId).put(node16XmlId, new LinkSegmentExpectedResultsDto(16, 12, 483,
          0.0100538, 1500, 1, 99.4653552));
      linkResults.get(timePeriod).get(mode1).get(node13XmlId).put(node9XmlId, new LinkSegmentExpectedResultsDto(9, 13, 0.6,
          0.01, 1500, 1, 100));
      linkResults.get(timePeriod).get(mode1).get(node9XmlId).put(node13XmlId, new LinkSegmentExpectedResultsDto(13, 9, 0.6,
          0.01, 1500, 1, 100));
      linkResults.get(timePeriod).get(mode1).get(node11XmlId).put(node10XmlId, new LinkSegmentExpectedResultsDto(10, 11,
          17.6, 0.03, 1500, 3, 100));
      linkResults.get(timePeriod).get(mode1).get(node10XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 10,
          17.6, 0.03, 1500, 3, 100));
      linkResults.get(timePeriod).get(mode1).get(node14XmlId).put(node10XmlId, new LinkSegmentExpectedResultsDto(10, 14,
          17.6, 0.01, 1500, 1, 100));
      linkResults.get(timePeriod).get(mode1).get(node10XmlId).put(node14XmlId, new LinkSegmentExpectedResultsDto(14, 10,
          17.6, 0.01, 1500, 1, 100));
      linkResults.get(timePeriod).get(mode1).get(node15XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 15, 500,
          0.0100617, 1500, 1, 99.3865031));
      linkResults.get(timePeriod).get(mode1).get(node11XmlId).put(node15XmlId, new LinkSegmentExpectedResultsDto(15, 11, 500,
          0.0100617, 1500, 1, 99.3865031));
      linkResults.get(timePeriod).get(mode1).get(node5XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 5, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      linkResults.get(timePeriod).get(mode1).get(node1XmlId).put(node5XmlId, new LinkSegmentExpectedResultsDto(5, 1, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      linkResults.get(timePeriod).get(mode1).get(node4XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 4, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      linkResults.get(timePeriod).get(mode1).get(node1XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 1, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      linkResults.get(timePeriod).get(mode1).get(node2XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      linkResults.get(timePeriod).get(mode1).get(node1XmlId).put(node2XmlId, new LinkSegmentExpectedResultsDto(2, 1, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      linkResults.get(timePeriod).get(mode1).get(node6XmlId).put(node2XmlId, new LinkSegmentExpectedResultsDto(2, 6, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      linkResults.get(timePeriod).get(mode1).get(node2XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 2, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      linkResults.get(timePeriod).get(mode1).get(node3XmlId).put(node2XmlId, new LinkSegmentExpectedResultsDto(2, 3, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      linkResults.get(timePeriod).get(mode1).get(node2XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 2, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      linkResults.get(timePeriod).get(mode1).get(node7XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 7, 1900,
          0.0228712, 1500, 1, 43.7230914));
      linkResults.get(timePeriod).get(mode1).get(node3XmlId).put(node7XmlId, new LinkSegmentExpectedResultsDto(7, 3, 1900,
          0.0228712, 1500, 1, 43.7230914));
      linkResults.get(timePeriod).get(mode1).get(node4XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      linkResults.get(timePeriod).get(mode1).get(node3XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 3, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      linkResults.get(timePeriod).get(mode1).get(node8XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 8, 1617,
          0.0167522, 1500, 1, 59.693666));
      linkResults.get(timePeriod).get(mode1).get(node4XmlId).put(node8XmlId, new LinkSegmentExpectedResultsDto(8, 4, 1617,
          0.0167522, 1500, 1, 59.693666));
      linkResults.get(timePeriod).get(mode1).get(node23XmlId).put(node16XmlId, new LinkSegmentExpectedResultsDto(16, 23, 483,
          0.0200001, 10000, 1, 49.9998639));
      linkResults.get(timePeriod).get(mode1).get(node16XmlId).put(node23XmlId, new LinkSegmentExpectedResultsDto(23, 16, 483,
          0.0200001, 10000, 1, 49.9998639));
      linkResults.get(timePeriod).get(mode1).get(node8XmlId).put(node23XmlId, new LinkSegmentExpectedResultsDto(23, 8, 1617,
          0.0200068, 10000, 1, 49.9829143));
      linkResults.get(timePeriod).get(mode1).get(node23XmlId).put(node8XmlId, new LinkSegmentExpectedResultsDto(8, 23, 1617,
          0.0200068, 10000, 1, 49.9829143));
      linkResults.get(timePeriod).get(mode1).get(node21XmlId).put(node13XmlId, new LinkSegmentExpectedResultsDto(13, 21, 0.6,
          0.02, 10000, 1, 50));
      linkResults.get(timePeriod).get(mode1).get(node13XmlId).put(node21XmlId, new LinkSegmentExpectedResultsDto(21, 13, 0.6,
          0.02, 10000, 1, 50));
      linkResults.get(timePeriod).get(mode1).get(node5XmlId).put(node21XmlId, new LinkSegmentExpectedResultsDto(21, 5, 899.4,
          0.0200007, 10000, 1, 49.9983642));
      linkResults.get(timePeriod).get(mode1).get(node21XmlId).put(node5XmlId, new LinkSegmentExpectedResultsDto(5, 21, 899.4,
          0.0200007, 10000, 1, 49.9983642));
      linkResults.get(timePeriod).get(mode1).get(node22XmlId).put(node14XmlId, new LinkSegmentExpectedResultsDto(14, 22,
          17.6, 0.02, 10000, 1, 50));
      linkResults.get(timePeriod).get(mode1).get(node14XmlId).put(node22XmlId, new LinkSegmentExpectedResultsDto(22, 14,
          17.6, 0.02, 10000, 1, 50));
      linkResults.get(timePeriod).get(mode1).get(node6XmlId).put(node22XmlId, new LinkSegmentExpectedResultsDto(22, 6,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      linkResults.get(timePeriod).get(mode1).get(node22XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 22,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      linkResults.get(timePeriod).get(mode1).get(node24XmlId).put(node15XmlId, new LinkSegmentExpectedResultsDto(15, 24, 500,
          0.0200001, 10000, 1, 49.9998438));
      linkResults.get(timePeriod).get(mode1).get(node15XmlId).put(node24XmlId, new LinkSegmentExpectedResultsDto(24, 15, 500,
          0.0200001, 10000, 1, 49.9998438));
      linkResults.get(timePeriod).get(mode1).get(node7XmlId).put(node24XmlId, new LinkSegmentExpectedResultsDto(24, 7, 1900,
          0.020013, 10000, 1, 49.967441));
      linkResults.get(timePeriod).get(mode1).get(node24XmlId).put(node7XmlId, new LinkSegmentExpectedResultsDto(7, 24, 1900,
          0.020013, 10000, 1, 49.967441));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter,
          maxIterations, linkResults);

      pathMap.put(timePeriod, new TreeMap<>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId,"[21,5,1,2,6,22]");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone3XmlId,"[21,5,1,4,8,23]");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone4XmlId,"[21,5,1,4,3,7,24]");
      pathMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId,"[22,6,2,1,5,21]");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone3XmlId,"[22,6,2,1,4,8,23]");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone4XmlId,"[22,6,2,3,7,24]");
      pathMap.get(timePeriod).get(mode1).put(zone3XmlId, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).get(zone3XmlId).put(zone1XmlId,"[23,8,4,1,5,21]");
      pathMap.get(timePeriod).get(mode1).get(zone3XmlId).put(zone2XmlId,"[23,8,4,1,2,6,22]");
      pathMap.get(timePeriod).get(mode1).get(zone3XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone3XmlId).put(zone4XmlId,"[23,8,4,3,7,24]");
      pathMap.get(timePeriod).get(mode1).put(zone4XmlId, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).get(zone4XmlId).put(zone1XmlId,"[24,7,3,4,1,5,21]");
      pathMap.get(timePeriod).get(mode1).get(zone4XmlId).put(zone2XmlId,"[24,7,3,2,6,22]");
      pathMap.get(timePeriod).get(mode1).get(zone4XmlId).put(zone3XmlId,"[24,7,3,4,8,23]");
      pathMap.get(timePeriod).get(mode1).get(zone4XmlId).put(zone4XmlId,""); 
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
       

      odMap.put(timePeriod, new TreeMap<>());
      odMap.get(timePeriod).put(mode1, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId, 0.0);
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId, 0.0767791);
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone3XmlId, 0.0776307);
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone4XmlId, 0.0944051);
      odMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId, 0.0767791);
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId, 0.0);
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone3XmlId, 0.0931159);
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone4XmlId, 0.0900226);
      odMap.get(timePeriod).get(mode1).put(zone3XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).get(zone3XmlId).put(zone1XmlId, 0.0776307);
      odMap.get(timePeriod).get(mode1).get(zone3XmlId).put(zone2XmlId, 0.0931159);
      odMap.get(timePeriod).get(mode1).get(zone3XmlId).put(zone3XmlId, 0.0);
      odMap.get(timePeriod).get(mode1).get(zone3XmlId).put(zone4XmlId, 0.0902602);
      odMap.get(timePeriod).get(mode1).put(zone4XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).get(zone4XmlId).put(zone1XmlId, 0.0944051);
      odMap.get(timePeriod).get(mode1).get(zone4XmlId).put(zone2XmlId, 0.0900226);
      odMap.get(timePeriod).get(mode1).get(zone4XmlId).put(zone3XmlId, 0.0902602);
      odMap.get(timePeriod).get(mode1).get(zone4XmlId).put(zone4XmlId, 0.0);
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

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
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the fourth route choice example from the Traditional Static Assignment Route
   * Choice Equilibration Test cases.docx document.
   * 
   * This test case uses two time periods.
   * 
   * This test case uses the <odrowmatrix> method in the macroscopicinput.xml file
   * to define the OD demands input matrix.
   */
  @Test
  public void test_4_bi_directional_links_route_choice_single_mode_with_two_time_periods() {
    try {
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"biDirectionalLinksRouteChoiceSingleModeWithTwoTimePeriods").toString();
      String description = "testRouteChoice42";
      String csvFileName1 = "Time_Period_1_500.csv";
      String odCsvFileName1 = "Time_Period_1_499.csv";
      String csvFileName2 = "Time_Period_2_500.csv";
      String odCsvFileName2 = "Time_Period_2_499.csv";
      String xmlFileName1 = "Time_Period_1.xml";
      String xmlFileName2 = "Time_Period_2.xml";
      Integer maxIterations = 500;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName1);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName1);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName1);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName1);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName1);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName1);
      
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName2);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName2);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName2);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName2);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName2);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName2);
      
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();        

      /* compare results */
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = testOutputDto.getB().demands.getFirst();
      TimePeriod timePeriod0 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
      
      linkResults.put(timePeriod0, new TreeMap<>());
      linkResults.get(timePeriod0).put(mode1, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node1XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node2XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node3XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node4XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node5XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node6XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node7XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node8XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node9XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node10XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node11XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node12XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node13XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node14XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node15XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node16XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node21XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node22XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node23XmlId, new TreeMap<>());
      linkResults.get(timePeriod0).get(mode1).put(node24XmlId, new TreeMap<>());

      linkResults.get(timePeriod0).get(mode1).get(node9XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 9, 0.6,
          0.029, 1500, 2.9, 100));
      linkResults.get(timePeriod0).get(mode1).get(node12XmlId).put(node9XmlId, new LinkSegmentExpectedResultsDto(9, 12, 0.6,
          0.029, 1500, 2.9, 100));
      linkResults.get(timePeriod0).get(mode1).get(node11XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 11,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      linkResults.get(timePeriod0).get(mode1).get(node12XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 12,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      linkResults.get(timePeriod0).get(mode1).get(node16XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 16,
          483, 0.0100538, 1500, 1, 99.4653552));
      linkResults.get(timePeriod0).get(mode1).get(node12XmlId).put(node16XmlId, new LinkSegmentExpectedResultsDto(16, 12,
          483, 0.0100538, 1500, 1, 99.4653552));
      linkResults.get(timePeriod0).get(mode1).get(node13XmlId).put(node9XmlId, new LinkSegmentExpectedResultsDto(9, 13, 0.6,
          0.01, 1500, 1, 100));
      linkResults.get(timePeriod0).get(mode1).get(node9XmlId).put(node13XmlId, new LinkSegmentExpectedResultsDto(13, 9, 0.6,
          0.01, 1500, 1, 100));
      linkResults.get(timePeriod0).get(mode1).get(node11XmlId).put(node10XmlId, new LinkSegmentExpectedResultsDto(10, 11,
          17.6, 0.03, 1500, 3, 100));
      linkResults.get(timePeriod0).get(mode1).get(node10XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 10,
          17.6, 0.03, 1500, 3, 100));
      linkResults.get(timePeriod0).get(mode1).get(node14XmlId).put(node10XmlId, new LinkSegmentExpectedResultsDto(10, 14,
          17.6, 0.01, 1500, 1, 100));
      linkResults.get(timePeriod0).get(mode1).get(node10XmlId).put(node14XmlId, new LinkSegmentExpectedResultsDto(14, 10,
          17.6, 0.01, 1500, 1, 100));
      linkResults.get(timePeriod0).get(mode1).get(node15XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 15,
          500, 0.0100617, 1500, 1, 99.3865031));
      linkResults.get(timePeriod0).get(mode1).get(node11XmlId).put(node15XmlId, new LinkSegmentExpectedResultsDto(15, 11,
          500, 0.0100617, 1500, 1, 99.3865031));
      linkResults.get(timePeriod0).get(mode1).get(node5XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 5, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      linkResults.get(timePeriod0).get(mode1).get(node1XmlId).put(node5XmlId, new LinkSegmentExpectedResultsDto(5, 1, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      linkResults.get(timePeriod0).get(mode1).get(node4XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 4, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      linkResults.get(timePeriod0).get(mode1).get(node1XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 1, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      linkResults.get(timePeriod0).get(mode1).get(node2XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      linkResults.get(timePeriod0).get(mode1).get(node1XmlId).put(node2XmlId, new LinkSegmentExpectedResultsDto(2, 1, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      linkResults.get(timePeriod0).get(mode1).get(node6XmlId).put(node2XmlId, new LinkSegmentExpectedResultsDto(2, 6, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      linkResults.get(timePeriod0).get(mode1).get(node2XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 2, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      linkResults.get(timePeriod0).get(mode1).get(node3XmlId).put(node2XmlId, new LinkSegmentExpectedResultsDto(2, 3, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      linkResults.get(timePeriod0).get(mode1).get(node2XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 2, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      linkResults.get(timePeriod0).get(mode1).get(node7XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 7, 1900,
          0.0228712, 1500, 1, 43.7230914));
      linkResults.get(timePeriod0).get(mode1).get(node3XmlId).put(node7XmlId, new LinkSegmentExpectedResultsDto(7, 3, 1900,
          0.0228712, 1500, 1, 43.7230914));
      linkResults.get(timePeriod0).get(mode1).get(node4XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      linkResults.get(timePeriod0).get(mode1).get(node3XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 3, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      linkResults.get(timePeriod0).get(mode1).get(node8XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 8, 1617,
          0.0167522, 1500, 1, 59.693666));
      linkResults.get(timePeriod0).get(mode1).get(node4XmlId).put(node8XmlId, new LinkSegmentExpectedResultsDto(8, 4, 1617,
          0.0167522, 1500, 1, 59.693666));
      linkResults.get(timePeriod0).get(mode1).get(node23XmlId).put(node16XmlId, new LinkSegmentExpectedResultsDto(16, 23,
          483, 0.0200001, 10000, 1, 49.9998639));
      linkResults.get(timePeriod0).get(mode1).get(node16XmlId).put(node23XmlId, new LinkSegmentExpectedResultsDto(23, 16,
          483, 0.0200001, 10000, 1, 49.9998639));
      linkResults.get(timePeriod0).get(mode1).get(node8XmlId).put(node23XmlId, new LinkSegmentExpectedResultsDto(23, 8, 1617,
          0.0200068, 10000, 1, 49.9829143));
      linkResults.get(timePeriod0).get(mode1).get(node23XmlId).put(node8XmlId, new LinkSegmentExpectedResultsDto(8, 23, 1617,
          0.0200068, 10000, 1, 49.9829143));
      linkResults.get(timePeriod0).get(mode1).get(node21XmlId).put(node13XmlId, new LinkSegmentExpectedResultsDto(13, 21,
          0.6, 0.02, 10000, 1, 50));
      linkResults.get(timePeriod0).get(mode1).get(node13XmlId).put(node21XmlId, new LinkSegmentExpectedResultsDto(21, 13,
          0.6, 0.02, 10000, 1, 50));
      linkResults.get(timePeriod0).get(mode1).get(node5XmlId).put(node21XmlId, new LinkSegmentExpectedResultsDto(21, 5,
          899.4, 0.0200007, 10000, 1, 49.9983642));
      linkResults.get(timePeriod0).get(mode1).get(node21XmlId).put(node5XmlId, new LinkSegmentExpectedResultsDto(5, 21,
          899.4, 0.0200007, 10000, 1, 49.9983642));
      linkResults.get(timePeriod0).get(mode1).get(node22XmlId).put(node14XmlId, new LinkSegmentExpectedResultsDto(14, 22,
          17.6, 0.02, 10000, 1, 50));
      linkResults.get(timePeriod0).get(mode1).get(node14XmlId).put(node22XmlId, new LinkSegmentExpectedResultsDto(22, 14,
          17.6, 0.02, 10000, 1, 50));
      linkResults.get(timePeriod0).get(mode1).get(node6XmlId).put(node22XmlId, new LinkSegmentExpectedResultsDto(22, 6,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      linkResults.get(timePeriod0).get(mode1).get(node22XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 22,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      linkResults.get(timePeriod0).get(mode1).get(node24XmlId).put(node15XmlId, new LinkSegmentExpectedResultsDto(15, 24,
          500, 0.0200001, 10000, 1, 49.9998438));
      linkResults.get(timePeriod0).get(mode1).get(node15XmlId).put(node24XmlId, new LinkSegmentExpectedResultsDto(24, 15,
          500, 0.0200001, 10000, 1, 49.9998438));
      linkResults.get(timePeriod0).get(mode1).get(node7XmlId).put(node24XmlId, new LinkSegmentExpectedResultsDto(24, 7, 1900,
          0.020013, 10000, 1, 49.967441));
      linkResults.get(timePeriod0).get(mode1).get(node24XmlId).put(node7XmlId, new LinkSegmentExpectedResultsDto(7, 24, 1900,
          0.020013, 10000, 1, 49.967441));

      TimePeriod timePeriod1 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("1"));
      
      linkResults.put(timePeriod1, new TreeMap<>());
      linkResults.get(timePeriod1).put(mode1, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node1XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node2XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node3XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node4XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node5XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node6XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node7XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node8XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node9XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node10XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node11XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node12XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node13XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node14XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node15XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node16XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node21XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node22XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node23XmlId, new TreeMap<>());
      linkResults.get(timePeriod1).get(mode1).put(node24XmlId, new TreeMap<>());

      linkResults.get(timePeriod1).get(mode1).get(node9XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 9, 0.6,
          0.029, 1500, 2.9, 100));
      linkResults.get(timePeriod1).get(mode1).get(node12XmlId).put(node9XmlId, new LinkSegmentExpectedResultsDto(9, 12, 0.6,
          0.029, 1500, 2.9, 100));
      linkResults.get(timePeriod1).get(mode1).get(node11XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 11,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      linkResults.get(timePeriod1).get(mode1).get(node12XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 12,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      linkResults.get(timePeriod1).get(mode1).get(node16XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 16,
          483, 0.0100538, 1500, 1, 99.4653552));
      linkResults.get(timePeriod1).get(mode1).get(node12XmlId).put(node16XmlId, new LinkSegmentExpectedResultsDto(16, 12,
          483, 0.0100538, 1500, 1, 99.4653552));
      linkResults.get(timePeriod1).get(mode1).get(node13XmlId).put(node9XmlId, new LinkSegmentExpectedResultsDto(9, 13, 0.6,
          0.01, 1500, 1, 100));
      linkResults.get(timePeriod1).get(mode1).get(node9XmlId).put(node13XmlId, new LinkSegmentExpectedResultsDto(13, 9, 0.6,
          0.01, 1500, 1, 100));
      linkResults.get(timePeriod1).get(mode1).get(node11XmlId).put(node10XmlId, new LinkSegmentExpectedResultsDto(10, 11,
          17.6, 0.03, 1500, 3, 100));
      linkResults.get(timePeriod1).get(mode1).get(node10XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 10,
          17.6, 0.03, 1500, 3, 100));
      linkResults.get(timePeriod1).get(mode1).get(node14XmlId).put(node10XmlId, new LinkSegmentExpectedResultsDto(10, 14,
          17.6, 0.01, 1500, 1, 100));
      linkResults.get(timePeriod1).get(mode1).get(node10XmlId).put(node14XmlId, new LinkSegmentExpectedResultsDto(14, 10,
          17.6, 0.01, 1500, 1, 100));
      linkResults.get(timePeriod1).get(mode1).get(node15XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 15,
          500, 0.0100617, 1500, 1, 99.3865031));
      linkResults.get(timePeriod1).get(mode1).get(node11XmlId).put(node15XmlId, new LinkSegmentExpectedResultsDto(15, 11,
          500, 0.0100617, 1500, 1, 99.3865031));
      linkResults.get(timePeriod1).get(mode1).get(node5XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 5, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      linkResults.get(timePeriod1).get(mode1).get(node1XmlId).put(node5XmlId, new LinkSegmentExpectedResultsDto(5, 1, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      linkResults.get(timePeriod1).get(mode1).get(node4XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 4, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      linkResults.get(timePeriod1).get(mode1).get(node1XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 1, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      linkResults.get(timePeriod1).get(mode1).get(node2XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      linkResults.get(timePeriod1).get(mode1).get(node1XmlId).put(node2XmlId, new LinkSegmentExpectedResultsDto(2, 1, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      linkResults.get(timePeriod1).get(mode1).get(node6XmlId).put(node2XmlId, new LinkSegmentExpectedResultsDto(2, 6, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      linkResults.get(timePeriod1).get(mode1).get(node2XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 2, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      linkResults.get(timePeriod1).get(mode1).get(node3XmlId).put(node2XmlId, new LinkSegmentExpectedResultsDto(2, 3, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      linkResults.get(timePeriod1).get(mode1).get(node2XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 2, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      linkResults.get(timePeriod1).get(mode1).get(node7XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 7, 1900,
          0.0228712, 1500, 1, 43.7230914));
      linkResults.get(timePeriod1).get(mode1).get(node3XmlId).put(node7XmlId, new LinkSegmentExpectedResultsDto(7, 3, 1900,
          0.0228712, 1500, 1, 43.7230914));
      linkResults.get(timePeriod1).get(mode1).get(node4XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      linkResults.get(timePeriod1).get(mode1).get(node3XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 3, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      linkResults.get(timePeriod1).get(mode1).get(node8XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 8, 1617,
          0.0167522, 1500, 1, 59.693666));
      linkResults.get(timePeriod1).get(mode1).get(node4XmlId).put(node8XmlId, new LinkSegmentExpectedResultsDto(8, 4, 1617,
          0.0167522, 1500, 1, 59.693666));
      linkResults.get(timePeriod1).get(mode1).get(node23XmlId).put(node16XmlId, new LinkSegmentExpectedResultsDto(16, 23,
          483, 0.0200001, 10000, 1, 49.9998639));
      linkResults.get(timePeriod1).get(mode1).get(node16XmlId).put(node23XmlId, new LinkSegmentExpectedResultsDto(23, 16,
          483, 0.0200001, 10000, 1, 49.9998639));
      linkResults.get(timePeriod1).get(mode1).get(node8XmlId).put(node23XmlId, new LinkSegmentExpectedResultsDto(23, 8, 1617,
          0.0200068, 10000, 1, 49.9829143));
      linkResults.get(timePeriod1).get(mode1).get(node23XmlId).put(node8XmlId, new LinkSegmentExpectedResultsDto(8, 23, 1617,
          0.0200068, 10000, 1, 49.9829143));
      linkResults.get(timePeriod1).get(mode1).get(node21XmlId).put(node13XmlId, new LinkSegmentExpectedResultsDto(13, 21,
          0.6, 0.02, 10000, 1, 50));
      linkResults.get(timePeriod1).get(mode1).get(node13XmlId).put(node21XmlId, new LinkSegmentExpectedResultsDto(21, 13,
          0.6, 0.02, 10000, 1, 50));
      linkResults.get(timePeriod1).get(mode1).get(node5XmlId).put(node21XmlId, new LinkSegmentExpectedResultsDto(21, 5,
          899.4, 0.0200007, 10000, 1, 49.9983642));
      linkResults.get(timePeriod1).get(mode1).get(node21XmlId).put(node5XmlId, new LinkSegmentExpectedResultsDto(5, 21,
          899.4, 0.0200007, 10000, 1, 49.9983642));
      linkResults.get(timePeriod1).get(mode1).get(node22XmlId).put(node14XmlId, new LinkSegmentExpectedResultsDto(14, 22,
          17.6, 0.02, 10000, 1, 50));
      linkResults.get(timePeriod1).get(mode1).get(node14XmlId).put(node22XmlId, new LinkSegmentExpectedResultsDto(22, 14,
          17.6, 0.02, 10000, 1, 50));
      linkResults.get(timePeriod1).get(mode1).get(node6XmlId).put(node22XmlId, new LinkSegmentExpectedResultsDto(22, 6,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      linkResults.get(timePeriod1).get(mode1).get(node22XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 22,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      linkResults.get(timePeriod1).get(mode1).get(node24XmlId).put(node15XmlId, new LinkSegmentExpectedResultsDto(15, 24,
          500, 0.0200001, 10000, 1, 49.9998438));
      linkResults.get(timePeriod1).get(mode1).get(node15XmlId).put(node24XmlId, new LinkSegmentExpectedResultsDto(24, 15,
          500, 0.0200001, 10000, 1, 49.9998438));
      linkResults.get(timePeriod1).get(mode1).get(node7XmlId).put(node24XmlId, new LinkSegmentExpectedResultsDto(24, 7, 1900,
          0.020013, 10000, 1, 49.967441));
      linkResults.get(timePeriod1).get(mode1).get(node24XmlId).put(node7XmlId, new LinkSegmentExpectedResultsDto(7, 24, 1900,
          0.020013, 10000, 1, 49.967441));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter,
          maxIterations, linkResults);

      pathMap.put(timePeriod0, new TreeMap<>());
      pathMap.get(timePeriod0).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod0).get(mode1).put(zone1XmlId, new TreeMap<>());
      pathMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone2XmlId,"[21,5,1,2,6,22]");
      pathMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone3XmlId,"[21,5,1,4,8,23]");
      pathMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone4XmlId,"[21,5,1,4,3,7,24]");
      pathMap.get(timePeriod0).get(mode1).put(zone2XmlId, new TreeMap<>());
      pathMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone1XmlId,"[22,6,2,1,5,21]");
      pathMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone3XmlId,"[22,6,2,1,4,8,23]");
      pathMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone4XmlId,"[22,6,2,3,7,24]");
      pathMap.get(timePeriod0).get(mode1).put(zone3XmlId, new TreeMap<>());
      pathMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone1XmlId,"[23,8,4,1,5,21]");
      pathMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone2XmlId,"[23,8,4,1,2,6,22]");
      pathMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone4XmlId,"[23,8,4,3,7,24]");
      pathMap.get(timePeriod0).get(mode1).put(zone4XmlId, new TreeMap<>());
      pathMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone1XmlId,"[24,7,3,4,1,5,21]");
      pathMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone2XmlId,"[24,7,3,2,6,22]");
      pathMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone3XmlId,"[24,7,3,4,8,23]");
      pathMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone4XmlId,"");   
      pathMap.put(timePeriod1, new TreeMap<>());
      pathMap.get(timePeriod1).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod1).get(mode1).put(zone1XmlId, new TreeMap<>());
      pathMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone2XmlId,"[21,5,1,2,6,22]");
      pathMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone3XmlId,"[21,5,1,4,8,23]");
      pathMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone4XmlId,"[21,5,1,4,3,7,24]");
      pathMap.get(timePeriod1).get(mode1).put(zone2XmlId, new TreeMap<>());
      pathMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone1XmlId,"[22,6,2,1,5,21]");
      pathMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone3XmlId,"[22,6,2,1,4,8,23]");
      pathMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone4XmlId,"[22,6,2,3,7,24]");
      pathMap.get(timePeriod1).get(mode1).put(zone3XmlId, new TreeMap<>());
      pathMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone1XmlId,"[23,8,4,1,5,21]");
      pathMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone2XmlId,"[23,8,4,1,2,6,22]");
      pathMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone4XmlId,"[23,8,4,3,7,24]");
      pathMap.get(timePeriod1).get(mode1).put(zone4XmlId, new TreeMap<>());
      pathMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone1XmlId,"[24,7,3,4,1,5,21]");
      pathMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone2XmlId,"[24,7,3,2,6,22]");
      pathMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone3XmlId,"[24,7,3,4,8,23]");
      pathMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone4XmlId,"");   
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
         

      odMap.put(timePeriod0, new TreeMap<>());
      odMap.get(timePeriod0).put(mode1, new TreeMap<>());
      odMap.get(timePeriod0).get(mode1).put(zone1XmlId, new TreeMap<>());
      odMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone1XmlId, 0.0);
      odMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone2XmlId, 0.0767791);
      odMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone3XmlId, 0.0776307);
      odMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone4XmlId, 0.0944051);
      odMap.get(timePeriod0).get(mode1).put(zone2XmlId, new TreeMap<>());
      odMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone1XmlId, 0.0767791);
      odMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone2XmlId, 0.0);
      odMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone3XmlId, 0.0931159);
      odMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone4XmlId, 0.0900226);
      odMap.get(timePeriod0).get(mode1).put(zone3XmlId, new TreeMap<>());
      odMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone1XmlId, 0.0776307);
      odMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone2XmlId, 0.0931159);
      odMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone3XmlId, 0.0);
      odMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone4XmlId, 0.0902602);
      odMap.get(timePeriod0).get(mode1).put(zone4XmlId, new TreeMap<>());
      odMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone1XmlId, 0.0944051);
      odMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone2XmlId, 0.0900226);
      odMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone3XmlId, 0.0902602);
      odMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone4XmlId, 0.0);
      odMap.put(timePeriod1, new TreeMap<>());
      odMap.get(timePeriod1).put(mode1, new TreeMap<>());
      odMap.get(timePeriod1).get(mode1).put(zone1XmlId, new TreeMap<>());
      odMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone1XmlId, 0.0);
      odMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone2XmlId, 0.0767791);
      odMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone3XmlId, 0.0776307);
      odMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone4XmlId, 0.0944051);
      odMap.get(timePeriod1).get(mode1).put(zone2XmlId, new TreeMap<>());
      odMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone1XmlId, 0.0767791);
      odMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone2XmlId, 0.0);
      odMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone3XmlId, 0.0931159);
      odMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone4XmlId, 0.0900226);
      odMap.get(timePeriod1).get(mode1).put(zone3XmlId, new TreeMap<>());
      odMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone1XmlId, 0.0776307);
      odMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone2XmlId, 0.0931159);
      odMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone3XmlId, 0.0);
      odMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone4XmlId, 0.0902602);
      odMap.get(timePeriod1).get(mode1).put(zone4XmlId, new TreeMap<>());
      odMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone1XmlId, 0.0944051);
      odMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone2XmlId, 0.0900226);
      odMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone3XmlId, 0.0902602);
      odMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone4XmlId, 0.0);
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName2, xmlFileName2);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the fourth route choice from the Traditional Static Assignment Route Choice
   * Equilibration Test cases.docx document.
   * 
   * This test case uses the <odrawmatrix> method in the macroscopicinput.xml file
   * to define the OD demands input matrix.
   */
  @Test
  public void test_4_bi_directional_links_route_choice_single_mode_using_odrawmatrix() {
    try {
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"biDirectionalLinksRouteChoiceSingleModeUsingOdRawMatrix").toString();
      String description = "testRouteChoice4raw";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
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
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the fourth route choice example from the Traditional Static Assignment Route
   * Choice Equilibration Test cases.docx document.
   * 
   * This test case uses the <odrawmatrix> method with the plus sign as separator
   * in the macroscopicinput.xml file to define the OD demands input matrix.
   */
  @Test
  public void test_4_bi_directional_links_route_choice_single_mode_with_plus_sign_separator() {
    try {
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"biDirectionalLinksRouteChoiceSingleModeWithPlusSignSeparator").toString();
      String description = "testRouteChoice4raw2";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(projectPath, description);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      runner.setupAndExecuteDefaultAssignment();      

      /* compare results */                 

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
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the fifth route choice example from the Traditional Static Assignment Route
   * Choice Equilibration Test cases.docx document.
   * 
   * This test case uses two modes and some modes are not allowed on some links.
   */
  @Test
  public void test_5_SIMO_MISO_route_choice_two_modes() {
    try {
      final String inputPath = ROUTE_CHOICE_TEST_CASE_INPUT_PATH_SIMO_MISO_TWO_MODES.toString();
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"SIMOMISOrouteChoiceTwoModes").toString();
      String description = "testRouteChoice5";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      TriConsumer<LayeredNetwork<?,?>, PhysicalCostConfigurator<?>, PlanItInputBuilder4Testing> setCostParameters =
              (physicalNetwork, bpr, inputBuilderListener) -> {
                Mode mode = physicalNetwork.getModes().getByXmlId("2");
                MacroscopicNetworkLayer layer = (MacroscopicNetworkLayer)physicalNetwork.getLayerByMode(mode);
                MacroscopicLinkSegmentType macroscopiclinkSegmentType = layer.getLinkSegmentTypes().getByXmlId("1");
                ((BPRConfigurator)bpr).setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
              };
      
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(inputPath, projectPath, description, TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      runner.setMaxIterations(maxIterations);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      runner.setGapFunctionEpsilonGap(0.0);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteWithPhysicalCostConfiguration(setCostParameters);

      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      MacroscopicNetwork network = (MacroscopicNetwork) testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Mode mode2 = network.getModes().getByXmlId("2");
      TimePeriod timePeriod = testOutputDto.getB().demands.getFirst().timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
      
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node1XmlId,  node11XmlId, new LinkSegmentExpectedResultsDto(11, 1, 3000,0.0370117, 3600.0, 1.0, 27.0184697));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node2XmlId,  node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 6, 0.0447625, 1200.0, 2.0, 44.6802634));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node3XmlId,  node1XmlId, new LinkSegmentExpectedResultsDto(1, 3, 1068, 0.0360526, 1200.0, 1.0, 27.7372551));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node4XmlId,  node1XmlId, new LinkSegmentExpectedResultsDto(1, 4, 1926, 0.0719659, 1200.0, 1.0, 13.8954751));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node4XmlId,  node2XmlId, new LinkSegmentExpectedResultsDto(2, 4, 6, 0.0447625, 1200.0, 2.0, 44.6802634));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node4XmlId,  node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 1068, 0.0360526, 1200.0, 1.0, 27.7372551));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode1, node12XmlId, node4XmlId, new LinkSegmentExpectedResultsDto(4, 12, 3000, 0.0370117, 3600.0, 1.0, 27.0184697));

      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode2, node1XmlId, node11XmlId, new LinkSegmentExpectedResultsDto(11, 1, 600, 0.0636732, 3600.0, 1.0, 15.705194));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode2, node2XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 434.4, 0.0609332, 1200.0, 2.0, 32.8228128));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode2, node3XmlId, node1XmlId, new LinkSegmentExpectedResultsDto(1, 3, 165.6, 0.0613639, 1200.0, 1.0, 16.296231));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode2, node4XmlId, node2XmlId, new LinkSegmentExpectedResultsDto(2, 4, 434.4, 0.0609332, 1200.0, 2.0, 32.8228128));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode2, node4XmlId, node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 165.6, 0.0613639, 1200.0, 1.0, 16.296231));
      PlanItIOTestHelper.addToNestedMap(linkResults, timePeriod, mode2, node12XmlId, node4XmlId, new LinkSegmentExpectedResultsDto(4, 12, 600, 0.0636732, 3600.0, 1.0, 15.705194));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter, maxIterations, linkResults);
      
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone27XmlId, zone27XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone27XmlId, zone31XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone31XmlId, zone27XmlId,"[11,1,4,12]");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone31XmlId, zone31XmlId,"");

      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode2, zone27XmlId, zone27XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode2, zone27XmlId, zone31XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode2, zone31XmlId, zone27XmlId,"[11,1,2,4,12]");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode2, zone31XmlId, zone31XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);           
      
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone27XmlId, zone27XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone27XmlId, zone31XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone31XmlId, zone27XmlId, 0.1457425);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone31XmlId, zone31XmlId, 0.0);
      
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode2, zone27XmlId, zone27XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode2, zone27XmlId, zone31XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode2, zone31XmlId, zone27XmlId, 0.249072);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode2, zone31XmlId, zone31XmlId, 0.0);
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

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
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the fifth route choice example from the Traditional Static Assignment Route
   * Choice Equilibration Test cases.docx document.
   * 
   * This test case identifies links using link Id (all other tests use link
   * external Id).
   */
  @Test
  public void test_5_SIMO_MISO_route_choice_two_modes_identify_links_by_id() {
    try {
      final String inputPath = ROUTE_CHOICE_TEST_CASE_INPUT_PATH_SIMO_MISO_TWO_MODES.toString();
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"SIMOMISOrouteChoiceTwoModesIdentifyLinksById").toString();
      String description = "testRouteChoice5";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      TriConsumer<LayeredNetwork<?,?>, PhysicalCostConfigurator<?>, PlanItInputBuilder4Testing> setCostParameters =
              (physicalNetwork, bpr, inputBuilderListener) -> {
                Mode mode = physicalNetwork.getModes().getByXmlId("2");
                MacroscopicNetworkLayer layer = (MacroscopicNetworkLayer)physicalNetwork.getLayerByMode(mode);
                MacroscopicLinkSegmentType macroscopiclinkSegmentType = layer.getLinkSegmentTypes().getByXmlId("1");
                ((BPRConfigurator)bpr).setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
              };

      Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties = (
          linkOutputTypeConfiguration) -> {
        try {
          linkOutputTypeConfiguration.removeProperty(OutputPropertyType.DOWNSTREAM_NODE_XML_ID);
          linkOutputTypeConfiguration.removeProperty(OutputPropertyType.UPSTREAM_NODE_XML_ID);
          linkOutputTypeConfiguration.addProperties(OutputPropertyType.DOWNSTREAM_NODE_ID, OutputPropertyType.UPSTREAM_NODE_ID, OutputPropertyType.LINK_SEGMENT_ID);
          linkOutputTypeConfiguration.addProperty(OutputPropertyType.MAXIMUM_SPEED);
        } catch (PlanItException e) {
          e.printStackTrace();
          LOGGER.severe(e.getMessage());
          fail(e.getMessage());
        }
      };
      
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(inputPath, projectPath, description, TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      runner.setMaxIterations(maxIterations);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      runner.setGapFunctionEpsilonGap(0.0);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = 
          runner.setupAndExecuteWithCustomBprAndLinkOutputTypeConfiguration(setCostParameters,setOutputTypeConfigurationProperties);      

      /* compare results */
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      MacroscopicNetwork network = (MacroscopicNetwork) testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Mode mode2 = network.getModes().getByXmlId("2");
      TimePeriod timePeriod = testOutputDto.getB().demands.getFirst().timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));

      PlanItIOTestHelper.addToNestedMap(linkIdResultsMap, timePeriod, mode1, linkSegment0Id, new LinkSegmentExpectedResultsDto(11, 1, 3000, 0.0370117,3600.0, 1.0, 27.0184697));
      PlanItIOTestHelper.addToNestedMap(linkIdResultsMap, timePeriod, mode1, linkSegment1Id, new LinkSegmentExpectedResultsDto(1, 4, 1926, 0.0719659,1200.0, 1.0, 13.8954751));
      PlanItIOTestHelper.addToNestedMap(linkIdResultsMap, timePeriod, mode1, linkSegment2Id, new LinkSegmentExpectedResultsDto(4, 12, 3000, 0.0370117,3600.0, 1.0, 27.0184697));
      PlanItIOTestHelper.addToNestedMap(linkIdResultsMap, timePeriod, mode1, linkSegment3Id, new LinkSegmentExpectedResultsDto(1, 2, 6, 0.0447625,1200.0, 2.0, 44.6802634));
      PlanItIOTestHelper.addToNestedMap(linkIdResultsMap, timePeriod, mode1, linkSegment4Id, new LinkSegmentExpectedResultsDto(2, 4, 6, 0.0447625,1200.0, 2.0, 44.6802634));
      PlanItIOTestHelper.addToNestedMap(linkIdResultsMap, timePeriod, mode1, linkSegment5Id, new LinkSegmentExpectedResultsDto(1, 3, 1068, 0.0360526,1200.0, 1.0, 27.7372551));
      PlanItIOTestHelper.addToNestedMap(linkIdResultsMap, timePeriod, mode1, linkSegment6Id, new LinkSegmentExpectedResultsDto(3, 4, 1068, 0.0360526,1200.0, 1.0, 27.7372551));

      PlanItIOTestHelper.addToNestedMap(linkIdResultsMap, timePeriod, mode2, linkSegment0Id, new LinkSegmentExpectedResultsDto(11, 1, 600, 0.0636732,3600.0, 1.0, 15.705194));
      PlanItIOTestHelper.addToNestedMap(linkIdResultsMap, timePeriod, mode2, linkSegment2Id, new LinkSegmentExpectedResultsDto(4, 12, 600, 0.0636732,3600.0, 1.0, 15.705194));
      PlanItIOTestHelper.addToNestedMap(linkIdResultsMap, timePeriod, mode2, linkSegment3Id, new LinkSegmentExpectedResultsDto(1, 2, 434.4, 0.0609332,1200.0, 2.0, 32.8228128));
      PlanItIOTestHelper.addToNestedMap(linkIdResultsMap, timePeriod, mode2, linkSegment4Id, new LinkSegmentExpectedResultsDto(2, 4, 434.4, 0.0609332,1200.0, 2.0, 32.8228128));
      PlanItIOTestHelper.addToNestedMap(linkIdResultsMap, timePeriod, mode2, linkSegment5Id, new LinkSegmentExpectedResultsDto(1, 3, 165.6, 0.0613639,1200.0, 1.0, 16.296231));
      PlanItIOTestHelper.addToNestedMap(linkIdResultsMap, timePeriod, mode2, linkSegment6Id, new LinkSegmentExpectedResultsDto(3, 4, 165.6, 0.0613639,1200.0, 1.0, 16.296231));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingLinkSegmentId(memoryOutputFormatter, maxIterations, linkIdResultsMap);
      
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone27XmlId, zone27XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone27XmlId, zone31XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone31XmlId, zone27XmlId,"[11,1,4,12]");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode1, zone31XmlId, zone31XmlId,"");

      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode2, zone27XmlId,zone27XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode2, zone27XmlId,zone31XmlId,"");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode2, zone31XmlId,zone27XmlId,"[11,1,2,4,12]");
      PlanItIOTestHelper.addToNestedMap(pathMap, timePeriod, mode2, zone31XmlId,zone31XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);         
      
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone27XmlId, zone27XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone27XmlId, zone31XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone31XmlId, zone27XmlId, 0.1457425);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode1, zone31XmlId, zone31XmlId, 0.0);
      
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode2, zone27XmlId, zone27XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode2, zone27XmlId, zone31XmlId, 0.0);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode2, zone31XmlId, zone27XmlId, 0.249072);
      PlanItIOTestHelper.addToNestedMap(odMap, timePeriod, mode2, zone31XmlId, zone31XmlId, 0.0);
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

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
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the fifth route choice example from the Traditional Static Assignment Route
   * Choice Equilibration Test cases.docx document.
   * 
   * This test case uses two modes and only one route is possible for trucks.  The
   * persistZeroFlow flag is set to true, meaning that some of the OD costs are 
   * reported as "Inifinity" for forbidden truck routes.
   */
  @Test
  public void test_5_SIMO_MISO_route_choice_two_modes_with_impossible_route() {
    try {
      final String projectPath = Path.of(ROUTE_CHOICE_TEST_CASE_PATH.toString(),"SIMOMISOrouteChoiceTwoModesWithImpossibleRoute").toString();
      final String inputPath = projectPath; // fiddled with network to create impossible route, hence not same as other two mode SIMOMISOs
      String description = "testRouteChoice5";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      TriConsumer<LayeredNetwork<?,?>, PhysicalCostConfigurator<?>, PlanItInputBuilder4Testing> setCostParameters =
          (network,bpr, inputBuilderListener) -> {
            Mode mode = network.getModes().getByXmlId("2");
            MacroscopicNetworkLayer layer = (MacroscopicNetworkLayer)network.getLayerByMode(mode);
            MacroscopicLinkSegmentType macroscopiclinkSegmentType = layer.getLinkSegmentTypes().getByXmlId("1");
            ((BPRConfigurator)bpr).setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
          };
          
      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(inputPath, projectPath, description, TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      runner.setMaxIterations(maxIterations);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(true);
      runner.setGapFunctionEpsilonGap(0.0);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = 
          runner.setupAndExecuteWithPhysicalCostConfiguration(setCostParameters);

      /* compare results */          

      //TODO - Comparisons with MemoryOutputFormatter have been commented out due to insufficient time to configure them      
      MacroscopicNetwork network = (MacroscopicNetwork) testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Mode mode2 = network.getModes().getByXmlId("2");
      TimePeriod timePeriod = testOutputDto.getB().demands.getFirst().timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
      
      linkResults.put(timePeriod, new TreeMap<>());
      linkResults.get(timePeriod).put(mode1, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node1XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node2XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node3XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node4XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).put(node12XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode1).get(node1XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 1, 3000,
          0.0370117, 3600.0, 1.0, 27.0184697));
      linkResults.get(timePeriod).get(mode1).get(node2XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 6,
          0.0447625, 1200.0, 2.0, 44.6802634));
      linkResults.get(timePeriod).get(mode1).get(node3XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 3, 1068,
          0.0360526, 1200.0, 1.0, 27.7372551));
      linkResults.get(timePeriod).get(mode1).get(node4XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 4, 1926,
          0.0719659, 1200.0, 1.0, 13.8954751));
      linkResults.get(timePeriod).get(mode1).get(node4XmlId).put(node2XmlId, new LinkSegmentExpectedResultsDto(2, 4, 6,
          0.0447625, 1200.0, 2.0, 44.6802634));
      linkResults.get(timePeriod).get(mode1).get(node4XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 1068,
          0.0360526, 1200.0, 1.0, 27.7372551));
      linkResults.get(timePeriod).get(mode1).get(node12XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 12, 3000,
          0.0370117, 3600.0, 1.0, 27.0184697));

      linkResults.get(timePeriod).put(mode2, new TreeMap<>());
      linkResults.get(timePeriod).get(mode2).put(node1XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode2).put(node2XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode2).put(node3XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode2).put(node4XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode2).put(node12XmlId, new TreeMap<>());
      linkResults.get(timePeriod).get(mode2).get(node1XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 1, 1500,
          0.0636732, 3600.0, 1.0, 15.705194));
      linkResults.get(timePeriod).get(mode2).get(node2XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 1086,
          0.0609332, 1200.0, 2.0, 32.8228128));
      linkResults.get(timePeriod).get(mode2).get(node3XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 3, 414,
          0.0613639, 1200.0, 1.0, 16.296231));
      linkResults.get(timePeriod).get(mode2).get(node4XmlId).put(node2XmlId, new LinkSegmentExpectedResultsDto(2, 4, 1086,
          0.0609332, 1200.0, 2.0, 32.8228128));
      linkResults.get(timePeriod).get(mode2).get(node4XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 414,
          0.0613639, 1200.0, 1.0, 16.296231));
      linkResults.get(timePeriod).get(mode2).get(node12XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 12, 1500,
          0.0636732, 3600.0, 1.0, 15.705194));
      //PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
      //    maxIterations, resultsMap);
      
      pathMap.put(timePeriod, new TreeMap<>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod).put(mode2, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).put(zone27XmlId, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).get(zone27XmlId).put(zone27XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone27XmlId).put(zone31XmlId,"");
      pathMap.get(timePeriod).get(mode2).put(zone27XmlId, new TreeMap<>());
      pathMap.get(timePeriod).get(mode2).get(zone27XmlId).put(zone27XmlId,"");
      pathMap.get(timePeriod).get(mode2).get(zone27XmlId).put(zone31XmlId,"");
      pathMap.get(timePeriod).get(mode1).put(zone31XmlId, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).get(zone31XmlId).put(zone27XmlId,"[11,1,4,12]");
      pathMap.get(timePeriod).get(mode1).get(zone31XmlId).put(zone31XmlId,"");
      pathMap.get(timePeriod).get(mode2).put(zone31XmlId, new TreeMap<>());
      pathMap.get(timePeriod).get(mode2).get(zone31XmlId).put(zone27XmlId,"[11,1,2,4,12]");
      pathMap.get(timePeriod).get(mode2).get(zone31XmlId).put(zone31XmlId,"");
      //PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      odMap.put(timePeriod, new TreeMap<>());
      odMap.get(timePeriod).put(mode1, new TreeMap<>());
      odMap.get(timePeriod).put(mode2, new TreeMap<>());
      
      odMap.get(timePeriod).get(mode1).put(zone27XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).get(zone27XmlId).put(zone27XmlId, 0.0);
      odMap.get(timePeriod).get(mode1).get(zone27XmlId).put(zone31XmlId, 0.0);
      
      odMap.get(timePeriod).get(mode2).put(zone27XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode2).get(zone27XmlId).put(zone27XmlId, 0.0);
      odMap.get(timePeriod).get(mode2).get(zone27XmlId).put(zone31XmlId, 0.0);
      
      odMap.get(timePeriod).get(mode1).put(zone31XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).get(zone31XmlId).put(zone27XmlId, 0.1457425);
      odMap.get(timePeriod).get(mode1).get(zone31XmlId).put(zone31XmlId, 0.0);
      
      odMap.get(timePeriod).get(mode2).put(zone31XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode2).get(zone31XmlId).put(zone27XmlId, 0.249072);
      odMap.get(timePeriod).get(mode2).get(zone31XmlId).put(zone31XmlId, 0.0);
      //PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
}