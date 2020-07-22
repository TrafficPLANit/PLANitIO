package demo;

import java.util.List;
import java.util.logging.Logger;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCostPeriod;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.demands.Demands;
import org.planit.gap.LinkBasedRelativeDualityGapFunction;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.io.output.formatter.PlanItOutputFormatter;
import org.planit.io.project.PlanItProject;
import org.planit.io.project.PlanItSimpleProject;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.OriginDestinationOutputTypeConfiguration;
import org.planit.output.configuration.PathOutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.RouteIdType;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.project.CustomPlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.trafficassignment.builder.TraditionalStaticAssignmentBuilder;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;

/**
 * Demo class. Show casing how to setup typical static assignment PLANit projects
 *
 * @author markr
 *
 */
public class PLANitStaticAssignmentProjectDemos {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PLANitStaticAssignmentProjectDemos.class.getCanonicalName());

  /**
   * Setup a stock standard traditional static assignment
   */
  public static void minimumExampleSimpleProjectDemo() {
    final String projectPath = "<insert the project path here>";

    try {
      // Create a simple PLANit project with all the default settings
      final PlanItSimpleProject project = new PlanItSimpleProject(projectPath);

      project.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }
  
  /**
   * Setup a stock standard traditional static assignment with cusotm project
   */
  public static void minimumExampleCustomProjectDemo() {
    final String projectPath = "<insert the project path here>";

    try {
      // Create a custom PLANit project with all the default settings
      final CustomPlanItProject project = new CustomPlanItProject(new PlanItInputBuilder(projectPath));
      
      // parse and register inputs
      PhysicalNetwork network = project.createAndRegisterPhysicalNetwork(PhysicalNetwork.MACROSCOPICNETWORK);
      Zoning zoning = project.createAndRegisterZoning(network);
      Demands demands = project.createAndRegisterDemands(zoning, network);
      
      // create and register project level output formatter
      OutputFormatter outputFormatter = project.createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER);

      // create traffic assignment with selected project inputs
      TrafficAssignmentBuilder taBuilder = project.createAndRegisterTrafficAssignment(
          TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT,
          demands,
          zoning,
          network);
      
      // activate the default output formatter on the assignment
      taBuilder.registerOutputFormatter(outputFormatter); 

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }  
  
  /**
   * Setup a project with the most basic initial costs
   */
  public static void minimumInitialCostExampleDemo() {
    final String projectPath = "<insert the project path here>";
    final String initialCostCSVPath = "<insert the initial cost CSV file path here>";

    try {
      // Create a  PLANit project with all the default settings
      final PlanItSimpleProject project = new PlanItSimpleProject(projectPath);
      
      InitialLinkSegmentCost initialLinkSegmentCost = project.createAndRegisterInitialLinkSegmentCost(
          project.getNetwork(), initialCostCSVPath);
      
      // create traffic assignment with selected project inputs
      TrafficAssignmentBuilder taBuilder = project.createAndRegisterTrafficAssignment(
          TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);

      // register initial link segment costs for all time periods
      taBuilder.registerInitialLinkSegmentCost(initialLinkSegmentCost);

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  } 
  
  /**
   * Setup a project with the initial link segment costs for a particular time period
   */
  public static void initialCostTimePeriodDemo() {

    try {
      // Create a PLANit project with all the default settings
      final PlanItSimpleProject project = new PlanItSimpleProject();
      TimePeriod theTimePeriod = project.getDemands().timePeriods.getTimePeriodById(0);
      
      InitialLinkSegmentCost initialLinkSegmentCost = project.createAndRegisterInitialLinkSegmentCost(
          project.getNetwork(), 
          "<insert the initial cost CSV file path here>", 
          theTimePeriod);
      
      // create traffic assignment with selected project inputs
      TrafficAssignmentBuilder taBuilder = project.createAndRegisterTrafficAssignment(
          TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);

      // register initial link segment costs for the given time period
      taBuilder.registerInitialLinkSegmentCost(theTimePeriod, initialLinkSegmentCost);

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }
  
  /**
   * Setup a project with the initial link segment costs for time periods available in the demands
   */
  public static void initialCostTimePeriodDemandsDemo() {

    try {
      // Create a PLANit project with all the default settings
      final PlanItSimpleProject project = new PlanItSimpleProject();

      // register on all available time periods
      List<InitialLinkSegmentCostPeriod> initialLinkSegmentCosts = project.createAndRegisterInitialLinkSegmentCost(
          project.getNetwork(), 
          "<insert the initial cost CSV file path here>", 
          project.getDemands());
      
      // create traffic assignment with selected project inputs
      TrafficAssignmentBuilder taBuilder = project.createAndRegisterTrafficAssignment(
          TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);

      // register initial link segment costs for the given time period
      for(InitialLinkSegmentCostPeriod initialCost : initialLinkSegmentCosts) {
        taBuilder.registerInitialLinkSegmentCost(initialCost);
      }      

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }    
  
  /**
   * Setup a traditional static assignment with explicit smoothing type
   */
  public static void simpleAssignmentSmoothingDemo() {
    try {

      final PlanItSimpleProject project = new PlanItSimpleProject();

      TrafficAssignmentBuilder taBuilder = 
          project.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      
      MSASmoothing smoothing = (MSASmoothing) taBuilder.createAndRegisterSmoothing(Smoothing.MSA);
      smoothing.getId();

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }  
  
  /**
   * Setup a traditional static assignment with explicit physical cost type (BPR)
   */
  public static void simpleAssignmentPhysicalCostDemo() {
    try {

      final PlanItSimpleProject project = new PlanItSimpleProject();

      TrafficAssignmentBuilder taBuilder = 
          project.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      
      BPRLinkTravelTimeCost bprCost = (BPRLinkTravelTimeCost) taBuilder.createAndRegisterPhysicalCost(PhysicalCost.BPR);
      bprCost.setDefaultParameters(0.87, 4);

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }   
  
  /**
   * Setup a traditional static assignment with explicit virtual cost type (SPEED)
   */
  public static void simpleAssignmentVirtualCostDemo() {
    try {

      final PlanItSimpleProject project = new PlanItSimpleProject();

      TrafficAssignmentBuilder taBuilder = 
          project.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      
      SpeedConnectoidTravelTimeCost virtualCost = (SpeedConnectoidTravelTimeCost) taBuilder.createAndRegisterVirtualCost(VirtualCost.SPEED);
      virtualCost.setConnectiodSpeed(25);
      
      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  } 
  
  /**
   * Setup a traditional static assignment with gap function configuration
   */
  public static void simpleAssignmentGapFunctionDemo() {
    try {

      final PlanItSimpleProject project = new PlanItSimpleProject();

      TrafficAssignmentBuilder taBuilder = 
          project.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      
      if(taBuilder.getGapFunction() instanceof LinkBasedRelativeDualityGapFunction) {
        LOGGER.info("Link based relative duality gap used as gap function");
      }else {
        LOGGER.severe("unknown gap function");
      }
      
      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }      
  
  /**
   * Setup a traditional static assignment with stop crtierion configuration
   */
  public static void simpleAssignmentStopCriterionDemo() {
    try {

      final PlanItSimpleProject project = new PlanItSimpleProject();

      TrafficAssignmentBuilder taBuilder = 
          project.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      
      taBuilder.getGapFunction().getStopCriterion().setMaxIterations(100);
      
      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }     

  /**
   * Setup a mininum configuration standard traditional static assignment:
   * - Use the full fledged configuration objects, but
   * - Explicitly set all defaults for demo purposes
   */
  public static void maximumExampleDemo() {
    // CONFIGURATION INPUT
    final String projectPath = "<insert the project path here>";
    final String logFile = "<insert logFile including path and extension here>";
    final String initialCsvCostFilePath = "<insert the initial cost file path here>";
    final String outputFileName = "<insert base output file name without extension here>";
    final String outputPath = "<insert path to output directory here>";

    final int maxIterations = 100;
    final double gapEpsilon = 0.001;

    // ---------------------------------------------------------------------------------------------------------------------------------------

    try {
      // INITIALISE PLANit PROJECT
      final PlanItProject project = new PlanItProject(projectPath);

      // INITIALISE INPUTS
      final PhysicalNetwork physicalNetwork = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class
          .getCanonicalName());
      final Zoning zoning = project.createAndRegisterZoning(physicalNetwork);
      final Demands demands = project.createAndRegisterDemands(zoning, physicalNetwork);
      final InitialLinkSegmentCost initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork,
          initialCsvCostFilePath, demands.timePeriods.asSortedSetByStartTime().first());
      // INITIALISE OUTPUT FORMATTERS
      final PlanItOutputFormatter xmlOutputFormatter = (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(
          PlanItOutputFormatter.class.getCanonicalName());
      final MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project
          .createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());

      final TraditionalStaticAssignmentBuilder taBuilder =
          (TraditionalStaticAssignmentBuilder) project.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT, demands, zoning, physicalNetwork);

      // Initial (physical) link segment cost
      taBuilder.registerInitialLinkSegmentCost(initialCost);
      // physical links: BPR cost function
      taBuilder.createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
      // virtual links: fixed cost function
      taBuilder.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
      // iteration smoothing: MSA
      taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
      // Output formatter: PLANitIO + MEMORY
      taBuilder.registerOutputFormatter(xmlOutputFormatter);
      taBuilder.registerOutputFormatter(memoryOutputFormatter);

      // COMPONENT CONFIGURATION - INPUT
      taBuilder.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
      taBuilder.getGapFunction().getStopCriterion().setEpsilon(gapEpsilon);

      // COMPONENT CONFIGURATION - OUTPUT CONTENTS
      // General:
      taBuilder.getOutputConfiguration().setPersistOnlyFinalIteration(true);
      // link outputs: ON + example configuration
      final LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) taBuilder
          .activateOutput(OutputType.LINK);
      linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_ID);
      // OD outputs: ON + example configuration
      final OriginDestinationOutputTypeConfiguration originDestinationOutputTypeConfiguration =
          (OriginDestinationOutputTypeConfiguration) taBuilder.activateOutput(OutputType.OD);
      originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
      originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.RUN_ID);
      // PATH outputs: ON + example configuration
      final PathOutputTypeConfiguration pathOutputTypeConfiguration = (PathOutputTypeConfiguration) taBuilder
          .activateOutput(OutputType.PATH);
      pathOutputTypeConfiguration.setPathIdType(RouteIdType.NODE_EXTERNAL_ID);

      // COMPONENT CONFIGURATION - PLANitIO OUTPUT FORMAT
      xmlOutputFormatter.setXmlNameRoot(outputFileName);
      xmlOutputFormatter.setCsvNameRoot(outputFileName);
      xmlOutputFormatter.setOutputDirectory(outputPath);

      // EXECUTE ASSIGNMENTS
      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }
}
