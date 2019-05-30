package org.planit.xml;

import java.util.logging.Logger;

import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.demand.Demands;
import org.planit.event.listener.InputBuilderListener;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.OutputType;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.formatter.CSVOutputFormatter;
import org.planit.output.formatter.OutputFormatter;
import org.planit.project.PlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.utils.IdGenerator;
import org.planit.xml.input.PlanItXml;
import org.planit.zoning.Zoning;

/**
 * Test class to run  XML input files.
 * 
 * During the development phase, this class will use a mixture of XML and CSV inputs.  As the XML input reading is developed, this will progressively replace the CSV input.
 * 
 * @author gman6028
 *
 */
public class PlanItXmlMain {

    private static final Logger LOGGER = Logger.getLogger(PlanItXmlMain.class.getName());
    
    private String networkFileLocation =       "src\\test\\resources\\route_choice\\xml\\test5\\network.csv"; 
    private String linkTypesFileLocation =      "src\\test\\resources\\route_choice\\xml\\test5\\link_types.csv";
    private String modeFileLocation =            "src\\test\\resources\\route_choicexml\\test5\\modes.csv";
    private String resultsFileLocation =          "src\\test\\resources\\route_choicexml\\test5\\results.csv";
    private String zoningXmlFileLocation =     "src\\test\\resources\\route_choicexml\\test5\\zones.xml";
    private String demandXmlFileLocation =  "src\\test\\resources\\route_choicexml\\test5\\demands.xml";
    private String networkXmlFileLocation =  "src\\test\\resources\\route_choicexml\\test5\\network.xml";
    private int maxIterations = 500;
    private double epsilon = 0.00;
	
/**
 * Main method for the BasicCsvMain program.  Only used to start the program
 * 
 * @param args				main method args
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
 * Top-level business method for the BasicCsvMain program.
 * 
 * This method instantiates the BasicCsvScan object to read the input files and the PlanItProject object to run the assignment.  The output results are currently saved to a CSV file.
 * 
 * Developers may need to edit this method to allow different traffic assignment and time modeling classes.
 * 
 * @throws PlanItException		thrown if there is an error								
 */
	public void execute() throws PlanItException {
		
		//SET UP SCANNER AND PROJECT
		IdGenerator.reset();
		InputBuilderListener inputBuilderListener = new PlanItXml(networkFileLocation, 
																		                linkTypesFileLocation, 
																		                modeFileLocation, 
																		                zoningXmlFileLocation, 
																		                demandXmlFileLocation, 
																		                networkXmlFileLocation);
		PlanItProject project = new PlanItProject(inputBuilderListener);
		
		//RAW INPUT START --------------------------------
		PhysicalNetwork physicalNetwork = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
		Zoning zoning = project.createAndRegisterZoning();
		Demands demands = project.createAndRegisterDemands(); 							
        //RAW INPUT END ----------------------------------- 
        
		// TRAFFIC ASSIGNMENT START------------------------			
		DeterministicTrafficAssignment assignment = project.createAndRegisterDeterministicAssignment(TraditionalStaticAssignment.class.getCanonicalName());		
		CapacityRestrainedTrafficAssignmentBuilder taBuilder = (CapacityRestrainedTrafficAssignmentBuilder) assignment.getBuilder();
 		
		// SUPPLY SIDE
		taBuilder.registerPhysicalNetwork(physicalNetwork);								
		// SUPPLY-DEMAND INTERACTIONS
		taBuilder.createAndRegisterPhysicalTravelTimeCostFunction(BPRLinkTravelTimeCost.class.getCanonicalName());
		taBuilder.createAndRegisterVirtualTravelTimeCostFunction(SpeedConnectoidTravelTimeCost.class.getCanonicalName()); 		
		taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());					
		// SUPPLY-DEMAND INTERFACE
		taBuilder.registerZoning(zoning);
		
		// DEMAND SIDE	
		taBuilder.registerDemands(demands);	
		
        // OUTPUT
        assignment.activateOutput(OutputType.LINK);
        OutputConfiguration outputConfiguration =  assignment.getOutputConfiguration();
        outputConfiguration.setPersistOnlyFinalIteration(true); // option to only persist the final iteration
        OutputFormatter outputFormatter = project.createAndRegisterOutputFormatter(CSVOutputFormatter.class.getCanonicalName());
        CSVOutputFormatter csvOutputFormatter = (CSVOutputFormatter) outputFormatter;
        csvOutputFormatter.setOutputFileName(resultsFileLocation);
        taBuilder.registerOutputFormatter(outputFormatter);
        
	    // "USER" configuration
        assignment.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
        assignment.getGapFunction().getStopCriterion().setEpsilon(epsilon);
        
        project.executeAllTrafficAssignments();
		
	}
}
