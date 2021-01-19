package org.planit.io.project;

import org.planit.assignment.TrafficAssignment;
import org.planit.assignment.TrafficAssignmentConfigurator;
import org.planit.demands.Demands;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.formatter.OutputFormatter;
import org.planit.project.CustomPlanItProject;
import org.planit.utils.exceptions.PlanItException;

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
  
  /**
   * the default output formatter registered on the project and subsequent assignment
   */
  private final OutputFormatter defaultOutputFormatter; 
  
  /** Constructor taking project path where to find all project input files
	 * 
	 * @param projectPath the project path to source input files from
	 * @throws PlanItException thrown if error
	 */
	public PlanItProject(String projectPath) throws PlanItException {
		super(new PlanItInputBuilder(projectPath));

		/* default output formatter */
     defaultOutputFormatter = createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER);
	}
	
	
  /**
   * {@inheritDoc}
   */
  @Override
  public TrafficAssignmentConfigurator<? extends TrafficAssignment> createAndRegisterTrafficAssignment(
      String trafficAssignmentType, 
      Demands theDemands,
      Zoning theZoning, 
      final InfrastructureNetwork theNetwork) throws PlanItException {
    
    /* delegate */
    TrafficAssignmentConfigurator<? extends TrafficAssignment> taConfigurator = 
        super.createAndRegisterTrafficAssignment(trafficAssignmentType, theDemands, theZoning, theNetwork);
    
    /* default output formatter */
    taConfigurator.registerOutputFormatter(defaultOutputFormatter);
    return taConfigurator;    
  }	

}