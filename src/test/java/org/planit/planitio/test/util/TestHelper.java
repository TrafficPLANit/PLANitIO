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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.xml.datatype.DatatypeConstants;

import org.apache.commons.io.FileUtils;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.generated.XMLElementColumn;
import org.planit.generated.XMLElementCsvdata;
import org.planit.generated.XMLElementIteration;
import org.planit.generated.XMLElementMetadata;
import org.planit.generated.XMLElementOutputConfiguration;
import org.planit.generated.XMLElementOutputTimePeriod;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.OriginDestinationOutputTypeConfiguration;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.PathOutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.RouteIdType;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.formatter.MemoryOutputIterator;
import org.planit.output.property.OutputProperty;
import org.planit.planitio.input.PlanItInputBuilder;
import org.planit.planitio.output.formatter.PlanItOutputFormatter;
import org.planit.planitio.test.integration.LinkSegmentExpectedResultsDto;
import org.planit.planitio.xml.util.XmlUtils;
import org.planit.project.CustomPlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.time.TimePeriod;
import org.planit.trafficassignment.TraditionalStaticAssignment;
import org.planit.trafficassignment.builder.TraditionalStaticAssignmentBuilder;
import org.planit.utils.TriConsumer;
import org.planit.utils.TriFunction;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.misc.Pair;
import org.planit.utils.network.physical.Mode;

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
      linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
      linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_ID);
      linkOutputTypeConfiguration.removeProperty(OutputProperty.TOTAL_COST_TO_END_NODE);
      linkOutputTypeConfiguration.removeProperty(OutputProperty.MAXIMUM_SPEED);
    } catch (final PlanItException e) {
      e.printStackTrace();
    }
  };

  /**
   * Compares the results from an assignment run stored in a MemoryOutputFormatter
   * object to known results stored in a Map. It generates a JUnit test failure if
   * the results maps have different contents.
   * 
   * @param memoryOutputFormatter the MemoryOuptutFormatter object which stores
   *          results from a test run
   * @param iteration the current iteration index
   * @param resultsMap Map containing the standard results for each time period and mode
   * @param getPositionKeys lambda function which generates the position of the key(s) in the key array
   * @param getResultDto lambda function which generates the known result for each iteration
   * @throws PlanItException thrown if there is an error
   */
  private static void compareResultsToMemoryOutputFormatter(
      final MemoryOutputFormatter memoryOutputFormatter, final Integer iterationIndex,
      final SortedMap<TimePeriod, ? extends SortedMap<Mode, ? extends Object>> resultsMap,
      TriFunction<Mode, TimePeriod, Integer, Object> getPositionKeys,
      TriFunction<Pair<Integer, Integer>, Object, Object[], LinkSegmentExpectedResultsDto> getResultDto) throws PlanItException {
    final int iteration = (iterationIndex == null) ? memoryOutputFormatter.getLastIteration() : iterationIndex;
    for (final TimePeriod timePeriod : resultsMap.keySet()) {
      for (final Mode mode : resultsMap.get(timePeriod).keySet()) {
        Object innerMap = resultsMap.get(timePeriod).get(mode);
        final int flowPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(mode, timePeriod,
            iteration, OutputType.LINK, OutputProperty.FLOW);
        final int costPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(mode, timePeriod,
            iteration, OutputType.LINK, OutputProperty.LINK_COST);
        final int lengthPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(mode, timePeriod,
            iteration, OutputType.LINK, OutputProperty.LENGTH);
        final int speedPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(mode, timePeriod,
            iteration, OutputType.LINK, OutputProperty.CALCULATED_SPEED);
        final int capacityPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(mode, timePeriod,
            iteration, OutputType.LINK, OutputProperty.CAPACITY_PER_LANE);
        final int numberOfLanesPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(mode, timePeriod,
            iteration, OutputType.LINK, OutputProperty.NUMBER_OF_LANES);
        final MemoryOutputIterator memoryOutputIterator = memoryOutputFormatter.getIterator(mode, timePeriod,
            iteration, OutputType.LINK);
        Object obj = getPositionKeys.apply(mode, timePeriod, iteration);
        if (obj instanceof PlanItException) {
          PlanItException pe = (PlanItException) obj;
          throw pe;
        }
       
        Pair<Integer, Integer> positionKeys = (Pair<Integer, Integer>) obj;
        while (memoryOutputIterator.hasNext()) {
          final Object[] keys = memoryOutputIterator.getKeys();
          LinkSegmentExpectedResultsDto resultDto = getResultDto.apply(positionKeys, innerMap, keys);
          final Object[] results = memoryOutputIterator.getValues();
          final double flow = (Double) results[flowPosition];
          final double cost = (Double) results[costPosition];
          final double length = (Double) results[lengthPosition];
          final double speed = (Double) results[speedPosition];
          final double capacityPerLane = (Double) results[capacityPosition];
          final int numberOfLanes = (Integer) results[numberOfLanesPosition];
          assertEquals(flow, resultDto.getLinkFlow(), epsilon);
          assertEquals(length, resultDto.getLength(), epsilon);
          assertEquals(speed, resultDto.getSpeed(), epsilon);
          assertEquals(cost, resultDto.getLinkCost(), epsilon);
          assertEquals(numberOfLanes * capacityPerLane, resultDto.getCapacity(), epsilon);
        }
      }
    }
  }
 
  /**
   * Compares the results from an assignment run stored in a MemoryOutputFormatter
   * object to known results stored in a Map. It generates a JUnit test failure if
   * the results maps have different contents.
   * 
   * This method uses links which are identified by the external Ids of the start and end nodes.
   *
   * @param memoryOutputFormatter the MemoryOuptutFormatter object which stores
   *          results from a test run
   * @param iterationIndex the current iteration index
   * @param resultsMap Map storing standard test results which have been generated previously,
   *          identified by start and end node external Ids
   * @throws PlanItException thrown if one of the test output properties has not
   *           been saved
   */
  public static void compareResultsToMemoryOutputFormatterUsingNodesExternalId(
      final MemoryOutputFormatter memoryOutputFormatter, final Integer iterationIndex,
      final SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>>> resultsMap)
      throws PlanItException {
    compareResultsToMemoryOutputFormatter(memoryOutputFormatter, iterationIndex, resultsMap,
        (mode, timePeriod, iteration) -> {
          try {
          final int downstreamNodeExternalIdPosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(mode,
              timePeriod, iteration, OutputType.LINK, OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID);
          final int upstreamNodeExternalIdPosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(mode,
              timePeriod, iteration, OutputType.LINK, OutputProperty.UPSTREAM_NODE_EXTERNAL_ID);
          return new Pair<Integer, Integer>(downstreamNodeExternalIdPosition, upstreamNodeExternalIdPosition);
          } catch (PlanItException pe) {
            return pe;
          }
        },
        (positionKeys, innerObj, keys) -> {
          final SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>> innerMap =
              (SortedMap<Long, SortedMap<Long, LinkSegmentExpectedResultsDto>>) innerObj;
          final int downstreamNodeExternalIdPosition = positionKeys.getFirst();
          final int upstreamNodeExternalIdPosition = positionKeys.getSecond();
          final long upstreamNodeExternalId = (Long) keys[downstreamNodeExternalIdPosition];
          final long downstreamNodeExternalId = (Long) keys[upstreamNodeExternalIdPosition];
          return innerMap.get(upstreamNodeExternalId).get(downstreamNodeExternalId);
        });
  }
  
  /**
   * Compares the results from an assignment run stored in a MemoryOutputFormatter
   * object to known results stored in a Map. It generates a JUnit test failure if
   * the results maps have different contents.
   * 
   * This method uses links which are identified by link segment Ids.
   *
   * @param memoryOutputFormatter the MemoryOuptutFormatter object which stores
   *          results from a test run
   * @param iterationIndex the current iteration index
   * @param resultsMap Map storing standard test results which have been generated previously,
   *          identified link segment Id
   * @throws PlanItException thrown if one of the test output properties has not
   *           been saved
   */
  public static void compareResultsToMemoryOutputFormatterUsingLinkSegmentId(
      final MemoryOutputFormatter memoryOutputFormatter, final Integer iterationIndex,
      final SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, LinkSegmentExpectedResultsDto>>> resultsMap)
      throws PlanItException {
    compareResultsToMemoryOutputFormatter(memoryOutputFormatter, iterationIndex, resultsMap,
        (mode, timePeriod, iteration) -> {
          try {
            final int linkSegmentIdPosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(mode, timePeriod,
                iteration, OutputType.LINK, OutputProperty.LINK_SEGMENT_ID);
          return new Pair<Integer, Integer>(linkSegmentIdPosition, 0);
          } catch (PlanItException pe) {
            return pe;
          }
        },
        (positionKeys, innerObj, keys) -> {
          final SortedMap<Long, LinkSegmentExpectedResultsDto> innerMap = (SortedMap<Long, LinkSegmentExpectedResultsDto>) innerObj;
          final int linkSegmentIdPosition = positionKeys.getFirst();
          final long linkSegmentId = (Long) keys[linkSegmentIdPosition];
          return innerMap.get(linkSegmentId);
        });
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
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties, final Integer maxIterations,
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters, 
      final String description)
      throws Exception {
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
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final Integer maxIterations,
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters,
      final String description)
      throws Exception {
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
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
      final String initialCostsFileLocation,
      final Integer maxIterations, 
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters, 
      final String description) throws Exception {
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
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final String initialCostsFileLocation,
      final Integer maxIterations, 
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters, 
      final String description) throws Exception {
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
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
      final String initialCostsFileLocation1, final String initialCostsFileLocation2, final int initCostsFilePos,
      final Integer maxIterations, 
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters, 
      final String description) throws Exception {
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
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final String initialCostsFileLocation1,
      final String initialCostsFileLocation2, final int initCostsFilePos, final Integer maxIterations,
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters, 
      final String description)
      throws Exception {
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
   * @param epsilon measure of how close successive iterations must be to each other to accept
   *          convergence
   * @param setCostParameters lambda function which sets parameters of cost function
   * @param description description used in temporary output file names
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties, final Integer maxIterations,
      final Double epsilon, 
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters, 
      final String description)
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
   * @param epsilon measure of how close successive iterations must be to each other to accept
   *          convergence
   * @param setCostParameters lambda function which sets parameters of cost function
   * @param description description used in temporary output file names
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final Integer maxIterations,
      final Double epsilon, 
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters, 
      final String description)
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
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters, 
      final String description)
      throws Exception {
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
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters, 
      final String description)
      throws Exception {
    return setupAndExecuteAssignment(projectPath, null, null, 0, null, null, setCostParameters, description);
  }

  public static TestOutputDto setupAndExecuteAssignmentAttemptToChangeLockedFormatter(
      final String projectPath,
      final BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, final String description)
      throws Exception {
    return setupAndExecuteAssignmentAttemptToChangeLockedFormatter(projectPath, null, null, 0, null, null,
        setCostParameters, description);
  }

  public static TestOutputDto setupAndExecuteAssignmentAttemptToChangeLockedFormatter(
      final String projectPath, final String initialCostsFileLocation1,
      final String initialCostsFileLocation2, final int initCostsFilePos, final Integer maxIterations,
      final Double epsilon,
      final BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters, final String description)
      throws Exception {

    final TriConsumer<TraditionalStaticAssignmentBuilder, CustomPlanItProject, PhysicalNetwork> registerInitialCosts = (
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

    return setupAndExecuteAssignmentAttemptToChangeLockedFormatter(projectPath,
        defaultSetOutputTypeConfigurationProperties, registerInitialCosts, maxIterations, epsilon, setCostParameters,
        description);
  }

  public static TestOutputDto setupAndExecuteAssignmentAttemptToChangeLockedFormatter(
      final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
      final TriConsumer<TraditionalStaticAssignmentBuilder, CustomPlanItProject, PhysicalNetwork> registerInitialCosts,
      final Integer maxIterations, final Double epsilon,
      final BiConsumer<PhysicalNetwork, BPRLinkTravelTimeCost> setCostParameters,
      final String description) throws Exception {
    IdGenerator.reset();

    PlanItInputBuilder planItInputBuilder = new PlanItInputBuilder(projectPath);
    final CustomPlanItProject project = new CustomPlanItProject(planItInputBuilder);

    // RAW INPUT START --------------------------------
    final PhysicalNetwork physicalNetwork = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class
        .getCanonicalName());
    final Zoning zoning = project.createAndRegisterZoning(physicalNetwork);
    final Demands demands = project.createAndRegisterDemands(zoning, physicalNetwork);
    // RAW INPUT END -----------------------------------

    // TRAFFIC ASSIGNMENT START------------------------
    final TraditionalStaticAssignmentBuilder taBuilder =
        (TraditionalStaticAssignmentBuilder) project.createAndRegisterTrafficAssignment(
            TraditionalStaticAssignment.class.getCanonicalName(), demands, zoning, physicalNetwork);

    // SUPPLY-DEMAND INTERACTIONS
    final BPRLinkTravelTimeCost bprLinkTravelTimeCost = (BPRLinkTravelTimeCost) taBuilder
        .createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
    if (setCostParameters != null) {
      setCostParameters.accept(physicalNetwork, bprLinkTravelTimeCost);
    }

    taBuilder.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
    taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());

    // DATA OUTPUT CONFIGURATION
    // PlanItXML test cases use expect outputConfiguration.setPersistOnlyFinalIteration() to be set
    // to true - outputs will not match test data otherwise
    final OutputConfiguration outputConfiguration = taBuilder.getOutputConfiguration();
    outputConfiguration.setPersistOnlyFinalIteration(true);

    // LINK OUTPUT
    final LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) taBuilder
        .activateOutput(OutputType.LINK);
    if (setOutputTypeConfigurationProperties != null) {
      setOutputTypeConfigurationProperties.accept(linkOutputTypeConfiguration);
    }

    // OD OUTPUT
    final OriginDestinationOutputTypeConfiguration originDestinationOutputTypeConfiguration =
        (OriginDestinationOutputTypeConfiguration) taBuilder.activateOutput(OutputType.OD);
    originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
    originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.RUN_ID);

    // OUTPUT FORMAT CONFIGURATION

    // PlanItXMLOutputFormatter
    final PlanItOutputFormatter xmlOutputFormatter = (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(
        PlanItOutputFormatter.class.getCanonicalName());
    xmlOutputFormatter.setXmlNameRoot(description);
    xmlOutputFormatter.setCsvNameRoot(description);
    xmlOutputFormatter.setOutputDirectory(projectPath);
    taBuilder.registerOutputFormatter(xmlOutputFormatter);

    // MemoryOutputFormatter
    final MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project
        .createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());
    taBuilder.registerOutputFormatter(memoryOutputFormatter);

    // "USER" configuration
    if (maxIterations != null) {
      taBuilder.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
    }
    if (epsilon != null) {
      taBuilder.getGapFunction().getStopCriterion().setEpsilon(epsilon);
    }

    registerInitialCosts.accept(taBuilder, project, physicalNetwork);

    Map<Long, PlanItException> exceptionMap = project.executeAllTrafficAssignments();
    if (!exceptionMap.keySet().isEmpty()) {
      for (final long id : exceptionMap.keySet()) {
        throw exceptionMap.get(id);
      }
    }
    linkOutputTypeConfiguration.removeAllProperties();
    linkOutputTypeConfiguration.addProperty(OutputProperty.DENSITY);
    linkOutputTypeConfiguration.addProperty(OutputProperty.LINK_SEGMENT_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.MODE_EXTERNAL_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.UPSTREAM_NODE_EXTERNAL_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.UPSTREAM_NODE_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.UPSTREAM_NODE_LOCATION);
    linkOutputTypeConfiguration.addProperty(OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.DOWNSTREAM_NODE_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.DOWNSTREAM_NODE_LOCATION);
    linkOutputTypeConfiguration.addProperty(OutputProperty.FLOW);
    linkOutputTypeConfiguration.addProperty(OutputProperty.CAPACITY_PER_LANE);
    linkOutputTypeConfiguration.addProperty(OutputProperty.NUMBER_OF_LANES);
    linkOutputTypeConfiguration.addProperty(OutputProperty.LENGTH);
    linkOutputTypeConfiguration.addProperty(OutputProperty.CALCULATED_SPEED);
    linkOutputTypeConfiguration.addProperty(OutputProperty.LINK_COST);
    linkOutputTypeConfiguration.addProperty(OutputProperty.MODE_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.MODE_EXTERNAL_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.MAXIMUM_SPEED);
    exceptionMap = project.executeAllTrafficAssignments();
    if (!exceptionMap.keySet().isEmpty()) {
      for (final long id : exceptionMap.keySet()) {
        throw exceptionMap.get(id);
      }
    }
    TestOutputDto testOutputDto = new TestOutputDto(memoryOutputFormatter, project, planItInputBuilder);
    return testOutputDto;
  }

  /**
   * Run a test case and store the results in a MemoryOutputFormatter
   *
   * @param projectPath project directory containing the input files
   * @param setOutputTypeConfigurationProperties lambda function to set output properties being used
   * @param registerInitialCosts lambda function to register initial costs on the Traffic Assignment
   *          Builder
   * @param maxIterations the maximum number of iterations allowed in this test run
   * @param epsilon measure of how close successive iterations must be to each other to accept
   *          convergence
   * @param setCostParameters lambda function which sets parameters of cost function
   * @param description description used in temporary output file names
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
      final TriConsumer<TraditionalStaticAssignmentBuilder, CustomPlanItProject, PhysicalNetwork> registerInitialCosts,
      final Integer maxIterations, final Double epsilon,
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters,
      final String description) throws Exception {
    IdGenerator.reset();

    PlanItInputBuilder planItInputBuilder = new PlanItInputBuilder(projectPath);
    final CustomPlanItProject project = new CustomPlanItProject(planItInputBuilder);

    // RAW INPUT START --------------------------------
    final PhysicalNetwork physicalNetwork = project.createAndRegisterPhysicalNetwork(MacroscopicNetwork.class.getCanonicalName());
    final Zoning zoning = project.createAndRegisterZoning(physicalNetwork);
    final Demands demands = project.createAndRegisterDemands(zoning, physicalNetwork);
    // RAW INPUT END -----------------------------------

    // TRAFFIC ASSIGNMENT START------------------------
    final TraditionalStaticAssignmentBuilder taBuilder =
        (TraditionalStaticAssignmentBuilder) project.createAndRegisterTrafficAssignment(
            TraditionalStaticAssignment.class.getCanonicalName(), demands, zoning, physicalNetwork);

    // SUPPLY-DEMAND INTERACTIONS
    final BPRLinkTravelTimeCost bprLinkTravelTimeCost = (BPRLinkTravelTimeCost) taBuilder
        .createAndRegisterPhysicalCost(BPRLinkTravelTimeCost.class.getCanonicalName());
    if (setCostParameters != null) {
      setCostParameters.accept(physicalNetwork, bprLinkTravelTimeCost, planItInputBuilder);
    }

    taBuilder.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
    taBuilder.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());

    // DATA OUTPUT CONFIGURATION
    final OutputConfiguration outputConfiguration = taBuilder.getOutputConfiguration();
    // PlanItXML test cases use expect outputConfiguration.setPersistOnlyFinalIteration() to be set
    // to true - outputs will not match test data otherwise
    outputConfiguration.setPersistOnlyFinalIteration(true);

    // LINK OUTPUT CONFIGURATION
    final LinkOutputTypeConfiguration linkOutputTypeConfiguration = (LinkOutputTypeConfiguration) taBuilder
        .activateOutput(OutputType.LINK);
    if (setOutputTypeConfigurationProperties != null) {
      setOutputTypeConfigurationProperties.accept(linkOutputTypeConfiguration);
    }

    // OD OUTPUT CONFIGURATION
    final OriginDestinationOutputTypeConfiguration originDestinationOutputTypeConfiguration =
        (OriginDestinationOutputTypeConfiguration) taBuilder.activateOutput(OutputType.OD);
    originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_EXTERNAL_ID);
    originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.RUN_ID);

    // PATH OUTPUT CONFIGURATION
    final PathOutputTypeConfiguration pathOutputTypeConfiguration = (PathOutputTypeConfiguration) taBuilder
        .activateOutput(OutputType.PATH);
    pathOutputTypeConfiguration.setPathIdType(RouteIdType.NODE_EXTERNAL_ID);

    // OUTPUT FORMAT CONFIGURATION

    // PlanItXMLOutputFormatter
    final PlanItOutputFormatter xmlOutputFormatter = (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(
        PlanItOutputFormatter.class.getCanonicalName());
    xmlOutputFormatter.setXmlNameRoot(description);
    xmlOutputFormatter.setCsvNameRoot(description);
    xmlOutputFormatter.setOutputDirectory(projectPath);
    taBuilder.registerOutputFormatter(xmlOutputFormatter);

    // MemoryOutputFormatter
    final MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project
        .createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());
    taBuilder.registerOutputFormatter(memoryOutputFormatter);

    // "USER" configuration
    if (maxIterations != null) {
      taBuilder.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
    }
    if (epsilon != null) {
      taBuilder.getGapFunction().getStopCriterion().setEpsilon(epsilon);
    }

    registerInitialCosts.accept(taBuilder, project, physicalNetwork);

    final Map<Long, PlanItException> exceptionMap = project.executeAllTrafficAssignments();
    if (!exceptionMap.keySet().isEmpty()) {
      for (final long id : exceptionMap.keySet()) {
        throw exceptionMap.get(id);
      }
    }
    TestOutputDto testOutputDto = new TestOutputDto(memoryOutputFormatter, project,planItInputBuilder);
    return testOutputDto;
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
   * @param epsilon measure of how close successive iterations must be to each other to accept
   *          convergence
   * @param setCostParameters lambda function which sets parameters of cost function
   * @param description description used in temporary output file names
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   *
   *           If the setCostParameters argument is null, the system default values for the cost
   *           function parameters are used.
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final String initialCostsFileLocation1,
      final String initialCostsFileLocation2, final int initCostsFilePos, final Integer maxIterations,
      final Double epsilon,
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters, 
      final String description)
      throws Exception {

    final TriConsumer<TraditionalStaticAssignmentBuilder, CustomPlanItProject, PhysicalNetwork> registerInitialCosts = (
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
   *          type configuration output properties
   * @param initialCostsFileLocation1 location of first initial costs file
   * @param initialCostsFileLocation2 location of second initial costs file
   * @param initCostsFilePos identifies which initial costs file is to be used
   * @param maxIterations the maximum number of iterations allowed in this test run
   * @param epsilon measure of how close successive iterations must be to each other
   *          to accept convergence
   * @param setCostParameters lambda function which sets parameters of cost function
   * @param description description used in temporary output file names
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   *
   *           If the setCostParameters argument is null, the system
   *           default values for the cost function parameters are used.
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
      final String initialCostsFileLocation1, final String initialCostsFileLocation2, final int initCostsFilePos,
      final Integer maxIterations, final Double epsilon,
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters, 
      final String description) throws Exception {

    final TriConsumer<TraditionalStaticAssignmentBuilder, CustomPlanItProject, PhysicalNetwork> registerInitialCosts = (
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
   * of initial cost for each time period and default link output properties))
   *
   * @param projectPath project directory containing the input files
   * @param initialLinkSegmentLocationsPerTimePeriod Map of initial cost objects for each time
   *          period
   * @param epsilon measure of how close successive iterations must be to each other to accept
   *          convergence
   * @param setCostParameters lambda function which sets parameters of cost function
   * @param description description used in temporary output file names
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto setupAndExecuteAssignment(final String projectPath,
      final Map<Long, String> initialLinkSegmentLocationsPerTimePeriod, final Integer maxIterations,
      final Double epsilon,
      final TriConsumer<PhysicalNetwork, BPRLinkTravelTimeCost, InputBuilderListener> setCostParameters, 
      final String description)
      throws Exception {

    final TriConsumer<TraditionalStaticAssignmentBuilder, CustomPlanItProject, PhysicalNetwork> registerInitialCosts = (
        taBuilder, project, physicalNetwork) -> {
      for (final Long timePeriodId : initialLinkSegmentLocationsPerTimePeriod.keySet()) {
        final TimePeriod timePeriod = TimePeriod.getById(timePeriodId);
        final String initialCostsFileLocation = initialLinkSegmentLocationsPerTimePeriod.get(timePeriodId);
        final InitialLinkSegmentCost initialCost = 
            project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCostsFileLocation);
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
  public static void deleteFile(final String filename) throws Exception {
    final String rootPath = System.getProperty("user.dir");
    final Path path = FileSystems.getDefault().getPath(rootPath + "\\" + filename);
    Files.delete(path);
  }

  /**
   * Delete a file from the directory of test files
   *
   * @param projectPath path to the test directory
   * @param description description part of the file name
   * @param fileName other part of the file name
   * @throws Exception thrown if there is an error deleting the file
   */
  public static void deleteFile(final OutputType outputType, final String projectPath, final String description,
      final String fileName) throws Exception {
    deleteFile(projectPath + "\\" + outputType.value() + "_" + description + "_" + fileName);
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
  public static boolean compareFiles(final String file1, final String file2) throws IOException {
    final File f1 = new File(file1);
    final File f2 = new File(file2);
    final boolean result = FileUtils.contentEqualsIgnoreEOL(f1, f2, "utf-8");
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
   * @param xmlFileStandard location of the first XML file to be compared (expected results, created
   *          previously)
   * @param xmlFileBeingTested location of the second XML file to be compared (created by the
   *          current test case)
   * @return true if the test passes, false otherwise
   * @throws Exception thrown if the there is an error opening one of the files
   */
  public static boolean isXmlFileSameExceptForTimestamp(final String xmlFileStandard, final String xmlFileBeingTested)
      throws Exception {
    final XMLElementMetadata metadataStandard = (XMLElementMetadata) XmlUtils.generateObjectFromXml(
        XMLElementMetadata.class,
        xmlFileStandard);
    final XMLElementMetadata metadataBeingTested = (XMLElementMetadata) XmlUtils.generateObjectFromXml(
        XMLElementMetadata.class,
        xmlFileBeingTested);

    // compare <columns> and <column> elements in the generated output file against the standard
    // output file
    final List<XMLElementColumn> elementColumnsStandard = metadataStandard.getColumns().getColumn();
    final List<XMLElementColumn> elementColumnsBeingTested = metadataBeingTested.getColumns().getColumn();
    final int sizeElementColumnsStandard = elementColumnsStandard.size();
    final int sizeElementColumnsBeingTested = elementColumnsBeingTested.size();
    if (sizeElementColumnsStandard != sizeElementColumnsBeingTested) {
      return false;
    }
    for (int i = 0; i < sizeElementColumnsStandard; i++) {
      final XMLElementColumn elementColumnStandard = elementColumnsStandard.get(i);
      final XMLElementColumn elementColumnBeingTested = elementColumnsBeingTested.get(i);
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

    // compare <outputconfiguration> elements in the generated output file against the standard
    // output file
    final XMLElementOutputConfiguration outputConfigurationStandard = metadataStandard.getOutputconfiguration();
    final XMLElementOutputConfiguration outputConfigurationBeingTested = metadataBeingTested.getOutputconfiguration();
    if (!outputConfigurationStandard.getAssignment().equals(outputConfigurationBeingTested.getAssignment())) {
      return false;
    }
    if (!outputConfigurationStandard.getPhysicalcost().equals(outputConfigurationBeingTested.getPhysicalcost())) {
      return false;
    }
    if (!outputConfigurationStandard.getVirtualcost().equals(outputConfigurationBeingTested.getVirtualcost())) {
      return false;
    }
    final XMLElementOutputTimePeriod timeperiodStandard = outputConfigurationStandard.getTimeperiod();
    final XMLElementOutputTimePeriod timeperiodBeingTested = outputConfigurationBeingTested.getTimeperiod();
    if (!timeperiodStandard.getId().equals(timeperiodBeingTested.getId())) {
      return false;
    }
    if (!timeperiodStandard.getName().equals(timeperiodBeingTested.getName())) {
      return false;
    }

    // compare <simulation> elements in the generated output file against the standard output file
    final List<XMLElementIteration> iterationsStandard = metadataStandard.getSimulation().getIteration();
    final int iterationsSizeStandard = iterationsStandard.size();
    final List<XMLElementIteration> iterationsBeingTested = metadataBeingTested.getSimulation().getIteration();
    final int iterationsSizeBeingTested = iterationsBeingTested.size();
    if (iterationsSizeStandard != iterationsSizeBeingTested) {
      return false;
    }

    for (int i = 0; i < iterationsSizeStandard; i++) {
      final XMLElementIteration iterationStandard = iterationsStandard.get(i);
      final XMLElementIteration iterationBeingTested = iterationsBeingTested.get(i);
      if (iterationStandard.getNr().intValue() != iterationBeingTested.getNr().intValue()) {
        return false;
      }
      final List<XMLElementCsvdata> csvDataListStandard = iterationStandard.getCsvdata();
      final int sizeCsvDataListStandard = csvDataListStandard.size();
      final List<XMLElementCsvdata> csvDataListBeingTested = iterationBeingTested.getCsvdata();
      final int sizeCsvDataListBeingTested = csvDataListBeingTested.size();
      if (sizeCsvDataListStandard != sizeCsvDataListBeingTested) {
        return false;
      }
      for (int j = 0; j < sizeCsvDataListStandard; j++) {
        final XMLElementCsvdata csvDataStandard = csvDataListStandard.get(j);
        final XMLElementCsvdata csvDataBeingTested = csvDataListBeingTested.get(j);
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