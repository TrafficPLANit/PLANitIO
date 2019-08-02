package org.planit.project;

import java.nio.file.Paths;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.input.xml.PlanItXMLInputBuilder;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.formatter.xml.PlanItXMLOutputFormatter;
import org.planit.trafficassignment.DeterministicTrafficAssignment;

/**
 * Wrapper around PLANitProject with most common defaults automatically activated. Only allows for a single assignment
 * within the project instead of full flexibility. Advanced users who want to utilise all the flexibility of PLANit should instead
 * use PLANitProject in the PLANit core.
 * 
 * Default configuration for this type of project:
 *   (i)     Use the native output formatter (PLANitXML format),
 *   (ii)    Use a macroscopic network,
 *   (iii)   Use the native input parser (PLANitXML format) 
 *   (iv)    The assignment will by default persist link outputs
 *   (v)     Parsing of the inputs occurs after configuration of all other components to quickly identify user configuration errors           
 * 
 * @author markr
 *
 */
public class PlanItSimpleProject extends CustomPlanItProject {
    
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(PlanItSimpleProject.class.getName());
    
    /**
     * Simple project registers native PLANitXML outputformatter by default which is stored in this reference
     */
    private PlanItXMLOutputFormatter defaultOutputFormatter = null;
    
    /**
     * Initialise this simple project with as many components as possible directly after its inception here
     */
    private void initialiseSimpleProject() {
        try {
            // register the default Output formatter as a formatter that is available
            defaultOutputFormatter = (PlanItXMLOutputFormatter) this.createAndRegisterOutputFormatter(PlanItXMLOutputFormatter.class.getCanonicalName());
        } catch (PlanItException e) {
            LOGGER.severe("Could not instantiate default settings for project");
        }
    }
    
    /**
     * Parse the input data for the project here
     * @throws PlanItException 
     */
    private void processSimpleProjectInputData() throws PlanItException {
        // parse a macroscopic network representation
        this.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
        // parse the zoning system
        this.createAndRegisterZoning();
        // parse the demands
        this.createAndRegisterDemands();
    }       
    
    // Public
    
    /**
     * Base constructor for simple project which adopts the PlanItXML input/output format. It is assumed
     * all input files are in the current working directory
     * @throws PlanItException 
     * 
     */
    public PlanItSimpleProject() throws PlanItException {  
        // use the default input builder with the current path as the project path
        super(new PlanItXMLInputBuilder(Paths.get(".").toAbsolutePath().toString()));
        LOGGER.info("Searching for input files in: "+Paths.get(".").toAbsolutePath().toString());
        initialiseSimpleProject();
    }       
        
    /**
     * Base constructor for simple project which adopts the PlanItXML input/output format
     * 
     * @param projectPath to retrieve the files from
     * @throws PlanItException 
     */
    public PlanItSimpleProject(String projectPath) throws PlanItException {
        // use the default input builder
        super(new PlanItXMLInputBuilder(projectPath));
        LOGGER.info("Searching for input files in: "+Paths.get(projectPath).toAbsolutePath().toString());
        initialiseSimpleProject();
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
     * @see org.planit.project.CustomPlanItProject#executeAllTrafficAssignments()
     */
    @Override
    public void executeAllTrafficAssignments() throws PlanItException {
        // parse inputs (not a choice when this happens on simple project, always do this last based on native input format)
        processSimpleProjectInputData();
        super.executeAllTrafficAssignments();
    }
    
    /** Collect the default outputformatter for PLANit simple project which is the native XMLFormatter
     * @return defaultOutputformatter
     */
    public PlanItXMLOutputFormatter getDefaultOutputFormatter() {
        return this.defaultOutputFormatter;
    }    

}