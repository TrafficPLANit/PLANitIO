package org.planit.project;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.exceptions.PlanItException;
import org.planit.input.PlanItXMLInputBuilder;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

public class PlanItProject extends CustomPlanItProject {

   
    /**
     * Object factory for physical costs
     */
    protected TrafficAssignmentComponentFactory<InitialPhysicalCost> initialPhysicalCostFactory;
  
    /**
     * Registered InitialLinkSegmentCost objects on this project
     */
    protected TreeMap<Long, InitialLinkSegmentCost> initialLinkSegmentCostMap;   
    
	public PlanItProject(String projectPath) throws PlanItException {
		super(new PlanItXMLInputBuilder(projectPath));
		initialPhysicalCostFactory = new TrafficAssignmentComponentFactory<InitialPhysicalCost>(InitialPhysicalCost.class);
		initialPhysicalCostFactory.setEventManager(eventManager);
		initialLinkSegmentCostMap = new TreeMap<Long, InitialLinkSegmentCost>();   
	}
	
	/**
	 * Create and register initial link segment costs from multiple files
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
	 * Create and register initial link segment costs from a (single) file
	 * 
	 * @param fileName file containing the initial link segment cost values
	 * @return the InitialLinkSegmentCost object
	 * @throws PlanItException thrown if there is an error
	 */
	public InitialLinkSegmentCost createAndRegisterInitialLinkSegmentCost(String fileName) throws PlanItException {
		InitialLinkSegmentCost initialLinkSegmentCost = (InitialLinkSegmentCost) initialPhysicalCostFactory.create(InitialLinkSegmentCost.class.getCanonicalName(), fileName);
		initialLinkSegmentCostMap.put(initialLinkSegmentCost.getId(), initialLinkSegmentCost );
        return initialLinkSegmentCost ;
	}
	
    /**
     * Retrieve a InitialLinkSegmentCost object given its id
     * 
     * @param id
     *            the id of the InitialLinkSegmentCost object
     * @return the retrieved InitialLinkSegmentCost object
     */
    public InitialLinkSegmentCost getInitialLinkSegmentCost(long id) {
        return initialLinkSegmentCostMap.get(id);
    }

}
