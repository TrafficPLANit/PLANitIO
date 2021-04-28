package org.planit.io.xml.util;

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
  
  /**
   * Default constructor using default file extensino and user must set output dir afterwards manually
   */
  public PlanitXmlReaderSettings() {
   this(null,PlanitXmlReader.DEFAULT_XML_FILE_EXTENSION); 
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
  
  /** the input path directory used
   * @return directory used
   */
  public String getInputDirectory() {
    return this.inputDirectory;
  }
  
  /** set the input path directory used
   * @param inputDirectory to use
   */
  public void setInputDirectory(String inputDirectory) {
    this.inputDirectory = inputDirectory;
  }  
  
  /** the xml extension used to check for within path directory used
   * @return xml file extension used
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
}
