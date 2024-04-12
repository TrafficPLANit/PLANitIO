package org.goplanit.io.converter.network;

import net.opengis.gml.*;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.io.converter.PlanitWriterImpl;
import org.goplanit.io.geo.PlanitGmlUtils;
import org.goplanit.io.xml.util.PlanitXmlWriterSettings;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

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
    xmlSettings = getSettingsAsXmlWriterSettings();
    CoordinatesType coordsType = PlanitGmlUtils.createGmlCoordinatesType(
        transformedCoordinates, xmlSettings.getCommaSeparator(), xmlSettings.getDecimalSeparator(), xmlSettings.getDecimalFormat(), xmlSettings.getTupleSeparator());

    /* gml line string */
    return PlanitGmlUtils.createGmlLineStringType(coordsType);
  }

  /** Constructor
   *
   * @param idMapperType to use
   */
  protected UnTypedPlanitCrsWriterImpl(IdMapperType idMapperType) {
    super(idMapperType);
  }


}
