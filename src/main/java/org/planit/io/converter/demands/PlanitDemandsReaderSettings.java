package org.planit.io.converter.demands;

import org.planit.zoning.Zoning;
import org.planit.converter.ConverterReaderSettings;
import org.planit.io.xml.util.PlanitXmlReaderSettings;
import org.planit.network.MacroscopicNetwork;

/**
 * Settings for the PLANit zoning reader
 * 
 * @author markr
 *
 */
public class PlanitDemandsReaderSettings extends PlanitXmlReaderSettings implements ConverterReaderSettings {

  /**
   * Reference network to use when demand relate to network entities
   */
  protected MacroscopicNetwork referenceNetwork;
  
  /**
   * Reference zoning to use when demands relate to zoning entities
   */  
  protected Zoning referenceZoning;    
  
  /** Collect reference network used
   * 
   * @return reference network
   */
  protected MacroscopicNetwork getReferenceNetwork() {
    return referenceNetwork;
  }
  
  /** Collect reference zoning used
   * 
   * @return reference zoning
   */  
  protected Zoning getReferenceZoning() {
    return referenceZoning;
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    // TODO     
  }    
       
  /** Set reference network to use
   * 
   * @param referenceNetwork to use
   */
  public void setReferenceNetwork(final MacroscopicNetwork referenceNetwork) {
    this.referenceNetwork = referenceNetwork;
  }

  /** Set reference zoning to use
   * 
   * @param referenceZoning to use
   */
  public void setReferenceZoning(final Zoning referenceZoning) {
    this.referenceZoning = referenceZoning;
  }
  


   
}
