package org.goplanit.io.input;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.goplanit.component.event.PlanitComponentEvent;
import org.goplanit.component.event.PopulateDemandsEvent;
import org.goplanit.component.event.PopulateInitialLinkSegmentCostEvent;
import org.goplanit.component.event.PopulateNetworkEvent;
import org.goplanit.component.event.PopulateRoutedServicesEvent;
import org.goplanit.component.event.PopulateServiceNetworkEvent;
import org.goplanit.component.event.PopulateZoningEvent;
import org.goplanit.cost.physical.initial.InitialMacroscopicLinkSegmentCost;
import org.goplanit.demands.Demands;
import org.goplanit.input.InputBuilderListener;
import org.goplanit.io.converter.demands.PlanitDemandsReader;
import org.goplanit.io.converter.network.PlanitNetworkReader;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.io.converter.service.PlanitRoutedServicesReader;
import org.goplanit.io.converter.service.PlanitRoutedServicesReaderFactory;
import org.goplanit.io.converter.service.PlanitServiceNetworkReader;
import org.goplanit.io.converter.service.PlanitServiceNetworkReaderFactory;
import org.goplanit.io.converter.zoning.PlanitZoningReader;
import org.goplanit.io.converter.zoning.PlanitZoningReaderFactory;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.xml.utils.JAXBUtils;
import org.goplanit.io.xml.util.PlanitXmlJaxbParser;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.output.property.DownstreamNodeXmlIdOutputProperty;
import org.goplanit.output.property.LinkSegmentCostOutputProperty;
import org.goplanit.output.property.LinkSegmentExternalIdOutputProperty;
import org.goplanit.output.property.LinkSegmentXmlIdOutputProperty;
import org.goplanit.output.property.ModeXmlIdOutputProperty;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.output.property.OutputPropertyType;
import org.goplanit.output.property.UpstreamNodeXmlIdOutputProperty;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.misc.FileUtils;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.wrapper.MapWrapper;
import org.goplanit.utils.wrapper.MapWrapperImpl;
import org.goplanit.xml.generated.*;
import org.goplanit.zoning.Zoning;

/**
 * Class which reads inputs from XML input files
 *
 * @author gman6028
 *
 */
public class PlanItInputBuilder extends InputBuilderListener {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanItInputBuilder.class.getCanonicalName());
    
  /** Generated object to store demand input data */
  private XMLElementMacroscopicDemand xmlRawDemand;

  /** Generated object to store zoning input data */
  private XMLElementMacroscopicZoning xmlRawZoning;
  
  /** Generated object to store network input data */  
  private XMLElementMacroscopicNetwork xmlRawNetwork;
  
  /** Generated object to store optional service network input data */
  private XMLElementServiceNetwork xmlRawServiceNetwork = null;
  
  /** Generated object to store optional routed services input data */
  private XMLElementRoutedServices xmlRawRoutedServices = null;    
    
  
  /** Project path to use */
  private final String projectPath;
  
  /** XML file extension to use */
  private final String xmlFileExtension;
   
           
  /**
   * Populate the input objects from specified XML files
   *
   * @param zoningXmlFileLocation location of the zoning input XML file
   * @param demandXmlFileLocation location of the demand input XML file
   * @param networkXmlFileLocation location of the network input XML file
   * @throws PlanItException thrown if there is an error during reading the files
   */
  private void createGeneratedClassesFromXmlLocations(final File zoningXmlFileLocation, final File demandXmlFileLocation, final File networkXmlFileLocation) throws PlanItException {
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
   * This method checks if a single file contains all mandatory components of a project; network, demand, and zoning. 
   * If no single file is found, it then checks for separate files, one for each type of input.
   *
   * @throws PlanItException thrown if not all of network, demand and zoning input data are available
   */
  private void parseXmlRawInputs() throws PlanItException {
    final File[] xmlFileNames = FileUtils.getFilesWithExtensionFromDir(projectPath, xmlFileExtension);
    
    boolean success = parseXmlRawInputsFromSingleFile(xmlFileNames);
    if(!success) {
      success = parseXmlRawInputSeparateFiles(xmlFileNames);
    }
    
    PlanItException.throwIf(!success, String.format("Directory %s does not contain file with all inputs nor separate files for zoning, demand, and network",projectPath));
  }

  /**
   * Checks if a single XML file containing all of network, demand and zoning
   * inputs is available, and reads it if it is.
   *
   * @param xmlFileNames array of names of XML files in the input directory
   * @return true if a single file containing all the inputs has been found and read, false otherwise
   */
  private boolean parseXmlRawInputsFromSingleFile(final File[] xmlFileNames) {
    
    XMLElementPLANit xmlRawPLANitAll = JAXBUtils.generateInstanceFromXml(XMLElementPLANit.class, xmlFileNames);
    if(xmlRawPLANitAll!= null) {
      xmlRawZoning = xmlRawPLANitAll.getMacroscopiczoning();
      xmlRawNetwork = xmlRawPLANitAll.getMacroscopicnetwork();
      xmlRawDemand = xmlRawPLANitAll.getMacroscopicdemand();
      if(xmlRawPLANitAll.getServicenetwork()!=null) {
        xmlRawServiceNetwork= xmlRawPLANitAll.getServicenetwork();
      }
      if(xmlRawPLANitAll.getRoutedservices()!=null) {
        xmlRawRoutedServices = xmlRawPLANitAll.getRoutedservices();
      }      
      return true;
    }
    return false;
  }
  
  /**
   * Populate the generated input objects from three separate XML files
   *
   * @param xmlFileNames array of names of XML files in the input directory
   * @return true if input demand, zoning and network file are found in xmlFileNames, false otherwise
   */
  private boolean parseXmlRawInputSeparateFiles(final File[] xmlFileNames) {
    xmlRawZoning = JAXBUtils.generateInstanceFromXml(XMLElementMacroscopicZoning.class, xmlFileNames);
    xmlRawNetwork = JAXBUtils.generateInstanceFromXml(XMLElementMacroscopicNetwork.class, xmlFileNames);
    xmlRawDemand = JAXBUtils.generateInstanceFromXml(XMLElementMacroscopicDemand.class, xmlFileNames);
    return (xmlRawZoning!=null && xmlRawNetwork!=null && xmlRawDemand!=null);
  }

  /**
   * Populate the generated input objects from three separate XML files by validating the input files first.
   *
   * This file does the same task as setInputFilesSeparateFiles(), it does it in a different way. This method runs much more slowly than
   * setInputFilesSeparateFiles(), it takes about 60 times as long for the same input data sets.
   *
   * @param projectPath the name of the project path directory
   * @throws PlanItException thrown if one or more of the input objects could not be populated from the XML files in the project directory
   */
  @SuppressWarnings("unused")
  private void setInputFilesSeparateFilesWithValidation(final String projectPath, final File[] xmlFileNames) throws PlanItException {
    
    File zoningFileName = null;
    File networkFileName = null;
    File demandFileName = null;
    boolean serviceNetworkFileDone = false;
    boolean routedServicesFileDone = false;
    for (int i = 0; i < xmlFileNames.length; i++) {
      if (zoningFileName==null && validateXmlInputFile(xmlFileNames[i], PlanitZoningReader.ZONING_XSD_FILE)) {
          zoningFileName = xmlFileNames[i];
          LOGGER.info(LoggingUtils.getClassNameWithBrackets(this)+"file " + zoningFileName + " provides the zoning input data.");
          continue;
      }
      if (networkFileName==null && validateXmlInputFile(xmlFileNames[i], PlanitNetworkReader.NETWORK_XSD_FILE)) {
          networkFileName = xmlFileNames[i];
          LOGGER.info(LoggingUtils.getClassNameWithBrackets(this)+"file " + networkFileName + " provides the network input data.");
          continue;
      }
      if (demandFileName==null && validateXmlInputFile(xmlFileNames[i], PlanitDemandsReader.DEMAND_XSD_FILE)) {
          demandFileName = xmlFileNames[i];
          LOGGER.info(LoggingUtils.getClassNameWithBrackets(this)+"file " +demandFileName + " provides the demand input data.");          
          continue;
      }
      if (!serviceNetworkFileDone && validateXmlInputFile(xmlFileNames[i], PlanitServiceNetworkReader.SERVICE_NETWORK_XSD_FILE)) {
        serviceNetworkFileDone = true;
        LOGGER.info(LoggingUtils.getClassNameWithBrackets(this)+"file " +xmlFileNames[i] + " provides the service network input data.");
        continue;
      }
      if (!routedServicesFileDone && validateXmlInputFile(xmlFileNames[i], PlanitRoutedServicesReader.ROUTED_SERVICES_XSD_FILE)) {
        routedServicesFileDone = true;
        LOGGER.info(LoggingUtils.getClassNameWithBrackets(this)+"file " +xmlFileNames[i] + " provides the routed services input data.");
        continue;
      }      
    }
    PlanItException.throwIfNull(zoningFileName, "Failed to find a valid zoning input file in directory %s", projectPath);
    PlanItException.throwIfNull(networkFileName, "Failed to find a valid network input file in directory %s", projectPath);
    PlanItException.throwIfNull(demandFileName, "Failed to find a valid demand input file in directory %s", projectPath);     

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
  private OutputPropertyType getInitialCostLinkIdentificationMethod(final Set<String> headers) throws PlanItException {
    boolean linkSegmentXmlIdPresent = false;
    boolean linkSegmentExternalIdPresent = false;
    boolean upstreamNodeXmlIdPresent = false;
    boolean downstreamNodeXmlIdPresent = false;
    boolean modeXmlIdPresent = false;
    boolean costPresent = false;
    for (final String header : headers) {
      final OutputPropertyType outputProperty = OutputPropertyType.fromHeaderName(header);
      switch (outputProperty) {
        case LINK_SEGMENT_XML_ID:
          linkSegmentXmlIdPresent = true;
          break;
        case LINK_SEGMENT_EXTERNAL_ID:
          linkSegmentExternalIdPresent = true;
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
        case LINK_SEGMENT_COST:
          costPresent = true;
      }
    }
    PlanItException.throwIf(!costPresent, "Cost column not present in initial link segment costs file");
    PlanItException.throwIf(!modeXmlIdPresent, "Mode xml Id not present in initial link segment costs file");
   
    if (linkSegmentXmlIdPresent) {
      return OutputPropertyType.LINK_SEGMENT_XML_ID;
    }
    if (linkSegmentExternalIdPresent) {
      return OutputPropertyType.LINK_SEGMENT_EXTERNAL_ID;
    }    
    if (upstreamNodeXmlIdPresent && downstreamNodeXmlIdPresent) {
      return OutputPropertyType.UPSTREAM_NODE_XML_ID;
    }
    
    throw new PlanItException("Links not correctly identified in initial link segment costs file");
  }

  /**
   * Set the initial link segment cost for the specified link segment using values
   * in the CSV initial segment costs file
   *
   * @param initialLinkSegmentCost the InitialLinkSegmentCost object to store the cost value
   * @param record the record in the CSV input file to get the data value from
   * @param linkSegment the current link segment
   * @param timePeriod to use (may be null)
   */
  private void setPhysicalInitialLinkSegmentCost(
          final InitialMacroscopicLinkSegmentCost initialLinkSegmentCost,
          final CSVRecord record,
          final MacroscopicLinkSegment linkSegment,
          final TimePeriod timePeriod) {
    
    final String modeXmlId = record.get(ModeXmlIdOutputProperty.NAME);

    /* mode check */
    Mode matchMode = null;
    //TODO: slow! create mapping by source id one-off and use that instead
    for(Mode  mode : linkSegment.getAllowedModes()) {
      if(mode.getXmlId().equals(modeXmlId)) {
        matchMode = mode;
        break;
      }
    }
    if(matchMode == null) {
     throw new PlanItRunTimeException("Mode xml id not supported by link segment used for initial link segment cost");
    }
    
    final double cost = Double.parseDouble(record.get(LinkSegmentCostOutputProperty.NAME));
    
    if(timePeriod==null) {
      initialLinkSegmentCost.setSegmentCost(matchMode, linkSegment, cost);
    }else {
      initialLinkSegmentCost.setSegmentCost(timePeriod, matchMode, linkSegment, cost);
    }
  }         
  
  /**
   * Creates the macroscopic network object from the data in the input file
   *
   * @param network the infrastructure network object to be populated from the input data
   * @throws PlanItException thrown if there is an error reading the input file
   */
  protected void populateMacroscopicNetwork(MacroscopicNetwork network) throws PlanItException {
    LOGGER.fine(LoggingUtils.getClassNameWithBrackets(this)+"populating network");
    
    /* parse raw inputs if not already done */
    if(xmlRawNetwork == null) {
      parseXmlRawInputs();
    }
        
    /* create parser and read/populate the network */
    PlanitNetworkReader networkReader = PlanitNetworkReaderFactory.create(xmlRawNetwork, network);
    networkReader.getSettings().setInputDirectory(projectPath);
    networkReader.read();  
    
    xmlRawNetwork=null;
  }

  /**
   * Creates the Zoning object and connectoids from the data in the input file
   *
   * @param zoning the Zoning object to be populated from the input data
   * @param network PhysicalNetwork object previously defined
   * @throws PlanItException thrown if there is an error reading the input file
   */
  protected void populateZoning(final Zoning zoning, final MacroscopicNetwork network) throws PlanItException {
    LOGGER.fine(LoggingUtils.getClassNameWithBrackets(this)+"populating Zoning");
    
    /** delegate to the dedicated zoning reader */
    PlanitZoningReader zoningReader = PlanitZoningReaderFactory.create(xmlRawZoning, network, zoning);
    zoningReader.getSettings().setInputDirectory(projectPath);    
    zoningReader.read();
    
    xmlRawZoning = null;
  }

  /**
   * Populates the Demands object from the input file
   *
   * @param demands the Demands object to be populated from the input data
   * @param zoning Zoning object previously defined
   * @param network network object previously defined
   * @throws PlanItException thrown if there is an error reading the input file
   */
  protected void populateDemands( Demands demands, final Zoning zoning, final MacroscopicNetwork network) throws PlanItException {
    LOGGER.fine(LoggingUtils.getClassNameWithBrackets(this)+"populating Demands");     
       
    /* delegate to the dedicated demands reader */
    PlanitDemandsReader demandsReader = new PlanitDemandsReader(xmlRawDemand, network, zoning, demands);
    demandsReader.getSettings().setInputDirectory(projectPath);
    demandsReader.read();
    
    xmlRawDemand = null;
  }

  /** Populate the routed services based on the local XML file if it can be found.
   * 
   * @param routedServicesToPopulate to instance to populate
   * @throws PlanItException thrown if error
   */
  protected void populateRoutedServices(final RoutedServices routedServicesToPopulate) throws PlanItException {
    
    /* parse raw inputs if not already done, because routed services are optional, they have not been parsed unless they were part of
     * a combined input XML that contained other parts of the definitions */
    if(xmlRawRoutedServices == null) {
      xmlRawRoutedServices = (XMLElementRoutedServices ) JAXBUtils.generateInstanceFromXml(XMLElementRoutedServices.class, FileUtils.getFilesWithExtensionFromDir(projectPath, xmlFileExtension));
    }
    if(xmlRawRoutedServices == null) {
      LOGGER.severe("Unable to locate routed services XML input");
      return;
    }
    
    /* prep reader */
    PlanitRoutedServicesReader routedServicesReader = PlanitRoutedServicesReaderFactory.create(xmlRawRoutedServices, routedServicesToPopulate);  
    routedServicesReader.read();
    
    xmlRawRoutedServices = null;
  }

  /** Populate the service network based on the local XML file if it can be found.
   * 
   * @param serviceNetworkToPopulate to instance to populate
   * @throws PlanItException thrown if error
   */  
  protected void populateServiceNetwork(final ServiceNetwork serviceNetworkToPopulate) throws PlanItException {
    
    /* parse raw inputs if not already done, because routed services are optional, they have not been parsed unless they were part of
     * a combined input XML that contained other parts of the definitions */
    if(xmlRawServiceNetwork== null) {
      xmlRawServiceNetwork = (XMLElementServiceNetwork ) JAXBUtils.generateInstanceFromXml(XMLElementServiceNetwork.class, FileUtils.getFilesWithExtensionFromDir(projectPath, xmlFileExtension));
    }
    if(xmlRawServiceNetwork == null) {
      LOGGER.severe("Unable to locate service network XML input");
      return;
    }
    
    /* prep reader */
    PlanitServiceNetworkReader serviceNetworkReader = PlanitServiceNetworkReaderFactory.create(xmlRawServiceNetwork, serviceNetworkToPopulate);
    serviceNetworkReader.getSettings().setInputDirectory(projectPath);
    /* perform parse action */
    serviceNetworkReader.read();
    
    xmlRawServiceNetwork = null;
  }

  /**
   * Populate the initial link segment cost from a CSV file
   *
   * @param initialCostEvent to extract context from to populate its component that it carries
   */
  protected void populatePhysicalInitialLinkSegmentCost(final PopulateInitialLinkSegmentCostEvent initialCostEvent) {
    LOGGER.fine(LoggingUtils.getClassNameWithBrackets(this)+"populating Initial Link Segment Costs");
    
    MacroscopicNetwork network = initialCostEvent.getParentNetwork();
    String fileName = initialCostEvent.getFileName();
    
    /* verify */
    PlanItRunTimeException.throwIfNull(network,"parent network for initial link segment cost is null");
    PlanItRunTimeException.throwIfNull(fileName, "file location for initial link segment cost is null");
        
    /* parse */
    try {
      final Reader in = new FileReader(fileName);
      final CSVParser parser = CSVParser.parse(in, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreSurroundingSpaces());
      final Set<String> headers = parser.getHeaderMap().keySet();
      
      /* populate this */
      var initialLinkSegmentCost = initialCostEvent.getInitialLinkSegmentCostToPopulate();

      /* lay index by reference method */
      final OutputPropertyType linkIdentificationMethod = getInitialCostLinkIdentificationMethod(headers);
      if(linkIdentificationMethod.equals(OutputPropertyType.UPSTREAM_NODE_XML_ID)) {
        
        /* special case requiring double key */
        Map<String, Map<String, MacroscopicLinkSegment>> indexByIdentificationMethod = new HashMap<>();
        for( MacroscopicNetworkLayer layer : network.getTransportLayers()) {
          for(MacroscopicLinkSegment linkSegment : layer.getLinkSegments()) {
            Vertex upstreamNode = linkSegment.getUpstreamVertex();
            indexByIdentificationMethod.putIfAbsent(upstreamNode.getXmlId(), new HashMap<>());
            indexByIdentificationMethod.get(upstreamNode.getXmlId()).put(linkSegment.getDownstreamVertex().getXmlId(), linkSegment);
          }
        }
        
        /* parse */
        for (final CSVRecord record : parser) {
          MacroscopicLinkSegment linkSegment = indexByIdentificationMethod.get(record.get(UpstreamNodeXmlIdOutputProperty.NAME)).get(record.get(DownstreamNodeXmlIdOutputProperty.NAME));         
          PlanItException.throwIfNull(linkSegment, "failed to find link segment for record %d", record.getRecordNumber());        
          setPhysicalInitialLinkSegmentCost(initialLinkSegmentCost, record, linkSegment, initialCostEvent.getTimePeriod());
        }
        
      }else {
        /* single key mapping */
        MapWrapper<Object, MacroscopicLinkSegment> indexByIdentificationMethod = null;
        String identificationColumnName = null;    
        switch (linkIdentificationMethod) {
          case LINK_SEGMENT_EXTERNAL_ID:
            indexByIdentificationMethod = new MapWrapperImpl<>(new HashMap<>(), MacroscopicLinkSegment::getExternalId);
            identificationColumnName = LinkSegmentExternalIdOutputProperty.NAME;
            break;            
          case LINK_SEGMENT_XML_ID:
            indexByIdentificationMethod = new MapWrapperImpl<>(new HashMap<>(), MacroscopicLinkSegment::getXmlId);
            identificationColumnName = LinkSegmentXmlIdOutputProperty.NAME;
            break;          
          default:
            throw new PlanItRunTimeException("Invalid Output Property "
                + OutputProperty.of(linkIdentificationMethod).getName()
                + " found in header of Initial Link Segment Cost CSV file");
        }    
        /* lay index */
        for(MacroscopicNetworkLayer layer : network.getTransportLayers()) {
          indexByIdentificationMethod.addAll(layer.getLinkSegments());
        }
        
        /* parse */
        for (final CSVRecord record : parser) {
          MacroscopicLinkSegment linkSegment = indexByIdentificationMethod.get(record.get(identificationColumnName));
          if(linkSegment == null) {
            throw new PlanItRunTimeException("Failed to find link segment for record %d", record.getRecordNumber());
          }
          setPhysicalInitialLinkSegmentCost(initialLinkSegmentCost, record, linkSegment, initialCostEvent.getTimePeriod());
        }
      }   
      in.close();
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException("Error when initialising link segment costs in PLANitIO",e);
    }
  }
  
  /** The default separator that is assumed when no separator is provided */
  public static final String DEFAULT_SEPARATOR = ",";  


  /**
   * Constructor which generates the input objects from files in a specified
   * directory, using the default extension ".xml"
   *
   * @param projectPath the location of the input file directory
   * @throws PlanItException thrown if one of the input required input files cannot be found, or if there is an error reading one of them
   */
  public PlanItInputBuilder(final String projectPath) throws PlanItException {
    this(projectPath, PlanitXmlJaxbParser.DEFAULT_XML_FILE_EXTENSION);
  }

  /**
   * Constructor which generates the input objects from files in a specified
   * directory
   *
   * @param projectPath the location of the input file directory
   * @param xmlFileExtension the extension of the data files to be searched through
   * @throws PlanItException thrown if one of the input required input files cannot be found, or if there is an error reading one of them
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
   * @throws PlanItException thrown if error
   */
  @Override
  public void onPlanitComponentEvent(PlanitComponentEvent event) throws PlanItException {
    // registered for create notifications
    if (event.getType().equals(PopulateNetworkEvent.EVENT_TYPE)) {
      /* NETWORK */
      populateMacroscopicNetwork(((PopulateNetworkEvent)event).getNetworkToPopulate());
    }else if(event.getType().equals(PopulateZoningEvent.EVENT_TYPE)){
      /* ZONING */
      PopulateZoningEvent zoningEvent = ((PopulateZoningEvent) event);
      populateZoning(zoningEvent.getZoningToPopulate(), zoningEvent.getParentNetwork());
    }else if(event.getType().equals(PopulateDemandsEvent.EVENT_TYPE)){
      /* DEMANDS */
      PopulateDemandsEvent demandsEvent = ((PopulateDemandsEvent) event);
      populateDemands(demandsEvent.getDemandsToPopulate(), demandsEvent.getParentZoning(), demandsEvent.getParentNetwork());
    }else if(event.getType().equals(PopulateInitialLinkSegmentCostEvent.EVENT_TYPE)){
      /* INITIAL COST */
      PopulateInitialLinkSegmentCostEvent initialCostEvent = ((PopulateInitialLinkSegmentCostEvent) event);
      populatePhysicalInitialLinkSegmentCost(initialCostEvent);
    }else if(event.getType().equals(PopulateServiceNetworkEvent.EVENT_TYPE)){
      /* SERVICE NETWORK */
      PopulateServiceNetworkEvent serviceNetworkEvent = ((PopulateServiceNetworkEvent) event);
      populateServiceNetwork(serviceNetworkEvent.getServiceNetworkToPopulate());
    }else if(event.getType().equals(PopulateRoutedServicesEvent.EVENT_TYPE)){
      /* ROUTED SERVICES */
      PopulateRoutedServicesEvent routedServicesEvent = ((PopulateRoutedServicesEvent) event);
      populateRoutedServices(routedServicesEvent.getRoutedServicesToPopulate());
    } else {      
      /* generic case */
      LOGGER.fine("Event component " + event.getClass().getCanonicalName() + " ignored by PlanItInputBuilder");
    }
  }

}