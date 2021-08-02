package org.planit.io.xml.util;

import java.io.File;
import java.util.logging.Logger;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitCrsUtils;
import org.planit.utils.geo.PlanitJtsCrsUtils;
import org.planit.utils.misc.FileUtils;
import org.planit.utils.misc.StringUtils;
import org.planit.xml.generated.XMLElementPLANit;

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
  
  /**
   * Find out which of the eligible sub-elements matches the desired type. Currently the following options are supported:
   * 
   * <ul>
   * <li>macroscopicnetwork</li>
   * <li>macroscopiczoning</li>
   * <li>macroscopicdemand</li>
   * <li>servicenetwork</li>
   * <li>routedservices</li>
   * </ul>
   * 
   * @param xmlRawPLANitAll to extract desired (sub) root element from
   * @return direct child acting as root element of PLANit root element of encompassing parsed XML file, null if no match
   * is found
   */
  @SuppressWarnings("unchecked")
  private T getSubEntityRootElementFromCombinedXmlRootElement(XMLElementPLANit xmlRawPLANitAll) {
    /* checks limited to explicitly allowed entities within the PLANit root element */
    if(xmlRawPLANitAll.getMacroscopicnetwork()!=null && xmlRawPLANitAll.getMacroscopicnetwork().getClass().equals(clazz)) {
      return (T) xmlRawPLANitAll.getMacroscopicnetwork();
    }else if (xmlRawPLANitAll.getMacroscopiczoning()!=null && xmlRawPLANitAll.getMacroscopiczoning().getClass().equals(clazz)) {
      return (T) xmlRawPLANitAll.getMacroscopiczoning();
    }else if (xmlRawPLANitAll.getMacroscopicdemand()!=null && xmlRawPLANitAll.getMacroscopicdemand().getClass().equals(clazz)) {
      return (T) xmlRawPLANitAll.getMacroscopicdemand();
    }else if (xmlRawPLANitAll.getServicenetwork()!=null && xmlRawPLANitAll.getServicenetwork().getClass().equals(clazz)) {
      return (T) xmlRawPLANitAll.getServicenetwork();
    }else if (xmlRawPLANitAll.getRoutedservices()!=null && xmlRawPLANitAll.getRoutedservices().getClass().equals(clazz)) {
      return (T) xmlRawPLANitAll.getRoutedservices();
    }
    
    return null;
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
  
  /**
   * Parse the raw XML root (and rest) from file if not already set via constructor
   * 
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   * @return true when success, false when something was parsed but no appropriate match could be found
   * @throws PlanItException thrown if error 
   */
  public boolean initialiseAndParseXmlRootElement(String inputPathDirectory, String xmlFileExtension) throws PlanItException {    
    if(this.xmlRootElement==null) {
      PlanItException.throwIfNull(inputPathDirectory, "Input path directory for XML reader is not provided, unable to parse");
      PlanItException.throwIfNull(xmlFileExtension, "No XML file extension provided, unable to parse files if extension is unknown");
      
      /* first try based on dedicated file for this entity T... */
      final File[] xmlFileNames = FileUtils.getFilesWithExtensionFromDir(inputPathDirectory, xmlFileExtension);
      PlanItException.throwIf(xmlFileNames.length == 0,String.format("Directory %s contains no files with extension %s",inputPathDirectory, xmlFileExtension));
      T rootElement = JAXBUtils.generateInstanceFromXml(clazz, xmlFileNames);
      if(rootElement==null) {
        /*...not available, try and see if embedded in single PLANit XML file for more than one entity */
        XMLElementPLANit xmlRawPLANitAll = JAXBUtils.generateInstanceFromXml(XMLElementPLANit.class, xmlFileNames);
        if(xmlRawPLANitAll==null) {
          LOGGER.severe(String.format("Unable to parse any appropriate XML input file from %s with extension %s, either no file is present, or file is not conforming to underlying XSD",inputPathDirectory, xmlFileExtension));
          return false;
        }
        rootElement = getSubEntityRootElementFromCombinedXmlRootElement(xmlRawPLANitAll);
        if(rootElement==null) {
          LOGGER.severe("Unable to identify which sub element of PLANit XML root element is to be chosen as (sub) root element for this parser");
          return false;
        }
      }
      setXmlRootElement(rootElement);
    }   
    return true;
  }

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
