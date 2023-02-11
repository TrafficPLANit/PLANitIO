package org.goplanit.io.test.integration;

import static org.junit.Assert.fail;

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
import org.goplanit.logging.Logging;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.locale.CountryNames;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.matchers.CompareMatcher;

/**
 * JUnit test cases for the converters provided in the Planit native format
 * 
 * @author markr
 *
 */
public class ConverterTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path testCasePath = Path.of("src","test","resources","testcases");

  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(ConverterTest.class);
    } 
  }

  @AfterClass
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }
  
  /**
   * Test that reading a PLANit network in native format and then writing it results in the same
   * files as the original input that was read.
   */
  @Test
  public void test_planit_2_planit_network_converter() {
    try {
      final String projectPath = Path.of(testCasePath.toString(),"converter_test").toString();
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
  public void test_planit_2_planit_intermodal_no_services_converter() {
    try {
      final String projectPath = Path.of(testCasePath.toString(),"converter_test").toString();
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

  //todo --> add test with services to see if this works as well
}