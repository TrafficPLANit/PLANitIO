package org.planit.io.converter.zoning;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.converter.zoning.ZoningReader;
import org.planit.io.xml.util.PlanitXmlReader;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.macroscopic.MacroscopicNetwork;
import org.planit.network.macroscopic.physical.MacroscopicPhysicalNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsCrsUtils;
import org.planit.utils.geo.PlanitJtsUtils;
import org.planit.utils.misc.StringUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.Modes;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.zoning.Centroid;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.ConnectoidType;
import org.planit.utils.zoning.DirectedConnectoid;
import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.TransferZoneGroup;
import org.planit.utils.zoning.TransferZoneType;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.utils.zoning.Zone;
import org.planit.utils.zoning.Zones;
import org.planit.xml.generated.Connectoidnodelocationtype;
import org.planit.xml.generated.Connectoidtype;
import org.planit.xml.generated.Connectoidtypetype;
import org.planit.xml.generated.Odconnectoid;
import org.planit.xml.generated.Transferzonetype;
import org.planit.xml.generated.XMLElementCentroid;
import org.planit.xml.generated.XMLElementConnectoid;
import org.planit.xml.generated.XMLElementMacroscopicZoning;
import org.planit.xml.generated.XMLElementMacroscopicZoning.XMLElementIntermodal;
import org.planit.xml.generated.XMLElementTransferGroup;
import org.planit.xml.generated.XMLElementTransferZoneAccess;
import org.planit.xml.generated.XMLElementTransferZoneGroups;
import org.planit.xml.generated.XMLElementTransferZones;
import org.planit.xml.generated.XMLElementZones;
import org.planit.zoning.Zoning;

import net.opengis.gml.LinearRingType;
import net.opengis.gml.PolygonType;


/**
 * Class to parse zoning from native XML format
 * 
 * @author markr
 *
 */

public class PlanitZoningReader extends PlanitXmlReader<XMLElementMacroscopicZoning> implements ZoningReader {
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitZoningReader.class.getCanonicalName());
    
  /** parse passed in transfer zone type
   * 
   * @param xmlTransferzoneType to parse
   * @return planit equivalent of the transfer zone type
   */
  private static TransferZoneType parseTransferZoneType(Transferzonetype xmlTransferZone ) {
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
  
  /** parse passed in connectoid type
   * 
   * @param xmlTransferzoneType to parse
   * @return planit equivalent of the transfer zone type
   */  
  private static ConnectoidType parseConnectoidType(Connectoidtypetype xmlConnectoidType) {
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
        LOGGER.warning(String.format("unknown connectoid type %s found, changed to `unknown`",xmlConnectoidType.value()));
        return ConnectoidType.UNKNOWN;
      }
    }
  }  
  
  /**pt to parse the geometry of the zone if any is provided
   * 
   * @param transferZone to populate geomtry on
   * @param xmlTransferzone to extract it from
   */
  private static void populateZoneGeometry(Zone zone, PolygonType xmlPolygon) {
    if(xmlPolygon != null) {
      if(xmlPolygon.getExterior() == null) {
        LOGGER.warning(String.format("zones only support polygon geometries with an outer exterior, however this is missing for zone %s",zone.getXmlId()));
      }else {
        if(xmlPolygon.getExterior().getValue().getRing() == null) {
          LOGGER.warning(String.format("expected ring element missing within polygon exterior element for zone %s",zone.getXmlId()));  
        }else if(xmlPolygon.getExterior().getValue().getRing().getValue() instanceof LinearRingType) {
          /* found the actual content */
          LinearRingType xmlLinearRing = (LinearRingType) xmlPolygon.getExterior().getValue().getRing().getValue();
          Polygon geometry = PlanitJtsUtils.create2DPolygon(xmlLinearRing.getPosList().getValue());
          zone.setGeometry(geometry);
        }else {
          LOGGER.warning(String.format("expected linear ring within polygon exterior element for zone %s, but different ring type was encountered",zone.getXmlId()));  
        }                    
      }
    }
  }  
  
  /** given the passed in connectoid, xml connectoid information and reference position (if any) determine the length
   * to each of the available access zones of the connectoid (assumed already registered)
   * 
   * @param connectoid to register lengths on
   * @param xmlConnectoid to extract explicit length from (if any)
   * @param position to compute geographic length from (if not null)
   * @param jtsUtils 
   * @throws PlanitException thrown if error 
   */
  private static void populateConnectoidToZoneLengths(Connectoid connectoid, Connectoidtype xmlConnectoid, Point position, PlanitJtsCrsUtils jtsUtils) throws PlanItException {       
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
   * @param <T>
   * @param zones to register on
   * @param xmlId to use
   * @param externalId to use (can be null)
   * @param xmlCentroid to extract centroid from
   * @return created and registered Planit zone
   * @throws PlanItException thrown if error
   */
  private <T extends Zone> T parseBaseZone(Zones<T> zones, String xmlId, String externalId, XMLElementCentroid xmlCentroid) throws PlanItException {
    /* create zone */
    T zone = zones.registerNew();
    
    /* xml id */
    if(xmlId != null && !xmlId.isBlank()) {
      zone.setXmlId(xmlId);
    }else {
      throw new PlanItException("zone cannot be parsed, its (XML) id is not set");
    }
    /* all zones regardless of subtype are expected to have unique ids */
    Zone duplicatezone = settings.getMapToIndexZoneByXmlIds().put(xmlId, zone);
    if(duplicatezone != null) {
      throw new PlanItException(String.format("zone with duplicate (XML) id %s found, this is not allowed",xmlId));
    }
    
    /* external id */        
    if(externalId != null && !externalId.isBlank()) {
      zone.setExternalId(externalId);  
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
    
    return zone;
  }  
  
  /**
   * Parse the connectoid based on the XML connectoid element
   * 
   * @param xmlConnectoid to be parsed
   * @param nodesByXmlId to identify mapping between OD zones and network (via nodes)
   * @param linkSegmentsByXmlId to identify mapping between (transfer) connectoids and network
   * @return created connectoid
   * @throws PlanItException thrown if error
   */
  private Connectoid parseBaseConnectoid(Connectoidtype xmlConnectoid) throws PlanItException {
    Connectoid theConnectoid = null;
    
    /* CONNECTOID */
    Node accessNode = null;
    if(xmlConnectoid instanceof Odconnectoid) {
      if(nodesByXmlId == null) {
        throw new PlanItException("provided nodes by XML id is null when parsing XML OD connectoid");
      }
      accessNode = nodesByXmlId.get( ((Odconnectoid)xmlConnectoid).getNoderef());
      if(accessNode == null) {
        throw new PlanItException(String.format("provided accessNode XML id %s is invalid given available nodes in network when parsing transfer connectoid %s", ((Odconnectoid)xmlConnectoid).getNoderef(), xmlConnectoid.getId()));
      }
      /* ACCESS NODE based*/
      theConnectoid = zoning.odConnectoids.registerNew(accessNode);
    }else if(xmlConnectoid instanceof XMLElementTransferZoneAccess.XMLElementTransferConnectoid) {
      XMLElementTransferZoneAccess.XMLElementTransferConnectoid xmlTransferConnectoid = (XMLElementTransferZoneAccess.XMLElementTransferConnectoid) xmlConnectoid;
      if(linkSegmentsByXmlId == null) {
        throw new PlanItException(String.format("provided link segments by XML id is null when parsing XML transfer connectoid %s", xmlConnectoid.getId()));
      }      
      String xmlLinkSegmentRef = xmlTransferConnectoid.getLsref();
      MacroscopicLinkSegment linkSegment = linkSegmentsByXmlId.get(xmlLinkSegmentRef);
      if(linkSegment == null) {
        throw new PlanItException(String.format("provided link segment XML id %s is invalid given available link segments in network when parsing transfer connectoid %s", xmlLinkSegmentRef, xmlConnectoid.getId()));
      }
      /* LINK SEGMENT based */
      theConnectoid = zoning.transferConnectoids.registerNew(linkSegment);
      
      /* special case: when upstream node should be used */
      if(xmlTransferConnectoid.getLoc()!= null && xmlTransferConnectoid.getLoc() == Connectoidnodelocationtype.UPSTREAM) {
        ((DirectedConnectoid)theConnectoid).setNodeAccessDownstream(false);
      }
    }
    
    /* xml id */
    if(xmlConnectoid.getId() != null && !xmlConnectoid.getId().isBlank()) {
      theConnectoid.setXmlId(xmlConnectoid.getId());
    }
    
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
  
  /** parse a transfer group based on provided xml element and register on zoning's transfer zone groups
   * @param xmlTransferGroup to parse
   * @return transfer zone group parsed(and registered)
   */
  private TransferZoneGroup parseTransferGroup(XMLElementTransferGroup xmlTransferGroup) {
    /* register new */
    TransferZoneGroup transferGroup = zoning.transferZoneGroups.registerNew();
    
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
      if(!getSettings().getMapToIndexZoneByXmlIds().containsKey(transferZoneXmlId)) {
        LOGGER.warning(String.format("Transfer zone group %s (id:%d) references transfer zone %s that is not available in the parser, trasnfer zone ignored",
            transferGroup.getXmlId(), transferGroup.getId(), transferZoneRefsByXmlId));
      }
      TransferZone transferZone = (TransferZone) getSettings().getMapToIndexZoneByXmlIds().get(transferZoneXmlId);
      transferGroup.addTransferZone(transferZone);
    }
    
    return transferGroup;
  }

  /** parse the transfer zones
   * 
   * @param xmlInterModal to extract them from
   * @return transfer zone access point references map to later be able to connect each transfer zone to the correct access points
   * @throws PlanItException thrown if error
   */
  private void populateTransferZones(XMLElementIntermodal xmlInterModal) throws PlanItException {
    
    /* no transfer zones */    
    if(xmlInterModal.getValue().getTransferzones() == null) {
      return ;
    }     
    XMLElementTransferZones xmlTransferZones = xmlInterModal.getValue().getTransferzones();
    
    /* transferzone */
    List<XMLElementTransferZones.XMLElementTransferZone> xmlTransferZonesList = xmlTransferZones.getZone();
    for(XMLElementTransferZones.XMLElementTransferZone xmlTransferzone : xmlTransferZonesList) {
      /* base zone elements parsed and planit version registered */
      TransferZone transferZone = parseBaseZone(zoning.transferZones, xmlTransferzone.getId(), xmlTransferzone.getExternalid(), xmlTransferzone.getCentroid());
      
      /* type */
      if(xmlTransferzone.getType()!= null) {
        transferZone.setType(parseTransferZoneType(xmlTransferzone.getType()));
      }
            
      /* geometry */
      populateZoneGeometry(transferZone, xmlTransferzone.getPolygon());     
      /* TODO: support linestring as well for transfer zone geometry */
    }
    
  }  
  
  /** parse the access points for the transfer zones
   * 
   * @param modes that can be referred to
   * @param xmlInterModal xml memory model element to extract from
   * @param linkSegmentsByXmlId to identify mapping between (transfer) connectoids and network
   * @throws PlanItException thrown if error
   */
  private void populateTransferZoneAccess(Modes modes, XMLElementIntermodal xmlInterModal, Map<String, MacroscopicLinkSegment> linkSegmentsByXmlId) throws PlanItException {
    
    /* no transfer zone connectoids */
    if(xmlInterModal.getValue().getTransferzoneaccess() == null) {
      return;
    }    
    XMLElementTransferZoneAccess xmlTransferZoneAccess = xmlInterModal.getValue().getTransferzoneaccess();
    
    Map<String, Mode> modesByXmlId = new HashMap<String, Mode>();
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
      if(modesRef != null && !modesRef.isBlank()) {        
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
      String TransferzoneRefs = xmlTransferConnectoid.getTzrefs();
      for(String xmlTransferZoneRef : List.of(TransferzoneRefs.split(","))){
        Zone accessZone = settings.getMapToIndexZoneByXmlIds().get(xmlTransferZoneRef);
        if(accessZone == null) {
          LOGGER.warning(String.format("invalid transfer zone %s referenced by transfer connectoid %s",xmlTransferZoneRef, connectoid.getXmlId()));
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
                        
      Connectoid duplicateConnectoid = settings.getMapToIndexConnectoidsByXmlIds().put(connectoid.getXmlId(), connectoid);
      if(duplicateConnectoid != null) {
        throw new PlanItException(String.format("(od/transfer) connectoid id %s used not unique across project, thsi is not allowed",connectoid.getXmlId())); 
      }
    }        
  }

  /** parse the transfer zone groups from Xml element into planit memory
   * @param xmlInterModal to parse from
   */
  private void populateTransferZoneGroups(XMLElementIntermodal xmlInterModal) {
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
   * parse the intermodal zones, i.e., platforms, stops, stations, etc. from Xml element into Planit memory
   * @param modes that can be referred to
   * 
   * @param linkSegmentsByXmlId to identify mapping between (transfer) connectoids and network
   * @throws PlanItException thrown if error
   */
  protected void populateIntermodal(Modes modes, Map<String, MacroscopicLinkSegment> linkSegmentsByXmlId) throws PlanItException{
    if(getXmlRootElement().getIntermodal() == null) {
      return;
    }
    
    /* intermodal elements present */
    XMLElementIntermodal xmlInterModal = getXmlRootElement().getIntermodal();
    
    /* transferzones */
    populateTransferZones(xmlInterModal);
    
    /* transfer zone access connectoids */
    populateTransferZoneAccess(modes, xmlInterModal, linkSegmentsByXmlId);
    
    /* transfer zone groups */
    populateTransferZoneGroups(xmlInterModal);    
  
  }

  /** use the zoning crs if it is available from file, otherwise revert to the network crs. When zoning and 
   * network crs are incompatible log to user, this is discouraged.
   * 
   * @param macroscopicNetwork containing the network crs
   */
  private void initialiseZoningCrs(MacroscopicNetwork macroscopicNetwork) {
    CoordinateReferenceSystem crs = macroscopicNetwork.getCoordinateReferenceSystem();
    if(getXmlRootElement().getSrsname()!=null && !getXmlRootElement().getSrsname().isBlank()) {
      crs = createPlanitCrs(getXmlRootElement().getSrsname());
    }
    
    if(!crs.equals(macroscopicNetwork.getCoordinateReferenceSystem())) {
      LOGGER.severe(
          String.format("zoning crs (%s) and network crs (%s) are not compatible",crs.getName(), macroscopicNetwork.getCoordinateReferenceSystem().getName()));
    }
    this.jtsUtils = new PlanitJtsCrsUtils(crs);
  }


  /** settings for the zoning reader */
  protected final PlanitZoningReaderSettings settings;
  
  /** the zoning to populate */
  protected Zoning zoning;
  
  /** the network this zoning relates to */
  protected InfrastructureNetwork<?,?> network;
  
  /**
   * mapping of nodes by xml id for quick lookups
   */
  protected Map<String, Node> nodesByXmlId = null; 

  /**
   * mapping of link segments by xml id for quick lookups
   */  
  protected Map<String, MacroscopicLinkSegment> linkSegmentsByXmlId = null;  
  
  /**
   * initialise indices if not done so by the user
   */
  protected void initialiseNetworkReferenceIndices(MacroscopicNetwork network) {
    /* xml ids are unique across all layers */
    if(nodesByXmlId == null) {
      nodesByXmlId = new HashMap<String, Node>();
      for(MacroscopicPhysicalNetwork layer : network.infrastructureLayers) {
        layer.nodes.forEach( node -> nodesByXmlId.put(node.getXmlId(), node));
      }
    }
    if(linkSegmentsByXmlId == null) {
      linkSegmentsByXmlId = new HashMap<String, MacroscopicLinkSegment>();
      for(MacroscopicPhysicalNetwork layer : network.infrastructureLayers) {
        layer.linkSegments.forEach( linkSegment -> linkSegmentsByXmlId.put(linkSegment.getXmlId(), linkSegment));
      }
    }
  }  
  
  /** set the zoning to populate
   * @param zoning to populate
   */
  protected void setZoning(Zoning zoning) {
    this.zoning = zoning;
  }
  
  /** set the network to utilise
   * @param network to use
   */
  protected void setNetwork(InfrastructureNetwork<?,?> network) {
    this.network = network;
  }
  
  /**
   * parse the OD zones from Xml element into Planit memory
   * @param nodesByXmlIds nodes indexed by xml id to use
   * @throws PlanItException thrown if error
   */
  protected void populateODZones() throws PlanItException{
    /* zone */
    for (final XMLElementZones.Zone xmlZone : getXmlRootElement().getZones().getZone()) {
      OdZone zone = parseBaseZone(zoning.odZones, xmlZone.getId(), xmlZone.getExternalid(), xmlZone.getCentroid());
      
      /* geometry */
      populateZoneGeometry(zone, xmlZone.getPolygon());      
                 
      /* connectoids */
      List<XMLElementConnectoid> xmlConnectoids = xmlZone.getConnectoids().getConnectoid();
      for(XMLElementConnectoid xmlConnectoid : xmlConnectoids) {                
        Odconnectoid xmlOdConnectoid = xmlConnectoid.getValue();
        
        /* parse the (Od, node reference based) undirected connectoid */
        UndirectedConnectoid connectoid = (UndirectedConnectoid) parseBaseConnectoid(xmlOdConnectoid);
        /* register zone */
        connectoid.addAccessZone(zone);
 
        /* parse length */
        populateConnectoidToZoneLengths(connectoid, xmlOdConnectoid, connectoid.getAccessVertex().getPosition(), jtsUtils);
      }             
    }
  }
  
  /** Constructor
   * @param settings to use
   * @param network to extract planit entities from by found references in zoning
   * @param zoning to populate
   */
  protected PlanitZoningReader(PlanitZoningReaderSettings settings, InfrastructureNetwork<?,?> network, Zoning zoning) {
    super(XMLElementMacroscopicZoning.class);
    this.settings = settings;
    setZoning(zoning);
    setNetwork(network);
  }  
    
  /** constructor
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param network to extract planit entities from by found references in zoning
   * @param zoning to populate
   * @throws PlanItException  thrown if error
   */
  protected PlanitZoningReader(String pathDirectory, String xmlFileExtension, InfrastructureNetwork<?,?> network, Zoning zoning) throws PlanItException{   
    super(XMLElementMacroscopicZoning.class);    
    this.settings = new PlanitZoningReaderSettings(pathDirectory, xmlFileExtension);    
    setZoning(zoning);
    setNetwork(network);
  }
  
  /** constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlMacroscopicZoning to extract from
   * @param network to extract planit entities from by found references in zoning
   * @param zoning to populate
   * @throws PlanItException  thrown if error
   */
  protected PlanitZoningReader(XMLElementMacroscopicZoning xmlMacroscopicZoning, InfrastructureNetwork<?,?> network, Zoning zoning) throws PlanItException{
    super(xmlMacroscopicZoning);
    this.settings =  new PlanitZoningReaderSettings();
    setZoning(zoning);
    setNetwork(network);
  }  

  /** read the zoning from disk
   * 
   * @param network this zoning is compatible with
   * @param nodesByXmlId to identify mapping between OD zones and network (via nodes)
   * @param linkSegmentsByXmlId to identify mapping between (transfer) connectoids and network
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
    initialiseNetworkReferenceIndices(macroscopicNetwork);
    
    // create and register zones, centroids and connectoids
    try {
      
      /* popoulate Xml memory model */
      initialiseAndParseXmlRootElement(getSettings().getInputPathDirectory(), getSettings().getXmlFileExtension());
      
      /* initialise and validate crs compatibility */
      initialiseZoningCrs(macroscopicNetwork);               
      
      /* OD zones */
      populateODZones();
      
      /* Intermodal/transfer zones, i.e., platforms, stations, etc. */
      populateIntermodal(macroscopicNetwork.modes, linkSegmentsByXmlId);
      
      /* free */
      clearXmlContent();
      
    } catch (PlanItException e) {
      throw e;
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when populating zoning in PLANitIO",e);
    }
    
    return zoning;
  }
  
  /** settings for this reader
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
    getSettings().reset();
    nodesByXmlId = null;
    linkSegmentsByXmlId = null;    
  }
  
  // GETTERS/SETTERS
  
  /** allow user to override the map containing the xml id to node mapping. If so, it avoids creating a duplicate index within the class instance
   * if one already exists
   * 
   * @param nodesByXmlId to use
   */
  public void setNodesByXmlId(Map<String, Node> nodesByXmlId) {
    this.nodesByXmlId = nodesByXmlId;
  }

  /** allow user to override the map containing the xml id to link segment mapping. If so, it avoids creating a duplicate index within the class instance
   * if one already exists
   * 
   * @param nodesByXmlId to use
   */  
  public void setLinkSegmentsByXmlId(Map<String, MacroscopicLinkSegment> linkSegmentsByXmlId) {
    this.linkSegmentsByXmlId = linkSegmentsByXmlId;
  }  
}