package org.goplanit.io.converter.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.DoublePredicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.goplanit.converter.network.NetworkReaderImpl;
import org.goplanit.io.xml.network.physical.macroscopic.XmlMacroscopicNetworkLayerHelper;
import org.goplanit.io.xml.util.XmlEnumConversionUtil;
import org.goplanit.io.xml.util.PlanitXmlJaxbParser;
import org.goplanit.mode.ModeFeaturesFactory;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetworkModifierUtils;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.CharacterUtils;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.mode.*;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.AccessGroupProperties;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.intersection.CapacityFactorOrigin;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSignalSegment;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovements;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersections;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.SpatialIntersection;
import org.goplanit.utils.network.layer.macroscopic.SpatialMovement;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.xml.generated.v2.*;
import org.goplanit.xml.utils.PlanitNamespacePrefixMapper;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import net.opengis.gml.PointType;

/**
 * Implementation of the network reader for the PLANit XML native format
 * 
 * @author gman, markr
 *
 */
public class PlanitNetworkReader extends NetworkReaderImpl {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkReader.class.getCanonicalName());

  /** elements removed from the network schema: an intersection's {@code <segments>} lists, replaced by its
   * {@code <approaches>}, {@code <internalsegments>} and {@code <internalsignalsegments>} */
  private static final Set<QName> ELEMENTS_REMOVED_FROM_SCHEMA =
      Set.of(new QName(PlanitNamespacePrefixMapper.NAMESPACE_V2_URI, "segments"));
  
  /** the settings for this reader */
  private final PlanitNetworkReaderSettings settings;
  
  /** parses the XML content in JAXB memory format */
  private final PlanitXmlJaxbParser<org.goplanit.xml.generated.v2.XMLElementMacroscopicNetwork, ?> xmlParser;
          
  /** the network memory model to populate */
  private MacroscopicNetwork network;
    
  /**
   * initialise the XML id trackers, so we can lay indices on the XML id as well for quick lookups
   */
  private void initialiseXmlIdTrackers() {
    initialiseSourceIdMap(Mode.class, Mode::getXmlId);
    initialiseSourceIdMap(Link.class, Link::getXmlId);
    initialiseSourceIdMap(MacroscopicLinkSegment.class, MacroscopicLinkSegment::getXmlId);
    initialiseSourceIdMap(MacroscopicLinkSegmentType.class, MacroscopicLinkSegmentType::getXmlId);
    initialiseSourceIdMap(Node.class, Node::getXmlId);
  }

  /**
   * Initialise event listeners in case we want to make changes to the XML ids after parsing is complete, e.g.,
   * if the parsed network is going to be modified and saved to disk afterwards, then it is advisable to sync all XML
   * ids to the internal ids upon parsing because this avoids the risk of generating duplicate XML ids during editing
   * of the network (when XML ids are chosen to be synced to internal ids)
   */
  private void syncXmlIdsToIds() {
    LOGGER.info("Syncing PLANit physical network XML ids to internally generated ids, overwriting " +
        "original XML ids");
    MacroscopicNetworkModifierUtils.updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(this.network);
  }

  /**
   * Update the XML macroscopic network element to include default values for any properties not included in
   * the input file
   */
  private void injectMissingDefaultsToRawXmlNetwork() {
    XMLElementMacroscopicNetwork rootElement = xmlParser.getXmlRootElement();
    if (xmlParser.getXmlRootElement().getConfiguration() == null) {
      rootElement.setConfiguration(new XMLElementConfiguration());
    }
    
    //if no modes defined, create single mode with default values
    if (rootElement.getConfiguration().getModes() == null) {
      rootElement.getConfiguration().setModes(new XMLElementModes());
      org.goplanit.xml.generated.v2.Mode xmlElementMode = new org.goplanit.xml.generated.v2.Mode();
      // default in absence of any modes is the predefined CAR mode
      xmlElementMode.setPredefined(true);
      xmlElementMode.setName(PredefinedModeType.CAR.value());
      xmlElementMode.setId(Mode.DEFAULT_XML_ID);
      rootElement.getConfiguration().getModes().getModes().add(xmlElementMode);
    }
           
   }  
      
  /** parse the usability component of the mode xml element. It is assumed they should be present, if not default
   * values are created
   * @param generatedMode mode to extract information from
   * @return usabilityFeatures that are parsed
   */
  private UsabilityModeFeatures parseUsabilityModeFeatures(org.goplanit.xml.generated.v2.Mode generatedMode) {
    if(generatedMode.getUsabilityfeatures() == null) {
      return ModeFeaturesFactory.createDefaultUsabilityFeatures();
    }
    
    /* parse set values */
    UseOfModeType useOfModeType = XmlEnumConversionUtil.xmlToPlanit(generatedMode.getUsabilityfeatures().getUsedtotype());
    
    return ModeFeaturesFactory.createUsabilityFeatures(useOfModeType);
  }

  /** parse the physical features component of the mode xml element. It is assumed they should be present, if not
   * default values are created
   * @param generatedMode mode to extract information from
   * @return physicalFeatures that are parsed
   */  
  private PhysicalModeFeatures parsePhysicalModeFeatures(org.goplanit.xml.generated.v2.Mode generatedMode) {
    if(generatedMode.getPhysicalfeatures() == null) {
      return ModeFeaturesFactory.createDefaultPhysicalFeatures();
    }
    
    /* parse set values */
    VehicularModeType vehicleType =
            XmlEnumConversionUtil.xmlToPlanit(generatedMode.getPhysicalfeatures().getVehicletype());
    MotorisationModeType motorisationType =
            XmlEnumConversionUtil.xmlToPlanit(generatedMode.getPhysicalfeatures().getMotorisationtype());
    TrackModeType trackType =
            XmlEnumConversionUtil.xmlToPlanit(generatedMode.getPhysicalfeatures().getTracktype());
    
    return ModeFeaturesFactory.createPhysicalFeatures(vehicleType, motorisationType, trackType);
  }    
  
  /**
   * Reads mode types from input file, register them on the network and also populate mapping based on XML ids
   */
  private void parseModes(){
    
    final XMLElementConfiguration xmlGeneralConfiguration = xmlParser.getXmlRootElement().getConfiguration();    
    for (org.goplanit.xml.generated.v2.Mode xmlMode : xmlGeneralConfiguration.getModes().getModes()) {
      
      /* xml id */
      String modeXmlId = null;
      if(xmlMode.getId() != null && !xmlMode.getId().isBlank()) {
        modeXmlId = xmlMode.getId();
      }      

      //todo: brittle, we should use the predefined attribute to map to the name of the predefined instead
      /* name, generate unique name if undefined */
      String name = xmlMode.getName();
      String potentialPredefinedModeType = name;
      if(potentialPredefinedModeType==null) {
        potentialPredefinedModeType = modeXmlId;
      }
      PredefinedModeType modeType = PredefinedModeType.create(potentialPredefinedModeType);      
      if(!xmlMode.isPredefined() && modeType != PredefinedModeType.CUSTOM) {
        LOGGER.warning(String.format("Mode is not registered as predefined mode but name or xmlid corresponds to " +
                "PLANit predefined mode, reverting to PLANit predefined mode %s",modeType.name()));
      }
      if(xmlMode.isPredefined() && modeType == PredefinedModeType.CUSTOM) {
        LOGGER.warning(String.format("Mode is known as predefined mode but XML flag indicates it should be a" +
                " PLANit predefined mode, reverting to PLANit custom mode %s",modeType.name()));
      }
      if(name==null && modeType == PredefinedModeType.CUSTOM) {
        name = PredefinedModeType.CUSTOM.value().concat(String.valueOf(this.network.getModes().size()));
      }
                 
      Mode mode = null;
      if(modeType != PredefinedModeType.CUSTOM) {
        /* predefined mode use factory, ignore other attributes (if any) */
        mode = this.network.getModes().getFactory().registerNew(modeType);
      }else {
        
        /* custom mode, parse all components to correctly configure the custom mode */
        double maxSpeed = xmlMode.getMaxspeed()==null ? Mode.GLOBAL_DEFAULT_MAXIMUM_SPEED_KMH : xmlMode.getMaxspeed();
        double pcu = xmlMode.getPcu()==null ? Mode.GLOBAL_DEFAULT_PCU : xmlMode.getPcu();
        
        PhysicalModeFeatures physicalFeatures = parsePhysicalModeFeatures(xmlMode);
        UsabilityModeFeatures usabilityFeatures = parseUsabilityModeFeatures(xmlMode);        
                
        mode = this.network.getModes().getFactory().registerNewCustomMode(
                name, maxSpeed, pcu, physicalFeatures, usabilityFeatures);
      }     
            
      /* external id*/
      if(xmlMode.getExternalid() != null && !xmlMode.getExternalid().isBlank()) {
        mode.setExternalId(xmlMode.getExternalid());
      }      
      
      /* xml id */
      mode.setXmlId(modeXmlId);
      registerBySourceId(Mode.class, mode);      
    }
  }  
  
  /**
   * parse the CRS from the raw XML or utilise the default if not present
   * 
   * @param xmlLayers element from which to parse crs
   */
  private CoordinateReferenceSystem parseCoordinateRerefenceSystem(
          XMLElementInfrastructureLayers xmlLayers) {
    CoordinateReferenceSystem crs = null;
    crs = PlanitXmlJaxbParser.createPlanitCrs(xmlLayers.getSrsname());
    return crs;
  }    
  
  /** Parse the various network layers
   * 
   * @throws PlanItException thrown if error
   */
  private void parseNetworkLayers() throws PlanItException {
    XMLElementInfrastructureLayers xmlLayers = xmlParser.getXmlRootElement().getInfrastructurelayers();
    PlanItException.throwIfNull(xmlLayers, "infrastructurelayers element not present in network file");
    
    /* crs */
    CoordinateReferenceSystem crs = parseCoordinateRerefenceSystem(xmlLayers);
    if(crs != null){
      LOGGER.info(String.format("CRS set to: %s", crs.getName()));
      network.setCoordinateReferenceSystem(crs);
    }
    PlanitJtsCrsUtils jtsUtils = new PlanitJtsCrsUtils(network.getCoordinateReferenceSystem());
    
    /* layers */
    List<XMLElementInfrastructureLayer> xmlLayerList = xmlLayers.getLayers();
    Set<Mode> usedModes = new TreeSet<>();
    for(XMLElementInfrastructureLayer xmlLayer : xmlLayerList) {
      
      /*layer */
      NetworkLayer layer = parseNetworkLayer(xmlLayer, jtsUtils);
      
      /* validate supported modes */
      int prevSize = usedModes.size();
      usedModes.addAll(layer.getSupportedModes());
      if(usedModes.size() != prevSize + layer.getSupportedModes().size()) {
        /* mode used in other layer already, this is not allowed */
        throw new PlanItException("modes are only allowed to be used in a single network layer, not multiple," +
                " please check your network inputs");
      }
    }    
  }    
  
  /**
   * parse the network layer
   * 
   * @param xmlLayer layer to extract from
   * @param jtsUtils to use
   * @return parsed network layer
   * @throws PlanItException thrown if error
   *
   */
  private NetworkLayer parseNetworkLayer(XMLElementInfrastructureLayer xmlLayer, PlanitJtsCrsUtils jtsUtils )
          throws PlanItException {
    
    /* create layer */
    MacroscopicNetworkLayer networkLayer = network.getTransportLayers().getFactory().registerNew();
    
    /* xml id */
    if(xmlLayer.getId() != null && !xmlLayer.getId().isBlank()) {
      networkLayer.setXmlId(xmlLayer.getId());
    }else {
      LOGGER.warning("infrastructure layer id missing in xml, use generated id instead");
      networkLayer.setXmlId(Long.toString(networkLayer.getId()));
    }
    
    /* external id*/
    if(xmlLayer.getExternalid() != null && !xmlLayer.getExternalid().isBlank()) {
      networkLayer.setExternalId(xmlLayer.getExternalid());
    }  
          
    /* supported modes*/
    if(xmlLayer.getModes() != null && !xmlLayer.getModes().isBlank()) {
      String xmlSupportedModes = xmlLayer.getModes();
      String[] modeRefs = xmlSupportedModes.split(CharacterUtils.COMMA.toString());
      for(String mode : Arrays.asList(modeRefs)) {
        Mode planitMode = getBySourceId(Mode.class, mode);
        if(planitMode != null) {
          networkLayer.registerSupportedMode(planitMode);
        }else {
          LOGGER.severe(String.format("mode %s is not present on the network, ignored on network layer", mode));
        }
      }      
    }else {
      /* absent, so register all modes (check if this is valid is to be executed by caller */
      networkLayer.registerSupportedModes(network.getModes().valuesAsNewSet());
    }
    
    /* link segment types */
    XMLElementLayerConfiguration xmlLayerconfiguration = xmlLayer.getLayerconfiguration();
    if(xmlLayerconfiguration == null) {
      xmlLayer.setLayerconfiguration(new XMLElementLayerConfiguration());
      xmlLayerconfiguration = xmlLayer.getLayerconfiguration();
    }
    parseLinkSegmentTypes(xmlLayerconfiguration, networkLayer);
    
    /* parse nodes */
    parseNodes(xmlLayer, networkLayer);                  
         
    /* parse links, link segments */
    parseLinkAndLinkSegments(xmlLayer, networkLayer, jtsUtils);

    /* parse turns */
    parseTurns(xmlLayer, networkLayer);

    /* parse the spatial movements that belong to no intersection */
    parseIndependentMovements(xmlLayer, networkLayer);

    /* parse intersections */
    parseIntersections(xmlLayer, networkLayer);
    
    return networkLayer;
  }

  /**
   * Parse a comma-separated list of mode ids
   *
   * @param modeRefs to parse, may be null or blank
   * @return the modes, empty when none are listed, null when one is unknown
   */
  private List<Mode> parseModeRefs(String modeRefs) {
    if(StringUtils.isNullOrBlank(modeRefs)){
      return List.of();
    }
    var modes = Arrays.stream(modeRefs.split(CharacterUtils.COMMA.toString()))
        .map(modeRef -> getBySourceId(Mode.class, modeRef.trim())).collect(Collectors.toList());
    return modes.contains(null) ? null : modes;
  }

  /**
   * Parse the member nodes of an intersection of either kind. A file written before an intersection named its node as
   * noderef, or a spatial intersection its nodes as noderefs, stops reading with an error. None are returned, with a
   * single warning, when the intersection lists a node that is unknown, or a node that belongs to an intersection read
   * earlier
   *
   * @param xmlId of the intersection
   * @param nodeRefs as listed, comma-separated
   * @param nodeRefsAttribute the name of the attribute listing them, for the error
   * @param intersections read so far
   * @return member nodes in the order listed, null when the intersection is to be discarded
   */
  private List<Node> parseIntersectionMemberNodes(
      String xmlId, String nodeRefs, String nodeRefsAttribute, Intersections intersections) {
    PlanItRunTimeException.throwIf(StringUtils.isNullOrBlank(nodeRefs),
        "Intersection %s has no %s, the attribute naming its nodes", xmlId, nodeRefsAttribute);

    var memberNodes = new ArrayList<Node>();
    for(var nodeRef : nodeRefs.split(CharacterUtils.COMMA.toString())){
      var node = getBySourceId(Node.class, nodeRef.trim());
      if(node == null){
        LOGGER.warning(String.format("DISCARD: Intersection %s references unknown node %s", xmlId, nodeRef.trim()));
        return null;
      }
      var other = intersections.getByMemberNode(node);
      if(other != null){
        LOGGER.warning(String.format("DISCARD: Intersection %s shares node %s with intersection %s read earlier",
            xmlId, nodeRef.trim(), other.getXmlId()));
        return null;
      }
      if(!memberNodes.contains(node)){
        memberNodes.add(node);
      }
    }
    return memberNodes;
  }

  /**
   * Find a link segment listed by an intersection, with a single warning when it is unknown
   *
   * @param xmlId of the intersection
   * @param segmentRef as listed
   * @return the link segment, null when unknown
   */
  private MacroscopicLinkSegment findIntersectionSegment(String xmlId, String segmentRef) {
    var segment = getBySourceId(MacroscopicLinkSegment.class, segmentRef.trim());
    if(segment == null){
      LOGGER.warning(String.format("IGNORE: Intersection %s lists unknown segment %s", xmlId, segmentRef.trim()));
    }
    return segment;
  }

  /**
   * Set the green ratio and saturation flow factor of an approach, internal segment with a signal or movement, those
   * present in the input, as set directly. A value outside 0 to 1, which its setter refuses, is skipped with a single
   * warning, so the default applies, and so is a green ratio listed for an approach without a signal
   *
   * @param greenRatio as read, null when left out
   * @param saturationFlowFactor as read, null when left out
   * @param greenRatioSetter sets the green ratio, false when it refuses the value, null when there is no signal
   * @param saturationFlowFactorSetter sets the saturation flow factor, false when it refuses the value
   * @param context where the values are listed, for the warning
   */
  private void parseCapacityFactors(Double greenRatio, Double saturationFlowFactor, DoublePredicate greenRatioSetter,
      DoublePredicate saturationFlowFactorSetter, Supplier<String> context) {
    if(greenRatio != null && greenRatioSetter == null){
      LOGGER.warning(String.format("IGNORE: %s lists green ratio %.2f, but has no signal", context.get(), greenRatio));
    }else if(greenRatio != null && !greenRatioSetter.test(greenRatio)){
      LOGGER.warning(String.format("IGNORE: %s lists green ratio %s outside 0 to 1", context.get(), greenRatio));
    }
    if(saturationFlowFactor != null && !saturationFlowFactorSetter.test(saturationFlowFactor)){
      LOGGER.warning(String.format("IGNORE: %s lists saturation flow factor %s outside 0 to 1",
          context.get(), saturationFlowFactor));
    }
  }

  /**
   * Parse the approaches of an intersection of either kind, with whether each has a signal of its own where that
   * differs from the control type, and its green ratio and saturation flow factor. A segment that is unknown, or that
   * the intersection refuses as an approach, is skipped with a single warning
   *
   * @param xmlId of the intersection
   * @param xmlIntersection to parse from, of either kind
   * @param intersection to add the approaches to
   */
  private void parseIntersectionApproaches(
      String xmlId, XMLElementIntersectionBase xmlIntersection, Intersection intersection) {
    var xmlApproaches = xmlIntersection.getApproaches();
    if(xmlApproaches != null){
      for(var xmlApproach : xmlApproaches.getApproaches()){
        var segment = findIntersectionSegment(xmlId, xmlApproach.getRef());
        if(segment == null){
          continue;
        }
        var hasOwnSignal = xmlApproach.isSignal() != null ? xmlApproach.isSignal() : intersection.isSignalised();
        var approach = intersection.addNewApproach(segment, hasOwnSignal);
        if(approach == null){
          LOGGER.warning(String.format("IGNORE: Intersection %s refuses approach %s (it does not enter the " +
              "intersection, or is listed twice)", xmlId, xmlApproach.getRef().trim()));
          continue;
        }
        parseCapacityFactors(xmlApproach.getGreenratio(), xmlApproach.getSatflowfactor(),
            approach instanceof IntersectionSignalSegment ? value -> ((IntersectionSignalSegment) approach)
                .setGreenRatio(value, CapacityFactorOrigin.SET_DIRECTLY) : null,
            value -> approach.setSaturationFlowFactor(value, CapacityFactorOrigin.SET_DIRECTLY),
            () -> String.format("Intersection %s, approach %s", xmlId, xmlApproach.getRef().trim()));
      }
    }
  }

  /**
   * Parse the plain internal segments and the internal segments with a signal of a spatial intersection, the latter
   * with their green ratio and saturation flow factor. A segment that is unknown, or that the intersection refuses for
   * its role, is skipped with a single warning. A segment listed both as a plain internal segment and as one with a
   * signal is kept with a signal, with a warning
   *
   * @param xmlIntersection to parse from
   * @param intersection to add the internal segments to
   */
  private void parseIntersectionInternalSegments(
      XMLElementSpatialIntersection xmlIntersection, SpatialIntersection intersection) {
    var xmlId = xmlIntersection.getId();
    var xmlInternalSegments = xmlIntersection.getInternalsegments();
    if(xmlInternalSegments != null && !StringUtils.isNullOrBlank(xmlInternalSegments.getRefs())){
      for(var segmentRef : xmlInternalSegments.getRefs().split(CharacterUtils.COMMA.toString())){
        var segment = findIntersectionSegment(xmlId, segmentRef);
        if(segment != null && !intersection.addNewInternalSegment(segment)){
          LOGGER.warning(String.format("IGNORE: Intersection %s refuses internal segment %s (it does not run " +
              "between its nodes, or is listed twice)", xmlId, segmentRef.trim()));
        }
      }
    }

    if(xmlIntersection.getInternalsignalsegments() != null){
      for(var xmlSignalSegment : xmlIntersection.getInternalsignalsegments().getInternalsignalsegments()){
        var segment = findIntersectionSegment(xmlId, xmlSignalSegment.getRef());
        if(segment == null){
          continue;
        }
        if(intersection.getInternalSegment(segment.getId()) != null &&
            !ListUtils.containsByIdentity(intersection.getInternalSegmentsWithSignal(), segment)){
          LOGGER.warning(String.format("Intersection %s lists segment %s both as a plain internal segment and as " +
              "one with a signal, kept with a signal", xmlId, xmlSignalSegment.getRef().trim()));
        }
        if(!intersection.addNewInternalSegmentWithSignal(segment)){
          LOGGER.warning(String.format("IGNORE: Intersection %s refuses internal segment with a signal %s (it does " +
              "not run between its nodes, or is listed twice)", xmlId, xmlSignalSegment.getRef().trim()));
          continue;
        }
        var withSignal = (IntersectionSignalSegment) intersection.getIntersectionInternalSegment(segment);
        parseCapacityFactors(xmlSignalSegment.getGreenratio(), xmlSignalSegment.getSatflowfactor(),
            value -> withSignal.setGreenRatio(value, CapacityFactorOrigin.SET_DIRECTLY),
            value -> withSignal.setSaturationFlowFactor(value, CapacityFactorOrigin.SET_DIRECTLY),
            () -> String.format("Intersection %s, internal segment with a signal %s",
                xmlId, xmlSignalSegment.getRef().trim()));
      }
    }
  }

  /**
   * Create a spatial movement from its xml element, not registered: its link segments, length, when there is one the
   * ban placed on it directly, and its green ratio and saturation flow factor when listed. Banned modes imply a direct
   * ban. A movement with an unknown segment or mode, or
   * without the fromlsref or tolsref naming its link segments, is not created, with a single warning
   *
   * @param xmlMovement to parse
   * @param spatialMovements whose factory creates the movement
   * @param context where the movement is listed, for the warning
   * @return the movement, null when it is not created
   */
  private SpatialMovement createSpatialMovement(
      XMLElementMovement xmlMovement, SpatialMovements spatialMovements, String context) {
    var fromRef = xmlMovement.getFromlsref();
    var toRef = xmlMovement.getTolsref();
    var from = StringUtils.isNullOrBlank(fromRef) ? null : getBySourceId(MacroscopicLinkSegment.class, fromRef.trim());
    var to = StringUtils.isNullOrBlank(toRef) ? null : getBySourceId(MacroscopicLinkSegment.class, toRef.trim());
    var bannedModes = parseModeRefs(xmlMovement.getBannedmoderefs());
    if(from == null || to == null || bannedModes == null){
      LOGGER.warning(String.format("IGNORE: %s lists movement %s with an unknown segment or mode",
          context, xmlMovement.getId()));
      return null;
    }
    var movement = spatialMovements.getFactory().create(from, to, xmlMovement.getLength());
    if(Boolean.TRUE.equals(xmlMovement.isDirectban()) || !bannedModes.isEmpty()){
      movement.banDirectly(bannedModes);
    }
    parseCapacityFactors(xmlMovement.getGreenratio(), xmlMovement.getSatflowfactor(), movement::setGreenRatio,
        movement::setSaturationFlowFactor, () -> String.format("%s, movement %s", context, xmlMovement.getId()));
    movement.setXmlId(xmlMovement.getId());
    if(!StringUtils.isNullOrBlank(xmlMovement.getExternalid())){
      movement.setExternalId(xmlMovement.getExternalid());
    }
    return movement;
  }

  /**
   * Parse the movements of a spatial intersection, once its approaches are parsed. A movement that cannot be created,
   * see {@link #createSpatialMovement(XMLElementMovement, SpatialMovements, String)}, or whose from segment is not an
   * approach of the intersection, is skipped with a single warning
   *
   * @param xmlIntersection to parse from
   * @param intersection to add the movements to
   * @param spatialMovements to register the movements on
   */
  private void parseSpatialIntersectionMovements(XMLElementSpatialIntersection xmlIntersection,
      SpatialIntersection intersection, SpatialMovements spatialMovements) {
    if(xmlIntersection.getMovements() == null){
      return;
    }
    var xmlId = xmlIntersection.getId();
    for(var xmlMovement : xmlIntersection.getMovements().getMovements()){
      var movement = createSpatialMovement(xmlMovement, spatialMovements, "Intersection " + xmlId);
      if(movement == null){
        continue;
      }
      if(!intersection.addMovement(movement)){
        LOGGER.warning(String.format("IGNORE: Intersection %s refuses movement %s (it does not start on one of its " +
            "approaches, or is listed twice)", xmlId, xmlMovement.getId()));
        continue;
      }
      if(intersection.isMovementGreenRatioBelowOneWithoutSignal(movement)){
        LOGGER.warning(String.format(
            "Intersection %s, movement %s lists green ratio %.2f, but its approach has no signal", xmlId,
            xmlMovement.getId(), movement.getGreenRatio()));
      }
      spatialMovements.register(movement);
    }
  }

  /**
   * Parse the kinds of an intersection, with a single warning when it has no known control or kind
   *
   * @param xmlId of the intersection
   * @param xmlIntersection to parse from, of either kind
   * @return the kinds, null when the intersection is to be discarded
   */
  private List<IntersectionType> parseIntersectionKinds(String xmlId, XMLElementIntersectionBase xmlIntersection) {
    var kinds = xmlIntersection.getTypes().stream().filter(Objects::nonNull).map(
        XmlEnumConversionUtil::xmlToPlanit).collect(Collectors.toList());
    if(xmlIntersection.getControl() == null || kinds.isEmpty()){
      LOGGER.warning(String.format("DISCARD: Intersection %s has no known control or kind", xmlId));
      return null;
    }
    return kinds;
  }

  /**
   * Register an intersection built completely, with its ids
   *
   * @param intersection to register
   * @param xmlId as read, null when missing
   * @param externalId as read, may be null or blank
   * @param intersections to register on
   */
  private void registerParsedIntersection(
      Intersection intersection, String xmlId, String externalId, Intersections intersections) {
    if(xmlId == null){
      LOGGER.warning("Intersection is missing a unique id, salvage by syncing to internal id, fix as may result " +
          "in undefined behaviour");
      xmlId = String.valueOf(intersection.getId());
    }
    intersection.setXmlId(xmlId);
    if(!StringUtils.isNullOrBlank(externalId)){
      intersection.setExternalId(externalId);
    }
    intersections.getFactory().register(intersection);
  }

  /** Parse the link segment types
   * 
   * @param xmlLayerconfiguration to extract them from
   * @param networkLayer to register them on
   * @throws PlanItException thrown if error
   */
  public void parseLinkSegmentTypes(
          final XMLElementLayerConfiguration xmlLayerconfiguration,
          final MacroscopicNetworkLayer networkLayer) throws PlanItException {
    
    /* link segment types */
    if(xmlLayerconfiguration.getLinksegmenttypes() == null) {
      /* inject default */
      XmlMacroscopicNetworkLayerHelper.injectDefaultLinkSegmentType(xmlLayerconfiguration);
    }
    
    double defaultMaxSpeedKph = Double.MAX_VALUE;
    for(Mode mode : networkLayer.getSupportedModes()) {
      defaultMaxSpeedKph = Math.max(defaultMaxSpeedKph, mode.getMaximumSpeedKmH());
    }
                     
    var xmlLinkSegmentTypes = xmlLayerconfiguration.getLinksegmenttypes().getLinksegmenttypes();
    for(XMLElementLinkSegmentType xmlLinkSegmentType : xmlLinkSegmentTypes) {
      
      /* xml id */
      String xmlId = xmlLinkSegmentType.getId();
      
      /* external id */
      String externalId = null;
      if(xmlLinkSegmentType.getExternalid() != null && !xmlLinkSegmentType.getExternalid().isBlank()) {
        externalId = xmlLinkSegmentType.getExternalid();
      }        
      
      /* name */
      String name = xmlLinkSegmentType.getName();
      /* capacity (may be null in which case the default is returned if required or it is retrieved elsewhere
      via for example the fundamental diagram which is able to
       * deliver a better estimate based on the free flow speed and FD shape) */
      Double capacityPcuPerHour = xmlLinkSegmentType.getCapacitylane();
      /* max density (may be null) */
      Double maximumDensityPcuPerKm = xmlLinkSegmentType.getMaxdensitylane();
        
      /* create and register */
      MacroscopicLinkSegmentType linkSegmentType = null;
      if(xmlLinkSegmentType.getCapacitylane() !=null && xmlLinkSegmentType.getMaxdensitylane()!=null) {
        linkSegmentType = networkLayer.getLinkSegmentTypes().getFactory().registerNew(
                name, capacityPcuPerHour, maximumDensityPcuPerKm);
      }else if(xmlLinkSegmentType.getCapacitylane() !=null ) {
        linkSegmentType = networkLayer.getLinkSegmentTypes().getFactory().registerNewWithCapacity(
                name, capacityPcuPerHour);
      }else if(xmlLinkSegmentType.getMaxdensitylane()!=null) {
        linkSegmentType = networkLayer.getLinkSegmentTypes().getFactory().registerNewWithMaxDensity(
                name, maximumDensityPcuPerKm);
      }else {
        linkSegmentType = networkLayer.getLinkSegmentTypes().getFactory().registerNew(name);
      }
      linkSegmentType.setXmlId(xmlId);
      linkSegmentType.setExternalId(externalId);
      
      registerBySourceId(MacroscopicLinkSegmentType.class, linkSegmentType);
      
      /* only road going modes are added by default if no access groups are specified */
      Collection<Mode> supportedDefaultRoadModes = networkLayer.getSupportedModes().stream().filter( 
          mode -> mode.getPhysicalFeatures().getTrackType() == TrackModeType.ROAD).collect(Collectors.toSet());
            
      /* mode properties, only set when allowed, otherwise not */       
      if(xmlLinkSegmentType.getAccess() != null) {
        List<XMLElementAccessGroup> xmlAccessGroups = xmlLinkSegmentType.getAccess().getAccessgroups();
        for (XMLElementAccessGroup xmlAccessGroup : xmlAccessGroups) {                  
          /* mode access properties */
          parseLinkSegmentTypeAccessProperties(xmlAccessGroup, linkSegmentType, supportedDefaultRoadModes);                                 
        }          
      }else {
        /* all ROAD modes allowed */
        parseLinkSegmentTypeAccessProperties(
                null /*results in supportedDefaultRoadModes mode access in single group*/,
                linkSegmentType,
                supportedDefaultRoadModes);
      }
    }
 
  } 
  
  /** parse the mode properties for given link segment type and populate the helper with them
  *
  * @param xmlAccessGroupProperties to extract information from on (TODO:ugly, helper should be removed)
  * @param linkSegmentType to register mode properties on
   * @param defaultModes to allow when no modes are specified
  * @throws PlanItException thrown if error
  */
 public void parseLinkSegmentTypeAccessProperties(
         XMLElementAccessGroup xmlAccessGroupProperties,
         MacroscopicLinkSegmentType linkSegmentType,
         final Collection<Mode> defaultModes) throws PlanItException{
   
   /* access modes */
   Collection<Mode> accessModes;
   if(xmlAccessGroupProperties==null || xmlAccessGroupProperties.getModerefs()==null) {
     /* all default modes allowed */
     accessModes = defaultModes;
   }else {
     String[] xmlModesRefArray = xmlAccessGroupProperties.getModerefs().split(",");
     accessModes = new TreeSet<>();
     for(int index = 0 ;index < xmlModesRefArray.length;++index) {
       Mode thePlanitMode = getBySourceId(Mode.class, xmlModesRefArray[index]);
       PlanItException.throwIfNull(thePlanitMode, String.format("Referenced mode (xml id:%s) does not exist in " +
               "PLANit parser",xmlModesRefArray[index]));
       accessModes.add(thePlanitMode);
     }      
   }    
   
   Collection<Mode> alreadyAllowedModes = linkSegmentType.getAllowedModes();
   if(!Collections.disjoint(alreadyAllowedModes, accessModes)) {
     LOGGER.warning(String.format("Access (mode) groups for link segment type %s have overlapping modes, " +
             "undefined behaviour of which properties prevail for duplicate modes",linkSegmentType.getXmlId()));
   }   
      
   /* mode properties link segment type speed settings */
   AccessGroupProperties groupProperties = null;
   if(xmlAccessGroupProperties != null) {
     
     if(xmlAccessGroupProperties.getMaxspeed() != null && xmlAccessGroupProperties.getCritspeed() != null) {
       groupProperties = AccessGroupPropertiesFactory.create(
               xmlAccessGroupProperties.getMaxspeed(), xmlAccessGroupProperties.getCritspeed(), accessModes);
     }else if(xmlAccessGroupProperties.getMaxspeed() != null) {
       groupProperties = AccessGroupPropertiesFactory.create(xmlAccessGroupProperties.getMaxspeed(), accessModes);
     }else if(xmlAccessGroupProperties.getCritspeed() != null) {
       LOGGER.warning(String.format("IGNORE: Not allowed to only set a critical speed for an access group (link " +
               "segment type %s)",linkSegmentType.getXmlId()));
     }          
   }
   
   if(groupProperties==null) {
     /* register without setting any speed information, which means they are to be derived from the mode and or
     links most restrictive information on-the-fly instead*/
     groupProperties = AccessGroupPropertiesFactory.create(accessModes);   
   }
   linkSegmentType.setAccessGroupProperties(groupProperties);       
 }    
    
  /**
   * Create and register nodes on the network
   * 
   * @param xmlLayer to extract from
   * @param networkLayer to populate
   */
  public void parseNodes(XMLElementInfrastructureLayer xmlLayer, MacroscopicNetworkLayer networkLayer) {
        
    /* parse nodes */
    for (org.goplanit.xml.generated.v2.Node xmlNode : xmlLayer.getNodes().getNodes()) {

      Node node = networkLayer.getNodes().getFactory().registerNew();
      
      /* xml id */
      if(xmlNode.getId() != null && !xmlNode.getId().isBlank()) {
        node.setXmlId(xmlNode.getId());
      }

      /* external id */
      if(xmlNode.getExternalid() != null && !xmlNode.getExternalid().isBlank()) {
        node.setExternalId(xmlNode.getExternalid());
      }
                  
      PointType pointType = xmlNode.getPoint();
      if (pointType != null) {
        List<Double> posValues = pointType.getPos().getValue();
        Point centrePointGeometry = PlanitJtsUtils.createPoint(posValues.get(0), posValues.get(1));
        node.setPosition(centrePointGeometry);
      }
      registerBySourceId(Node.class, node);
    }
  }  
  
  /** parse link and link segments
   * @param xmlLayer layer to extract them from
   * @param networkLayer to register them on
   * @param jtsUtils for length calculations based on crs
   * @throws PlanItException thrown if error
   */
  public void parseLinkAndLinkSegments(
      XMLElementInfrastructureLayer xmlLayer, MacroscopicNetworkLayer networkLayer, PlanitJtsCrsUtils jtsUtils)
          throws PlanItException {

    /* links */
    XMLElementLinks xmlLinks = xmlLayer.getLinks();
    PlanItException.throwIfNull(xmlLinks, "links xml element missing");
    
    for (org.goplanit.xml.generated.v2.Link xmlLink : xmlLinks.getLinks()) {
      
      /* LINK */
      MacroscopicLink link;
      {
        /* xml id */
        if(StringUtils.isNullOrBlank(xmlLink.getId())) {
          LOGGER.severe("IGNORE: Link has no (XML) id, unable to include link");
          continue;          
        }          
        String xmlId = xmlLink.getId();
        
        if(StringUtils.isNullOrBlank(xmlLink.getNodearef())){
          LOGGER.warning(String.format("IGNORE: No node A reference present on link %s",xmlId));
          continue;
        }        
        Node startNode = getBySourceId(Node.class, xmlLink.getNodearef());

        if(StringUtils.isNullOrBlank(xmlLink.getNodebref())){
          LOGGER.warning(String.format("IGNORE: No node B reference present on link %s",xmlId));
          continue;
        }         
        Node endNode = getBySourceId(Node.class, xmlLink.getNodebref());
        
        /* geometry */
        LineString theLineString = XmlMacroscopicNetworkLayerHelper.parseLinkGeometry(xmlLink);        
        double length = XmlMacroscopicNetworkLayerHelper.parseLength(xmlLink, theLineString, jtsUtils);   
        link = networkLayer.getLinks().getFactory().registerNew(startNode, endNode, length, true /* register on nodes */);
        link.setXmlId(xmlLink.getId());
        link.setGeometry(theLineString);                      
        
        /* external id */
        if(xmlLink.getExternalid() != null && !xmlLink.getExternalid().isBlank()) {
          link.setExternalId(xmlLink.getExternalid());
        }
        
        /* name */
        if(!StringUtils.isNullOrBlank(xmlLink.getName())) {
          link.setName(xmlLink.getName());
        }

        /* input properties */
        if(xmlLink.getCustom() != null){
          //todo only supporting string values, no way of enforcing a type (needs to be part of schema)
          var xmlCustomProperties = xmlLink.getCustom();
          var xmlEntryElements = xmlCustomProperties.getEntryElements();
          xmlEntryElements.forEach( entry -> link.addInputProperty(entry.getKeyAttribute(), entry.getValue()));
        }

        /* validate link */
        link.validate();
            
        registerBySourceId(Link.class, link);
      }      
      /* end LINK */
      
      boolean isFirstLinkSegment = true;
      boolean firstLinkDirection = true;

      /* LINK SEGMENT **/
      for (XMLElementLinkSegment xmlLinkSegment : xmlLink.getLinksegments()) {
        
        /* direction */
        boolean abDirection = xmlLinkSegment.getDir().equals(Direction.A_B);
        if (!isFirstLinkSegment) {
          if (abDirection == firstLinkDirection) {
            throw new PlanItException("Both link segments for the same link are in the same direction.  " +
                    "Link segment external Id is " + xmlLinkSegment.getId());
          }
        }        

        MacroscopicLinkSegment linkSegment = networkLayer.getLinkSegments().getFactory().registerNew(
                link, abDirection, true /* register on nodes and link*/);
            
        /* xml id */
        if(xmlLinkSegment.getId() != null && !xmlLinkSegment.getId().isBlank()) {
          linkSegment.setXmlId(xmlLinkSegment.getId());
        }else {
          LOGGER.severe("DISCARD: Link segment has no (XML) id, unable to include link segment");
          continue;
        }
        
        /* external id */
        if(xmlLinkSegment.getExternalid() != null && !xmlLinkSegment.getExternalid().isBlank()) {
          linkSegment.setExternalId(xmlLinkSegment.getExternalid());
        }         
        
        /* max speed */
        double maxSpeed = (xmlLinkSegment.getMaxspeed() == null) ?
                Double.POSITIVE_INFINITY : xmlLinkSegment.getMaxspeed();
        linkSegment.setPhysicalSpeedLimitKmH(maxSpeed);
        
        /* lanes */
        int noLanes = (xmlLinkSegment.getNumberoflanes() == null) ?
                LinkSegment.DEFAULT_NUMBER_OF_LANES : xmlLinkSegment.getNumberoflanes().intValue();
        linkSegment.setNumberOfLanes(noLanes);    
        
        registerBySourceId(MacroscopicLinkSegment.class, linkSegment);
        
        /* LINK SEGMENT TYPE */
        
        /* link segment type xml id */
        String linkSegmentTypeXmlId;
        if (xmlLinkSegment.getTyperef() == null) {
          if (networkLayer.getLinkSegmentTypes().size() > 1) {
            throw new PlanItException("Link Segment " + xmlLinkSegment.getId() + " has no link segment type " +
                    "defined, but there is more than one possible link segment type");
          }
          linkSegmentTypeXmlId = networkLayer.getLinkSegmentTypes().getFirst().getXmlId();
        } else {
          linkSegmentTypeXmlId = xmlLinkSegment.getTyperef();
        }  
        
        /* register type on link */
        MacroscopicLinkSegmentType linkSegmentType =
                getBySourceId(MacroscopicLinkSegmentType.class, linkSegmentTypeXmlId);
        if(linkSegmentType == null) {
          throw new PlanItException(String.format("Link segment type %s, unknown, cannot be registered on " +
                  "link segment %s",linkSegmentTypeXmlId,linkSegment));
        }
        linkSegment.setLinkSegmentType(linkSegmentType);    
        
        isFirstLinkSegment = false;
        firstLinkDirection = abDirection;        
      }
      /* end LINK SEGMENT */
    }
  }

  /**
   * Parse turns
   *
   * @param xmlLayer to parse from
   * @param networkLayer to populate
   */
  protected void parseTurns(XMLElementInfrastructureLayer xmlLayer, MacroscopicNetworkLayer networkLayer) {
    var xmlTurns = xmlLayer.getTurns();
    if(xmlTurns == null){
      return;
    }
    var xmlTurnBans = xmlTurns.getBen(); // should be bans, jaxb - naming bug
    if(xmlTurnBans == null){
      return;
    }

    var bannedTurnFactory = networkLayer.getBannedTurns().getFactory();
    for(var xmlBan : xmlTurnBans){
      var planitTurnXmlId = xmlBan.getId();

      /* written before banned turns named their link segments as fromlsref and tolsref: stop rather than guess */
      var planitFromSegmentXmlId = xmlBan.getFromlsref();
      var planitToSegmentXmlId = xmlBan.getTolsref();
      PlanItRunTimeException.throwIf(StringUtils.isNullOrBlank(planitFromSegmentXmlId),
          "From segment id (fromlsref) for turn ban (%s) null or blank", planitTurnXmlId);
      PlanItRunTimeException.throwIf(StringUtils.isNullOrBlank(planitToSegmentXmlId),
          "To segment id (tolsref) for turn ban (%s) null or blank", planitTurnXmlId);
      var fromSegment = getBySourceId(MacroscopicLinkSegment.class,planitFromSegmentXmlId);
      var toSegment = getBySourceId(MacroscopicLinkSegment.class,planitToSegmentXmlId);
      var bannedModes = parseModeRefs(xmlBan.getModerefs());
      if(bannedModes == null){
        LOGGER.warning(String.format("IGNORE: Turn ban %s lists an unknown mode", planitTurnXmlId));
        continue;
      }

      var bannedTurn = bannedTurnFactory.registerNew(fromSegment, toSegment, bannedModes);
      if(planitTurnXmlId == null){
        LOGGER.warning("Banned turn is missing a unique id, salvage by syncing to internal id, fix as may result " +
            "in undefined behaviour");
        planitTurnXmlId = String.valueOf(bannedTurn.getId());
      }
      bannedTurn.setXmlId(planitTurnXmlId);
    }
  }

  /**
   * Parse the spatial movements of the layer that belong to no intersection. Such a movement is stored only because a
   * ban is placed on it directly, so one without a direct ban is skipped with a single warning, as is one that cannot
   * be created, see {@link #createSpatialMovement(XMLElementMovement, SpatialMovements, String)}
   *
   * @param xmlLayer to parse from
   * @param networkLayer to populate
   */
  protected void parseIndependentMovements(
      XMLElementInfrastructureLayer xmlLayer, MacroscopicNetworkLayer networkLayer) {
    if(xmlLayer.getIndependentmovements() == null){
      return;
    }
    var spatialMovements = networkLayer.getSpatialMovements();
    for(var xmlMovement : xmlLayer.getIndependentmovements().getMovements()){
      var movement = createSpatialMovement(xmlMovement, spatialMovements, "Independent movements");
      if(movement == null){
        continue;
      }
      if(!movement.hasDirectBan()){
        LOGGER.warning(String.format("IGNORE: Independent movement %s has no direct ban, the only reason to store one",
            xmlMovement.getId()));
        continue;
      }
      spatialMovements.register(movement);
    }
  }

  /**
   * Parse the intersections of the layer, once its nodes and link segments are registered. Each intersection is built
   * completely before it is registered. It is discarded, with a single warning, when it has no known control or kind,
   * or when its nodes cannot be used; see {@link #parseIntersectionMemberNodes}
   *
   * @param xmlLayer to parse from
   * @param networkLayer to populate
   */
  protected void parseIntersections(XMLElementInfrastructureLayer xmlLayer, MacroscopicNetworkLayer networkLayer) {
    var xmlIntersections = xmlLayer.getIntersections();
    if(xmlIntersections == null){
      return;
    }

    var intersections = networkLayer.getIntersections();
    for(var xmlIntersection : xmlIntersections.getIntersections()){
      var xmlId = xmlIntersection.getId();
      var memberNodes = parseIntersectionMemberNodes(xmlId, xmlIntersection.getNoderef(), "noderef", intersections);
      var kinds = parseIntersectionKinds(xmlId, xmlIntersection);
      if(memberNodes == null || kinds == null){
        continue;
      }
      if(memberNodes.size() != 1){
        LOGGER.warning(String.format("DISCARD: Intersection %s lists more than one node in noderef", xmlId));
        continue;
      }

      var intersection = intersections.getFactory().createNonSpatial(
          memberNodes.get(0), XmlEnumConversionUtil.xmlToPlanit(xmlIntersection.getControl()), kinds.get(0));
      kinds.forEach(intersection::addType);
      parseIntersectionApproaches(xmlId, xmlIntersection, intersection);
      registerParsedIntersection(intersection, xmlId, xmlIntersection.getExternalid(), intersections);
    }

    for(var xmlIntersection : xmlIntersections.getSpatialintersections()){
      var xmlId = xmlIntersection.getId();
      var memberNodes = parseIntersectionMemberNodes(xmlId, xmlIntersection.getNoderefs(), "noderefs", intersections);
      var kinds = parseIntersectionKinds(xmlId, xmlIntersection);
      if(memberNodes == null || kinds == null){
        continue;
      }

      var intersection = intersections.getFactory().createSpatial(
          memberNodes, XmlEnumConversionUtil.xmlToPlanit(xmlIntersection.getControl()), kinds);
      parseIntersectionApproaches(xmlId, xmlIntersection, intersection);
      parseIntersectionInternalSegments(xmlIntersection, intersection);
      parseSpatialIntersectionMovements(xmlIntersection, intersection, networkLayer.getSpatialMovements());
      registerParsedIntersection(intersection, xmlId, xmlIntersection.getExternalid(), intersections);
    }
  }

  /** Place network to populate
   * 
   * @param network to populate
   */
  protected void setNetwork(final LayeredNetwork<?,?> network){
    /* currently we only support macroscopic infrastructure networks */
    if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItRunTimeException("Currently the PLANit network reader only supports macroscopic infrastructure" +
              " networks, the provided network is not of this type");
    }
    
    this.network = (MacroscopicNetwork) network;
  }
  
  /** Constructor where settings are directly provided such that input information can be exracted from it
   * 
   * @param settings to use
   * @param idToken to use for the network to populate
   */
  protected PlanitNetworkReader(PlanitNetworkReaderSettings settings, IdGroupingToken idToken){
    this(settings, new MacroscopicNetwork(idToken));
  }
  
  /** Constructor where settings are directly provided such that input information can be extracted from it
   * 
   * @param settings to use
   * @param network to populate
   */
  protected PlanitNetworkReader(PlanitNetworkReaderSettings settings, LayeredNetwork<?,?> network){
    super();
    this.xmlParser = new PlanitXmlJaxbParser<>(
        org.goplanit.xml.generated.v2.XMLElementMacroscopicNetwork.class,
        org.goplanit.xml.generated.v1.XMLElementMacroscopicNetwork.class);
    this.xmlParser.setElementsRemovedFromSchema(ELEMENTS_REMOVED_FROM_SCHEMA);
    this.settings = settings;
    setNetwork(network);
  }  
    
  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit
   * memory model
   * 
   * @param externalXmlRawNetwork to extract from
   * @param network to populate
   */
  protected PlanitNetworkReader(
      XMLElementMacroscopicNetwork externalXmlRawNetwork, LayeredNetwork<?,?> network){
    this(externalXmlRawNetwork, new PlanitNetworkReaderSettings(), network);
  }

  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit
   *  memory model
   *
   * @param externalXmlRawNetwork to extract from
   * @param network to populate
   * @param settings to use
   */
  protected PlanitNetworkReader(
          XMLElementMacroscopicNetwork externalXmlRawNetwork,
          PlanitNetworkReaderSettings settings,
          LayeredNetwork<?,?> network){
    this(settings, network);
    this.xmlParser.setXmlRootElement(externalXmlRawNetwork);
  }
  
  /** Constructor
   * 
   * @param networkPathDirectory to use
   * @param xmlFileExtension to use
   * @param network to populate
   */
  protected PlanitNetworkReader(
      String networkPathDirectory, String xmlFileExtension, LayeredNetwork<?,?> network){
    this(new PlanitNetworkReaderSettings(networkPathDirectory, xmlFileExtension), network);
  }
  
  /** Default XSD files used to validate input XML files against, TODO: move to properties file */
  public static final String NETWORK_XSD_FILE =
          "https://goplanit.org/xsd/v2/macroscopicnetworkinput.xsd";

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetwork read(){
        
    /* parse the XML raw network to extract PLANit network from */
    try {
      xmlParser.initialiseAndParseXmlRootElement(
          getSettings().getInputDirectory(), getSettings().getXmlFileExtension());
    } catch (IllegalStateException e) {
      /* a file holding an element removed from the schema */
      throw new PlanItRunTimeException(e.getMessage(), e);
    }
    PlanItRunTimeException.throwIfNull(xmlParser.getXmlRootElement(),
            "No valid PLANit XML network could be parsed into memory, abort");
    
    /* xml id */
    String networkXmlId = xmlParser.getXmlRootElement().getId();
    if(StringUtils.isNullOrBlank(networkXmlId)) {
      LOGGER.warning(String.format("Network has no XML id defined, adopting internally generated id %d instead",
              network.getId()));
      networkXmlId = String.valueOf(network.getId());
    }
    network.setXmlId(networkXmlId);
            
    /* defaults */
    initialiseXmlIdTrackers();
    injectMissingDefaultsToRawXmlNetwork();

    // log settings
    getSettings().logSettings();
    
    try {
      
      /* parse modes*/
      parseModes();

      /* parse layers */
      parseNetworkLayers();

      if(getSettings().isSyncXmlIdsToIds()){
        syncXmlIdsToIds();
      }

      /* log stats */
      network.logInfo(LoggingUtils.networkPrefix(network.getId()));
      
      /* free xml content */
      xmlParser.clearXmlContent();
      
    } catch (PlanItException e) {
      throw new PlanItRunTimeException(e);
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException("Error while populating physical network in PLANitIO",e);
    }    
    
    return network;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitNetworkReaderSettings getSettings() {
    return settings;
  }
  
  /**
   * returns the first link segment for which the given external id matches. Extremely slow, because it is not
   * indexed at the moment. Also external ids are not guaranteed to be unique so if multiple matches exist problems
   * may arise
   * 
   * @param network    to look in
   * @param externalId to look for
   * @return link segment
   */
  public MacroscopicLinkSegment getLinkSegmentByExternalId(MacroscopicNetwork network, String externalId) {
    for (MacroscopicNetworkLayer layer : network.getTransportLayers()) {
      MacroscopicLinkSegment firstMatch =
              layer.getLinkSegments().firstMatch(ls -> externalId.equals(ls.getExternalId()));
      if (firstMatch != null) {
        return firstMatch;
      }
    }
    return null;
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // TODO Auto-generated method stub    
  }
  
  


}
