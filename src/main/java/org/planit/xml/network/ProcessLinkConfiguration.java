package org.planit.xml.network;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.basiccsv.network.physical.macroscopic.BasicCsvMacroscopicLinkSegmentType;
import org.planit.exceptions.PlanItException;
import org.planit.generated.Linkconfiguration;
import org.planit.generated.Linksegmenttypes;
import org.planit.generated.Modes;
import org.planit.userclass.Mode;
import org.planit.xml.input.PlanItXml;

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
/*	
	public static Map<Integer, BasicCsvMacroscopicLinkSegmentType> createLinkSegmentTypeMap(Linkconfiguration linkconfiguration, Map<Integer, Mode> modeMap) {
        BasicCsvMacroscopicLinkSegmentType.reset();
        Map<Integer, BasicCsvMacroscopicLinkSegmentType> linkSegmentMap = new HashMap<Integer, BasicCsvMacroscopicLinkSegmentType>();
        for (Linksegmenttypes.Linksegmenttype linksegmenttype : linkconfiguration.getLinksegmenttypes().getLinksegmenttype()) {
            int type = linksegmenttype.getId().intValue();
            String name = linksegmenttype.getName();
            double maximumDensity = (linksegmenttype.getMaxdensitylane() == null) ? Double.POSITIVE_INFINITY : linksegmenttype.getMaxdensitylane();
            
            double speed = Double.parseDouble(record.get("Speed"));
            String capacityString = record.get("Capacity");
            double capacity;
            if ((capacityString != null) && (!capacityString.equals(""))) {
               capacity = Double.parseDouble(capacityString);
            } else {
               capacity = 0.0;
            }
            int modeId = Integer.parseInt(record.get("Mode"));
            if  ((modeId != 0) && (!modeMap.containsKey(modeId))) {
                throw new PlanItException("Mode Id " + modeId + " found in link types file but not in modes definition file");
            }
            double alpha = Double.parseDouble(record.get("Alpha"));
            double beta = Double.parseDouble(record.get("Beta"));
            BasicCsvMacroscopicLinkSegmentType linkSegmentType = BasicCsvMacroscopicLinkSegmentType.createOrUpdateLinkSegmentType(name, capacity, maximumDensity, speed, alpha, beta, modeId,  modeMap, type);
            linkSegmentMap.put(type, linkSegmentType);
        }
        //If a mode is missing for a link type, set the speed to zero for vehicles of this type in this link type, meaning they are forbidden
        for (Integer linkType : linkSegmentMap.keySet()) {
            BasicCsvMacroscopicLinkSegmentType linkSegmentType = linkSegmentMap.get(linkType);
            for (Mode mode : modeMap.values()) {
                long modeId = mode.getId();
                if (!linkSegmentType.getSpeedMap().containsKey(modeId)) {
                    LOGGER.info("Mode " + mode.getName() + " not defined for Link Type " + linkSegmentType.getName() + ".  Will be given a speed zero, meaning vehicles of this type are not allowed in links of this type.");
                    BasicCsvMacroscopicLinkSegmentType linkSegmentTypeNew = BasicCsvMacroscopicLinkSegmentType.createOrUpdateLinkSegmentType(linkSegmentType.getName(), 0.0, maximumDensity, 0.0, 0.0, 0.0, (int) modeId,  modeMap, linkType);
                    linkSegmentMap.put(linkType, linkSegmentTypeNew);
                }
            }
        }           
        return linkSegmentMap;
	}
*/
}
