package org.planit.project;

import org.planit.exceptions.PlanItException;
import org.planit.input.PlanItXMLInputBuilder;

public class PlanItProject extends CustomPlanItProject {

	public PlanItProject(String projectPath) throws PlanItException {
		super(new PlanItXMLInputBuilder(projectPath));
	}
	
}
