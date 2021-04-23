package org.planit.io.converter.zoning;

import org.planit.network.InfrastructureNetwork;
import org.planit.network.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.xml.generated.XMLElementMacroscopicZoning;
import org.planit.zoning.Zoning;

/**
 * Factory class for creating zoning reader in the native PLANit format
 * 
 * @author markr
 *
 */
public class PlanitZoningReaderFactory {
  
  /** factory method wit all default, expected that user configures the settings afterwards to reach minimum requirements for successful parsing
   *  (input dir especially)
   */
  public static PlanitZoningReader create() throws PlanItException{
    PlanitZoningReaderSettings settings = new PlanitZoningReaderSettings();
    MacroscopicNetwork networkToPopulate = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    Zoning zoningToPopulate = new Zoning(IdGroupingToken.collectGlobalToken(), networkToPopulate.getNetworkGroupingTokenId());
    return create(settings, networkToPopulate, zoningToPopulate);
  }  

  
  /** factory method
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @return created reader
   * @throws PlanItException  thrown if error
   */
  public static PlanitZoningReader create(String pathDirectory, String xmlFileExtension, InfrastructureNetwork<?,?> network, Zoning zoning) throws PlanItException{
    return create(new PlanitZoningReaderSettings(pathDirectory, xmlFileExtension),network, zoning);
  }
  
  /**Factory method
   * 
   * @param zoningSettings to use
   * @param referenceNetwork to use
   * @param zoningToPopulate to use
   * @return created reader
   */
  public static PlanitZoningReader create(PlanitZoningReaderSettings zoningSettings, InfrastructureNetwork<?, ?> referenceNetwork, Zoning zoningToPopulate) {
    return new PlanitZoningReader(zoningSettings, referenceNetwork, zoningToPopulate);
  }  
  
  /** constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlRawZoning to extract from
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @return created reader
   * @throws PlanItException  thrown if error
   */
  public static  PlanitZoningReader create(XMLElementMacroscopicZoning xmlRawZoning, InfrastructureNetwork<?,?> network, Zoning zoning) throws PlanItException{
    return new PlanitZoningReader(xmlRawZoning, network, zoning);
  }


}
