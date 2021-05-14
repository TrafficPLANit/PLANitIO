package org.planit.io.converter.zoning;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.converter.IdMapperType;
import org.planit.converter.zoning.ZoningWriter;
import org.planit.io.converter.PlanitWriterImpl;
import org.planit.io.xml.util.PlanitSchema;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.math.Precision;
import org.planit.utils.misc.StringUtils;
import org.planit.utils.mode.Mode;
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
import org.planit.xml.generated.Connectoidnodelocationtype;
import org.planit.xml.generated.Connectoidtypetype;
import org.planit.xml.generated.Intermodaltype;
import org.planit.xml.generated.Odconnectoid;
import org.planit.xml.generated.Transferzonetype;
import org.planit.xml.generated.XMLElementCentroid;
import org.planit.xml.generated.XMLElementConnectoid;
import org.planit.xml.generated.XMLElementConnectoids;
import org.planit.xml.generated.XMLElementMacroscopicZoning;
import org.planit.xml.generated.XMLElementMacroscopicZoning.XMLElementIntermodal;
import org.planit.xml.generated.XMLElementTransferGroup;
import org.planit.xml.generated.XMLElementTransferZoneAccess;
import org.planit.xml.generated.XMLElementTransferZoneAccess.XMLElementTransferConnectoid;
import org.planit.xml.generated.XMLElementTransferZoneGroups;
import org.planit.xml.generated.XMLElementTransferZones.XMLElementTransferZone;
import org.planit.xml.generated.XMLElementTransferZones;
import org.planit.xml.generated.XMLElementZones;
import org.planit.zoning.Zoning;

/**
 * A class that takes a PLANit zoning and persists it to file in the Planit native XML format. 
 * 
 * @author markr
 *
 */
public class PlanitZoningWriter extends PlanitWriterImpl<Zoning> implements ZoningWriter {
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitZoningWriter.class.getCanonicalName());  
    
  /** XML memory model equivalent of the PLANit memory mode */
  private final XMLElementMacroscopicZoning xmlRawZoning;
  
  /** the source crs to use */
  private final CoordinateReferenceSystem sourceCrs;
  
  /** mapping from zone to connectoids since in the memory model zones are not mapped to connectoids */
  private final Map<Zone,List<Connectoid>> zoneToConnectoidMap = new HashMap<Zone,List<Connectoid>>();
  
  /** settings to use */
  private final PlanitZoningWriterSettings settings;
  
  /** Convert planit connectoid type to xml planit connectoid type
   * @param connectoidType to convert
   * @return xml connectoid type created
   */
  private static Connectoidtypetype createXmlConnectoidType(ConnectoidType connectoidType) {
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
  
  /** Convert planit connectoid type to xml planit connectoid type
   * @param connectoidType to convert
   * @return xml connectoid type created
   */
  private static Transferzonetype createXmlTransferZoneType(TransferZoneType transferZoneType) {
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
  
  /** create mapping from zone to its connectoids
   * @param zoning to base mapping on
   */
  private void createZoneToConnectoidIndices(Zoning zoning) {
    for(Connectoid connectoid : zoning.odConnectoids) {
      for(Zone zone : connectoid.getAccessZones()) {
        zoneToConnectoidMap.putIfAbsent(zone, new ArrayList<Connectoid>(1));
        zoneToConnectoidMap.get(zone).add(connectoid);
      }
    }
    for(Connectoid connectoid : zoning.transferConnectoids) {
      for(Zone zone : connectoid.getAccessZones()) {
        zoneToConnectoidMap.putIfAbsent(zone, new ArrayList<Connectoid>(1));
        zoneToConnectoidMap.get(zone).add(connectoid);
      }
    }    
  }  
  
  /** populate the xml transfer group based on the planit memory model transfer zone group instance
   * @param xmlTransferGroup to populate
   * @param transferGroup to use
   */
  private void populateXmlTransferGroup(XMLElementTransferGroup xmlTransferGroup, TransferZoneGroup transferGroup) {
    if(xmlTransferGroup==null) {
      LOGGER.severe(String.format("Unable to add transfer zone group %s (id:%d) to xml element, xml element is null", transferGroup.getXmlId(), transferGroup.getId()));
      return;
    }
    
    if(!transferGroup.hasTransferZones()) {
      LOGGER.warning(String.format("DISCARD: transfer zone group %s (id:%d) has no transfer zones, it will not be populated", transferGroup.getXmlId(), transferGroup.getId()));
      return;
    }
    
    /* id */
    xmlTransferGroup.setId(getTransferZoneGroupIdMapper().apply(transferGroup));
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
        transferGroup.getTransferZones().stream().map( transferZone -> getZoneIdMapper().apply(transferZone)).collect(Collectors.joining(getSettings().getCommaSeparator().toString())));
  }

  /** populate the transfer zone groups within the intermodal xml element
   * @param zoning to use
   * @param xmlIntermodal to use
   */
  private void populateXmlTransferZoneGroups(Zoning zoning, XMLElementIntermodal xmlIntermodal) {
    /* transfer zone groups are optional, so simply ignore when nto present */
    if(zoning== null || zoning.transferZoneGroups.isEmpty()) {
      return;
    }
    
    if(xmlIntermodal.getValue().getTransferzonegroups()==null) {
      xmlIntermodal.getValue().setTransferzonegroups(new XMLElementTransferZoneGroups());
    }
    
    /* transfer zone groups */   
    XMLElementTransferZoneGroups xmlTransferZoneGroups = xmlIntermodal.getValue().getTransferzonegroups();
    for(TransferZoneGroup transferGroup : zoning.transferZoneGroups) {
      
      if(!transferGroup.hasTransferZones()) {
        LOGGER.warning(String.format("DISCARD: transfer zone group %s (id:%d) is dangling", transferGroup.getXmlId(), transferGroup.getId()));
        continue;
      }
      
      /* populate transfer group */
      XMLElementTransferGroup xmlTransferGroup = new XMLElementTransferGroup();              
      populateXmlTransferGroup(xmlTransferGroup, transferGroup);
                     
      /* register */        
      xmlTransferZoneGroups.getTransfergroup().add(xmlTransferGroup);
    }  
  }

  /** populate a transfer connectoid
   * @param xmlTransferConnectoid to populate
   * @param transferConnectoid to use
   * @throws PlanItException thrown if error
   */
  private void populateXmlTransferConnectoid(XMLElementTransferConnectoid xmlTransferConnectoid, DirectedConnectoid transferConnectoid) throws PlanItException {
    
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
      for(Zone zone : transferConnectoid.getAccessZones()) {
        Optional<Double> currLengthKm = transferConnectoid.getLengthKm(zone);
        if(lengthKm == null) {
          lengthKm = currLengthKm.get();
        }else if(currLengthKm.isPresent() && !Precision.isEqual(lengthKm, currLengthKm.get(), Precision.EPSILON_6)) {
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
    populateXmlConnectoidBase(xmlTransferConnectoid, transferConnectoid, transferConnectoid.getLengthKm(firstAccessZone), explicitAllowedModes);
    
    /* transferzone references */
    String xmlTzRefs = transferConnectoid.getAccessZones().stream().map( zone -> getZoneIdMapper().apply(zone)).collect(Collectors.joining(","));
    xmlTransferConnectoid.setTzrefs(xmlTzRefs);
    
    /* link segment reference */
    xmlTransferConnectoid.setLsref(getLinkSegmentIdMapper().apply((MacroscopicLinkSegment)transferConnectoid.getAccessLinkSegment()));
    
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

  /** Populate an xml transfer zone
   * 
   * @param transferZone to use
   * @param zoning to use
   * @param xmlTransferZones to add xml transfer zone to
   */
  private void populateXmlTransferZone(TransferZone transferZone, Zoning zoning, XMLElementTransferZones xmlTransferZones) {
    /* register */
    XMLElementTransferZone xmlTransferZone = new XMLElementTransferZone();
    xmlTransferZones.getZone().add(xmlTransferZone);
    
    /* id */
    xmlTransferZone.setId(getZoneIdMapper().apply(transferZone));
    
    /* external id */
    if(transferZone.hasExternalId()) {
      xmlTransferZone.setExternalid(transferZone.getExternalId());
    }
    
    /* name */
    if(transferZone.hasName()) {
      xmlTransferZone.setName(transferZone.getName());
    } 
    
    /* type */
    if(!transferZone.getTransferZoneType().equals(TransferZoneType.NONE)) {
      xmlTransferZone.setType(createXmlTransferZoneType(transferZone.getTransferZoneType()));
    }
    
    /* polygon/linestring - point is handled via centroid */
    if(transferZone.hasGeometry()) {
      if(transferZone.getGeometry() instanceof Polygon) {
        xmlTransferZone.setPolygon(createGmlPolygonType((Polygon)transferZone.getGeometry()));
      }else if(transferZone.getGeometry() instanceof LineString) {        
        xmlTransferZone.setLineString(createGmlLineStringType((LineString)transferZone.getGeometry()));
      }
    }   
    
    /* centroid */
    if(transferZone.hasCentroid()) {
      XMLElementCentroid xmlCentroid = new XMLElementCentroid();
      populateXmlCentroid(xmlCentroid, transferZone.getCentroid());
    }        
  }

  /** populate the transfer zone access (connectoids) within the intermodal xml element
   * @param zoning to use
   * @param xmlIntermodal to use
   * @throws PlanItException thrown if error
   */
  private void populateXmlTransferZoneAccess(Zoning zoning, XMLElementIntermodal xmlIntermodal) throws PlanItException {
    if(zoning== null || zoning.transferConnectoids.isEmpty()) {
      LOGGER.severe("transfer zone access should not be persisted when no transfer connectoids exist on the zoning");
      return;
    }
    
    if(xmlIntermodal.getValue().getTransferzoneaccess()==null) {
      xmlIntermodal.getValue().setTransferzoneaccess(new XMLElementTransferZoneAccess());
    }
    
    /* transfer zone access */   
    XMLElementTransferZoneAccess xmlTransferZoneAccess = xmlIntermodal.getValue().getTransferzoneaccess();
    for(DirectedConnectoid transferConnectoid : zoning.transferConnectoids) {
      
      if(!transferConnectoid.hasAccessZones()) {
        LOGGER.warning(String.format("DISCARD: transfer connectoid %s (id:%d) is dangling", transferConnectoid.getXmlId(), transferConnectoid.getId()));
        continue;
      }
      if(!transferConnectoid.hasAccessLinkSegment()) {
        LOGGER.warning(String.format("DISCARD: transfer connectoid %s (id:%d) has no access link segment", transferConnectoid.getXmlId(), transferConnectoid.getId()));
        continue;
      }
      
      /* populate od connectoid */
      XMLElementTransferConnectoid xmlTransferConnectoidBase = new XMLElementTransferConnectoid();              
      populateXmlTransferConnectoid(xmlTransferConnectoidBase, transferConnectoid);
                     
      /* register */        
      xmlTransferZoneAccess.getConnectoid().add(xmlTransferConnectoidBase);
    }     
  }


  /** populate the transfer zones within the intermodal xml element
   * @param zoning to use
   * @param xmlIntermodal to use
   */  
  private void populateXmlTransferZones(Zoning zoning, XMLElementIntermodal xmlIntermodal) {
    if(zoning==null || zoning.transferConnectoids.isEmpty()) {
      LOGGER.severe("transfer zones should not be persisted when no transfer zones exist on the zoning");
      return;
    }
    
    if(xmlIntermodal.getValue().getTransferzones()==null) {
      xmlIntermodal.getValue().setTransferzones(new XMLElementTransferZones());
    }
    
    /* transfer zones */
    XMLElementTransferZones xmlTransferZones = xmlIntermodal.getValue().getTransferzones();
    for(TransferZone transferZone : zoning.transferZones) {
      
      /* transfer zone */
      populateXmlTransferZone(transferZone, zoning, xmlTransferZones);
    }    
  }

  /** Populate a centroid
   * 
   * @param xmlCentroid to populate
   * @param centroid to extract information from
   */
  private void populateXmlCentroid(XMLElementCentroid xmlCentroid, Centroid centroid) {
    
    /* name */
    if(centroid.hasName()) {
      xmlCentroid.setName(centroid.getName());
    }
    
    /* position */
    if(centroid.hasPosition()) {            
      xmlCentroid.setPoint(createGmlPointType(centroid.getPosition()));
    }
  }
  
  /** populate the generic part of any connectoid
   * 
   * @param xmlConnectoid to populate
   * @param connectoid the planit connectoid to extract from
   * @param lengthKm when present the lengthi s set, when not, it is omitted (default assumed)
   * @param accessModes to use, when null it is left out (default), otherwise these modes are set as explicitly allowed access modes
   * @throws PlanItException thrown if error
   */  
  private void populateXmlConnectoidBase(org.planit.xml.generated.Connectoidtype xmlConnectoidBase, Connectoid connectoid, Optional<Double> length, Collection<Mode> accessModes) throws PlanItException {
    /* id */
    xmlConnectoidBase.setId(getConnectoidIdMapper().apply(connectoid));
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
    if(length.isPresent()) {
      xmlConnectoidBase.setLength(length.get());
    }    
    
    /* explicitly allowed modes for zone */
    if(accessModes!=null) {
      String csvModeIdString = 
          accessModes.stream().map( mode -> getModeIdMapper().apply(mode)).collect(Collectors.joining(String.valueOf(getSettings().getCommaSeparator())));
      xmlConnectoidBase.setModes(csvModeIdString);  
    }
        
  }

  /** populate the od specific part of the connectoid which is the access vertex reference
   * 
   * @param xmlConnectoid to populate
   * @param odConnectoid the planit connectoid to extract from
   * @param accessZone of this connectoid
   * @throws PlanItException thrown if error
   */
  private void populateXmlOdConnectoid(XMLElementConnectoid xmlConnectoid, UndirectedConnectoid odConnectoid, Zone accessZone) throws PlanItException {
    
    if(!odConnectoid.hasAccessZone(accessZone)) {
      LOGGER.severe(String.format("od conectoid %s (id:%d) is expected to support od zone %s (id:%d), but zone is not registered as access zone", 
          odConnectoid.getXmlId(), odConnectoid.getId(), accessZone.getXmlId(), accessZone.getId()));
    }
    
    Odconnectoid xmlOdConnectoid = new Odconnectoid();
    xmlConnectoid.setValue(xmlOdConnectoid);    
    
    /* populate extension pertaining to od connectoid */       
    xmlOdConnectoid.setNoderef(getVertexIdMapper().apply(odConnectoid.getAccessVertex()));    
    
    /* populate base pertaining to any connectoid*/
    populateXmlConnectoidBase(xmlOdConnectoid, odConnectoid, odConnectoid.getLengthKm(accessZone), odConnectoid.getExplicitlyAllowedModes(accessZone));            
  }   

  /** populate an xml od zone
   * @param zoning to use
   * @param odZone to extract information from
   * @throws PlanItException thrown if error
   */
  private void populateXmlOdZone(Zoning zoning, OdZone odZone) throws PlanItException {
    if(!zoneToConnectoidMap.containsKey(odZone)) {
      LOGGER.warning(String.format("DISCARD: od zone %s (id: %d) without connectoids found; dangling", odZone.getXmlId(), odZone.getId()));
      return;
    }
    
    org.planit.xml.generated.XMLElementZones.Zone xmlOdZone = new XMLElementZones.Zone();
    xmlRawZoning.getZones().getZone().add(xmlOdZone);
    
    /* (xml) id */
    xmlOdZone.setId(getZoneIdMapper().apply(odZone));
    
    /* external id */
    if(odZone.hasExternalId()) {
      xmlOdZone.setExternalid(odZone.getExternalId());
    }
    
    /* name */
    if(odZone.hasName()) {
      xmlOdZone.setName(odZone.getName());
    }
    
    /* centroid */
    if(odZone.hasCentroid()) {
      XMLElementCentroid xmlCentroid = new XMLElementCentroid();
      xmlOdZone.setCentroid(xmlCentroid);
      
      populateXmlCentroid(xmlCentroid, odZone.getCentroid());      
    }    
    
    /* polygon */
    if(odZone.hasGeometry() && odZone.getGeometry() instanceof Polygon) {
      xmlOdZone.setPolygon(createGmlPolygonType((Polygon)odZone.getGeometry()));
    }
    
    /* connectoids */
    XMLElementConnectoids xmlConnectoids = new XMLElementConnectoids();
    xmlOdZone.setConnectoids(xmlConnectoids);
    for(Connectoid connectoid : zoneToConnectoidMap.get(odZone)) {
            
      /* od zones in xml only record their undirected connectoids at this point in time since they allow access froma ll incoming link(segment)s */
      if(connectoid instanceof UndirectedConnectoid) {
        
        UndirectedConnectoid odConnectoid = (UndirectedConnectoid)connectoid;
        if(!odConnectoid.hasAccessZone(odZone)) {
          LOGGER.severe(String.format("od conectoid %s (id:%d) is expected to support od zone %s (id:%d), but zone is not registered as access zone", 
              odConnectoid.getXmlId(), odConnectoid.getId(), odZone.getXmlId(), odZone.getId()));
        }
        
        /* populate od connectoid */
        XMLElementConnectoid xmlOdConnectoidBase = new XMLElementConnectoid();              
        populateXmlOdConnectoid(xmlOdConnectoidBase, odConnectoid, odZone);
                       
        /* register */        
        xmlConnectoids.getConnectoid().add(xmlOdConnectoidBase);                        
      }
    }    
  }
  
  /** make sure the zonings destination crs is set (if any)
   * @throws PlanItException thrown if error
   */
  private void populateCrs() throws PlanItException {
    if(getSettings().getDestinationCoordinateReferenceSystem() != null) {
      xmlRawZoning.setSrsname(extractSrsName(getSettings()));
    }
  }  
  
  /** populate the od zones of this zoning
   * 
   * @param zoning to use
   * @throws PlanItException thrown if error
   */
  private void populateXmlOdZones(Zoning zoning) throws PlanItException {
    if(!zoning.odZones.isEmpty()) {

      XMLElementZones xmlOdZones = xmlRawZoning.getZones();
      if(xmlOdZones == null) {
        xmlOdZones = new XMLElementZones();
        xmlRawZoning.setZones(xmlOdZones);
      }
      
      for(OdZone odZone : zoning.odZones) {
        /* modes */
        populateXmlOdZone(zoning, odZone); 
      }
    }    
  }

  /** populate the transfer zones of this zoning
   * 
   * @param zoning to use
   * @throws PlanItException thrown if error
   */  
  private void populateXmlIntermodal(Zoning zoning) throws PlanItException {
    if(zoning.transferZones.isEmpty() && zoning.transferConnectoids.isEmpty()) {
      LOGGER.severe("Transfer zones and/or connectoids should be present when creating intermodal xml elements, but they are empty, abort");
      return;
    }

    XMLElementIntermodal xmlIntermodal = xmlRawZoning.getIntermodal();
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
  
  /** default network file name to use */
  public static final String DEFAULT_ZONING_FILE_NAME = "zoning.xml";  
  
  
  /** Constructor 
   * @param networkPath to persist network on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   * @param xmlRawNetwork to populate with PLANit network when persisting
   */
  protected PlanitZoningWriter(String zoningPath, String countryName, CoordinateReferenceSystem zoningCrs, XMLElementMacroscopicZoning xmlRawZoning) {
    super(IdMapperType.XML);
    this.settings = new PlanitZoningWriterSettings(zoningPath, DEFAULT_ZONING_FILE_NAME, countryName);
    this.sourceCrs = zoningCrs;    
    this.xmlRawZoning = xmlRawZoning;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(Zoning zoning) throws PlanItException {    
    PlanItException.throwIfNull(zoning, "Zoning is null cannot write to Planit native format");

    /* initialise */
    {
      super.initialiseIdMappingFunctions();    
      super.prepareCoordinateReferenceSystem(sourceCrs);
      LOGGER.info(String.format("Persisting PLANit zoning to: %s", Paths.get(getSettings().getOutputPathDirectory(), getSettings().getFileName()).toString()));
      
      createZoneToConnectoidIndices(zoning); 
    }    
    
    /* crs */
    populateCrs();
    
    /* Od zones */
    populateXmlOdZones(zoning);
    
    /* intermodal zones */
    if(!zoning.transferZones.isEmpty() || !zoning.transferConnectoids.isEmpty()) {
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
