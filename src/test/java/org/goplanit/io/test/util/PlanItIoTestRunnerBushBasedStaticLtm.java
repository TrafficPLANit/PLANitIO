package org.goplanit.io.test.util;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.TrafficAssignmentConfigurator;
import org.goplanit.assignment.ltm.sltm.input.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.common.StaticLtmType;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;

import java.util.logging.Logger;

/**
 * Helper class used by unit tests to conduct test runs with various configuration options set
 *
 * @author markr
 *
 */
public class PlanItIoTestRunnerBushBasedStaticLtm extends PlanItIoTestRunner {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanItIoTestRunnerBushBasedStaticLtm.class.getCanonicalName());

  /**
   * Factory method to construct Static LTM bush based assignment for testing
   *
   * @param project to use
   * @return static LTM bush based assignment configurator
   */
  public static TrafficAssignmentConfigurator<?> createTrafficAssignmentConfigurator(CustomPlanItProject project) {
    try {
        var taConfigurator =
                project.createAndRegisterTrafficAssignment(
                        TrafficAssignment.SLTM,
                        project.demands.getFirst(),
                        project.zonings.getFirst(),
                        project.physicalNetworks.getFirst());

        // steady state configurator
        taConfigurator.createAndRegisterPhysicalCost(AbstractPhysicalCost.STEADY_STATE);
        var sLtm = ((StaticLtmConfigurator)taConfigurator);

        // defaults 5/2024, but set explicitly so tests will not break if defaults change
        sLtm.setType(StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED);
        sLtm.createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);
        return taConfigurator;
    }catch (Exception ignored){
    }
    return null;
  }

  /**
   * Constructor
   *
   * @param inputPath to use
   * @param outputPath to use
   * @param description to use
   */
  public PlanItIoTestRunnerBushBasedStaticLtm(
          String inputPath, String outputPath, String description) {
    super(inputPath, outputPath, description, PlanItIoTestRunnerBushBasedStaticLtm::createTrafficAssignmentConfigurator);
  }

  /**
   * Constructor. Applies default traditional static assignment
   *
   * @param projectPath to use (both input and output path)
   * @param description to use
   */
  public PlanItIoTestRunnerBushBasedStaticLtm(String projectPath, String description) {
    this(projectPath, projectPath, description);
  }

}