package org.goplanit.io.test.util;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.TrafficAssignmentConfigurator;
import org.goplanit.assignment.ltm.sltm.input.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.common.StaticLtmType;
import org.goplanit.choice.ChoiceModel;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.path.choice.PathChoice;
import org.goplanit.path.choice.StochasticPathChoiceConfigurator;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;

import java.util.logging.Logger;

/**
 * Helper class used by unit tests to conduct test runs with various configuration options set
 *
 * @author markr
 *
 */
public class PlanItIoTestRunnerPathBasedStaticLtm extends PlanItIoTestRunner {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanItIoTestRunnerPathBasedStaticLtm.class.getCanonicalName());

  /**
   * Factory method to construct Static LTM path based assignment for testing
   *
   * @param project to use
   * @return static LTM path based assignment configurator
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
        var physicalCostConfigurator = taConfigurator.createAndRegisterPhysicalCost(AbstractPhysicalCost.STEADY_STATE);
        var sLtm = ((StaticLtmConfigurator)taConfigurator);

        // defaults 5/2024, but set explicitly so tests will not break if defaults change
        sLtm.setType(StaticLtmType.PATH_BASED);
        sLtm.createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);
        var pathChoice = (StochasticPathChoiceConfigurator) sLtm.createAndRegisterPathChoice(PathChoice.STOCHASTIC);
        pathChoice.createAndRegisterChoiceModel(ChoiceModel.MNL);
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
  public PlanItIoTestRunnerPathBasedStaticLtm(
          String inputPath, String outputPath, String description) {
    super(inputPath, outputPath, description, PlanItIoTestRunnerPathBasedStaticLtm::createTrafficAssignmentConfigurator);
  }

  /**
   * Constructor. Applies default traditional static assignment
   *
   * @param projectPath to use (both input and output path)
   * @param description to use
   */
  public PlanItIoTestRunnerPathBasedStaticLtm(String projectPath, String description) {
    this(projectPath, projectPath, description);
  }

}