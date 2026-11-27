package org.goplanit.io.converter.network;

import org.goplanit.utils.mode.Mode;

import java.util.function.Function;
import java.util.logging.Logger;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;

/**
 * Utilities for mode conversion especially for predefined modes
 * <p>
 * The PLANit XML refers to modes by their XML ids, predefined modes by their predefined names, on links, toll roads and
 * pricing schemes. Writing a mode reference, and reading the modes that pay, go through here, so the readers and
 * writers of the network and the pricing treat them alike.
 * </p>
 *
 * @author markr
 */
public final class ModeXmlUtils {

  private static final Logger LOGGER = Logger.getLogger(ModeXmlUtils.class.getCanonicalName());

  /** dummy */
  private ModeXmlUtils(){}

  /** Get the reference to use whenever a mode reference is encountered. Special treatment for predefined modes
   * so whenever we use mode ids either in parsing or writing it should go through this logic.
   *
   * @param mode to collect reference for
   * @param modeIdMapper to use
   * @return modeReference for the mode
   */
  public static String getXmlModeReference(Mode mode, Function<Mode, String> modeIdMapper) {
    String modeReference = null;

    if(mode.isPredefinedModeType()) {
      /* predefined modes, must utilise, their predefined XML id/name, this overrules the mapper (if any) */
      modeReference = mode.getXmlId();
    }else {
      modeReference =modeIdMapper.apply(mode);
    }

    if(modeReference == null) {
      LOGGER.severe(String.format("Mode reference could not be obtained for mode (%s)", mode.getIdsAsString()));
    }

    return modeReference;
  }

  /**
   * Find the modes that pay as given: by a comma-separated list of mode source ids naming them, such as a toll road's
   * {@code pricedmoderefs}, by one exempting them from those paying by default, or by neither, leaving the default.
   * Each problem is one warning starting with the subject: giving both lists discards the subject, an unknown mode is
   * skipped, and naming no known mode discards the subject, as reading it with the default modes would change who pays
   *
   * @param subject        the entity the modes are of, as the warnings name it, such as "Toll road m4"
   * @param pricedModeRefs the modes named, null or blank when not given
   * @param exemptModeRefs the modes exempted, null or blank when not given
   * @param findModes      finds the modes a comma-separated list of mode source ids refers to, telling the consumer
   *                       each id that is unknown: the reader's own lookup
   * @param logger         to warn on, the reader's own
   * @return the modes as given, null when the subject is not to be read
   */
  public static PricedModeSelection findPricedModeSelection(
      String subject,
      String pricedModeRefs,
      String exemptModeRefs,
      BiFunction<String, Consumer<String>, List<Mode>> findModes,
      Logger logger) {
    boolean named = !StringUtils.isNullOrBlank(pricedModeRefs);
    if (named && !StringUtils.isNullOrBlank(exemptModeRefs)) {
      logger.warning(String.format("DISCARD: %s gives both priced and priced exempt modes", subject));
      return null;
    }
    var modes = findModes.apply(named ? pricedModeRefs : exemptModeRefs,
        modeRef -> logger.warning(String.format("IGNORE: %s lists unknown mode %s", subject, modeRef)));
    if (named && modes.isEmpty()) {
      logger.warning(String.format("DISCARD: %s names no known priced mode", subject));
      return null;
    }
    if (modes.isEmpty()) {
      return PricedModeSelection.unspecified();
    }
    return named ? PricedModeSelection.named(modes) : PricedModeSelection.exempting(modes);
  }
}
