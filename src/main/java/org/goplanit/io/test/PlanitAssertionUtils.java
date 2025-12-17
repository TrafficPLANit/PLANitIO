package org.goplanit.io.test;

import org.goplanit.io.converter.demands.PlanitDemandsWriterSettings;
import org.goplanit.io.converter.network.PlanitNetworkWriterSettings;
import org.goplanit.io.converter.service.PlanitRoutedServicesWriterSettings;
import org.goplanit.io.converter.service.PlanitServiceNetworkWriterSettings;
import org.goplanit.io.converter.zoning.PlanitZoningWriterSettings;
import org.goplanit.utils.misc.FileUtils;
import org.xmlunit.matchers.CompareMatcher;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Utilities for asserting PLANit outputs of integration tests
 */
public class PlanitAssertionUtils {

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
   * Assert that network files in location are similar
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertNetworkFilesSimilar(Path resultDir, Path referenceDir) throws IOException {
    assertNetworkFilesSimilar(resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
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
   * Assert that zoning files in location are similar
   * @param resultDir result location
   * @param referenceDir reference location to compare to
   * @throws IOException when error
   */
  public static void assertZoningFilesSimilar(Path resultDir, Path referenceDir) throws IOException {
    assertZoningFilesSimilar(resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
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
