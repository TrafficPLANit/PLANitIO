package org.planit.io.test.integration;

import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.io.converter.intermodal.PlanitIntermodalReader;
import org.planit.io.converter.intermodal.PlanitIntermodalReaderFactory;
import org.planit.io.converter.network.PlanitNetworkReaderFactory;
import org.planit.io.converter.network.PlanitServiceNetworkReader;
import org.planit.io.converter.network.PlanitServiceNetworkReaderFactory;
import org.planit.io.demo.PLANitStaticAssignmentProjectDemos;
import org.planit.logging.Logging;
import org.planit.network.MacroscopicNetwork;
import org.planit.network.ServiceNetwork;
import org.planit.network.TransportLayerNetwork;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.misc.Pair;
import org.planit.zoning.Zoning;

/**
 * Test being able to read and write service networks
 * 
 * @author markr
 *
 */
public class ServiceNetworkTest {

  /** the logger */
  private static Logger LOGGER = null;

  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(ServiceNetworkTest.class);
    } 
  }

  @AfterClass
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }
  
  /**
   * TODO: in the future include a service network example in the getting started but as long as we do not have a PT assignment module
   * this is quite pointless. For now however we already put the resources under the getting started to remind ourselves this shoudl happen
   * at some point 
   */
  @Test
  public void gettingStartedTestWithServices() {
    try {
      final String INPUT_DIR = "src\\test\\resources\\testcases\\getting_started\\service";
      
      //TODO: for now no reader capable of parsing network, zoning and services exists, so instead use intermodal 
      // + separate service network reader. Ideally, we would also have one that can include service network as well
      //   do this after we also have routed services reader so we have a better overview on this
      PlanitIntermodalReader intermodalReader = PlanitIntermodalReaderFactory.create();
      intermodalReader.getSettings().setInputDirectory(INPUT_DIR);      
      Pair<MacroscopicNetwork, Zoning> resultPair = intermodalReader.read();
      
      
      PlanitServiceNetworkReader serviceNetworkReader = PlanitServiceNetworkReaderFactory.create(INPUT_DIR, resultPair.first());
      
      ServiceNetwork serviceNetwork = serviceNetworkReader.read();
      
      //TODO: add assertions on result!
      
    }catch(Exception e){
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }  

}
