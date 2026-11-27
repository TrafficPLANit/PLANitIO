package org.goplanit.io.converter.pricing;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.pricing.NetworkPricing;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * Factory for creating PLANit network pricing readers
 * <p>
 * A pricing is always read onto a network already in memory, as it refers to the network's link segments, modes and
 * toll roads. The factory makes the reader from a folder, from settings such as those made from a settings file, or
 * onto a pricing a project created already, so each caller gives only what it has.
 * </p>
 *
 * @author markr
 */
public class PlanitNetworkPricingReaderFactory {

  /**
   * Dummy constructor as never instantiated
   */
  private PlanitNetworkPricingReaderFactory() {
    // compliance to avoid javadoc warnings
  }

  /**
   * Create a reader of the network pricing file in the given folder, onto the network given
   *
   * @param inputDirectory to use, the first compatible file in it is read
   * @param parentNetwork the network the pricing refers to
   * @return created reader
   */
  public static PlanitNetworkPricingReader create(final String inputDirectory, MacroscopicNetwork parentNetwork) {
    var reader = create(new PlanitNetworkPricingReaderSettings(), parentNetwork);
    reader.getSettings().setInputDirectory(inputDirectory);
    return reader;
  }

  /**
   * Create a reader with the settings given, such as settings made from a settings file, onto the network given
   *
   * @param settings to use
   * @param parentNetwork the network the pricing refers to
   * @return created reader
   */
  public static PlanitNetworkPricingReader create(
      final PlanitNetworkPricingReaderSettings settings,
      MacroscopicNetwork parentNetwork) {
    return create(settings, new NetworkPricing(IdGroupingToken.collectGlobalToken(), parentNetwork));
  }

  /**
   * Create a reader with the settings given, populating the network pricing given, such as one a project created
   *
   * @param settings to use
   * @param networkPricing to populate, made on the network the file refers to
   * @return created reader
   */
  public static PlanitNetworkPricingReader create(
      final PlanitNetworkPricingReaderSettings settings,
      final NetworkPricing networkPricing) {
    return new PlanitNetworkPricingReader(settings, networkPricing);
  }
}
