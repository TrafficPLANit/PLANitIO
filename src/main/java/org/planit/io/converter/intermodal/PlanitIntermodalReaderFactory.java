package org.planit.io.converter.intermodal;

import org.planit.network.InfrastructureNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;
import org.planit.xml.generated.XMLElementMacroscopicZoning;
import org.planit.zoning.Zoning;

/**
 * Factory class for creating zoning reader in the native PLANit format
 * 
 * @author markr
 *
 */
public class PlanitIntermodalReaderFactory {
  
  /** factory method based on all defaults. IT is expected that the user will set the necessary settings via the exposed settings
   * @return created reader
   */
  public static PlanitIntermodalReader create() throws PlanItException{
    return create(new PlanitIntermodalReaderSettings());
  }  
  
  /** Factory method absed on passed in network and zoning reader settings
   * @param networkSettings to use
   * @param zoningSettings to use
   * @return created reader
   * @throws PlanItException
   */
  public static PlanitIntermodalReader create(final PlanitIntermodalReaderSettings intermodalSettings) throws PlanItException{
    return new PlanitIntermodalReader(intermodalSettings, IdGroupingToken.collectGlobalToken());
  }

  
  /** factory method
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @throws PlanItException  thrown if error
   */
  public static PlanitIntermodalReader create(String pathDirectory, String xmlFileExtension, InfrastructureNetwork<?,?> network, Zoning zoning) throws PlanItException{   
    return new PlanitIntermodalReader(pathDirectory, xmlFileExtension, network, zoning);
  }
  
  /** constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlRawZoning to extract from
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @throws PlanItException  thrown if error
   */
  public static  PlanitIntermodalReader create(XMLElementMacroscopicNetwork xmlRawNetwork, XMLElementMacroscopicZoning xmlRawZoning, InfrastructureNetwork<?,?> network, Zoning zoning) throws PlanItException{
    return new PlanitIntermodalReader(xmlRawNetwork, xmlRawZoning, network, zoning);
  }
}
