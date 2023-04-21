package org.goplanit.io.converter.demands;

import org.goplanit.xml.generated.XMLElementMacroscopicDemand;
import org.goplanit.zoning.Zoning;

import static org.goplanit.io.converter.demands.PlanitDemandsWriter.DEFAULT_DEMANDS_XML;

/**
 * Factory for creating PLANit Demands Writers
 * 
 * @author markr
 *
 */
public class PlanitDemandsWriterFactory {
  
  /** Create a PLANitDemandsWriter which can persist a PLANit demands in the native PLANit XML format
   * 
   * @param demandsPath the path to use for persisting
   * @param parentZoning to use
   * @return created demands writer 
   */
  public static PlanitDemandsWriter create(final String demandsPath, final Zoning parentZoning) {
    return create(new PlanitDemandsWriterSettings(demandsPath, DEFAULT_DEMANDS_XML), parentZoning, new XMLElementMacroscopicDemand());
  }
  
  /** Create a PLANitZoningWriter which can persist a PLANit zoning in the native PLANit XML format. By providing the XML memory model instance to populate
   * we make it possible for the writer to embed the persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   * 
   * @param settings the settings to use
   * @param xmlRawDemands, use this specific XML memory model instance to populate and marshall via JAXb
   * @return created demands writer 
   */
  public static PlanitDemandsWriter create(final PlanitDemandsWriterSettings settings, final Zoning parentZoning, final XMLElementMacroscopicDemand xmlRawDemands) {
    return new PlanitDemandsWriter(settings, parentZoning, xmlRawDemands);
  }    
     
     
}
