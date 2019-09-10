package org.planit.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConstants;

import org.apache.commons.io.FileUtils;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.data.MultiKeyPlanItData;
import org.planit.demand.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.generated.XMLElementColumn;
import org.planit.generated.XMLElementIteration;
import org.planit.generated.XMLElementMetadata;
import org.planit.generated.XMLElementOutputConfiguration;
import org.planit.generated.XMLElementOutputTimePeriod;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.OutputType;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.formatter.BasicMemoryOutputFormatter;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.formatter.xml.PlanItXMLOutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.project.PlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.test.integration.LinkSegmentExpectedResultsDto;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.userclass.Mode;
import org.planit.utils.IdGenerator;
import org.planit.xml.test.TriConsumer;
import org.planit.xml.util.XmlUtils;
import org.planit.zoning.Zoning;

/**
 * Helper class used by unit tests
 * 
 * @author gman6028
 *
 */
public class TestHelper {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(TestHelper.class.getName());

	private static final double epsilon = 0.00001;

	private static Consumer<LinkOutputTypeConfiguration> defaultSetOutputTypeConfigurationProperties = (
			linkOutputTypeConfiguration) -> {
		try {
			linkOutputTypeConfiguration.addAllProperties();
			linkOutputTypeConfiguration.removeProperty(OutputProperty.LINK_SEGMENT_EXTERNAL_ID);
			linkOutputTypeConfiguration.removeProperty(OutputProperty.ITERATION_INDEX);
		} catch (PlanItException e) {
			e.printStackTrace();
		}
	};

	/**
	 * Compares the contents of a results map for the current run with a results map
	 * from a previous run which had been stored in a file. It generates a JUnit
	 * test failure if the results maps have different contents.
	 * 
	 * @param resultsMap         Map storing result of the current test run
	 * @param resultsMapFromFile Map storing results of a previous run which had
	 *                           been stored in a file
	 */
	public static void compareResultsToCsvFileContents(
			SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>> resultsMap,
			SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>> resultsMapFromFile) {
		if (resultsMap.keySet().size() != resultsMapFromFile.keySet().size()) {
			fail("Test case returned " + resultsMap.keySet().size() + " runs where the results file contains "
					+ resultsMap.keySet().size() + ".");
			return;
		}
		for (Long runId : resultsMapFromFile.keySet()) {
			if (!resultsMap.containsKey(runId)) {
				fail("Run " + runId + " is present in the results file but was not found in the test case results.");
			}
			if (resultsMap.get(runId).keySet().size() != resultsMapFromFile.get(runId).keySet().size()) {
				fail("Test case returned " + resultsMap.get(runId).keySet().size() + " time periods for run " + runId
						+ " where the results file contains " + resultsMapFromFile.get(runId).keySet().size() + ".");
				return;
			}
			for (TimePeriod timePeriod : resultsMapFromFile.get(runId).keySet()) {
				if (!resultsMap.get(runId).containsKey(timePeriod)) {
					fail("Run " + runId + " time period " + timePeriod.getId()
							+ " is present in the results file but was not found in the test case results.");
					return;
				}
				if (resultsMap.get(runId).get(timePeriod).keySet().size() != resultsMapFromFile.get(runId)
						.get(timePeriod).keySet().size()) {
					fail("Test case returned " + resultsMap.get(runId).get(timePeriod).keySet().size()
							+ " modes for run " + runId + " and timePeriod " + timePeriod.getId()
							+ " where the results file contains "
							+ resultsMapFromFile.get(runId).get(timePeriod).keySet().size() + ".");
					return;
				}
				for (Mode mode : resultsMapFromFile.get(runId).get(timePeriod).keySet()) {
					if (!resultsMap.get(runId).get(timePeriod).containsKey(mode)) {
						fail("Run " + runId + " time period " + timePeriod.getId() + " mode " + mode.getId()
								+ " is present in the results file but was not found in the test case results.");
						return;
					}
					if (resultsMap.get(runId).get(timePeriod).get(mode).size() != resultsMapFromFile.get(runId)
							.get(timePeriod).get(mode).size()) {
						fail("Test case returned " + resultsMap.get(runId).get(timePeriod).get(mode).size()
								+ " results for run " + runId + ", timePeriod " + timePeriod.getId() + " and mode "
								+ mode.getId() + " where the results file contains "
								+ resultsMapFromFile.get(runId).get(timePeriod).get(mode).size() + ".");
						return;
					}
					for (LinkSegmentExpectedResultsDto resultDto : resultsMap.get(runId).get(timePeriod).get(mode)) {
						SortedSet<LinkSegmentExpectedResultsDto> resultsSetFromFile = resultsMapFromFile.get(runId)
								.get(timePeriod).get(mode);
						if (!resultsSetFromFile.contains(resultDto)) {
							boolean passed = false;
							Iterator<LinkSegmentExpectedResultsDto> iterator = resultsSetFromFile.iterator();
							while (!passed && iterator.hasNext()) {
								LinkSegmentExpectedResultsDto resultDtoFromFile = iterator.next();
								passed = resultDto.equals(resultDtoFromFile);
							}
							if (!passed) {
								fail("The result for runId " + runId + " time period " + timePeriod.getId() + " mode "
										+ mode.getId() + " " + resultDto.toString()
										+ " was not found in the results file.");
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Compares the results from an assignment run stored in a
	 * BasicMemoryOutputFormatter object to known results stored in a Map. It
	 * generates a JUnit test failure if the results maps have different contents.
	 * 
	 * @param basicMemoryOutputFormatter the BasicMemoryOuptutFormatter object which
	 *                                   stores results from a test run
	 * @param resultsMap                 Map storing standard test results which
	 *                                   have been generated previously
	 * @throws PlanItException thrown if one of the test output properties has not
	 *                         been saved
	 */
	public static void compareResultsToMemoryOutputFormatter(BasicMemoryOutputFormatter basicMemoryOutputFormatter,
			SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>> resultsMap)
			throws PlanItException {
		for (Long runId : resultsMap.keySet()) {
			for (TimePeriod timePeriod : resultsMap.get(runId).keySet()) {
				for (Mode mode : resultsMap.get(runId).get(timePeriod).keySet()) {
					for (LinkSegmentExpectedResultsDto resultDto : resultsMap.get(runId).get(timePeriod).get(mode)) {
						double flow = (Double) basicMemoryOutputFormatter.getLinkSegmentOutput(runId,
								timePeriod.getId(), mode.getExternalId(), resultDto.getStartNodeId(),
								resultDto.getEndNodeId(), OutputProperty.FLOW);
						double length = (Double) basicMemoryOutputFormatter.getLinkSegmentOutput(runId,
								timePeriod.getId(), mode.getExternalId(), resultDto.getStartNodeId(),
								resultDto.getEndNodeId(), OutputProperty.LENGTH);
						double speed = (Double) basicMemoryOutputFormatter.getLinkSegmentOutput(runId,
								timePeriod.getId(), mode.getExternalId(), resultDto.getStartNodeId(),
								resultDto.getEndNodeId(), OutputProperty.SPEED);
						double cost = (Double) basicMemoryOutputFormatter.getLinkSegmentOutput(runId,
								timePeriod.getId(), mode.getExternalId(), resultDto.getStartNodeId(),
								resultDto.getEndNodeId(), OutputProperty.COST);
						double capacityPerLane = (Double) basicMemoryOutputFormatter.getLinkSegmentOutput(runId,
								timePeriod.getId(), mode.getExternalId(), resultDto.getStartNodeId(),
								resultDto.getEndNodeId(), OutputProperty.CAPACITY_PER_LANE);
						int numberOfLanes = (Integer) basicMemoryOutputFormatter.getLinkSegmentOutput(runId,
								timePeriod.getId(), mode.getExternalId(), resultDto.getStartNodeId(),
								resultDto.getEndNodeId(), OutputProperty.NUMBER_OF_LANES);
						double capacity = capacityPerLane * numberOfLanes;
						assertEquals(flow, resultDto.getLinkFlow(), epsilon);
						assertEquals(length, resultDto.getLength(), epsilon);
						assertEquals(speed, resultDto.getSpeed(), epsilon);
						assertEquals(capacity, resultDto.getCapacity(), epsilon);
						assertEquals(cost, resultDto.getLinkCost(), epsilon);
					}
				}
			}
		}
	}

	/**
	 * Compares the results from an assignment run stored in a MemoryOutputFormatter
	 * object to known results stored in a Map. It generates a JUnit test failure if
	 * the results maps have different contents.
	 * 
	 * @param outputType            the current output type
	 * @param memoryOutputFormatter the MemoryOuptutFormatter object which stores
	 *                              results from a test run
	 * @param iterationIndex        the current iteration index
	 * @param resultsMap            Map storing standard test results which have
	 *                              been generated previously
	 * @throws PlanItException thrown if one of the test output properties has not
	 *                         been saved
	 */
	public static void compareResultsToMemoryOutputFormatter(OutputType outputType,
			MemoryOutputFormatter memoryOutputFormatter, Integer iterationIndex,
			SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>> resultsMap)
			throws PlanItException {

		if (iterationIndex == null) {
			iterationIndex = memoryOutputFormatter.getLastIteration();
		}
		for (Long runId : resultsMap.keySet()) {
			for (TimePeriod timePeriod : resultsMap.get(runId).keySet()) {
				for (Mode mode : resultsMap.get(runId).get(timePeriod).keySet()) {
					for (LinkSegmentExpectedResultsDto resultDto : resultsMap.get(runId).get(timePeriod).get(mode)) {
						OutputProperty[] outputKeyProperties = memoryOutputFormatter.getOutputKeyProperties(outputType);
						OutputProperty[] outputValueProperties = memoryOutputFormatter
								.getOutputValueProperties(outputType);
						MultiKeyPlanItData multiKeyPlanItData = memoryOutputFormatter.getOutputData(mode, timePeriod,
								iterationIndex, outputType);
						Object[] keyValues = new Object[outputKeyProperties.length];
						if (keyValues.length == 2) {
							keyValues[0] = Integer.valueOf((int) resultDto.getStartNodeId());
							keyValues[1] = Integer.valueOf((int) resultDto.getEndNodeId());
						}
						if (keyValues.length == 1) {
							keyValues[0] = Integer.valueOf((int) resultDto.getLinkSegmentId());
						}
						for (int i = 0; i < outputValueProperties.length; i++) {
							switch (outputValueProperties[i]) {
							case FLOW:
								double flow = (Double) multiKeyPlanItData.getRowValue(OutputProperty.FLOW, keyValues);
								assertEquals(flow, resultDto.getLinkFlow(), epsilon);
								break;
							case LENGTH:
								double length = (Double) multiKeyPlanItData.getRowValue(OutputProperty.LENGTH,
										keyValues);
								assertEquals(length, resultDto.getLength(), epsilon);
								break;
							case SPEED:
								double speed = (Double) multiKeyPlanItData.getRowValue(OutputProperty.SPEED, keyValues);
								assertEquals(speed, resultDto.getSpeed(), epsilon);
								break;
							case COST:
								double cost = (Double) multiKeyPlanItData.getRowValue(OutputProperty.COST, keyValues);
								assertEquals(cost, resultDto.getLinkCost(), epsilon);
								break;
							case CAPACITY_PER_LANE:
								double capacityPerLane = (Double) multiKeyPlanItData
										.getRowValue(OutputProperty.CAPACITY_PER_LANE, keyValues);
								int numberOfLanes = (Integer) multiKeyPlanItData
										.getRowValue(OutputProperty.NUMBER_OF_LANES, keyValues);
								assertEquals(numberOfLanes * capacityPerLane, resultDto.getCapacity(), epsilon);
								break;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (uses
	 * maximum number of iterations)
	 * 
	 * @param projectPath                          project directory containing the
	 *                                             input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output
	 *                                             properties being used
	 * @param maxIterations                        the maximum number of iterations
	 *                                             allowed in this test run
	 * @param setCostParameters                    lambda function which sets
	 *                                             parameters of cost function
	 * @param description                          description used in temporary
	 *                                             output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath,
			Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties, Integer maxIterations,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description) throws Exception {
		return setupAndExecuteAssignment(projectPath, setOutputTypeConfigurationProperties, null, null, 0,
				maxIterations, null, setCostParameters, description);
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (uses
	 * maximum number of iterations)
	 * 
	 * @param projectPath       project directory containing the input files
	 * @param maxIterations     the maximum number of iterations allowed in this
	 *                          test run
	 * @param setCostParameters lambda function which sets parameters of cost
	 *                          function
	 * @param description       description used in temporary output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath, Integer maxIterations,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description) throws Exception {
		return setupAndExecuteAssignment(projectPath, null, null, 0, maxIterations, null, setCostParameters,
				description);
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (requires
	 * assignment to converge, no maximum number of iterations)
	 * 
	 * @param projectPath                          project directory containing the
	 *                                             input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output
	 *                                             properties being used
	 * @param initialCostsFileLocation             location of initial costs file
	 * @param setCostParameters                    lambda function which sets
	 *                                             parameters of cost function
	 * @param description                          description used in temporary
	 *                                             output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath,
			Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties, String initialCostsFileLocation,
			Integer maxIterations, BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters,
			String description) throws Exception {
		return setupAndExecuteAssignment(projectPath, setOutputTypeConfigurationProperties, initialCostsFileLocation,
				null, 0, maxIterations, null, setCostParameters, description);
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (requires
	 * assignment to converge, no maximum number of iterations)
	 * 
	 * @param projectPath              project directory containing the input files
	 * @param initialCostsFileLocation location of initial costs file
	 * @param setCostParameters        lambda function which sets parameters of cost
	 *                                 function
	 * @param description              description used in temporary output file
	 *                                 names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath, String initialCostsFileLocation,
			Integer maxIterations, BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters,
			String description) throws Exception {
		return setupAndExecuteAssignment(projectPath, initialCostsFileLocation, null, 0, maxIterations, null,
				setCostParameters, description);
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (uses
	 * maximum number of iterations)
	 * 
	 * @param projectPath                          project directory containing the
	 *                                             input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output
	 *                                             properties being used
	 * @param initialCostsFileLocation1            location of first initial costs
	 *                                             file
	 * @param initialCostsFileLocation2            location of second initial costs
	 *                                             file
	 * @param initCostsFilePos                     identifies which initial costs
	 *                                             file is to be used
	 * @param maxIterations                        the maximum number of iterations
	 *                                             allowed in this test run
	 * @param setCostParameters                    lambda function which sets
	 *                                             parameters of cost function
	 * @param description                          description used in temporary
	 *                                             output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath,
			Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
			String initialCostsFileLocation1, String initialCostsFileLocation2, int initCostsFilePos,
			Integer maxIterations, BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters,
			String description) throws Exception {
		return setupAndExecuteAssignment(projectPath, setOutputTypeConfigurationProperties, initialCostsFileLocation1,
				initialCostsFileLocation2, initCostsFilePos, maxIterations, null, setCostParameters, description);
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (uses
	 * maximum number of iterations)
	 * 
	 * @param projectPath               project directory containing the input files
	 * @param initialCostsFileLocation1 location of first initial costs file
	 * @param initialCostsFileLocation2 location of second initial costs file
	 * @param initCostsFilePos          identifies which initial costs file is to be
	 *                                  used
	 * @param maxIterations             the maximum number of iterations allowed in
	 *                                  this test run
	 * @param setCostParameters         lambda function which sets parameters of
	 *                                  cost function
	 * @param description               description used in temporary output file
	 *                                  names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath, String initialCostsFileLocation1,
			String initialCostsFileLocation2, int initCostsFilePos, Integer maxIterations,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description) throws Exception {
		return setupAndExecuteAssignment(projectPath, initialCostsFileLocation1, initialCostsFileLocation2,
				initCostsFilePos, maxIterations, null, setCostParameters, description);
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (uses
	 * maximum number of iterations)
	 * 
	 * @param projectPath                          project directory containing the
	 *                                             input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output
	 *                                             properties being used
	 * @param maxIterations                        the maximum number of iterations
	 *                                             allowed in this test run
	 * @param epsilon                              measure of how close successive
	 *                                             iterations must be to each other
	 *                                             to accept convergence
	 * @param setCostParameters                    lambda function which sets
	 *                                             parameters of cost function
	 * @param description                          description used in temporary
	 *                                             output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath,
			Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties, Integer maxIterations,
			Double epsilon, BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description)
			throws Exception {
		return setupAndExecuteAssignment(projectPath, setOutputTypeConfigurationProperties, null, null, 0,
				maxIterations, epsilon, setCostParameters, description);
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (uses
	 * maximum number of iterations)
	 * 
	 * @param projectPath       project directory containing the input files
	 * @param maxIterations     the maximum number of iterations allowed in this
	 *                          test run
	 * @param epsilon           measure of how close successive iterations must be
	 *                          to each other to accept convergence
	 * @param setCostParameters lambda function which sets parameters of cost
	 *                          function
	 * @param description       description used in temporary output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath, Integer maxIterations,
			Double epsilon, BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description)
			throws Exception {
		return setupAndExecuteAssignment(projectPath, null, null, 0, maxIterations, epsilon, setCostParameters,
				description);
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (requires
	 * assignment to converge, no maximum number of iterations)
	 * 
	 * @param projectPath                          project directory containing the
	 *                                             input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output
	 *                                             properties being used
	 * @param setCostParameters                    lambda function which sets
	 *                                             parameters of cost function
	 * @param description                          description used in temporary
	 *                                             output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath,
			Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description) throws Exception {
		return setupAndExecuteAssignment(projectPath, setOutputTypeConfigurationProperties, null, null, 0, null, null,
				setCostParameters, description);
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (requires
	 * assignment to converge, no maximum number of iterations)
	 * 
	 * @param projectPath       project directory containing the input files
	 * @param setCostParameters lambda function which sets parameters of cost
	 *                          function
	 * @param description       description used in temporary output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description) throws Exception {
		return setupAndExecuteAssignment(projectPath, null, null, 0, null, null, setCostParameters, description);
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter
	 * 
	 * @param projectPath                          project directory containing the
	 *                                             input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output
	 *                                             properties being used
	 * @param registerInitialCosts                 lambda function to register
	 *                                             initial costs on the Traffic
	 *                                             Assignment Builder
	 * @param maxIterations                        the maximum number of iterations
	 *                                             allowed in this test run
	 * @param epsilon                              measure of how close successive
	 *                                             iterations must be to each other
	 *                                             to accept convergence
	 * @param setCostParameters                    lambda function which sets
	 *                                             parameters of cost function
	 * @param description                          description used in temporary
	 *                                             output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath,
			Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
			TriConsumer<CapacityRestrainedTrafficAssignmentBuilder, PlanItProject, PhysicalNetwork> registerInitialCosts,
			Integer maxIterations, Double epsilon, BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters,
			String description) throws Exception {
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
		outputConfiguration.setPersistOnlyFinalIteration(true); // option to persist only the final iteration
		LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) outputConfiguration
				.getOutputTypeConfiguration(OutputType.LINK);
		setOutputTypeConfigurationProperties.accept(linkOutputTypeConfiguration);

		// OUTPUT FORMAT CONFIGURATION

		// PlanItXMLOutputFormatter
		PlanItXMLOutputFormatter xmlOutputFormatter = (PlanItXMLOutputFormatter) project
				.createAndRegisterOutputFormatter(PlanItXMLOutputFormatter.class.getCanonicalName());
		xmlOutputFormatter.setXmlNamePrefix(description);
		xmlOutputFormatter.setCsvNamePrefix(description);
		xmlOutputFormatter.setOutputDirectory(projectPath);
		taBuilder.registerOutputFormatter(xmlOutputFormatter);

		// MemoryOutputFormatter
		MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project
				.createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());
		taBuilder.registerOutputFormatter(memoryOutputFormatter);

		// "USER" configuration
		if (maxIterations != null) {
			assignment.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
		}
		if (epsilon != null) {
			assignment.getGapFunction().getStopCriterion().setEpsilon(epsilon);
		}

		registerInitialCosts.accept(taBuilder, project, physicalNetwork);

		project.executeAllTrafficAssignments();
		return memoryOutputFormatter;
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (uses
	 * maximum number of iterations and default link output properties)
	 * 
	 * @param projectPath               project directory containing the input files
	 * @param initialCostsFileLocation1 location of first initial costs file
	 * @param initialCostsFileLocation2 location of second initial costs file
	 * @param initCostsFilePos          identifies which initial costs file is to be
	 *                                  used
	 * @param maxIterations             the maximum number of iterations allowed in
	 *                                  this test run
	 * @param epsilon                   measure of how close successive iterations
	 *                                  must be to each other to accept convergence
	 * @param setCostParameters         lambda function which sets parameters of
	 *                                  cost function
	 * @param description               description used in temporary output file
	 *                                  names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 * 
	 *                   If the setCostParameters argument is null, the system
	 *                   default values for the cost function parameters are used.
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath, String initialCostsFileLocation1,
			String initialCostsFileLocation2, int initCostsFilePos, Integer maxIterations, Double epsilon,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description) throws Exception {

		TriConsumer<CapacityRestrainedTrafficAssignmentBuilder, PlanItProject, PhysicalNetwork> registerInitialCosts = (
				taBuilder, project, physicalNetwork) -> {
			InitialLinkSegmentCost initialCost = null;
			if (initialCostsFileLocation1 != null) {
				if (initialCostsFileLocation2 != null) {
					if (initCostsFilePos == 0) {
						initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork,
								initialCostsFileLocation1);
					} else {
						initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork,
								initialCostsFileLocation2);
					}
				} else {
					initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork,
							initialCostsFileLocation1);
				}
				taBuilder.registerInitialLinkSegmentCost(initialCost);
			}
		};

		return setupAndExecuteAssignment(projectPath, defaultSetOutputTypeConfigurationProperties, registerInitialCosts,
				maxIterations, epsilon, setCostParameters, description);
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (uses
	 * maximum number of iterations)
	 * 
	 * @param projectPath                          project directory containing the
	 *                                             input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output
	 *                                             type configuration output
	 *                                             properties
	 * @param initialCostsFileLocation1            location of first initial costs
	 *                                             file
	 * @param initialCostsFileLocation2            location of second initial costs
	 *                                             file
	 * @param initCostsFilePos                     identifies which initial costs
	 *                                             file is to be used
	 * @param maxIterations                        the maximum number of iterations
	 *                                             allowed in this test run
	 * @param epsilon                              measure of how close successive
	 *                                             iterations must be to each other
	 *                                             to accept convergence
	 * @param setCostParameters                    lambda function which sets
	 *                                             parameters of cost function
	 * @param description                          description used in temporary
	 *                                             output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 * 
	 *                   If the setCostParameters argument is null, the system
	 *                   default values for the cost function parameters are used.
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath,
			Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
			String initialCostsFileLocation1, String initialCostsFileLocation2, int initCostsFilePos,
			Integer maxIterations, Double epsilon, BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters,
			String description) throws Exception {

		TriConsumer<CapacityRestrainedTrafficAssignmentBuilder, PlanItProject, PhysicalNetwork> registerInitialCosts = (
				taBuilder, project, physicalNetwork) -> {
			InitialLinkSegmentCost initialCost = null;
			if (initialCostsFileLocation1 != null) {
				if (initialCostsFileLocation2 != null) {
					if (initCostsFilePos == 0) {
						initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork,
								initialCostsFileLocation1);
					} else {
						initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork,
								initialCostsFileLocation2);
					}
				} else {
					initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork,
							initialCostsFileLocation1);
				}
				taBuilder.registerInitialLinkSegmentCost(initialCost);
			}
		};

		return setupAndExecuteAssignment(projectPath, setOutputTypeConfigurationProperties, registerInitialCosts,
				maxIterations, epsilon, setCostParameters, description);
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (uses a Map
	 * of initial cost for each time period)
	 * 
	 * @param projectPath                              project directory containing
	 *                                                 the input files
	 * @param setOutputTypeConfigurationProperties     lambda function to set output
	 *                                                 type configuration output
	 *                                                 properties
	 * @param initialLinkSegmentLocationsPerTimePeriod Map of initial cost objects
	 *                                                 for each time period
	 * @param epsilon                                  measure of how close
	 *                                                 successive iterations must be
	 *                                                 to each other to accept
	 *                                                 convergence
	 * @param setCostParameters                        lambda function which sets
	 *                                                 parameters of cost function
	 * @param description                              description used in temporary
	 *                                                 output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath,
			Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
			Map<Long, String> initialLinkSegmentLocationsPerTimePeriod, Integer maxIterations, Double epsilon,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description) throws Exception {

		TriConsumer<CapacityRestrainedTrafficAssignmentBuilder, PlanItProject, PhysicalNetwork> registerInitialCosts = (
				taBuilder, project, physicalNetwork) -> {
			for (Long timePeriodId : initialLinkSegmentLocationsPerTimePeriod.keySet()) {
				TimePeriod timePeriod = TimePeriod.getById(timePeriodId);
				String initialCostsFileLocation = initialLinkSegmentLocationsPerTimePeriod.get(timePeriodId);
				InitialLinkSegmentCost initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork,
						initialCostsFileLocation);
				taBuilder.registerInitialLinkSegmentCost(timePeriod, initialCost);
			}
		};

		return setupAndExecuteAssignment(projectPath, setOutputTypeConfigurationProperties, registerInitialCosts,
				maxIterations, epsilon, setCostParameters, description);
	}

	/**
	 * Run a test case and store the results in a MemoryOutputFormatter (uses a Map
	 * of initial cost for each time period and default link output properties))
	 * 
	 * @param projectPath                              project directory containing
	 *                                                 the input files
	 * @param initialLinkSegmentLocationsPerTimePeriod Map of initial cost objects
	 *                                                 for each time period
	 * @param epsilon                                  measure of how close
	 *                                                 successive iterations must be
	 *                                                 to each other to accept
	 *                                                 convergence
	 * @param setCostParameters                        lambda function which sets
	 *                                                 parameters of cost function
	 * @param description                              description used in temporary
	 *                                                 output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath,
			Map<Long, String> initialLinkSegmentLocationsPerTimePeriod, Integer maxIterations, Double epsilon,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description) throws Exception {

		TriConsumer<CapacityRestrainedTrafficAssignmentBuilder, PlanItProject, PhysicalNetwork> registerInitialCosts = (
				taBuilder, project, physicalNetwork) -> {
			for (Long timePeriodId : initialLinkSegmentLocationsPerTimePeriod.keySet()) {
				TimePeriod timePeriod = TimePeriod.getById(timePeriodId);
				String initialCostsFileLocation = initialLinkSegmentLocationsPerTimePeriod.get(timePeriodId);
				InitialLinkSegmentCost initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork,
						initialCostsFileLocation);
				taBuilder.registerInitialLinkSegmentCost(timePeriod, initialCost);
			}
		};

		return setupAndExecuteAssignment(projectPath, defaultSetOutputTypeConfigurationProperties, registerInitialCosts,
				maxIterations, epsilon, setCostParameters, description);
	}

	/**
	 * Deletes a file from the file system
	 * 
	 * @param filename location of the file to be deleted
	 * @throws Exception thrown if there is an error deleting the file
	 */
	public static void deleteFile(String filename) throws Exception {
		String rootPath = System.getProperty("user.dir");
		Path path = FileSystems.getDefault().getPath(rootPath + "\\" + filename);
		Files.delete(path);
	}

	/**
	 * Delete a file from the directory of test files
	 * 
	 * @param projectPath path to the test directory
	 * @param description description part of the file name
	 * @param fileName    other part of the file name
	 * @throws Exception thrown if there is an error deleting the file
	 */
	public static void deleteFile(String projectPath, String description, String fileName) throws Exception {
		deleteFile(projectPath + "\\" + description + "_" + fileName);
	}

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
	public static boolean compareFiles(String file1, String file2) throws IOException {
		File f1 = new File(file1);
		File f2 = new File(file2);
		boolean result = FileUtils.contentEqualsIgnoreEOL(f1, f2, "utf-8");
		return result;
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
	public static boolean isXmlFileSameExceptForTimestamp(String xmlFile1, String xmlFile2) throws Exception {
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
			if (iteration1.getNr().intValue() != iteration2.getNr().intValue()) {
				return false;
			}
			if (!iteration1.getCsvdata().equals(iteration2.getCsvdata())) {
				return false;
			}
		}
		// Time stamps should be different, to show that the two files were created
		// separately
		if (metadata1.getTimestamp().compare(metadata2.getTimestamp()) == DatatypeConstants.EQUAL) {
			return false;
		}
		return true;
	}

}
