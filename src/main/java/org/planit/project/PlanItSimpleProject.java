package org.planit.project;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.input.PlanItXMLInputBuilder;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.formatter.PlanItXMLOutputFormatter;
import org.planit.trafficassignment.DeterministicTrafficAssignment;

/**
 * Wrapper around PLANitProject with most common defaults automatically activated. Only allows for a single assignment
 * within the project instead of full flexibility. Advanced users who want to utilise all the flexibility of PLANit should instead
 * use PLANitProject in the PLANit core.
 * 
 * @author markr
 *
 */
public class PlanItSimpleProject extends PlanItProject {
    
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(PlanItProject.class.getName());
    
    /**
     * initialise this simple project
     */
    private void initialiseSimpleProject() {
        try {
            // use the default Output formatter
            this.createAndRegisterOutputFormatter(PlanItXMLOutputFormatter.class.getCanonicalName());
            // use a macroscopic network representation
            this.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
        } catch (PlanItException e) {
            LOGGER.severe("Could not instantiate default settings for project");
        }
    }
    
    /**
     * Parse the input data for the project here
     */
    private void processSimpleProjectInputData() {
        // TODO Auto-generated method stub        
    }       
    
    // Public
    
    /**
     * Base constructor for simple project which adopts the PlanItXML input/output format. It is assumed
     * all input files are in the current working directory
     * 
     */
    public PlanItSimpleProject() {  
        // use the default input builder with the current path
        super(new PlanItXMLInputBuilder(Paths.get("").toAbsolutePath().toString()));
        LOGGER.info("Searching for input files in: "+Paths.get("").toAbsolutePath().toString());
        initialiseSimpleProject();
    }       
        
    /**
     * Base constructor for simple project which adopts the PlanItXML input/output format
     * 
     * @param projectPath to retrieve the files from
     */
    public PlanItSimpleProject(String projectPath) {
        // use the default input builder
        super(new PlanItXMLInputBuilder(projectPath));
        LOGGER.info("Searching for input files in: "+Paths.get(projectPath).toAbsolutePath().toString());
        initialiseSimpleProject();
    }    
    
    public void someCall() {
        LOGGER.info("some call made to simple project");
    }
    
    /** On a simple project we only allow a single assignment to be registered. This is verified here. If multiple assignments
     * are required within the same project, then a simple project cannot be used 
     * 
     * @param trafficAssignmentType the traffic assignment type to be used
     */
    @Override
    public DeterministicTrafficAssignment createAndRegisterDeterministicAssignment(String trafficAssignmentType)
            throws PlanItException {
        if(super.hasRegisteredAssignments()) {
            throw new PlanItException("This type of PLANit project only allows a single assignment per project");
        }
        return super.createAndRegisterDeterministicAssignment(trafficAssignmentType);
    }    

    /** Override where we conduct the parsing of all inputs at the last moment such that any mistakes regarding the configuration
     * will be found quickly and are not hampered by long load times for parsing inputs. This is mainly useful for inexperienced users
     * who just want to run a single model. If one wants complete control of the process flow use @see org.planit.project.PlanItProject instead
     *  
     * @see org.planit.project.PlanItProject#executeAllTrafficAssignments()
     */
    @Override
    public void executeAllTrafficAssignments() throws PlanItException {
        trafficAssignments.forEach( (id,ta) -> {
            processSimpleProjectInputData();
            executeTrafficAssignment(ta);
        });
    }

}
