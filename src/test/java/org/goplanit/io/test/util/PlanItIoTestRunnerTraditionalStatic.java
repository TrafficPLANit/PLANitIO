package org.goplanit.io.test.util;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.TrafficAssignmentConfigurator;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.project.CustomPlanItProject;

import java.util.logging.Logger;

/**
 * Helper class used by unit tests to conduct test runs with various configuration options set
 *
 * @author markr
 *
 */
public class PlanItIoTestRunnerTraditionalStatic extends PlanItIoTestRunner {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanItIoTestRunnerTraditionalStatic.class.getCanonicalName());

  public static TrafficAssignmentConfigurator<?> createTrafficAssignmentConfigurator(CustomPlanItProject project) {
    try {
      var taConfigurator =
              project.createAndRegisterTrafficAssignment(
                      TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT,
                      project.demands.getFirst(),
                      project.zonings.getFirst(), project.physicalNetworks.getFirst());

      /* Physical cost - BPR */
      var physicalCostConfigurator = taConfigurator.createAndRegisterPhysicalCost(AbstractPhysicalCost.BPR);
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
  public PlanItIoTestRunnerTraditionalStatic(
          String inputPath, String outputPath, String description) {
    super(inputPath, outputPath, description, PlanItIoTestRunnerTraditionalStatic::createTrafficAssignmentConfigurator);
  }

  /**
   * Constructor. Applies default traditional static assignment
   *
   * @param projectPath to use (both input and output path)
   * @param description to use
   */
  public PlanItIoTestRunnerTraditionalStatic(String projectPath, String description) {
    this(projectPath, projectPath, description);
  }

}