package org.goplanit.io.xml.util;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.FileUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.xml.utils.PlanitXmlConstants;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Lightweight utility to inspect PLANit XML files for CRS metadata without unmarshalling the full JAXB object graph.
 */
public class PlanitXmlCrsInspector {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitXmlCrsInspector.class.getCanonicalName());

  /** PLANit macroscopic network CRS location */
  private static final List<String> XML_PATH_MACROSCOPIC_NETWORK_CRS =
      Arrays.asList(
          PlanitXmlConstants.XML_ROOT_MACROSCOPIC_NETWORK,
          PlanitXmlConstants.XML_ELEMENT_INFRASTRUCTURE_LAYERS);

  /** PLANit macroscopic zoning CRS location */
  private static final List<String> XML_PATH_MACROSCOPIC_ZONING_CRS =
      Arrays.asList(PlanitXmlConstants.XML_ROOT_MACROSCOPIC_ZONING);

  /**
   * Root element metadata.
   */
  private static class XmlRootData {

    /** root local name */
    private final String localName;

    /** root namespace URI, may be null */
    private final String namespaceUri;

    /**
     * Constructor.
     *
     * @param localName root local name
     * @param namespaceUri root namespace URI
     */
    private XmlRootData(String localName, String namespaceUri) {
      this.localName = localName;
      this.namespaceUri = namespaceUri;
    }
  }

  /**
   * Utility class.
   */
  private PlanitXmlCrsInspector() {
  }

  /**
   * Create streaming input factory with external entity support disabled.
   *
   * @return configured input factory
   */
  private static XMLInputFactory createInputFactory() {
    XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
    try {
      xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    } catch (IllegalArgumentException ignored) {
      // ignore when not supported by the active implementation
    }
    try {
      xmlInputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
    } catch (IllegalArgumentException ignored) {
      // ignore when not supported by the active implementation
    }
    return xmlInputFactory;
  }

  /**
   * Collect XML candidate files from a directory or direct XML file path.
   *
   * @param inputPath directory or XML file to inspect
   * @return candidate XML files
   */
  private static List<File> collectCandidateXmlFiles(String inputPath) {
    PlanItRunTimeException.throwIfNull(
        inputPath, "Input path for PLANit XML inspection is not provided, unable to inspect");

    File inputFile = new File(inputPath);
    PlanItRunTimeException.throwIf(
        !inputFile.exists(), "Input path %s does not exist, unable to inspect PLANit XML", inputPath);

    if (inputFile.isFile()) {
      return Arrays.asList(inputFile);
    }

    File[] xmlFiles =
        FileUtils.getFilesWithExtensionFromDir(inputPath, PlanitXmlReaderSettings.DEFAULT_XML_EXTENSION);
    PlanItRunTimeException.throwIf(
        xmlFiles.length == 0,
        "Directory %s contains no files with extension %s",
        inputPath,
        PlanitXmlReaderSettings.DEFAULT_XML_EXTENSION);
    return Arrays.asList(xmlFiles);
  }

  /**
   * Collect attribute value from current start element.
   *
   * @param xmlReader reader positioned at the desired element
   * @param attributeName attribute name to extract
   * @return attribute value, null when absent
   */
  private static String collectAttributeValue(XMLStreamReader xmlReader, String attributeName) {
    String attributeValue = xmlReader.getAttributeValue(null, attributeName);
    if (!StringUtils.isNullOrBlank(attributeValue)) {
      return attributeValue;
    }

    for (int index = 0; index < xmlReader.getAttributeCount(); ++index) {
      if (attributeName.equals(xmlReader.getAttributeLocalName(index))) {
        return xmlReader.getAttributeValue(index);
      }
    }
    return null;
  }

  /**
   * Determine PLANit XML version from root namespace.
   *
   * @param xmlRootData root element data
   * @return detected version
   */
  private static PlanitXmlVersion determineVersion(XmlRootData xmlRootData) {
    if (PlanitXmlConstants.XML_NAMESPACE_V2.equals(xmlRootData.namespaceUri)) {
      return PlanitXmlVersion.V2;
    }
    return PlanitXmlVersion.V1;
  }

  /**
   * Find the first XML file whose root matches the desired local name.
   *
   * @param inputPath directory or XML file to inspect
   * @param expectedRootLocalName root local name to match
   * @return matching file, null when absent
   */
  private static File findFirstXmlFileByRoot(String inputPath, String expectedRootLocalName) {
    for (File xmlFile : collectCandidateXmlFiles(inputPath)) {
      XmlRootData xmlRootData = peekXmlRootData(xmlFile);
      if (xmlRootData != null && expectedRootLocalName.equals(xmlRootData.localName)) {
        return xmlFile;
      }
    }
    return null;
  }

  /**
   * Collect path for network CRS lookup based on PLANit XML version.
   *
   * @param version PLANit XML version
   * @return path to inspect
   */
  private static List<String> getNetworkCrsElementPath(PlanitXmlVersion version) {
    switch (version) {
      case V2:
      case V1:
        return XML_PATH_MACROSCOPIC_NETWORK_CRS;
      default:
        throw new PlanItRunTimeException("Unsupported PLANit XML version %s for network CRS lookup", version);
    }
  }

  /**
   * Collect path for zoning CRS lookup based on PLANit XML version.
   *
   * @param version PLANit XML version
   * @return path to inspect
   */
  private static List<String> getZoningCrsElementPath(PlanitXmlVersion version) {
    switch (version) {
      case V2:
      case V1:
        return XML_PATH_MACROSCOPIC_ZONING_CRS;
      default:
        throw new PlanItRunTimeException("Unsupported PLANit XML version %s for zoning CRS lookup", version);
    }
  }

  /**
   * Check if current XML element path matches desired path.
   *
   * @param currentPath current XML path
   * @param desiredPath desired XML path
   * @return true when paths match
   */
  private static boolean matchesPath(List<String> currentPath, List<String> desiredPath) {
    if (currentPath.size() != desiredPath.size()) {
      return false;
    }

    for (int index = 0; index < desiredPath.size(); ++index) {
      if (!desiredPath.get(index).equals(currentPath.get(index))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Inspect a specific XML file for the PLANit XML version.
   *
   * @param xmlFile file to inspect
   * @return detected version, empty when root cannot be read
   */
  private static Optional<PlanitXmlVersion> peekPlanitXmlVersion(File xmlFile) {
    XmlRootData xmlRootData = peekXmlRootData(xmlFile);
    if (xmlRootData == null) {
      return Optional.empty();
    }
    return Optional.of(determineVersion(xmlRootData));
  }

  /**
   * Peek an attribute value on a specific XML element path.
   *
   * @param xmlFile XML file to inspect
   * @param elementPath target element path from root to desired element
   * @param attributeName attribute to extract
   * @return extracted attribute value, empty when absent
   */
  private static Optional<String> peekAttributeValue(File xmlFile, List<String> elementPath, String attributeName) {
    XMLInputFactory xmlInputFactory = createInputFactory();

    try (InputStream inputStream = new FileInputStream(xmlFile)) {
      XMLStreamReader xmlReader = xmlInputFactory.createXMLStreamReader(inputStream);
      List<String> currentPath = new ArrayList<String>();
      try {
        while (xmlReader.hasNext()) {
          int eventType = xmlReader.next();
          if (eventType == XMLStreamConstants.START_ELEMENT) {
            currentPath.add(xmlReader.getLocalName());
            if (matchesPath(currentPath, elementPath)) {
              String attributeValue = collectAttributeValue(xmlReader, attributeName);
              if (!StringUtils.isNullOrBlank(attributeValue)) {
                return Optional.of(attributeValue);
              }
            }
          } else if (eventType == XMLStreamConstants.END_ELEMENT && !currentPath.isEmpty()) {
            currentPath.remove(currentPath.size() - 1);
          }
        }
      } finally {
        xmlReader.close();
      }
    } catch (Exception e) {
      LOGGER.fine(
          String.format(
              "Unable to inspect XML attribute %s in %s",
              attributeName,
              xmlFile.getAbsolutePath()));
    }
    return Optional.empty();
  }

  /**
   * Inspect the root element of an XML file.
   *
   * @param xmlFile file to inspect
   * @return root data when successfully collected, null otherwise
   */
  private static XmlRootData peekXmlRootData(File xmlFile) {
    XMLInputFactory xmlInputFactory = createInputFactory();

    try (InputStream inputStream = new FileInputStream(xmlFile)) {
      XMLStreamReader xmlReader = xmlInputFactory.createXMLStreamReader(inputStream);
      try {
        while (xmlReader.hasNext()) {
          if (xmlReader.next() == XMLStreamConstants.START_ELEMENT) {
            return new XmlRootData(xmlReader.getLocalName(), xmlReader.getNamespaceURI());
          }
        }
      } finally {
        xmlReader.close();
      }
    } catch (Exception e) {
      LOGGER.fine(String.format("Unable to inspect XML root in %s", xmlFile.getAbsolutePath()));
    }
    return null;
  }

  /**
   * Peek PLANit XML version by inspecting the root element namespace on the first recognised PLANit XML file found.
   *
   * @param inputPath directory or XML file to inspect
   * @return detected PLANit XML version, empty when no recognised PLANit XML file is found
   */
  public static Optional<PlanitXmlVersion> peekPlanitXmlVersion(String inputPath) {
    for (File xmlFile : collectCandidateXmlFiles(inputPath)) {
      XmlRootData xmlRootData = peekXmlRootData(xmlFile);
      if (xmlRootData != null &&
          (PlanitXmlConstants.XML_ROOT_MACROSCOPIC_NETWORK.equals(xmlRootData.localName) ||
              PlanitXmlConstants.XML_ROOT_MACROSCOPIC_ZONING.equals(xmlRootData.localName))) {
        return Optional.of(determineVersion(xmlRootData));
      }
    }
    return Optional.empty();
  }

  /**
   * Peek network CRS srs name by first detecting the PLANit XML version.
   *
   * @param inputPath directory or XML file to inspect
   * @return detected network srs name, empty when absent or no matching network XML file can be found
   */
  public static Optional<String> peekNetworkSrsName(String inputPath) {
    File networkXmlFile = findFirstXmlFileByRoot(inputPath, PlanitXmlConstants.XML_ROOT_MACROSCOPIC_NETWORK);
    if (networkXmlFile == null) {
      return Optional.empty();
    }

    Optional<PlanitXmlVersion> version = peekPlanitXmlVersion(networkXmlFile);
    if (version.isEmpty()) {
      return Optional.empty();
    }
    return peekNetworkSrsName(networkXmlFile.getAbsolutePath(), version.get());
  }

  /**
   * Peek network CRS srs name for a known PLANit XML version.
   *
   * @param inputPath directory or XML file to inspect
   * @param version PLANit XML version to apply
   * @return detected network srs name, empty when absent or no matching network XML file can be found
   */
  public static Optional<String> peekNetworkSrsName(String inputPath, PlanitXmlVersion version) {
    File networkXmlFile = findFirstXmlFileByRoot(inputPath, PlanitXmlConstants.XML_ROOT_MACROSCOPIC_NETWORK);
    if (networkXmlFile == null) {
      return Optional.empty();
    }
    return peekAttributeValue(
        networkXmlFile, getNetworkCrsElementPath(version), PlanitXmlConstants.XML_ATTRIBUTE_SRS_NAME);
  }

  /**
   * Peek zoning CRS srs name by first detecting the PLANit XML version.
   *
   * @param inputPath directory or XML file to inspect
   * @return detected zoning srs name, empty when absent or no matching zoning XML file can be found
   */
  public static Optional<String> peekZoningSrsName(String inputPath) {
    File zoningXmlFile = findFirstXmlFileByRoot(inputPath, PlanitXmlConstants.XML_ROOT_MACROSCOPIC_ZONING);
    if (zoningXmlFile == null) {
      return Optional.empty();
    }

    Optional<PlanitXmlVersion> version = peekPlanitXmlVersion(zoningXmlFile);
    if (version.isEmpty()) {
      return Optional.empty();
    }
    return peekZoningSrsName(zoningXmlFile.getAbsolutePath(), version.get());
  }

  /**
   * Peek zoning CRS srs name for a known PLANit XML version.
   *
   * @param inputPath directory or XML file to inspect
   * @param version PLANit XML version to apply
   * @return detected zoning srs name, empty when absent or no matching zoning XML file can be found
   */
  public static Optional<String> peekZoningSrsName(String inputPath, PlanitXmlVersion version) {
    File zoningXmlFile = findFirstXmlFileByRoot(inputPath, PlanitXmlConstants.XML_ROOT_MACROSCOPIC_ZONING);
    if (zoningXmlFile == null) {
      return Optional.empty();
    }
    return peekAttributeValue(
        zoningXmlFile, getZoningCrsElementPath(version), PlanitXmlConstants.XML_ATTRIBUTE_SRS_NAME);
  }
}
