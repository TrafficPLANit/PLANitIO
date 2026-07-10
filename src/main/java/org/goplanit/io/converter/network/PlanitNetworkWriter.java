package org.goplanit.io.converter.network;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.io.xml.util.XmlEnumConversionUtil;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.converter.network.NetworkWriter;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.Modes;
import org.goplanit.utils.mode.PhysicalModeFeatures;
import org.goplanit.utils.mode.UsabilityModeFeatures;
import org.goplanit.utils.network.layer.macroscopic.*;
import org.goplanit.utils.graph.directed.BannedMovement;
import org.goplanit.utils.graph.directed.BannedMovements;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.Nodes;
import org.goplanit.xml.generated.v2.*;

/**
 * Writer to persist a PLANit network to disk in the native PLANit format. By default the xml ids are used for writing
 * out the ids in the XML.
 * 
 * @author markr
 *
 */
public class PlanitNetworkWriter extends UnTypedPlanitCrsWriterImpl<LayeredNetwork<?,?>> implements NetworkWriter {
 
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkWriter.class.getCanonicalName());
                 
  /** XML memory model equivalent of the PLANit memory mode */
  private final XMLElementMacroscopicNetwork xmlRawNetwork;
  
  /** network writer settings to use */
  private final PlanitNetworkWriterSettings settings;
  
  /* track logging prefix for current layer */
  private String currLayerLogPrefix;

  /**
   * Populate the xml /<ban/> element. If movement is not a ban, log and skip
   *
   * @param xmlTurnBanList to add movement to
   * @param bannedMovement to populate from
   */
  private void populateXmlTurnBan(
      List<org.goplanit.xml.generated.v2.BanType> xmlTurnBanList, final BannedMovement bannedMovement) {
    var xmlTurnBan = new org.goplanit.xml.generated.v2.BanType();

    /* XML id */
    xmlTurnBan.setId(getPrimaryIdMapper().getMovementIdMapper().apply(bannedMovement));

    /* external id */
    if(bannedMovement.hasExternalId()) {
      xmlTurnBan.setExternalid(bannedMovement.getExternalId());
    }

    xmlTurnBan.setFromref(getPrimaryIdMapper().getMacroscopicLinkSegmentIdMapper().apply(
        (MacroscopicLinkSegment) bannedMovement.getSegmentFrom()));
    xmlTurnBan.setToref(getPrimaryIdMapper().getMacroscopicLinkSegmentIdMapper().apply(
        (MacroscopicLinkSegment) bannedMovement.getSegmentTo()));

    /* turn bans */
    xmlTurnBanList.add(xmlTurnBan);
  }

  /** Populate the xml /<turns/> element
   *
   * @param xmlNetworkLayer to populate link segments on
   * @param bannedMovements to populate XML with
   */
  private void populateXmlTurns(XMLElementInfrastructureLayer xmlNetworkLayer, BannedMovements bannedMovements) {
    var xmlTurns = xmlNetworkLayer.getTurns();
    if(bannedMovements.isEmpty()){
      xmlNetworkLayer.setTurns(null);
      return;
    }

    if(xmlTurns == null) {
      xmlTurns = new TurnsType();
      xmlNetworkLayer.setTurns(xmlTurns);
    }

    // jaxb naming issue, should be "bans"
    final var xmlBannedTurns = xmlTurns.getBen();
    bannedMovements.streamSortedBy(getPrimaryIdMapper().getMovementIdMapper()).forEach(turnBan -> {
      populateXmlTurnBan(xmlBannedTurns, turnBan);
    });
  }

  /**
   * populate a single xml link segment element based on the passed in PLANit link segment
   * 
   * @param xmlLinkSegment to populate
   * @param linkSegment the PLANit link segment instance to populate from
   */
  private void populateLinkSegment(XMLElementLinkSegment xmlLinkSegment, MacroscopicLinkSegment linkSegment) {
    /* id */
    xmlLinkSegment.setId(getPrimaryIdMapper().getMacroscopicLinkSegmentIdMapper().apply(linkSegment));
    /* max speed */
    xmlLinkSegment.setMaxspeed(linkSegment.getPhysicalSpeedLimitKmH());
    /* number of lanes */
    xmlLinkSegment.setNumberoflanes(BigInteger.valueOf(linkSegment.getNumberOfLanes()));
    if(!linkSegment.hasLinkSegmentType()) {
      LOGGER.severe(String.format("missing link segment type on link segment %s (id:%d)",
              linkSegment.getExternalId(), linkSegment.getId()));
    }else {
      xmlLinkSegment.setTyperef(getPrimaryIdMapper().getLinkSegmentTypeIdMapper().apply(
              linkSegment.getLinkSegmentType()));
    }
  }  
  
  /** populate a link's link segments
   * 
   * @param xmlLink to populate link segments on
   * @param link to populate link segments from
   */
  private void populateLinkSegments(org.goplanit.xml.generated.v2.Link xmlLink, MacroscopicLink link) {
    List<XMLElementLinkSegment> xmlLinkSegments = xmlLink.getLinksegments();
    
    if(link.hasLinkSegmentAb()) {
      link.getLinkSegmentAb().validate();
      XMLElementLinkSegment xmlLinkSegment = new XMLElementLinkSegment();
      /* direction A->B */
      xmlLinkSegment.setDir(Direction.A_B);
      populateLinkSegment(xmlLinkSegment, link.getLinkSegmentAb());
      xmlLinkSegments.add(xmlLinkSegment);
    }
    if(link.hasLinkSegmentBa()) {
      link.getLinkSegmentBa().validate();
      XMLElementLinkSegment xmlLinkSegment = new XMLElementLinkSegment();
      /* direction B->A */
      xmlLinkSegment.setDir(Direction.B_A);
      populateLinkSegment(xmlLinkSegment, link.getLinkSegmentBa());
      xmlLinkSegments.add(xmlLinkSegment);
    }
    
    if(xmlLinkSegments==null && (link.hasLinkSegmentAb() || link.hasLinkSegmentBa())) {
      LOGGER.severe(String.format("link %s (id:%d) has no xm Link segment element, but does have link segments",
              link.getExternalId(), link.getId()));
    }    
  }
  
  /**
   * Populate the xml /<link/> element
   *  
   * @param xmlLinkList to add link to 
   * @param link to populate from
   */
  private void populateXmlLink(List<org.goplanit.xml.generated.v2.Link> xmlLinkList, final MacroscopicLink link) {
    var xmlLink = new org.goplanit.xml.generated.v2.Link();
    
    /* XML id */
    xmlLink.setId(getPrimaryIdMapper().getLinkIdMapper().apply(link));
    
    /* external id */
    if(link.hasExternalId()) {
      xmlLink.setExternalid(link.getExternalId());
    }
    
    /* length - only persist if it differs from the geographic length */
    boolean useOverrideLength = true;
    if(getGeoUtils()!= null && link.hasGeometry()) {
      double geographicLength = getGeoUtils().getDistanceInKilometres(link.getGeometry());
      if(Precision.equal(geographicLength, link.getLengthKm(), Precision.EPSILON_6)) {
        useOverrideLength = false;
      }
    }
    
    if(useOverrideLength) {
      XMLElementLinkLengthType xmlLinkLength = new XMLElementLinkLengthType();
      xmlLinkLength.setUnit(LengthUnit.KM);    
      xmlLinkLength.setValue(link.getLengthKm());
      xmlLink.setLength(xmlLinkLength);
    }
        
    if(link.hasName()) {
      /* name */
      xmlLink.setName(link.getName());      
    }
    /* node A ref */
    xmlLink.setNodearef(getPrimaryIdMapper().getVertexIdMapper().apply(link.getNodeA()));
    /* node B ref */
    xmlLink.setNodebref(getPrimaryIdMapper().getVertexIdMapper().apply(link.getNodeB()));
    
    /* line string */
    if(link.hasGeometry()) {
      xmlLink.setLineString(createGmlLineStringType(link.getGeometry()));
    }

    if(link.hasInputProperty()){
      //todo only supporting string values, no way of enforcing a type (needs to be part of schema)
      var customProperties = new CustomPropertiesType();
      xmlLink.setCustom(customProperties);
      var xmlEntryElements = customProperties.getEntryElements();
      link.getInputPropertyKeys().forEach( key ->
        {
          var xmlEntry = new org.goplanit.xml.generated.v2.EntryElement();
          xmlEntry.setKeyAttribute(key);
          xmlEntry.setValue(link.getInputProperty(key).toString());
          xmlEntryElements.add(xmlEntry);
        });
    }
        
    /* link segments */
    populateLinkSegments(xmlLink, link);
    
    xmlLinkList.add(xmlLink);
  }
  
  
  /**
   * Populate the xml /<links/> element
   * 
   * @param xmlNetworkLayer to populate on
   * @param links to populate from
   */
  private void populateXmlLinks(final XMLElementInfrastructureLayer xmlNetworkLayer, final MacroscopicLinks links) {
    XMLElementLinks xmlLinks = xmlNetworkLayer.getLinks();
    if(xmlLinks == null) {
      xmlLinks = new XMLElementLinks();
      xmlNetworkLayer.setLinks(xmlLinks);
    }
    
    /* link */
    final var xmlLinkList = xmlLinks.getLinks();
    links.streamSortedBy(getPrimaryIdMapper().getLinkIdMapper()).forEach(link -> {
      link.validate();
      populateXmlLink(xmlLinkList, link);
    });
  } 
  
  /**
   *  Populate the xml /<node/> element
   * 
   * @param xmlNodeList to add node to
   * @param node to populate from
   */  
  private void populateXmlNode(final List<org.goplanit.xml.generated.v2.Node> xmlNodeList, final Node node) {
    var xmlNode = new org.goplanit.xml.generated.v2.Node();
    xmlNodeList.add(xmlNode);
    
    /* Xml id */
    xmlNode.setId(getPrimaryIdMapper().getVertexIdMapper().apply(node));
    
    /* external id */
    if(node.hasExternalId()) {
      xmlNode.setExternalid(node.getExternalId());
    }    

    /* name */
    xmlNode.setName(node.getName());
    
    /* location */
    if(!node.hasPosition()){
      LOGGER.warning(String.format("Node (%s) without position, unable to create GML point", node.getIdsAsString()));
      return;
    }
    xmlNode.setPoint(createGmlPointType(node.getPosition()));    
  }

  /**
   * Populate the xml /<nodes/> element
   * 
   * @param xmlNetworkLayer to populate on
   * @param nodes to populate from
   */  
  private void populateXmlNodes(final XMLElementInfrastructureLayer xmlNetworkLayer, Nodes nodes) {
    XMLElementNodes xmlNodes = xmlNetworkLayer.getNodes(); 
    if(xmlNodes == null) {
      xmlNodes = new XMLElementNodes();
      xmlNetworkLayer.setNodes(xmlNodes);
    }
        
    /* node */
    final var xmlNodeList = xmlNodes.getNodes();
    nodes.streamSortedBy(getPrimaryIdMapper().getVertexIdMapper()).forEach(
            node -> populateXmlNode(xmlNodeList, node));
  }
  
  /**
   *  Populate the xml link segment type modes' /<mode/> element containing the mode specific properties of 
   *  this link segment type
   *  
   * @param xmlAccess to add mode access to 
   * @param accessGroupProperties to populate from
   */   
  private void populateLinkSegmentTypeAccessGroupProperties(
          Access xmlAccess, AccessGroupProperties accessGroupProperties) {
    var accessGroupList = xmlAccess.getAccessgroups();
    XMLElementAccessGroup xmlAccessGroup = new XMLElementAccessGroup();
    
    /* mode ref id */
    Set<Mode> accessModes = accessGroupProperties.getAccessModes();
    String modeRefs = accessModes.stream().map(mode ->
            getXmlModeReference(mode, getPrimaryIdMapper().getModeIdMapper())).sorted().collect
            (Collectors.joining(","));
    xmlAccessGroup.setModerefs(modeRefs);
    
    /* critical speed */
    if(accessGroupProperties.isCriticalSpeedKmHSet()) {
      xmlAccessGroup.setCritspeed(accessGroupProperties.getCriticalSpeedKmH());
    }
    
    /* maximum speed */
    if(accessGroupProperties.isMaximumSpeedKmHSet()) {
      xmlAccessGroup.setMaxspeed(accessGroupProperties.getMaximumSpeedKmH());
    }
    
    accessGroupList.add(xmlAccessGroup);
  }  
  
  /**
   *  Populate the xml /<linksegmenttype/> element
   *  
   * @param xmlLinkSegmentTypeList to add link segment type to 
   * @param linkSegmentType to populate from
   */  
  private void populateXmlLinkSegmentType(
          List<XMLElementLinkSegmentType> xmlLinkSegmentTypeList, MacroscopicLinkSegmentType linkSegmentType) {
    XMLElementLinkSegmentType xmlLinkSegmentType = new XMLElementLinkSegmentType();
    
    /* Xml id */
    xmlLinkSegmentType.setId(getPrimaryIdMapper().getLinkSegmentTypeIdMapper().apply(linkSegmentType));
    
    /* external id */
    if(linkSegmentType.hasExternalId()) {
      xmlLinkSegmentType.setExternalid(linkSegmentType.getExternalId());
    }
    
    /* capacity */
    if(linkSegmentType.isExplicitCapacityPerLaneSet()) {
      xmlLinkSegmentType.setCapacitylane(linkSegmentType.getExplicitCapacityPerLane());
    }
    /* max density */
    if(linkSegmentType.isExplicitMaximumDensityPerLaneSet()) {
      xmlLinkSegmentType.setMaxdensitylane(linkSegmentType.getExplicitMaximumDensityPerLane());
    }
    /* name */
    xmlLinkSegmentType.setName(linkSegmentType.getName());
    
    /* mode properties */
    var xmlTypeAccess = xmlLinkSegmentType.getAccess();
    if(xmlTypeAccess == null) {
      xmlTypeAccess = new org.goplanit.xml.generated.v2.Access();
      xmlLinkSegmentType.setAccess(xmlTypeAccess);
    }

    /* only apply once per access properties since it may be referenced by multiple modes */
    Set<Mode> processedModes = new TreeSet<>();
    final var finalXmlTypeAccess = xmlTypeAccess;
    linkSegmentType.getAllowedModes().stream().sorted(Comparator.comparing(Mode::getXmlId)).forEach( accessMode -> {
      if(!processedModes.contains(accessMode)) {
        AccessGroupProperties accessProperties = linkSegmentType.getAccessProperties(accessMode);
        processedModes.addAll(accessProperties.getAccessModes());
        populateLinkSegmentTypeAccessGroupProperties(finalXmlTypeAccess, accessProperties);
      }
    });
        
    xmlLinkSegmentTypeList.add(xmlLinkSegmentType);
  }   
  
  /** Populate the XML </linksegmenttypes/> element
   * 
   * @param xmlLayerConfiguration to populate on
   * @param linkSegmentTypes to populate from
   */
  private void populateXmlLinkSegmentTypes(
          XMLElementLayerConfiguration xmlLayerConfiguration, MacroscopicLinkSegmentTypes linkSegmentTypes) {
    XMLElementLinkSegmentTypes xmlLinkSegmentTypes = xmlLayerConfiguration.getLinksegmenttypes();
    if(xmlLinkSegmentTypes == null) {
      xmlLinkSegmentTypes = new XMLElementLinkSegmentTypes();
      xmlLayerConfiguration.setLinksegmenttypes(xmlLinkSegmentTypes);
    }
    
    /* link segment type */
    var xmlLinkSegmentTypeList = xmlLinkSegmentTypes.getLinksegmenttypes();
    linkSegmentTypes.streamSortedBy(getPrimaryIdMapper().getLinkSegmentTypeIdMapper()).forEach(
        linkSegmentType -> populateXmlLinkSegmentType(xmlLinkSegmentTypeList, linkSegmentType));
  }  
  
  /** Populate the xml </physicalfeatures/> element
   * 
   * @param xmlMode the mode to populate
   * @param physicalModeFeatures to populate from
   */  
  private void populateModePhysicalFeatures(
          org.goplanit.xml.generated.v2.Mode xmlMode, PhysicalModeFeatures physicalModeFeatures) {
    XMLElementPhysicalFeatures xmlPhysicalFeatures = xmlMode.getPhysicalfeatures();
    if(xmlPhysicalFeatures == null) {
      xmlPhysicalFeatures = new XMLElementPhysicalFeatures();
      xmlMode.setPhysicalfeatures(xmlPhysicalFeatures);
    }
    
    try {
      /* motorisation type */
      xmlPhysicalFeatures.setMotorisationtype(
              XmlEnumConversionUtil.planitToXml(physicalModeFeatures.getMotorisationType()));
      /* track type */
      xmlPhysicalFeatures.setTracktype(XmlEnumConversionUtil.planitToXml(physicalModeFeatures.getTrackType()));
      /* vehicle type */
      xmlPhysicalFeatures.setVehicletype(XmlEnumConversionUtil.planitToXml(physicalModeFeatures.getVehicularType()));
    }catch(PlanItRunTimeException e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("unable to set physical features on mode properties");
    }
    
  }  
  
  /** Populate the xml </usability features/> element
   * 
   * @param xmlMode the mode to populate
   * @param usabilityModeFeatures to populate from
   */    
  private void populateModeUsabilityFeatures(
          org.goplanit.xml.generated.v2.Mode xmlMode, UsabilityModeFeatures usabilityModeFeatures) {
    XMLElementUsabilityFeatures xmlUseFeatures = xmlMode.getUsabilityfeatures();
    if(xmlUseFeatures == null) {
      xmlUseFeatures = new XMLElementUsabilityFeatures();
      xmlMode.setUsabilityfeatures(xmlUseFeatures);
    }
    
    try {
      /* motorisation type */
      xmlUseFeatures.setUsedtotype(XmlEnumConversionUtil.planitToXml(usabilityModeFeatures.getUseOfType()));
    }catch(PlanItRunTimeException e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("unable to set physical features on mode properties");
    }
  }  
  
  /**
   *  populate the xml /<mode/> element
   *  
   * @param xmlModesList to add mode to 
   * @param mode to populate from
   */    
  private void populateXmlMode(List<org.goplanit.xml.generated.v2.Mode> xmlModesList, Mode mode) {
    var xmlMode = new org.goplanit.xml.generated.v2.Mode();
    
    /* Xml id */
    xmlMode.setId(getXmlModeReference(mode, getPrimaryIdMapper().getModeIdMapper()));
    
    /* external id */
    if(mode.hasExternalId()) {
      xmlMode.setExternalid(mode.getExternalId());
    }
    
    /* max speed */
    xmlMode.setMaxspeed(mode.getMaximumSpeedKmH());
    
    /* name */
    if(mode.hasName()) {
      xmlMode.setName(mode.getName());
    }
    
    /* pcu */
    xmlMode.setPcu(mode.getPcu());
    
    /* predefined */
    xmlMode.setPredefined(mode.isPredefinedModeType());
    
    /* physical features */
    if(mode.hasPhysicalFeatures()) {
      populateModePhysicalFeatures(xmlMode, mode.getPhysicalFeatures()); 
    }
    
    /* usability features */
    if(mode.hasUseFeatures()) {
      populateModeUsabilityFeatures(xmlMode, mode.getUseFeatures());
    }
        
    xmlModesList.add(xmlMode);
  }  

  /** Populate the xml </modes/> element
   * 
   * @param modes to populate from
   */
  private void populateXmlModes(Modes modes) {
    XMLElementModes xmlModes = xmlRawNetwork.getConfiguration().getModes();
    if(xmlModes == null) {
      xmlModes = new XMLElementModes();
      xmlRawNetwork.getConfiguration().setModes(xmlModes);
    }
    
    /* modes*/
    var xmlModesList = xmlModes.getModes();
    modes.stream().sorted(Comparator.comparing(getPrimaryIdMapper().getModeIdMapper())).forEach(
            mode -> populateXmlMode(xmlModesList, mode));
  }          

  /** Populate the XML id of the XML network element
   * 
   * @param network to extract XML id from
   */
  private void populateXmlId(MacroscopicNetwork network) {
    /* xml id */    
    if(!network.hasXmlId()) {
      LOGGER.warning(String.format("Network has no XML id defined, adopting internally generated id %d instead",
              network.getId()));
      network.setXmlId(String.valueOf(network.getId()));
    }
    xmlRawNetwork.setId(network.getXmlId());
  }

  /**
   * Populate the link configuration for this network, i.e., the modes
   * 
   * @param modes to use to populate the XML elements
   */
  protected void populateXmlConfiguration(Modes modes) {
    XMLElementConfiguration xmlConfiguration = xmlRawNetwork.getConfiguration();
    if(xmlConfiguration == null) {
      xmlConfiguration = new XMLElementConfiguration();
      xmlRawNetwork.setConfiguration(xmlConfiguration);
    }
    
    /* modes */
    populateXmlModes(modes);

  }  
  
  /**
   * Populate the layer configuration for this network, i.e., link segment types
   * 
   * @param xmlNetworkLayer to add types to
   * @param linkSegmentTypes to use to populate the XML elements
   */
  protected void populateXmlLayerConfiguration(
          XMLElementInfrastructureLayer xmlNetworkLayer, MacroscopicLinkSegmentTypes linkSegmentTypes) {
    XMLElementLayerConfiguration xmlLayerConfiguration = xmlNetworkLayer.getLayerconfiguration();
    if(xmlLayerConfiguration == null) {
      xmlLayerConfiguration = new XMLElementLayerConfiguration();
      xmlNetworkLayer.setLayerconfiguration(xmlLayerConfiguration);
    }
    
    /* link segment types */
    populateXmlLinkSegmentTypes(xmlLayerConfiguration, linkSegmentTypes);
  }     
  
  /**
   * Populate the network layer
   * 
   * @param xmlInfrastructureLayers to add xml layer to 
   * @param physicalNetworkLayer to populate from
   * @param network to extract from
     */
  protected void populateXmlNetworkLayer(
          final XMLElementInfrastructureLayers xmlInfrastructureLayers,
          MacroscopicNetworkLayerImpl physicalNetworkLayer,
          MacroscopicNetwork network) {

    XMLElementInfrastructureLayer xmlNetworkLayer = new XMLElementInfrastructureLayer();
    xmlInfrastructureLayers.getLayers().add(xmlNetworkLayer);
    
    /* XML id */
    xmlNetworkLayer.setId(physicalNetworkLayer.getXmlId());
    
    /* External id */
    if(physicalNetworkLayer.hasExternalId()) {
      xmlNetworkLayer.setExternalid(physicalNetworkLayer.getExternalId());
    }
    
    /* supported modes */
    if(!physicalNetworkLayer.hasSupportedModes()) {
      LOGGER.severe(String.format("%s Network layer has no supported modes, skip persistence",currLayerLogPrefix));
      return;
    }
    
    String xmlModesStr = physicalNetworkLayer.getSupportedModes().stream().map(
        m -> getPrimaryIdMapper().getModeIdMapper().apply(m)).sorted().collect(Collectors.joining(","));
    LOGGER.info(String.format("%s Supported modes: %s", currLayerLogPrefix, xmlModesStr));
    if(network.getTransportLayers().size()>1) {      
      xmlNetworkLayer.setModes(xmlModesStr);
    }
        
    /* layer configuration */    
    LOGGER.info(String.format("%s Link segment types: %d",
            currLayerLogPrefix, physicalNetworkLayer.linkSegmentTypes.size()));
    populateXmlLayerConfiguration(xmlNetworkLayer, physicalNetworkLayer.linkSegmentTypes);

    /* links */
    LOGGER.info(String.format("%s Links: %d", currLayerLogPrefix, physicalNetworkLayer.getLinks().size()));
    LOGGER.info(String.format("%s Link segments: %d",
            currLayerLogPrefix, physicalNetworkLayer.getLinkSegments().size()));
    populateXmlLinks(xmlNetworkLayer, physicalNetworkLayer.getLinks());
        
    /* nodes */
    LOGGER.info(String.format("%s Nodes: %d", currLayerLogPrefix, physicalNetworkLayer.getNodes().size()));
    populateXmlNodes(xmlNetworkLayer, physicalNetworkLayer.getNodes());

    /* restricted movements */
    LOGGER.info(String.format("%s Banned movements: %d", currLayerLogPrefix,
        physicalNetworkLayer.getBannedMovements().size()));
    populateXmlTurns(xmlNetworkLayer, physicalNetworkLayer.getBannedMovements());
  }


  /** Populate the available network layers
   * 
   * @param network to extract layers from and populate xml
   */
  protected void populateXmlNetworkLayers(MacroscopicNetwork network) {
    
    XMLElementInfrastructureLayers xmlInfrastructureLayers = xmlRawNetwork.getInfrastructurelayers();
    if(xmlInfrastructureLayers == null) {
      xmlRawNetwork.setInfrastructurelayers(new XMLElementInfrastructureLayers());
      xmlInfrastructureLayers = xmlRawNetwork.getInfrastructurelayers();
    }
    
    /* srs name */
    xmlInfrastructureLayers.setSrsname(extractSrsName(getDestinationCoordinateReferenceSystem()));
    
    LOGGER.info("Network layers:" + network.getTransportLayers().size());
    final var finalXmlInfrastructureLayers = xmlInfrastructureLayers;
    network.getTransportLayers().streamSortedBy(getPrimaryIdMapper().getNetworkLayerIdMapper()).forEach(layer -> {
      if(layer instanceof MacroscopicNetworkLayerImpl) {
        MacroscopicNetworkLayerImpl physicalNetworkLayer = ((MacroscopicNetworkLayerImpl)layer);
        
        /* XML id */
        if(physicalNetworkLayer.getXmlId() == null) {
          LOGGER.warning(String.format("Network layer has no XML id defined, adopting internally generated id %d " +
                  "instead",physicalNetworkLayer.getId()));
          physicalNetworkLayer.setXmlId(String.valueOf(physicalNetworkLayer.getId()));
        }
        this.currLayerLogPrefix = LoggingUtils.surroundWithBrackets(
                "layer: "+ getPrimaryIdMapper().getNetworkLayerIdMapper().apply(physicalNetworkLayer));
                        
        populateXmlNetworkLayer(finalXmlInfrastructureLayers, physicalNetworkLayer, network);
      }else {
        LOGGER.severe(String.format("Unsupported macroscopic infrastructure layer %s encountered",
                getPrimaryIdMapper().getNetworkLayerIdMapper().apply(layer)));
      }
    });
  }

  /** Constructor 
   * 
   * @param xmlRawNetwork to populate with PLANit network when persisting
   */
  protected PlanitNetworkWriter(XMLElementMacroscopicNetwork xmlRawNetwork) {
    this(null, CountryNames.GLOBAL, xmlRawNetwork);
  }

  /** Constructor 
   * 
   * @param networkPath to persist network on
   * @param xmlRawNetwork to populate with PLANit network when persisting
   */
  protected PlanitNetworkWriter(String networkPath, XMLElementMacroscopicNetwork xmlRawNetwork) {
    this(networkPath, CountryNames.GLOBAL, xmlRawNetwork);
  }
    
  /** Constructor 
   * 
   * @param networkPath to persist network on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   * @param xmlRawNetwork to populate with PLANit network when persisting
   */
  protected PlanitNetworkWriter(
          String networkPath, String countryName, XMLElementMacroscopicNetwork xmlRawNetwork) {
    this(new PlanitNetworkWriterSettings(
            networkPath, PlanitNetworkWriterSettings.DEFAULT_NETWORK_XML, countryName), xmlRawNetwork);
  }

  /** Constructor
   *
   * @param settings to use
   * @param xmlRawNetwork to populate with PLANit network when persisting
   */
  protected PlanitNetworkWriter(PlanitNetworkWriterSettings settings, XMLElementMacroscopicNetwork xmlRawNetwork) {
    super(IdMapperType.XML);
    this.settings = settings;
    this.xmlRawNetwork = xmlRawNetwork;
  }

  /**
   * @return network id mapper
   */
  @Override
  public NetworkIdMapper getPrimaryIdMapper() {
    return getComponentIdMappers().getNetworkIdMappers();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(LayeredNetwork<?,?> network)  {
    
    /* currently we only support macroscopic infrastructure networks */
    if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItRunTimeException("Currently the PLANit network writer only supports macroscopic " +
              "infrastructure networks, the provided network is not of this type");
    }    
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork)network;
    
    /* initialise */
    getComponentIdMappers().populateMissingIdMappers(getIdMapperType());
    prepareCoordinateReferenceSystem(
            macroscopicNetwork.getCoordinateReferenceSystem(),
            getSettings().getDestinationCoordinateReferenceSystem(), getSettings().getCountry());
    LOGGER.info(String.format("Persisting PLANit network to: %s",
            Paths.get(getSettings().getOutputDirectory(), getSettings().getFileName()).toString()));
    getSettings().logSettings();
    
    /* xml id */
    populateXmlId(macroscopicNetwork);
    
    /* general configuration */
    populateXmlConfiguration(network.getModes());
    
    /* network layers */
    populateXmlNetworkLayers(macroscopicNetwork);
    
    /* persist */
    super.persist(xmlRawNetwork, XMLElementMacroscopicNetwork.class, PlanitSchema.MACROSCOPIC_NETWORK_XSD);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    currLayerLogPrefix = null;
    xmlRawNetwork.setConfiguration(null);
    xmlRawNetwork.setInfrastructurelayers(null);
  }  
  
  // GETTERS/SETTERS
  
  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitNetworkWriterSettings getSettings() {
    return this.settings;
  }

}
