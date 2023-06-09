package org.goplanit.io.converter.zoning;

import net.opengis.gml.CoordinatesType;
import net.opengis.gml.LineStringType;
import net.opengis.gml.LinearRingType;
import net.opengis.gml.PolygonType;
import org.goplanit.converter.BaseReaderImpl;
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
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.zoning.*;
import org.goplanit.xml.generated.*;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning.XMLElementIntermodal;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.ZoningModifierUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.*;
import java.util.logging.Logger;


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
  private final PlanitXmlJaxbParser<XMLElementMacroscopicZoning> xmlParser;

  /**
   * Initialise event listeners in case we want to make changes to the XML ids after parsing is complete, e.g., if the parsed
   * zoning is going to be modified and saved to disk afterwards, then it is advisable to sync all XML ids to the internal ids upon parsing
   * because this avoids the risk of generating duplicate XML ids during editing of the network (when XML ids are chosen to be synced to internal ids)
   */
  private void syncXmlIdsToIds() {
    LOGGER.info("Syncing PLANit zoning XML ids to internally generated ids, overwriting original XML ids");
    ZoningModifierUtils.syncManagedIdEntitiesContainerXmlIdsToIds(zoning);
  }
  
  /**
   * initialise the XML id trackers and populate them for the network references, 
   * so we can lay indices on the XML id as well for quick lookups
   * 
   * @param network
   */
  private void initialiseParentNetworkXmlIdTrackers(MacroscopicNetwork network) {    
    initialiseSourceIdMap(Node.class, Node::getXmlId);
    network.getTransportLayers().forEach( layer -> getSourceIdContainer(Node.class).addAll(layer.getNodes()));    
    initialiseSourceIdMap(MacroscopicLinkSegment.class, MacroscopicLinkSegment::getXmlId);
    network.getTransportLayers().forEach( layer -> getSourceIdContainer(MacroscopicLinkSegment.class).addAll(layer.getLinkSegments()));
  }  
  
  /**
   * initialise the XML id trackers for the to be populated zoning entities, so we can lay indices on the XML id as well for quick lookups
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
        LOGGER.warning(String.format("Unsupported transfer stop type %s found, changed to `unknown`",xmlTransferZone.value()));
        return TransferZoneType.UNKNOWN;
      }
    }
    
  }
  
  /** Parse passed in connectoid type
   * 
   * @param xmlConnectoidType to parse
   * @return PLANit equivalent of the transfer zone type
   */  
  private static ConnectoidType parseConnectoidType(final Connectoidtypetype xmlConnectoidType) {
    
    if(xmlConnectoidType==null) {
      return ConnectoidType.NONE;
    }else {
      switch (xmlConnectoidType) {
      case PT_VEH_STOP:
        return ConnectoidType.PT_VEHICLE_STOP;
      case TRAVELLER_ACCESS:
        return ConnectoidType.TRAVELLER_ACCESS;
      case NONE:
        return ConnectoidType.NONE;         
      default:
        LOGGER.warning(String.format("Unknown connectoid type %s found, changed to `unknown`",xmlConnectoidType.value()));
        return ConnectoidType.UNKNOWN;
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
        LOGGER.warning(String.format("zones only support polygon geometries with an outer exterior, however this is missing for zone %s",zone.getXmlId()));
      }else {
        if(xmlPolygon.getExterior().getValue().getRing() == null) {
          LOGGER.warning(String.format("expected ring element missing within polygon exterior element for zone %s",zone.getXmlId()));  
        }else if(xmlPolygon.getExterior().getValue().getRing().getValue() instanceof LinearRingType) {
          /* found the actual content */
          LinearRingType xmlLinearRing = (LinearRingType) xmlPolygon.getExterior().getValue().getRing().getValue();
          geometry = PlanitJtsUtils.create2DPolygon(xmlLinearRing.getPosList().getValue());
        }else {
          LOGGER.warning(String.format("expected linear ring within polygon exterior element for zone %s, but different ring type was encountered",zone.getXmlId()));  
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
   * @param position to compute geographic length from (if not null)
   * @param jtsUtils to use
   */
  private static void populateConnectoidToZoneLengths(
      final Connectoid connectoid, final Connectoidtype xmlConnectoid, final Point position, final PlanitJtsCrsUtils jtsUtils){
    Double connectoidLength = null;
    
    /* Explicitly set length (apply to all access zones */
    if (xmlConnectoid.getLength() != null) {
      connectoidLength = Double.valueOf(xmlConnectoid.getLength());
      if(connectoid.getNumberOfAccessZones() > 1) {
        LOGGER.fine(String.format("connectoid %s has explicitly set length, yet has multiple access zones that now all receive equal lengths", connectoid.getXmlId()));
      }
      for(Zone accessZone : connectoid) {
        connectoid.setLength(accessZone, connectoidLength);
      }
      // :TODO - need to create some test cases in which nodes have a GML location
    }
    /* implicit based on locations of zone centroids */
    else if (position != null) {
      /* if node has a GML Point, get the GML Point from the centroid and calculate the length between them */
      for(Zone accessZone : connectoid) {
        if(accessZone.getCentroid() == null || accessZone.getCentroid().getPosition() != null) {
          LOGGER.warning(String.format("access zone of connectoid %s is null", connectoid.getXmlId()));
          continue;
        }
        if(accessZone.getCentroid().getPosition() != null) {
          connectoidLength = jtsUtils.getDistanceInKilometres(accessZone.getCentroid().getPosition(), position);
          connectoid.setLength(accessZone, connectoidLength);
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
   * @return created and registered Planit zone
   */
  private void parseBaseZone(
      final Zone zone, final String xmlId, final String externalId, final String name, final XMLElementCentroid xmlCentroid) {
    
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
   * Parse the connectoid based on the XML connectoid element
   * 
   * @param xmlConnectoid to be parsed
   * @return created connectoid
   */
  private Connectoid parseBaseConnectoid(final Connectoidtype xmlConnectoid) {
    Connectoid theConnectoid = null;
    
    /* xml id */
    String xmlId = null;
    if(!StringUtils.isNullOrBlank(xmlConnectoid.getId())) {
      xmlId = xmlConnectoid.getId();
    }else {
      LOGGER.severe("DISCARD: Parsed connectoid has no (XML) id");
      return null;
    }    
    
    /* CONNECTOID */
    if(xmlConnectoid instanceof XMLElementConnectoid) {

      /* ACCESS NODE BASED (OD) */
      var xmlOdConnectoid = ((XMLElementConnectoid)xmlConnectoid);
      Node accessNode = getBySourceId(Node.class, xmlOdConnectoid.getNoderef());
      if(accessNode == null) {
        throw new PlanItRunTimeException(String.format("Provided accessNode XML id %s is invalid given available nodes in network when parsing transfer connectoid %s", xmlOdConnectoid.getNoderef(), xmlConnectoid.getId()));
      }
      theConnectoid = zoning.getOdConnectoids().getFactory().registerNew(accessNode);

    }else if(xmlConnectoid instanceof XMLElementTransferZoneAccess.XMLElementTransferConnectoid) {

      /* ACCESS LINK SEGMENT BASED ((PT) TRANSFER ZONE) */
      XMLElementTransferZoneAccess.XMLElementTransferConnectoid xmlTransferConnectoid = (XMLElementTransferZoneAccess.XMLElementTransferConnectoid) xmlConnectoid;                  
      String xmlLinkSegmentRef = xmlTransferConnectoid.getLsref();
      MacroscopicLinkSegment linkSegment = getBySourceId(MacroscopicLinkSegment.class,xmlLinkSegmentRef);
      if(linkSegment == null) {
        throw new PlanItRunTimeException(String.format("Provided access link segment XML id %s is invalid given available link segments in network when parsing transfer connectoid %s", xmlLinkSegmentRef, xmlConnectoid.getId()));
      }

      boolean nodeAccessDownstream = true;
      if(xmlTransferConnectoid.getLoc()!= null && xmlTransferConnectoid.getLoc() == Connectoidnodelocationtype.UPSTREAM) {
        nodeAccessDownstream = false;
      }

      theConnectoid = zoning.getTransferConnectoids().getFactory().registerNew(nodeAccessDownstream, linkSegment);

    }
    theConnectoid.setXmlId(xmlId);
        
    /* external id */
    if(xmlConnectoid.getExternalid() != null && !xmlConnectoid.getExternalid().isBlank()) {
      theConnectoid.setExternalId(xmlConnectoid.getExternalid());
    }
    
    /* name */
    if(xmlConnectoid.getName() != null && !xmlConnectoid.getName().isBlank()) {
      theConnectoid.setName(xmlConnectoid.getName());
    }
    
    /* type */
    theConnectoid.setType(parseConnectoidType(xmlConnectoid.getType()));
    
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
    for(int index=0; index<transferZoneRefsByXmlId.length; ++index) {
      
      /* transfer zone */
      String transferZoneXmlId = transferZoneRefsByXmlId[index];
      TransferZone transferZone = (TransferZone) getBySourceId(Zone.class, transferZoneXmlId);
      if(transferZone == null) {
        LOGGER.warning(String.format("Transfer zone group %s (id:%d) references transfer zone %s that is not available in the parser, transfer zone ignored",
            transferGroup.getXmlId(), transferGroup.getId(), transferZoneRefsByXmlId));
      }
      transferGroup.addTransferZone(transferZone);
    }
    
    return transferGroup;
  }

  /** Parse the transfer zones
   * 
   * @param xmlInterModal to extract them from
   * @return transfer zone access point references map to later be able to connect each transfer zone to the correct access points
   * @throws PlanItException thrown if error
   */
  private void populateTransferZones(final XMLElementIntermodal xmlInterModal) throws PlanItException {
    
    /* no transfer zones */    
    if(xmlInterModal.getValue().getTransferzones() == null) {
      return ;
    }     
    XMLElementTransferZones xmlTransferZones = xmlInterModal.getValue().getTransferzones();
    
    /* transferzone */
    List<XMLElementTransferZones.XMLElementTransferZone> xmlTransferZonesList = xmlTransferZones.getZone();
    for(XMLElementTransferZones.XMLElementTransferZone xmlTransferzone : xmlTransferZonesList) {
      /* base zone elements parsed and PLANit version registered */
      TransferZone transferZone = zoning.getTransferZones().getFactory().registerNew();
      parseBaseZone(transferZone, xmlTransferzone.getId(), xmlTransferzone.getExternalid(), xmlTransferzone.getName(), xmlTransferzone.getCentroid());
      
      /* type */
      if(xmlTransferzone.getType()!= null) {
        transferZone.setType(parseTransferZoneType(xmlTransferzone.getType()));
      }

      /* platform names */
      if(xmlTransferzone.getPlatforms()!=null){
        transferZone.addTransferZonePlatformNames(xmlTransferzone.getPlatforms().split(CharacterUtils.COMMA.toString()));
      }
            
      /* geometry */
      populateZoneGeometry(transferZone, xmlTransferzone.getPolygon(), xmlTransferzone.getLineString());     
    }
    
  }  
  
  /** Parse the access points for the transfer zones
   * 
   * @param modes that can be referred to
   * @param xmlInterModal XML memory model element to extract from
   * @throws PlanItException thrown if error
   */
  private void populateTransferZoneAccess(
      final Modes modes, final XMLElementIntermodal xmlInterModal) throws PlanItException {
    
    /* no transfer zone connectoids */
    if(xmlInterModal.getValue().getTransferzoneaccess() == null) {
      return;
    }    
    XMLElementTransferZoneAccess xmlTransferZoneAccess = xmlInterModal.getValue().getTransferzoneaccess();
    
    Map<String, Mode> modesByXmlId = new HashMap<>();
    modes.forEach( mode -> modesByXmlId.put(mode.getXmlId(), mode));
    
    /* transfer zone connectoid access */
    List<XMLElementTransferZoneAccess.XMLElementTransferConnectoid> xmlTransferConnectoids = xmlTransferZoneAccess.getConnectoid();
    for(XMLElementTransferZoneAccess.XMLElementTransferConnectoid xmlTransferConnectoid : xmlTransferConnectoids) {
      /* base connectoid */
      DirectedConnectoid connectoid = (DirectedConnectoid) parseBaseConnectoid(xmlTransferConnectoid);
      
      
      /* modes that are allowed access */
      String modesRef = xmlTransferConnectoid.getModes();
      Collection<Mode> allowedModes = null;
      boolean implicitAllModesAllowed = true;
      if(!StringUtils.isNullOrBlank(modesRef)) {        
        /* capture explicit referenced modes by xml id */
        implicitAllModesAllowed = false;
        allowedModes = new HashSet<Mode>();
        for(String xmlModeRef : List.of(modesRef.split(","))){
          Mode mode = modesByXmlId.get(xmlModeRef);
          if(mode == null) {
            LOGGER.warning(String.format("invalid mode %s referenced by transfer connectoid %s",xmlModeRef, connectoid.getXmlId()));
            continue;
          }
          allowedModes.add(mode);                    
        }
      }
      
      /* register (transfer) access zones */
      String TransferZoneRefs = xmlTransferConnectoid.getTzrefs();
      for(String xmlAccessZoneRef : List.of(TransferZoneRefs.split(","))){
        Zone accessZone = getBySourceId(Zone.class, xmlAccessZoneRef);
        if(accessZone == null) {
          LOGGER.warning(String.format("invalid transfer zone %s referenced by transfer connectoid %s", xmlAccessZoneRef, connectoid.getXmlId()));
          continue;
        }
        /* register */
        connectoid.addAccessZone(accessZone);
        /* register explicitly allowed modes (if all modes allowed, none need to be explicitly set)*/
        if(!implicitAllModesAllowed) {
          allowedModes.forEach( allowedMode -> connectoid.addAllowedMode(accessZone, allowedMode));
        }
        
      }

      /* populate lengths using link segment downstream vertex position */
      populateConnectoidToZoneLengths(connectoid, xmlTransferConnectoid, connectoid.getAccessNode().getPosition(), jtsUtils);
                        
      registerBySourceId(Connectoid.class, connectoid);      
    }        
  }

  /** parse the transfer zone groups from XML element into PLANit memory
   * 
   * @param xmlInterModal to parse from
   */
  private void populateTransferZoneGroups(final XMLElementIntermodal xmlInterModal) {
    /* no transfer zone groups */
    if(xmlInterModal.getValue().getTransferzonegroups() == null) {
      return;
    }    
    XMLElementTransferZoneGroups xmlTransferZoneGroups = xmlInterModal.getValue().getTransferzonegroups();
    if(xmlTransferZoneGroups.getTransfergroup().isEmpty()) {
      LOGGER.warning("Dangling transfer zone groups element, no transfer zone groups can be parsed");
      return;
    }
    
    /* transfer zone groups */
    List<XMLElementTransferGroup> xmlTransferGroups = xmlTransferZoneGroups.getTransfergroup();
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
    XMLElementIntermodal xmlInterModal = xmlParser.getXmlRootElement().getIntermodal();
    
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
    CoordinateReferenceSystem crs = macroscopicNetwork.getCoordinateReferenceSystem();
    if(xmlParser.getXmlRootElement().getSrsname()!=null && !xmlParser.getXmlRootElement().getSrsname().isBlank()) {
      crs = PlanitXmlJaxbParser.createPlanitCrs(xmlParser.getXmlRootElement().getSrsname());
    }
    
    if(!crs.equals(macroscopicNetwork.getCoordinateReferenceSystem())) {
      LOGGER.severe(
          String.format("Zoning crs (%s) and network crs (%s) are not compatible",crs.getName(), macroscopicNetwork.getCoordinateReferenceSystem().getName()));
    }
    this.jtsUtils = new PlanitJtsCrsUtils(crs);
  }

  /** settings for the zoning reader */
  protected final PlanitZoningReaderSettings settings;
  
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
  protected void setNetwork(final LayeredNetwork<?,?> network) {
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
    
    /* zone */
    for (final XMLElementZones.Zone xmlZone : xmlParser.getXmlRootElement().getZones().getZone()) {
      /* create zone */
      OdZone zone = zoning.getOdZones().getFactory().registerNew();
      parseBaseZone(zone, xmlZone.getId(), xmlZone.getExternalid(), xmlZone.getId(), xmlZone.getCentroid());
      
      /* geometry */
      populateZoneGeometry(zone, xmlZone.getPolygon());      
                 
      /* connectoids */
      List<XMLElementConnectoid> xmlConnectoids = xmlZone.getConnectoids().getConnectoid();
      for(XMLElementConnectoid xmlOdConnectoid : xmlConnectoids) {
        /* parse the (Od, node reference based) undirected connectoid */
        UndirectedConnectoid planitOdConnectoid = (UndirectedConnectoid) parseBaseConnectoid(xmlOdConnectoid);
        /* register zone */
        planitOdConnectoid.addAccessZone(zone);
 
        /* parse length */
        populateConnectoidToZoneLengths(planitOdConnectoid, xmlOdConnectoid, planitOdConnectoid.getAccessVertex().getPosition(), jtsUtils);
      }             
    }
  }
  
  /** Constructor
   * 
   * @param settings to use
   * @param network to extract PLANit entities from by found references in zoning
   * @param zoning to populate
   */
  protected PlanitZoningReader(
      final PlanitZoningReaderSettings settings, final LayeredNetwork<?,?> network, final Zoning zoning) {
    this.xmlParser = new PlanitXmlJaxbParser<>(XMLElementMacroscopicZoning.class);
    this.settings = settings;
    setZoning(zoning);
    setNetwork(network);
  }  
    
  /** Constructor
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param network to extract planit entities from by found references in zoning
   * @param zoning to populate
   */
  protected PlanitZoningReader(
      final String pathDirectory, final String xmlFileExtension, final LayeredNetwork<?,?> network, final Zoning zoning) {
    this.xmlParser = new PlanitXmlJaxbParser<>(XMLElementMacroscopicZoning.class);
    this.settings = new PlanitZoningReaderSettings(pathDirectory, xmlFileExtension);    
    setZoning(zoning);
    setNetwork(network);
  }
  
  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlMacroscopicZoning to extract from
   * @param network to extract planit entities from by found references in zoning
   * @param zoning to populate
   */
  protected PlanitZoningReader(
      final XMLElementMacroscopicZoning xmlMacroscopicZoning, final LayeredNetwork<?,?> network, final Zoning zoning) {
    this(xmlMacroscopicZoning, new PlanitZoningReaderSettings(), network, zoning);
  }

  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   *
   * @param xmlMacroscopicZoning to extract from
   * @param settings to use
   * @param network to extract planit entities from by found references in zoning
   * @param zoning to populate
   */
  protected PlanitZoningReader(
      final XMLElementMacroscopicZoning xmlMacroscopicZoning, final PlanitZoningReaderSettings settings, final LayeredNetwork<?,?> network, final Zoning zoning) {
    this.xmlParser = new PlanitXmlJaxbParser<>(xmlMacroscopicZoning);
    this.settings =  settings;
    setZoning(zoning);
    setNetwork(network);
  }

  /** Read the zoning from disk
   * 
   * @return zoning parsed
   * @throws PlanItException thrown if error
   */
  @Override  
  public Zoning read() throws PlanItException {
        
    if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItException("unable to read zoning, network is not compatible with Macroscopic network");
    }
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) network;   

    /* initialise the indices used, if needed */
    initialiseXmlIdTrackers();
    initialiseParentNetworkXmlIdTrackers(macroscopicNetwork);
    
    // create and register zones, centroids and connectoids
    try {
      
      /* populate Xml memory model */
      xmlParser.initialiseAndParseXmlRootElement(getSettings().getInputDirectory(), getSettings().getXmlFileExtension());
      PlanItException.throwIfNull(xmlParser.getXmlRootElement(), "No valid PLANit XML zoning could be parsed into memory, abort");
      
      /* xml id */
      String zoningXmlId = xmlParser.getXmlRootElement().getId();
      if(StringUtils.isNullOrBlank(zoningXmlId)) {
        LOGGER.warning(String.format("Zoning has no XML id defined, adopting internally generated id %d instead",zoning.getId()));
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
      throw e;
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when populating zoning in PLANitIO",e);
    }
    
    return zoning;
  }
  
  /** Reference to zoning schema location, TODO: move to properties file */
  public static final String ZONING_XSD_FILE = "https://trafficplanit.github.io/PLANitManual/xsd/macroscopiczoninginput.xsd";  
  
  /** Settings for this reader
   * 
   * @return settings
   */
  public PlanitZoningReaderSettings getSettings() {
    return settings;
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
