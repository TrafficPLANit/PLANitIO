package org.planit.io.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.demands.Demands;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.ConnectoidType;
import org.planit.utils.zoning.DirectedConnectoid;
import org.planit.utils.zoning.TransferZone;
import org.planit.utils.zoning.TransferZoneType;
import org.planit.utils.zoning.UndirectedConnectoid;
import org.planit.zoning.Zoning;
import org.planit.logging.Logging;
import org.planit.network.InfrastructureLayer;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.macroscopic.MacroscopicNetwork;
import org.planit.network.macroscopic.physical.MacroscopicPhysicalNetwork;
import org.planit.project.CustomPlanItProject;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.math.Precision;
import org.planit.utils.mode.PredefinedModeType;

/**
 * JUnit test cases for explanatory tests for TraditionalStaticAssignment
 * 
 * @author markr
 *
 */
public class IntermodalTest {

  /** the logger */
  private static Logger LOGGER = null;
  
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
      String projectPath = "src\\test\\resources\\testcases\\intermodal\\xml\\minimal_input\\";
      
      final PlanItInputBuilder planItInputBuilder = new PlanItInputBuilder(projectPath);
      final CustomPlanItProject project = new CustomPlanItProject(planItInputBuilder);

      /* NETWORK */
      final InfrastructureNetwork network = project.createAndRegisterInfrastructureNetwork(MacroscopicNetwork.class.getCanonicalName());

      assertEquals(network.infrastructureLayers.size(), 1);
      assertEquals(network.modes.size(), 2);
      assertEquals(network.modes.containsPredefinedMode(PredefinedModeType.CAR), true);
      assertEquals(network.modes.containsPredefinedMode(PredefinedModeType.BUS), true);

      /* only single layer for both modes */
      assertTrue((network instanceof MacroscopicNetwork));
      MacroscopicNetwork macroNetwork = MacroscopicNetwork.class.cast(network);      
      assertEquals(macroNetwork.getInfrastructureLayerByMode(macroNetwork.modes.get(0)),macroNetwork.getInfrastructureLayerByMode(macroNetwork.modes.get(1)));
      
      InfrastructureLayer layer = macroNetwork.getInfrastructureLayerByMode(macroNetwork.modes.get(0));
      assertEquals(layer.getXmlId(),"road");
      assertFalse(!(layer instanceof MacroscopicPhysicalNetwork));
      MacroscopicPhysicalNetwork macroNetworklayer = (MacroscopicPhysicalNetwork) layer;
      assertEquals(macroNetworklayer.nodes.size(),3);
      assertEquals(macroNetworklayer.links.size(),2);
      assertEquals(macroNetworklayer.linkSegments.size(),4);
      assertEquals(macroNetworklayer.getSupportedModes().size(),2);
      
      /* ZONING */
      final Zoning zoning = project.createAndRegisterZoning(network);
      assertEquals(zoning.odZones.size(),2);
      assertEquals(zoning.transferZones.size(),3);
      assertEquals(zoning.getNumberOfCentroids(),5); /* defaults should have been created */
      assertEquals(zoning.connectoids.size(),5); /* one per zone + one transfer connectoid per node */
      
      for(Connectoid connectoid : zoning.connectoids) {
        if(connectoid instanceof UndirectedConnectoid) {
          UndirectedConnectoid odConnectoid = UndirectedConnectoid.class.cast(connectoid);
          assertEquals(odConnectoid.getAccessZones().size(),1);
          assertEquals(odConnectoid.isModeAllowed(odConnectoid.getFirstAccessZone(), network.modes.get(PredefinedModeType.CAR)),true);
          assertEquals(odConnectoid.isModeAllowed(odConnectoid.getFirstAccessZone(), network.modes.get(PredefinedModeType.BUS)),true);
          assertEquals(odConnectoid.getLength(odConnectoid.getFirstAccessZone()),1,Precision.EPSILON_6);
        }
        if(connectoid instanceof DirectedConnectoid) {
          DirectedConnectoid transferConnectoid = DirectedConnectoid.class.cast(connectoid);
          assertEquals(transferConnectoid.getAccessZones().size(),1);
          assertEquals(transferConnectoid.isModeAllowed(transferConnectoid.getFirstAccessZone(), network.modes.get(PredefinedModeType.CAR)),false);
          assertEquals(transferConnectoid.isModeAllowed(transferConnectoid.getFirstAccessZone(), network.modes.get(PredefinedModeType.BUS)),true);
          assertEquals(transferConnectoid.getType(), ConnectoidType.PT_VEHICLE_STOP);
          assertEquals(transferConnectoid.getLength(transferConnectoid.getFirstAccessZone()),Connectoid.DEFAULT_LENGTH_KM,Precision.EPSILON_6);
          
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
      }
      
      /* DEMANDS */
      final Demands demands = project.createAndRegisterDemands(zoning, network);      
      
      } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
      }
    }
  }
  