package org.planit.io.network.converter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.network.converter.IdMapperType;
import org.planit.geo.PlanitJtsUtils;
import org.planit.network.converter.IdMapperFunctionFactory;
import org.planit.network.converter.NetworkWriterImpl;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.enums.OutputType;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Modes;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegments;
import org.planit.utils.network.physical.Links;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.Nodes;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentTypes;
import org.planit.xml.generated.LengthUnit;
import org.planit.xml.generated.XMLElementInfrastructure;
import org.planit.xml.generated.XMLElementLinkLengthType;
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
  
  /** the idToken to use for generating id's in case the user decides to map id's based on newly generated id's rather than existing ones */
  private IdGroupingToken idToken = null;;
  
  /** id mapper for nodes */
  private BiFunction<Node, IdGroupingToken, String> nodeIdMapper;
  /** id mapper for links */
  private BiFunction<Link, IdGroupingToken, String> linkIdMapper;
  /** id mapper for link segments */
  private BiFunction<MacroscopicLinkSegment, IdGroupingToken, String> linkSegmentIdMapper;
  /** id mapper for link segment types */
  private BiFunction<MacroscopicLinkSegmentType, IdGroupingToken, String> linkSegmentTypeIdMapper;
    
  /** network path (file) to persist to */
  private final String networkPath;
    
  /** XML memory model equivalent of the PLANit memory mode */
  private final XMLElementMacroscopicNetwork xmlRawNetwork;
    
  
  /**
   * @param xmlLink to populate link segments on
   * @param link to populate link segments from
   */
  private void populateLinkSegments(org.planit.xml.generated.XMLElementLinks.Link xmlLink, Link link) {
    //TODO: continue here
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
    xmlLink.setId(BigInteger.valueOf(Long.parseLong(linkIdMapper.apply(link, idToken))));
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
    xmlLink.setNodearef(BigInteger.valueOf(Long.parseLong(nodeIdMapper.apply(link.getNodeA(), idToken))));
    /* node B ref */
    xmlLink.setNodebref(BigInteger.valueOf(Long.parseLong(nodeIdMapper.apply(link.getNodeB(), idToken))));
    
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
    xmlNode.setId(BigInteger.valueOf(Long.parseLong(nodeIdMapper.apply(node, idToken))));
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
    
    /* initialise token when used during id generation, otherwise leave at null */
    if(getIdMapper().equals(IdMapperType.GENERATED)) {
      idToken = IdGenerator.createIdGroupingToken(this.getClass().getCanonicalName());
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
  }



}
