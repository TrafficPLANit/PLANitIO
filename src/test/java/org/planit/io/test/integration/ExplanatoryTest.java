package org.planit.io.test.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
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
import org.planit.utils.network.physical.Mode;

/**
 * JUnit test cases for explanatory tests for TraditionalStaticAssignment
 * 
 * @author gman6028, markr
 *
 */
public class ExplanatoryTest {

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
      LOGGER = Logging.createLogger(ExplanatoryTest.class);
    } 
  }

  @AfterClass
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }
 
  /**
   * Trivial test case which matches the description in the README.md file.
   */
  @Test
  public void test_explanatory_original() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\original";
      String description = "explanatory";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = null;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0,
          2000.0, 10.0, 1.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[1,2]");
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
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, Double.valueOf(10.0));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
 
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
  
  /**
   * Tests that rows with zero output are included when testZeroOutput is set to true
   */
  @Test
  public void test_explanatory_report_zero_outputs() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\reportZeroOutputs";
      String description = "explanatory";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = null;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, null, description, true, true);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0,
          2000.0, 10.0, 1.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[1,2]");
      pathMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,"[2,1]");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, Double.valueOf(10.0));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, Double.valueOf(10.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
 
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
  
  /**
   * Test case in which the time period external Id values do not match the internally generated Id values
   */
  @Test
  public void test_explanatory_time_period_external_id_test() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\timePeriodExternalIdTest";
      String description = "explanatory";
      String csvFileName = "Time_Period_2_2.csv";
      String odCsvFileName = "Time_Period_2_1.csv";
      String xmlFileName = "Time_Period_2.xml";
      Integer maxIterations = null;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 2);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0,
          2000.0, 10.0, 1.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[1,2]");
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
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, Double.valueOf(10.0));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
 
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
  
  /**
   * we have <userclass> but no <travellertype> included in input file.
   */
  @Test
  public void test_explanatory_traveller_type_ref_missing_from_user_class() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\travellerTypeMissingFromUserClass";
      String description = "explanatory";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = null;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0,
          2000.0, 10.0, 1.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[1,2]");
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
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, Double.valueOf(10.0));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
 
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      ex.printStackTrace();
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
    }
  }
  
  /**
   * <travellertype> is defined in the <demandconfiguration> but no <userclass> defined, this should throw an exception.
   */
  @Test
  public void test_explanatory_traveller_types_but_no_user_classes() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\travellerTypesButNoUserClasses";
      String description = "explanatory";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, null, description, true, false);
      LOGGER.setLevel(oldLevel);
      fail("Test should throw an exception due to no user class to reference traveller types but it did not.");
     } catch (final Exception e) {
      assertTrue(true);
    }
  }
  
  /**
   * More than one <travellertype> is defined in the <demandconfiguration> but <userclass> does not specify which it is using.
   */
  @Test
  public void test_explanatory_not_specified_which_traveller_type_being_used() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\notSpecifiedWhichTravellerTypeBeingUsed";
      String description = "explanatory";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, null, description, true, false);
      LOGGER.setLevel(oldLevel);
      fail("Test should throw an exception due to no user class to reference traveller types but it did not.");
     } catch (final Exception e) {
       assertTrue(true);
    }
  }
 
  /**
   * No <travellertype> defined but <userclass> refers to it, this should throw an exception.
   */
  @Test
  public void test_explanatory_reference_to_missing_traveller_type() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\referenceToMissingTravellerType";
      String description = "explanatory";

      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, null, description, true, false);
      LOGGER.setLevel(oldLevel);
      fail("Test should throw an exception due to reference to missing traveller type but it did not.");
     } catch (final Exception e) {
       assertTrue(true);
    }
  }

  /**
   * Test case which uses the defaults for connectiod length and SpeedConnectoidTravelTimeCost
   */
  @Test
  public void test_explanatory_no_geolocation_elements() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\noGeolocationElements";
      String description = "explanatory";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = null;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, null, description, false, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0,
          2000.0, 10.0, 1.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[1,2]");
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
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, Double.valueOf(10.0));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
 
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
  
  /**
   * Test case which uses the defaults for connectiod length and SpeedConnectoidTravelTimeCost
   */
  @Test
  public void test_explanatory_no_geolocation_elements_with_length_1() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\noGeolocationElementsWithLength1";
      String description = "explanatory";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = null;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, null, description, false, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0,
          2000.0, 10.0, 1.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[1,2]");
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
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, Double.valueOf(10.08));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
 
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * Test case which checks that an exception is thrown if two link segments in the same link are in the same direction.
   */
  @Test
  public void test_explanatory_link_segments_in_same_direction() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\linkSegmentsInSameDirection";
      String description = "explanatory";
      
      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);      
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, null, description, true, false);
      LOGGER.setLevel(oldLevel);
      fail("Exception for link segment in same direction was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
    }
  
  
  /**
   * Trivial test case which matches the description in the README.md file.
   */
  @Test
  public void test_explanatory_defaults() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\defaults";
      String description = "explanatory";
      String csvFileName = "_2.csv";
      String odCsvFileName = "_1.csv";
      String xmlFileName = ".xml";
      Integer maxIterations = null;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1, 0.125,
          1800.0, 10.0, 80.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[1,2]");
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
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, Double.valueOf(0.125));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
 
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * Test case in which an attempt is made to change a locked formatter
   */
  @Test
  public void test_explanatory_attempt_to_change_locked_formatter() {
    String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\original";
    String description = "explanatory";
    String csvFileName = "Time_Period_1_2.csv";
    String odCsvFileName = "Time_Period_1_1.csv";
    String xmlFileName = "Time_Period_1.xml";
    Integer maxIterations = null;
    try {
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignmentAttemptToChangeLockedFormatter(projectPath, null, description, true);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0,
          2000.0, 10.0, 1.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[1,2]");
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
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, Double.valueOf(10.0));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
      
      fail("testExplanatoryAttemptToChangeLockedFormatter() did not throw PlanItException when expected");
    } catch (Exception e) {
      LOGGER.info(e.getMessage());
    }

    try {
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      assertTrue(true);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
}