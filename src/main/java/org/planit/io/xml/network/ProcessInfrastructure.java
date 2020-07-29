package org.planit.io.xml.network;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.coordinate.Position;
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
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.network.physical.Link;
import org.planit.utils.network.physical.LinkSegment;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.physical.Node;
import org.planit.utils.network.physical.macroscopic.MacroscopicLinkSegment;
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
  @SuppressWarnings("unused")
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
   * @param maxSpeed the value of the <maxspeed> element within the <linksegment> element in the
   *          input file, null if this element omitted for this link segment
   * @param network the physical network object
   * @param link the link from which the link segment will be created
   * @param abDirection direction of travel
   * @param linkSegmentTypeHelper object storing the input values for this link
   * @param noLanes the number of lanes in this link
   * @param externalId the external Id of this link segment
   * @param modeProperties properties of the link segment type for each mode
   * @param inputBuilderListener parser which holds the Map of nodes by external Id
   * @throws PlanItException thrown if there is an error
   */
  private static void createAndRegisterLinkSegment(Float maxSpeed, MacroscopicNetwork network, Link link,
      boolean abDirection,
      MacroscopicLinkSegmentTypeXmlHelper linkSegmentTypeHelper,
      int noLanes, long externalId,
      Map<Mode, MacroscopicModeProperties> modeProperties,
      InputBuilderListener inputBuilderListener) throws PlanItException {

    // create the link and store it in the network object
    MacroscopicLinkSegment linkSegment =
        (MacroscopicLinkSegment) network.linkSegments.createDirectionalLinkSegment(link, abDirection);

    double maxSpeedDouble = maxSpeed == null ? Double.POSITIVE_INFINITY : (double) maxSpeed;
    for (Mode mode : linkSegmentTypeHelper.getSpeedMap().keySet()) {
      linkSegment.setMaximumSpeed(mode, Math.min(maxSpeedDouble, linkSegmentTypeHelper.getSpeedMap().get(mode))); 
    }

    linkSegment.setNumberOfLanes(noLanes);
    linkSegment.setExternalId(externalId);
    MacroscopicLinkSegmentType existingLinkSegmentType = inputBuilderListener.getLinkSegmentTypeByExternalId(
        linkSegmentTypeHelper.getExternalId());
    if (existingLinkSegmentType == null) {
      MacroscopicLinkSegmentType macroscopicLinkSegmentType = network
          .createAndRegisterNewMacroscopicLinkSegmentType(linkSegmentTypeHelper.getName(), linkSegmentTypeHelper.getCapacityPerLane(),
              linkSegmentTypeHelper.getMaximumDensityPerLane(), linkSegmentTypeHelper.getExternalId(), modeProperties);
      inputBuilderListener.addLinkSegmentTypeToExternalIdMap(macroscopicLinkSegmentType.getExternalId(),
          macroscopicLinkSegmentType);
      linkSegment.setLinkSegmentType(macroscopicLinkSegmentType);
    } else {
      linkSegment.setLinkSegmentType(existingLinkSegmentType);
    }
    network.linkSegments.registerLinkSegment(link, linkSegment, abDirection);
    if (linkSegment.getExternalId() != null) {
      final boolean duplicateLinkSegmentExternalId = 
          inputBuilderListener.addLinkSegmentToExternalIdMap(linkSegment.getExternalId(), linkSegment);
      PlanItException.throwIf(duplicateLinkSegmentExternalId && inputBuilderListener.isErrorIfDuplicateExternalId(), 
          "Duplicate link segment external id " + linkSegment.getExternalId() + " found in network file");
    }
  }

  /**
   * Create and register nodes on the network
   * 
   * @param infrastructure Infrastructure object populated with data from XML file
   * @param network network the physical network object to be populated from the input data
   * @param inputBuilderListener parser which holds the Map of nodes by external Id
   * @throws PlanItException thrown if there is an error in storing the GML Point definition
   */
  public static void createAndRegisterNodes(XMLElementInfrastructure infrastructure, MacroscopicNetwork network,
      InputBuilderListener inputBuilderListener) throws PlanItException {
    for (XMLElementNodes.Node generatedNode : infrastructure.getNodes().getNode()) {

      Node node = network.nodes.registerNewNode();
      node.setExternalId(generatedNode.getId().longValue());
      PointType pointType = generatedNode.getPoint();
      if (pointType != null) {
        DirectPosition centrePointGeometry = XmlUtils.getDirectPositionFromPointType(planitGeoUtils, pointType);
        node.setCentrePointGeometry(centrePointGeometry);
      }
      boolean duplicateNodeExternalId = 
          inputBuilderListener.addNodeToExternalIdMap(generatedNode.getId().longValue(),node);
      PlanItException.throwIf(duplicateNodeExternalId && inputBuilderListener.isErrorIfDuplicateExternalId(),
          "Duplicate node external id " + generatedNode.getId().longValue() + " found in network file");
    }
  }

  /**
   * Generated and register link segments
   * 
   * @param infrastructure Infrastructure object populated with data from XML file
   * @param network network the physical network object to be populated from the input data
   * @param linkSegmentTypeHelperMap Map of MacroscopicLinkSegmentTypeXmlHelper objects
   * @param inputBuilderListener parser which holds the Map of nodes by external Id
   * @throws PlanItException thrown if there is an error during processing or reference to link segment types invalid
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
      boolean isFirstLinkSegment = true;
      boolean firstLinkDirection = true;
      for (XMLElementLinkSegment generatedLinkSegment : generatedLink.getLinksegment()) {
        long linkSegmentExternalId = generatedLinkSegment.getId().longValue();
        int noLanes = (generatedLinkSegment.getNumberoflanes() == null) ? LinkSegment.DEFAULT_NUMBER_OF_LANES
            : generatedLinkSegment.getNumberoflanes().intValue();
        long linkType = 0;
        if (generatedLinkSegment.getTyperef() == null) {
          if (linkSegmentTypeHelperMap.keySet().size() > 1) {
            String errorMessage = "Link Segment " + linkSegmentExternalId + " has no link segment defined, but there is more than one possible link segment type";
            throw new PlanItException(errorMessage);
          }
          for (long linkSegmentTypeExternalId : linkSegmentTypeHelperMap.keySet()) {
            linkType = linkSegmentTypeExternalId;
          }
        } else {
          linkType = generatedLinkSegment.getTyperef().longValue();
        }
        Float maxSpeed = generatedLinkSegment.getMaxspeed();
        MacroscopicLinkSegmentTypeXmlHelper macroscopicLinkSegmentTypeXmlHelper = linkSegmentTypeHelperMap.get(linkType);
        // TODO - We should be able to set the maximum speed for individual link
        // segments in the network XML file. This is where we would update it. However
        // we would then need to set it for
        // every mode. We need to change the XSD file to specify how to do this.

        Map<Mode, MacroscopicModeProperties> modeProperties = macroscopicLinkSegmentTypeXmlHelper
            .getModePropertiesMap();
        boolean abDirection = generatedLinkSegment.getDir().equals(Direction.A_B);
        if (!isFirstLinkSegment) {
          if (abDirection == firstLinkDirection) {
            String errorMessage =  "Both link segments for the same link are in the same direction.  Link segment external Id is " + linkSegmentExternalId;
            throw new PlanItException(errorMessage);
          }
        }
        createAndRegisterLinkSegment(maxSpeed, network, link, abDirection, macroscopicLinkSegmentTypeXmlHelper, noLanes,
            linkSegmentExternalId, modeProperties, inputBuilderListener);
        isFirstLinkSegment = false;
        firstLinkDirection = abDirection;
      }
    }
  }

}
