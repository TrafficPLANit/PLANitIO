package org.planit.planitio.project;

import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.logging.PlanItLogger;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.planitio.input.PlanItInputBuilder;
import org.planit.planitio.output.formatter.PlanItOutputFormatter;
import org.planit.project.CustomPlanItProject;
import org.planit.trafficassignment.TrafficAssignment;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;
import org.planit.zoning.Zoning;

/**
 * Wrapper around PLANitProject with most common defaults automatically activated. Limitations include:
 * - Only allows for a single assignment
 * - Only allows for a single zoning system, network and demands input
 *
 * Advanced users who want to utilize all the flexibility of PLANit should instead use PLANitProject in the PLANit core.
 * 
 * Default configuration for this type of project:
 *   (i)     Use the native output formatter (PLANitIO format),
 *   (ii)    Use a macroscopic network,
 *   (iii)   Use the native input parser (PLANitIO format) 
 *   (iv)    The assignment will by default persist link outputs and OD outputs (no paths)
 *   (v)     Parsing of the inputs occurs after configuration of all other components to quickly identify user configuration errors           
 * 
 * @author markr
 *
 */
public class PlanItSimpleProject extends CustomPlanItProject {
    
    /**
     * Simple project registers native PLANitXML output formatter by default which is stored in this reference
     */
    private PlanItOutputFormatter defaultOutputFormatter = null;
    
    /**
     * Initialize this simple project with as many components as possible directly after its inception here
     */
    private void initialiseSimpleProject() {
        try {
            // register the default Output formatter as a formatter that is available
            defaultOutputFormatter = (PlanItOutputFormatter) this.createAndRegisterOutputFormatter(PlanItOutputFormatter.class.getCanonicalName());
        } catch (PlanItException e) {
        	PlanItLogger.severe("Could not instantiate default settings for project");
        }
    }
    
    /**
     * Parse the input data for the project here and register it on the assignment
     * @throws PlanItException 
     */
    private void processSimpleProjectInputData(TrafficAssignment trafficAssignment) throws PlanItException {
        // parse a macroscopic network representation + register on assignment
        MacroscopicNetwork network = (MacroscopicNetwork) this.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
        trafficAssignment.setPhysicalNetwork(network);
        // parse the zoning system + register on assignment
        Zoning zoning = this.createAndRegisterZoning(network);
        trafficAssignment.setZoning(zoning);
        // parse the demands + register on assignment
        Demands demands = this.createAndRegisterDemands(zoning);
        trafficAssignment.setDemands(demands);
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
        super(new PlanItInputBuilder(Paths.get(".").toAbsolutePath().toString()));
        PlanItLogger.info("Searching for input files in: "+Paths.get(".").toAbsolutePath().toString());
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
        super(new PlanItInputBuilder(projectPath));
        PlanItLogger.info("Searching for input files in: "+Paths.get(projectPath).toAbsolutePath().toString());
        initialiseSimpleProject();
    }    
        
    /** On a simple project we only allow a single assignment to be registered. This is verified here. If multiple assignments
     * are required within the same project, then a simple project cannot be used 
     * 
     * @param trafficAssignmentType the traffic assignment type to be used
     */
    @Override
    public TrafficAssignmentBuilder createAndRegisterDeterministicAssignment(String trafficAssignmentType)
            throws PlanItException {
        if(super.trafficAssignments.hasRegisteredAssignments()) {
            throw new PlanItException("This type of PLANit project only allows a single assignment per project");
        }
        return super.createAndRegisterDeterministicAssignment(trafficAssignmentType);
    }    
    
    /** 
     * Override where we conduct the parsing of all inputs at the last moment such that any mistakes regarding the configuration
     * will be found quickly and are not hampered by long load times for parsing inputs. This is mainly useful for inexperienced users
     * who just want to run a single model. If one wants complete control of the process flow use @see org.planit.project.PlanItProject instead
     *  
     * @see org.planit.project.CustomPlanItProject#executeAllTrafficAssignments()
     * @return Map of ids of failed runs (key) together with their exceptions (value).  Empty if all runs succeeded
     * @throws PlanItException thrown if there is an error during configuration before the runs start
     */
    @Override
    public Map<Long, PlanItException> executeAllTrafficAssignments() throws PlanItException {
        Map<Long, PlanItException> exceptionMap = new TreeMap<Long, PlanItException>();
        if(super.trafficAssignments.hasRegisteredAssignments()) {
            // parse inputs (not a choice when this happens on simple project, always do this last based on native input format)
            processSimpleProjectInputData(this.trafficAssignments.getFirstTrafficAssignment());
            exceptionMap = super.executeAllTrafficAssignments();
        }else
        {
            PlanItLogger.info("No traffic assignment has been registered yet, terminating execution");
        }
        return exceptionMap;
    }
    
    /** Collect the default output formatter for PLANit simple project which is the native XMLFormatter
     * @return defaultOutputformatter
     */
    public PlanItOutputFormatter getDefaultOutputFormatter() {
        return this.defaultOutputFormatter;
    }    

}