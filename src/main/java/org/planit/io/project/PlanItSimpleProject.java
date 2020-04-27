package org.planit.io.project;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.io.output.formatter.PlanItOutputFormatter;
import org.planit.io.test.util.LinkSegmentExpectedResultsDto;
import org.planit.io.test.util.TestHelper;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.project.CustomPlanItProject;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.builder.TrafficAssignmentBuilder;
import org.planit.utils.network.physical.Mode;

/**
 * Wrapper around PLANitProject with most common defaults automatically activated. Limitations include:
 * - Only allows for a single assignment
 * - Only allows for a single zoning system, network and demands input
 *
 * Advanced users who want to utilize all the flexibility of PLANit should instead use CustomPLANitProject in the PLANit core.
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
  
    /** the logger */
    private static final Logger LOGGER = Logger.getLogger(PlanItSimpleProject.class.getCanonicalName());   

    /**
     * Simple project registers native PLANitXML output formatter by default which is stored in this reference
     */
    private PlanItOutputFormatter defaultOutputFormatter = null;
    
    /** the network parsed upon creation of the project */
    private MacroscopicNetwork network = null;
    /** the zoning parsed upon creation of the project */
    private Zoning zoning = null;
    /** the demands parsed upon creation of the project */
    private Demands demands = null;

    /**
     * Initialize this simple project with as many components as possible directly after its inception here
     */
    private void initialiseSimpleProject() {
        try {
            // register the default Output formatter as a formatter that is available
            defaultOutputFormatter = (PlanItOutputFormatter) this.createAndRegisterOutputFormatter(PlanItOutputFormatter.class.getCanonicalName());            
            // parse a macroscopic network representation + register on assignment
            network = (MacroscopicNetwork) this.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
            // parse the zoning system + register on assignment
            zoning = this.createAndRegisterZoning(network);
            // parse the demands + register on assignment
            demands = this.createAndRegisterDemands(zoning, network);            
        } catch (final PlanItException e) {
        	LOGGER.log(Level.SEVERE, "Could not instantiate default settings for project", e);
        }
    }

    // Public

    /**
     * Base constructor for simple project which adopts the PlanItIO input/output format. It is assumed
     * all input files are in the current working directory
     * @throws PlanItException thrown in case the default input builder cannot be created
     *
     */
    public PlanItSimpleProject() throws PlanItException {
        // use the default input builder with the current path as the project path
        super(new PlanItInputBuilder(System.getProperty("user.dir")));
        initialiseSimpleProject();
    }

    /**
     * Base constructor for simple project which adopts the PlanItIO input/output format
     *
     * @param projectPath to retrieve the files from
     * @throws PlanItException
     */
    public PlanItSimpleProject(final String projectPath) throws PlanItException {
        // use the default input builder
        super(new PlanItInputBuilder(projectPath));
        LOGGER.info("Searching for input files in: "+Paths.get(projectPath).toAbsolutePath().toString());
        initialiseSimpleProject();
    }

	public PlanItSimpleProject(final PlanItInputBuilder planItInputBuilder) {
      super(planItInputBuilder);
      initialiseSimpleProject();
    }
    
    
    /** On a simple project we only allow a single assignment to be registered. This is verified here. If multiple assignments
     * are required within the same project, then a simple project cannot be used. Registration of a traffic assignment type
     * also includes parsing the network, zoning, and demands that are registered alongside the chosen assignment method
     *
     * @param trafficAssignmentType the traffic assignment type to be used
     * @return trafficAssignmentBuilder the builder to configure this traffic assignment instance
     */
    public TrafficAssignmentBuilder createAndRegisterTrafficAssignment(final String trafficAssignmentType)
            throws PlanItException {
        if(super.trafficAssignments.hasRegisteredAssignments()) {
            String errorMessage = "This type of PLANit project only allows a single assignment per project";
            LOGGER.severe(errorMessage);
            throw new PlanItException(errorMessage);
       }
        return super.createAndRegisterTrafficAssignment(trafficAssignmentType, demands, zoning, network);
    }

    /**
     * Disallow the use of the generic create and register traffic assignment because a simple project
     * automatically determines its demands, zoning, and network
     *
     * @param trafficAssignmentType
     * @param theDemands
     * @param theZoning
     * @param thePhysicalNetwork
     */
   @Override
	public TrafficAssignmentBuilder createAndRegisterTrafficAssignment(
    		final String trafficAssignmentType,
    		final Demands theDemands,
    		final Zoning theZoning,
    		final PhysicalNetwork thePhysicalNetwork)
            throws PlanItException {
    	String errorMessage = "A simple project only allows to create and register a traffic assignment by type only, other inputs are automatically collected";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
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
            exceptionMap = super.executeAllTrafficAssignments();
        }else
        {
          LOGGER.info("No traffic assignment has been registered yet, terminating execution");
        }
        return exceptionMap;
    }

    /** Collect the default output formatter for PLANit simple project which is the native XMLFormatter
     * @return defaultOutputformatter
     */
    public PlanItOutputFormatter getDefaultOutputFormatter() {
        return this.defaultOutputFormatter;
    }

    /**
     * Return the current network object
     * 
     * @return the current network
     */
    public MacroscopicNetwork getNetwork() {
		return network;
	}

    /**
     * Return the current Zoning object
     * 
     * @return the current zoning object
     */
	public Zoning getZoning() {
		return zoning;
	}

	/**
	 * Return the current Demands object
	 * 
	 * @return the current demands
	 */
	public Demands getDemands() {
		return demands;
	}
	
	/**
	 * Retrieve a time period by its external Id
	 * 
	 * @param externalId the external Id to search on
	 * @return the retrieved time period
	 */
	public TimePeriod getTimePeriodByExternalId(int externalId)	{
	  return inputBuilderListener.getTimePeriodByExternalId((long) externalId);
	}
	
	/**
	 * Retrieve a mode by its external Id
	 * 
	 * @param externalId the external Id to search on
	 * @return the retrieved mode
	 */
	public Mode getModeByExternalId(int externalId) {
    return inputBuilderListener.getModeByExternalId((long) externalId);
	}
	
	/**
	 * Retrieve a list of the external Ids of all registered time periods 
	 * 
	 * @return List of all registered time periods
	 */
	public List<Object> getTimePeriodExternalIds() {
	  return inputBuilderListener.getTimePeriodExternalIds();
	}
	
}