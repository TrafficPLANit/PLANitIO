package org.goplanit.io.converter.pricing;

import org.goplanit.xml.generated.v2.XMLElementNetworkPricing;

/**
 * Factory for creating PLANit network pricing writers
 * <p>
 * The factory makes the writer from a folder, or from settings such as those made from a settings file, so each
 * caller gives only what it has. The pricing is written with the ids its network was written with, so the two files
 * are read back together.
 * </p>
 *
 * @author markr
 */
public class PlanitNetworkPricingWriterFactory {

  /**
   * Dummy constructor as never instantiated
   */
  private PlanitNetworkPricingWriterFactory() {
    // compliance to avoid javadoc warnings
  }

  /**
   * Create a writer persisting a network pricing in the native PLANit XML format in the folder given
   *
   * @param outputDirectory to write to
   * @return created writer
   */
  public static PlanitNetworkPricingWriter create(String outputDirectory) {
    return create(new PlanitNetworkPricingWriterSettings(outputDirectory));
  }

  /**
   * Create a writer persisting a network pricing in the native PLANit XML format with the settings given, such as
   * settings made from a settings file
   *
   * @param settings to use
   * @return created writer
   */
  public static PlanitNetworkPricingWriter create(PlanitNetworkPricingWriterSettings settings) {
    return new PlanitNetworkPricingWriter(settings, new XMLElementNetworkPricing());
  }
}
