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
import org.planit.network.converter.IdMapperFunctionFactory;
import org.planit.network.converter.NetworkWriterImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.math.TypeConversionUtil;
import org.planit.utils.mode.Modes;
import org.planit.xml.generated.Direction;
import org.planit.xml.generated.LengthUnit;
import org.planit.xml.generated.XMLElementInfrastructure;
import org.planit.xml.generated.XMLElementLinkLengthType;
import org.planit.xml.generated.XMLElementLinkSegment;
import org.planit.xml.generated.XMLElementLinks;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;
import org.planit.xml.generated.XMLElementNodes;

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
    //TODO: continue here
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
  }



}
