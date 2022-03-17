package org.goplanit.io.converter.demands;

import java.util.logging.Logger;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;

/**
 * Configurable settings for the PLANit demands writer
 * 
 * @author markr
 *
 */
public class PlanitDemandsWriterSettings extends PlanitXmlWriterSettings implements ConverterWriterSettings {
  
  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PlanitDemandsWriterSettings.class.getCanonicalName());
      
  /**
   * Default constructor
   */
  public PlanitDemandsWriterSettings() {
    super();
  }
  
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   */
  public PlanitDemandsWriterSettings(final String outputPathDirectory) {
    super(outputPathDirectory);
  }  
    
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   * @param fileName to use
   */
  public PlanitDemandsWriterSettings(String outputPathDirectory, final String fileName) {
    super(outputPathDirectory, fileName);
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
  }  
  
}
