package org.planit.planitio.test.integration;

import java.io.FileReader;
import java.io.Reader;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.ModeImpl;
import org.planit.planitio.test.integration.LinkSegmentExpectedResultsDto;
import org.planit.time.TimePeriod;
import org.planit.utils.network.physical.Mode;

/**
 * Utility class containing methods for saving run results to a CSV file and
 * reading previous run results from a CSV file.
 * 
 * At present MetroScan saves its results to a CSV file and retrieves previous
 * results from a CSV file to use in unit testing. Hence this class is in PlanIt
 * rather than BasicCsvScan.
 * 
 * @author gman6028
 *
 */
public class CsvIoUtils {

//	/**
//	 * Print the current record to a CSV file
//	 * 
//	 * @param printer             CSVPrinter to which record will be written
//	 * @param trafficAssignmentId id of the current traffic assignment run
//	 * @param timePeriod          the current time period
//	 * @param mode                the current mode
//	 * @param resultDto           BprResultDto storing the current results record
//	 * @throws Exception thrown if the record cannot be written
//	 */
//	public static void printCurrentRecord(CSVPrinter printer, long trafficAssignmentId, TimePeriod timePeriod,
//			Mode mode, LinkSegmentExpectedResultsDto resultDto) throws Exception {
//		printer.printRecord(trafficAssignmentId, timePeriod.getId(), mode.getExternalId(), resultDto.getStartNodeId(),
//				resultDto.getEndNodeId(), resultDto.getLinkFlow(), resultDto.getCapacity(), resultDto.getLength(),
//				resultDto.getSpeed(), resultDto.getLinkCost(), resultDto.getTotalCostToEndNode());
//	}
//
//	/**
//	 * Retrieves the results of a previous run from a CSV file
//	 * 
//	 * @param resultsFileLocation the location of the CSV file containing the run
//	 *                            results
//	 * @return Map storing the run results
//	 * @throws PlanItException thrown if there is an error opening the file
//	 */
//	public static SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>> createResultsMapFromCsvFile(
//			String resultsFileLocation) throws PlanItException {
//		SortedMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>> resultsMap = new TreeMap<Long, SortedMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>>();
//		try (Reader in = new FileReader(resultsFileLocation)) {
//			Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);
//			for (CSVRecord record : records) {
//				long runId = Long.parseLong(record.get("Run Id"));
//				if (!resultsMap.containsKey(runId)) {
//					resultsMap.put(runId, new TreeMap<TimePeriod, SortedMap<Mode, SortedSet<LinkSegmentExpectedResultsDto>>>());
//				}
//				long timePeriodId = Long.parseLong(record.get("Time Period Id"));
//				TimePeriod timePeriod = TimePeriod.getByExternalId(timePeriodId);
//				if (!resultsMap.get(runId).containsKey(timePeriod)) {
//					resultsMap.get(runId).put(timePeriod, new TreeMap<ModeImpl, SortedSet<LinkSegmentExpectedResultsDto>>());
//				}
//				long modeExternalId = Long.parseLong(record.get("Mode Id"));
//				ModeImpl mode = ModeImpl.getByExternalId(modeExternalId);
//				if (!resultsMap.get(runId).get(timePeriod).containsKey(mode)) {
//					resultsMap.get(runId).get(timePeriod).put(mode, new TreeSet<LinkSegmentExpectedResultsDto>());
//				}
//				long startNodeId = Long.parseLong(record.get("Upstream Node External Id"));
//				long endNodeId = Long.parseLong(record.get("Downstream Node External Id"));
//				double linkFlow = Double.parseDouble(record.get("Link Flow"));
//				double linkCost = Double.parseDouble(record.get("Link Cost"));
//				double totalCostToEndNode = Double.parseDouble(record.get("Cost to End Node"));
//				double capacity = Double.parseDouble(record.get("Capacity"));
//				double length = Double.parseDouble(record.get("Length"));
//				double speed = Double.parseDouble(record.get("Speed"));
//				LinkSegmentExpectedResultsDto resultDto = new LinkSegmentExpectedResultsDto(startNodeId, endNodeId, linkFlow, linkCost,
//						totalCostToEndNode, capacity, length, speed);
//				resultsMap.get(runId).get(timePeriod).get(mode).add(resultDto);
//			}
//			in.close();
//			return resultsMap;
//		} catch (Exception ex) {
//			throw new PlanItException(ex);
//		}
//	}

}
