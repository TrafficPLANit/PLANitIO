package org.planit.xml.network;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.coordinate.Position;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.constants.Default;
import org.planit.exceptions.PlanItException;
import org.planit.generated.Direction;
import org.planit.generated.Infrastructure;
import org.planit.generated.LengthUnit;
import org.planit.generated.LinkLengthType;
import org.planit.generated.Links;
import org.planit.generated.Linksegment;
import org.planit.geo.PlanitGeoUtils;
import org.planit.network.physical.Link;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.Node;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentTypeModeProperties;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.xml.network.physical.macroscopic.MacroscopicLinkSegmentTypeXmlHelper;

import net.opengis.gml.LineStringType;
import net.opengis.gml.PointType;

/**
 * Process the Infrastructure object populated with data from the XML file
 * 
 * @author gman6028
 *
 */
public class ProcessInfrastructure {

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(ProcessInfrastructure.class.getName());
	private static PlanitGeoUtils planitGeoUtils;

	static {
		CoordinateReferenceSystem coordinateReferenceSystem = Default.COORDINATE_REFERENCE_SYSTEM;
		planitGeoUtils = new PlanitGeoUtils(coordinateReferenceSystem);
	}

	/**
	 * Register nodes on the network
	 * 
	 * @param infrastructure Infrastructure object populated with data from XML file
	 * @param network        network the physical network object to be populated
	 *                       from the input data
	 * @throws PlanItException thrown if there is an error in storing the GML Point
	 *                         definition
	 */
	public static void registerNodes(Infrastructure infrastructure, MacroscopicNetwork network) throws PlanItException {
		for (org.planit.generated.Nodes.Node generatedNode : infrastructure.getNodes().getNode()) {

			Node node = new Node();
			node.setExternalId(generatedNode.getId().longValue());
			PointType pointType = generatedNode.getPoint();
			if (pointType != null) {
				DirectPosition centrePointGeometry = getDirectPositionFromPointType(pointType);
				node.setCentrePointGeometry(centrePointGeometry);
			}
			network.nodes.registerNode(node);
		}
	}

	/**
	 * Generated and register link segments
	 * 
	 * @param infrastructure     Infrastructure object populated with data from XML
	 *                           file
	 * @param network            network the physical network object to be populated
	 *                           from the input data
	 * @param linkSegmentTypeMap Map of link segment types
	 * @throws PlanItException thrown if there is an error during processing
	 */
	public static void generateAndRegisterLinkSegments(Infrastructure infrastructure, MacroscopicNetwork network,
			Map<Integer, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentTypeMap) throws PlanItException {
		for (Links.Link generatedLink : infrastructure.getLinks().getLink()) {
			long startNodeId = generatedLink.getNodearef().longValue();
			Node startNode = network.nodes.findNodeByExternalIdentifier(startNodeId);
			long endNodeId = generatedLink.getNodebref().longValue();
			Node endNode = network.nodes.findNodeByExternalIdentifier(endNodeId);
			double length = Double.MIN_VALUE;
			length = getLengthFromLineString(length, generatedLink);
			length = getLengthFromLength(length, generatedLink);
			if (length == Double.MIN_VALUE) {
				throw new PlanItException(
						"Error in network XML file: Must define either a length or GML LineString for link from node "
								+ startNodeId + " to node " + endNodeId);
			}
			Link link = network.links.registerNewLink(startNode, endNode, length);
			for (Linksegment generatedLinkSegment : generatedLink.getLinksegment()) {
				int noLanes = (generatedLinkSegment.getNumberoflanes() == null) ? LinkSegment.DEFAULT_NUMBER_OF_LANES
						: generatedLinkSegment.getNumberoflanes().intValue();
				int linkType = generatedLinkSegment.getTyperef().intValue();
				MacroscopicLinkSegmentTypeXmlHelper linkSegmentType = linkSegmentTypeMap.get(linkType);
				// TODO - We should be able to set the maximum speed for individual link
				// segments in the network XML file. This is where we would update it. However
				// we would then need to set it for
				// every mode. We need to change the XSD file to specify how to do this.

				MacroscopicLinkSegmentTypeModeProperties modeProperties = linkSegmentType
						.getMacroscopicLinkSegmentTypeModeProperties();
				boolean abDirection = generatedLinkSegment.getDir().equals(Direction.A_B);
				generateAndRegisterLinkSegment(network, link, abDirection, linkSegmentType, noLanes, modeProperties);
			}
		}
	}

	/**
	 * Get the link length from the <length> element in the XML file, if this has
	 * been set
	 * 
	 * @param initLength    initial length value
	 * @param generatedLink object storing link data from XML file
	 * @return final length value
	 */
	private static double getLengthFromLength(double initLength, Links.Link generatedLink) {
		LinkLengthType linkLengthType = generatedLink.getLength();
		if (linkLengthType != null) {
			double length = linkLengthType.getValue();
			LengthUnit lengthUnit = linkLengthType.getUnit();
			if ((lengthUnit != null) && (lengthUnit.equals(LengthUnit.M))) {
				length /= 1000.0;
			}
			return length;
		}
		return initLength;
	}

	/**
	 * Get the link length from the <gml:LineString> element in the XML file, if
	 * this has been set
	 * 
	 * @param initLength    initial length value
	 * @param generatedLink object storing link data from XML file
	 * @return final length value
	 * @throws PlanItException
	 */
//TODO  - Create some test cases for this, currently no test cases exist for it
	private static double getLengthFromLineString(double initLength, Links.Link generatedLink) throws PlanItException {
		LineStringType lineStringType = generatedLink.getLineString();
		if (lineStringType != null) {
			List<Double> posList = lineStringType.getPosList().getValue();
			double distanceInMetres = 0.0;
			Position startPosition = null;
			Position endPosition = null;
			for (int i = 0; i < posList.size(); i += 2) {
				endPosition = planitGeoUtils.getDirectPositionFromValues(posList.get(i), posList.get(i + 1));
				if (startPosition != null) {
					distanceInMetres += planitGeoUtils.getDistanceInMetres(startPosition, endPosition);
				}
				startPosition = endPosition;
			}
			return distanceInMetres / 1000.0;
		}
		return initLength;
	}

	/**
	 * Create GML position from generated PointType object
	 * 
	 * @param pointType PointType object storing the location, read in from an XML
	 *                  input file
	 * @return DirectPosition object storing the location
	 * @throws PlanItException thrown if there is an error during processing
	 */
	private static DirectPosition getDirectPositionFromPointType(PointType pointType) throws PlanItException {
		List<Double> value = pointType.getPos().getValue();
		return planitGeoUtils.getDirectPositionFromValues(value.get(0), value.get(1));
	}

	/**
	 * Create DirectPosition object from X- and Y-coordinates
	 * 
	 * @param xCoordinate X-coordinate
	 * @param yCoordinate Y-coordinate
	 * @return DirectPosition object representing the location
	 * @throws PlanItException thrown if there is an error during processing
	 */
/*
	private static DirectPosition getDirectPositionFromValues(double xCoordinate, double yCoordinate)
			throws PlanItException {
		Coordinate coordinate = new Coordinate(xCoordinate, yCoordinate);
		Coordinate[] coordinates = { coordinate };
		List<Position> positions = planitGeoUtils.convertToDirectPositions(coordinates);
		return (DirectPosition) positions.get(0);
	}
*/
	/**
	 * Registers a new link segment in the physical network
	 * 
	 * @param network         the physical network object
	 * @param link            the link from which the link segment will be created
	 * @param abDirection     direction of travel
	 * @param linkSegmentType object storing the input values for this link
	 * @param noLanes         the number of lanes in this link
	 * @throws PlanItException thrown if there is an error
	 */
	private static void generateAndRegisterLinkSegment(MacroscopicNetwork network, Link link, boolean abDirection,
			MacroscopicLinkSegmentTypeXmlHelper linkSegmentType, int noLanes,
			MacroscopicLinkSegmentTypeModeProperties modeProperties) throws PlanItException {

		// create the link and store it in the network object
		MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) network.linkSegments
				.createDirectionalLinkSegment(link, abDirection);
		linkSegment.setMaximumSpeedMap(linkSegmentType.getSpeedMap());
		linkSegment.setNumberOfLanes(noLanes);
		MacroscopicLinkSegmentType macroscopicLinkSegmentType = network
				.registerNewLinkSegmentType(linkSegmentType.getName(), linkSegmentType.getCapacityPerLane(),
						linkSegmentType.getMaximumDensityPerLane(), linkSegmentType.getExternalId(), modeProperties)
				.getFirst();
		linkSegment.setLinkSegmentType(macroscopicLinkSegmentType);
		network.linkSegments.registerLinkSegment(link, linkSegment, abDirection);

	}

}
