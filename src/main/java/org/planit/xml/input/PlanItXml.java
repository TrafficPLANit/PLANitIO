package org.planit.xml.input;

import java.util.logging.Logger;

import org.planit.basiccsv.input.BasicCsvScan;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
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
public class PlanItXml implements InputBuilderListener {
    
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(PlanItProject.class.getName());
    
    private BasicCsvScan basicCsvScan;
    
    /** Constructor of PLANit XML input taking the uri path where the input files are assumed to be located
     * @param projectPath
     */
 //   public PlanItXMLInputBuilder(String projectPath) {
 //       this.projectPath = Paths.get(projectPath);
 //   }

    public PlanItXml(String networkFileLocation, String demandFileLocation, String linkTypesFileLocation, String zoneFileLocation, String timePeriodFileLocation, String modeFileLocation) {
    	basicCsvScan = new BasicCsvScan(networkFileLocation, demandFileLocation, linkTypesFileLocation, zoneFileLocation, timePeriodFileLocation, modeFileLocation);
    }
    
    
    /* (non-Javadoc)
     * @see org.planit.event.listener.InputBuilderListener#onCreateProjectComponent(org.planit.event.CreatedProjectComponentEvent)
     */
    @Override
    public void onCreateProjectComponent(CreatedProjectComponentEvent<?> event) throws PlanItException {
        Object projectComponent = event.getProjectComponent();
        // deal with the various PLANit components currently available
        if (projectComponent instanceof MacroscopicNetwork) {
            basicCsvScan.populateNetwork((MacroscopicNetwork) projectComponent); 
            LOGGER.info("Populating Network - uses CSV");
        } else if (projectComponent instanceof Zoning) {
        	basicCsvScan.populateZoning((Zoning) projectComponent);
            LOGGER.info("Populating Zoning - use CSV");
        } else if (projectComponent instanceof Demands) {
        	basicCsvScan.populateDemands((Demands) projectComponent);
            LOGGER.info("Populating Demands - uses CSV");
        } else if (projectComponent instanceof BPRLinkTravelTimeCost) {
        	basicCsvScan.populateBprParameters((BPRLinkTravelTimeCost) projectComponent); //place parameters on the BPR cost component
            LOGGER.info("Populating BPR Costs - uses CSV");
        } else {
            LOGGER.fine("Event component is " + projectComponent.getClass().getCanonicalName() + " which is not yet implement by PLANitXml");
        }
        
    }
}
