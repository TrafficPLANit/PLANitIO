package org.planit.io.converter.zoning;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.xml.generated.XMLElementMacroscopicZoning;

/**
 * Factory for creating PLANit Zoning Writers
 * 
 * @author markr
 *
 */
public class PlanitZoningWriterFactory {
  
  /** Create a PLANitZonginWriter which can persist a PLANit zoning in the native PLANit XML format
   * 
   * @param zoningPath the file to use for persisting
   * @param countryName the country to base the projection method on if available
   * @param zoningCrs crs used by the zoning
   * @return created zoning writer 
   */
  public static PlanitZoningWriter create(String zoningPath, String countryName, CoordinateReferenceSystem zoningCrs) {
    return create(zoningPath, countryName, zoningCrs, new XMLElementMacroscopicZoning());    
  }
  
  /** Create a PLANitZoningWriter which can persist a PLANit zoning in the native PLANit XML format. By providing the XML memory model instance to populate
   * we make it possible for the writer to embed the persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   * 
   * @param zoningPath the file to use for persisting
   * @param countryName the country to base the projection method on if available
   * @param zoningCrs crs used by the zoning
   * @param xmlRawZoning, use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @return created network writer 
   */
  public static PlanitZoningWriter create(String zoningPath, String countryName, CoordinateReferenceSystem zoningCrs, XMLElementMacroscopicZoning xmlRawZoning) {
    return new PlanitZoningWriter(zoningPath, countryName, zoningCrs, xmlRawZoning);    
  }    
     
     
}
