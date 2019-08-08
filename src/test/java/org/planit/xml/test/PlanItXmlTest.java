package org.planit.xml.test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConstants;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.demand.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.generated.XMLElementColumn;
import org.planit.generated.XMLElementIteration;
import org.planit.generated.XMLElementMetadata;
import org.planit.generated.XMLElementOutputConfiguration;
import org.planit.generated.XMLElementOutputTimePeriod;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.OutputType;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.formatter.xml.PlanItXMLOutputFormatter;
import org.planit.output.property.CostOutputProperty;
import org.planit.output.property.DownstreamNodeExternalIdOutputProperty;
import org.planit.output.property.LinkSegmentExternalIdOutputProperty;
import org.planit.output.property.ModeExternalIdOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.UpstreamNodeExternalIdOutputProperty;
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
import org.planit.xml.util.XmlUtils;
import org.planit.zoning.Zoning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PlanItXmlTest {

	private static final Logger LOGGER = Logger.getLogger(PlanItXmlTest.class.getName());
	private static boolean clearOutputDirectories;

	/**
	 * Compares the contents of two text files
	 * 
	 * In this test the text contents of the files must be exactly equal. This test
	 * can be applied to any file type (CSV, XML etc)
	 * 
	 * @param file1 location of the first file to be compared
	 * @param file2 location of the second file to be compared
	 * @return true if the contents of the two files are exactly equal, false
	 *         otherwise
	 * @throws IOException thrown if there is an error opening one of the files
	 */
	private boolean compareFiles(String file1, String file2) throws IOException {
		File f1 = new File(file1);
		File f2 = new File(file2);
		return FileUtils.contentEqualsIgnoreEOL(f1, f2, "utf-8");
	}

	/**
	 * Tests whether two XML output files contain the same data contents but were
	 * created at different times.
	 * 
	 * This test only works on XML output files. For the test to pass, the data
	 * contents of the two files must be equal but their timestamps (the times they
	 * were created) must be different
	 * 
	 * @param xmlFile1 location of the first XML file to be compared
	 * @param xmlFile2 location of the second XML file to be compared
	 * @return true if the test passes, false otherwise
	 * @throws Exception thrown if the there is an error opening one of the files
	 */
	private boolean isXmlFileSameExceptForTimestamp(String xmlFile1, String xmlFile2) throws Exception {
		XMLElementMetadata metadata1 = (XMLElementMetadata) XmlUtils.generateObjectFromXml(XMLElementMetadata.class,
				xmlFile1);
		XMLElementMetadata metadata2 = (XMLElementMetadata) XmlUtils.generateObjectFromXml(XMLElementMetadata.class,
				xmlFile2);
		if (!metadata1.getVersion().equals(metadata2.getVersion())) {
			return false;
		}
		List<XMLElementColumn> elementColumns1 = metadata1.getColumns().getColumn();
		List<XMLElementColumn> elementColumns2 = metadata1.getColumns().getColumn();
		int size1 = elementColumns1.size();
		int size2 = elementColumns2.size();
		if (size1 != size2) {
			return false;
		}
		for (int i = 0; i < size1; i++) {
			XMLElementColumn elementColumn1 = elementColumns1.get(i);
			XMLElementColumn elementColumn2 = elementColumns2.get(i);
			if (!elementColumn1.getName().equals(elementColumn2.getName())) {
				return false;
			}
			if (!elementColumn1.getUnits().equals(elementColumn2.getUnits())) {
				return false;
			}
			if (!elementColumn1.getType().equals(elementColumn2.getType())) {
				return false;
			}
		}
		if (!metadata1.getDescription().equals(metadata2.getDescription())) {
			return false;
		}

		XMLElementOutputConfiguration outputConfiguration1 = metadata1.getOutputconfiguration();
		XMLElementOutputConfiguration outputConfiguration2 = metadata2.getOutputconfiguration();
		if (!outputConfiguration1.getAssignment().equals(outputConfiguration2.getAssignment())) {
			return false;
		}
		if (!outputConfiguration1.getPhysicalcost().equals(outputConfiguration2.getPhysicalcost())) {
			return false;
		}
		if (!outputConfiguration1.getVirtualcost().equals(outputConfiguration2.getVirtualcost())) {
			return false;
		}
		XMLElementOutputTimePeriod timeperiod1 = outputConfiguration1.getTimeperiod();
		XMLElementOutputTimePeriod timeperiod2 = outputConfiguration2.getTimeperiod();
		if (!timeperiod1.getId().equals(timeperiod2.getId())) {
			return false;
		}
		if (!timeperiod1.getName().equals(timeperiod2.getName())) {
			return false;
		}
		List<XMLElementIteration> iterations1 = metadata1.getSimulation().getIteration();
		size1 = iterations1.size();
		List<XMLElementIteration> iterations2 = metadata2.getSimulation().getIteration();
		size2 = iterations2.size();
		if (size1 != size2) {
			return false;
		}

		for (int i = 0; i < size1; i++) {
			XMLElementIteration iteration1 = iterations1.get(i);
			XMLElementIteration iteration2 = iterations2.get(i);
			if (!iteration1.getNr().equals(iteration2.getNr())) {
				return false;
			}
		}
		// Time stamps should be different, to show that the two files were created separately
		if (metadata1.getTimestamp().compare(metadata2.getTimestamp()) == DatatypeConstants.EQUAL) {
			return false;
		}
		return true;
	}

	/**
	 * Deletes a file from the file system
	 * 
	 * @param filename location of the file to be deleted
	 * @throws Exception thrown if there is an error deleting the file
	 */
	private void deleteFile(String filename) throws Exception {
		String rootPath = System.getProperty("user.dir");
		Path path = FileSystems.getDefault().getPath(rootPath + "\\" + filename);
		Files.delete(path);
	}

	private void deleteFile(String projectPath, String description, String fileName) throws Exception {
		deleteFile(projectPath + "\\" + description + "_" + fileName);
	}

	private void runTest(String projectPath, BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters,
			String description, String resultsFileLocation) throws Exception {
		runTest(projectPath, null, null, 0, null, null, setCostParameters, description, resultsFileLocation);
	}

	private void runTest(String projectPath, String initialCostsFileLocation,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description, String resultsFileLocation) throws Exception {
		runTest(projectPath, initialCostsFileLocation, null, 0, null, null, setCostParameters, description, resultsFileLocation);
	}

	private void runTest(String projectPath, String initialCostsFileLocation1, String initialCostsFileLocation2,
			int initCostsFilePos, BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters,
			String description, String resultsFileLocation) throws Exception {
		runTest(projectPath, initialCostsFileLocation1, initialCostsFileLocation2, initCostsFilePos, null, null,
				setCostParameters, description, resultsFileLocation);
	}

	private void runTest(String projectPath, Integer maxIterations, Double epsilon,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description, String resultsFileLocation) throws Exception {
		runTest(projectPath, null, null, 0, maxIterations, epsilon, setCostParameters, description, resultsFileLocation);
	}

	private void runTest(String projectPath, String initialCostsFileLocation1, String initialCostsFileLocation2,
			int initCostsFilePos, Integer maxIterations, Double epsilon,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description,
			String resultsFileLocation) throws Exception {
		IdGenerator.reset();
		PlanItProject project = new PlanItProject(projectPath);

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
				.createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
		if (setCostParameters != null) {
			setCostParameters.accept(physicalNetwork, bprLinkTravelTimeCost);
		}

		if (initialCostsFileLocation1 != null) {
			if (initialCostsFileLocation2 != null) {
				List<InitialLinkSegmentCost> initialCosts = project
						.createAndRegisterInitialLinkSegmentCosts(initialCostsFileLocation1, initialCostsFileLocation2);
				taBuilder.registerInitialLinkSegmentCost(initialCosts.get(initCostsFilePos));
			} else {
				InitialLinkSegmentCost initialCost = project
						.createAndRegisterInitialLinkSegmentCost(initialCostsFileLocation1);
				taBuilder.registerInitialLinkSegmentCost(initialCost);
			}
		}
		taBuilder
				.createAndRegisterVirtualTravelTimeCostFunction(SpeedConnectoidTravelTimeCost.class.getCanonicalName());
		taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());
		// SUPPLY-DEMAND INTERFACE
		taBuilder.registerZoning(zoning);

		// DEMAND SIDE
		taBuilder.registerDemands(demands);

		// DATA OUTPUT CONFIGURATION
		assignment.activateOutput(OutputType.LINK);
		OutputConfiguration outputConfiguration = assignment.getOutputConfiguration();
		outputConfiguration.setPersistOnlyFinalIteration(true); // option to only persist the final iteration
		LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) outputConfiguration
				.getOutputTypeConfiguration(OutputType.LINK);
		linkOutputTypeConfiguration.addAllProperties();
		//linkOutputTypeConfiguration.removeProperty(OutputProperty.NUMBER_OF_LANES);
		linkOutputTypeConfiguration.removeProperty(OutputProperty.LINK_SEGMENT_EXTERNAL_ID);

		// OUTPUT FORMAT CONFIGURATION
		PlanItXMLOutputFormatter xmlOutputFormatter = (PlanItXMLOutputFormatter) project
				.createAndRegisterOutputFormatter(PlanItXMLOutputFormatter.class.getCanonicalName());
		if (clearOutputDirectories) {
			xmlOutputFormatter.resetXmlOutputDirectory();
			xmlOutputFormatter.resetCsvOutputDirectory();
			clearOutputDirectories = false;
		}
		xmlOutputFormatter.setXmlNamePrefix(description);
		xmlOutputFormatter.setCsvNamePrefix(description);
		xmlOutputFormatter.setOutputDirectory(projectPath);
		taBuilder.registerOutputFormatter(xmlOutputFormatter);

		OutputFormatter outputFormatter = project
				.createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());
		MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) outputFormatter;
		taBuilder.registerOutputFormatter(memoryOutputFormatter);

		// "USER" configuration
		if (maxIterations != null) {
			assignment.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
		}
		if (epsilon != null) {
			assignment.getGapFunction().getStopCriterion().setEpsilon(epsilon);
		}

		project.executeAllTrafficAssignments();
		if (resultsFileLocation != null) {
			SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMapFromFile = CsvIoUtils
					.createResultsMapFromCsvFile(resultsFileLocation);
			TestHelper.compareResultsToMemoryOutputFormatter(memoryOutputFormatter, resultsMapFromFile);
		}
	}

	private void runAssertionsAndCleanUp(String projectPath, String description, String csvFileName, String xmlFileName)
			throws Exception {
		assertTrue(
				compareFiles(projectPath + "\\" + csvFileName, projectPath + "\\" + description + "_" + csvFileName));
		assertTrue(isXmlFileSameExceptForTimestamp(projectPath + "\\" + xmlFileName,
				projectPath + "\\" + description + "_" + xmlFileName));
		deleteFile(projectPath, description, csvFileName);
		deleteFile(projectPath, description, xmlFileName);
	}

	@Test
	public void testInitialCostValues() throws PlanItException {
		String projectPath = "src\\test\\resources\\initial_costs\\xml\\test1";
		String initialCostsFileLocation = "src\\test\\resources\\initial_costs\\xml\\test1\\initial_link_segment_costs.csv";
		String initialCostsFileLocationExternalId = "src\\test\\resources\\initial_costs\\xml\\test1\\initial_link_segment_costs_external_id.csv";
		IdGenerator.reset();
		PlanItProject project = new PlanItProject(projectPath);
		PhysicalNetwork physicalNetwork = project
				.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
		DeterministicTrafficAssignment assignment = project
				.createAndRegisterDeterministicAssignment(TraditionalStaticAssignment.class.getCanonicalName());
		CapacityRestrainedTrafficAssignmentBuilder taBuilder = (CapacityRestrainedTrafficAssignmentBuilder) assignment
				.getBuilder();
		taBuilder.registerPhysicalNetwork(physicalNetwork);
		InitialLinkSegmentCost initialCost = project.createAndRegisterInitialLinkSegmentCost(initialCostsFileLocation);
		try {
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
				assertEquals(cost, initialCost.getAllSegmentCostsPerMode(mode)[(int) linkSegment.getId()], 0.0001);
			}
			in.close();
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
	}

	@Test
	public void testInitialCostMissingRows() throws PlanItException {
		String projectPath = "src\\test\\resources\\initial_costs\\xml\\test1";
		String initialCostsFileLocation = "src\\test\\resources\\initial_costs\\xml\\test1\\initial_link_segment_costs.csv";
		String initialCostsFileLocationExternalId = "src\\test\\resources\\initial_costs\\xml\\test1\\initial_link_segment_costs1.csv";
		IdGenerator.reset();
		PlanItProject project = new PlanItProject(projectPath);
		PhysicalNetwork physicalNetwork = project
				.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
		DeterministicTrafficAssignment assignment = project
				.createAndRegisterDeterministicAssignment(TraditionalStaticAssignment.class.getCanonicalName());
		CapacityRestrainedTrafficAssignmentBuilder taBuilder = (CapacityRestrainedTrafficAssignmentBuilder) assignment
				.getBuilder();
		taBuilder.registerPhysicalNetwork(physicalNetwork);
		InitialLinkSegmentCost initialCost = project.createAndRegisterInitialLinkSegmentCost(initialCostsFileLocation);
		try {
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
				assertEquals(cost, initialCost.getAllSegmentCostsPerMode(mode)[(int) linkSegment.getId()], 0.0001);
			}
			in.close();
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
	}

	@Test
	public void testInitialCostValuesMissingColumns() throws PlanItException {
		try {
			String projectPath = "src\\test\\resources\\initial_costs\\xml\\test2";
			String description = "testBasic1";
			runTest(projectPath,
					"src\\test\\resources\\initial_costs\\xml\\test2\\initial_link_segment_costs_external_id.csv", null,
					description, null);
			fail("RunTest did not throw an exception when it should have (missing data in the input XML file in the link definition section).");
		} catch (Exception e) {
			assertTrue(true);
		}
	}

	@Test
	public void testBasic1InitialCostFile() {
		try {
			String projectPath = "src\\test\\resources\\basic\\xml\\test1";
			String description = "testBasic1";
			String csvFileName = "Time Period 1_2.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath, "src\\test\\resources\\basic\\xml\\test1\\initial_link_segment_costs1.csv", null,
					description, null);
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic1TwoInitialCostFiles() {
		try {
			String projectPath = "src\\test\\resources\\basic\\xml\\test1";
			String description = "testBasic1";
			String csvFileName = "Time Period 1_2.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath, "src\\test\\resources\\basic\\xml\\test1\\initial_link_segment_costs.csv",
					"src\\test\\resources\\basic\\xml\\test1\\initial_link_segment_costs1.csv", 0, null, description, null);
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic2() {
		try {
			String projectPath = "src\\test\\resources\\basic\\xml\\test2";
			String description = "testBasic2";
			String csvFileName = "Time Period 1_2.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath, null, description, "src\\test\\resources\\basic\\xml\\test2\\results.csv");
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic3() {
		try {
			String projectPath = "src\\test\\resources\\basic\\xml\\test3";
			String description = "testBasic3";
			String csvFileName = "Time Period 1_2.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath, null, description, "src\\test\\resources\\basic\\xml\\test3\\results.csv");
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic13() {
		try {
			String projectPath = "src\\test\\resources\\basic\\xml\\test13";
			String description = "testBasic3";
			String csvFileName1 = "Time Period 1_2.csv";
			String csvFileName2 = "Time Period 2_2.csv";
			String csvFileName3 = "Time Period 3_2.csv";
			String xmlFileName1 = "Time Period 1.xml";
			String xmlFileName2 = "Time Period 2.xml";
			String xmlFileName3 = "Time Period 3.xml";
			runTest(projectPath, null, description, "src\\test\\resources\\basic\\xml\\test13\\results.csv");
			runAssertionsAndCleanUp(projectPath, description, csvFileName1, xmlFileName1);
			runAssertionsAndCleanUp(projectPath, description, csvFileName2, xmlFileName2);
			runAssertionsAndCleanUp(projectPath, description, csvFileName3, xmlFileName3);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice1() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test1";
			String description = "testRouteChoice1";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath, 500, 0.0, null, description, "src\\test\\resources\\route_choice\\xml\\test1\\results.csv");
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice2() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test2";
			String description = "testRouteChoice2";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath, 500, 0.0, null, description, "src\\test\\resources\\route_choice\\xml\\test2\\results.csv");
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice2InitialCostsOneIteration() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test2initialCostsOneIteration";
			String description = "testRouteChoice2initialCosts";
			String csvFileName = "Time Period 1_1.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath,
					"src\\test\\resources\\route_choice\\xml\\test2initialCostsOneIteration\\initial_link_segment_costs.csv",
					null, 0, 1, 0.0, null, description, null);
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice2InitialCostsOneIterationExternalIds() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test2initialCostsOneIterationExternalIds";
			String description = "testRouteChoice2initialCosts";
			String csvFileName = "Time Period 1_1.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath,
					"src\\test\\resources\\route_choice\\xml\\test2initialCostsOneIterationExternalIds\\initial_link_segment_costs.csv",
					null, 0, 1, 0.0, null, description, null);
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice2InitialCosts500Iterations() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test2initialCosts500iterations";
			String description = "testRouteChoice2initialCosts";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath,
					"src\\test\\resources\\route_choice\\xml\\test2initialCosts500iterations\\initial_link_segment_costs.csv",
					null, 0, 500, 0.0, null, description, null);
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice2InitialCosts500IterationsExternalIds() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test2initialCosts500iterationsExternalIds";
			String description = "testRouteChoice2initialCosts";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath,
					"src\\test\\resources\\route_choice\\xml\\test2initialCosts500iterationsExternalIds\\initial_link_segment_costs.csv",
					null, 0, 500, 0.0, null, description, null);
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice3() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test3";
			String description = "testRouteChoice3";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath, 500, 0.0, null, description, "src\\test\\resources\\route_choice\\xml\\test3\\results.csv");
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice4() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test4";
			String description = "testRouteChoice4";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath, 500, 0.0, null, description, "src\\test\\resources\\route_choice\\xml\\test4\\results.csv");
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice42() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test42";
			String description = "testRouteChoice42";
			String csvFileName1 = "Time Period 1_500.csv";
			String csvFileName2 = "Time Period 2_500.csv";
			String xmlFileName1 = "Time Period 1.xml";
			String xmlFileName2 = "Time Period 2.xml";
			runTest(projectPath, 500, 0.0, null, description, "src\\test\\resources\\route_choice\\xml\\test42\\results.csv");
			runAssertionsAndCleanUp(projectPath, description, csvFileName1, xmlFileName1);
			runAssertionsAndCleanUp(projectPath, description, csvFileName2, xmlFileName2);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice4raw() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test4raw";
			String description = "testRouteChoice4raw";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath, 500, 0.0, null, description, "src\\test\\resources\\route_choice\\xml\\test4raw\\results.csv");
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice4raw2() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test4raw2";
			String description = "testRouteChoice4raw2";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath, 500, 0.0, null, description, "src\\test\\resources\\route_choice\\xml\\test4raw2\\results.csv");
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice5() {
		try {
			String projectPath = "src\\test\\resources\\route_choice\\xml\\test5";
			String description = "testRouteChoice5";
			String csvFileName = "Time Period 1_500.csv";
			String xmlFileName = "Time Period 1.xml";
			runTest(projectPath, 500, 0.0, (physicalNetwork, bprLinkTravelTimeCost) -> {
				MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) physicalNetwork;
				MacroscopicLinkSegmentType macroscopiclinkSegmentType = macroscopicNetwork
						.findMacroscopicLinkSegmentTypeByExternalId(1);
				Mode mode = Mode.getByExternalId(2);
				bprLinkTravelTimeCost.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
			}, description, "src\\test\\resources\\route_choice\\xml\\test5\\results.csv");
			runAssertionsAndCleanUp(projectPath, description, csvFileName, xmlFileName);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testExplanatory() {
		try {
			String projectPath = "src\\test\\resources\\explanatory\\xml";
			String description = "explanatory";
			String csvFileName1 = "Time Period 1_2.csv";
			String xmlFileName1 = "Time Period 1.xml";
			runTest(projectPath, null, description, "src\\test\\resources\\explanatory\\xml\\results.csv");
			runAssertionsAndCleanUp(projectPath, description, csvFileName1, xmlFileName1);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
