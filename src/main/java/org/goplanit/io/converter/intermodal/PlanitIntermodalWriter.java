package org.goplanit.io.converter.intermodal;

import org.goplanit.converter.IdMapperType;
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
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.misc.Pair;
import org.goplanit.xml.generated.XMLElementMacroscopicNetwork;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
import org.goplanit.xml.generated.XMLElementRoutedServices;
import org.goplanit.xml.generated.XMLElementServiceNetwork;
import org.goplanit.zoning.Zoning;

import java.security.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
   * Combine id mappigns from network and zoning writer to pass on to whomever requires references from either of those
   *
   * @param networkAndZoningWriter to use
   * @return created id mapping that can be used to set as paranet id mapping on writers that require it
   */
  private Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> createServiceNetworkParentIdMappings(
      Pair<PlanitNetworkWriter, PlanitZoningWriter> networkAndZoningWriter) {
    var networkIdMapping = networkAndZoningWriter.first().getIdMapperByType();
    var zoningIdMapping = networkAndZoningWriter.second().getIdMapperByType();
    return
        Stream.concat(networkIdMapping.entrySet().stream(), zoningIdMapping.entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * PErsist network and zoning and return writers
   *
   * @param macroscopicNetwork to persist
   * @param zoning to persist
   * @return used writers, network and zoning, respectively
   * @throws PlanItException
   */
  protected Pair<PlanitNetworkWriter, PlanitZoningWriter> writeNetworkAndZoning(MacroscopicNetwork macroscopicNetwork, Zoning zoning) throws PlanItException {
    /* network writer */
    PlanitNetworkWriterSettings networkSettings = getSettings().getNetworkSettings();
    PlanitNetworkWriter networkWriter = PlanitNetworkWriterFactory.create(networkSettings.getOutputPathDirectory(), networkSettings.getCountry(), xmlRawNetwork);
    networkWriter.setIdMapperType(getIdMapperType());
    networkWriter.write(macroscopicNetwork);

    /* zoning writer - with pt component via transfer zones */
    PlanitZoningWriterSettings zoningSettings = getSettings().getZoningSettings();
    PlanitZoningWriter zoningWriter =
            PlanitZoningWriterFactory.create(zoningSettings.getOutputPathDirectory(), zoningSettings.getCountry(), macroscopicNetwork.getCoordinateReferenceSystem());
    zoningWriter.setParentIdMapperTypes(networkWriter.getIdMapperByType()); // pass on parent ref mapping
    zoningWriter.setIdMapperType(getIdMapperType());
    zoningWriter.write(zoning);

    return Pair.of(networkWriter, zoningWriter);
  }

    
  /** Constructor with default country, use default destination Crs as a result.
   *  
   * @param outputDirectory to persist on
   * @param xmlRawNetwork to populate with PLANit network when persisting
   * @param xmlRawZoning to populate with PLANit zoning when persisting
   * @param xmlRawServiceNetwork to populate with PLANit service network when persisting
   * @param xmlRawRoutedServices to populate with PLANit routed services when persisting
   */
  protected PlanitIntermodalWriter(
          String outputDirectory,
          XMLElementMacroscopicNetwork xmlRawNetwork,
          XMLElementMacroscopicZoning xmlRawZoning,
          XMLElementServiceNetwork xmlRawServiceNetwork,
          XMLElementRoutedServices xmlRawRoutedServices) {
    this(outputDirectory,null,xmlRawNetwork, xmlRawZoning, xmlRawServiceNetwork, xmlRawRoutedServices);
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
                    serviceNetworkSettings.getOutputPathDirectory(), serviceNetworkSettings.getCountry(), xmlRawServiceNetwork);

    // service network writer requires physical network id ref mapping and possibly zoning one as well
    serviceNetworkWriter.setParentIdMapperTypes(
        createServiceNetworkParentIdMappings(networkAndZoningWriter)); // pass on parent ref mapping

    serviceNetworkWriter.setIdMapperType(getIdMapperType());
    serviceNetworkWriter.write(serviceNetwork);

    /* routed services writer */
    PlanitRoutedServicesWriterSettings routedServicesSettings = getSettings().getRoutedServicesSettings();
    PlanitRoutedServicesWriter routedServicesWriter =
        PlanitRoutedServicesWriterFactory.create(
            routedServicesSettings.getOutputPathDirectory(), routedServicesSettings.getCountry(), xmlRawRoutedServices);

    // routed services only requires service network entity references, those are present on the service network writer id mappings
    routedServicesWriter.setParentIdMapperTypes(serviceNetworkWriter.getIdMapperByType()); // pass on parent ref mapping

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
    getSettings().reset();
  }

  /**
   * {@inheritDoc}
   */    
  @Override
  public PlanitIntermodalWriterSettings getSettings() {
    return this.settings;
  }

}
