package org.planit.io.test.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.io.converter.network.PlanitNetworkReader;
import org.planit.io.converter.network.PlanitNetworkReaderFactory;
import org.planit.io.converter.service.PlanitRoutedServicesReader;
import org.planit.io.converter.service.PlanitRoutedServicesReaderFactory;
import org.planit.io.converter.service.PlanitServiceNetworkReader;
import org.planit.io.converter.service.PlanitServiceNetworkReaderFactory;
import org.planit.logging.Logging;
import org.planit.network.MacroscopicNetwork;
import org.planit.network.ServiceNetwork;
import org.planit.service.routed.RoutedModeServices;
import org.planit.service.routed.RoutedServices;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.mode.PredefinedModeType;

/**
 * Test being able to read and write routed services on top of a service network
 * 
 * @author markr
 *
 */
public class RoutedServicesTest {

  /** the logger */
  private static Logger LOGGER = null;

  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(RoutedServicesTest.class);
    } 
  }

  @AfterClass
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }
  
  /**
   * TODO: in the future include a routed services example in the getting started but as long as we do not have a PT assignment module
   * this is quite pointless. For now however we already put the resources under the getting started to remind ourselves this should happen
   * at some point 
   */
  @Test
  public void gettingStartedTestWithRoutedServices() {
    try {
      final String INPUT_DIR = "src\\test\\resources\\testcases\\getting_started\\service";
      
      /* parent network */
      PlanitNetworkReader networkReader = PlanitNetworkReaderFactory.create();
      networkReader.getSettings().setInputDirectory(INPUT_DIR);      
      MacroscopicNetwork parentNetwork = networkReader.read();
      
      /* the service network */
      PlanitServiceNetworkReader serviceNetworkReader = PlanitServiceNetworkReaderFactory.create(INPUT_DIR, parentNetwork);      
      ServiceNetwork serviceNetwork = serviceNetworkReader.read();
      
      /* the routed services */
      PlanitRoutedServicesReader routedServicesReader = PlanitRoutedServicesReaderFactory.create(INPUT_DIR, serviceNetwork);      
      RoutedServices routedServices = routedServicesReader.read();       
      
      /* general tests on the routed service top-level classes */
      assertTrue(routedServices.getLayers().size()==1);
      assertTrue(routedServices.getLayers().getFirst().getParentLayer().getXmlId().equals(serviceNetwork.getXmlId()));
      assertTrue(parentNetwork.getModes().get(PredefinedModeType.BUS)!=null);
      assertTrue(routedServices.getLayers().getFirst().getServicesByMode(parentNetwork.getModes().get(PredefinedModeType.BUS))!=null);
      RoutedModeServices busServices = routedServices.getLayers().getFirst().getServicesByMode(parentNetwork.getModes().get(PredefinedModeType.BUS));
      assertTrue(busServices.getMode().equals(parentNetwork.getModes().get(PredefinedModeType.BUS)));
      assertTrue(busServices.size()==2);
      /* run assertions on the service entries themselves*/
      //TODO
      
    }catch(Exception e){
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }  

}
