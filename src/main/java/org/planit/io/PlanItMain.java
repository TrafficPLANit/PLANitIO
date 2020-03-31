package org.planit.io;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.io.output.formatter.PlanItOutputFormatter;
import org.planit.logging.Logging;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.project.CustomPlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.TraditionalStaticAssignmentBuilder;
import org.planit.utils.misc.IdGenerator;
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
  private static Logger LOGGER;
  
  static {
    try {
      LOGGER = Logging.createLogger(PlanItMain.class);
    } catch (PlanItException e) {
      e.printStackTrace();
    }
  }
  
	private final String projectPath = "src\\test\\resources\\testcases\\route_choice\\xml\\test3";
	private final int maxIterations = 500;
	private final double epsilon = 0.00;

	/**
	 * Main method for the PLANitIO program. Only used to start the program
	 *
	 * @param args main method args
	 * @throws IOException
	 * @throws SecurityException
	 */
	public static void main(final String[] args) throws SecurityException, IOException {

		try {
			final PlanItMain planItMain = new PlanItMain();
			planItMain.execute();
		} catch (final Exception e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
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
		
		
		// RAW INPUT START --------------------------------
		final PhysicalNetwork physicalNetwork = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
		final Zoning zoning = project.createAndRegisterZoning(physicalNetwork);
		final Demands demands = project.createAndRegisterDemands(zoning, physicalNetwork);
		// RAW INPUT END -----------------------------------

		// TRAFFIC ASSIGNMENT START------------------------
		final TraditionalStaticAssignmentBuilder taBuilder =
                (TraditionalStaticAssignmentBuilder) project.createAndRegisterTrafficAssignment(
                		TraditionalStaticAssignment.class.getCanonicalName(), demands, zoning, physicalNetwork);
		// SUPPLY-DEMAND INTERACTIONS
		final BPRLinkTravelTimeCost bprLinkTravelTimeCost = 
		    (BPRLinkTravelTimeCost) taBuilder.createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
		final MacroscopicLinkSegmentType macroscopiclinkSegmentType = planItInputBuilder.getLinkSegmentTypeByExternalId((long) 1);
		final Mode mode = planItInputBuilder.getModeByExternalId((long) 2);
		bprLinkTravelTimeCost.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
		taBuilder.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
		taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());

		//DATA OUTPUT CONFIGURATION
		taBuilder.activateOutput(OutputType.LINK);
		final OutputConfiguration outputConfiguration = taBuilder.getOutputConfiguration();
		outputConfiguration.setPersistOnlyFinalIteration(true); // option to only persist the final iteration

		//OUTPUT FORMAT CONFIGURATION
		final PlanItOutputFormatter xmlOutputFormatter = 
		    (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(PlanItOutputFormatter.class.getCanonicalName());
		taBuilder.registerOutputFormatter( xmlOutputFormatter);
		xmlOutputFormatter.setXmlDirectory("C:\\Users\\Public\\PlanIt\\Xml");
		xmlOutputFormatter.setCsvDirectory("C:\\Users\\Public\\PlanIt\\Csv");
		xmlOutputFormatter.resetXmlDirectory();
		xmlOutputFormatter.resetCsvDirectory();
		xmlOutputFormatter.setXmlNameRoot("Route Choice Test 1");
		xmlOutputFormatter.setCsvNameRoot("Route Choice Test 1");

		// "USER" configuration
		taBuilder.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
		taBuilder.getGapFunction().getStopCriterion().setEpsilon(epsilon);

    final Map<Long, PlanItException> exceptionMap = project.executeAllTrafficAssignments();
    if (!exceptionMap.keySet().isEmpty()) {
    	for (final long id : exceptionMap.keySet() ) {
     		throw exceptionMap.get(id);
    	}
    }

	}
}