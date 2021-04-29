package org.planit.io.converter;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.planit.converter.BaseWriterImpl;
import org.planit.converter.IdMapperFunctionFactory;
import org.planit.converter.IdMapperType;
import org.planit.geo.PlanitOpenGisUtils;
import org.planit.io.xml.util.JAXBUtils;
import org.planit.io.xml.util.PlanitSchema;
import org.planit.io.xml.util.PlanitXmlWriterSettings;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsCrsUtils;
import org.planit.utils.graph.Vertex;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.zoning.Connectoid;
import org.planit.utils.zoning.TransferZoneGroup;
import org.planit.utils.zoning.Zone;

import net.opengis.gml.AbstractRingPropertyType;
import net.opengis.gml.CoordType;
import net.opengis.gml.DirectPositionType;
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
  
  /** transform the coordinate absed on the destination transformer
   * @param coordinate to transform
   * @return transformed coordinate
   */
  private Coordinate getTransformedCoordinate(Coordinate coordinate) {
    try {
      if(getDestinationCrsTransformer()!=null) {
        return JTS.transform(coordinate, null, getDestinationCrsTransformer());
      }
      return coordinate;  
    }catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe(String.format("unable to construct gml coordinate from %s ",coordinate.toString()));
    }
    return null;
  }  
  
  /** extract the src name to use based on the available crs information on network and settings
   * @return srsName to use
   * @throws PlanItException thrown if error
   */
  protected static String extractSrsName(PlanitXmlWriterSettings xmlSettings) throws PlanItException {
    String srsName = "";
    if(xmlSettings.getDestinationCoordinateReferenceSystem().getName().getCodeSpace().equals("EPSG")) {
      /* spatial crs based on epsg code*/
      Integer epsgCode = null;
      try {
        epsgCode = CRS.lookupEpsgCode(xmlSettings.getDestinationCoordinateReferenceSystem(), false);
        if(epsgCode == null) {
          /* full scan */
          epsgCode = CRS.lookupEpsgCode(xmlSettings.getDestinationCoordinateReferenceSystem(), true);
        }
        srsName = String.format("EPSG:%s",epsgCode.toString());
      }catch (Exception e) {
        LOGGER.severe(e.getMessage());
        throw new PlanItException("Unable to extract epsg code from destination crs %s", xmlSettings.getDestinationCoordinateReferenceSystem().getName());
      }      
    }else if(!xmlSettings.getDestinationCoordinateReferenceSystem().equals(PlanitJtsCrsUtils.CARTESIANCRS)) {
      throw new PlanItException("Unable to extract epsg code from destination crs %s", xmlSettings.getDestinationCoordinateReferenceSystem().getName());
    }
    return srsName;
  }  

  /**
   * @param Point to convert to gml
   * @return created gml pos
   */
  protected DirectPositionType createXmlOpenGisDirectPositionType(Point position) {
    Coordinate positioncoordinate = getTransformedCoordinate(position.getCoordinate());
    
    DirectPositionType xmlPos = new DirectPositionType();
    xmlPos.getValue().add(positioncoordinate.x);
    xmlPos.getValue().add(positioncoordinate.y);
    
    return xmlPos;
  }  


  /**
   * @param coordinate to convert to gml
   * @return created gml coordinate
   */
  protected CoordType createXmlOpenGisCoordType(Coordinate coordinate) {
    CoordType xmlCoord = new CoordType();
    
    Coordinate nodeCoordinate = getTransformedCoordinate(coordinate);    
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
    
    DirectPositionType gmlDirectPos = createXmlOpenGisDirectPositionType(position);
    PointType xmlPointType = new PointType();
    xmlPointType.setPos(gmlDirectPos);    

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
    
    PlanitXmlWriterSettings xmlWriterSettings = getSettingsAsXmlWriterSettings();
    
    /* CRS and transformer (if needed) */
    CoordinateReferenceSystem destinationCrs = 
        identifyDestinationCoordinateReferenceSystem(xmlWriterSettings.getDestinationCoordinateReferenceSystem(),xmlWriterSettings.getCountry(), sourceCrs);    
    PlanItException.throwIfNull(destinationCrs, "destination Coordinate Reference System is null, this is not allowed");
    xmlWriterSettings.setDestinationCoordinateReferenceSystem(destinationCrs);
    
    /* configure crs transformer if required, to be able to convert geometries to preferred CRS while writing */
    if(!destinationCrs.equals(sourceCrs)) {
      destinationCrsTransformer = PlanitOpenGisUtils.findMathTransform(sourceCrs, xmlWriterSettings.getDestinationCoordinateReferenceSystem());
    }
  }
  
  /**
   * depending on the chosen id mapping, create the mapping functions for all id carrying entities that are persisted
   * @throws PlanItException thrown if error
   */
  protected void initialiseIdMappingFunctions() throws PlanItException {
    this.vertexIdMapper = IdMapperFunctionFactory.createVertexIdMappingFunction(getIdMapperType());
    this.linkIdMapper = IdMapperFunctionFactory.createLinkIdMappingFunction(getIdMapperType());
    this.linkSegmentIdMapper = IdMapperFunctionFactory.createLinkSegmentIdMappingFunction(getIdMapperType());
    this.linkSegmentTypeIdMapper = IdMapperFunctionFactory.createLinkSegmentTypeIdMappingFunction(getIdMapperType());
    this.modeIdMapper = IdMapperFunctionFactory.createModeIdMappingFunction(getIdMapperType());
    this.zoneIdMapper = IdMapperFunctionFactory.createZoneIdMappingFunction(getIdMapperType());
    this.connectoidIdMapper = IdMapperFunctionFactory.createConnectoidIdMappingFunction(getIdMapperType());
    this.transferZoneGroupIdMapper = IdMapperFunctionFactory.createTransferZoneGroupIdMappingFunction(getIdMapperType());
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
    
  
  /**
   * persist the populated XML memory model to disk using JAXb
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
        Files.createDirectory(outputDir);
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
   
  
  /** constructor
   * @param idMapperType to use
   * @param path to use
   * @param countryName the network applies to, used to determine destination crs (transformer) if not explicitly set
   */
  protected PlanitWriterImpl(IdMapperType idMapperType) {
    super(idMapperType);
  }

 
 
}
