package org.goplanit.io.test.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.goplanit.io.converter.network.PlanitNetworkReader;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.io.converter.service.PlanitServiceNetworkReader;
import org.goplanit.io.converter.service.PlanitServiceNetworkReaderFactory;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.utils.id.IdGenerator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test being able to read and write service networks
 * 
 * @author markr
 *
 */
public class ServiceNetworkTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path testCasePath = Path.of("src","test","resources","testcases");
  private static final Path serviceNetworkTestCasePath = Path.of(testCasePath.toString(),"getting_started", "service");

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
   * this is quite pointless. For now however we already put the resources under the getting started to remind ourselves this should happen
   * at some point 
   */
  @Test
  public void gettingStartedTestWithServices() {
    try {
      /* parent network */
      PlanitNetworkReader networkReader = PlanitNetworkReaderFactory.create();
      networkReader.getSettings().setInputDirectory(serviceNetworkTestCasePath.toString());
      MacroscopicNetwork parentNetwork = networkReader.read();
      
      /* the service network */
      PlanitServiceNetworkReader serviceNetworkReader = PlanitServiceNetworkReaderFactory.create(serviceNetworkTestCasePath.toString(), parentNetwork);
      ServiceNetwork serviceNetwork = serviceNetworkReader.read();
      
      /* tests */
      assertTrue(serviceNetwork.getTransportLayers().size()==1);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getServiceNodes().size()==2);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getServiceNodes().getByXmlId("s1")!=null);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getServiceNodes().getByXmlId("s2")!=null);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getLegs().size()==1);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getLegs().getByXmlId("l1")!=null);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getLegSegments().size()==2);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getLegSegments().getByXmlId("ls1")!=null);
      assertTrue(serviceNetwork.getTransportLayers().getFirst().getLegSegments().getByXmlId("ls1").getParentLeg().getXmlId().equals("l1"));
      
    }catch(Exception e){
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }  

}
