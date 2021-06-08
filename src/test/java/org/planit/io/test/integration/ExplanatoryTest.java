package org.planit.io.test.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.utils.test.LinkSegmentExpectedResultsDto;
import org.planit.io.test.util.PlanItIOTestHelper;
import org.planit.io.test.util.PlanItIOTestRunner;
import org.planit.io.test.util.PlanItInputBuilder4Testing;
import org.planit.utils.test.TestOutputDto;

import org.planit.logging.Logging;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.project.CustomPlanItProject;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.mode.Mode;

/**
 * JUnit test cases for explanatory tests for TraditionalStaticAssignment
 * 
 * @author gman6028, markr
 *
 */
public class ExplanatoryTest {

  /** the logger */
  private static Logger LOGGER = null;
  
  private final String zone1XmlId = "1";
  private final String zone2XmlId = "2";
  
  private final String node1XmlId = "1";
  private final String node2XmlId = "2";  
  
  /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, path string */
  private Map<TimePeriod, Map<Mode, Map<String, Map<String, String>>>> pathMap;   
  /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, od value */
  private Map<TimePeriod, Map<Mode, Map<String, Map<String, Double>>>> odMap;
  /* TODO: refactor UGLY: timeperiod, mode origin zone xml id, destination zone xml id, result DTO */
  SortedMap<TimePeriod, SortedMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>> resultsMap;    

  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(ExplanatoryTest.class);
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
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);
      
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setPersistZeroFlow(false);
      runner.setUseFixedConnectoidCost();     
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();
      
      
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByXmlId("1");
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByXmlId("0");
      
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put(node2XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get(node2XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0, 2000.0, 10.0, 1.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter,maxIterations, resultsMap);

      pathMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, String>>());
      pathMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,2]");
      pathMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId,"");  
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);          
      
      odMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, Double>>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId, Double.valueOf(10.0));
      odMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId, Double.valueOf(0.0));
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
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);
      
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setPersistZeroFlow(true); // <--
      runner.setUseFixedConnectoidCost();     
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();

      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByXmlId("1");
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByXmlId("0");
      
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put(node2XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get(node2XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0,2000.0, 10.0, 1.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter,maxIterations, resultsMap);

      pathMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, String>>());
      pathMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,2]");
      pathMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId,"[2,1]");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId,"");  
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
                  
      odMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, Double>>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId, Double.valueOf(10.0));
      odMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId, Double.valueOf(10.0));
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
 
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId_0_" + description, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId_0_" + description, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId_0_" + description, csvFileName, xmlFileName);
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
  public void test_explanatory_time_period_xml_id_test() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\timePeriodXmlIdTest";
      String description = "explanatory";
      String csvFileName = "Time_Period_2_2.csv";
      String odCsvFileName = "Time_Period_2_1.csv";
      String xmlFileName = "Time_Period_2.xml";
      Integer maxIterations = null;
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);      

      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setPersistZeroFlow(false);
      runner.setUseFixedConnectoidCost();     
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();      
      
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByXmlId("1");
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByXmlId("2");
      
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put(node2XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get(node2XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0,2000.0, 10.0, 1.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter,maxIterations, resultsMap);

      pathMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, String>>());
      pathMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,2]");
      pathMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
                   
      odMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, Double>>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId, Double.valueOf(10.0));
      odMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
 
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName,xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName,xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName,xmlFileName);
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
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);        

      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setPersistZeroFlow(false);
      runner.setUseFixedConnectoidCost();     
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();
      
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByXmlId("1");
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByXmlId("0");
      
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put(node2XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get(node2XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0, 2000.0, 10.0, 1.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter, maxIterations, resultsMap);

      pathMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, String>>());
      pathMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,2]");
      pathMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
            
      odMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, Double>>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId, Double.valueOf(10.0));
      odMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId, Double.valueOf(0.0));
      PlanItIOTestHelper.compareOriginDestinationResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, odMap);
 
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath,runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, runIdDescription, csvFileName, xmlFileName);
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
      
      /* run test */
      new PlanItIOTestRunner(projectPath, description).setupAndExecuteDefaultAssignment();     
      
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
      
      /* run test */
      new PlanItIOTestRunner(projectPath, description).setupAndExecuteDefaultAssignment();     
     
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
      
      /* run test */
      new PlanItIOTestRunner(projectPath, description).setupAndExecuteDefaultAssignment();     
     
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
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);      

      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setPersistZeroFlow(false);
      runner.setUseSpeedBasedConnectoidCost();     
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();      
      
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByXmlId("1");
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByXmlId("0");
      
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put(node2XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get(node2XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0, 2000.0, 10.0, 1.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter, maxIterations, resultsMap);

      pathMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, String>>());
      pathMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,2]");
      pathMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      odMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, Double>>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId, Double.valueOf(10.0));
      odMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId, Double.valueOf(0.0));
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
      
      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.PATH, projectPath, runIdDescription, xmlFileName);         

      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setPersistZeroFlow(false);
      runner.setUseSpeedBasedConnectoidCost();     
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDto = runner.setupAndExecuteDefaultAssignment();      
      
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByXmlId("1");
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByXmlId("0");
      
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put(node2XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get(node2XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0,2000.0, 10.0, 1.0));
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(memoryOutputFormatter, maxIterations, resultsMap);

      pathMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, String>>());
      pathMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,2]");
      pathMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      odMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, Double>>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId, Double.valueOf(10.08));
      odMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId, Double.valueOf(0.0));
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
   * Test case which checks that an exception is thrown if two link segments in the same link are in the same direction.
   */
  @Test
  public void test_explanatory_link_segments_in_same_direction() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\linkSegmentsInSameDirection";
      String description = "explanatory";
      
      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);      
      
      /* run test */
      new PlanItIOTestRunner(projectPath, description).setupAndExecuteDefaultAssignment();     
     
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
      
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByXmlId("1");
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByXmlId("0");
      
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put(node2XmlId, new TreeMap<String, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get(node2XmlId).put(node1XmlId, new LinkSegmentExpectedResultsDto(1, 2, 1, 0.0769231, 1800.0, 10.0, 130.0));      
      PlanItIOTestHelper.compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId( memoryOutputFormatter, maxIterations, resultsMap);

      pathMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, String>>>());
      pathMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, String>>());
      pathMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId,"[1,2]");
      pathMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, String>());
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId,"");
      pathMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId,"");
      PlanItIOTestHelper.comparePathResultsToMemoryOutputFormatter(memoryOutputFormatter, maxIterations, pathMap);
      
      odMap.put(timePeriod, new TreeMap<Mode, Map<String, Map<String, Double>>>());
      odMap.get(timePeriod).put(mode1, new TreeMap<String, Map<String, Double>>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).put(zone1XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone1XmlId,Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone1XmlId).put(zone2XmlId, Double.valueOf(0.0769231));
      odMap.get(timePeriod).get(mode1).put(zone2XmlId, new TreeMap<String, Double>());
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone1XmlId, Double.valueOf(0.0));
      odMap.get(timePeriod).get(mode1).get(zone2XmlId).put(zone2XmlId, Double.valueOf(0.0));
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
   * Test case in which an attempt is made to change a locked formatter
   */
  @Test
  public void test_explanatory_attempt_to_change_locked_formatter() {
    String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml\\original";
    String description = "explanatory";
    String csvFileName = "Time_Period_1_2.csv";
    String odCsvFileName = "Time_Period_1_1.csv";
    String xmlFileName = "Time_Period_1.xml";
    
    String runIdDescription = "RunId_0_" + description;    
    try {
      
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, csvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.LINK, projectPath, runIdDescription, xmlFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, odCsvFileName);
      PlanItIOTestHelper.deleteFile(OutputType.OD, projectPath, runIdDescription, xmlFileName);
      
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);   
      runner.setupAndExecuteDefaultAssignment();      
      
      /* change link formatting*/
      Consumer<LinkOutputTypeConfiguration> changeLockedProperties = (linkOutputTypeConfiguration) -> {
        try {
          linkOutputTypeConfiguration.removeAllProperties();
          linkOutputTypeConfiguration.addProperty(OutputProperty.LINK_SEGMENT_ID);
          linkOutputTypeConfiguration.addProperty(OutputProperty.MODE_XML_ID);
          linkOutputTypeConfiguration.addProperty(OutputProperty.UPSTREAM_NODE_XML_ID);
          linkOutputTypeConfiguration.addProperty(OutputProperty.UPSTREAM_NODE_ID);
          linkOutputTypeConfiguration.addProperty(OutputProperty.UPSTREAM_NODE_LOCATION);
          linkOutputTypeConfiguration.addProperty(OutputProperty.DOWNSTREAM_NODE_XML_ID);
          linkOutputTypeConfiguration.addProperty(OutputProperty.DOWNSTREAM_NODE_ID);
          linkOutputTypeConfiguration.addProperty(OutputProperty.DOWNSTREAM_NODE_LOCATION);
          linkOutputTypeConfiguration.addProperty(OutputProperty.FLOW);
          linkOutputTypeConfiguration.addProperty(OutputProperty.CAPACITY_PER_LANE);
          linkOutputTypeConfiguration.addProperty(OutputProperty.NUMBER_OF_LANES);
          linkOutputTypeConfiguration.addProperty(OutputProperty.LENGTH);
          linkOutputTypeConfiguration.addProperty(OutputProperty.CALCULATED_SPEED);
          linkOutputTypeConfiguration.addProperty(OutputProperty.LINK_SEGMENT_COST);
          linkOutputTypeConfiguration.addProperty(OutputProperty.MODE_ID);
          linkOutputTypeConfiguration.addProperty(OutputProperty.MODE_XML_ID);
          linkOutputTypeConfiguration.addProperty(OutputProperty.MAXIMUM_SPEED);  
        } catch (final PlanItException e) {
          fail("testExplanatoryAttemptToChangeLockedFormatter() problem setting properties");
          LOGGER.severe(e.getMessage());
          e.printStackTrace();
        }
      };
      /* run again with updated configuration -> should throw error */
      runner.setupAndExecuteWithCustomLinkOutputConfiguration(changeLockedProperties);
      
      fail("testExplanatoryAttemptToChangeLockedFormatter() did not throw PlanItException when expected");
    } catch (Exception e) {
      LOGGER.info(e.getMessage());
    }

    try {
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, runIdDescription, odCsvFileName, xmlFileName);
      assertTrue(true);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }
}