package org.planit.project;

import java.util.ArrayList;
import java.util.List;

import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.PhysicalCost;
import org.planit.exceptions.PlanItException;
import org.planit.input.PlanItXMLInputBuilder;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

public class PlanItProject extends CustomPlanItProject {

   
    /**
     * Object factory for physical costs
     */
    protected TrafficAssignmentComponentFactory<PhysicalCost> physicalCostFactory;
    
	public PlanItProject(String projectPath) throws PlanItException {
		super(new PlanItXMLInputBuilder(projectPath));
    	physicalCostFactory = new TrafficAssignmentComponentFactory<PhysicalCost>(PhysicalCost.class);
        physicalCostFactory.setEventManager(eventManager);
	}
	
	public List<InitialLinkSegmentCost> createAndRegisterInitialLinkSegmentCosts(String ...fileName) throws PlanItException {
		List<InitialLinkSegmentCost> initialLinkSegmentCosts = new ArrayList<InitialLinkSegmentCost>();
		for (int i=0; i<fileName.length; i++) {
			initialLinkSegmentCosts.add(createAndRegisterInitialLinkSegmentCost(fileName[i]));
		}
		return initialLinkSegmentCosts;
	}
	
	public InitialLinkSegmentCost createAndRegisterInitialLinkSegmentCost(String fileName) throws PlanItException {
		InitialLinkSegmentCost initialLinkSegmentCost = (InitialLinkSegmentCost) physicalCostFactory.create(InitialLinkSegmentCost.class.getCanonicalName(), fileName);
		return initialLinkSegmentCost;
	}
	
}
