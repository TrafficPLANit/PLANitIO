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

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.planit.exceptions.PlanItException;
import org.planit.generated.Columns;
import org.planit.generated.Iteration;
import org.planit.generated.Metadata;
import org.planit.generated.Outputconfiguration;
import org.planit.generated.Simulation;
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
	private String xmlOutputFileName;
	
	private String csvNameExtension;
	private String csvNameRoot;
	private String csvOutputDirectory;
	private String csvOutputFileName;
	private CSVPrinter csvPrinter;
	private List<Column> columns;

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
	 * Generates the name of an output file from the class properties.
	 * 
	 * This method also creates the output file directory if it does not already
	 * exist.
	 * 
	 * @return the name of the output file
	 * @throws PlanItException thrown if the output directory cannot be opened
	 */
	private String generateOutputFileName(String outputDirectory, String nameRoot, String nameExtension)
			throws PlanItException {
		try {
			File directory = new File(outputDirectory);
			if (!directory.isDirectory()) {
				Files.createDirectories(directory.toPath());
			}
			String[] files = directory.list();
			int pos = files.length + 1;
			String newFileName = outputDirectory + "\\" + nameRoot + pos + nameExtension;
			return newFileName;
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	@Override
	public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputTypeConfiguration outputTypeConfiguration)
			throws PlanItException {
		try {
			OutputAdapter outputAdapter = outputTypeConfiguration.getOutputAdapter();
			if (outputAdapter instanceof TraditionalStaticAssignmentLinkOutputAdapter) {
				TraditionalStaticAssignmentLinkOutputAdapter traditionalStaticAssignmentLinkOutputAdapter = (TraditionalStaticAssignmentLinkOutputAdapter) outputAdapter;
				writeResultsForCurrentTimePeriod(traditionalStaticAssignmentLinkOutputAdapter, modes, timePeriod);
			} else {
				throw new PlanItException("OutputAdapter is of class " + outputAdapter.getClass().getCanonicalName()
						+ " which has not been defined yet");
			}
			String	xmlOutputFileName = generateOutputFileName(xmlOutputDirectory, xmlNameRoot, xmlNameExtension);
			Metadata metadata = new Metadata();
			GregorianCalendar gregorianCalendar = new GregorianCalendar();
			DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
			XMLGregorianCalendar xmlGregorianCalendar = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
			metadata.setTimestamp(xmlGregorianCalendar);
			metadata.setVersion("0.0.1");
			metadata.setDescription("Description");
			Outputconfiguration outputconfiguration = new Outputconfiguration();
			outputconfiguration.setAssignment("Traditional Static Assignment");
			outputconfiguration.setPhysicalcost("BPR");
			outputconfiguration.setVirtualcost("Fixed");
			org.planit.generated.Timeperiod timeperiod = new org.planit.generated.Timeperiod();
			timeperiod.setId(BigInteger.valueOf(timePeriod.getId()));
			timeperiod.setName(timePeriod.getDescription());
			outputconfiguration.setTimeperiod(timeperiod);
			metadata.setOutputconfiguration(outputconfiguration);
			Simulation simulation = new Simulation();
			Iteration iteration = new Iteration();
			iteration.setNr(BigInteger.valueOf(1));
			iteration.setCsvdata(csvOutputFileName);
			simulation.setIteration(iteration);
			metadata.setSimulation(simulation);
			Columns generatedColumns = new Columns();
			for (Column column : columns) {
				org.planit.generated.Column generatedColumn = new org.planit.generated.Column();
				generatedColumn.setName(Column.getName(column));
				generatedColumn.setUnit(Column.getUnits(column));
				generatedColumn.setType(Column.getType(column));
				generatedColumns.getColumn().add(generatedColumn);
			}
			metadata.setColumns(generatedColumns);
			XmlUtils.generateXmlFileFromObject(metadata, Metadata.class, xmlOutputFileName);
		} catch (Exception e) {
			throw new PlanItException(e);
		}
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
			Set<Mode> modes, TimePeriod timePeriod) throws PlanItException {
		TransportNetwork transportNetwork = outputAdapter.getTransportNetwork();
		for (Mode mode : modes) {
			double[] modalNetworkSegmentCosts = outputAdapter.getModalNetworkSegmentCosts(mode);
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

	@Override
	public void open() throws PlanItException {
		if (xmlOutputFileName == null) {
			xmlOutputFileName = generateOutputFileName(xmlOutputDirectory, xmlNameRoot, xmlNameExtension);
		}
		if (csvOutputFileName == null) {
			csvOutputFileName = generateOutputFileName(csvOutputDirectory, csvNameRoot, csvNameExtension);
		}
		try {
			csvPrinter = new CSVPrinter(new FileWriter(csvOutputFileName), CSVFormat.EXCEL);
			csvPrinter.printRecord("Run Id", "Time Period Id", "Mode Id", "Start Node Id", "End Node Id", "Link Flow",
					"Capacity", "Length", "Speed", "Link Cost", "Cost to End Node");
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	@Override
	public void close() throws PlanItException {
		try {
			csvPrinter.close();
		} catch (Exception e) {
			throw new PlanItException(e);
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
	 * Set the output file name
	 * 
	 * @param outputFileName the XML output file name
	 */
	public void setXmlOutputFileName(String xmlOutputFileName) {
		this.xmlOutputFileName = xmlOutputFileName;
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

}
