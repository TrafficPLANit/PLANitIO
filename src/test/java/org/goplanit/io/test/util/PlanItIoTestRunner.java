package org.goplanit.io.test.util;
 
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.TrafficAssignmentConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmType;
import org.goplanit.choice.ChoiceModel;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.physical.PhysicalCostConfigurator;
import org.goplanit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.goplanit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.goplanit.demands.Demands;
import org.goplanit.io.output.formatter.PlanItOutputFormatter;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.output.configuration.LinkOutputTypeConfiguration;
import org.goplanit.output.configuration.OdOutputTypeConfiguration;
import org.goplanit.output.configuration.OutputConfiguration;
import org.goplanit.output.configuration.PathOutputTypeConfiguration;
import org.goplanit.output.enums.OdSkimSubOutputType;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.PathOutputIdentificationType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.output.formatter.OutputFormatter;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.path.choice.PathChoice;
import org.goplanit.path.choice.StochasticPathChoice;
import org.goplanit.path.choice.StochasticPathChoiceConfigurator;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.sdinteraction.smoothing.MSASmoothing;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
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
  protected PlanItInputBuilder4Testing planItInputBuilder;
  
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
  
  /**
   * Run a test case and store the results in a MemoryOutputFormatter, most generic form with all consumers passable but could be nulls
   *
   * @param setLinkOutputTypeConfigurationProperties lambda function to set output properties being used
   * @param setCostParameters lambda function which sets parameters of cost function
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  protected TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> setupAndExecuteAssignment(
      final Consumer<LinkOutputTypeConfiguration> setLinkOutputTypeConfigurationProperties,
      final TriConsumer<LayeredNetwork<?,?>, PhysicalCostConfigurator<?>, PlanItInputBuilder4Testing> setCostParameters) throws Exception {
                
    if (setCostParameters != null) {
      setCostParameters.accept(network, physicalCostConfigurator, planItInputBuilder);
    }
    
    /* Virtual cost */
    if (useFixedConnectoidTravelTimeCost) {
      taConfigurator.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
    } else {
      taConfigurator.createAndRegisterVirtualCost(SpeedConnectoidTravelTimeCost.class.getCanonicalName());
    }
    
    /* Link output type consumer */
    if(setLinkOutputTypeConfigurationProperties != null) {
      setLinkOutputTypeConfigurationProperties.accept(linkOutputTypeConfiguration);
    }
    
    /* execute */
    project.executeAllTrafficAssignments();
    
    /* output */
    var testOutputDtoX = new TestOutputDto(memoryOutputFormatter, project, planItInputBuilder);
    return testOutputDtoX;
  }

  /**
   * Constructor
   *
   * @param inputPath to use
   * @param outputPath to use
   * @param description to use
   * @param assignmentType to apply
   */
  public PlanItIoTestRunner(String inputPath, String outputPath, String description, String assignmentType) {
    this.projectPath = inputPath;

    try {
      this.planItInputBuilder = new PlanItInputBuilder4Testing(projectPath);
      this.project = new CustomPlanItProject(planItInputBuilder);

      /* RAW INPUT START -------------------------------- */
      {
        this.network = (MacroscopicNetwork) project.createAndRegisterInfrastructureNetwork(MacroscopicNetwork.class.getCanonicalName());
        this.zoning = project.createAndRegisterZoning(network);
        this.demands = project.createAndRegisterDemands(zoning, network);
      }
      /* RAW INPUT END ----------------------------------- */

      /* TRAFFIC ASSIGNMENT */
      if(assignmentType.equals(TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT))
      {
        this.taConfigurator =
                project.createAndRegisterTrafficAssignment(
                        TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT, demands, zoning, network);

        /* Physical cost - BPR */
        this.physicalCostConfigurator = taConfigurator.createAndRegisterPhysicalCost(AbstractPhysicalCost.BPR);

      }else{
        this.taConfigurator =
                project.createAndRegisterTrafficAssignment(
                        assignmentType, demands, zoning, network);

        // steady state configurator
        this.physicalCostConfigurator = taConfigurator.createAndRegisterPhysicalCost(AbstractPhysicalCost.STEADY_STATE);
        var sLtm = ((StaticLtmConfigurator)taConfigurator);

        // defaults 5/2024, but set explicitly so tests will not break if defaults change
        sLtm.setType(StaticLtmType.PATH_BASED);
        sLtm.createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);
        var pathChoice = (StochasticPathChoiceConfigurator) sLtm.createAndRegisterPathChoice(PathChoice.STOCHASTIC);
        pathChoice.createAndRegisterChoiceModel(ChoiceModel.MNL);
      }

      /* Smoothing - MSA */
      taConfigurator.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());

      /* OUTPUT FORMAT CONFIGURATION */
      {
        /* Xml PlanItOutputFormatter */
        this.xmlOutputFormatter = (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER);
        xmlOutputFormatter.setXmlNameRoot(description);
        xmlOutputFormatter.setCsvNameRoot(description);
        xmlOutputFormatter.setOutputDirectory(outputPath);
        taConfigurator.registerOutputFormatter(xmlOutputFormatter);

        // MemoryOutputFormatter
        this.memoryOutputFormatter = (MemoryOutputFormatter) project.createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());
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

        /* for this test we prefer to get out flows and capacities in vehicles rather than pcus (no difference in result with pcu=1, only in metadata)*/
        linkOutputTypeConfiguration.overrideOutputPropertyUnits(OutputPropertyType.CAPACITY_PER_LANE, Unit.VEH_HOUR);
        linkOutputTypeConfiguration.overrideOutputPropertyUnits(OutputPropertyType.FLOW, Unit.VEH_HOUR);

        /* OD OUTPUT CONFIGURATION */
        final OdOutputTypeConfiguration originDestinationOutputTypeConfiguration = (OdOutputTypeConfiguration) taConfigurator.activateOutput(OutputType.OD);
        originDestinationOutputTypeConfiguration.deactivateOdSkimOutputType(OdSkimSubOutputType.NONE);
        originDestinationOutputTypeConfiguration.removeProperty(OutputPropertyType.TIME_PERIOD_XML_ID);

        /* PATH OUTPUT CONFIGURATION */
        final PathOutputTypeConfiguration pathOutputTypeConfiguration = (PathOutputTypeConfiguration) taConfigurator.activateOutput(OutputType.PATH);
        pathOutputTypeConfiguration.setPathIdentificationType(PathOutputIdentificationType.NODE_XML_ID);
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
    this(projectPath, projectPath, description, TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT);
  }
  
  /**
   * Run a test case with a default configuration and no additional changes via consumers. Store the results in a MemoryOutputFormatter.
   *
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */  
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> setupAndExecuteDefaultAssignment() throws Exception {
    return setupAndExecuteAssignment(null, null);
  }   

  /**
   * Run a test case with a custom physical cost configuration. Store the results in a MemoryOutputFormatter.
   *
   * @param setPhysicalCostParameters lambda function which sets parameters of cost function
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> setupAndExecuteWithPhysicalCostConfiguration(
      final TriConsumer<LayeredNetwork<?,?>, PhysicalCostConfigurator<?>, PlanItInputBuilder4Testing> setPhysicalCostParameters) throws Exception {
    return setupAndExecuteAssignment(null, setPhysicalCostParameters);
  }
  
  /**
   * Run a test case with a custom link output type configuration consumer. Store the results in a MemoryOutputFormatter.
   *
   * @param linkOutputTypeConfigurationConsumer lambda function which sets parameters of link output type configuration in addition to default settings
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */  
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> setupAndExecuteWithCustomLinkOutputConfiguration(
      Consumer<LinkOutputTypeConfiguration> linkOutputTypeConfigurationConsumer) throws Exception {
    return setupAndExecuteAssignment(linkOutputTypeConfigurationConsumer, null);    
  }  
  
  /**
   * Run a test case with a custom link output type configuration and Bpr cost consumers. Store the results in a MemoryOutputFormatter.
   *
   * @param setPhysicalCostParameters lambda function which sets parameters of cost function
   * @param linkOutputTypeConfigurationConsumer lambda function which sets parameters of link output type configuration in additino to default settings
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */    
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> setupAndExecuteWithCustomBprAndLinkOutputTypeConfiguration(
      TriConsumer<LayeredNetwork<?, ?>, PhysicalCostConfigurator<?>, PlanItInputBuilder4Testing> setPhysicalCostParameters,
      Consumer<LinkOutputTypeConfiguration> linkOutputTypeConfigurationConsumer) throws Exception {
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
  
  /** register (general) initial cost on the test based on the passed in location where to find it
   * 
   * @param initialCostLocation to parse initial costs from
   * @throws PlanItException thrown if error
   */
  public void registerInitialLinkSegmentCost(String initialCostLocation) throws PlanItException {
    var initialCost = project.createAndRegisterInitialLinkSegmentCost(network, initialCostLocation);
    taConfigurator.registerInitialLinkSegmentCost(initialCost);
  }

  /** register initial cost specific to a time period on the test based on the passed in location where to find it
   * 
   * @param timePeriodXmlId to use
   * @param initialCostLocation to parse initial costs from
   * @throws PlanItException thrown if error
   */  
  public void registerInitialLinkSegmentCostByTimePeriod(String timePeriodXmlId, String initialCostLocation) throws PlanItException {
    TimePeriod timePeriod = demands.timePeriods.getByXmlId(timePeriodXmlId);
    final var initialCost = project.createAndRegisterInitialLinkSegmentCost(network, initialCostLocation,timePeriod);
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

}