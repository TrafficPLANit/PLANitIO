package org.goplanit.io.output.formatter;

import org.apache.commons.csv.CSVPrinter;
import org.goplanit.io.xml.converter.XmlEnumConverter;
import org.goplanit.io.xml.util.ApplicationProperties;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.output.adapter.BushLinkOutputTypeAdapter;
import org.goplanit.output.adapter.OutputAdapter;
import org.goplanit.output.configuration.OutputConfiguration;
import org.goplanit.output.configuration.OutputTypeConfiguration;
import org.goplanit.output.configuration.SimulationOutputTypeConfiguration;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.enums.OutputTypeEnum;
import org.goplanit.output.enums.SubOutputTypeEnum;
import org.goplanit.output.formatter.CsvFileOutputFormatter;
import org.goplanit.output.formatter.CsvTextFileOutputFormatter;
import org.goplanit.output.formatter.XmlTextFileOutputFormatter;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.FileUtils;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.xml.generated.v2.*;
import org.goplanit.xml.utils.JAXBUtils;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The default output formatter of PlanIt
 * 
 * @author markr
 *
 */
public class PlanItOutputFormatter extends CsvFileOutputFormatter
    implements CsvTextFileOutputFormatter, XmlTextFileOutputFormatter {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanItOutputFormatter.class.getCanonicalName());

  /** default extension for XML files */
  private static final String DEFAULT_XML_NAME_EXTENSION = ".xml";
  
  /** default prefix to use for XML output */
  private static final String DEFAULT_XML_NAME_PREFIX = "XMLOutput";
  
  /** default extension for CSV files */
  private static final String DEFAULT_CSV_NAME_EXTENSION = ".csv";
  
  /** default prefix to use for CSV output */
  private static final String DEFAULT_CSV_NAME_PREFIX = "CSVOutput";

  /** default to indicate whether to consolidate all simulation data into a single file across iterations */
  private static final boolean DEFAULT_CONSOLIDATE_SIMULATION_OUTPUT = true;

  // CONFIG members

  /** The root directory to store the XML output files */
  private String xmlDirectory;

  /** The root directory of the CSV output files */
  private String csvDirectory;

  /** The extension of the XML output files */
  private String xmlNameExtension;

  /** The root name of the XML output files */
  private String xmlNameRoot;

  /** The extension of the CSV output files */
  private String csvNameExtension;

  /** The root name of the CSV output files */
  private String csvNameRoot;

  /** Flag to indicate whether XML output directory should be cleared before the run */
  private boolean resetXmlDirectory;

  /** Flag to indicate whether the CSV output directory should be cleared before the run */
  private boolean resetCsvDirectory;

  /** flag to indicate whether to consolidate all simulation data into a single file across iterations */
  private boolean consolidateSimulationOutput = DEFAULT_CONSOLIDATE_SIMULATION_OUTPUT;

  // INTERNAL members

  /** Map of XML output file names for each OutputType */
  private final Map<OutputType, String> xmlFileNameMap;
  
  /**
   * Generated object for the metadata element in the output XML file
   */
  private final Map<OutputTypeEnum, XMLElementMetadata> metadata;

  /** in case we consolidate simulation data, track the data in memory in this list and persist after final iteration in
   * single file instead */
  private final List<Map<Mode,List<Object>>> consolidatedSimulationData = new ArrayList<>();
 
  /** Create the logging prefix to use for non assignment specific logging messages
   * 
   * @return loggingPrefix
   */
  private String createLoggingPrefix() {
    return LoggingUtils.outputFormatterPrefix(this.id);
  }
  
  /** Create the logging prefix to use for assignment specific logging messages
   * 
   * @return loggingPrefix
   */
  private String createLoggingPrefix(long runId) {
    return LoggingUtils.runIdPrefix(runId) +createLoggingPrefix();
  }  
  
  /**
   * Generates the name of an output file using the relative path from XML to CSV files
   * 
   * @param outputType the OutputType of the output
   * @param outputAdapter outputAdapter
   * @param timePeriod the time period 
   * @param iteration current iteration
   * @return the name of the output file
   */
  private String generateRelativeCsvOutputFileName(
      final OutputType outputType,
      final OutputAdapter outputAdapter,
      final TimePeriod timePeriod,
      int iteration){
    
    String absoluteFileName = generateAbsoluteCsvFileName(
        csvDirectory, csvNameRoot, csvNameExtension, timePeriod, outputType, outputAdapter.getRunId(), iteration);
    Path pathBase = Paths.get(xmlDirectory);
    return pathBase.toAbsolutePath().relativize(Path.of(absoluteFileName)).toString();
  }

  /**
   * Finalize the persistence after the simulation generating the XML meta-data file(s)
   *
   * @param outputType the outputType
   * @param outputConfiguration OutputTypeConfiguration of the assignment that have been activated
   * @throws if JAXBUtils throws
   */
  private void finaliseXmlMetaFileAfterSimulation(
      OutputType outputType, OutputConfiguration outputConfiguration) throws Exception {

    final String metaDataSchemaUri = PlanitSchema.createPlanitSchemaUri(
        PlanitSchema.METADATA_XSD, PlanitSchema.LATEST_SCHEMA_VERSION);
    OutputTypeConfiguration outputTypeConfiguration = outputConfiguration.getOutputTypeConfiguration(outputType);
    if (xmlFileNameMap.containsKey(outputType)) {
      Path xmlFilePath = Paths.get(xmlFileNameMap.get(outputType));
      if (metadata.containsKey(outputType)) {
        JAXBUtils.marshalAndNormalize(
            metadata.get(outputType), XMLElementMetadata.class, xmlFilePath,metaDataSchemaUri);
      } else if (outputTypeConfiguration.hasActiveSubOutputTypes()) {
        Set<SubOutputTypeEnum> activeSubOutputTypes = outputTypeConfiguration.getActiveSubOutputTypes();
        for (SubOutputTypeEnum subOutputTypeEnum : activeSubOutputTypes) {
          JAXBUtils.marshalAndNormalize(
              metadata.get(subOutputTypeEnum), XMLElementMetadata.class,xmlFilePath,metaDataSchemaUri);
        }
      }
    }
  }

  /**
   * Persist the so-far in memory kept simulation data and persist it to disk in a single file instead
   *
   * @param simulationOutputTypeconfiguration to use
   * @param outputAdapter to use
   * @param resetConsolidatedData             when true reset data after persisting, otherwise not
   * @param timePeriod the last time period used before simulation ended
   * @param iterationIndex the last iteration index of the last time period used before the simulation eneded
   */
  private void persistConsolidatedSimulationDataAfterTimePeriod(
          SimulationOutputTypeConfiguration simulationOutputTypeconfiguration,
          OutputAdapter outputAdapter,
          TimePeriod timePeriod,
          int iterationIndex,
          boolean resetConsolidatedData) {

    if(consolidatedSimulationData.isEmpty()){
      return;
    }

    var concatenatedRowValueList =
            consolidatedSimulationData.stream().flatMap(
                iterationData -> iterationData.values().stream()).collect(Collectors.toList());

    /* print single iteration results to CSV in Lambda */
    Function<CSVPrinter, PlanItException> lambdaFunc = csvPrinter -> {
      try {
        for(var row : concatenatedRowValueList) {
          csvPrinter.printRecord(row);
        }
      }catch (Exception e) {
        LOGGER.severe(e.getMessage());
        return new PlanItException("Error when writing consolidated simulation results for current time period in" +
            "CSVOutputFileFormatter", e);
      }
      return null;
    };

    /* pass on Lambda so we persist consolidated iteration results */
    writeCombinedXmlAndCsvForTypeAndTimePeriodIteration(
        simulationOutputTypeconfiguration,
        OutputType.SIMULATION,
        outputAdapter,
        timePeriod,
        iterationIndex,
        lambdaFunc);

    if(resetConsolidatedData){
      consolidatedSimulationData.clear();
    }
  }

  /**
   * Update the generated metadata simulation output object for the current
   * iteration
   * 
   * @param iterationIndex index of the current iteration
   * @param csvFileName name of CSV file used to store data for the current iteration
   * @param currentOutputType the (sub) output type of the data the CSV file is storing
   * @throws PlanItException thrown if error
   */
  private void updateMetadataSimulationOutputForCurrentIteration(
      int iterationIndex, final String csvFileName, final OutputTypeEnum currentOutputType) throws PlanItException {
    
    XMLElementIteration iteration = new XMLElementIteration();
    iteration.setNr(BigInteger.valueOf(iterationIndex));
    XMLElementCsvdata csvdata = new XMLElementCsvdata();
    csvdata.setValue(csvFileName);
    iteration.getCsvdatas().add(csvdata);
    if (currentOutputType instanceof OutputType) {
      csvdata.setType(((OutputType) currentOutputType).value());
      metadata.get((OutputType) currentOutputType).getSimulation().getIterations().add(iteration);
    } else if (currentOutputType instanceof SubOutputTypeEnum) {
      csvdata.setType(((SubOutputTypeEnum) currentOutputType).value());
      metadata.get((SubOutputTypeEnum) currentOutputType).getSimulation().getIterations().add(iteration);
    } else {
      throw new PlanItException("invalid output type provided when updating metadata simulation output for current iteration");
    }

  }

  /**
   * Generate time stamp for the current date and time
   * 
   * @return XMLGregorianCalendar with the current date and time
   * @throws DatatypeConfigurationException thrown if the time stamp cannot be created
   */
  private XMLGregorianCalendar getTimestamp() throws DatatypeConfigurationException {
    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
    return datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
  }

  /**
   * Create generated Columns object to be used in the XML output, based on user selections
   * 
   * @param outputProperties sorted set of output properties to be included in the output
   * @return generated Columns object
   */
  private XMLElementColumns getGeneratedColumnsFromProperties(
      final SortedSet<OutputProperty> outputProperties) throws PlanItException {

    XMLElementColumns generatedColumns = new XMLElementColumns();
    for (var outputProperty : outputProperties) {
      XMLElementColumn generatedColumn = new XMLElementColumn();
      generatedColumn.setName(outputProperty.getName());
      generatedColumn.setUnits(XmlEnumConverter.convertFromPlanItToXmlGeneratedUnits(outputProperty));
      generatedColumn.setType(XmlEnumConverter.convertFromPlanItToXmlGeneratedType(outputProperty.getDataType()));
      generatedColumns.getColumns().add(generatedColumn);
    }
    return generatedColumns;
  }

  /**
   * Create the generated XMLElementOutputConfiguration object
   * 
   * @param outputAdapter the OutputAdapter object containing the run information
   * @param timePeriod the current time period
   * @return the XMLElementOutputConfiguration object
   */
  private XMLElementOutputConfiguration getXmlOutputConfiguration(
          final OutputAdapter outputAdapter, TimePeriod timePeriod) {
    XMLElementOutputConfiguration outputconfiguration = new XMLElementOutputConfiguration();
    outputconfiguration.setAssignment(outputAdapter.getAssignmentClassName());
    outputconfiguration.setPhysicalcost(outputAdapter.getPhysicalCostClassName());
    outputconfiguration.setVirtualcost(outputAdapter.getVirtualCostClassName());
    outputconfiguration.setSmoothing(outputAdapter.getSmoothingClassName());
    outputconfiguration.setGapfunction(outputAdapter.getGapFunctionClassName());
    outputconfiguration.setStopcriterion(outputAdapter.getStopCriterionClassName());
    XMLElementOutputTimePeriod xmlTimePeriod = new XMLElementOutputTimePeriod();
    xmlTimePeriod.setId(timePeriod.getXmlId());
    xmlTimePeriod.setName(timePeriod.getDescription());
    outputconfiguration.setTimeperiod(xmlTimePeriod);
    return outputconfiguration;
  }

  /**
   * Creates the output file directory if it does not already exist.
   * 
   * @param outputDirectory the output file directory
   * @param resetDirectory if true, directory will be purged of previous contents
   */
  private void createOrOpenOutputDirectory( final String outputDirectory, boolean resetDirectory){
    try {

      Path absoluteDir = Path.of(outputDirectory).toAbsolutePath();
      File dirAsFile = absoluteDir.toFile();
      if (!dirAsFile.isDirectory()) {
        Files.createDirectories(absoluteDir);
      }
      if (resetDirectory) {
        purgeDirectory(absoluteDir.toFile());
      }
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException("Error when creating output directory in PLANitIO OutputFormatter", e);
    }
  }

  /**
   * Initialize the current metadata output object with data which is only written once per time period
   * 
   * @param currentOutputType the current (sub)OutputType we're persisting
   * @param outputTypeConfiguration the OutputTypeConfiguration object containing the run information
   * @param outputAdapter the OutputAdapter object being used for the output
   * @param timePeriod current time period
   * @throws PlanItException thrown if there is an error writing the data to file
   */
  private void initializeMetadataObject(
      final OutputTypeEnum currentOutputType,
      final OutputTypeConfiguration outputTypeConfiguration,
      final OutputAdapter outputAdapter,
      final TimePeriod timePeriod) throws PlanItException {
    
    try {
      metadata.get(currentOutputType).setTimestamp(getTimestamp());
      metadata.get(currentOutputType).setVersion(ApplicationProperties.getVersion());
      metadata.get(currentOutputType).setDescription(ApplicationProperties.getDescription());

      XMLElementOutputConfiguration outputConfiguration = getXmlOutputConfiguration(outputAdapter, timePeriod);
      metadata.get(currentOutputType).setOutputconfiguration(outputConfiguration);
      SortedSet<OutputProperty> outputProperties = outputTypeConfiguration.getOutputProperties();
      metadata.get(currentOutputType).setColumns(getGeneratedColumnsFromProperties(outputProperties));
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when initialising meta data object in PLANitIO OutputFormatter", e);
    }
    
  }

  /**
   * Remove all files and sub-directories from a specified directory
   * 
   * @param directory directory to be cleared
   */
  private void purgeDirectory(final File directory) {
    for (File file : Objects.requireNonNull(directory.listFiles())) {
      if (file.isDirectory())
        purgeDirectory(file);
      file.delete();
    }
  }

  /**
   * Create a XML meta data file with content based on the current (sub) output type
   *
   * @param outputTypeConfiguration          the OutputTypeConfiguration object containing the run information
   * @param currentOutputType the current (sub)OutputType we're persisting
   * @param outputAdapter the current output adapter
   * @param timePeriod the current time period
   * @throws Exception thrown if there is an error
   */
  private void createAndPersistXmlMetaDataForTimePeriodCurrentIteration(
      final OutputTypeConfiguration outputTypeConfiguration,
      final OutputTypeEnum currentOutputType,
      final OutputAdapter outputAdapter,
      final TimePeriod timePeriod) throws Exception {

    OutputType outputType = outputTypeConfiguration.getOutputType();

    boolean isNewTimePeriod =
        !metadata.containsKey(currentOutputType) ||
            !metadata.get(currentOutputType).getOutputconfiguration().getTimeperiod().getId().equals(
                    //this is XML element
                String.valueOf(timePeriod.getXmlId()));

    if (isNewTimePeriod) {

      /* create XML meta data header setup */
      if (metadata.containsKey(currentOutputType)) {
        JAXBUtils.marshalAndNormalize(metadata.get(currentOutputType), XMLElementMetadata.class,
            Paths.get(xmlFileNameMap.get(outputType)),PlanitSchema.createPlanitSchemaUri(PlanitSchema.METADATA_XSD));
      }
      metadata.put(currentOutputType, new XMLElementMetadata());
      XMLElementSimulation simulation = new XMLElementSimulation();
      metadata.get(currentOutputType).setSimulation(simulation);
      initializeMetadataObject(currentOutputType, outputTypeConfiguration, outputAdapter, timePeriod);

      xmlFileNameMap.put(outputType,
          generateAbsoluteCsvFileName(
              xmlDirectory, xmlNameRoot, xmlNameExtension, timePeriod, outputType, outputAdapter.getRunId()));
    }
  }

  /**
   * Create a CSV file with output content based on the current (sub) output type
   *
   * @param csvFileName                      to use
   * @param outputTypeConfiguration          the OutputTypeConfiguration object containing the run information
   * @param createCsvFileForCurrentIteration lambda function which records data specific to the CSV file for the
   *                                         current iteration
   * @throws PlanItException thrown if there is an error
   */
  private void createAndPersistCsvFileForTimePeriodCurrentIteration(
      final String csvFileName,
      final OutputTypeConfiguration outputTypeConfiguration,
      final  Function<CSVPrinter, PlanItException> createCsvFileForCurrentIteration) throws PlanItException {

    try(CSVPrinter csvIterationPrinter = createCsvPrinter(csvFileName)) {
      // create the header (first line) of the file
      csvIterationPrinter.printRecord(generateCsvHeader(outputTypeConfiguration));

      // create content by delegating to (sub) output type specific function
      PlanItException ple = createCsvFileForCurrentIteration.apply(csvIterationPrinter);
      if (ple != null) {
        throw ple;
      }
    }catch( PlanItException e) {
      throw e;
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when creating CSV file name and file in PLANitIO OutputFormatter", e);
    }

  }

  /**
   * Write the results for the current mode and time period to file, create a CSV printer to feed to lambda
   * 
   * @param outputTypeConfiguration the current output type configuration
   * @param currentOutputType the current (sub)OutputType we're persisting
   * @param outputAdapter the current output adapter
   * @param timePeriod the current time period
   * @param iterationIndex iterationIndex relevant for this data
   * @param feedCsvPrinterExecuteCsvFileWriting lambda function which records data specific to the CSV file
   *                                         for the current iteration and is fed a CSV printer
   */
  private void writeCombinedXmlAndCsvForTypeAndTimePeriodIteration(
      final OutputTypeConfiguration outputTypeConfiguration,
      final OutputTypeEnum currentOutputType, 
      final OutputAdapter outputAdapter, 
      final TimePeriod timePeriod, 
      int iterationIndex,
      final Function<CSVPrinter, PlanItException> feedCsvPrinterExecuteCsvFileWriting){
    
    try {

      /* XML meta data */
      createAndPersistXmlMetaDataForTimePeriodCurrentIteration(
          outputTypeConfiguration, currentOutputType, outputAdapter, timePeriod);

      // create the name based on iteration, time period and related info
      String csvFileName = generateAbsoluteCsvFileName(
          csvDirectory,
          csvNameRoot,
          csvNameExtension,
          timePeriod,
          outputTypeConfiguration.getOutputType(),
          outputAdapter.getRunId(),
          iterationIndex);

      // do work
      createAndPersistCsvFileForTimePeriodCurrentIteration(
          csvFileName,
          outputTypeConfiguration,
          feedCsvPrinterExecuteCsvFileWriting);

      // add metadata to the XML content
      String relativeCsvFileName = generateRelativeCsvOutputFileName(
              outputTypeConfiguration.getOutputType(), outputAdapter, timePeriod, iterationIndex);
      updateMetadataSimulationOutputForCurrentIteration(iterationIndex, relativeCsvFileName, currentOutputType);
      addCsvFileNamePerOutputType(currentOutputType, csvFileName);

    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("PlanitException occurred when writing results for current time period in PLANitIO " +
              "OutputFormatter, verify file is not already open and/or sufficient permissions are available");
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException(
              "Error when writing results for current time period in PLANitIO OutputFormatter", e);
    }
  }

  /**
   * Log information regarding the output to the log
   * 
   * @param outputAdapter used
   */
  private void logOutputInformation(final OutputAdapter outputAdapter) {
    if (isXmlDirectorySet()) {
      LOGGER.info(this.createLoggingPrefix(outputAdapter.getRunId()) +
              "XML meta-data directory set: " + xmlDirectory);
    } else {
      LOGGER.info(this.createLoggingPrefix(outputAdapter.getRunId()) +
              "XML meta-data output directory unknown");
    }

    if (isCsvDirectorySet()) {
      LOGGER.info(this.createLoggingPrefix(outputAdapter.getRunId()) +
              "CSV result directory set: " + csvDirectory);
    } else {
      LOGGER.info(this.createLoggingPrefix(outputAdapter.getRunId()) +
              "CSV result directory unknown");
    }
  }

  /**
   * Write Simulation results for the current time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we are persisting for
   *                          (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   */
  @Override
  protected void writeSimulationResultsForCurrentTimePeriod(
      final OutputConfiguration outputConfiguration, 
      final OutputTypeConfiguration outputTypeConfiguration,
      final OutputTypeEnum currentOutputType, 
      final OutputAdapter outputAdapter, 
      final Set<Mode> modes, 
      final TimePeriod timePeriod,
      int iterationIndex){

    /* collect single iteration results */
    final var rowValuesByMode = constructSimulationResultsForCurrentTimePeriod(
      outputConfiguration, outputTypeConfiguration, currentOutputType, outputAdapter, modes, timePeriod);

    if(!isConsolidateSimulationOutput()) {
      /* print single iteration results to CSV in Lambda */
      Function<CSVPrinter, PlanItException> lambdaFunc = csvPrinter -> {
        try {
          for (Mode mode : modes) {
            csvPrinter.printRecord(rowValuesByMode.get(mode));
          }
        }catch (Exception e) {
          LOGGER.severe(e.getMessage());
          return new PlanItException(
              "Error when writing simulation results for current time period in CSVOutputFileFormatter", e);
        }
        return null;
      };

      /* pass on Lambda so we persist single iteration results */
      writeCombinedXmlAndCsvForTypeAndTimePeriodIteration(
          outputTypeConfiguration, currentOutputType, outputAdapter, timePeriod, iterationIndex, lambdaFunc);

    }else{
      /* store results in memory, delay printing until done with simulation */
      consolidatedSimulationData.add(rowValuesByMode);
    }
  }

  /**
   * Write General results for the current time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   */
  @Override
  protected void writeGeneralResultsForCurrentTimePeriod(
      final OutputConfiguration outputConfiguration, 
      final OutputTypeConfiguration outputTypeConfiguration,
      final OutputTypeEnum currentOutputType, 
      final OutputAdapter outputAdapter, 
      final Set<Mode> modes, 
      final TimePeriod timePeriod,
      int iterationIndex){
    
    LOGGER.info(this.createLoggingPrefix(
        outputAdapter.getRunId()) +"XML Output for OutputType GENERAL has not been implemented yet.");
    
  }

  /**
   * Write Origin-Destination results for the time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   */
  @Override
  protected void writeOdResultsForCurrentTimePeriod(
      final OutputConfiguration outputConfiguration, 
      final OutputTypeConfiguration outputTypeConfiguration,
      final OutputTypeEnum currentOutputType, 
      final OutputAdapter outputAdapter, 
      final Set<Mode> modes, 
      final TimePeriod timePeriod,
      int iterationIndex){
    
    writeCombinedXmlAndCsvForTypeAndTimePeriodIteration(
            outputTypeConfiguration, currentOutputType, outputAdapter, timePeriod,
        iterationIndex, (csvPrinter) ->
            writeOdResultsForCurrentTimePeriodToCsvPrinter(
              outputConfiguration,
              outputTypeConfiguration,
              currentOutputType,
              outputAdapter,
              modes,
              timePeriod,
              csvPrinter));
  }

  /**
   * Write Path results for the time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current  persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   */
  @Override
  protected void writePathResultsForCurrentTimePeriod(
      final OutputConfiguration outputConfiguration, 
      final OutputTypeConfiguration outputTypeConfiguration,
      final OutputTypeEnum currentOutputType, 
      final OutputAdapter outputAdapter, 
      final Set<Mode> modes, 
      final TimePeriod timePeriod,
      int iterationIndex){
    
    writeCombinedXmlAndCsvForTypeAndTimePeriodIteration(
            outputTypeConfiguration, currentOutputType, outputAdapter, timePeriod,
        iterationIndex, (csvPrinter) ->
            writePathResultsForCurrentTimePeriodToCsvPrinter(
                outputConfiguration,
                outputTypeConfiguration,
                currentOutputType,
                outputAdapter,
                modes,
                timePeriod,
                csvPrinter));
  }

  /**
   * Write link results for the current time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   */
  @Override
  protected void writeLinkResultsForCurrentTimePeriod(
      final OutputConfiguration outputConfiguration, 
      final OutputTypeConfiguration outputTypeConfiguration,
      final OutputTypeEnum currentOutputType, 
      final OutputAdapter outputAdapter, 
      final Set<Mode> modes, 
      final TimePeriod timePeriod,
      int iterationIndex){
    
    writeCombinedXmlAndCsvForTypeAndTimePeriodIteration(
        outputTypeConfiguration,
        currentOutputType,
        outputAdapter,
        timePeriod,
        iterationIndex,
        (csvPrinter) -> writeLinkResultsForCurrentTimePeriodToCsvPrinter(
            outputConfiguration,
            outputTypeConfiguration,
            currentOutputType,
            outputAdapter,
            modes,
            timePeriod,
            csvPrinter));
  }

  /**
   * Write link results for the current time period to the CSV file
   *
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   */
  @Override
  protected void writeBushResultsForCurrentTimePeriod(
      final OutputConfiguration outputConfiguration,
      final OutputTypeConfiguration outputTypeConfiguration,
      final OutputTypeEnum currentOutputType,
      final OutputAdapter outputAdapter,
      final Set<Mode> modes,
      final TimePeriod timePeriod,
      int iterationIndex){

    // NOTE: reworking of #writeLinkResultsForCurrentTimePeriod to allow for single XML meta data and
    // per bush CSV

    /* invoke a file per bush, so unlike other formats we call write method multiple times, such that
     * we create 1 XML meta data file and then x CSV files, where x is the number of bushes */
    OutputType outputType = (OutputType) currentOutputType;
    BushLinkOutputTypeAdapter bushLinkOutputTypeAdapter =
        (BushLinkOutputTypeAdapter) outputAdapter.getOutputTypeAdapter(outputType);

    try {

      /* XML meta data - once across all bushes to minimise overhead */
      createAndPersistXmlMetaDataForTimePeriodCurrentIteration(
          outputTypeConfiguration, currentOutputType, outputAdapter, timePeriod);

      var bushes = bushLinkOutputTypeAdapter.getBushes();
      for(var bush : bushes) {
        if(bush==null){
          continue;
        }

        if(!bush.getRootZone().hasXmlId()){
          LOGGER.warning("Bush root zone has no XML id, reverting to internal id indicated by '*' suffix");
        }

        String bushRootIdStr = "_D" + (bush.getRootZone().hasXmlId() ?
            bush.getRootZone().getXmlId() : (bush.getRootZone().getId() + "*"));

        // create the name based on iteration, time period, related info AND bush root zone id
        String csvFileName = generateAbsoluteCsvFileName(
            csvDirectory,
            csvNameRoot,
            csvNameExtension,
            timePeriod,
            outputTypeConfiguration.getOutputType(),
            outputAdapter.getRunId(),
            iterationIndex,
            bushRootIdStr); // use root zone to identify each bush in file name

        // do work by persisting the current bush's edge segments
        createAndPersistCsvFileForTimePeriodCurrentIteration(
            csvFileName,
            outputTypeConfiguration,
            (csvPrinter) -> writeBushResultsForCurrentTimePeriodToCsvPrinter(
                outputConfiguration,
                outputTypeConfiguration,
                currentOutputType,
                outputAdapter,
                modes,
                timePeriod,
                bush,
                csvPrinter));

        // add metadata to the XML content
        String relativeCsvFileName = generateRelativeCsvOutputFileName(
            outputTypeConfiguration.getOutputType(), outputAdapter, timePeriod, iterationIndex);
        updateMetadataSimulationOutputForCurrentIteration(iterationIndex, relativeCsvFileName, currentOutputType);
        addCsvFileNamePerOutputType(currentOutputType, csvFileName);
      }
    } catch (PlanItException e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("PlanitException occurred when writing results for current time period in PLANitIO " +
          "OutputFormatter, verify file is not already open and/or sufficient permissions are available");
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException(
          "Error when writing results for current time period in PLANitIO OutputFormatter", e);
    }
  }

  /**
   * Constructor, takes values for properties file name, description and version property
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public PlanItOutputFormatter(final IdGroupingToken groupId){
    super(groupId);
    xmlNameRoot = DEFAULT_XML_NAME_PREFIX;
    xmlNameExtension = DEFAULT_XML_NAME_EXTENSION;
    xmlFileNameMap = new HashMap<>();
    resetXmlDirectory = false;
    xmlDirectory = null;
    csvNameRoot = DEFAULT_CSV_NAME_PREFIX;
    csvNameExtension = DEFAULT_CSV_NAME_EXTENSION;
    resetCsvDirectory = false;
    csvDirectory = null;
    metadata = new HashMap<>();
  }

  /**
   * Create the output directories and open the CSV writers
   * 
   * @param outputConfiguration OutputConfiguration of the assignment
   * @param runId the id of the traffic assignment to be saved
   */
  @Override
  public void initialiseBeforeSimulation(final OutputConfiguration outputConfiguration, long runId) {
    
    PlanItRunTimeException.throwIf(xmlDirectory == null,
            "No common output directory or XML output directory has been defined");
    PlanItRunTimeException.throwIf(csvDirectory == null,
            "No common output directory or CSV output directory has been defined");
    
    createOrOpenOutputDirectory(xmlDirectory, resetXmlDirectory);
    createOrOpenOutputDirectory(csvDirectory, resetCsvDirectory);
  }

  /**
   * Finalize the persistence after the simulation:
   * <ul>
   *   <li>generate the XML meta-data file(s)</li>
   *   <li>persist consolidated iteration information (if configured to consolidate) </li>
   *   <li>log info</li>
   * </ul>
   * 
   * @param outputConfiguration OutputTypeConfiguration of the assignment that have been activated
   * @param outputAdapter the outputAdapter
   * @param timePeriod the last time period used before simulation ended
   * @param iterationIndex the last iteration index of the last time period used before the simulation eneded
   */
  @Override
  public void finaliseAfterSimulation(
      final OutputConfiguration outputConfiguration, final OutputAdapter outputAdapter, TimePeriod timePeriod, int iterationIndex){

    try {

      for (OutputType outputType : outputConfiguration.getActivatedOutputTypes()) {

        /* persist any consolidated simulation data to file */
        if(outputType.equals(OutputType.SIMULATION) && isConsolidateSimulationOutput()){
          persistConsolidatedSimulationDataAfterTimePeriod(
                  (SimulationOutputTypeConfiguration) outputConfiguration.getOutputTypeConfiguration(OutputType.SIMULATION),
                  outputAdapter,
                  timePeriod,
                  iterationIndex,
                  true);
        }

        /* finalise XML meta data */
        finaliseXmlMetaFileAfterSimulation(outputType, outputConfiguration);
      }      
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException("Error when finalising after simulation in PLANitIO OutputFormatter", e);
    }
    
    logOutputInformation(outputAdapter);    
  }

  /**
   * Call this method to delete all existing files in the XML output directory
   * 
   * @throws PlanItException thrown if there is an error
   */
  public void resetXmlDirectory() throws PlanItException {
    resetXmlDirectory = true;
  }

  /**
   * Call this method to delete all existing files in the CSV output directory
   * 
   * @throws PlanItException thrown if there is an error
   */
  public void resetCsvDirectory() throws PlanItException {
    resetCsvDirectory = true;
  }

  /**
   * Set the output directory for XML output files
   * 
   * @param xmlDirectory directory for XML output files
   */
  public void setXmlDirectory(String xmlDirectory) {
    this.xmlDirectory = xmlDirectory;
  }

  /**
   * Returns whether the XML directory has been set
   * 
   * @return true if the XML directory has been set, false otherwise
   */
  public boolean isXmlDirectorySet() {
    return xmlDirectory != null;
  }

  /**
   * Returns whether the csv directory has been set
   * 
   * @return true if the csv directory has been set, false otherwise
   */
  public boolean isCsvDirectorySet() {
    return csvDirectory != null;
  }

  /**
   * Set the directory for CSV output files
   * 
   * @param csvDirectory directory for CSV output files
   */
  @Override
  public void setCsvDirectory(String csvDirectory) {
    this.csvDirectory = csvDirectory;
  }

  /**
   * Set the common directory
   * 
   * @param outputDirectory common output directory
   */
  public void setOutputDirectory(final String outputDirectory) {
    csvDirectory = outputDirectory;
    xmlDirectory = outputDirectory;
  }

  /**
   * Sets the extension of the XML output file
   * 
   * @param xmlNameExtension the extension of the XML output file
   */
  public void setXmlNameExtension(final String xmlNameExtension) {
    this.xmlNameExtension = xmlNameExtension;
  }

  /**
   * Sets the root name of the XML output file
   * 
   * @param xmlNameRoot root name of XML output file
   */
  public void setXmlNameRoot(final String xmlNameRoot) {
    this.xmlNameRoot = xmlNameRoot;
  }

  /**
   * Sets the extension of the CSV output file
   * 
   * @param csvNameExtension the extension of the CSV output file
   */
  public void setCsvNameExtension(final String csvNameExtension) {
    this.csvNameExtension = csvNameExtension;
  }

  /**
   * Returns the list of names of CSV output file for a specified output type
   * 
   * @param outputType the specified output type
   * @return the name of the output file
   */
  public List<String> getCsvFileName(final OutputType outputType) {
    return csvFileNameMap.get(outputType);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void setCsvNameRoot(final String csvNameRoot) {
    this.csvNameRoot = csvNameRoot;
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canHandleMultipleIterations() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getXmlFileName(final OutputType outputType) {
    return xmlFileNameMap.get(outputType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setXmlFileNamePerOutputType(final OutputType outputType, final String xmlFileName) {
    xmlFileNameMap.put(outputType, xmlFileName);
  }

  /** flag to indicate whether to consolidate all simulation data into a single file across iterations
   *
   * @return flag
   */
  public boolean isConsolidateSimulationOutput() {
    return consolidateSimulationOutput;
  }

  /** flag to indicate whether to consolidate all simulation data into a single file across iterations
   *
   * @param consolidateSimulationOutput flag to set
   */
  public void setConsolidateSimulationOutput(boolean consolidateSimulationOutput) {
    this.consolidateSimulationOutput = consolidateSimulationOutput;
  }
}