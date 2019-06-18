package org.planit.xml.input;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.planit.demand.Demands;
import org.planit.event.CreatedProjectComponentEvent;
import org.planit.event.listener.InputBuilderListener;
import org.planit.exceptions.PlanItException;
import org.planit.generated.Demandconfiguration;
import org.planit.generated.Infrastructure;
import org.planit.generated.Linkconfiguration;
import org.planit.generated.Macroscopicdemand;
import org.planit.generated.Macroscopicnetwork;
import org.planit.generated.Macroscopiczoning;
import org.planit.generated.Odmatrix;
import org.planit.generated.Zones.Zone;
import org.planit.network.physical.PhysicalNetwork.Nodes;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Centroid;
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
public class PlanItXml implements InputBuilderListener {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(PlanItXml.class.getName());

	private String zoningXmlFileLocation;
	private String demandXmlFileLocation;
	private String networkXmlFileLocation;

	private int noCentroids;
	private Map<Integer, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentTypeMap;
	private Map<Integer, Mode> modeMap;
	private Nodes nodes;
	private Zoning.Zones zones;

	/**
	 * If no user class is defined the default user class will be assumed to have a
	 * mode referencing the default external mode id (1)
	 */
	public static final long DEFAULT_MODE_EXTERNAL_ID = 1;

	/**
	 * If no user class is defined the default user class will be assumed to have a
	 * traveler type referencing the default external traveler type id (1)
	 */
	public static final long DEFAULT_TRAVELER_TYPE_EXTERNAL_ID = 1;

	/**
	 * The default separator that is assumed when no separator is provided
	 */
	public static final String DEFAULT_SEPARATOR = ",";

	/**
	 * Constructor which reads in the XML input files
	 * 
	 * @param zoningXmlFileLocation  location of XML zones input file
	 * @param demandXmlFileLocation  location of XML demands input file
	 * @param networkXmlFileLocation location of XML network inputs file
	 */
	public PlanItXml(String zoningXmlFileLocation, String demandXmlFileLocation, String networkXmlFileLocation) {
		this.zoningXmlFileLocation = zoningXmlFileLocation;
		this.demandXmlFileLocation = demandXmlFileLocation;
		this.networkXmlFileLocation = networkXmlFileLocation;
	}

	/**
	 * Constructor which reads in the XML input files and XSD files to validate them
	 * against
	 * 
	 * If a null value is entered for the location of an XSD file, no validation is
	 * carried out on the corresponding input XML file.
	 * 
	 * @param zoningXmlFileLocation  location of XML zones input file
	 * @param demandXmlFileLocation  location of XML demands input file
	 * @param networkXmlFileLocation location of XML network inputs file
	 * @param zoningXsdFileLocation  location of XSD schema file for zones
	 * @param demandXsdFileLocation  location of XSD schema file for demands
	 * @param networkXsdFileLocation location of XSD schema file for network
	 * @throws PlanItException thrown if one of the input XML files is invalid
	 */
	public PlanItXml(String zoningXmlFileLocation, String demandXmlFileLocation, String networkXmlFileLocation,
			String zoningXsdFileLocation, String demandXsdFileLocation, String networkXsdFileLocation)
			throws PlanItException {
		this(zoningXmlFileLocation, demandXmlFileLocation, networkXmlFileLocation);
		try {
			if (zoningXsdFileLocation != null) {
				XmlUtils.validateXml(zoningXmlFileLocation, zoningXsdFileLocation);
			}
			if (demandXsdFileLocation != null) {
				XmlUtils.validateXml(demandXmlFileLocation, demandXsdFileLocation);
			}
			if (networkXsdFileLocation != null) {
				XmlUtils.validateXml(networkXmlFileLocation, networkXsdFileLocation);
			}
		} catch (Exception e) {
			throw new PlanItException(e);
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
			Macroscopicnetwork macroscopicnetwork = (Macroscopicnetwork) XmlUtils
					.generateObjectFromXml(Macroscopicnetwork.class, networkXmlFileLocation);
			Linkconfiguration linkconfiguration = macroscopicnetwork.getLinkconfiguration();
			modeMap = ProcessLinkConfiguration.getModeMap(linkconfiguration);
			linkSegmentTypeMap = ProcessLinkConfiguration.createLinkSegmentTypeMap(linkconfiguration, modeMap);
			Infrastructure infrastructure = macroscopicnetwork.getInfrastructure();
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
			Macroscopiczoning macroscopiczoning = (Macroscopiczoning) XmlUtils
					.generateObjectFromXml(Macroscopiczoning.class, zoningXmlFileLocation);
			for (Zone zone : macroscopiczoning.getZones().getZone()) {
				Centroid centroid = UpdateZoning.createAndRegisterZoneAndCentroid(zoning, zone);
				UpdateZoning.registerNewConnectoid(zoning, nodes, zone, centroid);
				noCentroids++;
			}
			zones = zoning.zones;
		} catch (Exception e) {
			e.printStackTrace();
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
			Macroscopicdemand macroscopicdemand = (Macroscopicdemand) XmlUtils
					.generateObjectFromXml(Macroscopicdemand.class, demandXmlFileLocation);
			Demandconfiguration demandconfiguration = macroscopicdemand.getDemandconfiguration();
			Map<Integer, TimePeriod> timePeriodMap = ProcessConfiguration
					.generateAndStoreConfigurationData(demandconfiguration);
			List<Odmatrix> oddemands = macroscopicdemand.getOddemands()
					.getOdcellbycellmatrixOrOdrowmatrixOrOdrawmatrix();
			UpdateDemands.createAndRegisterDemandMatrix(demands, oddemands, modeMap, timePeriodMap, noCentroids, zones);
		} catch (Exception e) {
			e.printStackTrace();
			throw new PlanItException(e);
		}
	}

}
