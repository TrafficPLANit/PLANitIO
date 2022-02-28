package org.goplanit.io.converter.intermodal;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.xml.generated.XMLElementMacroscopicNetwork;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
import org.goplanit.zoning.Zoning;

/**
 * Factory class for creating zoning reader in the native PLANit format
 * 
 * @author markr
 *
 */
public class PlanitIntermodalReaderFactory {
  
  /** Factory method based on all defaults. IT is expected that the user will set the necessary settings via the exposed settings
   * 
   * @return created reader
   * @throws PlanItException thrown if error
   */
  public static PlanitIntermodalReader create() throws PlanItException{
    return create(new PlanitIntermodalReaderSettings());
  }  
  
  /** Factory method absed on passed in network and zoning reader settings
   * 
   * @param intermodalSettings to use
   * @return created reader
   * @throws PlanItException thrown if error
   */
  public static PlanitIntermodalReader create(final PlanitIntermodalReaderSettings intermodalSettings) throws PlanItException{
    return new PlanitIntermodalReader(intermodalSettings, IdGroupingToken.collectGlobalToken());
  }

  
  /** Factory method
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @return created reader
   * @throws PlanItException  thrown if error
   */
  public static PlanitIntermodalReader create(String pathDirectory, String xmlFileExtension, MacroscopicNetwork network, Zoning zoning) throws PlanItException{   
    return new PlanitIntermodalReader(pathDirectory, xmlFileExtension, network, zoning);
  }
  
  /** constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlRawNetwork to extract from
   * @param xmlRawZoning to extract from
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @return created reader
   * @throws PlanItException  thrown if error
   */
  public static  PlanitIntermodalReader create(XMLElementMacroscopicNetwork xmlRawNetwork, XMLElementMacroscopicZoning xmlRawZoning, MacroscopicNetwork network, Zoning zoning) throws PlanItException{
    return new PlanitIntermodalReader(xmlRawNetwork, xmlRawZoning, network, zoning);
  }
}