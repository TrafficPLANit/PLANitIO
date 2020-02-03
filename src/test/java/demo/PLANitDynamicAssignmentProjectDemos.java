package demo;

import org.planit.demands.Demands;
import org.planit.logging.PlanItLogger;
import org.planit.ltm.trafficassignment.ELTM;
import org.planit.ltm.trafficassignment.ELTMTrafficAssignmentBuilder;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.planitio.project.PlanItProject;
import org.planit.planitio.project.PlanItSimpleProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;

/**
 * Demo class. Show casing how to setup typical dynamic assignment PLANit projects
 *
 * @author markr
 *
 */
public class PLANitDynamicAssignmentProjectDemos {

    /**
     * Setup a stock standard eLTM assignment
     */
    public static void minimumExampleDemo(){
        final String projectPath =                "<insert the project path here>";

        try{
        	// Create a simple PLANit project with all the default settings
        	final PlanItSimpleProject project = new PlanItSimpleProject(projectPath);

        	project.createAndRegisterTrafficAssignment(ELTM.class.getCanonicalName());

            project.executeAllTrafficAssignments();
        }catch (final Exception e)
        {
            PlanItLogger.severe(e.getMessage());
        }
    }

    /**
     * Setup a maximum configuration eLTM dynamic traffic assignment
     */
    public static void maximumExampleDemo(){
        // CONFIGURATION INPUT
        final String projectPath =                "<insert the project path here>";
        final String logFile =                    "<insert logFile including path and extension here>";
        final String outputFileName =             "<insert base output file name without extension here>";
        final String outputPath =                 "<insert path to output directory here>";

        try{
        	// Create a simple PLANit project with all the default settings
        	final PlanItProject project = new PlanItProject(projectPath);

            // INITIALISE INPUTS
            final PhysicalNetwork physicalNetwork             = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
            final Zoning zoning                               = project.createAndRegisterZoning(physicalNetwork);
            final Demands demands                             = project.createAndRegisterDemands(zoning);
            // INITIALISE OUTPUT FORMATTERS
            final MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project.createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());


        	final ELTMTrafficAssignmentBuilder taBuilder =
        			(ELTMTrafficAssignmentBuilder) project.createAndRegisterTrafficAssignment(ELTM.class.getCanonicalName());

            // CREATE/REGISTER ASSIGNMENT COMPONENTS
            // OD: demands and zoning structure and network
            taBuilder.registerDemandZoningAndNetwork(demands, zoning, physicalNetwork);
            // physical links: BPR cost function
            //final CumulativeCost physicalCumulativeCost = (CumulativeCost) taBuilder.createAndRegisterPhysicalCost(CumulativeCost.class.getCanonicalName());
            // virtual links: fixed cost function
    		//final CumulativeCost virtualCumulativeCost = (CumulativeCost) taBuilder.createAndRegisterVirtualTravelTimeCostFunction(CumulativeCost.class.getCanonicalName());
            // iteration smoothing: MSA
            taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
            // Output formatter: MEMORY
            taBuilder.registerOutputFormatter(memoryOutputFormatter);


            // EXECUTE ASSIGNMENTS
            project.executeAllTrafficAssignments();

            project.executeAllTrafficAssignments();
        }catch (final Exception e)
        {
            PlanItLogger.severe(e.getMessage());
        }
    }
}
