package org.goplanit.io.xml.util;

import org.goplanit.xml.utils.XsdLatestVersion;

import java.util.regex.Pattern;

/**
 * Helper class to deal with XSD schema related locations for the various XML components
 * 
 * @author markr
 *
 */
public class PlanitSchema {
  
  private static final String XSD_DIR= "/xsd/";
  
  /** verify if the current version is a release version or not. A version is considered a release version when
   * it has only numbers in it and nothing else (no a, alpha, beta, or other strings)
   * 
   * @param version to check
   * @return true when release version, false otherwise
   */
  private static final boolean isReleaseVersion(String version) {
    /* when no matches, it is assumed to be a valid release */
    return Pattern.matches("^[a-zA-Z]*$",version);
  }
  
  /** return the version as uri used by PLANit where all '.' are repplaced by '_'
   * 
   * @return converted version
   */
  private static final String getReleaseVersionAsUriString() {
    return ApplicationProperties.getVersion().replace('.', '_');
  }

  /** latest known version from PLANitXml */
  public static final String LATEST_SCHEMA_VERSION = XsdLatestVersion.VERSION;

  /** schema for demand */
  public static final String MACROSCOPIC_NETWORK_XSD= "macroscopicnetworkinput.xsd";

  /** schema for demand */
  public static final String MACROSCOPIC_DEMAND_XSD= "macroscopicdemandinput.xsd";

  /** schema for zoning */
  public static final String MACROSCOPIC_ZONING_XSD= "macroscopiczoninginput.xsd";

  /** schema for service network */
  public static final String SERVICE_NETWORK_XSD= "servicenetworkinput.xsd";

  /** schema for routed services */
  public static final String ROUTED_SERVICES_XSD= "routedservicesinput.xsd";

  /** metadata schema XSD name*/
  public static final String METADATA_XSD= "metadata.xsd";

  /** schema for combined input XSD */
  public static final String MACROSCOPIC_COMBINED_INPUT_XSD= "macroscopicinput.xsd";
  
  /** the schema URI location (dir) of XML components during development **/
  public static final String XSD_SCHEMA_URI_DEVELOPMENT_GENERIC = "https://www.goplanit.org/xsd/";
  
  /** base location for any resources (such as schemas) for any release version, but without the actual version or resource appended yet**/
  public static final String RESOURCES_RELEASE_GENERIC = "planitmanual.github.io/version/";
    
  /** the schema URI location of planit macroscopic network input during development **/
  public static final String MACROSCOPIC_NETWORK_XSD_SCHEMA_URI_DEV =
      XSD_SCHEMA_URI_DEVELOPMENT_GENERIC+MACROSCOPIC_NETWORK_XSD;
  
  /** the schema URI location of planit macroscopic zoning input during development **/
  public static final String MACROSCOPIC_ZONING_XSD_SCHEMA_URI_DEV =
      XSD_SCHEMA_URI_DEVELOPMENT_GENERIC+MACROSCOPIC_ZONING_XSD;
  
  /** the schema URI location of planit macroscopic zoning input during development **/
  public static final String MACROSCOPIC_DEMAND_XSD_SCHEMA_URI_DEV =
      XSD_SCHEMA_URI_DEVELOPMENT_GENERIC+MACROSCOPIC_DEMAND_XSD;
  
  /** the schema URI location of any PLANit XSD schema for any release version, but without the actual version
   * or schema appended yet**/
  public static final String XSD_SCHEMA_URI_RELEASE_GENERIC = "https://planitmanual.github.io/version/";

  /**
   * Dummy constructor as never instantiated
   */
  private PlanitSchema() {
    // compliance to avoid javadoc warnings
  }
  
  /** create the appropriate URI reference for the xsd schema assuming it is published according to PLANit guidelines
   * meaning that when this is a development version it resides under {@code XSD_SCHEMA_URI_DEVELOPMENT_GENERIC}
   * whereas if this is a release version the {@code RESOURCES_RELEASE_GENERIC} is supplemented with the correct
   * release version and appended with the xsd schema location and schema version
   * 
   * @param xsdFileName to create URI reference for
   * @param schemaVersion  to use
   * @return PLANit compatible URI reference
   */
  public static String createPlanitSchemaUri(String xsdFileName, String schemaVersion) {
    boolean releaseVersion = isReleaseVersion(ApplicationProperties.getVersion());
    if(releaseVersion) {
      return RESOURCES_RELEASE_GENERIC + getReleaseVersionAsUriString() + XSD_DIR + xsdFileName;
    }else {
      return XSD_SCHEMA_URI_DEVELOPMENT_GENERIC + schemaVersion + "/" + xsdFileName;
    }
  }

  /**
   * Identical to {@link #createPlanitSchemaUri(String, String)} but with latest known version injected
   * @param xsdFileName to use
   * @return uri
   */
  public static String createPlanitSchemaUri(String xsdFileName) {
    return createPlanitSchemaUri(xsdFileName, PlanitSchema.LATEST_SCHEMA_VERSION);
  }
}
