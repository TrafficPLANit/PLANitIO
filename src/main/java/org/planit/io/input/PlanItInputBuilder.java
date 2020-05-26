package org.planit.io.input;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongFunction;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.djutils.event.EventInterface;
import org.opengis.geometry.DirectPosition;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.generated.XMLElementDemandConfiguration;
import org.planit.generated.XMLElementInfrastructure;
import org.planit.generated.XMLElementLinkConfiguration;
import org.planit.generated.XMLElementLinkSegmentTypes;
import org.planit.generated.XMLElementMacroscopicDemand;
import org.planit.generated.XMLElementMacroscopicNetwork;
import org.planit.generated.XMLElementMacroscopicZoning;
import org.planit.generated.XMLElementModes;
import org.planit.generated.XMLElementOdMatrix;
import org.planit.generated.XMLElementPLANit;
import org.planit.input.InputBuilderListener;
import org.planit.io.xml.demands.ProcessConfiguration;
import org.planit.io.xml.demands.UpdateDemands;
import org.planit.io.xml.network.ProcessInfrastructure;
import org.planit.io.xml.network.ProcessLinkConfiguration;
import org.planit.io.xml.network.physical.macroscopic.MacroscopicLinkSegmentTypeXmlHelper;
import org.planit.io.xml.util.XmlUtils;
import org.planit.io.xml.zoning.UpdateZoning;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.PhysicalNetwork.Nodes;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.DownstreamNodeExternalIdOutputProperty;
import org.planit.output.property.LinkCostOutputProperty;
import org.planit.output.property.LinkSegmentExternalIdOutputProperty;
import org.planit.output.property.LinkSegmentIdOutputProperty;
import org.planit.output.property.ModeExternalIdOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.UpstreamNodeExternalIdOutputProperty;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;
import org.planit.userclass.TravelerType;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.Zone;

/**
 * Class which reads inputs from XML input files
 *
 * @author gman6028
 *
 */
public class PlanItInputBuilder extends InputBuilderListener {

  /** generated UID */
  private static final long serialVersionUID = -8928911341112445424L;
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanItInputBuilder.class.getCanonicalName());   
  
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
   * Default extension for XML input files
   */
  private static final String DEFAULT_XML_NAME_EXTENSION = ".xml";
  
  /**
   * Default PCU value for modes 
   */
  private static final float DEFAULT_PCU_VALUE = 1.0f;
  
  /**
   * Default external Id value
   */
  private static final long DEFAULT_EXTERNAL_ID = 1;
  
  private static final float DEFAULT_MAXIMUM_CAPACITY_PER_LANE = 1800.0f;
  
  private static final float DEFAULT_MAXIMUM_DENSITY_PER_LANE = 180.0f;

  /**
   * Default XSD files used to validate input XML files against
   */
  private static final String NETWORK_XSD_FILE = "src\\main\\resources\\xsd\\macroscopicnetworkinput.xsd";
  private static final String ZONING_XSD_FILE = "src\\main\\resources\\xsd\\macroscopiczoninginput.xsd";
  private static final String DEMAND_XSD_FILE = "src\\main\\resources\\xsd\\macroscopicdemandinput.xsd";

  /**
   * The default separator that is assumed when no separator is provided
   */
  public static final String DEFAULT_SEPARATOR = ",";

  /**
   * Populate the input objects from specified XML files
   *
   * @param zoningXmlFileLocation location of the zoning input XML file
   * @param demandXmlFileLocation location of the demand input XML file
   * @param networkXmlFileLocation location of the network input XML file
   * @throws PlanItException thrown if there is an error during reading the files
   */
  private void createGeneratedClassesFromXmlLocations(final String zoningXmlFileLocation,
      final String demandXmlFileLocation,
      final String networkXmlFileLocation) throws PlanItException {
    try {
      macroscopiczoning = (XMLElementMacroscopicZoning) XmlUtils
          .generateObjectFromXml(XMLElementMacroscopicZoning.class, zoningXmlFileLocation);
      macroscopicdemand = (XMLElementMacroscopicDemand) XmlUtils
          .generateObjectFromXml(XMLElementMacroscopicDemand.class, demandXmlFileLocation);
      macroscopicnetwork = (XMLElementMacroscopicNetwork) XmlUtils
          .generateObjectFromXml(XMLElementMacroscopicNetwork.class, networkXmlFileLocation);
    } catch (final Exception e) {
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
   * @param projectPath the project path directory
   * @param xmlNameExtension the extension of the files to search through
   * @throws PlanItException thrown if not all of network, demand and zoning input
   *           data are available
   */
  private void setInputFiles(final String projectPath, final String xmlNameExtension) throws PlanItException {
    final String[] xmlFileNames = getXmlFileNames(projectPath, xmlNameExtension);
    if (setInputFilesSingleFile(xmlFileNames)) {
      return;
    }
    if (setInputFilesSeparateFiles(xmlFileNames)) {
      return;
    }
    String errorMessage = "The directory " + projectPath
        + " does not contain either one file with all the macroscopic inputs or a separate file for each of zoning, demand and network.";
    LOGGER.severe(errorMessage);
    throw new PlanItException(errorMessage);
  }

  /**
   * Return an array of the names of all the input files in the project path
   * directory
   *
   * @param projectPath the project path directory
   * @param xmlNameExtension the extension of the files to search through
   * @return array of names of files in the directory with the specified extension
   * @throws PlanItException thrown if no files with the specified extension can
   *           be found
   */
  private String[] getXmlFileNames(final String projectPath, final String xmlNameExtension) throws PlanItException {
    final File xmlFilesDirectory = new File(projectPath);
    if (!xmlFilesDirectory.isDirectory()) {
      String errorMessage = projectPath + " is not a valid directory.";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    final String[] fileNames = xmlFilesDirectory.list((d, name) -> name.endsWith(xmlNameExtension));
    if (fileNames.length == 0) {
      String errorMessage = "Directory " + projectPath + " contains no files with extension " + xmlNameExtension;
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
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
  private boolean setInputFilesSingleFile(final String[] xmlFileNames) {
    for (int i = 0; i < xmlFileNames.length; i++) {
      try {
        final XMLElementPLANit planit = 
            (XMLElementPLANit) XmlUtils.generateObjectFromXml(XMLElementPLANit.class, xmlFileNames[i]);
        LOGGER.info("File " + xmlFileNames[i] + " provides the network, demands and zoning input data.");
        macroscopiczoning = planit.getMacroscopiczoning();
        macroscopicnetwork = planit.getMacroscopicnetwork();
        macroscopicdemand = planit.getMacroscopicdemand();
        return true;
      } catch (final Exception e) {            
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
  private boolean setInputFilesSeparateFiles(final String[] xmlFileNames) {
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
        } catch (final Exception e) {
        }
      }
      if (!foundNetworkFile) {
        try {
          macroscopicnetwork = (XMLElementMacroscopicNetwork) XmlUtils
              .generateObjectFromXml(XMLElementMacroscopicNetwork.class, xmlFileNames[i]);
          LOGGER.info("File " + xmlFileNames[i] + " provides the network input data.");
          foundNetworkFile = true;
          continue;
        } catch (final Exception e) {
        }
      }
      if (!foundDemandFile) {
        try {
          macroscopicdemand = (XMLElementMacroscopicDemand) XmlUtils
              .generateObjectFromXml(XMLElementMacroscopicDemand.class, xmlFileNames[i]);
          LOGGER.info("File " + xmlFileNames[i] + " provides the demand input data.");
          foundDemandFile = true;
          continue;
        } catch (final Exception e) {
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
   *           be populated from the XML files in the project
   *           directory
   */
  @SuppressWarnings("unused")
  private void setInputFilesSeparateFilesWithValidation(final String projectPath, final String[] xmlFileNames)
      throws PlanItException {
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
        foundZoningFile = validateXmlInputFile(xmlFileNames[i], ZONING_XSD_FILE);
        if (foundZoningFile) {
          zoningFileName = xmlFileNames[i];
          LOGGER.info("File " + xmlFileNames[i] + " provides the zoning input data.");
        }
      }
      if (!foundNetworkFile) {
        foundNetworkFile = validateXmlInputFile(xmlFileNames[i], NETWORK_XSD_FILE);
        if (foundNetworkFile) {
          networkFileName = xmlFileNames[i];
          LOGGER.info("File " + xmlFileNames[i] + " provides the network input data.");
        }
      }
      if (!foundDemandFile) {
        foundDemandFile = validateXmlInputFile(xmlFileNames[i], DEMAND_XSD_FILE);
        if (foundDemandFile) {
          demandFileName = xmlFileNames[i];
          LOGGER.info("File " + xmlFileNames[i] + " provides the demand input data.");
        }
      }
    }
    if (!foundZoningFile) {
      String errorMessage = "Failed to find a valid zoning input file in the project directory " + projectPath;
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    if (!foundNetworkFile) {
      String errorMessage = "Failed to find a valid network input file in the project directory " + projectPath;
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    if (!foundDemandFile) {
      String errorMessage = "Failed to find a valid demand input file in the project directory " + projectPath;
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
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
  private OutputProperty getLinkIdentificationMethod(final Set<String> headers) throws PlanItException {
    boolean linkSegmentExternalIdPresent = false;
    boolean linkSegmentIdPresent = false;
    boolean upstreamNodeExternalIdPresent = false;
    boolean downstreamNodeExternalIdPresent = false;
    boolean modeExternalIdPresent = false;
    boolean costPresent = false;
    for (final String header : headers) {
      final OutputProperty outputProperty = OutputProperty.fromHeaderName(header);
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
        case LINK_COST:
          costPresent = true;
      }
    }
    if (!costPresent) {
      String errorMessage = "Cost column not present in initial link segment costs file";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    if (!modeExternalIdPresent) {
      String errorMessage = "Mode External Id not present in initial link segment costs file";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
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
    String errorMessage = "Links not correctly identified in initial link segment costs file";
    LOGGER.severe(errorMessage);
    throw new PlanItException(errorMessage);
  }

  /**
   * Set the initial link segment cost for the specified link segment using values
   * in the CSV initial segment costs file
   *
   * @param initialLinkSegmentCost the InitialLinkSegmentCost object to store the
   *          cost value
   * @param record the record in the CSV input file to get the
   *          data value from
   * @param linkSegment the current link segment
   * @throws PlanItException
   */
  private void setInitialLinkSegmentCost(final InitialLinkSegmentCost initialLinkSegmentCost, final CSVRecord record,
      final LinkSegment linkSegment) throws PlanItException {
    final long modeExternalId = Long.parseLong(record.get(ModeExternalIdOutputProperty.NAME));
    final Mode mode = getModeByExternalId(modeExternalId);
    if (mode == null) {
      String errorMessage = "mode external id not available in configuration";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    final double cost = Double.parseDouble(record.get(LinkCostOutputProperty.NAME));
    initialLinkSegmentCost.setSegmentCost(mode, linkSegment, cost);
  }

  /**
   * Update the initial link segment cost object using the data from the CSV input
   * file for the current record
   *
   * @param initialLinkSegmentCost the InitialLinkSegmentCost object to be updated
   * @param parser the CSVParser containing all CSV records
   * @param record the current CSVRecord
   * @param outputProperty the OutputProperty corresponding to the column to be read from
   * @param header the header specifying the column to be read from
   * @param findLinkFunction the function which finds the link segment for the current header
   * @throws PlanItException thrown if no link segment is found
   */
  private void updateInitialLinkSegmentCost(final InitialLinkSegmentCost initialLinkSegmentCost, final CSVParser parser,
      final CSVRecord record, final OutputProperty outputProperty, final String header,
      final LongFunction<LinkSegment> findLinkFunction)
      throws PlanItException {
    final long id = Long.parseLong(record.get(header));
    final LinkSegment linkSegment = findLinkFunction.apply(id);
    if (linkSegment == null) {
      String errorMessage = "Failed to find link segment";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
   }
    setInitialLinkSegmentCost(initialLinkSegmentCost, record, linkSegment);
  }

  /**
   * Update the initial link segment cost object using the data from the CSV input
   * file for the current record specified by start and end node external Id
   *
   * @param network the physical network
   * @param initialLinkSegmentCost the InitialLinkSegmentCost object to be updated
   * @param parser the CSVParser containing all CSV records
   * @param record the current CSVRecord
   * @param startOutputProperty the OutputProperty corresponding to the column
   *          to the start node
   * @param endOutputProperty the OutputProperty corresponding to the column
   *          to the end node
   * @param startHeader the header specifying the start node column
   * @param endHeader the header specifying the end node column
   * @throws PlanItException thrown if there is an error during searching for the
   *           link segment
   */
  private void updateInitialLinkSegmentCostFromStartAndEndNodeExternalId(
      final PhysicalNetwork network,
      final InitialLinkSegmentCost initialLinkSegmentCost, final CSVParser parser, final CSVRecord record,
      final OutputProperty startOutputProperty, final OutputProperty endOutputProperty, final String startHeader,
      final String endHeader)
      throws PlanItException {
    final long upstreamNodeExternalId = Long.parseLong(record.get(startHeader));
    final long downstreamNodeExternalId = Long.parseLong(record.get(endHeader));
    final long startId = getNodeByExternalId(upstreamNodeExternalId).getId();
    final long endId = getNodeByExternalId(downstreamNodeExternalId).getId();
    final LinkSegment linkSegment = network.linkSegments.getLinkSegmentByStartAndEndNodeId(startId, endId);

    if (linkSegment == null) {
      String errorMessage = "Failed to find link segment";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    setInitialLinkSegmentCost(initialLinkSegmentCost, record, linkSegment);
  }
  
  /**
   * Update the XML macroscopic network element to include default values for any properties not included in the input file
   */
  private void addDefaultValuesToXmlMacroscopicNetwork() {
    if (macroscopicnetwork.getLinkconfiguration() == null) {
      macroscopicnetwork.setLinkconfiguration(new XMLElementLinkConfiguration());
    }
    
    if (macroscopicnetwork.getLinkconfiguration().getModes() == null) {
      macroscopicnetwork.getLinkconfiguration().setModes(new XMLElementModes());
      XMLElementModes.Mode xmlElementMode = new XMLElementModes.Mode();
      xmlElementMode.setPcu(DEFAULT_PCU_VALUE);
      xmlElementMode.setName("");
      xmlElementMode.setId(BigInteger.valueOf(DEFAULT_EXTERNAL_ID));
      macroscopicnetwork.getLinkconfiguration().getModes().getMode().add(xmlElementMode);
    }
    
    if (macroscopicnetwork.getLinkconfiguration().getLinksegmenttypes() == null) {
      macroscopicnetwork.getLinkconfiguration().setLinksegmenttypes(new XMLElementLinkSegmentTypes());
      XMLElementLinkSegmentTypes.Linksegmenttype xmlLinkSegmentType = new XMLElementLinkSegmentTypes.Linksegmenttype();
      xmlLinkSegmentType.setName("");
      xmlLinkSegmentType.setId(BigInteger.valueOf(DEFAULT_EXTERNAL_ID));
      xmlLinkSegmentType.setCapacitylane(DEFAULT_MAXIMUM_CAPACITY_PER_LANE);
      xmlLinkSegmentType.setMaxdensitylane(DEFAULT_MAXIMUM_DENSITY_PER_LANE);
      macroscopicnetwork.getLinkconfiguration().getLinksegmenttypes().getLinksegmenttype().add(xmlLinkSegmentType);
    }
    
    for (XMLElementLinkSegmentTypes.Linksegmenttype xmlLinkSegmentType : macroscopicnetwork.getLinkconfiguration().getLinksegmenttypes().getLinksegmenttype()) {
      if (xmlLinkSegmentType.getModes() == null) {
        XMLElementLinkSegmentTypes.Linksegmenttype.Modes xmlLinkSegmentModes = new XMLElementLinkSegmentTypes.Linksegmenttype.Modes();
        XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode xmlLinkSegmentMode = new XMLElementLinkSegmentTypes.Linksegmenttype.Modes.Mode();
        xmlLinkSegmentMode.setRef(BigInteger.valueOf(0));
        xmlLinkSegmentModes.getMode().add(xmlLinkSegmentMode);
        xmlLinkSegmentType.setModes(xmlLinkSegmentModes);
      }
    }
  }

  /**
   * Creates the physical network object from the data in the input file
   *
   * @param physicalNetwork the physical network object to be populated from the input data
   * @throws PlanItException thrown if there is an error reading the input file
   */
  protected void populatePhysicalNetwork(@Nonnull final PhysicalNetwork physicalNetwork) throws PlanItException {

    LOGGER.info("Populating Network");

    final MacroscopicNetwork network = (MacroscopicNetwork) physicalNetwork;
    try {
      addDefaultValuesToXmlMacroscopicNetwork();
      final XMLElementLinkConfiguration linkconfiguration = macroscopicnetwork.getLinkconfiguration();
      ProcessLinkConfiguration.createAndRegisterModes(physicalNetwork, linkconfiguration, this);
      final Map<Long, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentTypeHelperMap = ProcessLinkConfiguration.createLinkSegmentTypeHelperMap(linkconfiguration, this);  
      final XMLElementInfrastructure infrastructure = macroscopicnetwork.getInfrastructure();
      ProcessInfrastructure.createAndRegisterNodes(infrastructure, network, this);
      ProcessInfrastructure.createAndRegisterLinkSegments(infrastructure, network, linkSegmentTypeHelperMap, this, DEFAULT_EXTERNAL_ID);
    } catch (PlanItException pe) {
      throw pe;
    } catch (final Exception ex) {
      throw new PlanItException(ex);
    }
  }

  /**
   * Creates the Zoning object and connectoids from the data in the input file
   *
   * @param zoning the Zoning object to be populated from the input data
   * @param parameter1 PhysicalNetwork object previously defined
   * @throws PlanItException thrown if there is an error reading the input file
   */
  protected void populateZoning(final Zoning zoning, final Object parameter1) throws PlanItException {
    LOGGER.info("Populating Zoning");
    if (!(parameter1 instanceof PhysicalNetwork)) {
      String errorMessage = "Parameter of call to populateZoning() is not of class PhysicalNetwork";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    final PhysicalNetwork physicalNetwork = (PhysicalNetwork) parameter1;
    final Nodes nodes = physicalNetwork.nodes;

    // create and register zones, centroids and connectoids
    try {
      for (final org.planit.generated.XMLElementZones.Zone xmlZone : macroscopiczoning.getZones().getZone()) {
        long zoneExternalId = xmlZone.getId().longValue();
        Zone zone = zoning.zones.createAndRegisterNewZone(zoneExternalId);
        addZoneToExternalIdMap(zone.getExternalId(), zone);
        Centroid centroid = zone.getCentroid();
        if (xmlZone.getCentroid().getPoint() != null) {
          DirectPosition centrePointGeometry = UpdateZoning.getCentrePointGeometry(xmlZone);
          centroid.setCentrePointGeometry(centrePointGeometry);
        }
        UpdateZoning.registerNewConnectoid(zoning, nodes, xmlZone, centroid, this);
      }
    } catch (PlanItException pe) {
      throw pe;
    } catch (Exception e) {
      throw new PlanItException(e);
    }
  }

  /**
   * Populates the Demands object from the input file
   *
   * @param demands the Demands object to be populated from the input data
   * @param parameter1 Zoning object previously defined
   * @param parameter2 PhysicalNetwork object previously defined
   * @throws PlanItException thrown if there is an error reading the input file
   */
  protected void populateDemands(@Nonnull Demands demands, final Object parameter1, final Object parameter2) throws PlanItException {
    LOGGER.info("Populating Demands");
    if (!(parameter1 instanceof Zoning)) {
      String errorMessage = "Parameter of call to populateDemands() is not of class Zoning.";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    final Zoning zoning = (Zoning) parameter1;
    try {
      final XMLElementDemandConfiguration demandconfiguration = macroscopicdemand.getDemandconfiguration();
      ProcessConfiguration.generateAndStoreConfigurationData(demands, demandconfiguration, this);
      final List<XMLElementOdMatrix> oddemands = macroscopicdemand.getOddemands().getOdcellbycellmatrixOrOdrowmatrixOrOdrawmatrix();
      UpdateDemands.createAndRegisterDemandMatrix(demands, oddemands, zoning.zones, this);
      for (TravelerType travelerType : travelerTypeExternalIdToTravelerTypeMap.values()) {
    	  demands.registerTravelerType(travelerType);
      }
    } catch (final Exception e) {
      throw new PlanItException(e);
    }
  }

  /**
   * Populate the initial link segment cost from a CSV file
   *
   * @param initialLinkSegmentCost InitialLinkSegmentCost object to be populated
   * @param parameter1 previously created network object
   * @param parameter2 CSV file containing the initial link segment cost values
   * @throws PlanItException
   */
  protected void populateInitialLinkSegmentCost(final InitialLinkSegmentCost initialLinkSegmentCost,
      final Object parameter1, final Object parameter2)
      throws PlanItException {
    LOGGER.info("Populating Initial Link Segment Costs");
    if (!(parameter1 instanceof PhysicalNetwork)) {
      throw new PlanItException(
          "Parameter 1 of call to populateInitialLinkSegments() is not of class PhysicalNework");
    }
    if (!(parameter2 instanceof String)) {
      String errorMessage = "Parameter 2 of call to populateInitialLinkSegments() is not a file name";
      LOGGER.severe(errorMessage);
      throw new PlanItException(errorMessage);
    }
    final PhysicalNetwork network = (PhysicalNetwork) parameter1;
    final String fileName = (String) parameter2;
    try {
      final Reader in = new FileReader(fileName);
      final CSVParser parser = CSVParser.parse(in, CSVFormat.DEFAULT.withFirstRecordAsHeader());
      final Set<String> headers = parser.getHeaderMap().keySet();
      final OutputProperty linkIdentificationMethod = getLinkIdentificationMethod(headers);
      for (final CSVRecord record : parser) {
        switch (linkIdentificationMethod) {
          case LINK_SEGMENT_ID:
            updateInitialLinkSegmentCost(initialLinkSegmentCost, parser, record, OutputProperty.LINK_SEGMENT_ID,
                LinkSegmentIdOutputProperty.NAME, (id) -> {
                  return network.linkSegments.getLinkSegment(id);
                });
            break;
          case LINK_SEGMENT_EXTERNAL_ID:
            updateInitialLinkSegmentCost(initialLinkSegmentCost, parser, record,
                OutputProperty.LINK_SEGMENT_EXTERNAL_ID,
                LinkSegmentExternalIdOutputProperty.NAME, (externalId) -> {
                  return getLinkSegmentByExternalId(externalId);
                });
            break;
          case UPSTREAM_NODE_EXTERNAL_ID:
            updateInitialLinkSegmentCostFromStartAndEndNodeExternalId(network, initialLinkSegmentCost, parser, record,
                OutputProperty.UPSTREAM_NODE_EXTERNAL_ID, OutputProperty.DOWNSTREAM_NODE_EXTERNAL_ID,
                UpstreamNodeExternalIdOutputProperty.NAME, DownstreamNodeExternalIdOutputProperty.NAME);
            break;
          default:
            String errorMessage = "Invalid Output Property "
                + BaseOutputProperty.convertToBaseOutputProperty(linkIdentificationMethod).getName()
                + " found in header of Initial Link Segment Cost CSV file";
            LOGGER.severe(errorMessage);
            throw new PlanItException(errorMessage);
       }
      }
      in.close();
    } catch (final Exception ex) {
      throw new PlanItException(ex);
    }
  }

  /**
   * Constructor which generates the input objects from files in a specified
   * directory, using the default extension ".xml"
   *
   * @param projectPath the location of the input file directory
   * @throws PlanItException thrown if one of the input required input files
   *           cannot be found, or if there is an error reading one
   *           of them
   */
  public PlanItInputBuilder(final String projectPath) throws PlanItException {
    this(projectPath, DEFAULT_XML_NAME_EXTENSION);
  }

  /**
   * Constructor which generates the input objects from files in a specified
   * directory
   *
   * @param projectPath the location of the input file directory
   * @param xmlNameExtension the extension of the data files to be searched
   *          through
   * @throws PlanItException thrown if one of the input required input files
   *           cannot be found, or if there is an error reading one
   *           of them
   */
  public PlanItInputBuilder(final String projectPath, final String xmlNameExtension) throws PlanItException {
    super();
    LOGGER.info("Project path is set to: "+ projectPath);
    setInputFiles(projectPath, xmlNameExtension);
  }

  /**
   * Validates an input XML file against an XSD file
   *
   * @param xmlFileLocation input XML file
   * @param schemaFileLocation XSD file to validate XML file against
   * @return true if the file is valid, false otherwise
   */
  public static boolean validateXmlInputFile(final String xmlFileLocation, final String schemaFileLocation) {
    try {
      XmlUtils.validateXml(xmlFileLocation, schemaFileLocation);
      return true;
    } catch (final Exception e) {
      LOGGER.info(e.getMessage());
      return false;
    }
  }

  /**
   * Whenever a project component is created this method will be invoked
   *
   * @param event event containing the created (and empty) project component
   * @throws PlanItException thrown if there is an error
   */
  @Override
  public void notify(final EventInterface event) throws RemoteException {
    // registered for create notifications
    if (event.getType() == TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE) {
      final Object[] content = (Object[]) event.getContent();
      final Object projectComponent = content[0];
      // the content consists of the actual traffic assignment component and an array of object
      // parameters (second parameter)
      final Object[] parameters = (Object[]) content[1];
      try {
        if (projectComponent instanceof PhysicalNetwork) {
          populatePhysicalNetwork((PhysicalNetwork) projectComponent);
        } else if (projectComponent instanceof Zoning) {
          populateZoning((Zoning) projectComponent, parameters[0]);
        } else if (projectComponent instanceof Demands) {
          populateDemands((Demands) projectComponent, parameters[0], parameters[1]);
        } else if (projectComponent instanceof InitialPhysicalCost) {
          populateInitialLinkSegmentCost((InitialLinkSegmentCost) projectComponent, parameters[0], parameters[1]);
        } else {
          LOGGER.info("Event component is " + projectComponent.getClass().getCanonicalName()
              + " which is not handled by PlanItInputBuilder.");
        }
      } catch (final PlanItException e) {
        e.printStackTrace(); 
        throw new RemoteException(e.getMessage(), e);
      }
    }
  }

}