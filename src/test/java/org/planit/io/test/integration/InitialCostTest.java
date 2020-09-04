package org.planit.io.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileReader;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.io.test.util.PlanItIOTestHelper;
import org.planit.logging.Logging;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.property.DownstreamNodeExternalIdOutputProperty;
import org.planit.output.property.LinkCostOutputProperty;
import org.planit.output.property.LinkSegmentExternalIdOutputProperty;
import org.planit.output.property.ModeExternalIdOutputProperty;
import org.planit.output.property.UpstreamNodeExternalIdOutputProperty;
import org.planit.project.CustomPlanItProject;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;

/**
 * JUnit test cases for initial cost tests for TraditionalStaticAssignment
 * 
 * @author gman6028, markr
 *
 */
public class InitialCostTest {

  /** the logger */
  private static Logger LOGGER = null;

  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(InitialCostTest.class);
    } 
  }

  @AfterClass
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
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
      PhysicalNetwork<?,?,?> physicalNetwork =
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

      PhysicalNetwork<?,?,?> physicalNetwork =
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
        final LinkSegment linkSegment = physicalNetwork.linkSegments.getByStartAndEndNodeId(startId, endId);
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
      
      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);      
      PlanItIOTestHelper.setupAndExecuteAssignment(projectPath,
          "src\\test\\resources\\testcases\\initial_costs\\xml\\readingInitialCostValuesWithLinkSegmentsMissingInInputFile\\initial_link_segment_costs_external_id.csv",
          maxIterations, null, description, true, false);
      LOGGER.setLevel(oldLevel);
      fail(
          "RunTest did not throw an exception when it should have (missing data in the input XML file in the link definition section).");
    } catch (Exception e) {
      assertTrue(true);
    }
  }
}