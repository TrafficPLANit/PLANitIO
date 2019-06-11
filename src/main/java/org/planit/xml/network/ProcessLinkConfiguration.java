package org.planit.xml.network;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.exceptions.PlanItException;
import org.planit.generated.Linkconfiguration;
import org.planit.generated.Linksegmenttypes;
import org.planit.generated.Modes;
import org.planit.userclass.Mode;
import org.planit.constants.Default;
import org.planit.xml.network.physical.macroscopic.MacroscopicLinkSegmentTypeXmlHelper;

public class ProcessLinkConfiguration {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(ProcessLinkConfiguration.class.getName());

	/**
	 * Reads mode types from input file and stores them in a Map
	 * 
	 * @param linkconfiguration LinkConfiguration object populated with data from
	 *                          XML file
	 */
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

	/**
	 * Reads link type values from input file and stores them in a Map
	 * 
	 * @param linkconfiguration LinkConfiguration object populated with data from
	 *                          XML file
	 * @param modeMap           Map of Mode objects
	 * @return Map containing link type values
	 * @throws PlanItException thrown if there is an error reading the input file
	 */
	public static Map<Integer, MacroscopicLinkSegmentTypeXmlHelper> createLinkSegmentTypeMap(
			Linkconfiguration linkconfiguration, Map<Integer, Mode> modeMap) throws PlanItException {
		MacroscopicLinkSegmentTypeXmlHelper.reset();
		Map<Integer, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentMap = new HashMap<Integer, MacroscopicLinkSegmentTypeXmlHelper>();
		for (Linksegmenttypes.Linksegmenttype linkSegmentTypeGenerated : linkconfiguration.getLinksegmenttypes()
				.getLinksegmenttype()) {
			int type = linkSegmentTypeGenerated.getId().intValue();
			String name = linkSegmentTypeGenerated.getName();
			Float capacity = (linkSegmentTypeGenerated.getCapacitylane() == null) ? Default.LANE_CAPACITY
					: linkSegmentTypeGenerated.getCapacitylane();
			Float maximumDensity = (linkSegmentTypeGenerated.getMaxdensitylane() == null) ? Default.MAXIMUM_LANE_DENSITY
					: linkSegmentTypeGenerated.getMaxdensitylane();
			for (Linksegmenttypes.Linksegmenttype.Modes.Mode mode : linkSegmentTypeGenerated.getModes().getMode()) {
				int modeId = mode.getRef().intValue();
				Float speed = mode.getMaxspeed();
				MacroscopicLinkSegmentTypeXmlHelper linkSegmentType = MacroscopicLinkSegmentTypeXmlHelper
						.createOrUpdateLinkSegmentType(name, capacity, maximumDensity, speed, modeId, modeMap, type);
				linkSegmentMap.put(type, linkSegmentType);
			}
		}
		// If a mode is missing for a link type, set the speed to zero for vehicles of
		// this type in this link type, meaning they are forbidden
		for (Integer linkType : linkSegmentMap.keySet()) {
			MacroscopicLinkSegmentTypeXmlHelper linkSegmentType = linkSegmentMap.get(linkType);
			for (Mode mode : modeMap.values()) {
				long modeExternalId = mode.getExternalId();
				if (!linkSegmentType.getSpeedMap().containsKey(modeExternalId)) {
					LOGGER.info("Mode " + mode.getName() + " not defined for Link Type " + linkSegmentType.getName()
							+ ".  Will be given a speed zero, meaning vehicles of this type are not allowed in links of this type.");
					MacroscopicLinkSegmentTypeXmlHelper linkSegmentTypeNew = MacroscopicLinkSegmentTypeXmlHelper
							.createOrUpdateLinkSegmentType(linkSegmentType.getName(), 0.0, Default.MAXIMUM_LANE_DENSITY,
									0.0, (int) modeExternalId, modeMap, linkType);
					linkSegmentMap.put(linkType, linkSegmentTypeNew);
				}
			}
		}
		return linkSegmentMap;
	}

}
