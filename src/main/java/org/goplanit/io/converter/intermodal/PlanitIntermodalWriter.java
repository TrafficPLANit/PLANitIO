package org.goplanit.io.converter.intermodal;

import org.goplanit.converter.IdMapperType;
import org.goplanit.converter.intermodal.IntermodalWriter;
import org.goplanit.io.converter.network.PlanitNetworkWriter;
import org.goplanit.io.converter.network.PlanitNetworkWriterFactory;
import org.goplanit.io.converter.network.PlanitNetworkWriterSettings;
import org.goplanit.io.converter.service.PlanitServiceNetworkWriter;
import org.goplanit.io.converter.service.PlanitServiceNetworkWriterFactory;
import org.goplanit.io.converter.service.PlanitServiceNetworkWriterSettings;
import org.goplanit.io.converter.zoning.PlanitZoningWriter;
import org.goplanit.io.converter.zoning.PlanitZoningWriterFactory;
import org.goplanit.io.converter.zoning.PlanitZoningWriterSettings;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.misc.Pair;
import org.goplanit.xml.generated.XMLElementMacroscopicNetwork;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
import org.goplanit.xml.generated.XMLElementServiceNetwork;
import org.goplanit.zoning.Zoning;

import java.security.Provider;

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
  
  /**
   * the id mapper to use
   */
  protected IdMapperType idMapper;

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
    zoningWriter.setIdMapperType(getIdMapperType()); //todo should also pass on parent ref mapping as we do for service network here!
    zoningWriter.write(zoning);

    return Pair.of(networkWriter, zoningWriter);
  }

    
  /** Constructor with default country, use default destination Crs as a result.
   *  
   * @param outputDirectory to persist on
   * @param xmlRawNetwork to populate with PLANit network when persisting
   * @param xmlRawZoning to populate with PLANit zoning when persisting
   * @param xmlRawServiceNetwork to populate with PLANit service network when persisting
   */
  protected PlanitIntermodalWriter(
          String outputDirectory,
          XMLElementMacroscopicNetwork xmlRawNetwork,
          XMLElementMacroscopicZoning xmlRawZoning,
          XMLElementServiceNetwork xmlRawServiceNetwork) {
    this(outputDirectory,null,xmlRawNetwork, xmlRawZoning, xmlRawServiceNetwork);
  }  
    
  /** Constructor 
   * @param outputDirectory to persist on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   * @param xmlRawNetwork to populate with PLANit network when persisting
   * @param xmlRawZoning to populate with PLANit zoning when persisting
   * @param xmlRawServiceNetwork to populate with PLANit service network when persisting
   */
  protected PlanitIntermodalWriter(
          String outputDirectory,
          String countryName,
          XMLElementMacroscopicNetwork xmlRawNetwork,
          XMLElementMacroscopicZoning xmlRawZoning,
          XMLElementServiceNetwork xmlRawServiceNetwork) {
    this.idMapper = IdMapperType.XML;
    this.settings = new PlanitIntermodalWriterSettings(outputDirectory, countryName);
    this.xmlRawNetwork = xmlRawNetwork;
    this.xmlRawZoning = xmlRawZoning;
    this.xmlRawServiceNetwork = xmlRawServiceNetwork;
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
    serviceNetworkWriter.setParentIdMapperTypes(networkAndZoningWriter.first().getIdMapperByType()); // pass on parent ref mapping
    serviceNetworkWriter.setIdMapperType(getIdMapperType());
    serviceNetworkWriter.write(serviceNetwork);

    /* routed services writer */
    throw new PlanItRunTimeException("PLANit IO routed services writer not yet implemented");
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
