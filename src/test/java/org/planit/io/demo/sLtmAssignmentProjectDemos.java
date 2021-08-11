package org.planit.io.demo;

import java.util.List;
import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.assignment.sltm.StaticLtmConfigurator;
import org.planit.assignment.traditionalstatic.TraditionalStaticAssignmentConfigurator;
import org.planit.cost.physical.BPRConfigurator;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.AbstractPhysicalCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCostPeriod;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.cost.virtual.SpeedVirtualCostConfigurator;
import org.planit.cost.virtual.AbstractVirtualCost;
import org.planit.demands.Demands;
import org.planit.gap.LinkBasedRelativeGapConfigurator;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.io.output.formatter.PlanItOutputFormatter;
import org.planit.io.project.PlanItProject;
import org.planit.io.project.PlanItSimpleProject;
import org.planit.network.MacroscopicNetwork;
import org.planit.network.Network;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.ODOutputTypeConfiguration;
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
import org.planit.supply.fundamentaldiagram.FundamentalDiagram;
import org.planit.utils.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.zoning.Zoning;

/**
 * Demo class. Show casing how to setup typical sLTM assignment projects in PLANit. Curretnly they exist to showcase the proposed
 * user options for sLTM even though sLTM itself is not yet available within PLANit (11_8_2021), based on these examples we will start implemenbting the underlying 
 * algorithms based on Raadsen and Bliemer (2021): General solution scheme for the Static Link Transmission Model .
 * 
 * Future Note: if any of the demos no longer work or do not compile, fix them here but also in the Example section of the Java manual website as these are all
 * copies of those examples.
 * 
 * TODO: Somehow include assertions and include in testing cycle, this is currently not done yet
 *
 * @author markr
 *
 */
public class sLtmAssignmentProjectDemos {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(sLtmAssignmentProjectDemos.class.getCanonicalName());

  /**
   * Setup a stock standard sLTM assignment
   */
  public static void minimumExampleSimpleProjectDemo() {
    final String projectPath = "<insert the project path here>";

    try {
      // Create a simple PLANit project with all the default settings
      final PlanItSimpleProject project = new PlanItSimpleProject(projectPath);

      project.createAndRegisterTrafficAssignment(TrafficAssignment.SLTM);

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }       
  
  /**
   * Setup a stock standard sLTM assignment with the spillback option switched off
   */
  public static void minimumExampleNoSpillbackSimpleProjectDemo() {
    final String projectPath = "<insert the project path here>";

    try {
      // Create a simple PLANit project with all the default settings
      final PlanItSimpleProject project = new PlanItSimpleProject(projectPath);

      /* disable the use of storage capacities on links, resulting in a point queue model version of sLTM */
      StaticLtmConfigurator sLtm = (StaticLtmConfigurator) project.createAndRegisterTrafficAssignment(TrafficAssignment.SLTM);
      sLtm.disableLinkStorageConstraints();      

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
    }
  }  
  
  
  /**
   * Setup a project with traditional static assignment and physical cost component
   */
  public static void TrafficAssignmentPhysicalCostDemo() {
    try {

      final PlanItSimpleProject project = new PlanItSimpleProject();
      
      StaticLtmConfigurator sLtm = (StaticLtmConfigurator) project.createAndRegisterTrafficAssignment(TrafficAssignment.SLTM);
      
      // * NEW *
      // set FD to use --> underneath this should be generalised in registering two separate branches (FF,Congested)
      // come up with a way on how to integrate this with cost (or not), should this be a component anyway or just a setting on the 
      // assignment generator? (in case it requires link level model parameters it is a component via builder, otherwise not would be my proposal)
      sLtm.createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);
      
      // sLTM might compute cost on a per link or per path basis. Likely this can be a setting as well as it does not require any information
      // from links (unlike BPR link configuration with link parameters), but possibly it would be good to add
      //createAndRegisterPhysicalCost(TODO)

      project.executeAllTrafficAssignments();
    } catch (final Exception e) {
      // do something
    }
  }   
  
  /**
   * Setup a project with a sLTM compatible gap function
   */
  public static void TrafficAssignmentGapFunctionDemo() {
    //TODO
  }  
  
  /**
   * Setup a project with an sLTM compatible stop criterion
   */
  public static void TrafficAssignmentStopCriterionDemo() {
    //TODO
  }  
  
  /**
   * Setup a project with multiple output formatters
   */
  public static void MultipleOutputFormattersDemo() {
    try {
      final CustomPlanItProject project = new CustomPlanItProject(new PlanItInputBuilder("<insert the project path here>"));
     
      MacroscopicNetwork network = (MacroscopicNetwork) project.createAndRegisterInfrastructureNetwork(Network.MACROSCOPIC_NETWORK);
      Zoning zoning = project.createAndRegisterZoning(network);
      Demands demands = project.createAndRegisterDemands(zoning, network);
      
      // *NEW*
      OutputFormatter defaultOutputFormatter = project.createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER);
      MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project.createAndRegisterOutputFormatter(OutputFormatter.MEMORY_OUTPUT_FORMATTER);

      StaticLtmConfigurator ta = (StaticLtmConfigurator) 
          project.createAndRegisterTrafficAssignment(
            TrafficAssignment.SLTM, demands, zoning, network);
      
      // *NEW*
      ta.registerOutputFormatter(defaultOutputFormatter);
      ta.registerOutputFormatter(memoryOutputFormatter); 

      project.executeAllTrafficAssignments();
      
      // * NEW *
      MemoryOutputIterator outputIterator = memoryOutputFormatter.getIterator(
          network.getModes().getFirst(), 
          demands.timePeriods.getFirst(), 
          memoryOutputFormatter.getLastIteration(), 
          OutputType.LINK);
      
      // * NEW *
      int idPosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(OutputType.LINK, OutputProperty.LINK_SEGMENT_ID);
      int flowPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputProperty.FLOW);
      int linkSegmentCostPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputProperty.LINK_SEGMENT_COST);
      
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
   * Setup a minimum configuration standard traditional static assignment:
   * - Use the full fledged configuration objects, but
   * - Explicitly set all defaults for demo purposes
   */
  public static void maximumExampleDemo() {
    //TODO
  }
}
