package org.goplanit.io.xml.util;

/**
 * Settings relevant for a Planit Xml reader
 * 
 * @author markr
 *
 */
public class PlanitXmlReaderSettings {

  /** directory to look in */
  private String inputDirectory;
  
  /** xml file extension to use */
  private String xmlFileExtension;

  /** flag indicating if XML ids are to be overwritten with internal ids after completion of parsing */
  private boolean syncXmlIdsToIds = DEFAULT_SYNC_XMLIDS_TO_IDS;

  public static String DEFAULT_XML_EXTENSION = ".xml";

  public static boolean DEFAULT_SYNC_XMLIDS_TO_IDS = false;

  
  /**
   * Default constructor using default file extensino and user must set output dir afterwards manually
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
    this.inputDirectory = inputDirectory;
    this.xmlFileExtension = xmlFileExtension;
  }  
  
  /** The input path directory used
   * 
   * @return directory used
   */
  public String getInputDirectory() {
    return this.inputDirectory;
  }
  
  /** Set the input path directory used
   * 
   * @param inputDirectory to use
   */
  public void setInputDirectory(String inputDirectory) {
    this.inputDirectory = inputDirectory;
  }  
  
  /** the XML extension used to check for within path directory used
   * 
   * @return XML file extension used
   */
  public String getXmlFileExtension() {
    return this.xmlFileExtension;
  } 
  
  /** the xml extension used to check for within path directory used
   * @param xmlFileExtension extension to use
   */
  public void setXmlFileExtension(String xmlFileExtension) {
    this.xmlFileExtension = xmlFileExtension;
  }

  /**
   * Verify if reader is designated to replace parsed XML ids with internally generated ids
   *
   * @return true when syncing is active, false otherwise
   */
  public boolean isSyncXmlIdsToIds() {
    return syncXmlIdsToIds;
  }

  /**
   * Determine if reader should replace parsed XML ids with internally generated ids
   *
   * @param syncXmlIdsToIds syncing active when true, false otherwise
   */
  public void setSyncXmlIdsToIds(boolean syncXmlIdsToIds) {
    this.syncXmlIdsToIds = syncXmlIdsToIds;
  }
}
