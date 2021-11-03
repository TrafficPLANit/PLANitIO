package org.goplanit.io.test.util;
 
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.traditionalstatic.TraditionalStaticAssignmentConfigurator;
import org.goplanit.cost.physical.AbstractPhysicalCost;
import org.goplanit.cost.physical.BPRConfigurator;
import org.goplanit.cost.physical.initial.InitialLinkSegmentCost;
import org.goplanit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.goplanit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.goplanit.demands.Demands;
import org.goplanit.io.output.formatter.PlanItOutputFormatter;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.TransportLayerNetwork;
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
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.sdinteraction.smoothing.MSASmoothing;
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
public class PlanItIOTestRunner {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanItIOTestRunner.class.getCanonicalName());

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
  protected TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> setupAndExecuteAssignment(
      final Consumer<LinkOutputTypeConfiguration> setLinkOutputTypeConfigurationProperties,
      final TriConsumer<TransportLayerNetwork<?,?>, BPRConfigurator, PlanItInputBuilder4Testing> setCostParameters) throws Exception {
                
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
    TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> testOutputDtoX = 
        new TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing>(memoryOutputFormatter, project, planItInputBuilder);
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
        linkOutputTypeConfiguration.addProperty(OutputPropertyType.MODE_ID);
        linkOutputTypeConfiguration.addProperty(OutputPropertyType.DOWNSTREAM_NODE_ID);
        linkOutputTypeConfiguration.addProperty(OutputPropertyType.DOWNSTREAM_NODE_LOCATION);
        linkOutputTypeConfiguration.addProperty(OutputPropertyType.UPSTREAM_NODE_ID);
        linkOutputTypeConfiguration.addProperty(OutputPropertyType.UPSTREAM_NODE_LOCATION);
        linkOutputTypeConfiguration.addProperty(OutputPropertyType.LINK_SEGMENT_ID);
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
        pathOutputTypeConfiguration.addProperty(OutputPropertyType.RUN_ID);
        
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
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> setupAndExecuteDefaultAssignment() throws Exception {
    return setupAndExecuteAssignment(null, null);
  }   

  /**
   * Run a test case with a custom Bpr configuration. Store the results in a MemoryOutputFormatter.
   *
   * @param setBprCostParameters lambda function which sets parameters of cost function
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> setupAndExecuteWithCustomBprConfiguration(
      final TriConsumer<TransportLayerNetwork<?,?>, BPRConfigurator, PlanItInputBuilder4Testing> setBprCostParameters) throws Exception {
    return setupAndExecuteAssignment(null, setBprCostParameters);
  }
  
  /**
   * Run a test case with a custom link output type configuration consumer. Store the results in a MemoryOutputFormatter.
   *
   * @param linkOutputTypeConfigurationConsumer lambda function which sets parameters of link output type configuration in additino to default settings
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
   * @param setBprCostParameters lambda function which sets parameters of cost function
   * @param linkOutputTypeConfigurationConsumer lambda function which sets parameters of link output type configuration in additino to default settings
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */    
  public TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, PlanItInputBuilder4Testing> setupAndExecuteWithCustomBprAndLinkOutputTypeConfiguration(
      TriConsumer<TransportLayerNetwork<?, ?>, BPRConfigurator, PlanItInputBuilder4Testing> setBprCostParameters,
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
    TimePeriod timePeriod = demands.timePeriods.getByXmlId(timePeriodXmlId);
    final InitialLinkSegmentCost initialCost = project.createAndRegisterInitialLinkSegmentCost(network, initialCostLocation,timePeriod);
    taConfigurator.registerInitialLinkSegmentCost(timePeriod, initialCost.getTimePeriodCosts(timePeriod));    
  }

}