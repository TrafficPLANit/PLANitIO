package org.goplanit.io.converter.intermodal;

import org.goplanit.converter.intermodal.IntermodalReader;
import org.goplanit.io.converter.network.PlanitNetworkReader;
import org.goplanit.io.converter.network.PlanitNetworkReaderFactory;
import org.goplanit.io.converter.service.PlanitRoutedServicesReader;
import org.goplanit.io.converter.service.PlanitRoutedServicesReaderFactory;
import org.goplanit.io.converter.service.PlanitServiceNetworkReader;
import org.goplanit.io.converter.service.PlanitServiceNetworkReaderFactory;
import org.goplanit.io.converter.zoning.PlanitZoningReader;
import org.goplanit.io.converter.zoning.PlanitZoningReaderFactory;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.Quadruple;
import org.goplanit.xml.generated.XMLElementMacroscopicNetwork;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
import org.goplanit.xml.generated.XMLElementRoutedServices;
import org.goplanit.xml.generated.XMLElementServiceNetwork;
import org.goplanit.zoning.Zoning;

/**
 * Planit intermodal reader in native format. Wraps a network and zoning reader in one
 * 
 * @author markr
 *
 */
public class PlanitIntermodalReader implements IntermodalReader<ServiceNetwork, RoutedServices> {
  
  /** intermodal reader settings to use */
  protected final PlanitIntermodalReaderSettings intermodalReaderSettings;

  /** the network to populate */
  protected final MacroscopicNetwork networkToPopulate;

  /** the zoning to populate */
  protected final Zoning zoningToPopulate;

  /** the service network to (optionally) populate */
  protected final ServiceNetwork serviceNetworkToPopulate;

  /** the routed services to (optionally) populate */
  protected final RoutedServices routedServicesToPopulate;

  protected final XMLElementMacroscopicNetwork xmlRawNetwork;

  protected final XMLElementMacroscopicZoning xmlRawZoning;

  protected final XMLElementServiceNetwork xmlRawServiceNetwork;
  protected final XMLElementRoutedServices xmlRawRoutedServices;


  private void validate(boolean withRoutedServices){
    PlanItRunTimeException.throwIf(networkToPopulate==null, "physical network to populate is null");
    PlanItRunTimeException.throwIf(zoningToPopulate==null, "zoning to populate is null");
    if(withRoutedServices) {
      PlanItRunTimeException.throwIf(serviceNetworkToPopulate == null, "service network to populate is null");
      PlanItRunTimeException.throwIf(!networkToPopulate.equals(serviceNetworkToPopulate.getParentNetwork()), "network to populate differs from service network parent network");
            PlanItRunTimeException.throwIf(routedServicesToPopulate == null, "routed services to populate is null");
      PlanItRunTimeException.throwIf(!serviceNetworkToPopulate.equals(routedServicesToPopulate.getParentNetwork()), "service network to populate differs from routed services parent service network");
    }
  }
    
  /** constructor where xml content is still on disk and first needs to be parsed into memory before converted to planit memory model. Network and zoning instance
   * are created internally and returned upon completion
   * 
   * @param inputPathDirectory to use for both network and zoning input file assuming default input file names for both (network.xml, zoning.xml)   * 
   * @param xmlFileExtension to use
   * @param idToken to use for the network and zoning that are to be created
   */
  protected PlanitIntermodalReader(String inputPathDirectory, String xmlFileExtension, IdGroupingToken idToken) {
    this(new PlanitIntermodalReaderSettings(inputPathDirectory, xmlFileExtension), idToken);    
  }   
  
  /** constructor where xml content is still on disk and first needs to be parsed into memory before converted to planit memory model. Network and zoning instance
   * are created internally and returned upon completion
   * 
   * @param settings to use
   * @param idToken to use for the network and zoning that are to be created
   */
  protected PlanitIntermodalReader(PlanitIntermodalReaderSettings settings, IdGroupingToken idToken) {
    this.intermodalReaderSettings = settings;

    this.networkToPopulate = new MacroscopicNetwork(idToken);
    this.xmlRawNetwork = null;

    this.zoningToPopulate = new Zoning(idToken, networkToPopulate.getNetworkGroupingTokenId());
    this.xmlRawZoning = null;

    this.serviceNetworkToPopulate = null;
    this.xmlRawServiceNetwork = null;
    this.routedServicesToPopulate =null;
    this.xmlRawRoutedServices = null;
  }  
  
  /** constructor where xml content is still on disk and first needs to be parsed into memory before converted to planit memory model
   * 
   * @param inputPathDirectory to use for both network and zoning input file assuming default input file names for both (network.xml, zoning.xml)   * 
   * @param xmlFileExtension to use
   * @param network to populate
   * @param zoning to populate
   */
  protected PlanitIntermodalReader(String inputPathDirectory, String xmlFileExtension, MacroscopicNetwork network, Zoning zoning) {
    this(inputPathDirectory, xmlFileExtension, network, zoning, null, null);
  }

  /** constructor where xml content is still on disk and first needs to be parsed into memory before converted to planit memory model
   *
   * @param inputPathDirectory to use for both network and zoning input file assuming default input file names for both (network.xml, zoning.xml)   *
   * @param xmlFileExtension to use
   * @param network to populate
   * @param zoning to populate
   * @param serviceNetwork to populate (if reading with services)
   * @param routedServices to populate (if reading with services)
   */
  protected PlanitIntermodalReader(String inputPathDirectory,
                                   String xmlFileExtension,
                                   MacroscopicNetwork network,
                                   Zoning zoning,
                                   ServiceNetwork serviceNetwork,
                                   RoutedServices routedServices) {
    this(new PlanitIntermodalReaderSettings(inputPathDirectory, xmlFileExtension), null, null, null, null, network, zoning, serviceNetwork, routedServices);
  }

  /** constructor where the xml content has already been parsed into a JAXB memory model which subsequently needs to be converted into the planit memory model
   * 
   * @param xmlRawNetwork to extract from
   * @param xmlRawZoning to extract from
   * @param network to extract referenced entities from
   * @param zoning to populate
   */
  public PlanitIntermodalReader(XMLElementMacroscopicNetwork xmlRawNetwork, XMLElementMacroscopicZoning xmlRawZoning, MacroscopicNetwork network, Zoning zoning) {
    this(new PlanitIntermodalReaderSettings(), xmlRawNetwork, xmlRawZoning, null, null, network, zoning, null, null);
  }

  /** Constructor
   *
   * @param settings to use
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @param serviceNetwork to populate
   * @param routedServices to populate
   */
  public PlanitIntermodalReader(
      final PlanitIntermodalReaderSettings settings,
      MacroscopicNetwork network,
      Zoning zoning,
      ServiceNetwork serviceNetwork,
      RoutedServices routedServices) {
    this(settings, null, null, null, null, network, zoning, serviceNetwork, routedServices);
  }

  /** constructor where the xml content has already been parsed into a JAXB memory model which subsequently needs to be converted into the planit memory model
   *
   * @param settings to use
   * @param xmlRawNetwork to extract from
   * @param xmlRawZoning to extract from
   * @param xmlRawServiceNetwork to extract from
   * @param xmlRawRoutedServices to extract from
   * @param network to extract references from (if any)
   * @param zoning to populate
   * @param serviceNetwork to populate
   * @param routedServices to populate
   */
  public PlanitIntermodalReader(
      final PlanitIntermodalReaderSettings settings,
      XMLElementMacroscopicNetwork xmlRawNetwork,
      XMLElementMacroscopicZoning xmlRawZoning,
      XMLElementServiceNetwork xmlRawServiceNetwork,
      XMLElementRoutedServices xmlRawRoutedServices,
      MacroscopicNetwork network,
      Zoning zoning,
      ServiceNetwork serviceNetwork,
      RoutedServices routedServices) {
    this.intermodalReaderSettings =  settings;

    this.networkToPopulate = network;
    this.xmlRawNetwork = xmlRawNetwork;

    this.zoningToPopulate = zoning;
    this.xmlRawZoning = xmlRawZoning;

    this.serviceNetworkToPopulate = serviceNetwork;
    this.xmlRawServiceNetwork = xmlRawServiceNetwork;

    this.routedServicesToPopulate = routedServices;
    this.xmlRawRoutedServices = xmlRawRoutedServices;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pair<MacroscopicNetwork, Zoning> read(){
    validate(false);

    /* network */
    PlanitNetworkReader networkReader;
    if(xmlRawNetwork == null) {
      networkReader = PlanitNetworkReaderFactory.create(getSettings().getNetworkSettings(), networkToPopulate);
    }else{
      networkReader = PlanitNetworkReaderFactory.create(xmlRawNetwork, getSettings().getNetworkSettings(), networkToPopulate);
    }
    MacroscopicNetwork referenceNetwork = networkReader.read();
        
    /* zoning */   
    PlanitZoningReader zoningReader;
    if(xmlRawZoning == null){
      zoningReader = PlanitZoningReaderFactory.create(getSettings().getZoningSettings(), referenceNetwork, zoningToPopulate);
    }else{
      zoningReader = PlanitZoningReaderFactory.create(xmlRawZoning, getSettings().getZoningSettings(), referenceNetwork, zoningToPopulate);
    }
    
    /* parse */
    Zoning zoning = zoningReader.read();
    
    /* result */
    return Pair.of(referenceNetwork, zoning);
  }

  /**
   * PArse network, zoning, service network, and routed services that belong together
   *
   * @return created network, zoning, service network and services
   */
  @Override
  public Quadruple<MacroscopicNetwork, Zoning, ServiceNetwork, RoutedServices> readWithServices(){
    validate(true);

    // network + zoning
    var networkZoning = read();

    // service network
    PlanitServiceNetworkReader serviceNetworkReader;
    if(xmlRawServiceNetwork == null) {
      serviceNetworkReader = PlanitServiceNetworkReaderFactory.create(getSettings().getServiceNetworkSettings(), serviceNetworkToPopulate);
    }else{
      serviceNetworkReader = PlanitServiceNetworkReaderFactory.create(xmlRawServiceNetwork, getSettings().getServiceNetworkSettings(), serviceNetworkToPopulate);
    }
    var serviceNetwork = serviceNetworkReader.read();

    // routed services
    PlanitRoutedServicesReader routedServicesReader;
    if(xmlRawRoutedServices == null) {
      routedServicesReader = PlanitRoutedServicesReaderFactory.create(getSettings().getRoutedServicesSettings(), routedServicesToPopulate);
    }else{
      routedServicesReader = PlanitRoutedServicesReaderFactory.create(xmlRawRoutedServices, getSettings().getRoutedServicesSettings(), routedServicesToPopulate);
    }
    var routedServices = routedServicesReader.read();

    return Quadruple.of(networkZoning.first(), networkZoning.second(), serviceNetwork, routedServices);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void reset() {
  }

  /**
   * {@inheritDoc}
   */    
  @Override
  public PlanitIntermodalReaderSettings getSettings() {
    return this.intermodalReaderSettings;
  }

  /**
   * Currently no support for this yet on the PLANit side. To be implemented in the future
   * @return false
   */
  @Override
  public boolean supportServiceConversion() {
    return false;
  }

}
