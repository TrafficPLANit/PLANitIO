package org.planit.xml.network.physical.macroscopic;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.userclass.Mode;

/**
 * Helper class used to create MacroscopicLinkSegmentType objects for XML input
 * 
 * @author gman6028
 *
 */
public class XmlMacroscopicLinkSegmentType {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(XmlMacroscopicLinkSegmentType.class.getName());

	/**
	 * External reference number of link type
	 */
	private int linkType;

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

	private static Map<Integer, XmlMacroscopicLinkSegmentType> existingLinks;

	/**
	 * Constructor
	 * 
	 * @param name                  name of the link type
	 * @param capacityPerLane       capacity per lane
	 * @param maximumDensityPerLane maximum density per lane
	 */
	public XmlMacroscopicLinkSegmentType(String name, double capacityPerLane, double maximumDensityPerLane,
			int linkType) {
		this.name = name;
		this.capacityPerLane = capacityPerLane;
		this.maximumDensityPerLane = maximumDensityPerLane;
		this.linkType = linkType;
		speedMap = new HashMap<Long, Double>();
	}

	/**
	 * Reset the store of existing links
	 */
	public static void reset() {
		existingLinks = new HashMap<Integer, XmlMacroscopicLinkSegmentType>();
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
	 * @param modeNo                number of the mode used in the XML input file
	 * @param modeMap               Map storing modes
	 * @param linkType              id number of the link type
	 * @return XmlMacroscopicLinkSegmentType object, created or updated to include
	 *         data from current row in the XML file
	 */
	public static XmlMacroscopicLinkSegmentType createOrUpdateLinkSegmentType(String name, double capacityPerLane,
			double maximumDensityPerLane, double speed,
			// int modeNo,
			long modeExternalId, Map<Integer, Mode> modeMap, int linkType) {
		XmlMacroscopicLinkSegmentType linkSegmentType;
		if (!existingLinks.containsKey(linkType)) {
			if (capacityPerLane == 0.0) {
				LOGGER.warning(
						"Link Type " + name + " initially defined without a capacity, being given a capacity of zero.");
			}
			linkSegmentType = new XmlMacroscopicLinkSegmentType(name, capacityPerLane, maximumDensityPerLane, linkType);
		} else {
			linkSegmentType = existingLinks.get(linkType);
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
		// if (modeNo == 0) {
		if (modeExternalId == 0) {
			modeMap.keySet().forEach(eachModeNo -> {
				updateLinkSegmentType(linkSegmentType, modeMap, eachModeNo, speed, linkType);
			});
		} else {
			// updateLinkSegmentType(linkSegmentType, modeMap, modeNo, speed, linkType);
			updateLinkSegmentType(linkSegmentType, modeMap, modeExternalId, speed, linkType);
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
	 * @param linkType        id number of the link type
	 */
	private static void updateLinkSegmentType(XmlMacroscopicLinkSegmentType linkSegmentType, Map<Integer, Mode> modeMap,
			long modeExternalId, double speed, int linkType) {
		if (speed < 0.0) {
			LOGGER.warning("A negative maximum speed has been defined for Link Type " + linkSegmentType.getName()
					+ " and Mode " + modeMap.get((int) modeExternalId).getName()
					+ ".  Setting the speed to zero instead (which means vehicles of this type are forbidden in links of this type.)");
			speed = 0.0;
		}
		linkSegmentType.getSpeedMap().put(modeExternalId, speed);
		existingLinks.put(linkType, linkSegmentType);
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

	public int getLinkType() {
		return linkType;
	}

}