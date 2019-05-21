package org.planit.test.utils;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.logging.Logger;

import org.planit.test.dto.BprResultDto;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;

import static org.junit.Assert.fail;

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
  * Compares the contents of a results map for the current run with a results map from a previous run which had been stored in a file.  It generates a JUnit test failure if the results maps have different contents
  * 
  * @param resultsMap                 Map storing result of the current test run
  * @param resultsMapFromFile  Map storing results of a previous run which had been stored in a file
  */
	public static void compareResultsToCsvFileContents(SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap, 
			                                                                                  SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMapFromFile) {
		if (resultsMap.keySet().size() != resultsMapFromFile.keySet().size()) {
			fail("Test case returned " + resultsMap.keySet().size() + " runs where the results file contains " 	+ resultsMap.keySet().size() + ".");
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
				if (resultsMap.get(runId).get(timePeriod).keySet().size() != resultsMapFromFile.get(runId)	.get(timePeriod).keySet().size()) {
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
					for (BprResultDto resultDto : resultsMap.get(runId).get(timePeriod).get(mode)) {
						SortedSet<BprResultDto> resultsSetFromFile = resultsMapFromFile.get(runId).get(timePeriod).get(mode);
						if (!resultsSetFromFile.contains(resultDto)) {
							boolean passed = false;
							Iterator<BprResultDto> iterator = resultsSetFromFile.iterator();
							while (!passed && iterator.hasNext()) {
								BprResultDto resultDtoFromFile = iterator.next();
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
}
