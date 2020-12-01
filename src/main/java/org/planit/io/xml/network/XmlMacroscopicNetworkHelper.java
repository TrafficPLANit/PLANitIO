package org.planit.io.xml.network;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.geo.PlanitJtsUtils;
import org.planit.geo.PlanitOpenGisUtils;
import org.planit.io.network.converter.PlanitNetworkReaderSettings;
import org.planit.io.xml.network.physical.macroscopic.MacroscopicLinkSegmentTypeXmlHelper;
import org.planit.io.xml.util.EnumConversionUtil;
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
import org.planit.xml.generated.Accessmode;
import org.planit.xml.generated.Direction;
import org.planit.xml.generated.LengthUnit;
import org.planit.xml.generated.XMLElementInfrastructure;
import org.planit.xml.generated.XMLElementLinkConfiguration;
import org.planit.xml.generated.XMLElementLinkLengthType;
import org.planit.xml.generated.XMLElementLinkSegment;
import org.planit.xml.generated.XMLElementLinkSegmentType;
import org.planit.xml.generated.XMLElementLinkSegmentTypes;
import org.planit.xml.generated.XMLElementLinks;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;
import org.planit.xml.generated.XMLElementModes;
import org.planit.xml.generated.XMLElementNodes;

import net.opengis.gml.AbstractGMLType;
import net.opengis.gml.LineStringType;
import net.opengis.gml.PointType;


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
  private PlanitJtsUtils jtsUtils;
  
  /** object to extract PLANit network from once file is parsed */
  private XMLElementMacroscopicNetwork xmlRawNetwork;   
  
  /** network to populate */
  private MacroscopicNetwork network;

  /** settings specific to parsing the raw XML network and extracting the PLANit memory model equivalent */
  private PlanitNetworkReaderSettings settings;
    
  /**
   * parse the CRS from the raw XML or utilise the default if not present
   */
  private void parseCoordinateRerefenceSystem() {
    CoordinateReferenceSystem crs = null;
    if(xmlRawNetwork.getInfrastructure().getSrsname()==null || xmlRawNetwork.getInfrastructure().getSrsname().isBlank()) {
      crs = PlanitJtsUtils.DEFAULT_GEOGRAPHIC_CRS;
      LOGGER.warning(String.format("coordinate reference system not set for PLANit network reader, applying default %s",crs.getName().getCode()));
    }else {
      crs = PlanitOpenGisUtils.createCoordinateReferenceSystem(xmlRawNetwork.getInfrastructure().getSrsname());
    }
    network.setCoordinateReferenceSystem(crs);     
  }    
  
  /* PROTECTED */
  
  /** parse the usability component of the mode xml element. It is assumed they should be present, if not default values are created
   * @param generatedMode mode to extract information from
   * @return usabilityFeatures that are parsed
   * @throws PlanItException 
   */
  protected UsabilityModeFeatures parseUsabilityModeFeatures(org.planit.xml.generated.XMLElementModes.Mode generatedMode) throws PlanItException {
    if(generatedMode.getUsabilityfeatures() == null) {
      return ModeFeaturesFactory.createDefaultUsabilityFeatures();
    }
    
    /* parse set values */
    UseOfModeType useOfModeType = EnumConversionUtil.xmlToPlanit(generatedMode.getUsabilityfeatures().getUsedtotype());    
    
    return ModeFeaturesFactory.createUsabilityFeatures(useOfModeType);
  }

  /** parse the physical features component of the mode xml element. It is assumed they should be present, if not default values are created
   * @param generatedMode mode to extract information from
   * @return physicalFeatures that are parsed
   * @throws PlanItException 
   */  
  protected PhysicalModeFeatures parsePhysicalModeFeatures(org.planit.xml.generated.XMLElementModes.Mode generatedMode) throws PlanItException {
    if(generatedMode.getPhysicalfeatures() == null) {
      return ModeFeaturesFactory.createDefaultPhysicalFeatures();
    }
    
    /* parse set values */
    VehicularModeType vehicleType = EnumConversionUtil.xmlToPlanit(generatedMode.getPhysicalfeatures().getVehicletype());    
    MotorisationModeType motorisationType = EnumConversionUtil.xmlToPlanit(generatedMode.getPhysicalfeatures().getMotorisationtype());       
    TrackModeType trackType = EnumConversionUtil.xmlToPlanit(generatedMode.getPhysicalfeatures().getTracktype());         
    
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
  protected double getLengthFromLength(double initLength, XMLElementLinks.Link generatedLink) {
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
  protected double getLengthFromLineString(double initLength, XMLElementLinks.Link generatedLink)
      throws PlanItException {
    LineStringType lineStringType = generatedLink.getLineString();
    if (lineStringType != null) {
      List<Double> posList = lineStringType.getPosList().getValue();
      double distance = 0.0;
      Point startPosition = null;
      Point endPosition = null;
      for (int i = 0; i < posList.size(); i += 2) {
        endPosition = PlanitJtsUtils.createPoint(posList.get(i), posList.get(i + 1));
        if (startPosition != null) {
          distance += jtsUtils.getDistanceInKilometres(startPosition, endPosition);
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
   * @param maxSpeedKmH the value of the {@code <maxspeed>} element within the {@code <linksegment>} element in the
   *          input file, null if this element omitted for this link segment
   * @param link the link from which the link segment will be created
   * @param abDirection direction of travel
   * @param noLanes the number of lanes in this link
   * @param externalId the external Id of this link segment
   * @throws PlanItException thrown if there is an error
   */
  protected MacroscopicLinkSegment createAndRegisterLinkSegment(
      double maxSpeedKmH, Link link, boolean abDirection, int noLanes, String externalId) throws PlanItException {
    
    // create the link and store it in the network object
    MacroscopicLinkSegment linkSegment = network.linkSegments.registerNew(link, abDirection, true /* register on nodes and link*/);
        
    linkSegment.setPhysicalSpeedLimitKmH(maxSpeedKmH);    
    linkSegment.setNumberOfLanes(noLanes);
    linkSegment.setExternalId(externalId); 
    
    return linkSegment;    
  }  
  
  /**
   * parse the geometry from the xml link
   * 
   * @param generatedLink xml link
   * @return created LineString if any, null if not present
   * @throws PlanItException thrown if error
   */
  protected LineString parseLinkGeometry(org.planit.xml.generated.XMLElementLinks.Link generatedLink) throws PlanItException {
    /* geometry of link */
    if(generatedLink.getLineString()!=null) {
      LineStringType lst = generatedLink.getLineString();
      if(lst.getCoordinates() != null) {
        return PlanitJtsUtils.createLineStringFromCsvString(lst.getCoordinates().getValue(), lst.getCoordinates().getTs(), lst.getCoordinates().getCs());
      }else if(lst.getPosList()!=null) {
        return PlanitJtsUtils.createLineString(lst.getPosList().getValue());
      }
    }
    return null;    
  }  
  
  /** parse the mode properties for given link segment type and populate the helper with them
   *
   * @param linkSegmentTypeXmlHelper to populate on (TODO:ugly, helper should be removed)
   * @param linkSegmentTypeXmlId reference
   * @param xmlMode to extract from if available (otherwise use defaults)
   * @param thePlanitMode properties relate to
   */
  protected void populateLinkSegmentTypeModeProperties(MacroscopicLinkSegmentTypeXmlHelper linkSegmentTypeXmlHelper, long linkSegmentTypeXmlId, Accessmode xmlMode,
      Mode thePlanitMode) {
    
    double maxSpeed = thePlanitMode.getMaximumSpeedKmH();
    double critSpeed = Math.min(maxSpeed, MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED_KMH); 
    if(xmlMode != null) {

      /** cap max speed of mode properties to global mode's default if it exceeds this maximum */ 
      maxSpeed = (xmlMode.getMaxspeed() == null) ? thePlanitMode.getMaximumSpeedKmH() : xmlMode.getMaxspeed();
      if( Precision.isGreater(maxSpeed, thePlanitMode.getMaximumSpeedKmH(), Precision.EPSILON_6)) {
        maxSpeed = thePlanitMode.getMaximumSpeedKmH();
        LOGGER.warning(String.format("Capped maximum speed for mode %s on link segment type %d to mode's global maximum speed %.1f",
            thePlanitMode.getName(), linkSegmentTypeXmlId, maxSpeed));          
      }        
      
      critSpeed = (xmlMode.getCritspeed() == null) ? MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED_KMH  : xmlMode.getCritspeed();
      /* critical speed can never exceed max speed, so cap it if needed */
      critSpeed = Math.min(maxSpeed, critSpeed);
    }
    
    linkSegmentTypeXmlHelper.updateLinkSegmentTypeModeProperties(linkSegmentTypeXmlId, thePlanitMode, maxSpeed, critSpeed);    
  }  
  
  /**
   * Reads MacroscopicLinkSegmentTypeXmlHelper objects from input file and stores them in a Map
   * @param modesByXmlId to use for reference
   * 
   * @return Map containing link type values identified by their external Ids
   * @throws PlanItException thrown if there is an error reading the input file
   */
  protected Map<Long, MacroscopicLinkSegmentTypeXmlHelper> populateLinkSegmentTypeHelperMap(Map<Long, Mode> modesByXmlId) throws PlanItException {       
    MacroscopicLinkSegmentTypeXmlHelper.reset();
    
    Map<Long, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentTypeXmlHelperMap = new HashMap<Long, MacroscopicLinkSegmentTypeXmlHelper>();
    XMLElementLinkConfiguration linkconfiguration = xmlRawNetwork.getLinkconfiguration();
    List<XMLElementLinkSegmentType> xmlLinkSegmentTypes = linkconfiguration.getLinksegmenttypes().getLinksegmenttype();    
    for(XMLElementLinkSegmentType generatedLinkSegmentType : xmlLinkSegmentTypes) {
      long linkSegmentTypeXmlId = generatedLinkSegmentType.getId().longValue();
      if (linkSegmentTypeXmlHelperMap.containsKey(linkSegmentTypeXmlId) && settings.isErrorIfDuplicateXmlId()) {
        String errorMessage = "Duplicate link segment type external id " + linkSegmentTypeXmlId + " found in network file.";
        throw new PlanItException(errorMessage);
      }
      
      /* name */
      String name = generatedLinkSegmentType.getName();
      /* capacity */
      double capacity = (generatedLinkSegmentType.getCapacitylane() == null) ? MacroscopicLinkSegmentType.DEFAULT_CAPACITY_LANE  : generatedLinkSegmentType.getCapacitylane();
      /* max density */
      double maximumDensity = (generatedLinkSegmentType.getMaxdensitylane() == null) ? LinkSegment.MAXIMUM_DENSITY  : generatedLinkSegmentType.getMaxdensitylane();
      /* external id */
      String externalId = String.valueOf(generatedLinkSegmentType.getId());
      if(generatedLinkSegmentType.getExternalid() != null && !generatedLinkSegmentType.getExternalid().isBlank()) {
        externalId = generatedLinkSegmentType.getExternalid();
      }       
      
      MacroscopicLinkSegmentTypeXmlHelper linkSegmentTypeXmlHelper = new MacroscopicLinkSegmentTypeXmlHelper(name,capacity, maximumDensity, externalId);
      linkSegmentTypeXmlHelperMap.put(linkSegmentTypeXmlId, linkSegmentTypeXmlHelper);      
      
      /* mode properties, only set when allowed, otherwise not */
      Collection<Mode> thePlanitModes = new HashSet<org.planit.utils.mode.Mode>();      
      List<Accessmode> xmlModes = generatedLinkSegmentType.getAccess().getMode();      
      if(generatedLinkSegmentType.getAccess() != null) {
        for (Accessmode xmlMode : xmlModes) {        
          Long modeXmlRefId = xmlMode.getRef().longValue();

          Mode thePlanitMode = modesByXmlId.get(modeXmlRefId);
          PlanItException.throwIfNull(thePlanitMode, String.format("referenced mode (xml id:%d) does not exist in PLANit parser",modeXmlRefId));
          
          /* mode properties */
          populateLinkSegmentTypeModeProperties(linkSegmentTypeXmlHelper, linkSegmentTypeXmlId, xmlMode, thePlanitMode);
          
          thePlanitModes.add(thePlanitMode);                                    
        }          
      }else {
        /* all ROAD modes allowed */
        thePlanitModes = modesByXmlId.values().stream().filter( 
            mode -> mode.getPhysicalFeatures().getTrackType() == TrackModeType.ROAD).collect(Collectors.toSet());
        thePlanitModes.forEach( planitMode -> populateLinkSegmentTypeModeProperties(linkSegmentTypeXmlHelper, linkSegmentTypeXmlId, null, planitMode));
      }
    }
    return linkSegmentTypeXmlHelperMap;
  }   
  
  /* PUBLIC */

 

  /** Constructor
   * @param xmlRawNetwork to extract from
   * @param network to populate
   * @param settings 
   */
  public XmlMacroscopicNetworkHelper(XMLElementMacroscopicNetwork xmlRawNetwork, MacroscopicNetwork network, PlanitNetworkReaderSettings settings) {
    this.xmlRawNetwork = xmlRawNetwork;
    this.network = network;
    this.settings = settings;
    
    /* crs */
    parseCoordinateRerefenceSystem();
    this.jtsUtils = new PlanitJtsUtils(network.getCoordinateReferenceSystem());
  }
 
  /**
   * Reads mode types from input file, register them on the network and also populate mapping based on external ids
   * 
   * @return map with modesByExternalId 
   * @throws PlanItException thrown if there is a Mode value of 0 in the modes definition file
   */
  public Map<Long, Mode> createAndRegisterModes() throws PlanItException {

    /* populate if referenced later on by xml id */
    if(settings.getMapToIndexModeByXmlIds()==null) {
      settings.setMapToIndexModeByXmlIds(new HashMap<Long, Mode>());
    }
    Map<Long, Mode> modesByXmlId = settings.getMapToIndexModeByXmlIds();    
    
    final XMLElementLinkConfiguration linkconfiguration = xmlRawNetwork.getLinkconfiguration();    
    for (XMLElementModes.Mode generatedMode : linkconfiguration.getModes().getMode()) {      
      /* name, generate unique name if undefined */
      String name = generatedMode.getName();
      if(name==null) {
        name = PredefinedModeType.CUSTOM.value().concat(String.valueOf(this.network.modes.size()));
      }
      
      /* external id: = xmlId, unless explicitly set */
      String externalId = String.valueOf(generatedMode.getId());
      if(generatedMode.getExternalid() != null && !generatedMode.getExternalid().isBlank()) {
        externalId = generatedMode.getExternalid();
      }
     
      PredefinedModeType modeType = PredefinedModeType.create(name);      
      if(!generatedMode.isPredefined() && modeType != PredefinedModeType.CUSTOM) {
        LOGGER.warning(String.format("mode %s is not registered as predefined mode but name corresponds to PLANit predefined mode, reverting to PLANit predefined mode",generatedMode.getName()));
      }
      
      Mode mode = null;
      if(modeType != PredefinedModeType.CUSTOM) {
        /* predefined mode use factory, ignore other attributes (if any) */
        mode = this.network.modes.registerNew(modeType);
        mode.setExternalId(externalId);
      }else {
        
        /* custom mode, parse all components to correctly configure the custom mode */
        double maxSpeed = generatedMode.getMaxspeed()==null ? Mode.GLOBAL_DEFAULT_MAXIMUM_SPEED_KMH : generatedMode.getMaxspeed();
        double pcu = generatedMode.getPcu()==null ? Mode.GLOBAL_DEFAULT_PCU : generatedMode.getPcu();
        
        PhysicalModeFeatures physicalFeatures = parsePhysicalModeFeatures(generatedMode);
        UsabilityModeFeatures usabilityFeatures = parseUsabilityModeFeatures(generatedMode);        
                
        mode = this.network.modes.registerNewCustomMode(externalId, name, maxSpeed, pcu, physicalFeatures, usabilityFeatures);        
      }      
      
      final Mode prevValue = modesByXmlId.put(generatedMode.getId().longValue(), mode);
      if (prevValue!=null && settings.isErrorIfDuplicateXmlId()) {
        String errorMessage = "duplicate mode xml id " + generatedMode.getId().longValue() + " found in network file.";
        throw new PlanItException(errorMessage);
      }
    }
    
    return modesByXmlId;
  }
  
  /**
   * Create and register nodes on the network
   * 
   * return nodesByExternalIdToPopulate map for reference
   * @throws PlanItException thrown if there is an error in storing the GML Point definition
   */
  public Map<Long, Node> createAndRegisterNodes() throws PlanItException {
    if(settings.getMapToIndexNodeByXmlIds()==null) {
      settings.setMapToIndexModeByXmlIds(new HashMap<Long, Mode>());
    }
    Map<Long, Node>  nodesByXmlId = settings.getMapToIndexNodeByXmlIds();    
    
    XMLElementInfrastructure infrastructure = xmlRawNetwork.getInfrastructure();    
    for (XMLElementNodes.Node generatedNode : infrastructure.getNodes().getNode()) {

      Node node = network.nodes.registerNew();

      /* external id: = xmlId, unless explicitly set */
      String externalId = String.valueOf(generatedNode.getId());
      if(generatedNode.getExternalid() != null && !generatedNode.getExternalid().isBlank()) {
        externalId = generatedNode.getExternalid();
      }
      node.setExternalId(externalId);      
      
      PointType pointType = generatedNode.getPoint();
      if (pointType != null) {
        List<Double> posValues = pointType.getPos().getValue();
        Point centrePointGeometry = PlanitJtsUtils.createPoint(posValues.get(0), posValues.get(1));
        node.setPosition(centrePointGeometry);
      }
      final Node prevValue = nodesByXmlId.put(generatedNode.getId().longValue(), node);
      PlanItException.throwIf(prevValue!=null && settings.isErrorIfDuplicateXmlId(),
          "Duplicate node external id " + generatedNode.getId().longValue() + " found in network file");
    }
    return nodesByXmlId;
  }  
  
  /**
   * Generated and register link segments
   * @param modesByXmlId to use for reference
   * @param nodesByXmlId to use for reference
   * 
   * @throws PlanItException thrown if there is an error during processing or reference to link segment types invalid
   */
  public void createAndRegisterLinkAndLinkSegments(Map<Long, Mode> modesByXmlId, Map<Long, Node> nodesByXmlId) throws PlanItException {
    //TODO REFACTOR THIS, it is a disaster how this method is implemented
    
    /* link segment xml id map to populate */
    if(settings.getMapToIndexLinkSegmentByXmlIds()==null) {
      settings.setMapToIndexLinkSegmentByXmlIds(new HashMap<Long, MacroscopicLinkSegment>());
    }
    Map<Long, MacroscopicLinkSegment> linkSegmentsByXmlId = settings.getMapToIndexLinkSegmentByXmlIds();
    
    /* link segment type xml id map to populate */
    if(settings.getMapToIndexLinkSegmentTypeByXmlIds()==null) {
      settings.setMapToIndexLinkSegmentTypeByXmlIds(new HashMap<Long, MacroscopicLinkSegmentType>());
    }
    Map<Long, MacroscopicLinkSegmentType> linkSegmentTypesByXmlId = settings.getMapToIndexLinkSegmentTypeByXmlIds();    
    
    /* parse link segment types and put findings in helper --> TODO: refactor, do not use helper */
    final Map<Long, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentTypeHelperMap = populateLinkSegmentTypeHelperMap(modesByXmlId);    

    XMLElementInfrastructure infrastructure = xmlRawNetwork.getInfrastructure();
    for (XMLElementLinks.Link generatedLink : infrastructure.getLinks().getLink()) {
      
      /** LINK **/
      Link link = null;
      {
        Node startNode = nodesByXmlId.get(generatedLink.getNodearef().longValue());
        Node endNode = nodesByXmlId.get(generatedLink.getNodebref().longValue());
        double length = Double.MIN_VALUE;
        length = getLengthFromLineString(length, generatedLink);
        length = getLengthFromLength(length, generatedLink);
        if (length == Double.MIN_VALUE) {
          throw new PlanItException(
              "Error in network XML file: Must define either a length or GML LineString for link from node "
                  + generatedLink.getNodearef().longValue() + " to node " + generatedLink.getNodebref().longValue());
        }      
        link = network.links.registerNew(startNode, endNode, length);
        LineString theLineString = parseLinkGeometry(generatedLink);
        link.setGeometry(theLineString);
        
        /* external id */
        String externalId = String.valueOf(generatedLink.getId());
        if(generatedLink.getExternalid() != null && !generatedLink.getExternalid().isBlank()) {
          externalId = generatedLink.getExternalid();
        } 
        link.setExternalId(externalId);        
      }      
      /** end LINK **/
      
      boolean isFirstLinkSegment = true;
      boolean firstLinkDirection = true;
      for (XMLElementLinkSegment generatedLinkSegment : generatedLink.getLinksegment()) {
        /** LINK SEGMENT **/
        int noLanes = (generatedLinkSegment.getNumberoflanes() == null) ? LinkSegment.DEFAULT_NUMBER_OF_LANES : generatedLinkSegment.getNumberoflanes().intValue();
        long linkTypeXmlId = 0;
        if (generatedLinkSegment.getTyperef() == null) {
          if (linkSegmentTypeHelperMap.keySet().size() > 1) {
            String errorMessage = "Link Segment " + generatedLinkSegment.getId() + " has no link segment defined, but there is more than one possible link segment type";
            throw new PlanItException(errorMessage);
          }
          for (long linkSegmentTypeXmlId : linkSegmentTypeHelperMap.keySet()) {
            /* should only have a single entry, ugly --> refactor */
            linkTypeXmlId = linkSegmentTypeXmlId;
          }
        } else {
          linkTypeXmlId = generatedLinkSegment.getTyperef().longValue();
        }
        
        double maxSpeed = (generatedLinkSegment.getMaxspeed() == null) ? Double.POSITIVE_INFINITY : generatedLinkSegment.getMaxspeed(); 
        MacroscopicLinkSegmentTypeXmlHelper linkSegmentTypeXmlHelper = linkSegmentTypeHelperMap.get(linkTypeXmlId);
        // TODO - We should be able to set the maximum speed for individual link
        // segments in the network XML file. This is where we would update it. However
        // we would then need to set it for
        // every mode. We need to change the XSD file to specify how to do this.

        boolean abDirection = generatedLinkSegment.getDir().equals(Direction.A_B);
        if (!isFirstLinkSegment) {
          if (abDirection == firstLinkDirection) {
            String errorMessage =  "Both link segments for the same link are in the same direction.  Link segment external Id is " + generatedLinkSegment.getId();
            throw new PlanItException(errorMessage);
          }
        }
        
        /* external id */
        String externalId = String.valueOf(generatedLinkSegment.getId());
        if(generatedLinkSegment.getExternalid() != null && !generatedLinkSegment.getExternalid().isBlank()) {
          externalId = generatedLinkSegment.getExternalid();
        }     

        MacroscopicLinkSegment linkSegment = createAndRegisterLinkSegment(maxSpeed, link, abDirection, noLanes, externalId);
        
        Map<Mode, MacroscopicModeProperties> modeProperties = linkSegmentTypeXmlHelper.getModePropertiesMap();    
        MacroscopicLinkSegmentType existingLinkSegmentType = linkSegmentTypesByXmlId.get(linkTypeXmlId);
        if (existingLinkSegmentType == null) {
          existingLinkSegmentType = 
              network.linkSegmentTypes.createAndRegisterNew(
                  linkSegmentTypeXmlHelper.getName(), 
                  linkSegmentTypeXmlHelper.getCapacityPerLane(),
                  linkSegmentTypeXmlHelper.getMaximumDensityPerLane(), 
                  linkSegmentTypeXmlHelper.getExternalId(), 
                  modeProperties);
                
          if (existingLinkSegmentType.getExternalId() != null) {
            MacroscopicLinkSegmentType prevValue = linkSegmentTypesByXmlId.put(linkTypeXmlId, existingLinkSegmentType);
            PlanItException.throwIf(prevValue!=null && settings.isErrorIfDuplicateXmlId(), 
                "Duplicate link segment type external id " + linkSegment.getExternalId() + " found in network file");
          }           
        }
        linkSegment.setLinkSegmentType(existingLinkSegmentType);    

        if (linkSegment.getExternalId() != null) {
          final MacroscopicLinkSegment prevValue = linkSegmentsByXmlId.put(generatedLinkSegment.getId().longValue(), linkSegment);
          PlanItException.throwIf(prevValue!=null && settings.isErrorIfDuplicateXmlId(), 
              "Duplicate link segment xml id " + linkSegment.getExternalId() + " found in network file");
        } 
        /** end LINK SEGMENT **/
        
        isFirstLinkSegment = false;
        firstLinkDirection = abDirection;        
      }
    }
  }  

  

}
