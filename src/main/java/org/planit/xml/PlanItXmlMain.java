package org.planit.xml;

import java.util.logging.Logger;

import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.demand.Demands;
import org.planit.event.listener.InputBuilderListener;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.OutputType;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.formatter.PlanItXMLOutputFormatter;
import org.planit.project.PlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.userclass.Mode;
import org.planit.utils.IdGenerator;
import org.planit.input.PlanItXMLInputBuilder;
import org.planit.zoning.Zoning;

/**
 * Test class to run XML input files.
 * 
 * @author gman6028
 *
 */
public class PlanItXmlMain {

	private static final Logger LOGGER = Logger.getLogger(PlanItXmlMain.class.getName());

	private String csvResultsFileLocation = "src\\test\\resources\\route_choice\\xml\\test5\\results.csv";
	private String projectPath = "src\\test\\resources\\route_choice\\xml\\test5";
	private int maxIterations = 500;
	private double epsilon = 0.00;

	/**
	 * Main method for the BasicCsvMain program. Only used to start the program
	 * 
	 * @param args main method args
	 */
	public static void main(String[] args) {

		try {
			PlanItXmlMain planItXmlMain = new PlanItXmlMain();
			planItXmlMain.execute();
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
		//InputBuilderListener inputBuilderListener = new PlanItXMLInputBuilder(zoningXmlFileLocation, demandXmlFileLocation,
		//		networkXmlFileLocation);
		InputBuilderListener inputBuilderListener = new PlanItXMLInputBuilder(projectPath);
		PlanItProject project = new PlanItProject(inputBuilderListener);

		// RAW INPUT START --------------------------------
		PhysicalNetwork physicalNetwork = project
				.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
		Zoning zoning = project.createAndRegisterZoning();
		Demands demands = project.createAndRegisterDemands();
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
				.createAndRegisterPhysicalTravelTimeCostFunction(BPRLinkTravelTimeCost.class.getCanonicalName());
		MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) physicalNetwork;
		MacroscopicLinkSegmentType macroscopiclinkSegmentType = macroscopicNetwork
				.findMacroscopicLinkSegmentTypeByExternalId(1);
		Mode mode = Mode.getByExternalId(2);
		bprLinkTravelTimeCost.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
		taBuilder
				.createAndRegisterVirtualTravelTimeCostFunction(SpeedConnectoidTravelTimeCost.class.getCanonicalName());
		taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
		// SUPPLY-DEMAND INTERFACE
		taBuilder.registerZoning(zoning);

		// DEMAND SIDE
		taBuilder.registerDemands(demands);

		//DATA OUTPUT CONFIGURATION
		assignment.activateOutput(OutputType.LINK);
		OutputConfiguration outputConfiguration = assignment.getOutputConfiguration();
		outputConfiguration.setPersistOnlyFinalIteration(true); // option to only persist the final iteration
		LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) outputConfiguration.getOutputTypeConfiguration(OutputType.LINK);
		
		//OUTPUT FORMAT CONFIGURATION
		PlanItXMLOutputFormatter xmlOutputFormatter = (PlanItXMLOutputFormatter) project.createAndRegisterOutputFormatter(PlanItXMLOutputFormatter.class.getCanonicalName());
		taBuilder.registerOutputFormatter(xmlOutputFormatter);
		xmlOutputFormatter.resetXmlOutputDirectory();
		xmlOutputFormatter.resetCsvOutputDirectory();
		xmlOutputFormatter.setCsvSummaryOutputFileName(csvResultsFileLocation);
		xmlOutputFormatter.setXmlOutputDirectory("C:\\Users\\Public\\PlanIt\\Xml");
		xmlOutputFormatter.setCsvOutputDirectory("C:\\Users\\Public\\PlanIt\\Csv");

		// "USER" configuration
		assignment.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
		assignment.getGapFunction().getStopCriterion().setEpsilon(epsilon);

		project.executeAllTrafficAssignments();

	}
}
