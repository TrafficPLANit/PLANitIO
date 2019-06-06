package org.planit.xml.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.demand.Demands;
import org.planit.event.listener.InputBuilderListener;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.OutputType;
import org.planit.output.formatter.CSVOutputFormatter;
import org.planit.output.formatter.OutputFormatter;
import org.planit.project.PlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.test.BprResultDto;
import org.planit.test.CsvIoUtils;
import org.planit.test.TestHelper;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.userclass.Mode;
import org.planit.utils.IdGenerator;
import org.planit.xml.input.PlanItXml;
import org.planit.zoning.Zoning;

public class PlanItXmlTest {

	private static final Logger LOGGER = Logger.getLogger(PlanItXmlTest.class.getName());
	private static final String TEST_RESULTS_LOCATION = "src\\test\\testRunOutput.csv";

	private String zoningXsdFileLocation;
	private String demandXsdFileLocation;
	private String networkXsdFileLocation;
	private Consumer<BPRLinkTravelTimeCost> standardConsumer;
	private Consumer<BPRLinkTravelTimeCost> zeroConsumer;

	@Before
	public void setUp() throws Exception {
		zoningXsdFileLocation = "src\\main\\resources\\schemas\\macroscopiczoninginput.xsd";
		demandXsdFileLocation = "src\\main\\resources\\schemas\\macroscopicdemandinput.xsd";
		networkXsdFileLocation = "src\\main\\resources\\schemas\\macroscopicnetworkinput.xsd";
		standardConsumer = (bprLinkTravelTimeCost) -> {
			bprLinkTravelTimeCost.setDefaultParameters(0.5, 4.0);
		};
		zeroConsumer = (bprLinkTravelTimeCost) -> {
			bprLinkTravelTimeCost.setDefaultParameters(0.0, 0.0);
		};
	}

	@After
	public void tearDown() throws Exception {
		File tempFile = new File(TEST_RESULTS_LOCATION);
		tempFile.delete();
	}

	@Test
	public void testBasic1() {
		try {
			runTest("src\\test\\resources\\basic\\xml\\test1\\network.csv",
					"src\\test\\resources\\basic\\xml\\test1\\link_types.csv",
					"src\\test\\resources\\basic\\xml\\test1\\modes.csv",
					"src\\test\\resources\\basic\\xml\\test1\\results.csv",
					"src\\test\\resources\\basic\\xml\\test1\\zones.xml",
					"src\\test\\resources\\basic\\xml\\test1\\demands.xml",
					"src\\test\\resources\\basic\\xml\\test1\\network.xml", zeroConsumer);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic2() {
		try {
			runTest("src\\test\\resources\\basic\\xml\\test2\\network.csv",
					"src\\test\\resources\\basic\\xml\\test2\\link_types.csv",
					"src\\test\\resources\\basic\\xml\\test2\\modes.csv",
					"src\\test\\resources\\basic\\xml\\test2\\results.csv",
					"src\\test\\resources\\basic\\xml\\test2\\zones.xml",
					"src\\test\\resources\\basic\\xml\\test2\\demands.xml",
					"src\\test\\resources\\basic\\xml\\test2\\network.xml", zeroConsumer);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic3() {
		try {
			runTest("src\\test\\resources\\basic\\xml\\test3\\network.csv",
					"src\\test\\resources\\basic\\xml\\test3\\link_types.csv",
					"src\\test\\resources\\basic\\xml\\test3\\modes.csv",
					"src\\test\\resources\\basic\\xml\\test3\\results.csv",
					"src\\test\\resources\\basic\\xml\\test3\\zones.xml",
					"src\\test\\resources\\basic\\xml\\test3\\demands.xml",
					"src\\test\\resources\\basic\\xml\\test3\\network.xml", zeroConsumer);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic13() {
		try {
			runTest("src\\test\\resources\\basic\\xml\\test13\\network.csv",
					"src\\test\\resources\\basic\\xml\\test13\\link_types.csv",
					"src\\test\\resources\\basic\\xml\\test13\\modes.csv",
					"src\\test\\resources\\basic\\xml\\test13\\results.csv",
					"src\\test\\resources\\basic\\xml\\test13\\zones.xml",
					"src\\test\\resources\\basic\\xml\\test13\\demands.xml",
					"src\\test\\resources\\basic\\xml\\test13\\network.xml", zeroConsumer);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice1() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test1\\network.csv",
					"src\\test\\resources\\route_choice\\xml\\test1\\link_types.csv",
					"src\\test\\resources\\route_choice\\xml\\test1\\modes.csv",
					"src\\test\\resources\\route_choice\\xml\\test1\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test1\\zones.xml",
					"src\\test\\resources\\route_choice\\xml\\test1\\demands.xml",
					"src\\test\\resources\\route_choice\\xml\\test1\\network.xml", 500, 0.0, standardConsumer);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice2() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test2\\network.csv",
					"src\\test\\resources\\route_choice\\xml\\test2\\link_types.csv",
					"src\\test\\resources\\route_choice\\xml\\test2\\modes.csv",
					"src\\test\\resources\\route_choice\\xml\\test2\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test2\\zones.xml",
					"src\\test\\resources\\route_choice\\xml\\test2\\demands.xml",
					"src\\test\\resources\\route_choice\\xml\\test2\\network.xml", 500, 0.0, standardConsumer);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice3() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test3\\network.csv",
					"src\\test\\resources\\route_choice\\xml\\test3\\link_types.csv",
					"src\\test\\resources\\route_choice\\xml\\test3\\modes.csv",
					"src\\test\\resources\\route_choice\\xml\\test3\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test3\\zones.xml",
					"src\\test\\resources\\route_choice\\xml\\test3\\demands.xml",
					"src\\test\\resources\\route_choice\\xml\\test3\\network.xml", 500, 0.0, standardConsumer);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice4() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test4\\network.csv",
					"src\\test\\resources\\route_choice\\xml\\test4\\link_types.csv",
					"src\\test\\resources\\route_choice\\xml\\test4\\modes.csv",
					"src\\test\\resources\\route_choice\\xml\\test4\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test4\\zones.xml",
					"src\\test\\resources\\route_choice\\xml\\test4\\demands.xml",
					"src\\test\\resources\\route_choice\\xml\\test4\\network.xml", 500, 0.0, standardConsumer);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice42() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test42\\network.csv",
					"src\\test\\resources\\route_choice\\xml\\test42\\link_types.csv",
					"src\\test\\resources\\route_choice\\xml\\test42\\modes.csv",
					"src\\test\\resources\\route_choice\\xml\\test42\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test42\\zones.xml",
					"src\\test\\resources\\route_choice\\xml\\test42\\demands.xml",
					"src\\test\\resources\\route_choice\\xml\\test42\\network.xml", 500, 0.0, standardConsumer);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice4raw() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test4raw\\network.csv",
					"src\\test\\resources\\route_choice\\xml\\test4raw\\link_types.csv",
					"src\\test\\resources\\route_choice\\xml\\test4raw\\modes.csv",
					"src\\test\\resources\\route_choice\\xml\\test4raw\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test4raw\\zones.xml",
					"src\\test\\resources\\route_choice\\xml\\test4raw\\demands.xml",
					"src\\test\\resources\\route_choice\\xml\\test4raw\\network.xml", 500, 0.0, standardConsumer);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice4raw2() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test4raw2\\network.csv",
					"src\\test\\resources\\route_choice\\xml\\test4raw2\\link_types.csv",
					"src\\test\\resources\\route_choice\\xml\\test4raw2\\modes.csv",
					"src\\test\\resources\\route_choice\\xml\\test4raw2\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test4raw2\\zones.xml",
					"src\\test\\resources\\route_choice\\xml\\test4raw2\\demands.xml",
					"src\\test\\resources\\route_choice\\xml\\test4raw2\\network.xml", 500, 0.0, standardConsumer);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice5() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test5\\network.csv",
					"src\\test\\resources\\route_choice\\xml\\test5\\link_types.csv",
					"src\\test\\resources\\route_choice\\xml\\test5\\modes.csv",
					"src\\test\\resources\\route_choice\\xml\\test5\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test5\\zones.xml",
					"src\\test\\resources\\route_choice\\xml\\test5\\demands.xml",
					"src\\test\\resources\\route_choice\\xml\\test5\\network.xml", 500, 0.0,
					(bprLinkTravelTimeCost) -> {
						standardConsumer.accept(bprLinkTravelTimeCost);
						bprLinkTravelTimeCost.setDefaultParameters(1, 2, 0.8, 4.5);
					});
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void runTest(String networkFileLocation, String linkTypesFileLocation, String modeFileLocation,
			String resultsFileLocation, String zoningXmlFileLocation, String demandXmlFileLocation,
			String networkXmlFileLocation, Consumer<BPRLinkTravelTimeCost> setCostParameters) throws Exception {
		runTest(networkFileLocation, linkTypesFileLocation, modeFileLocation, resultsFileLocation,
				zoningXmlFileLocation, demandXmlFileLocation, networkXmlFileLocation, null, null, setCostParameters);
	}

	private void runTest(String networkFileLocation, String linkTypesFileLocation, String modeFileLocation,
			String resultsFileLocation, String zoningXmlFileLocation, String demandXmlFileLocation,
			String networkXmlFileLocation, Integer maxIterations, Double epsilon,
			Consumer<BPRLinkTravelTimeCost> setCostParameters) throws Exception {
		// SET UP SCANNER AND PROJECT
		IdGenerator.reset();
		InputBuilderListener inputBuilderListener = new PlanItXml(networkFileLocation, linkTypesFileLocation,
				modeFileLocation, zoningXmlFileLocation, demandXmlFileLocation, networkXmlFileLocation);
		runTestFrominputBuilderListener(inputBuilderListener, resultsFileLocation, maxIterations, epsilon,
				setCostParameters);
	}

	private void runTest(String networkFileLocation, String linkTypesFileLocation, String modeFileLocation,
			String resultsFileLocation, String zoningXmlFileLocation, String demandXmlFileLocation,
			String networkXmlFileLocation, String zoningXsdFileLocation, String demandXsdFileLocation,
			String supplyXsdFileLocation, Consumer<BPRLinkTravelTimeCost> setCostParameters) throws Exception {
		runTest(networkFileLocation, linkTypesFileLocation, modeFileLocation, resultsFileLocation,
				zoningXmlFileLocation, demandXmlFileLocation, networkXmlFileLocation, zoningXsdFileLocation,
				demandXsdFileLocation, supplyXsdFileLocation, null, null, setCostParameters);
	}

	private void runTest(String networkFileLocation, String linkTypesFileLocation, String modeFileLocation,
			String resultsFileLocation, String zoningXmlFileLocation, String demandXmlFileLocation,
			String networkXmlFileLocation, String zoningXsdFileLocation, String demandXsdFileLocation,
			String networkXsdFileLocation, Integer maxIterations, Double epsilon,
			Consumer<BPRLinkTravelTimeCost> setCostParameters) throws Exception {
		// SET UP SCANNER AND PROJECT
		IdGenerator.reset();
		InputBuilderListener inputBuilderListener = new PlanItXml(networkFileLocation, linkTypesFileLocation,
				modeFileLocation, zoningXmlFileLocation, demandXmlFileLocation, networkXmlFileLocation,
				zoningXsdFileLocation, demandXsdFileLocation, networkXsdFileLocation);
		runTestFrominputBuilderListener(inputBuilderListener, resultsFileLocation, maxIterations, epsilon,
				setCostParameters);
	}

	private void runTestFrominputBuilderListener(InputBuilderListener inputBuilderListener, String resultsFileLocation,
			Integer maxIterations, Double epsilon, Consumer<BPRLinkTravelTimeCost> setCostParameters)
			throws PlanItException {
		PlanItProject project = new PlanItProject(inputBuilderListener);

		// RAW INPUT START --------------------------------
		PhysicalNetwork physicalNetwork = project
				.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
		Zoning zoning = project.createAndRegisterZoning();
		Demands demands = project.createAndRegisterDemands();
		// RAW INPUT END -----------------------------------

		// TRAFFIC ASSIGNMENT START------------------------
		DeterministicTrafficAssignment assignment = project
				.createAndRegisterDeterministicAssignment(TraditionalStaticAssignment.class.getCanonicalName());
		CapacityRestrainedTrafficAssignmentBuilder taBuilder = (CapacityRestrainedTrafficAssignmentBuilder) assignment
				.getBuilder();

		// SUPPLY SIDE
		taBuilder.registerPhysicalNetwork(physicalNetwork);
		// SUPPLY-DEMAND INTERACTIONS
		BPRLinkTravelTimeCost bprLinkTravelTimeCost = (BPRLinkTravelTimeCost) taBuilder
				.createAndRegisterPhysicalTravelTimeCostFunction(BPRLinkTravelTimeCost.class.getCanonicalName());
		setCostParameters.accept(bprLinkTravelTimeCost);
		SpeedConnectoidTravelTimeCost speedConnectoidTravelTimeCost = (SpeedConnectoidTravelTimeCost) taBuilder
				.createAndRegisterVirtualTravelTimeCostFunction(SpeedConnectoidTravelTimeCost.class.getCanonicalName());
		MSASmoothing msaSmoothing = (MSASmoothing) taBuilder
				.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
		// SUPPLY-DEMAND INTERFACE
		taBuilder.registerZoning(zoning);

		// DEMAND SIDE
		taBuilder.registerDemands(demands);

		// OUTPUT
		assignment.activateOutput(OutputType.LINK);
		OutputFormatter outputFormatter = project
				.createAndRegisterOutputFormatter(CSVOutputFormatter.class.getCanonicalName());
		CSVOutputFormatter csvOutputFormatter = (CSVOutputFormatter) outputFormatter;
		csvOutputFormatter.setOutputFileName(TEST_RESULTS_LOCATION);
		taBuilder.registerOutputFormatter(outputFormatter);

		// "USER" configuration
		if (maxIterations != null) {
			assignment.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
		}
		if (epsilon != null) {
			assignment.getGapFunction().getStopCriterion().setEpsilon(epsilon);
		}

		project.executeAllTrafficAssignments();
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMapFromFile = CsvIoUtils
				.createResultsMapFromCsvFile(resultsFileLocation);
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap = CsvIoUtils
				.createResultsMapFromCsvFile(TEST_RESULTS_LOCATION);
		TestHelper.compareResultsToCsvFileContents(resultsMap, resultsMapFromFile);
	}

}
