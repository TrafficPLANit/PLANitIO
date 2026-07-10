package org.goplanit.io.converter.demands;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.xml.generated.v2.XMLElementDiscreteDemand;
import org.goplanit.xml.generated.v2.XMLElementMacroscopicDemand;
import org.goplanit.zoning.Zoning;

/**
 * Factory for creating PLANit Discrete Demands Writers
 * 
 * @author markr
 *
 */
public class PlanitDiscreteDemandsWriterFactory {

  /**
   * Dummy constructor as never instantiated
   */
  private PlanitDiscreteDemandsWriterFactory() {
    // compliance to avoid javadoc warnings
  }

  /** Create a PlanitDiscreteDemandsWriter which can persist a PLANit PlanitDiscreteDemandsWriter in the native
   * PLANit XML format. The reference
   * zoning is expected to be set manually, or will be provided automatically when using a converter. The user is
   * expected to provide the output location via the settings afterwards.
   *
   * @return created PlanitDiscreteDemandsWriter writer
   */
  public static PlanitDiscreteDemandsWriter create() {
    return new PlanitDiscreteDemandsWriter(
        new PlanitDiscreteDemandsWriterSettings(
            PlanitDiscreteDemandsWriterSettings.DEFAULT_DEMANDS_XML),
        new XMLElementDiscreteDemand());
  }

  /** Create a PlanitDiscreteDemandsWriter which can persist a PLANit PlanitDiscreteDemandsWriter in the native
   * PLANit XML format. The reference zoning is expected to be set manually, or will be provided automatically
   * when using a converter.
   *
   * @param demandsPath the path to use for persisting
   * @return created PlanitDiscreteDemandsWriter writer
   */
  public static PlanitDiscreteDemandsWriter create(final String demandsPath) {
    return new PlanitDiscreteDemandsWriter(
        new PlanitDiscreteDemandsWriterSettings(
            demandsPath, PlanitDiscreteDemandsWriterSettings.DEFAULT_DEMANDS_XML),
        new XMLElementDiscreteDemand());
  }
  
  /** Create a PlanitDiscreteDemandsWriter which can persist a PLANit PlanitDiscreteDemandsWriter
   *  in the native PLANit XML format
   * 
   * @param demandsPath the path to use for persisting
   * @param parentNetwork to use
   * @param parentZoning to use
   * @return created PlanitDiscreteDemandsWriter writer
   */
  public static PlanitDiscreteDemandsWriter create(
      final String demandsPath,
      final MacroscopicNetwork parentNetwork,
      final Zoning parentZoning) {
    return create(
        new PlanitDiscreteDemandsWriterSettings(
            demandsPath,
            PlanitDiscreteDemandsWriterSettings.DEFAULT_DEMANDS_XML),
        parentNetwork,
        parentZoning,
        new XMLElementDiscreteDemand());
  }
  
  /** Create a PlanitDiscreteDemandsWriter which can persist a PLANit PlanitDiscreteDemandsWriter in the native
   * PLANit XML format. By providing the XML memory model instance to populate
   * we make it possible for the writer to embed the persisting in another larger XML memory model that is
   * marshalled by an entity other than this writer in the future
   * 
   * @param settings the settings to use
   * @param parentNetwork to use
   * @param parentZoning to use
   * @param xmlRawDemands, use this specific XML memory model instance to populate and marshall via JAXb
   * @return created PlanitDiscreteDemandsWriter writer
   */
  public static PlanitDiscreteDemandsWriter create(
      final PlanitDiscreteDemandsWriterSettings settings,
      final MacroscopicNetwork parentNetwork,
      final Zoning parentZoning,
      final XMLElementDiscreteDemand xmlRawDemands) {
    return new PlanitDiscreteDemandsWriter(settings, parentZoning, xmlRawDemands);
  }    
     
     
}
