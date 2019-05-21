package org.planit.test.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.planit.test.dto.BprResultDto;
import org.planit.exceptions.PlanItException;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;

/**
 * Utility class containing methods for saving run results to a CSV file and reading previous run results from a CSV file.
 * 
 * At present MetroScan saves its results to a CSV file and retrieves previous results from a CSV file to use in unit testing.  Hence this class is in PlanIt rather than BasicCsvScan. 
 * 
 * @author gman6028
 *
 */
public class CsvIoUtils {
	
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(CsvIoUtils.class.getName());
        
/**
 * Saves the results of a complete run to a CSV file
 * 
 * @param resultsMap					Map containing the results of the run
 * @param resultsFileLocation		location of CSV file the results are saved to
 * @throws PlanItException			thrown if there is an error creating the CSV file
 */
	public static void saveResultsToCsvFile(SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap, String resultsFileLocation) throws PlanItException {

		File existingFile = new File(resultsFileLocation);
		if (existingFile.exists()) {
			existingFile.delete();
		}
		try (CSVPrinter printer = new CSVPrinter(new FileWriter(resultsFileLocation), CSVFormat.EXCEL)) {
			printer.printRecord("Run Id", "Time Period Id", "Mode Id", "Start Node Id", "End Node Id", "Link Flow", "Capacity", "Length", "Speed", "Link Cost",  "Cost to End Node");
			for (Long runId : resultsMap.keySet()) {
				for (TimePeriod timePeriod : resultsMap.get(runId).keySet()) {
					for (Mode mode : resultsMap.get(runId).get(timePeriod).keySet()) {
						for (BprResultDto resultDto : resultsMap.get(runId).get(timePeriod).get(mode)) {
						    printCurrentRecord(printer, runId, timePeriod, mode, resultDto);
						}
					}
				}	
			} 
			printer.close();
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
	}
	
/**
 * Print the current record to a CSV file
 * 
 * @param printer                         CSVPrinter to which record will be written
 * @param trafficAssignmentId    id of the current traffic assignment run
 * @param timePeriod                  the current time period
 * @param mode                          the current mode
 * @param resultDto                    BprResultDto storing the current results record
 * @throws Exception                  thrown if the record cannot be written
 */
	public static void printCurrentRecord(CSVPrinter printer, long trafficAssignmentId, TimePeriod timePeriod, Mode mode, BprResultDto resultDto) throws Exception {
        printer.printRecord(trafficAssignmentId, 
                                        timePeriod.getId(), 
                                        mode.getId(), 
                                        resultDto.getStartNodeId(),
                                        resultDto.getEndNodeId(), 
                                        resultDto.getLinkFlow(), 
                                        resultDto.getCapacity(),
                                        resultDto.getLength(),
                                        resultDto.getSpeed(),
                                        resultDto.getLinkCost(), 
                                        resultDto.getTotalCostToEndNode());
 	}
	
/**
 * Retrieves the results of a previous run from a CSV file
 * 				
 * @param resultsFileLocation		the location of the CSV file containing the run results
 * @return										Map storing the run results
 * @throws PlanItException			thrown if there is an error opening the file
 */
	public static SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> createResultsMapFromCsvFile(String resultsFileLocation) throws PlanItException {
		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>> resultsMap = new TreeMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<BprResultDto>>>>();
		try (Reader in = new FileReader(resultsFileLocation)) {
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);
			for (CSVRecord record : records) {
				long runId = Long.parseLong(record.get("Run Id"));
				if (!resultsMap.containsKey(runId)) {
					resultsMap.put(runId, new TreeMap<TimePeriod,SortedMap<Mode, SortedSet<BprResultDto>>>());
				}
				long timePeriodId = Long.parseLong(record.get("Time Period Id"));
				TimePeriod timePeriod = TimePeriod.getById(timePeriodId);
				if (!resultsMap.get(runId).containsKey(timePeriod)) {
					resultsMap.get(runId).put(timePeriod, new TreeMap<Mode, SortedSet<BprResultDto>>());
				}			
				long modeId = Long.parseLong(record.get("Mode Id"));
				Mode mode = Mode.getById(modeId);
				if (!resultsMap.get(runId).get(timePeriod).containsKey(mode)) {
					resultsMap.get(runId).get(timePeriod).put(mode, new TreeSet<BprResultDto>());
				}
				long startNodeId = Long.parseLong(record.get("Start Node Id"));
				long endNodeId = Long.parseLong(record.get("End Node Id"));
				double linkFlow = Double.parseDouble(record.get("Link Flow"));
				double linkCost = Double.parseDouble(record.get("Link Cost"));
				double totalCostToEndNode = Double.parseDouble(record.get("Cost to End Node"));
				double capacity = Double.parseDouble(record.get("Capacity"));
				double length = Double.parseDouble(record.get("Length"));
				double speed = Double.parseDouble(record.get("Speed"));
				BprResultDto resultDto = new BprResultDto(startNodeId, endNodeId, linkFlow, linkCost, totalCostToEndNode, capacity, length, speed);
				resultsMap.get(runId).get(timePeriod).get(mode).add(resultDto);
			}
			in.close();
			return resultsMap;
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
	}
	
}
