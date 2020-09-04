package org.planit.io;

import java.util.logging.Logger;

import org.planit.assignment.TrafficAssignment;
import org.planit.assignment.traditionalstatic.TraditionalStaticAssignmentConfigurator;
import org.planit.cost.physical.BPRConfigurator;
import org.planit.cost.physical.AbstractPhysicalCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.demands.Demands;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.io.output.formatter.PlanItOutputFormatter;
import org.planit.logging.Logging;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.OutputFormatter;
import org.planit.project.CustomPlanItProject;
import org.planit.sdinteraction.smoothing.Smoothing;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;

/**
 * Test class to run XML input files.
 *
 * @author gman6028
 *
 */
public class PlanItMain {

	/** the logger */
	private static Logger LOGGER = null;

	private final String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\test3";
	private final int maxIterations  = 500;
	private final double epsilon     = 0.00;

	/**
	 * Main method for the PLANitIO program. Only used to start the program
	 *
	 * @param args main method args
	 */
	public static void main(final String[] args) {

		try {
			LOGGER = Logging.createLogger(PlanItMain.class);
			final PlanItMain planItMain = new PlanItMain();
			planItMain.execute();
		} catch (final Exception e) {
		  LOGGER.severe(e.getMessage());
			e.printStackTrace();
    } finally {
      Logging.closeLogger(LOGGER);
    }
	}

	/**
	 * Top-level business method for the PlanItXmlMain program.
	 *
	 * This method instantiates the PlanItXmlMain object to read the input files and
	 * the PlanItProject object to run the assignment. The output results are
	 * currently saved to a CSV file.
	 *
	 * Developers may need to edit this method to allow different traffic assignment
	 * and time modeling classes.
	 *
	 * @throws PlanItException thrown if there is an error
	 */
	public void execute() throws PlanItException {

		// SET UP SCANNER AND PROJECT
		IdGenerator.reset();
		PlanItInputBuilder planItInputBuilder = new PlanItInputBuilder(projectPath);
		final CustomPlanItProject project = new CustomPlanItProject(planItInputBuilder);

		// output formatter
		final PlanItOutputFormatter xmlOutputFormatter = (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER);		
    xmlOutputFormatter.setXmlDirectory("C:\\Users\\Public\\PlanIt\\Xml");
    xmlOutputFormatter.setCsvDirectory("C:\\Users\\Public\\PlanIt\\Csv");
    xmlOutputFormatter.resetXmlDirectory();
    xmlOutputFormatter.resetCsvDirectory();
    xmlOutputFormatter.setXmlNameRoot("Route Choice Test 1");
    xmlOutputFormatter.setCsvNameRoot("Route Choice Test 1");
		

		// RAW INPUT START --------------------------------
		final PhysicalNetwork<?,?,?> physicalNetwork = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
		final Zoning zoning = project.createAndRegisterZoning(physicalNetwork);
		final Demands demands = project.createAndRegisterDemands(zoning, physicalNetwork);
		// RAW INPUT END -----------------------------------

		// TRAFFIC ASSIGNMENT START------------------------
		final TraditionalStaticAssignmentConfigurator ta = 
		    (TraditionalStaticAssignmentConfigurator) project.createAndRegisterTrafficAssignment(
		        TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT, demands, zoning, physicalNetwork);

		// TA CONFIGURATION
		final BPRConfigurator bpr = (BPRConfigurator) ta.createAndRegisterPhysicalCost(AbstractPhysicalCost.BPR);
		final MacroscopicLinkSegmentType macroscopiclinkSegmentType = planItInputBuilder.getLinkSegmentTypeByExternalId((long) 1);
		final Mode mode = planItInputBuilder.getModeByExternalId((long) 2);
		bpr.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);

		ta.createAndRegisterVirtualCost(VirtualCost.FIXED);
		
		ta.createAndRegisterSmoothing(Smoothing.MSA);
		
    ta.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
    ta.getGapFunction().getStopCriterion().setEpsilon(epsilon);
		

		// DATA OUTPUT CONFIGURATION
    ta.registerOutputFormatter(xmlOutputFormatter);		
		ta.activateOutput(OutputType.LINK);
		ta.getOutputConfiguration().setPersistOnlyFinalIteration(true); // option to only persist the final iteration

		project.executeAllTrafficAssignments();
	}
}