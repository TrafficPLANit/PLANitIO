package org.planit.io.network.converter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import org.planit.utils.network.physical.*;
import org.planit.utils.network.physical.macroscopic.*;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.network.converter.IdMapperType;
import org.planit.geo.PlanitJtsUtils;
import org.planit.io.xml.util.EnumConversionUtil;
import org.planit.io.xml.util.JAXBUtils;
import org.planit.network.converter.IdMapperFunctionFactory;
import org.planit.network.converter.NetworkWriterImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.math.TypeConversionUtil;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.Modes;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.UsabilityModeFeatures;
import org.planit.xml.generated.Direction;
import org.planit.xml.generated.LengthUnit;
import org.planit.xml.generated.XMLElementInfrastructure;
import org.planit.xml.generated.XMLElementLinkConfiguration;
import org.planit.xml.generated.XMLElementLinkLengthType;
import org.planit.xml.generated.XMLElementLinkSegment;
import org.planit.xml.generated.XMLElementLinkSegmentTypes;
import org.planit.xml.generated.XMLElementLinkSegmentTypes.Linksegmenttype;
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
 * Writer to persist a PLANit network to disk in the native PLANit format. By default the external ids are used for writing out the ids in the XML 
 * based on the {@link IdMapper.EXTERNAL_ID}.
 * 
 * @author markr
 *
 */
public class PlanitNetworkWriter extends NetworkWriterImpl {
  
 
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkWriter.class.getCanonicalName());
  
  /** user configurable settings for the writer */
  PlanitNetworkWriterSettings settings;
    
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
    
  /** XML memory model equivalent of the PLANit memory mode */
  private final XMLElementMacroscopicNetwork xmlRawNetwork;
    
  
  /**
   * populate a single xml link segment element based on the passed in PLANit link segment
   * 
   * @param xmlLinkSegment to populate
   * @param linkSegment the PLANit link segment instance to populate from
   */
  private void populateLinkSegment(XMLElementLinkSegment xmlLinkSegment, MacroscopicLinkSegment linkSegment) {
    /* id */
    xmlLinkSegment.setId(TypeConversionUtil.toBigInteger(linkSegmentIdMapper.apply(linkSegment)));
    /* max speed */
    xmlLinkSegment.setMaxspeed(linkSegment.getPhysicalSpeedLimitKmH());
    /* number of lanes */
    xmlLinkSegment.setNumberoflanes(BigInteger.valueOf(linkSegment.getNumberOfLanes()));
    if(!linkSegment.hasLinkSegmentType()) {
      LOGGER.severe(String.format("missing link segment type on link segment %s (id:%d)", linkSegment.getExternalId(), linkSegment.getId()));      
    }else {
      xmlLinkSegment.setTyperef(TypeConversionUtil.toBigInteger(linkSegmentTypeIdMapper.apply(linkSegment.getLinkSegmentType())));  
    }
  }  
  
  /** populate a link's link segments
   * 
   * @param xmlLink to populate link segments on
   * @param link to populate link segments from
   */
  private void populateLinkSegments(XMLElementLinks.Link xmlLink, Link link) {
    List<XMLElementLinkSegment> xmlLinkSegments = xmlLink.getLinksegment();
    if(xmlLinkSegments==null && link.hasLinkSegmentAb() || link.hasLinkSegmentBa()) {
      LOGGER.severe(String.format("link %s (id:%d)has no xm lLink segment element, but does have link segments",link.getExternalId(), link.getId()));
      return;
    }
    
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
  }
  
  /**
   *  populate the xml /<link/> element
   *  
   * @param xmlLinkList to add link to 
   * @param link to populate from
   */
  private void populateXmlLink(List<XMLElementLinks.Link> xmlLinkList, final Link link) {
    XMLElementLinks.Link xmlLink = new XMLElementLinks.Link();
    
    /* persisting id equates to external id when parsing the network again, since ids are internally generated always */
    xmlLink.setId(BigInteger.valueOf(Long.parseLong(linkIdMapper.apply(link))));
    /* length */
    XMLElementLinkLengthType xmlLinkLength = new XMLElementLinkLengthType();     
    xmlLinkLength.setUnit(LengthUnit.KM);
    xmlLinkLength.setValue(link.getLengthKm());
    xmlLink.setLength(xmlLinkLength);
    /* line string */        
    String coordinateCsvValue = PlanitJtsUtils.createCsvStringFromLineString(link.getGeometry(), settings.getTupleSeparator(), settings.getCommaSeparator(), settings.getDecimalFormat());
    CoordinatesType xmlCoordinates = new CoordinatesType();
    xmlCoordinates.setValue(coordinateCsvValue);
    xmlCoordinates.setCs(settings.getCommaSeparator().toString());
    xmlCoordinates.setTs(settings.getTupleSeparator().toString());
    xmlCoordinates.setDecimal(settings.getDecimalSeparator().toString());
    LineStringType xmlLineString = new LineStringType();
    xmlLineString.setCoordinates(xmlCoordinates);
    /* name */
    xmlLink.setName(link.getName());
    /* node A ref */
    xmlLink.setNodearef(TypeConversionUtil.toBigInteger(nodeIdMapper.apply(link.getNodeA())));
    /* node B ref */
    xmlLink.setNodebref(TypeConversionUtil.toBigInteger(nodeIdMapper.apply(link.getNodeB())));
    
    /* link segments */
    populateLinkSegments(xmlLink, link);
    
    xmlLinkList.add(xmlLink);
  }
  
  
  /**
   * populate the xml /<links/> element
   * 
   * @param links to populate from
   */
  private void populateXmlLinks(final Links<Link> links) {
    XMLElementLinks xmlLinks = xmlRawNetwork.getInfrastructure().getLinks(); 
    if(xmlLinks == null) {
      xmlLinks = new XMLElementLinks();
      xmlRawNetwork.getInfrastructure().setLinks(xmlLinks );
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
    
    /* persisting id equates to external id when parsing the network again, since ids are internally generated always */
    xmlNode.setId(BigInteger.valueOf(Long.parseLong(nodeIdMapper.apply(node))));
    /* name */
    xmlNode.setName(node.getName());
    /* location */
    CoordType xmlCoord = new CoordType();
    xmlCoord.setX(BigDecimal.valueOf(node.getPosition().getCoordinate().x));
    xmlCoord.setY(BigDecimal.valueOf(node.getPosition().getCoordinate().y));
    PointType xmlPointType = new PointType();
    xmlPointType.setCoord(xmlCoord);
    xmlNode.setPoint(xmlPointType);
  }

  /**
   * populate the xml /<nodes/> element
   * 
   * @param nodes to populate from
   */  
  private void populateXmlNodes(Nodes<Node> nodes) {
    XMLElementNodes xmlNodes = xmlRawNetwork.getInfrastructure().getNodes(); 
    if(xmlNodes == null) {
      xmlNodes = new XMLElementNodes();
      xmlRawNetwork.getInfrastructure().setNodes(xmlNodes);
    }
        
    /* node */
    final List<XMLElementNodes.Node> xmlNodeList = xmlNodes.getNode();    
    nodes.forEach( node -> populateXmlNode(xmlNodeList, node));    
  }
  
  /**
   *  populate the xml link segment type modes' /<mode/> element containing the mode specific properties of 
   *  this link segment type
   *  
   * @param xmlModePropertiesList to add mode properties to 
   * @param modeProperties to populate from
   */   
  private void populateLinkSegmentTypeModeProperties(List<XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode> xmlModePropertiesList, Mode mode, MacroscopicModeProperties modeProperties) {
    XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode xmlModeProperties = new XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode();

    /* mode ref id */
    xmlModeProperties.setRef(TypeConversionUtil.toBigInteger(modeIdMapper.apply(mode)));
    
    /* critical speed */
    xmlModeProperties.setCritspeed(modeProperties.getCriticalSpeedKmH());
    /* maximum speed */
    xmlModeProperties.setMaxspeed(modeProperties.getMaximumSpeedKmH());
    
    xmlModePropertiesList.add(xmlModeProperties);
  }  
  
  /**
   *  populate the xml /<linksegmenttype/> element
   *  
   * @param xmlLinkSegmentTypeList to add link segment type to 
   * @param linkSegmentType to populate from
   */  
  private void populateXmlLinkSegmentType(List<Linksegmenttype> xmlLinkSegmentTypeList, MacroscopicLinkSegmentType linkSegmentType) {
    Linksegmenttype xmlLinkSegmentType = new Linksegmenttype();
    
    /* id */
    xmlLinkSegmentType.setId(TypeConversionUtil.toBigInteger(linkSegmentTypeIdMapper.apply(linkSegmentType)));
    
    /* capacity */
    xmlLinkSegmentType.setCapacitylane(linkSegmentType.getCapacityPerLane());
    /* max density */
    xmlLinkSegmentType.setMaxdensitylane(linkSegmentType.getMaximumDensityPerLane());
    /* name */
    xmlLinkSegmentType.setName(linkSegmentType.getName());
    
    /* mode properties */
    XMLElementLinkSegmentTypes.Linksegmenttype.Modes xmlModes = xmlLinkSegmentType.getModes();
    if(xmlModes == null) {
      xmlModes = new XMLElementLinkSegmentTypes.Linksegmenttype.Modes();
      xmlLinkSegmentType.setModes(xmlModes);
    }
    List<XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode> xmlModePropertiesList = xmlModes.getMode();
    /* mode property entry */
    linkSegmentType.getAvailableModes().forEach( mode -> populateLinkSegmentTypeModeProperties(xmlModePropertiesList, mode, linkSegmentType.getModeProperties(mode)));
    
    xmlLinkSegmentTypeList.add(xmlLinkSegmentType);
  }   
  
  /** populate the xml </linksegmenttypes/> element
   * @param linkSegmentTypes to populate from
   */
  private void populateXmlLinkSegmentTypes(MacroscopicLinkSegmentTypes linkSegmentTypes) {
    XMLElementLinkSegmentTypes xmlLinkSegmentTypes = xmlRawNetwork.getLinkconfiguration().getLinksegmenttypes();
    if(xmlLinkSegmentTypes == null) {
      xmlLinkSegmentTypes = new XMLElementLinkSegmentTypes();
      xmlRawNetwork.getLinkconfiguration().setLinksegmenttypes(xmlLinkSegmentTypes);
    }
    
    /* link segment type */
    List<Linksegmenttype> xmlLinkSegmentTypeList = xmlLinkSegmentTypes.getLinksegmenttype();
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
    
    /* id */
    xmlMode.setId(TypeConversionUtil.toBigInteger(modeIdMapper.apply(mode)));
    
    /* max speed */
    xmlMode.setMaxspeed(mode.getMaximumSpeedKmH());
    /* name */
    xmlMode.setName(mode.getName());
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
    XMLElementModes xmlModes = xmlRawNetwork.getLinkconfiguration().getModes();
    if(xmlModes == null) {
      xmlModes = new XMLElementModes();
      xmlRawNetwork.getLinkconfiguration().setModes(xmlModes);
    }
    
    /* modes*/
    List<XMLElementModes.Mode> xmlModesList = xmlModes.getMode();
    modes.forEach( mode -> populateXmlMode(xmlModesList, mode));        
  }          

  /** populate the xml /<infrastructure/> element
   * @param coordinateReferenceSystem to use
   * @param linkSegments 
   * @param links 
   * @param nodes 
   */
  protected void populateXmlInfrastructure(CoordinateReferenceSystem coordinateReferenceSystem, Nodes<Node> nodes, Links<Link> links, LinkSegments<MacroscopicLinkSegment> linkSegments) {
    XMLElementInfrastructure xmlInfrastructure = xmlRawNetwork.getInfrastructure(); 
    if(xmlInfrastructure == null) {
      xmlInfrastructure = new XMLElementInfrastructure();
      xmlRawNetwork.setInfrastructure(xmlInfrastructure );
    }
    
    /* SRS/CRS */
    xmlInfrastructure.setSrsname(coordinateReferenceSystem.getName().getCode());

    /* links */
    populateXmlLinks(links);
        
    /* nodes */
    populateXmlNodes(nodes);        
  }


  /**
   * populate the link configuration for this network, i.e., the modes and link types
   * 
   * @param modes to use to populate the XML elements
   * @param linkSegmentTypes to use to populate the XML elements
   */
  protected void populateXmlLinkConfiguration(Modes modes, MacroscopicLinkSegmentTypes linkSegmentTypes) {
    XMLElementLinkConfiguration xmlLinkconfiguration = xmlRawNetwork.getLinkconfiguration();
    if(xmlLinkconfiguration == null) {
      xmlLinkconfiguration = new XMLElementLinkConfiguration();
      xmlRawNetwork.setLinkconfiguration(xmlLinkconfiguration);
    }
    
    /* modes */
    populateXmlModes(modes);
    
    /* link segment types */
    populateXmlLinkSegmentTypes(linkSegmentTypes);
  }  
  


  /**
   * depending on the chosen id mapping, create the mapping functions for all id carrying entities that are persisted
   * @throws PlanItException thrown if error
   */
  protected void initialiseIdMappingFunctions() throws PlanItException {
    nodeIdMapper = IdMapperFunctionFactory.createNodeIdMappingFunction(getIdMapper());
    linkIdMapper = IdMapperFunctionFactory.createLinkIdMappingFunction(getIdMapper());
    linkSegmentIdMapper = IdMapperFunctionFactory.createLinkSegmentIdMappingFunction(getIdMapper());
    linkSegmentTypeIdMapper = IdMapperFunctionFactory.createLinkSegmentTypeIdMappingFunction(getIdMapper());
    modeIdMapper = IdMapperFunctionFactory.createModeIdMappingFunction(getIdMapper());
  }  
  
  /**
   * persist the populated XML memory model to disk using JAXb
   * @throws PlanItException thrown if error
   */
  private void persist() throws PlanItException {
    try {
      JAXBUtils.generateXmlFileFromObject(xmlRawNetwork, XMLElementMacroscopicNetwork.class, networkPath);
    }catch(Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("unable to persist PLANit network in native format");
    }
  }  
  
  /** Constructor 
   * @param networkPath to persist network on
   * @param network to persist
   */
  public PlanitNetworkWriter(String networkPath, XMLElementMacroscopicNetwork xmlRawNetwork) {
    super(IdMapperType.EXTERNAL_ID);
    this.networkPath = networkPath;
    this.xmlRawNetwork = xmlRawNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(MacroscopicNetwork network) throws PlanItException {
    /* initialise */
    initialiseIdMappingFunctions();
    
    /* configuration */
    populateXmlLinkConfiguration(network.modes, network.linkSegmentTypes);
    /* infrastructure */
    populateXmlInfrastructure(network.getCoordinateReferenceSystem(), network.nodes, network.links, network.linkSegments);
    
    /* persist */
    persist();
  }


}
