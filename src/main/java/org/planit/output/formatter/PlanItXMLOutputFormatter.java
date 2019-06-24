package org.planit.output.formatter;

import java.util.Set;
import java.util.logging.Logger;

import org.planit.data.SimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.project.PlanItProject;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;

/**
 * The default output formatter of PlanIt
 * 
 * @author markr
 *
 */
public class PlanItXMLOutputFormatter extends BaseOutputFormatter {
    
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(PlanItProject.class.getName());      

    @Override
    public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputTypeConfiguration outputTypeConfiguration,
			SimulationData simulationData)
            throws PlanItException {
        LOGGER.info("Persisting time period: "+ timePeriod.toString());
        for(Mode mode: modes) {
            LOGGER.info("Persisting mode: "+ mode.toString() + " persist type: "+ outputTypeConfiguration.toString());    
        }
        LOGGER.info("NOT IMPLEMENTED - PERSISTENCE IGNORED FOR ALL OF THE ABOVE");
    }

    @Override
    public void open() throws PlanItException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void close() throws PlanItException {
        // TODO Auto-generated method stub
        
    }

}