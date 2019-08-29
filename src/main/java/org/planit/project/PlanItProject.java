package org.planit.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.exceptions.PlanItException;
import org.planit.input.xml.PlanItXMLInputBuilder;
import org.planit.network.physical.PhysicalNetwork;
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
    
    /**
     * Map to store all InitialLinkSegmentCost objects for each physical network
     */
    protected Map<PhysicalNetwork, List<InitialLinkSegmentCost>> initialLinkSegmentCosts;
  
	/** Constructor taking project path where to find all project input files
	 * 
	 * @param projectPath
	 * @throws PlanItException
	 */
	public PlanItProject(String projectPath) throws PlanItException {
		super(new PlanItXMLInputBuilder(projectPath));
		initialPhysicalCostFactory = new TrafficAssignmentComponentFactory<InitialPhysicalCost>(InitialPhysicalCost.class);
		initialPhysicalCostFactory.setEventManager(eventManager);
		initialLinkSegmentCosts = new HashMap<PhysicalNetwork, List<InitialLinkSegmentCost>>();
	}
	
	/**
	 * Create and register initial link segment costs from multiple files which we assume are available in the native xml/csv output format
	 * as provided in this project
	 * 
	 * @param network physical network the InitialLinkSegmentCost objects will be registered for
	 * @param fileName a series of files containing initial link segment cost values
	 * @return a list of InitialLinkSegmentCost objects
	 * @throws PlanItException thrown if there is an error 
	 */
	public List<InitialLinkSegmentCost> createAndRegisterInitialLinkSegmentCosts(PhysicalNetwork network, String ...fileName) throws PlanItException {
	    List<InitialLinkSegmentCost> initialLinkSegmentCostList = new ArrayList<InitialLinkSegmentCost>();
		for (int i=0; i<fileName.length; i++) {
			initialLinkSegmentCostList.add(createAndRegisterInitialLinkSegmentCost(network, fileName[i]));
		}
		return initialLinkSegmentCostList;
	}
	
	/**
	 * Create and register initial link segment costs from a (single) file which we assume are available in the native xml/csv output format
     * as provided in this project
	 * 
	 * @param network physical network the InitialLinkSegmentCost object will be registered for
	 * @param fileName file containing the initial link segment cost values
	 * @return the InitialLinkSegmentCost object
	 * @throws PlanItException thrown if there is an error
	 */
	public InitialLinkSegmentCost createAndRegisterInitialLinkSegmentCost(PhysicalNetwork network, String fileName) throws PlanItException {
		if (!initialLinkSegmentCosts.containsKey(network)) {
			initialLinkSegmentCosts.put(network, new ArrayList<InitialLinkSegmentCost>());
		}
		InitialLinkSegmentCost initialLinkSegmentCost = (InitialLinkSegmentCost) initialPhysicalCostFactory.create(InitialLinkSegmentCost.class.getCanonicalName(), fileName);
		//initialLinkSegmentCost.setNoLinkSegments(network.linkSegments.getNumberOfLinkSegments());
		initialLinkSegmentCosts.get(network).add(initialLinkSegmentCost);
        return initialLinkSegmentCost;
	}
	
}
