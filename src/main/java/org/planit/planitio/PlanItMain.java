package org.planit.planitio;

import java.io.IOException;

import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.input.InputBuilderListener;
import org.planit.logging.PlanItLogger;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.property.OutputProperty;
import org.planit.planitio.input.PlanItInputBuilder;
import org.planit.planitio.output.formatter.PlanItOutputFormatter;
import org.planit.project.CustomPlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.userclass.Mode;
import org.planit.utils.IdGenerator;
import org.planit.zoning.Zoning;

/**
 * Test class to run XML input files.
 * 
 * @author gman6028
 *
 */
public class PlanItMain {

	private String projectPath = "src\\test\\resources\\route_choice\\xml\\test1";
	private int maxIterations = 500;
	private double epsilon = 0.00;

	/**
	 * Main method for the BasicCsvMain program. Only used to start the program
	 * 
	 * @param args main method args
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	public static void main(String[] args) throws SecurityException, IOException {

		try {
			PlanItLogger.setLogging("logs\\PlanItXmlMain.log", PlanItMain.class);
			PlanItMain planItMain = new PlanItMain();
			planItMain.execute();
			PlanItLogger.close();
		} catch (Exception e) {
			e.printStackTrace();
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
		InputBuilderListener inputBuilderListener = new PlanItInputBuilder(projectPath);
		CustomPlanItProject project = new CustomPlanItProject(inputBuilderListener);

		// RAW INPUT START --------------------------------
		PhysicalNetwork physicalNetwork = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
		Zoning zoning = project.createAndRegisterZoning(physicalNetwork);
		Demands demands = project.createAndRegisterDemands(zoning);
		// RAW INPUT END -----------------------------------

		// TRAFFIC ASSIGNMENT START------------------------
		DeterministicTrafficAssignment assignment = project
				.createAndRegisterDeterministicAssignment(TraditionalStaticAssignment.class.getCanonicalName());
		CapacityRestrainedTrafficAssignmentBuilder taBuilder = (CapacityRestrainedTrafficAssignmentBuilder) assignment
				.getBuilder();

		// SUPPLY SIDE
		taBuilder.registerPhysicalNetwork(physicalNetwork);
		// SUPPLY-DEMAND INTERACTIONS
		BPRLinkTravelTimeCost bprLinkTravelTimeCost = (BPRLinkTravelTimeCost) taBuilder
				.createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
		MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) physicalNetwork;
		MacroscopicLinkSegmentType macroscopiclinkSegmentType = macroscopicNetwork
				.findMacroscopicLinkSegmentTypeByExternalId(1);
		Mode mode = Mode.getByExternalId(2);
		bprLinkTravelTimeCost.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
		taBuilder
				.createAndRegisterVirtualTravelTimeCostFunction(SpeedConnectoidTravelTimeCost.class.getCanonicalName());
		taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());

		// SUPPLY-DEMAND INTERFACE
		taBuilder.registerDemandsAndZoning(demands, zoning);	

		//DATA OUTPUT CONFIGURATION
		assignment.activateOutput(OutputType.LINK);
		OutputConfiguration outputConfiguration = assignment.getOutputConfiguration();
		outputConfiguration.setPersistOnlyFinalIteration(true); // option to only persist the final iteration
		LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) outputConfiguration.getOutputTypeConfiguration(OutputType.LINK);
		linkOutputTypeConfiguration.addAllProperties();
		linkOutputTypeConfiguration.removeProperty(OutputProperty.TOTAL_COST_TO_END_NODE);
		linkOutputTypeConfiguration.removeProperty(OutputProperty.OD_COST);
		linkOutputTypeConfiguration.removeProperty(OutputProperty.OD_PATH);
		
		//OUTPUT FORMAT CONFIGURATION
		PlanItOutputFormatter xmlOutputFormatter = (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(PlanItOutputFormatter.class.getCanonicalName());
		taBuilder.registerOutputFormatter( xmlOutputFormatter);
		xmlOutputFormatter.setXmlDirectory("C:\\Users\\Public\\PlanIt\\Xml");
		xmlOutputFormatter.setCsvDirectory("C:\\Users\\Public\\PlanIt\\Csv");
		xmlOutputFormatter.resetXmlDirectory();
		xmlOutputFormatter.resetCsvDirectory();
		xmlOutputFormatter.setXmlNameRoot("Route Choice Test 1");
		xmlOutputFormatter.setCsvNameRoot("Route Choice Test 1");

		// "USER" configuration
		assignment.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
		assignment.getGapFunction().getStopCriterion().setEpsilon(epsilon);

		project.executeAllTrafficAssignments();

	}
}
