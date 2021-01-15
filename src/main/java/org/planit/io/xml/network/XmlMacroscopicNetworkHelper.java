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
import org.planit.network.macroscopic.physical.MacroscopicNetwork;
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
import org.planit.xml.generated.XMLElementLinks;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;
import org.planit.xml.generated.XMLElementModes;
import org.planit.xml.generated.XMLElementNodes;

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
  protected void populateLinkSegmentTypeModeProperties(MacroscopicLinkSegmentTypeXmlHelper linkSegmentTypeXmlHelper, String linkSegmentTypeXmlId, Accessmode xmlMode,
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
  protected Map<String, MacroscopicLinkSegmentTypeXmlHelper> populateLinkSegmentTypeHelperMap(Map<String, Mode> modesByXmlId) throws PlanItException {       
    MacroscopicLinkSegmentTypeXmlHelper.reset();
    
    Map<String, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentTypeXmlHelperMap = new HashMap<String, MacroscopicLinkSegmentTypeXmlHelper>();
    XMLElementLinkConfiguration linkconfiguration = xmlRawNetwork.getLinkconfiguration();
    List<XMLElementLinkSegmentType> xmlLinkSegmentTypes = linkconfiguration.getLinksegmenttypes().getLinksegmenttype();    
    for(XMLElementLinkSegmentType xmlLinkSegmentType : xmlLinkSegmentTypes) {
      
      /* xml id */
      String xmlId = xmlLinkSegmentType.getId();      
      if (linkSegmentTypeXmlHelperMap.containsKey(xmlId) && settings.isErrorIfDuplicateXmlId()) {
        throw new PlanItException("Duplicate link segment type external id " + xmlId + " found in network file");
      }
      
      /* external id */
      String externalId = null;
      if(xmlLinkSegmentType.getExternalid() != null && !xmlLinkSegmentType.getExternalid().isBlank()) {
        externalId = xmlLinkSegmentType.getExternalid();
      }        
      
      /* name */
      String name = xmlLinkSegmentType.getName();
      /* capacity */
      double capacity = (xmlLinkSegmentType.getCapacitylane() == null) ? MacroscopicLinkSegmentType.DEFAULT_CAPACITY_LANE  : xmlLinkSegmentType.getCapacitylane();
      /* max density */
      double maximumDensity = (xmlLinkSegmentType.getMaxdensitylane() == null) ? LinkSegment.MAXIMUM_DENSITY  : xmlLinkSegmentType.getMaxdensitylane();    
      
      MacroscopicLinkSegmentTypeXmlHelper linkSegmentTypeXmlHelper = new MacroscopicLinkSegmentTypeXmlHelper(name,capacity, maximumDensity, externalId, xmlId);
      linkSegmentTypeXmlHelperMap.put(xmlId, linkSegmentTypeXmlHelper);      
            
      /* mode properties, only set when allowed, otherwise not */
      Collection<Mode> thePlanitModes = new HashSet<Mode>();            
      if(xmlLinkSegmentType.getAccess() != null) {
        List<Accessmode> xmlModes = xmlLinkSegmentType.getAccess().getMode();        
        for (Accessmode xmlMode : xmlModes) {        
          String modeXmlRefId = xmlMode.getRef();

          Mode thePlanitMode = modesByXmlId.get(modeXmlRefId);
          PlanItException.throwIfNull(thePlanitMode, String.format("referenced mode (xml id:%s) does not exist in PLANit parser",modeXmlRefId));
          
          /* mode properties */
          populateLinkSegmentTypeModeProperties(linkSegmentTypeXmlHelper, xmlId, xmlMode, thePlanitMode);
          
          thePlanitModes.add(thePlanitMode);                                    
        }          
      }else {
        /* all ROAD modes allowed */
        thePlanitModes = modesByXmlId.values().stream().filter( 
            mode -> mode.getPhysicalFeatures().getTrackType() == TrackModeType.ROAD).collect(Collectors.toSet());
        thePlanitModes.forEach( planitMode -> populateLinkSegmentTypeModeProperties(linkSegmentTypeXmlHelper, xmlId, null, planitMode));
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
   * Reads mode types from input file, register them on the network and also populate mapping based on XML ids
   * 
   * @return map with modesByXmlId 
   * @throws PlanItException thrown if there is a Mode value of 0 in the modes definition file
   */
  public Map<String, Mode> createAndRegisterModes() throws PlanItException {

    /* populate if referenced later on by xml id */
    if(settings.getMapToIndexModeByXmlIds()==null) {
      settings.setMapToIndexModeByXmlIds(new HashMap<String, Mode>());
    }
    Map<String, Mode> modesByXmlId = settings.getMapToIndexModeByXmlIds();    
    
    final XMLElementLinkConfiguration linkconfiguration = xmlRawNetwork.getLinkconfiguration();    
    for (XMLElementModes.Mode xmlMode : linkconfiguration.getModes().getMode()) {      
      /* name, generate unique name if undefined */
      String name = xmlMode.getName();
      if(name==null) {
        name = PredefinedModeType.CUSTOM.value().concat(String.valueOf(this.network.modes.size()));
      }
           
      PredefinedModeType modeType = PredefinedModeType.create(name);      
      if(!xmlMode.isPredefined() && modeType != PredefinedModeType.CUSTOM) {
        LOGGER.warning(String.format("mode %s is not registered as predefined mode but name corresponds to PLANit predefined mode, reverting to PLANit predefined mode",xmlMode.getName()));
      }
      
      Mode mode = null;
      if(modeType != PredefinedModeType.CUSTOM) {
        /* predefined mode use factory, ignore other attributes (if any) */
        mode = this.network.modes.registerNew(modeType);
      }else {
        
        /* custom mode, parse all components to correctly configure the custom mode */
        double maxSpeed = xmlMode.getMaxspeed()==null ? Mode.GLOBAL_DEFAULT_MAXIMUM_SPEED_KMH : xmlMode.getMaxspeed();
        double pcu = xmlMode.getPcu()==null ? Mode.GLOBAL_DEFAULT_PCU : xmlMode.getPcu();
        
        PhysicalModeFeatures physicalFeatures = parsePhysicalModeFeatures(xmlMode);
        UsabilityModeFeatures usabilityFeatures = parseUsabilityModeFeatures(xmlMode);        
                
        mode = this.network.modes.registerNewCustomMode(name, maxSpeed, pcu, physicalFeatures, usabilityFeatures);        
      }     
      
      /* xml id */
      if(xmlMode.getId() != null && !xmlMode.getId().isBlank()) {
        mode.setXmlId(xmlMode.getId());
      }
      
      /* external id*/
      if(xmlMode.getExternalid() != null && !xmlMode.getExternalid().isBlank()) {
        mode.setExternalId(xmlMode.getExternalid());
      }      
      
      final Mode prevValue = modesByXmlId.put(mode.getXmlId(), mode);
      if (prevValue!=null && settings.isErrorIfDuplicateXmlId()) {
        String errorMessage = "duplicate mode xml id " + mode.getXmlId() + " found in network file.";
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
  public Map<String, Node> createAndRegisterNodes() throws PlanItException {
    if(settings.getMapToIndexNodeByXmlIds()==null) {
      settings.setMapToIndexNodeByXmlIds(new HashMap<String, Node>());
    }
    Map<String, Node>  nodesByXmlId = settings.getMapToIndexNodeByXmlIds();    
    
    XMLElementInfrastructure infrastructure = xmlRawNetwork.getInfrastructure();    
    for (XMLElementNodes.Node xmlNode : infrastructure.getNodes().getNode()) {

      Node node = network.nodes.registerNew();
      
      /* xml id */
      if(xmlNode.getId() != null && !xmlNode.getId().isBlank()) {
        node.setXmlId(xmlNode.getId());
      }

      /* external id */
      if(xmlNode.getExternalid() != null && !xmlNode.getExternalid().isBlank()) {
        node.setExternalId(xmlNode.getExternalid());
      }
            
      
      PointType pointType = xmlNode.getPoint();
      if (pointType != null) {
        List<Double> posValues = pointType.getPos().getValue();
        Point centrePointGeometry = PlanitJtsUtils.createPoint(posValues.get(0), posValues.get(1));
        node.setPosition(centrePointGeometry);
      }
      final Node prevValue = nodesByXmlId.put(node.getXmlId(), node);
      PlanItException.throwIf(prevValue!=null && settings.isErrorIfDuplicateXmlId(),
          "Duplicate node external id " + node.getXmlId() + " found in network file");
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
  public void createAndRegisterLinkAndLinkSegments(Map<String, Mode> modesByXmlId, Map<String, Node> nodesByXmlId) throws PlanItException {
    //TODO REFACTOR THIS, it is a disaster how this method is implemented
    
    /* link segment xml id map to populate */
    if(settings.getMapToIndexLinkSegmentByXmlIds()==null) {
      settings.setMapToIndexLinkSegmentByXmlIds(new HashMap<String, MacroscopicLinkSegment>());
    }
    Map<String, MacroscopicLinkSegment> linkSegmentsByXmlId = settings.getMapToIndexLinkSegmentByXmlIds();
    
    /* link segment type xml id map to populate */
    if(settings.getMapToIndexLinkSegmentTypeByXmlIds()==null) {
      settings.setMapToIndexLinkSegmentTypeByXmlIds(new HashMap<String, MacroscopicLinkSegmentType>());
    }
    Map<String, MacroscopicLinkSegmentType> linkSegmentTypesByXmlId = settings.getMapToIndexLinkSegmentTypeByXmlIds();    
    
    /* parse link segment types and put findings in helper --> TODO: refactor, do not use helper */
    final Map<String, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentTypeHelpersByXmlId = populateLinkSegmentTypeHelperMap(modesByXmlId);    

    XMLElementInfrastructure infrastructure = xmlRawNetwork.getInfrastructure();
    for (XMLElementLinks.Link xmlLink : infrastructure.getLinks().getLink()) {
      
      /** LINK **/
      Link link = null;
      {
        Node startNode = nodesByXmlId.get(xmlLink.getNodearef());
        Node endNode = nodesByXmlId.get(xmlLink.getNodebref());
        double length = Double.MIN_VALUE;
        length = getLengthFromLineString(length, xmlLink);
        length = getLengthFromLength(length, xmlLink);
        if (length == Double.MIN_VALUE) {
          throw new PlanItException(
              "Error in network XML file: Must define either a length or GML LineString for link from node "
                  + xmlLink.getNodearef() + " to node " + xmlLink.getNodebref());
        }      
        link = network.links.registerNew(startNode, endNode, length);
        
        /* geometry */
        LineString theLineString = parseLinkGeometry(xmlLink);
        link.setGeometry(theLineString);
        
        /* xml id */
        if(xmlLink.getId() != null && !xmlLink.getId().isBlank()) {
          link.setXmlId(xmlLink.getId());
        }else {
          LOGGER.fine(String.format("link id absent, generating internal id instead (node a: %s, node b: %s)",link.getNodeA().getXmlId(), link.getNodeB().getXmlId()));
          link.setXmlId(String.valueOf(link.getId()));
        }         
        
        /* external id */
        if(xmlLink.getExternalid() != null && !xmlLink.getExternalid().isBlank()) {
          link.setExternalId(xmlLink.getExternalid());
        }
      }      
      /** end LINK **/
      
      boolean isFirstLinkSegment = true;
      boolean firstLinkDirection = true;
      
      /** LINK SEGMENT **/
      for (XMLElementLinkSegment xmlLinkSegment : xmlLink.getLinksegment()) {
                                
        // TODO - We should be able to set the maximum speed for individual link
        // segments in the network XML file. This is where we would update it. However
        // we would then need to set it for
        // every mode. We need to change the XSD file to specify how to do this.        
                        
        
        /* direction */
        boolean abDirection = xmlLinkSegment.getDir().equals(Direction.A_B);
        if (!isFirstLinkSegment) {
          if (abDirection == firstLinkDirection) {
            throw new PlanItException("Both link segments for the same link are in the same direction.  Link segment external Id is " + xmlLinkSegment.getId());
          }
        }        

        MacroscopicLinkSegment linkSegment = network.linkSegments.registerNew(link, abDirection, true /* register on nodes and link*/);
            
        /* xml id */
        if(xmlLinkSegment.getId() != null && !xmlLinkSegment.getId().isBlank()) {
          linkSegment.setXmlId(xmlLinkSegment.getId());
        }            
        
        /* external id */
        if(xmlLinkSegment.getExternalid() != null && !xmlLinkSegment.getExternalid().isBlank()) {
          linkSegment.setExternalId(xmlLinkSegment.getExternalid());
        }         
        
        /* max speed */
        double maxSpeed = (xmlLinkSegment.getMaxspeed() == null) ? Double.POSITIVE_INFINITY : xmlLinkSegment.getMaxspeed();        
        linkSegment.setPhysicalSpeedLimitKmH(maxSpeed);
        
        /* lanes */
        int noLanes = (xmlLinkSegment.getNumberoflanes() == null) ? LinkSegment.DEFAULT_NUMBER_OF_LANES : xmlLinkSegment.getNumberoflanes().intValue();        
        linkSegment.setNumberOfLanes(noLanes);   
        
        final MacroscopicLinkSegment prevLinkSegment = linkSegmentsByXmlId.put(linkSegment.getXmlId(), linkSegment);
        PlanItException.throwIf(prevLinkSegment!=null && settings.isErrorIfDuplicateXmlId(), "Duplicate link segment xml id " + linkSegment.getXmlId() + " found in network file");        
        
        /** LINK SEGMENT TYPE **/
        
        /* link segment type xml id */
        String linkTypeXmlId = null;
        if (xmlLinkSegment.getTyperef() == null) {
          if (linkSegmentTypeHelpersByXmlId.keySet().size() > 1) {
            throw new PlanItException("Link Segment " + xmlLinkSegment.getId() + " has no link segment type defined, but there is more than one possible link segment type");
          }
          linkTypeXmlId = linkSegmentTypeHelpersByXmlId.keySet().iterator().next();
        } else {
          linkTypeXmlId = xmlLinkSegment.getTyperef();
        }  
        
        MacroscopicLinkSegmentTypeXmlHelper linkSegmentTypeXmlHelper = linkSegmentTypeHelpersByXmlId.get(linkTypeXmlId);        
        Map<Mode, MacroscopicModeProperties> modeProperties = linkSegmentTypeXmlHelper.getModePropertiesMap();    
        MacroscopicLinkSegmentType existingLinkSegmentType = linkSegmentTypesByXmlId.get(linkTypeXmlId);
        if (existingLinkSegmentType == null) {
          existingLinkSegmentType = 
              network.linkSegmentTypes.createAndRegisterNew(
                  linkSegmentTypeXmlHelper.getName(), 
                  linkSegmentTypeXmlHelper.getCapacityPerLane(),
                  linkSegmentTypeXmlHelper.getMaximumDensityPerLane(),  
                  modeProperties);
          /* link segment type xml id */
          existingLinkSegmentType.setXmlId(linkSegmentTypeXmlHelper.getXmlId());          
          /* link segment type external id */
          existingLinkSegmentType.setExternalId(linkSegmentTypeXmlHelper.getExternalId());
                
          if (existingLinkSegmentType.getXmlId() != null) {
            MacroscopicLinkSegmentType prevValue = linkSegmentTypesByXmlId.put(linkTypeXmlId, existingLinkSegmentType);
            PlanItException.throwIf(prevValue!=null && settings.isErrorIfDuplicateXmlId(),"Duplicate link segment type external id " + linkSegment.getExternalId() + " found in network file");
          }           
        }
        linkSegment.setLinkSegmentType(existingLinkSegmentType);    

        
        isFirstLinkSegment = false;
        firstLinkDirection = abDirection;        
      }
      /** end LINK SEGMENT **/      
    }
  }  

  

}
