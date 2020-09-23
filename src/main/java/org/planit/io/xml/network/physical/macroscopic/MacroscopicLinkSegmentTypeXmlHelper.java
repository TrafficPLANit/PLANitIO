package org.planit.io.xml.network.physical.macroscopic;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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
   * Map of existing link types
   */
  private static Map<Object, MacroscopicLinkSegmentTypeXmlHelper> existingLinkTypeHelpers;  

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
   * Link Segment Mode Properties (maximum speed and critical speed) for each mode
   */
  private Map<Mode, MacroscopicModeProperties> macroscopicLinkSegmentTypeModeProperties;
  

  /**
   * Update an existing MacroscopicLinkSegmentTypeXmlHelper
   * 
   * @param linkTypeExternalId id number of the link type
   * @param mode mode used in the XML input file
   * @param maxSpeed maximum speed
   * @param critSpeed critical speed
   */
  public void updateLinkSegmentTypeModeProperties(
      Object linkTypeExternalId,
      Mode mode, 
      double maxSpeed, 
      double critSpeed) {
    
    if (maxSpeed < 0.0) {
      LOGGER.warning("a negative maximum speed has been defined for Link Type "
          + getName() + " and Mode " + mode.getName()
          + ".  Setting the speed to zero instead (which means vehicles of this type are forbidden in links of this type.)");
      maxSpeed = 0.0;
    }
    MacroscopicModeProperties macroscopicModeProperties = new MacroscopicModePropertiesImpl(maxSpeed, critSpeed);
    macroscopicLinkSegmentTypeModeProperties.put(mode, macroscopicModeProperties);
  }

  /**
   * Reset the store of existing links
   */
  public static void reset() {
    existingLinkTypeHelpers = new HashMap<Object, MacroscopicLinkSegmentTypeXmlHelper>();
  }

  /**
   * Constructor
   * 
   * @param name name of the link type
   * @param capacityPerLane capacity per lane
   * @param maximumDensityPerLane maximum density per lane
   * @param externalId external Id of the link segment type
   */
  public MacroscopicLinkSegmentTypeXmlHelper(
      String name, double capacityPerLane, double maximumDensityPerLane, Object externalId) {
    
    this.name = name;
    if (capacityPerLane == 0.0) {
      LOGGER.warning("link Type " + name + " initially defined without a capacity, being given a capacity of zero.");
    }
    
    this.capacityPerLane = capacityPerLane;
    this.maximumDensityPerLane = maximumDensityPerLane;
    this.externalId = externalId;
    macroscopicLinkSegmentTypeModeProperties = new HashMap<Mode, MacroscopicModeProperties>();        
    
    /* check for duplication, log issues */
    if (existingLinkTypeHelpers.containsKey(externalId)) {
      MacroscopicLinkSegmentTypeXmlHelper other = existingLinkTypeHelpers.get(externalId);
      if (capacityPerLane != other.getCapacityPerLane()) {
        LOGGER.warning("different capacity per lane values for Link Type " + other
            .getName() + ".  Will use the highest one.");
      }
      if (capacityPerLane > other.getCapacityPerLane()) {
        this.setCapacityPerLane(capacityPerLane);
      }
      if (maximumDensityPerLane != other.getMaximumDensityPerLane()) {
        LOGGER.warning("different maximum density per lane values for link type "
            + other.getName() + ".  Will use the highest one.");
      }
      if (maximumDensityPerLane > other.getMaximumDensityPerLane()) {
        this.setMaximumDensityPerLane(maximumDensityPerLane);
      }
    }  
    
    existingLinkTypeHelpers.put(externalId, this);    
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

  public Object getExternalId() {
    return externalId;
  }

  public Map<Mode, MacroscopicModeProperties> getModePropertiesMap() {
    return macroscopicLinkSegmentTypeModeProperties;
  }

}
