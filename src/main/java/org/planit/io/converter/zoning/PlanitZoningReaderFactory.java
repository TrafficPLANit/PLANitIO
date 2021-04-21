package org.planit.io.converter.zoning;

import org.planit.network.InfrastructureNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.xml.generated.XMLElementMacroscopicZoning;
import org.planit.zoning.Zoning;

/**
 * Factory class for creating zoning reader in the native PLANit format
 * 
 * @author markr
 *
 */
public class PlanitZoningReaderFactory {

  
  /** factory method
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @throws PlanItException  thrown if error
   */
  public static PlanitZoningReader create(String pathDirectory, String xmlFileExtension, InfrastructureNetwork<?,?> network, Zoning zoning) throws PlanItException{   
    return new PlanitZoningReader(pathDirectory, xmlFileExtension, network, zoning);
  }
  
  /** constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlRawZoning to extract from
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @throws PlanItException  thrown if error
   */
  public static  PlanitZoningReader create(XMLElementMacroscopicZoning xmlRawZoning, InfrastructureNetwork<?,?> network, Zoning zoning) throws PlanItException{
    return new PlanitZoningReader(xmlRawZoning, network, zoning);
  }
}
