package org.planit.io.xml.network;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.planit.io.converter.network.PlanitNetworkReader;
import org.planit.network.layer.macroscopic.MacroscopicModePropertiesFactory;
import org.planit.network.layer.macroscopic.MacroscopicPhysicalLayer;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsCrsUtils;
import org.planit.utils.geo.PlanitJtsUtils;
import org.planit.utils.misc.StringUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.TrackModeType;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.layer.macroscopic.MacroscopicModeProperties;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.network.layer.physical.Node;
import org.planit.xml.generated.Accessmode;
import org.planit.xml.generated.Direction;
import org.planit.xml.generated.LengthUnit;
import org.planit.xml.generated.XMLElementInfrastructureLayer;
import org.planit.xml.generated.XMLElementLayerConfiguration;
import org.planit.xml.generated.XMLElementLinkLengthType;
import org.planit.xml.generated.XMLElementLinkSegment;
import org.planit.xml.generated.XMLElementLinkSegmentType;
import org.planit.xml.generated.XMLElementLinkSegmentTypes;
import org.planit.xml.generated.XMLElementLinks;
import org.planit.xml.generated.XMLElementNodes;

import net.opengis.gml.DirectPositionListType;
import net.opengis.gml.LineStringType;
import net.opengis.gml.PointType;


/**
 * Process the Infrastructure object populated with data from the XML file
 * 
 * @author gman6028, markr
 *
 */
public class XmlMacroscopicNetworkLayerHelper {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(XmlMacroscopicNetworkLayerHelper.class.getCanonicalName());         
  
  /* PROTECTED */ 
  
  /**
   * Get the link length from the length element in the XML file, if this has
   * been set
   * 
   * @param generatedLink object storing link data from XML file
   * @return final length value
   */
  protected static Double parseLengthElementFromLink(XMLElementLinks.Link generatedLink) {
    Double length = null;
    XMLElementLinkLengthType linkLengthType = generatedLink.getLength();
    if (linkLengthType != null) {
      LengthUnit lengthUnit = linkLengthType.getUnit();
      if ((lengthUnit != null) && (lengthUnit.equals(LengthUnit.M))) {
        length = linkLengthType.getValue()/1000.0;
      }else {
        length = linkLengthType.getValue();
      }
    }
    return length;    
  }

  /**
   * Get the link length from the gml:LineString element in the XML file, if
   * this has been set
   * 
   * @param generatedLink object storing link data from XML file
   * @param jtsUtils to compute length from geometry
   * @return final length value
   * @throws PlanItException thown if error
   */
  protected static Double parseLengthFromLineString(XMLElementLinks.Link generatedLink, PlanitJtsCrsUtils jtsUtils) throws PlanItException {
    Double length = 0.0;
    
    LineStringType lineStringType = generatedLink.getLineString();
    if (lineStringType != null) {
      DirectPositionListType positionList = lineStringType.getPosList();
      if(positionList==null) {
        LOGGER.severe(
            String.format("Link %s has a line string without any positions, this should not happen, consider specifying a length instead, setting length to 0.0", generatedLink.getId()));
        return length;
      }
      
      List<Double> posList = lineStringType.getPosList().getValue();
      Point startPosition = null;
      Point endPosition = null;
      for (int i = 0; i < posList.size(); i += 2) {
        endPosition = PlanitJtsUtils.createPoint(posList.get(i), posList.get(i + 1));
        if (startPosition != null) {
          length += jtsUtils.getDistanceInKilometres(startPosition, endPosition);
        }
        startPosition = endPosition;
      }
    }
    return length;    
  }
  
  /**
   * parse the geometry from the xml link
   * 
   * @param generatedLink xml link
   * @return created LineString if any, null if not present
   * @throws PlanItException thrown if error
   */
  protected static LineString parseLinkGeometry(org.planit.xml.generated.XMLElementLinks.Link generatedLink) throws PlanItException {
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
  
  /** parse the length of an xmlLink based on geometry or length attribute
   * 
   * @param xmlLink to extract length from
   * @param theLineString to extract length from (if not null) when no explicit length is set
   * @param jtsUtils to compute length from geometry
   * @return length (in km)
   * @throws PlanItException thrown if error
   */
  protected static double parseLength(org.planit.xml.generated.XMLElementLinks.Link xmlLink, LineString theLineString, PlanitJtsCrsUtils jtsUtils) throws PlanItException {
    Double length = parseLengthElementFromLink(xmlLink);
    if(length == null && theLineString!=null) {
      /* not explicitly set, try extracting it from geometry  instead */
      length = jtsUtils.getDistanceInKilometres(theLineString);
    }
    
    if (length == null) {
      LOGGER.severe(String.format(
          "Must define either a length or GML LineString for link %s, setting length to 0.0 instead", xmlLink.getId()));
      length = 0.0;
    }  
    
    return length;
  }    
  
  /** parse the mode properties for given link segment type and populate the helper with them
   *
   * @param xmlMode to extract information from on (TODO:ugly, helper should be removed)
   * @param linkSegmentType to register mode properties on
   * @param modesByXmlId to collect referenced planit mode from
   * @throws PlanItException thrown if error
   */
  protected static void parseLinkSegmentTypeModeProperties(Accessmode xmlMode, MacroscopicLinkSegmentType linkSegmentType, Map<String, Mode> modesByXmlId) throws PlanItException{
    /* mode ref */
    String modeXmlRefId = xmlMode.getRef();

    Mode thePlanitMode = modesByXmlId.get(modeXmlRefId);
    PlanItException.throwIfNull(thePlanitMode, String.format("referenced mode (xml id:%s) does not exist in PLANit parser",modeXmlRefId));    
    
    /* planit mode speed settings*/
    double maxSpeed = thePlanitMode.getMaximumSpeedKmH();
    /* crit speed */
    double critSpeed = Math.min(maxSpeed, MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED_KMH);
    
    /* mode properties link segment type speed settings */
    if(xmlMode != null) {
      
      maxSpeed = (xmlMode.getMaxspeed() == null) ? thePlanitMode.getMaximumSpeedKmH() : xmlMode.getMaxspeed();                          
      critSpeed = (xmlMode.getCritspeed() == null) ? MacroscopicModeProperties.DEFAULT_CRITICAL_SPEED_KMH  : xmlMode.getCritspeed();
      /* critical speed can never exceed max speed, so cap it if needed */
      critSpeed = Math.min(maxSpeed, critSpeed);
    }
    
    /* register */
    linkSegmentType.addModeProperties(thePlanitMode, MacroscopicModePropertiesFactory.create(maxSpeed, critSpeed));    
  }  
  
  /**
   * in case no link segment types are defined on the layer, we inject a default link segment type
   *
   * @param xmlLayerConfiguration to inject xml entry into
   */
  protected static void injectDefaultLinkSegmentType(XMLElementLayerConfiguration xmlLayerConfiguration) {
    if (xmlLayerConfiguration.getLinksegmenttypes() == null) {
      /* crete entry */
      xmlLayerConfiguration.setLinksegmenttypes(new XMLElementLinkSegmentTypes());
      /* create defautl type */
      XMLElementLinkSegmentType xmlLinkSegmentType = new XMLElementLinkSegmentType();
      xmlLinkSegmentType.setName("");
      xmlLinkSegmentType.setId(MacroscopicLinkSegmentType.DEFAULT_XML_ID);
      xmlLinkSegmentType.setCapacitylane(MacroscopicLinkSegmentType.DEFAULT_CAPACITY_LANE);
      xmlLinkSegmentType.setMaxdensitylane(LinkSegment.MAXIMUM_DENSITY);
      xmlLayerConfiguration.getLinksegmenttypes().getLinksegmenttype().add(xmlLinkSegmentType);
    }
  }
    
  
  /* PUBLIC */
  
  /** Parse the link segment types
   * 
   * @param xmlLayerconfiguration to extract them from
   * @param networkLayer to register them on
   * @param networkReader to register types on by source id
   * @throws PlanItException thrown if error
   */
  public static void parseLinkSegmentTypes(XMLElementLayerConfiguration xmlLayerconfiguration, MacroscopicPhysicalLayer networkLayer, PlanitNetworkReader networkReader) throws PlanItException {
    
    /* link segment types */
    if(xmlLayerconfiguration.getLinksegmenttypes() == null) {
      /* inject default */
      injectDefaultLinkSegmentType(xmlLayerconfiguration);
    }
                     
    List<XMLElementLinkSegmentType> xmlLinkSegmentTypes = xmlLayerconfiguration.getLinksegmenttypes().getLinksegmenttype();       
    for(XMLElementLinkSegmentType xmlLinkSegmentType : xmlLinkSegmentTypes) {
      
      /* xml id */
      String xmlId = xmlLinkSegmentType.getId();
      
      /* external id */
      String externalId = null;
      if(xmlLinkSegmentType.getExternalid() != null && !xmlLinkSegmentType.getExternalid().isBlank()) {
        externalId = xmlLinkSegmentType.getExternalid();
      }        
      
      /* name */
      String name = xmlLinkSegmentType.getName();
      /* capacity */
      double capacityPcuPerHour = (xmlLinkSegmentType.getCapacitylane() == null) ? MacroscopicLinkSegmentType.DEFAULT_CAPACITY_LANE  : xmlLinkSegmentType.getCapacitylane();
      /* max density */
      double maximumDensityPcuPerKm = (xmlLinkSegmentType.getMaxdensitylane() == null) ? LinkSegment.MAXIMUM_DENSITY  : xmlLinkSegmentType.getMaxdensitylane();
      
      if (networkReader.getLinkSegmentTypeBySourceId(xmlId) != null) {
        throw new PlanItException("duplicate link segment type id " + xmlId + " found in network");
      }      
      /* create and register */
      final MacroscopicLinkSegmentType linkSegmentType = networkLayer.linkSegmentTypes.createAndRegisterNew(name, capacityPcuPerHour, maximumDensityPcuPerKm);
      linkSegmentType.setXmlId(xmlId);
      linkSegmentType.setExternalId(externalId);
      
      networkReader.addLinkSegmentTypeToSourceIdMap(xmlId, linkSegmentType);
            
      /* mode properties, only set when allowed, otherwise not */
      Collection<Mode> thePlanitModes = new HashSet<Mode>();            
      if(xmlLinkSegmentType.getAccess() != null) {
        List<Accessmode> xmlModes = xmlLinkSegmentType.getAccess().getMode();
        for (Accessmode xmlMode : xmlModes) {                  
          /* mode properties */
          parseLinkSegmentTypeModeProperties(xmlMode, linkSegmentType, networkReader.getAllModesBySourceId());                                 
        }          
      }else {
        /* all ROAD modes allowed */
        thePlanitModes = networkReader.getAllModesBySourceId().values().stream().filter( 
            mode -> mode.getPhysicalFeatures().getTrackType() == TrackModeType.ROAD).collect(Collectors.toSet());
        thePlanitModes.forEach( planitMode -> linkSegmentType.addModeProperties(planitMode, MacroscopicModePropertiesFactory.create(planitMode.getMaximumSpeedKmH())));
      }
    }
 
  }   
    
  /**
   * Create and register nodes on the network
   * 
   * return nodesByExternalIdToPopulate map for reference
   * @param xmlLayer to extract from
   * @param networkReader to register nodes on by source id
   * @param networkLayer to populate
   * @return parsed nodes
   * @throws PlanItException thrown if there is an error in storing the GML Point definition
   */
  public static void parseNodes(XMLElementInfrastructureLayer xmlLayer, MacroscopicPhysicalLayer networkLayer, PlanitNetworkReader networkReader) throws PlanItException {  
        
    /* parse nodes */
    for (XMLElementNodes.Node xmlNode : xmlLayer.getNodes().getNode()) {

      Node node = networkLayer.nodes.registerNew();
      
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
      boolean exists = networkReader.addNodeToSourceIdMap(node.getXmlId(), node);
      if(exists) {
       throw new PlanItException("Duplicate node external id %s found in network file", node.getXmlId());
      }
    }
  }  
  
  /** parse link and link segments
   * @param xmlLayer layer to extract them from
   * @param networkLayer to register them on
   * @param networkReader to register links and link segments on by source id
   * @param jtsUtils for length calculations absed on crs
   * @throws PlanItException thrown if error
   */
  public static void parseLinkAndLinkSegments(
      XMLElementInfrastructureLayer xmlLayer, MacroscopicPhysicalLayer networkLayer, PlanitNetworkReader networkReader, PlanitJtsCrsUtils jtsUtils) throws PlanItException {                

    /* links */
    XMLElementLinks xmlLinks = xmlLayer.getLinks();
    PlanItException.throwIfNull(xmlLinks, "links xml element missing");
    
    for (XMLElementLinks.Link xmlLink : xmlLinks.getLink()) {
      
      /** LINK **/
      Link link = null;
      {
        Node startNode = networkReader.getNodeBySourceId(xmlLink.getNodearef());
        Node endNode = networkReader.getNodeBySourceId(xmlLink.getNodebref());
        
        /* geometry */
        LineString theLineString = parseLinkGeometry(xmlLink);        
        double length = parseLength(xmlLink, theLineString, jtsUtils);   
        link = networkLayer.links.registerNew(startNode, endNode, length);
        link.setGeometry(theLineString);
                
        /* xml id */
        if(xmlLink.getId() != null && !xmlLink.getId().isBlank()) {
          link.setXmlId(xmlLink.getId());
        }else {
          LOGGER.severe("DISCARD: Link has no (XML) id, unable to include link");
          continue;          
        }         
        
        /* external id */
        if(xmlLink.getExternalid() != null && !xmlLink.getExternalid().isBlank()) {
          link.setExternalId(xmlLink.getExternalid());
        }
        
        /* name */
        if(!StringUtils.isNullOrBlank(xmlLink.getName())) {
          link.setName(xmlLink.getName());
        }        
      }      
      /** end LINK **/
      
      boolean isFirstLinkSegment = true;
      boolean firstLinkDirection = true;
      
      /** LINK SEGMENT **/
      for (XMLElementLinkSegment xmlLinkSegment : xmlLink.getLinksegment()) {                                                       
        
        /* direction */
        boolean abDirection = xmlLinkSegment.getDir().equals(Direction.A_B);
        if (!isFirstLinkSegment) {
          if (abDirection == firstLinkDirection) {
            throw new PlanItException("Both link segments for the same link are in the same direction.  Link segment external Id is " + xmlLinkSegment.getId());
          }
        }        

        MacroscopicLinkSegment linkSegment = networkLayer.linkSegments.registerNew(link, abDirection, true /* register on nodes and link*/);
            
        /* xml id */
        if(xmlLinkSegment.getId() != null && !xmlLinkSegment.getId().isBlank()) {
          linkSegment.setXmlId(xmlLinkSegment.getId());
        }else {
          LOGGER.severe("DISCARD: Link segment has no (XML) id, unable to include link segment");
          continue;
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
        
        final boolean duplicate = networkReader.addLinkSegmentToSourceIdMap(linkSegment.getXmlId(), linkSegment);
        if(duplicate) {
          throw new PlanItException("Duplicate link segment xml id %s found in network", linkSegment.getXmlId() );        
        }
        
        /** LINK SEGMENT TYPE **/
        
        /* link segment type xml id */
        String linkSegmentTypeXmlId = null;
        if (xmlLinkSegment.getTyperef() == null) {
          if (networkLayer.linkSegmentTypes.size() > 1) {
            throw new PlanItException("Link Segment " + xmlLinkSegment.getId() + " has no link segment type defined, but there is more than one possible link segment type");
          }
          linkSegmentTypeXmlId = networkLayer.linkSegmentTypes.getFirst().getXmlId();
        } else {
          linkSegmentTypeXmlId = xmlLinkSegment.getTyperef();
        }  
        
        /* register type on link */
        MacroscopicLinkSegmentType linkSegmentType = networkReader.getLinkSegmentTypeBySourceId(linkSegmentTypeXmlId);
        if(linkSegmentType == null) {
          throw new PlanItException(String.format("link segment type %s, unknown, cannot be registered on link segment %s",linkSegmentTypeXmlId,linkSegment));
        }
        linkSegment.setLinkSegmentType(linkSegmentType);    
        
        isFirstLinkSegment = false;
        firstLinkDirection = abDirection;        
      }
      /** end LINK SEGMENT **/      
    }
  }

}
