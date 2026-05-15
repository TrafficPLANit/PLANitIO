package org.goplanit.io.test.util;
 
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.assignment.TrafficAssignmentConfigurator;
import org.goplanit.cost.physical.PhysicalCostConfigurator;
import org.goplanit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.goplanit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.goplanit.demands.Demands;
import org.goplanit.io.input.PlanItInputBuilder;
import org.goplanit.io.output.formatter.PlanItOutputFormatter;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.output.configuration.*;
import org.goplanit.output.enums.SkimSubOutputType;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.PathOutputIdentificationType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.output.formatter.OutputFormatter;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.functionalinterface.TriConsumer;
import org.goplanit.utils.test.TestOutputDto;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.unit.Unit;
import org.goplanit.zoning.Zoning;

/**
 * Helper class used by unit tests to conduct test runs with various configuration options set
 *
 * @author markr
 *
 */
public class PlanItIoTestRunner {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanItIoTestRunner.class.getCanonicalName());

  /** project path to use */
  protected final String projectPath;
  
  /** input builder used */
  protected PlanItInputBuilder planItInputBuilder;
  
  /** project used */
  protected CustomPlanItProject project;
  
  /** network used */
  protected MacroscopicNetwork network;
  
  /** zoning used */
  protected Zoning zoning;
  
  /** demands used */
  protected Demands demands;
  
  /** the traffic assignment configurator used */
  protected TrafficAssignmentConfigurator<?> taConfigurator;
  
  /** Physical cost - Bpr configuration */
  protected PhysicalCostConfigurator<?> physicalCostConfigurator;

  /** Output formatter - Xml (Planit default) */
  protected PlanItOutputFormatter xmlOutputFormatter; 
  
  /** Output formatter - memory */
  protected MemoryOutputFormatter memoryOutputFormatter;
  
  /** output configuration used */
  protected OutputConfiguration outputConfiguration;
  
  /** link output type configuration used */
  protected LinkOutputTypeConfiguration linkOutputTypeConfiguration;
  
  /* local config options */
  
  /** use fixed connectoid travel time cost, if false, we use speed based */
  protected boolean useFixedConnectoidTravelTimeCost = true;

  /** activate simulation data output using its defaults */
  protected boolean activateDefaultSimulationData = false;

  /** choose smoothing type, default MSA */
  protected String smoothingType = Smoothing.MSA;

  /**
   * Run a test case and store the results in a MemoryOutputFormatter, most generic form with all consumers passable
   * but could be nulls
   *
   * @param setLinkOutputTypeConfigurationProperties lambda function to set output properties being used
   * @param setCostParameters lambda function which sets parameters of cost function
   * @return TestOutputDto containing results, builder and project from the run
   */
  protected TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder> setupAndExecuteAssignment(
      final Consumer<LinkOutputTypeConfiguration> setLinkOutputTypeConfigurationProperties,
      final TriConsumer<LayeredNetwork<?,?>, PhysicalCostConfigurator<?>, PlanItInputBuilder> setCostParameters) {

    /* Smoothing - MSA default */
    taConfigurator.createAndRegisterSmoothing(getSmoothingType());

    if (setCostParameters != null) {
      setCostParameters.accept(network, physicalCostConfigurator, planItInputBuilder);
    }
    
    /* Virtual cost */
    if (useFixedConnectoidTravelTimeCost) {
      taConfigurator.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
    } else {
      taConfigurator.createAndRegisterVirtualCost(SpeedConnectoidTravelTimeCost.class.getCanonicalName());
    }

    if(activateDefaultSimulationData){
      var simConfig = taConfigurator.activateOutput(OutputType.SIMULATION);
      simConfig.addProperty(OutputPropertyType.ROUTE_CHOICE_CONVERGENCE_GAP);
      //simConfig.addProperty(OutputPropertyType.ROUTE_CHOICE_ITERATION_RUN_TIME); // hard to assert against
    }
    
    /* Link output type consumer */
    if(setLinkOutputTypeConfigurationProperties != null) {
      setLinkOutputTypeConfigurationProperties.accept(linkOutputTypeConfiguration);
    }
    
    /* execute */
    project.executeAllTrafficAssignments();
    
    /* output */
    return new TestOutputDto(memoryOutputFormatter, project, planItInputBuilder);
  }

  /**
   * Constructor
   *
   * @param inputPath to use
   * @param outputPath to use
   * @param description to use
   */
  public PlanItIoTestRunner(
          String inputPath,
          String outputPath,
          String description,
          Function<CustomPlanItProject, TrafficAssignmentConfigurator<?>> taConfiguratorFactory) {
    this.projectPath = inputPath;

    try {
      this.planItInputBuilder = new PlanItInputBuilder(projectPath);
      this.project = new CustomPlanItProject(planItInputBuilder);

      /* RAW INPUT START -------------------------------- */
      {
        this.network = (MacroscopicNetwork) project.createAndRegisterInfrastructureNetwork(
                MacroscopicNetwork.class.getCanonicalName());
        this.zoning = project.createAndRegisterZoning(network);
        this.demands = project.createAndRegisterDemands(zoning, network);
      }
      /* RAW INPUT END ----------------------------------- */

      /* TRAFFIC ASSIGNMENT */
      this.taConfigurator = taConfiguratorFactory.apply(project);
      this.physicalCostConfigurator = taConfigurator.getPhysicalCost();

      /* OUTPUT FORMAT CONFIGURATION */
      {
        /* Xml PlanItOutputFormatter */
        this.xmlOutputFormatter =
                (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER);
        xmlOutputFormatter.setXmlNameRoot(description);
        xmlOutputFormatter.setCsvNameRoot(description);
        xmlOutputFormatter.setOutputDirectory(outputPath);
        taConfigurator.registerOutputFormatter(xmlOutputFormatter);

        // MemoryOutputFormatter
        this.memoryOutputFormatter =
                (MemoryOutputFormatter) project.createAndRegisterOutputFormatter(OutputFormatter.MEMORY_OUTPUT_FORMATTER);
        taConfigurator.registerOutputFormatter(memoryOutputFormatter);
      }

      /* OUTPUT (TYPE) CONFIGURATION */
      {
        /* general */
        this.outputConfiguration = taConfigurator.getOutputConfiguration();
        outputConfiguration.setPersistOnlyFinalIteration(true);

        /* Link OUTPUT CONFIGURATION */
        linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) taConfigurator.activateOutput(OutputType.LINK);

        linkOutputTypeConfiguration.addProperty(OutputPropertyType.CAPACITY_PER_LANE);
        linkOutputTypeConfiguration.addProperty(OutputPropertyType.NUMBER_OF_LANES);
        linkOutputTypeConfiguration.addProperty(OutputPropertyType.LENGTH);
        linkOutputTypeConfiguration.removeProperty(OutputPropertyType.TIME_PERIOD_XML_ID);
        linkOutputTypeConfiguration.removeProperty(OutputPropertyType.MAXIMUM_SPEED);

        /* for this test we prefer to get out flows and capacities in vehicles rather than pcus (no difference in
        result with pcu=1, only in metadata)*/
        linkOutputTypeConfiguration.overrideOutputPropertyUnits(OutputPropertyType.CAPACITY_PER_LANE, Unit.VEH_HOUR);
        linkOutputTypeConfiguration.overrideOutputPropertyUnits(OutputPropertyType.FLOW, Unit.VEH_HOUR);

        /* OD OUTPUT CONFIGURATION */
        final OdOutputTypeConfiguration originDestinationOutputTypeConfiguration =
                (OdOutputTypeConfiguration) taConfigurator.activateOutput(OutputType.OD);
        originDestinationOutputTypeConfiguration.deactivateOdSkimOutputType(SkimSubOutputType.NONE);
        originDestinationOutputTypeConfiguration.removeProperty(OutputPropertyType.TIME_PERIOD_XML_ID);

        /* PATH OUTPUT CONFIGURATION */
        final PathOutputTypeConfiguration pathOutputTypeConfiguration =
                (PathOutputTypeConfiguration) taConfigurator.activateOutput(OutputType.PATH);
        pathOutputTypeConfiguration.setPathIdentificationType(PathOutputIdentificationType.NODE_XML_ID);

        /* BUSH OUTPUT CONFIGURATION */
        final BushOutputTypeConfiguration bushOutputTypeConfiguration =
                (BushOutputTypeConfiguration) taConfigurator.activateOutput(OutputType.BUSH);
        bushOutputTypeConfiguration.removeProperty(OutputPropertyType.TIME_PERIOD_XML_ID);
        bushOutputTypeConfiguration.removeProperty(OutputPropertyType.MODE_XML_ID);
      }

    }catch(PlanItException e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("Unable to initialise PlanitIo testhelper");
    }
  }

  /**
   * Constructor. Applies default traditional static assignment
   * 
   * @param projectPath to use (both input and output path)
   * @param description to use
   */
  public PlanItIoTestRunner(String projectPath, String description) {
    this(projectPath,
            projectPath,
            description,
            PlanItIoTestRunnerTraditionalStatic::createTrafficAssignmentConfigurator);
  }
  
  /**
   * Run a test case with a default configuration and no additional changes via consumers. Store the results in a
   * MemoryOutputFormatter.
   *
   * @return TestOutputDto containing results, builder and project from the run
   */
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder>
  setupAndExecuteDefaultAssignment(){
    return setupAndExecuteAssignment(null, null);
  }   

  /**
   * Run a test case with a custom physical cost configuration. Store the results in a MemoryOutputFormatter.
   *
   * @param setPhysicalCostParameters lambda function which sets parameters of cost function
   * @return TestOutputDto containing results, builder and project from the run
   */
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder>
  setupAndExecuteWithPhysicalCostConfiguration(
      final TriConsumer<LayeredNetwork<?,?>, PhysicalCostConfigurator<?>, PlanItInputBuilder> setPhysicalCostParameters) {
    return setupAndExecuteAssignment(null, setPhysicalCostParameters);
  }
  
  /**
   * Run a test case with a custom link output type configuration consumer. Store the results in a
   * MemoryOutputFormatter.
   *
   * @param linkOutputTypeConfigurationConsumer lambda function which sets parameters of link output type
   *                                            configuration in addition to default settings
   * @return TestOutputDto containing results, builder and project from the run
   */  
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder>
  setupAndExecuteWithCustomLinkOutputConfiguration(
          Consumer<LinkOutputTypeConfiguration> linkOutputTypeConfigurationConsumer) {
    return setupAndExecuteAssignment(linkOutputTypeConfigurationConsumer, null);    
  }  
  
  /**
   * Run a test case with a custom link output type configuration and Bpr cost consumers. Store the results in a
   * MemoryOutputFormatter.
   *
   * @param setPhysicalCostParameters lambda function which sets parameters of cost function
   * @param linkOutputTypeConfigurationConsumer lambda function which sets parameters of link output type
   *                                            configuration
   *                                            in addition to default settings
   * @return TestOutputDto containing results, builder and project from the run
   */    
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder>
  setupAndExecuteWithCustomBprAndLinkOutputTypeConfiguration(
      TriConsumer<LayeredNetwork<?, ?>, PhysicalCostConfigurator<?>, PlanItInputBuilder> setPhysicalCostParameters,
      Consumer<LinkOutputTypeConfiguration> linkOutputTypeConfigurationConsumer) {
    return setupAndExecuteAssignment(linkOutputTypeConfigurationConsumer, setPhysicalCostParameters);
  }  
   

  /* Getters/Setters */
  
  /** Set the maximum number of iterations to run
   * 
   * @param maxIterations to run at maximum
   */
  public void setMaxIterations(int maxIterations) {
    taConfigurator.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
  }
  
  /** set the epsilon gap to use for the gap function convergence test
   * 
   * @param epsilon to use
   */
  public void setGapFunctionEpsilonGap(double epsilon) {
    taConfigurator.getGapFunction().getStopCriterion().setEpsilon(epsilon);
  }
  
  /** indicate if zero flows should be persisted by the simulation 
   * @param persistZeroFlow to set
   */
  public void setPersistZeroFlow(boolean persistZeroFlow) {
    outputConfiguration.setPersistZeroFlow(persistZeroFlow);
  }

  /**
   * indicates using fixed connectoid costs (of zero)
   */  
  public void setUseFixedConnectoidCost() {
    this.useFixedConnectoidTravelTimeCost = true;
  }
  
  /**
   * indicates using speed based connectoid costs
   */
  public void setUseSpeedBasedConnectoidCost() {
    this.useFixedConnectoidTravelTimeCost = false;
  }

  /**
   * Indicates to activate simulation data output
   *
   * @param activateDefaultSimulationData when true activate, otherwise deactivate
   */
  public void setActivateSimulationData(boolean activateDefaultSimulationData) {
    this.activateDefaultSimulationData = activateDefaultSimulationData;
  }
  
  /** register (general) initial cost on the test based on the passed in location where to find it
   * 
   * @param initialCostLocation to parse initial costs from
   * @throws PlanItException thrown if error
   */
  public void registerInitialLinkSegmentCost(String initialCostLocation) throws PlanItException {
    var initialCost =
        project.createAndRegisterInitialLinkSegmentCost(network, initialCostLocation);
    taConfigurator.registerInitialLinkSegmentCost(initialCost);
  }

  /** register initial cost specific to a time period on the test based on the passed in location where to find it
   * 
   * @param timePeriodXmlId to use
   * @param initialCostLocation to parse initial costs from
   * @throws PlanItException thrown if error
   */  
  public void registerInitialLinkSegmentCostByTimePeriod(
          String timePeriodXmlId, String initialCostLocation) throws PlanItException {
    TimePeriod timePeriod = demands.timePeriods.getByXmlId(timePeriodXmlId);
    final var initialCost =
        project.createAndRegisterInitialLinkSegmentCost(network, initialCostLocation,timePeriod);
    taConfigurator.registerInitialLinkSegmentCost(timePeriod, initialCost.getTimePeriodCosts(timePeriod));    
  }

  /**
   * For expert use to configure directly on the traffic assignment configurator for options not exposed
   * by this class as shortcuts
   *
   * @return traffic assignment configurator
   */
  public TrafficAssignmentConfigurator<?> getRawTrafficAssignmentConfigurator(){
    return taConfigurator;
  }

  public String getSmoothingType() {
    return smoothingType;
  }

  public void setSmoothingType(String smoothingType) {
    this.smoothingType = smoothingType;
  }

}