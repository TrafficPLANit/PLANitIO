package org.planit.io.network.converter;

import java.io.File;
import java.math.BigInteger;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.io.xml.network.XmlMacroscopicNetworkHelper;
import org.planit.io.xml.util.JAXBUtils;
import org.planit.network.converter.NetworkReader;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.misc.FileUtils;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.PredefinedModeType;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;
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
  
  /**
   * Default external Id value for link segment types when none are specified
   */
  private static final long DEFAULT_LINKSEGMENTTYPE_EXTERNAL_ID = 1;  
    
  /** object to extract PLANit network from once file is parsed */
  private XMLElementMacroscopicNetwork xmlRawNetwork;          
  
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
  private final MacroscopicNetwork network;
  
  /**
   * Update the XML macroscopic network element to include default values for any properties not included in the input file
   */
  private void injectMissingDefaultsToRawXmlNetwork() {
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
      xmlRawNetwork.getLinkconfiguration().getModes().getMode().add(xmlElementMode);
    }
    
    //if no link segment types defined, create single link segment type with default parameters
    if (xmlRawNetwork.getLinkconfiguration().getLinksegmenttypes() == null) {
      xmlRawNetwork.getLinkconfiguration().setLinksegmenttypes(new XMLElementLinkSegmentTypes());
      XMLElementLinkSegmentType xmlLinkSegmentType = new XMLElementLinkSegmentType();
      xmlLinkSegmentType.setName("");
      xmlLinkSegmentType.setId(BigInteger.valueOf(DEFAULT_LINKSEGMENTTYPE_EXTERNAL_ID));
      xmlLinkSegmentType.setCapacitylane(DEFAULT_MAXIMUM_CAPACITY_PER_LANE);
      xmlLinkSegmentType.setMaxdensitylane(LinkSegment.MAXIMUM_DENSITY);
      xmlRawNetwork.getLinkconfiguration().getLinksegmenttypes().getLinksegmenttype().add(xmlLinkSegmentType);
    }       
   }  
    
  /**
   * parse the raw network from file if not already set via constructor
   * @throws PlanItException thrown if error
   */
  private void parseXmlRawNetwork() throws PlanItException {
    if(xmlRawNetwork==null) {
      final File[] xmlFileNames = FileUtils.getFilesWithExtensionFromDir(networkPathDirectory, xmlFileExtension);
      PlanItException.throwIf(xmlFileNames.length == 0,String.format("Directory %s contains no files with extension %s",networkPathDirectory, xmlFileExtension));
      xmlRawNetwork = JAXBUtils.generateInstanceFromXml(XMLElementMacroscopicNetwork.class, xmlFileNames);
    }    
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
   */
  public PlanitNetworkReader(String networkPathDirectory, String xmlFileExtension, MacroscopicNetwork network){
    this.networkPathDirectory = networkPathDirectory;
    this.xmlFileExtension = xmlFileExtension;
    this.network = network;
    this.xmlRawNetwork = null;
  }
  
  /** constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlRawNetwork to extract from
   * @param network to populate
   */
  public PlanitNetworkReader(XMLElementMacroscopicNetwork xmlRawNetwork, MacroscopicNetwork network){
    this.xmlRawNetwork = xmlRawNetwork;
    this.network = network;
    this.networkPathDirectory = null;
    this.xmlFileExtension = null;
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicNetwork read() throws PlanItException {
    /* parse the XML raw network to extract PLANit network from */
    parseXmlRawNetwork();
    /* defaults */
    injectMissingDefaultsToRawXmlNetwork();       
    
    try {
      XmlMacroscopicNetworkHelper physicalNetworkHelper = new XmlMacroscopicNetworkHelper(xmlRawNetwork, network, settings);
      
      /* parse modes*/
      Map<Long, Mode> modesByExternalId = physicalNetworkHelper.createAndRegisterModes();
      
      /* parse nodes */
      Map<Long, Node> nodesByExternalId = physicalNetworkHelper.createAndRegisterNodes();      
           
      /* parse links, link segments, and link segment types  (implementation requires refactoring)*/
      physicalNetworkHelper.createAndRegisterLinkAndLinkSegments(modesByExternalId, nodesByExternalId);
      
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
