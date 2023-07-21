package org.goplanit.io.converter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.converter.*;
import org.goplanit.converter.idmapping.*;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.xml.utils.JAXBUtils;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.mode.Mode;

/**
 * Common functionality for writing in the native PLANit format across different writers
 * 
 * @author markr
 *
 * @param <T> type to generate with this writer
 */
public abstract class PlanitWriterImpl<T> extends CrsWriterImpl<T>{
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitWriterImpl.class.getCanonicalName());

  /** convert to xml writer settings if possible
   * @return xml writer settings
   */
  protected PlanitXmlWriterSettings getSettingsAsXmlWriterSettings(){
    if(!(getSettings() instanceof PlanitXmlWriterSettings)) {
      throw new PlanItRunTimeException("Planit writer settings expected to be of type PlanitXmlWriterSettings, this is not the case");
    }
    return ((PlanitXmlWriterSettings)getSettings());
  }

  /** Get the reference to use whenever a mode reference is encountered
   *
   * @param mode to collect reference for
   * @param modeIdMapper to use
   * @return modeReference for the mode
   */
  protected String getXmlModeReference(Mode mode, Function<Mode, String> modeIdMapper) {
    String modeReference = null;

    if(mode.isPredefinedModeType()) {
      /* predefined modes, must utilise, their predefined XML id/name, this overrules the mapper (if any) */
      modeReference = mode.getXmlId();
    }else {
      modeReference =modeIdMapper.apply(mode);
    }

    if(modeReference == null) {
      LOGGER.severe(String.format("mode reference cound not be obtained for mode %s", mode));
    }

    return modeReference;
  }

  /**
   * Persist the populated XML memory model to disk using JAXb
   * 
   * @param xmlRootElement to persist from
   * @param rootElementClazz the type of the root element object
   * @param planitSchemaName schema the XML complies with
   */
  protected void persist(final Object xmlRootElement, final Class<?> rootElementClazz, final String planitSchemaName) {
    PlanitXmlWriterSettings xmlWriterSettings = getSettingsAsXmlWriterSettings();

    PlanItRunTimeException.throwIf(
        xmlWriterSettings.getOutputDirectory()==null || xmlWriterSettings.getOutputDirectory().isBlank(), "no output directory provided, unable to persist in native Planit XML format");
    PlanItRunTimeException.throwIf(
        xmlWriterSettings.getFileName()==null || xmlWriterSettings.getFileName().isBlank(), "no output file name provided, unable to persist in native Planit XML format");
    Path outputDir = Paths.get(xmlWriterSettings.getOutputDirectory());
    Path outputPath = Paths.get(xmlWriterSettings.getOutputDirectory(), xmlWriterSettings.getFileName());
    
    /* try to create the directory if it does not exist */
    
    try { 
      if(!Files.exists(outputDir)) {
        // Files.createDirectory(outputDir.toAbsolutePath().normalize()); //<- preferred but for some reason doesn't always work with more than one subdir missing
        new File(outputDir.toAbsolutePath().normalize().toString()).mkdirs();
      }
    }catch(Exception e) {      
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException(String.format("Unable to create output directory for %s", Paths.get(xmlWriterSettings.getOutputDirectory()).toAbsolutePath()));
    }
    
    try {      
      JAXBUtils.generateXmlFileFromObject(xmlRootElement, rootElementClazz, outputPath, PlanitSchema.createPlanitSchemaUri(planitSchemaName));
    }catch(Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItRunTimeException("Unable to persist PLANit network in native format");
    }
  }

  /** Constructor
   * 
   * @param idMapperType to use
   */
  protected PlanitWriterImpl(IdMapperType idMapperType) {
    super(idMapperType);
  }

}
