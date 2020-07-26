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
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.input.InputBuilderListener;
import org.planit.utils.test.LinkSegmentExpectedResultsDto;
import org.planit.io.test.util.PlanItIOTestHelper;
import org.planit.utils.test.TestOutputDto;

import org.planit.logging.Logging;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.project.CustomPlanItProject;
import org.planit.time.TimePeriod;
import org.planit.utils.functionalinterface.TriConsumer;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;

/**
 * JUnit test cases for BPR tests for TraditionalStaticAssignment
 * 
 * @author gman6028, markr
 *
 */
public class BPRTest {

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
      LOGGER = Logging.createLogger(BPRTest.class);
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
}