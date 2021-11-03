package org.goplanit.io.project;

import org.goplanit.assignment.TrafficAssignment;
import org.goplanit.assignment.TrafficAssignmentConfigurator;
import org.goplanit.demands.Demands;
import org.goplanit.io.input.PlanItInputBuilder;
import org.goplanit.network.TransportLayerNetwork;
import org.goplanit.output.formatter.OutputFormatter;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.zoning.Zoning;

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
      final TransportLayerNetwork<?,?> theNetwork) throws PlanItException {
    
    /* delegate */
    TrafficAssignmentConfigurator<? extends TrafficAssignment> taConfigurator = 
        super.createAndRegisterTrafficAssignment(trafficAssignmentType, theDemands, theZoning, theNetwork);
    
    /* default output formatter */
    taConfigurator.registerOutputFormatter(defaultOutputFormatter);
    return taConfigurator;    
  }	

}