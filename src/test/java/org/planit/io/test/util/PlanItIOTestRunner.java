package org.planit.io.test.util;
 
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.assignment.traditionalstatic.TraditionalStaticAssignmentConfigurator;
import org.planit.cost.physical.AbstractPhysicalCost;
import org.planit.cost.physical.BPRConfigurator;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCostPeriod;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.demands.Demands;
import org.planit.input.InputBuilderListener;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.io.output.formatter.PlanItOutputFormatter;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.macroscopic.MacroscopicNetwork;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.ODOutputTypeConfiguration;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.PathOutputTypeConfiguration;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.PathOutputIdentificationType;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.project.CustomPlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.functionalinterface.TriConsumer;
import org.planit.utils.test.TestOutputDto;
import org.planit.zoning.Zoning;

/**
 * Helper class used by unit tests to conduct test runs with various configuration options set
 *
 * @author markr
 *
 */
public class PlanItIOTestRunner {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanItIOTestRunner.class.getCanonicalName());

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
  
  /** the traffic assignment configurated used */
  protected TraditionalStaticAssignmentConfigurator taConfigurator;
  
  /** Physical cost - Bpr configuration */
  protected BPRConfigurator bprPhysicalCost;
  
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
   * Run a test case and store the results in a MemoryOutputFormatter, most egneric form with all consumers passable but could be nulls
   *
   * @param setLinkOutputTypeConfigurationProperties lambda function to set output properties being used
   * @param setCostParameters lambda function which sets parameters of cost function
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  protected TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(
      final Consumer<LinkOutputTypeConfiguration> setLinkOutputTypeConfigurationProperties,
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters) throws Exception {
                
    if (setCostParameters != null) {
      setCostParameters.accept(network, bprPhysicalCost, planItInputBuilder);
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
    TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDtoX = new TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener>(memoryOutputFormatter, project, planItInputBuilder);
    return testOutputDtoX;
  }  
    
  /**
   * Constructor
   * 
   * @param projectPath to use
   * @param description to use
   */
  public PlanItIOTestRunner(String projectPath, String description) {
    this.projectPath = projectPath;
    
    try {
      this.planItInputBuilder = new PlanItInputBuilder(projectPath);
      this.project = new CustomPlanItProject(planItInputBuilder);
  
      /* RAW INPUT START -------------------------------- */
      {
        this.network = (MacroscopicNetwork) project.createAndRegisterInfrastructureNetwork(MacroscopicNetwork.class.getCanonicalName());
        this.zoning = project.createAndRegisterZoning(network);
        this.demands = project.createAndRegisterDemands(zoning, network); 
      }      
      /* RAW INPUT END ----------------------------------- */

      /* TRAFFIC ASSIGNMENT */
      {
        this.taConfigurator =
            (TraditionalStaticAssignmentConfigurator) project.createAndRegisterTrafficAssignment(
                TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT, demands, zoning, network);
        
        /* Smoothing - MSA */
        taConfigurator.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
        
        /* Physical cost - BPR */
        this.bprPhysicalCost = (BPRConfigurator) taConfigurator.createAndRegisterPhysicalCost(AbstractPhysicalCost.BPR);
      }
      
      /* OUTPUT FORMAT CONFIGURATION */
      {
        /* Xml PlanItOutputFormatter */
        this.xmlOutputFormatter = (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER);
        xmlOutputFormatter.setXmlNameRoot(description);
        xmlOutputFormatter.setCsvNameRoot(description);
        xmlOutputFormatter.setOutputDirectory(projectPath);    
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
        /* 27/2/2010
         * Note -> originally these we created as a default consumer, so if a non-default consumer is used for link output type configuration
         * these would not be set anymore in the original situation, but here they are, so this might cause some differences in results
         * Once verified, remove this comment
         */
        linkOutputTypeConfiguration.addProperty(OutputProperty.MODE_ID);
        linkOutputTypeConfiguration.addProperty(OutputProperty.DOWNSTREAM_NODE_ID);
        linkOutputTypeConfiguration.addProperty(OutputProperty.DOWNSTREAM_NODE_LOCATION);
        linkOutputTypeConfiguration.addProperty(OutputProperty.UPSTREAM_NODE_ID);
        linkOutputTypeConfiguration.addProperty(OutputProperty.UPSTREAM_NODE_LOCATION);
        linkOutputTypeConfiguration.addProperty(OutputProperty.LINK_SEGMENT_ID);
        linkOutputTypeConfiguration.addProperty(OutputProperty.CAPACITY_PER_LANE);
        linkOutputTypeConfiguration.addProperty(OutputProperty.NUMBER_OF_LANES);
        linkOutputTypeConfiguration.addProperty(OutputProperty.LENGTH);
        linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_XML_ID);
        linkOutputTypeConfiguration.removeProperty(OutputProperty.MAXIMUM_SPEED);
        
        /* OD OUTPUT CONFIGURATION */
        final ODOutputTypeConfiguration originDestinationOutputTypeConfiguration = (ODOutputTypeConfiguration) taConfigurator.activateOutput(OutputType.OD);
        originDestinationOutputTypeConfiguration.deactivateOdSkimOutputType(ODSkimSubOutputType.NONE);
        originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_XML_ID);

        /* PATH OUTPUT CONFIGURATION */
        final PathOutputTypeConfiguration pathOutputTypeConfiguration = (PathOutputTypeConfiguration) taConfigurator.activateOutput(OutputType.PATH);
        pathOutputTypeConfiguration.setPathIdentificationType(PathOutputIdentificationType.NODE_XML_ID);
        pathOutputTypeConfiguration.addProperty(OutputProperty.RUN_ID);
        
      }
      
    }catch(PlanItException e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("Unable to initialise PlanitIo testhelper");
    }
  }
  
  /**
   * Run a test case with a default configuration and no additional changes via consumers. Store the results in a MemoryOutputFormatter.
   * @return 
   *
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */  
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteDefaultAssignment() throws Exception {
    return setupAndExecuteAssignment(null, null);
  }   

  /**
   * Run a test case with a custom Bpr configuration. Store the results in a MemoryOutputFormatter.
   *
   * @param setBprCostParameters lambda function which sets parameters of cost function
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteWithCustomBprConfiguration(
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setBprCostParameters) throws Exception {
    return setupAndExecuteAssignment(null, setBprCostParameters);
  }
  
  /**
   * Run a test case with a custom link output type configuration consumer. Store the results in a MemoryOutputFormatter.
   *
   * @param linkOutputTypeConfigurationConsumer lambda function which sets parameters of link output type configuration in additino to default settings
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */  
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteWithCustomLinkOutputConfiguration(
      Consumer<LinkOutputTypeConfiguration> linkOutputTypeConfigurationConsumer) throws Exception {
    return setupAndExecuteAssignment(linkOutputTypeConfigurationConsumer, null);    
  }  
  
  /**
   * Run a test case with a custom link output type configuration and Bpr cost consumers. Store the results in a MemoryOutputFormatter.
   *
   * @param setBprCostParameters lambda function which sets parameters of cost function
   * @param linkOutputTypeConfigurationConsumer lambda function which sets parameters of link output type configuration in additino to default settings
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */    
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteWithCustomBprAndLinkOutputTypeConfiguration(
      TriConsumer<InfrastructureNetwork<?, ?>, BPRConfigurator, InputBuilderListener> setBprCostParameters,
      Consumer<LinkOutputTypeConfiguration> setLinkOutputTypeConfiguration) throws Exception {
    return setupAndExecuteAssignment(setLinkOutputTypeConfiguration, setBprCostParameters);
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
    InitialLinkSegmentCost initialCost = project.createAndRegisterInitialLinkSegmentCost(network, initialCostLocation);
    taConfigurator.registerInitialLinkSegmentCost(initialCost);
  }

  /** register initial cost specific to a time period on the test based on the passed in location where to find it
   * 
   * @param timePeriodXmlId to use
   * @param initialCostLocation to parse initial costs from
   * @throws PlanItException thrown if error
   */  
  public void registerInitialLinkSegmentCostByTimePeriod(String timePeriodXmlId, String initialCostLocation) throws PlanItException {
    TimePeriod timePeriod = demands.timePeriods.getTimePeriodByXmlId(timePeriodXmlId);
    final InitialLinkSegmentCostPeriod initialCost = project.createAndRegisterInitialLinkSegmentCost(network, initialCostLocation,timePeriod);
    taConfigurator.registerInitialLinkSegmentCost(initialCost);    
  }

}