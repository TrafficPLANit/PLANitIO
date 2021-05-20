package org.planit.io.converter.network;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.converter.network.NetworkReader;
import org.planit.io.xml.network.XmlMacroscopicNetworkLayerHelper;
import org.planit.io.xml.util.EnumConversionUtil;
import org.planit.io.xml.util.PlanitXmlReader;
import org.planit.mode.ModeFeaturesFactory;
import org.planit.network.InfrastructureLayer;
import org.planit.network.InfrastructureNetwork;
import org.planit.network.macroscopic.MacroscopicNetwork;
import org.planit.network.macroscopic.physical.MacroscopicPhysicalNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsCrsUtils;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.CharacterUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.MotorisationModeType;
import org.planit.utils.mode.PhysicalModeFeatures;
import org.planit.utils.mode.PredefinedModeType;
import org.planit.utils.mode.TrackModeType;
import org.planit.utils.mode.UsabilityModeFeatures;
import org.planit.utils.mode.UseOfModeType;
import org.planit.utils.mode.VehicularModeType;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.xml.generated.XMLElementConfiguration;
import org.planit.xml.generated.XMLElementInfrastructureLayer;
import org.planit.xml.generated.XMLElementInfrastructureLayers;
import org.planit.xml.generated.XMLElementLayerConfiguration;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;
import org.planit.xml.generated.XMLElementModes;

/**
 * Implementation of the network reader for the PLANit XML native format
 * 
 * @author gman, markr
 *
 */
public class PlanitNetworkReader extends PlanitXmlReader<XMLElementMacroscopicNetwork> implements NetworkReader {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitNetworkReader.class.getCanonicalName());            
  
  /** the settings for this reader */
  private final PlanitNetworkReaderSettings settings;
    
  /**
   * Default XSD files used to validate input XML files against
   * TODO: replace with online schema
   */
  public static final String NETWORK_XSD_FILE = "src\\main\\resources\\xsd\\macroscopicnetworkinput.xsd";
      
  /** the network memory model to populate */
  private MacroscopicNetwork network;
    
  /**
   * Update the XML macroscopic network element to include default values for any properties not included in the input file
   */
  private void injectMissingDefaultsToRawXmlNetwork() {
    if (getXmlRootElement().getConfiguration() == null) {
      getXmlRootElement().setConfiguration(new XMLElementConfiguration());
    }
    
    //if no modes defined, create single mode with default values
    if (getXmlRootElement().getConfiguration().getModes() == null) {
      getXmlRootElement().getConfiguration().setModes(new XMLElementModes());
      XMLElementModes.Mode xmlElementMode = new XMLElementModes.Mode();
      // default in absence of any modes is the predefined CAR mode
      xmlElementMode.setPredefined(true);
      xmlElementMode.setName(PredefinedModeType.CAR.value());
      xmlElementMode.setId(Mode.DEFAULT_XML_ID);
      getXmlRootElement().getConfiguration().getModes().getMode().add(xmlElementMode);
    }
           
   }  
      
  /** parse the usability component of the mode xml element. It is assumed they should be present, if not default values are created
   * @param generatedMode mode to extract information from
   * @return usabilityFeatures that are parsed
   * @throws PlanItException thrown if error
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
   * @throws PlanItException thrown if error
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
   * @return map with modesByXmlId 
   * @throws PlanItException thrown if there is a Mode value of 0 in the modes definition file
   */
  private Map<String, Mode> parseModes() throws PlanItException {

    /* populate if referenced later on by xml id */
    if(settings.getMapToIndexModeByXmlIds()==null) {
      settings.setMapToIndexModeByXmlIds(new HashMap<String, Mode>());
    }
    Map<String, Mode> modesByXmlId = settings.getMapToIndexModeByXmlIds();    
    
    final XMLElementConfiguration xmlGeneralConfiguration = getXmlRootElement().getConfiguration();    
    for (XMLElementModes.Mode xmlMode : xmlGeneralConfiguration.getModes().getMode()) {
      
      /* xml id */
      String modeXmlId = null;
      if(xmlMode.getId() != null && !xmlMode.getId().isBlank()) {
        modeXmlId = xmlMode.getId();
      }      
      
      /* name, generate unique name if undefined */
      String name = xmlMode.getName();
      String potentialPredefinedModeType = name;
      if(potentialPredefinedModeType==null) {
        potentialPredefinedModeType = modeXmlId;
      }
      PredefinedModeType modeType = PredefinedModeType.create(potentialPredefinedModeType);      
      if(!xmlMode.isPredefined() && modeType != PredefinedModeType.CUSTOM) {
        LOGGER.warning(String.format("mode is not registered as predefined mode but name or xmlid corresponds to PLANit predefined mode, reverting to PLANit predefined mode %s",modeType.name()));
      }            
      if(name==null && modeType == PredefinedModeType.CUSTOM) {
        name = PredefinedModeType.CUSTOM.value().concat(String.valueOf(this.network.modes.size()));
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
            
      /* external id*/
      if(xmlMode.getExternalid() != null && !xmlMode.getExternalid().isBlank()) {
        mode.setExternalId(xmlMode.getExternalid());
      }      
      
      /* xml id */
      mode.setXmlId(modeXmlId);
      final Mode prevValue = modesByXmlId.put(mode.getXmlId(), mode);
      if (prevValue!=null) {
        String errorMessage = "duplicate mode xml id " + mode.getXmlId() + " found in network file";
        throw new PlanItException(errorMessage);
      }
    }
    
    return modesByXmlId;
  }  
  
  /**
   * parse the CRS from the raw XML or utilise the default if not present
   * 
   * @param xmlLayers element from which to parse crs
   * @throws PlanItException thrown if error
   */
  private CoordinateReferenceSystem parseCoordinateRerefenceSystem(XMLElementInfrastructureLayers xmlLayers) throws PlanItException {
    CoordinateReferenceSystem crs = null;
    crs = createPlanitCrs(xmlLayers.getSrsname());
    return crs;
  }    
  
  /**
   * parse the network layer
   * 
   * @param xmlLayer layer to extract from
   * @param modesByXmlId modes to reference
   * @param jtsUtils to use
   * @return parsed network layer
   * @throws PlanItException thrown if error
   *
   */
  private InfrastructureLayer parseNetworkLayer(XMLElementInfrastructureLayer xmlLayer, Map<String, Mode> modesByXmlId, PlanitJtsCrsUtils jtsUtils ) throws PlanItException {
    
    /* create layer */
    MacroscopicPhysicalNetwork networkLayer = network.infrastructureLayers.createNew();
    
    /* xml id */
    if(xmlLayer.getId() != null && !xmlLayer.getId().isBlank()) {
      networkLayer.setXmlId(xmlLayer.getId());
    }else {
      LOGGER.warning("infrastructure layer id missing in xml, use generated id instead");
      networkLayer.setXmlId(Long.toString(networkLayer.getId()));
    }
    
    /* external id*/
    if(xmlLayer.getExternalid() != null && !xmlLayer.getExternalid().isBlank()) {
      networkLayer.setExternalId(xmlLayer.getExternalid());
    }  
          
    /* supported modes*/
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
      networkLayer.registerSupportedModes(network.modes.setOf());
    }
    
    /* link segment types */
    XMLElementLayerConfiguration xmlLayerconfiguration = xmlLayer.getLayerconfiguration();
    if(xmlLayerconfiguration == null) {
      xmlLayer.setLayerconfiguration(new XMLElementLayerConfiguration());
      xmlLayerconfiguration = xmlLayer.getLayerconfiguration();
    }
    Map<String, MacroscopicLinkSegmentType> linkSegmentTypesByXmlId = XmlMacroscopicNetworkLayerHelper.parseLinkSegmentTypes(xmlLayerconfiguration, networkLayer, settings, modesByXmlId);
    
    /* parse nodes */
    Map<String, Node> nodesByXmlId = XmlMacroscopicNetworkLayerHelper.parseNodes(xmlLayer, networkLayer, settings);                  
         
    /* parse links, link segments */
    XmlMacroscopicNetworkLayerHelper.parseLinkAndLinkSegments(xmlLayer, networkLayer, settings, nodesByXmlId, linkSegmentTypesByXmlId, jtsUtils);
    
    return networkLayer;
  }  
  
  /** parse the various network layers
   * 
   * @param modesByXmlId the already parsed modes by XML id
   * @throws PlanItException thrown if error
   */
  private void parseNetworkLayers(Map<String, Mode> modesByXmlId) throws PlanItException {
    XMLElementInfrastructureLayers xmlLayers = getXmlRootElement().getInfrastructurelayers();
    PlanItException.throwIfNull(xmlLayers, "infrastructurelayers element not present in network file");
    
    /* crs */
    CoordinateReferenceSystem crs = parseCoordinateRerefenceSystem(xmlLayers);
    network.setCoordinateReferenceSystem(crs);  
    PlanitJtsCrsUtils jtsUtils = new PlanitJtsCrsUtils(network.getCoordinateReferenceSystem());
    
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

  /** Place network to populate
   * 
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected void setNetwork(final InfrastructureNetwork<?,?> network) throws PlanItException {
    /* currently we only support macroscopic infrastructure networks */
    if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItException("currently the PLANit network reader only supports macroscopic infrastructure networks, the provided network is not of this type");
    }
    
    this.network = (MacroscopicNetwork) network;
  }
  
  /** Constructor where settings are directly provided such that input information can be exracted from it
   * 
   * @param settings to use
   * @param idToken to use for the network to populate
   * @throws PlanItException  thrown if error
   */
  protected PlanitNetworkReader(PlanitNetworkReaderSettings settings, IdGroupingToken idToken) throws PlanItException{
    super(XMLElementMacroscopicNetwork.class);
    this.settings = settings;
    setNetwork(new MacroscopicNetwork(idToken));
  }  
  
  /** Constructor where settings are directly provided such that input information can be exracted from it
   * 
   * @param settings to use
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitNetworkReader(PlanitNetworkReaderSettings settings, InfrastructureNetwork<?,?> network) throws PlanItException{
    super(XMLElementMacroscopicNetwork.class);
    this.settings = settings;
    setNetwork(network);
  }  
    
  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param externalXmlRawNetwork to extract from
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitNetworkReader(XMLElementMacroscopicNetwork externalXmlRawNetwork, InfrastructureNetwork<?,?> network) throws PlanItException{
    super(externalXmlRawNetwork);
    this.settings = new PlanitNetworkReaderSettings();
    setNetwork(network);
  }
  
  /** Constructor
   * 
   * @param networkPathDirectory to use
   * @param xmlFileExtension to use
   * @param network to populate
   * @throws PlanItException thrown if error
   */
  protected PlanitNetworkReader(String networkPathDirectory, String xmlFileExtension, InfrastructureNetwork<?,?> network) throws PlanItException{   
    super(XMLElementMacroscopicNetwork.class);
    this.settings = new PlanitNetworkReaderSettings(networkPathDirectory, xmlFileExtension);
    setNetwork(network);
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public InfrastructureNetwork<?,?> read() throws PlanItException {
        
    /* parse the XML raw network to extract PLANit network from */   
    initialiseAndParseXmlRootElement(getSettings().getInputDirectory(), getSettings().getXmlFileExtension());
    
    /* defaults */
    injectMissingDefaultsToRawXmlNetwork();       
    
    try {
      
      /* parse modes*/
      Map<String, Mode> modesByXmlId = parseModes();

      /* parse layers */
      parseNetworkLayers(modesByXmlId);
      
      /* free xml content */
      clearXmlContent();
      
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
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // TODO Auto-generated method stub    
  }

}
