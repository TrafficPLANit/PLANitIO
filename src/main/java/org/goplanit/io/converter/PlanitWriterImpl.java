package org.goplanit.io.converter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.logging.Logger;

import org.goplanit.converter.*;
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
public abstract class PlanitWriterImpl<T> extends BaseWriterImpl<T>{
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitWriterImpl.class.getCanonicalName());

  /** id mappers for network entities */
  private NetworkIdMapper networkIdMappers;

  /** id mappers for zoning entities */
  private ZoningIdMapper zoningIdMappers;

  /** id mappers for service network entities */
  private ServiceNetworkIdMapper serviceNetworkIdMapper;

  /** id mappers for routed services entities */
  private RoutedServicesIdMapper routedServicesIdMapper;

  /** id mappers for routed services entities */
  private DemandsIdMapper demandsIdMapperIdMapper;


  /** convert to xml writer settings if possible
   * @return xml writer settings
   * @throws PlanItException thrown if error
   */
  private PlanitXmlWriterSettings getSettingsAsXmlWriterSettings() throws PlanItException {
    if(!(getSettings() instanceof PlanitXmlWriterSettings)) {
      throw new PlanItException("planit writer settings expected to be of type PlanitXmlWriterSettings, this is not the case");
    }
    return ((PlanitXmlWriterSettings)getSettings());
  }

  /**
   * depending on the chosen id mapping, create the mapping functions for all id carrying entities that are persisted
   */
  protected void initialiseIdMappingFunctions(){
    /* when not set as parent based, create based on chosen type of current writer */
    if(networkIdMappers == null) {
      networkIdMappers = new NetworkIdMapper(getIdMapperType());
    }
    if(zoningIdMappers == null){
      zoningIdMappers = new ZoningIdMapper(getIdMapperType());
    }
    if(serviceNetworkIdMapper == null){
      serviceNetworkIdMapper = new ServiceNetworkIdMapper(getIdMapperType());
    }
    if(routedServicesIdMapper == null){
      routedServicesIdMapper = new RoutedServicesIdMapper(getIdMapperType());
    }
    if(demandsIdMapperIdMapper == null){
      demandsIdMapperIdMapper = new DemandsIdMapper((getIdMapperType()));
    }
  }

  protected NetworkIdMapper getNetworkIdMappers(){
    return networkIdMappers;
  }

  protected ZoningIdMapper getZoningIdMappers(){
    return zoningIdMappers;
  }

  protected ServiceNetworkIdMapper getServiceNetworkIdMappers(){
    return serviceNetworkIdMapper;
  }

  protected RoutedServicesIdMapper getRoutedServicesIdMapper(){
    return routedServicesIdMapper;
  }

  protected DemandsIdMapper getDemandsIdMapper(){
    return demandsIdMapperIdMapper;
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
   * @throws PlanItException thrown if error
   */
  protected void persist(final Object xmlRootElement, final Class<?> rootElementClazz, final String planitSchemaName) throws PlanItException {
    PlanitXmlWriterSettings xmlWriterSettings = getSettingsAsXmlWriterSettings();
        
    PlanItException.throwIf(
        xmlWriterSettings.getOutputPathDirectory()==null || xmlWriterSettings.getOutputPathDirectory().isBlank(), "no output directory provided, unable to persist in native Planit XML format");
    PlanItException.throwIf(
        xmlWriterSettings.getFileName()==null || xmlWriterSettings.getFileName().isBlank(), "no output file name provided, unable to persist in native Planit XML format");
    Path outputDir = Paths.get(xmlWriterSettings.getOutputPathDirectory());
    Path outputPath = Paths.get(xmlWriterSettings.getOutputPathDirectory(), xmlWriterSettings.getFileName());
    
    /* try to create the directory if it does not exist */
    
    try { 
      if(!Files.exists(outputDir)) {
        // Files.createDirectory(outputDir.toAbsolutePath().normalize()); //<- preferred but for some reason doesn't always work with more than one subdir missing
        new File(outputDir.toAbsolutePath().normalize().toString()).mkdirs();
      }
    }catch(Exception e) {      
      LOGGER.severe(e.getMessage());
      throw new PlanItException(String.format("Unable to create output directory for %s", Paths.get(xmlWriterSettings.getOutputPathDirectory()).toAbsolutePath()));      
    }
    
    try {      
      JAXBUtils.generateXmlFileFromObject(xmlRootElement, rootElementClazz, outputPath, PlanitSchema.createPlanitSchemaUri(planitSchemaName));
    }catch(Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Unable to persist PLANit network in native format");
    }
  }

  /** Constructor
   * 
   * @param idMapperType to use
   */
  protected PlanitWriterImpl(IdMapperType idMapperType) {
    super(idMapperType);
  }

  /**
   * The (main) Id mapper used by this writer
   *
   * @return mapper
   */
  public abstract PlanitComponentIdMapper getPrimaryIdMapper();

  /**
   * The explicit id mapping used by the parent(s), so we use the appropriate referencing
   *
   * @param parentMappers to register
   */
  public void setParentIdMappers(PlanitComponentIdMapper... parentMappers) {
    for(var mapper : parentMappers) {
      if (mapper instanceof ZoningIdMapper) {
        this.zoningIdMappers = (ZoningIdMapper) mapper;
      } else if (mapper instanceof NetworkIdMapper) {
        this.networkIdMappers = (NetworkIdMapper) mapper;
      } else if( mapper instanceof ServiceNetworkIdMapper){
        this.serviceNetworkIdMapper = (ServiceNetworkIdMapper) mapper;
      }else if( mapper instanceof RoutedServicesIdMapper){
        this.routedServicesIdMapper = (RoutedServicesIdMapper) mapper;
      }else if( mapper instanceof  DemandsIdMapper){
        this.demandsIdMapperIdMapper = (DemandsIdMapper) mapper;
      }else{
        LOGGER.warning("Unknown parent id mapper provided, ignored");
      }
    }
  }

}
