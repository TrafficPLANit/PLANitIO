package org.planit.io.converter.zoning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.converter.IdMapperType;
import org.planit.converter.zoning.ZoningWriter;
import org.planit.io.converter.PlanitWriterImpl;
import org.planit.io.xml.util.PlanitSchema;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.zoning.Centroid;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.ConnectoidType;
import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.utils.zoning.Zone;
import org.planit.xml.generated.Connectoidtypetype;
import org.planit.xml.generated.Odconnectoid;
import org.planit.xml.generated.XMLElementCentroid;
import org.planit.xml.generated.XMLElementConnectoid;
import org.planit.xml.generated.XMLElementConnectoids;
import org.planit.xml.generated.XMLElementMacroscopicZoning;
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
      default:
        return Connectoidtypetype.NONE;
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
      xmlCentroid.setPoint(createXmlOpenGisPointType(centroid.getPosition()));
    }
  }
  
  /** populate the od specific part of the connectoid which is the access vertex reference
   * 
   * @param xmlOdConnectoid to populate
   * @param connectoid the planit connectoid to extract from
   */
  private void populateXmlOdConnectoid(Odconnectoid xmlOdConnectoid, UndirectedConnectoid connectoid) {
    xmlOdConnectoid.setNoderef(getVertexIdMapper().apply(connectoid.getAccessVertex()));
  }   

  /** populate the generic part of any connectoid
   * 
   * @param xmlConnectoid to populate
   * @param connectoid the planit connectoid to extract from
   * @param zone we are popoulating for
   * @throws PlanItException thrown if error
   */  
  private void populateXmlConnectoidBase(org.planit.xml.generated.Connectoidtype xmlConnectoidBase, Connectoid connectoid, Zone zone) throws PlanItException {
    /* id */
    xmlConnectoidBase.setId(getConnectoidIdMapper().apply(connectoid));
    
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
    if(connectoid.hasLength(zone)) {
      xmlConnectoidBase.setLength(connectoid.getLengthKm(zone).get());
    }    
    
    /* allowed modes for zone */
    if(connectoid.hasAllowedModes(zone)) {
      Collection<Mode> allowedModesForZone = connectoid.getAllowedModes(zone);
      String csvModeIdString = 
          allowedModesForZone.stream().map( mode -> getModeIdMapper().apply(mode)).collect(Collectors.joining(String.valueOf(getSettings().getCommaSeparator())));
      xmlConnectoidBase.setModes(csvModeIdString);  
    }
        
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
    
    /* connectoids */
    XMLElementConnectoids xmlConnectoids = new XMLElementConnectoids();
    xmlOdZone.setConnectoids(xmlConnectoids);
    for(Connectoid connectoid : zoneToConnectoidMap.get(odZone)) {
      /* od zones, only pertain to undirected connectoids at this point in time since they allow access froma ll incoming link(segment)s */
      if(connectoid instanceof UndirectedConnectoid) {
        
        /* populate extension pertaining to od connectoid */
        Odconnectoid xmlOdConnectoid = new Odconnectoid();               
        populateXmlOdConnectoid(xmlOdConnectoid, (UndirectedConnectoid)connectoid);
        
        /* populate base pertaining to any connectoid*/
        XMLElementConnectoid xmlOdConnectoidBase = new XMLElementConnectoid(xmlOdConnectoid);
        populateXmlConnectoidBase(xmlOdConnectoid, connectoid, odZone);
        
        /* register */
        xmlConnectoids.getConnectoid().add(xmlOdConnectoidBase);        
      }
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
   */  
  private void populateXmlIntermodal(Zoning zoning) {
    // TODO Auto-generated method stub
    
  }  
  
  /** default network file name to use */
  public static final String DEFAULT_ZONING_FILE_NAME = "zoning.xml";  
  
  
  /** Constructor 
   * @param networkPath to persist network on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   * @param xmlRawNetwork to populate with PLANit network when persisting
   */
  public PlanitZoningWriter(String zoningPath, String countryName, CoordinateReferenceSystem zoningCrs, XMLElementMacroscopicZoning xmlRawZoning) {
    super(IdMapperType.DEFAULT, zoningPath, DEFAULT_ZONING_FILE_NAME, countryName);
    this.sourceCrs = zoningCrs;    
    this.xmlRawZoning = xmlRawZoning;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(Zoning zoning) throws PlanItException {

    /* initialise */
    super.initialiseIdMappingFunctions();    
    super.prepareCoordinateReferenceSystem(sourceCrs);    
    super.logSettings();
    
    createZoneToConnectoidIndices(zoning);
    
    /* Od zones */
    populateXmlOdZones(zoning);
    
    /* intermodal zones */
    populateXmlIntermodal(zoning);
    
    /* persist */
    super.persist(xmlRawZoning, XMLElementMacroscopicZoning.class, PlanitSchema.MACROSCOPIC_ZONING_XSD);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void reset() {
    zoneToConnectoidMap.clear();    
  }
  
}
