package org.goplanit.io.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConstants;

import org.apache.commons.io.FileUtils;
import org.goplanit.xml.utils.JAXBUtils;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.output.formatter.MemoryOutputIterator;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.functionalinterface.TriFunction;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.test.LinkSegmentExpectedResultsDto;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.xml.generated.XMLElementColumn;
import org.goplanit.xml.generated.XMLElementCsvdata;
import org.goplanit.xml.generated.XMLElementIteration;
import org.goplanit.xml.generated.XMLElementMetadata;
import org.goplanit.xml.generated.XMLElementOutputConfiguration;
import org.goplanit.xml.generated.XMLElementOutputTimePeriod;

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
  
  /**
   * Compares the results from an assignment run stored in a MemoryOutputFormatter
   * object to known results stored in a Map. It generates a JUnit test failure if
   * the results maps have different contents.
   * 
   * @param memoryOutputFormatter the MemoryOuptutFormatter object which stores results from a test run
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

        final int flowPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputPropertyType.FLOW);
        final int costPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputPropertyType.LINK_SEGMENT_COST);
        final int lengthPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputPropertyType.LENGTH);
        final int speedPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputPropertyType.CALCULATED_SPEED);
        final int capacityPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputPropertyType.CAPACITY_PER_LANE);
        final int numberOfLanesPosition = memoryOutputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputPropertyType.NUMBER_OF_LANES);
        final MemoryOutputIterator memoryOutputIterator = memoryOutputFormatter.getIterator(mode, timePeriod, iteration, OutputType.LINK);
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
    OutputPropertyType outputProperty = OutputPropertyType.PATH_STRING;
    if (outputType.equals(OutputType.OD)) {
      iteration--;
      outputProperty = OutputPropertyType.OD_COST;
    }
    for (TimePeriod timePeriod : map.keySet()) {
      @SuppressWarnings("unchecked") Map<Mode, Map<String, Map<String, ?>>> mapPerTimePeriod = (Map<Mode, Map<String, Map<String, ?>>>) map.get(timePeriod);
      for (Mode mode : mapPerTimePeriod.keySet()) {
        Map<String, Map<String, ?>> mapPerTimePeriodAndMode = mapPerTimePeriod.get(mode);
        final int position = memoryOutputFormatter.getPositionOfOutputValueProperty(outputType, outputProperty);
        final int originZonePosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(outputType, OutputPropertyType.ORIGIN_ZONE_XML_ID);
        final int destinationZonePosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(outputType, OutputPropertyType.DESTINATION_ZONE_XML_ID);
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
   * @param memoryOutputFormatter the MemoryOuptutFormatter object which stores results from a test run
   * @param iterationIndex the current iteration index
   * @param resultsMap Map storing standard test results which have been generated previously, identified by start and end node external Ids
   * @return true if all the tests pass, false otherwise
   * @throws PlanItException thrown if one of the test output properties has not been saved
   */
  public static boolean compareLinkResultsToMemoryOutputFormatterUsingNodesXmlId(
      final MemoryOutputFormatter memoryOutputFormatter, final Integer iterationIndex,
      final SortedMap<TimePeriod, SortedMap<Mode, SortedMap<String, SortedMap<String, LinkSegmentExpectedResultsDto>>>> resultsMap)
      throws PlanItException {
    
    return compareLinkResultsToMemoryOutputFormatter(memoryOutputFormatter, iterationIndex, resultsMap,
        (mode, timePeriod, iteration) -> {
          try {
          final int downstreamNodeXmlIdPosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(OutputType.LINK, OutputPropertyType.DOWNSTREAM_NODE_XML_ID);
          final int upstreamNodeXmlIdPosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(OutputType.LINK, OutputPropertyType.UPSTREAM_NODE_XML_ID);
          return Pair.of(downstreamNodeXmlIdPosition, upstreamNodeXmlIdPosition);
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
   * @param memoryOutputFormatter the MemoryOuptutFormatter object which stores results from a test run
   * @param iterationIndex the current iteration index
   * @param resultsMap Map storing standard test results which have been generated previously, identified link segment Id
   * @return true if all the tests pass, false otherwise
   * @throws PlanItException thrown if one of the test output properties has not been saved
   */
  public static boolean compareLinkResultsToMemoryOutputFormatterUsingLinkSegmentId(
      final MemoryOutputFormatter memoryOutputFormatter, final Integer iterationIndex,
      final SortedMap<TimePeriod, SortedMap<Mode, SortedMap<Long, LinkSegmentExpectedResultsDto>>> resultsMap)
      throws PlanItException {
    return compareLinkResultsToMemoryOutputFormatter(memoryOutputFormatter, iterationIndex, resultsMap,
        (mode, timePeriod, iteration) -> {
          try {
            final int linkSegmentIdPosition = memoryOutputFormatter.getPositionOfOutputKeyProperty(OutputType.LINK, OutputPropertyType.LINK_SEGMENT_ID);
         return Pair.of(linkSegmentIdPosition, 0);
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
   * Deletes a file from the file system
   *
   * @param fileName location of the file to be deleted
   * @throws Exception thrown if there is an error deleting the file (except when it does not exist)
   */
  public static void deleteFile(final String fileName) throws Exception {
    try {
      final String rootPath = System.getProperty("user.dir");
      Path path =  !fileName.startsWith(rootPath) ? Path.of(rootPath, fileName) : Path.of(fileName);
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
    deleteFile(Path.of(projectPath, outputType.value() + "_" + description + "_" + fileName).toString());
  }

  /**
   * Compares the contents of two text files
   *
   * In this test the text contents of the files must be exactly equal. This test
   * can be applied to any file type (CSV, XML etc)
   *
   * @param file1 location of the first file to be compared
   * @param file2 location of the second file to be compared
   * @param printFilesOnFalse when comparison returns false we can print the files when set to true, otherwise not
   * @return true if the contents of the two files are exactly equal, false otherwise
   * @throws IOException thrown if there is an error opening one of the files
   */
  public static boolean compareFiles(final String file1, final String file2, final boolean printFilesOnFalse) throws IOException {
    final var charSetName = "utf-8";
    final Path f1 = Path.of(file1).toAbsolutePath();
    if(Files.notExists(f1)){
      LOGGER.warning(String.format("File %s does not exist, printing available xml and csv files in dir",f1));
      FileUtils.listFiles(f1.getParent().toFile(),new String[]{"csv","xml"},false).forEach(f -> LOGGER.warning(f.toString()));
      return false;
    }
    final Path f2 = Path.of(file2).toAbsolutePath();
    if(Files.notExists(f2)){
      LOGGER.warning(String.format("File %s does not exist, printing available xml and csv files in dir",f2));
      FileUtils.listFiles(f2.getParent().toFile(),new String[]{"csv","xml"},false).forEach(f -> LOGGER.warning(f.toString()));
      return false;
    }

    final boolean contentEquals = FileUtils.contentEqualsIgnoreEOL(f1.toFile(), f2.toFile(), charSetName);
    if(!contentEquals && printFilesOnFalse) {
      LOGGER.warning("FILE NOT THE SAME: Printing contents for comparison");
      LOGGER.warning("File 1:");
      LOGGER.warning(FileUtils.readFileToString(f1.toFile(), charSetName));
      LOGGER.warning("File 2:");
      LOGGER.warning(FileUtils.readFileToString(f2.toFile(), charSetName));
    }
    return contentEquals;
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
   * then remove the results files, when files differ, always print the two files for comparison
   *
   * @param projectPath project directory containing the input files
   * @param description description used in temporary output file names
   * @param csvFileName name of CSV file containing run results
   * @param xmlFileName name of XML file containing run results
   * @throws Exception thrown if there is an error
   */

  public static void runFileEqualAssertionsAndCleanUp(OutputType outputType, String projectPath, String description,
                                                      String csvFileName, String xmlFileName) throws Exception {

    runFileEqualAssertionsAndCleanUp(outputType, projectPath, description, csvFileName, xmlFileName, true);
  }

  /**
   * Run assertions which confirm that results files contain the correct data, and
   * then remove the results files
   * 
   * @param projectPath project directory containing the input files
   * @param description description used in temporary output file names
   * @param csvFileName name of CSV file containing run results
   * @param xmlFileName name of XML file containing run results
   * @param printFilesOnFalse when comparison returns false we can print the files when set to true, otherwise not
   * @throws Exception thrown if there is an error
   */

  public static void runFileEqualAssertionsAndCleanUp(OutputType outputType, String projectPath, String description,
      String csvFileName, String xmlFileName, boolean printFilesOnFalse) throws Exception {
    
    String fullCsvFileNameWithoutDescription = Path.of(projectPath, outputType.value() + "_" + csvFileName).toString();
    String fullCsvFileNameWithDescription =  Path.of(projectPath, outputType.value() + "_" + description + "_" + csvFileName).toString();

    assertTrue(compareFiles(fullCsvFileNameWithoutDescription,fullCsvFileNameWithDescription, printFilesOnFalse));
    deleteFile(outputType, projectPath, description, csvFileName);
    
    String fullXmlFileNameWithoutDescription = Path.of(projectPath , outputType.value() + "_" + xmlFileName).toString();
    String fullXmlFileNameWithDescription = Path.of( projectPath , outputType.value() + "_" + description + "_" + xmlFileName).toString();
    assertTrue(isXmlFileSameExceptForTimestamp(fullXmlFileNameWithoutDescription, fullXmlFileNameWithDescription));
    deleteFile(outputType, projectPath, description, xmlFileName);
  }

}