package org.goplanit.io.test.integration.converter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.logging.Logger;

import org.goplanit.converter.intermodal.IntermodalConverterFactory;
import org.goplanit.demands.Demands;
import org.goplanit.io.converter.PlanitXmlReaderSettingsFactory;
import org.goplanit.io.converter.PlanitXmlWriterSettingsFactory;
import org.goplanit.io.converter.intermodal.PlanitIntermodalReaderFactory;
import org.goplanit.io.converter.intermodal.PlanitIntermodalReaderSettings;
import org.goplanit.io.converter.intermodal.PlanitIntermodalWriterFactory;
import org.goplanit.io.converter.intermodal.PlanitIntermodalWriterSettings;
import org.goplanit.io.input.PlanItInputBuilder;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.LayeredNetwork;
import org.goplanit.network.layer.macroscopic.MacroscopicNetworkLayerImpl;
import org.goplanit.project.CustomPlanItProject;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.locale.CountryNames;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.time.ExtendedLocalTime;
import org.goplanit.utils.zoning.*;
import org.goplanit.utils.zoning.connectoid.ConnectoidAccessZoneEntry;
import org.goplanit.utils.settings.SettingsSource;
import org.goplanit.utils.zoning.connectoid.ZoneConnectoidType;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.builder.Input;
import org.xmlunit.matchers.CompareMatcher;

import static org.junit.jupiter.api.Assertions.*;

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

  /** the files an intermodal writer writes with services, by the file names its default settings give */
  private static final List<String> FILES_WRITTEN_WITH_SERVICES =
      List.of("network.xml", "zoning.xml", "service_network.xml", "routed_services.xml");

  /**
   * Convert with services through PLANit's intermodal reader and writer, the ids generated starting afresh so two
   * conversions give the same ids
   *
   * @param readerSettings of the intermodal reader
   * @param writerSettings of the intermodal writer
   * @throws PlanItException when the conversion fails
   */
  private static void convertWithServices(
      PlanitIntermodalReaderSettings readerSettings, PlanitIntermodalWriterSettings writerSettings)
      throws PlanItException {
    IdGenerator.reset();
    IntermodalConverterFactory.create(
        PlanitIntermodalReaderFactory.create(readerSettings), PlanitIntermodalWriterFactory.create(writerSettings))
        .convertWithServices();
  }


  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(IntermodalTest.class);
    } 
  }
  
  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }

  /** folder for the settings files and the files written */
  @TempDir
  public Path tempDir;

  @BeforeEach
  public void beforeTest() {
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
      final LayeredNetwork<?,?> network = project.createAndRegisterInfrastructureNetwork(
          MacroscopicNetwork.class.getCanonicalName());

      assertEquals(network.getTransportLayers().size(), 1);
      assertEquals(network.getModes().size(), 2);
      assertTrue(network.getModes().containsPredefinedMode(PredefinedModeType.CAR));
      assertTrue(network.getModes().containsPredefinedMode(PredefinedModeType.BUS));

      /* only single layer for both modes */
      assertTrue((network instanceof MacroscopicNetwork));
      MacroscopicNetwork macroNetwork = MacroscopicNetwork.class.cast(network);      
      assertEquals(macroNetwork.getLayerByMode(macroNetwork.getModes().get(0)),
          macroNetwork.getLayerByMode(macroNetwork.getModes().get(1)));
      
      NetworkLayer layer = macroNetwork.getLayerByMode(macroNetwork.getModes().get(0));
      assertEquals(layer.getXmlId(),"road");
      assertTrue(layer instanceof MacroscopicNetworkLayerImpl);
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
        assertEquals(odConnectoid.getNumberOfAccessZoneEntries(),1);
        var accessEntry = odConnectoid.getFirstAccessZoneEntry();
        assertTrue(accessEntry.isModeAllowed(network.getModes().get(PredefinedModeType.CAR)));
        assertTrue(accessEntry.isModeAllowed(network.getModes().get(PredefinedModeType.BUS)));
        assertEquals(accessEntry.getLengthKm().get(),1,Precision.EPSILON_6);
      }
      for(var transferConnectoid : zoning.getTransferConnectoids()) {
        assertEquals(transferConnectoid.getNumberOfAccessZoneEntries(), 1);
        var accessEntry = transferConnectoid.getFirstAccessZoneEntry();
        assertFalse(accessEntry.isModeAllowed(network.getModes().get(PredefinedModeType.CAR)));
        assertTrue(accessEntry.isModeAllowed(network.getModes().get(PredefinedModeType.BUS)));
        assertEquals(accessEntry.getType(), ZoneConnectoidType.PT_VEHICLE_STOP);
        assertEquals(accessEntry.getLengthKm().get(), ConnectoidAccessZoneEntry.DEFAULT_LENGTH_KM.get(),
            Precision.EPSILON_6);

        var accessVertex = transferConnectoid.getReferenceVertex();
        transferConnectoid.getExplicitAccessLinkSegmentsStream().forEach(accessSegment -> {
          var firstEntry = transferConnectoid.getFirstAccessZoneEntry();
          switch (accessSegment.getXmlId()) {
            case linkSegment1XmlId:
              if (accessVertex.getXmlId().equals(node1XmlId)) {
                assertEquals(transferConnectoid.getXmlId(), transferconnectoid1XmlId);
                assertEquals(firstEntry.getAccessZone().getXmlId(), transferZoneStop1XmlId);
                assertEquals(
                    ((TransferZone) firstEntry.getAccessZone()).getTransferZoneType(), TransferZoneType.PLATFORM);
              } else {
                assertEquals(accessVertex.getXmlId(), node2XmlId);
                assertEquals(transferConnectoid.getXmlId(), transferconnectoid2XmlId);
                assertEquals(firstEntry.getAccessZone().getXmlId(), transferZoneStop2XmlId);
                assertEquals(((TransferZone) firstEntry.getAccessZone()).getTransferZoneType(), TransferZoneType.POLE);
              }
              break;
            case linkSegment3XmlId:
              assertEquals(accessVertex.getXmlId(), node3XmlId);
              assertEquals(transferConnectoid.getXmlId(), transferconnectoid3XmlId);
              assertEquals(firstEntry.getAccessZone().getXmlId(), transferZoneStop3XmlId);
              assertEquals(((TransferZone) firstEntry.getAccessZone()).getTransferZoneType(), TransferZoneType.NONE);
              break;
            default:
              break;
          }
        });
      }

      /* DEMANDS */
      @SuppressWarnings("unused") final Demands demands = project.createAndRegisterDemands(zoning, network);
      
    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * test case which only tries to parse a minimal intermodal setup with routes, does not run an assignment
   * but simply tests the correct parsing of the inputs
   */
  @Test
  public void test_intermodal_minimal_input_withServices() {
    try {
      final String projectPath = Path.of(intermodalTestCasePath.toString(),"minimal_input_with_services").toString();

      final PlanItInputBuilder planItInputBuilder = new PlanItInputBuilder(projectPath);
      final CustomPlanItProject project = new CustomPlanItProject(planItInputBuilder);

      /* NETWORK (already tested in test_intermodal_minimal_input)*/
      var network = project.createAndRegisterMacroscopicNetwork();
      /* ZONING (already tested in test_intermodal_minimal_input)*/
      final Zoning zoning = project.createAndRegisterZoning(network);
      /* DEMANDS (already tested in test_intermodal_minimal_input)*/
      @SuppressWarnings("unused") final Demands demands = project.createAndRegisterDemands(zoning, network);

      /*SERVICE NETWORK*/
      var serviceNetwork = project.createAndRegisterServiceNetwork(network);
      assertEquals(serviceNetwork.getTransportLayers().size(),1);
      var serviceLayer = serviceNetwork.getTransportLayers().getFirst();
      assertEquals(serviceLayer.getSupportedModes().size(), 2); // parent layer supports two modes
      assertEquals(serviceLayer.getServiceNodes().size(),3);
      assertEquals(serviceLayer.getLegs().size(),3);
      assertEquals(serviceLayer.getLegSegments().size(),3);
      assertEquals(1, serviceLayer.getLegSegments().getByXmlId("ls1").getPhysicalParentSegments().size());
      assertEquals("1", serviceLayer.getLegSegments().getByXmlId("ls1").getFirstPhysicalLinkSegment().getXmlId());
      assertEquals(2, serviceLayer.getLegSegments().getByXmlId("lsX").getPhysicalParentSegments().size());
      assertEquals("1", serviceLayer.getLegSegments().getByXmlId("ls1").getFirstPhysicalLinkSegment().getXmlId());
      assertEquals(serviceLayer.getParentNetworkLayer().getXmlId(),"road");

      /*ROUTED SERVICES*/
      var routedServices = project.createAndRegisterRoutedServices(serviceNetwork);
      assertEquals(routedServices.getParentNetwork(), serviceNetwork);
      assertEquals(1,routedServices.getLayers().size());
      var rsLayer = routedServices.getLayers().getFirst();
      assertEquals(rsLayer.getParentLayer(), serviceLayer);
      assertTrue(rsLayer.getSupportedModes().containsAll(network.getModes().toCollection()));
      var busServices = rsLayer.getServicesByMode(network.getModes().get(PredefinedModeType.BUS));
      assertFalse(busServices.isEmpty());
      assertEquals(2, busServices.size());
      /* scheduled trip */
      var line1X = busServices.firstMatch(rs -> rs.getName().equals("line_1_X"));
      assertFalse(line1X.getTripInfo().hasFrequencyBasedTrips());
      assertTrue(line1X.getTripInfo().hasScheduleBasedTrips());
      assertEquals(1, line1X.getTripInfo().getScheduleBasedTrips().size());
      assertEquals(1, line1X.getTripInfo().getScheduleBasedTrips().getFirst().getRelativeLegTimingsSize());
      assertEquals(LocalTime.of(0,2,0), line1X.getTripInfo().getScheduleBasedTrips().getFirst().getRelativeLegTiming(0).getDwellTime());
      assertEquals(LocalTime.of(0,3,0), line1X.getTripInfo().getScheduleBasedTrips().getFirst().getRelativeLegTiming(0).getDuration());
      assertEquals(serviceLayer.getLegSegments().getByXmlId("lsX"), line1X.getTripInfo().getScheduleBasedTrips().getFirst().getRelativeLegTiming(0).getParentLegSegment());
      assertEquals(3, line1X.getTripInfo().getScheduleBasedTrips().getFirst().getDepartures().size());
      assertEquals(ExtendedLocalTime.of("08:00:00"), line1X.getTripInfo().getScheduleBasedTrips().getFirst().getDepartures().getFirst().getDepartureTime());
      /* frequency trip */
      var line1 = busServices.firstMatch(rs -> rs.getName().equals("line_1"));
      assertTrue(line1.getTripInfo().hasFrequencyBasedTrips());
      assertFalse(line1.getTripInfo().hasScheduleBasedTrips());
      assertEquals(1, line1.getTripInfo().getFrequencyBasedTrips().size());
      assertEquals(3.0, line1.getTripInfo().getFrequencyBasedTrips().getFirst().getFrequencyPerHour(), Precision.EPSILON_6);
      assertEquals(2, line1.getTripInfo().getFrequencyBasedTrips().getFirst().getNumberOfLegSegments());
      assertEquals("ls1", line1.getTripInfo().getFrequencyBasedTrips().getFirst().getFirstLegSegment().getXmlId());
      assertEquals("ls2", line1.getTripInfo().getFrequencyBasedTrips().getFirst().getLastLegSegment().getXmlId());


    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe( e.getMessage());
      fail(e.getMessage());
    }
  }

  /**
   * The minimal intermodal case with services, converted through PLANit's intermodal reader and writer with settings
   * made from settings files, writes the same files as with the same settings made in code. The files name the files
   * of the readers and writers held, the reader's naming one file for all four readers, which reads the one input file
   * holding every part, and the writer's giving each writer's output directory relative to its file
   */
  @Test
  public void test_intermodal_conversion_with_settings_from_files() throws IOException, PlanItException {
    final Path caseFolder = Path.of(intermodalTestCasePath.toString(), "minimal_input_with_services").toAbsolutePath();
    final Path inCode = Files.createDirectories(tempDir.resolve("in_code"));
    final Path fromFiles = Files.createDirectories(tempDir.resolve("from_files"));

    convertWithServices(new PlanitIntermodalReaderSettings(caseFolder.toString()),
        new PlanitIntermodalWriterSettings(inCode.toString(), CountryNames.AUSTRALIA));

    final String readerHeader = "planitIoReaderSettings: 1\n";
    Files.writeString(tempDir.resolve("planit_input.yaml"),
        readerHeader + "inputSource: " + caseFolder.resolve("macroscopicinput.xml") + "\n");
    final Path readerFile = Files.writeString(tempDir.resolve("intermodal_reader.yaml"), readerHeader +
        "network: planit_input.yaml\nzoning: planit_input.yaml\nserviceNetwork: planit_input.yaml\n" +
        "routedServices: planit_input.yaml\n");

    final String writerHeader = "planitIoWriterSettings: 1\noutputDirectory: from_files\ncountry: Australia\n";
    Files.writeString(tempDir.resolve("network_writer.yaml"), writerHeader + "fileName: network.xml\n");
    Files.writeString(tempDir.resolve("zoning_writer.yaml"), writerHeader + "fileName: zoning.xml\n");
    Files.writeString(tempDir.resolve("service_network_writer.yaml"), writerHeader + "fileName: service_network.xml\n");
    Files.writeString(tempDir.resolve("routed_services_writer.yaml"), writerHeader + "fileName: routed_services.xml\n");
    final Path writerFile = Files.writeString(tempDir.resolve("intermodal_writer.yaml"),
        "planitIoWriterSettings: 1\nnetwork: network_writer.yaml\nzoning: zoning_writer.yaml\n" +
            "serviceNetwork: service_network_writer.yaml\nroutedServices: routed_services_writer.yaml\n");

    convertWithServices(
        PlanitXmlReaderSettingsFactory.createIntermodalReaderSettingsFromYaml(List.of(SettingsSource.ofFile(readerFile))),
        PlanitXmlWriterSettingsFactory.createIntermodalWriterSettingsFromYaml(List.of(SettingsSource.ofFile(writerFile))));

    for (var fileName : FILES_WRITTEN_WITH_SERVICES) {
      org.hamcrest.MatcherAssert.assertThat(fileName, Input.fromFile(fromFiles.resolve(fileName).toString()),
          CompareMatcher.isSimilarTo(Input.fromFile(inCode.resolve(fileName).toString())));
    }
  }

  /**
   * The intermodal writer writes the service network and the routed services with the settings it holds for them, not
   * only their output directory and country: here the file names given to those two settings
   */
  @Test
  public void test_intermodal_writer_writes_services_with_their_settings() throws IOException, PlanItException {
    final Path caseFolder = Path.of(intermodalTestCasePath.toString(), "minimal_input_with_services").toAbsolutePath();
    final Path output = Files.createDirectories(tempDir.resolve("output"));
    var writerSettings = new PlanitIntermodalWriterSettings(output.toString(), CountryNames.AUSTRALIA);
    writerSettings.getServiceNetworkSettings().setFileName("lines.xml");
    writerSettings.getRoutedServicesSettings().setFileName("services.xml");

    convertWithServices(new PlanitIntermodalReaderSettings(caseFolder.toString()), writerSettings);

    assertTrue(Files.exists(output.resolve("lines.xml")));
    assertTrue(Files.exists(output.resolve("services.xml")));
    assertFalse(Files.exists(output.resolve("service_network.xml")));
    assertFalse(Files.exists(output.resolve("routed_services.xml")));
  }
}
  