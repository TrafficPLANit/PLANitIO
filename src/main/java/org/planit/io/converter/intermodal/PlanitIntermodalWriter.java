package org.planit.io.converter.intermodal;

import org.planit.converter.IdMapperType;
import org.planit.converter.intermodal.IntermodalWriter;
import org.planit.io.converter.network.PlanitNetworkWriter;
import org.planit.io.converter.network.PlanitNetworkWriterFactory;
import org.planit.io.converter.network.PlanitNetworkWriterSettings;
import org.planit.io.converter.zoning.PlanitZoningWriter;
import org.planit.io.converter.zoning.PlanitZoningWriterFactory;
import org.planit.io.converter.zoning.PlanitZoningWriterSettings;
import org.planit.network.TransportLayerNetwork;
import org.planit.network.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.xml.generated.XMLElementMacroscopicNetwork;
import org.planit.xml.generated.XMLElementMacroscopicZoning;
import org.planit.zoning.Zoning;

/**
 * Planit intermodal writer for native Planit format, wrapping a planit network writer and planit zoning writer in one
 * 
 * @author markr
 *
 */
public class PlanitIntermodalWriter implements IntermodalWriter {
  
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
  public void write(TransportLayerNetwork<?, ?> network, Zoning zoning) throws PlanItException {
    
    if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItException("PLANit intermodal writer currently only supports macroscopic networks");
    }
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork)network;
    
    /* network writer */
    PlanitNetworkWriterSettings networkSettings = getSettings().getNetworkSettings();
    PlanitNetworkWriter networkWriter = PlanitNetworkWriterFactory.create(networkSettings.getOutputPathDirectory(), networkSettings.getCountry(), xmlRawNetwork);
    networkWriter.setIdMapperType(getIdMapperType());
    networkWriter.write(network);

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
