package org.goplanit.io.test.integration.sLTM;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.ltm.sltm.StaticLtmConfigurator;
import org.goplanit.io.test.integration.TestBase;
import org.goplanit.io.test.util.PlanItIOTestHelper;
import org.goplanit.io.test.util.PlanItInputBuilder4Testing;
import org.goplanit.io.test.util.PlanItIoTestRunner;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.test.LinkSegmentExpectedResultsDto;
import org.goplanit.utils.test.TestOutputDto;
import org.goplanit.utils.time.TimePeriod;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.util.TreeMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * JUnit test case for static Link Transmission Model, specifically certain more complex
 * setups with IO which are adaptations from - for example - the RouteChoiceTests for the traditional assignment
 *
 * @author markr
 *
 */
public class sLTMAssignmentTest extends TestBase {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path SLTM_PATH = Path.of(TEST_CASE_PATH.toString(),"sltm", "xml");

  private static final Path SLTM_INPUT_PATH = Path.of(SLTM_PATH.toString(),"_input");

  private static final Path SLTM_SIMO_MISO_INPUT_PATH = Path.of(SLTM_INPUT_PATH.toString(),"SIMOMISO");


  private static final Path SLTM_SIMO_MISO_THREE_TP =
          Path.of(SLTM_SIMO_MISO_INPUT_PATH.toString(),"three_time_periods");

  private static final Path SLTM_SIMO_MISO_ONE_TP =
          Path.of(SLTM_SIMO_MISO_INPUT_PATH.toString(),"one_time_period");


  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLTMAssignmentTest.class);
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
   * This test checks that PlanItProject reads the initial costs from a file
   * correctly, and outputs them after 500 iterations using sLTM traffic assignment
   *
   * The test input initial costs file uses Link Segment Id to identify link
   * segments
   *
   * TODO: <a href="https://github.com/TrafficPLANit/PLANit/issues/117">
   *   only 2 of three possible routes in choice set, this is incorrect, should be able to better create paths to avoid this</a>
   */
  @Test
  public void test_2_SIMO_MISO_route_choice_single_mode_with_initial_costs_and_500_iterations() {
    try {
      final String inputPath = SLTM_SIMO_MISO_ONE_TP.toString();
      final String projectPath = Path.of(SLTM_PATH.toString(),"SIMOMISOSltm1ModeInitialCosts500Iterations").toString();
      String description = "sltm";
      String csvFileName = "Time_Period_1_500.csv";
      String odCsvFileName = "Time_Period_1_499.csv";
      String xmlFileName = "Time_Period_1.xml";
      Integer maxIterations = 500;

      String runIdDescription = "RunId_0_" + description;
      PlanItIOTestHelper.deleteLinkFiles(projectPath, runIdDescription, csvFileName, xmlFileName);
      PlanItIOTestHelper.deleteOdFiles(projectPath, runIdDescription, odCsvFileName, xmlFileName);
      PlanItIOTestHelper.deletePathFiles(projectPath, runIdDescription, csvFileName, xmlFileName);

      /* run test */
      PlanItIoTestRunner runner = new PlanItIoTestRunner(inputPath, projectPath, description, TrafficAssignment.SLTM);
      runner.setMaxIterations(maxIterations);
      runner.setGapFunctionEpsilonGap(0.0);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      runner.registerInitialLinkSegmentCost(Path.of(inputPath,"initial_link_segment_costs.csv").toString());

      var sLtm = ((StaticLtmConfigurator)runner.getRawTrafficAssignmentConfigurator());
      //sLtm.activateDetailedLogging(true);

      // link results show non-zero flows on multiple paths, but path result only shows a single path? Debug to see what is going on
      //sLtm.addTrackOdsForLogging(IdMapperType.XML, Pair.of("1","2"));

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
  @Disabled // not yet finalised --> todo: continue with this
  @Test
  public void test_2_SIMO_MISO_route_choice_single_mode_with_initial_costs_and_500_iterations_and_three_time_periods() {
    try {
      final String inputPath = SLTM_SIMO_MISO_THREE_TP.toString();
      final String projectPath = Path.of(SLTM_PATH.toString(),"SIMOMISOSltm1ModeInitCost500Iterations3TimePeriods").toString();
      String description = "sltm";
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
      
      /* run test with sLTM*/
      PlanItIoTestRunner runner = new PlanItIoTestRunner(inputPath, projectPath, description, TrafficAssignment.SLTM);
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



}