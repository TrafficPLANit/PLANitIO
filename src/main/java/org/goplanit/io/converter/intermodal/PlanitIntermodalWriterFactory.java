package org.goplanit.io.converter.intermodal;

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
   * We assume the user sets the output directory and destination country afterwards
   * 
   * @return created Planit native format network writer 
   */
  public static PlanitIntermodalWriter create() {
    return create(null);    
  }    
  
  /** Create a PLANitIntermodalWriter which can persist a PLANit network and zoning in the native PLANit XML format. No destination country is provided, so we assume the current
   * Crs for persisting
   * 
   * @param outputDirectory the path to use for persisting
   * @return created network writer 
   */
  public static PlanitIntermodalWriter create(String outputDirectory) {
    return create(outputDirectory, CountryNames.GLOBAL);    
  }  
  
  /** Create a PLANitIntermodalWriter which can persist a PLANit network and zoning in the native PLANit XML format
   * 
   * @param outputDirectory the path to use for persisting
   * @param countryName the country to base the projection method on if available
   * @return created network writer 
   */
  public static PlanitIntermodalWriter create(String outputDirectory, String countryName) {
    return create(
            outputDirectory,
            countryName,
            new XMLElementMacroscopicNetwork(),
            new XMLElementMacroscopicZoning(),
            new XMLElementServiceNetwork(),
            new XMLElementRoutedServices());
  }
  
  /** Create a PLANitIntermodalWriter which can persist a PLANit network and zoning in the native PLANit XML format. No destination country is provided, so we assume the current
   * Crs for persisting
   * 
   * @param outputDirectory the file to use for persisting
   * @param xmlRawNetwork use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @param xmlRawZoning use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @param xmlRawServiceNetwork, use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @param xmlRawRoutedServices use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @return created network writer 
   */
  public static PlanitIntermodalWriter create(
          String outputDirectory,
          XMLElementMacroscopicNetwork xmlRawNetwork,
          XMLElementMacroscopicZoning xmlRawZoning,
          XMLElementServiceNetwork xmlRawServiceNetwork,
          XMLElementRoutedServices xmlRawRoutedServices) {
    return create(outputDirectory, null, xmlRawNetwork, xmlRawZoning, xmlRawServiceNetwork, xmlRawRoutedServices);
  }  
  
  /** Create a PLANitNetworkWriter which can persist a PLANit network in the native PLANit XML format. By providing the XML memory model instance to populate
   * we make it possible for the writer to embed the persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   * 
   * @param outputDirectory the file to use for persisting
   * @param countryName the country to base the projection method on if available
   * @param xmlRawNetwork use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @param xmlRawZoning use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @param xmlRawServiceNetwork use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @param xmlRawRoutedServices use this specific xml memory model equivalent in this instance before marshalling via JAXb
   * @return created network writer 
   */
  public static PlanitIntermodalWriter create(
          String outputDirectory,
          String countryName,
          XMLElementMacroscopicNetwork xmlRawNetwork,
          XMLElementMacroscopicZoning xmlRawZoning,
          XMLElementServiceNetwork xmlRawServiceNetwork,
          XMLElementRoutedServices xmlRawRoutedServices) {
    return new PlanitIntermodalWriter(outputDirectory, countryName, xmlRawNetwork, xmlRawZoning, xmlRawServiceNetwork, xmlRawRoutedServices);
  }    
     
     
}
