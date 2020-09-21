package org.planit.io.xml.network;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.xml.generated.XMLElementLinkConfiguration;
import org.planit.xml.generated.XMLElementLinkSegmentTypes;
import org.planit.xml.generated.XMLElementModes;
import org.planit.input.InputBuilderListener;
import org.planit.io.xml.network.physical.macroscopic.MacroscopicLinkSegmentTypeXmlHelper;
import org.planit.mode.ModeFeaturesFactory;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.MotorisationModeType;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.PredefinedModeType;
import org.planit.utils.mode.TrackModeType;
import org.planit.utils.mode.UsabilityModeFeatures;
import org.planit.utils.mode.UseOfModeType;
import org.planit.utils.mode.VehicularModeType;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Process the LinkConfiguration object populated with data from the XML file
 * 
 * @author gman6028, markr
 *
 */
public class ProcessLinkConfiguration {

  /** the logger */  
  private static final Logger LOGGER = Logger.getLogger(ProcessLinkConfiguration.class.getCanonicalName());
  
  /** parse the usability component of the mode xml element. It is assumed they should be present, if not default values are created
   * @param generatedMode mode to extract information from
   * @return usabilityFeatures that are parsed
   * @throws PlanItException 
   */
  private static UsabilityModeFeatures parseUsabilityModeFeatures(org.planit.xml.generated.XMLElementModes.Mode generatedMode) throws PlanItException {
    if(generatedMode.getUsabilityfeatures() == null) {
      return ModeFeaturesFactory.createDefaultUsabilityFeatures();
    }
    
    /* parse set values */
    UseOfModeType useOfModeType = null; 
    switch (generatedMode.getUsabilityfeatures().getUsedtotype()) {
    case PRIVATE:
      useOfModeType = UseOfModeType.PRIVATE;
      break;
    case PUBLIC:
      useOfModeType = UseOfModeType.PUBLIC;
      break;
    case HIGH_OCCUPANCY:
      useOfModeType = UseOfModeType.HIGH_OCCUPANCY;
      break;
    case RIDE_SHARE:
      useOfModeType = UseOfModeType.RIDE_SHARE;
      break;
    case GOODS:
      useOfModeType = UseOfModeType.GOODS;
      break;
    default:
      throw new PlanItException(String.format("invalid usability type for mode %s defined; %s",generatedMode.getName(),generatedMode.getUsabilityfeatures().getUsedtotype().toString()));
    }
    
    return ModeFeaturesFactory.createUsabilityFeatures(useOfModeType);
  }

  /** parse the physical features component of the mode xml element. It is assumed they should be present, if not default values are created
   * @param generatedMode mode to extract information from
   * @return physicalFeatures that are parsed
   * @throws PlanItException 
   */  
  private static PhysicalModeFeatures parsePhysicalModeFeatures(org.planit.xml.generated.XMLElementModes.Mode generatedMode) throws PlanItException {
    if(generatedMode.getPhysicalfeatures() == null) {
      return ModeFeaturesFactory.createDefaultPhysicalFeatures();
    }
    
    /* parse set values */
    VehicularModeType vehicleType = null; 
    switch (generatedMode.getPhysicalfeatures().getVehicletype()) {
    case VEHICLE:
      vehicleType = VehicularModeType.VEHICLE;
      break;
    case NO_VEHICLE:
      vehicleType = VehicularModeType.NO_VEHICLE;
      break;
    default:
      throw new PlanItException(String.format("invalid vehicular mode type for mode %s defined; %s",generatedMode.getName(),generatedMode.getPhysicalfeatures().getVehicletype().toString()));
    }
    
    MotorisationModeType motorisationType = null; 
    switch (generatedMode.getPhysicalfeatures().getMotorisationtype()) {
    case MOTORISED:
      motorisationType = MotorisationModeType.MOTORISED;
      break;
    case NON_MOTORISED:
      motorisationType = MotorisationModeType.NON_MOTORISED;
      break;
    default:
      throw new PlanItException(String.format("invalid motorisation type for mode %s defined; %s",generatedMode.getName(),generatedMode.getPhysicalfeatures().getMotorisationtype().toString()));
    }    
    
    TrackModeType trackType = null; 
    switch (generatedMode.getPhysicalfeatures().getTracktype()) {
    case DOUBLE:
      trackType = TrackModeType.DOUBLE;
      break;
    case SINGLE:
      trackType = TrackModeType.SINGLE;
      break;
    default:
      throw new PlanItException(String.format("invalid track type for mode %s defined; %s",generatedMode.getName(),generatedMode.getPhysicalfeatures().getTracktype().toString()));
    }     
    
    return ModeFeaturesFactory.createPhysicalFeatures(vehicleType, motorisationType, trackType);
  }  

  /**
   * Reads mode types from input file and stores them in a Map
   * 
   * @param physicalNetwork the network
   * @param linkconfiguration LinkConfiguration object populated with data from XML file
   * @param inputBuilderListener parser which holds the Map of nodes by external Id
   * @throws PlanItException thrown if there is a Mode value of 0 in the modes definition file
   */
  public static void createAndRegisterModes(PhysicalNetwork<?,?,?> physicalNetwork, XMLElementLinkConfiguration linkconfiguration, InputBuilderListener inputBuilderListener) throws PlanItException {
    for (XMLElementModes.Mode generatedMode : linkconfiguration.getModes().getMode()) {
      PredefinedModeType modeType = PredefinedModeType.create(generatedMode.getName()); 
      if(!generatedMode.isPredefined() && modeType != PredefinedModeType.CUSTOM) {
        LOGGER.warning(String.format("mode %s is not registered as predefined mode but name corresponds to PLANit predefined mode, reverting to PLANit predefined mode",generatedMode.getName()));
      }
      
      Mode mode = null;
      if(modeType != PredefinedModeType.CUSTOM) {
        /* predefined mode use factory, ignore other attributes (if any) */
        mode = physicalNetwork.modes.registerNew(modeType);
      }else {
        /* custom mode, parse all components to correctly configure the custom mode */
        long externalModeId = generatedMode.getId().longValue();
        if (externalModeId == 0) {
          String errorMessage = "found a Mode value of 0 in the modes definition file, this is prohibited";
          throw new PlanItException(errorMessage);
        }
        String name = generatedMode.getName();
        double pcu = generatedMode.getPcu();
        
        PhysicalModeFeatures physicalFeatures = parsePhysicalModeFeatures(generatedMode);
        UsabilityModeFeatures usabilityFeatures = parseUsabilityModeFeatures(generatedMode);        
                
        mode = physicalNetwork.modes.registerNewCustomMode(externalModeId, name, pcu, physicalFeatures, usabilityFeatures);        
      }
      
      final boolean duplicateModeExternalId = inputBuilderListener.addModeToExternalIdMap(mode.getExternalId(), mode);
      if (duplicateModeExternalId && inputBuilderListener.isErrorIfDuplicateExternalId()) {
        String errorMessage = "duplicate mode external id " + mode.getExternalId() + " found in network file.";
        throw new PlanItException(errorMessage);
      }
    }
  }

  /**
   * Reads MacroscopicLinkSegmentTypeXmlHelper objects from input file and stores them in a Map
   * 
   * @param linkconfiguration LinkConfiguration object populated with data from XML file
   * @param inputBuilderListener parser which holds the Map of nodes by external Id
   * @return Map containing link type values identified by their external Ids
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
        String errorMessage = "Duplicate link segment type external id " + externalId + " found in network file.";
        throw new PlanItException(errorMessage);
      }
      String name = linkSegmentTypeGenerated.getName();
      double capacity = (linkSegmentTypeGenerated.getCapacitylane() == null) ? MacroscopicLinkSegmentType.DEFAULT_CAPACITY_LANE  : linkSegmentTypeGenerated.getCapacitylane();
      double maximumDensity = (linkSegmentTypeGenerated.getMaxdensitylane() == null) ? LinkSegment.MAXIMUM_DENSITY  : linkSegmentTypeGenerated.getMaxdensitylane();      
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