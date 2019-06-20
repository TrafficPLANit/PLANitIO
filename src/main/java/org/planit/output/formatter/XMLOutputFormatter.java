package org.planit.output.formatter;

import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.planit.data.SimulationData;
import org.planit.data.TraditionalStaticAssignmentSimulationData;
import org.planit.exceptions.PlanItException;
import org.planit.generated.Columns;
import org.planit.generated.Iteration;
import org.planit.generated.Metadata;
import org.planit.generated.Outputconfiguration;
import org.planit.generated.Simulation;
import org.planit.generated.Timeperiod;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.Node;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.network.transport.TransportNetwork;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputAdapter;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.output.formatter.BaseOutputFormatter;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.xml.util.XmlUtils;
import org.planit.output.xml.Column;

/**
 * The default output formatter of PlanIt
 * 
 * @author markr
 *
 */
public class XMLOutputFormatter extends BaseOutputFormatter {

	private static final String DEFAULT_XML_NAME_EXTENSION = ".xml";
	private static final String DEFAULT_XML_NAME_ROOT = "XMLOutput";
	private static final String DEFAULT_XML_OUTPUT_DIRECTORY = "C:\\Users\\Public\\PlanIt\\Xml";
	private static final String DEFAULT_CSV_NAME_EXTENSION = ".csv";
	private static final String DEFAULT_CSV_NAME_ROOT = "CSVOutput";
	private static final String DEFAULT_CSV_OUTPUT_DIRECTORY = "C:\\Users\\Public\\PlanIt\\Csv";
	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(XMLOutputFormatter.class.getName());

	private String xmlNameExtension;
	private String xmlNameRoot;
	private String xmlOutputDirectory;
	private String currentXmlOutputDirectory;
	private String currentCsvOutputDirectory;

	private String csvNameExtension;
	private String csvNameRoot;
	private String csvOutputDirectory;
	private String csvOutputFileName;
	private CSVPrinter csvPrinter;
	private List<Column> columns;
	private String description;
	private String version;

	private static int directoryCounter = 0;

	/**
	 * Base constructor
	 */
	public XMLOutputFormatter() {
		xmlOutputDirectory = DEFAULT_XML_OUTPUT_DIRECTORY;
		xmlNameRoot = DEFAULT_XML_NAME_ROOT;
		xmlNameExtension = DEFAULT_XML_NAME_EXTENSION;
		csvOutputDirectory = DEFAULT_CSV_OUTPUT_DIRECTORY;
		csvNameRoot = DEFAULT_CSV_NAME_ROOT;
		csvNameExtension = DEFAULT_CSV_NAME_EXTENSION;
		csvOutputFileName = null;
		columns = new ArrayList<Column>();
	}

	public void addColumn(Column column) {
		columns.add(column);
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
	private String generateOutputFileName(String outputDirectory, String nameRoot, String nameExtension, int iteration)
			throws PlanItException {
		try {
			String newFileName = outputDirectory + "\\" + nameRoot + iteration + nameExtension;
			return newFileName;
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
	 * @return the name of the output file
	 * @throws PlanItException thrown if the output directory cannot be opened
	 */
	private String generateOutputFileName(String outputDirectory, String nameRoot, String nameExtension)
			throws PlanItException {
		try {
			String newFileName = outputDirectory + "\\" + nameRoot + nameExtension;
			return newFileName;
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Persist the output data based on the passed in configuration and adapter
	 * (contained in the configuration)
	 * 
	 * @param timePeriod              TimePeriod for the assignment to be saved
	 * @param modes                   Set of modes for the assignment to be saved
	 * @param outputTypeConfiguration OutputTypeConfiguration for the assignment to
	 *                                be saved
	 * @param simulationData          simulation data for the current iteration
	 * @throws PlanItException thrown if there is an error
	 */
	@Override
	public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputTypeConfiguration outputTypeConfiguration,
			SimulationData simulationData) throws PlanItException {
		OutputAdapter outputAdapter = outputTypeConfiguration.getOutputAdapter();
		if (outputAdapter instanceof TraditionalStaticAssignmentLinkOutputAdapter) {
			TraditionalStaticAssignmentLinkOutputAdapter traditionalStaticAssignmentLinkOutputAdapter = (TraditionalStaticAssignmentLinkOutputAdapter) outputAdapter;
			TraditionalStaticAssignmentSimulationData traditionalStaticAssignmentSimulationData = (TraditionalStaticAssignmentSimulationData) simulationData;
			persistForTraditionalStaticAssignmentLinkOutputAdapter(timePeriod, modes,
					traditionalStaticAssignmentLinkOutputAdapter, traditionalStaticAssignmentSimulationData);
		} else {
			throw new PlanItException("OutputAdapter is of class " + outputAdapter.getClass().getCanonicalName()
					+ " which has not been defined yet");
		}
	}

	/**
	 * Persist the data for the current iteration using
	 * TraditionalStaticAssignmentLinkOutputAdapter
	 * 
	 * @param timePeriod                                   TimePeriod for the
	 *                                                     assignment to be saved
	 * @param modes                                        Set of modes for the
	 *                                                     assignment to be saved
	 * @param traditionalStaticAssignmentLinkOutputAdapter output adapter
	 * @param simulationData                               simulation data for the
	 *                                                     current iteration
	 * @throws PlanItException thrown if there is an error
	 */
	private void persistForTraditionalStaticAssignmentLinkOutputAdapter(TimePeriod timePeriod, Set<Mode> modes,
			TraditionalStaticAssignmentLinkOutputAdapter traditionalStaticAssignmentLinkOutputAdapter,
			TraditionalStaticAssignmentSimulationData simulationData) throws PlanItException {
		try {
			int iterationIndex = simulationData.getIterationIndex();
			String csvFileName = generateOutputFileName(currentCsvOutputDirectory, csvNameRoot, csvNameExtension,
					iterationIndex);
			String currentXmlOutputFileName = generateOutputFileName(currentXmlOutputDirectory,
					xmlNameRoot + "_" + timePeriod.getDescription() + "_" + iterationIndex, xmlNameExtension);
			if (simulationData.isConverged()) {
				writeResultsForCurrentTimePeriod(traditionalStaticAssignmentLinkOutputAdapter, simulationData, modes,
						timePeriod);
			}
			Metadata metadata = new Metadata();

			metadata = new Metadata();
			metadata.setTimestamp(getTimestamp());
			if (version != null)
				metadata.setVersion(version);
			if (description != null)
				metadata.setDescription(description);
			metadata.setOutputconfiguration(
					getOutputconfiguration(traditionalStaticAssignmentLinkOutputAdapter, timePeriod));
			metadata.setSimulation(getSimulation(iterationIndex, csvFileName));
			metadata.setColumns(getColumns());

			createCsvFileForCurrentIteration(traditionalStaticAssignmentLinkOutputAdapter, simulationData, modes,
					csvFileName);
			XmlUtils.generateXmlFileFromObject(metadata, Metadata.class, currentXmlOutputFileName);

		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Create the CSV file for the current iteration
	 * 
	 * @param traditionalStaticAssignmentLinkOutputAdapter outputAdapter storing
	 *                                                     network
	 * @param traditionalStaticAssignmentSimulationData    simulation data for the
	 *                                                     current iteration
	 * @param csvFileName                                  name of the CSV output
	 *                                                     file for the current
	 *                                                     iteration
	 * @throws PlanItException thrown if the CSV file cannot be created or written
	 *                         to
	 */
	private void createCsvFileForCurrentIteration(TraditionalStaticAssignmentLinkOutputAdapter outputAdapter,
			TraditionalStaticAssignmentSimulationData simulationData, Set<Mode> modes, String csvFileName)
			throws PlanItException {

		try {
			CSVPrinter csvIterationPrinter = new CSVPrinter(new FileWriter(csvFileName), CSVFormat.EXCEL);
			List<String> titles = new ArrayList<String>();
			columns.forEach(column -> {
				titles.add(Column.getName(column));
			});
			csvIterationPrinter.printRecord(titles);
			TransportNetwork transportNetwork = outputAdapter.getTransportNetwork();

			for (Mode mode : modes) {
				double[] modalNetworkSegmentCosts = simulationData.getModalNetworkSegmentCosts(mode);
				double[] modalNetworkSegmentFlows = simulationData.getModalNetworkSegmentFlows(mode);

				Iterator<LinkSegment> linkSegmentIter = transportNetwork.linkSegments.iterator();
				while (linkSegmentIter.hasNext()) {
					MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) linkSegmentIter.next();
					int id = (int) linkSegment.getId();
					double flow = modalNetworkSegmentFlows[id];
					if (flow > 0.0) {
						double travelTime = modalNetworkSegmentCosts[id];
						double length = linkSegment.getParentLink().getLength();
						double speed = length / travelTime;
						Node startNode = (Node) linkSegment.getUpstreamVertex();
						Node endNode = (Node) linkSegment.getDownstreamVertex();
						List<Object> row = new ArrayList<Object>();
						for (Column column : columns) {
							switch (column) {
							case LINK_ID:
								row.add(id);
								break;
							case MODE_ID:
								row.add(mode.getExternalId());
								break;
							case SPEED:
								row.add(speed);
								break;
							case DENSITY:
								row.add(linkSegment.getLinkSegmentType().getMaximumDensityPerLane());
								break;
							case FLOW:
								row.add(flow);
								break;
							case TRAVEL_TIME:
								row.add(travelTime);
								break;
							case LENGTH:
								row.add(length);
								break;
							case START_NODE_ID:
								row.add(startNode.getExternalId());
								break;
							case END_NODE_ID:
								row.add(endNode.getExternalId());
								break;
							}
						}
						csvIterationPrinter.printRecord(row);
					}

				}
			}
			csvIterationPrinter.close();
		} catch (Exception e) {
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
	 * @return generated Columns object
	 */
	private Columns getColumns() {
		Columns generatedColumns = new Columns();
		for (Column column : columns) {
			org.planit.generated.Column generatedColumn = new org.planit.generated.Column();
			generatedColumn.setName(Column.getName(column));
			generatedColumn.setUnit(Column.getUnits(column));
			generatedColumn.setType(Column.getType(column));
			generatedColumns.getColumn().add(generatedColumn);
		}
		return generatedColumns;
	}

	/**
	 * Create the generated Simulation object to be used in the XML output
	 * 
	 * @param iterationIndex index of the current iteration
	 * @param csvFileName    name of the output CSV file for this iteration
	 * @return generated Simulation object
	 * @throws PlanItException
	 */
	private Simulation getSimulation(int iterationIndex, String csvFileName) throws PlanItException {
		Simulation simulation = new Simulation();
		Iteration iteration = new Iteration();
		iteration.setNr(BigInteger.valueOf(iterationIndex));
		iteration.setCsvdata(csvFileName);
		simulation.setIteration(iteration);
		return simulation;
	}

	/**
	 * Create the generated Outputconfiguration object
	 * 
	 * @param outputAdapter the OutputAdapter containing the run information
	 * @param timePeriod    the current time period
	 * @return the Outputconfiguration object
	 */
	private Outputconfiguration getOutputconfiguration(OutputAdapter outputAdapter, TimePeriod timePeriod) {
		Outputconfiguration outputconfiguration = new Outputconfiguration();
		outputconfiguration.setAssignment(getClassName(outputAdapter.getTrafficAssignment()));
		outputconfiguration.setPhysicalcost(getClassName(outputAdapter.getTrafficAssignment().getPhysicalCost()));
		outputconfiguration.setVirtualcost(getClassName(outputAdapter.getTrafficAssignment().getVirtualCost()));
		Timeperiod timeperiod = new Timeperiod();
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
	 * Write the results for the current time period to the CSV file
	 * 
	 * @param outputAdapter TraditionalStaticAssignmentLinkOutputAdapter used to
	 *                      retrieve the results of the assignment
	 * @param modes         Set of modes of travel
	 * @param timePeriod    the current time period
	 * @throws PlanItException thrown if there is an error
	 */
	private void writeResultsForCurrentTimePeriod(TraditionalStaticAssignmentLinkOutputAdapter outputAdapter,
			TraditionalStaticAssignmentSimulationData simulationData, Set<Mode> modes, TimePeriod timePeriod)
			throws PlanItException {
		TransportNetwork transportNetwork = outputAdapter.getTransportNetwork();
		for (Mode mode : modes) {
			// double[] modalNetworkSegmentCosts =
			// outputAdapter.getModalNetworkSegmentCosts(mode);
			double[] modalNetworkSegmentCosts = simulationData.getModalNetworkSegmentCosts(mode);
			double[] modalNetworkSegmentFlows = outputAdapter.getModalNetworkSegmentFlows(mode);
			writeResultsForCurrentModeAndTimePeriod(outputAdapter, mode, timePeriod, modalNetworkSegmentCosts,
					modalNetworkSegmentFlows, transportNetwork);
		}
	}

	/**
	 * Write results for the current mode and time period to the CSV file
	 * 
	 * @param outputAdapter            TraditionalStaticAssignmentLinkOutputAdapter
	 * @param mode                     current mode of travel
	 * @param timePeriod               current time period
	 * @param modalNetworkSegmentCosts calculated segment costs for the physical
	 *                                 network
	 * @param modalNetworkSegmentFlows calculated flows for the network
	 * @param transportNetwork         the transport network
	 * @throws PlanItException thrown if there is an error
	 */
	private void writeResultsForCurrentModeAndTimePeriod(TraditionalStaticAssignmentLinkOutputAdapter outputAdapter,
			Mode mode, TimePeriod timePeriod, double[] modalNetworkSegmentCosts, double[] modalNetworkSegmentFlows,
			TransportNetwork transportNetwork) throws PlanItException {
		try {
			double totalCost = 0.0;
			Iterator<LinkSegment> linkSegmentIter = transportNetwork.linkSegments.iterator();
			while (linkSegmentIter.hasNext()) {
				MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) linkSegmentIter.next();
				Node startNode = (Node) linkSegment.getUpstreamVertex();
				Node endNode = (Node) linkSegment.getDownstreamVertex();
				int id = (int) linkSegment.getId();
				double flow = modalNetworkSegmentFlows[id];
				if (flow > 0.0) {
					double cost = modalNetworkSegmentCosts[id];
					totalCost += flow * cost;
					long trafficAssignmentId = outputAdapter.getTrafficAssignmentId();
					csvPrinter.printRecord(trafficAssignmentId, timePeriod.getId(), mode.getExternalId(),
							startNode.getExternalId(), endNode.getExternalId(), flow,
							linkSegment.getLinkSegmentType().getCapacityPerLane() * linkSegment.getNumberOfLanes(),
							linkSegment.getParentLink().getLength(), linkSegment.getMaximumSpeed(mode.getExternalId()),
							cost, totalCost);
				}
			}
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Create the output directories and open the CSV writers
	 */
	@Override
	public void open() throws PlanItException {
		directoryCounter++;
		currentXmlOutputDirectory = xmlOutputDirectory + "\\" + directoryCounter;
		createOrOpenOutputDirectory(currentXmlOutputDirectory);

		currentCsvOutputDirectory = csvOutputDirectory + "\\" + directoryCounter;
		createOrOpenOutputDirectory(currentCsvOutputDirectory);

		if (csvOutputFileName == null) {
			int pos = createOrOpenOutputDirectory(csvOutputDirectory) + 1;
			csvOutputFileName = generateOutputFileName(csvOutputDirectory, csvNameRoot, csvNameExtension, pos);
		}
		try {
			csvPrinter = new CSVPrinter(new FileWriter(csvOutputFileName), CSVFormat.EXCEL);
			csvPrinter.printRecord("Run Id", "Time Period Id", "Mode Id", "Start Node Id", "End Node Id", "Link Flow",
					"Capacity", "Length", "Speed", "Link Cost", "Cost to End Node");
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Close the XML and CSV writers
	 */
	@Override
	public void close() throws PlanItException {
		try {
			csvPrinter.close();
		} catch (Exception e) {
			throw new PlanItException(e);
		}
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
	 * Call this method to delete all existing files in the XML output directory
	 * 
	 * @throws PlanItException
	 */
	public void resetXmlOutputDirectory() throws PlanItException {
		createOrOpenOutputDirectory(xmlOutputDirectory);
		File directory = new File(xmlOutputDirectory);
		purgeDirectory(directory);
	}

	/**
	 * Call this method to delete all existing files in the CSV output directory
	 * 
	 * @throws PlanItException
	 */
	public void resetCsvOutputDirectory() throws PlanItException {
		createOrOpenOutputDirectory(csvOutputDirectory);
		File directory = new File(csvOutputDirectory);
		purgeDirectory(directory);
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
	 * Sets the name of the XML output file directory
	 * 
	 * @param outputDirectory the name of the XML output file directory
	 */
	public void setXmlDirectory(String xmlOutputDirectory) {
		this.xmlOutputDirectory = xmlOutputDirectory;
	}

	/**
	 * Sets the extension of the XML output file
	 * 
	 * @param nameExtension the extension of the XML output file
	 */
	public void setXmlNameExtension(String xmlNameExtension) {
		this.xmlNameExtension = xmlNameExtension;
	}

	/**
	 * Sets the root name of the XML output file
	 * 
	 * @param nameRoot root name of XML output file
	 */
	public void setXmlNameRoot(String xmlNameRoot) {
		this.xmlNameRoot = xmlNameRoot;
	}

	/**
	 * Sets the name of the CSV output file directory
	 * 
	 * @param outputDirectory the name of the CSV output file directory
	 */
	public void setCsvDirectory(String csvOutputDirectory) {
		this.csvOutputDirectory = csvOutputDirectory;
	}

	/**
	 * Sets the extension of the CSV output file
	 * 
	 * @param nameExtension the extension of the CSV output file
	 */
	public void setCsvNameExtension(String csvNameExtension) {
		this.csvNameExtension = csvNameExtension;
	}

	/**
	 * Sets the root name of the CSV output file
	 * 
	 * @param nameRoot root name of CSV output file
	 */
	public void setCsvNameRoot(String csvNameRoot) {
		this.csvNameRoot = csvNameRoot;
	}

	/**
	 * Set the output file name
	 * 
	 * @param outputFileName the CSV output file name
	 */
	public void setCsvOutputFileName(String csvOutputFileName) {
		this.csvOutputFileName = csvOutputFileName;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
