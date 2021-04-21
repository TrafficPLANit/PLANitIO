package org.planit.io.converter.intermodal;

import org.planit.converter.IdMapperType;
import org.planit.converter.intermodal.IntermodalWriter;
import org.planit.io.converter.network.PlanitNetworkWriter;
import org.planit.io.converter.network.PlanitNetworkWriterFactory;
import org.planit.io.converter.zoning.PlanitZoningWriter;
import org.planit.io.converter.zoning.PlanitZoningWriterFactory;
import org.planit.network.InfrastructureNetwork;
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
  
  /**
   * The network writer to use
   */
  protected final PlanitNetworkWriter networkWriter;
  
  /**
   * The zoning writer to use
   */
  protected PlanitZoningWriter zoningWriter = null;
    
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
    this.networkWriter = PlanitNetworkWriterFactory.create(outputDirectory, countryName, xmlRawNetwork);
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(InfrastructureNetwork<?, ?> network, Zoning zoning) throws PlanItException {
    
    if(!(network instanceof MacroscopicNetwork)) {
      throw new PlanItException("PLANit intermodal writer currently only supports macroscopic networks");
    }
    MacroscopicNetwork macroscopicNetwork = (MacroscopicNetwork)network;
    
    /* network writer */
    networkWriter.write(network);

    /* zoning writer - with pt component via transfer zones */
    zoningWriter = PlanitZoningWriterFactory.create(networkWriter.getPath(), networkWriter.getCountryName(), macroscopicNetwork.getCoordinateReferenceSystem());
    zoningWriter.setIdMapperType(networkWriter.getIdMapperType());    
    zoningWriter.write(zoning);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public IdMapperType getIdMapperType() {
    return networkWriter.getIdMapperType();
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void setIdMapperType(IdMapperType idMapper) {
    networkWriter.setIdMapperType(idMapper);
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void reset() {
    networkWriter.reset();
    zoningWriter.reset();
  }

}
