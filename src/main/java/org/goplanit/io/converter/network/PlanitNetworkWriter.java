package org.goplanit.io.converter.network;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.converter.IdMapperType;
import org.goplanit.converter.network.NetworkWriter;
import org.goplanit.io.converter.PlanitWriterImpl;
import org.goplanit.io.xml.util.EnumConversionUtil;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.TransportLayerNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.Modes;
import org.goplanit.utils.mode.PhysicalModeFeatures;
import org.goplanit.utils.mode.UsabilityModeFeatures;
import org.goplanit.utils.network.layer.TransportLayer;
import org.goplanit.utils.network.layer.macroscopic.AccessGroupProperties;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypes;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.Links;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.Nodes;
import org.goplanit.xml.generated.Direction;
import org.goplanit.xml.generated.LengthUnit;
import org.goplanit.xml.generated.XMLElementAccessGroup;
import org.goplanit.xml.generated.XMLElementConfiguration;
import org.goplanit.xml.generated.XMLElementInfrastructureLayer;
import org.goplanit.xml.generated.XMLElementInfrastructureLayers;
import org.goplanit.xml.generated.XMLElementLayerConfiguration;
import org.goplanit.xml.generated.XMLElementLinkLengthType;
import org.goplanit.xml.generated.XMLElementLinkSegment;
import org.goplanit.xml.generated.XMLElementLinkSegmentType;
import org.goplanit.xml.generated.XMLElementLinkSegmentTypes;
import org.goplanit.xml.generated.XMLElementLinks;
import org.goplanit.xml.generated.XMLElementMacroscopicNetwork;
import org.goplanit.xml.generated.XMLElementModes;
import org.goplanit.xml.generated.XMLElementNodes;
import org.goplanit.xml.generated.XMLElementPhysicalFeatures;
import org.goplanit.xml.generated.XMLElementUsabilityFeatures;
import org.goplanit.xml.generated.XMLElementLinkSegmentType.Access;

/**
 * Writer to persist a PLANit network to disk in the native PLANit format. By default the xml ids are used for writing out the ids in the XML. 
 * 
 * @author markr
 *
 */
public class PlanitNetworkWriter extends PlanitWriterImpl<TransportLayerNetwork<?,?>> implements NetworkWriter {  
 
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkWriter.class.getCanonicalName());
                 
  /** XML memory model equivalent of the PLANit memory mode */
  private final XMLElementMacroscopicNetwork xmlRawNetwork;
  
  /** network writer settings to use */
  private final PlanitNetworkWriterSettings settings;
   
    
  /** Get the reference to use whenever a mode reference is encountered
   * 
   * @param mode to collect reference for
   * @return modeReference for the mode
   */
  private String getXmlModeReference(Mode mode) {
    /* Xml id */
    if(mode.isPredefinedModeType()) {
      /* predefined modes, must utilise, their predefined Xml id/name, this overrules the mapper (if any) */
      return mode.getXmlId();  
    }else {
      return getModeIdMapper().apply(mode);
    }
  }  
  
  /**
   * populate a single xml link segment element based on the passed in PLANit link segment
   * 
   * @param xmlLinkSegment to populate
   * @param linkSegment the PLANit link segment instance to populate from
   */
  private void populateLinkSegment(XMLElementLinkSegment xmlLinkSegment, MacroscopicLinkSegment linkSegment) {   
    /* id */
    xmlLinkSegment.setId(getLinkSegmentIdMapper().apply(linkSegment));
    /* max speed */
    xmlLinkSegment.setMaxspeed(linkSegment.getPhysicalSpeedLimitKmH());
    /* number of lanes */
    xmlLinkSegment.setNumberoflanes(BigInteger.valueOf(linkSegment.getNumberOfLanes()));
    if(!linkSegment.hasLinkSegmentType()) {
      LOGGER.severe(String.format("missing link segment type on link segment %s (id:%d)", linkSegment.getExternalId(), linkSegment.getId()));      
    }else {
      xmlLinkSegment.setTyperef(getLinkSegmentTypeIdMapper().apply(linkSegment.getLinkSegmentType()));  
    }
  }  
  
  /** populate a link's link segments
   * 
   * @param xmlLink to populate link segments on
   * @param link to populate link segments from
   */
  private void populateLinkSegments(XMLElementLinks.Link xmlLink, Link link) {
    List<XMLElementLinkSegment> xmlLinkSegments = xmlLink.getLinksegment();
    
    if(link.hasLinkSegmentAb()) {      
      XMLElementLinkSegment xmlLinkSegment = new XMLElementLinkSegment();
      /* direction A->B */
      xmlLinkSegment.setDir(Direction.A_B);
      populateLinkSegment(xmlLinkSegment, link.getLinkSegmentAb());
      xmlLinkSegments.add(xmlLinkSegment);
    }
    if(link.hasLinkSegmentBa()) {
      XMLElementLinkSegment xmlLinkSegment = new XMLElementLinkSegment();
      /* direction B->A */
      xmlLinkSegment.setDir(Direction.B_A);
      populateLinkSegment(xmlLinkSegment, link.getLinkSegmentBa());
      xmlLinkSegments.add(xmlLinkSegment);
    }
    
    if(xmlLinkSegments==null && (link.hasLinkSegmentAb() || link.hasLinkSegmentBa())) {
      LOGGER.severe(String.format("link %s (id:%d) has no xm Link segment element, but does have link segments",link.getExternalId(), link.getId()));
    }    
  }
  
  /**
   * Populate the xml /<link/> element
   *  
   * @param xmlLinkList to add link to 
   * @param link to populate from
   * @throws PlanItException thrown if error
   */
  private void populateXmlLink(List<XMLElementLinks.Link> xmlLinkList, final Link link) throws PlanItException {
    XMLElementLinks.Link xmlLink = new XMLElementLinks.Link();
    
    /* XML id */
    xmlLink.setId(getLinkIdMapper().apply(link));
    
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
    xmlLink.setNodearef(getVertexIdMapper().apply(link.getNodeA()));
    /* node B ref */
    xmlLink.setNodebref(getVertexIdMapper().apply(link.getNodeB()));    
    
    /* line string */
    xmlLink.setLineString(createGmlLineStringType(link.getGeometry()));
        
    /* link segments */
    populateLinkSegments(xmlLink, link);
    
    xmlLinkList.add(xmlLink);
  }
  
  
  /**
   * Populate the xml /<links/> element
   * 
   * @param xmlNetworkLayer to populate on
   * @param links to populate from
   * @throws PlanItException thrown if error
   */
  private void populateXmlLinks(final XMLElementInfrastructureLayer xmlNetworkLayer, final Links links) throws PlanItException {
    XMLElementLinks xmlLinks = xmlNetworkLayer.getLinks(); 
    if(xmlLinks == null) {
      xmlLinks = new XMLElementLinks();
      xmlNetworkLayer.setLinks(xmlLinks);
    }
    
    /* link */
    final List<XMLElementLinks.Link> xmlLinkList = xmlLinks.getLink();    
    for(Link link: links) {
      populateXmlLink(xmlLinkList, link);
    }
  } 
  
  /**
   *  Populate the xml /<node/> element
   * 
   * @param xmlNodeList to add node to
   * @param node to populate from
   */  
  private void populateXmlNode(final List<XMLElementNodes.Node> xmlNodeList, final Node node) {
    XMLElementNodes.Node xmlNode = new XMLElementNodes.Node();
    xmlNodeList.add(xmlNode);
    
    /* Xml id */
    xmlNode.setId(getVertexIdMapper().apply(node));
    
    /* external id */
    if(node.hasExternalId()) {
      xmlNode.setExternalid(node.getExternalId());
    }    

    /* name */
    xmlNode.setName(node.getName());
    
    /* location */
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
    final List<XMLElementNodes.Node> xmlNodeList = xmlNodes.getNode();    
    nodes.forEach( node -> populateXmlNode(xmlNodeList, node));    
  }
  
  /**
   *  Populate the xml link segment type modes' /<mode/> element containing the mode specific properties of 
   *  this link segment type
   *  
   * @param xmlAccess to add mode access to 
   * @param accessGroupProperties to populate from
   */   
  private void populateLinkSegmentTypeAccessGroupProperties(Access xmlAccess, AccessGroupProperties accessGroupProperties) {
    List<XMLElementAccessGroup> accessGroupList = xmlAccess.getAccessgroup();
    XMLElementAccessGroup xmlAccessGroup = new XMLElementAccessGroup();
    
    /* mode ref id */
    Set<Mode> accessModes = accessGroupProperties.getAccessModes();
    String modeRefs = accessModes.stream().map(mode -> getXmlModeReference(mode)).collect(Collectors.joining(","));
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
  private void populateXmlLinkSegmentType(List<XMLElementLinkSegmentType> xmlLinkSegmentTypeList, MacroscopicLinkSegmentType linkSegmentType) {
    XMLElementLinkSegmentType xmlLinkSegmentType = new XMLElementLinkSegmentType();
    
    /* Xml id */
    xmlLinkSegmentType.setId(getLinkSegmentTypeIdMapper().apply(linkSegmentType));
    
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
    XMLElementLinkSegmentType.Access xmlTypeAccess = xmlLinkSegmentType.getAccess();
    if(xmlTypeAccess == null) {
      xmlTypeAccess = new XMLElementLinkSegmentType.Access();
      xmlLinkSegmentType.setAccess(xmlTypeAccess);
    }

    /* only apply once per access properties since it may be referenced by multiple modes */
    Set<Mode> processedModes = new TreeSet<Mode>();
    for(Mode accessMode : linkSegmentType.getAllowedModes()) {
      if(!processedModes.contains(accessMode)) {
        AccessGroupProperties accessProperties = linkSegmentType.getAccessProperties(accessMode);
        processedModes.addAll(accessProperties.getAccessModes());
        populateLinkSegmentTypeAccessGroupProperties(xmlTypeAccess, accessProperties);
      }
    }
        
    xmlLinkSegmentTypeList.add(xmlLinkSegmentType);
  }   
  
  /** Populate the XML </linksegmenttypes/> element
   * 
   * @param xmlLayerConfiguration to populate on
   * @param linkSegmentTypes to populate from
   */
  private void populateXmlLinkSegmentTypes(XMLElementLayerConfiguration xmlLayerConfiguration, MacroscopicLinkSegmentTypes linkSegmentTypes) {
    XMLElementLinkSegmentTypes xmlLinkSegmentTypes = xmlLayerConfiguration.getLinksegmenttypes();
    if(xmlLinkSegmentTypes == null) {
      xmlLinkSegmentTypes = new XMLElementLinkSegmentTypes();
      xmlLayerConfiguration.setLinksegmenttypes(xmlLinkSegmentTypes);
    }
    
    /* link segment type */
    List<XMLElementLinkSegmentType> xmlLinkSegmentTypeList = xmlLinkSegmentTypes.getLinksegmenttype();
    linkSegmentTypes.forEach( linkSegmentType -> populateXmlLinkSegmentType(xmlLinkSegmentTypeList, linkSegmentType));
  }  
  
  /** Populate the xml </physicalfeatures/> element
   * 
   * @param xmlMode the mode to populate
   * @param physicalModeFeatures to populate from
   */  
  private void populateModePhysicalFeatures(XMLElementModes.Mode xmlMode, PhysicalModeFeatures physicalModeFeatures) {   
    XMLElementPhysicalFeatures xmlPhysicalFeatures = xmlMode.getPhysicalfeatures();
    if(xmlPhysicalFeatures == null) {
      xmlPhysicalFeatures = new XMLElementPhysicalFeatures();
      xmlMode.setPhysicalfeatures(xmlPhysicalFeatures);
    }
    
    try {
      /* motorisation type */
      xmlPhysicalFeatures.setMotorisationtype(EnumConversionUtil.planitToXml(physicalModeFeatures.getMotorisationType()));
      /* track type */
      xmlPhysicalFeatures.setTracktype(EnumConversionUtil.planitToXml(physicalModeFeatures.getTrackType()));
      /* vehicle type */
      xmlPhysicalFeatures.setVehicletype(EnumConversionUtil.planitToXml(physicalModeFeatures.getVehicularType()));
    }catch(PlanItException e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("unable to set physical features on mode properties");
    }
    
  }  
  
  /** Populate the xml </usability features/> element
   * 
   * @param xmlMode the mode to populate
   * @param usabilityModeFeatures to populate from
   */    
  private void populateModeUsabilityFeatures(XMLElementModes.Mode xmlMode, UsabilityModeFeatures usabilityModeFeatures) {
    XMLElementUsabilityFeatures xmlUseFeatures = xmlMode.getUsabilityfeatures();
    if(xmlUseFeatures == null) {
      xmlUseFeatures = new XMLElementUsabilityFeatures();
      xmlMode.setUsabilityfeatures(xmlUseFeatures);
    }
    
    try {
      /* motorisation type */
      xmlUseFeatures.setUsedtotype(EnumConversionUtil.planitToXml(usabilityModeFeatures.getUseOfType()));

    }catch(PlanItException e) {
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
  private void populateXmlMode(List<XMLElementModes.Mode> xmlModesList, Mode mode) {
    XMLElementModes.Mode xmlMode = new XMLElementModes.Mode();
    
    /* Xml id */
    xmlMode.setId(getXmlModeReference(mode));
    
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
    List<XMLElementModes.Mode> xmlModesList = xmlModes.getMode();
    modes.forEach( mode -> populateXmlMode(xmlModesList, mode));        
  }          

  /** Populate the XML id of the XML network element
   * 
   * @param network to extract XML id from
   */
  private void populateXmlId(MacroscopicNetwork network) {
    /* xml id */    
    if(!network.hasXmlId()) {
      LOGGER.warning(String.format("Network has no XML id defined, adopting internally generated id %d instead",network.getId()));
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
  protected void populateXmlLayerConfiguration(XMLElementInfrastructureLayer xmlNetworkLayer, MacroscopicLinkSegmentTypes linkSegmentTypes) {
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
   * @throws PlanItException thrown if error
   */
  protected void populateXmlNetworkLayer(XMLElementInfrastructureLayers xmlInfrastructureLayers, MacroscopicNetworkLayerImpl physicalNetworkLayer) throws PlanItException {
    XMLElementInfrastructureLayer xmlNetworkLayer = new XMLElementInfrastructureLayer();
    xmlInfrastructureLayers.getLayer().add(xmlNetworkLayer); 
    
    /* layer configuration */
    populateXmlLayerConfiguration(xmlNetworkLayer, physicalNetworkLayer.linkSegmentTypes);

    /* links */
    populateXmlLinks(xmlNetworkLayer, physicalNetworkLayer.getLinks());
        
    /* nodes */
    populateXmlNodes(xmlNetworkLayer, physicalNetworkLayer.getNodes());      
  }  
  
  /** Populate the available network layers
   * 
   * @param network to extract layers from and populate xml
   * @throws PlanItException thrown if error
   */
  protected void populateXmlNetworkLayers(MacroscopicNetwork network) throws PlanItException {
    
    XMLElementInfrastructureLayers xmlInfrastructureLayers = xmlRawNetwork.getInfrastructurelayers();
    if(xmlInfrastructureLayers == null) {
      xmlRawNetwork.setInfrastructurelayers(new XMLElementInfrastructureLayers());
      xmlInfrastructureLayers = xmlRawNetwork.getInfrastructurelayers();
    }
    
    /* srs name */
    xmlInfrastructureLayers.setSrsname(extractSrsName(getSettings()));
    
    for(TransportLayer networkLayer : network.getTransportLayers()) {
      if(networkLayer instanceof MacroscopicNetworkLayerImpl) {
        MacroscopicNetworkLayerImpl physicalNetworkLayer = ((MacroscopicNetworkLayerImpl)networkLayer);
        populateXmlNetworkLayer(xmlInfrastructureLayers, physicalNetworkLayer);
      }else {
        LOGGER.severe(String.format("unsupported macroscopic infrastructure layer %s encountered", networkLayer.getXmlId()));
      }
    }
  }  
  

  /** default network file name to use */
  public static final String DEFAULT_NETWORK_FILE_NAME = "network.xml";
  
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
  protected PlanitNetworkWriter(String networkPath, String countryName, XMLElementMacroscopicNetwork xmlRawNetwork) {
    super(IdMapperType.XML);
    this.settings = new PlanitNetworkWriterSettings(networkPath, DEFAULT_NETWORK_FILE_NAME, countryName);
    this.xmlRawNetwork = xmlRawNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(TransportLayerNetwork<?,?> network) throws PlanItException {
    
    /* currently we only support macroscopic infrastructure networks */
    if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItException("currently the PLANit network reader only supports macroscopic infrastructure networks, the provided network is not of this type");
    }    
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork)network;
    
    /* initialise */
    super.initialiseIdMappingFunctions();        
    super.prepareCoordinateReferenceSystem(macroscopicNetwork.getCoordinateReferenceSystem());  
    LOGGER.info(String.format("Persisting PLANit network to: %s",Paths.get(getSettings().getOutputPathDirectory(), getSettings().getFileName()).toString()));
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
  
  /** the country name of the network to write (if any is set)
   * 
   * @return countryname, null if unknown
   */
  public String getCountryName() {
    return getSettings().getCountry();
  }  

}
