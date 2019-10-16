package org.planit.xml.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.exceptions.PlanItException;
import org.planit.logging.PlanItLogger;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.OutputType;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.property.CostOutputProperty;
import org.planit.output.property.DownstreamNodeExternalIdOutputProperty;
import org.planit.output.property.LinkSegmentExternalIdOutputProperty;
import org.planit.output.property.ModeExternalIdOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.UpstreamNodeExternalIdOutputProperty;
import org.planit.project.PlanItProject;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.userclass.Mode;
import org.planit.utils.IdGenerator;
import org.planit.xml.test.integration.LinkSegmentExpectedResultsDto;
import org.planit.xml.test.util.TestHelper;

/**
 * JUnit test case for TraditionalStaticAssignment
 * 
 * Many of these unit tests use the ResultDto object to save standard results
 * against which the results of test runs are compared.
 * 
 * The arguments to the ResultDto constructor are:
 * 
 * startNodeId external id of start node (used to define the link segment)
 * endNodeId external id of end node (used to define the link segment) linkFlow
 * flow through link (output) linkCost cost (travel time) of link (output)
 * totalCostToEndNode cumulative travel time from start of output path to the
 * end of the current link (output) capacity capacity of the link (input) (no
 * lanes x capacity per lane)) length length of the link (input) speed travel
 * speed of the link (input)
 * 
 * 
 * @author gman6028
 *
 */
public class PlanItXmlIntegrationTest {

	/**
	 * Run assertions which confirm that results files contain the correct data, and
	 * then remove the results files
	 * 
	 * @param projectPath project directory containing the input files
	 * @param description description used in temporary output file names
	 * @param csvFileName name of CSV file containing run results
	 * @param xmlFileName name of XML file containing run results
	 * @throws Exception thrown if there is an error
	 */
	private void runFileEqualAssertionsAndCleanUp(OutputType outputType, String projectPath, String description,
			String csvFileName, String xmlFileName) throws Exception {
		assertTrue(TestHelper.compareFiles(projectPath + "\\" + outputType.value() + "_" + csvFileName,
				projectPath + "\\" + outputType.value() + "_" + description + "_" + csvFileName));
		TestHelper.deleteFile(outputType, projectPath, description, csvFileName);
		assertTrue(
				TestHelper.isXmlFileSameExceptForTimestamp(projectPath + "\\" + outputType.value() + "_" + xmlFileName,
						projectPath + "\\" + outputType.value() + "_" + description + "_" + xmlFileName));
		TestHelper.deleteFile(outputType, projectPath, description, xmlFileName);
	}

	@BeforeClass
	public static void setUp() throws Exception {
		PlanItLogger.setLogging("logs\\PlanItXmlIntegrationTest.log", PlanItXmlIntegrationTest.class);
	}

	@AfterClass
	public static void tearDown() {
		PlanItLogger.close();
	}

	/**
	 * Test that the values of an initial costs file are read in by start and end
	 * note and registered by PlanItProject and the stored values match the expected
	 * ones by link external Id
	 */
	@Test
	public void testInitialCostValues() {
		String projectPath = "src\\test\\resources\\initial_costs\\xml\\test1";
		String initialCostsFileLocation = "src\\test\\resources\\initial_costs\\xml\\test1\\initial_link_segment_costs.csv";
		String initialCostsFileLocationExternalId = "src\\test\\resources\\initial_costs\\xml\\test1\\initial_link_segment_costs_external_id.csv";
		try {
			IdGenerator.reset();
			PlanItProject project = new PlanItProject(projectPath);
			PhysicalNetwork physicalNetwork = project
					.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
			DeterministicTrafficAssignment assignment = project
					.createAndRegisterDeterministicAssignment(TraditionalStaticAssignment.class.getCanonicalName());
			CapacityRestrainedTrafficAssignmentBuilder taBuilder = (CapacityRestrainedTrafficAssignmentBuilder) assignment
					.getBuilder();
			taBuilder.registerPhysicalNetwork(physicalNetwork);
			InitialLinkSegmentCost initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork,
					initialCostsFileLocation);
			Reader in = new FileReader(initialCostsFileLocationExternalId);
			CSVParser parser = CSVParser.parse(in, CSVFormat.DEFAULT.withFirstRecordAsHeader());
			String modeHeader = ModeExternalIdOutputProperty.MODE_EXTERNAL_ID;
			String linkSegmentExternalIdHeader = LinkSegmentExternalIdOutputProperty.LINK_SEGMENT_EXTERNAL_ID;
			String costHeader = CostOutputProperty.COST;
			for (CSVRecord record : parser) {
				long modeExternalId = Long.parseLong(record.get(modeHeader));
				Mode mode = Mode.getByExternalId(modeExternalId);
				double cost = Double.parseDouble(record.get(costHeader));
				long linkSegmentExternalId = Long.parseLong(record.get(linkSegmentExternalIdHeader));
				LinkSegment linkSegment = physicalNetwork.linkSegments
						.getLinkSegmentByExternalId(linkSegmentExternalId);
				assertEquals(cost, initialCost.getSegmentCost(mode, linkSegment), 0.0001);
			}
			in.close();
		} catch (Exception ex) {
			PlanItLogger.severe(ex.getMessage());
			fail(ex.getMessage());
		}
	}

	/**
	 * Test that the read in initial cost values match the expected ones when there
	 * are some rows missing in the standard results file
	 */
	@Test
	public void testInitialCostMissingRows() {
		String projectPath = "src\\test\\resources\\initial_costs\\xml\\test1";
		String initialCostsFileLocation = "src\\test\\resources\\initial_costs\\xml\\test1\\initial_link_segment_costs.csv";
		String initialCostsFileLocationExternalId = "src\\test\\resources\\initial_costs\\xml\\test1\\initial_link_segment_costs1.csv";
		try {
			IdGenerator.reset();
			PlanItProject project = new PlanItProject(projectPath);
			PhysicalNetwork physicalNetwork = project
					.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
			DeterministicTrafficAssignment assignment = project
					.createAndRegisterDeterministicAssignment(TraditionalStaticAssignment.class.getCanonicalName());
			CapacityRestrainedTrafficAssignmentBuilder taBuilder = (CapacityRestrainedTrafficAssignmentBuilder) assignment
					.getBuilder();
			taBuilder.registerPhysicalNetwork(physicalNetwork);
			InitialLinkSegmentCost initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork,
					initialCostsFileLocation);
			Reader in = new FileReader(initialCostsFileLocationExternalId);
			CSVParser parser = CSVParser.parse(in, CSVFormat.DEFAULT.withFirstRecordAsHeader());
			String modeHeader = ModeExternalIdOutputProperty.MODE_EXTERNAL_ID;
			String upstreamNodeExternalIdHeader = UpstreamNodeExternalIdOutputProperty.UPSTREAM_NODE_EXTERNAL_ID;
			String downstreamNodeExternalIdHeader = DownstreamNodeExternalIdOutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID;
			String costHeader = CostOutputProperty.COST;
			for (CSVRecord record : parser) {
				long modeExternalId = Long.parseLong(record.get(modeHeader));
				Mode mode = Mode.getByExternalId(modeExternalId);
				double cost = Double.parseDouble(record.get(costHeader));
				long upstreamNodeExternalId = Long.parseLong(record.get(upstreamNodeExternalIdHeader));
				long downstreamNodeExternalId = Long.parseLong(record.get(downstreamNodeExternalIdHeader));
				LinkSegment linkSegment = physicalNetwork.linkSegments
						.getLinkSegmentByStartAndEndNodeExternalId(upstreamNodeExternalId, downstreamNodeExternalId);
				assertEquals(cost, initialCost.getSegmentCost(mode, linkSegment), 0.0001);
			}
			in.close();
		} catch (Exception ex) {
			PlanItLogger.severe(ex.getMessage());
			fail(ex.getMessage());
		}
	}

	/**
	 * Test that PlanItProject throws an exception when the initial costs file
	 * references a link segment which has not been defined
	 */
	@Test
	public void testInitialCostValuesMissingColumns() {
		try {
			String projectPath = "src\\test\\resources\\initial_costs\\xml\\test2";
			String description = "RunId 0_testBasic1";
			Integer maxIterations = null;
			TestHelper.setupAndExecuteAssignment(projectPath,
					"src\\test\\resources\\initial_costs\\xml\\test2\\initial_link_segment_costs_external_id.csv",
					maxIterations, null, description);
			fail("RunTest did not throw an exception when it should have (missing data in the input XML file in the link definition section).");
		} catch (Exception e) {
			PlanItLogger.info(e.getMessage());
			assertTrue(true);
		}
	}

	/**
	 * Test that PlanItProject reads in the values of one initial costs file
	 */
	@Test
	public void testBasic1OneInitialCostFile() {
		try {
			String projectPath = "src\\test\\resources\\basic\\xml\\test1";
			String description = "testBasic1";
			String csvFileName = "Time Period 1_2.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = null;

			TestHelper.setupAndExecuteAssignment(projectPath,
					"src\\test\\resources\\basic\\xml\\test1\\initial_link_segment_costs.csv", maxIterations, null,
					description);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * Test that PlanItProject reads in the values of two initial costs files
	 */
	@Test
	public void testBasic1TwoInitialCostFiles() {
		try {
			String projectPath = "src\\test\\resources\\basic\\xml\\test1";
			String description = "testBasic1";
			String csvFileName = "Time Period 1_2.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = null;

			TestHelper.setupAndExecuteAssignment(projectPath,
					"src\\test\\resources\\basic\\xml\\test1\\initial_link_segment_costs.csv",
					"src\\test\\resources\\basic\\xml\\test1\\initial_link_segment_costs1.csv", 0, maxIterations, null,
					description);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * Test of results for TraditionalStaticAssignment for simple test case.
	 * 
	 * This test case uses the example from the course notes of ITLS6102 Strategic
	 * Transport Planning, Lecture 1 (Overview), the example on Page 122 of the 2019
	 * course notes.
	 * 
	 * This test case uses route A to C in the example, which has a total route cost
	 * of 77 (the fifth argument in the ResultDto constructor).
	 */
	@Test
	public void testBasicShortestPathAlgorithmAtoC() {
		try {
			String projectPath = "src\\test\\resources\\basic\\xml\\test2";
			String description = "testBasic2";
			String csvFileName = "Time Period 1_2.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = null;

			MemoryOutputFormatter memoryOutputFormatter = TestHelper.setupAndExecuteAssignment(projectPath,
					maxIterations, null, description);
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>();
			TimePeriod timePeriod = TimePeriod.getByExternalId(Long.valueOf(0));
			resultsMap.put(timePeriod, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			Mode mode1 = Mode.getByExternalId(Long.valueOf(1));
			resultsMap.get(timePeriod).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 6, 1, 10, 10, 2000, 10, 1));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(6, 11, 1, 12, 22, 2000, 12, 1));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 12, 1, 8, 30, 2000, 8, 1));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 13, 1, 47, 77, 2000, 47, 1));
			TestHelper.compareResultsToMemoryOutputFormatter(OutputType.LINK, memoryOutputFormatter, maxIterations,
					resultsMap);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * Test of results for TraditionalStaticAssignment for simple test case.
	 * 
	 * This test case uses the example from the course notes of ITLS6102 Strategic
	 * Transport Planning, Lecture 1 (Overview), the example on Page 122 of the 2019
	 * course notes.
	 * 
	 * This test case uses route A to D in the example, which has a total route cost
	 * of 108 (the fifth argument in the ResultDto constructor).
	 */
	@Test
	public void testBasicShortestPathAlgorithmAtoD() {
		try {
			String projectPath = "src\\test\\resources\\basic\\xml\\test3";
			String description = "testBasic3";
			String csvFileName = "Time Period 1_2.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = null;

			MemoryOutputFormatter memoryOutputFormatter = TestHelper.setupAndExecuteAssignment(projectPath,
					maxIterations, null, description);
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>();
			TimePeriod timePeriod = TimePeriod.getByExternalId(Long.valueOf(0));
			resultsMap.put(timePeriod, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			Mode mode1 = Mode.getByExternalId(Long.valueOf(1));
			resultsMap.get(timePeriod).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 6, 1, 10, 10, 2000, 10, 1));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(7, 8, 1, 12, 22, 2000, 12, 1));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(8, 9, 1, 20, 42, 2000, 20, 1));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(6, 11, 1, 12, 54, 2000, 12, 1));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 7, 1, 5, 59, 2000, 5, 1));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(9, 14, 1, 10, 69, 2000, 10, 1));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 12, 1, 8, 77, 2000, 8, 1));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(14, 15, 1, 10, 87, 2000, 10, 1));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(15, 20, 1, 21, 108, 2000, 21, 1));
			TestHelper.compareResultsToMemoryOutputFormatter(OutputType.LINK, memoryOutputFormatter, maxIterations,
					resultsMap);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * Test of results for TraditionalStaticAssignment for simple test case using
	 * three time periods.
	 * 
	 * This test case uses the example from the course notes of ITLS6102 Strategic
	 * Transport Planning, Lecture 1 (Overview), the example on Page 122 of the 2019
	 * course notes.
	 * 
	 * Time Period 1 uses route A to B in the example, which has a total route cost
	 * of 85 (the fifth argument in the ResultDto constructor). Time Period 2 uses
	 * route A to C in the example, which has a total route cost of 77. Time Period
	 * 3 uses route A to D in the example, which has a total route cost of 108.
	 */
	@Test
	public void testBasicThreeTimePeriods() {
		try {
			String projectPath = "src\\test\\resources\\basic\\xml\\test13";
			String description = "testBasic13";
			String csvFileName1 = "Time Period 1_2.csv";
			String csvFileName2 = "Time Period 2_2.csv";
			String csvFileName3 = "Time Period 3_2.csv";
			String xmlFileName1 = "Time Period 1.xml";
			String xmlFileName2 = "Time Period 2.xml";
			String xmlFileName3 = "Time Period 3.xml";
			Integer maxIterations = null;

			MemoryOutputFormatter memoryOutputFormatter = TestHelper.setupAndExecuteAssignment(projectPath,
					maxIterations, null, description);
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>();
			TimePeriod timePeriod1 = TimePeriod.getByExternalId(Long.valueOf(0));
			resultsMap.put(timePeriod1, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			Mode mode1 = Mode.getByExternalId(Long.valueOf(1));
			resultsMap.get(timePeriod1).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 4, 1, 10, 10, 2000, 10, 1));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 5, 1, 10, 20, 2000, 10, 1));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 6, 1, 10, 30, 2000, 10, 1));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(8, 3, 1, 8, 38, 2000, 8, 1));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(5, 10, 1, 10, 48, 2000, 10, 1));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(7, 8, 1, 12, 60, 2000, 12, 1));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(6, 11, 1, 12, 72, 2000, 12, 1));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 7, 1, 5, 77, 2000, 5, 1));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 12, 1, 8, 85, 2000, 8, 1));

			TimePeriod timePeriod2 = TimePeriod.getById(Long.valueOf(1));
			resultsMap.put(timePeriod2, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			resultsMap.get(timePeriod2).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 6, 1, 10, 10, 2000, 10, 1));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(6, 11, 1, 12, 22, 2000, 12, 1));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 12, 1, 8, 30, 2000, 8, 1));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 13, 1, 47, 77, 2000, 47, 1));

			TimePeriod timePeriod3 = TimePeriod.getById(Long.valueOf(2));
			resultsMap.put(timePeriod3, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			resultsMap.get(timePeriod3).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod3).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 6, 1, 10, 10, 2000, 10, 1));
			resultsMap.get(timePeriod3).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(7, 8, 1, 12, 22, 2000, 12, 1));
			resultsMap.get(timePeriod3).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(8, 9, 1, 20, 42, 2000, 20, 1));
			resultsMap.get(timePeriod3).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(6, 11, 1, 12, 54, 2000, 12, 1));
			resultsMap.get(timePeriod3).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 7, 1, 5, 59, 2000, 5, 1));
			resultsMap.get(timePeriod3).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(9, 14, 1, 10, 69, 2000, 10, 1));
			resultsMap.get(timePeriod3).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 12, 1, 8, 77, 2000, 8, 1));
			resultsMap.get(timePeriod3).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(14, 15, 1, 10, 87, 2000, 10, 1));
			resultsMap.get(timePeriod3).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(15, 20, 1, 21, 108, 2000, 21, 1));

			TestHelper.compareResultsToMemoryOutputFormatter(OutputType.LINK, memoryOutputFormatter, maxIterations,
					resultsMap);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName1, xmlFileName1);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName2, xmlFileName2);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName3, xmlFileName3);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName1, xmlFileName1);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName2, xmlFileName2);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName3, xmlFileName3);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * Test of results for TraditionalStaticAssignment for simple test case using
	 * the first route choice example from the Traditional Static Assignment Route
	 * Choice Equilibration Test cases.docx document.
	 */
	@Test
	public void testRouteChoiceCompareWithOmniTRANS1() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test1";
			String description = "testRouteChoice1";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = 500;

			MemoryOutputFormatter memoryOutputFormatter = TestHelper.setupAndExecuteAssignment(projectPath,
					maxIterations, 0.0, null, description);
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>();
			TimePeriod timePeriod = TimePeriod.getByExternalId(Long.valueOf(0));
			resultsMap.put(timePeriod, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			Mode mode1 = Mode.getByExternalId(Long.valueOf(1));
			resultsMap.get(timePeriod).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 1, 2000, 0.015, 30, 2000, 1, 66.6666667));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 3, 2000, 0.09, 210, 1000, 1, 11.1111111));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 2, 2000, 0.015, 240, 2000, 1, 66.6666667));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(13, 4, 1000, 0.010000031, 250.0000313, 20000, 1, 99.9996875));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 2, 1000, 0.0103125, 260.3125313, 2000, 1, 96.969697));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(6, 3, 1000, 0.0103125, 270.6250313, 2000, 1, 96.969697));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(12, 6, 1000, 0.010000031, 280.6250625, 20000, 1, 99.9996875));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(15, 5, 1000, 0.010000031, 290.6250938, 20000, 1, 99.9996875));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(5, 1, 1000, 0.0103125, 300.9375938, 2000, 1, 96.969697));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 11, 1000, 0.0103125, 311.2500938, 2000, 1, 96.969697));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 14, 1000, 0.0103125, 321.5625938, 2000, 1, 96.969697));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 16, 1000, 0.0103125, 331.8750938, 2000, 1, 96.969697));
			TestHelper.compareResultsToMemoryOutputFormatter(OutputType.LINK, memoryOutputFormatter, maxIterations,
					resultsMap);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * Test of results for TraditionalStaticAssignment for simple test case using
	 * the second route choice example from the Traditional Static Assignment Route
	 * Choice Equilibration Test cases.docx document.
	 */
	@Test
	public void testRouteChoiceCompareWithOmniTRANS2() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test2";
			String description = "testRouteChoice2";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = 500;

			MemoryOutputFormatter memoryOutputFormatter = TestHelper.setupAndExecuteAssignment(projectPath,
					maxIterations, 0.0, null, description);
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>();
			TimePeriod timePeriod = TimePeriod.getByExternalId(Long.valueOf(0));
			resultsMap.put(timePeriod, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			Mode mode1 = Mode.getByExternalId(Long.valueOf(1));
			resultsMap.get(timePeriod).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 1, 3600, 0.025, 90, 3600, 1, 40));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(1, 4, 1879.2, 0.066416884, 214.8106084, 1200, 1, 15.0564125));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 12, 3600, 0.025, 304.8106084, 3600, 1, 40));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 2, 295.2, 0.033394861, 314.6687712, 1200, 2, 59.8894551));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 4, 295.2, 0.033394861, 324.526934, 1200, 2, 59.8894551));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(1, 3, 1425.6, 0.033399225, 372.1408694, 1200, 1, 29.9408144));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(3, 4, 1425.6, 0.033399225, 419.7548048, 1200, 1, 29.9408144));
			TestHelper.compareResultsToMemoryOutputFormatter(OutputType.LINK, memoryOutputFormatter, maxIterations,
					resultsMap);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}
	
	/**
	 * This test checks that PlanItProject reads the initial costs from a file
	 * correctly, and outputs them after the first iteration.
	 * 
	 * The test input initial costs file uses Link Segment Id to identify link
	 * segments
	 */
	@Test
	public void testRouteChoice2InitialCostsOneIteration() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test2initialCostsOneIteration";
			String description = "testRouteChoice2initialCosts";
			String csvFileName = "Time Period 1_1.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = 1;

			TestHelper.setupAndExecuteAssignment(projectPath,
					"src\\test\\resources\\route_choice\\xml\\test2initialCostsOneIteration\\initial_link_segment_costs.csv",
					null, 0, maxIterations, 0.0, null, description);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * This test runs the same network using one iteration with different initial
	 * costs for each time, checking that the results are different for each time
	 * period.
	 */
	@Test
	public void testRouteChoice2InitialCostsOneIterationThreeTimePeriods() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test2initialCostsOneIterationThreeTimePeriods";
			String description = "test2initialCostsOneIterationThreeTimePeriods";
			String csvFileName1 = "Time Period 1_1.csv";
			String csvFileName2 = "Time Period 2_1.csv";
			String csvFileName3 = "Time Period 3_1.csv";
			String xmlFileName1 = "Time Period 1.xml";
			String xmlFileName2 = "Time Period 2.xml";
			String xmlFileName3 = "Time Period 3.xml";
			Integer maxIterations = 1;
			Map<Long, String> initialLinkSegmentLocationsPerTimePeriod = new HashMap<Long, String>();
			initialLinkSegmentLocationsPerTimePeriod.put((long) 0,
					"src\\test\\resources\\route_choice\\xml\\test2initialCostsOneIterationThreeTimePeriods\\initial_link_segment_costs_time_period_1.csv");
			initialLinkSegmentLocationsPerTimePeriod.put((long) 1,
					"src\\test\\resources\\route_choice\\xml\\test2initialCostsOneIterationThreeTimePeriods\\initial_link_segment_costs_time_period_2.csv");
			initialLinkSegmentLocationsPerTimePeriod.put((long) 2,
					"src\\test\\resources\\route_choice\\xml\\test2initialCostsOneIterationThreeTimePeriods\\initial_link_segment_costs_time_period_3.csv");

			TestHelper.setupAndExecuteAssignment(projectPath, initialLinkSegmentLocationsPerTimePeriod, maxIterations,
					0.0, null, description);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName1, xmlFileName1);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName2, xmlFileName2);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName3, xmlFileName3);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName1, xmlFileName1);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName2, xmlFileName2);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName3, xmlFileName3);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * This test check that PlanItProject reads the initial costs from a file
	 * correctly, and outputs them after the first iteration.
	 * 
	 * The test input initial costs file uses Link Segment External Id to identify
	 * link segments
	 */
	@Test
	public void testRouteChoice2InitialCostsOneIterationExternalIds() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test2initialCostsOneIterationExternalIds";
			String description = "testRouteChoice2initialCosts";
			String csvFileName = "Time Period 1_1.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = 1;

			TestHelper.setupAndExecuteAssignment(projectPath,
					"src\\test\\resources\\route_choice\\xml\\test2initialCostsOneIterationExternalIds\\initial_link_segment_costs.csv",
					null, 0, maxIterations, 0.0, null, description);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * This test check that PlanItProject reads the initial costs from a file
	 * correctly, and outputs them after 500 iterations.
	 * 
	 * The test input initial costs file uses Link Segment Id to identify link
	 * segments
	 */
	@Test
	public void testRouteChoice2InitialCosts500Iterations() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test2initialCosts500iterations";
			String description = "testRouteChoice2initialCosts";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = 500;

			TestHelper.setupAndExecuteAssignment(projectPath,
					"src\\test\\resources\\route_choice\\xml\\test2initialCosts500iterations\\initial_link_segment_costs.csv",
					null, 0, maxIterations, 0.0, null, description);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * This test runs the same network using one iteration with different initial
	 * costs for each time, running the test for 500 iterations.
	 */
	@Test
	public void testRouteChoice2InitialCosts500IterationsThreeTimePeriods() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test2initialCosts500IterationsThreeTimePeriods";
			String description = "test2initialCosts500IterationsThreeTimePeriods";
			String csvFileName1 = "Time Period 1_500.csv";
			String csvFileName2 = "Time Period 2_500.csv";
			String csvFileName3 = "Time Period 3_500.csv";
			String xmlFileName1 = "Time Period 1.xml";
			String xmlFileName2 = "Time Period 2.xml";
			String xmlFileName3 = "Time Period 3.xml";
			Integer maxIterations = 500;
			Map<Long, String> initialLinkSegmentLocationsPerTimePeriod = new HashMap<Long, String>();
			initialLinkSegmentLocationsPerTimePeriod.put((long) 0,
					"src\\test\\resources\\route_choice\\xml\\test2initialCosts500IterationsThreeTimePeriods\\initial_link_segment_costs_time_period_1.csv");
			initialLinkSegmentLocationsPerTimePeriod.put((long) 1,
					"src\\test\\resources\\route_choice\\xml\\test2initialCosts500IterationsThreeTimePeriods\\initial_link_segment_costs_time_period_2.csv");
			initialLinkSegmentLocationsPerTimePeriod.put((long) 2,
					"src\\test\\resources\\route_choice\\xml\\test2initialCosts500IterationsThreeTimePeriods\\initial_link_segment_costs_time_period_3.csv");

			TestHelper.setupAndExecuteAssignment(projectPath, initialLinkSegmentLocationsPerTimePeriod, maxIterations,
					0.0, null, description);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName1, xmlFileName1);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName2, xmlFileName2);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName3, xmlFileName3);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName1, xmlFileName1);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName2, xmlFileName2);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName3, xmlFileName3);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * This test check that PlanItProject reads the initial costs from a file
	 * correctly, and outputs them after 500 iterations.
	 * 
	 * The test input initial costs file uses Link Segment External Id to identify
	 * link segments
	 */
	@Test
	public void testRouteChoice2InitialCosts500IterationsExternalIds() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test2initialCosts500iterationsExternalIds";
			String description = "testRouteChoice2initialCosts";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = 500;

			TestHelper.setupAndExecuteAssignment(projectPath,
					"src\\test\\resources\\route_choice\\xml\\test2initialCosts500iterationsExternalIds\\initial_link_segment_costs.csv",
					null, 0, maxIterations, 0.0, null, description);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * Test of results for TraditionalStaticAssignment for simple test case using
	 * the third route choice example from the Traditional Static Assignment Route
	 * Choice Equilibration Test cases.docx document.
	 */
	@Test
	public void testRouteChoiceCompareWithOmniTRANS3() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test3";
			String description = "testRouteChoice3";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = 500;

			MemoryOutputFormatter memoryOutputFormatter = TestHelper.setupAndExecuteAssignment(projectPath,
					maxIterations, 0.0, null, description);
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>();
			TimePeriod timePeriod = TimePeriod.getByExternalId(Long.valueOf(0));
			resultsMap.put(timePeriod, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			Mode mode1 = Mode.getByExternalId(Long.valueOf(1));
			resultsMap.get(timePeriod).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 1, 8000, 0.03, 240, 8000, 2, 66.6666667));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 3, 4048, 0.053416028, 456.2280829, 3000, 2, 37.4419451));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 2, 3952, 0.023870955, 550.566099, 5000, 2, 83.7838268));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 3, 3952, 0.029450575, 666.954771, 4000, 2, 67.9103891));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 5, 4144, 0.205796205, 1519.774245, 2000, 2, 9.7183522));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 4, 3856, 0.047059372, 1701.235183, 3000, 2, 42.4995047));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 5, 3856, 0.156988071, 2306.581184, 2000, 2, 12.7398215));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(5, 12, 8000, 2.58, 22946.58118, 2000, 2, 0.7751938));
			TestHelper.compareResultsToMemoryOutputFormatter(OutputType.LINK, memoryOutputFormatter, maxIterations,
					resultsMap);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * Test of results for TraditionalStaticAssignment for simple test case using
	 * the fourth route choice example from the Traditional Static Assignment Route
	 * Choice Equilibration Test cases.docx document.
	 * 
	 * This test case uses the <odrowmatrix> method in the macroscopicinput.xml file
	 * to define the OD demands input matrix.
	 */
	@Test
	public void testRouteChoiceCompareWithOmniTRANS4() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test4";
			String description = "testRouteChoice4";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = 500;

			MemoryOutputFormatter memoryOutputFormatter = TestHelper.setupAndExecuteAssignment(projectPath,
					maxIterations, 0.0, null, description);
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>();
			TimePeriod timePeriod = TimePeriod.getByExternalId(Long.valueOf(0));
			resultsMap.put(timePeriod, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			Mode mode1 = Mode.getByExternalId(Long.valueOf(1));
			resultsMap.get(timePeriod).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 9, 0.6, 0.029, 0.0174, 1500, 2.9, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(9, 12, 0.6, 0.029, 0.0348, 1500, 2.9, 100));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(12, 11, 482.4, 0.030161746, 14.58482622, 1500, 3, 99.4637383));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(11, 12, 482.4, 0.030161746, 29.13485243, 1500, 3, 99.4637383));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 16, 483, 0.010054184, 33.99102332, 1500, 1, 99.4610798));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(16, 12, 483, 0.010054184, 38.84719421, 1500, 1, 99.4610798));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(9, 13, 0.6, 0.01, 38.85319421, 1500, 1, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(13, 9, 0.6, 0.01, 38.85919421, 1500, 1, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(10, 11, 17.6, 0.03, 39.38719422, 1500, 3, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 10, 17.6, 0.03, 39.91519422, 1500, 3, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(10, 14, 17.6, 0.01, 40.09119422, 1500, 1, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(14, 10, 17.6, 0.01, 40.26719422, 1500, 1, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 15, 500, 0.010062225, 45.29830657, 1500, 1, 99.381601));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(15, 11, 500, 0.010062225, 50.32941893, 1500, 1, 99.381601));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 5, 899.4, 0.01064627, 59.90467441, 1500, 1, 93.9296086));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(5, 1, 899.4, 0.01064627, 69.47992989, 1500, 1, 93.9296086));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(1, 4, 1087.4, 0.010240864, 80.61584489, 1500, 0.9, 87.8832139));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(4, 1, 1087.4, 0.010240864, 91.75175988, 1500, 0.9, 87.8832139));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 2, 1012, 0.009933896, 101.804863, 1500, 0.9, 90.598892));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 1, 1012, 0.009933896, 111.857966, 1500, 0.9, 90.598892));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(2, 6, 1582.4, 0.016192006, 137.4801958, 1500, 1, 61.7588717));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(6, 2, 1582.4, 0.016192006, 163.1024255, 1500, 1, 61.7588717));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 3, 994.4, 0.01096723, 174.0082393, 1500, 1, 91.1807244));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 2, 994.4, 0.01096723, 184.9140531, 1500, 1, 91.1807244));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 7, 1900, 0.02284408, 228.3178046, 1500, 1, 43.7750179));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(7, 3, 1900, 0.02284408, 271.7215562, 1500, 1, 43.7750179));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 4, 905.6, 0.010660206, 281.3754383, 1500, 1, 93.8068219));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 3, 905.6, 0.010660206, 291.0293204, 1500, 1, 93.8068219));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 8, 1617, 0.016736043, 318.0915022, 1500, 1, 59.7512799));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(8, 4, 1617, 0.016736043, 345.153684, 1500, 1, 59.7512799));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(16, 23, 483, 0.020000055, 354.8137105, 10000, 1, 49.9998628));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(23, 16, 483, 0.020000055, 364.473737, 10000, 1, 49.9998628));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(23, 8, 1617, 0.02000682, 396.8247653, 10000, 1, 49.9829552));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(8, 23, 1617, 0.02000682, 429.1757937, 10000, 1, 49.9829552));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(13, 21, 0.6, 0.02, 429.1877937, 10000, 1, 50));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(21, 13, 0.6, 0.02, 429.1997937, 10000, 1, 50));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(21, 5, 899.4, 0.020000654, 447.1883822, 10000, 1, 49.9983642));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(5, 21, 899.4, 0.020000654, 465.1769707, 10000, 1, 49.9983642));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(14, 22, 17.6, 0.02, 465.5289707, 10000, 1, 50));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(22, 14, 17.6, 0.02, 465.8809707, 10000, 1, 50));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(22, 6, 1582.4, 0.020006269, 497.5388914, 10000, 1, 49.9843314));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(6, 22, 1582.4, 0.020006269, 529.1968121, 10000, 1, 49.9843314));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(15, 24, 500, 0.020000063, 539.1968436, 10000, 1, 49.9998425));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(24, 15, 500, 0.020000063, 549.1968751, 10000, 1, 49.9998425));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(24, 7, 1900, 0.020013005, 587.2215839, 10000, 1, 49.9675095));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(7, 24, 1900, 0.020013005, 625.2462927, 10000, 1, 49.9675095));
			TestHelper.compareResultsToMemoryOutputFormatter(OutputType.LINK, memoryOutputFormatter, maxIterations,
					resultsMap);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * Test of results for TraditionalStaticAssignment for simple test case using
	 * the fourth route choice example from the Traditional Static Assignment Route
	 * Choice Equilibration Test cases.docx document.
	 * 
	 * This test case uses two time periods.
	 * 
	 * This test case uses the <odrowmatrix> method in the macroscopicinput.xml file
	 * to define the OD demands input matrix.
	 */
	@Test
	public void testRouteChoiceCompareWithOmniTRANS4UsingTwoTimePeriods() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test42";
			String description = "testRouteChoice42";
			String csvFileName1 = "Time Period 1_500.csv";
			String csvFileName2 = "Time Period 2_500.csv";
			String xmlFileName1 = "Time Period 1.xml";
			String xmlFileName2 = "Time Period 2.xml";
			Integer maxIterations = 500;

			MemoryOutputFormatter memoryOutputFormatter = TestHelper.setupAndExecuteAssignment(projectPath,
					maxIterations, 0.0, null, description);
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>();
			TimePeriod timePeriod1 = TimePeriod.getByExternalId(Long.valueOf(0));
			resultsMap.put(timePeriod1, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			Mode mode1 = Mode.getByExternalId(Long.valueOf(1));
			resultsMap.get(timePeriod1).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 9, 0.6, 0.029, 0.0174, 1500, 2.9, 100));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(9, 12, 0.6, 0.029, 0.0348, 1500, 2.9, 100));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(12, 11, 482.4, 0.030161746, 14.58482622, 1500, 3, 99.4637383));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(11, 12, 482.4, 0.030161746, 29.13485243, 1500, 3, 99.4637383));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 16, 483, 0.010054184, 33.99102332, 1500, 1, 99.4610798));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(16, 12, 483, 0.010054184, 38.84719421, 1500, 1, 99.4610798));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(9, 13, 0.6, 0.01, 38.85319421, 1500, 1, 100));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(13, 9, 0.6, 0.01, 38.85919421, 1500, 1, 100));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(10, 11, 17.6, 0.03, 39.38719422, 1500, 3, 100));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 10, 17.6, 0.03, 39.91519422, 1500, 3, 100));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(10, 14, 17.6, 0.01, 40.09119422, 1500, 1, 100));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(14, 10, 17.6, 0.01, 40.26719422, 1500, 1, 100));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 15, 500, 0.010062225, 45.29830657, 1500, 1, 99.381601));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(15, 11, 500, 0.010062225, 50.32941893, 1500, 1, 99.381601));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 5, 899.4, 0.01064627, 59.90467441, 1500, 1, 93.9296086));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(5, 1, 899.4, 0.01064627, 69.47992989, 1500, 1, 93.9296086));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(1, 4, 1087.4, 0.010240864, 80.61584489, 1500, 0.9, 87.8832139));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(4, 1, 1087.4, 0.010240864, 91.75175988, 1500, 0.9, 87.8832139));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 2, 1012, 0.009933896, 101.804863, 1500, 0.9, 90.598892));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 1, 1012, 0.009933896, 111.857966, 1500, 0.9, 90.598892));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(2, 6, 1582.4, 0.016192006, 137.4801958, 1500, 1, 61.7588717));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(6, 2, 1582.4, 0.016192006, 163.1024255, 1500, 1, 61.7588717));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 3, 994.4, 0.01096723, 174.0082393, 1500, 1, 91.1807244));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 2, 994.4, 0.01096723, 184.9140531, 1500, 1, 91.1807244));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 7, 1900, 0.02284408, 228.3178046, 1500, 1, 43.7750179));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(7, 3, 1900, 0.02284408, 271.7215562, 1500, 1, 43.7750179));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 4, 905.6, 0.010660206, 281.3754383, 1500, 1, 93.8068219));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 3, 905.6, 0.010660206, 291.0293204, 1500, 1, 93.8068219));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 8, 1617, 0.016736043, 318.0915022, 1500, 1, 59.7512799));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(8, 4, 1617, 0.016736043, 345.153684, 1500, 1, 59.7512799));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(16, 23, 483, 0.020000055, 354.8137105, 10000, 1, 49.9998628));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(23, 16, 483, 0.020000055, 364.473737, 10000, 1, 49.9998628));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(23, 8, 1617, 0.02000682, 396.8247653, 10000, 1, 49.9829552));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(8, 23, 1617, 0.02000682, 429.1757937, 10000, 1, 49.9829552));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(13, 21, 0.6, 0.02, 429.1877937, 10000, 1, 50));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(21, 13, 0.6, 0.02, 429.1997937, 10000, 1, 50));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(21, 5, 899.4, 0.020000654, 447.1883822, 10000, 1, 49.9983642));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(5, 21, 899.4, 0.020000654, 465.1769707, 10000, 1, 49.9983642));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(14, 22, 17.6, 0.02, 465.5289707, 10000, 1, 50));
			resultsMap.get(timePeriod1).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(22, 14, 17.6, 0.02, 465.8809707, 10000, 1, 50));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(22, 6, 1582.4, 0.020006269, 497.5388914, 10000, 1, 49.9843314));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(6, 22, 1582.4, 0.020006269, 529.1968121, 10000, 1, 49.9843314));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(15, 24, 500, 0.020000063, 539.1968436, 10000, 1, 49.9998425));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(24, 15, 500, 0.020000063, 549.1968751, 10000, 1, 49.9998425));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(24, 7, 1900, 0.020013005, 587.2215839, 10000, 1, 49.9675095));
			resultsMap.get(timePeriod1).get(mode1).add(
					new LinkSegmentExpectedResultsDto(7, 24, 1900, 0.020013005, 625.2462927, 10000, 1, 49.9675095));
			TimePeriod timePeriod2 = TimePeriod.getById(Long.valueOf(1));
			resultsMap.put(timePeriod2, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			resultsMap.get(timePeriod2).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 9, 0.6, 0.029, 0.0174, 1500, 2.9, 100));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(9, 12, 0.6, 0.029, 0.0348, 1500, 2.9, 100));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(12, 11, 482.4, 0.030161746, 14.58482622, 1500, 3, 99.4637383));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(11, 12, 482.4, 0.030161746, 29.13485243, 1500, 3, 99.4637383));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 16, 483, 0.010054184, 33.99102332, 1500, 1, 99.4610798));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(16, 12, 483, 0.010054184, 38.84719421, 1500, 1, 99.4610798));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(9, 13, 0.6, 0.01, 38.85319421, 1500, 1, 100));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(13, 9, 0.6, 0.01, 38.85919421, 1500, 1, 100));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(10, 11, 17.6, 0.03, 39.38719422, 1500, 3, 100));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 10, 17.6, 0.03, 39.91519422, 1500, 3, 100));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(10, 14, 17.6, 0.01, 40.09119422, 1500, 1, 100));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(14, 10, 17.6, 0.01, 40.26719422, 1500, 1, 100));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 15, 500, 0.010062225, 45.29830657, 1500, 1, 99.381601));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(15, 11, 500, 0.010062225, 50.32941893, 1500, 1, 99.381601));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 5, 899.4, 0.01064627, 59.90467441, 1500, 1, 93.9296086));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(5, 1, 899.4, 0.01064627, 69.47992989, 1500, 1, 93.9296086));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(1, 4, 1087.4, 0.010240864, 80.61584489, 1500, 0.9, 87.8832139));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(4, 1, 1087.4, 0.010240864, 91.75175988, 1500, 0.9, 87.8832139));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 2, 1012, 0.009933896, 101.804863, 1500, 0.9, 90.598892));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 1, 1012, 0.009933896, 111.857966, 1500, 0.9, 90.598892));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(2, 6, 1582.4, 0.016192006, 137.4801958, 1500, 1, 61.7588717));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(6, 2, 1582.4, 0.016192006, 163.1024255, 1500, 1, 61.7588717));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 3, 994.4, 0.01096723, 174.0082393, 1500, 1, 91.1807244));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 2, 994.4, 0.01096723, 184.9140531, 1500, 1, 91.1807244));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 7, 1900, 0.02284408, 228.3178046, 1500, 1, 43.7750179));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(7, 3, 1900, 0.02284408, 271.7215562, 1500, 1, 43.7750179));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 4, 905.6, 0.010660206, 281.3754383, 1500, 1, 93.8068219));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 3, 905.6, 0.010660206, 291.0293204, 1500, 1, 93.8068219));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 8, 1617, 0.016736043, 318.0915022, 1500, 1, 59.7512799));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(8, 4, 1617, 0.016736043, 345.153684, 1500, 1, 59.7512799));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(16, 23, 483, 0.020000055, 354.8137105, 10000, 1, 49.9998628));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(23, 16, 483, 0.020000055, 364.473737, 10000, 1, 49.9998628));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(23, 8, 1617, 0.02000682, 396.8247653, 10000, 1, 49.9829552));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(8, 23, 1617, 0.02000682, 429.1757937, 10000, 1, 49.9829552));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(13, 21, 0.6, 0.02, 429.1877937, 10000, 1, 50));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(21, 13, 0.6, 0.02, 429.1997937, 10000, 1, 50));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(21, 5, 899.4, 0.020000654, 447.1883822, 10000, 1, 49.9983642));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(5, 21, 899.4, 0.020000654, 465.1769707, 10000, 1, 49.9983642));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(14, 22, 17.6, 0.02, 465.5289707, 10000, 1, 50));
			resultsMap.get(timePeriod2).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(22, 14, 17.6, 0.02, 465.8809707, 10000, 1, 50));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(22, 6, 1582.4, 0.020006269, 497.5388914, 10000, 1, 49.9843314));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(6, 22, 1582.4, 0.020006269, 529.1968121, 10000, 1, 49.9843314));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(15, 24, 500, 0.020000063, 539.1968436, 10000, 1, 49.9998425));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(24, 15, 500, 0.020000063, 549.1968751, 10000, 1, 49.9998425));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(24, 7, 1900, 0.020013005, 587.2215839, 10000, 1, 49.9675095));
			resultsMap.get(timePeriod2).get(mode1).add(
					new LinkSegmentExpectedResultsDto(7, 24, 1900, 0.020013005, 625.2462927, 10000, 1, 49.9675095));
			TestHelper.compareResultsToMemoryOutputFormatter(OutputType.LINK, memoryOutputFormatter, maxIterations,
					resultsMap);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName1, xmlFileName1);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName2, xmlFileName2);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName1, xmlFileName1);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName2, xmlFileName2);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * Test of results for TraditionalStaticAssignment for simple test case using
	 * the fourth route choice from the Traditional Static Assignment Route Choice
	 * Equilibration Test cases.docx document.
	 * 
	 * This test case uses the <odrawmatrix> method in the macroscopicinput.xml file
	 * to define the OD demands input matrix.
	 */
	@Test
	public void testRouteChoiceCompareWithOmniTRANS4UsingRawMatrixToSetODDemands() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test4raw";
			String description = "testRouteChoice4raw";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = 500;

			MemoryOutputFormatter memoryOutputFormatter = TestHelper.setupAndExecuteAssignment(projectPath,
					maxIterations, 0.0, null, description);
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>();
			TimePeriod timePeriod = TimePeriod.getByExternalId(Long.valueOf(0));
			resultsMap.put(timePeriod, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			Mode mode1 = Mode.getByExternalId(Long.valueOf(1));
			resultsMap.get(timePeriod).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 9, 0.6, 0.029, 0.0174, 1500, 2.9, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(9, 12, 0.6, 0.029, 0.0348, 1500, 2.9, 100));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(12, 11, 482.4, 0.030161746, 14.58482622, 1500, 3, 99.4637383));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(11, 12, 482.4, 0.030161746, 29.13485243, 1500, 3, 99.4637383));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 16, 483, 0.010054184, 33.99102332, 1500, 1, 99.4610798));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(16, 12, 483, 0.010054184, 38.84719421, 1500, 1, 99.4610798));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(9, 13, 0.6, 0.01, 38.85319421, 1500, 1, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(13, 9, 0.6, 0.01, 38.85919421, 1500, 1, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(10, 11, 17.6, 0.03, 39.38719422, 1500, 3, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 10, 17.6, 0.03, 39.91519422, 1500, 3, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(10, 14, 17.6, 0.01, 40.09119422, 1500, 1, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(14, 10, 17.6, 0.01, 40.26719422, 1500, 1, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 15, 500, 0.010062225, 45.29830657, 1500, 1, 99.381601));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(15, 11, 500, 0.010062225, 50.32941893, 1500, 1, 99.381601));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 5, 899.4, 0.01064627, 59.90467441, 1500, 1, 93.9296086));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(5, 1, 899.4, 0.01064627, 69.47992989, 1500, 1, 93.9296086));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(1, 4, 1087.4, 0.010240864, 80.61584489, 1500, 0.9, 87.8832139));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(4, 1, 1087.4, 0.010240864, 91.75175988, 1500, 0.9, 87.8832139));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 2, 1012, 0.009933896, 101.804863, 1500, 0.9, 90.598892));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 1, 1012, 0.009933896, 111.857966, 1500, 0.9, 90.598892));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(2, 6, 1582.4, 0.016192006, 137.4801958, 1500, 1, 61.7588717));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(6, 2, 1582.4, 0.016192006, 163.1024255, 1500, 1, 61.7588717));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 3, 994.4, 0.01096723, 174.0082393, 1500, 1, 91.1807244));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 2, 994.4, 0.01096723, 184.9140531, 1500, 1, 91.1807244));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 7, 1900, 0.02284408, 228.3178046, 1500, 1, 43.7750179));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(7, 3, 1900, 0.02284408, 271.7215562, 1500, 1, 43.7750179));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 4, 905.6, 0.010660206, 281.3754383, 1500, 1, 93.8068219));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 3, 905.6, 0.010660206, 291.0293204, 1500, 1, 93.8068219));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 8, 1617, 0.016736043, 318.0915022, 1500, 1, 59.7512799));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(8, 4, 1617, 0.016736043, 345.153684, 1500, 1, 59.7512799));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(16, 23, 483, 0.020000055, 354.8137105, 10000, 1, 49.9998628));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(23, 16, 483, 0.020000055, 364.473737, 10000, 1, 49.9998628));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(23, 8, 1617, 0.02000682, 396.8247653, 10000, 1, 49.9829552));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(8, 23, 1617, 0.02000682, 429.1757937, 10000, 1, 49.9829552));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(13, 21, 0.6, 0.02, 429.1877937, 10000, 1, 50));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(21, 13, 0.6, 0.02, 429.1997937, 10000, 1, 50));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(21, 5, 899.4, 0.020000654, 447.1883822, 10000, 1, 49.9983642));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(5, 21, 899.4, 0.020000654, 465.1769707, 10000, 1, 49.9983642));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(14, 22, 17.6, 0.02, 465.5289707, 10000, 1, 50));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(22, 14, 17.6, 0.02, 465.8809707, 10000, 1, 50));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(22, 6, 1582.4, 0.020006269, 497.5388914, 10000, 1, 49.9843314));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(6, 22, 1582.4, 0.020006269, 529.1968121, 10000, 1, 49.9843314));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(15, 24, 500, 0.020000063, 539.1968436, 10000, 1, 49.9998425));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(24, 15, 500, 0.020000063, 549.1968751, 10000, 1, 49.9998425));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(24, 7, 1900, 0.020013005, 587.2215839, 10000, 1, 49.9675095));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(7, 24, 1900, 0.020013005, 625.2462927, 10000, 1, 49.9675095));
			TestHelper.compareResultsToMemoryOutputFormatter(OutputType.LINK, memoryOutputFormatter, maxIterations,
					resultsMap);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * Test of results for TraditionalStaticAssignment for simple test case using
	 * the fourth route choice example from the Traditional Static Assignment Route
	 * Choice Equilibration Test cases.docx document.
	 * 
	 * This test case uses the <odrawmatrix> method with the plus sign as separator
	 * in the macroscopicinput.xml file to define the OD demands input matrix.
	 */
	@Test
	public void testRouteChoiceCompareWithOmniTRANS4UsingRawMatrixWithPlusSignAsSeparatorToSetODDemands() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test4raw2";
			String description = "testRouteChoice4raw2";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = 500;

			MemoryOutputFormatter memoryOutputFormatter = TestHelper.setupAndExecuteAssignment(projectPath,
					maxIterations, 0.0, null, description);
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>();
			TimePeriod timePeriod = TimePeriod.getByExternalId(Long.valueOf(0));
			resultsMap.put(timePeriod, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			Mode mode1 = Mode.getByExternalId(Long.valueOf(1));
			resultsMap.get(timePeriod).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 9, 0.6, 0.029, 0.0174, 1500, 2.9, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(9, 12, 0.6, 0.029, 0.0348, 1500, 2.9, 100));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(12, 11, 482.4, 0.030161746, 14.58482622, 1500, 3, 99.4637383));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(11, 12, 482.4, 0.030161746, 29.13485243, 1500, 3, 99.4637383));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(12, 16, 483, 0.010054184, 33.99102332, 1500, 1, 99.4610798));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(16, 12, 483, 0.010054184, 38.84719421, 1500, 1, 99.4610798));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(9, 13, 0.6, 0.01, 38.85319421, 1500, 1, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(13, 9, 0.6, 0.01, 38.85919421, 1500, 1, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(10, 11, 17.6, 0.03, 39.38719422, 1500, 3, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 10, 17.6, 0.03, 39.91519422, 1500, 3, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(10, 14, 17.6, 0.01, 40.09119422, 1500, 1, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(14, 10, 17.6, 0.01, 40.26719422, 1500, 1, 100));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(11, 15, 500, 0.010062225, 45.29830657, 1500, 1, 99.381601));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(15, 11, 500, 0.010062225, 50.32941893, 1500, 1, 99.381601));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 5, 899.4, 0.01064627, 59.90467441, 1500, 1, 93.9296086));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(5, 1, 899.4, 0.01064627, 69.47992989, 1500, 1, 93.9296086));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(1, 4, 1087.4, 0.010240864, 80.61584489, 1500, 0.9, 87.8832139));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(4, 1, 1087.4, 0.010240864, 91.75175988, 1500, 0.9, 87.8832139));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(1, 2, 1012, 0.009933896, 101.804863, 1500, 0.9, 90.598892));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 1, 1012, 0.009933896, 111.857966, 1500, 0.9, 90.598892));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(2, 6, 1582.4, 0.016192006, 137.4801958, 1500, 1, 61.7588717));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(6, 2, 1582.4, 0.016192006, 163.1024255, 1500, 1, 61.7588717));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(2, 3, 994.4, 0.01096723, 174.0082393, 1500, 1, 91.1807244));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 2, 994.4, 0.01096723, 184.9140531, 1500, 1, 91.1807244));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 7, 1900, 0.02284408, 228.3178046, 1500, 1, 43.7750179));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(7, 3, 1900, 0.02284408, 271.7215562, 1500, 1, 43.7750179));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(3, 4, 905.6, 0.010660206, 281.3754383, 1500, 1, 93.8068219));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 3, 905.6, 0.010660206, 291.0293204, 1500, 1, 93.8068219));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(4, 8, 1617, 0.016736043, 318.0915022, 1500, 1, 59.7512799));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(8, 4, 1617, 0.016736043, 345.153684, 1500, 1, 59.7512799));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(16, 23, 483, 0.020000055, 354.8137105, 10000, 1, 49.9998628));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(23, 16, 483, 0.020000055, 364.473737, 10000, 1, 49.9998628));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(23, 8, 1617, 0.02000682, 396.8247653, 10000, 1, 49.9829552));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(8, 23, 1617, 0.02000682, 429.1757937, 10000, 1, 49.9829552));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(13, 21, 0.6, 0.02, 429.1877937, 10000, 1, 50));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(21, 13, 0.6, 0.02, 429.1997937, 10000, 1, 50));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(21, 5, 899.4, 0.020000654, 447.1883822, 10000, 1, 49.9983642));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(5, 21, 899.4, 0.020000654, 465.1769707, 10000, 1, 49.9983642));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(14, 22, 17.6, 0.02, 465.5289707, 10000, 1, 50));
			resultsMap.get(timePeriod).get(mode1)
					.add(new LinkSegmentExpectedResultsDto(22, 14, 17.6, 0.02, 465.8809707, 10000, 1, 50));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(22, 6, 1582.4, 0.020006269, 497.5388914, 10000, 1, 49.9843314));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(6, 22, 1582.4, 0.020006269, 529.1968121, 10000, 1, 49.9843314));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(15, 24, 500, 0.020000063, 539.1968436, 10000, 1, 49.9998425));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(24, 15, 500, 0.020000063, 549.1968751, 10000, 1, 49.9998425));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(24, 7, 1900, 0.020013005, 587.2215839, 10000, 1, 49.9675095));
			resultsMap.get(timePeriod).get(mode1).add(
					new LinkSegmentExpectedResultsDto(7, 24, 1900, 0.020013005, 625.2462927, 10000, 1, 49.9675095));
			TestHelper.compareResultsToMemoryOutputFormatter(OutputType.LINK, memoryOutputFormatter, maxIterations,
					resultsMap);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}

	/**
	 * Test of results for TraditionalStaticAssignment for simple test case using
	 * the fifth route choice example from the Traditional Static Assignment Route
	 * Choice Equilibration Test cases.docx document.
	 * 
	 * This test case uses two modes and some modes are not allowed on some links.
	 */
	@Test
	public void testRouteChoiceCompareWithOmniTRANS5() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test5";
			String description = "testRouteChoice5";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = 500;

			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters = (physicalNetwork,
					bprLinkTravelTimeCost) -> {
				MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) physicalNetwork;
				MacroscopicLinkSegmentType macroscopiclinkSegmentType = macroscopicNetwork
						.findMacroscopicLinkSegmentTypeByExternalId(1);
				Mode mode = Mode.getByExternalId(2);
				bprLinkTravelTimeCost.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
			};

			MemoryOutputFormatter memoryOutputFormatter = TestHelper.setupAndExecuteAssignment(projectPath,
					maxIterations, 0.0, setCostParameters, description);
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>();
			TimePeriod timePeriod = TimePeriod.getByExternalId(Long.valueOf(0));
			resultsMap.put(timePeriod, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			Mode mode1 = Mode.getByExternalId(Long.valueOf(1));
			resultsMap.get(timePeriod).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(11, 1, 3000,
					0.0370117187500001, 111.03515625, 3600.0, 1.0, 27.0184697));
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(1, 4, 1926,
					0.0717190999688149, 249.166142789938, 1200.0, 1.0, 13.9432871));
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(4, 12, 3000,
					0.0370117187500001, 360.201299039938, 3600.0, 1.0, 27.0184697));
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(1, 2, 6,
					0.0448543857828265, 360.470425354635, 1200.0, 2.0, 44.5887278));
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(2, 4, 6,
					0.0448543857828265, 360.739551669332, 1200.0, 2.0, 44.5887278));
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(1, 3, 1068,
					0.0360507068130539, 399.241706545674, 1200.0, 1.0, 27.7387072));
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(3, 4, 1068,
					0.0360507068130539, 437.743861422015, 1200.0, 1.0, 27.7387072));
			Mode mode2 = Mode.getByExternalId(Long.valueOf(2));
			resultsMap.get(timePeriod).put(mode2, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod).get(mode2).add(new LinkSegmentExpectedResultsDto(11, 1, 1500,
					0.063673202685543, 95.5098040283147, 3600.0, 1.0, 15.705194));
			resultsMap.get(timePeriod).get(mode2).add(new LinkSegmentExpectedResultsDto(4, 12, 1500,
					0.063673202685543, 191.019608056629, 3600.0, 1.0, 15.705194));
			resultsMap.get(timePeriod).get(mode2).add(new LinkSegmentExpectedResultsDto(1, 2, 1086,
					0.0611216251945281, 257.397693017887, 1200.0, 2.0, 32.721643));
			resultsMap.get(timePeriod).get(mode2).add(new LinkSegmentExpectedResultsDto(2, 4, 1086,
					0.0611216251945281, 323.775777979144, 1200.0, 2.0, 32.721643));
			resultsMap.get(timePeriod).get(mode2).add(new LinkSegmentExpectedResultsDto(1, 3, 414,
					0.061091236386479, 349.067549843147, 1200.0, 1.0, 16.3689599));
			resultsMap.get(timePeriod).get(mode2).add(new LinkSegmentExpectedResultsDto(3, 4, 414,
					0.061091236386479, 374.359321707149, 1200.0, 1.0, 16.3689599));
			TestHelper.compareResultsToMemoryOutputFormatter(OutputType.LINK, memoryOutputFormatter, maxIterations,
					resultsMap);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}
	
	/**
	 * Test of results for TraditionalStaticAssignment for simple test case using
	 * the fifth route choice example from the Traditional Static Assignment Route
	 * Choice Equilibration Test cases.docx document.
	 * 
	 * This test case identifies links using link Id (all other tests use link
	 * external Id).
	 */
	@Test
	public void testRouteChoiceCompareWithOmniTRANS5IdentifyLinksById() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test5IdentifyLinksByLinkId";
			String description = "testRouteChoice5";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = 500;

			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters = (physicalNetwork,
					bprLinkTravelTimeCost) -> {
				MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) physicalNetwork;
				MacroscopicLinkSegmentType macroscopiclinkSegmentType = macroscopicNetwork
						.findMacroscopicLinkSegmentTypeByExternalId(1);
				Mode mode = Mode.getByExternalId(2);
				bprLinkTravelTimeCost.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
			};

			Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties = (
					linkOutputTypeConfiguration) -> {
				try {
					linkOutputTypeConfiguration.addAllProperties();
					linkOutputTypeConfiguration.removeProperty(OutputProperty.RUN_ID);
					linkOutputTypeConfiguration.removeProperty(OutputProperty.LINK_SEGMENT_EXTERNAL_ID);
					linkOutputTypeConfiguration.removeProperty(OutputProperty.UPSTREAM_NODE_EXTERNAL_ID);
					linkOutputTypeConfiguration.removeProperty(OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID);
					linkOutputTypeConfiguration.removeProperty(OutputProperty.ITERATION_INDEX);
					linkOutputTypeConfiguration.removeProperty(OutputProperty.DESTINATION_ZONE_ID);
					linkOutputTypeConfiguration.removeProperty(OutputProperty.ORIGIN_ZONE_ID);
					linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_ID);
					linkOutputTypeConfiguration.removeProperty(OutputProperty.TOTAL_COST_TO_END_NODE);
				} catch (PlanItException e) {
					PlanItLogger.severe(e.getMessage());
				}
			};

			MemoryOutputFormatter memoryOutputFormatter = TestHelper.setupAndExecuteAssignment(projectPath,
					setOutputTypeConfigurationProperties, maxIterations, 0.0, setCostParameters, description);
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>();
			TimePeriod timePeriod = TimePeriod.getByExternalId(Long.valueOf(0));
			resultsMap.put(timePeriod, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			Mode mode1 = Mode.getByExternalId(Long.valueOf(1));
			resultsMap.get(timePeriod).put(mode1, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(0, 3000,
					0.0370117187500001, 111.03515625, 3600.0, 1.0, 27.0184697));
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(1, 1926,
					0.0717190999688149, 249.166142789938, 1200.0, 1.0, 13.9432871));
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(2, 3000,
					0.0370117187500001, 360.201299039938, 3600.0, 1.0, 27.0184697));
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(3, 6,
					0.0448543857828265, 360.470425354635, 1200.0, 2.0, 44.5887278));
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(4, 6,
					0.0448543857828265, 360.739551669332, 1200.0, 2.0, 44.5887278));
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(5, 1068,
					0.0360507068130539, 399.241706545674, 1200.0, 1.0, 27.7387072));
			resultsMap.get(timePeriod).get(mode1).add(new LinkSegmentExpectedResultsDto(6, 1068,
					0.0360507068130539, 437.743861422015, 1200.0, 1.0, 27.7387072));
			Mode mode2 = Mode.getByExternalId(Long.valueOf(2));
			resultsMap.get(timePeriod).put(mode2, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod).get(mode2).add(new LinkSegmentExpectedResultsDto(0, 1500,
					0.063673202685543, 95.5098040283147, 3600.0, 1.0, 15.705194));
			resultsMap.get(timePeriod).get(mode2).add(new LinkSegmentExpectedResultsDto(2, 1500,
					0.063673202685543, 191.019608056629, 3600.0, 1.0, 15.705194));
			resultsMap.get(timePeriod).get(mode2).add(new LinkSegmentExpectedResultsDto(3, 1086,
					0.0611216251945281, 257.397693017887, 1200.0, 2.0, 32.721643));
			resultsMap.get(timePeriod).get(mode2).add(new LinkSegmentExpectedResultsDto(4, 1086,
					0.0611216251945281, 323.775777979144, 1200.0, 2.0, 32.721643));
			resultsMap.get(timePeriod).get(mode2).add(new LinkSegmentExpectedResultsDto(5, 414,
					0.061091236386479, 349.067549843147, 1200.0, 1.0, 16.3689599));
			resultsMap.get(timePeriod).get(mode2).add(new LinkSegmentExpectedResultsDto(6, 414,
					0.061091236386479, 374.359321707149, 1200.0, 1.0, 16.3689599));
			TestHelper.compareResultsToMemoryOutputFormatter(OutputType.LINK, memoryOutputFormatter, maxIterations,
					resultsMap);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}
	
	/**
	 * Trivial test case which matches the description in the README.md file.
	 */
	@Test
	public void testExplanatory() {
		try {
			String projectPath = "src\\test\\resources\\explanatory\\xml";
			String description = "explanatory";
			String csvFileName = "Time Period 1_2.csv";
			String xmlFileName = "Time Period 1.xml";
			Integer maxIterations = null;

			MemoryOutputFormatter memoryOutputFormatter = TestHelper.setupAndExecuteAssignment(projectPath, null, description);
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap = new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>();
			TimePeriod timePeriod = TimePeriod.getByExternalId(Long.valueOf(0));
			resultsMap.put(timePeriod, new TreeMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>());
			Mode mode = Mode.getByExternalId(Long.valueOf(1));
			resultsMap.get(timePeriod).put(mode, new TreeSet<LinkSegmentExpectedResultsDto>());
			resultsMap.get(timePeriod).get(mode).add(new LinkSegmentExpectedResultsDto(1, 2, 1, 10.0, 10.0, 2000.0, 10.0, 1.0));
			TestHelper.compareResultsToMemoryOutputFormatter(OutputType.LINK, memoryOutputFormatter, maxIterations, resultsMap);
			runFileEqualAssertionsAndCleanUp(OutputType.LINK, projectPath, "RunId 0_" + description, csvFileName,	xmlFileName);
			runFileEqualAssertionsAndCleanUp(OutputType.OD, projectPath, "RunId 0_" + description, csvFileName, xmlFileName);
		} catch (Exception e) {
			PlanItLogger.severe(e.getMessage());
			fail(e.getMessage());
		}
	}
}
