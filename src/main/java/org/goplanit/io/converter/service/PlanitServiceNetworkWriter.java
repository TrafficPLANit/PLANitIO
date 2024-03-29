package org.goplanit.io.converter.service;

import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.converter.idmapping.PlanitComponentIdMapper;
import org.goplanit.converter.idmapping.ServiceNetworkIdMapper;
import org.goplanit.converter.service.ServiceNetworkWriter;
import org.goplanit.io.converter.network.UnTypedPlanitCrsWriterImpl;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.service.*;
import org.goplanit.xml.generated.*;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Writer to persist a PLANit service network to disk in the native PLANit format. By default the xml ids are used for writing out the ids in the XML.
 * 
 * @author markr
 *
 */
public class PlanitServiceNetworkWriter extends UnTypedPlanitCrsWriterImpl<ServiceNetwork> implements ServiceNetworkWriter {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitServiceNetworkWriter.class.getCanonicalName());

  /** XML memory model equivalent of the PLANit memory model */
  private final XMLElementServiceNetwork xmlRawServiceNetwork;

  /** network writer settings to use */
  private final PlanitServiceNetworkWriterSettings settings;

  /* track logging prefix for current layer */
  private String currLayerLogPrefix;

  /**
   * Populate the XML element for the service leg segments of a service leg
   *
   * @param xmlLeg to add service leg segments to
   * @param leg parent leg of leg segment
   * @param serviceLegSegment to use
   */
  private void populateServiceLegSegments(XMLElementServiceLeg xmlLeg, ServiceLeg leg, ServiceLegSegment serviceLegSegment) {
    var legSegmentsList = xmlLeg.getLegsegment();
    XMLElementServiceLeg.Legsegment xmlElementLegSegment = new XMLElementServiceLeg.Legsegment();

    /* id */
    xmlElementLegSegment.setId(getPrimaryIdMapper().getServiceLegSegmentIdMapper().apply(serviceLegSegment));

    /* external id */
    if(serviceLegSegment.hasExternalId()) {
      xmlElementLegSegment.setExternalid(serviceLegSegment.getExternalId());
    }

    /* direction */
    xmlElementLegSegment.setDir( serviceLegSegment.isDirectionAb() ? Direction.A_B : Direction.B_A);

    /* physical parent link segments */
    if(!serviceLegSegment.hasPhysicalParentSegments()){
      LOGGER.warning(String.format("IGNORED: Service leg segment %s has no physical link segments referenced", xmlElementLegSegment.getId()));
      return;
    }

    /* physical segments refs, do not sort as ordering represents chain of adjacent link segments */
    String csvPhysicalLegSegmentRefs = serviceLegSegment.getPhysicalParentSegments().stream().map(ls ->
            getComponentIdMappers().getNetworkIdMappers().getLinkSegmentIdMapper().apply(MacroscopicLinkSegment.class.cast(ls))).collect(Collectors.joining(","));
    xmlElementLegSegment.setLsrefs(csvPhysicalLegSegmentRefs);


    legSegmentsList.add(xmlElementLegSegment);
  }

  /**
   * Populate the XML element for service leg
   *
   * @param xmlServiceLegList to add service leg to
   * @param leg to populate XML element with
   */
  private void populateXmlServiceLeg(List<XMLElementServiceLeg> xmlServiceLegList, ServiceLeg leg) {
    XMLElementServiceLeg xmlLeg = new XMLElementServiceLeg();

    /* XML id */
    xmlLeg.setId(getPrimaryIdMapper().getServiceLegIdMapper().apply(leg));

    /* external id */
    if(leg.hasExternalId()) {
      xmlLeg.setExternalid(leg.getExternalId());
    }

    /* node A ref */
    xmlLeg.setNodearef(getPrimaryIdMapper().getServiceNodeIdMapper().apply(leg.getServiceNodeA()));
    /* node B ref */
    xmlLeg.setNodebref(getPrimaryIdMapper().getServiceNodeIdMapper().apply(leg.getServiceNodeB()));

    /* link segments */
    leg.<ServiceLegSegment>forEachSegment( ls ->  populateServiceLegSegments(xmlLeg, leg, ls));

    xmlServiceLegList.add(xmlLeg);
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
    legs.streamSortedBy(getPrimaryIdMapper().getServiceLegIdMapper()).forEach( leg -> {
      leg.validate();
      populateXmlServiceLeg(xmlServiceLegList, leg);
    });
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
    xmlServiceNode.setId(getPrimaryIdMapper().getServiceNodeIdMapper().apply(serviceNode));

    /* external id */
    if(serviceNode.hasExternalId()) {
      xmlServiceNode.setExternalid(serviceNode.getExternalId());
    }

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
    serviceNodes.streamSortedBy(getPrimaryIdMapper().getServiceNodeIdMapper()).forEach( serviceNode ->
        populateXmlServiceNode(xmlServiceNodeList, serviceNode));
  }

  /**
   * Populate the network layer
   *
   * @param serviceNetworkLayer to populate from
   * @param serviceNetwork to extract from
   * @return populated new instance of service network layer, may be null in case no PLANit modes are supported for example
   */
  protected XMLElementServiceNetworkLayer createAndPopulateXmlNetworkLayer(
          ServiceNetworkLayer serviceNetworkLayer, ServiceNetwork serviceNetwork) {
    XMLElementServiceNetworkLayer xmlServiceNetworkLayer = new XMLElementServiceNetworkLayer();

    /* XML id */
    xmlServiceNetworkLayer.setId(getPrimaryIdMapper().getServiceNetworkLayerIdMapper().apply(serviceNetworkLayer));

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
    String parentLayerXmlId = getComponentIdMappers().getNetworkIdMappers().getNetworkLayerIdMapper().apply(serviceNetworkLayer.getParentNetworkLayer());
    if(StringUtils.isNullOrBlank(parentLayerXmlId)){
      throw new PlanItRunTimeException("Parent layer referenced by service network layer %s is required to have its (XML) id set, aborting", serviceNetworkLayer.getXmlId());
    }
    xmlServiceNetworkLayer.setParentlayerref(parentLayerXmlId);

    LOGGER.info(String.format("%s Supported modes : %s",
        currLayerLogPrefix, serviceNetworkLayer.getSupportedModes().stream().map( m -> m.toString()).sorted().collect(Collectors.joining(","))));

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
    serviceNetwork.getTransportLayers().streamSortedBy(getPrimaryIdMapper().getServiceNetworkLayerIdMapper()).forEach(serviceNetworkLayer -> {

      /* XML id */
      String xmlId = getPrimaryIdMapper().getServiceNetworkLayerIdMapper().apply(serviceNetworkLayer);
      if(StringUtils.isNullOrBlank(xmlId)) {
        LOGGER.warning(String.format("Service network layer has no XML id defined, adopting internally generated id %d instead", serviceNetworkLayer.getId()));
        xmlId = String.valueOf(serviceNetworkLayer.getId());
        serviceNetworkLayer.setXmlId(xmlId);
      }
      this.currLayerLogPrefix = LoggingUtils.surroundwithBrackets("sn-layer: " + xmlId);


      var xmlServiceNetworkLayer = createAndPopulateXmlNetworkLayer(serviceNetworkLayer, serviceNetwork);
      if(xmlServiceNetworkLayer != null) {
        xmlServiceNetworkLayers.add(xmlServiceNetworkLayer);
      }
    });
  }

  /**
   * Populate the top level XML element for the service network and include parent network reference. In case no XML id is set, attempt to salvage
   * with internal id's and log warnings for user verification of correctness.
   *
   * @param serviceNetwork to use
   */
  protected void populateTopLevelElement(ServiceNetwork serviceNetwork) {
    /* xml id */
    String xmlId = getPrimaryIdMapper().getServiceNetworkIdMapper().apply(serviceNetwork);
    if(StringUtils.isNullOrBlank(xmlId)) {
      LOGGER.warning(String.format("Service network has no XML id defined, adopting internally generated id %d instead",serviceNetwork.getId()));
      xmlId = String.valueOf(serviceNetwork.getId());
      serviceNetwork.setXmlId(xmlId);
    }
    xmlRawServiceNetwork.setId(xmlId);

    /* parent id */
    String parentNetworkXmlId = getComponentIdMappers().getNetworkIdMappers().getNetworkIdMapper().apply(serviceNetwork.getParentNetwork());
    if(StringUtils.isNullOrBlank(parentNetworkXmlId)) {
      LOGGER.severe(String.format("Service network's parent network has no XML id defined, assuming internally generated id %d as reference id instead, please verify this matches persisted parent network id",serviceNetwork.getParentNetwork().getId()));
      parentNetworkXmlId = String.valueOf(serviceNetwork.getParentNetwork().getId());
    }
    xmlRawServiceNetwork.setParentnetwork(parentNetworkXmlId);
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
    this.settings = new PlanitServiceNetworkWriterSettings(
        networkPath, PlanitServiceNetworkWriterSettings.DEFAULT_SERVICE_NETWORK_XML, countryName);
    this.xmlRawServiceNetwork = xmlRawServiceNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceNetworkIdMapper getPrimaryIdMapper() {
    return getComponentIdMappers().getServiceNetworkIdMapper();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(ServiceNetwork serviceNetwork) throws PlanItException {

    /* initialise */
    getComponentIdMappers().populateMissingIdMappers(getIdMapperType());
    prepareCoordinateReferenceSystem(serviceNetwork.getCoordinateReferenceSystem(), getSettings().getDestinationCoordinateReferenceSystem(), getCountryName());
    LOGGER.info(String.format("Persisting PLANit service network to: %s", Paths.get(getSettings().getOutputDirectory(), getSettings().getFileName())));
    getSettings().logSettings();
    
    /* xml id */
    populateTopLevelElement(serviceNetwork);

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


}
