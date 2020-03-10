package org.planit.planitio.xml.network;

import java.util.HashMap;
import java.util.Map;

import org.planit.exceptions.PlanItException;
import org.planit.generated.XMLElementLinkConfiguration;
import org.planit.generated.XMLElementLinkSegmentTypes;
import org.planit.generated.XMLElementModes;
import org.planit.input.InputBuilderListener;
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
   * 
   * @param physicalNetwork
   * @param linkconfiguration LinkConfiguration object populated with data from XML file
   * @param inputBuilderListeners parser which holds the Map of nodes by external Id
   * @throws PlanItException thrown if there is a Mode value of 0 in the modes definition file
   */
  public static void createAndRegisterModes(PhysicalNetwork physicalNetwork, XMLElementLinkConfiguration linkconfiguration, InputBuilderListener inputBuilderListener) throws PlanItException {
    for (XMLElementModes.Mode generatedMode : linkconfiguration.getModes().getMode()) {
      long externalModeId = generatedMode.getId().longValue();
      if (externalModeId == 0) {
        throw new PlanItException("Found a Mode value of 0 in the modes definition file, this is prohibited");
      }
      String name = generatedMode.getName();
      double pcu = generatedMode.getPcu();
      Mode mode = physicalNetwork.modes.registerNewMode(externalModeId, name, pcu);
      final boolean duplicateModeExternalId = inputBuilderListener.addModeToExternalIdMap(mode.getExternalId(), mode);
      if (duplicateModeExternalId && inputBuilderListener.isErrorIfDuplicateExternalId()) {
        throw new PlanItException("Duplicate mode external id " + mode.getExternalId() + " found in network file.");
      }
    }
  }

  /**
   * Reads MacroscopicLinkSegmentTypeXmlHelper objects from input file and stores them in a Map
   * 
   * @param linkconfiguration LinkConfiguration object populated with data from XML file
   * @param inputBuilderListener parser which holds the Map of nodes by external Id
   * @return Map containing link type values
   * @throws PlanItException thrown if there is an error reading the input file
   */
  public static Map<Long, MacroscopicLinkSegmentTypeXmlHelper> createLinkSegmentTypeHelperMap(
      final XMLElementLinkConfiguration linkconfiguration, 
      InputBuilderListener inputBuilderListener) throws PlanItException {
    MacroscopicLinkSegmentTypeXmlHelper.reset();
    Map<Long, MacroscopicLinkSegmentTypeXmlHelper> macroscopicLinkSegmentTypeXmlHelperMap =
        new HashMap<Long, MacroscopicLinkSegmentTypeXmlHelper>();
    for (XMLElementLinkSegmentTypes.Linksegmenttype linkSegmentTypeGenerated : linkconfiguration.getLinksegmenttypes()
        .getLinksegmenttype()) {
      long externalId = linkSegmentTypeGenerated.getId().longValue();
      if (macroscopicLinkSegmentTypeXmlHelperMap.containsKey(externalId) && inputBuilderListener.isErrorIfDuplicateExternalId()) {
        throw new PlanItException("Duplicate link segment type external id " + externalId + " found in network file.");
      }
      String name = linkSegmentTypeGenerated.getName();
      double capacity = (linkSegmentTypeGenerated.getCapacitylane() == null) ? MacroscopicLinkSegmentType.DEFAULT_CAPACITY_LANE  : linkSegmentTypeGenerated.getCapacitylane();
      double maximumDensity = (linkSegmentTypeGenerated.getMaxdensitylane() == null) ? MacroscopicLinkSegmentType.DEFAULT_MAXIMUM_DENSITY_LANE  : linkSegmentTypeGenerated.getMaxdensitylane();
      for (XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode mode : linkSegmentTypeGenerated.getModes().getMode()) {
        int modeExternalId = mode.getRef().intValue();
        double maxSpeed = (mode.getMaxspeed() == null) ? MacroscopicModeProperties.DEFAULT_MAXIMUM_SPEED : mode.getMaxspeed();
        double critSpeed = (mode.getCritspeed() == null) ? MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED  : mode.getCritspeed();
        MacroscopicLinkSegmentTypeXmlHelper macroscopicLinkSegmentTypeXmlHelper = MacroscopicLinkSegmentTypeXmlHelper
            .createOrUpdateLinkSegmentTypeHelper(name, capacity, maximumDensity, maxSpeed, critSpeed, modeExternalId,
                externalId, inputBuilderListener);
                
        macroscopicLinkSegmentTypeXmlHelperMap.put(externalId, macroscopicLinkSegmentTypeXmlHelper);
      }
    }
    return macroscopicLinkSegmentTypeXmlHelperMap;
  }

}