package org.goplanit.io.converter.demands;

import org.goplanit.xml.generated.XMLElementMacroscopicDemand;

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
   * @return created demands writer 
   */
  public static PlanitDemandsWriter create(final String demandsPath) {
    return create(demandsPath, new XMLElementMacroscopicDemand());    
  }
  
  /** Create a PLANitZoningWriter which can persist a PLANit zoning in the native PLANit XML format. By providing the XML memory model instance to populate
   * we make it possible for the writer to embed the persisting in another larger XML memory model that is marshalled by an entity other than this writer in the future
   * 
   * @param demandsPath the path to use for persisting
   * @param xmlRawDemands, use this specific XML memory model instance to populate and marshall via JAXb
   * @return created demands writer 
   */
  public static PlanitDemandsWriter create(final String demandsPath, final XMLElementMacroscopicDemand xmlRawDemands) {
    return new PlanitDemandsWriter(demandsPath, xmlRawDemands);    
  }    
     
     
}
