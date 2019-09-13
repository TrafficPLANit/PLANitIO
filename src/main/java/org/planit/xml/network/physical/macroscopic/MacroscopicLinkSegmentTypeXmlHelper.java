package org.planit.xml.network.physical.macroscopic;

import java.util.HashMap;
import java.util.Map;

import org.planit.logging.PlanItLogger;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentTypeModeProperties;
import org.planit.network.physical.macroscopic.MacroscopicModeProperties;
import org.planit.userclass.Mode;

/**
 * Helper class used to create MacroscopicLinkSegmentType objects for XML input
 * 
 * @author gman6028
 *
 */
public class MacroscopicLinkSegmentTypeXmlHelper {

	/**
	 * External reference number of link type
	 */
	private int externalId;

	/**
	 * Name of link type
	 */
	private String name;

	/**
	 * Capacity per lane of link type
	 */
	private double capacityPerLane;

	/**
	 * Maximum density per lane of link type
	 */
	private double maximumDensityPerLane;

	/**
	 * Map of maximum speed for each link type
	 */
	private Map<Long, Double> speedMap;

	/**
	 * Link Segment Mode Properties (maximum speed and critical speed)
	 */
	private MacroscopicLinkSegmentTypeModeProperties macroscopicLinkSegmentTypeModeProperties;

	private static Map<Integer, MacroscopicLinkSegmentTypeXmlHelper> existingLinks;

	/**
	 * Update an existing XmlMacroscopicLinkSegmentType linkSegmentType
	 * 
	 * @param linkSegmentType XmlMacroscopicLinkSegmentType linkSegmentType object
	 *                        to be updated
	 * @param modeMap         Map storing modes
	 * @param modeExternalId  external Id of the mode used in the XML input file
	 * @param  maxSpeed           maximum speed
	 * @param critSpeed       critical speed
	 * @param modeExternalId  reference to the mode for the current link segment
	 *                        type
	 * @param externalId      id number of the link type
	 */
	private static void updateLinkSegmentType(MacroscopicLinkSegmentTypeXmlHelper linkSegmentType,
			Map<Integer, Mode> modeMap, long modeExternalId, double maxSpeed, double critSpeed, int externalId) {
		if ( maxSpeed < 0.0) {
			PlanItLogger.warning("A negative maximum speed has been defined for Link Type " + linkSegmentType.getName()
					+ " and Mode " + modeMap.get((int) modeExternalId).getName()
					+ ".  Setting the speed to zero instead (which means vehicles of this type are forbidden in links of this type.)");
			 maxSpeed = 0.0;
		}
		linkSegmentType.getSpeedMap().put(modeExternalId,  maxSpeed);
		MacroscopicModeProperties macroscopicModeProperties = new MacroscopicModeProperties(maxSpeed, critSpeed);
		Mode mode = Mode.getByExternalId(modeExternalId);
		MacroscopicLinkSegmentTypeModeProperties macroscopicLinkSegmentTypeModeProperties = new MacroscopicLinkSegmentTypeModeProperties(
				mode, macroscopicModeProperties);
		linkSegmentType.setMacroscopicLinkSegmentTypeModeProperties(macroscopicLinkSegmentTypeModeProperties);
		existingLinks.put(externalId, linkSegmentType);
	}

	/**
	 * Constructor
	 * 
	 * @param name                  name of the link type
	 * @param capacityPerLane       capacity per lane
	 * @param maximumDensityPerLane maximum density per lane
	 * @param externalId            external Id of the link segment type
	 */
	public MacroscopicLinkSegmentTypeXmlHelper(String name, double capacityPerLane, double maximumDensityPerLane,
			int externalId) {
		this.name = name;
		this.capacityPerLane = capacityPerLane;
		this.maximumDensityPerLane = maximumDensityPerLane;
		this.externalId = externalId;
		speedMap = new HashMap<Long, Double>();
	}

	/**
	 * Reset the store of existing links
	 */
	public static void reset() {
		existingLinks = new HashMap<Integer, MacroscopicLinkSegmentTypeXmlHelper>();
	}

	/**
	 * Create or update an XmlMacroscopicLinkSegmentType object using the input from
	 * the current row in the XML input file
	 * 
	 * If the mode number is 0, all modes are updated
	 * 
	 * @param name                  link type name
	 * @param capacityPerLane       capacity per lane
	 * @param maximumDensityPerLane maximum density per lane
	 * @param maxSpeed                 maximum speed
	 * @param critSpeed             critical speed
	 * @param modeExternalId        reference to the mode used in the XML input file
	 * @param modeMap               Map storing modes
	 * @param externalId            id number of the link type
	 * @return XmlMacroscopicLinkSegmentType object, created or updated to include
	 *         data from current row in the XML file
	 */
	public static MacroscopicLinkSegmentTypeXmlHelper createOrUpdateLinkSegmentType(String name, double capacityPerLane,
			double maximumDensityPerLane, double maxSpeed, double critSpeed, long modeExternalId,
			Map<Integer, Mode> modeMap, int externalId) {
		MacroscopicLinkSegmentTypeXmlHelper linkSegmentType;
		if (!existingLinks.containsKey(externalId)) {
			if (capacityPerLane == 0.0) {
				PlanItLogger.warning(
						"Link Type " + name + " initially defined without a capacity, being given a capacity of zero.");
			}
			linkSegmentType = new MacroscopicLinkSegmentTypeXmlHelper(name, capacityPerLane, maximumDensityPerLane,
					externalId);
		} else {
			linkSegmentType = existingLinks.get(externalId);
			if (capacityPerLane != linkSegmentType.getCapacityPerLane()) {
				PlanItLogger.warning("Different capacity per lane values for Link Type " + linkSegmentType.getName()
						+ ".  Will use the highest one.");
			}
			if (capacityPerLane > linkSegmentType.getCapacityPerLane()) {
				linkSegmentType.setCapacityPerLane(capacityPerLane);
			}
			if (maximumDensityPerLane != linkSegmentType.getMaximumDensityPerLane()) {
				PlanItLogger.warning("Different maximum density per lane values for link type " + linkSegmentType.getName()
						+ ".  Will use the highest one.");
			}
			if (maximumDensityPerLane > linkSegmentType.getMaximumDensityPerLane()) {
				linkSegmentType.setMaximumDensityPerLane(maximumDensityPerLane);
			}
		}
		if (modeExternalId == 0) {
			modeMap.keySet().forEach(eachModeNo -> {
				updateLinkSegmentType(linkSegmentType, modeMap, eachModeNo, maxSpeed, critSpeed, externalId);
			});
		} else {
			updateLinkSegmentType(linkSegmentType, modeMap, modeExternalId, maxSpeed, critSpeed, externalId);
		}
		return linkSegmentType;
	}

	public String getName() {
		return name;
	}

	public double getCapacityPerLane() {
		return capacityPerLane;
	}

	public void setCapacityPerLane(double capacityPerLane) {
		this.capacityPerLane = capacityPerLane;
	}

	public double getMaximumDensityPerLane() {
		return maximumDensityPerLane;
	}

	public void setMaximumDensityPerLane(double maximumDensityPerLane) {
		this.maximumDensityPerLane = maximumDensityPerLane;
	}

	public Map<Long, Double> getSpeedMap() {
		return speedMap;
	}

	public int getExternalId() {
		return externalId;
	}

	public MacroscopicLinkSegmentTypeModeProperties getMacroscopicLinkSegmentTypeModeProperties() {
		return macroscopicLinkSegmentTypeModeProperties;
	}

	public void setMacroscopicLinkSegmentTypeModeProperties(
			MacroscopicLinkSegmentTypeModeProperties macroscopicLinkSegmentTypeModeProperties) {
		this.macroscopicLinkSegmentTypeModeProperties = macroscopicLinkSegmentTypeModeProperties;
	}

}