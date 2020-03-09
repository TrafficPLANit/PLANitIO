package org.planit.planitio.xml.zoning;

import java.math.BigInteger;
import java.util.List;

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.coordinate.Position;
import org.planit.exceptions.PlanItException;
import org.planit.generated.XMLElementCentroid;
import org.planit.generated.XMLElementConnectoid;
import org.planit.generated.XMLElementZones.Zone;
import org.planit.geo.PlanitGeoUtils;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork.Nodes;
import org.planit.network.virtual.Zoning;
import org.planit.planitio.xml.util.XmlUtils;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.virtual.Centroid;

import com.vividsolutions.jts.geom.Coordinate;

import net.opengis.gml.PointType;

/**
 * This class contains methods to update the Zoning object using input values from the XML zoning
 * input file.
 * 
 * @author gman6028
 *
 */
public class UpdateZoning {

  private static PlanitGeoUtils planitGeoUtils;

  static {
    planitGeoUtils = new PlanitGeoUtils();
  }

  /**
   * Generates an OpenGIS DirectPosition object for the Centroid location from the generated Zone
   * object
   * 
   * @param zone current generated Zone object
   * @return DirectPosition object containing the location of the centroid
   * @throws PlanItException thrown if there is an error during processing
   */
  public static DirectPosition getCentrePointGeometry(Zone zone) throws PlanItException {
    List<Double> value = zone.getCentroid().getPoint().getPos().getValue();
    Coordinate coordinate = new Coordinate(value.get(0), value.get(1));
    Coordinate[] coordinates = {coordinate};
    List<Position> positions = planitGeoUtils.convertToDirectPositions(coordinates);
    return (DirectPosition) positions.get(0);
  }

  /**
   * Generates and registers a Connectoid object from the current Centroid and generated Zone object
   * 
   * @param zoning Zoning object to be updated
   * @param nodes current Nodes object
   * @param zone current generated Zone object (from XML input file)
   * @param centroid current Centroid object
   * @param inputBuilderListener parser which contains nodes by node external Id
   * @throws PlanItException thrown if there is an error during processing
   */
  public static void registerNewConnectoid(Zoning zoning, Nodes nodes, Zone zone, Centroid centroid, InputBuilderListener inputBuilderListener) throws PlanItException {
    XMLElementConnectoid connectoid = zone.getConnectoids().getConnectoid().get(0);
    long nodeExternalId = connectoid.getNoderef().longValue();
    Node node = inputBuilderListener.getNodeByExternalId(nodeExternalId);
    DirectPosition nodePosition = node.getCentrePointGeometry();
    BigInteger externalId = connectoid.getId();
    double connectoidLength;
    if (connectoid.getLength() != null) {
      connectoidLength = connectoid.getLength();
      // :TODO - need to create some test cases in which nodes have a GML location
    } else if (nodePosition != null) {
      // if node has a GML Point, get the GML Point from the centroid and calculate the length
      // between them
      XMLElementCentroid generatedCentroid = zone.getCentroid();
      PointType pointType = generatedCentroid.getPoint();
      DirectPosition centroidPosition = XmlUtils.getDirectPositionFromPointType(planitGeoUtils, pointType);
      connectoidLength = planitGeoUtils.getDistanceInKilometres(centroidPosition, nodePosition);
    } else {
      connectoidLength = org.planit.utils.network.virtual.Connectoid.DEFAULT_LENGTH_KM;
    }

    if (externalId != null) {
      zoning.getVirtualNetwork().connectoids.registerNewConnectoid(centroid, node, connectoidLength, externalId
          .longValue());
    } else {
      zoning.getVirtualNetwork().connectoids.registerNewConnectoid(centroid, node, connectoidLength);
    }

  }

}