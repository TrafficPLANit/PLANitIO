package org.goplanit.io.converter.intermodal;

import org.goplanit.io.converter.network.PlanitNetworkReaderSettings;
import org.goplanit.io.converter.network.PlanitNetworkWriterSettings;
import org.goplanit.io.converter.zoning.PlanitZoningWriterSettings;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.xml.generated.XMLElementMacroscopicNetwork;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
import org.goplanit.xml.generated.XMLElementRoutedServices;
import org.goplanit.xml.generated.XMLElementServiceNetwork;

/**
 * Factory for creating PLANit intermodal writers persisting both a network and zoning (useful for intermodal networks with pt element where transfer zones are part of the
 * zoning), or also include services (service network and routed services)
 * 
 * @author markr
 *
 */
public class PlanitIntermodalWriterFactory {
  
  /** Default factory method. Create a PLANitIntermodalWriter which can persist a PLANit network and zoning in the native PLANit XML format. 
   * We assume the user sets the output directory (default now current working dir) and destination country afterwards
   * 
   * @return created writer
   */
  public static PlanitIntermodalWriter create() {
    return create(".");
  }    
  
  /** Create a PLANitIntermodalWriter which can persist a PLANit network and zoning in the native PLANit XML format. No destination country is provided, so we assume the current
   * Crs for persisting
   * 
   * @param outputDirectory the path to use for persisting
   * @return created writer
   */
  public static PlanitIntermodalWriter create(String outputDirectory) {
    return create(outputDirectory, CountryNames.GLOBAL);    
  }  
  
  /** Create a PLANitIntermodalWriter which can persist a PLANit network and zoning in the native PLANit XML format
   * 
   * @param outputDirectory the path to use for persisting
   * @param countryName the country to base the projection method on if available
   * @return created writer
   */
  public static PlanitIntermodalWriter create(String outputDirectory, String countryName) {
    return new PlanitIntermodalWriter(
            outputDirectory,
            countryName,
            new XMLElementMacroscopicNetwork(),
            new XMLElementMacroscopicZoning(),
            new XMLElementServiceNetwork(),
            new XMLElementRoutedServices());
  }

  /** Create a PLANitIntermodalWriter which can persist a PLANit network and zoning in the native PLANit XML format. It is assumed all mandatory settings
   * will be provided (or are provided already) via the settings
   *
   * @param settings to inject
   * @return created writer
   */
  public static PlanitIntermodalWriter create(PlanitIntermodalWriterSettings settings) {
    return new PlanitIntermodalWriter(settings);
  }

}
