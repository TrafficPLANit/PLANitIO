package org.goplanit.io.converter.demands;

import org.goplanit.demands.discrete.DiscreteDemands;
import org.goplanit.io.converter.zoning.PlanitZoningReader;
import org.goplanit.io.xml.util.PlanitXmlReaderSettings;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.xml.generated.v2.XMLElementDiscreteDemand;
import org.goplanit.zoning.Zoning;

/**
 * Factory class for creating discrete demands reader in the native PLANit format
 * 
 * @author markr
 *
 */
public class PlanitDiscreteDemandsReaderFactory {

  /**
   * Dummy constructor as never instantiated
   */
  private PlanitDiscreteDemandsReaderFactory() {
    // compliance to avoid javadoc warnings
  }

  /** Factory method
   *
   * @param pathDirectory to use
   * @param referenceNetwork to extract references from (if any)
   * @param referenceZoning to extract references from (if any)
   * @return created PlanitDiscreteDemandsReader
   */
  public static PlanitDiscreteDemandsReader create(
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
   * @return created PlanitDiscreteDemandsReader
   */
  public static PlanitDiscreteDemandsReader create(
      final String pathDirectory,
      final String xmlFileExtension,
      final LayeredNetwork<?,?> network,
      final Zoning referenceZoning){
    return create(new PlanitDiscreteDemandsReaderSettings(pathDirectory, xmlFileExtension), network, referenceZoning);
  }

  /** Factory method
   *
   * @param demandsSettings to use
   * @param referenceNetwork to use
   * @param referenceZoning to use
   * @return created PlanitDiscreteDemandsReader
   */
  public static PlanitDiscreteDemandsReader create(
      final PlanitDiscreteDemandsReaderSettings demandsSettings,
      final LayeredNetwork<?, ?> referenceNetwork,
      final Zoning referenceZoning) {
    return new PlanitDiscreteDemandsReader(
        demandsSettings,
        referenceNetwork,
        referenceZoning,
        new DiscreteDemands(referenceZoning.getIdGroupingToken()));
  }

  /** Factory method where file has already been parsed and we only need to convert from raw XML objects to
   * PLANit memory model
   * 
   * @param xmlRawDemands to extract from
   * @param referenceNetwork to use
   * @param referenceZoning to use
   * @return created PlanitDiscreteDemandsReader
   */
  public static  PlanitDiscreteDemandsReader create(
      final XMLElementDiscreteDemand xmlRawDemands,
      final LayeredNetwork<?,?> referenceNetwork,
      final Zoning referenceZoning){
    return new PlanitDiscreteDemandsReader(
        xmlRawDemands,
        referenceNetwork,
        referenceZoning,
        new DiscreteDemands(referenceZoning.getIdGroupingToken()));
  }

  /** Factory method where all contextual information is to be set afterwards via settings and zoning is to be obtained
   * from provided reader.
   *
   * @param zoningReader to extract reference zoning from
   * @return created PlanitDiscreteDemandsReader
   */
  public static  PlanitDiscreteDemandsReader create(final PlanitZoningReader zoningReader){
    return create(new PlanitDiscreteDemandsReaderSettings(), zoningReader);
  }

  /** Factory method where all contextual information is derived from settings and zoning is to be obtained
   * from provided reader.
   *
   * @param settings to use
   * @param zoningReader to extract reference zoning from
   * @return created PlanitDiscreteDemandsReader
   */
  public static  PlanitDiscreteDemandsReader create(
          final PlanitDiscreteDemandsReaderSettings settings, final PlanitZoningReader zoningReader){
    return new PlanitDiscreteDemandsReader(settings, zoningReader);
  }


}
