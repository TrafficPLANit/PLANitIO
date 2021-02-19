package org.planit.io.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConstants;

import org.apache.commons.io.FileUtils;
import org.planit.assignment.TrafficAssignment;
import org.planit.assignment.traditionalstatic.TraditionalStaticAssignmentConfigurator;
import org.planit.cost.physical.BPRConfigurator;
import org.planit.cost.physical.AbstractPhysicalCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialLinkSegmentCostPeriod;
import org.planit.cost.virtual.FixedConnectoidTravelTimeCost;
import org.planit.cost.virtual.SpeedConnectoidTravelTimeCost;
import org.planit.demands.Demands;
import org.planit.xml.generated.XMLElementColumn;
import org.planit.xml.generated.XMLElementCsvdata;
import org.planit.xml.generated.XMLElementIteration;
import org.planit.xml.generated.XMLElementMetadata;
import org.planit.xml.generated.XMLElementOutputConfiguration;
import org.planit.xml.generated.XMLElementOutputTimePeriod;
import org.planit.zoning.Zoning;
import org.planit.input.InputBuilderListener;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.io.output.formatter.PlanItOutputFormatter;
import org.planit.io.xml.util.JAXBUtils;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.macroscopic.MacroscopicNetwork;
import org.planit.output.configuration.LinkOutputTypeConfiguration;
import org.planit.output.configuration.ODOutputTypeConfiguration;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.PathOutputTypeConfiguration;
import org.planit.output.enums.ODSkimSubOutputType;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.PathOutputIdentificationType;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.formatter.MemoryOutputIterator;
import org.planit.output.formatter.OutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.project.CustomPlanItProject;
import org.planit.sdinteraction.smoothing.MSASmoothing;
import org.planit.time.TimePeriod;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.functionalinterface.QuadConsumer;
import org.planit.utils.functionalinterface.TriConsumer;
import org.planit.utils.functionalinterface.TriFunction;
import org.planit.utils.misc.Pair;
import org.planit.utils.mode.Mode;
import org.planit.utils.test.LinkSegmentExpectedResultsDto;
import org.planit.utils.test.TestOutputDto;

/**
 * Helper class used by unit tests
 *
 * @author gman6028
 *
 */
public class PlanItIOTestHelper {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanItIOTestHelper.class.getCanonicalName());

  private static final double epsilon = 0.00001;

  private static Consumer<LinkOutputTypeConfiguration> defaultSetOutputTypeConfigurationProperties = (
      linkOutputTypeConfiguration) -> {
    try {
      linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_XML_ID);
      linkOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_ID);
      linkOutputTypeConfiguration.removeProperty(OutputProperty.MAXIMUM_SPEED);
    } catch (final PlanItException e) {
      LOGGER.severe(e.getMessage());
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
   * @param iterationIndex the current iteration index
   * @param resultsMap Map containing the standard results for each time period and mode
   * @param getPositionKeys lambda function which generates the position of the key(s) in the key array
   * @param getResultDto lambda function which generates the known result for each iteration
   * @return true if all the tests have passed, false otherwise
   * @throws PlanItException thrown if there is an error
   */
  private static boolean compareLinkResultsToMemoryOutputFormatter(
      final MemoryOutputFormatter memoryOutputFormatter, final Integer iterationIndex,
      final SortedMap<TimePeriod, ? extends SortedMap<Mode, ? extends Object>> resultsMap,
      TriFunction<Mode, TimePeriod, Integer, Object> getPositionKeys,
      TriFunction<Pair<Integer, Integer>, Object, Object[], LinkSegmentExpectedResultsDto> getResultDto) throws PlanItException {
    boolean pass = true;
    final int iteration = (iterationIndex == null) ? memoryOutputFormatter.getLastIteration() : iterationIndex;
    for (final TimePeriod timePeriod : resultsMap.keySet()) {
      for (final Mode mode : resultsMap.get(timePeriod).keySet()) {
        Object innerMap = resultsMap.get(timePeriod).get(mode);

        final int flowPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputProperty.FLOW);
        final int costPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputProperty.LINK_COST);
        final int lengthPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputProperty.LENGTH);
        final int speedPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputProperty.CALCULATED_SPEED);
        final int capacityPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputProperty.CAPACITY_PER_LANE);
        final int numberOfLanesPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputProperty.NUMBER_OF_LANES);
        final MemoryOutputIterator memoryOutputIterator = memoryOutputFormatter.getIterator(mode, timePeriod,
            iteration, OutputType.LINK);
        Object obj = getPositionKeys.apply(mode, timePeriod, iteration);
        if (obj instanceof PlanItException) {
          PlanItException pe = (PlanItException) obj;
          throw pe;
        }
       
        @SuppressWarnings("unchecked") Pair<Integer, Integer> positionKeys = (Pair<Integer, Integer>) obj;
        while (memoryOutputIterator.hasNext()) {
          memoryOutputIterator.next();
          final Object[] keys = memoryOutputIterator.getKeys();
          LinkSegmentExpectedResultsDto resultDto = getResultDto.apply(positionKeys, innerMap, keys);
          final Object[] results = memoryOutputIterator.getValues();
          final double flow = (Double) results[flowPosition];
          final double cost = (Double) results[costPosition];
          final double length = (Double) results[lengthPosition];
          final double speed = (Double) results[speedPosition];
          final double capacityPerLane = (Double) results[capacityPosition];
          final int numberOfLanes = (Integer) results[numberOfLanesPosition];
          
          assertEquals(resultDto.getLinkFlow(), flow , epsilon);
          pass = pass && (Math.abs(flow - resultDto.getLinkFlow()) < epsilon);
          assertEquals(resultDto.getLength(), length , epsilon);
          pass = pass && (Math.abs(speed - resultDto.getSpeed()) < epsilon);
          assertEquals(resultDto.getSpeed(), speed , epsilon);
          pass = pass && (Math.abs(cost - resultDto.getLinkCost()) < epsilon);
          assertEquals(resultDto.getLinkCost(), cost , epsilon);
          pass = pass && (Math.abs(numberOfLanes *  capacityPerLane - resultDto.getCapacity()) < epsilon);
          assertEquals(numberOfLanes * capacityPerLane, resultDto.getCapacity(), epsilon);
        }
      }
    }
    return pass;
  }
  
  /**
   * Compares the Path or Origin-Destination values stored in the MemoryOutputFormatter with the expected results
   * 
   * @param memoryOutputFormatter the MemoryOuptutFormatter object which stores
   *          results from a test run
   * @param iterationIndex the current iteration index
   * @param map Map of expected paths by time period, mode, origin zone external Id and destination zone external Id
   * @param outputType the OutputType of the results being checked (Path or OD)
   * @return true if all the tests pass, false otherwise
   * @throws PlanItException thrown if one of the test output properties has not
   *           been saved
   */
  private static boolean compareResultsToMemoryOutputFormatter(
      final MemoryOutputFormatter memoryOutputFormatter, final Integer iterationIndex, final Map<TimePeriod,?> map, OutputType outputType) throws PlanItException {
    boolean pass = true;
    int iteration = (iterationIndex == null) ? memoryOutputFormatter.getLastIteration() : iterationIndex;
    OutputProperty outputProperty = OutputProperty.PATH_STRING;
    if (outputType.equals(OutputType.OD)) {
      iteration--;
      outputProperty = OutputProperty.OD_COST;
    }
    for (TimePeriod timePeriod : map.keySet()) {
      @SuppressWarnings("unchecked") Map<Mode, Map<String, Map<String, ?>>> mapPerTimePeriod = (Map<Mode, Map<String, Map<String, ?>>>) map.get(timePeriod);
      for (Mode mode : mapPerTimePeriod.keySet()) {
        Map<String, Map<String, ?>> mapPerTimePeriodAndMode = mapPerTimePeriod.get(mode);
        final int position = memoryOutputFormatter.getPositionOfOutputValueProperty(outputType, outputProperty);
        final int originZonePosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(outputType, OutputProperty.ORIGIN_ZONE_XML_ID);
        final int destinationZonePosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(outputType, OutputProperty.DESTINATION_ZONE_XML_ID);
        final MemoryOutputIterator memoryOutputIterator = memoryOutputFormatter.getIterator(mode, timePeriod, iteration, outputType);       
        while (memoryOutputIterator.hasNext()) {
          memoryOutputIterator.next();
          final Object[] keys = memoryOutputIterator.getKeys();
          final Object[] results = memoryOutputIterator.getValues();
          String originZoneXmlId = (String) keys[originZonePosition];
          String destinationZoneXmlId = (String) keys[destinationZonePosition];
          if (outputType.equals(OutputType.OD)) {
            Double expectedCost = (Double) mapPerTimePeriodAndMode.get(originZoneXmlId).get(destinationZoneXmlId);
            Double costFromMemoryOutputFormatter = (Double) results[position];
            assertEquals(expectedCost, costFromMemoryOutputFormatter, epsilon);
            pass = pass && (Math.abs(expectedCost - costFromMemoryOutputFormatter) < epsilon);
          } else {
            String expectedPath = (String) mapPerTimePeriodAndMode.get(originZoneXmlId).get(destinationZoneXmlId);
            String pathFromMemoryOutputFormatter = (String) results[position];
            assertEquals(expectedPath, pathFromMemoryOutputFormatter);
            pass = pass && (expectedPath.equals(pathFromMemoryOutputFormatter));
          }
       }
      }
    
    } 
    return pass;
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
   * @return true if all the tests pass, false otherwise
   * @throws PlanItException thrown if one of the test output properties has not
   *           been saved
   */
  public static boolean compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(
      final MemoryOutputFormatter memoryOutputFormatter, final Integer iterationIndex,
      final SortedMap<TimePeriod, SortedMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>> resultsMap)
      throws PlanItException {
    return compareLinkResultsToMemoryOutputFormatter(memoryOutputFormatter, iterationIndex, resultsMap,
        (mode, timePeriod, iteration) -> {
          try {
          final int downstreamNodeXmlIdPosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(OutputType.LINK, OutputProperty.DOWNSTREAM_NODE_XML_ID);
          final int upstreamNodeXmlIdPosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(OutputType.LINK, OutputProperty.UPSTREAM_NODE_XML_ID);
          return Pair.create(downstreamNodeXmlIdPosition, upstreamNodeXmlIdPosition);
          } catch (PlanItException e) {
            return e;
          }
        },
        (positionKeys, innerObj, keys) -> {
          @SuppressWarnings("unchecked") final SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>> innerMap =
              (SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>) innerObj;
          final int downstreamNodeXmlIdPosition = positionKeys.first();
          final int upstreamNodeXmlIdPosition = positionKeys.second();
          final String upstreamNodeXmlId = (String) keys[downstreamNodeXmlIdPosition];
          final String  downstreamNodeXmlId = (String) keys[upstreamNodeXmlIdPosition];
          return innerMap.get(upstreamNodeXmlId).get(downstreamNodeXmlId);
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
   * @return true if all the tests pass, false otherwise
   * @throws PlanItException thrown if one of the test output properties has not
   *           been saved
   */
  public static boolean compareLinkResultsToMemoryOutputFormatterUsingLinkSegmentId(
      final MemoryOutputFormatter memoryOutputFormatter, final Integer iterationIndex,
      final SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, LinkSegmentExpectedResultsDto>>> resultsMap)
      throws PlanItException {
    return compareLinkResultsToMemoryOutputFormatter(memoryOutputFormatter, iterationIndex, resultsMap,
        (mode, timePeriod, iteration) -> {
          try {
            final int linkSegmentIdPosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(OutputType.LINK, OutputProperty.LINK_SEGMENT_ID);
         return Pair.create(linkSegmentIdPosition, 0);
          } catch (PlanItException e) {
            return e;
          }
        },
        (positionKeys, innerObj, keys) -> {
          @SuppressWarnings("unchecked") final SortedMap<Long, LinkSegmentExpectedResultsDto> innerMap = (SortedMap<Long, LinkSegmentExpectedResultsDto>) innerObj;
          final int linkSegmentIdPosition = positionKeys.first();
          final long linkSegmentId = (Long) keys[linkSegmentIdPosition];
          return innerMap.get(linkSegmentId);
        });
  }

  /**
   * Compares the Path values stored in the MemoryOutputFormatter with the expected results
   * 
   * @param memoryOutputFormatter the MemoryOuptutFormatter object which stores
   *          results from a test run
   * @param iterationIndex the current iteration index
   * @param pathMap Map of expected paths by time period, mode, origin zone xml Id and destination zone xml Id
   * @return true if all the tests pass, false otherwise
   * @throws PlanItException thrown if one of the test output properties has not
   *           been saved
   */
  public static boolean comparePathResultsToMemoryOutputFormatter(
      final MemoryOutputFormatter memoryOutputFormatter, final Integer iterationIndex, final Map<TimePeriod, Map<Mode, Map<String, Map<String, String>>>> pathMap) throws PlanItException {
    return compareResultsToMemoryOutputFormatter(memoryOutputFormatter, iterationIndex, pathMap, OutputType.PATH);

  }
  
  /**
   * Compares the Origin-Destination cost values stored in the MemoryOutputFormatter with the expected results
   * 
   * @param memoryOutputFormatter the MemoryOuptutFormatter object which stores results from a test run
   * @param iterationIndex the current iteration index
   * @param odMap Map of expected OD costs by time period, mode, origin zone Xml Id and destination zone Xml Id
   * @return true if all the tests pass, false otherwise
   * @throws PlanItException thrown if one of the test output properties has not
   *           been saved
   */
  public static boolean compareOriginDestinationResultsToMemoryOutputFormatter(
      final MemoryOutputFormatter memoryOutputFormatter, final Integer iterationIndex, final Map<TimePeriod, Map<Mode, Map<String, Map<String, Double>>>> odMap) throws PlanItException {
    return compareResultsToMemoryOutputFormatter(memoryOutputFormatter, iterationIndex, odMap, OutputType.OD);
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
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties, final Integer maxIterations,
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters, 
      final String description,
      final boolean useFixedConnectoidTravelTimeCost,
      final boolean recordZeroFlow)
      throws Exception {
    
    return setupAndExecuteAssignment(
        projectPath, setOutputTypeConfigurationProperties, null, null, 0, maxIterations, null, setCostParameters, description, useFixedConnectoidTravelTimeCost, recordZeroFlow);
  }

  /**
   * Run a test case and store the results in a MemoryOutputFormatter (uses
   * maximum number of iterations)
   *
   * @param projectPath project directory containing the input files
   * @param maxIterations the maximum number of iterations allowed in this test run
   * @param setCostParameters lambda function which sets parameters of cost function
   * @param description description used in temporary output file names
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final Integer maxIterations,
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters,
      final String description,
      final boolean useFixedConnectoidTravelTimeCost,
      final boolean recordZeroFlow)
      throws Exception {
    
    return setupAndExecuteAssignment(
        projectPath, null, null, 0, maxIterations, null, setCostParameters, description, useFixedConnectoidTravelTimeCost, recordZeroFlow);
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
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
      final String initialCostsFileLocation,
      final Integer maxIterations, 
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters, 
      final String description,
      final boolean useFixedConnectoidTravelTimeCost,
      final boolean recordZeroFlow) throws Exception {
    
    return setupAndExecuteAssignment(
        projectPath, setOutputTypeConfigurationProperties, initialCostsFileLocation, null, 0, maxIterations, null, setCostParameters, description, useFixedConnectoidTravelTimeCost, recordZeroFlow);
  }

  /**
   * Run a test case and store the results in a MemoryOutputFormatter (requires
   * assignment to converge, no maximum number of iterations)
   *
   * @param projectPath project directory containing the input files
   * @param initialCostsFileLocation location of initial costs file
   * @param setCostParameters lambda function which sets parameters of cost function
   * @param description description used in temporary output file names
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final String initialCostsFileLocation,
      final Integer maxIterations, 
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters, 
      final String description,
      final boolean useFixedConnectoidTravelTimeCost,
      final boolean recordZeroFlow) throws Exception {
    
    return setupAndExecuteAssignment(
        projectPath, initialCostsFileLocation, null, 0, maxIterations, null, setCostParameters, description, useFixedConnectoidTravelTimeCost, recordZeroFlow);
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
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
      final String initialCostsFileLocation1, final String initialCostsFileLocation2, final int initCostsFilePos,
      final Integer maxIterations, 
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters, 
      final String description,
      final boolean useFixedConnectoidTravelTimeCost,
      final boolean recordZeroFlow) throws Exception {
    
    return setupAndExecuteAssignment(
        projectPath, 
        setOutputTypeConfigurationProperties, 
        initialCostsFileLocation1,
        initialCostsFileLocation2, 
        initCostsFilePos, 
        maxIterations, 
        null, 
        setCostParameters, 
        description, 
        useFixedConnectoidTravelTimeCost, 
        recordZeroFlow);
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
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final String initialCostsFileLocation1,
      final String initialCostsFileLocation2, final int initCostsFilePos, final Integer maxIterations,
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters, 
      final String description,
      final boolean useFixedConnectoidTravelTimeCost,
      final boolean recordZeroFlow)
      throws Exception {
    
    return setupAndExecuteAssignment(
        projectPath, 
        initialCostsFileLocation1, 
        initialCostsFileLocation2,
        initCostsFilePos, 
        maxIterations, 
        null, 
        setCostParameters, 
        description, 
        useFixedConnectoidTravelTimeCost, 
        recordZeroFlow);
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
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties, final Integer maxIterations,
      final Double epsilon, 
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters, 
      final String description,
      final boolean useFixedConnectoidTravelTimeCost,
      final boolean recordZeroFlow)
      throws Exception {
    return setupAndExecuteAssignment(projectPath, setOutputTypeConfigurationProperties, null, null, 0,
        maxIterations, epsilon, setCostParameters, description, useFixedConnectoidTravelTimeCost, recordZeroFlow);
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
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final Integer maxIterations,
      final Double epsilon, 
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters, 
      final String description,
      final boolean useFixedConnectoidTravelTimeCost,
      final boolean recordZeroFlow)
      throws Exception {
    
    return setupAndExecuteAssignment(
        projectPath, 
        null, 
        null, 
        0, 
        maxIterations, 
        epsilon, 
        setCostParameters,
        description, 
        useFixedConnectoidTravelTimeCost, 
        recordZeroFlow);
  }

  /**
   * Run a test case and store the results in a MemoryOutputFormatter (requires
   * assignment to converge, no maximum number of iterations)
   *
   * @param projectPath project directory containing the input files
   * @param setOutputTypeConfigurationProperties lambda function to set output properties being used
   * @param setCostParameters lambda function which sets parameters of cost function
   * @param description description used in temporary output file names
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters, 
      final String description,
      final boolean useFixedConnectoidTravelTimeCost,
      final boolean recordZeroFlow)
      throws Exception {
    
    return setupAndExecuteAssignment(
        projectPath, 
        setOutputTypeConfigurationProperties, 
        null, 
        null, 
        0, 
        null, 
        null,
        setCostParameters, description, useFixedConnectoidTravelTimeCost, recordZeroFlow);
  }

  /**
   * Run a test case and store the results in a MemoryOutputFormatter (requires
   * assignment to converge, no maximum number of iterations)
   *
   * @param projectPath project directory containing the input files
   * @param setCostParameters lambda function which sets parameters of cost function
   * @param description description used in temporary output file names
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters, 
      final String description,
      final boolean useFixedConnectoidTravelTimeCost,
      final boolean recordZeroFlow)
      throws Exception {
    
    return setupAndExecuteAssignment(
        projectPath, 
        null, 
        null, 
        0, 
        null, 
        null, 
        setCostParameters, 
        description, 
        useFixedConnectoidTravelTimeCost, 
        recordZeroFlow);
  }

 /**
  * Runs a test case which attempts to change a locked formatter
  * 
   * @param projectPath project directory containing the input files
   * @param setCostParameters lambda function which sets parameters of cost function
   * @param description description used in temporary output file names
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
  */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignmentAttemptToChangeLockedFormatter(
      final String projectPath,
      final BiConsumer<InfrastructureNetwork<?,?>, BPRConfigurator> setCostParameters, 
      final String description,
      final boolean useFixedConnectoidTravelTimeCost)
      throws Exception {
    
    return setupAndExecuteAssignmentAttemptToChangeLockedFormatter(
        projectPath, 
        null, 
        null, 
        0, 
        null, 
        null,
        setCostParameters, 
        description, 
        useFixedConnectoidTravelTimeCost);
  }

  /**
   * Runs a test case which attempts to change a locked formatter
   * 
   * @param projectPath project directory containing the input files
   * @param initialCostsFileLocation1
   * @param initialCostsFileLocation2
   * @param initCostsFilePos
   * @param maxIterations
   * @param epsilon
   * @param setCostParameters lambda function which sets parameters of cost function
   * @param description description used in temporary output file names
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignmentAttemptToChangeLockedFormatter(final String projectPath, 
      final String initialCostsFileLocation1, final String initialCostsFileLocation2, final int initCostsFilePos, 
      final Integer maxIterations,
      final Double epsilon,
      final BiConsumer<InfrastructureNetwork<?,?>, BPRConfigurator> setCostParameters, final String description,
      final boolean useFixedConnectoidTravelTimeCost)
      throws Exception {

    final TriConsumer<TraditionalStaticAssignmentConfigurator, CustomPlanItProject, InfrastructureNetwork<?,?>> registerInitialCosts = 
        (taConfigurator, project, network) -> {
          InitialLinkSegmentCost initialCost = null;
          if (initialCostsFileLocation1 != null) {
            if (initialCostsFileLocation2 != null) {
              if (initCostsFilePos == 0) {
                initialCost = project.createAndRegisterInitialLinkSegmentCost(network, initialCostsFileLocation1);
              } else {
                initialCost = project.createAndRegisterInitialLinkSegmentCost(network, initialCostsFileLocation2);
              }
            } else {
              initialCost = project.createAndRegisterInitialLinkSegmentCost(network, initialCostsFileLocation1);
            }
            taConfigurator.registerInitialLinkSegmentCost(initialCost);
          }
        };

    return setupAndExecuteAssignmentAttemptToChangeLockedFormatter(
        projectPath,
        defaultSetOutputTypeConfigurationProperties, 
        registerInitialCosts, 
        maxIterations, 
        epsilon, 
        setCostParameters,
        description, 
        useFixedConnectoidTravelTimeCost);
  }

  /**
   * Runs a test case which attempts to change a locked formatter
   * 
   * @param projectPath project directory containing the input files
   * @param setOutputTypeConfigurationProperties
   * @param registerInitialCosts
   * @param maxIterations
   * @param epsilon
   * @param setCostParameters lambda function which sets parameters of cost function
   * @param description description used in temporary output file names
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignmentAttemptToChangeLockedFormatter(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
      final TriConsumer<TraditionalStaticAssignmentConfigurator, CustomPlanItProject, InfrastructureNetwork<?,?>> registerInitialCosts,
      final Integer maxIterations, final Double epsilon,
      final BiConsumer<InfrastructureNetwork<?,?>, BPRConfigurator> setCostParameters,
      final String description,
      final boolean useFixedConnectoidTravelTimeCost) throws Exception {

    final PlanItInputBuilder planItInputBuilder = new PlanItInputBuilder(projectPath);
    final CustomPlanItProject project = new CustomPlanItProject(planItInputBuilder);

    // RAW INPUT START --------------------------------
    final MacroscopicNetwork physicalNetwork = (MacroscopicNetwork) project.createAndRegisterInfrastructureNetwork(MacroscopicNetwork.class.getCanonicalName());
    final Zoning zoning = project.createAndRegisterZoning(physicalNetwork);
    final Demands demands = project.createAndRegisterDemands(zoning, physicalNetwork);
    // RAW INPUT END -----------------------------------

    // TRAFFIC ASSIGNMENT START------------------------
    final TraditionalStaticAssignmentConfigurator taConfigurator =
        (TraditionalStaticAssignmentConfigurator) project.createAndRegisterTrafficAssignment(
            TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT, demands, zoning, physicalNetwork);

    // SUPPLY-DEMAND INTERACTIONS
    if (setCostParameters != null) {
      setCostParameters.accept(physicalNetwork, (BPRConfigurator) taConfigurator.getPhysicalCost());
    }

    if (useFixedConnectoidTravelTimeCost) {
      taConfigurator.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
    } else {
      taConfigurator.createAndRegisterVirtualCost(SpeedConnectoidTravelTimeCost.class.getCanonicalName());
    }
    taConfigurator.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());

    // DATA OUTPUT CONFIGURATION
    // PlanItXML test cases use expect outputConfiguration.setPersistOnlyFinalIteration() to be set
    // to true - outputs will not match test data otherwise
    final OutputConfiguration outputConfiguration = taConfigurator.getOutputConfiguration();
    outputConfiguration.setPersistOnlyFinalIteration(true);

    // LINK OUTPUT
    final LinkOutputTypeConfiguration linkOutputTypeConfiguration = 
        (LinkOutputTypeConfiguration) taConfigurator.activateOutput(OutputType.LINK);
    if (setOutputTypeConfigurationProperties != null) {
      setOutputTypeConfigurationProperties.accept(linkOutputTypeConfiguration);
    }

    // OD OUTPUT
    final ODOutputTypeConfiguration originDestinationOutputTypeConfiguration =
        (ODOutputTypeConfiguration) taConfigurator.activateOutput(OutputType.OD);
    originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_XML_ID);
    originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.RUN_ID);

    // OUTPUT FORMAT CONFIGURATION

    // PlanItXMLOutputFormatter
    final PlanItOutputFormatter xmlOutputFormatter = 
        (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER);
    xmlOutputFormatter.setXmlNameRoot(description);
    xmlOutputFormatter.setCsvNameRoot(description);
    xmlOutputFormatter.setOutputDirectory(projectPath);
    taConfigurator.registerOutputFormatter(xmlOutputFormatter);

    // MemoryOutputFormatter
    final MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project
        .createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());
    taConfigurator.registerOutputFormatter(memoryOutputFormatter);

    // "USER" configuration
    if (maxIterations != null) {
      taConfigurator.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
    }
    if (epsilon != null) {
      taConfigurator.getGapFunction().getStopCriterion().setEpsilon(epsilon);
    }

    registerInitialCosts.accept(taConfigurator, project, physicalNetwork);

    project.executeAllTrafficAssignments();
    linkOutputTypeConfiguration.removeAllProperties();
    linkOutputTypeConfiguration.addProperty(OutputProperty.LINK_SEGMENT_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.MODE_XML_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.UPSTREAM_NODE_XML_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.UPSTREAM_NODE_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.UPSTREAM_NODE_LOCATION);
    linkOutputTypeConfiguration.addProperty(OutputProperty.DOWNSTREAM_NODE_XML_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.DOWNSTREAM_NODE_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.DOWNSTREAM_NODE_LOCATION);
    linkOutputTypeConfiguration.addProperty(OutputProperty.FLOW);
    linkOutputTypeConfiguration.addProperty(OutputProperty.CAPACITY_PER_LANE);
    linkOutputTypeConfiguration.addProperty(OutputProperty.NUMBER_OF_LANES);
    linkOutputTypeConfiguration.addProperty(OutputProperty.LENGTH);
    linkOutputTypeConfiguration.addProperty(OutputProperty.CALCULATED_SPEED);
    linkOutputTypeConfiguration.addProperty(OutputProperty.LINK_COST);
    linkOutputTypeConfiguration.addProperty(OutputProperty.MODE_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.MODE_XML_ID);
    linkOutputTypeConfiguration.addProperty(OutputProperty.MAXIMUM_SPEED);
    project.executeAllTrafficAssignments();
    TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDto = new TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener>(memoryOutputFormatter, project, planItInputBuilder);
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
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
      final QuadConsumer<Demands, TraditionalStaticAssignmentConfigurator, CustomPlanItProject, InfrastructureNetwork<?,?>> registerInitialCosts,
      final Integer maxIterations, final Double epsilon,
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters,
      final String description,
      boolean useFixedConnectoidTravelTimeCost,
      boolean recordZeroFlow) throws Exception {

    final PlanItInputBuilder planItInputBuilder = new PlanItInputBuilder(projectPath);
    final CustomPlanItProject project = new CustomPlanItProject(planItInputBuilder);

    // RAW INPUT START --------------------------------
    final MacroscopicNetwork network = (MacroscopicNetwork) project.createAndRegisterInfrastructureNetwork(MacroscopicNetwork.class.getCanonicalName());
    final Zoning zoning = project.createAndRegisterZoning(network);
    final Demands demands = project.createAndRegisterDemands(zoning, network);
    
    // RAW INPUT END -----------------------------------

    // TRAFFIC ASSIGNMENT START------------------------
    final TraditionalStaticAssignmentConfigurator taConfigurator =
        (TraditionalStaticAssignmentConfigurator) project.createAndRegisterTrafficAssignment(
            TrafficAssignment.TRADITIONAL_STATIC_ASSIGNMENT, demands, zoning, network);
    
    //READ INITIAL COSTS
    registerInitialCosts.accept(demands, taConfigurator, project, network);
    
    // SUPPLY-DEMAND INTERACTIONS
    final BPRConfigurator bpr = (BPRConfigurator) taConfigurator.createAndRegisterPhysicalCost(AbstractPhysicalCost.BPR);
    
    if (setCostParameters != null) {
      setCostParameters.accept(network, bpr, planItInputBuilder);
    }

    if (useFixedConnectoidTravelTimeCost) {
      taConfigurator.createAndRegisterVirtualCost(FixedConnectoidTravelTimeCost.class.getCanonicalName());
    } else {
      taConfigurator.createAndRegisterVirtualCost(SpeedConnectoidTravelTimeCost.class.getCanonicalName());
    }
    taConfigurator.createAndRegisterSmoothing(MSASmoothing.class.getCanonicalName());

    // DATA OUTPUT CONFIGURATION
    OutputConfiguration outputConfiguration = taConfigurator.getOutputConfiguration();
    // PlanItXML test cases use expect outputConfiguration.setPersistOnlyFinalIteration() to be set
    // to true - outputs will not match test data otherwise
    outputConfiguration.setPersistOnlyFinalIteration(true);
    outputConfiguration.setPersistZeroFlow(recordZeroFlow);

    // LINK OUTPUT CONFIGURATION
    final LinkOutputTypeConfiguration linkOutputTypeConfiguration = 
        (LinkOutputTypeConfiguration) taConfigurator.activateOutput(OutputType.LINK);
    setOutputTypeConfigurationProperties.accept(linkOutputTypeConfiguration);

    // OD OUTPUT CONFIGURATION
    final ODOutputTypeConfiguration originDestinationOutputTypeConfiguration =
        (ODOutputTypeConfiguration) taConfigurator.activateOutput(OutputType.OD);
    originDestinationOutputTypeConfiguration.deactivateOdSkimOutputType(ODSkimSubOutputType.NONE);
    originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.TIME_PERIOD_XML_ID);
    originDestinationOutputTypeConfiguration.removeProperty(OutputProperty.RUN_ID);

    // PATH OUTPUT CONFIGURATION
    final PathOutputTypeConfiguration pathOutputTypeConfiguration = 
        (PathOutputTypeConfiguration) taConfigurator.activateOutput(OutputType.PATH);
    pathOutputTypeConfiguration.setPathIdentificationType(PathOutputIdentificationType.NODE_XML_ID);

    
    // OUTPUT FORMAT CONFIGURATION

    // PlanItOutputFormatter
    final PlanItOutputFormatter xmlOutputFormatter = 
        (PlanItOutputFormatter) project.createAndRegisterOutputFormatter(OutputFormatter.PLANIT_OUTPUT_FORMATTER);
    xmlOutputFormatter.setXmlNameRoot(description);
    xmlOutputFormatter.setCsvNameRoot(description);
    xmlOutputFormatter.setOutputDirectory(projectPath);
    
    //Illustration of how to set <description> element in output XML file below
    //xmlOutputFormatter.setDescription("Some other description");
    taConfigurator.registerOutputFormatter(xmlOutputFormatter);

    // MemoryOutputFormatter
    final MemoryOutputFormatter memoryOutputFormatter = (MemoryOutputFormatter) project
        .createAndRegisterOutputFormatter(MemoryOutputFormatter.class.getCanonicalName());
    taConfigurator.registerOutputFormatter(memoryOutputFormatter);

    // "USER" configuration
    if (maxIterations != null) {
      taConfigurator.getGapFunction().getStopCriterion().setMaxIterations(maxIterations);
    }
    if (epsilon != null) {
      taConfigurator.getGapFunction().getStopCriterion().setEpsilon(epsilon);
    }

    project.executeAllTrafficAssignments();
    TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> testOutputDtoX = new TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener>(memoryOutputFormatter, project, planItInputBuilder);
    return testOutputDtoX;
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
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   *
   *           If the setCostParameters argument is null, the system default values for the cost
   *           function parameters are used.
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final String initialCostsFileLocation1, final String initialCostsFileLocation2, final int initCostsFilePos, 
      final Integer maxIterations,
      final Double epsilon,
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters, 
      final String description,
      boolean useFixedConnectoidTravelTimeCost,
      boolean recordZeroFlow)
      throws Exception {

    final QuadConsumer<Demands, TraditionalStaticAssignmentConfigurator, CustomPlanItProject, InfrastructureNetwork<?,?>> registerInitialCosts = 
        (demands, taBuilder, project, network) -> {
          InitialLinkSegmentCost initialCost = null;
          if (initialCostsFileLocation1 != null) {
            if (initialCostsFileLocation2 != null) {
              if (initCostsFilePos == 0) {
                initialCost = project.createAndRegisterInitialLinkSegmentCost(network, initialCostsFileLocation1);
              } else {
                initialCost = project.createAndRegisterInitialLinkSegmentCost(network, initialCostsFileLocation2);
              }
            } else {
              initialCost = project.createAndRegisterInitialLinkSegmentCost(network, initialCostsFileLocation1);
            }
            taBuilder.registerInitialLinkSegmentCost(initialCost);
          }
        };

    return setupAndExecuteAssignment(projectPath, defaultSetOutputTypeConfigurationProperties, registerInitialCosts,
        maxIterations, epsilon, setCostParameters, description, useFixedConnectoidTravelTimeCost, recordZeroFlow);
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
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   *
   *           If the setCostParameters argument is null, the system
   *           default values for the cost function parameters are used.
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final Consumer<LinkOutputTypeConfiguration> setOutputTypeConfigurationProperties,
      final String initialCostsFileLocation1, final String initialCostsFileLocation2, final int initCostsFilePos,
      final Integer maxIterations, final Double epsilon,
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters, 
      final String description,
      boolean useFixedConnectoidTravelTimeCost,
      boolean recordZeroFlow) throws Exception {

    final QuadConsumer<Demands, TraditionalStaticAssignmentConfigurator, CustomPlanItProject, InfrastructureNetwork<?,?>> registerInitialCosts = 
        (demands, taConfigurator, project, physicalNetwork) -> {
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
            taConfigurator.registerInitialLinkSegmentCost(initialCost);
          }
        };

    return setupAndExecuteAssignment(projectPath, setOutputTypeConfigurationProperties, registerInitialCosts,
        maxIterations, epsilon, setCostParameters, description, useFixedConnectoidTravelTimeCost, recordZeroFlow);
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
   * @param useFixedConnectoidTravelTimeCost if true use FixedVirtualCost, otherwise use SpeedConnectoidTravelTimeCost
   * @param recordZeroFlow if true, paths, OD costs and links with zero cost are included in the output
   * @return TestOutputDto containing results, builder and project from the run
   * @throws Exception thrown if there is an error
   */
  public static TestOutputDto<MemoryOutputFormatter, CustomPlanItProject, InputBuilderListener> setupAndExecuteAssignment(final String projectPath,
      final Map<Long, String> initialLinkSegmentLocationsPerTimePeriod, final Integer maxIterations,
      final Double epsilon,
      final TriConsumer<InfrastructureNetwork<?,?>, BPRConfigurator, InputBuilderListener> setCostParameters, 
      final String description,
      boolean useFixedConnectoidTravelTimeCost,
      boolean recordZeroFlow)
      throws Exception {

    final QuadConsumer<Demands, TraditionalStaticAssignmentConfigurator, CustomPlanItProject, InfrastructureNetwork<?,?>> registerInitialCosts = (
        demands, taConfigurator, project, physicalNetwork) -> {
      for (final Long timePeriodId : initialLinkSegmentLocationsPerTimePeriod.keySet()) {
        final TimePeriod timePeriod = demands.timePeriods.getTimePeriodById(timePeriodId);
        final String initialCostsFileLocation = initialLinkSegmentLocationsPerTimePeriod.get(timePeriodId);
        final InitialLinkSegmentCostPeriod initialCost = project.createAndRegisterInitialLinkSegmentCost(physicalNetwork, initialCostsFileLocation,timePeriod);
        taConfigurator.registerInitialLinkSegmentCost(initialCost);
      }
    };

    return setupAndExecuteAssignment(
        projectPath, 
        defaultSetOutputTypeConfigurationProperties, 
        registerInitialCosts,
        maxIterations, 
        epsilon, 
        setCostParameters, 
        description, 
        useFixedConnectoidTravelTimeCost, 
        recordZeroFlow);
  }

  /**
   * Deletes a file from the file system
   *
   * @param fileName location of the file to be deleted
   * @throws Exception thrown if there is an error deleting the file (except when it does not exist)
   */
  public static void deleteFile(final String fileName) throws Exception {
    try {
      final String rootPath = System.getProperty("user.dir");
      final Path path = FileSystems.getDefault().getPath(rootPath + "\\" + fileName);
      Files.delete(path);
    }catch(NoSuchFileException e) {
      LOGGER.fine(String.format("File cannot be deleted, it does not exist; %s", fileName));
    }
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
    final XMLElementMetadata metadataStandard = (XMLElementMetadata) JAXBUtils.generateObjectFromXml(
        XMLElementMetadata.class, new File(xmlFileStandard));
    final XMLElementMetadata metadataBeingTested = (XMLElementMetadata) JAXBUtils.generateObjectFromXml(
        XMLElementMetadata.class, new File(xmlFileBeingTested));

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
    if (!outputConfigurationStandard.getGapfunction().equals(outputConfigurationBeingTested.getGapfunction())) {
      return false;
    }
    if (!outputConfigurationStandard.getSmoothing().equals(outputConfigurationBeingTested.getSmoothing())) {
      return false;
    }
    if (!outputConfigurationStandard.getStopcriterion().equals(outputConfigurationBeingTested.getStopcriterion())) {
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
 
  public static void runFileEqualAssertionsAndCleanUp(OutputType outputType, String projectPath, String description,
      String csvFileName, String xmlFileName) throws Exception {
    
    String fullCsvFileNameWithoutDescription = projectPath + "\\" + outputType.value() + "_" + csvFileName;
    String fullCsvFileNameWithDescription = projectPath + "\\" + outputType.value() + "_" + description + "_" + csvFileName;
    
    assertTrue(compareFiles(fullCsvFileNameWithoutDescription,fullCsvFileNameWithDescription));
    deleteFile(outputType, projectPath, description, csvFileName);
    
    String fullXmlFileNameWithoutDescription = projectPath + "\\" + outputType.value() + "_" + xmlFileName;
    String fullXmlFileNameWithDescription = projectPath + "\\" + outputType.value() + "_" + description + "_" + xmlFileName;
    assertTrue(isXmlFileSameExceptForTimestamp(fullXmlFileNameWithoutDescription, fullXmlFileNameWithDescription));
    deleteFile(outputType, projectPath, description, xmlFileName);
  }

}