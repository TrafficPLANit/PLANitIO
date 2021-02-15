package org.planit.io.converter.network;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import org.planit.utils.network.physical.*;
import org.planit.utils.network.physical.macroscopic.*;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.planit.converter.BaseWriterImpl;
import org.planit.converter.IdMapperFunctionFactory;
import org.planit.converter.IdMapperType;
import org.planit.converter.network.NetworkWriter;
import org.planit.geo.PlanitOpenGisUtils;
import org.planit.io.xml.util.EnumConversionUtil;
import org.planit.io.xml.util.JAXBUtils;
import org.planit.io.xml.util.PlanitSchema;
import org.planit.network.InfrastructureLayer;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.macroscopic.MacroscopicNetwork;
import org.planit.network.macroscopic.physical.MacroscopicPhysicalNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsUtils;
import org.planit.utils.locale.CountryNames;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.Modes;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.UsabilityModeFeatures;
import org.planit.xml.generated.Accessmode;
import org.planit.xml.generated.Direction;
import org.planit.xml.generated.LengthUnit;
import org.planit.xml.generated.XMLElementConfiguration;
import org.planit.xml.generated.XMLElementInfrastructureLayer;
import org.planit.xml.generated.XMLElementInfrastructureLayers;
import org.planit.xml.generated.XMLElementLayerConfiguration;
import org.planit.xml.generated.XMLElementLinkLengthType;
import org.planit.xml.generated.XMLElementLinkSegment;
import org.planit.xml.generated.XMLElementLinkSegmentType;
import org.planit.xml.generated.XMLElementLinkSegmentTypes;
import org.planit.xml.generated.XMLElementLinks;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;
import org.planit.xml.generated.XMLElementModes;
import org.planit.xml.generated.XMLElementNodes;
import org.planit.xml.generated.XMLElementPhysicalFeatures;
import org.planit.xml.generated.XMLElementUsabilityFeatures;

import net.opengis.gml.CoordType;
import net.opengis.gml.CoordinatesType;
import net.opengis.gml.LineStringType;
import net.opengis.gml.PointType;

/**
 * Writer to persist a PLANit network to disk in the native PLANit format. By default the xml ids are used for writing out the ids in the XML. 
 * 
 * @author markr
 *
 */
public class PlanitNetworkWriter extends BaseWriterImpl<InfrastructureNetwork> implements NetworkWriter {  
 
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkWriter.class.getCanonicalName());
  
  /** user configurable settings for the writer */
  protected final PlanitNetworkWriterSettings settings = new PlanitNetworkWriterSettings();
    
  /** id mapper for nodes */
  private Function<Node, String> nodeIdMapper;
  /** id mapper for links */
  private Function<Link, String> linkIdMapper;
  /** id mapper for link segments */
  private Function<MacroscopicLinkSegment, String> linkSegmentIdMapper;
  /** id mapper for link segment types */
  private Function<MacroscopicLinkSegmentType, String> linkSegmentTypeIdMapper;
  /** id mapper for link segment types */
  private Function<Mode, String> modeIdMapper;  
    
  /** network path (file) to persist to */
  private final String networkPath;
  
  /** the network XML file name */
  private String networkFileName = DEFAULT_NETWORK_FILE_NAME;
    
  /** XML memory model equivalent of the PLANit memory mode */
  private final XMLElementMacroscopicNetwork xmlRawNetwork;
  
  /** when the destination CRS differs from the network CRS all geometries require transforming, for which this transformer will be initialised */
  private MathTransform destinationCrsTransformer = null;  
    
  /** get the reference to use whenever a mode reference is encountered
   * @param mode to collect reference for
   * @return modeReference
   */
  private String getXmlModeReference(Mode mode) {
    /* Xml id */
    if(mode.isPredefinedModeType()) {
      /* predefined modes, must utilise, their predefined Xml id/name, this overrules the mapper (if any) */
      return mode.getXmlId();  
    }else {
      return modeIdMapper.apply(mode);
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
    xmlLinkSegment.setId(linkSegmentIdMapper.apply(linkSegment));
    /* max speed */
    xmlLinkSegment.setMaxspeed(linkSegment.getPhysicalSpeedLimitKmH());
    /* number of lanes */
    xmlLinkSegment.setNumberoflanes(BigInteger.valueOf(linkSegment.getNumberOfLanes()));
    if(!linkSegment.hasLinkSegmentType()) {
      LOGGER.severe(String.format("missing link segment type on link segment %s (id:%d)", linkSegment.getExternalId(), linkSegment.getId()));      
    }else {
      xmlLinkSegment.setTyperef(linkSegmentTypeIdMapper.apply(linkSegment.getLinkSegmentType()));  
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
   *  populate the xml /<link/> element
   *  
   * @param xmlLinkList to add link to 
   * @param link to populate from
   */
  private void populateXmlLink(List<XMLElementLinks.Link> xmlLinkList, final Link link) {
    XMLElementLinks.Link xmlLink = new XMLElementLinks.Link();
    
    /* XML id */
    xmlLink.setId(linkIdMapper.apply(link));
    
    /* external id */
    if(link.hasExternalId()) {
      xmlLink.setExternalid(link.getExternalId());
    }
    
    /* length */
    XMLElementLinkLengthType xmlLinkLength = new XMLElementLinkLengthType();     
    xmlLinkLength.setUnit(LengthUnit.KM);
    xmlLinkLength.setValue(link.getLengthKm());
    xmlLink.setLength(xmlLinkLength);
    
    if(link.hasName()) {
      /* name */
      xmlLink.setName(link.getName());      
    }
    /* node A ref */
    xmlLink.setNodearef(nodeIdMapper.apply(link.getNodeA()));
    /* node B ref */
    xmlLink.setNodebref(nodeIdMapper.apply(link.getNodeB()));    
    
    /* line string */
    LineString destinationLineString = link.getGeometry();
    try {
      if(destinationCrsTransformer!=null) {
        destinationLineString = (LineString) JTS.transform(destinationLineString, destinationCrsTransformer);
      }
    }catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe(String.format("unable to construct Planit Xml link geometry for link %d (id:%d)",link.getExternalId(), link.getId()));
    }    
    String coordinateCsvValue = PlanitJtsUtils.createCsvStringFromLineString(destinationLineString, settings.getTupleSeparator(), settings.getCommaSeparator(), settings.getDecimalFormat());
    CoordinatesType xmlCoordinates = new CoordinatesType();
    xmlCoordinates.setValue(coordinateCsvValue);
    xmlCoordinates.setCs(settings.getCommaSeparator().toString());
    xmlCoordinates.setTs(settings.getTupleSeparator().toString());
    xmlCoordinates.setDecimal(settings.getDecimalSeparator().toString());
    LineStringType xmlLineString = new LineStringType();
    xmlLineString.setCoordinates(xmlCoordinates);
        
    /* link segments */
    populateLinkSegments(xmlLink, link);
    
    xmlLinkList.add(xmlLink);
  }
  
  
  /**
   * populate the xml /<links/> element
   * 
   * @param xmlNetworkLayer to populate on
   * @param links to populate from
   */
  private void populateXmlLinks(final XMLElementInfrastructureLayer xmlNetworkLayer, final Links<Link> links) {
    XMLElementLinks xmlLinks = xmlNetworkLayer.getLinks(); 
    if(xmlLinks == null) {
      xmlLinks = new XMLElementLinks();
      xmlNetworkLayer.setLinks(xmlLinks);
    }
    
    /* link */
    final List<XMLElementLinks.Link> xmlLinkList = xmlLinks.getLink();    
    links.forEach( link -> populateXmlLink(xmlLinkList, link));
  } 
  
  /**
   *  populate the xml /<node/> element
   * 
   * @param xmlNodeList to add node to
   * @param node to populate from
   */  
  private void populateXmlNode(final List<XMLElementNodes.Node> xmlNodeList, final Node node) {
    XMLElementNodes.Node xmlNode = new XMLElementNodes.Node();
    xmlNodeList.add(xmlNode);
    
    /* Xml id */
    xmlNode.setId(nodeIdMapper.apply(node));
    
    /* external id */
    if(node.hasExternalId()) {
      xmlNode.setExternalid(node.getExternalId());
    }    

    /* name */
    xmlNode.setName(node.getName());
    
    /* location */
    CoordType xmlCoord = new CoordType();
    Coordinate nodeCoordinate = null;
    try {
      if(destinationCrsTransformer!=null) {
        nodeCoordinate = ((Point)JTS.transform(node.getPosition(), destinationCrsTransformer)).getCoordinate();
      }else {
        nodeCoordinate = node.getPosition().getCoordinate();  
      }
    }catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe(String.format("unable to construct Planit Xml node coordinates for node %d (id:%d)",node.getExternalId(), node.getId()));
    }
    xmlCoord.setX(BigDecimal.valueOf(nodeCoordinate.x));
    xmlCoord.setY(BigDecimal.valueOf(nodeCoordinate.y));
    PointType xmlPointType = new PointType();
    xmlPointType.setCoord(xmlCoord);
    xmlNode.setPoint(xmlPointType);    
  }

  /**
   * populate the xml /<nodes/> element
   * 
   * @param xmlNetworkLayer to populate on
   * @param nodes to populate from
   */  
  private void populateXmlNodes(final XMLElementInfrastructureLayer xmlNetworkLayer, Nodes<Node> nodes) {
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
   *  populate the xml link segment type modes' /<mode/> element containing the mode specific properties of 
   *  this link segment type
   *  
   * @param xmlModeAccessList to add mode properties to 
   * @param modeProperties to populate from
   */   
  private void populateLinkSegmentTypeModeProperties(List<Accessmode> xmlModeAccessList, Mode mode, MacroscopicModeProperties modeProperties) {
    Accessmode xmlModeAccess = new Accessmode();

    /* mode ref id */
    xmlModeAccess.setRef(getXmlModeReference(mode));
    
    /* critical speed */
    xmlModeAccess.setCritspeed(modeProperties.getCriticalSpeedKmH());
    /* maximum speed */
    xmlModeAccess.setMaxspeed(modeProperties.getMaximumSpeedKmH());
    
    xmlModeAccessList.add(xmlModeAccess);
  }  
  
  /**
   *  populate the xml /<linksegmenttype/> element
   *  
   * @param xmlLinkSegmentTypeList to add link segment type to 
   * @param linkSegmentType to populate from
   */  
  private void populateXmlLinkSegmentType(List<XMLElementLinkSegmentType> xmlLinkSegmentTypeList, MacroscopicLinkSegmentType linkSegmentType) {
    XMLElementLinkSegmentType xmlLinkSegmentType = new XMLElementLinkSegmentType();
    
    /* Xml id */
    xmlLinkSegmentType.setId(linkSegmentTypeIdMapper.apply(linkSegmentType));
    
    /* external id */
    if(linkSegmentType.hasExternalId()) {
      xmlLinkSegmentType.setExternalid(linkSegmentType.getExternalId());
    }
    
    /* capacity */
    xmlLinkSegmentType.setCapacitylane(linkSegmentType.getCapacityPerLane());
    /* max density */
    xmlLinkSegmentType.setMaxdensitylane(linkSegmentType.getMaximumDensityPerLane());
    /* name */
    xmlLinkSegmentType.setName(linkSegmentType.getName());
    
    /* mode properties */
    XMLElementLinkSegmentType.Access xmlAccessModes = xmlLinkSegmentType.getAccess();
    if(xmlAccessModes == null) {
      xmlAccessModes = new XMLElementLinkSegmentType.Access();
      xmlLinkSegmentType.setAccess(xmlAccessModes);
    }
    List<Accessmode> xmlModeAccessList = xmlAccessModes.getMode();
    /* mode property entry */
    linkSegmentType.getAvailableModes().forEach( mode -> populateLinkSegmentTypeModeProperties(xmlModeAccessList, mode, linkSegmentType.getModeProperties(mode)));
    
    xmlLinkSegmentTypeList.add(xmlLinkSegmentType);
  }   
  
  /** populate the xml </linksegmenttypes/> element
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
  
  /** populate the xml </physicalfeatures/> element
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
  
  /** populate the xml </usability features/> element
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

  /** populate the xml </modes/> element
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

  /**
   * populate the link configuration for this network, i.e., the modes
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
   * populate the layer configuration for this network, i.e., link segment types
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
   * depending on the chosen id mapping, create the mapping functions for all id carrying entities that are persisted
   * @throws PlanItException thrown if error
   */
  protected void initialiseIdMappingFunctions() throws PlanItException {
    nodeIdMapper = IdMapperFunctionFactory.createNodeIdMappingFunction(getIdMapperType());
    linkIdMapper = IdMapperFunctionFactory.createLinkIdMappingFunction(getIdMapperType());
    linkSegmentIdMapper = IdMapperFunctionFactory.createLinkSegmentIdMappingFunction(getIdMapperType());
    linkSegmentTypeIdMapper = IdMapperFunctionFactory.createLinkSegmentTypeIdMappingFunction(getIdMapperType());
    modeIdMapper = IdMapperFunctionFactory.createModeIdMappingFunction(getIdMapperType());
  }  
  
  /**
   * persist the populated XML memory model to disk using JAXb
   * @throws PlanItException thrown if error
   */
  protected void persist() throws PlanItException {
    try {      
      JAXBUtils.generateXmlFileFromObject(
          xmlRawNetwork, XMLElementMacroscopicNetwork.class, Paths.get(networkPath, networkFileName), PlanitSchema.createPlanitSchemaUri(PlanitSchema.MACROSCOPIC_NETWORK_XSD));
    }catch(Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("unable to persist PLANit network in native format");
    }
  } 
  
  /** prepare the Crs transformer (if any) based on the user configuration settings
   * 
   * @param network the network extract current Crs if no user specific settings can be found
   * @throws PlanItException thrown if error
   */
  protected void prepareCoordinateReferenceSystem(MacroscopicNetwork network) throws PlanItException {
    /* CRS and transformer (if needed) */
    CoordinateReferenceSystem destinationCrs = identifyDestinationCoordinateReferenceSystem(
        settings.getDestinationCoordinateReferenceSystem(),settings.getCountryName(), network.getCoordinateReferenceSystem());    
    PlanItException.throwIfNull(destinationCrs, "destination Coordinate Reference System is null, this is not allowed");
    settings.setDestinationCoordinateReferenceSystem(destinationCrs);
    
    /* configure crs transformer if required, to be able to convert geometries to preferred CRS while writing */
    if(!destinationCrs.equals(network.getCoordinateReferenceSystem())) {
      destinationCrsTransformer = PlanitOpenGisUtils.findMathTransform(network.getCoordinateReferenceSystem(), settings.getDestinationCoordinateReferenceSystem());
    }
  }
  
  /**
   * populate the network layer
   * 
   * @param xmlInfrastructureLayers to add xml layer to 
   * @param physicalNetworkLayer to populate from
   */
  protected void populateXmlNetworkLayer(XMLElementInfrastructureLayers xmlInfrastructureLayers, MacroscopicPhysicalNetwork physicalNetworkLayer) {
    XMLElementInfrastructureLayer xmlNetworkLayer = new XMLElementInfrastructureLayer();
    xmlInfrastructureLayers.getLayer().add(xmlNetworkLayer); 
    
    /* layer configuration */
    populateXmlLayerConfiguration(xmlNetworkLayer, physicalNetworkLayer.linkSegmentTypes);

    /* links */
    populateXmlLinks(xmlNetworkLayer, physicalNetworkLayer.links);
        
    /* nodes */
    populateXmlNodes(xmlNetworkLayer, physicalNetworkLayer.nodes);      
  }  
  
  /** populate the available network layers
   * 
   * @param network to extract layers from and populate xml
   */
  protected void populateXmlNetworkLayers(MacroscopicNetwork network) {
    
    XMLElementInfrastructureLayers xmlInfrastructureLayers = xmlRawNetwork.getInfrastructurelayers();
    if(xmlInfrastructureLayers == null) {
      xmlRawNetwork.setInfrastructurelayers(new XMLElementInfrastructureLayers());
      xmlInfrastructureLayers = xmlRawNetwork.getInfrastructurelayers();
    }
    
    /* crs */
    xmlInfrastructureLayers.setSrsname(settings.getDestinationCoordinateReferenceSystem().getName().getCode());
    
    for(InfrastructureLayer networkLayer : network.infrastructureLayers) {
      if(networkLayer instanceof MacroscopicPhysicalNetwork) {
        MacroscopicPhysicalNetwork physicalNetworkLayer = ((MacroscopicPhysicalNetwork)networkLayer);
        populateXmlNetworkLayer(xmlInfrastructureLayers, physicalNetworkLayer);
      }else {
        LOGGER.severe(String.format("unsupported macroscopic infrastructure layer %s encountered", networkLayer.getXmlId()));
      }
    }
  }  
  

  public static final String DEFAULT_NETWORK_FILE_NAME = "network.xml";  
  
  /** Constructor 
   * @param networkPath to persist network on
   * @param xmlRawNetwork to populate with PLANit network when persisting
   */
  public PlanitNetworkWriter(String networkPath, XMLElementMacroscopicNetwork xmlRawNetwork) {
    this(networkPath,null,xmlRawNetwork);
  }  
    
  /** Constructor 
   * @param networkPath to persist network on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   * @param xmlRawNetwork to populate with PLANit network when persisting
   */
  public PlanitNetworkWriter(String networkPath, String countryName, XMLElementMacroscopicNetwork xmlRawNetwork) {
    super(IdMapperType.DEFAULT);
    this.networkPath = networkPath;
    this.xmlRawNetwork = xmlRawNetwork;
    if(countryName!=null && !countryName.isBlank()) {
      settings.setCountryName(countryName);
    }else {
      settings.setCountryName(CountryNames.WORLD);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(InfrastructureNetwork network) throws PlanItException {
    
    /* currently we only support macroscopic infrastructure networks */
    if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItException("currently the PLANit network reader only supports macroscopic infrastructure networks, the provided network is not of this type");
    }    
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork)network;
    
    /* initialise */
    initialiseIdMappingFunctions();
        
    /* Crs */
    prepareCoordinateReferenceSystem(macroscopicNetwork);
    
    /* log settings */
    settings.logSettings();
    
    /* general configuration */
    populateXmlConfiguration(network.modes);
    
    /* network layers */
    populateXmlNetworkLayers(macroscopicNetwork);
    
    /* persist */
    persist();
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // TODO Auto-generated method stub    
  }  

}