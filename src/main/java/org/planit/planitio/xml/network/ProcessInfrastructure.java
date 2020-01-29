package org.planit.planitio.xml.network;

import java.util.List;
import java.util.Map;

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.coordinate.Position;
import org.planit.exceptions.PlanItException;
import org.planit.generated.Direction;
import org.planit.generated.LengthUnit;
import org.planit.generated.XMLElementInfrastructure;
import org.planit.generated.XMLElementLinkLengthType;
import org.planit.generated.XMLElementLinkSegment;
import org.planit.generated.XMLElementLinks;
import org.planit.generated.XMLElementNodes;
import org.planit.geo.PlanitGeoUtils;
import org.planit.network.physical.NodeImpl;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentImpl;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.planitio.xml.network.physical.macroscopic.MacroscopicLinkSegmentTypeXmlHelper;
import org.planit.planitio.xml.util.XmlUtils;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentTypeModeProperties;

import net.opengis.gml.LineStringType;
import net.opengis.gml.PointType;

/**
 * Process the Infrastructure object populated with data from the XML file
 * 
 * @author gman6028
 *
 */
public class ProcessInfrastructure {

	private static PlanitGeoUtils planitGeoUtils;

	static {
		planitGeoUtils = new PlanitGeoUtils();
	}

	/**
	 * Get the link length from the length element in the XML file, if this has
	 * been set
	 * 
	 * @param initLength    initial length value
	 * @param generatedLink object storing link data from XML file
	 * @return final length value
	 */
	private static double getLengthFromLength(double initLength, XMLElementLinks.Link generatedLink) {
		XMLElementLinkLengthType linkLengthType = generatedLink.getLength();
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
	 * Get the link length from the gml:LineString element in the XML file, if
	 * this has been set
	 * 
	 * @param initLength    initial length value
	 * @param generatedLink object storing link data from XML file
	 * @return final length value
	 * @throws PlanItException
	 */
//TODO  - Create some test cases for this, currently no test cases exist for it
	private static double getLengthFromLineString(double initLength, XMLElementLinks.Link generatedLink)
			throws PlanItException {
		LineStringType lineStringType = generatedLink.getLineString();
		if (lineStringType != null) {
			List<Double> posList = lineStringType.getPosList().getValue();
			double distance = 0.0;
			Position startPosition = null;
			Position endPosition = null;
			for (int i = 0; i < posList.size(); i += 2) {
				endPosition = planitGeoUtils.getDirectPositionFromValues(posList.get(i), posList.get(i + 1));
				if (startPosition != null) {
					distance += planitGeoUtils.getDistanceInKilometres(startPosition, endPosition);
				}
				startPosition = endPosition;
			}
			return distance;
		}
		return initLength;
	}

	/**
	 * Registers a new link segment in the physical network
	 * 
	 * @param network         the physical network object
	 * @param link            the link from which the link segment will be created
	 * @param abDirection     direction of travel
	 * @param linkSegmentType object storing the input values for this link
	 * @param noLanes         the number of lanes in this link
	 * @param externalId      the external Id of this link segment
	 * @throws PlanItException thrown if there is an error
	 */
	private static void generateAndRegisterLinkSegment(MacroscopicNetwork network, Link link, boolean abDirection,
			MacroscopicLinkSegmentTypeXmlHelper linkSegmentType, int noLanes, long externalId,
			MacroscopicLinkSegmentTypeModeProperties modeProperties) throws PlanItException {

		// create the link and store it in the network object
		MacroscopicLinkSegmentImpl linkSegment = (MacroscopicLinkSegmentImpl) network.linkSegments.createDirectionalLinkSegment(link, abDirection);
		linkSegment.setMaximumSpeedMap(linkSegmentType.getSpeedMap());
		linkSegment.setNumberOfLanes(noLanes);
		linkSegment.setExternalId(externalId);
		MacroscopicLinkSegmentType macroscopicLinkSegmentType = network
				.registerNewLinkSegmentType(linkSegmentType.getName(), linkSegmentType.getCapacityPerLane(),
						linkSegmentType.getMaximumDensityPerLane(), linkSegmentType.getExternalId(), modeProperties)
				.getFirst();
		linkSegment.setLinkSegmentType(macroscopicLinkSegmentType);
		network.linkSegments.registerLinkSegment(link, linkSegment, abDirection);

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
	public static void registerNodes(XMLElementInfrastructure infrastructure, MacroscopicNetwork network)
			throws PlanItException {
		for (XMLElementNodes.Node generatedNode : infrastructure.getNodes().getNode()) {

			NodeImpl node = (NodeImpl) network.nodes.registerNewNode();
			node.setExternalId(generatedNode.getId().longValue());
			PointType pointType = generatedNode.getPoint();
			if (pointType != null) {
				DirectPosition centrePointGeometry = XmlUtils.getDirectPositionFromPointType(planitGeoUtils, pointType);
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
	public static void generateAndRegisterLinkSegments(XMLElementInfrastructure infrastructure,
			MacroscopicNetwork network, Map<Integer, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentTypeMap)
			throws PlanItException {
		for (XMLElementLinks.Link generatedLink : infrastructure.getLinks().getLink()) {
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
			Link link = network.links.registerNewLink(startNode, endNode, length, "");
			for (XMLElementLinkSegment generatedLinkSegment : generatedLink.getLinksegment()) {
				int noLanes = (generatedLinkSegment.getNumberoflanes() == null) ? LinkSegment.DEFAULT_NUMBER_OF_LANES
						: generatedLinkSegment.getNumberoflanes().intValue();
				int linkType = generatedLinkSegment.getTyperef().intValue();
				long linkSegmentExternalId = generatedLinkSegment.getId().longValue();
				MacroscopicLinkSegmentTypeXmlHelper linkSegmentType = linkSegmentTypeMap.get(linkType);
				// TODO - We should be able to set the maximum speed for individual link
				// segments in the network XML file. This is where we would update it. However
				// we would then need to set it for
				// every mode. We need to change the XSD file to specify how to do this.

				MacroscopicLinkSegmentTypeModeProperties modeProperties = linkSegmentType
						.getMacroscopicLinkSegmentTypeModeProperties();
				boolean abDirection = generatedLinkSegment.getDir().equals(Direction.A_B);
				generateAndRegisterLinkSegment(network, link, abDirection, linkSegmentType, noLanes, linkSegmentExternalId, modeProperties);
			}
		}
	}

}
