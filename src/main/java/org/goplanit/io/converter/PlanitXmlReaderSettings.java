package org.goplanit.io.converter;

import org.goplanit.converter.FileBasedConverterReaderSettings;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.settings.SettingsSchema;
import org.goplanit.utils.settings.YamlWriteSettingsSupport;
import org.goplanit.utils.settings.annotations.CoveredBySettingsKey;
import org.goplanit.utils.settings.annotations.SettingsHeader;
import org.goplanit.utils.settings.annotations.SettingsKeyLevel;
import org.goplanit.utils.settings.annotations.SettingsRead;
import org.goplanit.utils.settings.annotations.SettingsWrite;

import java.util.logging.Logger;

/**
 * Settings relevant for a Planit Xml reader
 * <p>
 * Each PLANit reader has a settings file of its own, headed {@code planitIoReaderSettings: 1}, holding every key at
 * the top level, such as
 * </p>
 * <pre>
 * planitIoReaderSettings: 1
 * inputSource: planit_osm
 * syncXmlIdsToIds: true
 * </pre>
 * <p>
 * The input source is a folder holding the PLANit files, where the reader looks for the file of the part it reads, or
 * that one file. The readers' files are apart from the writers', so a step reading PLANit and writing another format
 * uses a whole file for each. {@link PlanitXmlReaderSettingsFactory} makes the settings of each reader from such
 * files.
 * </p>
 *
 * @author markr
 *
 */
@SettingsHeader(key = "planitIoReaderSettings", version = 1)
public class PlanitXmlReaderSettings extends FileBasedConverterReaderSettings implements YamlWriteSettingsSupport {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitXmlReaderSettings.class.getCanonicalName());

  /** xml file extension to use */
  private String xmlFileExtension;

  /** flag indicating if XML ids are to be overwritten with internal ids after completion of parsing */
  private boolean syncXmlIdsToIds = DEFAULT_SYNC_XMLIDS_TO_IDS;

  /** default extension for XML */
  public static final String DEFAULT_XML_EXTENSION = ".xml";

  /** flag indicating default for syncing XML ids with internal ids */
  public static final boolean DEFAULT_SYNC_XMLIDS_TO_IDS = false;

  /**
   * The schema of every PLANit reader settings source: the header of these settings, {@code planitIoReaderSettings: 1},
   * and every key of a reader at the top level, or, for the intermodal reader, the settings files of the readers it
   * holds. One schema serves every reader, as they all take the same keys
   */
  public static final SettingsSchema SCHEMA = new SettingsSchema(PlanitXmlReaderSettings.class);

  
  /**
   * Default constructor using default file extension and user must set output dir afterwards manually
   */
  public PlanitXmlReaderSettings() {
   this(null,DEFAULT_XML_EXTENSION);
  }

  /**
   * Constructor
   *
   *  @param inputDirectory to use
   */
  public PlanitXmlReaderSettings(final String inputDirectory) {
    this(inputDirectory, DEFAULT_XML_EXTENSION);
  }

  /**
   * Constructor
   * 
   *  @param inputDirectory to use
   *  @param xmlFileExtension to use
   */
  public PlanitXmlReaderSettings(final String inputDirectory, final String xmlFileExtension) {
    super();
    setInputDirectory(inputDirectory);
    this.xmlFileExtension = xmlFileExtension;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
    this.xmlFileExtension = DEFAULT_XML_EXTENSION;
    this.syncXmlIdsToIds = DEFAULT_SYNC_XMLIDS_TO_IDS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings(int level) {
    super.logSettings(level);
    LOGGER.info(LoggingUtils.settingsValue("XML file extension", getXmlFileExtension(), level));
    LOGGER.info(LoggingUtils.settingsValue("Sync XML ids to ids", isSyncXmlIdsToIds(), level));
  }
  
  /** Set the input source to a local folder, absolute or relative to the working folder, as a settings file in the
   * working folder giving it as {@code inputSource} would
   *
   * @param inputDirectory to use, null for none
   */
  @CoveredBySettingsKey("inputSource")
  public void setInputDirectory(String inputDirectory) {
    setInputSource(inputDirectory);
  }

  /** the XML extension used to check for within path directory used
   *
   * @return XML file extension used
   */
  @SettingsRead(key = "xmlFileExtension")
  public String getXmlFileExtension() {
    return this.xmlFileExtension;
  }

  /** the xml extension used to check for within path directory used
   * @param xmlFileExtension extension to use
   */
  @SettingsWrite(key = "xmlFileExtension", level = SettingsKeyLevel.UNCOMMON,
      description = "The extension of the PLANit files looked for when reading a folder, such as .xml")
  public void setXmlFileExtension(String xmlFileExtension) {
    this.xmlFileExtension = xmlFileExtension;
  }

  /**
   * Verify if reader is designated to replace parsed XML ids with internally generated ids
   *
   * @return true when syncing is active, false otherwise
   */
  @SettingsRead(key = "syncXmlIdsToIds")
  public boolean isSyncXmlIdsToIds() {
    return syncXmlIdsToIds;
  }

  /**
   * Determine if reader should replace parsed XML ids with internally generated ids
   *
   * @param syncXmlIdsToIds syncing active when true, false otherwise
   */
  @SettingsWrite(key = "syncXmlIdsToIds", level = SettingsKeyLevel.UNCOMMON,
      description = "Replace the XML ids read with the ids PLANit gives the entities read")
  public void setSyncXmlIdsToIds(boolean syncXmlIdsToIds) {
    this.syncXmlIdsToIds = syncXmlIdsToIds;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SettingsSchema getSettingsSchema() {
    return SCHEMA;
  }
}
