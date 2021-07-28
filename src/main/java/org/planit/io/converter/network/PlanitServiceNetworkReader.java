package org.planit.io.converter.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.planit.converter.network.NetworkReaderBase;
import org.planit.io.xml.util.PlanitXmlJaxbParser;
import org.planit.network.ServiceNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.CharacterUtils;
import org.planit.utils.misc.StringUtils;
import org.planit.utils.network.layer.MacroscopicNetworkLayer;
import org.planit.utils.network.layer.ServiceNetworkLayer;
import org.planit.utils.network.layer.physical.Link;
import org.planit.utils.network.layer.physical.Node;
import org.planit.utils.network.layer.physical.Nodes;
import org.planit.utils.network.layer.service.ServiceLeg;
import org.planit.utils.network.layer.service.ServiceLegSegment;
import org.planit.utils.network.layer.service.ServiceNode;
import org.planit.utils.wrapper.MapWrapper;
import org.planit.utils.wrapper.MapWrapperImpl;
import org.planit.xml.generated.Direction;
import org.planit.xml.generated.XMLElementServiceLeg;
import org.planit.xml.generated.XMLElementServiceLegs;
import org.planit.xml.generated.XMLElementServiceNetwork;
import org.planit.xml.generated.XMLElementServiceNetworkLayer;
import org.planit.xml.generated.XMLElementServiceNodes;

/**
 * Implementation of a service network reader in the PLANit XML native format
 * 
 * @author markr
 *
 */
public class PlanitServiceNetworkReader extends NetworkReaderBase {
  
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
   * @param serviceNetworkLayer to extract service legs to
   * @param xmlServicelegs to extract from
   * 
   * @throws PlanItException thrown if error
   */  
  private void parseServiceLegs(ServiceNetworkLayer serviceNetworkLayer, XMLElementServiceLegs xmlServicelegs) throws PlanItException {
    PlanItException.throwIfNull(xmlServicelegs, "No service legs element available on service network layer %s", serviceNetworkLayer.getXmlId());
    List<XMLElementServiceLeg> xmlServiceLegList = xmlServicelegs.getLeg();
    PlanItException.throwIf(xmlServiceLegList==null || xmlServiceLegList.isEmpty(), "No service leg available on service network layer %s", serviceNetworkLayer.getXmlId());
    
    /* create map indexed by XML id based on service nodes */
    MapWrapper<String, ServiceNode> serviceNodesByXmlId = new MapWrapperImpl<String, ServiceNode>(
        new HashMap<String,ServiceNode>(), ServiceNode::getXmlId, serviceNetworkLayer.getServiceNodes());
    
    /* service leg */
    final boolean registerLegsOnServiceNodes = true;
    for(XMLElementServiceLeg xmlServiceLeg : xmlServiceLegList) {
      
      /* XML id */
      String xmlId = xmlServiceLeg.getId();
      if(StringUtils.isNullOrBlank(xmlId)) {
        LOGGER.warning(String.format("IGNORE: Service leg in service layer %s has no XML id defined", serviceNetworkLayer.getXmlId()));
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
      
      /* parent link refs comprising the leg */
      String parentLinkRefs = xmlServiceLeg.getLrefs();
      if(StringUtils.isNullOrBlank(parentLinkRefs)) {
        LOGGER.warning(String.format("IGNORE: Service leg %s in service layer %s has no parent links that define the leg", xmlId, serviceNetworkLayer.getXmlId()));
        continue;
      }  
      
      /* parent links in memory model */
      String[] parentLinkRefsArray = parentLinkRefs.split(CharacterUtils.COMMA.toString());
      boolean valid = true;
      ArrayList<Link> parentLinksInOrder = new ArrayList<Link>(parentLinkRefsArray.length);
      for(int index=0;index<parentLinkRefsArray.length;++index) {
        String xmlParentLinkRef = parentLinkRefsArray[index];
        Link linkInLeg = settings.getParentLinksByXmlId().get(xmlParentLinkRef);
        if(linkInLeg==null) {
          LOGGER.warning(String.format("Service leg %s in service layer %s references unknown parent link %s", xmlId, serviceNetworkLayer.getXmlId(), xmlParentLinkRef));
          valid=false;
          continue;
        }                
        parentLinksInOrder.add(linkInLeg);
      }
      if(!valid) {
        LOGGER.warning(String.format("IGNORE: Service leg %s in service layer %s invalid", xmlId, serviceNetworkLayer.getXmlId()));
        continue;
      }
                 
      /* instance */
      ServiceLeg serviceLeg = serviceNetworkLayer.getLegs().getFactory().registerNew(startNode, endNode, parentLinksInOrder, registerLegsOnServiceNodes);
      if(!serviceLeg.validate()) {
        throw new PlanItException("Invalid service network file, inconsistency detected in service leg (%s) definition",serviceLeg.getXmlId());
      }
      serviceLeg.setXmlId(xmlId);      
            
      /* external id*/
      if(!StringUtils.isNullOrBlank(xmlServiceLeg.getExternalid())) {
        serviceLeg.setExternalId(xmlServiceLeg.getExternalid());
      }    
      
      /* service leg segment(s) */
      parseLegSegmentsOfLeg(serviceNetworkLayer, serviceLeg, xmlServiceLeg);
    }
  }

  /** Parse a service leg's service leg segments from XML to memory model
   * 
   * @param serviceNetworkLayer to extract service leg segments to
   * @param serviceLeg the parent leg
   * @param xmlServiceleg to extract segment(s) from
   * 
   * @throws PlanItException thrown if error
   */    
  private void parseLegSegmentsOfLeg(ServiceNetworkLayer serviceNetworkLayer, ServiceLeg serviceLeg, XMLElementServiceLeg xmlServiceLeg) throws PlanItException {

    PlanItException.throwIfNull(xmlServiceLeg, "No service leg element available to extract leg segments from");    
    List<XMLElementServiceLeg.Legsegment> xmlLegSegments = xmlServiceLeg.getLegsegment();
    PlanItException.throwIf(xmlLegSegments==null || xmlLegSegments.isEmpty(), "No service leg segments available on service network layer %s", serviceNetworkLayer.getXmlId());
    PlanItException.throwIf(xmlLegSegments.size()>2, "No more than two service leg segments allowed per service leg (one per direction) on service leg %s on service layer %s", serviceLeg.getXmlId(), serviceNetworkLayer.getXmlId());            
    
    /* leg segments */
    boolean registerLegSegmentsOnLegAndNode = true;
    for(XMLElementServiceLeg.Legsegment xmlLegSegment : xmlLegSegments) {
      
      /* XML id */
      String xmlId = xmlLegSegment.getId();
      if(StringUtils.isNullOrBlank(xmlId)) {
        LOGGER.warning(String.format("IGNORE: Service leg segment for leg %s has no XML id defined", serviceLeg.getXmlId()));
        continue;
      }
      
      /* XML id */
      Direction xmlDirection = xmlLegSegment.getDir();
      if(xmlDirection == null) {
        LOGGER.warning(String.format("IGNORE: Service leg segment for leg %s has no direction defined", serviceLeg.getXmlId()));
        continue;
      }   
                                      
      /* instance */
      boolean isDirectionAb = xmlDirection.equals(Direction.A_B) ? true : false;
      ServiceLegSegment serviceLegSegment = serviceNetworkLayer.getLegSegments().getFactory().registerNew(
          serviceLeg, isDirectionAb, registerLegSegmentsOnLegAndNode);
      serviceLegSegment.setXmlId(xmlId);
      
      /* external id*/
      if(!StringUtils.isNullOrBlank(xmlLegSegment.getExternalid())) {
        serviceLegSegment.setExternalId(xmlLegSegment.getExternalid());
      }  
    }
  }

  /** Parse service nodes from XML to memory model
   * 
   * @param serviceNetworkLayer to extract service nodes to
   * @param xmlServicenodes to extract from
   * 
   * @throws PlanItException thrown if error
   */
  private void parseServiceNodes(ServiceNetworkLayer serviceNetworkLayer, XMLElementServiceNodes xmlServicenodes) throws PlanItException {
    PlanItException.throwIfNull(xmlServicenodes, "No service nodes element available on service network layer %s", serviceNetworkLayer.getXmlId());
    List<XMLElementServiceNodes.Servicenode> xmlServiceNodeList = xmlServicenodes.getServicenode();
    PlanItException.throwIf(xmlServiceNodeList==null || xmlServiceNodeList.isEmpty(), "No service node available on service network layer %s", serviceNetworkLayer.getXmlId());
    MacroscopicNetworkLayer parentLayer = serviceNetworkLayer.getParentNetworkLayer();
    PlanItException.throwIf(parentLayer==null || parentLayer.isEmpty(), "No parent layer or empty parent layer for service network layer %s", serviceNetworkLayer.getXmlId());
    Nodes parentNodes = parentLayer.getNodes();
    PlanItException.throwIf(parentNodes==null || parentNodes.isEmpty(), "No parent nodes or empty parent nodes for service network layer %s", serviceNetworkLayer.getXmlId());    
    
    for(XMLElementServiceNodes.Servicenode xmlServiceNode : xmlServiceNodeList) {
                
      /* XML id */
      String xmlId = xmlServiceNode.getId();
      if(StringUtils.isNullOrBlank(xmlId)) {
        LOGGER.warning(String.format("IGNORE: Service node in service layer %s has no XML id defined", serviceNetworkLayer.getXmlId()));
        continue;
      }
      
      /* parent node XML id*/
      String parentNodeXmlId = xmlServiceNode.getNoderef();
      if(StringUtils.isNullOrBlank(parentNodeXmlId)) {
        LOGGER.warning(String.format("IGNORE: Service node %s in service layer %s has no parent node XML id reference defined", xmlId, serviceNetworkLayer.getXmlId()));
        continue;
      }                      
           
      if(!settings.getParentNodesByXmlId().containsKey(parentNodeXmlId)) {
        LOGGER.warning(String.format("IGNORE: Service node %s in service layer %s references unknown parent node %s", xmlId, serviceNetworkLayer.getXmlId(), parentNodeXmlId));
        continue;
      }
      
      /* instance */
      ServiceNode serviceNode = serviceNetworkLayer.getServiceNodes().getFactory().registerNew(settings.getParentNodesByXmlId().get(parentNodeXmlId));
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
    ServiceNetworkLayer serviceNetworkLayer = serviceNetwork.getTransportLayers().getFactory().registerNew(parentNetworkLayer);    
    
    /* XML id */
    String xmlId = xmlLayer.getId();
    if(StringUtils.isNullOrBlank(xmlId)) {
      LOGGER.warning(String.format("Service network layer has no XML id defined, adopting internally generated id %d instead", serviceNetwork.getId()));
      xmlId = String.valueOf(serviceNetworkLayer.getId());
    }
    serviceNetworkLayer.setXmlId(xmlId);
    
    /* external id*/
    if(!StringUtils.isNullOrBlank(xmlLayer.getExternalid())) {
      serviceNetworkLayer.setExternalId(xmlLayer.getExternalid());
    }    
    
    /* service nodes */
    parseServiceNodes(serviceNetworkLayer, xmlLayer.getServicenodes());
    
    /* service legs */
    parseServiceLegs(serviceNetworkLayer, xmlLayer.getServicelegs());    
    
    return serviceNetworkLayer;
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
   * In XML files we use the XML ids for referencing parent network entities. In memory internal ids are used for indexing, therefore
   * we keep a separate indices by XML within the reader to be able to quickly find entities by XML id if needed
   */
  private void initialiseParentNetworkReferenceIndices() {
    /* XML node ids are to be unique across all layers */
    if(settings.getParentNodesByXmlId() == null || settings.getParentNodesByXmlId().isEmpty()) {      
      settings.setParentNodesByXmlId(new HashMap<String, Node>());
      for(MacroscopicNetworkLayer layer : serviceNetwork.getParentNetwork().getTransportLayers()) {
        layer.getNodes().forEach( node -> settings.getParentNodesByXmlId().put(node.getXmlId(), node));
      }
    }
    /* XML link ids are to be unique across all layers */    
    if(settings.getParentLinksByXmlId() == null || settings.getParentLinksByXmlId().isEmpty()) {      
      settings.setParentLinksByXmlId(new HashMap<String, Link>());
      for(MacroscopicNetworkLayer layer : serviceNetwork.getParentNetwork().getTransportLayers()) {
        layer.getLinks().forEach( link -> settings.getParentLinksByXmlId().put(link.getXmlId(), link));
      }
    }    
  }

  /** Constructor where settings are directly provided such that input information can be extracted from it
   * 
   * @param idToken to use for the service network to populate
   * @param settings to use
   * @throws PlanItException  thrown if error
   */
  protected PlanitServiceNetworkReader(IdGroupingToken idToken, PlanitServiceNetworkReaderSettings settings) throws PlanItException{
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementServiceNetwork>(XMLElementServiceNetwork.class);
    this.settings = settings;
    this.serviceNetwork = new ServiceNetwork(idToken, settings.getParentNetwork());
  }  
  
  /** Constructor where settings and service network are directly provided
   * 
   * @param settings to use
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitServiceNetworkReader(PlanitServiceNetworkReaderSettings settings, ServiceNetwork serviceNetwork) throws PlanItException{
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
  protected PlanitServiceNetworkReader(XMLElementServiceNetwork populatedXmlRawServiceNetwork, ServiceNetwork serviceNetwork) throws PlanItException{
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
      initialiseParentNetworkReferenceIndices();      

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
