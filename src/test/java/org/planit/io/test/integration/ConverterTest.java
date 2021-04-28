package org.planit.io.test.integration;

import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.converter.network.NetworkConverterFactory;
import org.planit.io.converter.network.PlanitNetworkReader;
import org.planit.io.converter.network.PlanitNetworkReaderFactory;
import org.planit.io.converter.network.PlanitNetworkWriter;
import org.planit.io.converter.network.PlanitNetworkWriterFactory;
import org.planit.logging.Logging;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.locale.CountryNames;

/**
 * JUnit test cases for the converters provided in the Planit native format
 * 
 * @author markr
 *
 */
public class ConverterTest {

  /** the logger */
  private static Logger LOGGER = null;

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
   * Test that reading a planit network in native format and then writing it results in the same
   * files as the original input that was read.
   */
  @Test
  public void test_planit_2_planit_network_converter() {
    try {
      final String projectPath = "src\\test\\resources\\testcases\\converter_test";
      final String inputPath = projectPath+"\\input";
      
      /* reader */
      PlanitNetworkReader planitReader = PlanitNetworkReaderFactory.create();
      planitReader.getSettings().setInputDirectory(inputPath);
      
      /* writer */
      PlanitNetworkWriter planitWriter = PlanitNetworkWriterFactory.create(projectPath, CountryNames.AUSTRALIA);
      
      /* convert */
      NetworkConverterFactory.create(planitReader, planitWriter).convert();
      
      fail("Exception for duplicate link segment type external Id was not thrown");
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
      fail();
    }
  }
}