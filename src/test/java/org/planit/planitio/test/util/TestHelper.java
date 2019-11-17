package org.planit.planitio.test.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.xml.datatype.DatatypeConstants;

import org.apache.commons.io.FileUtils;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.generated.XMLElementColumn;
import org.planit.generated.XMLElementCsvdata;
import org.planit.generated.XMLElementIteration;
import org.planit.generated.XMLElementMetadata;
import org.planit.generated.XMLElementOutputConfiguration;
import org.planit.generated.XMLElementOutputTimePeriod;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.OriginDestinationOutputTypeConfiguration;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.planitio.output.formatter.PlanItOutputFormatter;
import org.planit.planitio.project.PlanItProject;
import org.planit.planitio.test.integration.LinkSegmentExpectedResultsDto;
import org.planit.planitio.xml.util.XmlUtils;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.DeterministicTrafficAssignment;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.CapacityRestrainedTrafficAssignmentBuilder;
import org.planit.userclass.Mode;
import org.planit.utils.IdGenerator;
import org.planit.utils.TriConsumer;
import org.planit.zoning.Zoning;

/**
 * Helper class used by unit tests
 * 
 * @author gman6028
 *
 */
public class TestHelper {

	private static final double epsilon = 0.00001;

	private static Consumer<LinkOutputTypeConfiguration> defaultSetOutputTypeConfigurationProperties = (
			linkOutputTypeConfiguration) -> {
		try {
			linkOutputTypeConfiguration.addAllProperties();
			linkOutputTypeConfiguration.removeProperty(OutputProperty.RUN_ID);
			linkOutputTypeConfiguration.removeProperty(OutputProperty.LINK_SEGMENT_EXTERNAL_ID);
			linkOutputTypeConfiguration.removeProperty(OutputProperty.ITERATION_INDEX);
			linkOutputTypeConfiguration.removeProperty(OutputProperty.DESTINATION_ZONE_ID);
			linkOutputTypeConfiguration.removeProperty(OutputProperty.ORIGIN_ZONE_ID);
			linkOutputTypeConfiguration.removeProperty(OutputProperty.DESTINATION_ZONE_EXTERNAL_ID);
			linkOutputTypeConfiguration.removeProperty(OutputProperty.ORIGIN_ZONE_EXTERNAL_ID);
			linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
			linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_ID);
			linkOutputTypeConfiguration.removeProperty(OutputProperty.TOTAL_COST_TO_END_NODE);
			linkOutputTypeConfiguration.removeProperty(OutputProperty.MAXIMUM_SPEED);
			linkOutputTypeConfiguration.removeProperty(OutputProperty.OD_COST);
		} catch (PlanItException e) {
			e.printStackTrace();
		}
	};

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
			SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>> resultsMap)
			throws PlanItException {

		if (iterationIndex == null) {
			iterationIndex = memoryOutputFormatter.getLastIteration();
		}
		for (TimePeriod timePeriod : resultsMap.keySet()) {
			for (Mode mode : resultsMap.get(timePeriod).keySet()) {
				for (LinkSegmentExpectedResultsDto resultDto : resultsMap.get(timePeriod).get(mode)) {
					OutputProperty[] outputKeyProperties = memoryOutputFormatter.getOutputKeyProperties(outputType);
					OutputProperty[] outputValueProperties = memoryOutputFormatter.getOutputValueProperties(outputType);
					Object[] keyValues = new Object[outputKeyProperties.length];
					if (keyValues.length == 2) {
						keyValues[0] = Long.valueOf((int) resultDto.getStartNodeId());
						keyValues[1] = Long.valueOf((int) resultDto.getEndNodeId());
					}
					if (keyValues.length == 1) {
						keyValues[0] = Long.valueOf((int) resultDto.getLinkSegmentId());
					}
					for (int i = 0; i < outputValueProperties.length; i++) {
						switch (outputValueProperties[i]) {
						case FLOW:
							double flow = (Double) memoryOutputFormatter.getOutputDataValue(mode, timePeriod, iterationIndex, outputType, OutputProperty.FLOW, keyValues);
							assertEquals(flow, resultDto.getLinkFlow(), epsilon);
							break;
						case LENGTH:
							double length = (Double) memoryOutputFormatter.getOutputDataValue(mode, timePeriod, iterationIndex, outputType, OutputProperty.LENGTH, keyValues);
							assertEquals(length, resultDto.getLength(), epsilon);
							break;
						case CALCULATED_SPEED:
							double speed = (Double) memoryOutputFormatter.getOutputDataValue(mode, timePeriod, iterationIndex, outputType, OutputProperty.CALCULATED_SPEED, keyValues);
							assertEquals(speed, resultDto.getSpeed(), epsilon);
							break;
						case LINK_COST:
							double cost = (Double) memoryOutputFormatter.getOutputDataValue(mode, timePeriod, iterationIndex, outputType, OutputProperty.LINK_COST, keyValues);
							assertEquals(cost, resultDto.getLinkCost(), epsilon);
							break;
						case CAPACITY_PER_LANE:
							double capacityPerLane = (Double) memoryOutputFormatter.getOutputDataValue(mode, timePeriod, iterationIndex, outputType, OutputProperty.CAPACITY_PER_LANE, keyValues);
							int numberOfLanes = (Integer) memoryOutputFormatter.getOutputDataValue(mode, timePeriod, iterationIndex, outputType, OutputProperty.NUMBER_OF_LANES, keyValues);
							assertEquals(numberOfLanes * capacityPerLane, resultDto.getCapacity(), epsilon);
							break;
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
	 * @param projectPath project directory containing the input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output properties being used
	 * @param maxIterations the maximum number of iterations allowed in this test run
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
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
	 * @param projectPath project directory containing the input files
	 * @param maxIterations the maximum number of iterations allowed in this test run
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
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
	 * @param projectPath project directory containing the input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output properties being used
	 * @param initialCostsFileLocation location of initial costs file
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
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
	 * @param projectPath project directory containing the input files
	 * @param initialCostsFileLocation location of initial costs file
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
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
	 * @param projectPath project directory containing the input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output properties being used
	 * @param initialCostsFileLocation1 location of first initial costs file
	 * @param initialCostsFileLocation2 location of second initial costs file
	 * @param initCostsFilePos identifies which initial costs file is to be used
	 * @param maxIterations the maximum number of iterations allowed in this test run
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
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
	 * @param projectPath project directory containing the input files
	 * @param initialCostsFileLocation1 location of first initial costs file
	 * @param initialCostsFileLocation2 location of second initial costs file
	 * @param initCostsFilePos identifies which initial costs file is to be used
	 * @param maxIterations the maximum number of iterations allowed in this test run
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
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
	 * @param projectPath project directory containing the input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output properties being used
	 * @param maxIterations the maximum number of iterations allowed in this test run
	 * @param epsilon measure of how close successive iterations must be to each other to accept convergence
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
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
	 * @param projectPath project directory containing the input files
	 * @param maxIterations the maximum number of iterations allowed in this test run
	 * @param epsilon measure of how close successive iterations must be to each other to accept convergence
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
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
	 * @param projectPath project directory containing the input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output properties being used
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
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
	 * @param projectPath project directory containing the input files
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 */
	public static MemoryOutputFormatter setupAndExecuteAssignment(String projectPath,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description) throws Exception {
		return setupAndExecuteAssignment(projectPath, null, null, 0, null, null, setCostParameters, description);
	}
	
	public static MemoryOutputFormatter setupAndExecuteAssignmentAttemptToChangeLockedFormatter(String projectPath,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description) throws Exception {
		return setupAndExecuteAssignmentAttemptToChangeLockedFormatter(projectPath, null, null, 0, null, null, setCostParameters, description);
	}
	
	public static MemoryOutputFormatter setupAndExecuteAssignmentAttemptToChangeLockedFormatter(String projectPath, String initialCostsFileLocation1,
			String initialCostsFileLocation2, int initCostsFilePos, Integer maxIterations, Double epsilon,
			BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, String description) throws Exception {

		TriConsumer<CapacityRestrainedTrafficAssignmentBuilder, PlanItProject, PhysicalNetwork> registerInitialCosts = (
				taBuilder, project, physicalNetwork) -> {
			InitialLinkSegmentCost initialCost = null;
			if (initialCostsFileLocation1 != null) {
				if (initialCostsFileLocation2 != null) {
					if (initCostsFilePos == 0) {
						initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCostsFileLocation1);
					} else {
						initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCostsFileLocation2);
					}
				} else {
					initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCostsFileLocation1);
				}
				taBuilder.registerInitialLinkSegmentCost(initialCost);
			}
		};

		return setupAndExecuteAssignmentAttemptToChangeLockedFormatter(projectPath, defaultSetOutputTypeConfigurationProperties, registerInitialCosts,
				maxIterations, epsilon, setCostParameters, description);
	}

	
	
	public static MemoryOutputFormatter setupAndExecuteAssignmentAttemptToChangeLockedFormatter(String projectPath,
			Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
			TriConsumer<CapacityRestrainedTrafficAssignmentBuilder, PlanItProject, PhysicalNetwork> registerInitialCosts,
			Integer maxIterations, Double epsilon, BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters,
			String description) throws Exception {
		IdGenerator.reset();

		PlanItProject project = new PlanItProject(projectPath);

		// RAW INPUT START --------------------------------
		PhysicalNetwork physicalNetwork = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
		Zoning zoning = project.createAndRegisterZoning(physicalNetwork);
		Demands demands = project.createAndRegisterDemands(zoning);
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

		taBuilder.createAndRegisterVirtualTravelTimeCostFunction(SpeedConnectoidTravelTimeCost.class.getCanonicalName());
		taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());

		// SUPPLY-DEMAND INTERFACE
		taBuilder.registerDemandsAndZoning(demands, zoning);	

		// DATA OUTPUT CONFIGURATION
		assignment.activateOutput(OutputType.LINK);
		assignment.activateOutput(OutputType.OD);
		OutputConfiguration outputConfiguration = assignment.getOutputConfiguration();
		
		//PlanItXML test cases use expect outputConfiguration.setPersistOnlyFinalIteration() to be set to true - outputs will not match test data otherwise
		outputConfiguration.setPersistOnlyFinalIteration(true);
		LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) outputConfiguration.getOutputTypeConfiguration(OutputType.LINK);
		setOutputTypeConfigurationProperties.accept(linkOutputTypeConfiguration);
		OriginDestinationOutputTypeConfiguration originDestinationOutputTypeConfiguration = (OriginDestinationOutputTypeConfiguration) outputConfiguration	.getOutputTypeConfiguration(OutputType.OD);
		originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
		originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.RUN_ID);
		
		// OUTPUT FORMAT CONFIGURATION

		// PlanItXMLOutputFormatter
		PlanItOutputFormatter xmlOutputFormatter = (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(PlanItOutputFormatter.class.getCanonicalName());
		xmlOutputFormatter.setXmlNameRoot(description);
		xmlOutputFormatter.setCsvNameRoot(description);
		xmlOutputFormatter.setOutputDirectory(projectPath);
		taBuilder.registerOutputFormatter(xmlOutputFormatter);

		// MemoryOutputFormatter
		MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project.createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());
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
		linkOutputTypeConfiguration.addAllProperties();
		project.executeAllTrafficAssignments();
		return memoryOutputFormatter;
	}



	/**
	 * Run a test case and store the results in a MemoryOutputFormatter
	 * 
	 * @param projectPath project directory containing the input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output properties being used
	 * @param registerInitialCosts lambda function to register initial costs on the Traffic Assignment Builder
	 * @param maxIterations the maximum number of iterations allowed in this test run
	 * @param epsilon measure of how close successive iterations must be to each other  to accept convergence
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
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
		PhysicalNetwork physicalNetwork = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
		Zoning zoning = project.createAndRegisterZoning(physicalNetwork);
		Demands demands = project.createAndRegisterDemands(zoning);
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

		taBuilder.createAndRegisterVirtualTravelTimeCostFunction(SpeedConnectoidTravelTimeCost.class.getCanonicalName());
		taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());

		// SUPPLY-DEMAND INTERFACE
		taBuilder.registerDemandsAndZoning(demands, zoning);	

		// DATA OUTPUT CONFIGURATION
		assignment.activateOutput(OutputType.LINK);
		assignment.activateOutput(OutputType.OD);
		OutputConfiguration outputConfiguration = assignment.getOutputConfiguration();
		
		//PlanItXML test cases use expect outputConfiguration.setPersistOnlyFinalIteration() to be set to true - outputs will not match test data otherwise
		outputConfiguration.setPersistOnlyFinalIteration(true);
		LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) outputConfiguration.getOutputTypeConfiguration(OutputType.LINK);
		setOutputTypeConfigurationProperties.accept(linkOutputTypeConfiguration);
		OriginDestinationOutputTypeConfiguration originDestinationOutputTypeConfiguration = (OriginDestinationOutputTypeConfiguration) outputConfiguration	.getOutputTypeConfiguration(OutputType.OD);
		originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
		originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.RUN_ID);
		
		// OUTPUT FORMAT CONFIGURATION

		// PlanItXMLOutputFormatter
		PlanItOutputFormatter xmlOutputFormatter = (PlanItOutputFormatter) project
				.createAndRegisterOutputFormatter(PlanItOutputFormatter.class.getCanonicalName());
		xmlOutputFormatter.setXmlNameRoot(description);
		xmlOutputFormatter.setCsvNameRoot(description);
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
	 * @param projectPath project directory containing the input files
	 * @param initialCostsFileLocation1 location of first initial costs file
	 * @param initialCostsFileLocation2 location of second initial costs file
	 * @param initCostsFilePos identifies which initial costs file is to be used
	 * @param maxIterations the maximum number of iterations allowed in this test run
	 * @param epsilon measure of how close successive iterations must be to each other to accept convergence
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
	 * @return MemoryOutputFormatter containing results from the run
	 * @throws Exception thrown if there is an error
	 * 
	 * If the setCostParameters argument is null, the system default values for the cost function parameters are used.
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
						initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCostsFileLocation1);
					} else {
						initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCostsFileLocation2);
					}
				} else {
					initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCostsFileLocation1);
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
	 * @param projectPath project directory containing the input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output
	 *                                             type configuration output properties
	 * @param initialCostsFileLocation1 location of first initial costs file
	 * @param initialCostsFileLocation2 location of second initial costs file
	 * @param initCostsFilePos identifies which initial costs file is to be used
	 * @param maxIterations the maximum number of iterations allowed in this test run
	 * @param epsilon measure of how close successive iterations must be to each other
	 *                                             to accept convergence
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
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
						initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCostsFileLocation1);
					} else {
						initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCostsFileLocation2);
					}
				} else {
					initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCostsFileLocation1);
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
	 * @param projectPath project directory containing the input files
	 * @param setOutputTypeConfigurationProperties lambda function to set output type configuration output properties
	 * @param initialLinkSegmentLocationsPerTimePeriod Map of initial cost objects for each time period
	 * @param epsilon measure of how close successive iterations must be to each other to accept convergence
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
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
				InitialLinkSegmentCost initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork,	initialCostsFileLocation);
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
	 * @param projectPath project directory containing  the input files
	 * @param initialLinkSegmentLocationsPerTimePeriod Map of initial cost objects for each time period
	 * @param epsilon measure of how close successive iterations must be to each other to accept convergence
	 * @param setCostParameters lambda function which sets parameters of cost function
	 * @param description description used in temporary output file names
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
				InitialLinkSegmentCost initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork,	initialCostsFileLocation);
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
	public static void deleteFile(OutputType outputType, String projectPath, String description, String fileName) throws Exception {
		deleteFile(projectPath + "\\" + outputType.value() + "_" +  description + "_" + fileName);
	}

	/**
	 * Compares the contents of two text files
	 * 
	 * In this test the text contents of the files must be exactly equal. This test
	 * can be applied to any file type (CSV, XML etc)
	 * 
	 * @param file1 location of the first file to be compared
	 * @param file2 location of the second file to be compared
	 * @return true if the contents of the two files are exactly equal, false otherwise
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
	 * @param xmlFileStandard location of the first XML file to be compared (expected results, created previously)
	 * @param xmlFileBeingTested location of the second XML file to be compared (created by the current test case) 
	 * @return true if the test passes, false otherwise
	 * @throws Exception thrown if the there is an error opening one of the files
	 */
	public static boolean isXmlFileSameExceptForTimestamp(String xmlFileStandard, String xmlFileBeingTested) throws Exception {
		XMLElementMetadata metadataStandard = (XMLElementMetadata) XmlUtils.generateObjectFromXml(XMLElementMetadata.class,
				xmlFileStandard);
		XMLElementMetadata metadataBeingTested = (XMLElementMetadata) XmlUtils.generateObjectFromXml(XMLElementMetadata.class,
				xmlFileBeingTested);
		
		//compare <columns> and <column> elements in the generated output file against the standard output file
		List<XMLElementColumn> elementColumnsStandard = metadataStandard.getColumns().getColumn();
		List<XMLElementColumn> elementColumnsBeingTested = metadataBeingTested.getColumns().getColumn();
		int sizeElementColumnsStandard = elementColumnsStandard.size();
		int sizeElementColumnsBeingTested = elementColumnsBeingTested.size();
		if (sizeElementColumnsStandard != sizeElementColumnsBeingTested) {
			return false;
		}
		for (int i = 0; i < sizeElementColumnsStandard; i++) {
			XMLElementColumn elementColumnStandard = elementColumnsStandard.get(i);
			XMLElementColumn elementColumnBeingTested = elementColumnsBeingTested.get(i);
			if (!elementColumnStandard.getName().equals(elementColumnBeingTested.getName())) {
				return false;
			}
			if (!elementColumnStandard.getUnits().equals(elementColumnBeingTested.getUnits())) {
				return false;
			}
			if (!elementColumnStandard.getType().equals(elementColumnBeingTested.getType())) {
				return false;
			}
		}

		//compare <outputconfiguration> elements in the generated output file against the standard output file
		XMLElementOutputConfiguration outputConfigurationStandard = metadataStandard.getOutputconfiguration();
		XMLElementOutputConfiguration outputConfigurationBeingTested = metadataBeingTested.getOutputconfiguration();
		if (!outputConfigurationStandard.getAssignment().equals(outputConfigurationBeingTested.getAssignment())) {
			return false;
		}
		if (!outputConfigurationStandard.getPhysicalcost().equals(outputConfigurationBeingTested.getPhysicalcost())) {
			return false;
		}
		if (!outputConfigurationStandard.getVirtualcost().equals(outputConfigurationBeingTested.getVirtualcost())) {
			return false;
		}
		XMLElementOutputTimePeriod timeperiodStandard = outputConfigurationStandard.getTimeperiod();
		XMLElementOutputTimePeriod timeperiodBeingTested = outputConfigurationBeingTested.getTimeperiod();
		if (!timeperiodStandard.getId().equals(timeperiodBeingTested.getId())) {
			return false;
		}
		if (!timeperiodStandard.getName().equals(timeperiodBeingTested.getName())) {
			return false;
		}
		
		//compare <simulation> elements in the generated output file against the standard output file
		List<XMLElementIteration> iterationsStandard = metadataStandard.getSimulation().getIteration();
		int iterationsSizeStandard = iterationsStandard.size();
		List<XMLElementIteration> iterationsBeingTested = metadataBeingTested.getSimulation().getIteration();
		int iterationsSizeBeingTested = iterationsBeingTested.size();
		if (iterationsSizeStandard != iterationsSizeBeingTested) {
			return false;
		}

		for (int i = 0; i < iterationsSizeStandard; i++) {
			XMLElementIteration iterationStandard= iterationsStandard.get(i);
			XMLElementIteration iterationBeingTested = iterationsBeingTested.get(i);
			if (iterationStandard.getNr().intValue() != iterationBeingTested.getNr().intValue()) {
				return false;
			}
			List<XMLElementCsvdata> csvDataListStandard = iterationStandard.getCsvdata();
			int sizeCsvDataListStandard = csvDataListStandard.size();
			List<XMLElementCsvdata> csvDataListBeingTested = iterationBeingTested.getCsvdata();
			int sizeCsvDataListBeingTested = csvDataListBeingTested.size();
			if (sizeCsvDataListStandard != sizeCsvDataListBeingTested) {
				return false;
			}
			for (int j=0; j<sizeCsvDataListStandard; j++) {
				XMLElementCsvdata csvDataStandard = csvDataListStandard.get(j);
				XMLElementCsvdata csvDataBeingTested = csvDataListBeingTested.get(j);
				if (!csvDataStandard.getValue().equals(csvDataBeingTested.getValue())) {
					return false;
				}
				if (!csvDataStandard.getType().equals(csvDataBeingTested.getType())) {
					return false;
				}
			}
		}
		// Time stamps should be different, to show that the two files were created
		// separately
		if (metadataStandard.getTimestamp().compare(metadataBeingTested.getTimestamp()) == DatatypeConstants.EQUAL) {
			return false;
		}
		return true;
	}

}
