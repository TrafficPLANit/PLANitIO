package org.goplanit.io.converter.demands;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import org.goplanit.converter.ConverterWriterSettings;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;

/**
 * Configurable settings for the PLANit demands writer. Note that the default decimalformat used is a maximum of 4 digits 
 * to reduce the size of the OD matrices. This can however be altered by the user if desired.
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
    super(outputPathDirectory);
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

  public String getOriginSeparator() {
    return originSeparator;
  }

  public void setOriginSeparator(String originSeparator) {
    this.originSeparator = originSeparator;
  }

  public String getDestinationSeparator() {
    return destinationSeparator;
  }

  public void setDestinationSeparator(String destinationSeparator) {
    this.destinationSeparator = destinationSeparator;
  }
  
}
