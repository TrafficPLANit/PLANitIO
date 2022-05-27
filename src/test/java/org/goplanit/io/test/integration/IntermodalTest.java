package org.goplanit.io.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Path;
import java.util.logging.Logger;

import org.goplanit.demands.Demands;
import org.goplanit.io.input.PlanItInputBuilder;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.ConnectoidType;
import org.goplanit.utils.zoning.TransferZone;
import org.goplanit.utils.zoning.TransferZoneType;
import org.goplanit.zoning.Zoning;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * JUnit test cases for explanatory tests for TraditionalStaticAssignment
 * 
 * @author markr
 *
 */
public class IntermodalTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final Path testCasePath = Path.of("src","test","resources","testcases");
  private static final Path intermodalTestCasePath = Path.of(testCasePath.toString(),"intermodal", "xml");


  @SuppressWarnings("unused")
  private static final String odZone1XmlId = "1";
  @SuppressWarnings("unused")
  private static final String odZone2XmlId = "2";
  
  private static final String transferZoneStop1XmlId = "stop_1";
  private static final String transferZoneStop2XmlId = "stop_2";
  private static final String transferZoneStop3XmlId = "stop_3";
  
  private static final String linkSegment1XmlId = "1";
  private static final String linkSegment3XmlId = "3";    
  
  private static final String node1XmlId = "1";
  private static final String node2XmlId = "2";
  private static final String node3XmlId = "3";  
  
  private static final String transferconnectoid1XmlId = "transfer1";
  private static final String transferconnectoid2XmlId = "transfer2";
  private static final String transferconnectoid3XmlId = "transfer3";    
     

  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(IntermodalTest.class);
    } 
  }
  
  @Before
  public void beforeTest() {
  }  

  @AfterClass
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }
 
  /**
   * test case which only tries to parse a minimal intermodal setup (without routes), does not run an assignment
   * but simply tests the correct parsing of the inputs 
   */
  @Test
  public void test_intermodal_minimal_input() {
    try {
      final String projectPath = Path.of(intermodalTestCasePath.toString(),"minimal_input").toString();

      final PlanItInputBuilder planItInputBuilder = new PlanItInputBuilder(projectPath);
      final CustomPlanItProject project = new CustomPlanItProject(planItInputBuilder);

      /* NETWORK */
      final LayeredNetwork<?,?> network = project.createAndRegisterInfrastructureNetwork(MacroscopicNetwork.class.getCanonicalName());

      assertEquals(network.getTransportLayers().size(), 1);
      assertEquals(network.getModes().size(), 2);
      assertEquals(network.getModes().containsPredefinedMode(PredefinedModeType.CAR), true);
      assertEquals(network.getModes().containsPredefinedMode(PredefinedModeType.BUS), true);

      /* only single layer for both modes */
      assertTrue((network instanceof MacroscopicNetwork));
      MacroscopicNetwork macroNetwork = MacroscopicNetwork.class.cast(network);      
      assertEquals(macroNetwork.getLayerByMode(macroNetwork.getModes().get(0)),macroNetwork.getLayerByMode(macroNetwork.getModes().get(1)));
      
      NetworkLayer layer = macroNetwork.getLayerByMode(macroNetwork.getModes().get(0));
      assertEquals(layer.getXmlId(),"road");
      assertFalse(!(layer instanceof MacroscopicNetworkLayerImpl));
      MacroscopicNetworkLayerImpl macroNetworklayer = (MacroscopicNetworkLayerImpl) layer;
      assertEquals(macroNetworklayer.getNumberOfNodes(),3);
      assertEquals(macroNetworklayer.getNumberOfLinks(),2);
      assertEquals(macroNetworklayer.getNumberOfLinkSegments(),4);
      assertEquals(macroNetworklayer.getSupportedModes().size(),2);
      
      /* ZONING */
      final Zoning zoning = project.createAndRegisterZoning(network);
      assertEquals(zoning.getOdZones().size(),2);
      assertEquals(zoning.getTransferZones().size(),3);
      assertEquals(zoning.getNumberOfCentroids(),5); /* defaults should have been created */
      assertEquals(zoning.getNumberOfConnectoids(),5); /* one per zone + one transfer connectoid per node */
      
      for(var odConnectoid : zoning.getOdConnectoids()) {
        assertEquals(odConnectoid.getAccessZones().size(),1);
        assertEquals(odConnectoid.isModeAllowed(odConnectoid.getFirstAccessZone(), network.getModes().get(PredefinedModeType.CAR)),true);
        assertEquals(odConnectoid.isModeAllowed(odConnectoid.getFirstAccessZone(), network.getModes().get(PredefinedModeType.BUS)),true);
        assertEquals(odConnectoid.getLengthKm(odConnectoid.getFirstAccessZone()).get(),1,Precision.EPSILON_6);
      }
      for(var transferConnectoid : zoning.getTransferConnectoids()) {      
        assertEquals(transferConnectoid.getAccessZones().size(),1);
        assertEquals(transferConnectoid.isModeAllowed(transferConnectoid.getFirstAccessZone(), network.getModes().get(PredefinedModeType.CAR)),false);
        assertEquals(transferConnectoid.isModeAllowed(transferConnectoid.getFirstAccessZone(), network.getModes().get(PredefinedModeType.BUS)),true);
        assertEquals(transferConnectoid.getType(), ConnectoidType.PT_VEHICLE_STOP);
        assertEquals(transferConnectoid.getLengthKm(transferConnectoid.getFirstAccessZone()).get(),Connectoid.DEFAULT_LENGTH_KM,Precision.EPSILON_6);
        
        switch (transferConnectoid.getAccessLinkSegment().getXmlId()) {
        case linkSegment1XmlId:
            if(transferConnectoid.getAccessNode().getXmlId().equals(node1XmlId)) {
              assertEquals(transferConnectoid.getXmlId(),transferconnectoid1XmlId);
              assertEquals(transferConnectoid.getFirstAccessZone().getXmlId(),transferZoneStop1XmlId);
              assertEquals(((TransferZone)transferConnectoid.getFirstAccessZone()).getTransferZoneType(),TransferZoneType.PLATFORM);
            }else {
              assertEquals(transferConnectoid.getAccessNode().getXmlId(),node2XmlId);
              assertEquals(transferConnectoid.getXmlId(),transferconnectoid2XmlId);
              assertEquals(transferConnectoid.getFirstAccessZone().getXmlId(),transferZoneStop2XmlId);
              assertEquals(((TransferZone)transferConnectoid.getFirstAccessZone()).getTransferZoneType(),TransferZoneType.POLE);
            }
          break;
        case linkSegment3XmlId:
          assertEquals(transferConnectoid.getAccessNode().getXmlId(),node3XmlId);
          assertEquals(transferConnectoid.getXmlId(),transferconnectoid3XmlId);
          assertEquals(transferConnectoid.getFirstAccessZone().getXmlId(),transferZoneStop3XmlId);
          assertEquals(((TransferZone)transferConnectoid.getFirstAccessZone()).getTransferZoneType(),TransferZoneType.NONE);
          break;
        default:
          break;
        }      
      }
      
      /* DEMANDS */
      @SuppressWarnings("unused") final Demands demands = project.createAndRegisterDemands(zoning, network);      
      
      } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
      }
    }
  }
  