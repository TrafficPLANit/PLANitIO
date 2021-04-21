package org.planit.io.converter.intermodal;

import org.planit.converter.intermodal.IntermodalReader;
import org.planit.io.converter.network.PlanitNetworkReader;
import org.planit.io.converter.network.PlanitNetworkReaderFactory;
import org.planit.io.converter.zoning.PlanitZoningReader;
import org.planit.io.converter.zoning.PlanitZoningReaderFactory;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.Pair;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;
import org.planit.xml.generated.XMLElementMacroscopicZoning;
import org.planit.zoning.Zoning;

/**
 * Planit intermodal reader in native format. Wraps a network and zoning reader in one
 * 
 * @author markr
 *
 */
public class PlanitIntermodalReader implements IntermodalReader {
  
  /** the network reader to use */
  protected final  PlanitNetworkReader networkReader;
  
  /** the zoning to populate */
  protected final Zoning zoningToPopulate;
  
  /** the zoning reader to use */
  protected PlanitZoningReader zoningReader = null;
  
  /** constructor where xml content is still on disk and first needs to be parsed into memory before converted to planit memory model. Network and zoning instance
   * are created internally and returned upon completion
   * 
   * @param inputPathDirectory to use for both network and zoning input file assuming default input file names for both (network.xml, zoning.xml)   * 
   * @param xmlFileExtension to use
   * @param idToken to use for the network and zoning that are to be created
   * @throws PlanItException  thrown if error
   */
  protected PlanitIntermodalReader(String inputPathDirectory, String xmlFileExtension, IdGroupingToken idToken) throws PlanItException{
    InfrastructureNetwork<?,?> network = new MacroscopicNetwork(idToken);
    this.networkReader = PlanitNetworkReaderFactory.create(inputPathDirectory, xmlFileExtension, network);    
    this.zoningToPopulate = new Zoning(idToken, network.getNetworkGroupingTokenId());
  }    
  
  /** constructor where xml content is still on disk and first needs to be parsed into memory before converted to planit memory model
   * 
   * @param inputPathDirectory to use for both network and zoning input file assuming default input file names for both (network.xml, zoning.xml)   * 
   * @param xmlFileExtension to use
   * @param referenceNetwork to populate
   * @param zoning to populate
   * @throws PlanItException  thrown if error
   */
  protected PlanitIntermodalReader(String inputPathDirectory, String xmlFileExtension, InfrastructureNetwork<?,?> referenceNetwork, Zoning zoning) throws PlanItException{   
    this.networkReader = PlanitNetworkReaderFactory.create(inputPathDirectory, xmlFileExtension, referenceNetwork);
    this.zoningReader = PlanitZoningReaderFactory.create(inputPathDirectory, xmlFileExtension, referenceNetwork, zoning);
    this.zoningToPopulate = zoning;
  }    

  /** constructor where the xml content has already been parsed into a JAXB memory model which subsequently needs to be converted into the planit memory model
   * 
   * @param xmlRawNetwork to extract from
   * @param xmlRawZoning to extract from
   * @param network to extract referenced entities from
   * @param zoning to populate
   * @throws PlanItException thrown if error
   */
  public PlanitIntermodalReader(XMLElementMacroscopicNetwork xmlRawNetwork, XMLElementMacroscopicZoning xmlRawZoning, InfrastructureNetwork<?, ?> network, Zoning zoning) throws PlanItException {
    this.networkReader = PlanitNetworkReaderFactory.create(xmlRawNetwork, network);
    this.zoningReader = PlanitZoningReaderFactory.create(xmlRawZoning, network, zoning);
    this.zoningToPopulate = zoning;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pair<InfrastructureNetwork<?, ?>, Zoning> read() throws PlanItException {

    /* network */
    InfrastructureNetwork<?, ?> referenceNetwork = networkReader.read();
        
    /* zoning */   
    if(zoningReader == null) {
      this.zoningReader = PlanitZoningReaderFactory.create(networkReader.getInputPathDirectory(), networkReader.getXmlFileExtension(), referenceNetwork, zoningToPopulate);
    }
    /* adopt xml index mapping from network reader so we do not create a duplicate mapping */
    zoningReader.setLinkSegmentsByXmlId(networkReader.getSettings().getMapToIndexLinkSegmentByXmlIds());
    zoningReader.setNodesByXmlId(networkReader.getSettings().getMapToIndexNodeByXmlIds());
    /* parse */
    Zoning zoning = zoningReader.read();
    
    /* result */
    return Pair.of(referenceNetwork, zoning);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void reset() {
    networkReader.reset();
    zoningReader.reset();
  }

}
