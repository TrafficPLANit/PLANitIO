package org.goplanit.io.converter.intermodal;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.xml.generated.v2.XMLElementMacroscopicNetwork;
import org.goplanit.xml.generated.v2.XMLElementMacroscopicZoning;
import org.goplanit.xml.generated.v2.XMLElementRoutedServices;
import org.goplanit.xml.generated.v2.XMLElementServiceNetwork;
import org.goplanit.zoning.Zoning;

/**
 * Factory class for creating intermodal reader in the native PLANit format
 * 
 * @author markr
 *
 */
public class PlanitIntermodalReaderFactory {

  /**
   * Dummy constructor as never instantiated
   */
  private PlanitIntermodalReaderFactory() {
    // compliance to avoid javadoc warnings
  }
  
  /** Factory method based on all defaults. IT is expected that the user will set the necessary settings via the exposed settings
   * 
   * @return created reader
   */
  public static PlanitIntermodalReader create(){
    return create(IdGroupingToken.collectGlobalToken());
  }

  /** Factory method based on defaults with custom set path directory. IT is expected that the user will set the necessary settings via the exposed settings
   *
   * @param pathDirectory to use
   * @return created reader
   */
  public static PlanitIntermodalReader create(String pathDirectory){
    var reader = create();
    reader.getSettings().setInputDirectory(pathDirectory);
    return reader;
  }

  /** Factory method based on all defaults. It is expected that the user will set the necessary settings via the exposed settings
   *
   * @param idGroupingToken  to use for creating ids
   * @return created reader
   */
  public static PlanitIntermodalReader create(IdGroupingToken idGroupingToken){
    return create(idGroupingToken, new PlanitIntermodalReaderSettings());
  }


  /** Factory method based on passed in network and zoning reader settings
   * 
   * @param intermodalSettings to use
   * @return created reader
   */
  public static PlanitIntermodalReader create(final PlanitIntermodalReaderSettings intermodalSettings){
    return create(IdGroupingToken.collectGlobalToken(), intermodalSettings);
  }

  /** Factory method based on settings. It is expected that the user will set the necessary settings via the exposed settings.
   *
   * @param idGroupingToken  to use for creating ids
   * @param intermodalSettings settings to use
   * @return created reader
   */
  public static PlanitIntermodalReader create(
      IdGroupingToken idGroupingToken, final PlanitIntermodalReaderSettings intermodalSettings){
    return new PlanitIntermodalReader(intermodalSettings, idGroupingToken);
  }

  /** Factory method for intermodal reader( without services)
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @return created reader
   */
  public static PlanitIntermodalReader create(
      String pathDirectory, String xmlFileExtension, MacroscopicNetwork network, Zoning zoning){
    return create(pathDirectory, xmlFileExtension, network, zoning, null, null);
  }

  /** Factory method
   *
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @param serviceNetwork to populate
   * @param routedServices to populate
   * @return created reader
   */
  public static PlanitIntermodalReader create(
      String pathDirectory, String xmlFileExtension, MacroscopicNetwork network, Zoning zoning, ServiceNetwork serviceNetwork, RoutedServices routedServices){
    return new PlanitIntermodalReader(pathDirectory, xmlFileExtension, network, zoning, serviceNetwork, routedServices);
  }
  
  /** constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlRawNetwork to extract from
   * @param xmlRawZoning to extract from
   * @param xmlRawServiceNetwork to extract from
   * @param xmlRawRoutedServices to extract from
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @param serviceNetwork to populate
   * @param routedServices to populate
   * @return created reader
   */
  public static  PlanitIntermodalReader create(
      XMLElementMacroscopicNetwork xmlRawNetwork,
      XMLElementMacroscopicZoning xmlRawZoning,
      XMLElementServiceNetwork xmlRawServiceNetwork,
      XMLElementRoutedServices xmlRawRoutedServices,
      MacroscopicNetwork network,
      Zoning zoning,
      ServiceNetwork serviceNetwork,
      RoutedServices routedServices){
    return new PlanitIntermodalReader(
        new PlanitIntermodalReaderSettings(),
        xmlRawNetwork,
        xmlRawZoning,
        xmlRawServiceNetwork,
        xmlRawRoutedServices,
        network,
        zoning,
        serviceNetwork,
        routedServices);
  }

}
