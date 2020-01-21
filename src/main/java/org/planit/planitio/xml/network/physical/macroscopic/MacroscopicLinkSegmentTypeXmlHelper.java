package org.planit.planitio.xml.network.physical.macroscopic;

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
	private Map<Mode, Double> speedMap;

	/**
	 * Link Segment Mode Properties (maximum speed and critical speed)
	 */
	private MacroscopicLinkSegmentTypeModeProperties macroscopicLinkSegmentTypeModeProperties;

	/**
	 * Map of existing links
	 */
	private static Map<Integer, MacroscopicLinkSegmentTypeXmlHelper> existingLinks;

	/**
	 * Update an existing XmlMacroscopicLinkSegmentType linkSegmentType
	 * 
	 * @param linkSegmentType MacroscopicLinkSegmentTypeXmlHelper object
	 *                        to be updated
	 * @param modeExternalId  external Id of the mode used in the XML input file
	 * @param  maxSpeed           maximum speed
	 * @param critSpeed       critical speed
	 * @param modeExternalId  reference to the mode for the current link segment
	 *                        type
	 * @param externalId      id number of the link type
	 */
	private static void updateLinkSegmentType(MacroscopicLinkSegmentTypeXmlHelper macroscopicLinkSegmentTypeXmlHelper,
			long modeExternalId, double maxSpeed, double critSpeed, int externalId) {
		if ( maxSpeed < 0.0) {
			PlanItLogger.warning("A negative maximum speed has been defined for Link Type " + macroscopicLinkSegmentTypeXmlHelper.getName()
			        + " and Mode " + Mode.getByExternalId(modeExternalId).getName()
					+ ".  Setting the speed to zero instead (which means vehicles of this type are forbidden in links of this type.)");
			 maxSpeed = 0.0;
		}
		MacroscopicModeProperties macroscopicModeProperties = new MacroscopicModeProperties(maxSpeed, critSpeed);
		Mode mode = Mode.getByExternalId(modeExternalId);
		macroscopicLinkSegmentTypeXmlHelper.getSpeedMap().put(mode,  maxSpeed);
		MacroscopicLinkSegmentTypeModeProperties macroscopicLinkSegmentTypeModeProperties = new MacroscopicLinkSegmentTypeModeProperties(
				mode, macroscopicModeProperties);
		macroscopicLinkSegmentTypeXmlHelper.setMacroscopicLinkSegmentTypeModeProperties(macroscopicLinkSegmentTypeModeProperties);
		existingLinks.put(externalId, macroscopicLinkSegmentTypeXmlHelper);
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
		speedMap = new HashMap<Mode, Double>();
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
	 * @param externalId            id number of the link type
	 * @return MacroscopicLinkSegmentTypeXmlHelper object, created or updated to include
	 *         data from current row in the XML file
	 */
	public static MacroscopicLinkSegmentTypeXmlHelper createOrUpdateLinkSegmentType(String name, double capacityPerLane,
			double maximumDensityPerLane, double maxSpeed, double critSpeed, long modeExternalId, int externalId) {
		MacroscopicLinkSegmentTypeXmlHelper macroscopicLinkSegmentTypeXmlHelper;
		if (!existingLinks.containsKey(externalId)) {
			if (capacityPerLane == 0.0) {
				PlanItLogger.warning(
						"Link Type " + name + " initially defined without a capacity, being given a capacity of zero.");
			}
			macroscopicLinkSegmentTypeXmlHelper = new MacroscopicLinkSegmentTypeXmlHelper(name, capacityPerLane, maximumDensityPerLane,
					externalId);
		} else {
			macroscopicLinkSegmentTypeXmlHelper = existingLinks.get(externalId);
			if (capacityPerLane != macroscopicLinkSegmentTypeXmlHelper.getCapacityPerLane()) {
				PlanItLogger.warning("Different capacity per lane values for Link Type " + macroscopicLinkSegmentTypeXmlHelper.getName()
						+ ".  Will use the highest one.");
			}
			if (capacityPerLane > macroscopicLinkSegmentTypeXmlHelper.getCapacityPerLane()) {
				macroscopicLinkSegmentTypeXmlHelper.setCapacityPerLane(capacityPerLane);
			}
			if (maximumDensityPerLane != macroscopicLinkSegmentTypeXmlHelper.getMaximumDensityPerLane()) {
				PlanItLogger.warning("Different maximum density per lane values for link type " + macroscopicLinkSegmentTypeXmlHelper.getName()
						+ ".  Will use the highest one.");
			}
			if (maximumDensityPerLane > macroscopicLinkSegmentTypeXmlHelper.getMaximumDensityPerLane()) {
				macroscopicLinkSegmentTypeXmlHelper.setMaximumDensityPerLane(maximumDensityPerLane);
			}
		}
		if (modeExternalId == 0) {
			Mode.getExternalIdSet().forEach(eachModeNo -> {
				updateLinkSegmentType(macroscopicLinkSegmentTypeXmlHelper, eachModeNo, maxSpeed, critSpeed, externalId);
			});
		} else {
			updateLinkSegmentType(macroscopicLinkSegmentTypeXmlHelper, modeExternalId, maxSpeed, critSpeed, externalId);
		}
		return macroscopicLinkSegmentTypeXmlHelper;
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

	public Map<Mode, Double> getSpeedMap() {
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