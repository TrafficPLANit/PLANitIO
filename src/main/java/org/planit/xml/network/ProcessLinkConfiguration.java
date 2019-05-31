package org.planit.xml.network;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.generated.Linkconfiguration;
import org.planit.generated.Modes;
import org.planit.userclass.Mode;

public class ProcessLinkConfiguration {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(ProcessLinkConfiguration.class.getName());
	
	public static Map<Integer, Mode> getModeMap(Linkconfiguration linkconfiguration) throws PlanItException {
        Map<Integer, Mode> modeMap = new HashMap<Integer, Mode>();
        for (Modes.Mode generatedMode : linkconfiguration.getModes().getMode()) {
            int modeId = generatedMode.getId().intValue();
            if (modeId == 0) {
                throw new PlanItException("Found a Mode value of 0 in the modes definition file, this is prohibited");
            }
            String name = generatedMode.getName();
            double pcu = generatedMode.getPcu();
            Mode mode = new Mode(modeId, name, pcu);
            modeMap.put(modeId, mode);
        }
        return modeMap;
	}
}
