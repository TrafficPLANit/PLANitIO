package org.planit.io.converter;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.planit.converter.BaseWriterImpl;
import org.planit.converter.IdMapperFunctionFactory;
import org.planit.converter.IdMapperType;
import org.planit.geo.PlanitOpenGisUtils;
import org.planit.io.converter.network.PlanitWriterSettings;
import org.planit.io.xml.util.JAXBUtils;
import org.planit.io.xml.util.PlanitSchema;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsCrsUtils;
import org.planit.utils.graph.Vertex;
import org.planit.utils.locale.CountryNames;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.TransferZoneGroup;
import org.planit.utils.zoning.Zone;

import net.opengis.gml.AbstractRingPropertyType;
import net.opengis.gml.CoordType;
import net.opengis.gml.LinearRingType;
import net.opengis.gml.ObjectFactory;
import net.opengis.gml.PointType;
import net.opengis.gml.PolygonType;

/**
 * Common functionality for writing in the native PLAnit format across different writers
 * 
 * @author markr
 *
 * @param <T>
 */
public abstract class PlanitWriterImpl<T> extends BaseWriterImpl<T>{
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitWriterImpl.class.getCanonicalName());

  /** user configurable settings for the writer */
  private final PlanitWriterSettings settings = new PlanitWriterSettings();
  
  /** path to persist to */
  private String path;
  
  /** file to persist to */
  private String fileName;  
  
  /** geo utils */
  private PlanitJtsCrsUtils geoUtils;
    
  /** when the destination CRS differs from the network CRS all geometries require transforming, for which this transformer will be initialised */
  private MathTransform destinationCrsTransformer = null;
  
  /** id mapper for nodes */
  private Function<Vertex, String> vertexIdMapper;
  /** id mapper for links */
  private Function<Link, String> linkIdMapper;
  /** id mapper for link segments */
  private Function<MacroscopicLinkSegment, String> linkSegmentIdMapper;
  /** id mapper for link segment types */
  private Function<MacroscopicLinkSegmentType, String> linkSegmentTypeIdMapper;
  /** id mapper for link segment types */
  private Function<Mode, String> modeIdMapper;  
  /** id mapper for zone ids */
  private Function<Zone, String> zoneIdMapper;  
  /** id mapper for connectoid ids */
  private Function<Connectoid, String> connectoidIdMapper;
  /** id mapper for transfer zone group ids */
  private Function<TransferZoneGroup, String> transferZoneGroupIdMapper;  
  
  /**
   * @param coordinate to convert to gml
   * @return created gml coordinate
   */
  protected CoordType createXmlOpenGisCoordType(Coordinate coordinate) {
    CoordType xmlCoord = new CoordType();
    Coordinate nodeCoordinate = null;
    try {
      if(getDestinationCrsTransformer()!=null) {
        nodeCoordinate = JTS.transform(coordinate, null, getDestinationCrsTransformer());
      }else {
        nodeCoordinate = coordinate;  
      }
    }catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe(String.format("unable to construct gml coordinate from %s ",coordinate.toString()));
    }
    
    xmlCoord.setX(BigDecimal.valueOf(nodeCoordinate.x));
    xmlCoord.setY(BigDecimal.valueOf(nodeCoordinate.y));   
    
    return xmlCoord;
  }  
  
  /** create an xml open gis PointType from a JTS Point and account for any crs transformation if needed
   * 
   * @param position to extract from
   * @return created PointType
   */
  protected PointType createXmlOpenGisPointType(Point position) {
    
    CoordType gmlcoord = createXmlOpenGisCoordType(position.getCoordinate());
    PointType xmlPointType = new PointType();
    xmlPointType.setCoord(gmlcoord);    

    return xmlPointType;
  }  
  
  /** create an xml open gis PolygonType from a JTS Point and account for any crs transformation if needed
   * 
   * @param polygon to extract from
   * @return created PolygonType
   */  
  protected PolygonType createOpenGisPolygonType(Polygon polygon) {
    ObjectFactory openGisObjectFactory = new ObjectFactory();
    PolygonType xmlPolygonType = new PolygonType();
    
    /* exterior */
    JAXBElement<AbstractRingPropertyType> xmlAbstractRingPropertyType = 
        openGisObjectFactory.createOuterBoundaryIs(openGisObjectFactory.createAbstractRingPropertyType());
    xmlPolygonType.setExterior(xmlAbstractRingPropertyType);    

    /* linearring */
    JAXBElement<LinearRingType> xmlLinearRingType = 
        openGisObjectFactory.createLinearRing(openGisObjectFactory.createLinearRingType());    
    xmlAbstractRingPropertyType.getValue().setRing(xmlLinearRingType);
    
    /* coordinates */
    List<CoordType> coordList = xmlLinearRingType.getValue().getCoord();    
    {
      Coordinate[] coords = polygon.getExteriorRing().getCoordinates();
      for(int index=0;index<coords.length;++index) {
        /* coordinate */
        coordList.add(createXmlOpenGisCoordType(coords[index]));
      }
    }       

    return xmlPolygonType;
  }  
     
  /** prepare the Crs transformer (if any) based on the user configuration settings
   * 
   * @param sourceCrs the crs used for the source material of this writer
   * @throws PlanItException thrown if error
   */
  protected void prepareCoordinateReferenceSystem(CoordinateReferenceSystem sourceCrs) throws PlanItException {
    
    if(sourceCrs != null) {
      geoUtils = new PlanitJtsCrsUtils(sourceCrs);
    }
    
    /* CRS and transformer (if needed) */
    CoordinateReferenceSystem destinationCrs = identifyDestinationCoordinateReferenceSystem(
        getSettings().getDestinationCoordinateReferenceSystem(),getSettings().getCountryName(), sourceCrs);    
    PlanItException.throwIfNull(destinationCrs, "destination Coordinate Reference System is null, this is not allowed");
    getSettings().setDestinationCoordinateReferenceSystem(destinationCrs);
    
    /* configure crs transformer if required, to be able to convert geometries to preferred CRS while writing */
    if(!destinationCrs.equals(sourceCrs)) {
      destinationCrsTransformer = PlanitOpenGisUtils.findMathTransform(sourceCrs, settings.getDestinationCoordinateReferenceSystem());
    }
  }
  
  /**
   * depending on the chosen id mapping, create the mapping functions for all id carrying entities that are persisted
   * @throws PlanItException thrown if error
   */
  protected void initialiseIdMappingFunctions() throws PlanItException {
    vertexIdMapper = IdMapperFunctionFactory.createVertexIdMappingFunction(getIdMapperType());
    linkIdMapper = IdMapperFunctionFactory.createLinkIdMappingFunction(getIdMapperType());
    linkSegmentIdMapper = IdMapperFunctionFactory.createLinkSegmentIdMappingFunction(getIdMapperType());
    linkSegmentTypeIdMapper = IdMapperFunctionFactory.createLinkSegmentTypeIdMappingFunction(getIdMapperType());
    modeIdMapper = IdMapperFunctionFactory.createModeIdMappingFunction(getIdMapperType());
    zoneIdMapper = IdMapperFunctionFactory.createZoneIdMappingFunction(getIdMapperType());
    connectoidIdMapper = IdMapperFunctionFactory.createConnectoidIdMappingFunction(getIdMapperType());
  } 
  
  /** get id mapper for nodes
   * @return id mapper
   */
  protected Function<Vertex, String> getVertexIdMapper(){
    return vertexIdMapper;
  }

  /** get id mapper for links
   * @return id mapper
   */  
  protected Function<Link, String> getLinkIdMapper(){
    return linkIdMapper;
  } 
  
  /** get id mapper for link segments
   * @return id mapper
   */  
  protected Function<MacroscopicLinkSegment, String> getLinkSegmentIdMapper(){
    return linkSegmentIdMapper;
  }  
  
  /** get id mapper for link segment types
   * @return id mapper
   */  
  protected Function<MacroscopicLinkSegmentType, String> getLinkSegmentTypeIdMapper(){
    return linkSegmentTypeIdMapper;
  }   
  
  /** get id mapper for modes
   * @return id mapper
   */  
  protected Function<Mode, String> getModeIdMapper(){
    return modeIdMapper;
  }   
  
  /** get id mapper for zones
   * @return id mapper
   */  
  protected Function<Zone, String> getZoneIdMapper(){
    return zoneIdMapper;
  }    
  
  /** get id mapper for connectoids
   * @return id mapper
   */  
  protected Function<Connectoid, String> getConnectoidIdMapper(){
    return connectoidIdMapper;
  } 
  
  /** get id mapper for transfer zone groups
   * @return id mapper
   */  
  protected Function<TransferZoneGroup, String> getTransferZoneGroupIdMapper(){
    return transferZoneGroupIdMapper;
  }   
  
  /** get the settings
   * @return settings
   */
  protected PlanitWriterSettings getSettings() {
    return settings;
  }
  
  /**
   * log settings
   */
  protected void logSettings() {
    settings.logSettings();
  }  
  

  /** get the destination crs transformer. Note it might be null and should only be collected after {@link prepareCoordinateReferenceSystem} has been invoked which determines
   * if and which transformer should be applied
   * 
   * @return destination crs transformer
   */
  protected MathTransform getDestinationCrsTransformer() {
    return destinationCrsTransformer;
  }
  
  /** geo util class based on source Crs (if any)
   * @return geoUtils
   */
  protected PlanitJtsCrsUtils getGeoUtils() {
    return geoUtils;
  }
  
  
  /** get file name to use
   * @param fileName to use
   */
  protected void setFileName(String fileName) {
    this.fileName = fileName;
  }  
    
  /** set path to use
   * @param path to use
   */
  protected void setPath(String path) {
    this.path = path;
  }    
  
  /**
   * persist the populated XML memory model to disk using JAXb
   * @throws PlanItException thrown if error
   */
  protected void persist(final Object xmlRootElement, final Class<?> rootElementClazz, final String planitSchemaName) throws PlanItException {
    try {      
      JAXBUtils.generateXmlFileFromObject(
          xmlRootElement, rootElementClazz, Paths.get(path, fileName), PlanitSchema.createPlanitSchemaUri(planitSchemaName));
    }catch(Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("unable to persist PLANit network in native format");
    }
  }   
   
  
  /** constructor
   * @param idMapperType to use
   * @param path to use
   * @param countryName the network applies to, used to determine destination crs (transformer) if not explicitly set
   */
  protected PlanitWriterImpl(IdMapperType idMapperType, String path, String fileName, String countryName) {
    super(idMapperType);
    this.path = path;
    this.fileName = fileName;
    
    if(countryName!=null && !countryName.isBlank()) {
      settings.setCountryName(countryName);
    }else {
      settings.setCountryName(CountryNames.WORLD);
    }
  }

  
  /** get file name to use
   * @return fileName used
   */
  public String getFileName() {
    return this.fileName;
  }
  
  /** get path 
   * @return path used
   */
  public String getPath() {
    return this.path;
  } 
 
}
