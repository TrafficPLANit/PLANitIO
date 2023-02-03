package org.goplanit.io.converter.intermodal;

import org.goplanit.converter.IdMapperType;
import org.goplanit.converter.intermodal.IntermodalWriter;
import org.goplanit.io.converter.network.PlanitNetworkWriter;
import org.goplanit.io.converter.network.PlanitNetworkWriterFactory;
import org.goplanit.io.converter.network.PlanitNetworkWriterSettings;
import org.goplanit.io.converter.zoning.PlanitZoningWriter;
import org.goplanit.io.converter.zoning.PlanitZoningWriterFactory;
import org.goplanit.io.converter.zoning.PlanitZoningWriterSettings;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.ServiceNetwork;
import org.goplanit.service.routed.RoutedServices;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.xml.generated.XMLElementMacroscopicNetwork;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
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
  
  /**
   * the id mapper to use
   */
  protected IdMapperType idMapper;

    
  /** Constructor with default country, use default destination Crs as a result.
   *  
   * @param outputDirectory to persist on
   * @param xmlRawNetwork to populate with PLANit network when persisting
   * @param xmlRawZoning to populate with PLANit zoning when persisting
   */
  protected PlanitIntermodalWriter(String outputDirectory, XMLElementMacroscopicNetwork xmlRawNetwork, XMLElementMacroscopicZoning xmlRawZoning) {
    this(outputDirectory,null,xmlRawNetwork, xmlRawZoning);
  }  
    
  /** Constructor 
   * @param outputDirectory to persist on
   * @param countryName to optimise projection for (if available, otherwise ignore)
   * @param xmlRawNetwork to populate with PLANit network when persisting
   * @param xmlRawZoning to populate with PLANit zoning when persisting
   */
  protected PlanitIntermodalWriter(String outputDirectory, String countryName, XMLElementMacroscopicNetwork xmlRawNetwork, XMLElementMacroscopicZoning xmlRawZoning) {
    this.idMapper = IdMapperType.XML;
    this.settings = new PlanitIntermodalWriterSettings(outputDirectory, countryName);
    this.xmlRawNetwork = xmlRawNetwork;
    this.xmlRawZoning = xmlRawZoning;
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(MacroscopicNetwork macroscopicNetwork, Zoning zoning) throws PlanItException {
    
    /* network writer */
    PlanitNetworkWriterSettings networkSettings = getSettings().getNetworkSettings();
    PlanitNetworkWriter networkWriter = PlanitNetworkWriterFactory.create(networkSettings.getOutputPathDirectory(), networkSettings.getCountry(), xmlRawNetwork);
    networkWriter.setIdMapperType(getIdMapperType());
    networkWriter.write(macroscopicNetwork);

    /* zoning writer - with pt component via transfer zones */
    PlanitZoningWriterSettings zoningSettings = getSettings().getZoningSettings();
    PlanitZoningWriter zoningWriter = 
        PlanitZoningWriterFactory.create(zoningSettings.getOutputPathDirectory(), zoningSettings.getCountry(), macroscopicNetwork.getCoordinateReferenceSystem());
    zoningWriter.setIdMapperType(getIdMapperType());    
    zoningWriter.write(zoning);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeWithServices(MacroscopicNetwork physicalNetwork, Zoning zoning, ServiceNetwork serviceNetwork, RoutedServices routedServices) throws PlanItException {
    write(physicalNetwork, zoning);
    throw new PlanItException("persisting routed services and service network not yet implemented for PLANit data format");
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
