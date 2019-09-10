package org.planit.project;

import org.planit.exceptions.PlanItException;
import org.planit.input.xml.PlanItXMLInputBuilder;

/**
 * PLANit project as it is intended by default using the native XML format input files obtained from the passed in project path
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
		super(new PlanItXMLInputBuilder(projectPath));
	}

}
