package org.planit.io.xml.util;

import java.io.File;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitCrsUtils;
import org.planit.utils.geo.PlanitJtsCrsUtils;
import org.planit.utils.misc.FileUtils;
import org.planit.utils.misc.StringUtils;

/**
 * Serves as a base class for readers of PLANit Xml files of which the root element is of type T
 * 
 * @author markr
 *
 */
public class PlanitXmlReader<T> {
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitXmlReader.class.getCanonicalName());
    
  /** the class to create xml root element for */
  private Class<T> clazz;
    
  /** root element to populate */
  private T xmlRootElement;
  
  /** create a crs for planit native format based on passed in srs name. If no srs name is provided the default will be created
   * 
   * @param srsName to use
   * @return craeted crs
   * @throws PlanItException thrown if error
   */
  protected CoordinateReferenceSystem createPlanitCrs(String srsName) throws PlanItException {
    CoordinateReferenceSystem crs = null;
    if(StringUtils.isNullOrBlank(srsName)) {
      crs = PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS;
      LOGGER.warning(String.format("coordinate reference system not set, applying default %s",crs.getName().getCode()));
    }else {
      crs = PlanitCrsUtils.createCoordinateReferenceSystem(srsName);
      if(crs==null) {
        throw new PlanItException("Srs name provided (%s) but it could not be converted into a coordinate reference system",srsName);
      }
    } 
    return crs;
  }  
  
  /**
   * parse the raw XML root (and rest) from file if not already set via constructor
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   * @return populated xml root element if possible based on provided input to constructor
   * @throws PlanItException thrown if error
   */
  protected void initialiseAndParseXmlRootElement(String inputPathDirectory, String xmlFileExtension) throws PlanItException {
    PlanItException.throwIfNull(inputPathDirectory, "input path directory for XML reader is not provided, unable to parse");
    PlanItException.throwIfNull(xmlFileExtension, "no XML file extension provided, unable to parse files if extension is unknown");
    
    if(this.xmlRootElement==null) {
      final File[] xmlFileNames = FileUtils.getFilesWithExtensionFromDir(inputPathDirectory, xmlFileExtension);
      PlanItException.throwIf(xmlFileNames.length == 0,String.format("Directory %s contains no files with extension %s",inputPathDirectory, xmlFileExtension));
      setXmlRootElement(JAXBUtils.generateInstanceFromXml(clazz, xmlFileNames));
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
  
  /** set the root element of this reader
   * 
   * @param xmlRootElement to use
   */
  protected void setXmlRootElement(T xmlRootElement) {
    this.xmlRootElement = xmlRootElement;
  }    
    
  /**
   * Default extension for XML input files
   */
  public static final String DEFAULT_XML_FILE_EXTENSION = ".xml";  
  
    
  /** location of where to collect XML file and populate an instance of provided class
   * 
   * @param clazz to create root element and populate it for
   */
  public PlanitXmlReader(Class<T> clazz) {
    this.clazz = clazz;
    this.xmlRootElement = null;
  }

  /** 
   * Constructor where root element is already provided and assumed to be populated as well
   * 
   * @param xmlRootElement to use
   */
  public PlanitXmlReader(T xmlRootElement) {
    this.clazz = null;
    this.xmlRootElement = xmlRootElement;
  }
 
 
  /**
   * mark the xml root element for garbage collection 
   */
  public void clearXmlContent() {
    this.xmlRootElement = null;
    
  }
}
