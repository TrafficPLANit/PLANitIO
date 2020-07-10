package org.planit.io.project;

import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.io.output.formatter.PlanItOutputFormatter;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.formatter.OutputFormatter;
import org.planit.project.CustomPlanItProject;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;

/**
 * 
 * PLANit project which is nothing more than a CustomPlanItProject without any custom configuration pre-embedded. So, it allows
 * maximum flexibility for users.
 * 
 * It does assume the PLANit default input format and registers the PLANit default output formatter automtically
 * 
 * @author markr, gman6028
 *
 */
public class PlanItProject extends CustomPlanItProject {
  
  private final OutputFormatter defaultOutputFormatter; 
  
  /** Constructor taking project path where to find all project input files
	 * 
	 * @param projectPath
	 * @throws PlanItException
	 */
	public PlanItProject(String projectPath) throws PlanItException {
		super(new PlanItInputBuilder(projectPath));

		// register default output formatter
     defaultOutputFormatter = createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER);
	}
	
	
  /**
   * {@inheritDoc}
   */
  @Override
  public TrafficAssignmentBuilder createAndRegisterTrafficAssignment(
      String trafficAssignmentType, 
      Demands theDemands,
      Zoning theZoning, 
      PhysicalNetwork thePhysicalNetwork) throws PlanItException {
    TrafficAssignmentBuilder taBuilder = super.createAndRegisterTrafficAssignment(trafficAssignmentType, theDemands, theZoning, thePhysicalNetwork);
    // register default output formatter by default
    taBuilder.registerOutputFormatter(defaultOutputFormatter);
    return taBuilder;
    
  }	

}