package org.planit.input;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.demand.Demands;
import org.planit.event.CreatedProjectComponentEvent;
import org.planit.event.listener.InputBuilderListener;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.project.PlanItProject;
import org.planit.zoning.Zoning;

/**
 * The default input builder for PLANit projects unless otherwise indicated
 * @author markr
 *
 */
public class PlanItXMLInputBuilder implements InputBuilderListener {
    
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(PlanItProject.class.getName());
    
    private final Path projectPath;  
    
    /** Constructor of PLANit XML input taking the uri path where the input files are assumed to be located
     * @param projectPath
     */
    public PlanItXMLInputBuilder(String projectPath) {
        this.projectPath = Paths.get(projectPath);
    }

    /* (non-Javadoc)
     * @see org.planit.event.listener.InputBuilderListener#onCreateProjectComponent(org.planit.event.CreatedProjectComponentEvent)
     */
    @Override
    public void onCreateProjectComponent(CreatedProjectComponentEvent<?> event) throws PlanItException {
        Object projectComponent = event.getProjectComponent();
        // deal with the various PLANit components currently available
        if (projectComponent instanceof MacroscopicNetwork) {
            //populateNetwork((MacroscopicNetwork) projectComponent); 
            LOGGER.info("populating network - not yet implemented - ignore");
        } else if (projectComponent instanceof Zoning) {
            //populateZoning((Zoning) projectComponent);
            LOGGER.info("populating zoning - not yet implemented - ignore");
        } else if (projectComponent instanceof Demands) {
            //populateDemands((Demands) projectComponent);
            LOGGER.info("populating demands - not yet implemented - ignore");
        } else if (projectComponent instanceof PhysicalCost) {
            //populatePhysicalCost((PhysicalCost) projectComponent); 
            LOGGER.info("populating physical cost - not yet implemented - ignore");
        } else if (projectComponent instanceof VirtualCost) {
            //populateVirtualCost((VirtualCost) projectComponent);
            LOGGER.info("populating virtual cost - not yet implemented - ignore");            
        } else {
            LOGGER.fine("Event component is " + projectComponent.getClass().getCanonicalName() + " which is not yet implement by PLANitXML");
        }
        
    }
}
