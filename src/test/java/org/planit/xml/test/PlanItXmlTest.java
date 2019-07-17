package org.planit.xml.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConstants;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.demand.Demands;
import org.planit.generated.XMLElementColumn;
import org.planit.generated.XMLElementIteration;
import org.planit.generated.XMLElementMetadata;
import org.planit.generated.XMLElementOutputConfiguration;
import org.planit.generated.XMLElementOutputTimePeriod;
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
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.userclass.Mode;
import org.planit.utils.IdGenerator;
import org.planit.xml.util.XmlUtils;
import org.planit.zoning.Zoning;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PlanItXmlTest {

	private static final Logger LOGGER = Logger.getLogger(PlanItXmlTest.class.getName());
	private static boolean clearOutputDirectories;

	private boolean compareFiles(String file1, String file2) throws IOException {
		File f1 = new File(file1);
		File f2 = new File(file2);
		return FileUtils.contentEqualsIgnoreEOL(f1, f2, "utf-8");
	}

	private void runTest(String projectPath,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description,
			String outputDirectory) throws Exception {
		runTest(projectPath, null, null, 0, null, null, setCostParameters, description,
				outputDirectory);
	}

	private void runTest(String projectPath, String initialCostsFileLocation,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description,
			String outputDirectory) throws Exception {
		runTest(projectPath, initialCostsFileLocation, null, 0, null, null, setCostParameters,
				description, outputDirectory);
	}

	private void runTest(String projectPath, String initialCostsFileLocation1,
			String initialCostsFileLocation2, int initCostsFilePos,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description,
			String outputDirectory) throws Exception {
		runTest(projectPath, initialCostsFileLocation1, initialCostsFileLocation2,
				initCostsFilePos, null, null, setCostParameters, description, outputDirectory);
	}

	private void runTest(String projectPath, Integer maxIterations, Double epsilon,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description,
			String outputDirectory) throws Exception {
		runTest(projectPath, null, null, 0, maxIterations, epsilon, setCostParameters, description,
				outputDirectory);
	}

	private void runTest(String projectPath, String initialCostsFileLocation1,
			String initialCostsFileLocation2, int initCostsFilePos, Integer maxIterations, Double epsilon,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description,
			String outputDirectory) throws Exception {
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
				.createAndRegisterPhysicalTravelTimeCostFunction(BPRLinkTravelTimeCost.class.getCanonicalName());
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

		// DATA OUTPUT CONFIGURATION
		assignment.activateOutput(OutputType.LINK);
		OutputConfiguration outputConfiguration = assignment.getOutputConfiguration();
		outputConfiguration.setPersistOnlyFinalIteration(true); // option to only persist the final iteration
		LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) outputConfiguration
				.getOutputTypeConfiguration(OutputType.LINK);
		linkOutputTypeConfiguration.addAllProperties();
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
		xmlOutputFormatter.setOutputDirectory(outputDirectory);
		taBuilder.registerOutputFormatter(xmlOutputFormatter);

		// "USER" configuration
		if (maxIterations != null) {
			assignment.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
		}
		if (epsilon != null) {
			assignment.getGapFunction().getStopCriterion().setEpsilon(epsilon);
		}

		project.executeAllTrafficAssignments();
	}
	
	private boolean isXmlFileSameExceptForTimestamp(String xmlFile1, String xmlFile2) throws Exception {
		XMLElementMetadata metadata1 = (XMLElementMetadata) XmlUtils.generateObjectFromXml(XMLElementMetadata.class, xmlFile1);
		XMLElementMetadata metadata2 = (XMLElementMetadata) XmlUtils.generateObjectFromXml(XMLElementMetadata.class, xmlFile2);
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
		for (int i=0; i<size1; i++) {
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
		
		for (int i=0; i<size1; i++) {
			XMLElementIteration iteration1 = iterations1.get(i);
			XMLElementIteration iteration2 = iterations2.get(i);
			if (!iteration1.getNr().equals(iteration2.getNr())) {
				return false;
			}
		}
		//Timestamps should be different, to show that the two files were created separately
		if (metadata1.getTimestamp().compare(metadata2.getTimestamp()) == DatatypeConstants.EQUAL) {
			return false;
		}
		return true;
	}
	
	private void deleteFile(String filename) throws Exception {
		String rootPath = System.getProperty("user.dir");
		Path path = FileSystems.getDefault().getPath(rootPath + filename);
		Files.delete(path);
	}

	@Test
	public void testBasic1InitialCostFile() {
		try {
			runTest("src\\test\\resources\\basic\\xml\\test1", "src\\test\\resources\\basic\\xml\\test1\\initial_link_segment_costs.csv", null, "testBasic1", "src\\test\\resources\\basic\\xml\\test1");
			assertTrue(compareFiles("src\\test\\resources\\basic\\xml\\test1\\Time Period 1_2.csv", "src\\test\\resources\\basic\\xml\\test1\\testBasic1_Time Period 1_2.csv"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\basic\\xml\\test1\\Time Period 1.xml", "src\\test\\resources\\basic\\xml\\test1\\testBasic1_Time Period 1.xml"));
			deleteFile("\\src\\test\\resources\\basic\\xml\\test1\\testBasic1_Time Period 1_2.csv");
			deleteFile("\\src\\test\\resources\\basic\\xml\\test1\\testBasic1_Time Period 1.xml");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic1TwoInitialCostFiles() {
		try {
			runTest("src\\test\\resources\\basic\\xml\\test1",
					"src\\test\\resources\\basic\\xml\\test1\\initial_link_segment_costs.csv",
					"src\\test\\resources\\basic\\xml\\test1\\initial_link_segment_costs1.csv", 0, null, "testBasic1",
					"src\\test\\resources\\basic\\xml\\test1");
			assertTrue(compareFiles("src\\test\\resources\\basic\\xml\\test1\\Time Period 1_2.csv", "src\\test\\resources\\basic\\xml\\test1\\testBasic1_Time Period 1_2.csv"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\basic\\xml\\test1\\Time Period 1.xml", "src\\test\\resources\\basic\\xml\\test1\\testBasic1_Time Period 1.xml"));
			deleteFile("\\src\\test\\resources\\basic\\xml\\test1\\testBasic1_Time Period 1_2.csv");
			deleteFile("\\src\\test\\resources\\basic\\xml\\test1\\testBasic1_Time Period 1.xml");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic2() {
		try {
			runTest("src\\test\\resources\\basic\\xml\\test2", null, "testBasic2", "src\\test\\resources\\basic\\xml\\test2");
			assertTrue(compareFiles("src\\test\\resources\\basic\\xml\\test2\\Time Period 1_2.csv", "src\\test\\resources\\basic\\xml\\test2\\testBasic2_Time Period 1_2.csv"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\basic\\xml\\test2\\Time Period 1.xml", "src\\test\\resources\\basic\\xml\\test2\\testBasic2_Time Period 1.xml"));
			deleteFile("\\src\\test\\resources\\basic\\xml\\test2\\testBasic2_Time Period 1_2.csv");
			deleteFile("\\src\\test\\resources\\basic\\xml\\test2\\testBasic2_Time Period 1.xml");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic3() {
		try {
			runTest("src\\test\\resources\\basic\\xml\\test3",	null, "testBasic3", "src\\test\\resources\\basic\\xml\\test3");
			assertTrue(compareFiles("src\\test\\resources\\basic\\xml\\test3\\Time Period 1_2.csv", "src\\test\\resources\\basic\\xml\\test3\\testBasic3_Time Period 1_2.csv"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\basic\\xml\\test3\\Time Period 1.xml", "src\\test\\resources\\basic\\xml\\test3\\testBasic3_Time Period 1.xml"));
			deleteFile("\\src\\test\\resources\\basic\\xml\\test3\\testBasic3_Time Period 1_2.csv");
			deleteFile("\\src\\test\\resources\\basic\\xml\\test3\\testBasic3_Time Period 1.xml");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testBasic13() {
		try {
			runTest("src\\test\\resources\\basic\\xml\\test13",	null, "testBasic13", "src\\test\\resources\\basic\\xml\\test13");
			assertTrue(compareFiles("src\\test\\resources\\basic\\xml\\test13\\Time Period 1_2.csv", "src\\test\\resources\\basic\\xml\\test13\\testBasic13_Time Period 1_2.csv"));
			assertTrue(compareFiles("src\\test\\resources\\basic\\xml\\test13\\Time Period 2_2.csv", "src\\test\\resources\\basic\\xml\\test13\\testBasic13_Time Period 2_2.csv"));
			assertTrue(compareFiles("src\\test\\resources\\basic\\xml\\test13\\Time Period 3_2.csv", "src\\test\\resources\\basic\\xml\\test13\\testBasic13_Time Period 3_2.csv"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\basic\\xml\\test13\\Time Period 1.xml", "src\\test\\resources\\basic\\xml\\test13\\testBasic13_Time Period 1.xml"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\basic\\xml\\test13\\Time Period 2.xml", "src\\test\\resources\\basic\\xml\\test13\\testBasic13_Time Period 2.xml"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\basic\\xml\\test13\\Time Period 3.xml", "src\\test\\resources\\basic\\xml\\test13\\testBasic13_Time Period 3.xml"));
			deleteFile("\\src\\test\\resources\\basic\\xml\\test13\\testBasic13_Time Period 1_2.csv");
			deleteFile("\\src\\test\\resources\\basic\\xml\\test13\\testBasic13_Time Period 2_2.csv");
			deleteFile("\\src\\test\\resources\\basic\\xml\\test13\\testBasic13_Time Period 3_2.csv");
			deleteFile("\\src\\test\\resources\\basic\\xml\\test13\\testBasic13_Time Period 1.xml");
			deleteFile("\\src\\test\\resources\\basic\\xml\\test13\\testBasic13_Time Period 2.xml");
			deleteFile("\\src\\test\\resources\\basic\\xml\\test13\\testBasic13_Time Period 3.xml");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice1() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test1", 500, 0.0, null, "testRouteChoice1", 	"src\\test\\resources\\route_choice\\xml\\test1");
			assertTrue(compareFiles("src\\test\\resources\\route_choice\\xml\\test1\\Time Period 1_500.csv", "src\\test\\resources\\route_choice\\xml\\test1\\testRouteChoice1_Time Period 1_500.csv"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\route_choice\\xml\\test1\\Time Period 1.xml", "src\\test\\resources\\route_choice\\xml\\test1\\testRouteChoice1_Time Period 1.xml"));
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test1\\testRouteChoice1_Time Period 1_500.csv");
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test1\\testRouteChoice1_Time Period 1.xml");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice2() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test2", 500, 0.0, null, "testRouteChoice2", "src\\test\\resources\\route_choice\\xml\\test2");
			assertTrue(compareFiles("src\\test\\resources\\route_choice\\xml\\test2\\Time Period 1_500.csv", "src\\test\\resources\\route_choice\\xml\\test2\\testRouteChoice2_Time Period 1_500.csv"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\route_choice\\xml\\test2\\Time Period 1.xml", "src\\test\\resources\\route_choice\\xml\\test2\\testRouteChoice2_Time Period 1.xml"));
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test2\\testRouteChoice2_Time Period 1_500.csv");
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test2\\testRouteChoice2_Time Period 1.xml");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice3() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test3", "src\\test\\resources\\route_choice\\xml\\test3\\initial_costs.csv", null, 0, 500, 0.0, null, "testRouteChoice3", "src\\test\\resources\\route_choice\\xml\\test3");
			assertTrue(compareFiles("src\\test\\resources\\route_choice\\xml\\test3\\Time Period 1_500.csv", "src\\test\\resources\\route_choice\\xml\\test3\\testRouteChoice3_Time Period 1_500.csv"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\route_choice\\xml\\test3\\Time Period 1.xml", "src\\test\\resources\\route_choice\\xml\\test3\\testRouteChoice3_Time Period 1.xml"));
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test3\\testRouteChoice3_Time Period 1_500.csv");
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test3\\testRouteChoice3_Time Period 1.xml");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice4() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test4", 500, 0.0, null, "testRouteChoice4", "src\\test\\resources\\route_choice\\xml\\test4");
			assertTrue(compareFiles("src\\test\\resources\\route_choice\\xml\\test4\\Time Period 1_500.csv", "src\\test\\resources\\route_choice\\xml\\test4\\testRouteChoice4_Time Period 1_500.csv"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\route_choice\\xml\\test4\\Time Period 1.xml", "src\\test\\resources\\route_choice\\xml\\test4\\testRouteChoice4_Time Period 1.xml"));
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test4\\testRouteChoice4_Time Period 1_500.csv");
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test4\\testRouteChoice4_Time Period 1.xml");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice42() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test42", 500, 0.0, null, "testRouteChoice42", "src\\test\\resources\\route_choice\\xml\\test42");
			assertTrue(compareFiles("src\\test\\resources\\route_choice\\xml\\test42\\Time Period 1_500.csv", "src\\test\\resources\\route_choice\\xml\\test42\\testRouteChoice42_Time Period 1_500.csv"));
			assertTrue(compareFiles("src\\test\\resources\\route_choice\\xml\\test42\\Time Period 2_500.csv", "src\\test\\resources\\route_choice\\xml\\test42\\testRouteChoice42_Time Period 2_500.csv"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\route_choice\\xml\\test42\\Time Period 1.xml", "src\\test\\resources\\route_choice\\xml\\test42\\testRouteChoice42_Time Period 1.xml"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\route_choice\\xml\\test42\\Time Period 2.xml", "src\\test\\resources\\route_choice\\xml\\test42\\testRouteChoice42_Time Period 2.xml"));
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test42\\testRouteChoice42_Time Period 1_500.csv");
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test42\\testRouteChoice42_Time Period 2_500.csv");
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test42\\testRouteChoice42_Time Period 1.xml");
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test42\\testRouteChoice42_Time Period 2.xml");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice4raw() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test4raw", 500, 0.0, null, "testRouteChoice4raw","src\\test\\resources\\route_choice\\xml\\test4raw");
			assertTrue(compareFiles("src\\test\\resources\\route_choice\\xml\\test4raw\\Time Period 1_500.csv", "src\\test\\resources\\route_choice\\xml\\test4raw\\testRouteChoice4raw_Time Period 1_500.csv"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\route_choice\\xml\\test4raw\\Time Period 1.xml", "src\\test\\resources\\route_choice\\xml\\test4raw\\testRouteChoice4raw_Time Period 1.xml"));
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test4raw\\testRouteChoice4raw_Time Period 1_500.csv");
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test4raw\\testRouteChoice4raw_Time Period 1.xml");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice4raw2() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test4raw2", 500, 0.0, null, "testRouteChoice4raw2","src\\test\\resources\\route_choice\\xml\\test4raw2");
			assertTrue(compareFiles("src\\test\\resources\\route_choice\\xml\\test4raw2\\Time Period 1_500.csv", "src\\test\\resources\\route_choice\\xml\\test4raw2\\Time Period 1_500.csv"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\route_choice\\xml\\test4raw2\\Time Period 1.xml", "src\\test\\resources\\route_choice\\xml\\test4raw2\\testRouteChoice4raw2_Time Period 1.xml"));
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test4raw2\\testRouteChoice4raw2_Time Period 1_500.csv");
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test4raw2\\testRouteChoice4raw2_Time Period 1.xml");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRouteChoice5() {
		try {
			runTest("src\\test\\resources\\route_choice\\xml\\test5", 500, 0.0,
					(physicalNetwork, bprLinkTravelTimeCost) -> {
						MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork) physicalNetwork;
						MacroscopicLinkSegmentType macroscopiclinkSegmentType = macroscopicNetwork
								.findMacroscopicLinkSegmentTypeByExternalId(1);
						Mode mode = Mode.getByExternalId(2);
						bprLinkTravelTimeCost.setDefaultParameters(macroscopiclinkSegmentType, mode, 0.8, 4.5);
					}, "testRouteChoice5", "src\\test\\resources\\route_choice\\xml\\test5");
			assertTrue(compareFiles("src\\test\\resources\\route_choice\\xml\\test5\\Time Period 1_500.csv", "src\\test\\resources\\route_choice\\xml\\test5\\Time Period 1_500.csv"));
			assertTrue(isXmlFileSameExceptForTimestamp("src\\test\\resources\\route_choice\\xml\\test5\\Time Period 1.xml", "src\\test\\resources\\route_choice\\xml\\test5\\testRouteChoice5_Time Period 1.xml"));
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test5\\testRouteChoice5_Time Period 1_500.csv");
			deleteFile("\\src\\test\\resources\\route_choice\\xml\\test5\\testRouteChoice5_Time Period 1.xml");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
