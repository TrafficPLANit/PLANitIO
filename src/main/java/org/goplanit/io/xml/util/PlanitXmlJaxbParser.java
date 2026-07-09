package org.goplanit.io.xml.util;

import java.io.File;
import java.util.logging.Logger;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitCrsUtils;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.misc.FileUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.xml.generated.v2.XMLElementPLANit;
import org.goplanit.xml.utils.JAXBUtils;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

/**
 * Serves as a base class for readers of PLANit XML files of which the root element is of type LATEST, or LEGACY.
 * In the latter case it will be normalised to LATEST.
 *
 * @param <LATEST> type of latest
 * @param <LEGACY> type of legacy
 * @author markr
 *
 */
public class PlanitXmlJaxbParser<LATEST, LEGACY> {
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitXmlJaxbParser.class.getCanonicalName());
    
  /** the class to create xml root element for */
  private Class<LATEST> latestVersionClazz;

  private Class<LEGACY> legacyVersionClazz;
    
  /** root element to populate */
  private LATEST xmlRootElement;

  /** flag to indicate if input was a legacy version */
  private boolean inputIsLegacyVersion;

  /**
   * Mark as legacy version
   */
  public void setInputIsLegacyVersion(){
    inputIsLegacyVersion = true;
  }
  
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
  private LATEST getSubEntityRootElementFromCombinedXmlRootElement(XMLElementPLANit xmlRawPLANitAll) {
    /* checks limited to explicitly allowed entities within the PLANit root element */
    if(xmlRawPLANitAll.getMacroscopicnetwork()!=null &&
            xmlRawPLANitAll.getMacroscopicnetwork().getClass().equals(latestVersionClazz)) {
      return (LATEST) xmlRawPLANitAll.getMacroscopicnetwork();
    }else if (xmlRawPLANitAll.getMacroscopiczoning()!=null &&
            xmlRawPLANitAll.getMacroscopiczoning().getClass().equals(latestVersionClazz)) {
      return (LATEST) xmlRawPLANitAll.getMacroscopiczoning();
    }else if (xmlRawPLANitAll.getMacroscopicdemand()!=null &&
            xmlRawPLANitAll.getMacroscopicdemand().getClass().equals(latestVersionClazz)) {
      return (LATEST) xmlRawPLANitAll.getMacroscopicdemand();
    }else if (xmlRawPLANitAll.getServicenetwork()!=null &&
            xmlRawPLANitAll.getServicenetwork().getClass().equals(latestVersionClazz)) {
      return (LATEST) xmlRawPLANitAll.getServicenetwork();
    }else if (xmlRawPLANitAll.getRoutedservices()!=null &&
            xmlRawPLANitAll.getRoutedservices().getClass().equals(latestVersionClazz)) {
      return (LATEST) xmlRawPLANitAll.getRoutedservices();
    }
    
    return null;
  }

  /**
   * Default extension for XML input files
   */
  public static final String DEFAULT_XML_FILE_EXTENSION = ".xml";  
  
    
  /** Location of where to collect XML file and populate an instance of provided class
   * 
   * @param latestVersionClazz to create root element and populate it for
   * @param legacyVersionClazz to create root element and populate it for in latest version normalised
   */
  public PlanitXmlJaxbParser(Class<LATEST> latestVersionClazz, Class<LEGACY> legacyVersionClazz) {
    this.latestVersionClazz = latestVersionClazz;
    this.legacyVersionClazz = legacyVersionClazz;
    this.xmlRootElement = null;
    this.inputIsLegacyVersion = false;
  }


  // GETTERS /SETTERS
  
  /**
   * Parse the raw XML root (and rest) from file if not already set via constructor
   * 
   * @param inputPathDirectory to use
   * @param xmlFileExtension to use
   * @return true when success, false when something was parsed but no appropriate match could be found
   */
  public boolean initialiseAndParseXmlRootElement(String inputPathDirectory, String xmlFileExtension) {
    if(this.xmlRootElement==null) {
      PlanItRunTimeException.throwIfNull(inputPathDirectory,
              "Input path directory for XML reader is not provided, unable to parse");
      PlanItRunTimeException.throwIfNull(xmlFileExtension,
              "No XML file extension provided, unable to parse files if extension is unknown");
      PlanItRunTimeException.throwIfNull(latestVersionClazz,
              "No XML root element class provided, unable to parse files if this is unknown");
      PlanItRunTimeException.throwIfNull(legacyVersionClazz,
              "No XML root element legacy class provided, unable to parse files if this is unknown");
      
      /* first try based on dedicated file for this entity LATEST... */
      final File[] xmlFileNames = FileUtils.getFilesWithExtensionFromDir(inputPathDirectory, xmlFileExtension);
      PlanItRunTimeException.throwIf(xmlFileNames.length == 0,
              String.format("Directory %s contains no files with extension %s",inputPathDirectory, xmlFileExtension));
      LATEST rootElement = JAXBUtils.generateInstanceFromXml(
          xmlFileNames, latestVersionClazz, legacyVersionClazz, this::setInputIsLegacyVersion);
      if(rootElement==null) {
        /*...not available, try and see if embedded in single PLANit XML file for more than one entity */
        XMLElementPLANit xmlRawPLANitAll = JAXBUtils.generateInstanceFromXml(xmlFileNames,
                XMLElementPLANit.class,
            org.goplanit.xml.generated.v1.XMLElementPLANit.class,
            this::setInputIsLegacyVersion);

        if(xmlRawPLANitAll==null) {
          LOGGER.severe(String.format("Unable to parse any appropriate XML input file from %s with extension %s," +
                  " either no file is present, or file is not conforming to underlying XSD",
                  inputPathDirectory, xmlFileExtension));
          return false;
        }
        rootElement = getSubEntityRootElementFromCombinedXmlRootElement(xmlRawPLANitAll);
        if(rootElement==null) {
          LOGGER.severe("Unable to identify which sub element of PLANit XML root element is to be " +
                  "chosen as (sub) root element for this parser");
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
   * @return created crs
   */
  public static CoordinateReferenceSystem createPlanitCrs(String srsName) {
    CoordinateReferenceSystem crs = null;
    if(StringUtils.isNullOrBlank(srsName)) {
      crs = PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS;
      LOGGER.warning(String.format("Coordinate reference system not set, applying default %s",crs.getName().getCode()));
    }else {
      crs = PlanitCrsUtils.createCoordinateReferenceSystem(srsName);
      PlanItRunTimeException.throwIfNull(crs, "Srs name provided (%s) but it could not " +
              "be converted into a coordinate reference system",srsName);
    }
    return crs;
  }

  /** Collect the root element of this reader
   * 
   * @return root element
   */
  public LATEST getXmlRootElement() {
    return xmlRootElement;
  }  
  
  /** Set the root element of this reader
   * 
   * @param xmlRootElement to use
   */
  public void setXmlRootElement(LATEST xmlRootElement) {
    this.xmlRootElement = xmlRootElement;
  }

  /**
   * mark the xml root element for garbage collection 
   */
  public void clearXmlContent() {
    this.xmlRootElement = null;
  }

  /**
   * Check if input was a legacy version. Only trustworthy after parsing
   *
   * @return true when legacy, false otherwise
   */
  public boolean isInputIsLegacyVersion(){
    return inputIsLegacyVersion;
  }
}
