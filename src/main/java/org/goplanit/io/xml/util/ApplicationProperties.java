package org.goplanit.io.xml.util;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import org.goplanit.io.output.formatter.PlanItOutputFormatter;

/**
 * Class supporting easy access to application.properties file
 * 
 * @author markr
 */
public class ApplicationProperties {
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(ApplicationProperties.class.getCanonicalName());
  
  /** holds the application properties */
  private static final Properties applicationProperties = new Properties();
    
  public static final String APPLICATION_PROPERTIES_FILE_NAME = "application.properties";
  public static final String DESCRIPTION_PROPERTY_KEY = "planit.description";
  public static final String VERSION_PROPERTY_KEY = "planit.version";  

  /* collect once */
  static {

    InputStream input = PlanItOutputFormatter.class.getClassLoader().getResourceAsStream(APPLICATION_PROPERTIES_FILE_NAME);
    try{
  
      if (input == null) {
        LOGGER.severe("Application properties " + APPLICATION_PROPERTIES_FILE_NAME + " could not be found");
      }     
  
      // load a properties file from class path, inside static method
      applicationProperties.load(input);
      input.close();
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("Error when parsing application.properties file in PLANitIO");
      
      try{
        input.close();
      }catch(Exception e2) {
        LOGGER.severe("Unable to close input stream for reading application.properties");    
      }
    }
  }
  
  /** collect property by key
   * @param key to collect value for
   * @return value if present
   */
  public static String getPropertyByKey(String key) {
    String value = applicationProperties.getProperty(key);
    if (value == null) {
      LOGGER.warning(String.format("property %s could not be collected from application.properties file",key));
    }   
    return value;
  }
  
  /** collect description value
   * @return description if present
   */
  public static String getDescription() {
    return getPropertyByKey(DESCRIPTION_PROPERTY_KEY);    
  }  
  
  /** collect version value
   * @return version if present
   */
  public static String getVersion() {
    return getPropertyByKey(VERSION_PROPERTY_KEY);    
  }   
}
