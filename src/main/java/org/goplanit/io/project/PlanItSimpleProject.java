package org.goplanit.io.project;

import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.TrafficAssignmentConfigurator;
import org.goplanit.demands.Demands;
import org.goplanit.io.input.PlanItInputBuilder;
import org.goplanit.io.output.formatter.PlanItOutputFormatter;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.output.formatter.OutputFormatter;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.zoning.Zoning;

/**
 * Wrapper around PLANitProject with most common defaults automatically activated. Limitations
 * include:
 * - Only allows for a single assignment
 * - Only allows for a single zoning system, network and demands input
 *
 * Advanced users who want to utilize all the flexibility of PLANit should instead use
 * CustomPLANitProject in the PLANit core.
 *
 * Default configuration for this type of project:
 * <ol>
 * <li>Use the native output formatter (PLANitIO format)</li>
 * <li>Use a macroscopic network</li>
 * <li>Use the native input parser (PLANitIO format)</li>
 * <li>The assignment will by default persist link outputs and OD outputs (no paths)</li>
 * <li>Parsing of the inputs occurs after configuration of all other components to quickly identify
 * user configuration errors</li>
 * </ol>
 *
 * @author markr
 *
 */
public class PlanItSimpleProject extends CustomPlanItProject {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanItSimpleProject.class.getCanonicalName());

  /**
   * Simple project registers native PLANitXML output formatter by default which is stored in this
   * reference
   */
  private PlanItOutputFormatter defaultOutputFormatter = null;

  /** the network parsed upon creation of the project */
  private MacroscopicNetwork network = null;
  /** the zoning parsed upon creation of the project */
  private Zoning zoning = null;
  /** the demands parsed upon creation of the project */
  private Demands demands = null;

  /**
   * Initialize this simple project with as many components as possible directly after its inception
   * here
   */
  private void initialiseSimpleProject() {
    try {
      // register the default Output formatter as a formatter that is available
      defaultOutputFormatter = (PlanItOutputFormatter) this.createAndRegisterOutputFormatter(
          OutputFormatter.PLANIT_OUTPUT_FORMATTER);
      // parse a macroscopic network representation + register on assignment
      network = (MacroscopicNetwork) this.createAndRegisterInfrastructureNetwork(MacroscopicNetwork.class.getCanonicalName());
      // parse the zoning system + register on assignment
      zoning = this.createAndRegisterZoning(network);
      // parse the demands + register on assignment
      demands = this.createAndRegisterDemands(zoning, network);
    } catch (final PlanItException e) {
      LOGGER.log(Level.SEVERE, "could not instantiate default settings for project", e);
    }
  }

  // Public

  /**
   * Base constructor for simple project which adopts the PlanItIO input/output format. It is
   * assumed
   * all input files are in the current working directory
   * 
   * @throws PlanItException thrown in case the default input builder cannot be created
   *
   */
  public PlanItSimpleProject() throws PlanItException {
    // use the default input builder with the current path as the project path
    super(new PlanItInputBuilder(System.getProperty("user.dir")));
    initialiseSimpleProject();
  }

  /**
   * Base constructor for simple project which adopts the PlanItIO input/output format
   *
   * @param projectPath to retrieve the files from
   * @throws PlanItException thrown if error
   */
  public PlanItSimpleProject(final String projectPath) throws PlanItException {
    // use the default input builder
    super(new PlanItInputBuilder(projectPath));
    LOGGER.info(LoggingUtils.createProjectPrefix(this.id) + String.format("searching for input files in: %s", Paths.get(
        projectPath).toAbsolutePath().toString()));
    initialiseSimpleProject();
  }

  /**
   * Default constructor without explicit project path (use default)
   * 
   * @param planItInputBuilder the input builder
   */
  public PlanItSimpleProject(final PlanItInputBuilder planItInputBuilder) {
    super(planItInputBuilder);
    initialiseSimpleProject();
  }

  /**
   * On a simple project we only allow a single assignment to be registered. This is verified here.
   * If multiple assignments
   * are required within the same project, then a simple project cannot be used. Registration of a
   * traffic assignment type
   * also includes parsing the network, zoning, and demands that are registered alongside the chosen
   * assignment method
   *
   * @param trafficAssignmentType the traffic assignment type to be used
   * @return trafficAssignmentConfigurator to configure this traffic assignment instance
   * @throws PlanItException thrown if error
   */
  public TrafficAssignmentConfigurator<? extends TrafficAssignment> createAndRegisterTrafficAssignment(
      final String trafficAssignmentType) throws PlanItException {
            
    PlanItException.throwIf(this.assignmentBuilders.isEmpty(), "this type of PLANit project only allows a single assignment per project");
    return super.createAndRegisterTrafficAssignment(trafficAssignmentType, demands, zoning, network);
  }

  /**
   * Disallow the use of the generic create and register traffic assignment because a simple project
   * automatically determines its demands, zoning, and network
   *
   * @param trafficAssignmentType the traffic assignment type
   * @param theDemands the demands
   * @param theZoning the zoning
   * @param theNetwork the network
   */
  @Override
  public TrafficAssignmentConfigurator<? extends TrafficAssignment> createAndRegisterTrafficAssignment(
      final String trafficAssignmentType,
      final Demands theDemands,
      final Zoning theZoning,
      final LayeredNetwork<?,?> theNetwork)
      throws PlanItException {
    throw new PlanItException(
        "a simple project only allows to create and register a traffic assignment by type only, other inputs are automatically collected");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void executeAllTrafficAssignments() throws PlanItException {
    if (super.assignmentBuilders.isEmpty()) {
      super.executeAllTrafficAssignments();
    } else {
      LOGGER.info(LoggingUtils.createProjectPrefix(this.id)
          + "no traffic assignment has been registered yet, ignoring execution");
    }
  }

  /**
   * Collect the default output formatter for PLANit simple project which is the native XMLFormatter
   * 
   * @return defaultOutputformatter
   */
  public PlanItOutputFormatter getDefaultOutputFormatter() {
    return this.defaultOutputFormatter;
  }

  /**
   * Return the current network object
   * 
   * @return the current network
   */
  public MacroscopicNetwork getNetwork() {
    return network;
  }

  /**
   * Return the current Zoning object
   * 
   * @return the current zoning object
   */
  public Zoning getZoning() {
    return zoning;
  }

  /**
   * Return the current Demands object
   * 
   * @return the current demands
   */
  public Demands getDemands() {
    return demands;
  }

//  /**
//   * Retrieve a list of the xml Ids of all registered time periods
//   * 
//   * @return List of all registered time periods
//   */
//  public List<String> getTimePeriodXmlIds() {
//    return inputBuilderListener.getTimePeriodSourceIds();
//  }

}
