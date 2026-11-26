package org.goplanit.io.converter;

import org.goplanit.utils.settings.SettingsSchema;

/**
 * The layout of the PLANit reader settings sources: the header {@code planitIoReaderSettings: 1} and every key of a
 * reader at the top level, or, for the intermodal reader, the settings files of the readers it holds
 * <p>
 * For example, a source of a network reader:
 * </p>
 * <pre>
 * planitIoReaderSettings: 1
 * inputSource: planit_osm
 * </pre>
 * <p>
 * The header and the keys are on the settings classes: the header on {@link PlanitXmlReaderSettings}, the input
 * source on the reader base of every converter, and the files of the readers an intermodal reader holds on the
 * getters of its settings. One schema serves every reader, as they all take the same keys.
 * </p>
 *
 * @author markr
 */
public final class PlanitXmlReaderSettingsSchema {

  /** Not instantiable, everything is static */
  private PlanitXmlReaderSettingsSchema() {
  }

  /** the schema of every PLANit reader settings source */
  public static final SettingsSchema SCHEMA = new SettingsSchema(PlanitXmlReaderSettings.class);
}
