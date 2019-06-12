package org.planit.xml.network.physical.macroscopic;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.constants.Default;
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
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentTypeXmlHelper.class.getName());

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

	private MacroscopicLinkSegmentTypeModeProperties macroscopicLinkSegmentTypeModeProperties;

	private static Map<Integer, MacroscopicLinkSegmentTypeXmlHelper> existingLinks;

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
	 * @param speed                 maximum speed
	 * @param modeExternalId        reference to the mode used in the XML input file
	 * @param modeMap               Map storing modes
	 * @param externalId            id number of the link type
	 * @return XmlMacroscopicLinkSegmentType object, created or updated to include
	 *         data from current row in the XML file
	 */
	public static MacroscopicLinkSegmentTypeXmlHelper createOrUpdateLinkSegmentType(String name, double capacityPerLane,
			double maximumDensityPerLane, double speed, long modeExternalId, Map<Integer, Mode> modeMap,
			int externalId) {
		MacroscopicLinkSegmentTypeXmlHelper linkSegmentType;
		if (!existingLinks.containsKey(externalId)) {
			if (capacityPerLane == 0.0) {
				LOGGER.warning(
						"Link Type " + name + " initially defined without a capacity, being given a capacity of zero.");
			}
			linkSegmentType = new MacroscopicLinkSegmentTypeXmlHelper(name, capacityPerLane, maximumDensityPerLane,
					externalId);
		} else {
			linkSegmentType = existingLinks.get(externalId);
			if (capacityPerLane != linkSegmentType.getCapacityPerLane()) {
				LOGGER.warning("Different capacity per lane values for Link Type " + linkSegmentType.getName()
						+ ".  Will use the highest one.");
			}
			if (capacityPerLane > linkSegmentType.getCapacityPerLane()) {
				linkSegmentType.setCapacityPerLane(capacityPerLane);
			}
			if (maximumDensityPerLane != linkSegmentType.getMaximumDensityPerLane()) {
				LOGGER.warning("Different maximum density per lane values for link type " + linkSegmentType.getName()
						+ ".  Will use the highest one.");
			}
			if (maximumDensityPerLane > linkSegmentType.getMaximumDensityPerLane()) {
				linkSegmentType.setMaximumDensityPerLane(maximumDensityPerLane);
			}
		}
		if (modeExternalId == 0) {
			modeMap.keySet().forEach(eachModeNo -> {
				updateLinkSegmentType(linkSegmentType, modeMap, eachModeNo, speed, externalId);
			});
		} else {
			updateLinkSegmentType(linkSegmentType, modeMap, modeExternalId, speed, externalId);
		}
		return linkSegmentType;
	}

	public static MacroscopicLinkSegmentTypeXmlHelper createOrUpdateLinkSegmentType(String name, double capacityPerLane,
			double maximumDensityPerLane, double speed, double critSpeed, long modeExternalId,
			Map<Integer, Mode> modeMap, int externalId) {
		MacroscopicLinkSegmentTypeXmlHelper linkSegmentType;
		if (!existingLinks.containsKey(externalId)) {
			if (capacityPerLane == 0.0) {
				LOGGER.warning(
						"Link Type " + name + " initially defined without a capacity, being given a capacity of zero.");
			}
			linkSegmentType = new MacroscopicLinkSegmentTypeXmlHelper(name, capacityPerLane, maximumDensityPerLane,
					externalId);
		} else {
			linkSegmentType = existingLinks.get(externalId);
			if (capacityPerLane != linkSegmentType.getCapacityPerLane()) {
				LOGGER.warning("Different capacity per lane values for Link Type " + linkSegmentType.getName()
						+ ".  Will use the highest one.");
			}
			if (capacityPerLane > linkSegmentType.getCapacityPerLane()) {
				linkSegmentType.setCapacityPerLane(capacityPerLane);
			}
			if (maximumDensityPerLane != linkSegmentType.getMaximumDensityPerLane()) {
				LOGGER.warning("Different maximum density per lane values for link type " + linkSegmentType.getName()
						+ ".  Will use the highest one.");
			}
			if (maximumDensityPerLane > linkSegmentType.getMaximumDensityPerLane()) {
				linkSegmentType.setMaximumDensityPerLane(maximumDensityPerLane);
			}
		}
		if (modeExternalId == 0) {
			modeMap.keySet().forEach(eachModeNo -> {
				updateLinkSegmentType(linkSegmentType, modeMap, eachModeNo, speed, critSpeed, externalId);
			});
		} else {
			updateLinkSegmentType(linkSegmentType, modeMap, modeExternalId, speed, critSpeed, externalId);
		}
		return linkSegmentType;
	}

	/**
	 * Update an existing XmlMacroscopicLinkSegmentType linkSegmentType
	 * 
	 * @param linkSegmentType XmlMacroscopicLinkSegmentType linkSegmentType object
	 *                        to be updated
	 * @param modeMap         Map storing modes
	 * @param modeExternalId  external Id of the mode used in the XML input file
	 * @param speed           maximum speed
	 * @param modeExternalId  reference to the mode for the current link segment
	 *                        type
	 * @param externalId      id number of the link type
	 */
	private static void updateLinkSegmentType(MacroscopicLinkSegmentTypeXmlHelper linkSegmentType,
			Map<Integer, Mode> modeMap, long modeExternalId, double speed, int externalId) {
		if (speed < 0.0) {
			LOGGER.warning("A negative maximum speed has been defined for Link Type " + linkSegmentType.getName()
					+ " and Mode " + modeMap.get((int) modeExternalId).getName()
					+ ".  Setting the speed to zero instead (which means vehicles of this type are forbidden in links of this type.)");
			speed = 0.0;
		}
		linkSegmentType.getSpeedMap().put(modeExternalId, speed);
		MacroscopicModeProperties macroscopicModeProperties = new MacroscopicModeProperties(speed,
				Default.CRITICAL_SPEED);
		Mode mode = Mode.getByExternalId(modeExternalId);
		MacroscopicLinkSegmentTypeModeProperties macroscopicLinkSegmentTypeModeProperties = new MacroscopicLinkSegmentTypeModeProperties(
				mode, macroscopicModeProperties);
		linkSegmentType.setMacroscopicLinkSegmentTypeModeProperties(macroscopicLinkSegmentTypeModeProperties);
		existingLinks.put(externalId, linkSegmentType);
	}

	/**
	 * Update an existing XmlMacroscopicLinkSegmentType linkSegmentType
	 * 
	 * @param linkSegmentType XmlMacroscopicLinkSegmentType linkSegmentType object
	 *                        to be updated
	 * @param modeMap         Map storing modes
	 * @param modeExternalId  external Id of the mode used in the XML input file
	 * @param speed           maximum speed
	 * @param critSpeed       critical speed
	 * @param modeExternalId  reference to the mode for the current link segment
	 *                        type
	 * @param externalId      id number of the link type
	 */
	private static void updateLinkSegmentType(MacroscopicLinkSegmentTypeXmlHelper linkSegmentType,
			Map<Integer, Mode> modeMap, long modeExternalId, double speed, double critSpeed, int externalId) {
		if (speed < 0.0) {
			LOGGER.warning("A negative maximum speed has been defined for Link Type " + linkSegmentType.getName()
					+ " and Mode " + modeMap.get((int) modeExternalId).getName()
					+ ".  Setting the speed to zero instead (which means vehicles of this type are forbidden in links of this type.)");
			speed = 0.0;
		}
		linkSegmentType.getSpeedMap().put(modeExternalId, speed);
		MacroscopicModeProperties macroscopicModeProperties = new MacroscopicModeProperties(speed, critSpeed);
		Mode mode = Mode.getByExternalId(modeExternalId);
		MacroscopicLinkSegmentTypeModeProperties macroscopicLinkSegmentTypeModeProperties = new MacroscopicLinkSegmentTypeModeProperties(
				mode, macroscopicModeProperties);
		linkSegmentType.setMacroscopicLinkSegmentTypeModeProperties(macroscopicLinkSegmentTypeModeProperties);
		existingLinks.put(externalId, linkSegmentType);
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