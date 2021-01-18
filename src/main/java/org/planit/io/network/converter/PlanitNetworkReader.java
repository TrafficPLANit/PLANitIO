package org.planit.io.network.converter;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.geo.PlanitJtsUtils;
import org.planit.geo.PlanitOpenGisUtils;
import org.planit.io.xml.network.XmlMacroscopicNetworkLayerHelper;
import org.planit.io.xml.util.EnumConversionUtil;
import org.planit.io.xml.util.JAXBUtils;
import org.planit.mode.ModeFeaturesFactory;
import org.planit.network.InfrastructureLayer;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.converter.NetworkReader;
import org.planit.network.macroscopic.MacroscopicNetwork;
import org.planit.network.macroscopic.physical.MacroscopicPhysicalNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.misc.CharacterUtils;
import org.planit.utils.misc.FileUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.MotorisationModeType;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.PredefinedModeType;
import org.planit.utils.mode.TrackModeType;
import org.planit.utils.mode.UsabilityModeFeatures;
import org.planit.utils.mode.UseOfModeType;
import org.planit.utils.mode.VehicularModeType;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.xml.generated.XMLElementInfrastructureLayer;
import org.planit.xml.generated.XMLElementInfrastructureLayers;
import org.planit.xml.generated.XMLElementLinkConfiguration;
import org.planit.xml.generated.XMLElementLinkSegmentType;
import org.planit.xml.generated.XMLElementLinkSegmentTypes;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;
import org.planit.xml.generated.XMLElementModes;

/**
 * Implementation of the network reader for the PLANit XML native format
 * 
 * @author gman, markr
 *
 */
public class PlanitNetworkReader implements NetworkReader {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkReader.class.getCanonicalName());            
  
  /** the settings for this reader */
  private final PlanitNetworkReaderSettings settings = new PlanitNetworkReaderSettings();
    
  /**
   * Default XSD files used to validate input XML files against
   * TODO: replace with online schema
   */
  public static final String NETWORK_XSD_FILE = "src\\main\\resources\\xsd\\macroscopicnetworkinput.xsd";
  
  /** network directory to look in */
  private final String networkPathDirectory;
  
  /** xml file extension to use */
  private final String xmlFileExtension;  
  
  /** the network memory model to populate */
  private MacroscopicNetwork network;
  
  /** external xmlRawNetwork to populate */
  private final XMLElementMacroscopicNetwork externalXmlRawNetwork;
  
  /**
   * Update the XML macroscopic network element to include default values for any properties not included in the input file
   */
  private void injectMissingDefaultsToRawXmlNetwork(XMLElementMacroscopicNetwork xmlRawNetwork) {
    if (xmlRawNetwork.getLinkconfiguration() == null) {
      xmlRawNetwork.setLinkconfiguration(new XMLElementLinkConfiguration());
    }
    
    //if no modes defined, create single mode with default values
    if (xmlRawNetwork.getLinkconfiguration().getModes() == null) {
      xmlRawNetwork.getLinkconfiguration().setModes(new XMLElementModes());
      XMLElementModes.Mode xmlElementMode = new XMLElementModes.Mode();
      // default in absence of any modes is the predefined CAR mode
      xmlElementMode.setPredefined(true);
      xmlElementMode.setName(PredefinedModeType.CAR.value());
      xmlElementMode.setId(Mode.DEFAULT_XML_ID);
      xmlRawNetwork.getLinkconfiguration().getModes().getMode().add(xmlElementMode);
    }
    
    //if no link segment types defined, create single link segment type with default parameters
    if (xmlRawNetwork.getLinkconfiguration().getLinksegmenttypes() == null) {
      xmlRawNetwork.getLinkconfiguration().setLinksegmenttypes(new XMLElementLinkSegmentTypes());
      XMLElementLinkSegmentType xmlLinkSegmentType = new XMLElementLinkSegmentType();
      xmlLinkSegmentType.setName("");
      xmlLinkSegmentType.setId(MacroscopicLinkSegmentType.DEFAULT_XML_ID);
      xmlLinkSegmentType.setCapacitylane(DEFAULT_MAXIMUM_CAPACITY_PER_LANE);
      xmlLinkSegmentType.setMaxdensitylane(LinkSegment.MAXIMUM_DENSITY);
      xmlRawNetwork.getLinkconfiguration().getLinksegmenttypes().getLinksegmenttype().add(xmlLinkSegmentType);
    }       
   }  
    
  /**
   * parse the raw network from file if not already set via constructor
   * @return 
   * @throws PlanItException thrown if error
   */
  private XMLElementMacroscopicNetwork collectPopulatedXmlRawNetwork() throws PlanItException {
    if(this.externalXmlRawNetwork==null) {
      final File[] xmlFileNames = FileUtils.getFilesWithExtensionFromDir(networkPathDirectory, xmlFileExtension);
      PlanItException.throwIf(xmlFileNames.length == 0,String.format("Directory %s contains no files with extension %s",networkPathDirectory, xmlFileExtension));
      return JAXBUtils.generateInstanceFromXml(XMLElementMacroscopicNetwork.class, xmlFileNames);
    }else { 
      return externalXmlRawNetwork;
    }
  }
  
  /** parse the usability component of the mode xml element. It is assumed they should be present, if not default values are created
   * @param generatedMode mode to extract information from
   * @return usabilityFeatures that are parsed
   * @throws PlanItException 
   */
  private UsabilityModeFeatures parseUsabilityModeFeatures(org.planit.xml.generated.XMLElementModes.Mode generatedMode) throws PlanItException {
    if(generatedMode.getUsabilityfeatures() == null) {
      return ModeFeaturesFactory.createDefaultUsabilityFeatures();
    }
    
    /* parse set values */
    UseOfModeType useOfModeType = EnumConversionUtil.xmlToPlanit(generatedMode.getUsabilityfeatures().getUsedtotype());    
    
    return ModeFeaturesFactory.createUsabilityFeatures(useOfModeType);
  }

  /** parse the physical features component of the mode xml element. It is assumed they should be present, if not default values are created
   * @param generatedMode mode to extract information from
   * @return physicalFeatures that are parsed
   * @throws PlanItException 
   */  
  private PhysicalModeFeatures parsePhysicalModeFeatures(org.planit.xml.generated.XMLElementModes.Mode generatedMode) throws PlanItException {
    if(generatedMode.getPhysicalfeatures() == null) {
      return ModeFeaturesFactory.createDefaultPhysicalFeatures();
    }
    
    /* parse set values */
    VehicularModeType vehicleType = EnumConversionUtil.xmlToPlanit(generatedMode.getPhysicalfeatures().getVehicletype());    
    MotorisationModeType motorisationType = EnumConversionUtil.xmlToPlanit(generatedMode.getPhysicalfeatures().getMotorisationtype());       
    TrackModeType trackType = EnumConversionUtil.xmlToPlanit(generatedMode.getPhysicalfeatures().getTracktype());         
    
    return ModeFeaturesFactory.createPhysicalFeatures(vehicleType, motorisationType, trackType);
  }    
  
  /**
   * Reads mode types from input file, register them on the network and also populate mapping based on XML ids
   * @param xmlRawNetwork 
   * @return map with modesByXmlId 
   * @throws PlanItException thrown if there is a Mode value of 0 in the modes definition file
   */
  private Map<String, Mode> parseModes(XMLElementMacroscopicNetwork xmlRawNetwork) throws PlanItException {

    /* populate if referenced later on by xml id */
    if(settings.getMapToIndexModeByXmlIds()==null) {
      settings.setMapToIndexModeByXmlIds(new HashMap<String, Mode>());
    }
    Map<String, Mode> modesByXmlId = settings.getMapToIndexModeByXmlIds();    
    
    final XMLElementLinkConfiguration linkconfiguration = xmlRawNetwork.getLinkconfiguration();    
    for (XMLElementModes.Mode xmlMode : linkconfiguration.getModes().getMode()) {      
      /* name, generate unique name if undefined */
      String name = xmlMode.getName();
      if(name==null) {
        name = PredefinedModeType.CUSTOM.value().concat(String.valueOf(this.network.modes.size()));
      }
           
      PredefinedModeType modeType = PredefinedModeType.create(name);      
      if(!xmlMode.isPredefined() && modeType != PredefinedModeType.CUSTOM) {
        LOGGER.warning(String.format("mode %s is not registered as predefined mode but name corresponds to PLANit predefined mode, reverting to PLANit predefined mode",xmlMode.getName()));
      }
      
      Mode mode = null;
      if(modeType != PredefinedModeType.CUSTOM) {
        /* predefined mode use factory, ignore other attributes (if any) */
        mode = this.network.modes.registerNew(modeType);
      }else {
        
        /* custom mode, parse all components to correctly configure the custom mode */
        double maxSpeed = xmlMode.getMaxspeed()==null ? Mode.GLOBAL_DEFAULT_MAXIMUM_SPEED_KMH : xmlMode.getMaxspeed();
        double pcu = xmlMode.getPcu()==null ? Mode.GLOBAL_DEFAULT_PCU : xmlMode.getPcu();
        
        PhysicalModeFeatures physicalFeatures = parsePhysicalModeFeatures(xmlMode);
        UsabilityModeFeatures usabilityFeatures = parseUsabilityModeFeatures(xmlMode);        
                
        mode = this.network.modes.registerNewCustomMode(name, maxSpeed, pcu, physicalFeatures, usabilityFeatures);        
      }     
      
      /* xml id */
      if(xmlMode.getId() != null && !xmlMode.getId().isBlank()) {
        mode.setXmlId(xmlMode.getId());
      }
      
      /* external id*/
      if(xmlMode.getExternalid() != null && !xmlMode.getExternalid().isBlank()) {
        mode.setExternalId(xmlMode.getExternalid());
      }      
      
      final Mode prevValue = modesByXmlId.put(mode.getXmlId(), mode);
      if (prevValue!=null && settings.isErrorIfDuplicateXmlId()) {
        String errorMessage = "duplicate mode xml id " + mode.getXmlId() + " found in network file.";
        throw new PlanItException(errorMessage);
      }
    }
    
    return modesByXmlId;
  }  
  
  /**
   * parse the CRS from the raw XML or utilise the default if not present
   * 
   * @param xmlLayers element from which ot parse crs
   */
  private CoordinateReferenceSystem parseCoordinateRerefenceSystem(XMLElementInfrastructureLayers xmlLayers) {
    CoordinateReferenceSystem crs = null;
    if(xmlLayers.getSrsname()==null || xmlLayers.getSrsname().isBlank()) {
      crs = PlanitJtsUtils.DEFAULT_GEOGRAPHIC_CRS;
      LOGGER.warning(String.format("coordinate reference system not set for PLANit network, applying default %s",crs.getName().getCode()));
    }else {
      crs = PlanitOpenGisUtils.createCoordinateReferenceSystem(xmlLayers.getSrsname());
    } 
    return crs;
  }    
  
  /**
   * parse the network layer
   * 
   * @param xmlLayer layer to extract from
   * @param modesByXmlId modes to reference
   * @param jtsUtils to use
   *
   */
  private InfrastructureLayer parseNetworkLayer(XMLElementInfrastructureLayer xmlLayer, Map<String, Mode> modesByXmlId, PlanitJtsUtils jtsUtils ) {
    
    /* create layer */
    MacroscopicPhysicalNetwork networkLayer = network.infrastructureLayers.createNew();
    
    /* xml id */
    if(xmlLayer.getId() != null && !xmlLayer.getId().isBlank()) {
      networkLayer.setXmlId(xmlLayer.getId());
    }
    
    /* external id*/
    if(xmlLayer.getExternalid() != null && !xmlLayer.getExternalid().isBlank()) {
      networkLayer.setExternalId(xmlLayer.getExternalid());
    }  
    
    CONTINUE HERE -> IMPLEMENT MISSING METHODS, REFACTOR PARSER FURTHER
    
    /* register supported modes on layer */
    if(xmlLayer.getModes() != null && !xmlLayer.getModes().isBlank()) {
      String xmlSupportedModes = xmlLayer.getModes();
      String[] modeRefs = xmlSupportedModes.split(CharacterUtils.COMMA.toString());
      for(String mode : Arrays.asList(modeRefs)) {
        if(modesByXmlId.containsKey(mode)) {
          networkLayer.registerSupportedMode(modesByXmlId.get(mode));
        }else {
          LOGGER.severe(String.format("mode %s is not present on the network, ignored on network layer", mode));
        }
      }      
    }else {
      /* absent, so register all modes (check if this is valid is to be executed by caller */
      networkLayer.registerSupportedModes((Mode[]) network.modes.setOf().toArray());
    }
    
    /* parse nodes */
    Map<String, Node> nodesByXmlId = XmlMacroscopicNetworkLayerHelper.createAndRegisterNodes(xmlLayer, network, settings);  
    
     MAKE OTHER METHODS ALSO STATIC, STARTING WITH PARSING LINK SEGMENT TYPES PROPERLY
    XmlMacroscopicNetworkLayerHelper layerHelper = new XmlMacroscopicNetworkLayerHelper(xmlRawNetwork, network, settings);
    
    /* parse nodes */
    Map<String, Node> nodesByXmlId = layerHelper.createAndRegisterNodes();      
         
    /* parse links, link segments, and link segment types  (implementation requires refactoring)*/
    layerHelper.createAndRegisterLinkAndLinkSegments(modesByXmlId, nodesByXmlId);
  }  
  
  /** parse the various network layers
   * 
   * @param xmlRawNetwork to extract network layers from
   * @param modesByXmlId the already parsed modes by xml id
   * @throws PlanItException thrown if error
   */
  private void parseNetworkLayers(XMLElementMacroscopicNetwork xmlRawNetwork, Map<String, Mode> modesByXmlId) throws PlanItException {
    XMLElementInfrastructureLayers xmlLayers = xmlRawNetwork.getInfrastructurelayers();
    
    /* crs */
    CoordinateReferenceSystem crs = parseCoordinateRerefenceSystem(xmlLayers);
    network.setCoordinateReferenceSystem(crs);  
    PlanitJtsUtils jtsUtils = new PlanitJtsUtils(network.getCoordinateReferenceSystem());
    
    /* layers */
    List<XMLElementInfrastructureLayer> xmlLayerList = xmlLayers.getLayer();
    Set<Mode> usedModes = new TreeSet<Mode>();
    for(XMLElementInfrastructureLayer xmlLayer : xmlLayerList) {
      /*layer */
      InfrastructureLayer layer = parseNetworkLayer(xmlLayer, modesByXmlId, jtsUtils);
      
      /* validate supported modes */
      int prevSize = usedModes.size();
      usedModes.addAll(layer.getSupportedModes());
      if(usedModes.size() != prevSize + layer.getSupportedModes().size()) {
        /* mode used in other layer already, this is not allowed */
        throw new PlanItException("modes are only allowed to be used in a single network layer, not multiple, please check your network inputs");
      }
    }
    
  }  
  


  /** place network to populate
   * 
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected void setNetwork(final InfrastructureNetwork network) throws PlanItException {
    /* currently we only support macroscopic infrastructure networks */
    if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItException("currently the PLANit network reader only supports macroscopic infrastructure networks, the provided network is not of this type");
    }
    
    this.network = (MacroscopicNetwork) network;
  }  
  
  /**
   * Default maximum capacity per lane
   */
  public static final double DEFAULT_MAXIMUM_CAPACITY_PER_LANE = 1800.0;      
  
  
  /** constructor
   * 
   * @param networkPath to use
   * @param xmlFileExtension to use
   * @param network to populate
   * @throws PlanItException  thrown if error
   */
  public PlanitNetworkReader(String networkPathDirectory, String xmlFileExtension, InfrastructureNetwork network) throws PlanItException{   
    this.externalXmlRawNetwork = null;
    this.networkPathDirectory = networkPathDirectory;
    this.xmlFileExtension = xmlFileExtension;
    
    setNetwork(network);
  }
  
  /** constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param externalXmlRawNetwork to extract from
   * @param network to populate
   * @throws PlanItException  thrown if error
   */
  public PlanitNetworkReader(XMLElementMacroscopicNetwork externalXmlRawNetwork, InfrastructureNetwork network) throws PlanItException{
    this.externalXmlRawNetwork = externalXmlRawNetwork;
    this.networkPathDirectory = null;        
    this.xmlFileExtension = null;
    
    setNetwork(network);
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public InfrastructureNetwork read() throws PlanItException {
        
    /* parse the XML raw network to extract PLANit network from */   
    XMLElementMacroscopicNetwork xmlRawNetwork = collectPopulatedXmlRawNetwork();
    
    /* defaults */
    injectMissingDefaultsToRawXmlNetwork(xmlRawNetwork);       
    
    try {
      
      /* parse modes*/
      Map<String, Mode> modesByXmlId = parseModes(xmlRawNetwork);

      /* parse layers */
      parseNetworkLayers(xmlRawNetwork, modesByXmlId);
      
    } catch (PlanItException e) {
      throw e;
    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error while populating physical network in PLANitIO",e);
    }    
    
    return network;
  }

  /** collect settings for this reader
   * @return settings
   */
  public PlanitNetworkReaderSettings getSettings() {
    return settings;
  }




}
