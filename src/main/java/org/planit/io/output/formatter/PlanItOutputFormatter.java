package org.planit.io.output.formatter;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.csv.CSVPrinter;
import org.planit.exceptions.PlanItException;
import org.planit.generated.XMLElementColumn;
import org.planit.generated.XMLElementColumns;
import org.planit.generated.XMLElementCsvdata;
import org.planit.generated.XMLElementIteration;
import org.planit.generated.XMLElementMetadata;
import org.planit.generated.XMLElementOutputConfiguration;
import org.planit.generated.XMLElementOutputTimePeriod;
import org.planit.generated.XMLElementSimulation;
import org.planit.io.xml.converter.EnumConverter;
import org.planit.io.xml.util.XmlUtils;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.configuration.OutputConfiguration;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.enums.OutputTypeEnum;
import org.planit.output.enums.SubOutputTypeEnum;
import org.planit.output.formatter.CsvFileOutputFormatter;
import org.planit.output.formatter.CsvTextFileOutputFormatter;
import org.planit.output.formatter.XmlTextFileOutputFormatter;
import org.planit.output.property.BaseOutputProperty;
import org.planit.time.TimePeriod;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.network.physical.Mode;

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

  /**
   * properties taken from PLANit main project resources which pass on the Maven
   * project properties.
   */
  private static final String DEFAULT_PROPERTIES_FILE_NAME = "application.properties";
  private static final String DEFAULT_DESCRIPTION_PROPERTY_NAME = "planit.description";
  private static final String DEFAULT_VERSION_PROPERTY_NAME = "planit.version";

  private static final String DEFAULT_XML_NAME_EXTENSION = ".xml";
  private static final String DEFAULT_XML_NAME_PREFIX = "XMLOutput";
  private static final String DEFAULT_CSV_NAME_EXTENSION = ".csv";
  private static final String DEFAULT_CSV_NAME_PREFIX = "CSVOutput";

  /**
   * The root directory to store the XML output files
   */
  private String xmlDirectory;

  /**
   * The root directory of the CSV output files
   */
  private String csvDirectory;

  /**
   * The extension of the XML output files
   */
  private String xmlNameExtension;

  /**
   * The root name of the XML output files
   */
  private String xmlNameRoot;

  /**
   * The extension of the CSV output files
   */
  private String csvNameExtension;

  /**
   * The root name of the CSV output files
   */
  private String csvNameRoot;

  /**
   * Map of XML output file names for each OutputType
   */
  private Map<OutputType, String> xmlFileNameMap;
  /**
   * Flag to indicate whether XML output directory should be cleared before the
   * run
   */
  private boolean resetXmlDirectory;

  /**
   * Flag to indicate whether the CSV output directory should be cleared before
   * the run
   */
  private boolean resetCsvDirectory;

  /**
   * Description property to be included in the output files
   */
  private String description;

  /**
   * Version property to be included in the output files
   */
  private String version;

  /**
   * Generated object for the metadata element in the output XML file
   */
  private Map<OutputTypeEnum, XMLElementMetadata> metadata;
 
  /** create the logging prefix to use for non assignment specific logging messages
   * @return loggingPrefix
   */
  private String createLoggingPrefix() {
    return LoggingUtils.createOutputFormatterPrefix(this.id);
  }
  
  /** create the logging prefix to use for assignment specific logging messages
   * @return loggingPrefix
   */
  private String createLoggingPrefix(long runId) {
    return LoggingUtils.createRunIdPrefix(runId) +createLoggingPrefix();
  }  
  
  /**
   * Generates the name of an output file using the relative path from XML to CSV files
   * 
   * @param outputType the OutputType of the output
   * @param outputAdapter outputAdapter
   * @param timePeriod the time period 
   * @param runId the id of the traffic assignment run
   * @param iteration current iteration
   * @return the name of the output file
   * @throws PlanItException thrown if the output directory cannot be opened
   */
  private String generateRelativeOutputFileName(OutputType outputType, OutputAdapter outputAdapter, TimePeriod timePeriod, int iteration)
      throws PlanItException {
    Path pathAbsolute = Paths.get(csvDirectory);
    Path pathBase = Paths.get(xmlDirectory);
    Path pathRelative = pathBase.relativize(pathAbsolute);
    String relativeCsvOutputDirectory = pathRelative.toString();
    relativeCsvOutputDirectory = relativeCsvOutputDirectory.equals("") ? "." : relativeCsvOutputDirectory;
    return generateOutputFileName(relativeCsvOutputDirectory, csvNameRoot, csvNameExtension, timePeriod, outputType, outputAdapter.getRunId(), iteration);
  }

  /**
   * Set the values of the version and description properties from a properties
   * file
   * 
   * @param propertiesFileName the name of the properties file
   * @param descriptionProperty the name of the description property used in the
   *          properties file
   * @param versionProperty the name of the version property used in the
   *          properties file
   * @throws PlanItException thrown if there is an error reading the properties
   *           file
   */
  private void setVersionAndDescription(String propertiesFileName, String descriptionProperty, String versionProperty)
      throws PlanItException {
    if (propertiesFileName == null) {
      LOGGER.info(this.createLoggingPrefix()+"no application properties file specified, version and description properties must be set from the code or will not be recorded");
      return;
    }
    try (InputStream input = PlanItOutputFormatter.class.getClassLoader().getResourceAsStream(propertiesFileName)) {

      if (input == null) {
        LOGGER.info(this.createLoggingPrefix()+"application properties " + propertiesFileName + " could not be found, version and description properties must be set from the code or will not be recorded.");
        return;
      }

      // load a properties file from class path, inside static method
      Properties prop = new Properties();
      prop.load(input);

      description = prop.getProperty(descriptionProperty);
      if (description == null) {
        LOGGER.info(this.createLoggingPrefix()+"description property could not be set from properties file " + propertiesFileName+ ", this must be set from the code or will not be recorded.");
      }
      version = prop.getProperty(versionProperty);
      if (version == null) {
        LOGGER.info(this.createLoggingPrefix()+"version property could not be set from properties file " + propertiesFileName+ ", this must be set from the code or will not be recorded.");
      }

    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when setting version and description in PLANitIO OutputFormatter", e);
    }
  }

  /**
   * Update the generated metadata simulation output object for the current
   * iteration
   * 
   * @param iterationIndex index of the current iteration
   * @param csvFileName name of CSV file used to store data for the current
   *          iteration
   * @param currentOutputType the (sub) output type of the data the CSV file is
   *          storing
   * @throws PlanItException
   */
  private void updateMetadataSimulationOutputForCurrentIteration(int iterationIndex, String csvFileName,
      OutputTypeEnum currentOutputType) throws PlanItException {
    XMLElementIteration iteration = new XMLElementIteration();
    iteration.setNr(BigInteger.valueOf(iterationIndex));
    XMLElementCsvdata csvdata = new XMLElementCsvdata();
    csvdata.setValue(csvFileName);
    iteration.getCsvdata().add(csvdata);
    if (currentOutputType instanceof OutputType) {
      csvdata.setType(((OutputType) currentOutputType).value());
      metadata.get((OutputType) currentOutputType).getSimulation().getIteration().add(iteration);
    } else if (currentOutputType instanceof SubOutputTypeEnum) {
      csvdata.setType(((SubOutputTypeEnum) currentOutputType).value());
      metadata.get((SubOutputTypeEnum) currentOutputType).getSimulation().getIteration().add(iteration);
    } else {
      throw new PlanItException("invalid output type provided when updating metadata simulation output for current iteration");
    }

  }

  /**
   * Generate time stamp for the current date and time
   * 
   * @return XMLGregorianCalendar with the current date and time
   * @throws DatatypeConfigurationException thrown if the time stamp cannot be
   *           created
   */
  private XMLGregorianCalendar getTimestamp() throws DatatypeConfigurationException {
    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
    return datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
  }

  /**
   * Create generated Columns object to be used in the XML output, based on user
   * selections
   * 
   * @param outputProperties sorted set of output properties to be included in the
   *          output
   * @return generated Columns object
   */
  private XMLElementColumns getGeneratedColumnsFromProperties(SortedSet<BaseOutputProperty> outputProperties)
      throws PlanItException {
    XMLElementColumns generatedColumns = new XMLElementColumns();
    for (BaseOutputProperty outputProperty : outputProperties) {
      XMLElementColumn generatedColumn = new XMLElementColumn();
      generatedColumn.setName(outputProperty.getName());
      generatedColumn.setUnits(EnumConverter.convertFromPlanItToXmlGeneratedUnits(outputProperty.getUnits()));
      generatedColumn.setType(EnumConverter.convertFromPlanItToXmlGeneratedType(outputProperty.getType()));
      generatedColumns.getColumn().add(generatedColumn);
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
  private XMLElementOutputConfiguration getOutputconfiguration(OutputAdapter outputAdapter, TimePeriod timePeriod) {
    XMLElementOutputConfiguration outputconfiguration = new XMLElementOutputConfiguration();
    outputconfiguration.setAssignment(outputAdapter.getAssignmentClassName());
    outputconfiguration.setPhysicalcost(outputAdapter.getPhysicalCostClassName());
    outputconfiguration.setVirtualcost(outputAdapter.getVirtualCostClassName());
    outputconfiguration.setSmoothing(outputAdapter.getSmoothingClassName());
    outputconfiguration.setGapfunction(outputAdapter.getGapFunctionClassName());
    outputconfiguration.setStopcriterion(outputAdapter.getStopCriterionClassName());
    XMLElementOutputTimePeriod timeperiod = new XMLElementOutputTimePeriod();
    long externalId = (long) timePeriod.getExternalId();
    timeperiod.setId(BigInteger.valueOf(externalId));
    timeperiod.setName(timePeriod.getDescription());
    outputconfiguration.setTimeperiod(timeperiod);
    return outputconfiguration;
  }

  /**
   * Creates the output file directory if it does not already exist.
   * 
   * @param outputDirectory the output file directory
   * @param resetDirectory if true, directory will be purged of previous contents
   * @throws PlanItException thrown if the directory cannot be opened or created
   */
  private void createOrOpenOutputDirectory(String outputDirectory, boolean resetDirectory) throws PlanItException {
    try {
      File directory = new File(outputDirectory);
      if (!directory.isDirectory()) {
        Files.createDirectories(directory.toPath());
      }
      if (resetDirectory) {
        purgeDirectory(outputDirectory);
      }
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when creating output directory in PLANitIO OutputFormatter", e);
    }
  }

  /**
   * Initialize the current metadata output object with data which is only written
   * once per time period
   * 
   * @param currentOutputType the current (sub)OutputType we're persisting
   * @param outputTypeConfiguration the OutputTypeConfiguration object containing
   *          the run information
   * @param outputAdapter the OutputAdapter object being used for the
   *          output
   * @param timePeriod current time period
   * @throws PlanItException thrown if there is an error writing the data to file
   */
  private void initializeMetadataObject(OutputTypeEnum currentOutputType,
      OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter, TimePeriod timePeriod)
      throws PlanItException {
    try {
      metadata.get(currentOutputType).setTimestamp(getTimestamp());
      if (version != null) {
        metadata.get(currentOutputType).setVersion(version);
      }
      if (description != null) {
        metadata.get(currentOutputType).setDescription(description);
      }
      XMLElementOutputConfiguration outputconfiguration = getOutputconfiguration(outputAdapter, timePeriod);
      metadata.get(currentOutputType).setOutputconfiguration(outputconfiguration);
      SortedSet<BaseOutputProperty> outputProperties = outputTypeConfiguration.getOutputProperties();
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
  private void purgeDirectory(File directory) {
    for (File file : directory.listFiles()) {
      if (file.isDirectory())
        purgeDirectory(file);
      file.delete();
    }
  }

  /**
   * Remove all files and sub-directories from a specified directory
   * 
   * @param directoryName name of the directory to be cleared
   */
  private void purgeDirectory(String directoryName) {
    File directory = new File(directoryName);
    purgeDirectory(directory);
  }

  /**
   * Create a CSV file with output content based on the current (sub) output type
   * 
   * @param outputTypeConfiguration the OutputTypeConfiguration object containing the run information
   * @param outputAdapter the outputAdapter
   * @param timePeriod current time period          
   * @param iterationIndex current iteration index
   * @param createCsvFileForCurrentIteration lambda function which records data
   *          specific to the CSV file for the
   *          current iteration
   * @throws PlanItException thrown if there is an error
   * @return name of CSV output file
   */
  private String createCsvFileNameAndFileForTimePeriodCurrentIteration(
      OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter, TimePeriod timePeriod, int iterationIndex,
      Function<CSVPrinter, PlanItException> createCsvFileForCurrentIteration) throws PlanItException {

    // create the name based on iteration, time period and related info
    // String csvFileName = generateOutputFileName(csvDirectory, csvNameRoot, csvNameExtension,
    // timePeriod, outputTypeConfiguration.getOutputType(), runId, iterationIndex);
    String csvFileName = generateOutputFileName(csvDirectory, csvNameRoot, csvNameExtension, timePeriod,
        outputTypeConfiguration.getOutputType(), outputAdapter.getRunId(), iterationIndex);
    try {
      // create the header (first line) of the file
      CSVPrinter csvIterationPrinter = openCsvFileAndWriteHeaders(outputTypeConfiguration, csvFileName);

      // create content by delegating to (sub) output type specific function
      PlanItException ple = createCsvFileForCurrentIteration.apply(csvIterationPrinter);
      if (ple != null) {
        throw ple;
      }
      csvIterationPrinter.close();
    }catch( PlanItException e) {
      throw e;
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when createing CSV file name and file in PLANitIO OutputFormatter", e);
    }

    return csvFileName;
  }

  /**
   * Write the results for the current mode and time period to file
   * 
   * @param outputTypeConfiguration the current output type configuration
   * @param currentOutputType the current (sub)OutputType we're
   *          persisting
   * @param outputAdapter the current output adapter
   * @param timePeriod the current time period
   * @param iterationIndex iterationIndex relevant for this data
   * @param createCsvFileForCurrentIteration lambda function which records data
   *          specific to the CSV file for the
   *          current iteration
   * @throws PlanItException thrown if there is an error
   */
  private void writeResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, TimePeriod timePeriod, int iterationIndex,
      Function<CSVPrinter, PlanItException> createCsvFileForCurrentIteration) throws PlanItException {
    try {
      OutputType outputType = outputTypeConfiguration.getOutputType();
      boolean isNewTimePeriod = ((!metadata.containsKey(currentOutputType)) || (metadata.get(currentOutputType)
          .getOutputconfiguration().getTimeperiod().getId().longValue() != timePeriod.getId()));
      if (isNewTimePeriod) {
        if (metadata.containsKey(currentOutputType)) {
          XmlUtils.generateXmlFileFromObject(metadata.get(currentOutputType), XMLElementMetadata.class,
              xmlFileNameMap.get(outputType));
        }
        metadata.put(currentOutputType, new XMLElementMetadata());
        XMLElementSimulation simulation = new XMLElementSimulation();
        metadata.get(currentOutputType).setSimulation(simulation);
        initializeMetadataObject(currentOutputType, outputTypeConfiguration, outputAdapter, timePeriod);
      }

      String csvFileName = createCsvFileNameAndFileForTimePeriodCurrentIteration(outputTypeConfiguration, outputAdapter, timePeriod, iterationIndex, createCsvFileForCurrentIteration);

      // add metadata to the XML content
      String relativeCsvFileName = generateRelativeOutputFileName(outputTypeConfiguration.getOutputType(), outputAdapter, timePeriod, iterationIndex);
      updateMetadataSimulationOutputForCurrentIteration(iterationIndex, relativeCsvFileName, currentOutputType);
      addCsvFileNamePerOutputType(currentOutputType, csvFileName);

      // MARK 6-1-2020: Why is this here and not immediately placed in the same if
      // that checks for a new period at the top of this method?
      if (isNewTimePeriod) {
        xmlFileNameMap.put(outputType, generateOutputFileName(xmlDirectory, xmlNameRoot, xmlNameExtension, timePeriod, outputType, outputAdapter.getRunId()));
      }
    } catch (PlanItException e) {
      throw e;
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when writing results for current time period in PLANitIO OutputFormatter", e);
    }
  }

  /**
   * Log information regarding the output to the log
   */
  private void logOutputInformation(OutputAdapter outputAdapter) {
    if (isXmlDirectorySet()) {
      LOGGER.info(this.createLoggingPrefix(outputAdapter.getRunId()) + "XML meta-data directory set: " + xmlDirectory);
    } else {
      LOGGER.info(this.createLoggingPrefix(outputAdapter.getRunId()) + "XML meta-data output directory unknown");
    }

    if (isCsvDirectorySet()) {
      LOGGER.info(this.createLoggingPrefix(outputAdapter.getRunId()) + "CSV result directory set: " + csvDirectory);
    } else {
      LOGGER.info(this.createLoggingPrefix(outputAdapter.getRunId()) + "CSV result directory unknown");
    }
  }

  /**
   * Write Simulation results for the current time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   * @throws PlanItException thrown if there is an error
   */
  @Override
  protected void writeSimulationResultsForCurrentTimePeriod(
      OutputConfiguration outputConfiguration, 
      OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, 
      OutputAdapter outputAdapter, 
      Set<Mode> modes, 
      TimePeriod timePeriod,
      int iterationIndex) throws PlanItException {
    
    LOGGER.info(this.createLoggingPrefix(outputAdapter.getRunId()) +"XML Output for OutputType SIMULATION has not been implemented yet.");
    
  }

  /**
   * Write General results for the current time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we
   *          are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   * @throws PlanItException thrown if there is an error
   */
  @Override
  protected void writeGeneralResultsForCurrentTimePeriod(
      OutputConfiguration outputConfiguration, 
      OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, 
      OutputAdapter outputAdapter, 
      Set<Mode> modes, 
      TimePeriod timePeriod,
      int iterationIndex) throws PlanItException {
    
    LOGGER.info(this.createLoggingPrefix(outputAdapter.getRunId()) +"XML Output for OutputType GENERAL has not been implemented yet.");
    
  }

  /**
   * Write Origin-Destination results for the time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we
   *          are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   * @throws PlanItException thrown if there is an error
   */
  @Override
  protected void writeOdResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod,
      int iterationIndex) throws PlanItException {
    writeResultsForCurrentTimePeriod(outputTypeConfiguration, currentOutputType, outputAdapter, timePeriod,
        iterationIndex, (csvPrinter) -> {
          return writeOdResultsForCurrentTimePeriodToCsvPrinter(outputConfiguration, outputTypeConfiguration, currentOutputType,
              outputAdapter, modes, timePeriod, csvPrinter);
        });
  }

  /**
   * Write Path results for the time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current  persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we
   *          are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   * @throws PlanItException thrown if there is an error
   */
  @Override
  protected void writePathResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod,
      int iterationIndex) throws PlanItException {
    writeResultsForCurrentTimePeriod(outputTypeConfiguration, currentOutputType, outputAdapter, timePeriod,
        iterationIndex, (csvPrinter) -> {
          return writePathResultsForCurrentTimePeriodToCsvPrinter(outputConfiguration, outputTypeConfiguration, currentOutputType,
              outputAdapter, modes, timePeriod, csvPrinter);
        });
  }

  /**
   * Write link results for the current time period to the CSV file
   * 
   * @param outputConfiguration output configuration
   * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
   * @param currentOutputType active OutputTypeEnum of the configuration we
   *          are persisting for (can be a SubOutputTypeEnum or an OutputType)
   * @param outputAdapter OutputAdapter for current persistence
   * @param modes Set of modes of travel
   * @param timePeriod current time period
   * @param iterationIndex current iteration index
   * @throws PlanItException thrown if there is an error
   */
  @Override
  protected void writeLinkResultsForCurrentTimePeriod(OutputConfiguration outputConfiguration, OutputTypeConfiguration outputTypeConfiguration,
      OutputTypeEnum currentOutputType, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod,
      int iterationIndex) throws PlanItException {
    writeResultsForCurrentTimePeriod(outputTypeConfiguration, currentOutputType, outputAdapter, timePeriod,
        iterationIndex, (csvPrinter) -> {
          return writeLinkResultsForCurrentTimePeriodToCsvPrinter(outputConfiguration, outputTypeConfiguration, currentOutputType,
              outputAdapter, modes, timePeriod, csvPrinter);
        });
  }

  /**
   * Constructor, uses default values for properties file name, description
   * property and version property
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public PlanItOutputFormatter(IdGroupingToken groupId) throws PlanItException {
    this(groupId, DEFAULT_PROPERTIES_FILE_NAME, DEFAULT_DESCRIPTION_PROPERTY_NAME, DEFAULT_VERSION_PROPERTY_NAME);
  }

  /**
   * Constructor, takes values for properties file name, description and version
   * property
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   * @param propertiesFileName the name of the application properties file
   * @param descriptionProperty the name of the description property
   * @param versionProperty the name of the version property
   * @throws PlanItException thrown if the application properties file exists but
   *           cannot be opened
   */
  public PlanItOutputFormatter(IdGroupingToken groupId, String propertiesFileName, String descriptionProperty, String versionProperty)
      throws PlanItException {
    super(groupId);
    xmlNameRoot = DEFAULT_XML_NAME_PREFIX;
    xmlNameExtension = DEFAULT_XML_NAME_EXTENSION;
    xmlFileNameMap = new HashMap<OutputType, String>();
    resetXmlDirectory = false;
    xmlDirectory = null;
    csvNameRoot = DEFAULT_CSV_NAME_PREFIX;
    csvNameExtension = DEFAULT_CSV_NAME_EXTENSION;
    resetCsvDirectory = false;
    csvDirectory = null;
    metadata = new HashMap<OutputTypeEnum, XMLElementMetadata>();
    setVersionAndDescription(propertiesFileName, descriptionProperty, versionProperty);
  }

  /**
   * Constructor, uses default values description property and version property
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   * @param propertiesFileName the name of the application properties file
   * @throws PlanItException thrown if the application properties file exists but
   *           cannot be opened
   */
  public PlanItOutputFormatter(IdGroupingToken groupId, String propertiesFileName) throws PlanItException {
    this(groupId, propertiesFileName, DEFAULT_DESCRIPTION_PROPERTY_NAME, DEFAULT_VERSION_PROPERTY_NAME);
  }

  /**
   * Create the output directories and open the CSV writers
   * 
   * @param outputTypeConfigurations OutputTypeConfiguration for the assignment to be saved
   * @param runId the id of the traffic assignment to be saved
   * @throws PlanItException thrown if there is an error or validation failure during set up of the output formatter
   */
  @Override
  public void initialiseBeforeSimulation(Map<OutputType, OutputTypeConfiguration> outputTypeConfigurations, long runId) throws PlanItException {
    
    PlanItException.throwIf(xmlDirectory == null, "No common output directory or XML output directory has been defined");
    PlanItException.throwIf(csvDirectory == null,"No common output directory or CSV output directory has been defined");
    
    createOrOpenOutputDirectory(xmlDirectory, resetXmlDirectory);
    createOrOpenOutputDirectory(csvDirectory, resetCsvDirectory);
  }

  /**
   * Finalize the persistence after the simulation. Here we generate the XML meta-data file(s)
   * *
   * 
   * @param outputTypeConfigurations OutputTypeConfigurations for the assignment that have been activated
   * @param outputAdapter the outputAdapter
   * @throws PlanItException thrown if there is an error closing a resource
   */
  @Override
  public void finaliseAfterSimulation(Map<OutputType, OutputTypeConfiguration> outputTypeConfigurations, OutputAdapter outputAdapter)
      throws PlanItException {
    try {
      for (Map.Entry<OutputType, OutputTypeConfiguration> entry : outputTypeConfigurations.entrySet()) {
        OutputType outputType = entry.getKey();
        OutputTypeConfiguration outputTypeConfiguration = entry.getValue();
        if (xmlFileNameMap.containsKey(outputType)) {
          String xmlFileName = xmlFileNameMap.get(outputType);
          if (metadata.containsKey(outputType)) {
            XmlUtils.generateXmlFileFromObject(metadata.get(outputType), XMLElementMetadata.class, xmlFileName);
          } else if (outputTypeConfiguration.hasActiveSubOutputTypes()) {
            Set<SubOutputTypeEnum> activeSubOutputTypes = outputTypeConfiguration.getActiveSubOutputTypes();
            for (SubOutputTypeEnum subOutputTypeEnum : activeSubOutputTypes) {
              XmlUtils.generateXmlFileFromObject(metadata.get(subOutputTypeEnum), XMLElementMetadata.class,
                  xmlFileName);
            }
          }
        }
      }      
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when finalising after simulation in PLANitIO OutputFormatter", e);
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
   * Returns whether the xml directory has been set
   * 
   * @return true if the xml directory has been set, false otherwise
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
  public void setOutputDirectory(String outputDirectory) {
    csvDirectory = outputDirectory;
    xmlDirectory = outputDirectory;
  }

  /**
   * Sets the extension of the XML output file
   * 
   * @param xmlNameExtension the extension of the XML output file
   */
  public void setXmlNameExtension(String xmlNameExtension) {
    this.xmlNameExtension = xmlNameExtension;
  }

  /**
   * Sets the root name of the XML output file
   * 
   * @param xmlNameRoot root name of XML output file
   */
  public void setXmlNameRoot(String xmlNameRoot) {
    this.xmlNameRoot = xmlNameRoot;
  }

  /**
   * Sets the root name of the CSV output file
   * 
   * @param csvNameRoot root name of CSV output file
   */
  @Override
  public void setCsvNameRoot(String csvNameRoot) {
    this.csvNameRoot = csvNameRoot;
  }

  /**
   * Sets the extension of the CSV output file
   * 
   * @param csvNameExtension the extension of the CSV output file
   */
  public void setCsvNameExtension(String csvNameExtension) {
    this.csvNameExtension = csvNameExtension;
  }

  /**
   * Allows the developer to set the output description property
   * 
   * @param description description to be included
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Collect the description property
   * 
   * @return description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the list of names of CSV output file for a specified output type
   * 
   * @param outputType the specified output type
   * @return the name of the output file
   */
  public List<String> getCsvFileName(OutputType outputType) {
    return csvFileNameMap.get(outputType);
  }

  /**
   * Flag to indicate whether an implementation can handle multiple iterations
   * 
   * If this returns false, acts as though
   * OutputConfiguration.setPersistOnlyFinalIteration() is set to true
   * 
   * @return flag to indicate whether the OutputFormatter can handle multiple
   *         iterations
   */
  @Override
  public boolean canHandleMultipleIterations() {
    return true;
  }

  /**
   * Returns the XML output file name for a specified output type
   * 
   * @param outputType the specified output type
   * @return the name of the output file
   */
  @Override
  public String getXmlFileName(OutputType outputType) {
    return xmlFileNameMap.get(outputType);
  }

  /**
   * Set the name of an XML output file for a specified output type
   * 
   * @param outputType the specified output type
   * @param xmlFileName the name of the output file to be added for the specified
   *          output type
   */
  @Override
  public void setXmlFileNamePerOutputType(OutputType outputType, String xmlFileName) {
    xmlFileNameMap.put(outputType, xmlFileName);
  }

}