package org.planit.project;

import java.util.ArrayList;
import java.util.List;

import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.exceptions.PlanItException;
import org.planit.input.xml.PlanItXMLInputBuilder;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

/**
 * PLANit project as it is intended by default using the native XML format input files obtained from the passed in project path
 * 
 * @author markr, gman6028
 *
 */
public class PlanItProject extends CustomPlanItProject {

   
    /**
     * Object factory for physical costs
     */
    protected TrafficAssignmentComponentFactory<InitialPhysicalCost> initialPhysicalCostFactory;
  
	/** Constructor taking project path where to find all project input files
	 * 
	 * @param projectPath
	 * @throws PlanItException
	 */
	public PlanItProject(String projectPath) throws PlanItException {
		super(new PlanItXMLInputBuilder(projectPath));
		initialPhysicalCostFactory = new TrafficAssignmentComponentFactory<InitialPhysicalCost>(InitialPhysicalCost.class);
		initialPhysicalCostFactory.setEventManager(eventManager);
	}
	
	/**
	 * Create and register initial link segment costs from multiple files which we assume are available in the native xml/csv output format
	 * as provided in this project
	 * 
	 * @param fileName a series of files containing initial link segment cost values
	 * @return a list of InitialLinkSegmentCost objects
	 * @throws PlanItException thrown if there is an error 
	 */
	public List<InitialLinkSegmentCost> createAndRegisterInitialLinkSegmentCosts(String ...fileName) throws PlanItException {
	    List<InitialLinkSegmentCost> initialLinkSegmentCosts = new ArrayList<InitialLinkSegmentCost>();
		for (int i=0; i<fileName.length; i++) {
			initialLinkSegmentCosts.add(createAndRegisterInitialLinkSegmentCost(fileName[i]));
		}
		return initialLinkSegmentCosts;
	}
	
	/**
	 * Create and register initial link segment costs from a (single) file which we assume are available in the native xml/csv output format
     * as provided in this project
	 * 
	 * @param fileName file containing the initial link segment cost values
	 * @return the InitialLinkSegmentCost object
	 * @throws PlanItException thrown if there is an error
	 */
	public InitialLinkSegmentCost createAndRegisterInitialLinkSegmentCost(String fileName) throws PlanItException {
		InitialLinkSegmentCost initialLinkSegmentCost = (InitialLinkSegmentCost) initialPhysicalCostFactory.create(InitialLinkSegmentCost.class.getCanonicalName(), fileName);
        return initialLinkSegmentCost ;
	}
	
}
