package org.planit.io.converter.intermodal;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.converter.ConverterWriterSettings;
import org.planit.io.converter.network.PlanitNetworkWriterSettings;
import org.planit.io.converter.zoning.PlanitZoningWriterSettings;

/**
 * Settings for Planit intermodal writer
 * 
 * @author markr
 *
 */
public class PlanitIntermodalWriterSettings implements ConverterWriterSettings {

  /** the network settings to use */
  protected final PlanitNetworkWriterSettings networkSettings;
  
  /** the zoning settings to use */
  protected final PlanitZoningWriterSettings zoningSettings;  
  
  /**
   * Default constructor
   */
  public PlanitIntermodalWriterSettings() {
    this(new PlanitNetworkWriterSettings(), new PlanitZoningWriterSettings());
  }
  
  /**
   * Constructor
   * 
   * @param outputDirectory to use
   * @param countryName to use
   */
  public PlanitIntermodalWriterSettings(final String outputDirectory, final String countryName) {
    this(new PlanitNetworkWriterSettings(outputDirectory, countryName), new PlanitZoningWriterSettings(outputDirectory, countryName));
  }      
  
  /**
   * Constructor
   * 
   * @param networkSettings to use
   * @param zoningSettings to use
   */
  public PlanitIntermodalWriterSettings(final PlanitNetworkWriterSettings networkSettings, final PlanitZoningWriterSettings zoningSettings) {
    this.networkSettings = networkSettings;
    this.zoningSettings = zoningSettings;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    getNetworkSettings().reset();
    getZoningSettings().reset();
  }

  /** Collect zoning settings
   * 
   * @return zoning settings
   */
  public PlanitZoningWriterSettings getZoningSettings() {
    return zoningSettings;
  }

  /** Collect network settings
   * 
   * @return network settings
   */
  public  PlanitNetworkWriterSettings getNetworkSettings() {
    return networkSettings;
  } 
    
  /** Set the outputPathDirectory used on both zoning and network settings
   * 
   * @param outputDirectory to use
   */
  public void setOutputDirectory(String outputDirectory) {
    getZoningSettings().setOutputDirectory(outputDirectory);
    getNetworkSettings().setOutputDirectory(outputDirectory);
  }

  /** Set country name used on both zoning and network settings
   * 
   * @param countryName to use
   */
  public void setCountry(String countryName) {
    getZoningSettings().setCountry(countryName);
    getNetworkSettings().setCountry(countryName);
  }
  
  /** Set the destination Crs to use (if not set, network's native Crs will be used, unless the user has specified a
   * specific country for which we have a more appropriate Crs registered) 
   * 
   * @param destinationCoordinateReferenceSystem to use
   */
  public void setDestinationCoordinateReferenceSystem(CoordinateReferenceSystem destinationCoordinateReferenceSystem) {
    getZoningSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
    getNetworkSettings().setDestinationCoordinateReferenceSystem(destinationCoordinateReferenceSystem);
  }  
  
}
