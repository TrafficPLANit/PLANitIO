package org.goplanit.io.converter.network;

import org.goplanit.io.converter.PlanitWriterImpl;
import org.goplanit.utils.mode.Mode;

import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Utilities for mode conversion especially for predefined modes
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
}
