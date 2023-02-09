package org.goplanit.io.converter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.goplanit.converter.BaseWriterImpl;
import org.goplanit.converter.IdMapperFunctionFactory;
import org.goplanit.converter.IdMapperType;
import org.goplanit.io.geo.PlanitGmlUtils;
import org.goplanit.utils.id.ExternalIdAble;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.layer.service.ServiceLeg;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.xml.utils.JAXBUtils;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;
import org.goplanit.userclass.TravellerType;
import org.goplanit.userclass.UserClass;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Link;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.TransferZoneGroup;
import org.goplanit.utils.zoning.Zone;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import net.opengis.gml.CoordType;
import net.opengis.gml.CoordinatesType;
import net.opengis.gml.DirectPositionType;
import net.opengis.gml.LineStringType;
import net.opengis.gml.PointType;
import net.opengis.gml.PolygonType;

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

  /** parent network (layer) used id mappings to use for parent refs, if not set, use the same mapping as used for physical network */
  private Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> parentIdMapperByType;

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
  protected abstract void initialiseIdMappingFunctions();

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
   * All id mappers per type used by the writer
   *
   * @return newly created map with all mappings as used
   */
  public abstract Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> getIdMapperByType();

  /**
   * The explicit id mapping used by the parent (if any), so we use the appropriate referencing
   *
   * @param idMapperByType to use when dealing with parent network related references
   */
  public void setParentIdMapperTypes(final Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> idMapperByType) {
    parentIdMapperByType = idMapperByType;
  }

  public boolean hasParentIdMapperTypes() {
    return parentIdMapperByType != null && !parentIdMapperByType.isEmpty();
  }

  public Map<Class<? extends ExternalIdAble>, Function<? extends ExternalIdAble, String>> getParentIdMapperTypes() {
    return parentIdMapperByType;
  }
}
