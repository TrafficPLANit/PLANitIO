package org.goplanit.io.test.integration;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.goplanit.io.test.util.PlanItIOTestRunner;
import org.goplanit.io.test.util.PlanItInputBuilder4Testing;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.output.property.DownstreamNodeXmlIdOutputProperty;
import org.goplanit.output.property.LinkSegmentCostOutputProperty;
import org.goplanit.output.property.LinkSegmentXmlIdOutputProperty;
import org.goplanit.output.property.ModeXmlIdOutputProperty;
import org.goplanit.output.property.UpstreamNodeXmlIdOutputProperty;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test cases for initial cost tests for TraditionalStaticAssignment
 * 
 * @author gman6028, markr
 *
 */
public class InitialCostTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path testCasePath = Path.of("src","test","resources","testcases");
  private static final Path intialCostTestCasePath = Path.of(testCasePath.toString(),"initial_costs", "xml");

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(InitialCostTest.class);
    } 
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }
  
  /**
   * Tests that the values of an initial costs file are read in by start and end
   * node and registered by PlanItProject and the stored values match the expected
   * ones by link XML Id
   */
  @Test
  public void test_reading_initial_cost_values() {
    final String projectPath = Path.of(intialCostTestCasePath.toString(),"readingInitialCostValues").toString();
    String initialCostsFileLocation = Path.of(projectPath.toString(),"initial_link_segment_costs.csv").toString();
    String initialCostsFileLocationXmlId = Path.of(projectPath.toString(),"initial_link_segment_costs_xml_id.csv").toString();
    try {
      
      /* planit */
      PlanItInputBuilder4Testing planItInputBuilder = new PlanItInputBuilder4Testing(projectPath);
      final CustomPlanItProject project = new CustomPlanItProject(planItInputBuilder);
      MacroscopicNetwork network = (MacroscopicNetwork) project.createAndRegisterInfrastructureNetwork(MacroscopicNetwork.class.getCanonicalName());
      var initialCost = project.createAndRegisterInitialLinkSegmentCost(network, initialCostsFileLocation);
      
      /* reference */
      Reader in = new FileReader(initialCostsFileLocationXmlId);
      CSVParser parser = CSVParser.parse(in, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreSurroundingSpaces());
      String modeHeader = ModeXmlIdOutputProperty.NAME;
      String linkSegmentXmlIdHeader = LinkSegmentXmlIdOutputProperty.NAME;
      String costHeader = LinkSegmentCostOutputProperty.NAME;
      for (CSVRecord record : parser) {
        /* compare */
        String modeXmlId = record.get(modeHeader);
        Mode mode = network.getModes().getByXmlId(modeXmlId);
        double cost = Double.parseDouble(record.get(costHeader));
        String linkSegmentXmlId = record.get(linkSegmentXmlIdHeader);
        MacroscopicLinkSegment linkSegment = network.getLayerByMode(mode).getLinkSegments().getByXmlId(linkSegmentXmlId);
        assertEquals(cost, initialCost.getGeneralisedCost(mode, linkSegment), 0.0001);
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
    final String projectPath = Path.of(intialCostTestCasePath.toString(),"readingInitialCostValues").toString();
    String initialCostsFileLocation = Path.of(projectPath.toString(),"initial_link_segment_costs.csv").toString();
    String initialCostsFileLocationMissingRows = Path.of(projectPath.toString(),"initial_link_segment_costs1.csv").toString();

    try {
      /* planit */
      PlanItInputBuilder4Testing planItInputBuilder = new PlanItInputBuilder4Testing(projectPath);
      final CustomPlanItProject project = new CustomPlanItProject(planItInputBuilder);
      MacroscopicNetwork network = (MacroscopicNetwork) project.createAndRegisterInfrastructureNetwork(MacroscopicNetwork.class.getCanonicalName());
      var initialCost = project.createAndRegisterInitialLinkSegmentCost(network, initialCostsFileLocation);
      
      /* reference */
      Reader in = new FileReader(initialCostsFileLocationMissingRows);
      CSVParser parser = CSVParser.parse(in, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreSurroundingSpaces());
      String modeHeader = ModeXmlIdOutputProperty.NAME;
      String upstreamNodeXmlIdHeader = UpstreamNodeXmlIdOutputProperty.NAME;
      String downstreamNodeXmlIdHeader = DownstreamNodeXmlIdOutputProperty.NAME;
      String costHeader = LinkSegmentCostOutputProperty.NAME;
      for (CSVRecord record : parser) {
        /* compare */
        String modeXmlId =record.get(modeHeader);
        Mode mode = network.getModes().getByXmlId(modeXmlId);
        double cost = Double.parseDouble(record.get(costHeader));
        String upstreamNodeXmlId = record.get(upstreamNodeXmlIdHeader);
        String downstreamNodeXmlId = record.get(downstreamNodeXmlIdHeader);
        Node startNode =  network.getLayerByMode(mode).getNodes().getByXmlId(upstreamNodeXmlId);
        Node endNode = network.getLayerByMode(mode).getNodes().getByXmlId(downstreamNodeXmlId);
        final MacroscopicLinkSegment linkSegment = startNode.getLinkSegment(endNode);
        assertEquals(cost, initialCost.getGeneralisedCost(mode, linkSegment), 0.0001);
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
      final String projectPath = Path.of(intialCostTestCasePath.toString(),"readingInitialCostValuesWithLinkSegmentsMissingInInputFile").toString();
      String initialCostsFileLocation = Path.of(projectPath.toString(),"initial_link_segment_costs_external_id.csv").toString();

      String description = "readingInitialCostValuesWithMissingRows";
      
      Level oldLevel = LOGGER.getLevel();
      LOGGER.setLevel(Level.OFF);      
      
      /* run test */
      PlanItIOTestRunner runner = new PlanItIOTestRunner(projectPath, description);
      runner.setUseFixedConnectoidCost();
      runner.setPersistZeroFlow(false);
      runner.registerInitialLinkSegmentCost(initialCostsFileLocation);
      runner.setupAndExecuteDefaultAssignment();      
      
      LOGGER.setLevel(oldLevel);
      fail(
          "RunTest did not throw an exception when it should have (missing data in the input XML file in the link definition section).");
    } catch (Exception e) {
      assertTrue(true);
    }
  }
}