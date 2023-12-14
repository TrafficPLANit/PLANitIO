package org.goplanit.io.demo;

import java.util.logging.Logger;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.ltm.eltm.EventBasedLtm;
import org.goplanit.assignment.ltm.eltm.EventBasedLtmConfigurator;
import org.goplanit.choice.ChoiceModel;
import org.goplanit.choice.logit.MultinomialLogitConfigurator;
import org.goplanit.demands.Demands;
import org.goplanit.io.project.PlanItProject;
import org.goplanit.io.project.PlanItSimpleProject;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.path.choice.PathChoice;
import org.goplanit.path.choice.StochasticPathChoiceConfigurator;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.zoning.Zoning;

/**
 * Demo class. Show casing how to setup typical dynamic assignment PLANit projects
 *
 * @author markr
 *
 */
public class DynamicAssignmentProjectDemos {
  
    /** the logger */
    private static final Logger LOGGER = Logger.getLogger(DynamicAssignmentProjectDemos.class.getCanonicalName());   

    /**
     * Setup a stock standard eLTM assignment
     */
    public static void minimumExampleDemo(){
        final String projectPath =                "<insert the project path here>";

        try{
          // Create a simple PLANit project with all the default settings
          final PlanItSimpleProject project = new PlanItSimpleProject(projectPath);
          // register ELTM as eligible assignment
          
          /*TODO: not great that we must register it separately as an option. Probably better to simply allow
           any implementation of the registered (meta)types and not bother with the actual implemented subclasses */
          project.registerEligibleTrafficComponentClass(EventBasedLtm.class);
          project.createAndRegisterTrafficAssignment(EventBasedLtm.class.getCanonicalName());

            project.executeAllTrafficAssignments();
        } catch (final PlanItException e) {
          LOGGER.severe(e.getMessage());
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
          final MacroscopicNetwork theNetwork          = (MacroscopicNetwork) project.createAndRegisterInfrastructureNetwork(MacroscopicNetwork.class.getCanonicalName());
          final Zoning zoning                          = project.createAndRegisterZoning(theNetwork);
          final Demands demands                        = project.createAndRegisterDemands(zoning, theNetwork);

          // INITIALISE OUTPUT FORMATTERS
          final MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project.createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());
          // route sets are defined on the project level and linked to a network layer, zoning combination
          project.createAndRegisterOdPathSets(theNetwork.getTransportLayers().getFirst(), zoning, routeInputPath);

          //:TODO: to be implemented
          // alternatively paths can be generated with a route generator
              

          final EventBasedLtmConfigurator eLTM = 
              (EventBasedLtmConfigurator) project.createAndRegisterTrafficAssignment(TrafficAssignment.ELTM, demands, zoning, theNetwork);

          // CREATE/REGISTER ASSIGNMENT COMPONENTS

          // eLTM only supports a triangular fundamental diagram, but it still needs to be registered
          eLTM.createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);

          // iteration smoothing: MSA
          eLTM.createAndRegisterSmoothing(Smoothing.MSA);

          // stochastic path choice is a path choice type that requires a logit model and paths as input
          final StochasticPathChoiceConfigurator suePathChoice = (StochasticPathChoiceConfigurator) eLTM.createAndRegisterPathChoice(PathChoice.STOCHASTIC);
          // MNL for path choice
          final var mnlConfigurator = (MultinomialLogitConfigurator) suePathChoice.createAndRegisterChoiceModel(ChoiceModel.MNL);
          // register a fixed od path set
          // mnlConfigurator.setFixedOdPathMatrix(project.odPathSets.getFirst().getFirstOdPathMatrix()); // no longer supported as we assume dynamic path set

          // Output formatter: MEMORY
          eLTM.registerOutputFormatter(memoryOutputFormatter);

          // EXECUTE ASSIGNMENTS
          project.executeAllTrafficAssignments();

            project.executeAllTrafficAssignments();
        } catch (final PlanItException e) {
          LOGGER.severe(e.getMessage());
        }
    }
}
