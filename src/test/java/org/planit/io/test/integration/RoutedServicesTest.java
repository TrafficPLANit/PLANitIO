package org.planit.io.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalTime;
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
import org.planit.service.routed.RelativeLegTiming;
import org.planit.service.routed.RoutedModeServices;
import org.planit.service.routed.RoutedService;
import org.planit.service.routed.RoutedServiceTripInfo;
import org.planit.service.routed.RoutedServices;
import org.planit.service.routed.RoutedTripDepartures;
import org.planit.service.routed.RoutedTripFrequency;
import org.planit.service.routed.RoutedTripSchedule;
import org.planit.service.routed.RoutedTripsFrequency;
import org.planit.service.routed.RoutedTripsSchedule;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.math.Precision;
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
      
      assertNotNull(line4TripInfo.getScheduleBasedTrips());
      RoutedTripsSchedule line4ScheduledTrips = line4TripInfo.getScheduleBasedTrips();
      assertEquals(line4ScheduledTrips.size(),1);
      assertNotNull(line4ScheduledTrips.getFirst());
      RoutedTripSchedule scheduleEntry = line4ScheduledTrips.getFirst();
      assertNotNull(scheduleEntry.getDepartures());
      RoutedTripDepartures scheduleDepartures = scheduleEntry.getDepartures();
      assertEquals(scheduleDepartures.size(),2);
      assertNotNull(scheduleDepartures.findFirst( dep -> dep.getXmlId().equals("dep1")));
      assertNotNull(scheduleDepartures.findFirst( dep -> dep.getXmlId().equals("dep2")));
      assertEquals(scheduleEntry.getRelativeLegTimingsSize(),1);
      assertNotNull(scheduleEntry.getRelativeLegTiming(0));
      RelativeLegTiming relTiming = scheduleEntry.getRelativeLegTiming(0);
      assertEquals(relTiming.getDuration(),LocalTime.of(0, 3));
      assertEquals(relTiming.getDwellTime(),LocalTime.of(0, 1));
      assertNotNull(relTiming.getParentLegSegment());
      assertEquals(relTiming.getParentLegSegment().getXmlId(),"ls1");

      
    }catch(Exception e){
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      fail(e.getMessage());
    }
  }  

}
