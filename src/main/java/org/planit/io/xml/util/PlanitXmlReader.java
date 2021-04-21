package org.planit.io.xml.util;

import java.io.File;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.geo.PlanitOpenGisUtils;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsCrsUtils;
import org.planit.utils.misc.FileUtils;

/**
 * Serves as a base class for readers of PLANit Xml files of which the root element is of type T
 * 
 * @author markr
 *
 */
public class PlanitXmlReader<T> {
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitXmlReader.class.getCanonicalName());
  
  /** directory to look in */
  private final String inputPathDirectory;
  
  /** xml file extension to use */
  private final String xmlFileExtension;
  
  /** the class to create xml root element for */
  private Class<T> clazz;
    
  /** root element to populate */
  private T xmlRootElement;
  
  /** create a crs for planit native format based on passed in srs name. If no srs name is provided the default will be created
   * 
   * @param srsname to use
   * @return craeted crs
   */
  protected CoordinateReferenceSystem createPlanitCrs(String srsname) {
    CoordinateReferenceSystem crs = null;
    if(srsname==null || srsname.isBlank()) {
      crs = PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS;
      LOGGER.warning(String.format("coordinate reference system not set, applying default %s",crs.getName().getCode()));
    }else {
      crs = PlanitOpenGisUtils.createCoordinateReferenceSystem(srsname);
    } 
    return crs;
  }  
  
  /**
   * parse the raw XML root (and rest) from file if not already set via constructor
   * @return populated xml root element if possible based on provided input to constructor
   * @throws PlanItException thrown if error
   */
  protected T initialiseAndParseXmlRootElement() throws PlanItException {
    if(this.xmlRootElement==null) {
      final File[] xmlFileNames = FileUtils.getFilesWithExtensionFromDir(inputPathDirectory, xmlFileExtension);
      PlanItException.throwIf(xmlFileNames.length == 0,String.format("Directory %s contains no files with extension %s",inputPathDirectory, xmlFileExtension));
      return JAXBUtils.generateInstanceFromXml(clazz, xmlFileNames);
    }else { 
      return xmlRootElement;
    }
  }  
  
  // GETTERS /SETTERS
  
  /** Collect the root element of this reader
   * 
   * @return root element
   */
  protected T getXmlRootElement() {
    return xmlRootElement;
  }  
  
  /** the input path directory used
   * @return directory used
   */
  protected String getInputPathDirectory() {
    return this.inputPathDirectory;
  }
  
  /** the xml extension used to check for within path directory used
   * @return directory used
   */
  protected String getXmlFileExtension() {
    return this.xmlFileExtension;
  }  
  
  /**
   * Default extension for XML input files
   */
  public static final String DEFAULT_XML_FILE_EXTENSION = ".xml";  
  
    
  /** location of where to collect Xml file and poopulate an instance of provided class
   * 
   * @param clazz to create root element and populate it for
   * @param networkPathDirectory directory on where to find the file
   * @param xmlFileExtension xml extension to use
   */
  public PlanitXmlReader(Class<T> clazz, String networkPathDirectory, String xmlFileExtension) {
    this.clazz = clazz;
    this.xmlRootElement = null;
    this.xmlFileExtension = null;
    this.inputPathDirectory = null;
  }

  /** 
   * Constructor where root element is already provided and assumed to be populated as well
   * 
   * @param xmlRootElement to use
   */
  public PlanitXmlReader(T xmlRootElement) {
    this.clazz = null;
    this.xmlRootElement = xmlRootElement;
    this.xmlFileExtension = null;
    this.inputPathDirectory = null;
  }
 
 
  /**
   * mark the xml root element for garbage collection 
   */
  public void clearXmlContent() {
    this.xmlRootElement = null;
    
  }
}
