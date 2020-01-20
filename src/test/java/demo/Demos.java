package demo;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.logging.PlanItLogger;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.OriginDestinationOutputTypeConfiguration;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.PathOutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.PathIdType;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.planitio.PlanItMain;
import org.planit.planitio.output.formatter.PlanItOutputFormatter;
import org.planit.planitio.project.PlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.zoning.Zoning;

/**
 * Demo class. Showcasing how to setup a typical PLANit projects
 * 
 * @author mraa2518
 *
 */
public class Demos {


    /**
     * Setup a stock standard traditional static assignment:
     * -    Use the full fledged configuration objects, but
     * -    Explicitly set all defaults for demo purposes
     */
    public static void maximumExampleDemo(){
        // CONFIGURATION INPUT
        String projectPath =                "<insert the project path here>";
        String logFile =                    "<insert logFile including path and extension here>";
        String initialCsvCostFilePath =     "<insert the initial cost file path here>";
        String outputFileName =             "<insert base output file name without extension here>";
        String outputPath =                 "<insert path to output directory here>"; 
        
        int maxIterations =                 100;
        double gapEpsilon =                 0.001;
        
        //---------------------------------------------------------------------------------------------------------------------------------------

        try{
            // INITIALISE PLANit PROJECT
            PlanItProject project = new PlanItProject(projectPath);
            PlanItLogger.setLogging(logFile, PlanItProject.class);
    
            // INITIALISE INPUTS
            PhysicalNetwork physicalNetwork             = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
            Zoning zoning                               = project.createAndRegisterZoning(physicalNetwork);
            Demands demands                             = project.createAndRegisterDemands(zoning);
            InitialLinkSegmentCost initialCost          = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCsvCostFilePath, demands.getRegisteredTimePeriods().first());
            // INITIALISE OUTPUT FORMATTERS
            PlanItOutputFormatter xmlOutputFormatter    = (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(PlanItOutputFormatter.class.getCanonicalName());
            MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project.createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());        
    
            // CHOOSE TRADITIONAL STATIC ASSIGNMENT --> COLLECT BUILDER
            DeterministicTrafficAssignment assignment = project.createAndRegisterDeterministicAssignment(TraditionalStaticAssignment.class.getCanonicalName());
            CapacityRestrainedTrafficAssignmentBuilder taBuilder = (CapacityRestrainedTrafficAssignmentBuilder) assignment.getBuilder();
    
            // CREATE/REGISTER ASSIGNMENT COMPONENTS
            // network
            taBuilder.registerPhysicalNetwork(physicalNetwork);
            // OD: demands and zoning structure
            taBuilder.registerDemandsAndZoning(demands, zoning);
            // Initial (physical) link segment cost
            taBuilder.registerInitialLinkSegmentCost(initialCost);
            // physical links: BPR cost function
            taBuilder.createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
            // virtual links: fixed cost function
            taBuilder.createAndRegisterVirtualTravelTimeCostFunction(SpeedConnectoidTravelTimeCost.class.getCanonicalName());
            // iteration smoothing: MSA
            taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
            // Output formatter: PLANitIO + MEMORY
            taBuilder.registerOutputFormatter(xmlOutputFormatter);
            taBuilder.registerOutputFormatter(memoryOutputFormatter);
            
            // COMPONENT CONFIGURATION - INPUT 
            assignment.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
            assignment.getGapFunction().getStopCriterion().setEpsilon(gapEpsilon);
      
            //COMPONENT CONFIGURATION - OUTPUT CONTENTS
            // General:       
            assignment.getOutputConfiguration().setPersistOnlyFinalIteration(true);        
            // link outputs:    ON + example configuration
            LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) assignment.activateOutput(OutputType.LINK);
            linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_ID);
            // OD outputs:      ON + example configuration
            OriginDestinationOutputTypeConfiguration originDestinationOutputTypeConfiguration = (OriginDestinationOutputTypeConfiguration) assignment.activateOutput(OutputType.OD);
            originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
            originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.RUN_ID);        
            // PATH outputs:    ON + example configuration
            PathOutputTypeConfiguration pathOutputTypeConfiguration = (PathOutputTypeConfiguration) assignment.activateOutput(OutputType.PATH);
            pathOutputTypeConfiguration.setPathIdType(PathIdType.NODE_EXTERNAL_ID);
            
            // COMPONENT CONFIGURATION - PLANitIO OUTPUT FORMAT
            xmlOutputFormatter.setXmlNameRoot(outputFileName);
            xmlOutputFormatter.setCsvNameRoot(outputFileName);
            xmlOutputFormatter.setOutputDirectory(outputPath);
    
            // EXECUTE ASSIGNMENTS
            project.executeAllTrafficAssignments();
        }catch (Exception e)
        {
            PlanItLogger.severe(e.getMessage());
        }
    }
}
