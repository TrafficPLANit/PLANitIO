package org.goplanit.io.converter.zoning;

import org.goplanit.converter.network.NetworkReader;
import org.goplanit.io.xml.util.PlanitXmlReaderSettings;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
import org.goplanit.zoning.Zoning;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Factory class for creating zoning reader in the native PLANit format
 * 
 * @author markr
 *
 */
public class PlanitZoningReaderFactory {

  /** Factory method
   *
   * @param pathDirectory to use
   * @param network to extract references from (if any)
   * @return created PLANit zoning reader
   */
  public static PlanitZoningReader create(
      final String pathDirectory, final LayeredNetwork<?,?> network){
    return create(
        pathDirectory,
        PlanitXmlReaderSettings.DEFAULT_XML_EXTENSION,
        network,
        new Zoning(network.getIdGroupingToken(), network.getNetworkGroupingTokenId()));
  }

  
  /** Factory method
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @return created PLANit zoning reader
   */
  public static PlanitZoningReader create(
      final String pathDirectory, final String xmlFileExtension, final LayeredNetwork<?,?> network, final Zoning zoning){
    return create(new PlanitZoningReaderSettings(pathDirectory, xmlFileExtension), network, zoning);
  }

  /** Factory method
   *
   * @param zoningSettings to use
   * @param referenceNetwork to use
   * @return created PLANit zoning reader
   */
  public static PlanitZoningReader create(
      final PlanitZoningReaderSettings zoningSettings, final LayeredNetwork<?, ?> referenceNetwork) {
    return new PlanitZoningReader(
        zoningSettings, referenceNetwork, new Zoning(referenceNetwork.getIdGroupingToken(), referenceNetwork.getNetworkGroupingTokenId()));
  }
  
  /** Factory method
   * 
   * @param zoningSettings to use
   * @param referenceNetwork to use
   * @param zoningToPopulate to use
   * @return created PLANit zoning reader
   */
  public static PlanitZoningReader create(
      final PlanitZoningReaderSettings zoningSettings, final LayeredNetwork<?, ?> referenceNetwork, final Zoning zoningToPopulate) {
    return new PlanitZoningReader(zoningSettings, referenceNetwork, zoningToPopulate);
  }

  /** Factory method where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlRawZoning to extract from
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @return created PLANit zoning reader
   */
  public static  PlanitZoningReader create(
      final XMLElementMacroscopicZoning xmlRawZoning, final LayeredNetwork<?,?> network, final Zoning zoning){
    return new PlanitZoningReader(xmlRawZoning, network, zoning);
  }

  /** Factory method where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   *
   * @param xmlRawZoning to extract from
   * @param zoningSettings to use
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @return created PLANit zoning reader
   */
  public static  PlanitZoningReader create(
      final XMLElementMacroscopicZoning xmlRawZoning, final PlanitZoningReaderSettings zoningSettings, final LayeredNetwork<?,?> network, final Zoning zoning){
    return new PlanitZoningReader(xmlRawZoning, zoningSettings, network, zoning);
  }

  /** Factory method where all contextual information is to be set afterwards via settings and network is to be obtained
   * from provided reader.
   *
   * @param networkReader to extract reference network from
   * @return created PLANit zoning reader
   */
  public static  PlanitZoningReader create(final NetworkReader networkReader){
    return create(new PlanitZoningReaderSettings(), networkReader);
  }

  /** Factory method where all contextual information is derived from settings and network is to be obtained
   * from provided reader.
   *
   * @param settings to use
   * @param networkReader to extract reference network from
   * @return created PLANit zoning reader
   */
  public static  PlanitZoningReader create(final PlanitZoningReaderSettings settings, final NetworkReader networkReader){
    return new PlanitZoningReader(settings, networkReader);
  }


}
