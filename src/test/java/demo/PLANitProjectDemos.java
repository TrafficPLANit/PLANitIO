package demo;

import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.demands.Demands;
import org.planit.logging.PlanItLogger;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.OriginDestinationOutputTypeConfiguration;
import org.planit.output.configuration.PathOutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.PathIdType;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.planitio.output.formatter.PlanItOutputFormatter;
import org.planit.planitio.project.PlanItProject;
import org.planit.planitio.project.PlanItSimpleProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.zoning.Zoning;

/**
 * Demo class. Show casing how to setup a typical PLANit projects
 * 
 * @author mraa2518
 *
 */
public class PLANitProjectDemos {


    /**
     * Setup a stock standard traditional static assignment
     */
    public static void minimumExampleDemo(){
        String projectPath =                "<insert the project path here>";

        try{
        	// Create a simple PLANit project with all the default settings
        	PlanItSimpleProject project = new PlanItSimpleProject(projectPath);
        	        	
        	project.createAndRegisterDeterministicAssignment(
        			TraditionalStaticAssignment.class.getCanonicalName());
        	
            project.executeAllTrafficAssignments();
        }catch (Exception e)
        {
            PlanItLogger.severe(e.getMessage());
        }
    }
    
    /**
     * Setup a mininum configuration standard traditional static assignment:
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
            CapacityRestrainedTrafficAssignmentBuilder taBuilder = 
                    (CapacityRestrainedTrafficAssignmentBuilder) project.createAndRegisterDeterministicAssignment(TraditionalStaticAssignment.class.getCanonicalName());
    
            // CREATE/REGISTER ASSIGNMENT COMPONENTS
            // OD: demands and zoning structure and network
            taBuilder.registerDemandZoningAndNetwork(demands, zoning, physicalNetwork);
            // Initial (physical) link segment cost
            taBuilder.registerInitialLinkSegmentCost(initialCost);
            // physical links: BPR cost function
            taBuilder.createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
            // virtual links: fixed cost function
    		int numberOfConnectoidSegments = zoning.getVirtualNetwork().connectoids.toList().size() * 2;
    		FixedConnectoidTravelTimeCost fixedConnectoidTravelTimeCost = (FixedConnectoidTravelTimeCost) taBuilder.createAndRegisterVirtualTravelTimeCostFunction(FixedConnectoidTravelTimeCost.class.getCanonicalName());
    		fixedConnectoidTravelTimeCost.populateToZero(numberOfConnectoidSegments);
            // iteration smoothing: MSA
            taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
            // Output formatter: PLANitIO + MEMORY
            taBuilder.registerOutputFormatter(xmlOutputFormatter);
            taBuilder.registerOutputFormatter(memoryOutputFormatter);
            
            // COMPONENT CONFIGURATION - INPUT 
            taBuilder.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
            taBuilder.getGapFunction().getStopCriterion().setEpsilon(gapEpsilon);
      
            //COMPONENT CONFIGURATION - OUTPUT CONTENTS
            // General:       
            taBuilder.getOutputConfiguration().setPersistOnlyFinalIteration(true);        
            // link outputs:    ON + example configuration
            LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) taBuilder.activateOutput(OutputType.LINK);
            linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_ID);
            // OD outputs:      ON + example configuration
            OriginDestinationOutputTypeConfiguration originDestinationOutputTypeConfiguration = (OriginDestinationOutputTypeConfiguration) taBuilder.activateOutput(OutputType.OD);
            originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
            originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.RUN_ID);        
            // PATH outputs:    ON + example configuration
            PathOutputTypeConfiguration pathOutputTypeConfiguration = (PathOutputTypeConfiguration) taBuilder.activateOutput(OutputType.PATH);
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
