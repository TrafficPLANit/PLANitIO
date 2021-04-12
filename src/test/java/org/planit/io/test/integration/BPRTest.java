package org.planit.io.test.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.cost.physical.BPRConfigurator;
import org.planit.input.InputBuilderListener;
import org.planit.io.test.util.PlanItIOTestHelper;
import org.planit.io.test.util.PlanItIOTestRunner;
import org.planit.utils.test.LinkSegmentExpectedResultsDto;
import org.planit.utils.test.TestOutputDto;

import org.planit.logging.Logging;
import org.planit.network.InfrastructureNetwork;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.project.CustomPlanItProject;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.functionalinterface.TriConsumer;
import org.planit.utils.mode.Mode;
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
  
  private final String zone1XmlId = "1";
  private final String zone2XmlId = "2";
  
  private final String node1XmlId = "1";
  private final String node2XmlId = "2";
  private final String node3XmlId = "3";
  private final String node4XmlId = "4";
  private final String node5XmlId = "5";
  private final String node6XmlId = "6";  
  
  /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, path string */
  private Map<TimePeriod, Map<Mode, Map<String, Map<String, String>>>> pathMap;    
  /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, od value */
  private Map<TimePeriod, Map<Mode, Map<String, Map<String, Double>>>> odMap;
  /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, result DTO */
  SortedMap<TimePeriod, SortedMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>> resultsMap;  

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
    String fullCsvFileNameWithDescription = projectPath + "\\" + outputType.value() + "_" + description + "_"+ csvFileName;

    assertTrue(PlanItIOTestHelper.compareFiles(fullCsvFileNameWithoutDescription, fullCsvFileNameWithDescription));
    PlanItIOTestHelper.deleteFile(outputType, projectPath, description, csvFileName);

    String fullXmlFileNameWithoutDescription = projectPath + "\\" + outputType.value() + "_" + xmlFileName;
    String fullXmlFileNameWithDescription = projectPath + "\\" + outputType.value() + "_" + description + "_"+ xmlFileName;
    assertTrue(PlanItIOTestHelper.isXmlFileSameExceptForTimestamp(fullXmlFileNameWithoutDescription,fullXmlFileNameWithDescription));
    PlanItIOTestHelper.deleteFile(outputType, projectPath, description, xmlFileName);
  }

  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(BPRTest.class);
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
  
      TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParametersConsumer = 
          (network, bpr, inputBuilderListener) -> {
            MacroscopicLinkSegmentType macroscopiclinkSegmentType = inputBuilderListener.getLinkSegmentTypeBySourceId("1");
            if(macroscopiclinkSegmentType == null) {
              fail("link segment type not present");
            }
            Mode mode = inputBuilderListener.getModeBySourceId("1");
            if(mode == null) {
              fail("link segment type not present");
            }            
            bpr.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
            MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) inputBuilderListener.getLinkSegmentByXmlId("3");
            bpr.setParameters(linkSegment, mode, 1.0, 5.0);
      };
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);           
      
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setPersistZeroFlow(false);
      runner.setUseFixedConnectoidCost();
  
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = runner.setupAndExecuteWithCustomBprConfiguration(setCostParametersConsumer);
      
      /* verify outcome */      
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();  
      Mode mode1 = testOutputDto.getC().getModeBySourceId("1");
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodBySourceId("0");
           
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put(node2XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get(node2XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 2000, 19.1019336, 1000.0, 10.0, 0.5235072));
      resultsMap.get(timePeriod).get(mode1).put(node3XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get(node3XmlId).put(node2XmlId, new LinkSegmentExpectedResultsDto(2, 3, 2000, 4.5, 1000.0, 10.0, 2.2222222));
      resultsMap.get(timePeriod).get(mode1).put(node4XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get(node4XmlId).put(node3XmlId, new LinkSegmentExpectedResultsDto(3, 4, 2000, 33.0, 1000.0, 10.0, 0.3030303));
      resultsMap.get(timePeriod).get(mode1).put(node5XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get(node5XmlId).put(node4XmlId, new LinkSegmentExpectedResultsDto(4, 5, 2000, 4.5, 1000.0, 10.0, 2.2222222));
      resultsMap.get(timePeriod).get(mode1).put(node6XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get(node6XmlId).put(node5XmlId, new LinkSegmentExpectedResultsDto(5, 6, 2000, 19.1019336, 1000.0, 10.0, 0.5235072));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter, maxIterations, resultsMap);
  
      pathMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, String>>());
      pathMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,2,3,4,5,6]");
      pathMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId,""); 
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);          
      
      odMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, Double>>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId, Double.valueOf(80.2038651));
      odMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
  
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName,xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName,xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName,xmlFileName);
    } catch (final Exception e) {
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
}