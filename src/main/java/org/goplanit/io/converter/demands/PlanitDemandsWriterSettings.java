package org.goplanit.io.converter.demands;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;
import org.goplanit.utils.misc.LoggingUtils;

/**
 * Configurable settings for the PLANit demands writer. Note that the default decimal format used is a maximum of
 * 4 digits to reduce the size of the OD matrices. This can however be altered by the user if desired.
 * 
 * @author markr
 *
 */
public class PlanitDemandsWriterSettings extends PlanitXmlWriterSettings implements ConverterWriterSettings {
  
  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(PlanitDemandsWriterSettings.class.getCanonicalName());
  
  /** origin separator used in matrix values element */
  private String originSeparator = DEFAULT_ORIGIN_SEPARATOR;
  
  /** destination separator used in matrix values element */
  private String destinationSeparator = DEFAULT_DESTINATION_SEPARATOR;   
      
  /** default origin separator used in matrix values element */
  public static String DEFAULT_ORIGIN_SEPARATOR = " ";
  
  /** default origin separator used in matrix values element */
  public static String DEFAULT_DESTINATION_SEPARATOR = ",";  

  /** Validate the settings
   * 
   * @return true when valid, false otherwise
   */
  protected boolean validate() {
    return super.validate();
  }

  /** default demands file name to use */
  public static final String DEFAULT_DEMANDS_XML = "demands.xml";
  
  /**
   * Default constructor
   */
  public PlanitDemandsWriterSettings() {
    super();
  }
  
  /**
   * Constructor, requires user to se file name
   * 
   * @param outputPathDirectory to use
   */
  public PlanitDemandsWriterSettings(final String outputPathDirectory) {
    this(outputPathDirectory, DEFAULT_DEMANDS_XML);
  }  
    
  /**
   * Constructor
   * 
   * @param outputPathDirectory to use
   * @param fileName to use
   */
  public PlanitDemandsWriterSettings(String outputPathDirectory, final String fileName) {
    super(outputPathDirectory, fileName, null /* no country used for demands */);
    
    /* update decimal format to 4 digits for od values */
    var demandWriterDecimalFormat = (DecimalFormat)this.getDecimalFormat().clone();
    demandWriterDecimalFormat.setMaximumFractionDigits(4);
    setDecimalFormat(demandWriterDecimalFormat);
  }  
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    super.reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logSettings() {
    LOGGER.info(LoggingUtils.settingsHeader("PLANit Demands Writer Settings"));
    LOGGER.info(LoggingUtils.settingsValue("Origin separator", getOriginSeparator(), 0));
    LOGGER.info(LoggingUtils.settingsValue("Destination separator", getDestinationSeparator(), 0));
    super.logSettings();
  }

  /**
   * separator used in between origins
   * @return sep string
   */
  public String getOriginSeparator() {
    return originSeparator;
  }

  /**
   * set separator used in between origins
   * @param originSeparator string to use
   */
  public void setOriginSeparator(String originSeparator) {
    this.originSeparator = originSeparator;
  }

  /**
   * separator used in between destinations
   * @return sep string
   */
  public String getDestinationSeparator() {
    return destinationSeparator;
  }

  /**
   * set separator used in between destinations
   * @param destinationSeparator string to use
   */
  public void setDestinationSeparator(String destinationSeparator) {
    this.destinationSeparator = destinationSeparator;
  }
  
}
