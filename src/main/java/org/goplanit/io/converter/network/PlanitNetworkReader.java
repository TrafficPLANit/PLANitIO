package org.goplanit.io.converter.network;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.converter.network.NetworkReaderImpl;
import org.goplanit.io.xml.network.physical.macroscopic.XmlMacroscopicNetworkLayerHelper;
import org.goplanit.io.xml.util.xmlEnumConversionUtil;
import org.goplanit.io.xml.util.PlanitXmlJaxbParser;
import org.goplanit.mode.ModeFeaturesFactory;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetworkModifierUtils;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.CharacterUtils;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.MotorisationModeType;
import org.goplanit.utils.mode.PhysicalModeFeatures;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.mode.TrackModeType;
import org.goplanit.utils.mode.UsabilityModeFeatures;
import org.goplanit.utils.mode.UseOfModeType;
import org.goplanit.utils.mode.VehicularModeType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.AccessGroupProperties;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.xml.generated.Direction;
import org.goplanit.xml.generated.XMLElementAccessGroup;
import org.goplanit.xml.generated.XMLElementConfiguration;
import org.goplanit.xml.generated.XMLElementInfrastructureLayer;
import org.goplanit.xml.generated.XMLElementInfrastructureLayers;
import org.goplanit.xml.generated.XMLElementLayerConfiguration;
import org.goplanit.xml.generated.XMLElementLinkSegment;
import org.goplanit.xml.generated.XMLElementLinkSegmentType;
import org.goplanit.xml.generated.XMLElementLinks;
import org.goplanit.xml.generated.XMLElementMacroscopicNetwork;
import org.goplanit.xml.generated.XMLElementModes;
import org.goplanit.xml.generated.XMLElementNodes;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import net.opengis.gml.PointType;

/**
 * Implementation of the network reader for the PLANit XML native format
 * 
 * @author gman, markr
 *
 */
public class PlanitNetworkReader extends NetworkReaderImpl {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkReader.class.getCanonicalName());            
  
  /** the settings for this reader */
  private final PlanitNetworkReaderSettings settings;
  
  /** parses the XML content in JAXB memory format */
  private final PlanitXmlJaxbParser<XMLElementMacroscopicNetwork> xmlParser;
          
  /** the network memory model to populate */
  private MacroscopicNetwork network;
    
  /**
   * initialise the XML id trackers, so we can lay indices on the XML id as well for quick lookups
   */
  private void initialiseXmlIdTrackers() {
    initialiseSourceIdMap(Mode.class, Mode::getXmlId);
    initialiseSourceIdMap(Link.class, Link::getXmlId);
    initialiseSourceIdMap(MacroscopicLinkSegment.class, MacroscopicLinkSegment::getXmlId);
    initialiseSourceIdMap(MacroscopicLinkSegmentType.class, MacroscopicLinkSegmentType::getXmlId);
    initialiseSourceIdMap(Node.class, Node::getXmlId);
  }

  /**
   * Initialise event listeners in case we want to make changes to the XML ids after parsing is complete, e.g., if the parsed
   * network is going to be modified and saved to disk afterwards, then it is advisable to sync all XML ids to the internal ids upon parsing
   * because this avoids the risk of generating duplicate XML ids during editing of the network (when XML ids are chosen to be synced to internal ids)
   */
  private void syncXmlIdsToIds() {
    LOGGER.info("Syncing PLANit physical network XML ids to internally generated ids, overwriting original XML ids");
    MacroscopicNetworkModifierUtils.syncManagedIdEntitiesContainerXmlIdsToIds(this.network);
  }

  /**
   * Update the XML macroscopic network element to include default values for any properties not included in the input file
   */
  private void injectMissingDefaultsToRawXmlNetwork() {
    XMLElementMacroscopicNetwork rootElement = xmlParser.getXmlRootElement();
    if (xmlParser.getXmlRootElement().getConfiguration() == null) {
      rootElement.setConfiguration(new XMLElementConfiguration());
    }
    
    //if no modes defined, create single mode with default values
    if (rootElement.getConfiguration().getModes() == null) {
      rootElement.getConfiguration().setModes(new XMLElementModes());
      XMLElementModes.Mode xmlElementMode = new XMLElementModes.Mode();
      // default in absence of any modes is the predefined CAR mode
      xmlElementMode.setPredefined(true);
      xmlElementMode.setName(PredefinedModeType.CAR.value());
      xmlElementMode.setId(Mode.DEFAULT_XML_ID);
      rootElement.getConfiguration().getModes().getMode().add(xmlElementMode);
    }
           
   }  
      
  /** parse the usability component of the mode xml element. It is assumed they should be present, if not default values are created
   * @param generatedMode mode to extract information from
   * @return usabilityFeatures that are parsed
   */
  private UsabilityModeFeatures parseUsabilityModeFeatures(org.goplanit.xml.generated.XMLElementModes.Mode generatedMode) {
    if(generatedMode.getUsabilityfeatures() == null) {
      return ModeFeaturesFactory.createDefaultUsabilityFeatures();
    }
    
    /* parse set values */
    UseOfModeType useOfModeType = xmlEnumConversionUtil.xmlToPlanit(generatedMode.getUsabilityfeatures().getUsedtotype());
    
    return ModeFeaturesFactory.createUsabilityFeatures(useOfModeType);
  }

  /** parse the physical features component of the mode xml element. It is assumed they should be present, if not default values are created
   * @param generatedMode mode to extract information from
   * @return physicalFeatures that are parsed
   */  
  private PhysicalModeFeatures parsePhysicalModeFeatures(org.goplanit.xml.generated.XMLElementModes.Mode generatedMode) {
    if(generatedMode.getPhysicalfeatures() == null) {
      return ModeFeaturesFactory.createDefaultPhysicalFeatures();
    }
    
    /* parse set values */
    VehicularModeType vehicleType = xmlEnumConversionUtil.xmlToPlanit(generatedMode.getPhysicalfeatures().getVehicletype());
    MotorisationModeType motorisationType = xmlEnumConversionUtil.xmlToPlanit(generatedMode.getPhysicalfeatures().getMotorisationtype());
    TrackModeType trackType = xmlEnumConversionUtil.xmlToPlanit(generatedMode.getPhysicalfeatures().getTracktype());
    
    return ModeFeaturesFactory.createPhysicalFeatures(vehicleType, motorisationType, trackType);
  }    
  
  /**
   * Reads mode types from input file, register them on the network and also populate mapping based on XML ids
   */
  private void parseModes(){
    
    final XMLElementConfiguration xmlGeneralConfiguration = xmlParser.getXmlRootElement().getConfiguration();    
    for (XMLElementModes.Mode xmlMode : xmlGeneralConfiguration.getModes().getMode()) {
      
      /* xml id */
      String modeXmlId = null;
      if(xmlMode.getId() != null && !xmlMode.getId().isBlank()) {
        modeXmlId = xmlMode.getId();
      }      
      
      /* name, generate unique name if undefined */
      String name = xmlMode.getName();
      String potentialPredefinedModeType = name;
      if(potentialPredefinedModeType==null) {
        potentialPredefinedModeType = modeXmlId;
      }
      PredefinedModeType modeType = PredefinedModeType.create(potentialPredefinedModeType);      
      if(!xmlMode.isPredefined() && modeType != PredefinedModeType.CUSTOM) {
        LOGGER.warning(String.format("Mode is not registered as predefined mode but name or xmlid corresponds to PLANit predefined mode, reverting to PLANit predefined mode %s",modeType.name()));
      }
      if(xmlMode.isPredefined() && modeType == PredefinedModeType.CUSTOM) {
        LOGGER.warning(String.format("Mode is known as predefined mode but XML flag indicates it should be a PLANit predefined mode, reverting to PLANit custom mode %s",modeType.name()));
      }
      if(name==null && modeType == PredefinedModeType.CUSTOM) {
        name = PredefinedModeType.CUSTOM.value().concat(String.valueOf(this.network.getModes().size()));
      }
                 
      Mode mode = null;
      if(modeType != PredefinedModeType.CUSTOM) {
        /* predefined mode use factory, ignore other attributes (if any) */
        mode = this.network.getModes().getFactory().registerNew(modeType);
      }else {
        
        /* custom mode, parse all components to correctly configure the custom mode */
        double maxSpeed = xmlMode.getMaxspeed()==null ? Mode.GLOBAL_DEFAULT_MAXIMUM_SPEED_KMH : xmlMode.getMaxspeed();
        double pcu = xmlMode.getPcu()==null ? Mode.GLOBAL_DEFAULT_PCU : xmlMode.getPcu();
        
        PhysicalModeFeatures physicalFeatures = parsePhysicalModeFeatures(xmlMode);
        UsabilityModeFeatures usabilityFeatures = parseUsabilityModeFeatures(xmlMode);        
                
        mode = this.network.getModes().getFactory().registerNewCustomMode(name, maxSpeed, pcu, physicalFeatures, usabilityFeatures);        
      }     
            
      /* external id*/
      if(xmlMode.getExternalid() != null && !xmlMode.getExternalid().isBlank()) {
        mode.setExternalId(xmlMode.getExternalid());
      }      
      
      /* xml id */
      mode.setXmlId(modeXmlId);
      registerBySourceId(Mode.class, mode);      
    }
  }  
  
  /**
   * parse the CRS from the raw XML or utilise the default if not present
   * 
   * @param xmlLayers element from which to parse crs
   * @throws PlanItException thrown if error
   */
  private CoordinateReferenceSystem parseCoordinateRerefenceSystem(XMLElementInfrastructureLayers xmlLayers) throws PlanItException {
    CoordinateReferenceSystem crs = null;
    crs = PlanitXmlJaxbParser.createPlanitCrs(xmlLayers.getSrsname());
    return crs;
  }    
  
  /** Parse the various network layers
   * 
   * @throws PlanItException thrown if error
   */
  private void parseNetworkLayers() throws PlanItException {
    XMLElementInfrastructureLayers xmlLayers = xmlParser.getXmlRootElement().getInfrastructurelayers();
    PlanItException.throwIfNull(xmlLayers, "infrastructurelayers element not present in network file");
    
    /* crs */
    CoordinateReferenceSystem crs = parseCoordinateRerefenceSystem(xmlLayers);
    network.setCoordinateReferenceSystem(crs);  
    PlanitJtsCrsUtils jtsUtils = new PlanitJtsCrsUtils(network.getCoordinateReferenceSystem());
    
    /* layers */
    List<XMLElementInfrastructureLayer> xmlLayerList = xmlLayers.getLayer();
    Set<Mode> usedModes = new TreeSet<Mode>();
    for(XMLElementInfrastructureLayer xmlLayer : xmlLayerList) {
      
      /*layer */
      NetworkLayer layer = parseNetworkLayer(xmlLayer, jtsUtils);
      
      /* validate supported modes */
      int prevSize = usedModes.size();
      usedModes.addAll(layer.getSupportedModes());
      if(usedModes.size() != prevSize + layer.getSupportedModes().size()) {
        /* mode used in other layer already, this is not allowed */
        throw new PlanItException("modes are only allowed to be used in a single network layer, not multiple, please check your network inputs");
      }
    }    
  }    
  
  /**
   * parse the network layer
   * 
   * @param xmlLayer layer to extract from
   * @param jtsUtils to use
   * @return parsed network layer
   * @throws PlanItException thrown if error
   *
   */
  private NetworkLayer parseNetworkLayer(XMLElementInfrastructureLayer xmlLayer, PlanitJtsCrsUtils jtsUtils ) throws PlanItException {
    
    /* create layer */
    MacroscopicNetworkLayer networkLayer = network.getTransportLayers().getFactory().registerNew();
    
    /* xml id */
    if(xmlLayer.getId() != null && !xmlLayer.getId().isBlank()) {
      networkLayer.setXmlId(xmlLayer.getId());
    }else {
      LOGGER.warning("infrastructure layer id missing in xml, use generated id instead");
      networkLayer.setXmlId(Long.toString(networkLayer.getId()));
    }
    
    /* external id*/
    if(xmlLayer.getExternalid() != null && !xmlLayer.getExternalid().isBlank()) {
      networkLayer.setExternalId(xmlLayer.getExternalid());
    }  
          
    /* supported modes*/
    if(xmlLayer.getModes() != null && !xmlLayer.getModes().isBlank()) {
      String xmlSupportedModes = xmlLayer.getModes();
      String[] modeRefs = xmlSupportedModes.split(CharacterUtils.COMMA.toString());
      for(String mode : Arrays.asList(modeRefs)) {
        Mode planitMode = getBySourceId(Mode.class, mode);
        if(planitMode != null) {
          networkLayer.registerSupportedMode(planitMode);
        }else {
          LOGGER.severe(String.format("mode %s is not present on the network, ignored on network layer", mode));
        }
      }      
    }else {
      /* absent, so register all modes (check if this is valid is to be executed by caller */
      networkLayer.registerSupportedModes(network.getModes().valuesAsNewSet());
    }
    
    /* link segment types */
    XMLElementLayerConfiguration xmlLayerconfiguration = xmlLayer.getLayerconfiguration();
    if(xmlLayerconfiguration == null) {
      xmlLayer.setLayerconfiguration(new XMLElementLayerConfiguration());
      xmlLayerconfiguration = xmlLayer.getLayerconfiguration();
    }
    parseLinkSegmentTypes(xmlLayerconfiguration, networkLayer);
    
    /* parse nodes */
    parseNodes(xmlLayer, networkLayer);                  
         
    /* parse links, link segments */
    parseLinkAndLinkSegments(xmlLayer, networkLayer, jtsUtils);
    
    return networkLayer;
  }

  /** Parse the link segment types
   * 
   * @param xmlLayerconfiguration to extract them from
   * @param networkLayer to register them on
   * @throws PlanItException thrown if error
   */
  public void parseLinkSegmentTypes(final XMLElementLayerConfiguration xmlLayerconfiguration, final MacroscopicNetworkLayer networkLayer) throws PlanItException {
    
    /* link segment types */
    if(xmlLayerconfiguration.getLinksegmenttypes() == null) {
      /* inject default */
      XmlMacroscopicNetworkLayerHelper.injectDefaultLinkSegmentType(xmlLayerconfiguration);
    }
    
    double defaultMaxSpeedKph = Double.MAX_VALUE;
    for(Mode mode : networkLayer.getSupportedModes()) {
      defaultMaxSpeedKph = Math.max(defaultMaxSpeedKph, mode.getMaximumSpeedKmH());
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
      /* capacity (may be null in which case the default is returned if required or it is retrieved elsewhere via for example the fundamental diagram which is able to
       * deliver a better estimate based on the free flow speed and FD shape) */
      Double capacityPcuPerHour = xmlLinkSegmentType.getCapacitylane();
      /* max density (may be null) */
      Double maximumDensityPcuPerKm = xmlLinkSegmentType.getMaxdensitylane();
        
      /* create and register */
      MacroscopicLinkSegmentType linkSegmentType = null;
      if(xmlLinkSegmentType.getCapacitylane() !=null && xmlLinkSegmentType.getMaxdensitylane()!=null) {
        linkSegmentType = networkLayer.getLinkSegmentTypes().getFactory().registerNew(name, capacityPcuPerHour, maximumDensityPcuPerKm);
      }else if(xmlLinkSegmentType.getCapacitylane() !=null ) {
        linkSegmentType = networkLayer.getLinkSegmentTypes().getFactory().registerNewWithCapacity(name, capacityPcuPerHour);
      }else if(xmlLinkSegmentType.getMaxdensitylane()!=null) {
        linkSegmentType = networkLayer.getLinkSegmentTypes().getFactory().registerNewWithMaxDensity(name, maximumDensityPcuPerKm);
      }else {
        linkSegmentType = networkLayer.getLinkSegmentTypes().getFactory().registerNew(name);
      }
      linkSegmentType.setXmlId(xmlId);
      linkSegmentType.setExternalId(externalId);
      
      registerBySourceId(MacroscopicLinkSegmentType.class, linkSegmentType);
      
      /* only road going modes are added by default if no access groups are specified */
      Collection<Mode> supportedDefaultRoadModes = networkLayer.getSupportedModes().stream().filter( 
          mode -> mode.getPhysicalFeatures().getTrackType() == TrackModeType.ROAD).collect(Collectors.toSet());
            
      /* mode properties, only set when allowed, otherwise not */       
      if(xmlLinkSegmentType.getAccess() != null) {
        List<XMLElementAccessGroup> xmlAccessGroups = xmlLinkSegmentType.getAccess().getAccessgroup();
        for (XMLElementAccessGroup xmlAccessGroup : xmlAccessGroups) {                  
          /* mode access properties */
          parseLinkSegmentTypeAccessProperties(xmlAccessGroup, linkSegmentType, supportedDefaultRoadModes);                                 
        }          
      }else {
        /* all ROAD modes allowed */
        parseLinkSegmentTypeAccessProperties(null /*results in supportedDefaultRoadModes mode access in single group*/, linkSegmentType, supportedDefaultRoadModes);
      }
    }
 
  } 
  
  /** parse the mode properties for given link segment type and populate the helper with them
  *
  * @param xmlAccessGroupProperties to extract information from on (TODO:ugly, helper should be removed)
  * @param linkSegmentType to register mode properties on
   * @param defaultModes to allow when no modes are specified
  * @throws PlanItException thrown if error
  */
 public void parseLinkSegmentTypeAccessProperties(XMLElementAccessGroup xmlAccessGroupProperties, MacroscopicLinkSegmentType linkSegmentType, final Collection<Mode> defaultModes) throws PlanItException{
   
   /* access modes */
   Collection<Mode> accessModes;
   if(xmlAccessGroupProperties==null || xmlAccessGroupProperties.getModerefs()==null) {
     /* all default modes allowed */
     accessModes = defaultModes;
   }else {
     String[] xmlModesRefArray = xmlAccessGroupProperties.getModerefs().split(",");
     accessModes = new TreeSet<>();
     for(int index = 0 ;index < xmlModesRefArray.length;++index) {
       Mode thePlanitMode = getBySourceId(Mode.class, xmlModesRefArray[index]);
       PlanItException.throwIfNull(thePlanitMode, String.format("Referenced mode (xml id:%s) does not exist in PLANit parser",xmlModesRefArray[index]));
       accessModes.add(thePlanitMode);
     }      
   }    
   
   Collection<Mode> alreadyAllowedModes = linkSegmentType.getAllowedModes();
   if(!Collections.disjoint(alreadyAllowedModes, accessModes)) {
     LOGGER.warning(String.format("Access (mode) groups for link segment type %s have overlapping modes, undefined behaviour of which properties prevail for duplicate modes",linkSegmentType.getXmlId()));
   }   
      
   /* mode properties link segment type speed settings */
   AccessGroupProperties groupProperties = null;
   if(xmlAccessGroupProperties != null) {
     
     if(xmlAccessGroupProperties.getMaxspeed() != null && xmlAccessGroupProperties.getCritspeed() != null) {
       groupProperties = AccessGroupPropertiesFactory.create(xmlAccessGroupProperties.getMaxspeed(), xmlAccessGroupProperties.getCritspeed(), accessModes);
     }else if(xmlAccessGroupProperties.getMaxspeed() != null) {
       groupProperties = AccessGroupPropertiesFactory.create(xmlAccessGroupProperties.getMaxspeed(), accessModes);
     }else if(xmlAccessGroupProperties.getCritspeed() != null) {
       LOGGER.warning(String.format("IGNORE: Not allowed to only set a critical speed for an access group (link segment type %s)",linkSegmentType.getXmlId()));
     }          
   }
   
   if(groupProperties==null) {
     /* register without setting any speed information, which means they are to be derived from the mode and or links most restrictive information on-the-fly instead*/
     groupProperties = AccessGroupPropertiesFactory.create(accessModes);   
   }
   linkSegmentType.setAccessGroupProperties(groupProperties);       
 }    
    
  /**
   * Create and register nodes on the network
   * 
   * @param xmlLayer to extract from
   * @param networkLayer to populate
   * @throws PlanItException thrown if there is an error in storing the GML Point definition
   */
  public void parseNodes(XMLElementInfrastructureLayer xmlLayer, MacroscopicNetworkLayer networkLayer) throws PlanItException {  
        
    /* parse nodes */
    for (XMLElementNodes.Node xmlNode : xmlLayer.getNodes().getNode()) {

      Node node = networkLayer.getNodes().getFactory().registerNew();
      
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
      registerBySourceId(Node.class, node);
    }
  }  
  
  /** parse link and link segments
   * @param xmlLayer layer to extract them from
   * @param networkLayer to register them on
   * @param jtsUtils for length calculations based on crs
   * @throws PlanItException thrown if error
   */
  public void parseLinkAndLinkSegments(
      XMLElementInfrastructureLayer xmlLayer, MacroscopicNetworkLayer networkLayer, PlanitJtsCrsUtils jtsUtils) throws PlanItException {                

    /* links */
    XMLElementLinks xmlLinks = xmlLayer.getLinks();
    PlanItException.throwIfNull(xmlLinks, "links xml element missing");
    
    for (XMLElementLinks.Link xmlLink : xmlLinks.getLink()) {
      
      /* LINK */
      MacroscopicLink link;
      {
        /* xml id */
        if(StringUtils.isNullOrBlank(xmlLink.getId())) {
          LOGGER.severe("IGNORE: Link has no (XML) id, unable to include link");
          continue;          
        }          
        String xmlId = xmlLink.getId();
        
        if(StringUtils.isNullOrBlank(xmlLink.getNodearef())){
          LOGGER.warning(String.format("IGNORE: No node A reference present on link %s",xmlId));
          continue;
        }        
        Node startNode = getBySourceId(Node.class, xmlLink.getNodearef());

        if(StringUtils.isNullOrBlank(xmlLink.getNodebref())){
          LOGGER.warning(String.format("IGNORE: No node B reference present on link %s",xmlId));
          continue;
        }         
        Node endNode = getBySourceId(Node.class, xmlLink.getNodebref());
        
        /* geometry */
        LineString theLineString = XmlMacroscopicNetworkLayerHelper.parseLinkGeometry(xmlLink);        
        double length = XmlMacroscopicNetworkLayerHelper.parseLength(xmlLink, theLineString, jtsUtils);   
        link = networkLayer.getLinks().getFactory().registerNew(startNode, endNode, length, true /* register on nodes */);
        link.setXmlId(xmlLink.getId());
        link.setGeometry(theLineString);                      
        
        /* external id */
        if(xmlLink.getExternalid() != null && !xmlLink.getExternalid().isBlank()) {
          link.setExternalId(xmlLink.getExternalid());
        }
        
        /* name */
        if(!StringUtils.isNullOrBlank(xmlLink.getName())) {
          link.setName(xmlLink.getName());
        }

        /* validate link */
        link.validate();
            
        registerBySourceId(Link.class, link);
      }      
      /* end LINK */
      
      boolean isFirstLinkSegment = true;
      boolean firstLinkDirection = true;

      /* LINK SEGMENT **/
      for (XMLElementLinkSegment xmlLinkSegment : xmlLink.getLinksegment()) {                                                       
        
        /* direction */
        boolean abDirection = xmlLinkSegment.getDir().equals(Direction.A_B);
        if (!isFirstLinkSegment) {
          if (abDirection == firstLinkDirection) {
            throw new PlanItException("Both link segments for the same link are in the same direction.  Link segment external Id is " + xmlLinkSegment.getId());
          }
        }        

        MacroscopicLinkSegment linkSegment = networkLayer.getLinkSegments().getFactory().registerNew(link, abDirection, true /* register on nodes and link*/);
            
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
        
        registerBySourceId(MacroscopicLinkSegment.class, linkSegment);
        
        /* LINK SEGMENT TYPE */
        
        /* link segment type xml id */
        String linkSegmentTypeXmlId;
        if (xmlLinkSegment.getTyperef() == null) {
          if (networkLayer.getLinkSegmentTypes().size() > 1) {
            throw new PlanItException("Link Segment " + xmlLinkSegment.getId() + " has no link segment type defined, but there is more than one possible link segment type");
          }
          linkSegmentTypeXmlId = networkLayer.getLinkSegmentTypes().getFirst().getXmlId();
        } else {
          linkSegmentTypeXmlId = xmlLinkSegment.getTyperef();
        }  
        
        /* register type on link */
        MacroscopicLinkSegmentType linkSegmentType = getBySourceId(MacroscopicLinkSegmentType.class, linkSegmentTypeXmlId);
        if(linkSegmentType == null) {
          throw new PlanItException(String.format("Link segment type %s, unknown, cannot be registered on link segment %s",linkSegmentTypeXmlId,linkSegment));
        }
        linkSegment.setLinkSegmentType(linkSegmentType);    
        
        isFirstLinkSegment = false;
        firstLinkDirection = abDirection;        
      }
      /* end LINK SEGMENT */
    }
  }  

  /** Place network to populate
   * 
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected void setNetwork(final LayeredNetwork<?,?> network) throws PlanItException {
    /* currently we only support macroscopic infrastructure networks */
    if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItException("currently the PLANit network reader only supports macroscopic infrastructure networks, the provided network is not of this type");
    }
    
    this.network = (MacroscopicNetwork) network;
  }
  
  /** Constructor where settings are directly provided such that input information can be exracted from it
   * 
   * @param settings to use
   * @param idToken to use for the network to populate
   * @throws PlanItException  thrown if error
   */
  protected PlanitNetworkReader(PlanitNetworkReaderSettings settings, IdGroupingToken idToken) throws PlanItException{
    super();
    this.xmlParser = new PlanitXmlJaxbParser<>(XMLElementMacroscopicNetwork.class);
    this.settings = settings;
    setNetwork(new MacroscopicNetwork(idToken));
  }  
  
  /** Constructor where settings are directly provided such that input information can be extracted from it
   * 
   * @param settings to use
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitNetworkReader(PlanitNetworkReaderSettings settings, LayeredNetwork<?,?> network) throws PlanItException{
    super();
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementMacroscopicNetwork>(XMLElementMacroscopicNetwork.class);
    this.settings = settings;
    setNetwork(network);
  }  
    
  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param externalXmlRawNetwork to extract from
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitNetworkReader(
      XMLElementMacroscopicNetwork externalXmlRawNetwork, LayeredNetwork<?,?> network) throws PlanItException{
    this(externalXmlRawNetwork, new PlanitNetworkReaderSettings(), network);
  }

  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   *
   * @param externalXmlRawNetwork to extract from
   * @param network to populate
   * @param settings to use
   * @throws PlanItException thrown if error
   */
  protected PlanitNetworkReader(XMLElementMacroscopicNetwork externalXmlRawNetwork, PlanitNetworkReaderSettings settings, LayeredNetwork<?,?> network) throws PlanItException{
    super();
    this.xmlParser = new PlanitXmlJaxbParser<>(externalXmlRawNetwork);
    this.settings = settings;
    setNetwork(network);
  }
  
  /** Constructor
   * 
   * @param networkPathDirectory to use
   * @param xmlFileExtension to use
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitNetworkReader(
      String networkPathDirectory, String xmlFileExtension, LayeredNetwork<?,?> network) throws PlanItException{
    super();
    this.xmlParser = new PlanitXmlJaxbParser<>(XMLElementMacroscopicNetwork.class);
    this.settings = new PlanitNetworkReaderSettings(networkPathDirectory, xmlFileExtension);
    setNetwork(network);
  }  
  
  /** Default XSD files used to validate input XML files against, TODO: move to properties file */
  public static final String NETWORK_XSD_FILE = "https://trafficplanit.github.io/PLANitManual/xsd/macroscopicnetworkinput.xsd";  

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetwork read(){
        
    /* parse the XML raw network to extract PLANit network from */   
    xmlParser.initialiseAndParseXmlRootElement(getSettings().getInputDirectory(), getSettings().getXmlFileExtension());
    PlanItRunTimeException.throwIfNull(xmlParser.getXmlRootElement(), "No valid PLANit XML network could be parsed into memory, abort");
    
    /* xml id */
    String networkXmlId = xmlParser.getXmlRootElement().getId();
    if(StringUtils.isNullOrBlank(networkXmlId)) {
      LOGGER.warning(String.format("Network has no XML id defined, adopting internally generated id %d instead",network.getId()));
      networkXmlId = String.valueOf(network.getId());
    }
    network.setXmlId(networkXmlId);
            
    /* defaults */
    initialiseXmlIdTrackers();
    injectMissingDefaultsToRawXmlNetwork();       
    
    try {
      
      /* parse modes*/
      parseModes();

      /* parse layers */
      parseNetworkLayers();

      if(getSettings().isSyncXmlIdsToIds()){
        syncXmlIdsToIds();
      }

      /* log stats */
      network.logInfo(LoggingUtils.networkPrefix(network.getId()));
      
      /* free xml content */
      xmlParser.clearXmlContent();
      
    } catch (PlanItException e) {
      throw new PlanItRunTimeException(e);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException("Error while populating physical network in PLANitIO",e);
    }    
    
    return network;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitNetworkReaderSettings getSettings() {
    return settings;
  }
  
  /**
   * returns the first link segment for which the given external id matches. Extremely slow, because it is not indexed at the moment. Also
   * external ids are not guaranteed to be unique so if multiple matches exist problems may arise
   * 
   * @param network    to look in
   * @param externalId to look for
   * @return link segment
   */
  public MacroscopicLinkSegment getLinkSegmentByExternalId(MacroscopicNetwork network, String externalId) {
    for (MacroscopicNetworkLayer layer : network.getTransportLayers()) {
      MacroscopicLinkSegment firstMatch = layer.getLinkSegments().firstMatch(ls -> externalId.equals(ls.getExternalId()));
      if (firstMatch != null) {
        return firstMatch;
      }
    }
    return null;
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // TODO Auto-generated method stub    
  }
  
  


}
