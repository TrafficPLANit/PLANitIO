package org.planit.planitio.xml.network;

import java.util.HashMap;
import java.util.Map;

import org.planit.exceptions.PlanItException;
import org.planit.generated.XMLElementLinkConfiguration;
import org.planit.generated.XMLElementLinkSegmentTypes;
import org.planit.generated.XMLElementModes;
import org.planit.logging.PlanItLogger;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.network.physical.macroscopic.MacroscopicModeProperties;
import org.planit.planitio.xml.network.physical.macroscopic.MacroscopicLinkSegmentTypeXmlHelper;
import org.planit.userclass.Mode;

/**
 * Process the LinkConfiguration object populated with data from the XML file
 * 
 * @author gman6028
 *
 */
public class ProcessLinkConfiguration {

	/**
	 * Reads mode types from input file and stores them in a Map
	 * 
	 * @param linkconfiguration LinkConfiguration object populated with data from
	 *                          XML file
	 */
	public static Map<Integer, Mode> getModeMap(XMLElementLinkConfiguration linkconfiguration) throws PlanItException {
		Map<Integer, Mode> modeMap = new HashMap<Integer, Mode>();
		for (XMLElementModes.Mode generatedMode : linkconfiguration.getModes().getMode()) {
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
			XMLElementLinkConfiguration linkconfiguration, Map<Integer, Mode> modeMap) throws PlanItException {
		MacroscopicLinkSegmentTypeXmlHelper.reset();
		Map<Integer, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentMap = new HashMap<Integer, MacroscopicLinkSegmentTypeXmlHelper>();
		for (XMLElementLinkSegmentTypes.Linksegmenttype linkSegmentTypeGenerated : linkconfiguration.getLinksegmenttypes()
				.getLinksegmenttype()) {
			int type = linkSegmentTypeGenerated.getId().intValue();
			String name = linkSegmentTypeGenerated.getName();
			double capacity = (linkSegmentTypeGenerated.getCapacitylane() == null)
					? MacroscopicLinkSegmentType.DEFAULT_CAPACITY_LANE
					: linkSegmentTypeGenerated.getCapacitylane();
			double maximumDensity = (linkSegmentTypeGenerated.getMaxdensitylane() == null)
					? MacroscopicLinkSegmentType.DEFAULT_MAXIMUM_DENSITY_LANE
					: linkSegmentTypeGenerated.getMaxdensitylane();
			for (XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode mode : linkSegmentTypeGenerated.getModes().getMode()) {
				int modeId = mode.getRef().intValue();
				double maxSpeed = (mode.getMaxspeed() == null) ? MacroscopicModeProperties.DEFAULT_MAXIMUM_SPEED
						: mode.getMaxspeed();
				double critSpeed = (mode.getCritspeed() == null) ? MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED
						: mode.getCritspeed();
				MacroscopicLinkSegmentTypeXmlHelper linkSegmentType = MacroscopicLinkSegmentTypeXmlHelper
						.createOrUpdateLinkSegmentType(name, capacity, maximumDensity, maxSpeed, critSpeed, modeId,
								modeMap, type);
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
					PlanItLogger.info("Mode " + mode.getName() + " not defined for Link Type " + linkSegmentType.getName()
							+ ".  Will be given a speed zero, meaning vehicles of this type are not allowed in links of this type.");
					MacroscopicLinkSegmentTypeXmlHelper linkSegmentTypeNew = MacroscopicLinkSegmentTypeXmlHelper
							.createOrUpdateLinkSegmentType(linkSegmentType.getName(), 0.0,
									MacroscopicLinkSegmentType.DEFAULT_MAXIMUM_DENSITY_LANE,
									MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED, 0.0, (int) modeExternalId,
									modeMap, linkType);
					linkSegmentMap.put(linkType, linkSegmentTypeNew);
				}
			}
		}
		return linkSegmentMap;
	}

}
