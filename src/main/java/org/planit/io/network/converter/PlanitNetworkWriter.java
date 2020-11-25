package org.planit.io.network.converter;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.network.converter.IdMapper;
import org.planit.network.converter.NetworkWriter;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Modes;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegments;
import org.planit.utils.network.physical.Links;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.Nodes;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentTypes;
import org.planit.xml.generated.XMLElementInfrastructure;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;

/**
 * Writer to persist a PLANit network to disk in the native PLANit format
 * 
 * @author markr
 *
 */
public class PlanitNetworkWriter implements NetworkWriter {
  
  /** network path (file) to persist to */
  private final String networkPath;
  
  /** XML memory model equivalent of the PLANit memory mode */
  private final XMLElementMacroscopicNetwork xmlRawNetwork;
  
  
  /** populate the xml /<infrastructure/> element
   * @param coordinateReferenceSystem to use
   * @param linkSegments 
   * @param links 
   * @param nodes 
   */
  protected void populateInfrastructure(CoordinateReferenceSystem coordinateReferenceSystem, Nodes<Node> nodes, Links<Link> links, LinkSegments<MacroscopicLinkSegment> linkSegments) {
    XMLElementInfrastructure xmlInfrastructure = xmlRawNetwork.getInfrastructure(); 
    if(xmlInfrastructure == null) {
      xmlInfrastructure = new XMLElementInfrastructure();
      xmlRawNetwork.setInfrastructure(xmlInfrastructure );
    }
    
    /* SRS/CRS */
    xmlInfrastructure.setSrsname(coordinateReferenceSystem.getName().getCode());
    
    
  }

  protected void populateLinkConfiguration(Modes modes, MacroscopicLinkSegmentTypes linkSegmentTypes) {
    // TODO Auto-generated method stub
    
  }  
  
  /** Constructor 
   * @param networkPath to persist network on
   * @param network to persist
   */
  public PlanitNetworkWriter(String networkPath, XMLElementMacroscopicNetwork xmlRawNetwork) {
    this.networkPath = networkPath;
    this.xmlRawNetwork = xmlRawNetwork;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(MacroscopicNetwork network) throws PlanItException {
    /* configuration */
    populateLinkConfiguration(network.modes, network.linkSegmentTypes);
    /* infrastructure */
    populateInfrastructure(network.getCoordinateReferenceSystem(), network.nodes, network.links, network.linkSegments);
  }




  /**
   * {@inheritDoc}
   */  
  @Override
  public IdMapper getIdMapper() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void setIdMapper(IdMapper idMapper) {
    // TODO Auto-generated method stub
    
  }

}
