package org.goplanit.io.converter.service;

import org.goplanit.converter.IdMapperFunctionFactory;
import org.goplanit.converter.IdMapperType;
import org.goplanit.converter.service.RoutedServicesWriter;
import org.goplanit.io.converter.PlanitWriterImpl;
import org.goplanit.io.converter.network.PlanitNetworkWriter;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.service.routed.RoutedService;
import org.goplanit.utils.service.routed.RoutedTrip;
import org.goplanit.utils.service.routed.RoutedTripDeparture;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.xml.generated.*;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Writer to persist PLANit routed services to disk in the native PLANit format. By default the xml ids are used for writing out the ids in the XML.
 * 
 * @author markr
 *
 */
public class PlanitRoutedServicesWriter extends PlanitWriterImpl<RoutedServices> implements RoutedServicesWriter {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitRoutedServicesWriter.class.getCanonicalName());

  /** XML memory model equivalent of the PLANit memory model */
  private final XMLElementRoutedServices xmlRawRoutedServices;

  /** routed services writer settings to use */
  private final PlanitRoutedServicesWriterSettings settings;

  /* track logging prefix for current layer */
  private String currLayerLogPrefix;

  /** id mappers for routed services entities */
  private Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> routedServicesIdMappers;


  /** Populate the available routed services layers
   *
   * @param routedServices to extract layers from and populate xml
   */
  protected void populateXmlRoutedServicesLayers(RoutedServices routedServices) {

//    /* element is list because multiple are allowed in sequence to be registered */
//    var xmlServiceNetworkLayers = xmlRawRoutedServices.getServicenetworklayer();
//
//    LOGGER.info("Service network layers:" + serviceNetwork.getTransportLayers().size());
//    for(ServiceNetworkLayer serviceNetworkLayer : serviceNetwork.getTransportLayers()) {
//
//      /* XML id */
//      if(serviceNetworkLayer.getXmlId() == null) {
//        LOGGER.warning(String.format("Service network layer has no XML id defined, adopting internally generated id %d instead", serviceNetworkLayer.getId()));
//        serviceNetworkLayer.setXmlId(String.valueOf(serviceNetworkLayer.getId()));
//      }
//      this.currLayerLogPrefix = LoggingUtils.surroundwithBrackets("layer: "+serviceNetworkLayer.getXmlId());
//
//
//      var xmlServiceNetworkLayer = createAndPopulateXmlNetworkLayer(serviceNetworkLayer, serviceNetwork);
//      if(xmlServiceNetworkLayer != null) {
//        xmlServiceNetworkLayers.add(xmlServiceNetworkLayer);
//      }
//    }
  }

  /**
   * Populate the top level XML element for the routed services and include parent network reference. In case no XML id is set, attempt to salvage
   * with internal id's and log warnings for user verification of correctness.
   *
   * @param routedServices to use
   */
  private void populateTopLevelElement(RoutedServices routedServices) {
    /* xml id */
    if(!routedServices.hasXmlId()) {
      LOGGER.warning(String.format("Routed services has no XML id defined, adopting internally generated id %d instead",routedServices.getId()));
      routedServices.setXmlId(String.valueOf(routedServices.getId()));
    }
    xmlRawRoutedServices.setId(routedServices.getXmlId());

    /* parent id */
    //todo: move to layers element as this is not stored on top level element in routed services for some reason
    String parentNetworkXmlId = routedServices.getParentNetwork().getXmlId();
    if(StringUtils.isNullOrBlank(parentNetworkXmlId)) {
      LOGGER.severe(String.format("Routed services' parent network has no XML id defined, assuming internally generated id %d as reference id instead, please verify this matches persisted parent network id",routedServices.getParentNetwork().getId()));
      parentNetworkXmlId = String.valueOf(routedServices.getParentNetwork().getId());
    }
    xmlRawRoutedServices.setParentnetwork(parentNetworkXmlId);
  }

  /**
   * Collect how parent node's refs were mapped to the XML ids when persisting the parent network, use this mapping for our references as well
   * by using this function
   *
   * @return mapping from parent node to string (XML id to persist)
   */
  protected Function<Vertex, String> getParentNodeRefIdMapper(){
    return (Function<Vertex, String>) getParentIdMapperTypes().get(Vertex.class);
  }

  /**
   * Collect how parent service leg segment ids were mapped to the XML ids when persisting the parent network, use this mapping for our references as well
   * by using this function
   *
   * @return mapping from parent service leg segment to string (XML id to persist)
   */
  protected Function<ServiceLegSegment, String> getParentServiceLegSegmentRefIdMapper(){
    return (Function<ServiceLegSegment, String>) getParentIdMapperTypes().get(ServiceLegSegment.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void initialiseIdMappingFunctions() {
    this.routedServicesIdMappers = createPlanitRoutedServicesIdMappingTypes(getIdMapperType());

    if(!hasParentIdMapperTypes()){
      LOGGER.warning("id mapping from parent network unknown for routed services, generate mappings and assume same mapping approach as for routed services");
      var serviceNetworkMappings = PlanitServiceNetworkWriter.createPlanitServiceNetworkIdMappingTypes(getIdMapperType());
      setParentIdMapperTypes(serviceNetworkMappings);
    }
  }

  /** Constructor
   *
   * @param xmlRawRoutedServices to populate with PLANit routed services when persisting
   */
  protected PlanitRoutedServicesWriter(XMLElementRoutedServices xmlRawRoutedServices) {
    this(null, CountryNames.GLOBAL, xmlRawRoutedServices);
  }

  /** Constructor
   *
   * @param outputPath to persist in
   * @param xmlRawRoutedServices to populate with PLANit routed services when persisting
   */
  protected PlanitRoutedServicesWriter(String outputPath, XMLElementRoutedServices xmlRawRoutedServices) {
    this(outputPath, CountryNames.GLOBAL, xmlRawRoutedServices);
  }

  /** Constructor
   *
   * @param outputPath to persist in
   * @param countryName to optimise projection for (if available, otherwise ignore)
   * @param xmlRawRoutedServices to populate with PLANit routed services when persisting
   */
  protected PlanitRoutedServicesWriter(String outputPath, String countryName, XMLElementRoutedServices xmlRawRoutedServices) {
    super(IdMapperType.XML);
    this.settings = new PlanitRoutedServicesWriterSettings(outputPath, DEFAULT_ROUTED_SERVICES_XML, countryName);
    this.xmlRawRoutedServices = xmlRawRoutedServices;
  }

  /** default routed services file name to use */
  public static final String DEFAULT_ROUTED_SERVICES_XML = "routed_services.xml";

  /**
   * Create id mappers per type based on a given id mapping type
   *
   * @return newly created map with all zoning entity mappings
   */
  public static Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> createPlanitRoutedServicesIdMappingTypes(IdMapperType mappingType){
    var result = new HashMap<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>>();
    result.put(RoutedTrip.class, IdMapperFunctionFactory.createRoutedTripIdMappingFunction(mappingType));
    result.put(RoutedTripDeparture.class,  IdMapperFunctionFactory.createRoutedTripDepartureIdMappingFunction(mappingType));
    result.put(RoutedService.class, IdMapperFunctionFactory.createRoutedServiceIdMappingFunction(mappingType));
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(RoutedServices routedServices) throws PlanItException {

    /* initialise */
    initialiseIdMappingFunctions();
    LOGGER.info(String.format("Persisting PLANit routed services to: %s", Paths.get(getSettings().getOutputPathDirectory(), getSettings().getFileName())));
    getSettings().logSettings();
    
    /* xml id */
    populateTopLevelElement(routedServices); //todo rename and add parent

    /* network layers */
    populateXmlRoutedServicesLayers(routedServices);
    
    /* persist */
    super.persist(xmlRawRoutedServices, XMLElementRoutedServices.class, PlanitSchema.ROUTED_SERVICES_XSD);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    currLayerLogPrefix = null;
    xmlRawRoutedServices.getServicelayers().getServicelayer().clear();
    xmlRawRoutedServices.setServicelayers(null);
    xmlRawRoutedServices.setId(null);
    xmlRawRoutedServices.setExternalid(null);
  }  
  
  // GETTERS/SETTERS
  
  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitRoutedServicesWriterSettings getSettings() {
    return this.settings;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> getIdMapperByType() {
    return new HashMap<>(this.routedServicesIdMappers);
  }

}
