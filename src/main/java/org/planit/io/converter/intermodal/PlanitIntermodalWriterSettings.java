package org.planit.io.converter.intermodal;

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
   * constructor
   * 
   * @param outputDirectory to use
   */
  public PlanitIntermodalWriterSettings(final String outputDirectory, final String countryName) {
    this(new PlanitNetworkWriterSettings(outputDirectory, countryName), new PlanitZoningWriterSettings(outputDirectory));
  }      
  
  /**
   * constructor
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
   * @return zoning settings
   */
  public PlanitZoningWriterSettings getZoningSettings() {
    return zoningSettings;
  }

  /** Collect network settings
   * @return network settings
   */
  public  PlanitNetworkWriterSettings getNetworkSettings() {
    return networkSettings;
  } 
  
}
