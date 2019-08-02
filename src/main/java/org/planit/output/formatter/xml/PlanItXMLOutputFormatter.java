package org.planit.output.formatter.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.planit.exceptions.PlanItException;
import org.planit.generated.XMLElementColumn;
import org.planit.generated.XMLElementColumns;
import org.planit.generated.XMLElementIteration;
import org.planit.generated.XMLElementMetadata;
import org.planit.generated.XMLElementOutputConfiguration;
import org.planit.generated.XMLElementSimulation;
import org.planit.generated.XMLElementOutputTimePeriod;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.network.transport.TransportNetwork;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputAdapter;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.formatter.BaseOutputFormatter;
import org.planit.output.property.BaseOutputProperty;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.xml.util.XmlUtils;
import org.planit.xml.converter.EnumConverter;

/**
 * The default output formatter of PlanIt
 * 
 * @author markr
 *
 */
public class PlanItXMLOutputFormatter extends BaseOutputFormatter {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(PlanItXMLOutputFormatter.class.getName());

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
	private String xmlOutputDirectory;

	/**
	 * The root directory of the CSV output files
	 */
	private String csvOutputDirectory;

	/**
	 * The extension of the XML output files
	 */
	private String xmlNameExtension;

	/**
	 * The prefix name of the XML output files
	 */
	private String xmlNamePrefix;

	/**
	 * The extension of the CSV output files
	 */
	private String csvNameExtension;

	/**
	 * The prefix name of the CSV output files
	 */
	private String csvNamePrefix;

	/**
	 * Flag to indicate whether XML output directory should be cleared before the
	 * run
	 */
	private boolean resetXmlOutputDirectory;

	/**
	 * Flag to indicate whether the CSV output directory should be cleared before
	 * the run
	 */
	private boolean resetCsvOutputDirectory;

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
	private XMLElementMetadata metadata;

	/**
	 * Name of the XML output file
	 */
	private String xmlOutputFileName;

	/**
	 * Constructor, uses default values for properties file name, description
	 * property and version property
	 */
	public PlanItXMLOutputFormatter() throws PlanItException {
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
	public PlanItXMLOutputFormatter(String propertiesFileName, String descriptionProperty, String versionProperty)
			throws PlanItException {
		super();
		xmlNamePrefix = DEFAULT_XML_NAME_PREFIX;
		xmlNameExtension = DEFAULT_XML_NAME_EXTENSION;
		csvNamePrefix = DEFAULT_CSV_NAME_PREFIX;
		csvNameExtension = DEFAULT_CSV_NAME_EXTENSION;
		resetXmlOutputDirectory = false;
		resetCsvOutputDirectory = false;
		xmlOutputDirectory = null;
		csvOutputDirectory = null;
		setVersionAndDescription(propertiesFileName, descriptionProperty, versionProperty);
	}

	/**
	 * Constructor, uses default values description property and version property
	 * 
	 * @param propertiesFileName the name of the application properties file
	 * @throws PlanItException thrown if the application properties file exists but
	 *                         cannot be opened
	 */
	public PlanItXMLOutputFormatter(String propertiesFileName) throws PlanItException {
		this(propertiesFileName, DEFAULT_DESCRIPTION_PROPERTY_NAME, DEFAULT_VERSION_PROPERTY_NAME);
	}

	/**
	 * Persist the output data based on the passed in configuration and adapter
	 * (contained in the configuration)
	 * 
	 * @param timePeriod              TimePeriod for the assignment to be saved
	 * @param modes                   Set of modes for the assignment to be saved
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to
	 *                                be saved
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputTypeConfiguration outputTypeConfiguration)
			throws PlanItException {
		OutputAdapter outputAdapter = outputTypeConfiguration.getOutputAdapter();
		if (!(outputAdapter instanceof TraditionalStaticAssignmentLinkOutputAdapter)) {
			throw new PlanItException("OutputAdapter is of class " + outputAdapter.getClass().getCanonicalName()
					+ " which has not been defined yet");
		}
		try {
			TraditionalStaticAssignmentLinkOutputAdapter traditionalStaticAssignmentLinkOutputAdapter = (TraditionalStaticAssignmentLinkOutputAdapter) outputAdapter;
			boolean isNewTimePeriod = ((metadata == null)
					|| (metadata.getOutputconfiguration().getTimeperiod().getId().longValue() != timePeriod.getId()));
			if (isNewTimePeriod) {
				if (metadata != null) {
					XmlUtils.generateXmlFileFromObject(metadata, XMLElementMetadata.class, xmlOutputFileName);
				}
				metadata = new XMLElementMetadata();
				XMLElementSimulation simulation = new XMLElementSimulation();
				metadata.setSimulation(simulation);
				initializeMetadataObject(traditionalStaticAssignmentLinkOutputAdapter, timePeriod);
			}
			persistForTraditionalStaticAssignmentLinkOutputAdapter(timePeriod, modes,
					traditionalStaticAssignmentLinkOutputAdapter);
			if (isNewTimePeriod) {
				xmlOutputFileName = xmlOutputDirectory + "\\" + xmlNamePrefix + "_" + timePeriod.getDescription()
						+ xmlNameExtension;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new PlanItException(e);
		}
	}

	/**
	 * Create the output directories and open the CSV writers
	 * 
	 * @throws PlanItException thrown if there is an error or validation failure
	 *                         during set up of the output formatter
	 */
	@Override
	public void open() throws PlanItException {

		if (xmlOutputDirectory == null) {
			throw new PlanItException(
					"No common output directory or XML output directory has been defined in the code.");
		}
		if (csvOutputDirectory == null) {
			throw new PlanItException(
					"No common output directory or CSV output directory has been defined in the code.");
		}

		if (resetXmlOutputDirectory) {
			createOrOpenOutputDirectory(xmlOutputDirectory);
			File directory = new File(xmlOutputDirectory);
			purgeDirectory(directory);
		}

		if (resetCsvOutputDirectory) {
			createOrOpenOutputDirectory(csvOutputDirectory);
			File directory = new File(csvOutputDirectory);
			purgeDirectory(directory);
		}

		createOrOpenOutputDirectory(csvOutputDirectory);
	}

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
			LOGGER.info(
					"No application properties file specified, version and description properties must be set from the code or will not be recorded.");
			return;
		}
		try (InputStream input = PlanItXMLOutputFormatter.class.getClassLoader()
				.getResourceAsStream(propertiesFileName)) {

			if (input == null) {
				LOGGER.info("Application properties " + propertiesFileName
						+ " could not be found, version and description properties must be set from the code or will not be recorded.");
				return;
			}

			// load a properties file from class path, inside static method
			Properties prop = new Properties();
			prop.load(input);

			description = prop.getProperty(descriptionProperty);
			if (description == null) {
				LOGGER.info("Description property could not be set from properties file " + propertiesFileName
						+ ", this must be set from the code or will not be recorded.");
			}
			version = prop.getProperty(versionProperty);
			if (version == null) {
				LOGGER.info("Version property could not be set from properties file " + propertiesFileName
						+ ", this must be set from the code or will not be recorded.");
			}

		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Generates the name of an output file.
	 * 
	 * @param outputDirectory location output files are to be written
	 * @param nameRoot        root name of the output files
	 * @param nameExtension   extension of the output files
	 * @param iteration       current iteration
	 * @return the name of the output file
	 * @throws PlanItException thrown if the output directory cannot be opened
	 */
	private String generateOutputFileName(String outputDirectory, String nameRoot, TimePeriod timePeriod,
			String nameExtension, int iteration) throws PlanItException {
		try {
			String newFileName = outputDirectory + "\\" + nameRoot + "_" + timePeriod.getDescription() + "_" + iteration
					+ nameExtension;
			return newFileName;
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Persist the data for the current iteration using
	 * TraditionalStaticAssignmentLinkOutputAdapter
	 * 
	 * @param timePeriod    TimePeriod for the assignment to be saved
	 * @param modes         Set of modes for the assignment to be saved
	 * @param outputAdapter output adapter
	 * @throws PlanItException thrown if there is an error
	 */
	private void persistForTraditionalStaticAssignmentLinkOutputAdapter(TimePeriod timePeriod, Set<Mode> modes,
			TraditionalStaticAssignmentLinkOutputAdapter outputAdapter) throws PlanItException {
		int iterationIndex = outputAdapter.getIterationIndex();
		String csvFileName = generateOutputFileName(csvOutputDirectory, csvNamePrefix, timePeriod, csvNameExtension,
				iterationIndex);
		createCsvFileForCurrentIteration(outputAdapter, modes, csvFileName);
		updateGeneratedSimulationOutputForCurrentIteration(iterationIndex, csvFileName);
	}

	/**
	 * Update the generated simulation output object for the current iteration
	 * 
	 * @param iterationIndex index of the current iteration
	 * @param csvFileName    name of CSV file used to store data for the current
	 *                       iteration
	 */
	private void updateGeneratedSimulationOutputForCurrentIteration(int iterationIndex, String csvFileName) {
		XMLElementIteration iteration = new XMLElementIteration();
		iteration.setNr(BigInteger.valueOf(iterationIndex));
		iteration.setCsvdata(csvFileName);
		metadata.getSimulation().getIteration().add(iteration);
	}

	/**
	 * Create the CSV file for the current iteration
	 * 
	 * @param outputAdapter outputAdapter storing network
	 * @param csvFileName name of the CSV output file for the current iteration
	 * @throws PlanItException thrown if the CSV file cannot be created or written to
	 */
	private void createCsvFileForCurrentIteration(TraditionalStaticAssignmentLinkOutputAdapter outputAdapter,
			Set<Mode> modes, String csvFileName) throws PlanItException {

		try {
			CSVPrinter csvIterationPrinter = new CSVPrinter(new FileWriter(csvFileName), CSVFormat.EXCEL);
			List<String> titles = new ArrayList<String>();
			SortedSet<BaseOutputProperty> outputProperties = outputAdapter.getOutputProperties();
			outputProperties.forEach(outputProperty -> {
				titles.add(outputProperty.getName());
			});
			csvIterationPrinter.printRecord(titles);
			TransportNetwork transportNetwork = outputAdapter.getTransportNetwork();

			for (Mode mode : modes) {
				Iterator<LinkSegment> linkSegmentIter = transportNetwork.linkSegments.iterator();
				while (linkSegmentIter.hasNext()) {
					MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) linkSegmentIter.next();
					if (outputAdapter.isFlowPositive(linkSegment, mode)) {
						List<Object> row = new ArrayList<Object>();
						outputProperties.forEach(outputProperty -> {
							Object outValue = outputAdapter.getPropertyValue(outputProperty, linkSegment, mode);
							if (outValue instanceof Double) {
								double outDouble = (double) outValue;
								outValue = String.format("%.7f", outDouble);
							}
							row.add(outValue);
						});
						csvIterationPrinter.printRecord(row);
					}
				}
			}
			csvIterationPrinter.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new PlanItException(e);
		}
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
	 * @param outputAdapter the OutputAdapter containing the run information
	 * @param timePeriod    the current time period
	 * @return the XMLElementOutputConfiguration object
	 */
	private XMLElementOutputConfiguration getOutputconfiguration(OutputAdapter outputAdapter, TimePeriod timePeriod) {
		XMLElementOutputConfiguration outputconfiguration = new XMLElementOutputConfiguration();
		outputconfiguration.setAssignment(getClassName(outputAdapter.getTrafficAssignment()));
		outputconfiguration
				.setPhysicalcost(getClassName(outputAdapter.getTrafficAssignment().getDynamicPhysicalCost()));
		outputconfiguration.setVirtualcost(getClassName(outputAdapter.getTrafficAssignment().getVirtualCost()));
		XMLElementOutputTimePeriod timeperiod = new XMLElementOutputTimePeriod();
		timeperiod.setId(BigInteger.valueOf(timePeriod.getId()));
		timeperiod.setName(timePeriod.getDescription());
		outputconfiguration.setTimeperiod(timeperiod);
		return outputconfiguration;
	}

	/**
	 * Return the name of a Java object class as a short string
	 * 
	 * @param object the Java object
	 * @return the name of the object
	 */
	private String getClassName(Object object) {
		String name = object.getClass().getCanonicalName();
		String[] words = name.split("\\.");
		return words[words.length - 1];
	}

	/**
	 * Creates the output file directory if it does not already exist.
	 * 
	 * @param outputDirectory the output file directory
	 * @return number of files in this directory
	 * @throws PlanItException thrown if the directory cannot be opened or created
	 */
	private int createOrOpenOutputDirectory(String outputDirectory) throws PlanItException {
		try {
			File directory = new File(outputDirectory);
			if (!directory.isDirectory()) {
				Files.createDirectories(directory.toPath());
			}
			return directory.list().length;
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Initialize the current metadata output object with data which is only written
	 * once per time period
	 * 
	 * @param outputAdapter output adapter
	 * @param timePeriod    current time period
	 * @throws PlanItException thrown if there is an error writing the data to file
	 */
	private void initializeMetadataObject(TraditionalStaticAssignmentLinkOutputAdapter outputAdapter,
			TimePeriod timePeriod) throws PlanItException {
		try {
			metadata.setTimestamp(getTimestamp());
			if (version != null) {
				metadata.setVersion(version);
			}
			if (description != null) {
				metadata.setDescription(description);
			}
			metadata.setOutputconfiguration(getOutputconfiguration(outputAdapter, timePeriod));
			SortedSet<BaseOutputProperty> outputProperties = outputAdapter.getOutputProperties();
			metadata.setColumns(getGeneratedColumnsFromProperties(outputProperties));
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
	 * Close the CSV writer
	 * 
	 * @throws PlanItException thrown if there is an error closing a resource
	 */
	@Override
	public void close() throws PlanItException {
		try {
			XmlUtils.generateXmlFileFromObject(metadata, XMLElementMetadata.class, xmlOutputFileName);
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Call this method to delete all existing files in the XML output directory
	 * 
	 * @throws PlanItException
	 */
	public void resetXmlOutputDirectory() throws PlanItException {
		resetXmlOutputDirectory = true;
	}

	/**
	 * Call this method to delete all existing files in the CSV output directory
	 * 
	 * @throws PlanItException
	 */
	public void resetCsvOutputDirectory() throws PlanItException {
		resetCsvOutputDirectory = true;
	}

	/**
	 * Set the output directory for XML output files
	 * 
	 * @param xmlOutputDirectory directory for XML output files
	 */
	public void setXmlOutputDirectory(String xmlOutputDirectory) {
		this.xmlOutputDirectory = xmlOutputDirectory;
	}

	/**
	 * Set the directory for CSV output files
	 * 
	 * @param csvOutputDirectory directory for CSV output files
	 */
	public void setCsvOutputDirectory(String csvOutputDirectory) {
		this.csvOutputDirectory = csvOutputDirectory;
	}

	/**
	 * Set the common directory
	 * 
	 * @param outputDirectory common output directory
	 */
	public void setOutputDirectory(String outputDirectory) {
		csvOutputDirectory = outputDirectory;
		xmlOutputDirectory = outputDirectory;
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
	public void setXmlNamePrefix(String xmlNamePrefix) {
		this.xmlNamePrefix = xmlNamePrefix;
	}

	/**
	 * Sets the root name of the CSV output file
	 * 
	 * @param csvNamePrefix root name of CSV output file
	 */
	public void setCsvNamePrefix(String csvNamePrefix) {
		this.csvNamePrefix = csvNamePrefix;
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

}
