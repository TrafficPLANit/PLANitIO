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
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.misc.Quadruple;
import org.goplanit.xml.generated.v2.XMLElementMacroscopicNetwork;
import org.goplanit.xml.generated.v2.XMLElementMacroscopicZoning;
import org.goplanit.xml.generated.v2.XMLElementRoutedServices;
import org.goplanit.xml.generated.v2.XMLElementServiceNetwork;
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

  /** id token for readers that create their own PLANit entities on each read */
  protected final IdGroupingToken internallyCreatedIdToken;

  /** track reuse when the caller provided the PLANit entities to populate */
  protected boolean externallyProvidedToPopulateConsumed;

  /** the network to populate */
  protected final MacroscopicNetwork networkToPopulate;

  /** the zoning to populate */
  protected final Zoning zoningToPopulate;

  /** the service network to (optionally) populate */
  protected final ServiceNetwork serviceNetworkToPopulate;

  /** the routed services to (optionally) populate */
  protected final RoutedServices routedServicesToPopulate;

  /** XML network element */
  protected final XMLElementMacroscopicNetwork xmlRawNetwork;

  /** XML zoning element */
  protected final XMLElementMacroscopicZoning xmlRawZoning;

  /** XML service network element */
  protected final XMLElementServiceNetwork xmlRawServiceNetwork;

  /** XML routed services element */
  protected final XMLElementRoutedServices xmlRawRoutedServices;


  /** Verify if this reader owns the entities it populates. */
  private boolean isToPopulateInternallyCreated() {
    return internallyCreatedIdToken != null;
  }

  /**
   * Collect a network and zoning for a read without services.
   *
   * @return network and zoning to populate
   */
  private Pair<MacroscopicNetwork, Zoning> collectNetworkAndZoningToPopulate() {
    if(isToPopulateInternallyCreated()) {
      MacroscopicNetwork network = new MacroscopicNetwork(internallyCreatedIdToken);
      return Pair.of(
          network,
          new Zoning(internallyCreatedIdToken, network.getNetworkGroupingTokenId()));
    }

    PlanItRunTimeException.throwIf(
        externallyProvidedToPopulateConsumed,
        "PLANit intermodal reader cannot be reused with caller-provided toPopulate entities");
    externallyProvidedToPopulateConsumed = true;

    return Pair.of(networkToPopulate, zoningToPopulate);
  }

  /**
   * Collect network, zoning, service network, and routed services for a read with services.
   *
   * @return entities to populate
   */
  private Quadruple<MacroscopicNetwork, Zoning, ServiceNetwork, RoutedServices> collectAllToPopulate() {
    if(isToPopulateInternallyCreated()) {
      MacroscopicNetwork network = new MacroscopicNetwork(internallyCreatedIdToken);
      ServiceNetwork serviceNetwork = new ServiceNetwork(internallyCreatedIdToken, network);
      return Quadruple.of(
          network,
          new Zoning(internallyCreatedIdToken, network.getNetworkGroupingTokenId()),
          serviceNetwork,
          new RoutedServices(internallyCreatedIdToken, serviceNetwork));
    }

    PlanItRunTimeException.throwIf(
        externallyProvidedToPopulateConsumed,
        "PLANit intermodal reader cannot be reused with caller-provided toPopulate entities");
    externallyProvidedToPopulateConsumed = true;

    return Quadruple.of(
        networkToPopulate,
        zoningToPopulate,
        serviceNetworkToPopulate,
        routedServicesToPopulate);
  }

  private void validate(
      MacroscopicNetwork networkToPopulate,
      Zoning zoningToPopulate,
      ServiceNetwork serviceNetworkToPopulate,
      RoutedServices routedServicesToPopulate,
      boolean withRoutedServices){
    PlanItRunTimeException.throwIf(networkToPopulate==null, "physical network to populate is null");
    PlanItRunTimeException.throwIf(zoningToPopulate==null, "zoning to populate is null");
    if(withRoutedServices) {
      PlanItRunTimeException.throwIf(
          serviceNetworkToPopulate == null, "service network to populate is null");
      PlanItRunTimeException.throwIf(
          !networkToPopulate.equals(serviceNetworkToPopulate.getParentNetwork()),
          "network to populate differs from service network parent network");
      PlanItRunTimeException.throwIf(
          routedServicesToPopulate == null, "routed services to populate is null");
      PlanItRunTimeException.throwIf(
          !serviceNetworkToPopulate.equals(routedServicesToPopulate.getParentNetwork()),
          "service network to populate differs from routed services parent service network");
    }
  }
    
  /** constructor where xml content is still on disk and first needs to be parsed into memory before converted to
   * planit memory model. Network and zoning instance
   * are created internally and returned upon completion
   * 
   * @param inputPathDirectory to use for both network and zoning input file assuming default input file names for
   *                           both (network.xml, zoning.xml)   *
   * @param xmlFileExtension to use
   * @param idToken to use for the network and zoning that are to be created
   */
  protected PlanitIntermodalReader(String inputPathDirectory, String xmlFileExtension, IdGroupingToken idToken) {
    this(new PlanitIntermodalReaderSettings(inputPathDirectory, xmlFileExtension), idToken);    
  }   
  
  /** constructor where xml content is still on disk and first needs to be parsed into memory before converted to
   * planit memory model. Network and zoning instance
   * are created internally and returned upon completion
   * 
   * @param settings to use
   * @param idToken to use for the network and zoning that are to be created
   */
  protected PlanitIntermodalReader(PlanitIntermodalReaderSettings settings, IdGroupingToken idToken) {
    this.intermodalReaderSettings = settings;
    this.internallyCreatedIdToken = idToken;
    this.externallyProvidedToPopulateConsumed = false;

    this.networkToPopulate = null;
    this.xmlRawNetwork = null;

    this.zoningToPopulate = null;
    this.xmlRawZoning = null;

    this.serviceNetworkToPopulate = null;
    this.xmlRawServiceNetwork = null;
    this.routedServicesToPopulate =null;
    this.xmlRawRoutedServices = null;
  }  
  
  /** constructor where xml content is still on disk and first needs to be parsed into memory before converted to
   * planit memory model
   * 
   * @param inputPathDirectory to use for both network and zoning input file assuming default input file names for
   *                           both (network.xml, zoning.xml)   *
   * @param xmlFileExtension to use
   * @param network to populate
   * @param zoning to populate
   */
  protected PlanitIntermodalReader(
      String inputPathDirectory, String xmlFileExtension, MacroscopicNetwork network, Zoning zoning) {
    this(inputPathDirectory, xmlFileExtension, network, zoning, null, null);
  }

  /** constructor where xml content is still on disk and first needs to be parsed into memory before converted to
   * planit memory model
   *
   * @param inputPathDirectory to use for both network and zoning input file assuming default input file names for
   *                           both (network.xml, zoning.xml)   *
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

  /** constructor where the xml content has already been parsed into a JAXB memory model which subsequently needs to
   * be converted into the planit memory model
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
    this.internallyCreatedIdToken = null;
    this.externallyProvidedToPopulateConsumed = false;

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
    var toPopulate = collectNetworkAndZoningToPopulate();
    return read(toPopulate.first(), toPopulate.second());
  }

  /**
   * Read network and zoning into the provided entities.
   *
   * @param networkToPopulate network to populate
   * @param zoningToPopulate zoning to populate
   * @return populated network and zoning
   */
  private Pair<MacroscopicNetwork, Zoning> read(
      MacroscopicNetwork networkToPopulate,
      Zoning zoningToPopulate){
    validate(networkToPopulate, zoningToPopulate, null, null, false);

    /* network */
    PlanitNetworkReader networkReader;
    if(xmlRawNetwork == null) {
      networkReader = PlanitNetworkReaderFactory.create(getSettings().getNetworkSettings(), networkToPopulate);
    }else{
      networkReader = PlanitNetworkReaderFactory.create(
          xmlRawNetwork, getSettings().getNetworkSettings(), networkToPopulate);
    }
    MacroscopicNetwork referenceNetwork = networkReader.read();
        
    /* zoning */   
    PlanitZoningReader zoningReader;
    if(xmlRawZoning == null){
      zoningReader = PlanitZoningReaderFactory.create(
          getSettings().getZoningSettings(), referenceNetwork, zoningToPopulate);
    }else{
      zoningReader = PlanitZoningReaderFactory.create(
          xmlRawZoning, getSettings().getZoningSettings(), referenceNetwork, zoningToPopulate);
    }
    
    /* parse */
    Zoning zoning = zoningReader.read();
    
    /* result */
    return Pair.of(referenceNetwork, zoning);
  }

  /**
   * Parse network, zoning, service network, and routed services that belong together
   *
   * @return created network, zoning, service network and services
   */
  @Override
  public Quadruple<MacroscopicNetwork, Zoning, ServiceNetwork, RoutedServices> readWithServices(){
    var toPopulate = collectAllToPopulate();
    validate(toPopulate.first(), toPopulate.second(), toPopulate.third(), toPopulate.fourth(), true);

    // network + zoning
    var networkZoning = read(toPopulate.first(), toPopulate.second());
    // sync CRS post-reading content (including CRS)
    toPopulate.third().setCoordinateReferenceSystem(networkZoning.first().getCoordinateReferenceSystem());

    // service network
    PlanitServiceNetworkReader serviceNetworkReader;
    if(xmlRawServiceNetwork == null) {
      serviceNetworkReader = PlanitServiceNetworkReaderFactory.create(
          getSettings().getServiceNetworkSettings(), toPopulate.third());
    }else{
      serviceNetworkReader = PlanitServiceNetworkReaderFactory.create(
          xmlRawServiceNetwork, getSettings().getServiceNetworkSettings(), toPopulate.third());
    }
    var serviceNetwork = serviceNetworkReader.read();

    // routed services
    PlanitRoutedServicesReader routedServicesReader;
    if(xmlRawRoutedServices == null) {
      routedServicesReader = PlanitRoutedServicesReaderFactory.create(
          getSettings().getRoutedServicesSettings(), toPopulate.fourth());
    }else{
      routedServicesReader = PlanitRoutedServicesReaderFactory.create(
          xmlRawRoutedServices, getSettings().getRoutedServicesSettings(), toPopulate.fourth());
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
