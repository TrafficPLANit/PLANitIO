package org.goplanit.io.test.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.goplanit.cost.physical.BPRConfigurator;
import org.goplanit.demands.Demands;
import org.goplanit.io.test.util.PlanItIOTestHelper;
import org.goplanit.io.test.util.PlanItIOTestRunner;
import org.goplanit.io.test.util.PlanItInputBuilder4Testing;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.utils.functionalinterface.TriConsumer;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.test.LinkSegmentExpectedResultsDto;
import org.goplanit.utils.test.TestOutputDto;
import org.goplanit.utils.time.TimePeriod;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * JUnit test cases for BPR tests for TraditionalStaticAssignment
 * 
 * @author gman6028, markr
 *
 */
public class BPRTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path testCasePath = Path.of("src","test","resources","testcases");
  
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

    String fullCsvFileNameWithoutDescription = Path.of(projectPath , outputType.value() + "_" + csvFileName).toString();
    String fullCsvFileNameWithDescription = Path.of(projectPath , outputType.value() + "_" + description + "_"+ csvFileName).toString();

    assertTrue(PlanItIOTestHelper.compareFiles(fullCsvFileNameWithoutDescription, fullCsvFileNameWithDescription, true));
    PlanItIOTestHelper.deleteFile(outputType, projectPath, description, csvFileName);

    String fullXmlFileNameWithoutDescription = Path.of(projectPath , outputType.value() + "_" + xmlFileName).toString();
    String fullXmlFileNameWithDescription = Path.of(projectPath , outputType.value() + "_" + description + "_"+ xmlFileName).toString();
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
    pathMap = new TreeMap<>();
    odMap = new TreeMap<>();
    resultsMap = new TreeMap<>();
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
      String projectPath = Path.of(testCasePath.toString(),"bpr_parameters_test","xml","simple").toString();
      String description = "mode_test";
      String csvFileName = "Time_Period_1_2.csv";
      String odCsvFileName = "Time_Period_1_1.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 2;
  
      TriConsumer<LayeredNetwork<?,?>, BPRConfigurator, PlanItInputBuilder4Testing> setCostParametersConsumer = 
          (network, bpr, inputBuilderListener) -> {
            Mode mode = network.getModes().getByXmlId("1");
            if(mode == null) {
              fail("link segment type not present");
            }                               
            MacroscopicNetworkLayer layer = (MacroscopicNetworkLayer)network.getLayerByMode(mode);
            MacroscopicLinkSegmentType macroscopiclinkSegmentType = layer.getLinkSegmentTypes().getByXmlId("1");
            if(macroscopiclinkSegmentType == null) {
              fail("link segment type not present");
            }          
            bpr.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
            MacroscopicLinkSegment linkSegment = layer.getLinkSegments().getByXmlId("3");
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
  
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = 
          runner.setupAndExecuteWithCustomBprConfiguration(setCostParametersConsumer);
      
      /* verify outcome */      
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      MacroscopicNetwork network = (MacroscopicNetwork)testOutputDto.getB().physicalNetworks.getFirst();
      Mode mode1 = network.getModes().getByXmlId("1");
      Demands demands = (Demands)testOutputDto.getB().demands.getFirst();
      TimePeriod timePeriod = demands.timePeriods.firstMatch(tp -> tp.getXmlId().equals("0"));
           
      resultsMap.put(timePeriod, new TreeMap<>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<>());
      resultsMap.get(timePeriod).get(mode1).put(node2XmlId, new TreeMap<>());
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
  
      pathMap.put(timePeriod, new TreeMap<>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<>());
      pathMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,2,3,4,5,6]");
      pathMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId,""); 
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);          
      
      odMap.put(timePeriod, new TreeMap<>());
      odMap.get(timePeriod).put(mode1, new TreeMap<>());
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