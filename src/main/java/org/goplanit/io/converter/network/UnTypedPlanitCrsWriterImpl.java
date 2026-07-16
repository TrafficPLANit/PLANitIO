package org.goplanit.io.converter.network;

import net.opengis.gml.*;
import org.geotools.geometry.jts.JTS;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.io.converter.PlanitWriterImpl;
import org.goplanit.io.geo.PlanitGmlUtils;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;
import org.locationtech.jts.geom.*;

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

  /** Create a position type based on point location
   *
   * @param position to convert to GML and transform if needed
   * @return created GML pos
   */
  protected DirectPositionType createGmlDirectPositionType(Point position) {
    Coordinate positioncoordinate = createTransformedCoordinate(position.getCoordinate());
    return PlanitGmlUtils.createGmlDirectPositionType(positioncoordinate);
  }


  /** Create a GML coord type from the provided coordinate
   *
   * @param coordinate to convert to GML and transform if needed
   * @return created GML coordinate
   */
  protected CoordType createGmlCoordType(Coordinate coordinate) {
    Coordinate nodeCoordinate = createTransformedCoordinate(coordinate);
    return PlanitGmlUtils.createGmlCoordType(nodeCoordinate);
  }

  /** Create a GML PointType from a JTS Point and account for any crs transformation if needed
   *
   * @param position to extract from
   * @return created PointType
   */
  protected PointType createGmlPointType(Point position) {
    Coordinate pointCoordinate = createTransformedCoordinate(position.getCoordinate());
    return PlanitGmlUtils.createGmlPointType(pointCoordinate);
  }

  /** create a GML PolygonType from a JTS Polygon and account for any crs transformation if needed
   *
   * @param polygon to extract from
   * @return created PolygonType
   */
  protected PolygonType createGmlPolygonType(Polygon polygon) {
    try{
      var thePolygon = polygon;
      if(getDestinationCrsTransformer() != null) {
        thePolygon = (Polygon) JTS.transform(polygon, getDestinationCrsTransformer());
      }
      var result = PlanitGmlUtils.createGmlPolygonType(thePolygon.getCoordinates());
      return result;
    }catch (Exception e){
      LOGGER.severe("Unable to transform polygon"+ e.getMessage());
    }
    return null;
  }

  /** create a GML MultiPolygonType from a JTS MultiPolygon and account for any crs transformation if needed
   *
   * @param multiPolygon to extract from
   * @return created PolygonType
   */
  protected MultiPolygonType createGmlMultiPolygonType(MultiPolygon multiPolygon) {
    try{
      var thePolygon = multiPolygon;
      if(getDestinationCrsTransformer() != null) {
        thePolygon = (MultiPolygon) JTS.transform(multiPolygon, getDestinationCrsTransformer());
      }
      var result = PlanitGmlUtils.createGmlMultiPolygonType(thePolygon);
      return result;
    }catch (Exception e){
      LOGGER.severe("Unable to transform multipolygon"+ e.getMessage());
    }
    return null;
  }

  /** Create a GML LineStringType from a JTS LineString and account for any crs transformation if needed
   *
   * @param lineString to extract from
   * @return created LineStringType
   */
  protected LineStringType createGmlLineStringType(LineString lineString) {
    /* transformed coords */
    Coordinate[] transformedCoordinates = getTransformedCoordinates(lineString.getCoordinates());

    /* gml poslist*/
    PlanitXmlWriterSettings xmlSettings = null;
    var geometryPositions = PlanitGmlUtils.createGmlPosListType(transformedCoordinates);
    /* gml line string */
    return PlanitGmlUtils.createGmlLineStringType(geometryPositions);
  }

  /** Constructor
   *
   * @param idMapperType to use
   */
  protected UnTypedPlanitCrsWriterImpl(IdMapperType idMapperType) {
    super(idMapperType);
  }


}
