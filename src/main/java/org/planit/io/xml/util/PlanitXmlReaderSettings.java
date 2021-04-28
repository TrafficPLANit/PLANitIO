package org.planit.io.xml.util;

/**
 * Settings relevant for a Planit Xml reader
 * 
 * @author markr
 *
 */
public class PlanitXmlReaderSettings {

  /** directory to look in */
  private String inputPathDirectory;
  
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
   *  @param inputPathDirectory to use
   *  @param xmlFileExtension to use
   */
  public PlanitXmlReaderSettings(final String inputPathDirectory, final String xmlFileExtension) {
    this.inputPathDirectory = inputPathDirectory;
    this.xmlFileExtension = xmlFileExtension;
  }  
  
  /** the input path directory used
   * @return directory used
   */
  public String getInputPathDirectory() {
    return this.inputPathDirectory;
  }
  
  /** set the input path directory used
   * @param inputPathDirectory to use
   */
  public void setInputPathDirectory(String inputPathDirectory) {
    this.inputPathDirectory = inputPathDirectory;
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
