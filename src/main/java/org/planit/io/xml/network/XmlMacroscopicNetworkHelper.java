package org.planit.io.xml.network;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.planit.xml.generated.*;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import net.opengis.gml.LineStringType;
import net.opengis.gml.PointType;

import org.planit.geo.PlanitJtsUtils;
import org.planit.input.InputBuilderListener;
import org.planit.io.xml.network.physical.macroscopic.MacroscopicLinkSegmentTypeXmlHelper;
import org.planit.mode.ModeFeaturesFactory;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
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
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;

/**
 * Process the Infrastructure object populated with data from the XML file
 * 
 * @author gman6028
 *
 */
public class XmlMacroscopicNetworkHelper {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(XmlMacroscopicNetworkHelper.class.getCanonicalName());

  /** geoUtils to use */
  private PlanitJtsUtils geoUtils;
  
  /** network to populate */
  private MacroscopicNetwork network;
  
  /** parse the usability component of the mode xml element. It is assumed they should be present, if not default values are created
   * @param generatedMode mode to extract information from
   * @return usabilityFeatures that are parsed
   * @throws PlanItException 
   */
  private UsabilityModeFeatures parseUsabilityModeFeatures(org.planit.xml.generated.XMLElementModes.Mode generatedMode) throws PlanItException {
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
  private PhysicalModeFeatures parsePhysicalModeFeatures(org.planit.xml.generated.XMLElementModes.Mode generatedMode) throws PlanItException {
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
    case ROAD:
      trackType = TrackModeType.ROAD;
      break;
    case RAIL:
      trackType = TrackModeType.RAIL;
      break;
    default:
      throw new PlanItException(String.format("invalid track type for mode %s defined; %s",generatedMode.getName(),generatedMode.getPhysicalfeatures().getTracktype().toString()));
    }     
    
    return ModeFeaturesFactory.createPhysicalFeatures(vehicleType, motorisationType, trackType);
  }    
  
  /**
   * Get the link length from the length element in the XML file, if this has
   * been set
   * 
   * @param initLength initial length value
   * @param generatedLink object storing link data from XML file
   * @return final length value
   */
  private double getLengthFromLength(double initLength, XMLElementLinks.Link generatedLink) {
    XMLElementLinkLengthType linkLengthType = generatedLink.getLength();
    if (linkLengthType != null) {
      double length = linkLengthType.getValue();
      LengthUnit lengthUnit = linkLengthType.getUnit();
      if ((lengthUnit != null) && (lengthUnit.equals(LengthUnit.M))) {
        length /= 1000.0;
      }
      return length;
    }
    return initLength;
  }

  /**
   * Get the link length from the gml:LineString element in the XML file, if
   * this has been set
   * 
   * @param initLength initial length value
   * @param generatedLink object storing link data from XML file
   * @return final length value
   * @throws PlanItException
   */
  // TODO - Create some test cases for this, currently no test cases exist for it
  private double getLengthFromLineString(double initLength, XMLElementLinks.Link generatedLink)
      throws PlanItException {
    LineStringType lineStringType = generatedLink.getLineString();
    if (lineStringType != null) {
      List<Double> posList = lineStringType.getPosList().getValue();
      double distance = 0.0;
      Point startPosition = null;
      Point endPosition = null;
      for (int i = 0; i < posList.size(); i += 2) {
        endPosition = geoUtils.createPoint(posList.get(i), posList.get(i + 1));
        if (startPosition != null) {
          distance += geoUtils.getDistanceInKilometres(startPosition, endPosition);
        }
        startPosition = endPosition;
      }
      return distance;
    }
    return initLength;
  }

  /**
   * Registers a new link segment in the physical network
   * 
   * @param maxSpeed the value of the {@code <maxspeed>} element within the {@code <linksegment>} element in the
   *          input file, null if this element omitted for this link segment
   * @param network the physical network object
   * @param link the link from which the link segment will be created
   * @param abDirection direction of travel
   * @param linkSegmentTypeHelper object storing the input values for this link
   * @param noLanes the number of lanes in this link
   * @param externalId the external Id of this link segment
   * @param inputBuilderListener parser which holds the Map of nodes by external Id
   * @throws PlanItException thrown if there is an error
   */
  private void createAndRegisterLinkSegment(Float maxSpeed, MacroscopicNetwork network, Link link,
      boolean abDirection,
      MacroscopicLinkSegmentTypeXmlHelper linkSegmentTypeHelper,
      int noLanes, long externalId,
      InputBuilderListener inputBuilderListener) throws PlanItException {
    
    // create the link and store it in the network object
    MacroscopicLinkSegment linkSegment = network.linkSegments.registerNew(link, abDirection, true /* register on nodes and link*/);

    double maxSpeedDouble = maxSpeed == null ? Double.POSITIVE_INFINITY : (double) maxSpeed;        
    linkSegment.setPhysicalSpeedLimitKmH(maxSpeedDouble);    
    linkSegment.setNumberOfLanes(noLanes);
    linkSegment.setExternalId(externalId);    
    
    Map<Mode, MacroscopicModeProperties> modeProperties = linkSegmentTypeHelper.getModePropertiesMap();    
    MacroscopicLinkSegmentType existingLinkSegmentType = inputBuilderListener.getLinkSegmentTypeByExternalId(linkSegmentTypeHelper.getExternalId());
    if (existingLinkSegmentType == null) {
      existingLinkSegmentType = 
          network.createAndRegisterNewMacroscopicLinkSegmentType(
              linkSegmentTypeHelper.getName(), 
              linkSegmentTypeHelper.getCapacityPerLane(),
              linkSegmentTypeHelper.getMaximumDensityPerLane(), 
              linkSegmentTypeHelper.getExternalId(), 
              modeProperties);
            
      inputBuilderListener.addLinkSegmentTypeToExternalIdMap(existingLinkSegmentType.getExternalId(), existingLinkSegmentType);
    }
    linkSegment.setLinkSegmentType(existingLinkSegmentType);    

    if (linkSegment.getExternalId() != null) {
      final boolean duplicateLinkSegmentExternalId = 
          inputBuilderListener.addLinkSegmentToExternalIdMap(linkSegment.getExternalId(), linkSegment);
      PlanItException.throwIf(duplicateLinkSegmentExternalId && inputBuilderListener.isErrorIfDuplicateExternalId(), 
          "Duplicate link segment external id " + linkSegment.getExternalId() + " found in network file");
    }
  }  
  
  /**
   * parse the geometry from the xml link
   * 
   * @param generatedLink xml link
   * @return created LineString if any, null if not present
   * @throws PlanItException thrown if error
   */
  private LineString parseLinkGeometry(org.planit.xml.generated.XMLElementLinks.Link generatedLink) throws PlanItException {
    /* geometry of link */
    if(generatedLink.getLineString()!=null) {
      LineStringType lst = generatedLink.getLineString();
      if(lst.getCoordinates() != null) {
        return geoUtils.createLineStringFromCsvString(lst.getCoordinates().getValue(), lst.getCoordinates().getTs(), lst.getCoordinates().getCs());
      }else if(lst.getPosList()!=null) {
        return geoUtils.createLineString(lst.getPosList().getValue());
      }
    }
    return null;    
  }  
  
  // PUBLIC

 
  /** Constructor
   * @param network to populate
   * @param geoUtils to use
   */
  public XmlMacroscopicNetworkHelper(MacroscopicNetwork network, PlanitJtsUtils geoUtils) {
    this.network = network;
    this.geoUtils = geoUtils;
  }


  /**
   * Create and register nodes on the network
   * 
   * @param infrastructure Infrastructure object populated with data from XML file
   * @param network network the physical network object to be populated from the input data
   * @param inputBuilderListener parser which holds the Map of nodes by external Id
   * @throws PlanItException thrown if there is an error in storing the GML Point definition
   */
  public void createAndRegisterNodes(XMLElementInfrastructure infrastructure, InputBuilderListener inputBuilderListener) throws PlanItException {
    for (XMLElementNodes.Node generatedNode : infrastructure.getNodes().getNode()) {

      Node node = network.nodes.registerNew();
      node.setExternalId(generatedNode.getId().longValue());
      PointType pointType = generatedNode.getPoint();
      if (pointType != null) {
        List<Double> posValues = pointType.getPos().getValue();
        Point centrePointGeometry = geoUtils.createPoint(posValues.get(0), posValues.get(1));
        node.setPosition(centrePointGeometry);
      }
      boolean duplicateNodeExternalId = 
          inputBuilderListener.addNodeToExternalIdMap(generatedNode.getId().longValue(),node);
      PlanItException.throwIf(duplicateNodeExternalId && inputBuilderListener.isErrorIfDuplicateExternalId(),
          "Duplicate node external id " + generatedNode.getId().longValue() + " found in network file");
    }
  }

  /**
   * Generated and register link segments
   * 
   * @param infrastructure Infrastructure object populated with data from XML file
   * @param network network the physical network object to be populated from the input data
   * @param linkSegmentTypeHelperMap Map of MacroscopicLinkSegmentTypeXmlHelper objects
   * @param inputBuilderListener parser which holds the Map of nodes by external Id
   * @throws PlanItException thrown if there is an error during processing or reference to link segment types invalid
   */
  public void createAndRegisterLinkAndLinkSegments(XMLElementInfrastructure infrastructure,
      Map<Long, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentTypeHelperMap,
      InputBuilderListener inputBuilderListener) throws PlanItException {

    for (XMLElementLinks.Link generatedLink : infrastructure.getLinks().getLink()) {
      Node startNode = inputBuilderListener.getNodeByExternalId(generatedLink.getNodearef().longValue());
      Node endNode = inputBuilderListener.getNodeByExternalId(generatedLink.getNodebref().longValue());
      double length = Double.MIN_VALUE;
      length = getLengthFromLineString(length, generatedLink);
      length = getLengthFromLength(length, generatedLink);
      if (length == Double.MIN_VALUE) {
        throw new PlanItException(
            "Error in network XML file: Must define either a length or GML LineString for link from node "
                + generatedLink.getNodearef().longValue() + " to node " + generatedLink.getNodebref().longValue());
      }      
      Link link = network.links.registerNew(startNode, endNode, length);
      LineString theLineString = parseLinkGeometry(generatedLink);
      link.setGeometry(theLineString);
      
      boolean isFirstLinkSegment = true;
      boolean firstLinkDirection = true;
      for (XMLElementLinkSegment generatedLinkSegment : generatedLink.getLinksegment()) {
        long linkSegmentExternalId = generatedLinkSegment.getId().longValue();
        int noLanes = (generatedLinkSegment.getNumberoflanes() == null) ? LinkSegment.DEFAULT_NUMBER_OF_LANES
            : generatedLinkSegment.getNumberoflanes().intValue();
        long linkType = 0;
        if (generatedLinkSegment.getTyperef() == null) {
          if (linkSegmentTypeHelperMap.keySet().size() > 1) {
            String errorMessage = "Link Segment " + linkSegmentExternalId + " has no link segment defined, but there is more than one possible link segment type";
            throw new PlanItException(errorMessage);
          }
          for (long linkSegmentTypeExternalId : linkSegmentTypeHelperMap.keySet()) {
            linkType = linkSegmentTypeExternalId;
          }
        } else {
          linkType = generatedLinkSegment.getTyperef().longValue();
        }
        Float maxSpeed = generatedLinkSegment.getMaxspeed();
        MacroscopicLinkSegmentTypeXmlHelper macroscopicLinkSegmentTypeXmlHelper = linkSegmentTypeHelperMap.get(linkType);
        // TODO - We should be able to set the maximum speed for individual link
        // segments in the network XML file. This is where we would update it. However
        // we would then need to set it for
        // every mode. We need to change the XSD file to specify how to do this.

        boolean abDirection = generatedLinkSegment.getDir().equals(Direction.A_B);
        if (!isFirstLinkSegment) {
          if (abDirection == firstLinkDirection) {
            String errorMessage =  "Both link segments for the same link are in the same direction.  Link segment external Id is " + linkSegmentExternalId;
            throw new PlanItException(errorMessage);
          }
        }
        createAndRegisterLinkSegment(maxSpeed, network, link, abDirection, macroscopicLinkSegmentTypeXmlHelper, noLanes,
            linkSegmentExternalId, inputBuilderListener);
        isFirstLinkSegment = false;
        firstLinkDirection = abDirection;
      }
    }
  }
  
  /**
   * Reads mode types from input file and stores them in a Map
   * 
   * @param linkconfiguration LinkConfiguration object populated with data from XML file
   * @param inputBuilderListener parser which holds the Map of nodes by external Id
   * @throws PlanItException thrown if there is a Mode value of 0 in the modes definition file
   */
  public void createAndRegisterModes(XMLElementLinkConfiguration linkconfiguration, InputBuilderListener inputBuilderListener) throws PlanItException {
    for (XMLElementModes.Mode generatedMode : linkconfiguration.getModes().getMode()) {
      String name = generatedMode.getName();
      
      /* generate unique name if undefined */
      if(name==null) {
        name = PredefinedModeType.CUSTOM.value().concat(String.valueOf(this.network.modes.size()));
      }
      
      PredefinedModeType modeType = PredefinedModeType.create(name);      
      if(!generatedMode.isPredefined() && modeType != PredefinedModeType.CUSTOM) {
        LOGGER.warning(String.format("mode %s is not registered as predefined mode but name corresponds to PLANit predefined mode, reverting to PLANit predefined mode",generatedMode.getName()));
      }
      
      Mode mode = null;
      if(modeType != PredefinedModeType.CUSTOM) {
        /* predefined mode use factory, ignore other attributes (if any) */
        mode = this.network.modes.registerNew(modeType);
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
                
        mode = this.network.modes.registerNewCustomMode(externalModeId, name, maxSpeed, pcu, physicalFeatures, usabilityFeatures);        
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
  public Map<Long, MacroscopicLinkSegmentTypeXmlHelper> createLinkSegmentTypeHelperMap(
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
      
      /* mode properties, only set when allowed, otherwise not */
      Collection<Mode> thePlanitModes = new HashSet<Mode>();
      if(linkSegmentTypeGenerated.getModes() != null) {
        for (XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode xmlMode : linkSegmentTypeGenerated.getModes().getMode()) {        
          Object modeExternalId = xmlMode.getRef().longValue();

          Mode thePlanitMode = inputBuilderListener.getModeByExternalId(modeExternalId);
          PlanItException.throwIfNull(thePlanitMode, String.format("referenced mode (%d) does not exist in PLANit parser",modeExternalId));
          thePlanitModes.add(thePlanitMode);                                    
        }          
      }else {
        /* all ROAD modes allowed */
        thePlanitModes = inputBuilderListener.getAllModes().stream().filter( 
            mode -> mode.getPhysicalFeatures().getTrackType() == TrackModeType.ROAD).collect(Collectors.toSet());
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
