package org.goplanit.io.geo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.graph.Vertex;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiLineString;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.PositionFactory;
import org.opengis.geometry.coordinate.GeometryFactory;
import org.opengis.geometry.coordinate.LineString;
import org.opengis.geometry.coordinate.PointArray;
import org.opengis.geometry.coordinate.Position;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * General geotools related utils. Uses geodetic distance when possible. In case the CRS is not based on an ellipsoid (2d plane) it will simply compute the distance between
 * coordinates using Pythagoras with the unit distance in meters, consistent with the {@code CartesianAuthorityFactory.GENERIC_2D}
 * 
 * It is assumed that x coordinate refers to latitude and y coordinate refers to longitude
 * 
 * @author markr
 *
 */
public class PlanitOpenGisUtils {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PlanitOpenGisUtils.class.getCanonicalName());

  /**
   * Default Coordinate Reference System: WGS84
   */
  public static final DefaultGeographicCRS DEFAULT_GEOGRAPHIC_CRS = PlanitJtsCrsUtils.DEFAULT_GEOGRAPHIC_CRS;

  /**
   * In absence of a geographic crs we can also use cartesian: GENERIC_2D
   */
  public static final CoordinateReferenceSystem CARTESIANCRS = PlanitJtsCrsUtils.CARTESIANCRS;

  /**
   * Geodetic calculator to construct distances between points. It is assumed the network CRS is geodetic in nature.
   */
  private GeodeticCalculator geodeticDistanceCalculator;
  
  /** geometry builder, holds crs ad well as factory methods to construct other factories*/
  private GeometryBuilder geometryBuilder;
  
  /** factory for geometries */
  private GeometryFactory geometryFactory;
  
  /** factory for positions */
  private PositionFactory positionFactory;

  /**
   * Constructor
   * 
   * Uses default coordinate reference system
   */
  public PlanitOpenGisUtils() {
    CoordinateReferenceSystem coordinateReferenceSystem = new DefaultGeographicCRS(DEFAULT_GEOGRAPHIC_CRS);
    geometryBuilder = new GeometryBuilder(coordinateReferenceSystem);
    geodeticDistanceCalculator = new GeodeticCalculator(coordinateReferenceSystem);
    geometryFactory = geometryBuilder.getGeometryFactory();
    positionFactory = geometryBuilder.getPositionFactory();
  }

  /**
   * Constructor
   * 
   * @param coordinateReferenceSystem OpenGIS CoordinateReferenceSystem object containing geometry
   */
  public PlanitOpenGisUtils(final CoordinateReferenceSystem coordinateReferenceSystem) {
    geometryBuilder = new GeometryBuilder(coordinateReferenceSystem);
    geometryFactory = geometryBuilder.getGeometryFactory();
    positionFactory = geometryBuilder.getPositionFactory();

    // geodetic only works on ellipsoids
    if (!coordinateReferenceSystem.equals(CARTESIANCRS)) {
      geodeticDistanceCalculator = new GeodeticCalculator(coordinateReferenceSystem);
    }
  }

  /**
   * Compute the distance in metres between two positions assuming the positions are provided in the same crs as registered on this class instance
   * 
   * @param startPosition location of the start point
   * @param endPosition   location of the end point
   * @return distance in metres between the points
   * @throws PlanItException thrown if there is an error
   */
  public double getDistanceInMetres(final Position startPosition, final Position endPosition) throws PlanItException {
    
    // not thread safe
    try {
      if (geodeticDistanceCalculator != null) {
        // ellipsoid crs
        geodeticDistanceCalculator.setStartingGeographicPoint(startPosition.getDirectPosition().getOrdinate(0), startPosition.getDirectPosition().getOrdinate(1));
        geodeticDistanceCalculator.setDestinationGeographicPoint(endPosition.getDirectPosition().getOrdinate(0), endPosition.getDirectPosition().getOrdinate(1));
        return geodeticDistanceCalculator.getOrthodromicDistance();
      } else {
        // cartesian in meters
        double deltaCoordinate0 = startPosition.getDirectPosition().getOrdinate(0) - endPosition.getDirectPosition().getOrdinate(0);
        double deltaCoordinate1 = startPosition.getDirectPosition().getOrdinate(1) - endPosition.getDirectPosition().getOrdinate(1);
        double distanceInMeters = Math.sqrt(Math.pow(deltaCoordinate0, 2) + Math.pow(deltaCoordinate1, 2));
        return distanceInMeters;
      }
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when computing distance in meters between two Positions in GeoUtils", e);
    }
  }

  /**
   * Compute the distance in kilometres between two positions assuming the positions are provided in the same crs as registered on this class instance
   * 
   * @param startPosition location of the start point
   * @param endPosition   location of the end point
   * @return distance in kilometres between the points
   * @throws PlanItException thrown if there is an error
   */
  public double getDistanceInKilometres(final Position startPosition, final Position endPosition) throws PlanItException {
    return getDistanceInMetres(startPosition, endPosition) / 1000.0;
  }

  /**
   * Compute the distance in kilometres between two vertices assuming the positions are set and based on the same crs as registered on this class instance
   * 
   * @param vertexA vertex with location
   * @param vertexB vertex with location
   * @return distance in kilometres between the points
   * @throws PlanItException thrown if there is an error
   */
  public double getDistanceInKilometres(final Vertex vertexA, final Vertex vertexB) throws PlanItException {
    DirectPosition positionA = JTS.toDirectPosition(vertexA.getPosition().getCoordinate(), geometryBuilder.getCoordinateReferenceSystem());
    DirectPosition positionB = JTS.toDirectPosition(vertexB.getPosition().getCoordinate(), geometryBuilder.getCoordinateReferenceSystem());
    return getDistanceInMetres(positionA, positionB) / 1000.0;
  }

  /**
   * Create DirectPosition object from X- and Y-coordinates
   * 
   * @param xCoordinate X-coordinate (longitude assumed)
   * @param yCoordinate Y-coordinate (latitude assumed)
   * @return DirectPosition object representing the location
   * @throws PlanItException thrown if there is an error during processing
   */
  public DirectPosition createDirectPosition(double xCoordinate, double yCoordinate) throws PlanItException {
    Coordinate coordinate = new Coordinate(xCoordinate, yCoordinate);
    DirectPosition newPosition = positionFactory.createDirectPosition(new double[] { coordinate.x, coordinate.y });
    return newPosition;
  }

  /**
   * Convert a JTS line string object to an OpenGis LineString instance by transferring the internal coordinates
   * 
   * @param jtsLineString JTS line string input
   * @return LineString GeoTools line string output object
   * @throws PlanItException thrown if there is an error
   */
  @SuppressWarnings("unchecked")
  public LineString convertToOpenGisLineString(final org.locationtech.jts.geom.LineString jtsLineString) throws PlanItException {
    Coordinate[] coordinates = jtsLineString.getCoordinates();
    List<? extends Position> positionList = (List<? extends Position>) convertToDirectPositions(coordinates);
    return geometryFactory.createLineString((List<Position>) positionList);
  }

  /**
   * Converts a JTS MultiLineString with a single entry into an OpenGIS LineString instance
   * 
   * @param jtsMultiLineString JTS MultiLineString input object
   * @return LineString GeoTools MultiLineString output object
   * @throws PlanItException thrown if there is an error
   */
  public LineString convertToOpenGisLineString(final MultiLineString jtsMultiLineString) throws PlanItException {
    PlanItException.throwIf(((MultiLineString) jtsMultiLineString).getNumGeometries() > 1, "MultiLineString contains multiple LineStrings");

    return convertToOpenGisLineString((org.locationtech.jts.geom.LineString) ((MultiLineString) jtsMultiLineString).getGeometryN(0));
  }

  /**
   * Create a line string from the doubles passed in (list of doubles containing x1,y1,x2,y2,etc. coordinates
   * 
   * @param coordinateList source
   * @return created line string
   * @throws PlanItException thrown if error
   */
  public LineString createLineString(final List<Double> coordinateList) throws PlanItException {
    PlanItException.throwIf(coordinateList.size() % 2 != 0, "coordinate list must contain an even number of entries to correctly identify (x,y) pairs");
    Iterator<Double> iter = coordinateList.iterator();
    List<Position> positionList = new ArrayList<Position>(coordinateList.size() / 2);
    while (iter.hasNext()) {
      positionList.add(createDirectPosition(iter.next(), iter.next()));
    }
    return geometryFactory.createLineString(positionList);
  }

  /**
   * Based on the CSV string construct a line string
   * 
   * @param value the values containing the x,y coordinates in the crs of this instance
   * @param ts    tuple separating character
   * @param cs    comma separating character
   * @return the LineString created from the String
   * @throws PlanItException thrown if error
   */
  public LineString createLineString(final String value, char ts, char cs) throws PlanItException {
    List<Double> coordinateDoubleList = new ArrayList<Double>();
    String[] tupleString = value.split("[" + ts + "]");
    for (int index = 0; index < tupleString.length; ++index) {
      String xyCoordinateString = tupleString[index];
      String[] coordinateString = xyCoordinateString.split("[" + cs + "]");
      if (coordinateString.length != 2) {
        throw new PlanItException(String.format("invalid coordinate encountered, expected two coordinates in tuple, but found %d", coordinateString.length));
      }
      coordinateDoubleList.add(Double.parseDouble(coordinateString[0]));
      coordinateDoubleList.add(Double.parseDouble(coordinateString[1]));
    }
    return createLineString(coordinateDoubleList);
  }

  /**
   * Create a line string from the passed in positions
   * 
   * @param positionList source
   * @return created line string
   * @throws PlanItException thrown if error
   */
  public LineString createLineStringFromPositions(final List<Position> positionList) throws PlanItException {
    return geometryFactory.createLineString(positionList);
  }

  /**
   * Based on the CSV string construct a line string
   * 
   * @param value the values containing the x,y coordinates in the crs of this instance
   * @param ts    tuple separating string (which must be a a character)
   * @param cs    comma separating string (which must be a a character)
   * @return the LineString created from the String
   * @throws PlanItException thrown if error
   */
  public LineString createLineStringFromCsvString(final String value, String ts, String cs) throws PlanItException {
    if (ts.length() > 1 || cs.length() > 1) {
      PlanItException.throwIf(ts.length() > 1, String.format("tuple separating string to create LineString is not a single character but %s", ts));
      PlanItException.throwIf(cs.length() > 1, String.format("comma separating string to create LineString is not a single character but %s", cs));
    }
    return createLineString(value, ts.charAt(0), cs.charAt(0));
  }

  /**
   * Convert JTS coordinates to OpenGIS directPositions
   * 
   * @param coordinates array of JTS Coordinate objects
   * @return List of GeoTools Position objects
   * @throws PlanItException thrown if there is an error
   */
  public List<DirectPosition> convertToDirectPositions(final Coordinate[] coordinates) throws PlanItException {
    List<DirectPosition> positionList = new ArrayList<DirectPosition>(coordinates.length);
    for (Coordinate coordinate : coordinates) {
      positionList.add(createDirectPosition(coordinate.x, coordinate.y));
    }
    return positionList;
  }

  /**
   * Compute the length of the line string by traversing all nodes and computing the segment by segment distances TODO: find out if a faster way is possible
   * 
   * @param geometry to extract length from
   * @return length in km
   * @throws PlanItException thrown if error
   */
  public double getDistanceInKilometres(final LineString geometry) throws PlanItException {

    PointArray pointArray = geometry.getControlPoints();
    int numberOfPoints = pointArray.size();

    if (numberOfPoints > 1) {

      double computedLengthInKm = 0;
      Position previousPoint = pointArray.get(0);
      for (int index = 1; index < numberOfPoints; ++index) {
        Position currentPoint = pointArray.get(index);
        computedLengthInKm += getDistanceInKilometres(previousPoint, currentPoint);
        previousPoint = currentPoint;
      }

      return computedLengthInKm;
    }
    throw new PlanItException("unable to compute distance for less than two points");
  }

  /**
   * Find the closest explicit sample point registered on the line string compared to the passed in position
   * 
   * @param toMatch    position to get closest to
   * @param lineString to sample ordinates from to check
   * @return closest ordinate (position) on line string to passed in toMatch position
   * @throws PlanItException thrown if error
   */
  public Position getClosestSamplePointOnLineString(final Position toMatch, final LineString lineString) throws PlanItException {
    if (lineString != null && toMatch != null) {
      double minDistance = Double.POSITIVE_INFINITY;
      Position minDistancePosition = null;
      for (Position samplePoint : lineString.getSamplePoints()) {
        double currDistance = getDistanceInMetres(toMatch, samplePoint);
        if (getDistanceInMetres(toMatch, samplePoint) < minDistance) {
          minDistance = currDistance;
          minDistancePosition = samplePoint;
        }
      }

      return minDistancePosition;
    }
    throw new PlanItException(" closest orindate position to lines tring could not be computed since either the line string or reference position is null");
  }

}
