package org.goplanit.io.converter.intermodal;

import org.goplanit.converter.intermodal.IntermodalReader;
import org.goplanit.io.converter.network.PlanitNetworkReader;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.io.converter.zoning.PlanitZoningReader;
import org.goplanit.io.converter.zoning.PlanitZoningReaderFactory;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.xml.generated.XMLElementMacroscopicNetwork;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
import org.goplanit.zoning.Zoning;

/**
 * Planit intermodal reader in native format. Wraps a network and zoning reader in one
 * 
 * @author markr
 *
 */
public class PlanitIntermodalReader implements IntermodalReader {
  
  /** intermodal reader settings to use */
  protected final PlanitIntermodalReaderSettings intermodalReaderSettings;
    
  /** the zoning to populate */
  protected final Zoning zoningToPopulate;
  
  /** the network to populate */
  protected final MacroscopicNetwork networkToPopulate;  
    
  /** constructor where xml content is still on disk and first needs to be parsed into memory before converted to planit memory model. Network and zoning instance
   * are created internally and returned upon completion
   * 
   * @param inputPathDirectory to use for both network and zoning input file assuming default input file names for both (network.xml, zoning.xml)   * 
   * @param xmlFileExtension to use
   * @param idToken to use for the network and zoning that are to be created
   * @throws PlanItException  thrown if error
   */
  protected PlanitIntermodalReader(String inputPathDirectory, String xmlFileExtension, IdGroupingToken idToken) throws PlanItException{        
    this(new PlanitIntermodalReaderSettings(inputPathDirectory, xmlFileExtension), idToken);    
  }   
  
  /** constructor where xml content is still on disk and first needs to be parsed into memory before converted to planit memory model. Network and zoning instance
   * are created internally and returned upon completion
   * 
   * @param settings to use
   * @param idToken to use for the network and zoning that are to be created
   * @throws PlanItException  thrown if error
   */
  protected PlanitIntermodalReader(PlanitIntermodalReaderSettings settings, IdGroupingToken idToken) throws PlanItException{        
    this.intermodalReaderSettings = settings;
    this.networkToPopulate = new MacroscopicNetwork(idToken);    
    this.zoningToPopulate = new Zoning(idToken, networkToPopulate.getNetworkGroupingTokenId());
  }  
  
  /** constructor where xml content is still on disk and first needs to be parsed into memory before converted to planit memory model
   * 
   * @param inputPathDirectory to use for both network and zoning input file assuming default input file names for both (network.xml, zoning.xml)   * 
   * @param xmlFileExtension to use
   * @param network to populate
   * @param zoning to populate
   * @throws PlanItException  thrown if error
   */
  protected PlanitIntermodalReader(String inputPathDirectory, String xmlFileExtension, MacroscopicNetwork network, Zoning zoning) throws PlanItException{   
    this.intermodalReaderSettings = new PlanitIntermodalReaderSettings(inputPathDirectory, xmlFileExtension);
    this.networkToPopulate = network;
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
  public PlanitIntermodalReader(XMLElementMacroscopicNetwork xmlRawNetwork, XMLElementMacroscopicZoning xmlRawZoning, MacroscopicNetwork network, Zoning zoning) throws PlanItException {
    this.intermodalReaderSettings = new PlanitIntermodalReaderSettings();
    this.networkToPopulate = network;
    this.zoningToPopulate = zoning;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pair<MacroscopicNetwork, Zoning> read() throws PlanItException {

    /* network */
    PlanitNetworkReader networkReader = PlanitNetworkReaderFactory.create(getSettings().getNetworkSettings(), networkToPopulate);
    MacroscopicNetwork referenceNetwork = networkReader.read();
        
    /* zoning */   
    PlanitZoningReader zoningReader = PlanitZoningReaderFactory.create(getSettings().getZoningSettings(), referenceNetwork, zoningToPopulate);
    
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
    getSettings().reset();
  }

  /**
   * {@inheritDoc}
   */    
  @Override
  public PlanitIntermodalReaderSettings getSettings() {
    return this.intermodalReaderSettings;
  }

}
