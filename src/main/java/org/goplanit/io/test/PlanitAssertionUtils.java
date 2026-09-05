package org.goplanit.io.test;

import org.goplanit.io.converter.demands.PlanitDemandsWriterSettings;
import org.goplanit.io.converter.network.PlanitNetworkWriterSettings;
import org.goplanit.io.converter.service.PlanitRoutedServicesWriterSettings;
import org.goplanit.io.converter.service.PlanitServiceNetworkWriterSettings;
import org.goplanit.io.converter.zoning.PlanitZoningWriterSettings;
import org.goplanit.utils.misc.FileUtils;
import org.xmlunit.matchers.CompareMatcher;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.DifferenceEvaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.nio.file.Path;

/**
 * Utilities for asserting PLANit outputs of integration tests
 */
public class PlanitAssertionUtils {

  /** Default absolute tolerance for GML coordinate comparisons */
  public static final double DEFAULT_GML_COORDINATE_TOLERANCE = 1e-6;

  /** Regex to parse numeric values from GML coordinate text */
  private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?\\d*\\.?\\d+(?:[Ee][-+]?\\d+)?");

  /**
   * Create Path from location string for network
   * @param theDir  location
   * @return path
   */
  private static Path pathOfNetworkFile(String theDir){
    return Path.of(theDir, PlanitNetworkWriterSettings.DEFAULT_NETWORK_XML).toAbsolutePath();
  }

  /**
   * Create Path from location string for zoning
   * @param theDir  location
   * @return path
   */
  private static Path pathOfZoningFile(String theDir){
    return Path.of(theDir, PlanitZoningWriterSettings.DEFAULT_ZONING_XML).toAbsolutePath();
  }

  /**
   * Create Path from location string for service network
   * @param theDir  location
   * @return path
   */
  private static Path pathOfServiceNetworkFile(String theDir){
    return Path.of(theDir, PlanitServiceNetworkWriterSettings.DEFAULT_SERVICE_NETWORK_XML).toAbsolutePath();
  }

  /**
   * Create Path from location string for rotue services
   * @param theDir  location
   * @return path
   */
  private static Path pathOfRoutedServicesFile(String theDir){
    return Path.of(theDir, PlanitRoutedServicesWriterSettings.DEFAULT_ROUTED_SERVICES_XML).toAbsolutePath();
  }

  /**
   * Create Path from location string for demands
   * @param theDir  location
   * @return path
   */
  private static Path pathOfDemandsFile(String theDir) {
    return Path.of(theDir, PlanitDemandsWriterSettings.DEFAULT_DEMANDS_XML).toAbsolutePath();
  }

  /**
   * Assert that XML files in location are similar
   * @param file1 result location
   * @param file2 reference location to compare to
   * @throws IOException when error
   */
  private static void assertXmlFileContentSimilar(String file1, String file2) throws IOException {
    org.hamcrest.MatcherAssert.assertThat(
        /* xml unit functionality comparing the two files */
        FileUtils.parseUtf8FileContentAsString(file1),
        CompareMatcher.isSimilarTo(FileUtils.parseUtf8FileContentAsString(file2)));
  }

  /**
   * Assert that XML files are similar while allowing small numeric differences in GML coordinate content
   * @param file1 result location
   * @param file2 reference location to compare to
   * @param gmlCoordinateTolerance absolute tolerance to apply to GML coordinate values
   * @throws IOException when error
   */
  private static void assertXmlFileContentSimilarWithGmlCoordinateTolerance(
      String file1, String file2, double gmlCoordinateTolerance) throws IOException {
    org.hamcrest.MatcherAssert.assertThat(
        /* xml unit functionality comparing the two files */
        FileUtils.parseUtf8FileContentAsString(file1),
        CompareMatcher
            .isSimilarTo(FileUtils.parseUtf8FileContentAsString(file2))
            .withDifferenceEvaluator(createGmlCoordinateDifferenceEvaluator(gmlCoordinateTolerance)));
  }

  /**
   * Create evaluator that treats small numeric differences in GML coordinate content as similar
   * @param tolerance absolute tolerance to apply
   * @return difference evaluator
   */
  private static DifferenceEvaluator createGmlCoordinateDifferenceEvaluator(double tolerance) {
    return (comparison, outcome) -> {
      if (outcome != ComparisonResult.DIFFERENT || !isGmlCoordinateTextComparison(comparison)) {
        return outcome;
      }
      return numericSequencesSimilar(
          comparison.getControlDetails().getValue(),
          comparison.getTestDetails().getValue(),
          tolerance) ? ComparisonResult.SIMILAR : outcome;
    };
  }

  /**
   * Check if XMLUnit is comparing text content of a GML coordinate-bearing element
   * @param comparison comparison to check
   * @return true when comparison relates to GML coordinate text
   */
  private static boolean isGmlCoordinateTextComparison(Comparison comparison) {
    if (comparison.getType() != ComparisonType.TEXT_VALUE) {
      return false;
    }
    String xPath = comparison.getControlDetails().getXPath();
    return containsElementInXPath(xPath, "pos")
        || containsElementInXPath(xPath, "posList")
        || containsElementInXPath(xPath, "coordinates");
  }

  /**
   * Check whether an XPath contains an element with the provided local name
   * @param xPath XPath to inspect
   * @param localName local name to find
   * @return true when present
   */
  private static boolean containsElementInXPath(String xPath, String localName) {
    if (xPath == null) {
      return false;
    }
    return xPath.contains(":" + localName + "[")
        || xPath.contains("/" + localName + "[")
        || xPath.contains("local-name()='" + localName + "'");
  }

  /**
   * Check whether two XML values contain equivalent numeric sequences within tolerance
   * @param value1 first value
   * @param value2 second value
   * @param tolerance absolute tolerance to apply
   * @return true when numeric sequences are the same length and values differ within tolerance
   */
  private static boolean numericSequencesSimilar(Object value1, Object value2, double tolerance) {
    List<Double> numbers1 = parseNumbers(value1);
    List<Double> numbers2 = parseNumbers(value2);
    if (numbers1.isEmpty() || numbers1.size() != numbers2.size()) {
      return false;
    }
    for (int index = 0; index < numbers1.size(); ++index) {
      if (Math.abs(numbers1.get(index) - numbers2.get(index)) > tolerance) {
        return false;
      }
    }
    return true;
  }

  /**
   * Parse all numeric values from an XML value
   * @param value XML value
   * @return parsed numeric values
   */
  private static List<Double> parseNumbers(Object value) {
    List<Double> result = new ArrayList<>();
    if (value == null) {
      return result;
    }
    var matcher = NUMBER_PATTERN.matcher(value.toString());
    while (matcher.find()) {
      result.add(Double.parseDouble(matcher.group()));
    }
    return result;
  }

  /**
   * Dummy constructor as never instantiated
   */
  private PlanitAssertionUtils() {
    // compliance to avoid javadoc warnings
  }

  /**
   * Assert that network files in location are similar
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertNetworkFilesSimilar(String resultDir, String referenceDir) throws IOException {
    String resultFile = pathOfNetworkFile(resultDir).toString();
    String referenceFile = pathOfNetworkFile(referenceDir).toString();

    assertXmlFileContentSimilar(resultFile, referenceFile);
  }

  /**
   * Assert that network files in location are similar while allowing small numeric differences in GML coordinates
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertNetworkFilesSimilarWithGmlCoordinateTolerance(String resultDir, String referenceDir)
      throws IOException {
    assertNetworkFilesSimilarWithGmlCoordinateTolerance(resultDir, referenceDir, DEFAULT_GML_COORDINATE_TOLERANCE);
  }

  /**
   * Assert that network files in location are similar while allowing small numeric differences in GML coordinates
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @param gmlCoordinateTolerance absolute tolerance to apply to GML coordinate values
   * @throws IOException when error
   */
  public static void assertNetworkFilesSimilarWithGmlCoordinateTolerance(
      String resultDir, String referenceDir, double gmlCoordinateTolerance) throws IOException {
    String resultFile = pathOfNetworkFile(resultDir).toString();
    String referenceFile = pathOfNetworkFile(referenceDir).toString();

    assertXmlFileContentSimilarWithGmlCoordinateTolerance(resultFile, referenceFile, gmlCoordinateTolerance);
  }

  /**
   * Assert that network files in location are similar
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertNetworkFilesSimilar(Path resultDir, Path referenceDir) throws IOException {
    assertNetworkFilesSimilar(resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
  }

  /**
   * Assert that network files in location are similar while allowing small numeric differences in GML coordinates
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertNetworkFilesSimilarWithGmlCoordinateTolerance(Path resultDir, Path referenceDir)
      throws IOException {
    assertNetworkFilesSimilarWithGmlCoordinateTolerance(
        resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
  }

  /**
   * Assert that network files in location are similar while allowing small numeric differences in GML coordinates
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @param gmlCoordinateTolerance absolute tolerance to apply to GML coordinate values
   * @throws IOException when error
   */
  public static void assertNetworkFilesSimilarWithGmlCoordinateTolerance(
      Path resultDir, Path referenceDir, double gmlCoordinateTolerance) throws IOException {
    assertNetworkFilesSimilarWithGmlCoordinateTolerance(
        resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString(), gmlCoordinateTolerance);
  }

  /**
   * Assert that zoning files in location are similar
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertZoningFilesSimilar(String resultDir, String referenceDir) throws IOException {
    String resultFile = pathOfZoningFile(resultDir).toString();
    String referenceFile = pathOfZoningFile(referenceDir).toString();

    assertXmlFileContentSimilar(resultFile, referenceFile);
  }

  /**
   * Assert that zoning files in location are similar while allowing small numeric differences in GML coordinates
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertZoningFilesSimilarWithGmlCoordinateTolerance(String resultDir, String referenceDir)
      throws IOException {
    assertZoningFilesSimilarWithGmlCoordinateTolerance(resultDir, referenceDir, DEFAULT_GML_COORDINATE_TOLERANCE);
  }

  /**
   * Assert that zoning files in location are similar while allowing small numeric differences in GML coordinates
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @param gmlCoordinateTolerance absolute tolerance to apply to GML coordinate values
   * @throws IOException when error
   */
  public static void assertZoningFilesSimilarWithGmlCoordinateTolerance(
      String resultDir, String referenceDir, double gmlCoordinateTolerance) throws IOException {
    String resultFile = pathOfZoningFile(resultDir).toString();
    String referenceFile = pathOfZoningFile(referenceDir).toString();

    assertXmlFileContentSimilarWithGmlCoordinateTolerance(resultFile, referenceFile, gmlCoordinateTolerance);
  }

  /**
   * Assert that zoning files in location are similar
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertZoningFilesSimilar(Path resultDir, Path referenceDir) throws IOException {
    assertZoningFilesSimilar(resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
  }

  /**
   * Assert that zoning files in location are similar while allowing small numeric differences in GML coordinates
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertZoningFilesSimilarWithGmlCoordinateTolerance(Path resultDir, Path referenceDir)
      throws IOException {
    assertZoningFilesSimilarWithGmlCoordinateTolerance(
        resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
  }

  /**
   * Assert that zoning files in location are similar while allowing small numeric differences in GML coordinates
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @param gmlCoordinateTolerance absolute tolerance to apply to GML coordinate values
   * @throws IOException when error
   */
  public static void assertZoningFilesSimilarWithGmlCoordinateTolerance(
      Path resultDir, Path referenceDir, double gmlCoordinateTolerance) throws IOException {
    assertZoningFilesSimilarWithGmlCoordinateTolerance(
        resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString(), gmlCoordinateTolerance);
  }

  /**
   * Assert that services network files in location are similar
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertServiceNetworkFilesSimilar(String resultDir, String referenceDir) throws IOException {
    String resultFile = pathOfServiceNetworkFile(resultDir).toString();
    String referenceFile = pathOfServiceNetworkFile(referenceDir).toString();

    assertXmlFileContentSimilar(resultFile, referenceFile);
  }

  /**
   * Assert that network files in location are similar
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertServiceNetworkFilesSimilar(Path resultDir, Path referenceDir) throws IOException {
    assertServiceNetworkFilesSimilar(resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
  }

  /**
   * Assert that route services files in location are similar
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertRoutedServicesFilesSimilar(String resultDir, String referenceDir) throws IOException {
    String resultFile = pathOfRoutedServicesFile(resultDir).toString();
    String referenceFile = pathOfRoutedServicesFile(referenceDir).toString();

    assertXmlFileContentSimilar(resultFile, referenceFile);
  }

  /**
   * Assert that route services files in location are similar
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertRoutedServicesFilesSimilar(Path resultDir, Path referenceDir) throws IOException {
    assertRoutedServicesFilesSimilar(resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
  }

  /**
   * Assert that demands files in location are similar
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertDemandsFilesSimilar(String resultDir, String referenceDir) throws IOException {
    String resultFile = pathOfDemandsFile(resultDir).toString();
    String referenceFile = pathOfDemandsFile(referenceDir).toString();

    assertXmlFileContentSimilar(resultFile, referenceFile);
  }

  /**
   * Assert that demands files in location are similar
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertDemandsFilesSimilar(Path resultDir, Path referenceDir) throws IOException {
    assertDemandsFilesSimilar(resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
  }
}
