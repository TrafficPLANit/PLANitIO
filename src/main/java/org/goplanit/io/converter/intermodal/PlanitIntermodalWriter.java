package org.goplanit.io.converter.intermodal;

import org.goplanit.converter.idmapping.IdMapperType;
import org.goplanit.converter.intermodal.IntermodalWriter;
import org.goplanit.io.converter.network.PlanitNetworkWriter;
import org.goplanit.io.converter.network.PlanitNetworkWriterFactory;
import org.goplanit.io.converter.network.PlanitNetworkWriterSettings;
import org.goplanit.io.converter.service.*;
import org.goplanit.io.converter.zoning.PlanitZoningWriter;
import org.goplanit.io.converter.zoning.PlanitZoningWriterFactory;
import org.goplanit.io.converter.zoning.PlanitZoningWriterSettings;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.misc.Pair;
import org.goplanit.xml.generated.XMLElementMacroscopicNetwork;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
import org.goplanit.xml.generated.XMLElementRoutedServices;
import org.goplanit.xml.generated.XMLElementServiceNetwork;
import org.goplanit.zoning.Zoning;

/**
 * Planit intermodal writer for native Planit format, wrapping a planit network writer and planit zoning writer in one
 * 
 * @author markr
 *
 */
public class PlanitIntermodalWriter implements IntermodalWriter<ServiceNetwork, RoutedServices> {
  
  /** intermodal writer settings to use */
  protected final PlanitIntermodalWriterSettings settings;
  
  /** xml element to populate network on */
  protected final  XMLElementMacroscopicNetwork xmlRawNetwork;
  
  /** xml element to populate zoning on */
  protected final XMLElementMacroscopicZoning xmlRawZoning;

  /** xml element to populate service network on */
  protected final XMLElementServiceNetwork xmlRawServiceNetwork;

  /** xml element to populate routed services on */
  protected final XMLElementRoutedServices xmlRawRoutedServices;
  
  /**
   * the id mapper to use
   */
  protected IdMapperType idMapper;

  /**
   * Persist network and zoning and return writers
   *
   * @param macroscopicNetwork to persist
   * @param zoning to persist
   * @return used writers, network and zoning, respectively
   */
  protected Pair<PlanitNetworkWriter, PlanitZoningWriter> writeNetworkAndZoning(MacroscopicNetwork macroscopicNetwork, Zoning zoning) {
    /* network writer */
    PlanitNetworkWriterSettings networkSettings = getSettings().getNetworkSettings();
    PlanitNetworkWriter networkWriter = PlanitNetworkWriterFactory.create(networkSettings.getOutputDirectory(), networkSettings.getCountry(), xmlRawNetwork);
    networkWriter.setIdMapperType(getIdMapperType());
    networkWriter.write(macroscopicNetwork);

    /* zoning writer - with pt component via transfer zones */
    PlanitZoningWriterSettings zoningSettings = getSettings().getZoningSettings();
    PlanitZoningWriter zoningWriter =
            PlanitZoningWriterFactory.create(zoningSettings.getOutputDirectory(), zoningSettings.getCountry(), macroscopicNetwork.getCoordinateReferenceSystem());
    zoningWriter.setParentIdMappers(networkWriter.getPrimaryIdMapper()); // pass on parent ref mapping
    zoningWriter.setIdMapperType(getIdMapperType());
    zoningWriter.write(zoning);

    return Pair.of(networkWriter, zoningWriter);
  }

  /** Constructor 
   * @param outputDirectory to persist on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   * @param xmlRawNetwork to populate with PLANit network when persisting
   * @param xmlRawZoning to populate with PLANit zoning when persisting
   * @param xmlRawServiceNetwork to populate with PLANit service network when persisting
   * @param xmlRawRoutedServices to populate with PLANit routed services when persisting
   */
  protected PlanitIntermodalWriter(
          String outputDirectory,
          String countryName,
          XMLElementMacroscopicNetwork xmlRawNetwork,
          XMLElementMacroscopicZoning xmlRawZoning,
          XMLElementServiceNetwork xmlRawServiceNetwork,
          XMLElementRoutedServices xmlRawRoutedServices) {
    this.idMapper = IdMapperType.XML;
    this.settings = new PlanitIntermodalWriterSettings(outputDirectory, countryName);
    this.xmlRawNetwork = xmlRawNetwork;
    this.xmlRawZoning = xmlRawZoning;
    this.xmlRawServiceNetwork = xmlRawServiceNetwork;
    this.xmlRawRoutedServices = xmlRawRoutedServices;
  }

  /** Constructor
   * @param settings to use
   */
  protected PlanitIntermodalWriter(PlanitIntermodalWriterSettings settings) {
    this.idMapper = IdMapperType.XML;
    this.settings = settings;
    this.xmlRawNetwork =  new XMLElementMacroscopicNetwork();
    this.xmlRawZoning = new XMLElementMacroscopicZoning();
    this.xmlRawServiceNetwork = new XMLElementServiceNetwork();
    this.xmlRawRoutedServices = new XMLElementRoutedServices();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(MacroscopicNetwork macroscopicNetwork, Zoning zoning) throws PlanItException {
    writeNetworkAndZoning(macroscopicNetwork, zoning);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeWithServices(MacroscopicNetwork macroscopicNetwork, Zoning zoning, ServiceNetwork serviceNetwork, RoutedServices routedServices) throws PlanItException {
    /* perform persistence without services first */
    var networkAndZoningWriter = writeNetworkAndZoning(macroscopicNetwork, zoning);

    /* perform persistence for services */

    /* service network writer */
    PlanitServiceNetworkWriterSettings serviceNetworkSettings = getSettings().getServiceNetworkSettings();
    PlanitServiceNetworkWriter serviceNetworkWriter =
            PlanitServiceNetworkWriterFactory.create(
                    serviceNetworkSettings.getOutputDirectory(), serviceNetworkSettings.getCountry(), xmlRawServiceNetwork);

    // service network writer requires physical network id ref mapping and possibly zoning one as well
    var networkIdMapper = networkAndZoningWriter.first().getPrimaryIdMapper();
    var zoningIdMapper = networkAndZoningWriter.second().getPrimaryIdMapper();
    serviceNetworkWriter.setParentIdMappers(networkIdMapper, zoningIdMapper);

    serviceNetworkWriter.setIdMapperType(getIdMapperType());
    serviceNetworkWriter.write(serviceNetwork);

    /* routed services writer */
    PlanitRoutedServicesWriterSettings routedServicesSettings = getSettings().getRoutedServicesSettings();
    PlanitRoutedServicesWriter routedServicesWriter =
        PlanitRoutedServicesWriterFactory.create(
            routedServicesSettings.getOutputDirectory(), routedServicesSettings.getCountry(), xmlRawRoutedServices);

    // routed services only requires service network entity references, those are present on the service network writer id mappings
    routedServicesWriter.setParentIdMappers(networkIdMapper, zoningIdMapper, serviceNetworkWriter.getPrimaryIdMapper());

    routedServicesWriter.setIdMapperType(getIdMapperType());
    routedServicesWriter.write(routedServices);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public IdMapperType getIdMapperType() {
    return idMapper;
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void setIdMapperType(IdMapperType idMapper) {
    this.idMapper = idMapper;
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
  public PlanitIntermodalWriterSettings getSettings() {
    return this.settings;
  }

}
