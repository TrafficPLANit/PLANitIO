package org.goplanit.io.converter.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.goplanit.converter.network.NetworkReaderImpl;
import org.goplanit.converter.service.ServiceNetworkReader;
import org.goplanit.io.xml.util.PlanitXmlJaxbParser;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.CharacterUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.Nodes;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.goplanit.utils.wrapper.MapWrapper;
import org.goplanit.utils.wrapper.MapWrapperImpl;
import org.goplanit.xml.generated.Direction;
import org.goplanit.xml.generated.XMLElementServiceLeg;
import org.goplanit.xml.generated.XMLElementServiceLegs;
import org.goplanit.xml.generated.XMLElementServiceNetwork;
import org.goplanit.xml.generated.XMLElementServiceNetworkLayer;
import org.goplanit.xml.generated.XMLElementServiceNodes;

/**
 * Implementation of a service network reader in the PLANit XML native format
 * 
 * @author markr
 *
 */
public class PlanitServiceNetworkReader extends NetworkReaderImpl implements ServiceNetworkReader {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitServiceNetworkReader.class.getCanonicalName());            
  
  /** the settings for this reader */
  private final PlanitServiceNetworkReaderSettings settings;
  
  /** parses the XML content in JAXB memory format */
  private final PlanitXmlJaxbParser<XMLElementServiceNetwork> xmlParser;
  
  /** the service network to populate */
  private final ServiceNetwork serviceNetwork;
      
  /** Parse service legs from XML to memory model
   * 
   * @param routedServiceLayer to extract service legs to
   * @param xmlServicelegs to extract from
   * 
   * @throws PlanItException thrown if error
   */  
  private void parseServiceLegs(ServiceNetworkLayer routedServiceLayer, XMLElementServiceLegs xmlServicelegs) throws PlanItException {
    PlanItException.throwIfNull(xmlServicelegs, "No service legs element available on service network layer %s", routedServiceLayer.getXmlId());
    List<XMLElementServiceLeg> xmlServiceLegList = xmlServicelegs.getLeg();
    PlanItException.throwIf(xmlServiceLegList==null || xmlServiceLegList.isEmpty(), "No service leg available on service network layer %s", routedServiceLayer.getXmlId());
    
    /* create map indexed by XML id based on service nodes */
    MapWrapper<String, ServiceNode> serviceNodesByXmlId = new MapWrapperImpl<String, ServiceNode>(
        new HashMap<String,ServiceNode>(), ServiceNode::getXmlId, routedServiceLayer.getServiceNodes());
    
    /* service leg */
    final boolean registerLegsOnServiceNodes = true;
    for(XMLElementServiceLeg xmlServiceLeg : xmlServiceLegList) {
      
      /* XML id */
      String xmlId = xmlServiceLeg.getId();
      if(StringUtils.isNullOrBlank(xmlId)) {
        LOGGER.warning(String.format("IGNORE: Service leg in service layer %s has no XML id defined", routedServiceLayer.getXmlId()));
        continue;
      }
      
      /* service node A */
      if(StringUtils.isNullOrBlank(xmlServiceLeg.getNodearef())){
        LOGGER.warning(String.format("IGNORE: No service node a reference present on service leg %s",xmlId));
        continue;
      }
      ServiceNode startNode = serviceNodesByXmlId.get(xmlServiceLeg.getNodearef());
      
      /* service node B */      
      if(StringUtils.isNullOrBlank(xmlServiceLeg.getNodebref())){
        LOGGER.warning(String.format("IGNORE: No service node b reference present on service leg %s",xmlId));
        continue;
      }      
      ServiceNode endNode = serviceNodesByXmlId.get(xmlServiceLeg.getNodebref());      

      /* instance */
      ServiceLeg serviceLeg = routedServiceLayer.getLegs().getFactory().registerNew(startNode, endNode, registerLegsOnServiceNodes);
      serviceLeg.setXmlId(xmlId);
            
      /* external id*/
      if(!StringUtils.isNullOrBlank(xmlServiceLeg.getExternalid())) {
        serviceLeg.setExternalId(xmlServiceLeg.getExternalid());
      }    
      
      /* service leg segment(s) */
      parseLegSegmentsOfLeg(routedServiceLayer, serviceLeg, xmlServiceLeg);

      if(!serviceLeg.validate()) {
        throw new PlanItException("Invalid service network file, inconsistency detected in service leg (%s) definition",serviceLeg.getXmlId());
      }
    }
  }

  /** Parse a service leg's service leg segments from XML to memory model
   * 
   * @param routedServiceLayer to extract service leg segments to
   * @param serviceLeg the parent leg
   * @param xmlServiceLeg to extract segment(s) from
   * 
   * @throws PlanItException thrown if error
   */    
  private void parseLegSegmentsOfLeg(ServiceNetworkLayer routedServiceLayer, ServiceLeg serviceLeg, XMLElementServiceLeg xmlServiceLeg) throws PlanItException {

    PlanItException.throwIfNull(xmlServiceLeg, "No service leg element available to extract leg segments from");    
    List<XMLElementServiceLeg.Legsegment> xmlLegSegments = xmlServiceLeg.getLegsegment();
    PlanItException.throwIf(xmlLegSegments==null || xmlLegSegments.isEmpty(), "No service leg segments available on service network layer %s", routedServiceLayer.getXmlId());
    PlanItException.throwIf(xmlLegSegments.size()>2, "No more than two service leg segments allowed per service leg (one per direction) on service leg %s on service layer %s", serviceLeg.getXmlId(), routedServiceLayer.getXmlId());
    
    /* leg segments */
    boolean registerLegSegmentsOnLegAndNode = true;
    for(XMLElementServiceLeg.Legsegment xmlLegSegment : xmlLegSegments) {
      
      /* XML id */
      String xmlId = xmlLegSegment.getId();
      if(StringUtils.isNullOrBlank(xmlId)) {
        LOGGER.warning(String.format("IGNORE: Service leg segment for leg %s has no XML id defined", serviceLeg.getXmlId()));
        continue;
      }
      
      /* direction */
      Direction xmlDirection = xmlLegSegment.getDir();
      if(xmlDirection == null) {
        LOGGER.warning(String.format("IGNORE: Service leg segment for leg %s has no direction defined", serviceLeg.getXmlId()));
        continue;
      }   
                                      
      /* instance */
      boolean isDirectionAb = xmlDirection.equals(Direction.A_B) ? true : false;
      ServiceLegSegment serviceLegSegment = routedServiceLayer.getLegSegments().getFactory().registerNew(
          serviceLeg, isDirectionAb, registerLegSegmentsOnLegAndNode);
      serviceLegSegment.setXmlId(xmlId);
      
      /* external id*/
      if(!StringUtils.isNullOrBlank(xmlLegSegment.getExternalid())) {
        serviceLegSegment.setExternalId(xmlLegSegment.getExternalid());
      }

      /* parent link segment refs comprising the leg segment*/
      String parentLinkRefs = xmlLegSegment.getLsrefs();
      if(StringUtils.isNullOrBlank(parentLinkRefs)) {
        LOGGER.warning(String.format("IGNORE: Service leg segment %s in service layer %s has no parent link segments that define the leg segment", xmlId, routedServiceLayer.getXmlId()));
        continue;
      }

      /* parent link segments in memory model */
      String[] parentLinkSegmentsRefsArray = parentLinkRefs.split(CharacterUtils.COMMA.toString());
      boolean valid = true;
      ArrayList<LinkSegment> parentLinkSegmentsInOrder = new ArrayList<>(parentLinkSegmentsRefsArray.length);
      for(int index=0;index<parentLinkSegmentsRefsArray.length;++index) {
        String xmlParentLinkSegmentRef = parentLinkSegmentsRefsArray[index].trim();
        LinkSegment linkSegmentInLeg = getBySourceId(LinkSegment.class, xmlParentLinkSegmentRef);
        if(linkSegmentInLeg==null) {
          LOGGER.warning(String.format("Service leg segment %s in service layer %s references unknown parent link segment %s", xmlId, routedServiceLayer.getXmlId(), xmlParentLinkSegmentRef));
          valid=false;
          continue;
        }
        parentLinkSegmentsInOrder.add(linkSegmentInLeg);
      }
      if(!valid) {
        LOGGER.warning(String.format("IGNORE: Service leg segment %s in service layer %s invalid", xmlId, routedServiceLayer.getXmlId()));
        continue;
      }

      if(!parentLinkSegmentsInOrder.isEmpty()){
        serviceLegSegment.setPhysicalParentSegments(parentLinkSegmentsInOrder);
      }
    }
  }

  /** Parse service nodes from XML to memory model
   * 
   * @param routedServiceLayer to extract service nodes to
   * @param xmlServicenodes to extract from
   * 
   * @throws PlanItException thrown if error
   */
  private void parseServiceNodes(ServiceNetworkLayer routedServiceLayer, XMLElementServiceNodes xmlServicenodes) throws PlanItException {
    PlanItException.throwIfNull(xmlServicenodes, "No service nodes element available on service network layer %s", routedServiceLayer.getXmlId());
    List<XMLElementServiceNodes.Servicenode> xmlServiceNodeList = xmlServicenodes.getServicenode();
    PlanItException.throwIf(xmlServiceNodeList==null || xmlServiceNodeList.isEmpty(), "No service node available on service network layer %s", routedServiceLayer.getXmlId());
    MacroscopicNetworkLayer parentLayer = routedServiceLayer.getParentNetworkLayer();
    PlanItException.throwIf(parentLayer==null || parentLayer.isEmpty(), "No parent layer or empty parent layer for service network layer %s", routedServiceLayer.getXmlId());
    Nodes parentNodes = parentLayer.getNodes();
    PlanItException.throwIf(parentNodes==null || parentNodes.isEmpty(), "No parent nodes or empty parent nodes for service network layer %s", routedServiceLayer.getXmlId());
    
    for(XMLElementServiceNodes.Servicenode xmlServiceNode : xmlServiceNodeList) {
                
      /* XML id */
      String xmlId = xmlServiceNode.getId();
      if(StringUtils.isNullOrBlank(xmlId)) {
        LOGGER.warning(String.format("IGNORE: Service node in service layer %s has no XML id defined", routedServiceLayer.getXmlId()));
        continue;
      }
      
      /* parent node XML id*/
      String parentNodeXmlId = xmlServiceNode.getNoderef();
      if(StringUtils.isNullOrBlank(parentNodeXmlId)) {
        LOGGER.warning(String.format("IGNORE: Service node %s in service layer %s has no parent node XML id reference defined", xmlId, routedServiceLayer.getXmlId()));
        continue;
      }                      
           
      if(getBySourceId(Node.class, parentNodeXmlId) == null) {
        LOGGER.warning(String.format("IGNORE: Service node %s in service layer %s references unknown parent node %s", xmlId, routedServiceLayer.getXmlId(), parentNodeXmlId));
        continue;
      }
      
      /* instance */
      ServiceNode serviceNode = routedServiceLayer.getServiceNodes().getFactory().registerNew(getBySourceId(Node.class,parentNodeXmlId));
      serviceNode.setXmlId(xmlId);
      
      /* external id*/
      if(!StringUtils.isNullOrBlank(xmlServiceNode.getExternalid())) {
        serviceNode.setExternalId(xmlServiceNode.getExternalid());
      }        
      
    }
    
  }

  /**
   * Parse the service network layer
   * 
   * @param xmlLayer layer to extract from
   * @return parsed layer
   * @throws PlanItException thrown if error
   */
  private ServiceNetworkLayer parseServiceNetworkLayer(XMLElementServiceNetworkLayer xmlLayer ) throws PlanItException {
        
    /* parent layer XML id */
    String parentLayerXmlId = xmlLayer.getParentlayerref();
    if(StringUtils.isNullOrBlank(parentLayerXmlId)) {
      throw new PlanItException("Service network layer %s has no parent layer XML id defined", xmlLayer.getId());
    }
    MacroscopicNetworkLayer parentNetworkLayer = serviceNetwork.getParentNetwork().getTransportLayers().getByXmlId(parentLayerXmlId);
    if(parentNetworkLayer==null || parentNetworkLayer.isEmpty()) {
      throw new PlanItException("Service network layer %s its parent layer %s does not exist in the parent network or is empty", xmlLayer.getId(), parentLayerXmlId);
    }
    
    /* memory model instance */
    ServiceNetworkLayer routedServiceLayer = serviceNetwork.getTransportLayers().getFactory().registerNew(parentNetworkLayer);
    
    /* XML id */
    String xmlId = xmlLayer.getId();
    if(StringUtils.isNullOrBlank(xmlId)) {
      LOGGER.warning(String.format("Service network layer has no XML id defined, adopting internally generated id %d instead", serviceNetwork.getId()));
      xmlId = String.valueOf(routedServiceLayer.getId());
    }
    routedServiceLayer.setXmlId(xmlId);
    
    /* external id*/
    if(!StringUtils.isNullOrBlank(xmlLayer.getExternalid())) {
      routedServiceLayer.setExternalId(xmlLayer.getExternalid());
    }    
    
    /* service nodes */
    parseServiceNodes(routedServiceLayer, xmlLayer.getServicenodes());
    
    /* service legs */
    parseServiceLegs(routedServiceLayer, xmlLayer.getServicelegs());
    
    return routedServiceLayer;
  }  
  
  /** Parse the various service network layers
   * 
   * @throws PlanItException thrown if error
   */
  private void parseServiceNetworkLayers() throws PlanItException {
    List<XMLElementServiceNetworkLayer> xmlLayers = xmlParser.getXmlRootElement().getServicenetworklayer();
    if(xmlLayers==null || xmlLayers.isEmpty()) {
      LOGGER.warning(String.format("IGNORE: No service layers present in service network file"));
      return;
    }
        
    /* layers */
    for(XMLElementServiceNetworkLayer xmlLayer : xmlLayers) {      
      /*layer */
      parseServiceNetworkLayer(xmlLayer);
    } 
  }  
  
  /**
   * initialise the XML id trackers and populate them for the network references, 
   * so we can lay indices on the XML id as well for quick lookups
   * 
   * @param network
   */
  private void initialiseParentXmlIdTrackers(MacroscopicNetwork network) {    
    initialiseSourceIdMap(Node.class, Node::getXmlId);
    network.getTransportLayers().forEach( layer -> getSourceIdContainer(Node.class).addAll(layer.getNodes()));    
    initialiseSourceIdMap(LinkSegment.class, LinkSegment::getXmlId);
    network.getTransportLayers().forEach( layer -> getSourceIdContainer(LinkSegment.class).addAll(layer.getLinkSegments()));
  }   
  
  /** Constructor where settings are directly provided such that input information can be extracted from it
   * 
   * @param idToken to use for the service network to populate
   * @param settings to use
   * @throws PlanItException  thrown if error
   */
  protected PlanitServiceNetworkReader(final IdGroupingToken idToken, final PlanitServiceNetworkReaderSettings settings) throws PlanItException{
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementServiceNetwork>(XMLElementServiceNetwork.class);
    this.settings = settings;
    this.serviceNetwork = new ServiceNetwork(idToken, settings.getParentNetwork());
  }  
  
  /** Constructor where settings and service network are directly provided
   * 
   * @param settings to use
   * @param serviceNetwork to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitServiceNetworkReader(final PlanitServiceNetworkReaderSettings settings, final ServiceNetwork serviceNetwork) throws PlanItException{
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementServiceNetwork>(XMLElementServiceNetwork.class);
    this.settings = settings;
    this.serviceNetwork = serviceNetwork;
    if(!settings.getParentNetwork().equals(serviceNetwork.getParentNetwork())) {
      LOGGER.severe("parent network in service network reader settings does not match the parent network in the provided service network for the PLANit service network reader");
    }
  }  
    
  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param populatedXmlRawServiceNetwork to extract from
   * @param serviceNetwork to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitServiceNetworkReader(final XMLElementServiceNetwork populatedXmlRawServiceNetwork, final ServiceNetwork serviceNetwork) throws PlanItException{
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementServiceNetwork>(populatedXmlRawServiceNetwork);
    this.settings = new PlanitServiceNetworkReaderSettings(serviceNetwork.getParentNetwork());
    this.serviceNetwork = serviceNetwork;
  }
  
  /** Constructor
   * 
   * @param networkPathDirectory to use
   * @param xmlFileExtension to use
   * @param serviceNetwork to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitServiceNetworkReader(String networkPathDirectory, String xmlFileExtension, ServiceNetwork serviceNetwork) throws PlanItException{
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementServiceNetwork>(XMLElementServiceNetwork.class);
    this.settings = new PlanitServiceNetworkReaderSettings(serviceNetwork.getParentNetwork(), networkPathDirectory, xmlFileExtension);
    this.serviceNetwork = serviceNetwork;
  }  
  
  /** Default XSD files used to validate input XML files against, TODO: move to properties file */
  public static final String SERVICE_NETWORK_XSD_FILE = "https://trafficplanit.github.io/PLANitManual/xsd/servicenetworkinput.xsd";  

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetwork read() throws PlanItException {
        
    /* parse the XML raw network to extract PLANit network from */   
    xmlParser.initialiseAndParseXmlRootElement(getSettings().getInputDirectory(), getSettings().getXmlFileExtension());
    PlanItException.throwIfNull(xmlParser.getXmlRootElement(), "No valid PLANit XML service network could be parsed into memory, abort");     
    
    /* XML id */
    String xmlId = xmlParser.getXmlRootElement().getId();
    if(StringUtils.isNullOrBlank(xmlId)) {
      LOGGER.warning(String.format("Service network has no XML id defined, adopting internally generated id %d instead", serviceNetwork.getId()));
      xmlId = String.valueOf(serviceNetwork.getId());
    }
    serviceNetwork.setXmlId(xmlId);
    
    /* parent network XML id */
    String parentNetworkXmlId = xmlParser.getXmlRootElement().getParentnetwork();
    if(StringUtils.isNullOrBlank(parentNetworkXmlId)) {
      throw new PlanItException("Service network %s has no parent network defined", serviceNetwork.getXmlId());
    }
    if(!settings.getParentNetwork().getXmlId().equals(parentNetworkXmlId)) {
      throw new PlanItException(
          "Service network %s parent network (%s) in memory does not correspond to the parent network id on file (%s)", serviceNetwork.getXmlId(), settings.getParentNetwork().getXmlId(), parentNetworkXmlId);
    }
          
    try {
      
      /* initialise the indices used, if needed */
      initialiseParentXmlIdTrackers(getSettings().getParentNetwork());   

      /* parse layers */
      parseServiceNetworkLayers();
      
      /* free XML content after parsing */
      xmlParser.clearXmlContent();
      
    } catch (PlanItException e) {
      throw e;
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException(String.format("Error while populating service network %s in PLANitIO", serviceNetwork.getXmlId()),e);
    }    
    
    return serviceNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitServiceNetworkReaderSettings getSettings() {
    return settings;
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    settings.reset();
  }

}
