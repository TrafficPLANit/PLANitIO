package demo;

import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.demands.Demands;
import org.planit.logging.PlanItLogger;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Zoning;
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
import org.planit.trafficassignment.builder.TraditionalStaticAssignmentBuilder;

/**
 * Demo class. Show casing how to setup typical static assignment PLANit projects
 *
 * @author markr
 *
 */
public class PLANitStaticAssignmentProjectDemos {


    /**
     * Setup a stock standard traditional static assignment
     */
    public static void minimumExampleDemo(){
        final String projectPath =                "<insert the project path here>";

        try{
        	// Create a simple PLANit project with all the default settings
        	final PlanItSimpleProject project = new PlanItSimpleProject(projectPath);

        	project.createAndRegisterTrafficAssignment(
        			TraditionalStaticAssignment.class.getCanonicalName());

            project.executeAllTrafficAssignments();
        }catch (final Exception e)
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
        final String projectPath =                "<insert the project path here>";
        final String logFile =                    "<insert logFile including path and extension here>";
        final String initialCsvCostFilePath =     "<insert the initial cost file path here>";
        final String outputFileName =             "<insert base output file name without extension here>";
        final String outputPath =                 "<insert path to output directory here>";

        final int maxIterations =                 100;
        final double gapEpsilon =                 0.001;

        //---------------------------------------------------------------------------------------------------------------------------------------

        try{
            // INITIALISE PLANit PROJECT
            final PlanItProject project = new PlanItProject(projectPath);
            PlanItLogger.setLogging(logFile, PlanItProject.class);

            // INITIALISE INPUTS
            final PhysicalNetwork physicalNetwork             = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
            final Zoning zoning                               = project.createAndRegisterZoning(physicalNetwork);
            final Demands demands                             = project.createAndRegisterDemands(zoning);
            final InitialLinkSegmentCost initialCost          = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCsvCostFilePath, demands.getRegisteredTimePeriods().first());
            // INITIALISE OUTPUT FORMATTERS
            final PlanItOutputFormatter xmlOutputFormatter    = (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(PlanItOutputFormatter.class.getCanonicalName());
            final MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project.createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());

            // CHOOSE TRADITIONAL STATIC ASSIGNMENT --> COLLECT BUILDER
            final TraditionalStaticAssignmentBuilder taBuilder =
                    (TraditionalStaticAssignmentBuilder) project.createAndRegisterTrafficAssignment(TraditionalStaticAssignment.class.getCanonicalName());

            // CREATE/REGISTER ASSIGNMENT COMPONENTS
            // OD: demands and zoning structure and network
            taBuilder.registerDemandZoningAndNetwork(demands, zoning, physicalNetwork);
            // Initial (physical) link segment cost
            taBuilder.registerInitialLinkSegmentCost(initialCost);
            // physical links: BPR cost function
            taBuilder.createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
            // virtual links: fixed cost function
    		final int numberOfConnectoidSegments = zoning.getVirtualNetwork().connectoids.toList().size() * 2;
    		final FixedConnectoidTravelTimeCost fixedConnectoidTravelTimeCost = (FixedConnectoidTravelTimeCost) taBuilder.createAndRegisterVirtualTravelTimeCostFunction(FixedConnectoidTravelTimeCost.class.getCanonicalName());
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
            final LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) taBuilder.activateOutput(OutputType.LINK);
            linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_ID);
            // OD outputs:      ON + example configuration
            final OriginDestinationOutputTypeConfiguration originDestinationOutputTypeConfiguration = (OriginDestinationOutputTypeConfiguration) taBuilder.activateOutput(OutputType.OD);
            originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
            originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.RUN_ID);
            // PATH outputs:    ON + example configuration
            final PathOutputTypeConfiguration pathOutputTypeConfiguration = (PathOutputTypeConfiguration) taBuilder.activateOutput(OutputType.PATH);
            pathOutputTypeConfiguration.setPathIdType(PathIdType.NODE_EXTERNAL_ID);

            // COMPONENT CONFIGURATION - PLANitIO OUTPUT FORMAT
            xmlOutputFormatter.setXmlNameRoot(outputFileName);
            xmlOutputFormatter.setCsvNameRoot(outputFileName);
            xmlOutputFormatter.setOutputDirectory(outputPath);

            // EXECUTE ASSIGNMENTS
            project.executeAllTrafficAssignments();
        }catch (final Exception e)
        {
            PlanItLogger.severe(e.getMessage());
        }
    }
}
