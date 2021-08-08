package org.planit.io.converter.network;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.converter.network.NetworkReaderImpl;
import org.planit.io.xml.network.physical.macroscopic.XmlMacroscopicNetworkLayerHelper;
import org.planit.io.xml.util.EnumConversionUtil;
import org.planit.io.xml.util.PlanitXmlJaxbParser;
import org.planit.mode.ModeFeaturesFactory;
import org.planit.network.MacroscopicNetwork;
import org.planit.network.TransportLayerNetwork;
import org.planit.network.layer.macroscopic.MacroscopicModePropertiesFactory;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsCrsUtils;
import org.planit.utils.geo.PlanitJtsUtils;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.CharacterUtils;
import org.planit.utils.misc.StringUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.MotorisationModeType;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.PredefinedModeType;
import org.planit.utils.mode.TrackModeType;
import org.planit.utils.mode.UsabilityModeFeatures;
import org.planit.utils.mode.UseOfModeType;
import org.planit.utils.mode.VehicularModeType;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.TransportLayer;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.layer.macroscopic.MacroscopicModeProperties;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.LinkSegment;
import org.planit.utils.network.layer.physical.Node;
import org.planit.xml.generated.Accessmode;
import org.planit.xml.generated.Direction;
import org.planit.xml.generated.XMLElementConfiguration;
import org.planit.xml.generated.XMLElementInfrastructureLayer;
import org.planit.xml.generated.XMLElementInfrastructureLayers;
import org.planit.xml.generated.XMLElementLayerConfiguration;
import org.planit.xml.generated.XMLElementLinkSegment;
import org.planit.xml.generated.XMLElementLinkSegmentType;
import org.planit.xml.generated.XMLElementLinks;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;
import org.planit.xml.generated.XMLElementModes;
import org.planit.xml.generated.XMLElementNodes;

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
   * @throws PlanItException thrown if error
   */
  private UsabilityModeFeatures parseUsabilityModeFeatures(org.planit.xml.generated.XMLElementModes.Mode generatedMode) throws PlanItException {
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
   * @throws PlanItException thrown if error
   */  
  private PhysicalModeFeatures parsePhysicalModeFeatures(org.planit.xml.generated.XMLElementModes.Mode generatedMode) throws PlanItException {
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
   * Reads mode types from input file, register them on the network and also populate mapping based on XML ids
   * 
   * @throws PlanItException thrown if there is a Mode value of 0 in the modes definition file
   */
  private void parseModes() throws PlanItException {   
    
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
        LOGGER.warning(String.format("mode is not registered as predefined mode but name or xmlid corresponds to PLANit predefined mode, reverting to PLANit predefined mode %s",modeType.name()));
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
      TransportLayer layer = parseNetworkLayer(xmlLayer, jtsUtils);
      
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
  private TransportLayer parseNetworkLayer(XMLElementInfrastructureLayer xmlLayer, PlanitJtsCrsUtils jtsUtils ) throws PlanItException {
    
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
        
      /* create and register */
      final MacroscopicLinkSegmentType linkSegmentType = networkLayer.getLinkSegmentTypes().getFactory().registerNew(name, capacityPcuPerHour, maximumDensityPcuPerKm);
      linkSegmentType.setXmlId(xmlId);
      linkSegmentType.setExternalId(externalId);
      
      registerBySourceId(MacroscopicLinkSegmentType.class, linkSegmentType);
            
      /* mode properties, only set when allowed, otherwise not */
      Collection<Mode> thePlanitModes = new HashSet<Mode>();            
      if(xmlLinkSegmentType.getAccess() != null) {
        List<Accessmode> xmlModes = xmlLinkSegmentType.getAccess().getMode();
        for (Accessmode xmlMode : xmlModes) {                  
          /* mode properties */
          parseLinkSegmentTypeModeProperties(xmlMode, linkSegmentType);                                 
        }          
      }else {
        /* all ROAD modes allowed */        
        thePlanitModes = getSourceIdContainer(Mode.class).toCollection().stream().filter( 
            mode -> mode.getPhysicalFeatures().getTrackType() == TrackModeType.ROAD).collect(Collectors.toSet());
        thePlanitModes.forEach( planitMode -> linkSegmentType.addModeProperties(planitMode, MacroscopicModePropertiesFactory.create(planitMode.getMaximumSpeedKmH())));
      }
    }
 
  } 
  
  /** parse the mode properties for given link segment type and populate the helper with them
  *
  * @param xmlMode to extract information from on (TODO:ugly, helper should be removed)
  * @param linkSegmentType to register mode properties on
  * @throws PlanItException thrown if error
  */
 public void parseLinkSegmentTypeModeProperties(Accessmode xmlMode, MacroscopicLinkSegmentType linkSegmentType) throws PlanItException{
   /* mode ref */
   String modeXmlRefId = xmlMode.getRef();   
   Mode thePlanitMode = getBySourceId(Mode.class, modeXmlRefId);
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
   * Create and register nodes on the network
   * 
   * return nodesByExternalIdToPopulate map for reference
   * @param xmlLayer to extract from
   * @param networkLayer to populate
   * @return parsed nodes
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
      
      /** LINK **/
      Link link = null;
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
        link = networkLayer.getLinks().getFactory().registerNew(startNode, endNode, length);
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
            
        registerBySourceId(Link.class, link);
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
        
        /** LINK SEGMENT TYPE **/
        
        /* link segment type xml id */
        String linkSegmentTypeXmlId = null;
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
          throw new PlanItException(String.format("link segment type %s, unknown, cannot be registered on link segment %s",linkSegmentTypeXmlId,linkSegment));
        }
        linkSegment.setLinkSegmentType(linkSegmentType);    
        
        isFirstLinkSegment = false;
        firstLinkDirection = abDirection;        
      }
      /** end LINK SEGMENT **/      
    }
  }  

  /** Place network to populate
   * 
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected void setNetwork(final TransportLayerNetwork<?,?> network) throws PlanItException {
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
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementMacroscopicNetwork>(XMLElementMacroscopicNetwork.class);
    this.settings = settings;
    setNetwork(new MacroscopicNetwork(idToken));
  }  
  
  /** Constructor where settings are directly provided such that input information can be exracted from it
   * 
   * @param settings to use
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitNetworkReader(PlanitNetworkReaderSettings settings, TransportLayerNetwork<?,?> network) throws PlanItException{
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
  protected PlanitNetworkReader(XMLElementMacroscopicNetwork externalXmlRawNetwork, TransportLayerNetwork<?,?> network) throws PlanItException{
    super();
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementMacroscopicNetwork>(externalXmlRawNetwork);
    this.settings = new PlanitNetworkReaderSettings();
    setNetwork(network);
  }
  
  /** Constructor
   * 
   * @param networkPathDirectory to use
   * @param xmlFileExtension to use
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitNetworkReader(String networkPathDirectory, String xmlFileExtension, TransportLayerNetwork<?,?> network) throws PlanItException{
    super();
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementMacroscopicNetwork>(XMLElementMacroscopicNetwork.class);
    this.settings = new PlanitNetworkReaderSettings(networkPathDirectory, xmlFileExtension);
    setNetwork(network);
  }  
  
  /** Default XSD files used to validate input XML files against, TODO: move to properties file */
  public static final String NETWORK_XSD_FILE = "https://trafficplanit.github.io/PLANitManual/xsd/macroscopicnetworkinput.xsd";  

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetwork read() throws PlanItException {
        
    /* parse the XML raw network to extract PLANit network from */   
    xmlParser.initialiseAndParseXmlRootElement(getSettings().getInputDirectory(), getSettings().getXmlFileExtension());
    PlanItException.throwIfNull(xmlParser.getXmlRootElement(), "No valid PLANit XML network could be parsed into memory, abort");
    
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
      
      /* free xml content */
      xmlParser.clearXmlContent();
      
    } catch (PlanItException e) {
      throw e;
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error while populating physical network in PLANitIO",e);
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
      MacroscopicLinkSegment firstMatch = layer.getLinkSegments().findFirst( ls -> externalId.equals(ls.getExternalId()));
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
