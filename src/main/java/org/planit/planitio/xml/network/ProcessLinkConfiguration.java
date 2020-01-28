package org.planit.planitio.xml.network;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.planit.exceptions.PlanItException;
import org.planit.generated.XMLElementLinkConfiguration;
import org.planit.generated.XMLElementLinkSegmentTypes;
import org.planit.generated.XMLElementModes;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.planitio.xml.network.physical.macroscopic.MacroscopicLinkSegmentTypeXmlHelper;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Process the LinkConfiguration object populated with data from the XML file
 * 
 * @author gman6028
 *
 */
public class ProcessLinkConfiguration {

	/**
	 * Reads mode types from input file and stores them in a Map
	 * @param physicalNetwork 
	 * @param linkconfiguration LinkConfiguration object populated with data from XML file
	 * @throws PlanItException thrown if there is a Mode value of 0 in the modes definition file
	 */
	public static Map<Long,Mode> createModes(PhysicalNetwork physicalNetwork, XMLElementLinkConfiguration linkconfiguration) throws PlanItException {
		Map<Long,Mode> modesByExternalId = new TreeMap<Long,Mode>();
		for (XMLElementModes.Mode generatedMode : linkconfiguration.getModes().getMode()) {
			long externalModeId = generatedMode.getId().longValue();
			if (externalModeId == 0) {
				throw new PlanItException("Found a Mode value of 0 in the modes definition file, this is prohibited");
			}
			String name = generatedMode.getName();
			double pcu = generatedMode.getPcu();
			Mode newMode = physicalNetwork.modes.registerNewMode(externalModeId, name, pcu);
			modesByExternalId.put(newMode.getExternalId(), newMode);
		}
		return modesByExternalId;
	}
	
	/**
	 * Reads link type values from input file and stores them in a Map
	 * 
	 * @param linkconfiguration LinkConfiguration object populated with data from XML file
	 * @param modesByExternalIdMap identified modes by their external id
	 * @return Map containing link type values
	 * @throws PlanItException thrown if there is an error reading the input file
	 */
	public static Map<Integer, MacroscopicLinkSegmentTypeXmlHelper> createLinkSegmentTypeMap(
			XMLElementLinkConfiguration linkconfiguration, 
			Map<Long, Mode> modesByExternalIdMap) throws PlanItException {
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
				int modeExternalId = mode.getRef().intValue();
				double maxSpeed = (mode.getMaxspeed() == null) ? MacroscopicModeProperties.DEFAULT_MAXIMUM_SPEED
						: mode.getMaxspeed();
				double critSpeed = (mode.getCritspeed() == null) ? MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED
						: mode.getCritspeed();
				MacroscopicLinkSegmentTypeXmlHelper linkSegmentType = MacroscopicLinkSegmentTypeXmlHelper
						.createOrUpdateLinkSegmentType(name, capacity, maximumDensity, maxSpeed, critSpeed, modeExternalId,	type, modesByExternalIdMap);
				linkSegmentMap.put(type, linkSegmentType);
			}
		}
		return linkSegmentMap;
	}

}
