package org.planit.planitio.output.formatter;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;

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
import org.planit.logging.PlanItLogger;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.enums.OutputType;
import org.planit.output.formatter.CsvFileOutputFormatter;
import org.planit.output.formatter.CsvTextFileOutputFormatter;
import org.planit.output.formatter.XmlTextFileOutputFormatter;
import org.planit.output.property.BaseOutputProperty;
import org.planit.planitio.xml.converter.EnumConverter;
import org.planit.planitio.xml.util.XmlUtils;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;

/**
 * The default output formatter of PlanIt
 * 
 * @author markr
 *
 */
public class PlanItOutputFormatter extends CsvFileOutputFormatter
		implements CsvTextFileOutputFormatter, XmlTextFileOutputFormatter {

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
	private Map<OutputType, XMLElementMetadata> metadata;

	/**
	 * The Id of the traffic assignment run being recorded
	 */
	private long runId;

	/**
	 * Set the values of the version and description properties from a properties
	 * file
	 * 
	 * @param propertiesFileName  the name of the properties file
	 * @param descriptionProperty the name of the description property used in the
	 *                            properties file
	 * @param versionProperty     the name of the version property used in the
	 *                            properties file
	 * @throws PlanItException thrown if there is an error reading the properties
	 *                         file
	 */
	private void setVersionAndDescription(String propertiesFileName, String descriptionProperty, String versionProperty)
			throws PlanItException {
		if (propertiesFileName == null) {
			PlanItLogger.info(
					"No application properties file specified, version and description properties must be set from the code or will not be recorded.");
			return;
		}
		try (InputStream input = PlanItOutputFormatter.class.getClassLoader()
				.getResourceAsStream(propertiesFileName)) {

			if (input == null) {
				PlanItLogger.info("Application properties " + propertiesFileName
						+ " could not be found, version and description properties must be set from the code or will not be recorded.");
				return;
			}

			// load a properties file from class path, inside static method
			Properties prop = new Properties();
			prop.load(input);

			description = prop.getProperty(descriptionProperty);
			if (description == null) {
				PlanItLogger.info("Description property could not be set from properties file " + propertiesFileName
						+ ", this must be set from the code or will not be recorded.");
			}
			version = prop.getProperty(versionProperty);
			if (version == null) {
				PlanItLogger.info("Version property could not be set from properties file " + propertiesFileName
						+ ", this must be set from the code or will not be recorded.");
			}

		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Update the generated metadata simulation output object for the current
	 * iteration
	 * 
	 * @param iterationIndex index of the current iteration
	 * @param csvFileName    name of CSV file used to store data for the current
	 *                       iteration
	 * @param outputType     the output type of the data the CSV file is storing
	 */
	private void updateMetadataSimulationOutputForCurrentIteration(int iterationIndex, String csvFileName,
			OutputType outputType) {
		XMLElementIteration iteration = new XMLElementIteration();
		iteration.setNr(BigInteger.valueOf(iterationIndex));
		XMLElementCsvdata csvdata = new XMLElementCsvdata();
		csvdata.setValue(csvFileName);
		csvdata.setType(outputType.value());
		iteration.getCsvdata().add(csvdata);
		metadata.get(outputType).getSimulation().getIteration().add(iteration);
	}

	/**
	 * Generate time stamp for the current date and time
	 * 
	 * @return XMLGregorianCalendar with the current date and time
	 * @throws DatatypeConfigurationException thrown if the time stamp cannot be
	 *                                        created
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
	 *                         output
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
	 * @param timePeriod    the current time period
	 * @return the XMLElementOutputConfiguration object
	 */
	private XMLElementOutputConfiguration getOutputconfiguration(OutputAdapter outputAdapter, TimePeriod timePeriod) {
		XMLElementOutputConfiguration outputconfiguration = new XMLElementOutputConfiguration();
		outputconfiguration.setAssignment(outputAdapter.getAssignmentClassName());
		outputconfiguration.setPhysicalcost(outputAdapter.getPhysicalCostClassName());
		outputconfiguration.setVirtualcost(outputAdapter.getVirtualCostClassName());
		XMLElementOutputTimePeriod timeperiod = new XMLElementOutputTimePeriod();
		timeperiod.setId(BigInteger.valueOf(timePeriod.getId()));
		timeperiod.setName(timePeriod.getDescription());
		outputconfiguration.setTimeperiod(timeperiod);
		return outputconfiguration;
	}

	/**
	 * Creates the output file directory if it does not already exist.
	 * 
	 * @param outputDirectory the output file directory
	 * @param resetDirectory  if true, directory will be purged of previous contents
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
			throw new PlanItException(e);
		}
	}

	/**
	 * Initialize the current metadata output object with data which is only written
	 * once per time period
	 * 
	 * @param outputTypeConfiguration the OutputTypeConfiguration object containing the run information
	 * @param outputAdapter the OutputAdapter object being used for the output
	 * @param timePeriod    current time period
	 * @throws PlanItException thrown if there is an error writing the data to file
	 */
	private void initializeMetadataObject(OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter, TimePeriod timePeriod) throws PlanItException {
		OutputType outputType = outputTypeConfiguration.getOutputType();
		try {
			metadata.get(outputType).setTimestamp(getTimestamp());
			if (version != null) {
				metadata.get(outputType).setVersion(version);
			}
			if (description != null) {
				metadata.get(outputType).setDescription(description);
			}
			metadata.get(outputType).setOutputconfiguration(getOutputconfiguration(outputAdapter, timePeriod));
			SortedSet<BaseOutputProperty> outputProperties =  outputTypeConfiguration.getOutputProperties();
			metadata.get(outputType).setColumns(getGeneratedColumnsFromProperties(outputProperties));
		} catch (Exception e) {
			throw new PlanItException(e);
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
	 * Write the results for the current mode and time period to file
	 * 
	 * @param outputTypeConfiguration          the current output type configuration 
	 * @param outputAdapter  the current output adapter
	 * @param modes                            Set of modes
	 * @param timePeriod                       the current time period
	 * @param createCsvFileForCurrentIteration lambda function which records data
	 *                                         specific to the CSV file for the
	 *                                         current iteration
	 * @throws PlanItException thrown if there is an error
	 */
	private void writeResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration,	OutputAdapter outputAdapter, TimePeriod timePeriod, Function<CSVPrinter, PlanItException> createCsvFileForCurrentIteration) throws PlanItException {
		try {
			OutputType outputType = outputTypeConfiguration.getOutputType();
			boolean isNewTimePeriod = ((!metadata.containsKey(outputType)) || (metadata.get(outputType).getOutputconfiguration().getTimeperiod().getId().longValue() != timePeriod.getId()));
			if (isNewTimePeriod) {
				if (metadata.containsKey(outputType)) {
					XmlUtils.generateXmlFileFromObject(metadata.get(outputType), XMLElementMetadata.class, xmlFileNameMap.get(outputType));
				}
				metadata.put(outputType, new XMLElementMetadata());
				XMLElementSimulation simulation = new XMLElementSimulation();
				metadata.get(outputType).setSimulation(simulation);
				initializeMetadataObject(outputTypeConfiguration, outputAdapter, timePeriod);
			}
			int iterationIndex = outputAdapter.getIterationIndex();
			String csvFileName = generateOutputFileName(csvDirectory, csvNameRoot, csvNameExtension, timePeriod,	outputType, runId, iterationIndex);
			CSVPrinter csvIterationPrinter = openCsvFileAndWriteHeaders(outputTypeConfiguration, csvFileName);
			PlanItException ple = createCsvFileForCurrentIteration.apply(csvIterationPrinter);
			if (ple != null) {
				throw ple;
			}
			csvIterationPrinter.close();
			updateMetadataSimulationOutputForCurrentIteration(iterationIndex, csvFileName, outputType);
			addCsvFileNamePerOutputType(outputType, csvFileName);
			if (isNewTimePeriod) {
				xmlFileNameMap.put(outputType, generateOutputFileName(xmlDirectory, xmlNameRoot, xmlNameExtension, timePeriod, outputType, runId));
			}
		} catch (PlanItException pe) {
			throw pe;
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Write Simulation results for the current time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
	 * @param outputAdapter OutputTypeAdapter for current persistence
	 * @param modes                   Set of modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	protected void writeSimulationResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter,
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException {
		PlanItLogger.info("XML Output for OutputType SIMULATION has not been implemented yet.");
	}

	/**
	 * Write General results for the current time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
	 * @param outputAdapter OutputAdapter for current persistence
	 * @param modes                   Set of current modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	protected void writeGeneralResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod) throws PlanItException {
		PlanItLogger.info("XML Output for OutputType GENERAL has not been implemented yet.");
	}

	/**
	 * Write Origin-Destination results for the time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
	 * @param outputAdapter OutputAdapter for current persistence
	 * @param modes                   Set of modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	protected void writeOdResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod) throws PlanItException {
		writeResultsForCurrentTimePeriod(outputTypeConfiguration, outputAdapter, timePeriod, (csvPrinter) -> {
			return writeOdResultsForCurrentTimePeriodToCsvPrinter(outputTypeConfiguration, outputAdapter, modes, timePeriod, csvPrinter);
		});
	}

	/**
	 * Write OD Path results for the time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
	 * @param outputAdapter OutputAdapter for current persistence
	 * @param modes                   Set of modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	protected void writeODPathResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod) throws PlanItException {
		writeResultsForCurrentTimePeriod(outputTypeConfiguration, outputAdapter, timePeriod, (csvPrinter) -> {
			return writeODPathResultsForCurrentTimePeriodToCsvPrinter(outputTypeConfiguration, outputAdapter, modes, timePeriod, csvPrinter);
		});
	}

	/**
	 * Write link results for the current time period to the CSV file
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for current persistence
	 * @param outputAdapter OutputAdapter for current persistence
	 * @param modes                   Set of modes of travel
	 * @param timePeriod              current time period
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	protected void writeLinkResultsForCurrentTimePeriod(OutputTypeConfiguration outputTypeConfiguration, OutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod) throws PlanItException {
		writeResultsForCurrentTimePeriod(outputTypeConfiguration, outputAdapter, timePeriod, (csvPrinter) -> {
			return writeLinkResultsForCurrentTimePeriodToCsvPrinter(outputTypeConfiguration, outputAdapter, modes, timePeriod,	csvPrinter);
		});
	}

	/**
	 * Constructor, uses default values for properties file name, description
	 * property and version property
	 */
	public PlanItOutputFormatter() throws PlanItException {
		this(DEFAULT_PROPERTIES_FILE_NAME, DEFAULT_DESCRIPTION_PROPERTY_NAME, DEFAULT_VERSION_PROPERTY_NAME);
	}

	/**
	 * Constructor, takes values for properties file name, description and version
	 * property
	 * 
	 * @param propertiesFileName  the name of the application properties file
	 * @param descriptionProperty the name of the description property
	 * @param versionProperty     the name of the version property
	 * @throws PlanItException thrown if the application properties file exists but
	 *                         cannot be opened
	 */
	public PlanItOutputFormatter(String propertiesFileName, String descriptionProperty, String versionProperty)
			throws PlanItException {
		super();
		xmlNameRoot = DEFAULT_XML_NAME_PREFIX;
		xmlNameExtension = DEFAULT_XML_NAME_EXTENSION;
		xmlFileNameMap = new HashMap<OutputType, String>();
		resetXmlDirectory = false;
		xmlDirectory = null;
		csvNameRoot = DEFAULT_CSV_NAME_PREFIX;
		csvNameExtension = DEFAULT_CSV_NAME_EXTENSION;
		resetCsvDirectory = false;
		csvDirectory = null;
		metadata = new HashMap<OutputType, XMLElementMetadata>();
		setVersionAndDescription(propertiesFileName, descriptionProperty, versionProperty);
	}

	/**
	 * Constructor, uses default values description property and version property
	 * 
	 * @param propertiesFileName the name of the application properties file
	 * @throws PlanItException thrown if the application properties file exists but
	 *                         cannot be opened
	 */
	public PlanItOutputFormatter(String propertiesFileName) throws PlanItException {
		this(propertiesFileName, DEFAULT_DESCRIPTION_PROPERTY_NAME, DEFAULT_VERSION_PROPERTY_NAME);
	}

	/**
	 * Create the output directories and open the CSV writers
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to be saved
	 * @param runId                   the id of the traffic assignment to be saved
	 * @throws PlanItException thrown if there is an error or validation failure
	 *                         during set up of the output formatter
	 */
	@Override
	public void open(OutputTypeConfiguration outputTypeConfiguration, long runId) throws PlanItException {

		this.runId = runId;
		if (xmlDirectory == null) {
			throw new PlanItException(
					"No common output directory or XML output directory has been defined in the code.");
		}
		if (csvDirectory == null) {
			throw new PlanItException(
					"No common output directory or CSV output directory has been defined in the code.");
		}

		createOrOpenOutputDirectory(xmlDirectory, resetXmlDirectory);
		createOrOpenOutputDirectory(csvDirectory, resetCsvDirectory);

	}

	/**
	 * Close the CSV writer
	 * 
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to
	 *                                be saved
	 * @throws PlanItException thrown if there is an error closing a resource
	 */
	@Override
	public void close(OutputTypeConfiguration outputTypeConfiguration) throws PlanItException {
		try {
			OutputType outputType = outputTypeConfiguration.getOutputType();
			if (xmlFileNameMap.containsKey(outputType)) {
				String xmlFileName = xmlFileNameMap.get(outputType);
				XmlUtils.generateXmlFileFromObject(metadata.get(outputType), XMLElementMetadata.class, xmlFileName);
			}
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Call this method to delete all existing files in the XML output directory
	 * 
	 * @throws PlanItException
	 */
	public void resetXmlDirectory() throws PlanItException {
		resetXmlDirectory = true;
	}

	/**
	 * Call this method to delete all existing files in the CSV output directory
	 * 
	 * @throws PlanItException
	 */
	public void resetCsvDirectory() throws PlanItException {
		resetCsvDirectory = true;
	}

	/**
	 * Set the output directory for XML output files
	 * 
	 * @param xmlOutputDirectory directory for XML output files
	 */
	public void setXmlDirectory(String xmlDirectory) {
		this.xmlDirectory = xmlDirectory;
	}

	/**
	 * Set the directory for CSV output files
	 * 
	 * @param csvOutputDirectory directory for CSV output files
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
	 * @param xmlNamePrefix root name of XML output file
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
	 * Allows the developer to set version output property
	 * 
	 * @param version version to be included
	 */
	public void setVersion(String version) {
		this.version = version;
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
	 * @param outputType  the specified output type
	 * @param xmlFileName the name of the output file to be added for the specified
	 *                    output type
	 */
	@Override
	public void setXmlFileNamePerOutputType(OutputType outputType, String xmlFileName) {
		xmlFileNameMap.put(outputType, xmlFileName);
	}

}