package org.planit.xml.zoning;

import java.util.List;
import java.util.logging.Logger;

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.coordinate.Position;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.exceptions.PlanItException;
import org.planit.generated.Connectoid;
import org.planit.generated.Zones.Zone;
import org.planit.geo.PlanitGeoUtils;
import org.planit.network.physical.Node;
import org.planit.network.physical.PhysicalNetwork.Nodes;
import org.planit.network.virtual.Centroid;
import org.planit.constants.Default;
import org.planit.zoning.Zoning;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * This class contains methods to update the Zoning object using input values from the XML zoning input file.
 * 
 * @author gman6028
 *
 */
public class UpdateZoning {
	
    private static final Logger LOGGER = Logger.getLogger(UpdateZoning.class.getName());
    
	private static PlanitGeoUtils planitGeoUtils;
	
	static {
		CoordinateReferenceSystem coordinateReferenceSystem = Default.COORDINATE_REFERENCE_SYSTEM;
     	planitGeoUtils = new PlanitGeoUtils(coordinateReferenceSystem);
     }
	
/**
 * Generates an OpenGIS DirectPosition object for the Centroid location from the generated Zone object
 * 
 * @param zone					   current generated Zone object
 * @return                                DirectPosition object containing the location of the centroid
 * @throws PlanItException     thrown if there is an error during processing
 */
	private static DirectPosition getCentrePointGeometry(Zone zone) throws PlanItException {
        List<Double> value = zone.getCentroid().getPoint().getPos().getValue();
        Coordinate coordinate = new Coordinate(value.get(0), value.get(1));
		Coordinate[] coordinates = {coordinate};
		List<Position> positions =  planitGeoUtils.convertToDirectPositions(coordinates);
		return (DirectPosition) positions.get(0);
	}
	
/**
 * Generates and registers Zone and Centroid objects from the generated Zone object
 * 
 * @param zoning						Zoning object to be updated
 * @param zone                        current generated Zone object
 * @return                                 registered Centroid object
 * @throws PlanItException      thrown if there is an error during processing
 */
	public static Centroid createAndRegisterZoneAndCentroid(Zoning zoning, Zone zone) throws PlanItException {
		DirectPosition centrePointGeometry = getCentrePointGeometry(zone);		
        long zoneExternalId = zone.getId().longValue();
        Centroid centroid = zoning.zones.createAndRegisterNewZone(zoneExternalId).getCentroid();
        centroid.setCentrePointGeometry(centrePointGeometry);
        return centroid;
	}
	
/**
 * Generates and registers a Connectoid object from the current Centroid and generated Zone object
 * 
 * @param zoning						Zoning object to be updated
 * @param zone                        current generated Zone object
 * @param zone					    current generated Zone object
 * @param centroid                  current Centroid object
 * @throws PlanItException      thrown if there is an error during processing
 */
	public static void registerNewConnectoid(Zoning zoning, Nodes nodes, Zone zone, Centroid centroid) throws PlanItException {
        Connectoid connectoid = zone.getConnectoids().getConnectoid().get(0);
        long nodeExternalId = connectoid.getNoderef().longValue();
        Node node = nodes.findNodeByExternalIdentifier(nodeExternalId);
		double connectoidLength = (connectoid.getLength() == null) ? org.planit.network.virtual.Connectoid.DEFAULT_LENGTH: connectoid.getLength();
        zoning.getVirtualNetwork().connectoids.registerNewConnectoid(centroid, node, connectoidLength);
	}

}
