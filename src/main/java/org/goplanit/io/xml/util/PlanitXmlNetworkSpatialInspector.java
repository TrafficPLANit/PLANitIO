package org.goplanit.io.xml.util;

import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.FileUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.xml.utils.PlanitXmlConstants;
import org.locationtech.jts.geom.Coordinate;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Lightweight utility to inspect PLANit network XML spatial metadata without unmarshalling the full JAXB object graph.
 */
public class PlanitXmlNetworkSpatialInspector {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitXmlNetworkSpatialInspector.class.getCanonicalName());

  /** GML direct position element local name */
  private static final String XML_ELEMENT_GML_POS = "pos";

  /** GML direct position list element local name */
  private static final String XML_ELEMENT_GML_POS_LIST = "posList";

  /** legacy GML coordinates element local name */
  private static final String XML_ELEMENT_GML_COORDINATES = "coordinates";

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
  private PlanitXmlNetworkSpatialInspector() {
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
   * Parse the first coordinate pair from the provided coordinate text.
   *
   * @param coordinateText to parse
   * @return parsed coordinate, empty when parsing fails
   */
  private static Optional<Coordinate> parseFirstCoordinate(String coordinateText) {
    if (StringUtils.isNullOrBlank(coordinateText)) {
      return Optional.empty();
    }

    String[] coordinateTokens = coordinateText.trim().split("[,\\s]+");
    if (coordinateTokens.length < 2) {
      return Optional.empty();
    }

    try {
      return Optional.of(new Coordinate(
          Double.parseDouble(coordinateTokens[0]),
          Double.parseDouble(coordinateTokens[1])));
    } catch (NumberFormatException e) {
      LOGGER.fine(String.format("Unable to parse coordinate pair from PLANit XML coordinate text: %s", coordinateText));
      return Optional.empty();
    }
  }

  /**
   * Inspect network source coordinate on a specific XML file.
   *
   * @param xmlFile network XML file to inspect
   * @return first coordinate encountered, empty when no coordinates are found
   */
  private static Optional<Coordinate> peekFirstCoordinate(File xmlFile) {
    XMLInputFactory xmlInputFactory = createInputFactory();

    try (InputStream inputStream = new FileInputStream(xmlFile)) {
      XMLStreamReader xmlReader = xmlInputFactory.createXMLStreamReader(inputStream);
      try {
        while (xmlReader.hasNext()) {
          if (xmlReader.next() == XMLStreamConstants.START_ELEMENT) {
            String localName = xmlReader.getLocalName();
            if (XML_ELEMENT_GML_POS.equals(localName)
                || XML_ELEMENT_GML_POS_LIST.equals(localName)
                || XML_ELEMENT_GML_COORDINATES.equals(localName)) {
              return parseFirstCoordinate(xmlReader.getElementText());
            }
          }
        }
      } finally {
        xmlReader.close();
      }
    } catch (Exception e) {
      LOGGER.fine(String.format("Unable to inspect representative network coordinate in %s", xmlFile.getAbsolutePath()));
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
   * Peek a representative source coordinate by first detecting the PLANit XML version.
   *
   * @param inputPath directory or XML file to inspect
   * @return first coordinate encountered, empty when absent or no matching network XML file can be found
   */
  public static Optional<Coordinate> peekNetworkReferenceCoordinate(String inputPath) {
    File networkXmlFile = findFirstXmlFileByRoot(inputPath, PlanitXmlConstants.XML_ROOT_MACROSCOPIC_NETWORK);
    if (networkXmlFile == null) {
      return Optional.empty();
    }

    var version = PlanitXmlCrsInspector.peekPlanitXmlVersion(networkXmlFile.getAbsolutePath());
    if (version.isEmpty()) {
      return Optional.empty();
    }
    return peekNetworkReferenceCoordinate(networkXmlFile.getAbsolutePath(), version.get());
  }

  /**
   * Peek a representative source coordinate for a known PLANit XML version.
   *
   * @param inputPath directory or XML file to inspect
   * @param version PLANit XML version to apply
   * @return first coordinate encountered, empty when absent or no matching network XML file can be found
   */
  public static Optional<Coordinate> peekNetworkReferenceCoordinate(String inputPath, PlanitXmlVersion version) {
    File networkXmlFile = findFirstXmlFileByRoot(inputPath, PlanitXmlConstants.XML_ROOT_MACROSCOPIC_NETWORK);
    if (networkXmlFile == null) {
      return Optional.empty();
    }

    switch (version) {
      case V2:
      case V1:
        return peekFirstCoordinate(networkXmlFile);
      default:
        throw new PlanItRunTimeException(
            "Unsupported PLANit XML version %s for representative coordinate lookup", version);
    }
  }
}
