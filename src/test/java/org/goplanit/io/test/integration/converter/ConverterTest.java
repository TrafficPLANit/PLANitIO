package org.goplanit.io.test.integration.converter;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.goplanit.converter.intermodal.IntermodalConverterFactory;
import org.goplanit.converter.network.NetworkConverterFactory;
import org.goplanit.io.converter.intermodal.PlanitIntermodalReader;
import org.goplanit.io.converter.intermodal.PlanitIntermodalReaderFactory;
import org.goplanit.io.converter.intermodal.PlanitIntermodalWriter;
import org.goplanit.io.converter.intermodal.PlanitIntermodalWriterFactory;
import org.goplanit.io.converter.network.PlanitNetworkReader;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.io.converter.network.PlanitNetworkWriter;
import org.goplanit.io.converter.network.PlanitNetworkWriterFactory;
import org.goplanit.io.test.integration.TestBase;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.matchers.CompareMatcher;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * JUnit test cases for the converters provided in the PLANit native format
 * 
 * @author markr
 *
 */
public class ConverterTest extends TestBase {

  /** the logger */
  private static Logger LOGGER = null;


  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(ConverterTest.class);
    } 
  }

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }
  
  /**
   * Test that reading a PLANit network in native format and then writing it results in the same
   * files as the original input that was read.
   */
  @Test
  public void testPlanit2PlanitNetworkConverter() {
    try {
      final String projectPath = Path.of(TEST_CASE_PATH.toString(),"converter_test").toString();
      final String inputPath = Path.of(projectPath, "input").toString();
      
      /* reader */
      PlanitNetworkReader planitReader = PlanitNetworkReaderFactory.create();
      planitReader.getSettings().setInputDirectory(inputPath);
      
      /* writer */
      PlanitNetworkWriter planitWriter = PlanitNetworkWriterFactory.create(projectPath, CountryNames.AUSTRALIA);
      
      /* convert */
      NetworkConverterFactory.create(planitReader, planitWriter).convert();
      
      /* use non-deprecated hamcrest version instead of junit for comparing */
      org.hamcrest.MatcherAssert.assertThat(
          /* xml unit functionality comparing the two files */
          Input.fromFile(Path.of(projectPath, "network.xml").toString()),
          CompareMatcher.isSimilarTo(Input.fromFile(Path.of(inputPath,"network.xml").toString())));
      
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail();
    }
  }
  
  /**
   * Test that reading a planit intermodal network (network and (pt) zoning) in native format and then writing it results in the same
   * files as the original input that was read.
   */
  @Test
  public void testPlanit2PlanitIntermodalNoServicesConverter() {
    try {
      final String projectPath = Path.of(TEST_CASE_PATH.toString(),"converter_test").toString();
      final String inputPath = Path.of(projectPath, "input").toString();
      
      /* reader */
      PlanitIntermodalReader planitReader = PlanitIntermodalReaderFactory.create();
      planitReader.getSettings().setInputDirectory(inputPath);
      
      /* writer */
      PlanitIntermodalWriter planitWriter = PlanitIntermodalWriterFactory.create(projectPath, CountryNames.AUSTRALIA);
      
      /* convert */
      IntermodalConverterFactory.create(planitReader, planitWriter).convert();
      
      /* use non-deprecated hamcrest version instead of junit for comparing */
      org.hamcrest.MatcherAssert.assertThat(
          /* xml unit functionality comparing the two files */
          Input.fromFile(Path.of(projectPath, "network.xml").toString()),
          CompareMatcher.isSimilarTo(Input.fromFile(Path.of(inputPath,"network.xml").toString())));
      
      /* use non-deprecated hamcrest version instead of junit for comparing */
      org.hamcrest.MatcherAssert.assertThat(
          /* xml unit functionality comparing the two files */
          Input.fromFile(Path.of(projectPath,"zoning.xml").toString()),
          CompareMatcher.isSimilarTo(Input.fromFile(Path.of(inputPath,"zoning.xml").toString())));
      
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Test that reading a planit intermodal network (network and (pt) zoning) in native format and then writing it results in the same
   * files as the original input that was read.
   */
  @Test
  public void testPlanit2PlanitIntermodalServicesConverter() {
    try {
      final String projectPath = Path.of(TEST_CASE_PATH.toString(),"converter_test").toString();
      final String inputPath = Path.of(projectPath, "input").toString();

      /* reader */
      PlanitIntermodalReader planitReader = PlanitIntermodalReaderFactory.create(inputPath);

      /* writer */
      PlanitIntermodalWriter planitWriter = PlanitIntermodalWriterFactory.create(projectPath, CountryNames.AUSTRALIA);

      /* convert */
      IntermodalConverterFactory.create(planitReader, planitWriter).convertWithServices();

      /* network and zoning already tested in #test_planit_2_planit_intermodal_no_services_converter

      /* use non-deprecated hamcrest version instead of junit for comparing */
      org.hamcrest.MatcherAssert.assertThat(
          /* xml unit functionality comparing the two files */
          Input.fromFile(Path.of(projectPath,"service_network.xml").toString()),
          CompareMatcher.isSimilarTo(Input.fromFile(Path.of(inputPath,"service_network.xml").toString())));

      /* use non-deprecated hamcrest version instead of junit for comparing */
      org.hamcrest.MatcherAssert.assertThat(
          /* xml unit functionality comparing the two files */
          Input.fromFile(Path.of(projectPath,"routed_services.xml").toString()),
          CompareMatcher.isSimilarTo(Input.fromFile(Path.of(inputPath,"routed_services.xml").toString())));

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Test that a factory-created intermodal reader and writer are reusable. Factory-created readers own the PLANit
   * entities they populate, so reusing the reader is expected to create fresh entities for each conversion pass.
   */
  @Test
  public void testPlanit2PlanitIntermodalConverterReuse() {
    try {
      final String projectPath = Path.of(TEST_CASE_PATH.toString(),"converter_test").toString();
      final String inputPath = Path.of(projectPath, "input").toString();

      /* reader */
      PlanitIntermodalReader planitReader = PlanitIntermodalReaderFactory.create(inputPath);

      /* writer */
      PlanitIntermodalWriter planitWriter = PlanitIntermodalWriterFactory.create(projectPath, CountryNames.AUSTRALIA);

      /* convert twice with the same converter-backed reader/writer pair */
      var converter = IntermodalConverterFactory.create(planitReader, planitWriter);
      converter.convert();
      converter.convertWithServices();

      org.hamcrest.MatcherAssert.assertThat(
          Input.fromFile(Path.of(projectPath, "network.xml").toString()),
          CompareMatcher.isSimilarTo(Input.fromFile(Path.of(inputPath,"network.xml").toString())));

      org.hamcrest.MatcherAssert.assertThat(
          Input.fromFile(Path.of(projectPath,"zoning.xml").toString()),
          CompareMatcher.isSimilarTo(Input.fromFile(Path.of(inputPath,"zoning.xml").toString())));

      org.hamcrest.MatcherAssert.assertThat(
          Input.fromFile(Path.of(projectPath,"service_network.xml").toString()),
          CompareMatcher.isSimilarTo(Input.fromFile(Path.of(inputPath,"service_network.xml").toString())));

      org.hamcrest.MatcherAssert.assertThat(
          Input.fromFile(Path.of(projectPath,"routed_services.xml").toString()),
          CompareMatcher.isSimilarTo(Input.fromFile(Path.of(inputPath,"routed_services.xml").toString())));

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Test that a reader with caller-provided toPopulate entities is intentionally single-use. The reader cannot safely
   * infer whether appending the same input again is valid for caller-owned objects, so reuse must fail explicitly.
   */
  @Test
  public void testPlanit2PlanitIntermodalCallerProvidedToPopulateReuseThrows() {
    final String projectPath = Path.of(TEST_CASE_PATH.toString(),"converter_test").toString();
    final String inputPath = Path.of(projectPath, "input").toString();

    IdGroupingToken idToken = IdGroupingToken.collectGlobalToken();
    MacroscopicNetwork network = new MacroscopicNetwork(idToken);
    Zoning zoning = new Zoning(idToken, network.getNetworkGroupingTokenId());
    ServiceNetwork serviceNetwork = new ServiceNetwork(idToken, network);
    RoutedServices routedServices = new RoutedServices(idToken, serviceNetwork);

    PlanitIntermodalReader planitReader = PlanitIntermodalReaderFactory.create(
        inputPath,
        ".xml",
        network,
        zoning,
        serviceNetwork,
        routedServices);
    PlanitIntermodalWriter planitWriter = PlanitIntermodalWriterFactory.create(projectPath, CountryNames.AUSTRALIA);
    var converter = IntermodalConverterFactory.create(planitReader, planitWriter);

    try {
      converter.convert();
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail();
    }

    assertThrows(PlanItRunTimeException.class, converter::convertWithServices);
  }

}
