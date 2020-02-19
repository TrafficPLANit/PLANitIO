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
import org.planit.route.choice.StochasticRouteChoice;
import org.planit.route.choice.logit.MultinomialLogit;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.supply.fundamentaldiagram.NewellFundamentalDiagram;

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
        	// register ELTM as eligible assignment
        	
        	//TODO: not great that we must register it separately as an option. Probably better to simply
        	//allow any implementation of the registered (meta)types and not bother with the actual implemented subclasses
        	project.registerEligibleTrafficComponentClass(ELTM.class);

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
        final String routeInputPath =             "<insert path to input path directory here>";

        try{
        	// Create a simple PLANit project with all the default settings
        	final PlanItProject project = new PlanItProject(projectPath);

            // INITIALISE INPUTS
            final PhysicalNetwork physicalNetwork             = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
            final Zoning zoning                               = project.createAndRegisterZoning(physicalNetwork);
            final Demands demands                             = project.createAndRegisterDemands(zoning);
            // INITIALISE OUTPUT FORMATTERS
            final MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project.createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());
            // route sets are defined on the project level and linked to a network, zoning combination
            project.createAndRegisterODRouteSets(physicalNetwork, zoning, routeInputPath);

            //:TODO: to be implemented
            // alternatively routes can be generated with a route generator
//        	final RouteGenerator routeGenerator = project.createRouteGenerator(physicalNetwork);
//        	final ODRouteSet routeSet = routeGenerator.generateODRouteSet(zoning);
//        	project.registerODRouteSet(routeSet);

            // this saves an additional call AND will allow us to provide defaults such as using the
            // triangular FD on eLTM because we already know the network (for example) in the constructor of the
            // builder!
        	final ELTMTrafficAssignmentBuilder taBuilder =
        			(ELTMTrafficAssignmentBuilder) project.createAndRegisterTrafficAssignment(
        					ELTM.class.getCanonicalName(), demands, zoning, physicalNetwork);

        	// CREATE/REGISTER ASSIGNMENT COMPONENTS

            // eLTM only supports a triangular fundamental diagram, but it still needs to be registered
            taBuilder.createAndRegisterFundamentalDiagram(NewellFundamentalDiagram.class.getCanonicalName(), physicalNetwork);

            // iteration smoothing: MSA
            taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());

        	// stochastic route choice is a route choice type that requires a logit model and routes as input
        	final StochasticRouteChoice sueRouteChoice =
        			(StochasticRouteChoice) taBuilder.createAndRegisterRouteChoice(StochasticRouteChoice.class.getCanonicalName());
        	// MNL for route choice
        	sueRouteChoice.createAndRegisterLogitModel(MultinomialLogit.class.getCanonicalName());
        	// register a fixed od route set
        	sueRouteChoice.RegisterODRouteMatrix(project.odRouteSets.getFirstODRouteSets().getFirstODRouteMatrix());

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
