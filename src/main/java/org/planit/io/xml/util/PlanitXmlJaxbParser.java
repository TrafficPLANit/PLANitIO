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
 * Serves as a base class for readers of PLANit XML files of which the root element is of type T
 * 
 * @author markr
 *
 */
public class PlanitXmlJaxbParser<T> {
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitXmlJaxbParser.class.getCanonicalName());
    
  /** the class to create xml root element for */
  private Class<T> clazz;
    
  /** root element to populate */
  private T xmlRootElement;
  
  /** Create a crs based on passed in srs name. If no srs name is provided the default will be created
   * 
   * @param srsName to use
   * @return craeted crs
   * @throws PlanItException thrown if error
   */
  public static CoordinateReferenceSystem createPlanitCrs(String srsName) throws PlanItException {
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
   * Parse the raw XML root (and rest) from file if not already set via constructor
   * 
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   * @throws PlanItException thrown if error
   */
  public void initialiseAndParseXmlRootElement(String inputPathDirectory, String xmlFileExtension) throws PlanItException {
    PlanItException.throwIfNull(inputPathDirectory, "input path directory for XML reader is not provided, unable to parse");
    PlanItException.throwIfNull(xmlFileExtension, "no XML file extension provided, unable to parse files if extension is unknown");
    
    if(this.xmlRootElement==null) {
      final File[] xmlFileNames = FileUtils.getFilesWithExtensionFromDir(inputPathDirectory, xmlFileExtension);
      PlanItException.throwIf(xmlFileNames.length == 0,String.format("Directory %s contains no files with extension %s",inputPathDirectory, xmlFileExtension));
      setXmlRootElement(JAXBUtils.generateInstanceFromXml(clazz, xmlFileNames));
    } 
    
  }        
 
  /**
   * Default extension for XML input files
   */
  public static final String DEFAULT_XML_FILE_EXTENSION = ".xml";  
  
    
  /** location of where to collect XML file and populate an instance of provided class
   * 
   * @param clazz to create root element and populate it for
   */
  public PlanitXmlJaxbParser(Class<T> clazz) {
    this.clazz = clazz;
    this.xmlRootElement = null;
  }

  /** 
   * Constructor where root element is already provided and assumed to be populated as well
   * 
   * @param xmlRootElement to use
   */
  public PlanitXmlJaxbParser(T xmlRootElement) {
    this.clazz = null;
    this.xmlRootElement = xmlRootElement;
  }
  
  // GETTERS /SETTERS
  
  /** Collect the root element of this reader
   * 
   * @return root element
   */
  public T getXmlRootElement() {
    return xmlRootElement;
  }  
  
  /** Set the root element of this reader
   * 
   * @param xmlRootElement to use
   */
  public void setXmlRootElement(T xmlRootElement) {
    this.xmlRootElement = xmlRootElement;
  }    
      
 
 
  /**
   * mark the xml root element for garbage collection 
   */
  public void clearXmlContent() {
    this.xmlRootElement = null;
    
  }
}
