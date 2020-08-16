package org.planit.io.demo;

import java.util.List;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.assignment.traditionalstatic.TraditionalStaticAssignmentConfigurator;
import org.planit.cost.physical.BPRConfigurator;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCostPeriod;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.cost.virtual.SpeedVirtualCostConfigurator;
import org.planit.cost.virtual.VirtualCost;
import org.planit.demands.Demands;
import org.planit.gap.LinkBasedRelativeGapConfigurator;
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
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.PathOutputIdentificationType;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.formatter.MemoryOutputIterator;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.project.CustomPlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.sdinteraction.smoothing.MSASmoothingConfigurator;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;

/**
 * Demo class. Show casing how to setup typical static assignment PLANit projects
 * 
 * Note: if any of the demos no longer work or do not compile, fix them here but also in the Example section of the Java manual website as these are all
 * copies of those examples.
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
      MacroscopicNetwork network = (MacroscopicNetwork) project.createAndRegisterPhysicalNetwork(PhysicalNetwork.MACROSCOPICNETWORK);
      Zoning zoning = project.createAndRegisterZoning(network);
      Demands demands = project.createAndRegisterDemands(zoning, network);
      
      // create and register project level output formatter
      OutputFormatter outputFormatter = project.createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER);

      // create traffic assignment with selected project inputs
      TraditionalStaticAssignmentConfigurator ta = 
          (TraditionalStaticAssignmentConfigurator) project.createAndRegisterTrafficAssignment(
            TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT,
            demands,
            zoning,
            network);
      
      // activate the default output formatter on the assignment
      ta.registerOutputFormatter(outputFormatter); 

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
      TraditionalStaticAssignmentConfigurator ta = 
          (TraditionalStaticAssignmentConfigurator) project.createAndRegisterTrafficAssignment(
              TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);

      // register initial link segment costs for all time periods
      ta.registerInitialLinkSegmentCost(initialLinkSegmentCost);

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }
  
  /**
   * Setup a project with the initial costs by time period
   */
  public static void initialCostWithTimePeriodDemo() {
    try{
      
      final PlanItSimpleProject simpleProject = new PlanItSimpleProject();
      
      // * NEW *
      TimePeriod theTimePeriod = simpleProject.getDemands().timePeriods.getTimePeriodById(0);  
      InitialLinkSegmentCost initialLinkSegmentCost = 
          simpleProject.createAndRegisterInitialLinkSegmentCost(
              simpleProject.getNetwork(),"<insert the initial cost CSV file path here>", theTimePeriod);  

      TraditionalStaticAssignmentConfigurator taBuilder = (TraditionalStaticAssignmentConfigurator)
          simpleProject.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      
      // * NEW *
      taBuilder.registerInitialLinkSegmentCost(theTimePeriod, initialLinkSegmentCost);
      
      simpleProject.executeAllTrafficAssignments();
        
    } catch (final Exception e) {
      // do something
    }
  }   
  
  /**
   * Setup a project with the initial costs by demands
   */
  public static void initialCostWithDemandsDemo() {
    try{
      
      final PlanItSimpleProject simpleProject = new PlanItSimpleProject();
      
      // * NEW * source time periods from Demands instance and register initial costs on each one
      List<InitialLinkSegmentCostPeriod> initialLinkSegmentCosts = 
          simpleProject.createAndRegisterInitialLinkSegmentCost(
              simpleProject.getNetwork(),"<insert the initial cost CSV file path here>", simpleProject.getDemands());  

      TraditionalStaticAssignmentConfigurator ta = (TraditionalStaticAssignmentConfigurator) 
          simpleProject.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      
      // * NEW * register for all available time periods
      for(InitialLinkSegmentCostPeriod initialCost : initialLinkSegmentCosts) {
        ta.registerInitialLinkSegmentCost(initialCost.getTimePeriod(), initialCost);
      }
      
      simpleProject.executeAllTrafficAssignments();
        
    } catch (final Exception e) {
      // do something
    }
  }   
  
  /**
   * Setup a project with the initial costs by time period and without time period
   */
  public static void initialCostHybridDemo() {
    final String initialCostCSVPath1 = "<insert the initial cost CSV file path1 here>";
    final String initialCostCSVPath2 = "<insert the initial cost CSV file path2 here>";
    
    try{
       
      final PlanItSimpleProject simpleProject = new PlanItSimpleProject();
      
      // * NEW * initial cost WITHOUT time period
      InitialLinkSegmentCost initialLinkSegmentCostNoTimePeriod = 
          simpleProject.createAndRegisterInitialLinkSegmentCost(simpleProject.getNetwork(),initialCostCSVPath1);
        
      // * NEW * intial cost WITH time period
      TimePeriod theTimePeriod = simpleProject.getDemands().timePeriods.getTimePeriodById(0);  
      InitialLinkSegmentCost initialLinkSegmentCostTimePeriod = 
          simpleProject.createAndRegisterInitialLinkSegmentCost(simpleProject.getNetwork(),initialCostCSVPath2, theTimePeriod);      
    
      TraditionalStaticAssignmentConfigurator ta = (TraditionalStaticAssignmentConfigurator) 
          simpleProject.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      
      // * NEW * register with and without time period
      ta.registerInitialLinkSegmentCost(initialLinkSegmentCostNoTimePeriod);
      ta.registerInitialLinkSegmentCost(theTimePeriod, initialLinkSegmentCostTimePeriod);
      
      simpleProject.executeAllTrafficAssignments();
        
    } catch (final Exception e) {
      // do something
    }
  }    
  
  /**
   * Setup a project with traditional static assignment and smoothing component
   */
  public static void TrafficAssignmentSmoothingDemo() {
    try {

      final PlanItSimpleProject project = new PlanItSimpleProject();
      
      TraditionalStaticAssignmentConfigurator ta = (TraditionalStaticAssignmentConfigurator)
          project.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      
      // * NEW *
      // set MSA Smoothing as the type
      // instance is returned for follow up configuration
      @SuppressWarnings("unused") 
      MSASmoothingConfigurator smoothing = (MSASmoothingConfigurator) ta.createAndRegisterSmoothing(Smoothing.MSA);
      // smoothing.callAMethod()  

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      // do something
    }
  }  
  
  /**
   * Setup a project with traditional static assignment and physical cost component
   */
  public static void TrafficAssignmentPhysicalCostDemo() {
    try {

      final PlanItSimpleProject project = new PlanItSimpleProject();
      
      TraditionalStaticAssignmentConfigurator ta = (TraditionalStaticAssignmentConfigurator) 
          project.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      
      // * NEW *
      // set BPR link performance function as the type
      BPRConfigurator bprCost = (BPRConfigurator) ta.createAndRegisterPhysicalCost(PhysicalCost.BPR);
      // override default alpha and beta parameters
      bprCost.setDefaultParameters(0.5, 5); 

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      // do something
    }
  }  
  
  /**
   * Setup a project with traditional static assignment and virtual cost component
   */
  public static void TrafficAssignmentVirtualCostDemo() {
    try {

      final PlanItSimpleProject project = new PlanItSimpleProject();
      
      TraditionalStaticAssignmentConfigurator ta = (TraditionalStaticAssignmentConfigurator) 
          project.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      
      // * NEW *
      // set speed based virtual cost function as the type
      SpeedVirtualCostConfigurator virtualCost =
        (SpeedVirtualCostConfigurator) ta.createAndRegisterVirtualCost(VirtualCost.SPEED);
      // set default speed to 25 km/h
      virtualCost.setConnectoidSpeed(25);

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      // do something
    }
  }  
  
  /**
   * Setup a project with traditional static assignment and gap function
   */
  public static void TrafficAssignmentGapFunctionDemo() {
    try {

      final PlanItSimpleProject project = new PlanItSimpleProject();
      
      TraditionalStaticAssignmentConfigurator ta = (TraditionalStaticAssignmentConfigurator)
          project.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
        
      // * NEW *
      // check type of default created gap function
      if(ta.getGapFunction() instanceof LinkBasedRelativeGapConfigurator) {
        System.out.println("Link based relative duality gap used as gap function");
      }else {
        System.out.println("unknown gap function");
      }

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      // do something
    }
  }  
  
  /**
   * Setup a project with traditional static assignment and stop criterion
   */
  public static void TrafficAssignmentStopCriterionDemo() {
    try {

      final PlanItSimpleProject project = new PlanItSimpleProject();
      
      TraditionalStaticAssignmentConfigurator ta = (TraditionalStaticAssignmentConfigurator)
          project.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      
      // * NEW *
      // access stop criterion via gap function and set max number of iterations to 100  
      ta.getGapFunction().getStopCriterion().setMaxIterations(100);

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      // do something
    }
  }  
  
  /**
   * Setup a project with multiple output formatters
   */
  public static void MultipleOutputFormattersDemo() {
    try {
      final CustomPlanItProject project = new CustomPlanItProject(new PlanItInputBuilder("<insert the project path here>"));
     
      MacroscopicNetwork network = (MacroscopicNetwork) project.createAndRegisterPhysicalNetwork(PhysicalNetwork.MACROSCOPICNETWORK);
      Zoning zoning = project.createAndRegisterZoning(network);
      Demands demands = project.createAndRegisterDemands(zoning, network);
      
      // *NEW*
      OutputFormatter defaultOutputFormatter = project.createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER);
      OutputFormatter memoryOutputFormatter = project.createAndRegisterOutputFormatter(OutputFormatter.MEMORY_OUTPUT_FORMATTER);

      TraditionalStaticAssignmentConfigurator ta = (TraditionalStaticAssignmentConfigurator) 
          project.createAndRegisterTrafficAssignment(
            TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT, demands, zoning, network);
      
      // *NEW*
      ta.registerOutputFormatter(defaultOutputFormatter);
      ta.registerOutputFormatter(memoryOutputFormatter); 

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      // do something
    }
  }    
  
  /**
   * Setup a project with a memory output formatter
   */
  public static void MemoryOutputFormatterDemo() {
    try {
      final CustomPlanItProject project = new CustomPlanItProject(new PlanItInputBuilder("<insert the project path here>"));
     
      MacroscopicNetwork network = (MacroscopicNetwork) project.createAndRegisterPhysicalNetwork(PhysicalNetwork.MACROSCOPICNETWORK);
      Zoning zoning = project.createAndRegisterZoning(network);
      Demands demands = project.createAndRegisterDemands(zoning, network);
      
      MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project.createAndRegisterOutputFormatter(OutputFormatter.MEMORY_OUTPUT_FORMATTER);

      TraditionalStaticAssignmentConfigurator ta = (TraditionalStaticAssignmentConfigurator)
          project.createAndRegisterTrafficAssignment( TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT, demands, zoning, network);
      
      ta.registerOutputFormatter(memoryOutputFormatter); 

      project.executeAllTrafficAssignments();
      
      // * NEW *
      MemoryOutputIterator outputIterator = memoryOutputFormatter.getIterator(
          network.modes.getFirst(), 
          demands.timePeriods.getFirst(), 
          memoryOutputFormatter.getLastIteration(), 
          OutputType.LINK);
      
      // * NEW *
      int idPosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(OutputType.LINK, OutputProperty.LINK_SEGMENT_ID);
      int flowPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputProperty.FLOW);
      int linkSegmentCostPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputProperty.LINK_COST);
      
      // * NEW *
      while(outputIterator.hasNext()) {
        outputIterator.next();
        long linkSegmentId = (long)outputIterator.getKeys()[idPosition];
        Object[] values = outputIterator.getValues();        
        double linkSegmentFlow = (double)values[flowPosition];
        double linkSegmentCost = (double)values[linkSegmentCostPosition];
        LOGGER.info(String.format("link: %d with flow: %f and cost: %f", linkSegmentId, linkSegmentFlow, linkSegmentCost));
      }
      
    } catch (final Exception e) {
      // do something
    }
  }
  
  /**
   * Setup a project and set some general output configuration options 
   */
  public static void OutputConfigurationDemo() {
    try {
      final CustomPlanItProject project = new CustomPlanItProject(new PlanItInputBuilder("<insert the project path here>"));
     
      MacroscopicNetwork network = (MacroscopicNetwork) project.createAndRegisterPhysicalNetwork(PhysicalNetwork.MACROSCOPICNETWORK);
      Zoning zoning = project.createAndRegisterZoning(network);
      Demands demands = project.createAndRegisterDemands(zoning, network);
      
      TraditionalStaticAssignmentConfigurator ta = (TraditionalStaticAssignmentConfigurator) 
          project.createAndRegisterTrafficAssignment(
              TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT, demands, zoning, network);
                  
      ta.registerOutputFormatter(project.createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER));
           
      // * NEW *
      ta.getOutputConfiguration().setPersistOnlyFinalIteration(false);
      ta.getOutputConfiguration().setPersistZeroFlow(true);
      
      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      // do something
    }
  }   
  
  /**
   * Setup a project with  explicit (de-)activating of output type 
   */
  public static void OutputTypeActivationDemo() {
    try {
      final CustomPlanItProject project = new CustomPlanItProject(new PlanItInputBuilder("<insert the project path here>"));
     
      MacroscopicNetwork network = (MacroscopicNetwork) project.createAndRegisterPhysicalNetwork(PhysicalNetwork.MACROSCOPICNETWORK);
      Zoning zoning = project.createAndRegisterZoning(network);
      Demands demands = project.createAndRegisterDemands(zoning, network);
      
      TraditionalStaticAssignmentConfigurator ta = (TraditionalStaticAssignmentConfigurator) 
          project.createAndRegisterTrafficAssignment(
              TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT, demands, zoning, network);
                  
      ta.registerOutputFormatter(project.createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER));
      
      // * NEW *
      ta.deactivateOutput(OutputType.LINK);
      
      // * NEW *
      PathOutputTypeConfiguration pathOutputConfiguration = (PathOutputTypeConfiguration) ta.activateOutput(OutputType.PATH);
      
      // * NEW *
      pathOutputConfiguration.removeProperty(OutputProperty.RUN_ID);
      pathOutputConfiguration.addProperty(OutputProperty.MODE_ID);

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      // do something
    }
  } 
  
  /**
   * Setup a project with  OD output type example 
   */
  public static void ODOutputTypeDemo() {
    try {
      final CustomPlanItProject project = new CustomPlanItProject(new PlanItInputBuilder("<insert the project path here>"));
     
      MacroscopicNetwork network = (MacroscopicNetwork) project.createAndRegisterPhysicalNetwork(PhysicalNetwork.MACROSCOPICNETWORK);
      Zoning zoning = project.createAndRegisterZoning(network);
      Demands demands = project.createAndRegisterDemands(zoning, network);
      
      TraditionalStaticAssignmentConfigurator ta = (TraditionalStaticAssignmentConfigurator)
          project.createAndRegisterTrafficAssignment(
              TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT, demands, zoning, network);
                  
      ta.registerOutputFormatter(project.createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER));
           
      // * NEW *
      OriginDestinationOutputTypeConfiguration odOutputConfiguration = (OriginDestinationOutputTypeConfiguration) ta.activateOutput(OutputType.OD);
      
      // * NEW *
      odOutputConfiguration.activateOdSkimOutputType(ODSkimSubOutputType.COST);
      odOutputConfiguration.removeProperty(OutputProperty.RUN_ID);

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      // do something
    }
  }  
  
  /**
   * The Getting started demo as provided on the website. We let it throw an exception instead of cathing it because it is the only runnable test
   * of the bunch and is called from the integration tests to ensure it remains runnable.
   * 
   *  It must be a viable example because it is used in the getting started on the website. Hence it is part of the testing cycle to ensure it remains up to date
   *  and viable. If any issues are found that require changing the demo, they should also be reflected in the getting started on the website
   *  
   * @throws PlanItException thrown when error
   */
  public static void gettingStartedDemo(String overrideProjectPath) throws PlanItException {
      // PROJECT INSTANCE
      final PlanItSimpleProject project = new PlanItSimpleProject(
          overrideProjectPath==null ? "c:\\Users\\Public\\planit\\" : overrideProjectPath);
           
      // ASSIGNMENT INSTANCE
      TraditionalStaticAssignmentConfigurator assignment = 
        (TraditionalStaticAssignmentConfigurator) project.createAndRegisterTrafficAssignment(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
      
      // COMPONENTS
      BPRConfigurator bprCost = (BPRConfigurator) assignment.createAndRegisterPhysicalCost(PhysicalCost.BPR);
      assignment.createAndRegisterVirtualCost(VirtualCost.FIXED);
      assignment.createAndRegisterSmoothing(Smoothing.MSA);
      
      // CONFIGURE COST COMPONENT
      // BPR 
      double alpha = 0.9;
      double beta = 4.5;
      bprCost.setDefaultParameters(alpha, beta);
      
      // CONFIGURE OUTPUT
      assignment.getOutputConfiguration().setPersistOnlyFinalIteration(false);
      
      // EXECUTE ASSIGNMENT
      project.executeAllTrafficAssignments();
  }  

  /**
   * Setup a mininum configuration standard traditional static assignment:
   * - Use the full fledged configuration objects, but
   * - Explicitly set all defaults for demo purposes
   */
  public static void maximumExampleDemo() {
    // CONFIGURATION INPUT
    final String projectPath = "<insert the project path here>";
    //final String logFile = "<insert logFile including path and extension here>";
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
      final MacroscopicNetwork network = (MacroscopicNetwork) 
          project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class .getCanonicalName());
      final Zoning zoning = project.createAndRegisterZoning(network);
      final Demands demands = project.createAndRegisterDemands(zoning, network);
      final InitialLinkSegmentCost initialCost = 
          project.createAndRegisterInitialLinkSegmentCost(
              network, initialCsvCostFilePath, demands.timePeriods.asSortedSetByStartTime().first());
      
      // INITIALISE OUTPUT FORMATTERS
      final PlanItOutputFormatter xmlOutputFormatter = (PlanItOutputFormatter) 
          project.createAndRegisterOutputFormatter(
              PlanItOutputFormatter.class.getCanonicalName());
      final MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) 
          project.createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());

      final TraditionalStaticAssignmentConfigurator ta = (TraditionalStaticAssignmentConfigurator) 
          project.createAndRegisterTrafficAssignment(
              TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT, demands, zoning, network);

      // Initial (physical) link segment cost
      ta.registerInitialLinkSegmentCost(initialCost);
      // physical links: BPR cost function
      ta.createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
      // virtual links: fixed cost function
      ta.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
      // iteration smoothing: MSA
      ta.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
      // Output formatter: PLANitIO + MEMORY
      ta.registerOutputFormatter(xmlOutputFormatter);
      ta.registerOutputFormatter(memoryOutputFormatter);

      // COMPONENT CONFIGURATION - INPUT
      ta.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
      ta.getGapFunction().getStopCriterion().setEpsilon(gapEpsilon);

      // COMPONENT CONFIGURATION - OUTPUT CONTENTS
      // General:
      ta.getOutputConfiguration().setPersistOnlyFinalIteration(true);
      // link outputs: ON + example configuration
      final LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) 
          ta.activateOutput(OutputType.LINK);
      linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_ID);
      // OD outputs: ON + example configuration
      final OriginDestinationOutputTypeConfiguration originDestinationOutputTypeConfiguration =
          (OriginDestinationOutputTypeConfiguration) ta.activateOutput(OutputType.OD);
      originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
      originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.RUN_ID);
      // PATH outputs: ON + example configuration
      final PathOutputTypeConfiguration pathOutputTypeConfiguration = (PathOutputTypeConfiguration) 
          ta.activateOutput(OutputType.PATH);
      pathOutputTypeConfiguration.setPathIdentificationType(PathOutputIdentificationType.NODE_EXTERNAL_ID);

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
