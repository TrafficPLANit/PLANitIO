package org.planit.io.test.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.io.converter.network.PlanitNetworkReader;
import org.planit.io.converter.network.PlanitNetworkReaderFactory;
import org.planit.io.converter.service.PlanitServiceNetworkReader;
import org.planit.io.converter.service.PlanitServiceNetworkReaderFactory;
import org.planit.logging.Logging;
import org.planit.network.MacroscopicNetwork;
import org.planit.network.ServiceNetwork;
import org.planit.utils.id.IdGenerator;

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
      
      /* parent network */
      PlanitNetworkReader networkReader = PlanitNetworkReaderFactory.create();
      networkReader.getSettings().setInputDirectory(INPUT_DIR);      
      MacroscopicNetwork parentNetwork = networkReader.read();
      
      /* the service network */
      PlanitServiceNetworkReader serviceNetworkReader = PlanitServiceNetworkReaderFactory.create(INPUT_DIR, parentNetwork);      
      ServiceNetwork serviceNetwork = serviceNetworkReader.read();
      
      /* tests */
      assertTrue(serviceNetwork.getTransportLayers().size()==1);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getServiceNodes().size()==2);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getServiceNodes().getByXmlId("s1")!=null);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getServiceNodes().getByXmlId("s2")!=null);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getLegs().size()==1);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getLegs().getByXmlId("l1")!=null);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getLegSegments().size()==1);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getLegSegments().getByXmlId("ls1")!=null);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getLegSegments().getByXmlId("ls1").getParentLeg().getXmlId().equals("l1"));
      
    }catch(Exception e){
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }  

}
