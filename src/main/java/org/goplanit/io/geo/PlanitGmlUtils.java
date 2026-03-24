package org.goplanit.io.geo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.opengis.gml.*;

import org.goplanit.utils.geo.PlanitJtsUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

/**
 * Utilities specific to GML
 * 
 * @author markr
 *
 */
public class PlanitGmlUtils {

  /**
   * Dummy constructor as never instantiated
   */
  private PlanitGmlUtils() {
    // compliance to avoid javadoc warnings
  }

  
  /**
   * Takes a list of JTS coordinates and converts it to GML coordinates value, i.e., converts all coordinates
   * to a string based on provided comma, decimal, tuple separators in 2D format. The result is a GML CoordinatesType
   * 
   * @param coordinates array of coordinates
   * @param commaSeparator to use
   * @param decimalSeparator to use
   * @param decimalFormat to use
   * @param tupleSeparator to use 
   * @return created GML coordinates type instance
   */
  @Deprecated (forRemoval = true, since = "replaced by pos list")
  public static CoordinatesType createGmlCoordinatesType(
      final Coordinate[] coordinates,
      final Character commaSeparator,
      final Character decimalSeparator,
      final DecimalFormat decimalFormat,
      final Character tupleSeparator) {

    /* coordinates value */
    String coordinateCsvValue = PlanitJtsUtils.createCsvStringFromCoordinates(
            coordinates, tupleSeparator, commaSeparator, decimalFormat);
    CoordinatesType xmlCoordinates = new CoordinatesType();
    xmlCoordinates.setValue(coordinateCsvValue);
    
    /* coordinates formatting */
    xmlCoordinates.setCs(commaSeparator.toString());
    xmlCoordinates.setTs(tupleSeparator.toString());
    xmlCoordinates.setDecimal(decimalSeparator.toString());
    
    return xmlCoordinates;
  }

  /**
   * Takes a list of JTS coordinates and converts it to GML coordinates value, i.e., converts all coordinates
   * to a string based on defined pos list format. The result is a GML PosListType
   *
   * @param coordinates array of coordinates
   * @return created GML coordinates type instance
   */
  public static DirectPositionListType createGmlPosListType(
          final Coordinate[] coordinates) {

    DirectPositionListType posList = new DirectPositionListType();

    List<Double> values = posList.getValue();
    for (Coordinate c : coordinates) {
      // control precision before double takes over as we no longer can run decimal format and do not want to
      // make it go through a string value
      values.add(BigDecimal.valueOf(c.x).setScale(8, RoundingMode.HALF_UP).doubleValue());
      values.add(BigDecimal.valueOf(c.y).setScale(8, RoundingMode.HALF_UP).doubleValue());
    }

    // for 2D better
    posList.setSrsDimension(BigInteger.valueOf(2));

    return posList;
  }
  
  /** Create a coordType instance based on provided JTS coordinate
   * 
   * @param coordinate to convert
   * @return coordType created 
   */
  public static CoordType createGmlCoordType(final Coordinate coordinate) {
    CoordType gmlCoordType = new CoordType();
        
    gmlCoordType.setX(BigDecimal.valueOf(coordinate.x));
    gmlCoordType.setY(BigDecimal.valueOf(coordinate.y));
    
    return gmlCoordType;
  }  
  
  /** Create a list of coordType instances based on provided JTS coordinates
   * 
   * @param coordinates to convert
   * @return coordtype list
   */
  public static List<CoordType> createGmlCoordList(final Coordinate[] coordinates) {
      
    /* coordinates */
    List<CoordType> coordList = new ArrayList<CoordType>(coordinates.length);    
    {
      for(int index=0;index<coordinates.length;++index) {
        /* coordinate */
        coordList.add(createGmlCoordType(coordinates[index]));
      }
    }      
    return coordList;
  }   
  
  /** Convert coordinate to DirectPositionType
   * 
   * @param coordinate to convert to GML direct position
   * @return created GML pos
   */
  public static DirectPositionType createGmlDirectPositionType(final Coordinate coordinate) {
    
    DirectPositionType gmlPos = new DirectPositionType();
    gmlPos.getValue().add(coordinate.x);
    gmlPos.getValue().add(coordinate.y);
    
    return gmlPos;
  }   
  
  /** Convert coordinate to PointType
   * 
   * @param coordinate to convert
   * @return created GML point type
   */
  public static PointType createGmlPointType(final Coordinate coordinate) {
    DirectPositionType gmlDirectPos = createGmlDirectPositionType(coordinate);
    PointType gmlPointType = new PointType();
    gmlPointType.setPos(gmlDirectPos);
    return gmlPointType;
  }   
  
  /** Takes a JTS line string and converts it to GML LineStringType.
   *   
   * @param lineString to extract from
   * @param commaSeparator to use
   * @param decimalSeparator to use
   * @param decimalFormat to use
   * @param tupleSeparator to use   
   * @return created GML LineStringType instance
   */
  public static LineStringType createGmlLineStringType(final LineString lineString, 
      final Character commaSeparator, final Character decimalSeparator, final DecimalFormat decimalFormat, final Character tupleSeparator) {  

    /* coordinates type */
    CoordinatesType coordinatesType = createGmlCoordinatesType(lineString.getCoordinates(), commaSeparator, decimalSeparator, decimalFormat, tupleSeparator);
    
    /* line string type */
    return createGmlLineStringType(coordinatesType);
  } 
  
  /**
   * Takes a JTS line string and converts it to GML LineStringType. Deprecated but keep for parsing of older
   * files so we remain compatible
   *   
  * @param coordsType to use
  * @return created GML LineStringType instance   
  */
  @Deprecated(forRemoval = false,since = "replaced by poslist")
  public static LineStringType createGmlLineStringType(final CoordinatesType coordsType) {  
    
    /* line string type */
    LineStringType xmlLineString = new LineStringType();
    xmlLineString.setCoordinates(coordsType);

    return xmlLineString;
  }

  /**
   * Takes a JTS line string and converts it to GML LineStringType.
   *
   * @param posType to use
   * @return created GML LineStringType instance
   */
  public static LineStringType createGmlLineStringType(final DirectPositionListType posType) {

    /* line string type */
    LineStringType xmlLineString = new LineStringType();
    xmlLineString.setPosList(posType);

    return xmlLineString;
  }
  
  /** Create a GML PolygonType from a JTS Polygon
   * 
   * @param outerBoundaryCoordinates of the JTS polygon, e.g. last coordinate is equal to first and at least three coordinates
   * @return created PolygonType
   */  
  public static PolygonType createGmlPolygonType(Coordinate[] outerBoundaryCoordinates) {
    ObjectFactory openGisObjectFactory = new ObjectFactory();
    PolygonType gmlPolygonType = new PolygonType();
    
    /* exterior */
    var xmlAbstractRingPropertyType =
        openGisObjectFactory.createOuterBoundaryIs(openGisObjectFactory.createAbstractRingPropertyType());
    gmlPolygonType.setExterior(xmlAbstractRingPropertyType);    

    /* linear ring */
    var xmlLinearRingType =
        openGisObjectFactory.createLinearRing(openGisObjectFactory.createLinearRingType());
    xmlAbstractRingPropertyType.getValue().setRing(xmlLinearRingType);
    
    /* coordinates */
    xmlLinearRingType.getValue().getCoord().addAll(createGmlCoordList(outerBoundaryCoordinates));          

    return gmlPolygonType;
  }  

 

}


