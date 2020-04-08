package org.planit.io.xml.network;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
import org.planit.input.InputBuilderListener;
import org.planit.io.xml.network.physical.macroscopic.MacroscopicLinkSegmentTypeXmlHelper;
import org.planit.io.xml.util.XmlUtils;
import org.planit.network.physical.NodeImpl;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentImpl;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.misc.Pair;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.utils.network.physical.macroscopic.MacroscopicModeProperties;
import net.opengis.gml.LineStringType;
import net.opengis.gml.PointType;

/**
 * Process the Infrastructure object populated with data from the XML file
 * 
 * @author gman6028
 *
 */
public class ProcessInfrastructure {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ProcessInfrastructure.class.getCanonicalName());   

  private static PlanitGeoUtils planitGeoUtils;

  static {
    planitGeoUtils = new PlanitGeoUtils();
  }

  /**
   * Get the link length from the length element in the XML file, if this has
   * been set
   * 
   * @param initLength initial length value
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
   * @param initLength initial length value
   * @param generatedLink object storing link data from XML file
   * @return final length value
   * @throws PlanItException
   */
  // TODO - Create some test cases for this, currently no test cases exist for it
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
   * @param network the physical network object
   * @param link the link from which the link segment will be created
   * @param abDirection direction of travel
   * @param linkSegmentType object storing the input values for this link
   * @param noLanes the number of lanes in this link
   * @param externalId the external Id of this link segment
   * @param modeProperties properties of the link segment type for each mode
   * @param inputBuilderListeners parser which holds the Map of nodes by external Id
   * @throws PlanItException thrown if there is an error
   */
  private static void createAndRegisterLinkSegment(MacroscopicNetwork network, Link link, boolean abDirection,
      MacroscopicLinkSegmentTypeXmlHelper linkSegmentType, 
      int noLanes, long externalId,
      Map<Mode, MacroscopicModeProperties> modeProperties,
      InputBuilderListener inputBuilderListener) throws PlanItException {

    // create the link and store it in the network object
    MacroscopicLinkSegmentImpl linkSegment = 
        (MacroscopicLinkSegmentImpl) network.linkSegments.createDirectionalLinkSegment(link, abDirection);
    linkSegment.setMaximumSpeedMap(linkSegmentType.getSpeedMap());
    linkSegment.setNumberOfLanes(noLanes);
    linkSegment.setExternalId(externalId);
    Pair<MacroscopicLinkSegmentType, Boolean> linkSegmentTypePair = network
        .registerNewLinkSegmentType(linkSegmentType.getName(), linkSegmentType.getCapacityPerLane(),
            linkSegmentType.getMaximumDensityPerLane(), linkSegmentType.getExternalId(), modeProperties);
    MacroscopicLinkSegmentType macroscopicLinkSegmentType = linkSegmentTypePair.getFirst();

    boolean linkSegmentTypeAlreadyExists = linkSegmentTypePair.getSecond();
    if (!linkSegmentTypeAlreadyExists) {
      inputBuilderListener.addLinkSegmentTypeToExternalIdMap(macroscopicLinkSegmentType.getExternalId(), macroscopicLinkSegmentType);
    }   

    linkSegment.setLinkSegmentType(macroscopicLinkSegmentType);
    network.linkSegments.registerLinkSegment(link, linkSegment, abDirection);
    if (linkSegment.getExternalId() != null) {
      final boolean duplicateLinkSegmentExternalId = inputBuilderListener.addLinkSegmentToExternalIdMap(linkSegment.getExternalId(), linkSegment);
      if (duplicateLinkSegmentExternalId && inputBuilderListener.isErrorIfDuplicateExternalId()) {
        String errorMessage = "Duplicate link segment external id " + linkSegment.getExternalId() + " found in network file.";
        LOGGER.severe(errorMessage);
        throw new PlanItException(errorMessage);
      }
    }
  }
  
  /**
   * Create and register nodes on the network
   * 
   * @param infrastructure Infrastructure object populated with data from XML file
   * @param network network the physical network object to be populated from the input data
   * @param inputBuilderListeners parser which holds the Map of nodes by external Id
   * @throws PlanItException thrown if there is an error in storing the GML Point  definition
   */
  public static void createAndRegisterNodes(XMLElementInfrastructure infrastructure, MacroscopicNetwork network, InputBuilderListener inputBuilderListener) throws PlanItException {
    for (XMLElementNodes.Node generatedNode : infrastructure.getNodes().getNode()) {

      NodeImpl node = (NodeImpl) network.nodes.registerNewNode();
      node.setExternalId(generatedNode.getId().longValue());
      PointType pointType = generatedNode.getPoint();
      if (pointType != null) {
        DirectPosition centrePointGeometry = XmlUtils.getDirectPositionFromPointType(planitGeoUtils, pointType);
        node.setCentrePointGeometry(centrePointGeometry);
      }
      network.nodes.registerNode(node);
      boolean duplicateNodeExternalId = inputBuilderListener.addNodeToExternalIdMap(generatedNode.getId().longValue(), node);
      if (duplicateNodeExternalId && inputBuilderListener.isErrorIfDuplicateExternalId()) {
        String errorMessage = "Duplicate node external id " + generatedNode.getId().longValue() + " found in network file.";
        LOGGER.severe(errorMessage);
        throw new PlanItException(errorMessage);
      }
    }
  }

  /**
   * Generated and register link segments
   * 
   * @param infrastructure Infrastructure object populated with data from XML file
   * @param network network the physical network object to be populated from the input data
   * @param linkSegmentTypeHelperMap Map of MacroscopicLinkSegmentTypeXmlHelper objects
   * @param inputBuilderListeners parser which holds the Map of nodes by external Id
   * @throws PlanItException thrown if there is an error during processing
   */
  public static void createAndRegisterLinkSegments(XMLElementInfrastructure infrastructure,
      MacroscopicNetwork network, 
      Map<Long, MacroscopicLinkSegmentTypeXmlHelper> linkSegmentTypeHelperMap,
      InputBuilderListener inputBuilderListener) throws PlanItException {
      
    for (XMLElementLinks.Link generatedLink : infrastructure.getLinks().getLink()) {
      Node startNode = inputBuilderListener.getNodeByExternalId(generatedLink.getNodearef().longValue());
      Node endNode = inputBuilderListener.getNodeByExternalId(generatedLink.getNodebref().longValue());
      double length = Double.MIN_VALUE;
      length = getLengthFromLineString(length, generatedLink);
      length = getLengthFromLength(length, generatedLink);
      if (length == Double.MIN_VALUE) {
        throw new PlanItException(
            "Error in network XML file: Must define either a length or GML LineString for link from node "
                + generatedLink.getNodearef().longValue() + " to node " + generatedLink.getNodebref().longValue());
      }
      Link link = network.links.registerNewLink(startNode, endNode, length, "");
      for (XMLElementLinkSegment generatedLinkSegment : generatedLink.getLinksegment()) {
        int noLanes = (generatedLinkSegment.getNumberoflanes() == null) ? LinkSegment.DEFAULT_NUMBER_OF_LANES
            : generatedLinkSegment.getNumberoflanes().intValue();
        long linkType = generatedLinkSegment.getTyperef().longValue();
        long linkSegmentExternalId = generatedLinkSegment.getId().longValue();
        MacroscopicLinkSegmentTypeXmlHelper macroscopicLinkSegmentTypeXmlHelper = linkSegmentTypeHelperMap.get(linkType);
        // TODO - We should be able to set the maximum speed for individual link
        // segments in the network XML file. This is where we would update it. However
        // we would then need to set it for
        // every mode. We need to change the XSD file to specify how to do this.

        Map<Mode, MacroscopicModeProperties> modeProperties = macroscopicLinkSegmentTypeXmlHelper.getModePropertiesMap();
        boolean abDirection = generatedLinkSegment.getDir().equals(Direction.A_B);
        createAndRegisterLinkSegment(network, link, abDirection, macroscopicLinkSegmentTypeXmlHelper, noLanes, linkSegmentExternalId, modeProperties, inputBuilderListener);
      }
    }
  }

}