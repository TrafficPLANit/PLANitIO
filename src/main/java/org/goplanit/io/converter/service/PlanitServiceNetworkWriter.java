package org.goplanit.io.converter.service;

import org.goplanit.converter.IdMapperType;
import org.goplanit.converter.service.ServiceNetworkWriter;
import org.goplanit.io.converter.PlanitWriterImpl;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegs;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.goplanit.utils.network.layer.service.ServiceNodes;
import org.goplanit.xml.generated.*;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Writer to persist a PLANit service network to disk in the native PLANit format. By default the xml ids are used for writing out the ids in the XML.
 * 
 * @author markr
 *
 */
public class PlanitServiceNetworkWriter extends PlanitWriterImpl<ServiceNetwork> implements ServiceNetworkWriter {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitServiceNetworkWriter.class.getCanonicalName());

  /** XML memory model equivalent of the PLANit memory model */
  private final XMLElementServiceNetwork xmlRawServiceNetwork;

  /** network writer settings to use */
  private final PlanitServiceNetworkWriterSettings settings;

  /* track logging prefix for current layer */
  private String currLayerLogPrefix;

  /** parent network (layer) used id mappings to use for parent refs, if not set, use the same mapping as used for service network */
  private Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> parentIdMapperByType;

  /**
   * Populate the XML element for service leg
   *
   * @param xmlServiceLegList to add service leg to
   * @param leg to populate XML element with
   */
  private void populateXmlServiceLeg(List<XMLElementServiceLeg> xmlServiceLegList, ServiceLeg leg) {
    //todo
    throw new PlanItRunTimeException("service leg writing not yet implemented, see service nodes for inspiration to continue");
  }

  /**
   * Populate the container element for Service legs and then populate each individual service leg in XML form
   *
   * @param xmlServiceNetworkLayer to populate with service legs
   * @param legs to populate XML container with
   */
  private void populateXmlServiceLegsAndLegSegments(XMLElementServiceNetworkLayer xmlServiceNetworkLayer, ServiceLegs legs) {
    XMLElementServiceLegs xmlServiceLegs = xmlServiceNetworkLayer.getServicelegs();
    if(xmlServiceLegs == null) {
      xmlServiceLegs = new XMLElementServiceLegs();
      xmlServiceNetworkLayer.setServicelegs(xmlServiceLegs);
    }

    /* service leg */
    final List<XMLElementServiceLeg> xmlServiceLegList = xmlServiceLegs.getLeg();
    for(var leg: legs) {
      leg.validate();
      populateXmlServiceLeg(xmlServiceLegList, leg);
    }
  }

  /**
   * Populate the XML element for service node
   *
   * @param xmlServiceNodeList to add service node to
   * @param serviceNode to populate XML element with
   */
  private void populateXmlServiceNode(List<XMLElementServiceNodes.Servicenode> xmlServiceNodeList, ServiceNode serviceNode) {
    XMLElementServiceNodes.Servicenode xmlServiceNode = new XMLElementServiceNodes.Servicenode();

    /* Xml id */
    xmlServiceNode.setId(getVertexIdMapper().apply(serviceNode));

    /* external id */
    if(serviceNode.hasExternalId()) {
      xmlServiceNode.setExternalid(serviceNode.getExternalId());
    }

    /* parent node ref */
    xmlServiceNode.setNoderef(getParentNodeRefIdMapper().apply(serviceNode.getPhysicalParentNode()));

    xmlServiceNodeList.add(xmlServiceNode);
  }

  /**
   * Populate the container element for Service nodes and then populate each individual service node in XML form
   *
   * @param xmlServiceNetworkLayer to populate with service nodes
   * @param serviceNodes to populate XML container with
   */
  private void populateXmlServiceNodes(XMLElementServiceNetworkLayer xmlServiceNetworkLayer, ServiceNodes serviceNodes) {
    XMLElementServiceNodes xmlServiceNodes = xmlServiceNetworkLayer.getServicenodes();
    if(xmlServiceNodes == null) {
      xmlServiceNodes = new XMLElementServiceNodes();
      xmlServiceNetworkLayer.setServicenodes(xmlServiceNodes);
    }

    /* node */
    final List<XMLElementServiceNodes.Servicenode> xmlServiceNodeList = xmlServiceNodes.getServicenode();
    serviceNodes.forEach( serviceNode -> populateXmlServiceNode(xmlServiceNodeList, serviceNode));
  }

  /**
   * Populate the network layer
   *
   * @param serviceNetworkLayer to populate from
   * @param serviceNetwork to extract from
   * @return populated new instance of service networklayer, may be null in case no PLANit modes are supported for example
   */
  protected XMLElementServiceNetworkLayer createAndPopulateXmlNetworkLayer(
          ServiceNetworkLayer serviceNetworkLayer, ServiceNetwork serviceNetwork) {
    XMLElementServiceNetworkLayer xmlServiceNetworkLayer = new XMLElementServiceNetworkLayer();

    /* XML id */
    xmlServiceNetworkLayer.setId(serviceNetworkLayer.getXmlId());

    /* External id */
    if(serviceNetworkLayer.hasExternalId()) {
      xmlServiceNetworkLayer.setExternalid(serviceNetworkLayer.getExternalId());
    }

    /* supported modes */
    if(!serviceNetworkLayer.hasSupportedModes()) {
      LOGGER.severe(String.format("%s Network layer has no supported modes, skip persistence",currLayerLogPrefix));
      return null;
    }

    /* parent layer reference */
    if(!serviceNetworkLayer.getParentNetworkLayer().hasXmlId()){
      throw new PlanItRunTimeException("Parent layer referenced by service network layer %s is required to have its XML id set, aborting", serviceNetworkLayer.getXmlId());
    }
    xmlServiceNetworkLayer.setParentlayerref(serviceNetworkLayer.getParentNetworkLayer().getXmlId());

    /* service nodes */
    LOGGER.info(String.format("%s Service nodes : %d", currLayerLogPrefix, serviceNetworkLayer.getServiceNodes().size()));
    populateXmlServiceNodes(xmlServiceNetworkLayer, serviceNetworkLayer.getServiceNodes());

    /* legs and leg segments */
    LOGGER.info(String.format("%s Service legs: %d", currLayerLogPrefix, serviceNetworkLayer.getLegs().size()));
    LOGGER.info(String.format("%s Service leg segments: %d", currLayerLogPrefix, serviceNetworkLayer.getLegSegments().size()));
    populateXmlServiceLegsAndLegSegments(xmlServiceNetworkLayer, serviceNetworkLayer.getLegs());

    return xmlServiceNetworkLayer;
  }

  /** Populate the available service network layers
   *
   * @param serviceNetwork to extract layers from and populate xml
   */
  protected void populateXmlServiceNetworkLayers(ServiceNetwork serviceNetwork) {

    /* element is list because multiple are allowed in sequence to be registered */
    var xmlServiceNetworkLayers = xmlRawServiceNetwork.getServicenetworklayer();

    LOGGER.info("Service network layers:" + serviceNetwork.getTransportLayers().size());
    for(ServiceNetworkLayer serviceNetworkLayer : serviceNetwork.getTransportLayers()) {

      /* XML id */
      if(serviceNetworkLayer.getXmlId() == null) {
        LOGGER.warning(String.format("Service network layer has no XML id defined, adopting internally generated id %d instead", serviceNetworkLayer.getId()));
        serviceNetworkLayer.setXmlId(String.valueOf(serviceNetworkLayer.getId()));
      }
      this.currLayerLogPrefix = LoggingUtils.surroundwithBrackets("layer: "+serviceNetworkLayer.getXmlId());


      var xmlServiceNetworkLayer = createAndPopulateXmlNetworkLayer(serviceNetworkLayer, serviceNetwork);
      if(xmlServiceNetworkLayer != null) {
        xmlServiceNetworkLayers.add(xmlServiceNetworkLayer);
      }
    }
  }

  /**
   * Populate the top level XML element for the service network and include parent network reference. In case no XML id is set, attempt to salvage
   * with internal id's and log warnings for user verification of correctness.
   *
   * @param serviceNetwork to use
   */
  private void populateTopLevelElement(ServiceNetwork serviceNetwork) {
    /* xml id */
    if(!serviceNetwork.hasXmlId()) {
      LOGGER.warning(String.format("Service network has no XML id defined, adopting internally generated id %d instead",serviceNetwork.getId()));
      serviceNetwork.setXmlId(String.valueOf(serviceNetwork.getId()));
    }
    xmlRawServiceNetwork.setId(serviceNetwork.getXmlId());

    /* parent id */
    String parentNetworkXmlId = serviceNetwork.getParentNetwork().getXmlId();
    if(StringUtils.isNullOrBlank(parentNetworkXmlId)) {
      LOGGER.severe(String.format("Service network's parent network has no XML id defined, assuming internally generated id %d as reference id instead, please verify this matches persisted parent network id",serviceNetwork.getParentNetwork().getId()));
      parentNetworkXmlId = String.valueOf(serviceNetwork.getParentNetwork().getId());
    }
    xmlRawServiceNetwork.setParentnetwork(parentNetworkXmlId);
  }

  /**
   * Collect how parent node's refs were mapped to the XML ids when persisting the parent network, use this mapping for our references as well
   * by using this function
   *
   * @return mapping from parent node to string (XML id to persist)
   */
  protected Function<Vertex, String> getParentNodeRefIdMapper(){
    return (Function<Vertex, String>) parentIdMapperByType.get(Vertex.class);
  }

  /**
   * Collect how parent node's refs were mapped to the XML ids when persisting the parent network, use this mapping for our references as well
   * by using this function
   *
   * @return mapping from parent node to string (XML id to persist)
   */
  protected Function<MacroscopicLinkSegment, String> getParentLinkSegmentRefIdMapper(){
    return (Function<MacroscopicLinkSegment, String>) parentIdMapperByType.get(MacroscopicLinkSegment.class);
  }


  /** Constructor
   *
   * @param xmlRawServiceNetwork to populate with PLANit network when persisting
   */
  protected PlanitServiceNetworkWriter(XMLElementServiceNetwork xmlRawServiceNetwork) {
    this(null, CountryNames.GLOBAL, xmlRawServiceNetwork);
  }

  /** Constructor
   *
   * @param networkPath to persist network on
   * @param xmlRawServiceNetwork to populate with PLANit service network when persisting
   */
  protected PlanitServiceNetworkWriter(String networkPath, XMLElementServiceNetwork xmlRawServiceNetwork) {
    this(networkPath, CountryNames.GLOBAL, xmlRawServiceNetwork);
  }

  /** Constructor
   *
   * @param networkPath to persist network on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   * @param xmlRawServiceNetwork to populate with PLANit service network when persisting
   */
  protected PlanitServiceNetworkWriter(String networkPath, String countryName, XMLElementServiceNetwork xmlRawServiceNetwork) {
    super(IdMapperType.XML);
    this.settings = new PlanitServiceNetworkWriterSettings(networkPath, DEFAULT_SERVICE_NETWORK_XML, countryName);
    this.xmlRawServiceNetwork = xmlRawServiceNetwork;
  }

  /** default network file name to use */
  public static final String DEFAULT_SERVICE_NETWORK_XML = "service_network.xml";

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(ServiceNetwork serviceNetwork) throws PlanItException {

    /* initialise */
    super.initialiseIdMappingFunctions();        
    super.prepareCoordinateReferenceSystem(serviceNetwork.getCoordinateReferenceSystem());
    LOGGER.info(String.format("Persisting PLANit service network to: %s",Paths.get(getSettings().getOutputPathDirectory(), getSettings().getFileName()).toString()));
    getSettings().logSettings();
    
    /* xml id */
    populateTopLevelElement(serviceNetwork); //todo rename and add parent

    /* network layers */
    populateXmlServiceNetworkLayers(serviceNetwork);
    
    /* persist */
    super.persist(xmlRawServiceNetwork, XMLElementServiceNetwork.class, PlanitSchema.SERVICE_NETWORK_XSD);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    currLayerLogPrefix = null;
    xmlRawServiceNetwork.getServicenetworklayer().clear();
  }  
  
  // GETTERS/SETTERS
  
  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitServiceNetworkWriterSettings getSettings() {
    return this.settings;
  }
  
  /** the country name of the network to write (if any is set)
   * 
   * @return country name, null if unknown
   */
  public String getCountryName() {
    return getSettings().getCountry();
  }

  /**
   * The explicit id mapping used by the parent network, so we use the appropriate referencing
   *
   * @param idMapperByType to use when dealing with parent network related references
   */
  public void setParentIdMapperTypes(final Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> idMapperByType) {
    parentIdMapperByType = idMapperByType;
  }

  public boolean hasParentIdMapperTypes() {
    return parentIdMapperByType != null && !parentIdMapperByType.isEmpty();
  }

  public Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> getParentIdMapperTypes() {
    return parentIdMapperByType;
  }
}
