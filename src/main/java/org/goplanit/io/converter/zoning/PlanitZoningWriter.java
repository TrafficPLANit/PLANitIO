package org.goplanit.io.converter.zoning;

import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.converter.idmapping.ZoningIdMapper;
import org.goplanit.converter.zoning.ZoningWriter;
import org.goplanit.io.converter.network.UnTypedPlanitCrsWriterImpl;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.CharacterUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.zoning.Centroid;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.ConnectoidType;
import org.goplanit.utils.zoning.DirectedConnectoid;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.TransferZoneType;
import org.goplanit.utils.zoning.UndirectedConnectoid;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.xml.generated.Connectoidnodelocationtype;
import org.goplanit.xml.generated.Connectoidtypetype;
import org.goplanit.xml.generated.Intermodaltype;
import org.goplanit.xml.generated.Transferzonetype;
import org.goplanit.xml.generated.XMLElementCentroid;
import org.goplanit.xml.generated.XMLElementConnectoid;
import org.goplanit.xml.generated.XMLElementConnectoids;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
import org.goplanit.xml.generated.XMLElementTransferGroup;
import org.goplanit.xml.generated.XMLElementTransferZoneAccess;
import org.goplanit.xml.generated.XMLElementTransferZoneGroups;
import org.goplanit.xml.generated.XMLElementTransferZones;
import org.goplanit.xml.generated.XMLElementZones;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning.XMLElementIntermodal;
import org.goplanit.xml.generated.XMLElementTransferZoneAccess.XMLElementTransferConnectoid;
import org.goplanit.xml.generated.XMLElementTransferZones.XMLElementTransferZone;
import org.goplanit.zoning.Zoning;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A class that takes a PLANit zoning and persists it to file in the Planit native XML format. 
 * 
 * @author markr
 *
 */
public class PlanitZoningWriter extends UnTypedPlanitCrsWriterImpl<Zoning> implements ZoningWriter {
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitZoningWriter.class.getCanonicalName());  
    
  /** XML memory model equivalent of the PLANit memory mode */
  private final XMLElementMacroscopicZoning xmlRawZoning;

  /** mapping from zone to connectoids since in the memory model zones are not mapped to connectoids */
  private final Map<Zone,List<Connectoid>> zoneToConnectoidMap = new HashMap<>();
  
  /** settings to use */
  private final PlanitZoningWriterSettings settings;

  /** Convert PLANit connectoid type to XML PLANit connectoid type
   * 
   * @param connectoidType to convert
   * @return xml connectoid type created
   */
  private static Connectoidtypetype createXmlConnectoidType(final ConnectoidType connectoidType) {
    switch (connectoidType) {
      case UNKNOWN:
        return Connectoidtypetype.UNKNOWN;
      case PT_VEHICLE_STOP:
        return Connectoidtypetype.PT_VEH_STOP;
      case TRAVELLER_ACCESS:
        return Connectoidtypetype.TRAVELLER_ACCESS;
      case NONE:
        return Connectoidtypetype.NONE;        
      default:
        LOGGER.warning(String.format("Unsupported connectoid type %s found, changed to `unknown`",connectoidType.value()));
        return Connectoidtypetype.UNKNOWN;
    }
  } 
  
  /** Convert PLANit connectoid type to XML PLANit connectoid type
   * 
   * @param transferZoneType to convert
   * @return xml connectoid type created
   */
  private static Transferzonetype createXmlTransferZoneType(final TransferZoneType transferZoneType) {
    switch (transferZoneType) {
      case UNKNOWN:
        return Transferzonetype.UNKNOWN;
      case PLATFORM:
        return Transferzonetype.PLATFORM;
      case POLE:
        return Transferzonetype.STOP_POLE;
      case SMALL_STATION:
        return Transferzonetype.SMALL_STATION;
      case STATION:
        return Transferzonetype.STATION;        
      default:
        LOGGER.warning(String.format("Unsupported transfer zone type %s found, changed to `unknown`",transferZoneType.value()));
        return Transferzonetype.UNKNOWN;
    }
  }   
  
  /** Create mapping from zone to its connectoids
   * 
   * @param zoning to base mapping on
   */
  private void createZoneToConnectoidIndices(final Zoning zoning) {
    for(var connectoid : zoning.getOdConnectoids()) {
      for(var zone : connectoid.getAccessZones()) {
        zoneToConnectoidMap.putIfAbsent(zone, new ArrayList<>(1));
        zoneToConnectoidMap.get(zone).add(connectoid);
      }
    }
    for(var connectoid : zoning.getTransferConnectoids()) {
      for(var zone : connectoid.getAccessZones()) {
        zoneToConnectoidMap.putIfAbsent(zone, new ArrayList<>(1));
        zoneToConnectoidMap.get(zone).add(connectoid);
      }
    }    
  }  
  
  /** Populate the XML transfer group based on the PLANit memory model transfer zone group instance
   * @param xmlTransferGroup to populate
   * @param transferGroup to use
   */
  private void populateXmlTransferGroup(final XMLElementTransferGroup xmlTransferGroup, final TransferZoneGroup transferGroup) {
    if(xmlTransferGroup==null) {
      LOGGER.severe(String.format("Unable to add transfer zone group %s (id:%d) to xml element, xml element is null", transferGroup.getXmlId(), transferGroup.getId()));
      return;
    }
    
    if(!transferGroup.hasTransferZones()) {
      LOGGER.warning(String.format("DISCARD: transfer zone group %s (id:%d) has no transfer zones, it will not be populated", transferGroup.getXmlId(), transferGroup.getId()));
      return;
    }
    
    /* id */
    xmlTransferGroup.setId(getPrimaryIdMapper().getTransferZoneGroupIdMapper().apply(transferGroup));
    if(StringUtils.isNullOrBlank(xmlTransferGroup.getId())) {
      LOGGER.severe(String.format("Transfer zone group id for XML not set successfully for planit transfer zone group %s (id:%d)",transferGroup.getXmlId(), transferGroup.getId()));
    }
    
    /* external id */
    if(transferGroup.hasExternalId()) {
      xmlTransferGroup.setExternalid(transferGroup.getExternalId());
    }
    
    /* name */
    if(transferGroup.hasName()) {
      xmlTransferGroup.setName(transferGroup.getName());
    } 
        
    /* transfer zones */
    xmlTransferGroup.setTzrefs(
        transferGroup.getTransferZones().stream().map(
                transferZone -> getPrimaryIdMapper().getZoneIdMapper().apply(transferZone)).sorted().collect(
                        Collectors.joining(getSettings().getCommaSeparator().toString())));
  }

  /** Populate the transfer zone groups within the intermodal XML element
   * 
   * @param zoning to use
   * @param xmlIntermodal to use
   */
  private void populateXmlTransferZoneGroups(final Zoning zoning, final XMLElementIntermodal xmlIntermodal) {
    /* transfer zone groups are optional, so simply ignore when nto present */
    if(zoning== null || zoning.getTransferZoneGroups().isEmpty()) {
      return;
    }
    LOGGER.info("Transfer zone groups: " + zoning.getTransferZoneGroups().size());
    
    if(xmlIntermodal.getValue().getTransferzonegroups()==null) {
      xmlIntermodal.getValue().setTransferzonegroups(new XMLElementTransferZoneGroups());
    }
    
    /* transfer zone groups */   
    XMLElementTransferZoneGroups xmlTransferZoneGroups = xmlIntermodal.getValue().getTransferzonegroups();
    zoning.getTransferZoneGroups().streamSortedBy(getPrimaryIdMapper().getTransferZoneGroupIdMapper()).forEach( transferGroup -> {
      
      if(!transferGroup.hasTransferZones()) {
        LOGGER.warning(String.format("DISCARD: transfer zone group %s (id:%d) is dangling", transferGroup.getXmlId(), transferGroup.getId()));
        return;
      }
      
      /* populate transfer group */
      XMLElementTransferGroup xmlTransferGroup = new XMLElementTransferGroup();              
      populateXmlTransferGroup(xmlTransferGroup, transferGroup);
                     
      /* register */        
      xmlTransferZoneGroups.getTransfergroup().add(xmlTransferGroup);
    });
  }

  /** Populate a transfer connectoid
   * 
   * @param xmlTransferConnectoid to populate
   * @param transferConnectoid to use
   */
  private void populateXmlTransferConnectoid(
      final XMLElementTransferConnectoid xmlTransferConnectoid, final DirectedConnectoid transferConnectoid) {
    
    if(!transferConnectoid.hasAccessZones()) {
      LOGGER.warning(String.format("DISCARD: transfer connectoid %s (id:%d) is dangling", transferConnectoid.getXmlId(), transferConnectoid.getId()));
      return;
    }
    if(!transferConnectoid.hasAccessLinkSegment()) {
      LOGGER.warning(String.format("DISCARD: transfer connectoid %s (id:%d) has no access link segment", transferConnectoid.getXmlId(), transferConnectoid.getId()));
      return;
    }    
    Zone firstAccessZone = transferConnectoid.getFirstAccessZone();
    
    /* the memory model supports a dedicated length for each transfer zone - connectoid combination. However, currently
     * the xml format only supports a single length per transfer connectoid across all transfer zones. Hence, we verify
     * that all lengths across its access zones are equal and if not we log a warning and indicate what length we choose */
    if(transferConnectoid.getAccessZones().size()>1) {
      Double lengthKm = null;
      for(var zone : transferConnectoid.getAccessZones()){
        Optional<Double> currLengthKm = transferConnectoid.getLengthKm(zone);
        if(lengthKm == null) {
          lengthKm = currLengthKm.get();
        }else if(currLengthKm.isPresent() && !Precision.equal(lengthKm, currLengthKm.get(), Precision.EPSILON_6)) {
          /* TODO: should be rectified in xml format xsd and implementation see issue #12 in PlanitXMLGenerator */
          LOGGER.warning(String.format(
              "Transfer connectoid %s (id:%d) has different lengths specified for different access zones it services, this is not yet supported in the Planit XML format, choosing first available length %.2f",
                transferConnectoid.getXmlId(), transferConnectoid.getId(), transferConnectoid.getLengthKm(transferConnectoid.getFirstAccessZone()).get()));
          break;
        }
      }
    }
    
    /* the memory model also supports a dedicated number of supported modes for each transfer zone - connectoid combination. Same problem applies
     * here as for length. It should be per combination, but is only supported across the connectoid. Check if this is a problem. If so, log and indicated
     * we allow all modes found across all combinations as the modes that we support */
    Collection<Mode> explicitAllowedModes =  transferConnectoid.getExplicitlyAllowedModes(firstAccessZone);
    if(transferConnectoid.getAccessZones().size()>1) {
      boolean valid = true;
      Zone prevZone = firstAccessZone;
      for(Zone zone : transferConnectoid.getAccessZones()) {
        if(transferConnectoid.isAllModesAllowed(prevZone) == transferConnectoid.isAllModesAllowed(zone)) {
          prevZone = zone;         
        }
        
        if(valid && !transferConnectoid.isAllModesAllowed(zone)) {
          valid = explicitAllowedModes!=null;
          if(!valid) {
            continue;
          }
          valid = transferConnectoid.getExplicitlyAllowedModes(zone).containsAll(explicitAllowedModes);
          valid = valid || explicitAllowedModes.containsAll(transferConnectoid.getExplicitlyAllowedModes(zone));
          if(!valid) {
            explicitAllowedModes.addAll(transferConnectoid.getExplicitlyAllowedModes(zone));
          }
        }
      }
      if(!valid) {
        /* TODO: should be rectified in xml format xsd and implementation see issue #14 in PlanitXMLGenerator */
        LOGGER.warning(String.format(
            "Transfer connectoid has different supported modes for different access zones it services, this is not yet supported in the Planit XML format: Allowing all modes across all access zones of connectoid instead",
              transferConnectoid.getXmlId(), transferConnectoid.getId()));
      }
    }

    /* populate base pertaining to any connectoid*/
    populateXmlConnectoidBase(
        xmlTransferConnectoid, transferConnectoid, transferConnectoid.getLengthKm(firstAccessZone), explicitAllowedModes);
    
    /* transferzone references */
    String xmlTzRefs = transferConnectoid.getAccessZones().stream().map(
            zone -> getPrimaryIdMapper().getZoneIdMapper().apply(zone)).sorted().collect(Collectors.joining(","));
    xmlTransferConnectoid.setTzrefs(xmlTzRefs);
    
    /* link segment reference */
    xmlTransferConnectoid.setLsref(
            getComponentIdMappers().getNetworkIdMappers().getLinkSegmentIdMapper().apply(
                    (MacroscopicLinkSegment)transferConnectoid.getAccessLinkSegment()));
    
    /* access node is derived based on up or downstream location relative to link segment, 
     * only persist if not the default is used*/
    if(!transferConnectoid.isNodeAccessDownstream()) {
      xmlTransferConnectoid.setLoc(Connectoidnodelocationtype.UPSTREAM);
    }
    if( (transferConnectoid.isNodeAccessDownstream() && !transferConnectoid.getAccessNode().idEquals(transferConnectoid.getAccessLinkSegment().getDownstreamVertex()))
        ||
        (!transferConnectoid.isNodeAccessDownstream() && !transferConnectoid.getAccessNode().idEquals(transferConnectoid.getAccessLinkSegment().getUpstreamVertex()))){
      LOGGER.warning(String.format(
          "Transfer connectoid %s (id:%d) access node location is in conflict with the registered access node", transferConnectoid.getXmlId(), transferConnectoid.getId()));
    }

  }

  /** Populate an XML transfer zone
   * 
   * @param transferZone to use
   * @param xmlTransferZones to add xml transfer zone to
   */
  private void populateXmlTransferZone(final TransferZone transferZone, final XMLElementTransferZones xmlTransferZones) {
    /* register */
    XMLElementTransferZone xmlTransferZone = new XMLElementTransferZone();
    xmlTransferZones.getZone().add(xmlTransferZone);
    
    /* id */
    xmlTransferZone.setId(getPrimaryIdMapper().getZoneIdMapper().apply(transferZone));
    
    /* external id */
    if(transferZone.hasExternalId()) {
      xmlTransferZone.setExternalid(transferZone.getExternalId());
    }
    
    /* name */
    if(transferZone.hasName()) {
      xmlTransferZone.setName(transferZone.getName());
    }

    /* platform names */
    if(transferZone.hasPlatformNames()){
      xmlTransferZone.setPlatforms(transferZone.getTransferZonePlatformNames().stream().sorted().collect(Collectors.joining(CharacterUtils.COMMA.toString())));
    }
    
    /* type */
    if(!transferZone.getTransferZoneType().equals(TransferZoneType.NONE)) {
      xmlTransferZone.setType(createXmlTransferZoneType(transferZone.getTransferZoneType()));
    }
    
    /* polygon/linestring - point is handled via centroid */
    boolean geometryIsPoint = false;
    Function<Zone, Point> getCentroidLocation = z -> z.getCentroid().getPosition();
    if(transferZone.hasGeometry()) {
      if(transferZone.getGeometry() instanceof Polygon) {
        xmlTransferZone.setPolygon(createGmlPolygonType((Polygon)transferZone.getGeometry()));
      }else if(transferZone.getGeometry() instanceof LineString) {        
        xmlTransferZone.setLineString(createGmlLineStringType((LineString)transferZone.getGeometry()));
      }else if(transferZone.getGeometry() instanceof Point) {
        getCentroidLocation = z -> (Point) z.getGeometry();
        geometryIsPoint = true;
      }
    }

    /* centroid or geometry is point, which will be processed as centroid */
    if(transferZone.hasCentroid() && transferZone.getCentroid().hasPosition() || geometryIsPoint) {
      XMLElementCentroid xmlCentroid = new XMLElementCentroid();
      var centroid = transferZone.getCentroid();
      populateXmlCentroid(
          xmlCentroid, centroid!= null ? transferZone.getCentroid().getName() : "", getCentroidLocation.apply(transferZone));
      xmlTransferZone.setCentroid(xmlCentroid);
    }        
  }

  /** Populate the transfer zone access (connectoids) within the intermodal XML element
   * 
   * @param zoning to use
   * @param xmlIntermodal to use
   */
  private void populateXmlTransferZoneAccess(final Zoning zoning, final XMLElementIntermodal xmlIntermodal) {
    if(zoning== null || zoning.getTransferConnectoids().isEmpty()) {
      LOGGER.severe("transfer zone access should not be persisted when no transfer connectoids exist on the zoning");
      return;
    }
    
    if(xmlIntermodal.getValue().getTransferzoneaccess()==null) {
      xmlIntermodal.getValue().setTransferzoneaccess(new XMLElementTransferZoneAccess());
    }
    
    /* transfer zone access */   
    var xmlTransferZoneAccess = xmlIntermodal.getValue().getTransferzoneaccess();
    zoning.getTransferConnectoids().streamSortedBy(getPrimaryIdMapper().getConnectoidIdMapper()).forEach( transferConnectoid -> {
      
      if(!transferConnectoid.hasAccessZones()) {
        LOGGER.warning(String.format("DISCARD: transfer connectoid %s (id:%d) is dangling", transferConnectoid.getXmlId(), transferConnectoid.getId()));
        return;
      }
      if(!transferConnectoid.hasAccessLinkSegment()) {
        LOGGER.warning(String.format("DISCARD: transfer connectoid %s (id:%d) has no access link segment", transferConnectoid.getXmlId(), transferConnectoid.getId()));
        return;
      }
      
      /* populate od connectoid */
      var xmlTransferConnectoidBase = new XMLElementTransferConnectoid();              
      populateXmlTransferConnectoid(xmlTransferConnectoidBase, transferConnectoid);
                     
      /* register */        
      xmlTransferZoneAccess.getConnectoid().add(xmlTransferConnectoidBase);
    });
    LOGGER.info("Transfer connectoids: " +zoning.getTransferConnectoids().size());
  }


  /** Populate the transfer zones within the intermodal XML element
   * 
   * @param zoning to use
   * @param xmlIntermodal to use
   */  
  private void populateXmlTransferZones(final Zoning zoning, final XMLElementIntermodal xmlIntermodal) {
    if(zoning==null || zoning.getTransferConnectoids().isEmpty()) {
      LOGGER.severe("Transfer zones should not be persisted when no transfer zones exist on the zoning");
      return;
    }    
    LOGGER.info("TransferZones: " + zoning.getTransferZones().size());
    
    if(xmlIntermodal.getValue().getTransferzones()==null) {
      xmlIntermodal.getValue().setTransferzones(new XMLElementTransferZones());
    }
    
    /* transfer zones */
    var xmlTransferZones = xmlIntermodal.getValue().getTransferzones();
    zoning.getTransferZones().streamSortedBy(getPrimaryIdMapper().getZoneIdMapper()).forEach( transferZone -> {
      
      /* transfer zone */
      populateXmlTransferZone(transferZone, xmlTransferZones);
    });
  }

  /** Populate a centroid
   * 
   * @param xmlCentroid to populate
   * @param name of the centroid
   * @param centroidLocation of the centroid
   */
  private void populateXmlCentroid(final XMLElementCentroid xmlCentroid, final String name, final Point centroidLocation) {
    
    /* name */
    if(!StringUtils.isNullOrBlank(name)) {
      xmlCentroid.setName(name);
    }
    
    /* position */
    if(centroidLocation != null) {
      xmlCentroid.setPoint(createGmlPointType(centroidLocation));
    }
  }
  
  /** Populate the generic part of any connectoid
   * 
   * @param xmlConnectoidBase to populate
   * @param connectoid the planit connectoid to extract from
   * @param lengthKm when present the length is set, when not, it is omitted (default assumed)
   * @param accessModes to use, when null it is left out (default), otherwise these modes are set as explicitly allowed access modes
   */  
  private void populateXmlConnectoidBase(
      final org.goplanit.xml.generated.Connectoidtype xmlConnectoidBase, final Connectoid connectoid, final Optional<Double> lengthKm, final Collection<Mode> accessModes) {
    /* id */
    xmlConnectoidBase.setId(getPrimaryIdMapper().getConnectoidIdMapper().apply(connectoid));
    if(StringUtils.isNullOrBlank(xmlConnectoidBase.getId())){
      LOGGER.severe(String.format("Connectoid id for xml remains null for connectoid (id:%d), this is not allowed",connectoid.getId()));
    }
    
    /* external id */
    if(connectoid.hasExternalId()) {
      xmlConnectoidBase.setExternalid(connectoid.getExternalId());  
    }
    
    /* name */
    if(connectoid.hasName()) {
      xmlConnectoidBase.setName(connectoid.getName());
    }
    
    /* type */
    if(!connectoid.getType().equals(ConnectoidType.NONE)) {
      xmlConnectoidBase.setType(createXmlConnectoidType(connectoid.getType()));
    }
    
    /* length */
    if(lengthKm.isPresent()) {
      xmlConnectoidBase.setLength(lengthKm.get());
    }    
    
    /* explicitly allowed modes for zone */
    if(accessModes!=null) {
      String csvModeIdString = 
          accessModes.stream().map(
                  mode -> getComponentIdMappers().getNetworkIdMappers().getModeIdMapper().apply(mode)).sorted().collect(
                          Collectors.joining(String.valueOf(getSettings().getCommaSeparator())));
      xmlConnectoidBase.setModes(csvModeIdString);  
    }
        
  }

  /** Populate the od specific part of the connectoid which is the access vertex reference
   * 
   * @param xmlConnectoid to populate
   * @param odConnectoid the planit connectoid to extract from
   * @param accessZone of this connectoid
   */
  private void populateXmlOdConnectoid(final XMLElementConnectoid xmlConnectoid, final UndirectedConnectoid odConnectoid, final Zone accessZone) {
    
    if(!odConnectoid.hasAccessZone(accessZone)) {
      LOGGER.severe(String.format("od conectoid %s (id:%d) is expected to support od zone %s (id:%d), but zone is not registered as access zone", 
          odConnectoid.getXmlId(), odConnectoid.getId(), accessZone.getXmlId(), accessZone.getId()));
    }

    xmlConnectoid.setType(Connectoidtypetype.TRAVELLER_ACCESS);
    
    /* populate extension pertaining to od connectoid */
    xmlConnectoid.setNoderef(getComponentIdMappers().getNetworkIdMappers().getVertexIdMapper().apply(odConnectoid.getAccessVertex()));
    
    /* populate base pertaining to any connectoid*/
    populateXmlConnectoidBase(
            xmlConnectoid, odConnectoid, odConnectoid.getLengthKm(accessZone), odConnectoid.getExplicitlyAllowedModes(accessZone));
  }   

  /** Populate an XML origin-destination zone
   * 
   * @param zoning to use
   * @param odZone to extract information from
   */
  private void populateXmlOdZone(final Zoning zoning, final OdZone odZone) {
    if(!zoneToConnectoidMap.containsKey(odZone)) {
      LOGGER.warning(String.format("DISCARD: od zone %s (id: %d) without connectoids found; dangling", odZone.getXmlId(), odZone.getId()));
      return;
    }
    
    var xmlOdZone = new XMLElementZones.Zone();
    xmlRawZoning.getZones().getZone().add(xmlOdZone);
    
    /* (xml) id */
    xmlOdZone.setId(getPrimaryIdMapper().getZoneIdMapper().apply(odZone));
    
    /* external id */
    if(odZone.hasExternalId()) {
      xmlOdZone.setExternalid(odZone.getExternalId());
    }
    
    /* name */
    if(odZone.hasName()) {
      xmlOdZone.setName(odZone.getName());
    }
    
    /* main geometry, e.g., polygon */
    boolean geometryIsPoint = false;
    Function<Zone, Point> getCentroidLocation = z -> z.getCentroid().getPosition();
    if(odZone.hasGeometry()) {
      if(odZone.getGeometry() instanceof Polygon) {
        xmlOdZone.setPolygon(createGmlPolygonType((Polygon)odZone.getGeometry()));
      }else if(odZone.getGeometry() instanceof Point) {
        getCentroidLocation = z -> (Point) z.getGeometry();
        geometryIsPoint = true;
      }
    }

    /* centroid or geometry is point, which will be processed as centroid */
    if(odZone.hasCentroid() && odZone.getCentroid().hasPosition() || geometryIsPoint) {
      XMLElementCentroid xmlCentroid = new XMLElementCentroid();
      var centroid = odZone.getCentroid();
      populateXmlCentroid(
          xmlCentroid, centroid!= null ? odZone.getCentroid().getName() : "", getCentroidLocation.apply(odZone));
      xmlOdZone.setCentroid(xmlCentroid);
    }

    /* connectoids */
    var xmlConnectoids = new XMLElementConnectoids();
    xmlOdZone.setConnectoids(xmlConnectoids);
    zoneToConnectoidMap.get(odZone).stream().sorted(
        Comparator.comparing(getPrimaryIdMapper().getConnectoidIdMapper())).forEach(connectoid -> {
            
      /* od zones in xml only record their undirected connectoids at this point in time since they allow access from all incoming link(segment)s */
      if(connectoid instanceof UndirectedConnectoid) {
        
        var odConnectoid = (UndirectedConnectoid)connectoid;
        if(!odConnectoid.hasAccessZone(odZone)) {
          LOGGER.severe(String.format("OD conectoid %s (id:%d) is expected to support od zone %s (id:%d), but zone is not registered as access zone",
              odConnectoid.getXmlId(), odConnectoid.getId(), odZone.getXmlId(), odZone.getId()));
        }
        
        /* populate od connectoid */
        var xmlOdConnectoidBase = new XMLElementConnectoid();              
        populateXmlOdConnectoid(xmlOdConnectoidBase, odConnectoid, odZone);
                       
        /* register */        
        xmlConnectoids.getConnectoid().add(xmlOdConnectoidBase);                        
      }
    });
    LOGGER.info("Od connectoids: " +zoning.getOdConnectoids().size());
  }
  
  /** Populate the XML id of the XML zoning element
   * 
   * @param zoning to extract XML id from
   */
  private void populateXmlId(Zoning zoning) {
    /* xml id */
    String xmlId = getPrimaryIdMapper().getZoningIdMapper().apply(zoning);
    if(StringUtils.isNullOrBlank(xmlId)) {
      LOGGER.warning(String.format("Zoning has no XML id defined, adopting internally generated id %d instead",zoning.getId()));
      xmlId = String.valueOf(zoning.getId());
      zoning.setXmlId(xmlId);
    }
    xmlRawZoning.setId(xmlId);
  }

  /** Make sure the XML zonings destination crs is set (if any)
   */
  private void populateXmlZoningSrsName(){
    xmlRawZoning.setSrsname(extractSrsName(getDestinationCoordinateReferenceSystem()));
  }  
  
  /** Populate the origin-destination zones of this zoning
   * 
   * @param zoning to use
   */
  private void populateXmlOdZones(final Zoning zoning) {
    if(zoning.getOdZones().isEmpty()) {
      LOGGER.severe("No OD zones present when creating zoning XML elements");
      return;
    }

    LOGGER.info("OD Zones: " + zoning.getOdZones().size());      
    var xmlOdZones = xmlRawZoning.getZones();
    if(xmlOdZones == null) {
      xmlOdZones = new XMLElementZones();
      xmlRawZoning.setZones(xmlOdZones);
    }
    
    zoning.getOdZones().streamSortedBy(getPrimaryIdMapper().getZoneIdMapper()).forEach( odZone -> {
      /* modes */
      populateXmlOdZone(zoning, odZone); 
    });
  }

  /** Populate the transfer zones of this zoning
   * 
   * @param zoning to use
   */
  private void populateXmlIntermodal(final Zoning zoning) {
    if(zoning.getTransferZones().isEmpty() && zoning.getTransferConnectoids().isEmpty()) {
      LOGGER.severe("Transfer zones and/or connectoids should be present when creating intermodal XML elements, but they are empty, abort");
      return;
    }

    var xmlIntermodal = xmlRawZoning.getIntermodal();
    if(xmlIntermodal == null) {
      xmlIntermodal = new XMLElementIntermodal(new Intermodaltype());
      xmlRawZoning.setIntermodal(xmlIntermodal);
    }
    
    /* transfer zones */
    populateXmlTransferZones(zoning, xmlIntermodal);
    
    /* transfer zone access */
    populateXmlTransferZoneAccess(zoning, xmlIntermodal);
    
    /* transfer zone groups */
    populateXmlTransferZoneGroups(zoning, xmlIntermodal);
  }

  /** Constructor 
   * 
   * @param zoningPath to persist zoning on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   * @param xmlRawZoning XML zoning to populate
   */
  protected PlanitZoningWriter(
      final String zoningPath, final String countryName, final XMLElementMacroscopicZoning xmlRawZoning) {
    this(
        new PlanitZoningWriterSettings(zoningPath, PlanitZoningWriterSettings.DEFAULT_ZONING_XML, countryName),
        xmlRawZoning);
  }

  /** Constructor
   *
   * @param settings to use
   * @param xmlRawZoning XML zoning to populate
   */
  protected PlanitZoningWriter(PlanitZoningWriterSettings settings, XMLElementMacroscopicZoning xmlRawZoning) {
    super(IdMapperType.XML);
    this.settings = settings;
    this.xmlRawZoning = xmlRawZoning;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ZoningIdMapper getPrimaryIdMapper() {
    return getComponentIdMappers().getZoningIdMappers();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final Zoning zoning) {
    PlanItRunTimeException.throwIfNull(zoning, "Zoning is null cannot write to Planit native format");

    /* initialise */
    {
      getComponentIdMappers().populateMissingIdMappers(getIdMapperType());
      prepareCoordinateReferenceSystem(zoning.getCoordinateReferenceSystem(), getSettings().getDestinationCoordinateReferenceSystem(), getSettings().getCountry());
      LOGGER.info(String.format("Persisting PLANit zoning to: %s", Paths.get(getSettings().getOutputDirectory(), getSettings().getFileName())));
      
      createZoneToConnectoidIndices(zoning); 
    }
    
    getSettings().logSettings();
    
    /* xml id */
    populateXmlId(zoning);
    
    /* crs */
    populateXmlZoningSrsName();
    
    /* Od zones */
    populateXmlOdZones(zoning);
    
    /* intermodal zones */
    if(!zoning.getTransferZones().isEmpty() || !zoning.getTransferConnectoids().isEmpty()) {
      populateXmlIntermodal(zoning);
    }
    
    /* persist */
    super.persist(xmlRawZoning, XMLElementMacroscopicZoning.class, PlanitSchema.MACROSCOPIC_ZONING_XSD);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void reset() {
    xmlRawZoning.setZones(null);
    xmlRawZoning.setIntermodal(null);
    xmlRawZoning.setSrsname(null);
    
    zoneToConnectoidMap.clear();    
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitZoningWriterSettings getSettings() {
    return this.settings;
  }

}
