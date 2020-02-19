package org.planit.planitio.project;

import org.planit.exceptions.PlanItException;
import org.planit.planitio.input.PlanItInputBuilder;
import org.planit.project.CustomPlanItProject;

/**
 * 
 * PLANit project which is nothing more than a CustomPlanItProject without any custom configuration pre-embedded. So, it allows
 * maximum flexibility for users.
 * 
 * @author markr, gman6028
 *
 */
public class PlanItProject extends CustomPlanItProject {
  
	/** Constructor taking project path where to find all project input files
	 * 
	 * @param projectPath
	 * @throws PlanItException
	 */
	public PlanItProject(String projectPath) throws PlanItException {
		super(new PlanItInputBuilder(projectPath));
	}

}