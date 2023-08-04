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

  private static Path pathOfNetworkFile(String theDir){
    return Path.of(theDir, PlanitNetworkWriterSettings.DEFAULT_NETWORK_XML).toAbsolutePath();
  }

  private static Path pathOfZoningFile(String theDir){
    return Path.of(theDir, PlanitZoningWriterSettings.DEFAULT_ZONING_XML).toAbsolutePath();
  }

  private static Path pathOfServiceNetworkFile(String theDir){
    return Path.of(theDir, PlanitServiceNetworkWriterSettings.DEFAULT_SERVICE_NETWORK_XML).toAbsolutePath();
  }

  private static Path pathOfRoutedServicesFile(String theDir){
    return Path.of(theDir, PlanitRoutedServicesWriterSettings.DEFAULT_ROUTED_SERVICES_XML).toAbsolutePath();
  }

  private static Path pathOfDemandsFile(String theDir) {
    return Path.of(theDir, PlanitDemandsWriterSettings.DEFAULT_DEMANDS_XML).toAbsolutePath();
  }

  private static void assertXmlFileContentSimilar(String file1, String file2) throws IOException {
    org.hamcrest.MatcherAssert.assertThat(
        /* xml unit functionality comparing the two files */
        FileUtils.parseUtf8FileContentAsString(file1),
        CompareMatcher.isSimilarTo(FileUtils.parseUtf8FileContentAsString(file2)));
  }

  public static void assertNetworkFilesSimilar(String resultDir, String referenceDir) throws IOException {
    String resultFile = pathOfNetworkFile(resultDir).toString();
    String referenceFile = pathOfNetworkFile(referenceDir).toString();

    assertXmlFileContentSimilar(resultFile, referenceFile);
  }

  public static void assertNetworkFilesSimilar(Path resultDir, Path referenceDir) throws IOException {
    assertNetworkFilesSimilar(resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
  }


  public static void assertZoningFilesSimilar(String resultDir, String referenceDir) throws IOException {
    String resultFile = pathOfZoningFile(resultDir).toString();
    String referenceFile = pathOfZoningFile(referenceDir).toString();

    assertXmlFileContentSimilar(resultFile, referenceFile);
  }

  public static void assertZoningFilesSimilar(Path resultDir, Path referenceDir) throws IOException {
    assertZoningFilesSimilar(resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
  }

  public static void assertServiceNetworkFilesSimilar(String resultDir, String referenceDir) throws IOException {
    String resultFile = pathOfServiceNetworkFile(resultDir).toString();
    String referenceFile = pathOfServiceNetworkFile(referenceDir).toString();

    assertXmlFileContentSimilar(resultFile, referenceFile);
  }

  public static void assertServiceNetworkFilesSimilar(Path resultDir, Path referenceDir) throws IOException {
    assertServiceNetworkFilesSimilar(resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
  }

  public static void assertRoutedServicesFilesSimilar(String resultDir, String referenceDir) throws IOException {
    String resultFile = pathOfRoutedServicesFile(resultDir).toString();
    String referenceFile = pathOfRoutedServicesFile(referenceDir).toString();

    assertXmlFileContentSimilar(resultFile, referenceFile);
  }

  public static void assertRoutedServicesFilesSimilar(Path resultDir, Path referenceDir) throws IOException {
    assertRoutedServicesFilesSimilar(resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
  }

  public static void assertDemandsFilesSimilar(String resultDir, String referenceDir) throws IOException {
    String resultFile = pathOfDemandsFile(resultDir).toString();
    String referenceFile = pathOfDemandsFile(referenceDir).toString();

    assertXmlFileContentSimilar(resultFile, referenceFile);
  }

  public static void assertDemandsFilesSimilar(Path resultDir, Path referenceDir) throws IOException {
    assertDemandsFilesSimilar(resultDir.toAbsolutePath().toString(), referenceDir.toAbsolutePath().toString());
  }
}
