package org.goplanit.io.converter.demands;

import org.goplanit.converter.network.NetworkReader;
import org.goplanit.converter.zoning.ZoningReader;
import org.goplanit.demands.Demands;
import org.goplanit.io.converter.zoning.PlanitZoningReader;
import org.goplanit.io.converter.zoning.PlanitZoningReaderSettings;
import org.goplanit.io.xml.util.PlanitXmlReaderSettings;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.xml.generated.XMLElementMacroscopicDemand;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
import org.goplanit.zoning.Zoning;

/**
 * Factory class for creating demands reader in the native PLANit format
 * 
 * @author markr
 *
 */
public class PlanitDemandsReaderFactory {

  /** Factory method
   *
   * @param pathDirectory to use
   * @param referenceNetwork to extract references from (if any)
   * @param referenceZoning to extract references from (if any)
   * @return created PLANit demands reader
   */
  public static PlanitDemandsReader create(
      final String pathDirectory, final LayeredNetwork<?,?> referenceNetwork, final Zoning referenceZoning){
    return create(
        pathDirectory,
        PlanitXmlReaderSettings.DEFAULT_XML_EXTENSION,
            referenceNetwork,
            referenceZoning);
  }

  
  /** Factory method
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param network to extract references from (if any)
   * @param referenceZoning to extract references from (if any)
   * @return created PLANit demands reader
   */
  public static PlanitDemandsReader create(
      final String pathDirectory, final String xmlFileExtension, final LayeredNetwork<?,?> network, final Zoning referenceZoning){
    return create(new PlanitDemandsReaderSettings(pathDirectory, xmlFileExtension), network, referenceZoning);
  }

  /** Factory method
   *
   * @param demandsSettings to use
   * @param referenceNetwork to use
   * @param referenceZoning to use
   * @return created PLANit demands reader
   */
  public static PlanitDemandsReader create(
      final PlanitDemandsReaderSettings demandsSettings,
      final LayeredNetwork<?, ?> referenceNetwork, final Zoning referenceZoning) {
    return new PlanitDemandsReader(
            demandsSettings, referenceNetwork, referenceZoning, new Demands(referenceZoning.getIdGroupingToken()));
  }

  /** Factory method where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlRawDemands to extract from
   * @param referenceNetwork to use
   * @param referenceZoning to use
   * @return created PLANit demands reader
   */
  public static  PlanitDemandsReader create(
      final XMLElementMacroscopicDemand xmlRawDemands, final LayeredNetwork<?,?> referenceNetwork, final Zoning referenceZoning){
    return new PlanitDemandsReader(
            xmlRawDemands, referenceNetwork, referenceZoning, new Demands(referenceZoning.getIdGroupingToken()));
  }

  /** Factory method where all contextual information is to be set afterwards via settings and zoning is to be obtained
   * from provided reader.
   *
   * @param zoningReader to extract reference zoning from
   * @return created PLANit demands reader
   */
  public static  PlanitDemandsReader create(final PlanitZoningReader zoningReader){
    return create(new PlanitDemandsReaderSettings(), zoningReader);
  }

  /** Factory method where all contextual information is derived from settings and zoning is to be obtained
   * from provided reader.
   *
   * @param settings to use
   * @param zoningReader to extract reference zoning from
   * @return created PLANit demands reader
   */
  public static  PlanitDemandsReader create(final PlanitDemandsReaderSettings settings, final PlanitZoningReader zoningReader){
    return new PlanitDemandsReader(settings, zoningReader);
  }


}
