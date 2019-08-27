package org.planit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.planit.data.MultiKeyPlanItData;
import org.planit.exceptions.PlanItException;
import org.planit.output.OutputType;
import org.planit.output.formatter.BasicMemoryOutputFormatter;
import org.planit.output.formatter.MemoryOutputFormatter;
import org.planit.output.property.OutputProperty;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;

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
        
/**
  * Compares the contents of a results map for the current run with a results map from a previous run which had been stored in a file.  It generates a JUnit test failure if the results maps have different contents.
  * 
  * @param resultsMap                 Map storing result of the current test run
  * @param resultsMapFromFile  Map storing results of a previous run which had been stored in a file
  */
	public static void compareResultsToCsvFileContents(SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>> resultsMap, 
			                                                                                 SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>> resultsMapFromFile) {
		if (resultsMap.keySet().size() != resultsMapFromFile.keySet().size()) {
			fail("Test case returned " + resultsMap.keySet().size() + " runs where the results file contains " + resultsMap.keySet().size() + ".");
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
				if (resultsMap.get(runId).get(timePeriod).keySet().size() != resultsMapFromFile.get(runId).get(timePeriod).keySet().size()) {
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
					if (resultsMap.get(runId).get(timePeriod).get(mode).size() != resultsMapFromFile.get(runId).get(timePeriod).get(mode).size()) {
						fail("Test case returned " + resultsMap.get(runId).get(timePeriod).get(mode).size()
								+ " results for run " + runId + ", timePeriod " + timePeriod.getId() + " and mode "
								+ mode.getId() + " where the results file contains "
								+ resultsMapFromFile.get(runId).get(timePeriod).get(mode).size() + ".");
						return;
					}
					for (LinkSegmentExpectedResultsDto resultDto : resultsMap.get(runId).get(timePeriod).get(mode)) {
						SortedSet<LinkSegmentExpectedResultsDto> resultsSetFromFile = resultsMapFromFile.get(runId).get(timePeriod).get(mode);
						if (!resultsSetFromFile.contains(resultDto)) {
							boolean passed = false;
							Iterator<LinkSegmentExpectedResultsDto> iterator = resultsSetFromFile.iterator();
							while (!passed && iterator.hasNext()) {
								LinkSegmentExpectedResultsDto resultDtoFromFile = iterator.next();
								passed = resultDto.equals(resultDtoFromFile);
							}
							if (!passed) {
								fail("The result for runId " + runId + " time period " + timePeriod.getId()
										+ " mode " + mode.getId() + " " + resultDto.toString()
										+ " was not found in the results file.");
							}
						}
					}
				}
			}
		}
	}
	
/**
 * Compares the results from an assignment run stored in a BasicMemoryOutputFormatter object to known results stored in a Map.  It generates a JUnit test failure if the results maps have different contents.
 * 
 * @param basicMemoryOutputFormatter the BasicMemoryOuptutFormatter object which stores results from a test run
 * @param resultsMap Map storing standard test results which have been generated previously
 * @throws PlanItException thrown if one of the test output properties has not been saved
 */
	public static void compareResultsToMemoryOutputFormatter(BasicMemoryOutputFormatter basicMemoryOutputFormatter, 
			                                                                                                 SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>> resultsMap) throws PlanItException {
		for (Long runId : resultsMap.keySet()) {
			for (TimePeriod timePeriod : resultsMap.get(runId).keySet()) {
				for (Mode mode : resultsMap.get(runId).get(timePeriod).keySet()) {
					for (LinkSegmentExpectedResultsDto resultDto : resultsMap.get(runId).get(timePeriod).get(mode)) {
						double flow = (Double) basicMemoryOutputFormatter.getLinkSegmentOutput(runId, timePeriod.getId(), mode.getExternalId(), resultDto.getStartNodeId(), resultDto.getEndNodeId(), OutputProperty.FLOW);
						double length = (Double) basicMemoryOutputFormatter.getLinkSegmentOutput(runId, timePeriod.getId(), mode.getExternalId(), resultDto.getStartNodeId(), resultDto.getEndNodeId(), OutputProperty.LENGTH);
						double speed = (Double) basicMemoryOutputFormatter.getLinkSegmentOutput(runId, timePeriod.getId(), mode.getExternalId(), resultDto.getStartNodeId(), resultDto.getEndNodeId(), OutputProperty.SPEED);
						double cost = (Double) basicMemoryOutputFormatter.getLinkSegmentOutput(runId, timePeriod.getId(), mode.getExternalId(), resultDto.getStartNodeId(), resultDto.getEndNodeId(), OutputProperty.COST);
						double capacityPerLane = (Double) basicMemoryOutputFormatter.getLinkSegmentOutput(runId, timePeriod.getId(), mode.getExternalId(), resultDto.getStartNodeId(), resultDto.getEndNodeId(), OutputProperty.CAPACITY_PER_LANE);
						int numberOfLanes = (Integer) basicMemoryOutputFormatter.getLinkSegmentOutput(runId, timePeriod.getId(), mode.getExternalId(), resultDto.getStartNodeId(), resultDto.getEndNodeId(), OutputProperty.NUMBER_OF_LANES);
						double capacity = capacityPerLane * numberOfLanes;
						assertEquals(flow, resultDto.getLinkFlow(), 0.00001);
						assertEquals(length, resultDto.getLength(), 0.00001);
						assertEquals(speed, resultDto.getSpeed(), 0.00001);
						assertEquals(capacity, resultDto.getCapacity(), 0.00001);
						assertEquals(cost, resultDto.getLinkCost(), 0.00001);
					}					
				}
			}		
		}		
	}
	
/**
 * Compares the results from an assignment run stored in a MemoryOutputFormatter object to known results stored in a Map.  It generates a JUnit test failure if the results maps have different contents.
 * 
 * @param outputType the current output type
 * @param memoryOutputFormatter the MemoryOuptutFormatter object which stores results from a test run
 * @param iterationIndex the current iteration index
 * @param resultsMap Map storing standard test results which have been generated previously
 * @throws PlanItException thrown if one of the test output properties has not been saved
 */
	public static void compareResultsToMemoryOutputFormatter(OutputType outputType, MemoryOutputFormatter memoryOutputFormatter, Integer iterationIndex,
            SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>> resultsMap) throws PlanItException {
		
		if (iterationIndex == null) {
			iterationIndex = memoryOutputFormatter.getLastIteration();
		}
		for (Long runId : resultsMap.keySet()) {
			for (TimePeriod timePeriod : resultsMap.get(runId).keySet()) {
				for (Mode mode : resultsMap.get(runId).get(timePeriod).keySet()) {
					for (LinkSegmentExpectedResultsDto resultDto : resultsMap.get(runId).get(timePeriod).get(mode)) {
						OutputProperty[] outputKeyProperties = memoryOutputFormatter.getOutputKeyProperties(outputType);
						OutputProperty[] outputValueProperties = memoryOutputFormatter.getOutputValueProperties(outputType);
						MultiKeyPlanItData multiKeyPlanItData = memoryOutputFormatter.getOutputData(mode, timePeriod, iterationIndex, outputType);
						Object[] keyValues = new Object[outputKeyProperties.length];
						keyValues[0] = Integer.valueOf((int) resultDto.getStartNodeId());
						keyValues[1] = Integer.valueOf((int) resultDto.getEndNodeId());
						for (int i=0; i<outputValueProperties.length; i++) {
							switch (outputValueProperties[i]) {
							case FLOW: 
								double flow = (Double) multiKeyPlanItData.getRowValue(OutputProperty.FLOW,  keyValues);
							    assertEquals(flow, resultDto.getLinkFlow(), 0.00001);
							    break;
							case LENGTH: 
								double length = (Double) multiKeyPlanItData.getRowValue(OutputProperty.LENGTH,  keyValues);
								assertEquals(length, resultDto.getLength(), 0.00001);
								break;
							case SPEED:
								double speed = (Double) multiKeyPlanItData.getRowValue(OutputProperty.SPEED,  keyValues);
								assertEquals(speed, resultDto.getSpeed(), 0.00001);
								break;
							case COST:
								double cost = (Double) multiKeyPlanItData.getRowValue(OutputProperty.COST,  keyValues);
								assertEquals(cost, resultDto.getLinkCost(), 0.00001);
								break;
							case CAPACITY_PER_LANE:
								double capacityPerLane = (Double) multiKeyPlanItData.getRowValue(OutputProperty.CAPACITY_PER_LANE,  keyValues);
								int numberOfLanes = (Integer) multiKeyPlanItData.getRowValue(OutputProperty.NUMBER_OF_LANES,  keyValues);
								assertEquals(numberOfLanes * capacityPerLane, resultDto.getCapacity(), 0.00001);
								break;
								}
						}
					}					
				}
			}		
		}		
	}
	
}
