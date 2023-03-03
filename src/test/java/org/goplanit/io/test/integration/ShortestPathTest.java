package org.goplanit.io.test.integration;

import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.goplanit.demands.Demands;
import org.goplanit.io.test.util.PlanItIOTestHelper;
import org.goplanit.io.test.util.PlanItIOTestRunner;
import org.goplanit.io.test.util.PlanItInputBuilder4Testing;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.test.LinkSegmentExpectedResultsDto;
import org.goplanit.utils.test.TestOutputDto;
import org.goplanit.utils.time.TimePeriod;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * JUnit test cases for shortest path (AON) tests for TraditionalStaticAssignment
 * 
 * @author gman6028, markr
 *
 */
public class ShortestPathTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path testCasePath = Path.of("src","test","resources","testcases");
  
  private final String zone1XmlId = "1";
  private final String zone2XmlId = "2";
  private final String zone3XmlId = "3";
  private final String zone4XmlId = "4";
  
  private final String node1XmlId = "1";
  private final String node3XmlId = "3";
  private final String node4XmlId = "4";
  private final String node5XmlId = "5";
  private final String node6XmlId = "6";
  private final String node7XmlId = "7";
  private final String node8XmlId = "8";
  private final String node9XmlId = "9";
  private final String node10XmlId = "10";  
  private final String node11XmlId = "11";
  private final String node12XmlId = "12";
  private final String node13XmlId = "13";
  private final String node14XmlId = "14";
  private final String node15XmlId = "15";
  private final String node20XmlId = "20";  
  
  /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, path string */
  private Map<TimePeriod, Map<Mode, Map<String, Map<String, String>>>> pathMap;  
  /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, od value */
  private Map<TimePeriod, Map<Mode, Map<String, Map<String, Double>>>> odMap;
  /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, result DTO */
  SortedMap<TimePeriod, SortedMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>> resultsMap;

  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(ShortestPathTest.class);
    } 
  }
  
  @Before
  public void beforeTest() {
    pathMap = new TreeMap<TimePeriod, Map<Mode, Map<String, Map<String, String>>>>();
    odMap = new TreeMap<TimePeriod, Map<Mode, Map<String, Map<String, Double>>>>();    
    resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>>();      
  }

  @AfterClass
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
      String projectPath = Path.of(testCasePath.toString(),"basicShortestPathAlgorithm","xml","AtoB").toString();
      String initialCostPath = Path.of(projectPath,"initial_link_segment_costs.csv").toString();
      String description = "testBasic1";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);     

      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
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
      String projectPath = Path.of(testCasePath.toString(),"basicShortestPathAlgorithm","xml","AtoB").toString();
      String initialCostPath = Path.of(projectPath,"initial_link_segment_costs.csv").toString();
      String initialCostPath1 = Path.of(projectPath,"initial_link_segment_costs1.csv").toString();
      String description = "testBasic1";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);
      
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
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
      String projectPath = Path.of(testCasePath.toString(),"basicShortestPathAlgorithm","xml","AtoCLinkSegmentMaximumSpeed").toString();
      String description = "testBasic2";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);    
      
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();       

      /* compare results */
      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = (Demands)testOutputDto.getB().demands.getFirst();
      TimePeriod timePeriod = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      resultsMap.put(timePeriod, new TreeMap<>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).put(node6XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node6XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put(node11XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node11XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1, 12,2000, 12, 1));
      resultsMap.get(timePeriod).get(mode1).put(node12XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node12XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1,8, 2000, 8, 1));
      resultsMap.get(timePeriod).get(mode1).put(node13XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node13XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 13, 1,47, 2000, 47, 1));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter,null, resultsMap);

      pathMap.put(timePeriod, new TreeMap<>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,6,11,12,13]");
      pathMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, null, pathMap);
      
      odMap.put(timePeriod, new TreeMap<>());
      odMap.get(timePeriod).put(mode1, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId, Double.valueOf(77.0));
      odMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);
      
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
   */
  @Test
  public void test_basic_shortest_path_algorithm_a_to_c() {
    try {
      String projectPath = Path.of(testCasePath.toString(),"basicShortestPathAlgorithm","xml","AtoC").toString();
      String description = "testBasic2";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);        

      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();       

      /* compare results */
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = (Demands)testOutputDto.getB().demands.getFirst();
      TimePeriod timePeriod = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
      
      resultsMap.put(timePeriod, new TreeMap<>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).put(node6XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node6XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put(node11XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node11XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1, 12,2000, 12, 1));
      resultsMap.get(timePeriod).get(mode1).put(node12XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node12XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1,8, 2000, 8, 1));
      resultsMap.get(timePeriod).get(mode1).put(node13XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node13XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 13, 1,47, 2000, 47, 1));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter, null, resultsMap);

      pathMap.put(timePeriod, new TreeMap<>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,6,11,12,13]");
      pathMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, null, pathMap);
      
      odMap.put(timePeriod, new TreeMap<>());
      odMap.get(timePeriod).put(mode1, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId, Double.valueOf(77.0));
      odMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);
      
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
   * This test case uses route A to D in the example, which has a total route cost
   * of 108 (the fifth argument in the ResultDto constructor).
   */
  @Test
  public void test_basic_shortest_path_algorithm_a_to_d() {
    try {
      String projectPath = Path.of(testCasePath.toString(),"basicShortestPathAlgorithm","xml","AtoD").toString();
      String description = "testBasic3";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);      

      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();       

      /* compare results */
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = testOutputDto.getB().demands.getFirst();
      TimePeriod timePeriod = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
      
      resultsMap.put(timePeriod, new TreeMap<>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).put(node6XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node6XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put(node8XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node8XmlId).put(node7XmlId, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,2000, 12, 1));
      resultsMap.get(timePeriod).get(mode1).put(node9XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node9XmlId).put(node8XmlId, new LinkSegmentExpectedResultsDto(8, 9, 1, 20,2000, 20, 1));
      resultsMap.get(timePeriod).get(mode1).put(node11XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node11XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1, 12,2000, 12, 1));
      resultsMap.get(timePeriod).get(mode1).put(node7XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node7XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,2000, 5, 1));
      resultsMap.get(timePeriod).get(mode1).put(node14XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node14XmlId).put(node9XmlId, new LinkSegmentExpectedResultsDto(9, 14, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put(node12XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node12XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1,8, 2000, 8, 1));
      resultsMap.get(timePeriod).get(mode1).put(node15XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node15XmlId).put(node14XmlId, new LinkSegmentExpectedResultsDto(14, 15, 1,10, 2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put(node20XmlId, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).get(node20XmlId).put(node15XmlId, new LinkSegmentExpectedResultsDto(15, 20, 1,21, 2000, 21, 1));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter,null, resultsMap);

      pathMap.put(timePeriod, new TreeMap<>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,6,11,12,7,8,9,14,15,20]");
      pathMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, null, pathMap);
      
      odMap.put(timePeriod, new TreeMap<>());
      odMap.get(timePeriod).put(mode1, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId, Double.valueOf(108.0));
      odMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<>());
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);

      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
  
  @Test
  public void test_basic_shortest_path_algorithm_three_time_periods_record_zero_flow() {
    try {
      String projectPath = Path.of(testCasePath.toString(),"basicShortestPathAlgorithm","xml","ThreeTimePeriodsRecordZeroFlow").toString();
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
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName3);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName3);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName3);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName3);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName3);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName3);           

      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(true); // <- zero flow
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();       

      /* compare results */
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = (Demands)testOutputDto.getB().demands.getFirst();
      TimePeriod timePeriod0 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
      
      resultsMap.put(timePeriod0, new TreeMap<>());
      resultsMap.get(timePeriod0).put(mode1, new TreeMap<>());
      resultsMap.get(timePeriod0).get(mode1).put(node3XmlId, new TreeMap<>());
      resultsMap.get(timePeriod0).get(mode1).put(node4XmlId, new TreeMap<>());
      resultsMap.get(timePeriod0).get(mode1).put(node5XmlId, new TreeMap<>());
      resultsMap.get(timePeriod0).get(mode1).put(node6XmlId, new TreeMap<>());
      resultsMap.get(timePeriod0).get(mode1).put(node7XmlId, new TreeMap<>());
      resultsMap.get(timePeriod0).get(mode1).put(node8XmlId, new TreeMap<>());
      resultsMap.get(timePeriod0).get(mode1).put(node10XmlId, new TreeMap<>());
      resultsMap.get(timePeriod0).get(mode1).put(node11XmlId, new TreeMap<>());
      resultsMap.get(timePeriod0).get(mode1).put(node12XmlId, new TreeMap<>());
      resultsMap.get(timePeriod0).get(mode1).get(node3XmlId).put(node8XmlId, new LinkSegmentExpectedResultsDto(8, 3, 1, 8,2000, 8, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node4XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node5XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 5, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node6XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node7XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,2000, 5, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node8XmlId).put(node7XmlId, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,2000, 12, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node10XmlId).put(node5XmlId, new LinkSegmentExpectedResultsDto(5, 10, 1,10, 2000, 10, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node11XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1,12, 2000, 12, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node12XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1,8, 2000, 8, 1));

      /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, path string */ 
      pathMap.put(timePeriod0, new TreeMap<>());
      pathMap.get(timePeriod0).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod0).get(mode1).put(zone1XmlId, new TreeMap<>());
      pathMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,6,11,12,7,8,3,4,5,10]");
      pathMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone3XmlId,"[1,6,11,12,13]");
      pathMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone4XmlId,"[1,6,11,12,7,8,9,14,15,20]");
      pathMap.get(timePeriod0).get(mode1).put(zone2XmlId, new TreeMap<>());
      pathMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone1XmlId,"[10,5,4,3,8,7,12,11,6,1]");
      pathMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone3XmlId,"[10,15,14,13]");
      pathMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone4XmlId,"[10,15,20]");
      pathMap.get(timePeriod0).get(mode1).put(zone3XmlId, new TreeMap<>());
      pathMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone1XmlId,"[13,12,11,6,1]");
      pathMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone2XmlId,"[13,14,15,10]");
      pathMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone4XmlId,"[13,14,15,20]");
      pathMap.get(timePeriod0).get(mode1).put(zone4XmlId, new TreeMap<>());
      pathMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone1XmlId,"[20,15,14,9,8,7,12,11,6,1]");
      pathMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone2XmlId,"[20,15,10]");
      pathMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone3XmlId,"[20,15,14,13]");
      pathMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone4XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, null, pathMap);
      
      odMap.put(timePeriod0, new TreeMap<>());
      odMap.get(timePeriod0).put(mode1, new TreeMap<>());
      odMap.get(timePeriod0).get(mode1).put(zone1XmlId, new TreeMap<>());
      odMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone2XmlId,Double.valueOf(85.0));
      odMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone3XmlId,Double.valueOf(77.0));
      odMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone4XmlId,Double.valueOf(108.0));
      odMap.get(timePeriod0).get(mode1).put(zone2XmlId, new TreeMap<>());
      odMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone1XmlId,Double.valueOf(85.0));
      odMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone3XmlId,Double.valueOf(18.0));
      odMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone4XmlId,Double.valueOf(24.0));
      odMap.get(timePeriod0).get(mode1).put(zone3XmlId, new TreeMap<>());
      odMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone1XmlId,Double.valueOf(77.0));
      odMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone2XmlId,Double.valueOf(18.0));
      odMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone4XmlId,Double.valueOf(36.0));
      odMap.get(timePeriod0).get(mode1).put(zone4XmlId, new TreeMap<>());
      odMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone1XmlId,Double.valueOf(108.0));
      odMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone2XmlId,Double.valueOf(24.0));
      odMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone3XmlId,Double.valueOf(36.0));
      odMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone4XmlId,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);
 
      TimePeriod timePeriod1 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("1"));
      
      resultsMap.put(timePeriod1, new TreeMap<>());
      resultsMap.get(timePeriod1).put(mode1, new TreeMap<>());
      resultsMap.get(timePeriod1).get(mode1).put(node6XmlId, new TreeMap<>());
      resultsMap.get(timePeriod1).get(mode1).put(node11XmlId, new TreeMap<>());
      resultsMap.get(timePeriod1).get(mode1).put(node12XmlId, new TreeMap<>());
      resultsMap.get(timePeriod1).get(mode1).put(node13XmlId, new TreeMap<>());
      resultsMap.get(timePeriod1).get(mode1).get(node6XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get(node11XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1,12, 2000, 12, 1));
      resultsMap.get(timePeriod1).get(mode1).get(node12XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1,8, 2000, 8, 1));
      resultsMap.get(timePeriod1).get(mode1).get(node13XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 13, 1,47, 2000, 47, 1));
      
      /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, path string */ 
      pathMap.put(timePeriod1, new TreeMap<>());
      pathMap.get(timePeriod1).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod1).get(mode1).put(zone1XmlId, new TreeMap<>());
      pathMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,6,11,12,7,8,3,4,5,10]");
      pathMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone3XmlId,"[1,6,11,12,13]");
      pathMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone4XmlId,"[1,6,11,12,7,8,9,14,15,20]");
      pathMap.get(timePeriod1).get(mode1).put(zone2XmlId, new TreeMap<>());
      pathMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone1XmlId,"[10,5,4,3,8,7,12,11,6,1]");
      pathMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone3XmlId,"[10,15,14,13]");
      pathMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone4XmlId,"[10,15,20]");
      pathMap.get(timePeriod1).get(mode1).put(zone3XmlId, new TreeMap<>());
      pathMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone1XmlId,"[13,12,11,6,1]");
      pathMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone2XmlId,"[13,14,15,10]");
      pathMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone4XmlId,"[13,14,15,20]");
      pathMap.get(timePeriod1).get(mode1).put(zone4XmlId, new TreeMap<>());
      pathMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone1XmlId,"[20,15,14,9,8,7,12,11,6,1]");
      pathMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone2XmlId,"[20,15,10]");
      pathMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone3XmlId,"[20,15,14,13]");
      pathMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone4XmlId,"");      
      
      odMap.put(timePeriod1, new TreeMap<>());
      odMap.get(timePeriod1).put(mode1, new TreeMap<>());
      odMap.get(timePeriod1).get(mode1).put(zone1XmlId, new TreeMap<>());
      odMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone2XmlId,Double.valueOf(85.0));
      odMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone3XmlId,Double.valueOf(77.0));
      odMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone4XmlId,Double.valueOf(108.0));
      odMap.get(timePeriod1).get(mode1).put(zone2XmlId, new TreeMap<>());
      odMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone1XmlId,Double.valueOf(85.0));
      odMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone3XmlId,Double.valueOf(18.0));
      odMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone4XmlId,Double.valueOf(24.0));
      odMap.get(timePeriod1).get(mode1).put(zone3XmlId, new TreeMap<>());
      odMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone1XmlId,Double.valueOf(77.0));
      odMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone2XmlId,Double.valueOf(18.0));
      odMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone4XmlId,Double.valueOf(36.0));
      odMap.get(timePeriod1).get(mode1).put(zone4XmlId, new TreeMap<>());
      odMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone1XmlId,Double.valueOf(108.0));
      odMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone2XmlId,Double.valueOf(24.0));
      odMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone3XmlId,Double.valueOf(36.0));
      odMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone4XmlId,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);

      TimePeriod timePeriod2 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("2"));
      resultsMap.put(timePeriod2, new TreeMap<>());
      resultsMap.get(timePeriod2).put(mode1, new TreeMap<>());
      resultsMap.get(timePeriod2).get(mode1).put(node6XmlId, new TreeMap<>());
      resultsMap.get(timePeriod2).get(mode1).put(node7XmlId, new TreeMap<>());
      resultsMap.get(timePeriod2).get(mode1).put(node8XmlId, new TreeMap<>());
      resultsMap.get(timePeriod2).get(mode1).put(node9XmlId, new TreeMap<>());
      resultsMap.get(timePeriod2).get(mode1).put(node11XmlId, new TreeMap<>());
      resultsMap.get(timePeriod2).get(mode1).put(node12XmlId, new TreeMap<>());
      resultsMap.get(timePeriod2).get(mode1).put(node14XmlId, new TreeMap<>());
      resultsMap.get(timePeriod2).get(mode1).put(node15XmlId, new TreeMap<>());
      resultsMap.get(timePeriod2).get(mode1).put(node20XmlId, new TreeMap<>());
      resultsMap.get(timePeriod2).get(mode1).get(node6XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node7XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,2000, 5, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node8XmlId).put(node7XmlId, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,2000, 12, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node9XmlId).put(node8XmlId, new LinkSegmentExpectedResultsDto(8, 9, 1, 20,2000, 20, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node11XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1,12, 2000, 12, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node12XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1,8, 2000, 8, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node14XmlId).put(node9XmlId, new LinkSegmentExpectedResultsDto(9, 14, 1,10, 2000, 10, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node15XmlId).put(node14XmlId, new LinkSegmentExpectedResultsDto(14, 15, 1,10, 2000, 10, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node20XmlId).put(node15XmlId, new LinkSegmentExpectedResultsDto(15, 20, 1,21, 2000, 21, 1));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter,null, resultsMap);

      /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, path string */ 
      pathMap.put(timePeriod2, new TreeMap<>());
      pathMap.get(timePeriod2).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod2).get(mode1).put(zone1XmlId, new TreeMap<>());
      pathMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,6,11,12,7,8,3,4,5,10]");
      pathMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone3XmlId,"[1,6,11,12,13]");
      pathMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone4XmlId,"[1,6,11,12,7,8,9,14,15,20]");
      pathMap.get(timePeriod2).get(mode1).put(zone2XmlId, new TreeMap<>());
      pathMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone1XmlId,"[10,5,4,3,8,7,12,11,6,1]");
      pathMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone3XmlId,"[10,15,14,13]");
      pathMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone4XmlId,"[10,15,20]");
      pathMap.get(timePeriod2).get(mode1).put(zone3XmlId, new TreeMap<>());
      pathMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone1XmlId,"[13,12,11,6,1]");
      pathMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone2XmlId,"[13,14,15,10]");
      pathMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone4XmlId,"[13,14,15,20]");
      pathMap.get(timePeriod2).get(mode1).put(zone4XmlId, new TreeMap<>());
      pathMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone1XmlId,"[20,15,14,9,8,7,12,11,6,1]");
      pathMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone2XmlId,"[20,15,10]");
      pathMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone3XmlId,"[20,15,14,13]");
      pathMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone4XmlId,""); 
      
      odMap.put(timePeriod2, new TreeMap<>());
      odMap.get(timePeriod2).put(mode1, new TreeMap<>());
      odMap.get(timePeriod2).get(mode1).put(zone1XmlId, new TreeMap<>());
      odMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone2XmlId,Double.valueOf(85.0));
      odMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone3XmlId,Double.valueOf(77.0));
      odMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone4XmlId,Double.valueOf(108.0));
      odMap.get(timePeriod2).get(mode1).put(zone2XmlId, new TreeMap<>());
      odMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone1XmlId,Double.valueOf(85.0));
      odMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone3XmlId,Double.valueOf(18.0));
      odMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone4XmlId,Double.valueOf(24.0));
      odMap.get(timePeriod2).get(mode1).put(zone3XmlId, new TreeMap<>());
      odMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone1XmlId,Double.valueOf(77.0));
      odMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone2XmlId,Double.valueOf(18.0));
      odMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone4XmlId,Double.valueOf(36.0));
      odMap.get(timePeriod2).get(mode1).put(zone4XmlId, new TreeMap<>());
      odMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone1XmlId,Double.valueOf(108.0));
      odMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone2XmlId,Double.valueOf(24.0));
      odMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone3XmlId,Double.valueOf(36.0));
      odMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone4XmlId,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);

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
      String projectPath = Path.of(testCasePath.toString(),"basicShortestPathAlgorithm","xml","ThreeTimePeriods").toString();
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
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName3);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName3);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName3);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName3);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName3);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName3);          

      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();       

      /* compare results */
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = (Demands)testOutputDto.getB().demands.getFirst();
      TimePeriod timePeriod0 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
      
      resultsMap.put(timePeriod0, new TreeMap<>());
      resultsMap.get(timePeriod0).put(mode1, new TreeMap<>());
      resultsMap.get(timePeriod0).get(mode1).put(node3XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod0).get(mode1).put(node4XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod0).get(mode1).put(node5XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod0).get(mode1).put(node6XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod0).get(mode1).put(node7XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod0).get(mode1).put(node8XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod0).get(mode1).put(node10XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod0).get(mode1).put(node11XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod0).get(mode1).put(node12XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod0).get(mode1).get(node3XmlId).put(node8XmlId, new LinkSegmentExpectedResultsDto(8, 3, 1, 8,2000, 8, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node4XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node5XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 5, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node6XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node7XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,2000, 5, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node8XmlId).put(node7XmlId, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,2000, 12, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node10XmlId).put(node5XmlId, new LinkSegmentExpectedResultsDto(5, 10, 1,10, 2000, 10, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node11XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1,12, 2000, 12, 1));
      resultsMap.get(timePeriod0).get(mode1).get(node12XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1,8, 2000, 8, 1));

      /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, path string */ 
      pathMap.put(timePeriod0, new TreeMap<>());
      pathMap.get(timePeriod0).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod0).get(mode1).put(zone1XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,6,11,12,7,8,3,4,5,10]");
      pathMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone4XmlId,"");
      pathMap.get(timePeriod0).get(mode1).put(zone2XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone4XmlId,"");
      pathMap.get(timePeriod0).get(mode1).put(zone3XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone4XmlId,"");
      pathMap.get(timePeriod0).get(mode1).put(zone4XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone4XmlId,""); 
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, null, pathMap);            
      
      odMap.put(timePeriod0, new TreeMap<>());
      odMap.get(timePeriod0).put(mode1, new TreeMap<>());
      odMap.get(timePeriod0).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone2XmlId,Double.valueOf(85.0));
      odMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone1XmlId).put(zone4XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).put(zone2XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone2XmlId).put(zone4XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).put(zone3XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone3XmlId).put(zone4XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).put(zone4XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod0).get(mode1).get(zone4XmlId).put(zone4XmlId,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);
 
      TimePeriod timePeriod1 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("1"));
      resultsMap.put(timePeriod1, new TreeMap<>());
      resultsMap.get(timePeriod1).put(mode1, new TreeMap<>());
      resultsMap.get(timePeriod1).get(mode1).put(node6XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put(node11XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put(node12XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put(node13XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).get(node6XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get(node11XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1,12, 2000, 12, 1));
      resultsMap.get(timePeriod1).get(mode1).get(node12XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1,8, 2000, 8, 1));
      resultsMap.get(timePeriod1).get(mode1).get(node13XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 13, 1,47, 2000, 47, 1));

      /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, path string */ 
      pathMap.put(timePeriod1, new TreeMap<>());
      pathMap.get(timePeriod1).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod1).get(mode1).put(zone1XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone3XmlId,"[1,6,11,12,13]");
      pathMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone4XmlId,"");
      pathMap.get(timePeriod1).get(mode1).put(zone2XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone4XmlId,"");
      pathMap.get(timePeriod1).get(mode1).put(zone3XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone4XmlId,"");
      pathMap.get(timePeriod1).get(mode1).put(zone4XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone4XmlId,"");  
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, null, pathMap);           
      
      odMap.put(timePeriod1, new TreeMap<>());
      odMap.get(timePeriod1).put(mode1, new TreeMap<>());
      odMap.get(timePeriod1).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone3XmlId,Double.valueOf(77.0));
      odMap.get(timePeriod1).get(mode1).get(zone1XmlId).put(zone4XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).put(zone2XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone2XmlId).put(zone4XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).put(zone3XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone3XmlId).put(zone4XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).put(zone4XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get(zone4XmlId).put(zone4XmlId,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);

      TimePeriod timePeriod2 = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("2"));
      resultsMap.put(timePeriod2, new TreeMap<>());
      resultsMap.get(timePeriod2).put(mode1, new TreeMap<>());
      resultsMap.get(timePeriod2).get(mode1).put(node6XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put(node7XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put(node8XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put(node9XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put(node11XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put(node12XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put(node14XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put(node15XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put(node20XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).get(node6XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,2000, 10, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node7XmlId).put(node12XmlId, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,2000, 5, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node8XmlId).put(node7XmlId, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,2000, 12, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node9XmlId).put(node8XmlId, new LinkSegmentExpectedResultsDto(8, 9, 1, 20,2000, 20, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node11XmlId).put(node6XmlId, new LinkSegmentExpectedResultsDto(6, 11, 1,12, 2000, 12, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node12XmlId).put(node11XmlId, new LinkSegmentExpectedResultsDto(11, 12, 1,8, 2000, 8, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node14XmlId).put(node9XmlId, new LinkSegmentExpectedResultsDto(9, 14, 1,10, 2000, 10, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node15XmlId).put(node14XmlId, new LinkSegmentExpectedResultsDto(14, 15, 1,10, 2000, 10, 1));
      resultsMap.get(timePeriod2).get(mode1).get(node20XmlId).put(node15XmlId, new LinkSegmentExpectedResultsDto(15, 20, 1,21, 2000, 21, 1));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter,null, resultsMap);

      /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, path string */ 
      pathMap.put(timePeriod2, new TreeMap<>());
      pathMap.get(timePeriod2).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod2).get(mode1).put(zone1XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone4XmlId,"[1,6,11,12,7,8,9,14,15,20]");
      pathMap.get(timePeriod2).get(mode1).put(zone2XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone4XmlId,"");
      pathMap.get(timePeriod2).get(mode1).put(zone3XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone4XmlId,"");
      pathMap.get(timePeriod2).get(mode1).put(zone4XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone2XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone3XmlId,"");
      pathMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone4XmlId,""); 
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, null, pathMap);
             
      
      odMap.put(timePeriod2, new TreeMap<>());
      odMap.get(timePeriod2).put(mode1, new TreeMap<>());
      odMap.get(timePeriod2).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone1XmlId).put(zone4XmlId,Double.valueOf(108.0));
      odMap.get(timePeriod2).get(mode1).put(zone2XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone2XmlId).put(zone4XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).put(zone3XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone3XmlId).put(zone4XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).put(zone4XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone2XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone3XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get(zone4XmlId).put(zone4XmlId,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, null, odMap);

      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName3, xmlFileName3);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName3, xmlFileName3);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath,runIdDescription, csvFileName1, xmlFileName1);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName3, xmlFileName3);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
}