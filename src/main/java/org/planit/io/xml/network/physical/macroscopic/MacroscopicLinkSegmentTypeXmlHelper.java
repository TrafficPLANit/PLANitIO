package org.planit.io.xml.network.physical.macroscopic;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.input.InputBuilderListener;
import org.planit.network.physical.macroscopic.MacroscopicModePropertiesImpl;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Helper class used to create MacroscopicLinkSegmentType objects for XML input
 * 
 * @author gman6028
 *
 */
public class MacroscopicLinkSegmentTypeXmlHelper {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicLinkSegmentTypeXmlHelper.class.getCanonicalName());

  /**
   * External reference number of link type
   */
  private Object externalId;

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
   * Link Segment Mode Properties (maximum speed and critical speed) for each mode
   */
  private Map<Mode, MacroscopicModeProperties> macroscopicLinkSegmentTypeModeProperties;

  /**
   * Map of existing link types
   */
  private static Map<Object, MacroscopicLinkSegmentTypeXmlHelper> existingLinkTypeHelpers;

  /**
   * Update an existing MacroscopicLinkSegmentTypeXmlHelper
   * 
   * @param macroscopicLinkSegmentTypeXmlHelper MacroscopicLinkSegmentTypeXmlHelper object
   *          to be updated
   * @param mode mode used in the XML input file
   * @param maxSpeed maximum speed
   * @param critSpeed critical speed
   * @param linkTypeExternalId id number of the link type
   */
  private static void updateLinkSegmentTypeHelper(
      MacroscopicLinkSegmentTypeXmlHelper macroscopicLinkSegmentTypeXmlHelper,
      Mode mode, double maxSpeed, double critSpeed, Object linkTypeExternalId) {
    if (maxSpeed < 0.0) {
      LOGGER.warning("a negative maximum speed has been defined for Link Type "
          + macroscopicLinkSegmentTypeXmlHelper.getName() + " and Mode " + mode.getName()
          + ".  Setting the speed to zero instead (which means vehicles of this type are forbidden in links of this type.)");
      maxSpeed = 0.0;
    }
    macroscopicLinkSegmentTypeXmlHelper.getSpeedMap().put(mode, maxSpeed);
    MacroscopicModeProperties macroscopicModeProperties = new MacroscopicModePropertiesImpl(maxSpeed, critSpeed);
    macroscopicLinkSegmentTypeXmlHelper.addMacroscopicModeProperties(mode, macroscopicModeProperties);
    existingLinkTypeHelpers.put(linkTypeExternalId, macroscopicLinkSegmentTypeXmlHelper);
  }

  /**
   * Reset the store of existing links
   */
  public static void reset() {
    existingLinkTypeHelpers = new HashMap<Object, MacroscopicLinkSegmentTypeXmlHelper>();
  }

  /**
   * Create or update an MacroscopicLinkSegmentTypeXmlHelper object using the input from
   * the current row in the XML input file
   * 
   * If the mode number is 0, all modes are updated
   * 
   * @param name link type name
   * @param capacityPerLane capacity per lane
   * @param maximumDensityPerLane maximum density per lane
   * @param maxSpeed maximum speed
   * @param critSpeed critical speed
   * @param modeExternalId reference to the mode used in the XML input file
   * @param linkTypeExternalId id number of the link type
   * @param inputBuilderListener parser containing Map of modes by external Id
   * @return MacroscopicLinkSegmentTypeXmlHelper object, created or updated to include
   *         data from current row in the XML file
   */
  public static MacroscopicLinkSegmentTypeXmlHelper createOrUpdateLinkSegmentTypeHelper(String name,
      double capacityPerLane,
      double maximumDensityPerLane, 
      double maxSpeed, 
      double critSpeed, 
      long modeExternalId, 
      Object linkTypeExternalId,
      InputBuilderListener inputBuilderListener) {
    MacroscopicLinkSegmentTypeXmlHelper macroscopicLinkSegmentTypeXmlHelper;
    if (!existingLinkTypeHelpers.containsKey(linkTypeExternalId)) {
      if (capacityPerLane == 0.0) {
        LOGGER.warning("link Type " + name + " initially defined without a capacity, being given a capacity of zero.");
      }
      macroscopicLinkSegmentTypeXmlHelper =
          new MacroscopicLinkSegmentTypeXmlHelper(name, capacityPerLane, maximumDensityPerLane, linkTypeExternalId);
    } else {
      macroscopicLinkSegmentTypeXmlHelper = existingLinkTypeHelpers.get(linkTypeExternalId);
      if (capacityPerLane != macroscopicLinkSegmentTypeXmlHelper.getCapacityPerLane()) {
        LOGGER.warning("different capacity per lane values for Link Type " + macroscopicLinkSegmentTypeXmlHelper
            .getName() + ".  Will use the highest one.");
      }
      if (capacityPerLane > macroscopicLinkSegmentTypeXmlHelper.getCapacityPerLane()) {
        macroscopicLinkSegmentTypeXmlHelper.setCapacityPerLane(capacityPerLane);
      }
      if (maximumDensityPerLane != macroscopicLinkSegmentTypeXmlHelper.getMaximumDensityPerLane()) {
        LOGGER.warning("different maximum density per lane values for link type "
            + macroscopicLinkSegmentTypeXmlHelper.getName() + ".  Will use the highest one.");
      }
      if (maximumDensityPerLane > macroscopicLinkSegmentTypeXmlHelper.getMaximumDensityPerLane()) {
        macroscopicLinkSegmentTypeXmlHelper.setMaximumDensityPerLane(maximumDensityPerLane);
      }
    }
    if (modeExternalId == 0) {
      inputBuilderListener.getAllModes().forEach(eachMode -> {
        updateLinkSegmentTypeHelper(macroscopicLinkSegmentTypeXmlHelper, eachMode, maxSpeed, critSpeed,
            linkTypeExternalId);
      });
    } else {
      Mode mode = inputBuilderListener.getModeByExternalId(modeExternalId);
      updateLinkSegmentTypeHelper(macroscopicLinkSegmentTypeXmlHelper, mode, maxSpeed, critSpeed, linkTypeExternalId);
    }
    return macroscopicLinkSegmentTypeXmlHelper;
  }

  /**
   * Constructor
   * 
   * @param name name of the link type
   * @param capacityPerLane capacity per lane
   * @param maximumDensityPerLane maximum density per lane
   * @param externalId external Id of the link segment type
   */
  public MacroscopicLinkSegmentTypeXmlHelper(String name, double capacityPerLane, double maximumDensityPerLane,
      Object externalId) {
    this.name = name;
    this.capacityPerLane = capacityPerLane;
    this.maximumDensityPerLane = maximumDensityPerLane;
    this.externalId = externalId;
    speedMap = new HashMap<Mode, Double>();
    macroscopicLinkSegmentTypeModeProperties = new HashMap<Mode, MacroscopicModeProperties>();
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

  public Object getExternalId() {
    return externalId;
  }

  public Map<Mode, MacroscopicModeProperties> getModePropertiesMap() {
    return macroscopicLinkSegmentTypeModeProperties;
  }

  public void addMacroscopicModeProperties(Mode mode, MacroscopicModeProperties macroscopicModeProperties) {
    macroscopicLinkSegmentTypeModeProperties.put(mode, macroscopicModeProperties);
  }

}
