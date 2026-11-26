package org.goplanit.io.converter;

import org.goplanit.utils.settings.SettingsSchema;

/**
 * The layout of the PLANit writer settings sources: the header {@code planitIoWriterSettings: 1} and every key of a
 * writer at the top level, or, for the intermodal writer, the settings files of the writers it holds
 * <p>
 * For example, a source of a zoning writer:
 * </p>
 * <pre>
 * planitIoWriterSettings: 1
 * outputDirectory: out
 * destinationCrs: EPSG:28356
 * fileName: zones.xml
 * removeDanglingZones: false
 * </pre>
 * <p>
 * The header and the keys are on the settings classes: the header on {@link PlanitXmlWriterSettings}, the keys every
 * writer of a converter takes on the writer bases, and those of one writer on its own settings. The destination CRS
 * is given by its code, the decimal format by its pattern, and the time unit of trip frequencies by its name. One
 * schema serves every writer; a key another writer takes is unknown to a writer that does not.
 * </p>
 *
 * @author markr
 */
public final class PlanitXmlWriterSettingsSchema {

  /** Not instantiable, everything is static */
  private PlanitXmlWriterSettingsSchema() {
  }

  /** the schema of every PLANit writer settings source */
  public static final SettingsSchema SCHEMA = new SettingsSchema(PlanitXmlWriterSettings.class);
}
