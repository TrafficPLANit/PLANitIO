package org.goplanit.io.converter.demands;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;

import java.text.DecimalFormat;
import java.util.logging.Logger;

/**
 * Configurable settings for the PLANit discrete demands writer.
 * 
 * @author markr
 *
 */
public class PlanitDiscreteDemandsWriterSettings extends PlanitXmlWriterSettings implements ConverterWriterSettings {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PlanitDiscreteDemandsWriterSettings.class.getCanonicalName());

  /** Validate the settings
   *
   * @return true when valid, false otherwise
   */
  protected boolean validate() {
    return super.validate();
  }

  /** default demands file name to use */
  public static final String DEFAULT_DEMANDS_XML = "discrete_demands.xml";

  /**
   * Default constructor
   */
  public PlanitDiscreteDemandsWriterSettings() {
    super();
  }

  /**
   * Constructor, requires user to se file name
   *
   * @param outputPathDirectory to use
   */
  public PlanitDiscreteDemandsWriterSettings(final String outputPathDirectory) {
    this(outputPathDirectory, DEFAULT_DEMANDS_XML);
  }

  /**
   * Constructor
   *
   * @param outputPathDirectory to use
   * @param fileName to use
   */
  public PlanitDiscreteDemandsWriterSettings(String outputPathDirectory, final String fileName) {
    super(outputPathDirectory, fileName, null /* no country used for demands */);
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
  }

}
