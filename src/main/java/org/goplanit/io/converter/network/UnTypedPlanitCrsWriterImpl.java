package org.goplanit.io.converter.network;

import net.opengis.gml.*;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.goplanit.converter.IdMapperType;
import org.goplanit.io.converter.PlanitWriterImpl;
import org.goplanit.io.geo.PlanitGmlUtils;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.geo.PlanitJtsUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.util.logging.Logger;

/**
 * Common functionality for writing in the native PLANit format across different writers tailored towards Crs based writers, i.e., requiring
 * to write out GIS data
 * 
 * @author markr
 *
 * @param <T> type to generate with this writer
 */
public abstract class UnTypedPlanitCrsWriterImpl<T> extends PlanitWriterImpl<T> {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(UnTypedPlanitCrsWriterImpl.class.getCanonicalName());

  /** geo utils */
  private PlanitJtsCrsUtils geoUtils;

  /** when the destination CRS differs from the network CRS all geometries require transforming, for which this transformer will be initialised */
  private MathTransform destinationCrsTransformer = null;

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
  private Coordinate getTransformedCoordinate(final Coordinate coordinate) {
    try {
      if(getDestinationCrsTransformer()!=null) {
        return JTS.transform(coordinate, null, getDestinationCrsTransformer());
      }
      return coordinate;
    }catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe(String.format("unable to transform coordinate from %s ",coordinate.toString()));
    }
    return null;
  }

  /** Transform the coordinate absed on the destination transformer
   *
   * @param coordinates to transform
   * @return transformed coordinates (if no conversion is required, input is returned
   */
  private Coordinate[] getTransformedCoordinates(final Coordinate[] coordinates) {
    Coordinate[] transformedCoordinates = null;
    try {
      if(getDestinationCrsTransformer()!=null) {

        transformedCoordinates = new Coordinate[coordinates.length];
        for(int index = 0; index < coordinates.length ; ++index) {
          transformedCoordinates[index] = JTS.transform(coordinates[index], null, getDestinationCrsTransformer());
        }
      }else {
        transformedCoordinates = coordinates;
      }
    }catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe(String.format("unable to transform coordinates from %s ",coordinates.toString()));
    }
    return transformedCoordinates;
  }

  /** Extract the srs name to use based on the available crs information on network and settings
   *
   * @param xmlSettings to use
   * @return srsName to use
   */
  protected static String extractSrsName(PlanitXmlWriterSettings xmlSettings) {
    String srsName = "";
    if("EPSG".equals(xmlSettings.getDestinationCoordinateReferenceSystem().getName().getCodeSpace())) {
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
        throw new PlanItRunTimeException("Unable to extract epsg code from destination crs %s", xmlSettings.getDestinationCoordinateReferenceSystem().getName());
      }
    }else if(!xmlSettings.getDestinationCoordinateReferenceSystem().equals(PlanitJtsCrsUtils.CARTESIANCRS)) {
      throw new PlanItRunTimeException("Unable to extract epsg code from destination crs %s", xmlSettings.getDestinationCoordinateReferenceSystem().getName());
    }
    return srsName;
  }

  /** Create a position type based on point location
   *
   * @param position to convert to GML and transform if needed
   * @return created GML pos
   */
  protected DirectPositionType createGmlDirectPositionType(Point position) {
    Coordinate positioncoordinate = getTransformedCoordinate(position.getCoordinate());
    return PlanitGmlUtils.createGmlDirectPositionType(positioncoordinate);
  }


  /** Create a GML coord type from the provided coordinate
   *
   * @param coordinate to convert to GML and transform if needed
   * @return created GML coordinate
   */
  protected CoordType createGmlCoordType(Coordinate coordinate) {
    Coordinate nodeCoordinate = getTransformedCoordinate(coordinate);
    return PlanitGmlUtils.createGmlCoordType(nodeCoordinate);
  }

  /** Create a GML PointType from a JTS Point and account for any crs transformation if needed
   *
   * @param position to extract from
   * @return created PointType
   */
  protected PointType createGmlPointType(Point position) {
    Coordinate pointCoordinate = getTransformedCoordinate(position.getCoordinate());
    return PlanitGmlUtils.createGmlPointType(pointCoordinate);
  }

  /** create a GML PolygonType from a JTS Polygon and account for any crs transformation if needed
   *
   * @param polygon to extract from
   * @return created PolygonType
   */
  protected PolygonType createGmlPolygonType(Polygon polygon) {
    Coordinate[] transformedCoordinates = getTransformedCoordinates(polygon.getCoordinates());
    return PlanitGmlUtils.createGmlPolygonType(transformedCoordinates);
  }

  /** Create a GML LineStringType from a JTS LineStringand account for any crs transformation if needed
   *
   * @param lineString to extract from
   * @return created LineStringType
   */
  protected LineStringType createGmlLineStringType(LineString lineString) {
    /* transformed coords */
    Coordinate[] transformedCoordinates = getTransformedCoordinates(lineString.getCoordinates());

    /* gml coords*/
    PlanitXmlWriterSettings xmlSettings = null;
    try {
      xmlSettings = getSettingsAsXmlWriterSettings();
    } catch (PlanItException e) {
      LOGGER.severe("settings not available as XML writer settings, this shouldn't happen");
    }
    CoordinatesType coordsType = PlanitGmlUtils.createGmlCoordinatesType(
        transformedCoordinates, xmlSettings.getCommaSeparator(), xmlSettings.getDecimalSeparator(), xmlSettings.getDecimalFormat(), xmlSettings.getTupleSeparator());

    /* gml line string */
    return PlanitGmlUtils.createGmlLineStringType(coordsType);
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
      destinationCrsTransformer = PlanitJtsUtils.findMathTransform(sourceCrs, xmlWriterSettings.getDestinationCoordinateReferenceSystem());
    }
  }


  /** get the destination crs transformer. Note it might be null and should only be collected after {@link #prepareCoordinateReferenceSystem(CoordinateReferenceSystem)} has been invoked which determines
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

  /** Constructor
   *
   * @param idMapperType to use
   */
  protected UnTypedPlanitCrsWriterImpl(IdMapperType idMapperType) {
    super(idMapperType);
  }


}
