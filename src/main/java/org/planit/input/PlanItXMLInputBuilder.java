package org.planit.input;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.cost.physical.PhysicalCost;
import org.planit.cost.virtual.VirtualCost;
import org.planit.demand.Demands;
import org.planit.event.CreatedProjectComponentEvent;
import org.planit.event.listener.InputBuilderListener;
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
import org.planit.network.physical.PhysicalNetwork.Nodes;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Centroid;
import org.planit.sdinteraction.smoothing.Smoothing;
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
public class PlanItXMLInputBuilder implements InputBuilderListener {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(PlanItXMLInputBuilder.class.getName());

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
	 * Default extension for XML input files
	 */
	private static final String DEFAULT_XML_NAME_EXTENSION = ".xml";

	/**
	 * The default separator that is assumed when no separator is provided
	 */
	public static final String DEFAULT_SEPARATOR = ",";

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
	 * This method first checks for a single file containing all three of network, demand and zoning inputs.  If
	 * no single file is found, it then checks for three separate files, one for each type of input.
	 * 
	 * @param projectPath      the project path directory
	 * @param xmlNameExtension the extension of the files to search through
	 * @throws PlanItException thrown if not all of network, demand and zoning input data are available
	 */
	private void setInputFiles(String projectPath, String xmlNameExtension) throws PlanItException {
		String[] xmlFileNames = getXmlFileNames(projectPath, xmlNameExtension);
		if (!setInputFilesSingleFile(projectPath, xmlFileNames)) {
			setInputFilesSeparateFiles(projectPath, xmlFileNames);
		}
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
			LOGGER.info(e.getMessage());
			return false;
		}
	}

	/**
	 * Handles creation events for network, zones, demands and travel time
	 * parameters
	 * 
	 * We only handle BPR travel time functions at this stage.
	 * 
	 * @param event object creation event
	 * @throws PlanItException captures any exception thrown during creation events
	 */
	@Override
	public void onCreateProjectComponent(CreatedProjectComponentEvent<?> event) throws PlanItException {
		Object projectComponent = event.getProjectComponent();
		try {
			if (projectComponent instanceof MacroscopicNetwork) {
				populateNetwork((MacroscopicNetwork) projectComponent);
			} else if (projectComponent instanceof Zoning) {
				populateZoning((Zoning) projectComponent);
			} else if (projectComponent instanceof Demands) {
				populateDemands((Demands) projectComponent);
			} else if (projectComponent instanceof PhysicalCost) {
				populatePhysicalCost((PhysicalCost) projectComponent);
			} else if (projectComponent instanceof VirtualCost) {
				populateVirtualCost((VirtualCost) projectComponent);
			} else if (projectComponent instanceof Smoothing) {
				populateSmoothing((Smoothing) projectComponent);
			} else {
				LOGGER.fine("Event component is " + projectComponent.getClass().getCanonicalName()
						+ " which is not handled by BascCsvScan");
			}
		} catch (PlanItException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates the physical network object from the data in the input file
	 * 
	 * @param network the physical network object to be populated from the input
	 *                data
	 * @throws PlanItException thrown if there is an error reading the input file
	 */
	public void populateNetwork(@Nonnull MacroscopicNetwork network) throws PlanItException {

		LOGGER.info("Populating Network");

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
	}

	/**
	 * Creates the Zoning object and connectoids from the data in the input file
	 * 
	 * @param zoning the Zoning object to be populated from the input data
	 * @throws PlanItException thrown if there is an error reading the input file
	 */
	public void populateZoning(Zoning zoning) throws PlanItException {
		LOGGER.info("Populating Zoning");
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
	public void populateDemands(@Nonnull Demands demands) throws PlanItException {
		LOGGER.info("Populating Demands");
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
	 * Handles events for populating the PhysicalCost object
	 * 
	 * At present the creation event is generated but no immediate action is
	 * required to populate the PhysicalCost object. This is populated later by code
	 * calls or system defaults, no file reading is required. If we later change to
	 * reading in cost parameter values from a value, that would be done in this
	 * method.
	 * 
	 * @param physicalCost the PhysicalCost object to be populated
	 * @throws PlanItException thrown if there is an error.
	 */
	public void populatePhysicalCost(@Nonnull PhysicalCost physicalCost) throws PlanItException {
		LOGGER.info("Populating Physical Cost");
	}

	/**
	 * Handles events for populating the VirtualCost object
	 * 
	 * At present the creation event is generated but no immediate action is
	 * required to populate the VirtualCost object. The VirtualCost implementation
	 * currently uses a fixed parameter for speed. If we later change to reading in
	 * parameter values from a value, that would be done in this method.
	 * 
	 * @param virtualCost the VirtualCost object to be populated
	 * @throws PlanItException thrown if there is an error.
	 */
	public void populateVirtualCost(@Nonnull VirtualCost virtualCost) throws PlanItException {
		LOGGER.info("Populating Virtual Cost ");
	}

	/**
	 * Handles events for populating the Smoothing object
	 * 
	 * At present the creation event is generated but no immediate action is
	 * required to populate the Smoothing object. The Smoothing implementation is
	 * currently an algorithm which requires no parameters. If we later change to
	 * reading in parameter values from a value, that would be done in this method.
	 * 
	 * @param smoothing the Smoothing object to be populated
	 * @throws PlanItException thrown if there is an error.
	 */
	public void populateSmoothing(@Nonnull Smoothing smoothing) throws PlanItException {
		LOGGER.info("Populating Smoothing");
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
	 * Checks if a single XML file containing all of network, demand and zoning inputs is available, and reads it if it is.
	 * 
	 * @param projectPath      the project path directory
	 * @param xmlNameExtension the extension of the files to search through
	 * @return true if a single file containing all the inputs has been found and read, false otherwise
	 */
	private boolean setInputFilesSingleFile(String projectPath, String[] xmlFileNames) {
		for (int i = 0; i < xmlFileNames.length; i++) {
			try {
				XMLElementPLANit planit = (XMLElementPLANit) XmlUtils.generateObjectFromXml(XMLElementPLANit.class,
						xmlFileNames[i]);
				LOGGER.info("File " + xmlFileNames[i] + " provides the network, demands and zoning input data.");
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
	 * @param projectPath the name of the project path directory
	 * @throws PlanItException thrown if one or more of the input objects could not
	 *                         be populated from the XML files in the project
	 *                         directory
	 */
	private void setInputFilesSeparateFiles(String projectPath, String[] xmlFileNames) throws PlanItException {
		boolean foundZoningFile = false;
		boolean foundNetworkFile = false;
		boolean foundDemandFile = false;
		for (int i = 0; i < xmlFileNames.length; i++) {
			if (foundZoningFile && foundDemandFile && foundNetworkFile) {
				LOGGER.info("File " + xmlFileNames[i] + " exists but was not parsed.");
			}
			if (!foundZoningFile) {
				try {
					macroscopiczoning = (XMLElementMacroscopicZoning) XmlUtils
							.generateObjectFromXml(XMLElementMacroscopicZoning.class, xmlFileNames[i]);
					LOGGER.info("File " + xmlFileNames[i] + " provides the zoning input data.");
					foundZoningFile = true;
					continue;
				} catch (Exception e) {
				}
			}
			if (!foundNetworkFile) {
				try {
					macroscopicnetwork = (XMLElementMacroscopicNetwork) XmlUtils
							.generateObjectFromXml(XMLElementMacroscopicNetwork.class, xmlFileNames[i]);
					LOGGER.info("File " + xmlFileNames[i] + " provides the network input data.");
					foundNetworkFile = true;
					continue;
				} catch (Exception e) {
				}
			}
			if (!foundDemandFile) {
				try {
					macroscopicdemand = (XMLElementMacroscopicDemand) XmlUtils
							.generateObjectFromXml(XMLElementMacroscopicDemand.class, xmlFileNames[i]);
					LOGGER.info("File " + xmlFileNames[i] + " provides the demand input data.");
					foundDemandFile = true;
					continue;
				} catch (Exception e) {
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
	}

	/**
	 * Populate the generated input objects from three separate XML files by validating the input files first.
	 * 
	 * This file does the same task as setInputFilesSeparateFiles(), it does it in a different way.  This
	 * method runs much more slowly than setInputFilesSeparateFiles(), it takes about 60 times as 
	 * long for the same input data sets.
	 * 
	 * @param projectPath the name of the project path directory
	 * @throws PlanItException thrown if one or more of the input objects could not
	 *                         be populated from the XML files in the project
	 *                         directory
	 */
	private void setInputFilesSeparateFilesWithValidation(String projectPath, String[] xmlFileNames) throws PlanItException {
		boolean foundZoningFile = false;
		String zoningFileName = null;
		boolean foundNetworkFile = false;
		String networkFileName = null;
		boolean foundDemandFile = false;
		String demandFileName = null;
		for (int i = 0; i < xmlFileNames.length; i++) {
			if (foundZoningFile && foundDemandFile && foundNetworkFile) {
				LOGGER.info("File " + xmlFileNames[i] + " exists but was not parsed.");
			}
			if (!foundZoningFile) {
				foundZoningFile = validateXmlInputFile(xmlFileNames[i], "src\\main\\resources\\xsd\\macroscopiczoninginput.xsd");
				if (foundZoningFile) {
					zoningFileName = xmlFileNames[i];
					LOGGER.info("File " + xmlFileNames[i] + " provides the zoning input data.");
				}
			}
			if (!foundNetworkFile) {
				foundNetworkFile = validateXmlInputFile(xmlFileNames[i], "src\\main\\resources\\xsd\\macroscopicnetworkinput.xsd");
				if (foundNetworkFile) {
					networkFileName = xmlFileNames[i];
					LOGGER.info("File " + xmlFileNames[i] + " provides the network input data.");
				}
			}
			if (!foundDemandFile) {
				foundDemandFile = validateXmlInputFile(xmlFileNames[i], "src\\main\\resources\\xsd\\macroscopicdemandinput.xsd");
				if (foundDemandFile) {
					demandFileName = xmlFileNames[i];
					LOGGER.info("File " + xmlFileNames[i] + " provides the demand input data.");
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

}
