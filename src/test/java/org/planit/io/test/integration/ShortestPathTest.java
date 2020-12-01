package org.planit.io.test.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.input.InputBuilderListener;
import org.planit.utils.test.LinkSegmentExpectedResultsDto;
import org.planit.io.test.util.PlanItIOTestHelper;
import org.planit.utils.test.TestOutputDto;

import org.planit.logging.Logging;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.project.CustomPlanItProject;
import org.planit.time.TimePeriod;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.mode.Mode;

/**
 * JUnit test cases for shortest path (AON) tests for TraditionalStaticAssignment
 * 
 * @author gman6028, markr
 *
 */
public class ShortestPathTest {

  /** the logger */
  private static Logger LOGGER = null;

  /**
   * Run assertions which confirm that results files contain the correct data, and
   * then remove the results files
   * 
   * @param projectPath project directory containing the input files
   * @param description description used in temporary output file names
   * @param csvFileName name of CSV file containing run results
   * @param xmlFileName name of XML file containing run results
   * @throws Exception thrown if there is an error
   */
  private void runFileEqualAssertionsAndCleanUp(OutputType outputType, String projectPath, String description,
      String csvFileName, String xmlFileName) throws Exception {

    String fullCsvFileNameWithoutDescription = projectPath + "\\" + outputType.value() + "_" + csvFileName;
    String fullCsvFileNameWithDescription = projectPath + "\\" + outputType.value() + "_" + description + "_"
        + csvFileName;

    assertTrue(PlanItIOTestHelper.compareFiles(fullCsvFileNameWithoutDescription, fullCsvFileNameWithDescription));
    PlanItIOTestHelper.deleteFile(outputType, projectPath, description, csvFileName);

    String fullXmlFileNameWithoutDescription = projectPath + "\\" + outputType.value() + "_" + xmlFileName;
    String fullXmlFileNameWithDescription = projectPath + "\\" + outputType.value() + "_" + description + "_"
        + xmlFileName;
    assertTrue(PlanItIOTestHelper.isXmlFileSameExceptForTimestamp(fullXmlFileNameWithoutDescription,
        fullXmlFileNameWithDescription));
    PlanItIOTestHelper.deleteFile(outputType, projectPath, description, xmlFileName);
  }

  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(ShortestPathTest.class);
    } 
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
      String projectPath = "src\\test\\resources\\testcases\\basicShortestPathAlgorithm\\xml\\AtoB";
      String description = "testBasic1";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = null;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);     

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\basicShortestPathAlgorithm\\xml\\AtoB\\initial_link_segment_costs.csv", maxIterations, null,
          description, true, false);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath,runIdDescription, csvFileName, xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
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
   * of 85 (the fifth argument in the ResultDto constructor).
   */
  @Test
  public void test_basic_shortest_path_algorithm_a_to_b_two_initial_cost_files() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\basicShortestPathAlgorithm\\xml\\AtoB";
      String description = "testBasic1";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = null;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);       

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\basicShortestPathAlgorithm\\xml\\AtoB\\initial_link_segment_costs.csv",
          "src\\test\\resources\\testcases\\basicShortestPathAlgorithm\\xml\\AtoB\\initial_link_segment_costs1.csv", 0, maxIterations, null,
          description, true, false);
      
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
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
      String projectPath = "src\\test\\resources\\testcases\\basicShortestPathAlgorithm\\xml\\AtoCLinkSegmentMaximumSpeed";
      String description = "testBasic2";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = null;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);         

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      Mode mode1 = testOutputDto.getC().getModeByXmlId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByXmlId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 1, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1, 12,
          2000, 12, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 2000, 8, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 13, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 12, new LinkSegmentExpectedResultsDto(12, 13, 1,
          47, 2000, 47, 1));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[1,6,11,12,13]");
      pathMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, Double.valueOf(77.0));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
      
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
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
      String projectPath = "src\\test\\resources\\testcases\\basicShortestPathAlgorithm\\xml\\AtoC";
      String description = "testBasic2";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = null;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);        

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      Mode mode1 = testOutputDto.getC().getModeByXmlId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByXmlId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 1, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1, 12,
          2000, 12, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 2000, 8, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 13, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 12, new LinkSegmentExpectedResultsDto(12, 13, 1,
          47, 2000, 47, 1));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[1,6,11,12,13]");
      pathMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, Double.valueOf(77.0));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
      
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
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
      String projectPath = "src\\test\\resources\\testcases\\basicShortestPathAlgorithm\\xml\\AtoD";
      String description = "testBasic3";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = null;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);      

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      Mode mode1 = testOutputDto.getC().getModeByXmlId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByXmlId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 1, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 8, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 7, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,
          2000, 12, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 9, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 9).put((long) 8, new LinkSegmentExpectedResultsDto(8, 9, 1, 20,
          2000, 20, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1, 12,
          2000, 12, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 7, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 12, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,
          2000, 5, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 14, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 9, new LinkSegmentExpectedResultsDto(9, 14, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 2000, 8, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 15, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 14, new LinkSegmentExpectedResultsDto(14, 15, 1,
          10, 2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 20, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 20).put((long) 15, new LinkSegmentExpectedResultsDto(15, 20, 1,
          21, 2000, 21, 1));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[1,6,11,12,7,8,9,14,15,20]");
      pathMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, Double.valueOf(108.0));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
  
  @Test
  public void test_basic_shortest_path_algorithm_three_time_periods_record_zero_flow() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\basicShortestPathAlgorithm\\xml\\ThreeTimePeriodsRecordZeroFlow";
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
      Integer maxIterations = null;
      
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
      

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, null, description, true, true);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      Mode mode1 = testOutputDto.getC().getModeByXmlId((long) 1);

      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      TimePeriod timePeriod1 = testOutputDto.getC().getTimePeriodByXmlId((long) 0);
      resultsMap.put(timePeriod1, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod1).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 5, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 7, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 8, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 10, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 8, new LinkSegmentExpectedResultsDto(8, 3, 1, 8,
          2000, 8, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 5).put((long) 4, new LinkSegmentExpectedResultsDto(4, 5, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 6).put((long) 1, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 7).put((long) 12, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,
          2000, 5, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 8).put((long) 7, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,
          2000, 12, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 10).put((long) 5, new LinkSegmentExpectedResultsDto(5, 10, 1,
          10, 2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1,
          12, 2000, 12, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 2000, 8, 1));

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod1, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod1).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod1).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 2,"[1,6,11,12,7,8,3,4,5,10]");
      pathMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 3,"[1,6,11,12,13]");
      pathMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 4,"[1,6,11,12,7,8,9,14,15,20]");
      pathMap.get(timePeriod1).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 1,"[10,5,4,3,8,7,12,11,6,1]");
      pathMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 2,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 3,"[10,15,14,13]");
      pathMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 4,"[10,15,20]");
      pathMap.get(timePeriod1).get(mode1).put((long) 3, new TreeMap<Long, String>());
      pathMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 1,"[13,12,11,6,1]");
      pathMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 2,"[13,14,15,10]");
      pathMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 3,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 4,"[13,14,15,20]");
      pathMap.get(timePeriod1).get(mode1).put((long) 4, new TreeMap<Long, String>());
      pathMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 1,"[20,15,14,9,8,7,12,11,6,1]");
      pathMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 2,"[20,15,10]");
      pathMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 3,"[20,15,14,13]");
      pathMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 4,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod1, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod1).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod1).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 2,Double.valueOf(85.0));
      odMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 3,Double.valueOf(77.0));
      odMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 4,Double.valueOf(108.0));
      odMap.get(timePeriod1).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 1,Double.valueOf(85.0));
      odMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 3,Double.valueOf(18.0));
      odMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 4,Double.valueOf(24.0));
      odMap.get(timePeriod1).get(mode1).put((long) 3, new TreeMap<Long, Double>());
      odMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 1,Double.valueOf(77.0));
      odMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 2,Double.valueOf(18.0));
      odMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 4,Double.valueOf(36.0));
      odMap.get(timePeriod1).get(mode1).put((long) 4, new TreeMap<Long, Double>());
      odMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 1,Double.valueOf(108.0));
      odMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 2,Double.valueOf(24.0));
      odMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 3,Double.valueOf(36.0));
      odMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 4,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
 
      TimePeriod timePeriod2 = testOutputDto.getC().getTimePeriodByXmlId((long) 1);
      resultsMap.put(timePeriod2, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod2).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 13, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).get((long) 6).put((long) 1, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod2).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1,
          12, 2000, 12, 1));
      resultsMap.get(timePeriod2).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 2000, 8, 1));
      resultsMap.get(timePeriod2).get(mode1).get((long) 13).put((long) 12, new LinkSegmentExpectedResultsDto(12, 13, 1,
          47, 2000, 47, 1));

      pathMap =  new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod2, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod2).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod2).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 2,"[1,6,11,12,7,8,3,4,5,10]");
      pathMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 3,"[1,6,11,12,13]");
      pathMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 4,"[1,6,11,12,7,8,9,14,15,20]");
      pathMap.get(timePeriod2).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 1,"[10,5,4,3,8,7,12,11,6,1]");
      pathMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 2,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 3,"[10,15,14,13]");
      pathMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 4,"[10,15,20]");
      pathMap.get(timePeriod2).get(mode1).put((long) 3, new TreeMap<Long, String>());
      pathMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 1,"[13,12,11,6,1]");
      pathMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 2,"[13,14,15,10]");
      pathMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 3,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 4,"[13,14,15,20]");
      pathMap.get(timePeriod2).get(mode1).put((long) 4, new TreeMap<Long, String>());
      pathMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 1,"[20,15,14,9,8,7,12,11,6,1]");
      pathMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 2,"[20,15,10]");
      pathMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 3,"[20,15,14,13]");
      pathMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 4,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      odMap = new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod2, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod2).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod2).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 2,Double.valueOf(85.0));
      odMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 3,Double.valueOf(77.0));
      odMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 4,Double.valueOf(108.0));
      odMap.get(timePeriod2).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 1,Double.valueOf(85.0));
      odMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 3,Double.valueOf(18.0));
      odMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 4,Double.valueOf(24.0));
      odMap.get(timePeriod2).get(mode1).put((long) 3, new TreeMap<Long, Double>());
      odMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 1,Double.valueOf(77.0));
      odMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 2,Double.valueOf(18.0));
      odMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 4,Double.valueOf(36.0));
      odMap.get(timePeriod2).get(mode1).put((long) 4, new TreeMap<Long, Double>());
      odMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 1,Double.valueOf(108.0));
      odMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 2,Double.valueOf(24.0));
      odMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 3,Double.valueOf(36.0));
      odMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 4,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      TimePeriod timePeriod3 = testOutputDto.getC().getTimePeriodByXmlId((long) 2);
      resultsMap.put(timePeriod3, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod3).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 7, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 8, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 9, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 14, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 15, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 20, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).get((long) 6).put((long) 1, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 7).put((long) 12, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,
          2000, 5, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 8).put((long) 7, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,
          2000, 12, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 9).put((long) 8, new LinkSegmentExpectedResultsDto(8, 9, 1, 20,
          2000, 20, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1,
          12, 2000, 12, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 2000, 8, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 14).put((long) 9, new LinkSegmentExpectedResultsDto(9, 14, 1,
          10, 2000, 10, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 15).put((long) 14, new LinkSegmentExpectedResultsDto(14, 15, 1,
          10, 2000, 10, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 20).put((long) 15, new LinkSegmentExpectedResultsDto(15, 20, 1,
          21, 2000, 21, 1));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      pathMap =  new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod3, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod3).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod3).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 2,"[1,6,11,12,7,8,3,4,5,10]");
      pathMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 3,"[1,6,11,12,13]");
      pathMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 4,"[1,6,11,12,7,8,9,14,15,20]");
      pathMap.get(timePeriod3).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 1,"[10,5,4,3,8,7,12,11,6,1]");
      pathMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 2,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 3,"[10,15,14,13]");
      pathMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 4,"[10,15,20]");
      pathMap.get(timePeriod3).get(mode1).put((long) 3, new TreeMap<Long, String>());
      pathMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 1,"[13,12,11,6,1]");
      pathMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 2,"[13,14,15,10]");
      pathMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 3,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 4,"[13,14,15,20]");
      pathMap.get(timePeriod3).get(mode1).put((long) 4, new TreeMap<Long, String>());
      pathMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 1,"[20,15,14,9,8,7,12,11,6,1]");
      pathMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 2,"[20,15,10]");
      pathMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 3,"[20,15,14,13]");
      pathMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 4,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      odMap = new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod3, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod3).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod3).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 2,Double.valueOf(85.0));
      odMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 3,Double.valueOf(77.0));
      odMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 4,Double.valueOf(108.0));
      odMap.get(timePeriod3).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 1,Double.valueOf(85.0));
      odMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 3,Double.valueOf(18.0));
      odMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 4,Double.valueOf(24.0));
      odMap.get(timePeriod3).get(mode1).put((long) 3, new TreeMap<Long, Double>());
      odMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 1,Double.valueOf(77.0));
      odMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 2,Double.valueOf(18.0));
      odMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 4,Double.valueOf(36.0));
      odMap.get(timePeriod3).get(mode1).put((long) 4, new TreeMap<Long, Double>());
      odMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 1,Double.valueOf(108.0));
      odMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 2,Double.valueOf(24.0));
      odMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 3,Double.valueOf(36.0));
      odMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 4,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName1, xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName3, xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName1, xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName2, xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName3, xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName1, xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName3, xmlFileName3);
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
      String projectPath = "src\\test\\resources\\testcases\\basicShortestPathAlgorithm\\xml\\ThreeTimePeriods";
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
      Integer maxIterations = null;
      
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

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      Mode mode1 = testOutputDto.getC().getModeByXmlId((long) 1);

      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      TimePeriod timePeriod1 = testOutputDto.getC().getTimePeriodByXmlId((long) 0);
      resultsMap.put(timePeriod1, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod1).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 5, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 7, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 8, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 10, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 8, new LinkSegmentExpectedResultsDto(8, 3, 1, 8,
          2000, 8, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 5).put((long) 4, new LinkSegmentExpectedResultsDto(4, 5, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 6).put((long) 1, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 7).put((long) 12, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,
          2000, 5, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 8).put((long) 7, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,
          2000, 12, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 10).put((long) 5, new LinkSegmentExpectedResultsDto(5, 10, 1,
          10, 2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1,
          12, 2000, 12, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 2000, 8, 1));

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod1, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod1).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod1).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 2,"[1,6,11,12,7,8,3,4,5,10]");
      pathMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 3,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 4,"");
      pathMap.get(timePeriod1).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 1,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 2,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 3,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 4,"");
      pathMap.get(timePeriod1).get(mode1).put((long) 3, new TreeMap<Long, String>());
      pathMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 1,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 2,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 3,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 4,"");
      pathMap.get(timePeriod1).get(mode1).put((long) 4, new TreeMap<Long, String>());
      pathMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 1,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 2,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 3,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 4,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod1, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod1).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod1).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 2,Double.valueOf(85.0));
      odMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 4,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 4,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).put((long) 3, new TreeMap<Long, Double>());
      odMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 4,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).put((long) 4, new TreeMap<Long, Double>());
      odMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 4,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
 
      TimePeriod timePeriod2 = testOutputDto.getC().getTimePeriodByXmlId((long) 1);
      resultsMap.put(timePeriod2, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod2).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 13, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).get((long) 6).put((long) 1, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod2).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1,
          12, 2000, 12, 1));
      resultsMap.get(timePeriod2).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 2000, 8, 1));
      resultsMap.get(timePeriod2).get(mode1).get((long) 13).put((long) 12, new LinkSegmentExpectedResultsDto(12, 13, 1,
          47, 2000, 47, 1));

      pathMap =  new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod2, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod2).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod2).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 2,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 3,"[1,6,11,12,13]");
      pathMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 4,"");
      pathMap.get(timePeriod2).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 1,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 2,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 3,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 4,"");
      pathMap.get(timePeriod2).get(mode1).put((long) 3, new TreeMap<Long, String>());
      pathMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 1,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 2,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 3,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 4,"");
      pathMap.get(timePeriod2).get(mode1).put((long) 4, new TreeMap<Long, String>());
      pathMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 1,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 2,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 3,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 4,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      odMap = new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod2, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod2).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod2).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 3,Double.valueOf(77.0));
      odMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 4,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 4,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).put((long) 3, new TreeMap<Long, Double>());
      odMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 4,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).put((long) 4, new TreeMap<Long, Double>());
      odMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 4,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      TimePeriod timePeriod3 = testOutputDto.getC().getTimePeriodByXmlId((long) 2);
      resultsMap.put(timePeriod3, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod3).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 7, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 8, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 9, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 14, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 15, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).put((long) 20, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod3).get(mode1).get((long) 6).put((long) 1, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,
          2000, 10, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 7).put((long) 12, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,
          2000, 5, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 8).put((long) 7, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,
          2000, 12, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 9).put((long) 8, new LinkSegmentExpectedResultsDto(8, 9, 1, 20,
          2000, 20, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1,
          12, 2000, 12, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 2000, 8, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 14).put((long) 9, new LinkSegmentExpectedResultsDto(9, 14, 1,
          10, 2000, 10, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 15).put((long) 14, new LinkSegmentExpectedResultsDto(14, 15, 1,
          10, 2000, 10, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 20).put((long) 15, new LinkSegmentExpectedResultsDto(15, 20, 1,
          21, 2000, 21, 1));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      pathMap =  new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod3, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod3).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod3).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 2,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 3,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 4,"[1,6,11,12,7,8,9,14,15,20]");
      pathMap.get(timePeriod3).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 1,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 2,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 3,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 4,"");
      pathMap.get(timePeriod3).get(mode1).put((long) 3, new TreeMap<Long, String>());
      pathMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 1,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 2,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 3,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 4,"");
      pathMap.get(timePeriod3).get(mode1).put((long) 4, new TreeMap<Long, String>());
      pathMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 1,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 2,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 3,"");
      pathMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 4,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      odMap = new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod3, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod3).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod3).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 1).put((long) 4,Double.valueOf(108.0));
      odMap.get(timePeriod3).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 2).put((long) 4,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).put((long) 3, new TreeMap<Long, Double>());
      odMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 3).put((long) 4,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).put((long) 4, new TreeMap<Long, Double>());
      odMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod3).get(mode1).get((long) 4).put((long) 4,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName1, xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName3, xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName1, xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName2, xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName3, xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath,runIdDescription, csvFileName1, xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName2, xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName3, xmlFileName3);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
}