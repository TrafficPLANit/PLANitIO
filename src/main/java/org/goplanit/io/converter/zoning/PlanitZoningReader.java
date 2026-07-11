package org.goplanit.io.converter.zoning;

import net.opengis.gml.CoordinatesType;
import net.opengis.gml.LineStringType;
import net.opengis.gml.LinearRingType;
import net.opengis.gml.PolygonType;
import org.goplanit.converter.BaseReaderImpl;
import org.goplanit.converter.network.NetworkReader;
import org.goplanit.converter.zoning.ZoningReader;
import org.goplanit.io.xml.util.PlanitXmlJaxbParser;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.misc.CharacterUtils;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.Modes;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.zoning.*;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.utils.zoning.connectoid.*;
import org.goplanit.xml.generated.v2.*;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.ZoningModifierUtils;
import org.locationtech.jts.geom.Geometry;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import java.util.*;
import java.util.logging.Logger;

import static org.goplanit.xml.mapstruct.TransferConnectoidV1ToV2Mapper.TO_BE_POPULATED_FROM_LSREFS_DOWNSTREAM;
import static org.goplanit.xml.mapstruct.TransferConnectoidV1ToV2Mapper.TO_BE_POPULATED_FROM_LSREFS_UPSTREAM;


/**
 * Class to parse zoning from native XML format
 * 
 * @author markr
 *
 */
public class PlanitZoningReader extends BaseReaderImpl<Zoning> implements ZoningReader {
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitZoningReader.class.getCanonicalName());
  
  /** parses the xml content in JAXB memory format */
  private final PlanitXmlJaxbParser<org.goplanit.xml.generated.v2.XMLElementMacroscopicZoning, ?> xmlParser;

  /**
   * Initialise event listeners in case we want to make changes to the XML ids after parsing is complete, e.g., if the
   * parsed
   * zoning is going to be modified and saved to disk afterwards, then it is advisable to sync all XML ids to the
   * internal ids upon parsing
   * because this avoids the risk of generating duplicate XML ids during editing of the network (when XML ids are
   * chosen to be synced to internal ids)
   */
  private void syncXmlIdsToIds() {
    LOGGER.info("Syncing PLANit zoning XML ids to internally generated ids, overwriting original XML ids");
    ZoningModifierUtils.updateAndSyncManagedIdEntitiesContainerXmlIdsToIds(zoning);
  }
  
  /**
   * initialise the XML id trackers and populate them for the network references, 
   * so we can lay indices on the XML id as well for quick lookups
   * 
   * @param network to use
   */
  private void initialiseParentNetworkXmlIdTrackers(MacroscopicNetwork network) {    
    initialiseSourceIdMap(Node.class, Node::getXmlId);
    network.getTransportLayers().forEach( layer -> getSourceIdContainer(Node.class).addAll(layer.getNodes()));    
    initialiseSourceIdMap(MacroscopicLinkSegment.class, MacroscopicLinkSegment::getXmlId);
    network.getTransportLayers().forEach( layer -> getSourceIdContainer(MacroscopicLinkSegment.class).addAll(
        layer.getLinkSegments()));
  }  
  
  /**
   * initialise the XML id trackers for the to be populated zoning entities, so we can lay indices on the XML
   * id as well for quick lookups
   */
  private void initialiseXmlIdTrackers() {
    initialiseSourceIdMap(Zone.class, Zone::getXmlId);
    initialiseSourceIdMap(Connectoid.class, Connectoid::getXmlId);
  }  
    
  /** Parse passed in transfer zone type
   * 
   * @param xmlTransferZone to parse
   * @return PLANit equivalent of the transfer zone type
   */
  private static TransferZoneType parseTransferZoneType(final Transferzonetype xmlTransferZone ) {
    
    if(xmlTransferZone==null) {
      return TransferZoneType.NONE;
    }else {
      switch (xmlTransferZone) {
      case PLATFORM:
        return TransferZoneType.PLATFORM;
      case STOP_POLE:
        return TransferZoneType.POLE;
      case UNKNOWN:
        return TransferZoneType.UNKNOWN;
      case NONE:
        return TransferZoneType.NONE;        
      default:
        LOGGER.warning(String.format("Unsupported transfer stop type %s found, changed to `unknown`",
            xmlTransferZone.value()));
        return TransferZoneType.UNKNOWN;
      }
    }
    
  }
  
  /** Parse passed in connectoid type, if not specified revert to UNKNOWN
   * 
   * @param xmlConnectoidType to parse
   * @return PLANit equivalent of the transfer zone type
   */  
  private static ZoneConnectoidType parseZoneConnectoidType(final Connectoidtypetype xmlConnectoidType) {
    
    if(xmlConnectoidType==null) {
      return ZoneConnectoidType.NONE;
    }else {
      switch (xmlConnectoidType) {
        case ACCESS_EGRESS:
          return ZoneConnectoidType.ZONE_ACCESS_EGRESS;
        case PT_VEH_STOP:
        return ZoneConnectoidType.PT_VEHICLE_STOP;
        case ACCESS:
          return ZoneConnectoidType.ZONE_ACCESS;
        case EGRESS:
          return ZoneConnectoidType.ZONE_EGRESS;
        case NONE:
          return ZoneConnectoidType.NONE;
        default:
          LOGGER.warning(String.format("Unknown connectoid type %s found, changed to `unknown`",
              xmlConnectoidType.value()));
          return ZoneConnectoidType.UNKNOWN;
      }
    }
  }

  /**
   * Parse modes and populate entry provided
   * @param connectoid at hand
   * @param planitZoneAccessEntry to use
   * @param xmlModesRef modes to parse
   * @param planitModesByXmlId mode mapping
   */
  private static void populateConnectoidZoneEntryModes(
      Connectoid connectoid,
      ConnectoidAccessZoneEntry planitZoneAccessEntry,
      String xmlModesRef, Map<String, Mode> planitModesByXmlId) {

    // when no modes, all are implicitly allowed, so do nothing
    if(!StringUtils.isNullOrBlank(xmlModesRef)) {
      /* capture explicit referenced modes by xml id */
      for(String xmlModeRef : List.of(xmlModesRef.split(","))){
        Mode mode = planitModesByXmlId.get(xmlModeRef);
        if(mode == null) {
          LOGGER.warning(String.format("Invalid mode %s referenced by connectoid (%s)",
              xmlModeRef, connectoid.getIdsAsString()));
          continue;
        }
        planitZoneAccessEntry.addExplicitAllowedMode(mode);
      }
    }
  }
  
  /** Public Transport to parse the geometry of the zone if any is provided
   * 
   * @param zone to populate geometry on
   * @param xmlPolygon to extract it from, or
   * @param xmlLineString to extract it from
   */
  private static void populateZoneGeometry(
      final Zone zone, final PolygonType xmlPolygon, final LineStringType xmlLineString) {
    
    Geometry geometry = null;
    if(xmlPolygon != null) {
      if(xmlPolygon.getExterior() == null) {
        LOGGER.warning(String.format("zones only support polygon geometries with an outer exterior, however " +
                "this is missing for zone %s",zone.getXmlId()));
      }else {
        if(xmlPolygon.getExterior().getValue().getRing() == null) {
          LOGGER.warning(String.format("expected ring element missing within polygon exterior element for zone %s",
                  zone.getXmlId()));
        }else if(xmlPolygon.getExterior().getValue().getRing().getValue() instanceof LinearRingType) {
          /* found the actual content */
          LinearRingType xmlLinearRing = (LinearRingType) xmlPolygon.getExterior().getValue().getRing().getValue();
          geometry = PlanitJtsUtils.create2DPolygon(xmlLinearRing.getPosList().getValue());
        }else {
          LOGGER.warning(String.format("expected linear ring within polygon exterior element for zone %s, " +
                  "but different ring type was encountered",zone.getXmlId()));
        }                    
      }
    }else if(xmlLineString != null) {
      if(xmlLineString.getCoordinates() != null) {
        CoordinatesType ct = xmlLineString.getCoordinates();
        geometry = PlanitJtsUtils.createLineStringFromCsvString(ct.getValue(), ct.getTs(), ct.getCs());
      }else if(xmlLineString.getPosList()!=null) {
        geometry = PlanitJtsUtils.createLineString(xmlLineString.getPosList().getValue());
      }
    }
    zone.setGeometry(geometry);    
  }  
  
  /** Parse the geometry of the zone if any is provided
   * 
   * @param zone to populate geometry on
   * @param xmlPolygon to extract it from
   */
  private static void populateZoneGeometry(
      final Zone zone, final PolygonType xmlPolygon) {
    populateZoneGeometry(zone, xmlPolygon, null);
  }  
  
  /** Given the passed in connectoid, xml connectoid information and reference position (if any) determine the length
   * to each of the available access zones of the connectoid (assumed already registered)
   * 
   * @param connectoid to register lengths on
   * @param xmlConnectoid to extract explicit length from (if any)
   */
  private static void populateOdConnectoidToZoneLength(
      final OdConnectoid connectoid,
      final XMLElementConnectoid xmlConnectoid){

    /* Explicitly set length (apply to all access zones */
    Double connectoidLength = null;
    if (xmlConnectoid.getLength() != null) {
      connectoidLength = xmlConnectoid.getLength().doubleValue();
      for(var accessZoneEntryByType : connectoid.getAccessZoneEntriesByType().values()) {
        for(var entry : accessZoneEntryByType.values()) {
          connectoid.getAccessZoneEntry(entry.getAccessZone(), entry.getType()).setLengthKm(connectoidLength);
        }
      }
    }
  }    
  
  /**
   * geometric utility class based on network crs 
   */
  private PlanitJtsCrsUtils jtsUtils = null;  
  
  /**
   * Parse common properties of a zone regardless if it is an od or transfer zone
   * 
   * @param zone to register on
   * @param xmlId to use
   * @param externalId to use (can be null)
   * @param name to use (can be null)
   * @param xmlCentroid to extract centroid from
   */
  private void parseBaseZone(
      final Zone zone,
      final String xmlId,
      final String externalId,
      final String name,
      final XMLElementCentroid xmlCentroid) {
    
    /* xml id */
    if(!StringUtils.isNullOrBlank(xmlId)) {
      zone.setXmlId(xmlId);
    }else {
      throw new PlanItRunTimeException("Zone cannot be parsed, its (XML) id is not set");
    }
    /* all zones regardless of subtype are expected to have unique ids */
    registerBySourceId(Zone.class, zone);
    
    /* external id */        
    if(externalId != null && !externalId.isBlank()) {
      zone.setExternalId(externalId);  
    }  
    
    /* name */
    if(!StringUtils.isNullOrBlank(name)) {
      zone.setName(name);
    }    
      
    
    /* centroid (optional location) */
    Centroid centroid = zone.getCentroid();
    if (xmlCentroid !=null) {
      
      /* name */
      if(xmlCentroid.getName()!= null) {
        centroid.setName(xmlCentroid.getName());
      }
      
      /* position */
      if(xmlCentroid.getPoint() != null) {
        List<Double> value = xmlCentroid.getPoint().getPos().getValue();        
        centroid.setPosition(PlanitJtsUtils.createPoint(value.get(0), value.get(1)));
      }
    }
  }

  /**
   * Legacy version aware parsing logic
   * <p>
   *   in v1 node ref was not present for od connectoids nor transfer ocnnectoid because it was derived from the link
   *   segment. In v2 this is changed. This means in the mapping from v1 to v2 we do not yet have the ability to
   *   populate that field. Hence we do it here after the network has been parsed
   * </p>
   */
  private Node parseConnectoidAccessNode(Connectoidtype xmlConnectoid) {
    String accessNodeRef = xmlConnectoid.getNoderef();
    if(accessNodeRef == null){
      throw new PlanItRunTimeException(String.format("AccessNode XML id for connectoid (Id:%s) is missing",
          xmlConnectoid.getId()));
    }

    // legacy V1 logic conversion
    if(accessNodeRef.equals(TO_BE_POPULATED_FROM_LSREFS_DOWNSTREAM) ||
        accessNodeRef.equals(TO_BE_POPULATED_FROM_LSREFS_UPSTREAM)){
      // special case from legacy v1 --> obtain from single entry access zone link segment
      if(xmlConnectoid.getAccesszones().size()<=0){
        throw new PlanItRunTimeException(
            String.format("Expected at least one access entry when parsing connectoid %s",
                xmlConnectoid.getId()));
      }
      var xmlLinkSegmentRefs = xmlConnectoid.getAccesszones().get(0).getLsrefs();
      if(xmlLinkSegmentRefs == null || StringUtils.isNullOrBlank(xmlLinkSegmentRefs)){
        throw new PlanItRunTimeException(
            String.format("Expected at least one reference access link segment when parsing connectoid %s",
                xmlConnectoid.getId()));
      }
      var lsRef0 = xmlLinkSegmentRefs.split(String.valueOf(CharacterUtils.COMMA))[0];
      var linkSegment = getBySourceId(MacroscopicLinkSegment.class, lsRef0);
      if(linkSegment == null){
        throw new PlanItRunTimeException(
            String.format("Expected reference access link segment %s to be available when parsing connectoid %s",
                lsRef0, xmlConnectoid.getId()));
      }
      accessNodeRef = accessNodeRef.equals(TO_BE_POPULATED_FROM_LSREFS_UPSTREAM) ?
          linkSegment.getUpstreamNode().getXmlId() : linkSegment.getDownstreamNode().getXmlId();
    }

    Node accessNode = getBySourceId(Node.class, accessNodeRef);
    if(accessNode == null) {
      throw new PlanItRunTimeException(String.format("Provided accessNode XML id %s is invalid given " +
              "available nodes in network when parsing transfer connectoid %s",
          accessNodeRef, xmlConnectoid.getId()));
    }
    return accessNode;
  }

  /**
   * Parse the connectoid based on the XML connectoid element
   * 
   * @param xmlConnectoid to be parsed
   * @param accessNode to use
   * @return created connectoid
   */
  private Connectoid parseBaseConnectoid(final Connectoidtype xmlConnectoid, Node accessNode) {

    /* xml id */
    String xmlId = null;
    if(!StringUtils.isNullOrBlank(xmlConnectoid.getId())) {
      xmlId = xmlConnectoid.getId();
    }else {
      LOGGER.severe("DISCARD: Parsed connectoid has no (XML) id");
      return null;
    }

    Connectoid theConnectoid = null;
    /* CONNECTOID */
    if(xmlConnectoid instanceof XMLElementConnectoid) {
      theConnectoid = zoning.getOdConnectoids().getFactory().registerNew(accessNode);
    }else if(xmlConnectoid instanceof XMLElementTransferConnectoid) {
      theConnectoid = zoning.getTransferConnectoids().getFactory().registerNew(accessNode);
    }else{
      throw new PlanItRunTimeException("Unsupported XML connectoid type encountered, abort");
    }

    /* XML id */
    theConnectoid.setXmlId(xmlId);

    /* external id */
    if(xmlConnectoid.getExternalid() != null && !xmlConnectoid.getExternalid().isBlank()) {
      theConnectoid.setExternalId(xmlConnectoid.getExternalid());
    }

    /* name */
    if(xmlConnectoid.getName() != null && !xmlConnectoid.getName().isBlank()) {
      theConnectoid.setName(xmlConnectoid.getName());
    }

    return theConnectoid;
  }

  /** Parse a transfer group based on provided XML element and register on zoning's transfer zone groups
   * 
   * @param xmlTransferGroup to parse
   * @return transfer zone group parsed(and registered)
   */
  private TransferZoneGroup parseTransferGroup(final XMLElementTransferGroup xmlTransferGroup) {
    /* register new */
    TransferZoneGroup transferGroup = zoning.getTransferZoneGroups().getFactory().registerNew();
    
    /* xm id */
    transferGroup.setXmlId(xmlTransferGroup.getId());
    
    /* external id */
    if(xmlTransferGroup.getExternalid()!=null && !xmlTransferGroup.getExternalid().isBlank()) {
      transferGroup.setExternalId(xmlTransferGroup.getExternalid());
    }
    
    /* name */
    if(xmlTransferGroup.getName() != null && !xmlTransferGroup.getName().isBlank()) {
      transferGroup.setName(xmlTransferGroup.getName());
    }    
    
    /* transfer zones */
    String[] transferZoneRefsByXmlId = StringUtils.splitByAnythingExceptAlphaNumeric(xmlTransferGroup.getTzrefs());
    for (String transferZoneXmlId : transferZoneRefsByXmlId) {

        /* transfer zone */
        TransferZone transferZone = (TransferZone) getBySourceId(Zone.class, transferZoneXmlId);
        if (transferZone == null) {
            LOGGER.warning(String.format("Transfer zone group %s (id:%d) references transfer zone %s that is " +
                            "not available in the parser, transfer zone ignored",
                    transferGroup.getXmlId(), transferGroup.getId(), transferZoneRefsByXmlId));
        }
        transferGroup.addTransferZone(transferZone);
    }
    
    return transferGroup;
  }

  /** Parse the transfer zones
   * 
   * @param xmlInterModal to extract them from
   */
  private void populateTransferZones(final Macroscopicintermodal xmlInterModal) {
    
    /* no transfer zones */
    XMLElementTransferZones xmlTransferZones = xmlInterModal.getTransferzones();
    if(xmlTransferZones == null) {
      return ;
    }     

    /* transferzone */
    List<XMLElementTransferZone> xmlTransferZonesList = xmlTransferZones.getZones();
    for(var xmlTransferzone : xmlTransferZonesList) {
      /* base zone elements parsed and PLANit version registered */
      TransferZone transferZone = zoning.getTransferZones().getFactory().registerNew();
      parseBaseZone(transferZone, xmlTransferzone.getId(), xmlTransferzone.getExternalid(),
          xmlTransferzone.getName(), xmlTransferzone.getCentroid());
      
      /* type */
      if(xmlTransferzone.getType()!= null) {
        transferZone.setType(parseTransferZoneType(xmlTransferzone.getType()));
      }

      /* platform names */
      if(xmlTransferzone.getPlatforms()!=null){
        transferZone.addTransferZonePlatformNames(
            xmlTransferzone.getPlatforms().split(CharacterUtils.COMMA.toString()));
      }
            
      /* geometry */
      populateZoneGeometry(transferZone, xmlTransferzone.getPolygon(), xmlTransferzone.getLineString());     
    }
    
  }

  /**
   * Check what network and layer is referenced for the transfer zone connectoids. Issue warnings if inconconsistent
   * and try to salvage
   *
   * @param xmlTransferZoneAccess to check
   * @return network layer to use
   */
  private NetworkLayer checkTransferZoneNetworkLayerReference(XMLElementTransferZoneAccess xmlTransferZoneAccess) {
    var networkRef = xmlTransferZoneAccess.getNetworkRef();
    var networkLayerRef = xmlTransferZoneAccess.getNetworkLayerRef();
    if (StringUtils.isNullOrBlank(networkRef)) {
      LOGGER.warning(String.format("Transfer zone access does not reference a network, will attempt to match to " +
          "provided network (%s)", getReferenceNetwork().getXmlId()));
    } else if (!networkRef.equals(getReferenceNetwork().getXmlId())) {
      LOGGER.warning(String.format("Transfer zone access references network %s but provided %s, will attempt to " +
              "match to provided network", networkRef, getReferenceNetwork().getXmlId()));
    }

    NetworkLayer networkLayer = null;
    if (StringUtils.isNullOrBlank(networkLayerRef)) {
      LOGGER.warning("Transfer zone access does not reference a network layer, will attempt to match to " +
          "provided network's initial layer %s");
      networkLayer = getReferenceNetwork().getTransportLayers().getFirst();
    } else{
      networkLayer = getReferenceNetwork().getTransportLayers().getByXmlId(networkLayerRef);
      if (networkLayer == null) {
        networkLayer = getReferenceNetwork().getTransportLayers().getFirst();
        LOGGER.warning(String.format("Transfer zone access references a non-existent network layer %s, will " +
            "attempt to match to provided network's initial layer %s instead",
            networkLayerRef, networkLayer.getXmlId()));
      }
    }
    return networkLayer;
  }
  
  /** Parse the access points for the transfer zones
   * 
   * @param modes that can be referred to
   * @param xmlInterModal XML memory model element to extract from
   */
  private void populateTransferZoneAccess(
      final Modes modes, final Macroscopicintermodal xmlInterModal) {
    
    /* no transfer zone connectoids */
    XMLElementTransferZoneAccess xmlTransferZoneAccess = xmlInterModal.getTransferzoneaccess();
    if(xmlTransferZoneAccess == null) {
      return;
    }

    // prep
    var layer = checkTransferZoneNetworkLayerReference(xmlTransferZoneAccess);

    Map<String, Mode> planitModesByXmlId = new HashMap<>();
    var supportedModes = layer == null ? modes : layer.getSupportedModes();
    if(layer == null){
      LOGGER.severe("No network layer found, allow all modes instead and try to salvage, this should not happen");
    }
    supportedModes.forEach( mode -> planitModesByXmlId.put(mode.getXmlId(), mode));
    
    /* transfer zone connectoid access */
    List<XMLElementTransferConnectoid> xmlTransferConnectoids = xmlTransferZoneAccess.getConnectoids();
    for(var xmlTransferConnectoid : xmlTransferConnectoids) {

      // accessNode
      var accessNode = parseConnectoidAccessNode(xmlTransferConnectoid);

      /* base connectoid */
      var connectoid = (TransferConnectoid) parseBaseConnectoid(xmlTransferConnectoid, accessNode);
      if(connectoid == null) {
        continue;
      }

      var xmlAccessZoneEntries = xmlTransferConnectoid.getAccesszones();
      for(var xmlAccessZoneEntry :  xmlAccessZoneEntries){

        // zone ref
        var zoneRef = xmlAccessZoneEntry.getRef();
        var transferZone = (TransferZone) getBySourceId(Zone.class, zoneRef);
        if(transferZone == null) {
          throw new PlanItRunTimeException(String.format("Provided Zone ref XML id %s is " +
                  "invalid when parsing transfer connectoid %s",
              zoneRef, xmlTransferConnectoid.getId()));
        }

        // type
        var zoneConnectoidType = parseZoneConnectoidType(xmlAccessZoneEntry.getType());

        // ls refs
        String xmlLinkSegmentRefs = xmlAccessZoneEntry.getLsrefs();
        if(xmlLinkSegmentRefs == null && zoneConnectoidType.equals(ZoneConnectoidType.PT_VEHICLE_STOP)){
          LOGGER.severe(String.format("PT_VEHICLE_STOP entry for connectoid %s is expected to have explicit link " +
              "access segments, but it has not, skip", connectoid.getIdsAsString()));
          continue;
        }else if(xmlLinkSegmentRefs != null && !zoneConnectoidType.equals(ZoneConnectoidType.PT_VEHICLE_STOP)){
          LOGGER.severe(String.format("%s type entry for connectoid %s is expected to not have explicit link " +
              "access segments, but it has, assume type should be PT_STOP as legacy v1 format",
              zoneConnectoidType, connectoid.getIdsAsString()));
          zoneConnectoidType = ZoneConnectoidType.PT_VEHICLE_STOP;
        }

        ConnectoidAccessZoneEntry planitZoneAccessEntry = null;
        if(xmlLinkSegmentRefs != null){
          planitZoneAccessEntry = connectoid.createDirectedAccessZoneEntry(transferZone, zoneConnectoidType);
        }else{
          planitZoneAccessEntry = connectoid.createUndirectedAccessZoneEntry(transferZone, zoneConnectoidType);
        }

        if (planitZoneAccessEntry == null) {
          LOGGER.warning(String.format("Unable to create access zone entry for transfer zone (%s) " +
                  "connectoid (%s) of type %s, skip entry",
              transferZone.getIdsAsString(), connectoid.getIdsAsString(), zoneConnectoidType));
          continue;
        }

        if(xmlLinkSegmentRefs != null){
          var lsRefsArray = xmlLinkSegmentRefs.split(String.valueOf(CharacterUtils.COMMA));
          for(var lsRef : lsRefsArray) {
            MacroscopicLinkSegment linkSegment = getBySourceId(MacroscopicLinkSegment.class, lsRef);
            if (linkSegment == null) {
              LOGGER.warning(String.format("Provided access link segment XML id %s is invalid given " +
                      "available link segments in network when parsing transfer connectoid (%s)",
                  lsRef, xmlTransferConnectoid.getId()));
              continue;
            }
            ((DirectedConnectoidAccessZoneEntry)planitZoneAccessEntry).addAccessLinkSegment(linkSegment);
          }
        }

        /* modes that are allowed access */
        String xmlModesRef = xmlAccessZoneEntry.getModes();
        populateConnectoidZoneEntryModes(connectoid, planitZoneAccessEntry, xmlModesRef, planitModesByXmlId);

        // length
        if (xmlAccessZoneEntry.getLengthkm() != null) {
          double connectoidLength = xmlAccessZoneEntry.getLengthkm().doubleValue();
          planitZoneAccessEntry.setLengthKm(connectoidLength);
        }

      }

      registerBySourceId(Connectoid.class, connectoid);
    }        
  }

  /** parse the transfer zone groups from XML element into PLANit memory
   * 
   * @param xmlInterModal to parse from
   */
  private void populateTransferZoneGroups(final Macroscopicintermodal xmlInterModal) {
    /* no transfer zone groups */
    if(xmlInterModal.getTransferzonegroups() == null) {
      return;
    }    
    XMLElementTransferZoneGroups xmlTransferZoneGroups = xmlInterModal.getTransferzonegroups();
    if(xmlTransferZoneGroups.getTransfergroups().isEmpty()) {
      LOGGER.warning("Dangling transfer zone groups element, no transfer zone groups can be parsed");
      return;
    }
    
    /* transfer zone groups */
    List<XMLElementTransferGroup> xmlTransferGroups = xmlTransferZoneGroups.getTransfergroups();
    for(XMLElementTransferGroup xmlTransferGroup : xmlTransferGroups) {
      /* transfer group */
      parseTransferGroup(xmlTransferGroup);
    }
  }

  /**
   * Parse the intermodal zones, i.e., platforms, stops, stations, etc. from XML element into PLANit memory
   * 
   * @param modes that can be referred to
   * @throws PlanItException thrown if error
   */
  protected void populateIntermodal(final Modes modes) throws PlanItException{
    if(xmlParser.getXmlRootElement().getIntermodal() == null) {
      LOGGER.info("No Transfer zones present, skip");
      return;
    }
    LOGGER.info("Parsing transfer zones...");
    
    /* intermodal elements present */
    var xmlInterModal = xmlParser.getXmlRootElement().getIntermodal();
    
    /* transferzones */
    populateTransferZones(xmlInterModal);
    
    /* transfer zone access connectoids */
    populateTransferZoneAccess(modes, xmlInterModal);
    
    /* transfer zone groups */
    populateTransferZoneGroups(xmlInterModal);    
  
  }

  /** Use the zoning crs if it is available from file, otherwise revert to the network crs. When zoning and 
   * network crs are incompatible log to user, this is discouraged.
   * 
   * @param macroscopicNetwork containing the network crs
   */
  private void parseCoordinateReferenceSystem(final MacroscopicNetwork macroscopicNetwork){
    var srsName = xmlParser.getXmlRootElement().getSrsname();
    CoordinateReferenceSystem crs;
    if(StringUtils.isNullOrBlank(srsName)){
      LOGGER.severe("Zoning crs not defined on XML root element, compulsory since v0.4.0 using network " +
          "fallback instead if possible");
      crs = macroscopicNetwork.getCoordinateReferenceSystem();
    }else{
      crs = PlanitXmlJaxbParser.createPlanitCrs(xmlParser.getXmlRootElement().getSrsname());
    }
    zoning.setCoordinateReferenceSystem(crs);

    if(!zoning.getCoordinateReferenceSystem().equals(macroscopicNetwork.getCoordinateReferenceSystem())) {
      LOGGER.severe(
          String.format("Zoning crs (%s) and network crs (%s) are not compatible",
              crs.getName(), macroscopicNetwork.getCoordinateReferenceSystem().getName()));
    }
    this.jtsUtils = new PlanitJtsCrsUtils(crs);
  }

  /** settings for the zoning reader */
  protected final PlanitZoningReaderSettings settings;

  /** network reader to use to create network if not provided outright (may be null) */
  private final NetworkReader networkReader;
  
  /** the zoning to populate */
  protected Zoning zoning;
  
  /** the network this zoning relates to */
  protected LayeredNetwork<?,?> network;
      
  /** Set the zoning to populate
   * 
   * @param zoning to populate
   */
  protected void setZoning(final Zoning zoning) {
    this.zoning = zoning;
  }
  
  /** Set the network to utilise
   * 
   * @param network to use
   */
  protected void setReferenceNetwork(final LayeredNetwork<?,?> network) {
    this.network = network;
  }
  
  /**
   * Parse the OD zones from Xml element into Planit memory
   * 
   */
  protected void populateODZones(){
    
    /* check if present */
    if(xmlParser.getXmlRootElement().getZones()==null) {
      LOGGER.info("No OD zones found in zoning, skip");
      return;
    }
    LOGGER.info("Parsing OD zones...");

    if(getReferenceNetwork().getTransportLayers().size() > 1){
      throw new PlanItRunTimeException("Currently PlaniIO does not support more than a single layer");
    }
    if(getReferenceNetwork().getTransportLayers().size() < 1){
      throw new PlanItRunTimeException("At least a single network layer should be present");
    }
    var layer = getReferenceNetwork().getTransportLayers().getFirst();
    Map<String, Mode> planitModesByXmlId = new HashMap<>();
    var modes = layer.getSupportedModes();
    modes.forEach( mode -> planitModesByXmlId.put(mode.getXmlId(), mode));
    
    /* zone */
    for (final org.goplanit.xml.generated.v2.Zone xmlZone : xmlParser.getXmlRootElement().getZones().getZones()) {
      /* create zone */
      OdZone zone = zoning.getOdZones().getFactory().registerNew();
      parseBaseZone(zone, xmlZone.getId(), xmlZone.getExternalid(), xmlZone.getId(), xmlZone.getCentroid());
      
      /* geometry */
      populateZoneGeometry(zone, xmlZone.getPolygon());      
                 
      /* connectoids */
      List<XMLElementConnectoid> xmlConnectoids = xmlZone.getConnectoids().getConnectoids();
      for(var xmlOdConnectoid : xmlConnectoids) {

        // accessNode
        var accessNode = parseConnectoidAccessNode(xmlOdConnectoid);
        /* parse the (Od, node reference based) undirected connectoid */
        OdConnectoid odConnectoid = (OdConnectoid) parseBaseConnectoid(xmlOdConnectoid, accessNode);
        if(odConnectoid == null){
          continue;
        }

        //todo: we are missing stuff here such as type because of compromised way we write out connectoids for ODs
        // register zone and type is located on zone-connectoid-entry which we do not write out currently due to
        // inversion
        var zoneConnectoidType = parseZoneConnectoidType(xmlOdConnectoid.getType());
        var planitZoneAccessEntry = odConnectoid.createUndirectedAccessZoneEntry(zone, zoneConnectoidType);
        if(planitZoneAccessEntry == null){
          LOGGER.warning(String.format("Unable to create access zone entry for od zone (%s) " +
                  "connectoid (%s) of type %s, skip entry",
              zone.getIdsAsString(), odConnectoid.getIdsAsString(), zoneConnectoidType));
          continue;
        }

        // modes
        var xmlModesRef = xmlOdConnectoid.getModes();
        populateConnectoidZoneEntryModes(odConnectoid, planitZoneAccessEntry, xmlModesRef, planitModesByXmlId);

        /* parse length */
        populateOdConnectoidToZoneLength(odConnectoid, xmlOdConnectoid);
      }             
    }
  }

  /** Constructor
   *
   * @param settings to use
   * @param networkReader to construct reference network from
   */
  protected PlanitZoningReader(
      final PlanitZoningReaderSettings settings, final NetworkReader networkReader) {
    this.xmlParser = new PlanitXmlJaxbParser<>(
        org.goplanit.xml.generated.v2.XMLElementMacroscopicZoning.class,
        org.goplanit.xml.generated.v1.XMLElementMacroscopicZoning.class);
    this.settings = settings;
    this.networkReader = networkReader;

    setZoning(null);
    setReferenceNetwork(null);
  }

  /** Constructor
   * 
   * @param settings to use
   * @param network to extract PLANit entities from by found references in zoning
   * @param zoning to populate
   */
  protected PlanitZoningReader(
      final PlanitZoningReaderSettings settings, final LayeredNetwork<?,?> network, final Zoning zoning) {
    this.xmlParser = new PlanitXmlJaxbParser<>(
        org.goplanit.xml.generated.v2.XMLElementMacroscopicZoning.class,
        org.goplanit.xml.generated.v1.XMLElementMacroscopicZoning.class);
    this.settings = settings;
    this.networkReader = null;

    setZoning(zoning);
    setReferenceNetwork(network);
  }  
    
  /** Constructor
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param network to extract PLANit entities from by found references in zoning
   * @param zoning to populate
   */
  protected PlanitZoningReader(
      final String pathDirectory,
      final String xmlFileExtension,
      final LayeredNetwork<?,?> network,
      final Zoning zoning) {
    this(new PlanitZoningReaderSettings(pathDirectory, xmlFileExtension), network, zoning);
  }
  
  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to
   * PLANit memory model
   * 
   * @param xmlMacroscopicZoning to extract from
   * @param network to extract PLANit entities from by found references in zoning
   * @param zoning to populate
   */
  protected PlanitZoningReader(
      final XMLElementMacroscopicZoning xmlMacroscopicZoning, final LayeredNetwork<?,?> network, final Zoning zoning) {
    this(xmlMacroscopicZoning, new PlanitZoningReaderSettings(), network, zoning);
  }

  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to
   * PLANit memory model
   *
   * @param xmlMacroscopicZoning to extract from
   * @param settings to use
   * @param network to extract PLANit entities from by found references in zoning
   * @param zoning to populate
   */
  protected PlanitZoningReader(
      final XMLElementMacroscopicZoning xmlMacroscopicZoning,
      final PlanitZoningReaderSettings settings,
      final LayeredNetwork<?,?> network,
      final Zoning zoning) {
    this(settings, network, zoning);
    this.xmlParser.setXmlRootElement(xmlMacroscopicZoning);
  }

  /** Read the zoning from disk
   * 
   * @return zoning parsed
   */
  @Override  
  public Zoning read(){

    if(networkReader != null && this.network == null){
      var readNetwork = networkReader.read();
      if(readNetwork == null || !(readNetwork instanceof MacroscopicNetwork)){
        throw new PlanItRunTimeException("No reference network available after parsing from provided network reader" +
            " unable to read zoning");
      }
      setReferenceNetwork(readNetwork);
      setZoning(new Zoning(network.getIdGroupingToken(), network.getNetworkGroupingTokenId()));
    }else if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItRunTimeException("Unable to read zoning, provided network is not compatible with " +
          "Macroscopic network");
    }

    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) network;   

    /* initialise the indices used, if needed */
    initialiseXmlIdTrackers();
    initialiseParentNetworkXmlIdTrackers(macroscopicNetwork);

    // log settings
    getSettings().logSettings();
    
    // create and register zones, centroids and connectoids
    try {
      
      /* populate Xml memory model */
      xmlParser.initialiseAndParseXmlRootElement(
          getSettings().getInputDirectory(), getSettings().getXmlFileExtension());
      PlanItRunTimeException.throwIfNull(xmlParser.getXmlRootElement(),
          "No valid PLANit XML zoning could be parsed into memory, abort");
      
      /* xml id */
      String zoningXmlId = xmlParser.getXmlRootElement().getId();
      if(StringUtils.isNullOrBlank(zoningXmlId)) {
        LOGGER.warning(String.format("Zoning has no XML id defined, adopting internally generated id %d " +
            "instead",zoning.getId()));
        zoningXmlId = String.valueOf(zoning.getId());
      }
      zoning.setXmlId(zoningXmlId);      
      
      /* initialise and validate crs compatibility */
      parseCoordinateReferenceSystem(macroscopicNetwork);               
      
      /* OD zones */
      populateODZones();
      
      /* Intermodal/transfer zones, i.e., platforms, stations, etc. */
      populateIntermodal(macroscopicNetwork.getModes());

      if(getSettings().isSyncXmlIdsToIds()){
        syncXmlIdsToIds();
      }

      /* log stats */
      zoning.logInfo(LoggingUtils.zoningPrefix(zoning.getId()));
      
      /* free */
      reset();
      
    } catch (PlanItException e) {
      throw new PlanItRunTimeException(e);
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      throw new PlanItRunTimeException("Error when populating zoning in PLANitIO",e);
    }
    
    return zoning;
  }
  
  /** Reference to zoning schema location, TODO: move to properties file */
  public static final String ZONING_XSD_FILE = "https://www.goplanit.org/xsd/macroscopiczoninginput.xsd";
  
  /** Settings for this reader
   * 
   * @return settings
   */
  public PlanitZoningReaderSettings getSettings() {
    return settings;
  }

  /**
   * Access to reference network used
   * @return reference network
   */
  public LayeredNetwork<?,?> getReferenceNetwork(){
    return this.network;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
    xmlParser.clearXmlContent();
  }
  

}
