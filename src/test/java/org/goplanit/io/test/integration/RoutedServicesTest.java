package org.goplanit.io.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.logging.Logger;

import org.goplanit.io.converter.network.PlanitNetworkReader;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.io.converter.service.PlanitRoutedServicesReader;
import org.goplanit.io.converter.service.PlanitRoutedServicesReaderFactory;
import org.goplanit.io.converter.service.PlanitServiceNetworkReader;
import org.goplanit.io.converter.service.PlanitServiceNetworkReaderFactory;
import org.goplanit.io.input.PlanItInputBuilder;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.Network;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.utils.service.routed.*;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.PredefinedModeType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test being able to read and write routed services on top of a service network
 * 
 * @author markr
 *
 */
public class RoutedServicesTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path testCasePath = Path.of("src","test","resources","testcases");
  private static final Path routedServicesTestCasePath = Path.of(testCasePath.toString(),"getting_started", "service");

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
      /* parent network */
      PlanitNetworkReader networkReader = PlanitNetworkReaderFactory.create();
      networkReader.getSettings().setInputDirectory(routedServicesTestCasePath.toString());
      MacroscopicNetwork parentNetwork = networkReader.read();
      
      /* the service network */
      PlanitServiceNetworkReader serviceNetworkReader = PlanitServiceNetworkReaderFactory.create(routedServicesTestCasePath.toString(), parentNetwork);
      ServiceNetwork serviceNetwork = serviceNetworkReader.read();
      
      /* the routed services */
      PlanitRoutedServicesReader routedServicesReader = PlanitRoutedServicesReaderFactory.create(routedServicesTestCasePath.toString(), serviceNetwork);
      RoutedServices routedServices = routedServicesReader.read();       
      
      /* general tests on the routed service top-level classes */
      assertTrue(routedServices.getLayers().size()==1);
      assertTrue(routedServices.getParentNetwork().getXmlId().equals(serviceNetwork.getXmlId()));
      assertTrue(routedServices.getLayers().getFirst().getParentLayer().getXmlId().equals(serviceNetwork.getTransportLayers().getFirst().getXmlId()));
      assertTrue(parentNetwork.getModes().get(PredefinedModeType.BUS)!=null);
      assertTrue(routedServices.getLayers().getFirst().getServicesByMode(parentNetwork.getModes().get(PredefinedModeType.BUS))!=null);
      RoutedModeServices busServices = routedServices.getLayers().getFirst().getServicesByMode(parentNetwork.getModes().get(PredefinedModeType.BUS));
      assertTrue(busServices.getMode().equals(parentNetwork.getModes().get(PredefinedModeType.BUS)));
      assertTrue(busServices.size()==2);
      /* run assertions on the service entries themselves*/
      
      RoutedService line4Service = busServices.findFirst( service -> service.getXmlId().equals("line_4"));
      assertNotNull(line4Service);
      assertEquals(line4Service.getName(),"4");
      assertEquals(line4Service.getNameDescription(),"city to beach");
      assertEquals(line4Service.getServiceDescription(),"bus line running from the city to the beach directly");
      assertNotNull(line4Service.getTripInfo());
      RoutedServiceTripInfo line4TripInfo = line4Service.getTripInfo();
      assertEquals(line4TripInfo.hasScheduleBasedTrips(),true);
      assertEquals(line4TripInfo.hasFrequencyBasedTrips(),true);
      assertNotNull(line4TripInfo.getFrequencyBasedTrips());
      RoutedTripsFrequency line4FrequencyTrips = line4TripInfo.getFrequencyBasedTrips();
      assertEquals(line4FrequencyTrips.size(),1);
      assertNotNull(line4FrequencyTrips.getFirst());
      RoutedTripFrequency frequencyEntry = line4FrequencyTrips.getFirst();
      assertEquals(frequencyEntry.getFrequencyPerHour(),3, Precision.EPSILON_6);
      assertEquals(frequencyEntry.getNumberOfLegSegments(),1);
      assertTrue(frequencyEntry.getFirstLegSegment().equals(frequencyEntry.getLastLegSegment()));
      assertTrue(frequencyEntry.getFirstLegSegment().equals(frequencyEntry.getLegSegment(0)));
      
      RoutedService line4OppService = busServices.findFirst( service -> service.getXmlId().equals("line_4_opp"));
      RoutedServiceTripInfo line4OppTripInfo = line4OppService.getTripInfo();
      assertNotNull(line4OppTripInfo.getScheduleBasedTrips());
      RoutedTripsSchedule line4OppScheduledTrips = line4OppTripInfo.getScheduleBasedTrips();
      assertEquals(line4OppScheduledTrips.size(),1);
      assertNotNull(line4OppScheduledTrips.getFirst());
      RoutedTripSchedule scheduleEntry = line4OppScheduledTrips.getFirst();
      assertNotNull(scheduleEntry.getDepartures());
      RoutedTripDepartures scheduleDepartures = scheduleEntry.getDepartures();
      assertEquals(scheduleDepartures.size(),3);
      assertNotNull(scheduleDepartures.findFirst( dep -> dep.getXmlId().equals("dep1")));
      assertNotNull(scheduleDepartures.findFirst( dep -> dep.getXmlId().equals("dep2")));
      assertNotNull(scheduleDepartures.findFirst( dep -> dep.getXmlId().equals("dep3")));
      assertEquals(scheduleEntry.getRelativeLegTimingsSize(),1);
      assertNotNull(scheduleEntry.getRelativeLegTiming(0));
      RelativeLegTiming relTiming = scheduleEntry.getRelativeLegTiming(0);
      assertEquals(relTiming.getDuration(),LocalTime.of(0, 3));
      assertEquals(relTiming.getDwellTime(),LocalTime.of(0, 2));
      assertNotNull(relTiming.getParentLegSegment());
      assertEquals(relTiming.getParentLegSegment().getXmlId(),"ls2");

      
    }catch(Exception e){
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }  
  
  /**
   * Instead of using converters, we go through a PLANit project instead, which is was most users would likely do. Here we simply test
   * if this process works without throwing exceptions and yielding non-null results.
   */
  @Test
  public void routedServicesViaPlanitProject() {  

    try {
      final CustomPlanItProject project = new CustomPlanItProject(new PlanItInputBuilder(routedServicesTestCasePath.toString()));
      
      /* physical network needed for service network... */
      MacroscopicNetwork network = (MacroscopicNetwork) project.createAndRegisterInfrastructureNetwork(Network.MACROSCOPIC_NETWORK);
      assertNotNull(network);
      /* service network needed for routed services... */
      ServiceNetwork serviceNetwork = project.createAndRegisterServiceNetwork(network);
      assertNotNull(serviceNetwork);
      assertFalse(serviceNetwork.getTransportLayers().isEmpty());
      RoutedServices routedServices = project.createAndRegisterRoutedServices(serviceNetwork);
      assertNotNull(routedServices);
      assertFalse(routedServices.getLayers().isEmpty());

    }catch(Exception e) {
      e.printStackTrace();
      LOGGER.severe(e.getMessage());      
      fail();
    }
  }

}
