package org.goplanit.io.converter.zoning;

import org.goplanit.network.LayeredNetwork;
import org.goplanit.xml.generated.v2.XMLElementMacroscopicZoning;

/**
 * Factory for creating PLANit Zoning Writers
 * 
 * @author markr
 *
 */
public class PlanitZoningWriterFactory {

  /**
   * Dummy constructor as never instantiated
   */
  private PlanitZoningWriterFactory() {
    // compliance to avoid javadoc warnings
  }

  /** Create a PLANitZoningWriter which can persist a PLANit zoning in the native PLANit XML format.
   * User is expected to provide the required inputs via settings and writer after creation as they
   * are not pre-populated.
   *
   * @param network network the zoning connects to for its connectoids
   * @return created zoning writer
   */
  public static PlanitZoningWriter create(
          final LayeredNetwork<?,?> network) {
    return create(null, null, network);
  }

  /** Create a PLANitZoningWriter which can persist a PLANit zoning in the native PLANit XML format. No country
   * provided, so destination Crs may need to be set explicitly.
   *
   * @param zoningPath the file to use for persisting
   * @param network network the zoning connects to for its connectoids
   * @return created zoning writer
   */
  public static PlanitZoningWriter create(final String zoningPath, final LayeredNetwork<?,?> network) {
    return create(zoningPath, null, network);
  }
  
  /** Create a PLANitZoningWriter which can persist a PLANit zoning in the native PLANit XML format
   * 
   * @param zoningPath the file to use for persisting
   * @param countryName the country to base the projection method on if available
   * @param network network the zoning connects to for its connectoids
   * @return created zoning writer 
   */
  public static PlanitZoningWriter create(
      final String zoningPath, final String countryName, final LayeredNetwork<?,?> network) {
    return create(zoningPath, countryName, new XMLElementMacroscopicZoning(), network);
  }
  
  /** Create a PLANitZoningWriter which can persist a PLANit zoning in the native PLANit XML format.
   * By providing the XML memory model instance to populate we make it possible for the writer to embed the
   * persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   * 
   * @param zoningPath the file to use for persisting
   * @param countryName the country to base the projection method on if available
   * @param xmlRawZoning, use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @param network to use
   * @return created zoning writer 
   */
  public static PlanitZoningWriter create(
      final String zoningPath,
      final String countryName,
      final XMLElementMacroscopicZoning xmlRawZoning,
      final LayeredNetwork<?,?> network) {
    return new PlanitZoningWriter(zoningPath, countryName, xmlRawZoning, network);
  }

  /** Create a PLANitZoningWriter which can persist a PLANit zoning in the native PLANit XML format.
   * By providing the XML memory model instance to populate we make it possible for the writer to embed the
   * persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   *
   * @param settings the settings to use for persisting
   * @param network network the zoning connects to for its connectoids
   * @return created zoning writer
   */
  public static PlanitZoningWriter create(
          final PlanitZoningWriterSettings settings, final LayeredNetwork<?,?> network) {
    return new PlanitZoningWriter(settings, new XMLElementMacroscopicZoning(), network);
  }


}
