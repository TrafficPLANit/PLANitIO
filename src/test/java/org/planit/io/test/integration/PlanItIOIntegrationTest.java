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
  private static Logger LOGGER = Logger.getLogger(PlanItIOIntegrationTest.class.getCanonicalName());

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
   * Trivial test case which matches the description in the README.md file.
   */
  @Test
  public void testExplanatory() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml";
      String description = "explanatory";
      String csvFileName = "Time Period 1_2.csv";
      String odCsvFileName = "Time Period 1_1.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = null;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, null, description);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0,
          10.0, 2000.0, 10.0, 1.0));
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
    }
  }

  /**
   * Test case in which an attempt is made to change a locked formatter
   */
  @Test
  public void testExplanatoryAttemptToChangeLockedFormatter() {
    String projectPath = "src\\test\\resources\\testcases\\explanatory\\xml";
    String description = "explanatory";
    String csvFileName = "Time Period 1_2.csv";
    String odCsvFileName = "Time Period 1_1.csv";
    String xmlFileName = "Time Period 1.xml";
    Integer maxIterations = null;
    try {
      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignmentAttemptToChangeLockedFormatter(projectPath, null, description);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0,
          10.0, 2000.0, 10.0, 1.0));
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
      
      fail("testExplanatoryAttemptToChangeLockedFormatter() did not throw PlanItException when expected");
    } catch (Exception pe) {
      LOGGER.info(pe.getMessage());
    }

    try {
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      assertTrue(true);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
    }
  }

  /**
   * Test that a duplicate external Id for a Link Segment Type is flagged as a error
   */
  @Test
  public void testDuplicateLinkSegmentTypeExternalId() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateLinkSegmentType";
      String description = "testDuplicateLinkSegmentType";
      Integer maxIterations = 1;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
      fail("Exception for duplicate link segment type external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate external Id for a Link Segment is flagged as a error
   */
  @Test
  public void testDuplicateLinkSegmentExternalId() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateLinkSegment";
      String description = "testDuplicateLinkSegment";
      Integer maxIterations = 1;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
      fail("Exception for duplicate link segment external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate external Id for a Node is flagged as a error
   */
  @Test
  public void testDuplicateNodeExternalId() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateNode";
      String description = "testDuplicateNode";
      Integer maxIterations = 1;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
      fail("Exception for duplicate node external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate external Id for a Mode is flagged as a error
   */
  @Test
  public void testDuplicateModeExternalId() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateMode";
      String description = "testDuplicateMode";
      Integer maxIterations = 1;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
      fail("Exception for duplicate mode external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate external Id for a Zone is flagged as a error
   */
  @Test
  public void testDuplicateZoneExternalId() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateZone";
      String description = "testDuplicateZone";
      Integer maxIterations = 1;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
      fail("Exception for duplicate zone external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate external Id for a Time Period is flagged as a error
   */
  @Test
  public void testDuplicateTimePeriodExternalId() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateTimePeriod";
      String description = "testDuplicateTimePeriod";
      Integer maxIterations = 1;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
      fail("Exception for duplicate time period external Id was not thrown");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  /**
   * Test that a duplicate external Id for a Time Period is flagged as a error
   */
  @Test
  public void testDuplicateUserClassExternalId() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\duplicate_tests\\xml\\duplicateUserClass";
      String description = "testDuplicateUserClass";
      Integer maxIterations = 1;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
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
  public void testInitialCostValues() {
    String projectPath = "src\\test\\resources\\testcases\\initial_costs\\xml\\test1";
    String initialCostsFileLocation =
        "src\\test\\resources\\testcases\\initial_costs\\xml\\test1\\initial_link_segment_costs.csv";
    String initialCostsFileLocationExternalId =
        "src\\test\\resources\\testcases\\initial_costs\\xml\\test1\\initial_link_segment_costs_external_id.csv";
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
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
    }
  }

  /**
   * Test that the read in initial cost values match the expected ones when there
   * are some rows missing in the standard results file
   */
  @Test
  public void testInitialCostMissingRows() {
    String projectPath = "src\\test\\resources\\testcases\\initial_costs\\xml\\test1";
    String initialCostsFileLocation =
        "src\\test\\resources\\testcases\\initial_costs\\xml\\test1\\initial_link_segment_costs.csv";
    String initialCostsFileLocationExternalId =
        "src\\test\\resources\\testcases\\initial_costs\\xml\\test1\\initial_link_segment_costs1.csv";
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
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
    }
  }

  /**
   * Test that PlanItProject throws an exception when the initial costs file
   * references a link segment which has not been defined
   */
  @Test
  public void testInitialCostValuesMissingColumns() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\testcases\\initial_costs\\xml\\test2";
      String description = "RunId 0_testBasic1";
      Integer maxIterations = null;
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\initial_costs\\xml\\test2\\initial_link_segment_costs_external_id.csv",
          maxIterations, null, description);
      fail(
          "RunTest did not throw an exception when it should have (missing data in the input XML file in the link definition section).");
    } catch (Exception e) {
      LOGGER.info(e.getMessage());
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
   * Time Period 1 uses route A to B in the example, which has a total route cost
   * of 85 (the fifth argument in the ResultDto constructor).
   */
  @Test
  public void testBasicShortestPathAlgorithmAtoBOneInitialCostFile() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\basic\\xml\\test1";
      String description = "testBasic1";
      String csvFileName = "Time Period 1_2.csv";
      String odCsvFileName = "Time Period 1_1.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = null;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\basic\\xml\\test1\\initial_link_segment_costs.csv", maxIterations, null,
          description);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
    }
  }

  /**
   * Test that PlanItProject reads in the values of two initial costs files
   * 
   * This test case uses the example from the course notes of ITLS6102 Strategic
   * Transport Planning, Lecture 1 (Overview), the example on Page 122 of the 2019
   * course notes.
   * 
   * Time Period 1 uses route A to B in the example, which has a total route cost
   * of 85 (the fifth argument in the ResultDto constructor).
   */
  @Test
  public void testBasicShortestPathAlgorithmAtoBTwoInitialCostFiles() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\basic\\xml\\test1";
      String description = "testBasic1";
      String csvFileName = "Time Period 1_2.csv";
      String odCsvFileName = "Time Period 1_1.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = null;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\basic\\xml\\test1\\initial_link_segment_costs.csv",
          "src\\test\\resources\\testcases\\basic\\xml\\test1\\initial_link_segment_costs1.csv", 0, maxIterations, null,
          description);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
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
  public void testBasicShortestPathAlgorithmAtoC() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\basic\\xml\\test2";
      String description = "testBasic2";
      String csvFileName = "Time Period 1_2.csv";
      String odCsvFileName = "Time Period 1_1.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = null;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, null, description);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 1, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,
          10, 2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1, 12,
          22, 2000, 12, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 30, 2000, 8, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 13, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 12, new LinkSegmentExpectedResultsDto(12, 13, 1,
          47, 77, 2000, 47, 1));
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
      
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
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
  public void testBasicShortestPathAlgorithmAtoD() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\basic\\xml\\test3";
      String description = "testBasic3";
      String csvFileName = "Time Period 1_2.csv";
      String odCsvFileName = "Time Period 1_1.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = null;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, null, description);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();
      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);
      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 1, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,
          10, 2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 8, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 7, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,
          22, 2000, 12, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 9, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 9).put((long) 8, new LinkSegmentExpectedResultsDto(8, 9, 1, 20,
          42, 2000, 20, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1, 12,
          54, 2000, 12, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 7, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 12, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,
          59, 2000, 5, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 14, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 9, new LinkSegmentExpectedResultsDto(9, 14, 1, 10,
          69, 2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 77, 2000, 8, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 15, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 14, new LinkSegmentExpectedResultsDto(14, 15, 1,
          10, 87, 2000, 10, 1));
      resultsMap.get(timePeriod).get(mode1).put((long) 20, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).get((long) 20).put((long) 15, new LinkSegmentExpectedResultsDto(15, 20, 1,
          21, 108, 2000, 21, 1));
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
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
   * Time Period 1 uses route A to B in the example, which has a total route cost
   * of 85 (the fifth argument in the ResultDto constructor). Time Period 2 uses
   * route A to C in the example, which has a total route cost of 77. Time Period
   * 3 uses route A to D in the example, which has a total route cost of 108.
   */
  @Test
  public void testBasicShortestPathAlgorithmAtoBThreeTimePeriods() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\basic\\xml\\test13";
      String description = "testBasic13";
      String csvFileName1 = "Time Period 1_2.csv";
      String odCsvFileName1 = "Time Period 1_1.csv";
      String csvFileName2 = "Time Period 2_2.csv";
      String odCsvFileName2 = "Time Period 2_1.csv";
      String csvFileName3 = "Time Period 3_2.csv";
      String odCsvFileName3 = "Time Period 3_1.csv";
      String xmlFileName1 = "Time Period 1.xml";
      String xmlFileName2 = "Time Period 2.xml";
      String xmlFileName3 = "Time Period 3.xml";
      Integer maxIterations = null;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, null, description);
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
          38, 2000, 8, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 1, 10,
          10, 2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 5).put((long) 4, new LinkSegmentExpectedResultsDto(4, 5, 1, 10,
          20, 2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 6).put((long) 1, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,
          30, 2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 7).put((long) 12, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,
          77, 2000, 5, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 8).put((long) 7, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,
          60, 2000, 12, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 10).put((long) 5, new LinkSegmentExpectedResultsDto(5, 10, 1,
          10, 48, 2000, 10, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1,
          12, 72, 2000, 12, 1));
      resultsMap.get(timePeriod1).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 85, 2000, 8, 1));

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
  
      TimePeriod timePeriod2 = testOutputDto.getC().getTimePeriodByExternalId((long) 1);
      resultsMap.put(timePeriod2, new TreeMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>());
      resultsMap.get(timePeriod2).put(mode1, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 6, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 11, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).put((long) 13, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod2).get(mode1).get((long) 6).put((long) 1, new LinkSegmentExpectedResultsDto(1, 6, 1, 10,
          10, 2000, 10, 1));
      resultsMap.get(timePeriod2).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1,
          12, 22, 2000, 12, 1));
      resultsMap.get(timePeriod2).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 30, 2000, 8, 1));
      resultsMap.get(timePeriod2).get(mode1).get((long) 13).put((long) 12, new LinkSegmentExpectedResultsDto(12, 13, 1,
          47, 77, 2000, 47, 1));

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
          10, 2000, 10, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 7).put((long) 12, new LinkSegmentExpectedResultsDto(12, 7, 1, 5,
          59, 2000, 5, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 8).put((long) 7, new LinkSegmentExpectedResultsDto(7, 8, 1, 12,
          22, 2000, 12, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 9).put((long) 8, new LinkSegmentExpectedResultsDto(8, 9, 1, 20,
          42, 2000, 20, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 11).put((long) 6, new LinkSegmentExpectedResultsDto(6, 11, 1,
          12, 54, 2000, 12, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12, 1,
          8, 77, 2000, 8, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 14).put((long) 9, new LinkSegmentExpectedResultsDto(9, 14, 1,
          10, 69, 2000, 10, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 15).put((long) 14, new LinkSegmentExpectedResultsDto(14, 15, 1,
          10, 87, 2000, 10, 1));
      resultsMap.get(timePeriod3).get(mode1).get((long) 20).put((long) 15, new LinkSegmentExpectedResultsDto(15, 20, 1,
          21, 108, 2000, 21, 1));
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName3,
          xmlFileName3);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
    }
  }

  /**
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the first route choice example from the Traditional Static Assignment Route
   * Choice Equilibration Test cases.docx document.
   */
  @Test
  public void testRouteChoiceCompareWithOmniTRANS1() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\test1";
      String description = "testRouteChoice1";
      String csvFileName = "Time Period 1_500.csv";
      String odCsvFileName = "Time Period 1_499.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
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
          0.015, 30, 2000, 1, 66.6666667));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 5, new LinkSegmentExpectedResultsDto(5, 1, 1000,
          0.0103125, 300.9375938, 2000, 1, 96.969697));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3, new LinkSegmentExpectedResultsDto(3, 2, 2000,
          0.015, 240, 2000, 1, 66.6666667));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 4, new LinkSegmentExpectedResultsDto(4, 2, 1000,
          0.0103125, 260.3125313, 2000, 1, 96.969697));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1, new LinkSegmentExpectedResultsDto(1, 3, 2000,
          0.09, 210, 1000, 1, 11.1111111));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 6, new LinkSegmentExpectedResultsDto(6, 3, 1000,
          0.0103125, 270.6250313, 2000, 1, 96.969697));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 13, new LinkSegmentExpectedResultsDto(13, 4, 1000,
          0.01, 250.0000313, 20000, 1, 99.9996875));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 15, new LinkSegmentExpectedResultsDto(15, 5, 1000,
          0.01, 290.6250938, 20000, 1, 99.9996875));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 12, new LinkSegmentExpectedResultsDto(12, 6, 1000,
          0.01, 280.6250625, 20000, 1, 99.9996875));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 2, new LinkSegmentExpectedResultsDto(2, 11, 1000,
          0.0103125, 311.2500938, 2000, 1, 96.969697));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 3, new LinkSegmentExpectedResultsDto(3, 14, 1000,
          0.0103125, 321.5625938, 2000, 1, 96.969697));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 1, new LinkSegmentExpectedResultsDto(1, 16, 1000,
          0.0103125, 331.8750938, 2000, 1, 96.969697));
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
    }
  }

  /**
   * /**
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the second route choice example from the Traditional Static Assignment Route
   * Choice Equilibration Test cases.docx document.
   */
  @Test
  public void testRouteChoiceCompareWithOmniTRANS2() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\test2";
      String description = "testRouteChoice2";
      String csvFileName = "Time Period 1_500.csv";
      String odCsvFileName = "Time Period 1_499.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
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
          0.025, 90, 3600, 1, 40));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 295.2,
          0.0333944, 314.6687712, 1200, 2, 59.8903352));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1, new LinkSegmentExpectedResultsDto(1, 3, 1425.6,
          0.0332658, 372.1408694, 1200, 1, 30.0609344));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1879.2,
          0.0667837, 214.8106084, 1200, 1, 14.9737025));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 2, new LinkSegmentExpectedResultsDto(2, 4, 295.2,
          0.0333944, 324.526934, 1200, 2, 59.8903352));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 1425.6,
          0.0332658, 419.7548048, 1200, 1, 30.0609344));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 4, new LinkSegmentExpectedResultsDto(4, 12, 3600,
          0.025, 304.8106084, 3600, 1, 40));
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
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
  public void testRouteChoice2InitialCostsOneIteration() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCostsOneIteration";
      String description = "testRouteChoice2initialCosts";
      String csvFileName = "Time Period 1_1.csv";
      String odCsvFileName = "Time Period 1_0.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = 1;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCostsOneIteration\\initial_link_segment_costs.csv",
          null, 0, maxIterations, 0.0, null, description);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
    }
  }

  /**
   * This test runs the same network using one iteration with different initial
   * costs for each time, checking that the results are different for each time
   * period.
   */
  @Test
  public void testRouteChoice2InitialCostsOneIterationThreeTimePeriods() {
    try {
      String projectPath =
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCostsOneIterationThreeTimePeriods";
      String description = "test2initialCostsOneIterationThreeTimePeriods";
      String csvFileName1 = "Time Period 1_1.csv";
      String odCsvFileName1 = "Time Period 1_0.csv";
      String csvFileName2 = "Time Period 2_1.csv";
      String odCsvFileName2 = "Time Period 2_0.csv";
      String csvFileName3 = "Time Period 3_1.csv";
      String odCsvFileName3 = "Time Period 3_0.csv";
      String xmlFileName1 = "Time Period 1.xml";
      String xmlFileName2 = "Time Period 2.xml";
      String xmlFileName3 = "Time Period 3.xml";
      Integer maxIterations = 1;
      Map<Long, String> initialLinkSegmentLocationsPerTimePeriod = new HashMap<Long, String>();
      initialLinkSegmentLocationsPerTimePeriod.put((long) 0,
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCostsOneIterationThreeTimePeriods\\initial_link_segment_costs_time_period_1.csv");
      initialLinkSegmentLocationsPerTimePeriod.put((long) 1,
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCostsOneIterationThreeTimePeriods\\initial_link_segment_costs_time_period_2.csv");
      initialLinkSegmentLocationsPerTimePeriod.put((long) 2,
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCostsOneIterationThreeTimePeriods\\initial_link_segment_costs_time_period_3.csv");

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, initialLinkSegmentLocationsPerTimePeriod, maxIterations,
          0.0, null, description);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName3,
          xmlFileName3);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
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
  public void testRouteChoice2InitialCostsOneIterationExternalIds() {
    try {
      String projectPath =
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCostsOneIterationExternalIds";
      String description = "testRouteChoice2initialCosts";
      String csvFileName = "Time Period 1_1.csv";
      String odCsvFileName = "Time Period 1_0.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = 1;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCostsOneIterationExternalIds\\initial_link_segment_costs.csv",
          null, 0, maxIterations, 0.0, null, description);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
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
  public void testRouteChoice2InitialCosts500Iterations() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCosts500iterations";
      String description = "testRouteChoice2initialCosts";
      String csvFileName = "Time Period 1_500.csv";
      String odCsvFileName = "Time Period 1_499.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = 500;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCosts500iterations\\initial_link_segment_costs.csv",
          null, 0, maxIterations, 0.0, null, description);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
    }
  }

  /**
   * This test runs the same network with three time periods with different initial
   * costs for each, running the test for 500 iterations.
   */
  @Test
  public void testRouteChoice2InitialCosts500IterationsThreeTimePeriods() {
    try {
      String projectPath =
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCosts500IterationsThreeTimePeriods";
      String description = "test2initialCosts500IterationsThreeTimePeriods";
      String csvFileName1 = "Time Period 1_500.csv";
      String odCsvFileName1 = "Time Period 1_499.csv";
      String csvFileName2 = "Time Period 2_500.csv";
      String odCsvFileName2 = "Time Period 2_499.csv";
      String csvFileName3 = "Time Period 3_500.csv";
      String odCsvFileName3 = "Time Period 3_499.csv";
      String xmlFileName1 = "Time Period 1.xml";
      String xmlFileName2 = "Time Period 2.xml";
      String xmlFileName3 = "Time Period 3.xml";
      Integer maxIterations = 500;
      Map<Long, String> initialLinkSegmentLocationsPerTimePeriod = new HashMap<Long, String>();
      initialLinkSegmentLocationsPerTimePeriod.put((long) 0,
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCosts500IterationsThreeTimePeriods\\initial_link_segment_costs_time_period_1.csv");
      initialLinkSegmentLocationsPerTimePeriod.put((long) 1,
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCosts500IterationsThreeTimePeriods\\initial_link_segment_costs_time_period_2.csv");
      initialLinkSegmentLocationsPerTimePeriod.put((long) 2,
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCosts500IterationsThreeTimePeriods\\initial_link_segment_costs_time_period_3.csv");

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath, initialLinkSegmentLocationsPerTimePeriod, maxIterations,
          0.0, null, description);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName3,
          xmlFileName3);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName3,
          xmlFileName3);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
    }
  }

  /**
   * This test check that PlanItProject reads the initial costs from a file
   * correctly, and outputs them after 500 iterations.
   * 
   * The test input initial costs file uses Link Segment External Id to identify
   * link segments
   */
  @Test
  public void testRouteChoice2InitialCosts500IterationsExternalIds() {
    try {
      String projectPath =
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCosts500iterationsExternalIds";
      String description = "testRouteChoice2initialCosts";
      String csvFileName = "Time Period 1_500.csv";
      String odCsvFileName = "Time Period 1_499.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = 500;

      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\route_choice\\xml\\test2initialCosts500iterationsExternalIds\\initial_link_segment_costs.csv",
          null, 0, maxIterations, 0.0, null, description);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
    }
  }

  /**
   * Test of results for TraditionalStaticAssignment for simple test case using
   * the third route choice example from the Traditional Static Assignment Route
   * Choice Equilibration Test cases.docx document.
   */
  @Test
  public void testRouteChoiceCompareWithOmniTRANS3() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\test3";
      String description = "testRouteChoice3";
      String csvFileName = "Time Period 1_500.csv";
      String odCsvFileName = "Time Period 1_499.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
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
          0.03, 240, 8000, 2, 66.6666667));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 3952,
          0.0239029, 550.566099, 5000, 2, 83.6718462));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1, new LinkSegmentExpectedResultsDto(1, 3, 4048,
          0.0531495, 456.2280829, 3000, 2, 37.6297041));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2, new LinkSegmentExpectedResultsDto(2, 3, 3952,
          0.0295286, 666.954771, 4000, 2, 67.7310119));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 3856,
          0.0472937, 1701.235183, 3000, 2, 42.2888931));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 3, new LinkSegmentExpectedResultsDto(3, 5, 4144,
          0.2043143, 1519.774245, 2000, 2, 9.7888406));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 4, new LinkSegmentExpectedResultsDto(4, 5, 3856,
          0.1581746, 2306.581184, 2000, 2, 12.6442576));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 5, new LinkSegmentExpectedResultsDto(5, 12, 8000,
          2.58, 22946.58118, 2000, 2, 0.7751938));
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
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
  public void testRouteChoiceCompareWithOmniTRANS4() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\test4";
      String description = "testRouteChoice4";
      String csvFileName = "Time Period 1_500.csv";
      String odCsvFileName = "Time Period 1_499.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
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
          0.029, 0.0174, 1500, 2.9, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 9, new LinkSegmentExpectedResultsDto(9, 12, 0.6,
          0.029, 0.0348, 1500, 2.9, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 12, new LinkSegmentExpectedResultsDto(12, 11,
          482.4, 0.0301605, 14.58482622, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12,
          482.4, 0.0301605, 29.13485243, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 12, new LinkSegmentExpectedResultsDto(12, 16, 483,
          0.0100538, 33.99102332, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 16, new LinkSegmentExpectedResultsDto(16, 12, 483,
          0.0100538, 38.84719421, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 9, new LinkSegmentExpectedResultsDto(9, 13, 0.6,
          0.01, 38.85319421, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 9).put((long) 13, new LinkSegmentExpectedResultsDto(13, 9, 0.6,
          0.01, 38.85919421, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 10, new LinkSegmentExpectedResultsDto(10, 11,
          17.6, 0.03, 39.38719422, 1500, 3, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 10).put((long) 11, new LinkSegmentExpectedResultsDto(11, 10,
          17.6, 0.03, 39.91519422, 1500, 3, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 10, new LinkSegmentExpectedResultsDto(10, 14,
          17.6, 0.01, 40.09119422, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 10).put((long) 14, new LinkSegmentExpectedResultsDto(14, 10,
          17.6, 0.01, 40.26719422, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 11, new LinkSegmentExpectedResultsDto(11, 15, 500,
          0.0100617, 45.29830657, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 15, new LinkSegmentExpectedResultsDto(15, 11, 500,
          0.0100617, 50.32941893, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 1, new LinkSegmentExpectedResultsDto(1, 5, 899.4,
          0.0106463, 59.90467441, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 5, new LinkSegmentExpectedResultsDto(5, 1, 899.4,
          0.0106463, 69.47992989, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1087.4,
          0.0102428, 80.61584489, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4, new LinkSegmentExpectedResultsDto(4, 1, 1087.4,
          0.0102428, 91.75175988, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1012,
          0.0099323, 101.804863, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, new LinkSegmentExpectedResultsDto(2, 1, 1012,
          0.0099323, 111.857966, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 2, new LinkSegmentExpectedResultsDto(2, 6, 1582.4,
          0.0161926, 137.4801958, 1500, 1, 61.756766));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 6, new LinkSegmentExpectedResultsDto(6, 2, 1582.4,
          0.0161926, 163.1024255, 1500, 1, 61.756766));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2, new LinkSegmentExpectedResultsDto(2, 3, 994.4,
          0.0109657, 174.0082393, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3, new LinkSegmentExpectedResultsDto(3, 2, 994.4,
          0.0109657, 184.9140531, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 3, new LinkSegmentExpectedResultsDto(3, 7, 1900,
          0.0228712, 228.3178046, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 7, new LinkSegmentExpectedResultsDto(7, 3, 1900,
          0.0228712, 271.7215562, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 905.6,
          0.0106643, 281.3754383, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4, new LinkSegmentExpectedResultsDto(4, 3, 905.6,
          0.0106643, 291.0293204, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 4, new LinkSegmentExpectedResultsDto(4, 8, 1617,
          0.0167522, 318.0915022, 1500, 1, 59.693666));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 8, new LinkSegmentExpectedResultsDto(8, 4, 1617,
          0.0167522, 345.153684, 1500, 1, 59.693666));
      resultsMap.get(timePeriod).get(mode1).get((long) 23).put((long) 16, new LinkSegmentExpectedResultsDto(16, 23, 483,
          0.0200001, 354.8137105, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 23, new LinkSegmentExpectedResultsDto(23, 16, 483,
          0.0200001, 364.473737, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 23, new LinkSegmentExpectedResultsDto(23, 8, 1617,
          0.0200068, 396.8247653, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod).get(mode1).get((long) 23).put((long) 8, new LinkSegmentExpectedResultsDto(8, 23, 1617,
          0.0200068, 429.1757937, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod).get(mode1).get((long) 21).put((long) 13, new LinkSegmentExpectedResultsDto(13, 21, 0.6,
          0.02, 429.1877937, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 21, new LinkSegmentExpectedResultsDto(21, 13, 0.6,
          0.02, 429.1997937, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 21, new LinkSegmentExpectedResultsDto(21, 5, 899.4,
          0.0200007, 447.1883822, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod).get(mode1).get((long) 21).put((long) 5, new LinkSegmentExpectedResultsDto(5, 21, 899.4,
          0.0200007, 465.1769707, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod).get(mode1).get((long) 22).put((long) 14, new LinkSegmentExpectedResultsDto(14, 22,
          17.6, 0.02, 465.5289707, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 22, new LinkSegmentExpectedResultsDto(22, 14,
          17.6, 0.02, 465.8809707, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 22, new LinkSegmentExpectedResultsDto(22, 6,
          1582.4, 0.0200063, 497.5388914, 10000, 1, 49.98433));
      resultsMap.get(timePeriod).get(mode1).get((long) 22).put((long) 6, new LinkSegmentExpectedResultsDto(6, 22,
          1582.4, 0.0200063, 529.1968121, 10000, 1, 49.98433));
      resultsMap.get(timePeriod).get(mode1).get((long) 24).put((long) 15, new LinkSegmentExpectedResultsDto(15, 24, 500,
          0.0200001, 539.1968436, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 24, new LinkSegmentExpectedResultsDto(24, 15, 500,
          0.0200001, 549.1968751, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 24, new LinkSegmentExpectedResultsDto(24, 7, 1900,
          0.020013, 587.2215839, 10000, 1, 49.967441));
      resultsMap.get(timePeriod).get(mode1).get((long) 24).put((long) 7, new LinkSegmentExpectedResultsDto(7, 24, 1900,
          0.020013, 625.2462927, 10000, 1, 49.967441));
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
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
  public void testRouteChoiceCompareWithOmniTRANS4UsingTwoTimePeriods() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\test42";
      String description = "testRouteChoice42";
      String csvFileName1 = "Time Period 1_500.csv";
      String odCsvFileName1 = "Time Period 1_499.csv";
      String csvFileName2 = "Time Period 2_500.csv";
      String odCsvFileName2 = "Time Period 2_499.csv";
      String xmlFileName1 = "Time Period 1.xml";
      String xmlFileName2 = "Time Period 2.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
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
          0.029, 0.0174, 1500, 2.9, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 12).put((long) 9, new LinkSegmentExpectedResultsDto(9, 12, 0.6,
          0.029, 0.0348, 1500, 2.9, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 11).put((long) 12, new LinkSegmentExpectedResultsDto(12, 11,
          482.4, 0.0301605, 14.58482622, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod1).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12,
          482.4, 0.0301605, 29.13485243, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod1).get(mode1).get((long) 16).put((long) 12, new LinkSegmentExpectedResultsDto(12, 16,
          483, 0.0100538, 33.99102332, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod1).get(mode1).get((long) 12).put((long) 16, new LinkSegmentExpectedResultsDto(16, 12,
          483, 0.0100538, 38.84719421, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod1).get(mode1).get((long) 13).put((long) 9, new LinkSegmentExpectedResultsDto(9, 13, 0.6,
          0.01, 38.85319421, 1500, 1, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 9).put((long) 13, new LinkSegmentExpectedResultsDto(13, 9, 0.6,
          0.01, 38.85919421, 1500, 1, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 11).put((long) 10, new LinkSegmentExpectedResultsDto(10, 11,
          17.6, 0.03, 39.38719422, 1500, 3, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 10).put((long) 11, new LinkSegmentExpectedResultsDto(11, 10,
          17.6, 0.03, 39.91519422, 1500, 3, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 14).put((long) 10, new LinkSegmentExpectedResultsDto(10, 14,
          17.6, 0.01, 40.09119422, 1500, 1, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 10).put((long) 14, new LinkSegmentExpectedResultsDto(14, 10,
          17.6, 0.01, 40.26719422, 1500, 1, 100));
      resultsMap.get(timePeriod1).get(mode1).get((long) 15).put((long) 11, new LinkSegmentExpectedResultsDto(11, 15,
          500, 0.0100617, 45.29830657, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod1).get(mode1).get((long) 11).put((long) 15, new LinkSegmentExpectedResultsDto(15, 11,
          500, 0.0100617, 50.32941893, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod1).get(mode1).get((long) 5).put((long) 1, new LinkSegmentExpectedResultsDto(1, 5, 899.4,
          0.0106463, 59.90467441, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 5, new LinkSegmentExpectedResultsDto(5, 1, 899.4,
          0.0106463, 69.47992989, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1087.4,
          0.0102428, 80.61584489, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 4, new LinkSegmentExpectedResultsDto(4, 1, 1087.4,
          0.0102428, 91.75175988, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1012,
          0.0099323, 101.804863, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod1).get(mode1).get((long) 1).put((long) 2, new LinkSegmentExpectedResultsDto(2, 1, 1012,
          0.0099323, 111.857966, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod1).get(mode1).get((long) 6).put((long) 2, new LinkSegmentExpectedResultsDto(2, 6, 1582.4,
          0.0161926, 137.4801958, 1500, 1, 61.756766));
      resultsMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 6, new LinkSegmentExpectedResultsDto(6, 2, 1582.4,
          0.0161926, 163.1024255, 1500, 1, 61.756766));
      resultsMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 2, new LinkSegmentExpectedResultsDto(2, 3, 994.4,
          0.0109657, 174.0082393, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod1).get(mode1).get((long) 2).put((long) 3, new LinkSegmentExpectedResultsDto(3, 2, 994.4,
          0.0109657, 184.9140531, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod1).get(mode1).get((long) 7).put((long) 3, new LinkSegmentExpectedResultsDto(3, 7, 1900,
          0.0228712, 228.3178046, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 7, new LinkSegmentExpectedResultsDto(7, 3, 1900,
          0.0228712, 271.7215562, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 905.6,
          0.0106643, 281.3754383, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod1).get(mode1).get((long) 3).put((long) 4, new LinkSegmentExpectedResultsDto(4, 3, 905.6,
          0.0106643, 291.0293204, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod1).get(mode1).get((long) 8).put((long) 4, new LinkSegmentExpectedResultsDto(4, 8, 1617,
          0.0167522, 318.0915022, 1500, 1, 59.693666));
      resultsMap.get(timePeriod1).get(mode1).get((long) 4).put((long) 8, new LinkSegmentExpectedResultsDto(8, 4, 1617,
          0.0167522, 345.153684, 1500, 1, 59.693666));
      resultsMap.get(timePeriod1).get(mode1).get((long) 23).put((long) 16, new LinkSegmentExpectedResultsDto(16, 23,
          483, 0.0200001, 354.8137105, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod1).get(mode1).get((long) 16).put((long) 23, new LinkSegmentExpectedResultsDto(23, 16,
          483, 0.0200001, 364.473737, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod1).get(mode1).get((long) 8).put((long) 23, new LinkSegmentExpectedResultsDto(23, 8, 1617,
          0.0200068, 396.8247653, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod1).get(mode1).get((long) 23).put((long) 8, new LinkSegmentExpectedResultsDto(8, 23, 1617,
          0.0200068, 429.1757937, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod1).get(mode1).get((long) 21).put((long) 13, new LinkSegmentExpectedResultsDto(13, 21,
          0.6, 0.02, 429.1877937, 10000, 1, 50));
      resultsMap.get(timePeriod1).get(mode1).get((long) 13).put((long) 21, new LinkSegmentExpectedResultsDto(21, 13,
          0.6, 0.02, 429.1997937, 10000, 1, 50));
      resultsMap.get(timePeriod1).get(mode1).get((long) 5).put((long) 21, new LinkSegmentExpectedResultsDto(21, 5,
          899.4, 0.0200007, 447.1883822, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod1).get(mode1).get((long) 21).put((long) 5, new LinkSegmentExpectedResultsDto(5, 21,
          899.4, 0.0200007, 465.1769707, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod1).get(mode1).get((long) 22).put((long) 14, new LinkSegmentExpectedResultsDto(14, 22,
          17.6, 0.02, 465.5289707, 10000, 1, 50));
      resultsMap.get(timePeriod1).get(mode1).get((long) 14).put((long) 22, new LinkSegmentExpectedResultsDto(22, 14,
          17.6, 0.02, 465.8809707, 10000, 1, 50));
      resultsMap.get(timePeriod1).get(mode1).get((long) 6).put((long) 22, new LinkSegmentExpectedResultsDto(22, 6,
          1582.4, 0.0200063, 497.5388914, 10000, 1, 49.98433));
      resultsMap.get(timePeriod1).get(mode1).get((long) 22).put((long) 6, new LinkSegmentExpectedResultsDto(6, 22,
          1582.4, 0.0200063, 529.1968121, 10000, 1, 49.98433));
      resultsMap.get(timePeriod1).get(mode1).get((long) 24).put((long) 15, new LinkSegmentExpectedResultsDto(15, 24,
          500, 0.0200001, 539.1968436, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod1).get(mode1).get((long) 15).put((long) 24, new LinkSegmentExpectedResultsDto(24, 15,
          500, 0.0200001, 549.1968751, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod1).get(mode1).get((long) 7).put((long) 24, new LinkSegmentExpectedResultsDto(24, 7, 1900,
          0.020013, 587.2215839, 10000, 1, 49.967441));
      resultsMap.get(timePeriod1).get(mode1).get((long) 24).put((long) 7, new LinkSegmentExpectedResultsDto(7, 24, 1900,
          0.020013, 625.2462927, 10000, 1, 49.967441));

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
          0.029, 0.0174, 1500, 2.9, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 12).put((long) 9, new LinkSegmentExpectedResultsDto(9, 12, 0.6,
          0.029, 0.0348, 1500, 2.9, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 11).put((long) 12, new LinkSegmentExpectedResultsDto(12, 11,
          482.4, 0.0301605, 14.58482622, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod2).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12,
          482.4, 0.0301605, 29.13485243, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod2).get(mode1).get((long) 16).put((long) 12, new LinkSegmentExpectedResultsDto(12, 16,
          483, 0.0100538, 33.99102332, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod2).get(mode1).get((long) 12).put((long) 16, new LinkSegmentExpectedResultsDto(16, 12,
          483, 0.0100538, 38.84719421, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod2).get(mode1).get((long) 13).put((long) 9, new LinkSegmentExpectedResultsDto(9, 13, 0.6,
          0.01, 38.85319421, 1500, 1, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 9).put((long) 13, new LinkSegmentExpectedResultsDto(13, 9, 0.6,
          0.01, 38.85919421, 1500, 1, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 11).put((long) 10, new LinkSegmentExpectedResultsDto(10, 11,
          17.6, 0.03, 39.38719422, 1500, 3, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 10).put((long) 11, new LinkSegmentExpectedResultsDto(11, 10,
          17.6, 0.03, 39.91519422, 1500, 3, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 14).put((long) 10, new LinkSegmentExpectedResultsDto(10, 14,
          17.6, 0.01, 40.09119422, 1500, 1, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 10).put((long) 14, new LinkSegmentExpectedResultsDto(14, 10,
          17.6, 0.01, 40.26719422, 1500, 1, 100));
      resultsMap.get(timePeriod2).get(mode1).get((long) 15).put((long) 11, new LinkSegmentExpectedResultsDto(11, 15,
          500, 0.0100617, 45.29830657, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod2).get(mode1).get((long) 11).put((long) 15, new LinkSegmentExpectedResultsDto(15, 11,
          500, 0.0100617, 50.32941893, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod2).get(mode1).get((long) 5).put((long) 1, new LinkSegmentExpectedResultsDto(1, 5, 899.4,
          0.0106463, 59.90467441, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 5, new LinkSegmentExpectedResultsDto(5, 1, 899.4,
          0.0106463, 69.47992989, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1087.4,
          0.0102428, 80.61584489, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 4, new LinkSegmentExpectedResultsDto(4, 1, 1087.4,
          0.0102428, 91.75175988, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1012,
          0.0099323, 101.804863, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod2).get(mode1).get((long) 1).put((long) 2, new LinkSegmentExpectedResultsDto(2, 1, 1012,
          0.0099323, 111.857966, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod2).get(mode1).get((long) 6).put((long) 2, new LinkSegmentExpectedResultsDto(2, 6, 1582.4,
          0.0161926, 137.4801958, 1500, 1, 61.756766));
      resultsMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 6, new LinkSegmentExpectedResultsDto(6, 2, 1582.4,
          0.0161926, 163.1024255, 1500, 1, 61.756766));
      resultsMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 2, new LinkSegmentExpectedResultsDto(2, 3, 994.4,
          0.0109657, 174.0082393, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod2).get(mode1).get((long) 2).put((long) 3, new LinkSegmentExpectedResultsDto(3, 2, 994.4,
          0.0109657, 184.9140531, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod2).get(mode1).get((long) 7).put((long) 3, new LinkSegmentExpectedResultsDto(3, 7, 1900,
          0.0228712, 228.3178046, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 7, new LinkSegmentExpectedResultsDto(7, 3, 1900,
          0.0228712, 271.7215562, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 905.6,
          0.0106643, 281.3754383, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod2).get(mode1).get((long) 3).put((long) 4, new LinkSegmentExpectedResultsDto(4, 3, 905.6,
          0.0106643, 291.0293204, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod2).get(mode1).get((long) 8).put((long) 4, new LinkSegmentExpectedResultsDto(4, 8, 1617,
          0.0167522, 318.0915022, 1500, 1, 59.693666));
      resultsMap.get(timePeriod2).get(mode1).get((long) 4).put((long) 8, new LinkSegmentExpectedResultsDto(8, 4, 1617,
          0.0167522, 345.153684, 1500, 1, 59.693666));
      resultsMap.get(timePeriod2).get(mode1).get((long) 23).put((long) 16, new LinkSegmentExpectedResultsDto(16, 23,
          483, 0.0200001, 354.8137105, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod2).get(mode1).get((long) 16).put((long) 23, new LinkSegmentExpectedResultsDto(23, 16,
          483, 0.0200001, 364.473737, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod2).get(mode1).get((long) 8).put((long) 23, new LinkSegmentExpectedResultsDto(23, 8, 1617,
          0.0200068, 396.8247653, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod2).get(mode1).get((long) 23).put((long) 8, new LinkSegmentExpectedResultsDto(8, 23, 1617,
          0.0200068, 429.1757937, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod2).get(mode1).get((long) 21).put((long) 13, new LinkSegmentExpectedResultsDto(13, 21,
          0.6, 0.02, 429.1877937, 10000, 1, 50));
      resultsMap.get(timePeriod2).get(mode1).get((long) 13).put((long) 21, new LinkSegmentExpectedResultsDto(21, 13,
          0.6, 0.02, 429.1997937, 10000, 1, 50));
      resultsMap.get(timePeriod2).get(mode1).get((long) 5).put((long) 21, new LinkSegmentExpectedResultsDto(21, 5,
          899.4, 0.0200007, 447.1883822, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod2).get(mode1).get((long) 21).put((long) 5, new LinkSegmentExpectedResultsDto(5, 21,
          899.4, 0.0200007, 465.1769707, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod2).get(mode1).get((long) 22).put((long) 14, new LinkSegmentExpectedResultsDto(14, 22,
          17.6, 0.02, 465.5289707, 10000, 1, 50));
      resultsMap.get(timePeriod2).get(mode1).get((long) 14).put((long) 22, new LinkSegmentExpectedResultsDto(22, 14,
          17.6, 0.02, 465.8809707, 10000, 1, 50));
      resultsMap.get(timePeriod2).get(mode1).get((long) 6).put((long) 22, new LinkSegmentExpectedResultsDto(22, 6,
          1582.4, 0.0200063, 497.5388914, 10000, 1, 49.98433));
      resultsMap.get(timePeriod2).get(mode1).get((long) 22).put((long) 6, new LinkSegmentExpectedResultsDto(6, 22,
          1582.4, 0.0200063, 529.1968121, 10000, 1, 49.98433));
      resultsMap.get(timePeriod2).get(mode1).get((long) 24).put((long) 15, new LinkSegmentExpectedResultsDto(15, 24,
          500, 0.0200001, 539.1968436, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod2).get(mode1).get((long) 15).put((long) 24, new LinkSegmentExpectedResultsDto(24, 15,
          500, 0.0200001, 549.1968751, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod2).get(mode1).get((long) 7).put((long) 24, new LinkSegmentExpectedResultsDto(24, 7, 1900,
          0.020013, 587.2215839, 10000, 1, 49.967441));
      resultsMap.get(timePeriod2).get(mode1).get((long) 24).put((long) 7, new LinkSegmentExpectedResultsDto(7, 24, 1900,
          0.020013, 625.2462927, 10000, 1, 49.967441));
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName2,
          xmlFileName2);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName1,
          xmlFileName1);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName2,
          xmlFileName2);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
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
  public void testRouteChoiceCompareWithOmniTRANS4UsingRawMatrixToSetODDemands() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\test4raw";
      String description = "testRouteChoice4raw";
      String csvFileName = "Time Period 1_500.csv";
      String odCsvFileName = "Time Period 1_499.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
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
          0.029, 0.0174, 1500, 2.9, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 9, new LinkSegmentExpectedResultsDto(9, 12, 0.6,
          0.029, 0.0348, 1500, 2.9, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 12, new LinkSegmentExpectedResultsDto(12, 11,
          482.4, 0.0301605, 14.58482622, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12,
          482.4, 0.0301605, 29.13485243, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 12, new LinkSegmentExpectedResultsDto(12, 16, 483,
          0.0100538, 33.99102332, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 16, new LinkSegmentExpectedResultsDto(16, 12, 483,
          0.0100538, 38.84719421, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 9, new LinkSegmentExpectedResultsDto(9, 13, 0.6,
          0.01, 38.85319421, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 9).put((long) 13, new LinkSegmentExpectedResultsDto(13, 9, 0.6,
          0.01, 38.85919421, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 10, new LinkSegmentExpectedResultsDto(10, 11,
          17.6, 0.03, 39.38719422, 1500, 3, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 10).put((long) 11, new LinkSegmentExpectedResultsDto(11, 10,
          17.6, 0.03, 39.91519422, 1500, 3, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 10, new LinkSegmentExpectedResultsDto(10, 14,
          17.6, 0.01, 40.09119422, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 10).put((long) 14, new LinkSegmentExpectedResultsDto(14, 10,
          17.6, 0.01, 40.26719422, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 11, new LinkSegmentExpectedResultsDto(11, 15, 500,
          0.0100617, 45.29830657, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 15, new LinkSegmentExpectedResultsDto(15, 11, 500,
          0.0100617, 50.32941893, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 1, new LinkSegmentExpectedResultsDto(1, 5, 899.4,
          0.0106463, 59.90467441, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 5, new LinkSegmentExpectedResultsDto(5, 1, 899.4,
          0.0106463, 69.47992989, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1087.4,
          0.0102428, 80.61584489, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4, new LinkSegmentExpectedResultsDto(4, 1, 1087.4,
          0.0102428, 91.75175988, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1012,
          0.0099323, 101.804863, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, new LinkSegmentExpectedResultsDto(2, 1, 1012,
          0.0099323, 111.857966, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 2, new LinkSegmentExpectedResultsDto(2, 6, 1582.4,
          0.0161926, 137.4801958, 1500, 1, 61.756766));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 6, new LinkSegmentExpectedResultsDto(6, 2, 1582.4,
          0.0161926, 163.1024255, 1500, 1, 61.756766));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2, new LinkSegmentExpectedResultsDto(2, 3, 994.4,
          0.0109657, 174.0082393, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3, new LinkSegmentExpectedResultsDto(3, 2, 994.4,
          0.0109657, 184.9140531, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 3, new LinkSegmentExpectedResultsDto(3, 7, 1900,
          0.0228712, 228.3178046, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 7, new LinkSegmentExpectedResultsDto(7, 3, 1900,
          0.0228712, 271.7215562, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 905.6,
          0.0106643, 281.3754383, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4, new LinkSegmentExpectedResultsDto(4, 3, 905.6,
          0.0106643, 291.0293204, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 4, new LinkSegmentExpectedResultsDto(4, 8, 1617,
          0.0167522, 318.0915022, 1500, 1, 59.693666));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 8, new LinkSegmentExpectedResultsDto(8, 4, 1617,
          0.0167522, 345.153684, 1500, 1, 59.693666));
      resultsMap.get(timePeriod).get(mode1).get((long) 23).put((long) 16, new LinkSegmentExpectedResultsDto(16, 23, 483,
          0.0200001, 354.8137105, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 23, new LinkSegmentExpectedResultsDto(23, 16, 483,
          0.0200001, 364.473737, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 23, new LinkSegmentExpectedResultsDto(23, 8, 1617,
          0.0200068, 396.8247653, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod).get(mode1).get((long) 23).put((long) 8, new LinkSegmentExpectedResultsDto(8, 23, 1617,
          0.0200068, 429.1757937, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod).get(mode1).get((long) 21).put((long) 13, new LinkSegmentExpectedResultsDto(13, 21, 0.6,
          0.02, 429.1877937, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 21, new LinkSegmentExpectedResultsDto(21, 13, 0.6,
          0.02, 429.1997937, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 21, new LinkSegmentExpectedResultsDto(21, 5, 899.4,
          0.0200007, 447.1883822, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod).get(mode1).get((long) 21).put((long) 5, new LinkSegmentExpectedResultsDto(5, 21, 899.4,
          0.0200007, 465.1769707, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod).get(mode1).get((long) 22).put((long) 14, new LinkSegmentExpectedResultsDto(14, 22,
          17.6, 0.02, 465.5289707, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 22, new LinkSegmentExpectedResultsDto(22, 14,
          17.6, 0.02, 465.8809707, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 22, new LinkSegmentExpectedResultsDto(22, 6,
          1582.4, 0.0200063, 497.5388914, 10000, 1, 49.98433));
      resultsMap.get(timePeriod).get(mode1).get((long) 22).put((long) 6, new LinkSegmentExpectedResultsDto(6, 22,
          1582.4, 0.0200063, 529.1968121, 10000, 1, 49.98433));
      resultsMap.get(timePeriod).get(mode1).get((long) 24).put((long) 15, new LinkSegmentExpectedResultsDto(15, 24, 500,
          0.0200001, 539.1968436, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 24, new LinkSegmentExpectedResultsDto(24, 15, 500,
          0.0200001, 549.1968751, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 24, new LinkSegmentExpectedResultsDto(24, 7, 1900,
          0.020013, 587.2215839, 10000, 1, 49.967441));
      resultsMap.get(timePeriod).get(mode1).get((long) 24).put((long) 7, new LinkSegmentExpectedResultsDto(7, 24, 1900,
          0.020013, 625.2462927, 10000, 1, 49.967441));
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
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
  public void testRouteChoiceCompareWithOmniTRANS4UsingRawMatrixWithPlusSignAsSeparatorToSetODDemands() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\test4raw2";
      String description = "testRouteChoice4raw2";
      String csvFileName = "Time Period 1_500.csv";
      String odCsvFileName = "Time Period 1_499.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = 500;

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, null, description);
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
          0.029, 0.0174, 1500, 2.9, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 9, new LinkSegmentExpectedResultsDto(9, 12, 0.6,
          0.029, 0.0348, 1500, 2.9, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 12, new LinkSegmentExpectedResultsDto(12, 11,
          482.4, 0.0301605, 14.58482622, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 11, new LinkSegmentExpectedResultsDto(11, 12,
          482.4, 0.0301605, 29.13485243, 1500, 3, 99.4679928));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 12, new LinkSegmentExpectedResultsDto(12, 16, 483,
          0.0100538, 33.99102332, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 16, new LinkSegmentExpectedResultsDto(16, 12, 483,
          0.0100538, 38.84719421, 1500, 1, 99.4653552));
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 9, new LinkSegmentExpectedResultsDto(9, 13, 0.6,
          0.01, 38.85319421, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 9).put((long) 13, new LinkSegmentExpectedResultsDto(13, 9, 0.6,
          0.01, 38.85919421, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 10, new LinkSegmentExpectedResultsDto(10, 11,
          17.6, 0.03, 39.38719422, 1500, 3, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 10).put((long) 11, new LinkSegmentExpectedResultsDto(11, 10,
          17.6, 0.03, 39.91519422, 1500, 3, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 10, new LinkSegmentExpectedResultsDto(10, 14,
          17.6, 0.01, 40.09119422, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 10).put((long) 14, new LinkSegmentExpectedResultsDto(14, 10,
          17.6, 0.01, 40.26719422, 1500, 1, 100));
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 11, new LinkSegmentExpectedResultsDto(11, 15, 500,
          0.0100617, 45.29830657, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod).get(mode1).get((long) 11).put((long) 15, new LinkSegmentExpectedResultsDto(15, 11, 500,
          0.0100617, 50.32941893, 1500, 1, 99.3865031));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 1, new LinkSegmentExpectedResultsDto(1, 5, 899.4,
          0.0106463, 59.90467441, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 5, new LinkSegmentExpectedResultsDto(5, 1, 899.4,
          0.0106463, 69.47992989, 1500, 1, 93.9295781));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1087.4,
          0.0102428, 80.61584489, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 4, new LinkSegmentExpectedResultsDto(4, 1, 1087.4,
          0.0102428, 91.75175988, 1500, 0.9, 87.8665119));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1012,
          0.0099323, 101.804863, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod).get(mode1).get((long) 1).put((long) 2, new LinkSegmentExpectedResultsDto(2, 1, 1012,
          0.0099323, 111.857966, 1500, 0.9, 90.613182));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 2, new LinkSegmentExpectedResultsDto(2, 6, 1582.4,
          0.0161926, 137.4801958, 1500, 1, 61.756766));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 6, new LinkSegmentExpectedResultsDto(6, 2, 1582.4,
          0.0161926, 163.1024255, 1500, 1, 61.756766));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 2, new LinkSegmentExpectedResultsDto(2, 3, 994.4,
          0.0109657, 174.0082393, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 3, new LinkSegmentExpectedResultsDto(3, 2, 994.4,
          0.0109657, 184.9140531, 1500, 1, 91.1933155));
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 3, new LinkSegmentExpectedResultsDto(3, 7, 1900,
          0.0228712, 228.3178046, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 7, new LinkSegmentExpectedResultsDto(7, 3, 1900,
          0.0228712, 271.7215562, 1500, 1, 43.7230914));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 905.6,
          0.0106643, 281.3754383, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 4, new LinkSegmentExpectedResultsDto(4, 3, 905.6,
          0.0106643, 291.0293204, 1500, 1, 93.7709887));
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 4, new LinkSegmentExpectedResultsDto(4, 8, 1617,
          0.0167522, 318.0915022, 1500, 1, 59.693666));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 8, new LinkSegmentExpectedResultsDto(8, 4, 1617,
          0.0167522, 345.153684, 1500, 1, 59.693666));
      resultsMap.get(timePeriod).get(mode1).get((long) 23).put((long) 16, new LinkSegmentExpectedResultsDto(16, 23, 483,
          0.0200001, 354.8137105, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod).get(mode1).get((long) 16).put((long) 23, new LinkSegmentExpectedResultsDto(23, 16, 483,
          0.0200001, 364.473737, 10000, 1, 49.9998639));
      resultsMap.get(timePeriod).get(mode1).get((long) 8).put((long) 23, new LinkSegmentExpectedResultsDto(23, 8, 1617,
          0.0200068, 396.8247653, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod).get(mode1).get((long) 23).put((long) 8, new LinkSegmentExpectedResultsDto(8, 23, 1617,
          0.0200068, 429.1757937, 10000, 1, 49.9829143));
      resultsMap.get(timePeriod).get(mode1).get((long) 21).put((long) 13, new LinkSegmentExpectedResultsDto(13, 21, 0.6,
          0.02, 429.1877937, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 13).put((long) 21, new LinkSegmentExpectedResultsDto(21, 13, 0.6,
          0.02, 429.1997937, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 5).put((long) 21, new LinkSegmentExpectedResultsDto(21, 5, 899.4,
          0.0200007, 447.1883822, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod).get(mode1).get((long) 21).put((long) 5, new LinkSegmentExpectedResultsDto(5, 21, 899.4,
          0.0200007, 465.1769707, 10000, 1, 49.9983642));
      resultsMap.get(timePeriod).get(mode1).get((long) 22).put((long) 14, new LinkSegmentExpectedResultsDto(14, 22,
          17.6, 0.02, 465.5289707, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 14).put((long) 22, new LinkSegmentExpectedResultsDto(22, 14,
          17.6, 0.02, 465.8809707, 10000, 1, 50));
      resultsMap.get(timePeriod).get(mode1).get((long) 6).put((long) 22, new LinkSegmentExpectedResultsDto(22, 6,
          1582.4, 0.0200063, 497.5388914, 10000, 1, 49.98433));
      resultsMap.get(timePeriod).get(mode1).get((long) 22).put((long) 6, new LinkSegmentExpectedResultsDto(6, 22,
          1582.4, 0.0200063, 529.1968121, 10000, 1, 49.98433));
      resultsMap.get(timePeriod).get(mode1).get((long) 24).put((long) 15, new LinkSegmentExpectedResultsDto(15, 24, 500,
          0.0200001, 539.1968436, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod).get(mode1).get((long) 15).put((long) 24, new LinkSegmentExpectedResultsDto(24, 15, 500,
          0.0200001, 549.1968751, 10000, 1, 49.9998438));
      resultsMap.get(timePeriod).get(mode1).get((long) 7).put((long) 24, new LinkSegmentExpectedResultsDto(24, 7, 1900,
          0.020013, 587.2215839, 10000, 1, 49.967441));
      resultsMap.get(timePeriod).get(mode1).get((long) 24).put((long) 7, new LinkSegmentExpectedResultsDto(7, 24, 1900,
          0.020013, 625.2462927, 10000, 1, 49.967441));
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
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
  public void testRouteChoiceCompareWithOmniTRANS5() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\test5";
      String description = "testRouteChoice5";
      String csvFileName = "Time Period 1_500.csv";
      String odCsvFileName = "Time Period 1_499.csv";
      String xmlFileName = "Time Period 1.xml";
      Integer maxIterations = 500;

      TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters = (physicalNetwork,
          bprLinkTravelTimeCost, inputBuilderListener) -> {
        MacroscopicLinkSegmentType macroscopiclinkSegmentType = inputBuilderListener.getLinkSegmentTypeByExternalId(
            (long) 1);
        Mode mode = inputBuilderListener.getModeByExternalId((long) 2);
        bprLinkTravelTimeCost.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
      };

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, maxIterations, 0.0, setCostParameters, description);
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
          0.0370117, 111.03515625, 3600.0, 1.0, 27.0184697));
      resultsMap.get(timePeriod).get(mode1).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 6,
          0.0447625, 360.470425354635, 1200.0, 2.0, 44.6802634));
      resultsMap.get(timePeriod).get(mode1).get((long) 3).put((long) 1, new LinkSegmentExpectedResultsDto(1, 3, 1068,
          0.0360526, 399.241706545674, 1200.0, 1.0, 27.7372551));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1926,
          0.0719659, 249.166142789938, 1200.0, 1.0, 13.8954751));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 2, new LinkSegmentExpectedResultsDto(2, 4, 6,
          0.0447625, 360.739551669332, 1200.0, 2.0, 44.6802634));
      resultsMap.get(timePeriod).get(mode1).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 1068,
          0.0360526, 437.743861422015, 1200.0, 1.0, 27.7372551));
      resultsMap.get(timePeriod).get(mode1).get((long) 12).put((long) 4, new LinkSegmentExpectedResultsDto(4, 12, 3000,
          0.0370117, 360.201299039938, 3600.0, 1.0, 27.0184697));

      resultsMap.get(timePeriod).put(mode2, new TreeMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).get(mode2).put((long) 1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 3, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 4, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 12, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).get((long) 1).put((long) 11, new LinkSegmentExpectedResultsDto(11, 1, 1500,
          0.0636732, 95.5098040283147, 3600.0, 1.0, 15.705194));
      resultsMap.get(timePeriod).get(mode2).get((long) 2).put((long) 1, new LinkSegmentExpectedResultsDto(1, 2, 1086,
          0.0609332, 257.397693017887, 1200.0, 2.0, 32.8228128));
      resultsMap.get(timePeriod).get(mode2).get((long) 3).put((long) 1, new LinkSegmentExpectedResultsDto(1, 3, 414,
          0.0613639, 349.067549843147, 1200.0, 1.0, 16.296231));
      resultsMap.get(timePeriod).get(mode2).get((long) 4).put((long) 2, new LinkSegmentExpectedResultsDto(2, 4, 1086,
          0.0609332, 323.775777979144, 1200.0, 2.0, 32.8228128));
      resultsMap.get(timePeriod).get(mode2).get((long) 4).put((long) 3, new LinkSegmentExpectedResultsDto(3, 4, 414,
          0.0613639, 374.359321707149, 1200.0, 1.0, 16.296231));
      resultsMap.get(timePeriod).get(mode2).get((long) 12).put((long) 4, new LinkSegmentExpectedResultsDto(4, 12, 1500,
          0.0636732, 191.019608056629, 3600.0, 1.0, 15.705194));
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
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
  public void testRouteChoiceCompareWithOmniTRANS5IdentifyLinksById() {
    try {
      String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\test5IdentifyLinksByLinkId";
      String description = "testRouteChoice5";
      String csvFileName = "Time Period 1_500.csv";
      String odCsvFileName = "Time Period 1_499.csv";
      String xmlFileName = "Time Period 1.xml";
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
          linkOutputTypeConfiguration.removeProperty(OutputProperty.TOTAL_COST_TO_END_NODE);
          linkOutputTypeConfiguration.removeProperty(OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID);
          linkOutputTypeConfiguration.removeProperty(OutputProperty.UPSTREAM_NODE_EXTERNAL_ID);
        } catch (PlanItException e) {
          fail(e.getMessage());
        }
      };

      TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = PlanItIOTestHelper
          .setupAndExecuteAssignment(projectPath, setOutputTypeConfigurationProperties, maxIterations, 0.0,
              setCostParameters, description);
      MemoryOutputFormatter memoryOutputFormatter = testOutputDto.getA();

      Mode mode1 = testOutputDto.getC().getModeByExternalId((long) 1);
      Mode mode2 = testOutputDto.getC().getModeByExternalId((long) 2);
      TimePeriod timePeriod = testOutputDto.getC().getTimePeriodByExternalId((long) 0);

      SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, LinkSegmentExpectedResultsDto>>> resultsMap =
          new TreeMap<TimePeriod, SortedMap<Mode, SortedMap<Long, LinkSegmentExpectedResultsDto>>>();
      resultsMap.put(timePeriod, new TreeMap<Mode, SortedMap<Long, LinkSegmentExpectedResultsDto>>());
      resultsMap.get(timePeriod).put(mode1, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode1).put((long) 0, new LinkSegmentExpectedResultsDto(11, 1, 3000, 0.0370117,
          111.03515625, 3600.0, 1.0, 27.0184697));
      resultsMap.get(timePeriod).get(mode1).put((long) 1, new LinkSegmentExpectedResultsDto(1, 4, 1926, 0.0719659,
          249.166142789938, 1200.0, 1.0, 13.8954751));
      resultsMap.get(timePeriod).get(mode1).put((long) 2, new LinkSegmentExpectedResultsDto(4, 12, 3000, 0.0370117,
          360.201299039938, 3600.0, 1.0, 27.0184697));
      resultsMap.get(timePeriod).get(mode1).put((long) 3, new LinkSegmentExpectedResultsDto(1, 2, 6, 0.0447625,
          360.470425354635, 1200.0, 2.0, 44.6802634));
      resultsMap.get(timePeriod).get(mode1).put((long) 4, new LinkSegmentExpectedResultsDto(2, 4, 6, 0.0447625,
          360.739551669332, 1200.0, 2.0, 44.6802634));
      resultsMap.get(timePeriod).get(mode1).put((long) 5, new LinkSegmentExpectedResultsDto(1, 3, 1068, 0.0360526,
          399.241706545674, 1200.0, 1.0, 27.7372551));
      resultsMap.get(timePeriod).get(mode1).put((long) 6, new LinkSegmentExpectedResultsDto(3, 4, 1068, 0.0360526,
          437.743861422015, 1200.0, 1.0, 27.7372551));
      resultsMap.get(timePeriod).put(mode2, new TreeMap<Long, LinkSegmentExpectedResultsDto>());
      resultsMap.get(timePeriod).get(mode2).put((long) 0, new LinkSegmentExpectedResultsDto(11, 1, 1500, 0.0636732,
          95.5098040283147, 3600.0, 1.0, 15.705194));
      resultsMap.get(timePeriod).get(mode2).put((long) 2, new LinkSegmentExpectedResultsDto(4, 12, 1500, 0.0636732,
          191.019608056629, 3600.0, 1.0, 15.705194));
      resultsMap.get(timePeriod).get(mode2).put((long) 3, new LinkSegmentExpectedResultsDto(1, 2, 1086, 0.0609332,
          257.397693017887, 1200.0, 2.0, 32.8228128));
      resultsMap.get(timePeriod).get(mode2).put((long) 4, new LinkSegmentExpectedResultsDto(2, 4, 1086, 0.0609332,
          323.775777979144, 1200.0, 2.0, 32.8228128));
      resultsMap.get(timePeriod).get(mode2).put((long) 5, new LinkSegmentExpectedResultsDto(1, 3, 414, 0.0613639,
          349.067549843147, 1200.0, 1.0, 16.296231));
      resultsMap.get(timePeriod).get(mode2).put((long) 6, new LinkSegmentExpectedResultsDto(3, 4, 414, 0.0613639,
          374.359321707149, 1200.0, 1.0, 16.296231));
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

      runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, odCsvFileName,
          xmlFileName);
      runFileEqualAssertionsAndCleanUp(OutputType.PATH, projectPath, "RunId 0_" + description, csvFileName,
          xmlFileName);
    } catch (final Exception ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      fail(ex.getMessage());
    }
  }

}
