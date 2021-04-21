package org.planit.io.converter.intermodal;

import org.planit.xml.generated.XMLElementMacroscopicNetwork;
import org.planit.xml.generated.XMLElementMacroscopicZoning;

/**
 * Factory for creating PLANit intermodal writers persisting both a network and zoning (useful for intermodal networks with pt element where transfer zones are part of the
 * zoning.
 * 
 * @author markr
 *
 */
public class PlanitIntermodalWriterFactory {
  
  /** Create a PLANitIntermodalWriter which can persist a PLANit network and zoning in the native PLANit XML format. No destination country is provided, so we assume the current
   * Crs for persisting
   * 
   * @param outputDirectory the path to use for persisting
   * @param countryName the country to base the projection method on if available
   * @return created network writer 
   */
  public static PlanitIntermodalWriter create(String outputDirectory) {
    return create(outputDirectory, null);    
  }  
  
  /** Create a PLANitIntermodalWriter which can persist a PLANit network and zoning in the native PLANit XML format
   * 
   * @param outputDirectory the path to use for persisting
   * @param countryName the country to base the projection method on if available
   * @return created network writer 
   */
  public static PlanitIntermodalWriter create(String outputDirectory, String countryName) {
    return create(outputDirectory, countryName, new XMLElementMacroscopicNetwork(), new XMLElementMacroscopicZoning());    
  }
  
  /** Create a PLANitIntermodalWriter which can persist a PLANit network and zoning in the native PLANit XML format. No destination country is provided, so we assume the current
   * Crs for persisting
   * 
   * @param outputDirectory the file to use for persisting
   * @param xmlRawNetwork, use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @param xmlRawZoning, use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @return created network writer 
   */
  public static PlanitIntermodalWriter create(String outputDirectory, XMLElementMacroscopicNetwork xmlRawNetwork, XMLElementMacroscopicZoning xmlRawZoning) {
    return create(outputDirectory, null, xmlRawNetwork, xmlRawZoning);    
  }  
  
  /** Create a PLANitNetworkWriter which can persist a PLANit network in the native PLANit XML format. By providing the XML memory model instance to populate
   * we make it possible for the writer to embed the persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   * 
   * @param outputDirectory the file to use for persisting
   * @param countryName the country to base the projection method on if available
   * @param xmlRawNetwork, use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @param xmlRawZoning, use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @return created network writer 
   */
  public static PlanitIntermodalWriter create(String outputDirectory, String countryName, XMLElementMacroscopicNetwork xmlRawNetwork, XMLElementMacroscopicZoning xmlRawZoning) {
    return new PlanitIntermodalWriter(outputDirectory, countryName, xmlRawNetwork, xmlRawZoning);    
  }    
     
     
}
