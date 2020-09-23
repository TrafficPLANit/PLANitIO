package org.planit.io.xml.network;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import org.planit.utils.math.Precision;
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
      String name = generatedMode.getName();
      
      /* generate unique name if undefined */
      if(name==null) {
        name = PredefinedModeType.CUSTOM.value().concat(String.valueOf(physicalNetwork.modes.size()));
      }
      
      PredefinedModeType modeType = PredefinedModeType.create(name);      
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

        double maxSpeed = generatedMode.getMaxspeed()==null ? Mode.GLOBAL_DEFAULT_MAXIMUM_SPEED_KMH : generatedMode.getMaxspeed();
        double pcu = generatedMode.getPcu()==null ? Mode.GLOBAL_DEFAULT_PCU : generatedMode.getPcu();
        
        PhysicalModeFeatures physicalFeatures = parsePhysicalModeFeatures(generatedMode);
        UsabilityModeFeatures usabilityFeatures = parseUsabilityModeFeatures(generatedMode);        
                
        mode = physicalNetwork.modes.registerNewCustomMode(externalModeId, name, maxSpeed, pcu, physicalFeatures, usabilityFeatures);        
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
      final XMLElementLinkConfiguration linkconfiguration, InputBuilderListener inputBuilderListener) throws PlanItException {
    
    MacroscopicLinkSegmentTypeXmlHelper.reset();
    
    Map<Long, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentTypeXmlHelperMap = new HashMap<Long, MacroscopicLinkSegmentTypeXmlHelper>();
    for (XMLElementLinkSegmentTypes.Linksegmenttype linkSegmentTypeGenerated : linkconfiguration.getLinksegmenttypes().getLinksegmenttype()) {
      
      long linkSegmentTypeExternalId = linkSegmentTypeGenerated.getId().longValue();
      if (linkSegmentTypeXmlHelperMap.containsKey(linkSegmentTypeExternalId) && inputBuilderListener.isErrorIfDuplicateExternalId()) {
        String errorMessage = "Duplicate link segment type external id " + linkSegmentTypeExternalId + " found in network file.";
        throw new PlanItException(errorMessage);
      }
      
      String name = linkSegmentTypeGenerated.getName();
      double capacity = (linkSegmentTypeGenerated.getCapacitylane() == null) ? MacroscopicLinkSegmentType.DEFAULT_CAPACITY_LANE  : linkSegmentTypeGenerated.getCapacitylane();
      double maximumDensity = (linkSegmentTypeGenerated.getMaxdensitylane() == null) ? LinkSegment.MAXIMUM_DENSITY  : linkSegmentTypeGenerated.getMaxdensitylane();
      
      MacroscopicLinkSegmentTypeXmlHelper linkSegmentTypeXmlHelper = new MacroscopicLinkSegmentTypeXmlHelper(name,capacity, maximumDensity, linkSegmentTypeExternalId);
      linkSegmentTypeXmlHelperMap.put(linkSegmentTypeExternalId, linkSegmentTypeXmlHelper);      
      
      /* mode properties, only set when defined, otherwise not */
      Collection<Mode> thePlanitModes = new HashSet<Mode>();
      if(linkSegmentTypeGenerated.getModes() != null) {
        for (XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode xmlMode : linkSegmentTypeGenerated.getModes().getMode()) {        
          Object modeExternalId = xmlMode.getRef().longValue();

          Mode thePlanitMode = inputBuilderListener.getModeByExternalId(modeExternalId);
          PlanItException.throwIfNull(thePlanitMode, String.format("referenced mode (%d) does not exist in PLANit parser",modeExternalId));
          thePlanitModes.add(thePlanitMode);                                    
        }          
      }else {
        /* all modes allowed */
        thePlanitModes = inputBuilderListener.getAllModes();
      }
      
      /* populate the mode properties with either defaults, or actually defined values */
      for (Mode thePlanitMode : thePlanitModes) {
        XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode xmlMode = null;
        if(linkSegmentTypeGenerated.getModes() != null) {
          xmlMode = linkSegmentTypeGenerated.getModes().getMode().stream().dropWhile( currXmlMode -> ((long)thePlanitMode.getExternalId()) != currXmlMode.getRef().longValue()).findFirst().get();
        }
        
        double maxSpeed = thePlanitMode.getMaximumSpeed();
        double critSpeed = Math.min(maxSpeed, MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED); 
        if(xmlMode != null) {

          /** cap max speed of mode properties to global mode's default if it exceeds this maximum */ 
          maxSpeed = (xmlMode.getMaxspeed() == null) ? thePlanitMode.getMaximumSpeed() : xmlMode.getMaxspeed();
          if( Precision.isGreater(maxSpeed, thePlanitMode.getMaximumSpeed(), Precision.EPSILON_6)) {
            maxSpeed = thePlanitMode.getMaximumSpeed();
            LOGGER.warning(String.format("Capped maximum speed for mode %s on link segment type %d to mode's global maximum speed %.1f",
                thePlanitMode.getName(), linkSegmentTypeExternalId, maxSpeed));          
          }        
          
          critSpeed = (xmlMode.getCritspeed() == null) ? MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED  : xmlMode.getCritspeed();
          /* critical speed can never exceed max speed, so capp it if needed */
          critSpeed = Math.min(maxSpeed, critSpeed);
        }
        
        linkSegmentTypeXmlHelper.updateLinkSegmentTypeModeProperties(linkSegmentTypeExternalId, thePlanitMode, maxSpeed, critSpeed);
      }
    }
    return linkSegmentTypeXmlHelperMap;
  }

}