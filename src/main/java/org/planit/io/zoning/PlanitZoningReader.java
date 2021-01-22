package org.planit.io.zoning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.planit.geo.PlanitJtsUtils;
import org.planit.io.xml.util.PlanitXmlReader;
import org.planit.network.InfrastructureNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.Connectoid;
import org.planit.utils.zoning.Centroid;
import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.TransferZoneType;
import org.planit.utils.zoning.Zone;
import org.planit.utils.zoning.Zones;
import org.planit.xml.generated.Odconnectoid;
import org.planit.xml.generated.Transferzonetype;
import org.planit.xml.generated.XMLElementCentroid;
import org.planit.xml.generated.XMLElementConnectoid;
import org.planit.xml.generated.XMLElementMacroscopicIntermodal;
import org.planit.xml.generated.XMLElementMacroscopicZoning;
import org.planit.xml.generated.XMLElementTransferZone;
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

public class PlanitZoningReader extends PlanitXmlReader<XMLElementMacroscopicZoning>{
  
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
      default:
        LOGGER.warning(String.format("unknown transfer stop type %s found, changed to `unknown`",xmlTransferZone.value()));
        return TransferZoneType.UNKNOWN;
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
  
  
  /**
   * geometric utility class based on network crs 
   */
  private PlanitJtsUtils jtsUtils = null;
  
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
    
    /* centroid */
    Centroid centroid = zone.getCentroid();
    if (xmlCentroid !=null && xmlCentroid.getPoint() != null) {
      List<Double> value = xmlCentroid.getPoint().getPos().getValue();        
      centroid.setPosition(PlanitJtsUtils.createPoint(value.get(0), value.get(1)));
    }
    
    return zone;
  }  
  
  /** parse the transfer zones
   * 
   * @param xmlInterModal to extract them from
   * @return transfer zone access point references map to later be able to connect each transfer zone to the correct access points
   * @throws PlanItException thrown if error
   */
  private Map<TransferZone, String> populateTransferZones(XMLElementMacroscopicIntermodal xmlInterModal) throws PlanItException {
    /* track the references to access points (which have not yet been parsed */
    Map<TransferZone, String> transferZoneAccessPointXmlRefs = new HashMap<TransferZone, String>();
    
    /* no transfer zones */
    if(xmlInterModal.getTransferzones() == null) {
      return transferZoneAccessPointXmlRefs;
    }     
    
    /* transferzone */
    List<XMLElementTransferZone> xmlTransferZones = xmlInterModal.getTransferzones().getTransferzone();
    for(XMLElementTransferZone xmlTransferzone : xmlTransferZones) {
      TransferZone transferZone = parseBaseZone(zoning.transferZones, xmlTransferzone.getId(), xmlTransferzone.getExternalid(), xmlTransferzone.getCentroid());
      
      /* type */
      transferZone.setTransferZoneType(parseTransferZoneType(xmlTransferzone.getType()));
      
      /* tzarefs*/
      /* collect access points but we cannot convert them to connectoids yet, as they have yet to be parsed, so delay and store the refs for now */
      if(xmlTransferzone.getTzarefs() == null) {
        LOGGER.severe(String.format("transfer zone %s has no transfer zone access references, so it cannot be used",transferZone.getXmlId()));
      }else {
        transferZoneAccessPointXmlRefs.put(transferZone,xmlTransferzone.getTzarefs());
      }
      
      /* geometry */
      populateZoneGeometry(transferZone, xmlTransferzone.getPolygon());      
    }
    
    return transferZoneAccessPointXmlRefs;
  }  
  
//  /** parse the access points for the transfer zones
//   * 
//   * @param xmlInterModal to extract them from
//   * @return transfer zones references map to later be able to connect each access point to their correct transfer zones
//   * @throws PlanItException thrown if error
//   */
//  private Map<TransferConnectoid, String> populateTransferZoneAccess(XMLElementMacroscopicIntermodal xmlInterModal) throws PlanItException {
//    return null;
//  }

  /** settings for the zoning reader */
  protected final PlanitZoningReaderSettings settings = new PlanitZoningReaderSettings();
  
  /** the zoning to populate */
  protected Zoning zoning;
  
  /** set the zoning to populate
   * @param zoning to populate
   */
  protected void setZoning(Zoning zoning) {
    this.zoning = zoning;
  }
  
  /**
   * parse the OD zones from Xml element into Planit memory
   * @param nodesByXmlIds nodes indexed by xml id to use
   */
  protected void populateODZones(Map<String, Node> nodesByXmlIds) throws PlanItException{
    /* zone */
    for (final XMLElementZones.Zone xmlZone : getXmlRootElement().getZones().getZone()) {
      OdZone zone = parseBaseZone(zoning.odZones, xmlZone.getId(), xmlZone.getExternalid(), xmlZone.getCentroid());
      
      /* geometry */
      populateZoneGeometry(zone, xmlZone.getPolygon());      
                 
      /* connectoids */
      List<XMLElementConnectoid> xmlConnectoids = xmlZone.getConnectoids().getConnectoid();
      for(XMLElementConnectoid xmlConnectoid : xmlConnectoids) {
        Odconnectoid xmlOdConnectoid = xmlConnectoid.getValue();
        Node node = nodesByXmlIds.get(xmlOdConnectoid.getNoderef());
        Point nodePosition = node.getPosition();
        
        double connectoidLength;
        if (xmlOdConnectoid.getLength() != null) {
          connectoidLength = xmlOdConnectoid.getLength();
          // :TODO - need to create some test cases in which nodes have a GML location
        } else if (nodePosition != null) {
          // if node has a GML Point, get the GML Point from the centroid and calculate the length
          // between them
          connectoidLength = jtsUtils.getDistanceInKilometres(zone.getCentroid().getPosition(), nodePosition);
        } else {
          connectoidLength = org.planit.utils.network.virtual.Connectoid.DEFAULT_LENGTH_KM;
        }
                  
        Connectoid theConnectoid = zoning.connectoids.registerNew(zone, node, connectoidLength);

        /* xml id */
        if(xmlOdConnectoid.getId() != null && xmlOdConnectoid.getId().isBlank()) {
          theConnectoid.setXmlId(xmlOdConnectoid.getId());
        }
        
        /* external id */
        if(xmlOdConnectoid.getExternalid() != null && !xmlOdConnectoid.getExternalid().isBlank()) {
          theConnectoid.setExternalId(xmlOdConnectoid.getExternalid());
        }   
      }             
    }
  }  
  
  /**
   * parse the intermodal zones, i.e., platforms, stops, stations, etc. from Xml element into Planit memory
   */
  protected void populateIntermodal() throws PlanItException{
    if(getXmlRootElement().getMacroscopicintermodal() == null) {
      return;
    }
    
    /* intermodal elements present */
    XMLElementMacroscopicIntermodal xmlInterModal = getXmlRootElement().getMacroscopicintermodal();
    
    /* transferzones */
    Map<TransferZone, String> transferZoneAccessXmlRefsByTransferZone = populateTransferZones(xmlInterModal);
    
    /* transfer zone access connectoids */
    //Map<TransferConnectoid, String> transferZoneXmlRefsByAccess = populateTransferZoneAccess(xmlInterModal);

    /* now connect the transfer zones to their access points and the access points to their transfer zones */
    //connectTransferZonesAndTransferZoneAccess(transferZoneAccessXmlRefsByTransferZone, transferZoneXmlRefsByAccess);
  }  
  
  /** constructor
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param zoning to populate
   * @throws PlanItException  thrown if error
   */
  public PlanitZoningReader(String pathDirectory, String xmlFileExtension, Zoning zoning) throws PlanItException{   
    super(XMLElementMacroscopicZoning.class,pathDirectory, xmlFileExtension);    
    setZoning(zoning);
  }
  
  /** constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlMacroscopicZoning to extract from
   * @param zoning to populate
   * @throws PlanItException  thrown if error
   */
  public PlanitZoningReader(XMLElementMacroscopicZoning xmlMacroscopicZoning, Zoning zoning) throws PlanItException{
    super(xmlMacroscopicZoning);    
    setZoning(zoning);
  }  

  /** read the zoning from disk
   * 
   * @param network this zoning is compatible with
   * @param nodesByXmlIds to identify mapping between zones and network
   * @return zoning parsed
   * @throws PlanItException thrown if error
   */
  public Zoning read(InfrastructureNetwork network, Map<String, Node> nodesByXmlIds) throws PlanItException {  
    // create and register zones, centroids and connectoids
    try {
      
      /* popoulate Xml memory model */
      initialiseAndParseXmlRootElement();
      
      this.jtsUtils = new PlanitJtsUtils(network.getCoordinateReferenceSystem());
      
      /* OD zones */
      populateODZones(nodesByXmlIds);
      
      /* Intermodal/transfer zones, i.e., platforms, stations, etc. */
      populateIntermodal();
      
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
}
