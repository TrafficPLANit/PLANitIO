package org.goplanit.io.converter.zoning;

import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Factory for creating PLANit Zoning Writers
 * 
 * @author markr
 *
 */
public class PlanitZoningWriterFactory {

  /** Create a PLANitZoningWriter which can persist a PLANit zoning in the native PLANit XML format. User is expected to
   * provide the required inputs via settings and writer after creation as they are not pre-populated.
   *
   * @return created zoning writer
   */
  public static PlanitZoningWriter create() {
    return create(new PlanitZoningWriterSettings());
  }
  
  /** Create a PLANitZoningWriter which can persist a PLANit zoning in the native PLANit XML format
   * 
   * @param zoningPath the file to use for persisting
   * @param countryName the country to base the projection method on if available
   * @return created zoning writer 
   */
  public static PlanitZoningWriter create(final String zoningPath, final String countryName) {
    return create(zoningPath, countryName, new XMLElementMacroscopicZoning());
  }
  
  /** Create a PLANitZoningWriter which can persist a PLANit zoning in the native PLANit XML format. By providing the XML memory model instance to populate
   * we make it possible for the writer to embed the persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   * 
   * @param zoningPath the file to use for persisting
   * @param countryName the country to base the projection method on if available
   * @param xmlRawZoning, use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @return created zoning writer 
   */
  public static PlanitZoningWriter create(
      final String zoningPath, final String countryName, final XMLElementMacroscopicZoning xmlRawZoning) {
    return new PlanitZoningWriter(zoningPath, countryName, xmlRawZoning);
  }

  /** Create a PLANitZoningWriter which can persist a PLANit zoning in the native PLANit XML format. By providing the XML memory model instance to populate
   * we make it possible for the writer to embed the persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   *
   * @param settings the settings to use for persisting
   * @return created zoning writer
   */
  public static PlanitZoningWriter create(final PlanitZoningWriterSettings settings) {
    return new PlanitZoningWriter(settings, new XMLElementMacroscopicZoning());
  }


}
