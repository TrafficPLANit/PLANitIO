package org.planit.io.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.utils.test.LinkSegmentExpectedResultsDto;
import org.planit.io.test.util.PlanItIOTestHelper;
import org.planit.utils.test.TestOutputDto;
import org.planit.logging.Logging;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.property.DownstreamNodeExternalIdOutputProperty;
import org.planit.output.property.LinkCostOutputProperty;
import org.planit.output.property.LinkSegmentExternalIdOutputProperty;
import org.planit.output.property.ModeExternalIdOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.UpstreamNodeExternalIdOutputProperty;
import org.planit.project.CustomPlanItProject;
import org.planit.time.TimePeriod;
import org.planit.utils.functionalinterface.TriConsumer;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;

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
public class PlanItIOIntegrationTest {

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
      LOGGER = Logging.createLogger(PlanItIOIntegrationTest.class);
    }
  }

  @AfterClass
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
  }
  
  /**
   * Test case which indicates the effects of changing BPR parameters when flows are close to lane capacity.
   */
  @Test
  public void test_bpr_parameters_test() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\bpr_parameters_test\\xml\\simple";
      String description = "mode_test";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 2;

      TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters = (physicalNetwork,
          bprLinkTravelTimeCost, inputBuilderListener) -> {
        MacroscopicLinkSegmentType macroscopiclinkSegmentType = inputBuilderListener.getLinkSegmentTypeByExternalId(
            (long) 1);
        Mode mode = inputBuilderListener.getModeByExternalId((long) 1);
        bprLinkTravelTimeCost.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
        MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) inputBuilderListener.getLinkSegmentByExternalId((long) 3);
        bprLinkTravelTimeCost.setParameters(linkSegment, mode, 1.0, 5.0);
      };

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, setCostParameters, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 2000, 19.1019336, 1000.0, 10.0, 0.5235072));
      resultsMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2, new LinkSegmentExpectedResultsDto(2, 3, 2000, 4.5, 1000.0, 10.0, 2.2222222));
      resultsMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 2000, 33.0, 1000.0, 10.0, 0.3030303));
      resultsMap.get(timePeriod).get(mode1).put((long) 5, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 4, new LinkSegmentExpectedResultsDto(4, 5, 2000, 4.5, 1000.0, 10.0, 2.2222222));
      resultsMap.get(timePeriod).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 5, new LinkSegmentExpectedResultsDto(5, 6, 2000, 19.1019336, 1000.0, 10.0, 0.5235072));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[1,2,3,4,5,6]");
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
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, Double.valueOf(80.2038651));
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
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
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

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, null, description, true, false);
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

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, null, description, true, false);
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

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, null, description, true, false);
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
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, null, description, true, false);
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
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
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

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
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

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
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

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
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

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
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

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
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

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
      fail("Exception for duplicate user class external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Tests that the values of an initial costs file are read in by start and end
   * node and registered by PlanItProject and the stored values match the expected
   * ones by link external Id
   */
  @Test
  public void test_reading_initial_cost_values() {
    String projectPath = "src\\test\\resources\\testcases\\initial_costs\\xml\\readingInitialCostValues";
    String initialCostsFileLocation =
        "src\\test\\resources\\testcases\\initial_costs\\xml\\readingInitialCostValues\\initial_link_segment_costs.csv";
    String initialCostsFileLocationExternalId =
        "src\\test\\resources\\testcases\\initial_costs\\xml\\readingInitialCostValues\\initial_link_segment_costs_external_id.csv";
    try {
      IdGenerator.reset();
      PlanItInputBuilder planItInputBuilder = new PlanItInputBuilder(projectPath);
      final CustomPlanItProject project = new CustomPlanItProject(planItInputBuilder);
      PhysicalNetwork physicalNetwork =
          project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());

      InitialLinkSegmentCost initialCost =
         project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCostsFileLocation);
      Reader in = new FileReader(initialCostsFileLocationExternalId);
      CSVParser parser = CSVParser.parse(in, CSVFormat.DEFAULT.withFirstRecordAsHeader());
      String modeHeader = ModeExternalIdOutputProperty.NAME;
      String linkSegmentExternalIdHeader = LinkSegmentExternalIdOutputProperty.NAME;
      String costHeader = LinkCostOutputProperty.NAME;
      for (CSVRecord record : parser) {
        long modeExternalId = Long.parseLong(record.get(modeHeader));
        Mode mode = planItInputBuilder.getModeByExternalId(modeExternalId);
        double cost = Double.parseDouble(record.get(costHeader));
        long linkSegmentExternalId = Long.parseLong(record.get(linkSegmentExternalIdHeader));
        LinkSegment linkSegment = planItInputBuilder.getLinkSegmentByExternalId(linkSegmentExternalId);
        assertEquals(cost, initialCost.getSegmentCost(mode, linkSegment), 0.0001);
      }
      in.close();
    } catch (final Exception e) {
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * Test that the read in initial cost values match the expected ones when there
   * are some rows missing in the standard results file
   */
  @Test
  public void test_reading_initial_cost_values_with_missing_rows() {
    String projectPath = "src\\test\\resources\\testcases\\initial_costs\\xml\\readingInitialCostValues";
    String initialCostsFileLocation =
        "src\\test\\resources\\testcases\\initial_costs\\xml\\readingInitialCostValues\\initial_link_segment_costs.csv";
    String initialCostsFileLocationMissingRows =
        "src\\test\\resources\\testcases\\initial_costs\\xml\\readingInitialCostValues\\initial_link_segment_costs1.csv";
    try {
      IdGenerator.reset();
      PlanItInputBuilder planItInputBuilder = new PlanItInputBuilder(projectPath);
      final CustomPlanItProject project = new CustomPlanItProject(planItInputBuilder);

      PhysicalNetwork physicalNetwork =
          project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());

      InitialLinkSegmentCost initialCost =
          project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCostsFileLocation);
      Reader in = new FileReader(initialCostsFileLocationMissingRows);
      CSVParser parser = CSVParser.parse(in, CSVFormat.DEFAULT.withFirstRecordAsHeader());
      String modeHeader = ModeExternalIdOutputProperty.NAME;
      String upstreamNodeExternalIdHeader = UpstreamNodeExternalIdOutputProperty.NAME;
      String downstreamNodeExternalIdHeader = DownstreamNodeExternalIdOutputProperty.NAME;
      String costHeader = LinkCostOutputProperty.NAME;
      for (CSVRecord record : parser) {
        long modeExternalId = Long.parseLong(record.get(modeHeader));
        Mode mode = planItInputBuilder.getModeByExternalId(modeExternalId);
        double cost = Double.parseDouble(record.get(costHeader));
        long upstreamNodeExternalId = Long.parseLong(record.get(upstreamNodeExternalIdHeader));
        long downstreamNodeExternalId = Long.parseLong(record.get(downstreamNodeExternalIdHeader));
        final long startId = planItInputBuilder.getNodeByExternalId(upstreamNodeExternalId).getId();
        final long endId = planItInputBuilder.getNodeByExternalId(downstreamNodeExternalId).getId();
        final LinkSegment linkSegment = physicalNetwork.linkSegments.getLinkSegmentByStartAndEndNodeId(startId, endId);
        assertEquals(cost, initialCost.getSegmentCost(mode, linkSegment), 0.0001);
      }
      in.close();
    } catch (final Exception e) {
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * Test that PlanItProject throws an exception when the input XML is missing some link segments which are included in the initial costs file
   */
  @Test
  public void test_reading_initial_cost_values_with_missing_rows_in_input_file() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\initial_costs\\xml\\readingInitialCostValuesWithLinkSegmentsMissingInInputFile";
      String description = "readingInitialCostValuesWithMissingRows";
      Integer maxIterations = null;
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\initial_costs\\xml\\readingInitialCostValuesWithLinkSegmentsMissingInInputFile\\initial_link_segment_costs_external_id.csv",
          maxIterations, null, description, true, false);
      fail(
          "RunTest did not throw an exception when it should have (missing data in the input XML file in the link definition section).");
    } catch (Exception e) {
      assertTrue(true);
    }
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

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\basicShortestPathAlgorithm\\xml\\AtoB\\initial_link_segment_costs.csv", maxIterations, null,
          description, true, false);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\basicShortestPathAlgorithm\\xml\\AtoB\\initial_link_segment_costs.csv",
          "src\\test\\resources\\testcases\\basicShortestPathAlgorithm\\xml\\AtoB\\initial_link_segment_costs1.csv", 0, maxIterations, null,
          description, true, false);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
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
      
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
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
      
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, null, description, true, true);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);

      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      TimePeriod timePeriod1 = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
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
 
      TimePeriod timePeriod2 = testOutputDto.getC().getTimePeriodByExternalId((long) 1);
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

      TimePeriod timePeriod3 = testOutputDto.getC().getTimePeriodByExternalId((long) 2);
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName3,
          xmlFileName3);
    } catch (final Exception e) {
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

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);

      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      TimePeriod timePeriod1 = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
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
 
      TimePeriod timePeriod2 = testOutputDto.getC().getTimePeriodByExternalId((long) 1);
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

      TimePeriod timePeriod3 = testOutputDto.getC().getTimePeriodByExternalId((long) 2);
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName3,
          xmlFileName3);
    } catch (final Exception e) {
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the first route choice example from the Traditional Static Assignment Route
   * Choice Equilibration Test cases.docx document.
   */
  @Test
  public void test_1_no_route_choice_single_mode() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\noRouteChoiceSingleMode";
      String description = "testRouteChoice1";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 5, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 14, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 16, new TreeMap<Long, LinkSegmentExpectedResultsDto>());

      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, new LinkSegmentExpectedResultsDto(2, 1, 2000,
          0.015, 2000, 1, 66.6666667));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 5, new LinkSegmentExpectedResultsDto(5, 1, 1000,
          0.0103125, 2000, 1, 96.969697));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3, new LinkSegmentExpectedResultsDto(3, 2, 2000,
          0.015, 2000, 1, 66.6666667));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 4, new LinkSegmentExpectedResultsDto(4, 2, 1000,
          0.0103125, 2000, 1, 96.969697));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1, new LinkSegmentExpectedResultsDto(1, 3, 2000,
          0.09, 1000, 1, 11.1111111));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 6, new LinkSegmentExpectedResultsDto(6, 3, 1000,
          0.0103125, 2000, 1, 96.969697));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 13, new LinkSegmentExpectedResultsDto(13, 4, 1000,
          0.01, 20000, 1, 99.9996875));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 15, new LinkSegmentExpectedResultsDto(15, 5, 1000,
          0.01, 20000, 1, 99.9996875));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 12, new LinkSegmentExpectedResultsDto(12, 6, 1000,
          0.01, 20000, 1, 99.9996875));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 2, new LinkSegmentExpectedResultsDto(2, 11, 1000,
          0.0103125, 2000, 1, 96.969697));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 3, new LinkSegmentExpectedResultsDto(3, 14, 1000,
          0.0103125, 2000, 1, 96.969697));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 1, new LinkSegmentExpectedResultsDto(1, 16, 1000,
          0.0103125, 2000, 1, 96.969697));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 3,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 5,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 6,"");
      pathMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,"");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3,"");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 4,"");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 5,"");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 6,"[12,6,3,2,1,16]");
      pathMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2,"");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 3,"");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4,"[13,4,2,1,3,14]");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 5,"");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 6,"");
      pathMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 2,"");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3,"");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 4,"");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 5,"");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 6,"");
      pathMap.get(timePeriod).get(mode1).put((long) 5, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 5).put((long) 1,"[15,5,1,3,2,11]");
      pathMap.get(timePeriod).get(mode1).get((long) 5).put((long) 2,"");
      pathMap.get(timePeriod).get(mode1).get((long) 5).put((long) 3,"");
      pathMap.get(timePeriod).get(mode1).get((long) 5).put((long) 4,"");
      pathMap.get(timePeriod).get(mode1).get((long) 5).put((long) 5,"");
      pathMap.get(timePeriod).get(mode1).get((long) 5).put((long) 6,"");
      pathMap.get(timePeriod).get(mode1).put((long) 6, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 6).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 6).put((long) 2,"");
      pathMap.get(timePeriod).get(mode1).get((long) 6).put((long) 3,"");
      pathMap.get(timePeriod).get(mode1).get((long) 6).put((long) 4,"");
      pathMap.get(timePeriod).get(mode1).get((long) 6).put((long) 5,"");
      pathMap.get(timePeriod).get(mode1).get((long) 6).put((long) 6,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 5,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 6,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 4,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 5,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 6,Double.valueOf(0.060625));
      odMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4,Double.valueOf(0.135625));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 5,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 6,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 4,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 5,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 6,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).put((long) 5, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 5).put((long) 1,Double.valueOf(0.135625));
      odMap.get(timePeriod).get(mode1).get((long) 5).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 5).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 5).put((long) 4,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 5).put((long) 5,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 5).put((long) 6,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).put((long) 6, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 6).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 6).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 6).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 6).put((long) 4,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 6).put((long) 5,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 6).put((long) 6,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
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
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceSingleMode";
      String description = "testRouteChoice2";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());

      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 11, new LinkSegmentExpectedResultsDto(11, 1, 3600,
          0.025, 3600, 1, 40));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 295.2,
          0.0333944, 1200, 2, 59.8903352));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1, new LinkSegmentExpectedResultsDto(1, 3, 1425.6,
          0.0332658, 1200, 1, 30.0609344));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1879.2,
          0.0667837, 1200, 1, 14.9737025));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 2, new LinkSegmentExpectedResultsDto(2, 4, 295.2,
          0.0333944, 1200, 2, 59.8903352));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 1425.6,
          0.0332658, 1200, 1, 30.0609344));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 4, new LinkSegmentExpectedResultsDto(4, 12, 3600,
          0.025, 3600, 1, 40));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[11,1,4,12]");
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
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,Double.valueOf(0.1164169));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
      
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceSingleModeInitialCostsOneIteration";
      String description = "testRouteChoice2initialCosts";
      String csvFileName = "Time_Period_1_1.csv";
      String odCsvFileName = "Time_Period_1_0.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 1;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceSingleModeInitialCostsOneIteration\\initial_link_segment_costs.csv",
          null, 0, maxIterations, 0.0, null, description, true, false);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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
      String projectPath =
          "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceInitialCostsOneIterationThreeTimePeriods";
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
      Map<Long, String> initialLinkSegmentLocationsPerTimePeriod = new HashMap<Long, String>();
      initialLinkSegmentLocationsPerTimePeriod.put((long) 0,
          "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceInitialCostsOneIterationThreeTimePeriods\\initial_link_segment_costs_time_period_1.csv");
      initialLinkSegmentLocationsPerTimePeriod.put((long) 1,
          "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceInitialCostsOneIterationThreeTimePeriods\\initial_link_segment_costs_time_period_2.csv");
      initialLinkSegmentLocationsPerTimePeriod.put((long) 2,
          "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceInitialCostsOneIterationThreeTimePeriods\\initial_link_segment_costs_time_period_3.csv");

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, initialLinkSegmentLocationsPerTimePeriod, maxIterations,
          0.0, null, description, true, false);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName3,
          xmlFileName3);
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
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceInitialCostsOneIterationExternalIds";
      String description = "testRouteChoice2initialCosts";
      String csvFileName = "Time_Period_1_1.csv";
      String odCsvFileName = "Time_Period_1_0.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 1;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceInitialCostsOneIterationExternalIds\\initial_link_segment_costs.csv",
          null, 0, maxIterations, 0.0, null, description, true, false);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceSingleModeWithInitialCosts500Iterations";
      String description = "testRouteChoice2initialCosts";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceSingleModeWithInitialCosts500Iterations\\initial_link_segment_costs.csv",
          null, 0, maxIterations, 0.0, null, description, true, false);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
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
      String projectPath =
          "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceSingleModeWithInitialCosts500IterationsThreeTimePeriods";
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
      Map<Long, String> initialLinkSegmentLocationsPerTimePeriod = new HashMap<Long, String>();
      initialLinkSegmentLocationsPerTimePeriod.put((long) 0,
          "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceSingleModeWithInitialCosts500IterationsThreeTimePeriods\\initial_link_segment_costs_time_period_1.csv");
      initialLinkSegmentLocationsPerTimePeriod.put((long) 1,
          "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceSingleModeWithInitialCosts500IterationsThreeTimePeriods\\initial_link_segment_costs_time_period_2.csv");
      initialLinkSegmentLocationsPerTimePeriod.put((long) 2,
          "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceSingleModeWithInitialCosts500IterationsThreeTimePeriods\\initial_link_segment_costs_time_period_3.csv");

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, initialLinkSegmentLocationsPerTimePeriod, maxIterations,
          0.0, null, description, true, false);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName3,
          xmlFileName3);
    } catch (final Exception e) {
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
      String projectPath =
          "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceSingleModeWithInitialCosts500IterationsLinkSegmentExternalIds";
      String description = "testRouteChoice2initialCosts";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceSingleModeWithInitialCosts500IterationsLinkSegmentExternalIds\\initial_link_segment_costs.csv",
          null, 0, maxIterations, 0.0, null, description, true, false);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
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
  public void test_3_SIMO_MIMO_MISO_route_choice_single_mode() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\MIMOrouteChoiceSingleMode";
      String description = "testRouteChoice3";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 5, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());

      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 11, new LinkSegmentExpectedResultsDto(11, 1, 8000,
          0.03, 8000, 2, 66.6666667));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 3952,
          0.0239029, 5000, 2, 83.6718462));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1, new LinkSegmentExpectedResultsDto(1, 3, 4048,
          0.0531495, 3000, 2, 37.6297041));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2, new LinkSegmentExpectedResultsDto(2, 3, 3952,
          0.0295286, 4000, 2, 67.7310119));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 3856,
          0.0472937, 3000, 2, 42.2888931));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 3, new LinkSegmentExpectedResultsDto(3, 5, 4144,
          0.2043143, 2000, 2, 9.7888406));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 4, new LinkSegmentExpectedResultsDto(4, 5, 3856,
          0.1581746, 2000, 2, 12.6442576));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 5, new LinkSegmentExpectedResultsDto(5, 12, 8000,
          2.58, 2000, 2, 0.7751938));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[11,1,2,3,4,5,12]");
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
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,Double.valueOf(2.8673689));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\biDirectionalLinksRouteChoiceSingleMode";
      String description = "testRouteChoice4";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 5, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 7, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 8, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 9, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 10, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 13, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 14, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 15, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 16, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 21, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 22, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 23, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 24, new TreeMap<Long, LinkSegmentExpectedResultsDto>());

      resultsMap.get(timePeriod).get(mode1).get((long) 9).put((long) 12, new LinkSegmentExpectedResultsDto(12, 9, 0.6,
          0.029, 1500, 2.9, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 9, new LinkSegmentExpectedResultsDto(9, 12, 0.6,
          0.029,1500, 2.9, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 12, new LinkSegmentExpectedResultsDto(12, 11,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12,
          482.4, 0.0301605,  1500, 3, 99.4679928));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 12, new LinkSegmentExpectedResultsDto(12, 16, 483,
          0.0100538, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 16, new LinkSegmentExpectedResultsDto(16, 12, 483,
          0.0100538, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 9, new LinkSegmentExpectedResultsDto(9, 13, 0.6,
          0.01, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 9).put((long) 13, new LinkSegmentExpectedResultsDto(13, 9, 0.6,
          0.01, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 10, new LinkSegmentExpectedResultsDto(10, 11,
          17.6, 0.03, 1500, 3, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 10).put((long) 11, new LinkSegmentExpectedResultsDto(11, 10,
          17.6, 0.03, 1500, 3, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 10, new LinkSegmentExpectedResultsDto(10, 14,
          17.6, 0.01, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 10).put((long) 14, new LinkSegmentExpectedResultsDto(14, 10,
          17.6, 0.01, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 11, new LinkSegmentExpectedResultsDto(11, 15, 500,
          0.0100617, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 15, new LinkSegmentExpectedResultsDto(15, 11, 500,
          0.0100617, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 1, new LinkSegmentExpectedResultsDto(1, 5, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 5, new LinkSegmentExpectedResultsDto(5, 1, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4, new LinkSegmentExpectedResultsDto(4, 1, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, new LinkSegmentExpectedResultsDto(2, 1, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 2, new LinkSegmentExpectedResultsDto(2, 6, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 6, new LinkSegmentExpectedResultsDto(6, 2, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2, new LinkSegmentExpectedResultsDto(2, 3, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3, new LinkSegmentExpectedResultsDto(3, 2, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 3, new LinkSegmentExpectedResultsDto(3, 7, 1900,
          0.0228712, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 7, new LinkSegmentExpectedResultsDto(7, 3, 1900,
          0.0228712, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4, new LinkSegmentExpectedResultsDto(4, 3, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 4, new LinkSegmentExpectedResultsDto(4, 8, 1617,
          0.0167522, 1500, 1, 59.693666));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 8, new LinkSegmentExpectedResultsDto(8, 4, 1617,
          0.0167522, 1500, 1, 59.693666));
      resultsMap.get(timePeriod).get(mode1).get((long) 23).put((long) 16, new LinkSegmentExpectedResultsDto(16, 23, 483,
          0.0200001, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 23, new LinkSegmentExpectedResultsDto(23, 16, 483,
          0.0200001, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 23, new LinkSegmentExpectedResultsDto(23, 8, 1617,
          0.0200068, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod).get(mode1).get((long) 23).put((long) 8, new LinkSegmentExpectedResultsDto(8, 23, 1617,
          0.0200068, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod).get(mode1).get((long) 21).put((long) 13, new LinkSegmentExpectedResultsDto(13, 21, 0.6,
          0.02, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 21, new LinkSegmentExpectedResultsDto(21, 13, 0.6,
          0.02, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 21, new LinkSegmentExpectedResultsDto(21, 5, 899.4,
          0.0200007, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod).get(mode1).get((long) 21).put((long) 5, new LinkSegmentExpectedResultsDto(5, 21, 899.4,
          0.0200007, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod).get(mode1).get((long) 22).put((long) 14, new LinkSegmentExpectedResultsDto(14, 22,
          17.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 22, new LinkSegmentExpectedResultsDto(22, 14,
          17.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 22, new LinkSegmentExpectedResultsDto(22, 6,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      resultsMap.get(timePeriod).get(mode1).get((long) 22).put((long) 6, new LinkSegmentExpectedResultsDto(6, 22,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      resultsMap.get(timePeriod).get(mode1).get((long) 24).put((long) 15, new LinkSegmentExpectedResultsDto(15, 24, 500,
          0.0200001, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 24, new LinkSegmentExpectedResultsDto(24, 15, 500,
          0.0200001, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 24, new LinkSegmentExpectedResultsDto(24, 7, 1900,
          0.020013, 10000, 1, 49.967441));
      resultsMap.get(timePeriod).get(mode1).get((long) 24).put((long) 7, new LinkSegmentExpectedResultsDto(7, 24, 1900,
          0.020013, 10000, 1, 49.967441));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[21,5,1,2,6,22]");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 3,"[21,5,1,4,8,23]");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4,"[21,5,1,4,3,7,24]");
      pathMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,"[22,6,2,1,5,21]");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,"");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3,"[22,6,2,1,4,8,23]");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 4,"[22,6,2,3,7,24]");
      pathMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1,"[23,8,4,1,5,21]");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2,"[23,8,4,1,2,6,22]");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 3,"");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4,"[23,8,4,3,7,24]");
      pathMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1,"[24,7,3,4,1,5,21]");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 2,"[24,7,3,2,6,22]");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3,"[24,7,3,4,8,23]");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 4,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,Double.valueOf(0.0767791));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 3,Double.valueOf(0.0776307));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4,Double.valueOf(0.0944051));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,Double.valueOf(0.0767791));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3,Double.valueOf(0.0931159));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 4,Double.valueOf(0.0900226));
      odMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1,Double.valueOf(0.0776307));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2,Double.valueOf(0.0931159));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4,Double.valueOf(0.0902602));
      odMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1,Double.valueOf(0.0944051));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 2,Double.valueOf(0.0900226));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3,Double.valueOf(0.0902602));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 4,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\biDirectionalLinksRouteChoiceSingleModeWithTwoTimePeriods";
      String description = "testRouteChoice42";
      String csvFileName1 = "Time_Period_1_500.csv";
      String odCsvFileName1 = "Time_Period_1_499.csv";
      String csvFileName2 = "Time_Period_2_500.csv";
      String odCsvFileName2 = "Time_Period_2_499.csv";
      String xmlFileName1 = "Time_Period_1.xml";
      String xmlFileName2 = "Time_Period_2.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod1 = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod1, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod1).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 5, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 7, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 8, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 9, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 10, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 13, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 14, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 15, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 16, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 21, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 22, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 23, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod1).get(mode1).put((long) 24, new TreeMap<Long, LinkSegmentExpectedResultsDto>());

      resultsMap.get(timePeriod1).get(mode1).get((long) 9).put((long) 12, new LinkSegmentExpectedResultsDto(12, 9, 0.6,
          0.029, 1500, 2.9, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 12).put((long) 9, new LinkSegmentExpectedResultsDto(9, 12, 0.6,
          0.029, 1500, 2.9, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 11).put((long) 12, new LinkSegmentExpectedResultsDto(12, 11,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod1).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod1).get(mode1).get((long) 16).put((long) 12, new LinkSegmentExpectedResultsDto(12, 16,
          483, 0.0100538, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod1).get(mode1).get((long) 12).put((long) 16, new LinkSegmentExpectedResultsDto(16, 12,
          483, 0.0100538, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod1).get(mode1).get((long) 13).put((long) 9, new LinkSegmentExpectedResultsDto(9, 13, 0.6,
          0.01, 1500, 1, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 9).put((long) 13, new LinkSegmentExpectedResultsDto(13, 9, 0.6,
          0.01, 1500, 1, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 11).put((long) 10, new LinkSegmentExpectedResultsDto(10, 11,
          17.6, 0.03, 1500, 3, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 10).put((long) 11, new LinkSegmentExpectedResultsDto(11, 10,
          17.6, 0.03, 1500, 3, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 14).put((long) 10, new LinkSegmentExpectedResultsDto(10, 14,
          17.6, 0.01, 1500, 1, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 10).put((long) 14, new LinkSegmentExpectedResultsDto(14, 10,
          17.6, 0.01, 1500, 1, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 15).put((long) 11, new LinkSegmentExpectedResultsDto(11, 15,
          500, 0.0100617, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod1).get(mode1).get((long) 11).put((long) 15, new LinkSegmentExpectedResultsDto(15, 11,
          500, 0.0100617, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod1).get(mode1).get((long) 5).put((long) 1, new LinkSegmentExpectedResultsDto(1, 5, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 5, new LinkSegmentExpectedResultsDto(5, 1, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 4, new LinkSegmentExpectedResultsDto(4, 1, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 2, new LinkSegmentExpectedResultsDto(2, 1, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod1).get(mode1).get((long) 6).put((long) 2, new LinkSegmentExpectedResultsDto(2, 6, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      resultsMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 6, new LinkSegmentExpectedResultsDto(6, 2, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      resultsMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 2, new LinkSegmentExpectedResultsDto(2, 3, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 3, new LinkSegmentExpectedResultsDto(3, 2, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod1).get(mode1).get((long) 7).put((long) 3, new LinkSegmentExpectedResultsDto(3, 7, 1900,
          0.0228712, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 7, new LinkSegmentExpectedResultsDto(7, 3, 1900,
          0.0228712, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 4, new LinkSegmentExpectedResultsDto(4, 3, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod1).get(mode1).get((long) 8).put((long) 4, new LinkSegmentExpectedResultsDto(4, 8, 1617,
          0.0167522, 1500, 1, 59.693666));
      resultsMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 8, new LinkSegmentExpectedResultsDto(8, 4, 1617,
          0.0167522, 1500, 1, 59.693666));
      resultsMap.get(timePeriod1).get(mode1).get((long) 23).put((long) 16, new LinkSegmentExpectedResultsDto(16, 23,
          483, 0.0200001, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod1).get(mode1).get((long) 16).put((long) 23, new LinkSegmentExpectedResultsDto(23, 16,
          483, 0.0200001, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod1).get(mode1).get((long) 8).put((long) 23, new LinkSegmentExpectedResultsDto(23, 8, 1617,
          0.0200068, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod1).get(mode1).get((long) 23).put((long) 8, new LinkSegmentExpectedResultsDto(8, 23, 1617,
          0.0200068, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod1).get(mode1).get((long) 21).put((long) 13, new LinkSegmentExpectedResultsDto(13, 21,
          0.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod1).get(mode1).get((long) 13).put((long) 21, new LinkSegmentExpectedResultsDto(21, 13,
          0.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod1).get(mode1).get((long) 5).put((long) 21, new LinkSegmentExpectedResultsDto(21, 5,
          899.4, 0.0200007, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod1).get(mode1).get((long) 21).put((long) 5, new LinkSegmentExpectedResultsDto(5, 21,
          899.4, 0.0200007, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod1).get(mode1).get((long) 22).put((long) 14, new LinkSegmentExpectedResultsDto(14, 22,
          17.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod1).get(mode1).get((long) 14).put((long) 22, new LinkSegmentExpectedResultsDto(22, 14,
          17.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod1).get(mode1).get((long) 6).put((long) 22, new LinkSegmentExpectedResultsDto(22, 6,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      resultsMap.get(timePeriod1).get(mode1).get((long) 22).put((long) 6, new LinkSegmentExpectedResultsDto(6, 22,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      resultsMap.get(timePeriod1).get(mode1).get((long) 24).put((long) 15, new LinkSegmentExpectedResultsDto(15, 24,
          500, 0.0200001, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod1).get(mode1).get((long) 15).put((long) 24, new LinkSegmentExpectedResultsDto(24, 15,
          500, 0.0200001, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod1).get(mode1).get((long) 7).put((long) 24, new LinkSegmentExpectedResultsDto(24, 7, 1900,
          0.020013, 10000, 1, 49.967441));
      resultsMap.get(timePeriod1).get(mode1).get((long) 24).put((long) 7, new LinkSegmentExpectedResultsDto(7, 24, 1900,
          0.020013, 10000, 1, 49.967441));

      TimePeriod timePeriod2 = testOutputDto.getC().getTimePeriodByExternalId((long) 1);
      resultsMap.put(timePeriod2, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod2).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 5, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 7, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 8, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 9, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 10, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 13, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 14, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 15, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 16, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 21, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 22, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 23, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 24, new TreeMap<Long, LinkSegmentExpectedResultsDto>());

      resultsMap.get(timePeriod2).get(mode1).get((long) 9).put((long) 12, new LinkSegmentExpectedResultsDto(12, 9, 0.6,
          0.029, 1500, 2.9, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 12).put((long) 9, new LinkSegmentExpectedResultsDto(9, 12, 0.6,
          0.029, 1500, 2.9, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 11).put((long) 12, new LinkSegmentExpectedResultsDto(12, 11,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod2).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod2).get(mode1).get((long) 16).put((long) 12, new LinkSegmentExpectedResultsDto(12, 16,
          483, 0.0100538, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod2).get(mode1).get((long) 12).put((long) 16, new LinkSegmentExpectedResultsDto(16, 12,
          483, 0.0100538, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod2).get(mode1).get((long) 13).put((long) 9, new LinkSegmentExpectedResultsDto(9, 13, 0.6,
          0.01, 1500, 1, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 9).put((long) 13, new LinkSegmentExpectedResultsDto(13, 9, 0.6,
          0.01, 1500, 1, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 11).put((long) 10, new LinkSegmentExpectedResultsDto(10, 11,
          17.6, 0.03, 1500, 3, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 10).put((long) 11, new LinkSegmentExpectedResultsDto(11, 10,
          17.6, 0.03, 1500, 3, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 14).put((long) 10, new LinkSegmentExpectedResultsDto(10, 14,
          17.6, 0.01, 1500, 1, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 10).put((long) 14, new LinkSegmentExpectedResultsDto(14, 10,
          17.6, 0.01, 1500, 1, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 15).put((long) 11, new LinkSegmentExpectedResultsDto(11, 15,
          500, 0.0100617, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod2).get(mode1).get((long) 11).put((long) 15, new LinkSegmentExpectedResultsDto(15, 11,
          500, 0.0100617, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod2).get(mode1).get((long) 5).put((long) 1, new LinkSegmentExpectedResultsDto(1, 5, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 5, new LinkSegmentExpectedResultsDto(5, 1, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 4, new LinkSegmentExpectedResultsDto(4, 1, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 2, new LinkSegmentExpectedResultsDto(2, 1, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod2).get(mode1).get((long) 6).put((long) 2, new LinkSegmentExpectedResultsDto(2, 6, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      resultsMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 6, new LinkSegmentExpectedResultsDto(6, 2, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      resultsMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 2, new LinkSegmentExpectedResultsDto(2, 3, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 3, new LinkSegmentExpectedResultsDto(3, 2, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod2).get(mode1).get((long) 7).put((long) 3, new LinkSegmentExpectedResultsDto(3, 7, 1900,
          0.0228712, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 7, new LinkSegmentExpectedResultsDto(7, 3, 1900,
          0.0228712, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 4, new LinkSegmentExpectedResultsDto(4, 3, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod2).get(mode1).get((long) 8).put((long) 4, new LinkSegmentExpectedResultsDto(4, 8, 1617,
          0.0167522, 1500, 1, 59.693666));
      resultsMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 8, new LinkSegmentExpectedResultsDto(8, 4, 1617,
          0.0167522, 1500, 1, 59.693666));
      resultsMap.get(timePeriod2).get(mode1).get((long) 23).put((long) 16, new LinkSegmentExpectedResultsDto(16, 23,
          483, 0.0200001, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod2).get(mode1).get((long) 16).put((long) 23, new LinkSegmentExpectedResultsDto(23, 16,
          483, 0.0200001, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod2).get(mode1).get((long) 8).put((long) 23, new LinkSegmentExpectedResultsDto(23, 8, 1617,
          0.0200068, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod2).get(mode1).get((long) 23).put((long) 8, new LinkSegmentExpectedResultsDto(8, 23, 1617,
          0.0200068, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod2).get(mode1).get((long) 21).put((long) 13, new LinkSegmentExpectedResultsDto(13, 21,
          0.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod2).get(mode1).get((long) 13).put((long) 21, new LinkSegmentExpectedResultsDto(21, 13,
          0.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod2).get(mode1).get((long) 5).put((long) 21, new LinkSegmentExpectedResultsDto(21, 5,
          899.4, 0.0200007, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod2).get(mode1).get((long) 21).put((long) 5, new LinkSegmentExpectedResultsDto(5, 21,
          899.4, 0.0200007, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod2).get(mode1).get((long) 22).put((long) 14, new LinkSegmentExpectedResultsDto(14, 22,
          17.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod2).get(mode1).get((long) 14).put((long) 22, new LinkSegmentExpectedResultsDto(22, 14,
          17.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod2).get(mode1).get((long) 6).put((long) 22, new LinkSegmentExpectedResultsDto(22, 6,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      resultsMap.get(timePeriod2).get(mode1).get((long) 22).put((long) 6, new LinkSegmentExpectedResultsDto(6, 22,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      resultsMap.get(timePeriod2).get(mode1).get((long) 24).put((long) 15, new LinkSegmentExpectedResultsDto(15, 24,
          500, 0.0200001, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod2).get(mode1).get((long) 15).put((long) 24, new LinkSegmentExpectedResultsDto(24, 15,
          500, 0.0200001, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod2).get(mode1).get((long) 7).put((long) 24, new LinkSegmentExpectedResultsDto(24, 7, 1900,
          0.020013, 10000, 1, 49.967441));
      resultsMap.get(timePeriod2).get(mode1).get((long) 24).put((long) 7, new LinkSegmentExpectedResultsDto(7, 24, 1900,
          0.020013, 10000, 1, 49.967441));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod1, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod1).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod1).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 2,"[21,5,1,2,6,22]");
      pathMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 3,"[21,5,1,4,8,23]");
      pathMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 4,"[21,5,1,4,3,7,24]");
      pathMap.get(timePeriod1).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 1,"[22,6,2,1,5,21]");
      pathMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 2,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 3,"[22,6,2,1,4,8,23]");
      pathMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 4,"[22,6,2,3,7,24]");
      pathMap.get(timePeriod1).get(mode1).put((long) 3, new TreeMap<Long, String>());
      pathMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 1,"[23,8,4,1,5,21]");
      pathMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 2,"[23,8,4,1,2,6,22]");
      pathMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 3,"");
      pathMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 4,"[23,8,4,3,7,24]");
      pathMap.get(timePeriod1).get(mode1).put((long) 4, new TreeMap<Long, String>());
      pathMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 1,"[24,7,3,4,1,5,21]");
      pathMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 2,"[24,7,3,2,6,22]");
      pathMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 3,"[24,7,3,4,8,23]");
      pathMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 4,"");
      pathMap.put(timePeriod2, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod2).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod2).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 2,"[21,5,1,2,6,22]");
      pathMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 3,"[21,5,1,4,8,23]");
      pathMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 4,"[21,5,1,4,3,7,24]");
      pathMap.get(timePeriod2).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 1,"[22,6,2,1,5,21]");
      pathMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 2,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 3,"[22,6,2,1,4,8,23]");
      pathMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 4,"[22,6,2,3,7,24]");
      pathMap.get(timePeriod2).get(mode1).put((long) 3, new TreeMap<Long, String>());
      pathMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 1,"[23,8,4,1,5,21]");
      pathMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 2,"[23,8,4,1,2,6,22]");
      pathMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 3,"");
      pathMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 4,"[23,8,4,3,7,24]");
      pathMap.get(timePeriod2).get(mode1).put((long) 4, new TreeMap<Long, String>());
      pathMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 1,"[24,7,3,4,1,5,21]");
      pathMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 2,"[24,7,3,2,6,22]");
      pathMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 3,"[24,7,3,4,8,23]");
      pathMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 4,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);

      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod1, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod1).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod1).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 2,Double.valueOf(0.0767791));
      odMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 3,Double.valueOf(0.0776307));
      odMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 4,Double.valueOf(0.0944051));
      odMap.get(timePeriod1).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 1,Double.valueOf(0.0767791));
      odMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 3,Double.valueOf(0.0931159));
      odMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 4,Double.valueOf(0.0900226));
      odMap.get(timePeriod1).get(mode1).put((long) 3, new TreeMap<Long, Double>());
      odMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 1,Double.valueOf(0.0776307));
      odMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 2,Double.valueOf(0.0931159));
      odMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 4,Double.valueOf(0.0902602));
      odMap.get(timePeriod1).get(mode1).put((long) 4, new TreeMap<Long, Double>());
      odMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 1,Double.valueOf(0.0944051));
      odMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 2,Double.valueOf(0.0900226));
      odMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 3,Double.valueOf(0.0902602));
      odMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 4,Double.valueOf(0.0));
      odMap.put(timePeriod2, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod2).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod2).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 2,Double.valueOf(0.0767791));
      odMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 3,Double.valueOf(0.0776307));
      odMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 4,Double.valueOf(0.0944051));
      odMap.get(timePeriod2).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 1,Double.valueOf(0.0767791));
      odMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 3,Double.valueOf(0.0931159));
      odMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 4,Double.valueOf(0.0900226));
      odMap.get(timePeriod2).get(mode1).put((long) 3, new TreeMap<Long, Double>());
      odMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 1,Double.valueOf(0.0776307));
      odMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 2,Double.valueOf(0.0931159));
      odMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 4,Double.valueOf(0.0902602));
      odMap.get(timePeriod2).get(mode1).put((long) 4, new TreeMap<Long, Double>());
      odMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 1,Double.valueOf(0.0944051));
      odMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 2,Double.valueOf(0.0900226));
      odMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 3,Double.valueOf(0.0902602));
      odMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 4,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName2,
          xmlFileName2);
    } catch (final Exception e) {
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
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\biDirectionalLinksRouteChoiceSingleModeUsingOdRawMatrix";
      String description = "testRouteChoice4raw";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 5, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 7, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 8, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 9, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 10, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 13, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 14, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 15, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 16, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 21, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 22, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 23, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 24, new TreeMap<Long, LinkSegmentExpectedResultsDto>());

      resultsMap.get(timePeriod).get(mode1).get((long) 9).put((long) 12, new LinkSegmentExpectedResultsDto(12, 9, 0.6,
          0.029, 1500, 2.9, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 9, new LinkSegmentExpectedResultsDto(9, 12, 0.6,
          0.029, 1500, 2.9, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 12, new LinkSegmentExpectedResultsDto(12, 11,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 12, new LinkSegmentExpectedResultsDto(12, 16, 483,
          0.0100538, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 16, new LinkSegmentExpectedResultsDto(16, 12, 483,
          0.0100538, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 9, new LinkSegmentExpectedResultsDto(9, 13, 0.6,
          0.01, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 9).put((long) 13, new LinkSegmentExpectedResultsDto(13, 9, 0.6,
          0.01, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 10, new LinkSegmentExpectedResultsDto(10, 11,
          17.6, 0.03, 1500, 3, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 10).put((long) 11, new LinkSegmentExpectedResultsDto(11, 10,
          17.6, 0.03, 1500, 3, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 10, new LinkSegmentExpectedResultsDto(10, 14,
          17.6, 0.01, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 10).put((long) 14, new LinkSegmentExpectedResultsDto(14, 10,
          17.6, 0.01, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 11, new LinkSegmentExpectedResultsDto(11, 15, 500,
          0.0100617, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 15, new LinkSegmentExpectedResultsDto(15, 11, 500,
          0.0100617, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 1, new LinkSegmentExpectedResultsDto(1, 5, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 5, new LinkSegmentExpectedResultsDto(5, 1, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4, new LinkSegmentExpectedResultsDto(4, 1, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, new LinkSegmentExpectedResultsDto(2, 1, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 2, new LinkSegmentExpectedResultsDto(2, 6, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 6, new LinkSegmentExpectedResultsDto(6, 2, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2, new LinkSegmentExpectedResultsDto(2, 3, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3, new LinkSegmentExpectedResultsDto(3, 2, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 3, new LinkSegmentExpectedResultsDto(3, 7, 1900,
          0.0228712, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 7, new LinkSegmentExpectedResultsDto(7, 3, 1900,
          0.0228712, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4, new LinkSegmentExpectedResultsDto(4, 3, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 4, new LinkSegmentExpectedResultsDto(4, 8, 1617,
          0.0167522, 1500, 1, 59.693666));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 8, new LinkSegmentExpectedResultsDto(8, 4, 1617,
          0.0167522, 1500, 1, 59.693666));
      resultsMap.get(timePeriod).get(mode1).get((long) 23).put((long) 16, new LinkSegmentExpectedResultsDto(16, 23, 483,
          0.0200001, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 23, new LinkSegmentExpectedResultsDto(23, 16, 483,
          0.0200001, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 23, new LinkSegmentExpectedResultsDto(23, 8, 1617,
          0.0200068, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod).get(mode1).get((long) 23).put((long) 8, new LinkSegmentExpectedResultsDto(8, 23, 1617,
          0.0200068, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod).get(mode1).get((long) 21).put((long) 13, new LinkSegmentExpectedResultsDto(13, 21, 0.6,
          0.02, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 21, new LinkSegmentExpectedResultsDto(21, 13, 0.6,
          0.02, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 21, new LinkSegmentExpectedResultsDto(21, 5, 899.4,
          0.0200007, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod).get(mode1).get((long) 21).put((long) 5, new LinkSegmentExpectedResultsDto(5, 21, 899.4,
          0.0200007, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod).get(mode1).get((long) 22).put((long) 14, new LinkSegmentExpectedResultsDto(14, 22,
          17.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 22, new LinkSegmentExpectedResultsDto(22, 14,
          17.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 22, new LinkSegmentExpectedResultsDto(22, 6,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      resultsMap.get(timePeriod).get(mode1).get((long) 22).put((long) 6, new LinkSegmentExpectedResultsDto(6, 22,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      resultsMap.get(timePeriod).get(mode1).get((long) 24).put((long) 15, new LinkSegmentExpectedResultsDto(15, 24, 500,
          0.0200001, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 24, new LinkSegmentExpectedResultsDto(24, 15, 500,
          0.0200001, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 24, new LinkSegmentExpectedResultsDto(24, 7, 1900,
          0.020013, 10000, 1, 49.967441));
      resultsMap.get(timePeriod).get(mode1).get((long) 24).put((long) 7, new LinkSegmentExpectedResultsDto(7, 24, 1900,
          0.020013, 10000, 1, 49.967441));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[21,5,1,2,6,22]");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 3,"[21,5,1,4,8,23]");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4,"[21,5,1,4,3,7,24]");
      pathMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,"[22,6,2,1,5,21]");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,"");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3,"[22,6,2,1,4,8,23]");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 4,"[22,6,2,3,7,24]");
      pathMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1,"[23,8,4,1,5,21]");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2,"[23,8,4,1,2,6,22]");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 3,"");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4,"[23,8,4,3,7,24]");
      pathMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1,"[24,7,3,4,1,5,21]");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 2,"[24,7,3,2,6,22]");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3,"[24,7,3,4,8,23]");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 4,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,Double.valueOf(0.0767791));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 3,Double.valueOf(0.0776307));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4,Double.valueOf(0.0944051));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,Double.valueOf(0.0767791));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3,Double.valueOf(0.0931159));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 4,Double.valueOf(0.0900226));
      odMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1,Double.valueOf(0.0776307));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2,Double.valueOf(0.0931159));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4,Double.valueOf(0.0902602));
      odMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1,Double.valueOf(0.0944051));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 2,Double.valueOf(0.0900226));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3,Double.valueOf(0.0902602));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 4,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\biDirectionalLinksRouteChoiceSingleModeWithPlusSignSeparator";
      String description = "testRouteChoice4raw2";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 5, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 7, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 8, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 9, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 10, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 13, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 14, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 15, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 16, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 21, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 22, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 23, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 24, new TreeMap<Long, LinkSegmentExpectedResultsDto>());

      resultsMap.get(timePeriod).get(mode1).get((long) 9).put((long) 12, new LinkSegmentExpectedResultsDto(12, 9, 0.6,
          0.029, 1500, 2.9, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 9, new LinkSegmentExpectedResultsDto(9, 12, 0.6,
          0.029, 1500, 2.9, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 12, new LinkSegmentExpectedResultsDto(12, 11,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12,
          482.4, 0.0301605, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 12, new LinkSegmentExpectedResultsDto(12, 16, 483,
          0.0100538, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 16, new LinkSegmentExpectedResultsDto(16, 12, 483,
          0.0100538, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 9, new LinkSegmentExpectedResultsDto(9, 13, 0.6,
          0.01, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 9).put((long) 13, new LinkSegmentExpectedResultsDto(13, 9, 0.6,
          0.01, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 10, new LinkSegmentExpectedResultsDto(10, 11,
          17.6, 0.03, 1500, 3, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 10).put((long) 11, new LinkSegmentExpectedResultsDto(11, 10,
          17.6, 0.03, 1500, 3, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 10, new LinkSegmentExpectedResultsDto(10, 14,
          17.6, 0.01, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 10).put((long) 14, new LinkSegmentExpectedResultsDto(14, 10,
          17.6, 0.01, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 11, new LinkSegmentExpectedResultsDto(11, 15, 500,
          0.0100617, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 15, new LinkSegmentExpectedResultsDto(15, 11, 500,
          0.0100617, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 1, new LinkSegmentExpectedResultsDto(1, 5, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 5, new LinkSegmentExpectedResultsDto(5, 1, 899.4,
          0.0106463, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4, new LinkSegmentExpectedResultsDto(4, 1, 1087.4,
          0.0102428, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, new LinkSegmentExpectedResultsDto(2, 1, 1012,
          0.0099323, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 2, new LinkSegmentExpectedResultsDto(2, 6, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 6, new LinkSegmentExpectedResultsDto(6, 2, 1582.4,
          0.0161926, 1500, 1, 61.756766));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2, new LinkSegmentExpectedResultsDto(2, 3, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3, new LinkSegmentExpectedResultsDto(3, 2, 994.4,
          0.0109657, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 3, new LinkSegmentExpectedResultsDto(3, 7, 1900,
          0.0228712, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 7, new LinkSegmentExpectedResultsDto(7, 3, 1900,
          0.0228712, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4, new LinkSegmentExpectedResultsDto(4, 3, 905.6,
          0.0106643, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 4, new LinkSegmentExpectedResultsDto(4, 8, 1617,
          0.0167522, 1500, 1, 59.693666));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 8, new LinkSegmentExpectedResultsDto(8, 4, 1617,
          0.0167522, 1500, 1, 59.693666));
      resultsMap.get(timePeriod).get(mode1).get((long) 23).put((long) 16, new LinkSegmentExpectedResultsDto(16, 23, 483,
          0.0200001, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 23, new LinkSegmentExpectedResultsDto(23, 16, 483,
          0.0200001, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 23, new LinkSegmentExpectedResultsDto(23, 8, 1617,
          0.0200068, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod).get(mode1).get((long) 23).put((long) 8, new LinkSegmentExpectedResultsDto(8, 23, 1617,
          0.0200068, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod).get(mode1).get((long) 21).put((long) 13, new LinkSegmentExpectedResultsDto(13, 21, 0.6,
          0.02, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 21, new LinkSegmentExpectedResultsDto(21, 13, 0.6,
          0.02, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 21, new LinkSegmentExpectedResultsDto(21, 5, 899.4,
          0.0200007, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod).get(mode1).get((long) 21).put((long) 5, new LinkSegmentExpectedResultsDto(5, 21, 899.4,
          0.0200007, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod).get(mode1).get((long) 22).put((long) 14, new LinkSegmentExpectedResultsDto(14, 22,
          17.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 22, new LinkSegmentExpectedResultsDto(22, 14,
          17.6, 0.02, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 22, new LinkSegmentExpectedResultsDto(22, 6,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      resultsMap.get(timePeriod).get(mode1).get((long) 22).put((long) 6, new LinkSegmentExpectedResultsDto(6, 22,
          1582.4, 0.0200063, 10000, 1, 49.98433));
      resultsMap.get(timePeriod).get(mode1).get((long) 24).put((long) 15, new LinkSegmentExpectedResultsDto(15, 24, 500,
          0.0200001, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 24, new LinkSegmentExpectedResultsDto(24, 15, 500,
          0.0200001, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 24, new LinkSegmentExpectedResultsDto(24, 7, 1900,
          0.020013, 10000, 1, 49.967441));
      resultsMap.get(timePeriod).get(mode1).get((long) 24).put((long) 7, new LinkSegmentExpectedResultsDto(7, 24, 1900,
          0.020013, 10000, 1, 49.967441));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,"");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,"[21,5,1,2,6,22]");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 3,"[21,5,1,4,8,23]");
      pathMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4,"[21,5,1,4,3,7,24]");
      pathMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,"[22,6,2,1,5,21]");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,"");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3,"[22,6,2,1,4,8,23]");
      pathMap.get(timePeriod).get(mode1).get((long) 2).put((long) 4,"[22,6,2,3,7,24]");
      pathMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1,"[23,8,4,1,5,21]");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2,"[23,8,4,1,2,6,22]");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 3,"");
      pathMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4,"[23,8,4,3,7,24]");
      pathMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1,"[24,7,3,4,1,5,21]");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 2,"[24,7,3,2,6,22]");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3,"[24,7,3,4,8,23]");
      pathMap.get(timePeriod).get(mode1).get((long) 4).put((long) 4,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 1,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2,Double.valueOf(0.0767791));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 3,Double.valueOf(0.0776307));
      odMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4,Double.valueOf(0.0944051));
      odMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1,Double.valueOf(0.0767791));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 2,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3,Double.valueOf(0.0931159));
      odMap.get(timePeriod).get(mode1).get((long) 2).put((long) 4,Double.valueOf(0.0900226));
      odMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1,Double.valueOf(0.0776307));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2,Double.valueOf(0.0931159));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 3,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4,Double.valueOf(0.0902602));
      odMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1,Double.valueOf(0.0944051));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 2,Double.valueOf(0.0900226));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3,Double.valueOf(0.0902602));
      odMap.get(timePeriod).get(mode1).get((long) 4).put((long) 4,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceTwoModes";
      String description = "testRouteChoice5";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;

      TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters = (physicalNetwork,
          bprLinkTravelTimeCost, inputBuilderListener) -> {
        MacroscopicLinkSegmentType macroscopiclinkSegmentType = inputBuilderListener.getLinkSegmentTypeByExternalId(
            (long) 1);
        Mode mode = inputBuilderListener.getModeByExternalId((long) 2);
        bprLinkTravelTimeCost.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
      };

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, setCostParameters, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      Mode mode2 = testOutputDto.getC().getModeByExternalId((long) 2);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 11, new LinkSegmentExpectedResultsDto(11, 1, 3000,
          0.0370117, 3600.0, 1.0, 27.0184697));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 6,
          0.0447625, 1200.0, 2.0, 44.6802634));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1, new LinkSegmentExpectedResultsDto(1, 3, 1068,
          0.0360526, 1200.0, 1.0, 27.7372551));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1926,
          0.0719659, 1200.0, 1.0, 13.8954751));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 2, new LinkSegmentExpectedResultsDto(2, 4, 6,
          0.0447625, 1200.0, 2.0, 44.6802634));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 1068,
          0.0360526, 1200.0, 1.0, 27.7372551));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 4, new LinkSegmentExpectedResultsDto(4, 12, 3000,
          0.0370117, 3600.0, 1.0, 27.0184697));

      resultsMap.get(timePeriod).put(mode2, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode2).put((long) 1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).get((long) 1).put((long) 11, new LinkSegmentExpectedResultsDto(11, 1, 1500,
          0.0636732, 3600.0, 1.0, 15.705194));
      resultsMap.get(timePeriod).get(mode2).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1086,
          0.0609332, 1200.0, 2.0, 32.8228128));
      resultsMap.get(timePeriod).get(mode2).get((long) 3).put((long) 1, new LinkSegmentExpectedResultsDto(1, 3, 414,
          0.0613639, 1200.0, 1.0, 16.296231));
      resultsMap.get(timePeriod).get(mode2).get((long) 4).put((long) 2, new LinkSegmentExpectedResultsDto(2, 4, 1086,
          0.0609332, 1200.0, 2.0, 32.8228128));
      resultsMap.get(timePeriod).get(mode2).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 414,
          0.0613639, 1200.0, 1.0, 16.296231));
      resultsMap.get(timePeriod).get(mode2).get((long) 12).put((long) 4, new LinkSegmentExpectedResultsDto(4, 12, 1500,
          0.0636732, 3600.0, 1.0, 15.705194));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
          maxIterations, resultsMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).put(mode2, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 27, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 27).put((long) 27,"");
      pathMap.get(timePeriod).get(mode1).get((long) 27).put((long) 31,"");
      pathMap.get(timePeriod).get(mode2).put((long) 27, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode2).get((long) 27).put((long) 27,"");
      pathMap.get(timePeriod).get(mode2).get((long) 27).put((long) 31,"");
      pathMap.get(timePeriod).get(mode1).put((long) 31, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 31).put((long) 27,"[11,1,4,12]");
      pathMap.get(timePeriod).get(mode1).get((long) 31).put((long) 31,"");
      pathMap.get(timePeriod).get(mode2).put((long) 31, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode2).get((long) 31).put((long) 27,"[11,1,2,4,12]");
      pathMap.get(timePeriod).get(mode2).get((long) 31).put((long) 31,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod).put(mode2, new TreeMap<Long, Map<Long, Double>>());
      
      odMap.get(timePeriod).get(mode1).put((long) 27, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 27).put((long) 27,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 27).put((long) 31,Double.valueOf(0.0));
      
      odMap.get(timePeriod).get(mode2).put((long) 27, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode2).get((long) 27).put((long) 27,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode2).get((long) 27).put((long) 31,Double.valueOf(0.0));
      
      odMap.get(timePeriod).get(mode1).put((long) 31, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 31).put((long) 27,Double.valueOf(0.1457425));
      odMap.get(timePeriod).get(mode1).get((long) 31).put((long) 31,Double.valueOf(0.0));
      
      odMap.get(timePeriod).get(mode2).put((long) 31, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode2).get((long) 31).put((long) 27,Double.valueOf(0.249072));
      odMap.get(timePeriod).get(mode2).get((long) 31).put((long) 31,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceTwoModesIdentifyLinksById";
      String description = "testRouteChoice5";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;

      TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters = (physicalNetwork,
          bprLinkTravelTimeCost, inputBuilderListener) -> {
        MacroscopicLinkSegmentType macroscopiclinkSegmentType = inputBuilderListener.getLinkSegmentTypeByExternalId(
            (long) 1);
        Mode mode = inputBuilderListener.getModeByExternalId((long) 2);
        bprLinkTravelTimeCost.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
      };

      Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties = (
          linkOutputTypeConfiguration) -> {
        try {
          linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
          linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_ID);
          linkOutputTypeConfiguration.removeProperty(OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID);
          linkOutputTypeConfiguration.removeProperty(OutputProperty.UPSTREAM_NODE_EXTERNAL_ID);
        } catch (PlanItException e) {
          LOGGER.severe(e.getMessage());
          fail(e.getMessage());
        }
      };

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, setOutputTypeConfigurationProperties, maxIterations, 0.0,
              setCostParameters, description, true, false);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      Mode mode2 = testOutputDto.getC().getModeByExternalId((long) 2);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);

      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, LinkSegmentExpectedResultsDto>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, LinkSegmentExpectedResultsDto>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 0, new LinkSegmentExpectedResultsDto(11, 1, 3000, 0.0370117,
          3600.0, 1.0, 27.0184697));
      resultsMap.get(timePeriod).get(mode1).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1926, 0.0719659,
          1200.0, 1.0, 13.8954751));
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new LinkSegmentExpectedResultsDto(4, 12, 3000, 0.0370117,
          3600.0, 1.0, 27.0184697));
      resultsMap.get(timePeriod).get(mode1).put((long) 3, new LinkSegmentExpectedResultsDto(1, 2, 6, 0.0447625,
          1200.0, 2.0, 44.6802634));
      resultsMap.get(timePeriod).get(mode1).put((long) 4, new LinkSegmentExpectedResultsDto(2, 4, 6, 0.0447625,
          1200.0, 2.0, 44.6802634));
      resultsMap.get(timePeriod).get(mode1).put((long) 5, new LinkSegmentExpectedResultsDto(1, 3, 1068, 0.0360526,
          1200.0, 1.0, 27.7372551));
      resultsMap.get(timePeriod).get(mode1).put((long) 6, new LinkSegmentExpectedResultsDto(3, 4, 1068, 0.0360526,
          1200.0, 1.0, 27.7372551));
      resultsMap.get(timePeriod).put(mode2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 0, new LinkSegmentExpectedResultsDto(11, 1, 1500, 0.0636732,
          3600.0, 1.0, 15.705194));
      resultsMap.get(timePeriod).get(mode2).put((long) 2, new LinkSegmentExpectedResultsDto(4, 12, 1500, 0.0636732,
          3600.0, 1.0, 15.705194));
      resultsMap.get(timePeriod).get(mode2).put((long) 3, new LinkSegmentExpectedResultsDto(1, 2, 1086, 0.0609332,
          1200.0, 2.0, 32.8228128));
      resultsMap.get(timePeriod).get(mode2).put((long) 4, new LinkSegmentExpectedResultsDto(2, 4, 1086, 0.0609332,
          1200.0, 2.0, 32.8228128));
      resultsMap.get(timePeriod).get(mode2).put((long) 5, new LinkSegmentExpectedResultsDto(1, 3, 414, 0.0613639,
          1200.0, 1.0, 16.296231));
      resultsMap.get(timePeriod).get(mode2).put((long) 6, new LinkSegmentExpectedResultsDto(3, 4, 414, 0.0613639,
          1200.0, 1.0, 16.296231));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingLinkSegmentId(memoryOutputFormatter, maxIterations,
          resultsMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).put(mode2, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 27, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 27).put((long) 27,"");
      pathMap.get(timePeriod).get(mode1).get((long) 27).put((long) 31,"");
      pathMap.get(timePeriod).get(mode2).put((long) 27, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode2).get((long) 27).put((long) 27,"");
      pathMap.get(timePeriod).get(mode2).get((long) 27).put((long) 31,"");
      pathMap.get(timePeriod).get(mode1).put((long) 31, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 31).put((long) 27,"[11,1,4,12]");
      pathMap.get(timePeriod).get(mode1).get((long) 31).put((long) 31,"");
      pathMap.get(timePeriod).get(mode2).put((long) 31, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode2).get((long) 31).put((long) 27,"[11,1,2,4,12]");
      pathMap.get(timePeriod).get(mode2).get((long) 31).put((long) 31,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod).put(mode2, new TreeMap<Long, Map<Long, Double>>());
      
      odMap.get(timePeriod).get(mode1).put((long) 27, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 27).put((long) 27,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 27).put((long) 31,Double.valueOf(0.0));
      
      odMap.get(timePeriod).get(mode2).put((long) 27, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode2).get((long) 27).put((long) 27,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode2).get((long) 27).put((long) 31,Double.valueOf(0.0));
      
      odMap.get(timePeriod).get(mode1).put((long) 31, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 31).put((long) 27,Double.valueOf(0.1457425));
      odMap.get(timePeriod).get(mode1).get((long) 31).put((long) 31,Double.valueOf(0.0));
      
      odMap.get(timePeriod).get(mode2).put((long) 31, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode2).get((long) 31).put((long) 27,Double.valueOf(0.249072));
      odMap.get(timePeriod).get(mode2).get((long) 31).put((long) 31,Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
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
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\SIMOMISOrouteChoiceTwoModesWithImpossibleRoute";
      String description = "testRouteChoice5";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;

      TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters = (physicalNetwork,
          bprLinkTravelTimeCost, inputBuilderListener) -> {
        MacroscopicLinkSegmentType macroscopiclinkSegmentType = inputBuilderListener.getLinkSegmentTypeByExternalId(
            (long) 1);
        Mode mode = inputBuilderListener.getModeByExternalId((long) 2);
        bprLinkTravelTimeCost.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
      };

      //TODO - Comparisons with MemoryOutputFormatter have been commented out due to insufficient time 
      //to configure them
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, setCostParameters, description, true, true);
      //MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      Mode mode2 = testOutputDto.getC().getModeByExternalId((long) 2);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 11, new LinkSegmentExpectedResultsDto(11, 1, 3000,
          0.0370117, 3600.0, 1.0, 27.0184697));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 6,
          0.0447625, 1200.0, 2.0, 44.6802634));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1, new LinkSegmentExpectedResultsDto(1, 3, 1068,
          0.0360526, 1200.0, 1.0, 27.7372551));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1926,
          0.0719659, 1200.0, 1.0, 13.8954751));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 2, new LinkSegmentExpectedResultsDto(2, 4, 6,
          0.0447625, 1200.0, 2.0, 44.6802634));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 1068,
          0.0360526, 1200.0, 1.0, 27.7372551));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 4, new LinkSegmentExpectedResultsDto(4, 12, 3000,
          0.0370117, 3600.0, 1.0, 27.0184697));

      resultsMap.get(timePeriod).put(mode2, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode2).put((long) 1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).get((long) 1).put((long) 11, new LinkSegmentExpectedResultsDto(11, 1, 1500,
          0.0636732, 3600.0, 1.0, 15.705194));
      resultsMap.get(timePeriod).get(mode2).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1086,
          0.0609332, 1200.0, 2.0, 32.8228128));
      resultsMap.get(timePeriod).get(mode2).get((long) 3).put((long) 1, new LinkSegmentExpectedResultsDto(1, 3, 414,
          0.0613639, 1200.0, 1.0, 16.296231));
      resultsMap.get(timePeriod).get(mode2).get((long) 4).put((long) 2, new LinkSegmentExpectedResultsDto(2, 4, 1086,
          0.0609332, 1200.0, 2.0, 32.8228128));
      resultsMap.get(timePeriod).get(mode2).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 414,
          0.0613639, 1200.0, 1.0, 16.296231));
      resultsMap.get(timePeriod).get(mode2).get((long) 12).put((long) 4, new LinkSegmentExpectedResultsDto(4, 12, 1500,
          0.0636732, 3600.0, 1.0, 15.705194));
      //PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesExternalId(memoryOutputFormatter,
      //    maxIterations, resultsMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>> pathMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, String>>>>();
      pathMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).put(mode2, new TreeMap<Long, Map<Long, String>>());
      pathMap.get(timePeriod).get(mode1).put((long) 27, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 27).put((long) 27,"");
      pathMap.get(timePeriod).get(mode1).get((long) 27).put((long) 31,"");
      pathMap.get(timePeriod).get(mode2).put((long) 27, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode2).get((long) 27).put((long) 27,"");
      pathMap.get(timePeriod).get(mode2).get((long) 27).put((long) 31,"");
      pathMap.get(timePeriod).get(mode1).put((long) 31, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode1).get((long) 31).put((long) 27,"[11,1,4,12]");
      pathMap.get(timePeriod).get(mode1).get((long) 31).put((long) 31,"");
      pathMap.get(timePeriod).get(mode2).put((long) 31, new TreeMap<Long, String>());
      pathMap.get(timePeriod).get(mode2).get((long) 31).put((long) 27,"[11,1,2,4,12]");
      pathMap.get(timePeriod).get(mode2).get((long) 31).put((long) 31,"");
      //PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      Map<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>> odMap =
          new TreeMap<TimePeriod, Map<Mode, Map<Long, Map<Long, Double>>>>();
      odMap.put(timePeriod, new TreeMap<Mode, Map<Long, Map<Long, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<Long, Map<Long, Double>>());
      odMap.get(timePeriod).put(mode2, new TreeMap<Long, Map<Long, Double>>());
      
      odMap.get(timePeriod).get(mode1).put((long) 27, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 27).put((long) 27,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get((long) 27).put((long) 31,Double.valueOf(0.0));
      
      odMap.get(timePeriod).get(mode2).put((long) 27, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode2).get((long) 27).put((long) 27,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode2).get((long) 27).put((long) 31,Double.valueOf(0.0));
      
      odMap.get(timePeriod).get(mode1).put((long) 31, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode1).get((long) 31).put((long) 27,Double.valueOf(0.1457425));
      odMap.get(timePeriod).get(mode1).get((long) 31).put((long) 31,Double.valueOf(0.0));
      
      odMap.get(timePeriod).get(mode2).put((long) 31, new TreeMap<Long, Double>());
      odMap.get(timePeriod).get(mode2).get((long) 31).put((long) 27,Double.valueOf(0.249072));
      odMap.get(timePeriod).get(mode2).get((long) 31).put((long) 31,Double.valueOf(0.0));
      //PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception e) {
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
  
}