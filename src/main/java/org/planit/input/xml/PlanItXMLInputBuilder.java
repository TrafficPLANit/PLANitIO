package org.planit.input.xml;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongFunction;

import javax.annotation.Nonnull;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.demands.Demands;
import org.planit.event.CreatedProjectComponentEvent;
import org.planit.exceptions.PlanItException;
import org.planit.generated.XMLElementDemandConfiguration;
import org.planit.generated.XMLElementInfrastructure;
import org.planit.generated.XMLElementLinkConfiguration;
import org.planit.generated.XMLElementMacroscopicDemand;
import org.planit.generated.XMLElementMacroscopicNetwork;
import org.planit.generated.XMLElementMacroscopicZoning;
import org.planit.generated.XMLElementOdMatrix;
import org.planit.generated.XMLElementPLANit;
import org.planit.generated.XMLElementZones.Zone;
import org.planit.input.InputBuilderListener;
import org.planit.logging.PlanItLogger;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.PhysicalNetwork.Nodes;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Centroid;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.CostOutputProperty;
import org.planit.output.property.DownstreamNodeExternalIdOutputProperty;
import org.planit.output.property.LinkSegmentExternalIdOutputProperty;
import org.planit.output.property.LinkSegmentIdOutputProperty;
import org.planit.output.property.ModeExternalIdOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.UpstreamNodeExternalIdOutputProperty;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.xml.demands.ProcessConfiguration;
import org.planit.xml.demands.UpdateDemands;
import org.planit.xml.network.ProcessInfrastructure;
import org.planit.xml.network.ProcessLinkConfiguration;
import org.planit.xml.network.physical.macroscopic.MacroscopicLinkSegmentTypeXmlHelper;
import org.planit.xml.util.XmlUtils;
import org.planit.xml.zoning.UpdateZoning;
import org.planit.zoning.Zoning;

/**
 * Class which reads inputs from XML input files
 * 
 * @author gman6028
 *
 */
public class PlanItXMLInputBuilder extends InputBuilderListener {

	/**
	 * Generated object to store input network data
	 */
	private XMLElementMacroscopicNetwork macroscopicnetwork;

	/**
	 * Generated object to store demand input data
	 */
	private XMLElementMacroscopicDemand macroscopicdemand;

	/**
	 * Generated object to store zoning input data
	 */
	private XMLElementMacroscopicZoning macroscopiczoning;

	/**
	 * Records the number of centroids, which is derived from the zoning input data
	 * and then used when reading in the demands input
	 */
	private int noCentroids;

	/**
	 * Map of Mode by mode Id, which is read in from the network input and used by
	 * the demands input
	 */
	private Map<Integer, Mode> modeMap;

	/**
	 * Network nodes object, which is read in from the network input and used by the
	 * zoning input
	 */
	private Nodes nodes;

	/**
	 * Zoning zones object, which is read in from the zoning input and used by the
	 * demands input
	 */
	private Zoning.Zones zones;

	/**
	 * Link segments object, which is read in from the network input and used by the
	 * initial costs input
	 */
	private MacroscopicNetwork.LinkSegments linkSegments;

	/**
	 * Default extension for XML input files
	 */
	private static final String DEFAULT_XML_NAME_EXTENSION = ".xml";

	/**
	 * The default separator that is assumed when no separator is provided
	 */
	public static final String DEFAULT_SEPARATOR = ",";

	/**
	 * Default XSD files used to validate input XML files against
	 */
	private static final String NETWORK_XSD_FILE = "src\\main\\resources\\xsd\\macroscopicnetworkinput.xsd";
	private static final String ZONING_XSD_FILE = "src\\main\\resources\\xsd\\macroscopiczoninginput.xsd";
	private static final String DEMAND_XSD_FILE = "src\\main\\resources\\xsd\\macroscopicdemandinput.xsd";

	/**
	 * Populate the input objects from specified XML files
	 * 
	 * @param zoningXmlFileLocation  location of the zoning input XML file
	 * @param demandXmlFileLocation  location of the demand input XML file
	 * @param networkXmlFileLocation location of the network input XML file
	 * @throws PlanItException thrown if there is an error during reading the files
	 */
	private void createGeneratedClassesFromXmlLocations(String zoningXmlFileLocation, String demandXmlFileLocation,
			String networkXmlFileLocation) throws PlanItException {
		try {
			macroscopiczoning = (XMLElementMacroscopicZoning) XmlUtils
					.generateObjectFromXml(XMLElementMacroscopicZoning.class, zoningXmlFileLocation);
			macroscopicdemand = (XMLElementMacroscopicDemand) XmlUtils
					.generateObjectFromXml(XMLElementMacroscopicDemand.class, demandXmlFileLocation);
			macroscopicnetwork = (XMLElementMacroscopicNetwork) XmlUtils
					.generateObjectFromXml(XMLElementMacroscopicNetwork.class, networkXmlFileLocation);
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Read the input XML file(s)
	 * 
	 * This method first checks for a single file containing all three of network,
	 * demand and zoning inputs. If no single file is found, it then checks for
	 * three separate files, one for each type of input.
	 * 
	 * @param projectPath      the project path directory
	 * @param xmlNameExtension the extension of the files to search through
	 * @throws PlanItException thrown if not all of network, demand and zoning input
	 *                         data are available
	 */
	private void setInputFiles(String projectPath, String xmlNameExtension) throws PlanItException {
		String[] xmlFileNames = getXmlFileNames(projectPath, xmlNameExtension);
		if (setInputFilesSingleFile(xmlFileNames)) {
			return;
		}
		if (setInputFilesSeparateFiles(xmlFileNames)) {
			return;
		}
		throw new PlanItException("The directory " + projectPath
				+ " does not contain either one file with all the macroscopic inputs or a separate file for each of zoning, demand and network.");
	}

	/**
	 * Return an array of the names of all the input files in the project path
	 * directory
	 * 
	 * @param projectPath      the project path directory
	 * @param xmlNameExtension the extension of the files to search through
	 * @return array of names of files in the directory with the specified extension
	 * 
	 * @throws PlanItException thrown if no files with the specified extension can
	 *                         be found
	 */
	private String[] getXmlFileNames(String projectPath, String xmlNameExtension) throws PlanItException {
		File xmlFilesDirectory = new File(projectPath);
		if (!xmlFilesDirectory.isDirectory()) {
			throw new PlanItException(projectPath + " is not a valid directory.");
		}
		String[] fileNames = xmlFilesDirectory.list((d, name) -> name.endsWith(xmlNameExtension));
		if (fileNames.length == 0) {
			throw new PlanItException(
					"Directory " + projectPath + " contains no files with extension " + xmlNameExtension);
		}
		for (int i = 0; i < fileNames.length; i++) {
			fileNames[i] = projectPath + "\\" + fileNames[i];
		}
		return fileNames;
	}

	/**
	 * Checks if a single XML file containing all of network, demand and zoning
	 * inputs is available, and reads it if it is.
	 * 
	 * @param xmlFileNames array of names of XML files in the input directory
	 * @return true if a single file containing all the inputs has been found and
	 *         read, false otherwise
	 */
	private boolean setInputFilesSingleFile(String[] xmlFileNames) {
		for (int i = 0; i < xmlFileNames.length; i++) {
			try {
				XMLElementPLANit planit = (XMLElementPLANit) XmlUtils.generateObjectFromXml(XMLElementPLANit.class,
						xmlFileNames[i]);
				PlanItLogger.info("File " + xmlFileNames[i] + " provides the network, demands and zoning input data.");
				macroscopiczoning = planit.getMacroscopiczoning();
				macroscopicnetwork = planit.getMacroscopicnetwork();
				macroscopicdemand = planit.getMacroscopicdemand();
				return true;
			} catch (Exception e) {
			}
		}
		return false;
	}

	/**
	 * Populate the generated input objects from three separate XML files
	 * 
	 * @param xmlFileNames array of names of XML files in the input directory
	 * @return true if input demand, zoning and network file are found in
	 *         xmlFileNames, false otherwise
	 */
	private boolean setInputFilesSeparateFiles(String[] xmlFileNames) {
		boolean foundZoningFile = false;
		boolean foundNetworkFile = false;
		boolean foundDemandFile = false;
		for (int i = 0; i < xmlFileNames.length; i++) {
			if (foundZoningFile && foundDemandFile && foundNetworkFile) {
				PlanItLogger.info("File " + xmlFileNames[i] + " exists but was not parsed.");
			}
			if (!foundZoningFile) {
				try {
					macroscopiczoning = (XMLElementMacroscopicZoning) XmlUtils
							.generateObjectFromXml(XMLElementMacroscopicZoning.class, xmlFileNames[i]);
					PlanItLogger.info("File " + xmlFileNames[i] + " provides the zoning input data.");
					foundZoningFile = true;
					continue;
				} catch (Exception e) {
				}
			}
			if (!foundNetworkFile) {
				try {
					macroscopicnetwork = (XMLElementMacroscopicNetwork) XmlUtils
							.generateObjectFromXml(XMLElementMacroscopicNetwork.class, xmlFileNames[i]);
					PlanItLogger.info("File " + xmlFileNames[i] + " provides the network input data.");
					foundNetworkFile = true;
					continue;
				} catch (Exception e) {
				}
			}
			if (!foundDemandFile) {
				try {
					macroscopicdemand = (XMLElementMacroscopicDemand) XmlUtils
							.generateObjectFromXml(XMLElementMacroscopicDemand.class, xmlFileNames[i]);
					PlanItLogger.info("File " + xmlFileNames[i] + " provides the demand input data.");
					foundDemandFile = true;
					continue;
				} catch (Exception e) {
				}
			}
		}
		return (foundZoningFile && foundNetworkFile && foundDemandFile);
	}

	/**
	 * Populate the generated input objects from three separate XML files by
	 * validating the input files first.
	 * 
	 * This file does the same task as setInputFilesSeparateFiles(), it does it in a
	 * different way. This method runs much more slowly than
	 * setInputFilesSeparateFiles(), it takes about 60 times as long for the same
	 * input data sets.
	 * 
	 * @param projectPath the name of the project path directory
	 * @throws PlanItException thrown if one or more of the input objects could not
	 *                         be populated from the XML files in the project
	 *                         directory
	 */
	private void setInputFilesSeparateFilesWithValidation(String projectPath, String[] xmlFileNames)
			throws PlanItException {
		boolean foundZoningFile = false;
		String zoningFileName = null;
		boolean foundNetworkFile = false;
		String networkFileName = null;
		boolean foundDemandFile = false;
		String demandFileName = null;
		for (int i = 0; i < xmlFileNames.length; i++) {
			if (foundZoningFile && foundDemandFile && foundNetworkFile) {
				PlanItLogger.info("File " + xmlFileNames[i] + " exists but was not parsed.");
			}
			if (!foundZoningFile) {
				foundZoningFile = validateXmlInputFile(xmlFileNames[i], ZONING_XSD_FILE);
				if (foundZoningFile) {
					zoningFileName = xmlFileNames[i];
					PlanItLogger.info("File " + xmlFileNames[i] + " provides the zoning input data.");
				}
			}
			if (!foundNetworkFile) {
				foundNetworkFile = validateXmlInputFile(xmlFileNames[i], NETWORK_XSD_FILE);
				if (foundNetworkFile) {
					networkFileName = xmlFileNames[i];
					PlanItLogger.info("File " + xmlFileNames[i] + " provides the network input data.");
				}
			}
			if (!foundDemandFile) {
				foundDemandFile = validateXmlInputFile(xmlFileNames[i], DEMAND_XSD_FILE);
				if (foundDemandFile) {
					demandFileName = xmlFileNames[i];
					PlanItLogger.info("File " + xmlFileNames[i] + " provides the demand input data.");
				}
			}
		}
		if (!foundZoningFile) {
			throw new PlanItException(
					"Failed to find a valid zoning input file in the project directory " + projectPath);
		}
		if (!foundNetworkFile) {
			throw new PlanItException(
					"Failed to find a valid network input file in the project directory " + projectPath);
		}
		if (!foundDemandFile) {
			throw new PlanItException(
					"Failed to find a valid demand input file in the project directory " + projectPath);
		}
		createGeneratedClassesFromXmlLocations(zoningFileName, demandFileName, networkFileName);
	}

	/**
	 * Get the output property representing the identification method for links in
	 * the initial link cost input CSV file
	 * 
	 * @param headers set of headers used in the input file
	 * @return the identification method identified
	 * @throws PlanItException thrown if there is an error reading the file
	 */
	@SuppressWarnings("incomplete-switch")
	private OutputProperty getLinkIdentificationMethod(Set<String> headers) throws PlanItException {
		boolean linkSegmentExternalIdPresent = false;
		boolean linkSegmentIdPresent = false;
		boolean upstreamNodeExternalIdPresent = false;
		boolean downstreamNodeExternalIdPresent = false;
		boolean modeExternalIdPresent = false;
		boolean costPresent = false;
		for (String header : headers) {
			OutputProperty outputProperty = OutputProperty.fromHeaderName(header);
			switch (outputProperty) {
			case LINK_SEGMENT_EXTERNAL_ID:
				linkSegmentExternalIdPresent = true;
				break;
			case LINK_SEGMENT_ID:
				linkSegmentIdPresent = true;
				break;
			case MODE_EXTERNAL_ID:
				modeExternalIdPresent = true;
				break;
			case UPSTREAM_NODE_EXTERNAL_ID:
				upstreamNodeExternalIdPresent = true;
				break;
			case DOWNSTREAM_NODE_EXTERNAL_ID:
				downstreamNodeExternalIdPresent = true;
				break;
			case COST:
				costPresent = true;
			}
		}
		if (!costPresent) {
			throw new PlanItException("Cost column not present in initial link segment costs file");
		}
		if (!modeExternalIdPresent) {
			throw new PlanItException("Mode External Id not present in initial link segment costs file");
		}
		if (linkSegmentExternalIdPresent) {
			return OutputProperty.LINK_SEGMENT_EXTERNAL_ID;
		}
		if (linkSegmentIdPresent) {
			return OutputProperty.LINK_SEGMENT_ID;
		}
		if (upstreamNodeExternalIdPresent && downstreamNodeExternalIdPresent) {
			return OutputProperty.UPSTREAM_NODE_EXTERNAL_ID;
		}
		throw new PlanItException("Links not correctly identified in initial link segment costs file");
	}

	/**
	 * Set the initial link segment cost for the specified link segment using values
	 * in the CSV initial segment costs file
	 * 
	 * @param initialLinkSegmentCost the InitialLinkSegmentCost object to store the
	 *                               cost value
	 * @param record                 the record in the CSV input file to get the
	 *                               data value from
	 * @param linkSegment            the current link segment
	 */
	private void setInitialLinkSegmentCost(InitialLinkSegmentCost initialLinkSegmentCost, CSVRecord record,
			LinkSegment linkSegment) {
		long modeExternalId = Long.parseLong(record.get(ModeExternalIdOutputProperty.MODE_EXTERNAL_ID));
		Mode mode = Mode.getByExternalId(modeExternalId);
		double cost = Double.parseDouble(record.get(CostOutputProperty.COST));
		initialLinkSegmentCost.setSegmentCost(mode, linkSegment, cost);
	}

	/**
	 * Update the initial link segment cost object using the data from the CSV input
	 * file for the current record
	 * 
	 * @param initialLinkSegmentCost the InitialLinkSegmentCost object to be updated
	 * @param parser                 the CSVParser containing all CSV records
	 * @param record                 the current CSVRecord
	 * @param outputProperty         the OutputProperty corresponding to the column
	 *                               to be read from
	 * @param header                 the header specifying the column to be read
	 *                               from
	 * @param findLinkFunction       the function which finds the link segment for
	 *                               the current header
	 * @throws PlanItException thrown if no link segment is found
	 */
	private void updateInitialLinkSegmentCost(InitialLinkSegmentCost initialLinkSegmentCost, CSVParser parser,
			CSVRecord record, OutputProperty outputProperty, String header, LongFunction<LinkSegment> findLinkFunction)
			throws PlanItException {
		long id = Long.parseLong(record.get(header));
		LinkSegment linkSegment = findLinkFunction.apply(id);
		if (linkSegment == null) {
			throw new PlanItException("Failed to find link segment");
		}
		setInitialLinkSegmentCost(initialLinkSegmentCost, record, linkSegment);
	}

	/**
	 * Update the initial link segment cost object using the data from the CSV input
	 * file for the current record specified by start and end node external Id
	 * 
	 * @param initialLinkSegmentCost the InitialLinkSegmentCost object to be updated
	 * @param parser                 the CSVParser containing all CSV records
	 * @param record                 the current CSVRecord
	 * @param startOutputProperty    the OutputProperty corresponding to the column
	 *                               to the start node
	 * @param endOutputProperty      the OutputProperty corresponding to the column
	 *                               to the end node
	 * @param startHeader            the header specifying the start node column
	 * @param endHeader              the header specifying the end node column
	 * @throws PlanItException thrown if there is an error during searching for the
	 *                         link segment
	 */
	private void updateInitialLinkSegmentCostFromStartAndEndNodeExternalId(
			InitialLinkSegmentCost initialLinkSegmentCost, CSVParser parser, CSVRecord record,
			OutputProperty startOutputProperty, OutputProperty endOutputProperty, String startHeader, String endHeader)
			throws PlanItException {
		long upstreamNodeExternalId = Long.parseLong(record.get(startHeader));
		long downstreamNodeExternalId = Long.parseLong(record.get(endHeader));
		LinkSegment linkSegment = linkSegments.getLinkSegmentByStartAndEndNodeExternalId(upstreamNodeExternalId,
				downstreamNodeExternalId);
		if (linkSegment == null) {
			throw new PlanItException("Failed to find link segment");
		}
		setInitialLinkSegmentCost(initialLinkSegmentCost, record, linkSegment);
	}

	/**
	 * Creates the physical network object from the data in the input file
	 * 
	 * @param physicalNetwork the physical network object to be populated from the
	 *                        input data
	 * @throws PlanItException thrown if there is an error reading the input file
	 */
	protected void populatePhysicalNetwork(@Nonnull PhysicalNetwork physicalNetwork) throws PlanItException {

		PlanItLogger.info("Populating Network");

		MacroscopicNetwork network = (MacroscopicNetwork) physicalNetwork;
		try {
			XMLElementLinkConfiguration linkconfiguration = macroscopicnetwork.getLinkconfiguration();
			modeMap = ProcessLinkConfiguration.getModeMap(linkconfiguration);
			Map<Integer, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentTypeMap = ProcessLinkConfiguration
					.createLinkSegmentTypeMap(linkconfiguration, modeMap);
			XMLElementInfrastructure infrastructure = macroscopicnetwork.getInfrastructure();
			ProcessInfrastructure.registerNodes(infrastructure, network);
			ProcessInfrastructure.generateAndRegisterLinkSegments(infrastructure, network, linkSegmentTypeMap);
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
		nodes = network.nodes;
		linkSegments = network.linkSegments;
	}

	/**
	 * Creates the Zoning object and connectoids from the data in the input file
	 * 
	 * @param zoning the Zoning object to be populated from the input data
	 * @throws PlanItException thrown if there is an error reading the input file
	 */
	protected void populateZoning(Zoning zoning) throws PlanItException {
		PlanItLogger.info("Populating Zoning");
		if (nodes.getNumberOfNodes() == 0)
			throw new PlanItException("Cannot parse zoning input file before the network input file has been parsed.");
		noCentroids = 0;

		// create and register zones, centroids and connectoids
		try {
			for (Zone zone : macroscopiczoning.getZones().getZone()) {
				Centroid centroid = UpdateZoning.createAndRegisterZoneAndCentroid(zoning, zone);
				UpdateZoning.registerNewConnectoid(zoning, nodes, zone, centroid);
				noCentroids++;
			}
			zones = zoning.zones;
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Populates the Demands object from the input file
	 * 
	 * @param demands the Demands object to be populated from the input data
	 * @throws PlanItException thrown if there is an error reading the input file
	 */
	protected void populateDemands(@Nonnull Demands demands) throws PlanItException {
		PlanItLogger.info("Populating Demands");
		if (noCentroids == 0)
			throw new PlanItException("Cannot parse demand input file before zones input file has been parsed.");
		try {
			XMLElementDemandConfiguration demandconfiguration = macroscopicdemand.getDemandconfiguration();
			Map<Integer, TimePeriod> timePeriodMap = ProcessConfiguration
					.generateAndStoreConfigurationData(demandconfiguration);
			List<XMLElementOdMatrix> oddemands = macroscopicdemand.getOddemands()
					.getOdcellbycellmatrixOrOdrowmatrixOrOdrawmatrix();
			UpdateDemands.createAndRegisterDemandMatrix(demands, oddemands, modeMap, timePeriodMap, noCentroids, zones);
		} catch (Exception e) {
			throw new PlanItException(e);
		}
	}

	/**
	 * Populate the initial link segment cost from a CSV file
	 * 
	 * @param initialLinkSegmentCost InitialLinkSegmentCost object to be populated
	 * @param parameter              CSV file containing the initial link segment
	 *                               cost values
	 * @throws PlanItException
	 */
	protected void populateInitialLinkSegmentCost(InitialLinkSegmentCost initialLinkSegmentCost, Object parameter1)
			throws PlanItException {
		PlanItLogger.info("Populating Initial Link Segment Costs");
		String fileName = (String) parameter1;
		try {
			Reader in = new FileReader(fileName);
			CSVParser parser = CSVParser.parse(in, CSVFormat.DEFAULT.withFirstRecordAsHeader());
			Set<String> headers = parser.getHeaderMap().keySet();
			OutputProperty linkIdentificationMethod = getLinkIdentificationMethod(headers);
			for (CSVRecord record : parser) {
				switch (linkIdentificationMethod) {
				case LINK_SEGMENT_ID:
					updateInitialLinkSegmentCost(initialLinkSegmentCost, parser, record, OutputProperty.LINK_SEGMENT_ID,
							LinkSegmentIdOutputProperty.LINK_SEGMENT_ID, (id) -> {
								return linkSegments.getLinkSegment(id);
							});
					break;
				case LINK_SEGMENT_EXTERNAL_ID:
					updateInitialLinkSegmentCost(initialLinkSegmentCost, parser, record,
							OutputProperty.LINK_SEGMENT_EXTERNAL_ID,
							LinkSegmentExternalIdOutputProperty.LINK_SEGMENT_EXTERNAL_ID, (id) -> {
								return linkSegments.getLinkSegmentByExternalId(id);
							});
					break;
				case UPSTREAM_NODE_EXTERNAL_ID:
					updateInitialLinkSegmentCostFromStartAndEndNodeExternalId(initialLinkSegmentCost, parser, record,
							OutputProperty.UPSTREAM_NODE_EXTERNAL_ID, OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID,
							UpstreamNodeExternalIdOutputProperty.UPSTREAM_NODE_EXTERNAL_ID,
							DownstreamNodeExternalIdOutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID);
					break;
				default:
					throw new PlanItException("Invalid Output Property "
							+ BaseOutputProperty.convertToBaseOutputProperty(linkIdentificationMethod).getName()
							+ " found in header of Initial Link Segment Cost CSV file");
				}
			}
			in.close();
		} catch (Exception ex) {
			throw new PlanItException(ex);
		}
	}

	/**
	 * Constructor which generates the input objects from files in a specified
	 * directory, using the default extension ".xml"
	 * 
	 * @param projectPath the location of the input file directory
	 * @throws PlanItException thrown if one of the input required input files
	 *                         cannot be found, or if there is an error reading one
	 *                         of them
	 */
	public PlanItXMLInputBuilder(String projectPath) throws PlanItException {
		this(projectPath, DEFAULT_XML_NAME_EXTENSION);
	}

	/**
	 * Constructor which generates the input objects from files in a specified
	 * directory
	 * 
	 * @param projectPath      the location of the input file directory
	 * @param xmlNameExtension the extension of the data files to be searched
	 *                         through
	 * @throws PlanItException thrown if one of the input required input files
	 *                         cannot be found, or if there is an error reading one
	 *                         of them
	 */
	public PlanItXMLInputBuilder(String projectPath, String xmlNameExtension) throws PlanItException {
		setInputFiles(projectPath, xmlNameExtension);
	}

	/**
	 * Validates an input XML file against an XSD file
	 * 
	 * @param xmlFileLocation    input XML file
	 * @param schemaFileLocation XSD file to validate XML file against
	 * @return true if the file is valid, false otherwise
	 */
	public static boolean validateXmlInputFile(String xmlFileLocation, String schemaFileLocation) {
		try {
			XmlUtils.validateXml(xmlFileLocation, schemaFileLocation);
			return true;
		} catch (Exception e) {
			PlanItLogger.info(e.getMessage());
			return false;
		}
	}

	/**
	 * Whenever a project component is created this method will be invoked
	 * 
	 * @param event event containing the created (and empty) project component
	 * @throws PlanItException thrown if there is an error
	 */
	public void onCreateProjectComponent(CreatedProjectComponentEvent<?> event) throws PlanItException {
		Object projectComponent = event.getProjectComponent();
		if (projectComponent instanceof PhysicalNetwork) {
			populatePhysicalNetwork((PhysicalNetwork) projectComponent);
		} else if (projectComponent instanceof Zoning) {
			populateZoning((Zoning) projectComponent);
		} else if (projectComponent instanceof Demands) {
			populateDemands((Demands) projectComponent);
		} else if (projectComponent instanceof InitialPhysicalCost) {
			populateInitialLinkSegmentCost((InitialLinkSegmentCost) projectComponent, event.getParameter1());
		} else {
			PlanItLogger.info("Event component is " + projectComponent.getClass().getCanonicalName()
					+ " which is not handled by PlanItXMLInputBuilder.");
		}
	}

}
