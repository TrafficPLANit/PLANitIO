package org.planit.xml.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.demand.Demands;
import org.planit.event.listener.InputBuilderListener;
import org.planit.exceptions.PlanItException;
import org.planit.input.PlanItXMLInputBuilder;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.OutputType;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.formatter.PlanItXMLOutputFormatter;
import org.planit.output.property.OutputProperty;
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
import org.planit.zoning.Zoning;

public class PlanItXmlTest {

	private static final Logger LOGGER = Logger.getLogger(PlanItXmlTest.class.getName());
	private static final String CSV_TEST_RESULTS_LOCATION = "src\\test\\testRunOutput.csv";
	private static boolean clearOutputDirectories;

	@BeforeClass
	public static void setUp() throws Exception {
		clearOutputDirectories = true;
	}

	@After
	public void tearDown() throws Exception {
		File tempFile = new File(CSV_TEST_RESULTS_LOCATION);
		tempFile.delete();
	}

	@Test
	public void testBasic1() {
		try {
			runTest("src\\test\\resources\\basic\\xml\\test1\\results.csv",
					"src\\test\\resources\\basic\\xml\\test1", null, "testBasic1");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic2() {
		try {
			runTest("src\\test\\resources\\basic\\xml\\test2\\results.csv",
					"src\\test\\resources\\basic\\xml\\test2", null, "testBasic2");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic3() {
		try {
			runTest("src\\test\\resources\\basic\\xml\\test3\\results.csv",
					"src\\test\\resources\\basic\\xml\\test3", null, "testBasic3");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic13() {
		try {
			runTest("src\\test\\resources\\basic\\xml\\test13\\results.csv",
					"src\\test\\resources\\basic\\xml\\test13", null, "testBasic13");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice1() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test1\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test1", 500, 0.0, null, "testRouteChoice1");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice2() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test2\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test2", 500, 0.0, null, "testRouteChoice2");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice3() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test3\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test3", 500, 0.0, null, "testRouteChoice3");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice4() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test4\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test4", 500, 0.0, null, "testRouteChoice4");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice42() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test42\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test42", 500, 0.0, null, "testRouteChoice42");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice4raw() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test4raw\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test4raw", 500, 0.0, null, "testRouteChoice4raw");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice4raw2() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test4raw2\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test4raw2", 500, 0.0, null, "testRouteChoice4raw2");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice5() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test5\\results.csv",
					"src\\test\\resources\\route_choice\\xml\\test5", 500, 0.0,
					(physicalNetwork, bprLinkTravelTimeCost) -> {
						MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) physicalNetwork;
						MacroscopicLinkSegmentType macroscopiclinkSegmentType = macroscopicNetwork
								.findMacroscopicLinkSegmentTypeByExternalId(1);
						Mode mode = Mode.getByExternalId(2);
						bprLinkTravelTimeCost.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
					}, "testRouteChoice5");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void runTest(String resultsFileLocation, String projectPath, BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters,
			String description) throws Exception {
		IdGenerator.reset();
		InputBuilderListener inputBuilderListener = new PlanItXMLInputBuilder(projectPath);
		runTestFromInputBuilderListener(inputBuilderListener, resultsFileLocation, null, null,
				setCostParameters, description);
	}
	
	private void runTest(String resultsFileLocation, String projectPath, int maxIterations, Double epsilon, BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters,
			String description) throws Exception {
		IdGenerator.reset();
		InputBuilderListener inputBuilderListener = new PlanItXMLInputBuilder(projectPath);
		runTestFromInputBuilderListener(inputBuilderListener, resultsFileLocation, maxIterations, epsilon,
				setCostParameters, description);
	}
	
	private void runTestFromInputBuilderListener(InputBuilderListener inputBuilderListener, String resultsFileLocation,
			Integer maxIterations, Double epsilon, BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters,
			String description) throws PlanItException {
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
		if (setCostParameters != null) {
			setCostParameters.accept(physicalNetwork, bprLinkTravelTimeCost);
		}
		SpeedConnectoidTravelTimeCost speedConnectoidTravelTimeCost = (SpeedConnectoidTravelTimeCost) taBuilder
				.createAndRegisterVirtualTravelTimeCostFunction(SpeedConnectoidTravelTimeCost.class.getCanonicalName());
		MSASmoothing msaSmoothing = (MSASmoothing) taBuilder
				.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
		// SUPPLY-DEMAND INTERFACE
		taBuilder.registerZoning(zoning);

		// DEMAND SIDE
		taBuilder.registerDemands(demands);

		//DATA OUTPUT CONFIGURATION
		assignment.activateOutput(OutputType.LINK);
		OutputConfiguration outputConfiguration = assignment.getOutputConfiguration();
		outputConfiguration.setPersistOnlyFinalIteration(false); // option to only persist the final iteration
		LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) outputConfiguration.getOutputTypeConfiguration(OutputType.LINK);		
		linkOutputTypeConfiguration.addAllProperties();
		linkOutputTypeConfiguration.removeProperty(OutputProperty.TRAVEL_TIME);
		
		//OUTPUT FORMAT CONFIGURATION
		PlanItXMLOutputFormatter xmlOutputFormatter = (PlanItXMLOutputFormatter) project.createAndRegisterOutputFormatter(PlanItXMLOutputFormatter.class.getCanonicalName());
		xmlOutputFormatter.setCsvSummaryOutputFileName(CSV_TEST_RESULTS_LOCATION);
		if (clearOutputDirectories) {
			xmlOutputFormatter.resetXmlOutputDirectory();
			xmlOutputFormatter.resetCsvOutputDirectory();
			clearOutputDirectories = false;
		}
		xmlOutputFormatter.setXmlNamePrefix(description);
		xmlOutputFormatter.setCsvNamePrefix(description);
		xmlOutputFormatter.setXmlOutputDirectory("C:\\Users\\Public\\PlanIt\\Xml");
		xmlOutputFormatter.setCsvOutputDirectory("C:\\Users\\Public\\PlanIt\\Csv");
		taBuilder.registerOutputFormatter(xmlOutputFormatter);

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
				.createResultsMapFromCsvFile(CSV_TEST_RESULTS_LOCATION);
		TestHelper.compareResultsToCsvFileContents(resultsMap, resultsMapFromFile);
	}

}
