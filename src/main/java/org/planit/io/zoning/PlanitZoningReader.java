package org.planit.io.zoning;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Point;
import org.planit.geo.PlanitJtsUtils;
import org.planit.io.xml.util.PlanitXmlReader;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.Connectoid;
import org.planit.utils.network.virtual.Zone;
import org.planit.xml.generated.Odconnectoid;
import org.planit.xml.generated.XMLElementConnectoid;
import org.planit.xml.generated.XMLElementMacroscopicZoning;
import org.planit.xml.generated.XMLElementZones;


/**
 * Class to parse zoning from native XML format
 * 
 * @author markr
 *
 */

public class PlanitZoningReader extends PlanitXmlReader<XMLElementMacroscopicZoning>{
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitZoningReader.class.getCanonicalName());
  
  /** settings for the zoning reader */
  protected final PlanitZoningReaderSettings settings = new PlanitZoningReaderSettings();
  
  /** the zoning to populate */
  protected Zoning zoning;
  
  /** set the oning to populate
   * @param zoning to populate
   */
  protected void setZoning(Zoning zoning) {
    this.zoning = zoning;
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
      PlanitJtsUtils jtsUtils = new PlanitJtsUtils(network.getCoordinateReferenceSystem());
      
      /* zone */
      for (final XMLElementZones.Zone xmlZone : getXmlRootElement().getZones().getZone()) {
        Zone zone = zoning.zones.createAndRegisterNewZone();
        settings.getMapToIndexZoneByXmlIds().put(xmlZone.getId(), zone);
        
        /* xml id */
        if(xmlZone.getId() != null && !xmlZone.getId().isBlank()) {
          zone.setXmlId(xmlZone.getId());
        }
        
        /* external id */        
        if(xmlZone.getExternalid() != null && !xmlZone.getExternalid().isBlank()) {
          zone.setExternalId(xmlZone.getExternalid());  
        }                  
      
        
        /* centroid */
        Centroid centroid = zone.getCentroid();
        if (xmlZone.getCentroid().getPoint() != null) {
          List<Double> value = xmlZone.getCentroid().getPoint().getPos().getValue();        
          centroid.setPosition(PlanitJtsUtils.createPoint(value.get(0), value.get(1)));
        }
             
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
            connectoidLength = jtsUtils.getDistanceInKilometres(centroid.getPosition(), nodePosition);
          } else {
            connectoidLength = org.planit.utils.network.virtual.Connectoid.DEFAULT_LENGTH_KM;
          }
                    
          Connectoid theConnectoid = zoning.getVirtualNetwork().connectoids.registerNewConnectoid(centroid, node, connectoidLength);

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
