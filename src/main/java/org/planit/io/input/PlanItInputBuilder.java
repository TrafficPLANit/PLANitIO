package org.planit.io.input;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.djutils.event.EventInterface;
import org.planit.assignment.TrafficAssignmentComponentFactory;
import org.planit.cost.physical.initial.InitialLinkSegmentCost;
import org.planit.cost.physical.initial.InitialPhysicalCost;
import org.planit.demands.Demands;
import org.planit.geo.PlanitJtsUtils;
import org.planit.xml.generated.*;

import org.locationtech.jts.geom.Point;

import org.planit.input.InputBuilderListener;
import org.planit.io.xml.demands.ProcessConfiguration;
import org.planit.io.network.converter.PlanitNetworkReader;
import org.planit.io.network.converter.PlanitNetworkReaderFactory;
import org.planit.io.xml.demands.DemandsPopulator;
import org.planit.io.xml.util.JAXBUtils;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Zoning;
import org.planit.output.property.BaseOutputProperty;
import org.planit.output.property.DownstreamNodeXmlIdOutputProperty;
import org.planit.output.property.LinkCostOutputProperty;
import org.planit.output.property.LinkSegmentIdOutputProperty;
import org.planit.output.property.LinkSegmentXmlIdOutputProperty;
import org.planit.output.property.ModeXmlIdOutputProperty;
import org.planit.output.property.OutputProperty;
import org.planit.output.property.UpstreamNodeXmlIdOutputProperty;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.misc.FileUtils;
import org.planit.utils.misc.LoggingUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.Connectoid;
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
   * Generated object to store demand input data
   */
  private XMLElementMacroscopicDemand xmlRawDemand;

  /**
   * Generated object to store zoning input data
   */
  private XMLElementMacroscopicZoning xmlRawZoning;
  
  /**
   * Generated object to store network input data
   */  
  private XMLElementMacroscopicNetwork xmlRawNetwork;  
    
  
  /** project path to use */
  private final String projectPath;
  
  /** xml file extension to use */
  private final String xmlFileExtension;  

  /**
   * Default extension for XML input files
   */
  private static final String DEFAULT_XML_FILE_EXTENSION = ".xml";
       
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
  private void createGeneratedClassesFromXmlLocations(final File zoningXmlFileLocation,
      final File demandXmlFileLocation,
      final File networkXmlFileLocation) throws PlanItException {
    try {
      xmlRawZoning = (XMLElementMacroscopicZoning) JAXBUtils.generateObjectFromXml(XMLElementMacroscopicZoning.class, zoningXmlFileLocation);
      xmlRawDemand = (XMLElementMacroscopicDemand) JAXBUtils.generateObjectFromXml(XMLElementMacroscopicDemand.class, demandXmlFileLocation);
      xmlRawNetwork = (XMLElementMacroscopicNetwork) JAXBUtils.generateObjectFromXml(XMLElementMacroscopicNetwork.class, networkXmlFileLocation);
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error while generating classes from XML locations in PLANitIO",e);
    }
  }

  /**
   * Read the input XML file(s) 
   *
   * This method checks if a single file contains all components of the of project; network, demand, and zoning. 
   * If no single file is found, it then checks for separate files, one for each type of input.
   *
   * @throws PlanItException thrown if not all of network, demand and zoning input
   *           data are available
   */
  private void parseXmlRawInputs() throws PlanItException {
    final File[] xmlFileNames = FileUtils.getFilesWithExtensionFromDir(projectPath, xmlFileExtension);
    
    boolean success = parseXmlRawInputsFromSingleFile(xmlFileNames);
    if(!success) {
      success = parseXmlRawInputSeparateFiles(xmlFileNames);
    }
    
    PlanItException.throwIf(!success, String.format("The directory %s does not contain either one file with all the macroscopic inputs or a separate file for each of zoning, demand and network",projectPath));
  }

  /**
   * Checks if a single XML file containing all of network, demand and zoning
   * inputs is available, and reads it if it is.
   *
   * @param xmlFileNames array of names of XML files in the input directory
   * @return true if a single file containing all the inputs has been found and
   *         read, false otherwise
   */
  private boolean parseXmlRawInputsFromSingleFile(final File[] xmlFileNames) {
    
    XMLElementPLANit xmlRawPLANitAll = JAXBUtils.generateInstanceFromXml(XMLElementPLANit.class, xmlFileNames);
    if(xmlRawPLANitAll!= null) {
      xmlRawZoning = xmlRawPLANitAll.getMacroscopiczoning();
      xmlRawNetwork = xmlRawPLANitAll.getMacroscopicnetwork();
      xmlRawDemand = xmlRawPLANitAll.getMacroscopicdemand();
      return true;
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
  private boolean parseXmlRawInputSeparateFiles(final File[] xmlFileNames) {
    xmlRawZoning = JAXBUtils.generateInstanceFromXml(XMLElementMacroscopicZoning.class, xmlFileNames);
    xmlRawNetwork = JAXBUtils.generateInstanceFromXml(XMLElementMacroscopicNetwork.class, xmlFileNames);
    xmlRawDemand = JAXBUtils.generateInstanceFromXml(XMLElementMacroscopicDemand.class, xmlFileNames);
    return (xmlRawZoning!=null && xmlRawNetwork!=null && xmlRawDemand!=null);
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
  private void setInputFilesSeparateFilesWithValidation(final String projectPath, final File[] xmlFileNames)
      throws PlanItException {
    boolean foundZoningFile = false;
    File zoningFileName = null;
    boolean foundNetworkFile = false;
    File networkFileName = null;
    boolean foundDemandFile = false;
    File demandFileName = null;
    for (int i = 0; i < xmlFileNames.length; i++) {
      if (zoningFileName==null && validateXmlInputFile(xmlFileNames[i], ZONING_XSD_FILE)) {
          zoningFileName = xmlFileNames[i];
      }
      if (networkFileName==null && validateXmlInputFile(xmlFileNames[i], PlanitNetworkReader.NETWORK_XSD_FILE)) {
          networkFileName = xmlFileNames[i];
      }
      if (demandFileName==null && validateXmlInputFile(xmlFileNames[i], DEMAND_XSD_FILE)) {
          demandFileName = xmlFileNames[i];
      }
    }
    PlanItException.throwIf(zoningFileName==null, "Failed to find a valid zoning input file in the project directory " + projectPath);
    PlanItException.throwIf(networkFileName==null, "Failed to find a valid network input file in the project directory " + projectPath);
    PlanItException.throwIf(demandFileName==null, "Failed to find a valid demand input file in the project directory " + projectPath);
    
    LOGGER.info(LoggingUtils.getClassNameWithBrackets(this)+"file " + zoningFileName + " provides the zoning input data.");
    LOGGER.info(LoggingUtils.getClassNameWithBrackets(this)+"file " + networkFileName + " provides the network input data.");
    LOGGER.info(LoggingUtils.getClassNameWithBrackets(this)+"file " +demandFileName + " provides the demand input data.");

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
    boolean linkSegmentXmlIdPresent = false;
    boolean linkSegmentExternalIdPresent = false;
    boolean linkSegmentIdPresent = false;
    boolean upstreamNodeXmlIdPresent = false;
    boolean downstreamNodeXmlIdPresent = false;
    boolean modeXmlIdPresent = false;
    boolean costPresent = false;
    for (final String header : headers) {
      final OutputProperty outputProperty = OutputProperty.fromHeaderName(header);
      switch (outputProperty) {
        case LINK_SEGMENT_XML_ID:
          linkSegmentXmlIdPresent = true;
          break;
        case LINK_SEGMENT_EXTERNAL_ID:
          linkSegmentExternalIdPresent = true;
          break;          
        case LINK_SEGMENT_ID:
          linkSegmentIdPresent = true;
          break;
        case MODE_XML_ID:
          modeXmlIdPresent = true;
          break;
        case UPSTREAM_NODE_XML_ID:
          upstreamNodeXmlIdPresent = true;
          break;
        case DOWNSTREAM_NODE_XML_ID:
          downstreamNodeXmlIdPresent = true;
          break;
        case LINK_COST:
          costPresent = true;
      }
    }
    PlanItException.throwIf(!costPresent, "Cost column not present in initial link segment costs file");
    PlanItException.throwIf(!modeXmlIdPresent, "Mode xml Id not present in initial link segment costs file");
   
    if (linkSegmentXmlIdPresent) {
      return OutputProperty.LINK_SEGMENT_XML_ID;
    }
    if (linkSegmentExternalIdPresent) {
      return OutputProperty.LINK_SEGMENT_EXTERNAL_ID;
    }    
    if (linkSegmentIdPresent) {
      return OutputProperty.LINK_SEGMENT_ID;
    }
    if (upstreamNodeXmlIdPresent && downstreamNodeXmlIdPresent) {
      return OutputProperty.UPSTREAM_NODE_XML_ID;
    }
    
    throw new PlanItException("Links not correctly identified in initial link segment costs file");
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
   * @throws PlanItException thrown if error
   */
  private void setInitialLinkSegmentCost(final InitialLinkSegmentCost initialLinkSegmentCost, final CSVRecord record,
      final MacroscopicLinkSegment linkSegment) throws PlanItException {
    
    final String modeXmlId = record.get(ModeXmlIdOutputProperty.NAME);
    final Mode mode = getModeBySourceId(modeXmlId);
    PlanItException.throwIf(mode == null, "mode xml id not available in configuration");
    
    final double cost = Double.parseDouble(record.get(LinkCostOutputProperty.NAME));
    initialLinkSegmentCost.setSegmentCost(mode, linkSegment, cost);
  }
  
  /**
   * Creates the physical network object from the data in the input file
   *
   * @param physicalNetwork the physical network object to be populated from the input data
   * @throws PlanItException thrown if there is an error reading the input file
   */
  protected void populateNetwork(MacroscopicNetwork network) throws PlanItException {
    LOGGER.fine(LoggingUtils.getClassNameWithBrackets(this)+"populating Network");
    
    /* parse raw inputs if not already done */
    if(xmlRawNetwork == null) {
      parseXmlRawInputs();
    }
        
    /* create parser and read/populate the network */
    PlanitNetworkReader reader = PlanitNetworkReaderFactory.createReader(xmlRawNetwork, network);
    /* make sure the external ids are indexed via the input builder's already present maps */
    reader.getSettings().setMapToIndexLinkSegmentByXmlIds(sourceIdLinkSegmentMap);
    reader.getSettings().setMapToIndexLinkSegmentTypeByXmlIds(sourceIdLinkSegmentTypeMap);
    reader.getSettings().setMapToIndexModeByXmlIds(sourceIdModeMap);
    reader.getSettings().setMapToIndexNodeByXmlIds(sourceIdNodeMap);
    /* pass on relevant general settings to reader settings */
    reader.getSettings().setErrorIfDuplicatexXmlId(isErrorIfDuplicateSourceId());
    network = reader.read();        
  }

  /**
   * Creates the Zoning object and connectoids from the data in the input file
   *
   * @param zoning the Zoning object to be populated from the input data
   * @param parameter1 PhysicalNetwork object previously defined
   * @throws PlanItException thrown if there is an error reading the input file
   */
  protected void populateZoning(final Zoning zoning, final MacroscopicNetwork physicalNetwork) throws PlanItException {
    LOGGER.fine(LoggingUtils.getClassNameWithBrackets(this)+"populating Zoning");
    
    /* parse raw inputs if not already done */
    if(xmlRawNetwork == null) {
      parseXmlRawInputs();
    }
    
    // create and register zones, centroids and connectoids
    try {
      PlanitJtsUtils jtsUtils = new PlanitJtsUtils(physicalNetwork.getCoordinateReferenceSystem());
      
      /* zone */
      for (final XMLElementZones.Zone xmlZone : xmlRawZoning.getZones().getZone()) {
        Zone zone = zoning.zones.createAndRegisterNewZone();
        addZoneToSourceIdMap(xmlZone.getId(), zone);
        
        /* xml id */
        if(xmlZone.getId() != null && !xmlZone.getId().isBlank()) {
          zone.setXmlId(xmlZone.getId());
        }
        
        /* external id */        
        if(xmlZone.getExternalid() != null && !xmlZone.getExternalid().isBlank()) {
          zone.setExternalId(xmlZone.getExternalid());  
        }                  
      
        
        /* centroid */
        Centroid centroid = zone.getCentroid();
        if (xmlZone.getCentroid().getPoint() != null) {
          List<Double> value = xmlZone.getCentroid().getPoint().getPos().getValue();        
          centroid.setPosition(PlanitJtsUtils.createPoint(value.get(0), value.get(1)));
        }
             
        /* connectoids */
        List<XMLElementConnectoid> xmlConnectoids = xmlZone.getConnectoids().getConnectoid();
        for(XMLElementConnectoid xmlConnectoid : xmlConnectoids) {
          Node node = getNodeBySourceId(xmlConnectoid.getNoderef());
          Point nodePosition = node.getPosition();
          
          double connectoidLength;
          if (xmlConnectoid.getLength() != null) {
            connectoidLength = xmlConnectoid.getLength();
            // :TODO - need to create some test cases in which nodes have a GML location
          } else if (nodePosition != null) {
            // if node has a GML Point, get the GML Point from the centroid and calculate the length
            // between them
            connectoidLength = jtsUtils.getDistanceInKilometres(centroid.getPosition(), nodePosition);
          } else {
            connectoidLength = org.planit.utils.network.virtual.Connectoid.DEFAULT_LENGTH_KM;
          }
                    
          Connectoid theConnectoid = zoning.getVirtualNetwork().connectoids.registerNewConnectoid(centroid, node, connectoidLength);

          /* xml id */
          if(xmlConnectoid.getId() != null && xmlConnectoid.getId().isBlank()) {
            theConnectoid.setXmlId(xmlConnectoid.getId());
          }
          
          /* external id */
          if(xmlConnectoid.getExternalid() != null && !xmlConnectoid.getExternalid().isBlank()) {
            theConnectoid.setExternalId(xmlConnectoid.getExternalid());
          }   
        }             
      }
    } catch (PlanItException e) {
      throw e;
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when populating zoning in PLANitIO",e);
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
  protected void populateDemands( Demands demands, final Object parameter1, final Object parameter2) throws PlanItException {
    LOGGER.fine(LoggingUtils.getClassNameWithBrackets(this)+"populating Demands");
    PlanItException.throwIf(!(parameter1 instanceof Zoning),"Parameter 1 of call to populateDemands() is not of class Zoning");
    PlanItException.throwIf(!(parameter2 instanceof PhysicalNetwork),"Parameter 2 of call to populateDemands() is not of class PhysicalNetwork");
    
    /* parse raw inputs if not already done */
    if(xmlRawNetwork == null) {
      parseXmlRawInputs();
    }    
    
    final Zoning zoning = (Zoning) parameter1;
    final PhysicalNetwork<?,?,?> physicalNetwork = (PhysicalNetwork<?,?,?>) parameter2;
    try {
      final XMLElementDemandConfiguration demandconfiguration = xmlRawDemand.getDemandconfiguration();
      ProcessConfiguration.generateAndStoreConfigurationData(demands, demandconfiguration, physicalNetwork, this);
      final List<XMLElementOdMatrix> oddemands = xmlRawDemand.getOddemands().getOdcellbycellmatrixOrOdrowmatrixOrOdrawmatrix();
      DemandsPopulator.createAndRegisterDemandMatrix(demands, oddemands, zoning.zones, this);
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when populating demands in PLANitIO",e);
    }
  }

  /**
   * Populate the initial link segment cost from a CSV file
   *
   * @param initialLinkSegmentCost InitialLinkSegmentCost object to be populated
   * @param parameter1 previously created network object
   * @param parameter2 CSV file containing the initial link segment cost values
   * @throws PlanItException thrown if error
   */
  protected void populateInitialLinkSegmentCost(final InitialLinkSegmentCost initialLinkSegmentCost,
      final Object parameter1, final Object parameter2)
      throws PlanItException {
    LOGGER.fine(LoggingUtils.getClassNameWithBrackets(this)+"populating Initial Link Segment Costs");
    
    PlanItException.throwIf(!(parameter1 instanceof PhysicalNetwork),"Parameter 1 of call to populateInitialLinkSegments() is not of class PhysicalNework");
    PlanItException.throwIf(!(parameter2 instanceof String), "Parameter 2 of call to populateInitialLinkSegments() is not a file name");
        
    final MacroscopicNetwork network = (MacroscopicNetwork) parameter1;
    final String fileName = (String) parameter2;
    try {
      final Reader in = new FileReader(fileName);
      final CSVParser parser = CSVParser.parse(in, CSVFormat.DEFAULT.withFirstRecordAsHeader());
      final Set<String> headers = parser.getHeaderMap().keySet();
      final OutputProperty linkIdentificationMethod = getLinkIdentificationMethod(headers);
      for (final CSVRecord record : parser) {
        MacroscopicLinkSegment linkSegment = null;
        switch (linkIdentificationMethod) {
          case LINK_SEGMENT_ID:
            final long id = Long.parseLong(record.get(LinkSegmentIdOutputProperty.NAME));
            linkSegment = network.linkSegments.get(id);            
            break;
          case LINK_SEGMENT_XML_ID:
            final String xmlId = record.get(LinkSegmentXmlIdOutputProperty.NAME);
            linkSegment = getLinkSegmentBySourceId(xmlId);
            break;          
          case UPSTREAM_NODE_XML_ID:
            final Node startNode = getNodeBySourceId(record.get(UpstreamNodeXmlIdOutputProperty.NAME));
            final Node endNode = getNodeBySourceId(record.get(DownstreamNodeXmlIdOutputProperty.NAME));
            linkSegment = startNode.getLinkSegment(endNode);
            break;
          default:
            throw new PlanItException("Invalid Output Property "
                + BaseOutputProperty.convertToBaseOutputProperty(linkIdentificationMethod).getName()
                + " found in header of Initial Link Segment Cost CSV file");
       }
       PlanItException.throwIf(linkSegment == null, String.format("failed to find link segment for record %d", record.getRecordNumber() ));        
       setInitialLinkSegmentCost(initialLinkSegmentCost, record, linkSegment);        
      }
      in.close();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when initialising link segment costs in PLANitIO",e);
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
    this(projectPath, DEFAULT_XML_FILE_EXTENSION);
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
  public PlanItInputBuilder(final String projectPath, final String xmlFileExtension) throws PlanItException {
    super();
    LOGGER.info(LoggingUtils.getClassNameWithBrackets(this)+"project path is set to: "+ projectPath);
    
    this.projectPath = projectPath;
    this.xmlFileExtension = xmlFileExtension;
    
  }

  /**
   * Validates an input XML file against an XSD file
   *
   * @param xmlFileLocation input XML file
   * @param schemaFileLocation XSD file to validate XML file against
   * @return true if the file is valid, false otherwise
   */
  public static boolean validateXmlInputFile(final File xmlFileLocation, final String schemaFileLocation) {
    try {
      JAXBUtils.validateXml(xmlFileLocation, schemaFileLocation);
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
   * @throws RemoteException thrown if there is an error
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
        if (projectComponent instanceof MacroscopicNetwork) {
          populateNetwork((MacroscopicNetwork) projectComponent);
        } else if (projectComponent instanceof Zoning) {
          PlanItException.throwIf(!(parameters[0] instanceof MacroscopicNetwork), "Parameter of call to populateZoning() is not of class PhysicalNetwork");
          final MacroscopicNetwork physicalNetwork = (MacroscopicNetwork) parameters[0];
          populateZoning((Zoning) projectComponent, physicalNetwork);
        } else if (projectComponent instanceof Demands) {
          populateDemands((Demands) projectComponent, parameters[0], parameters[1]);
        } else if (projectComponent instanceof InitialPhysicalCost) {
          populateInitialLinkSegmentCost((InitialLinkSegmentCost) projectComponent, parameters[0], parameters[1]);
        } else {
          LOGGER.fine("Event component is " + projectComponent.getClass().getCanonicalName()
              + " which is not handled by PlanItInputBuilder.");
        }
      } catch (final PlanItException e) {
        LOGGER.severe(e.getMessage());
        throw new RemoteException("Rethrowing as remote exception in notify", e);
      }
    }
  }

}