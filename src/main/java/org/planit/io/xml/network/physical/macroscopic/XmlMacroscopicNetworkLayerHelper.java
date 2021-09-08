package org.planit.io.xml.network.physical.macroscopic;

import java.util.List;
import java.util.logging.Logger;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsCrsUtils;
import org.planit.utils.geo.PlanitJtsUtils;
import org.planit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.planit.xml.generated.LengthUnit;
import org.planit.xml.generated.XMLElementLayerConfiguration;
import org.planit.xml.generated.XMLElementLinkLengthType;
import org.planit.xml.generated.XMLElementLinkSegmentType;
import org.planit.xml.generated.XMLElementLinkSegmentTypes;
import org.planit.xml.generated.XMLElementLinks;
import net.opengis.gml.DirectPositionListType;
import net.opengis.gml.LineStringType;


/**
 * Process the Infrastructure object populated with data from the XML file
 * 
 * @author gman6028, markr
 *
 */
public class XmlMacroscopicNetworkLayerHelper {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(XmlMacroscopicNetworkLayerHelper.class.getCanonicalName());         
  
  /* PROTECTED */ 
  
  /**
   * Get the link length from the length element in the XML file, if this has
   * been set
   * 
   * @param generatedLink object storing link data from XML file
   * @return final length value
   */
  public static Double parseLengthElementFromLink(XMLElementLinks.Link generatedLink) {
    Double length = null;
    XMLElementLinkLengthType linkLengthType = generatedLink.getLength();
    if (linkLengthType != null) {
      LengthUnit lengthUnit = linkLengthType.getUnit();
      if ((lengthUnit != null) && (lengthUnit.equals(LengthUnit.M))) {
        length = linkLengthType.getValue()/1000.0;
      }else {
        length = linkLengthType.getValue();
      }
    }
    return length;    
  }

  /**
   * Get the link length from the gml:LineString element in the XML file, if
   * this has been set
   * 
   * @param generatedLink object storing link data from XML file
   * @param jtsUtils to compute length from geometry
   * @return final length value
   * @throws PlanItException thown if error
   */
  public static Double parseLengthFromLineString(XMLElementLinks.Link generatedLink, PlanitJtsCrsUtils jtsUtils) throws PlanItException {
    Double length = 0.0;
    
    LineStringType lineStringType = generatedLink.getLineString();
    if (lineStringType != null) {
      DirectPositionListType positionList = lineStringType.getPosList();
      if(positionList==null) {
        LOGGER.severe(
            String.format("Link %s has a line string without any positions, this should not happen, consider specifying a length instead, setting length to 0.0", generatedLink.getId()));
        return length;
      }
      
      List<Double> posList = lineStringType.getPosList().getValue();
      Point startPosition = null;
      Point endPosition = null;
      for (int i = 0; i < posList.size(); i += 2) {
        endPosition = PlanitJtsUtils.createPoint(posList.get(i), posList.get(i + 1));
        if (startPosition != null) {
          length += jtsUtils.getDistanceInKilometres(startPosition, endPosition);
        }
        startPosition = endPosition;
      }
    }
    return length;    
  }
  
  /**
   * parse the geometry from the xml link
   * 
   * @param generatedLink xml link
   * @return created LineString if any, null if not present
   * @throws PlanItException thrown if error
   */
  public static LineString parseLinkGeometry(org.planit.xml.generated.XMLElementLinks.Link generatedLink) throws PlanItException {
    /* geometry of link */
    if(generatedLink.getLineString()!=null) {
      LineStringType lst = generatedLink.getLineString();
      if(lst.getCoordinates() != null) {
        return PlanitJtsUtils.createLineStringFromCsvString(lst.getCoordinates().getValue(), lst.getCoordinates().getTs(), lst.getCoordinates().getCs());
      }else if(lst.getPosList()!=null) {
        return PlanitJtsUtils.createLineString(lst.getPosList().getValue());
      }
    }
    return null;    
  }  
  
  /** parse the length of an xmlLink based on geometry or length attribute
   * 
   * @param xmlLink to extract length from
   * @param theLineString to extract length from (if not null) when no explicit length is set
   * @param jtsUtils to compute length from geometry
   * @return length (in km)
   * @throws PlanItException thrown if error
   */
  public static double parseLength(org.planit.xml.generated.XMLElementLinks.Link xmlLink, LineString theLineString, PlanitJtsCrsUtils jtsUtils) throws PlanItException {
    Double length = parseLengthElementFromLink(xmlLink);
    if(length == null && theLineString!=null) {
      /* not explicitly set, try extracting it from geometry  instead */
      length = jtsUtils.getDistanceInKilometres(theLineString);
    }
    
    if (length == null) {
      LOGGER.severe(String.format(
          "Must define either a length or GML LineString for link %s, setting length to 0.0 instead", xmlLink.getId()));
      length = 0.0;
    }  
    
    return length;
  }    
    
  /**
   * in case no link segment types are defined on the layer, we inject a default link segment type
   *
   * @param xmlLayerConfiguration to inject xml entry into
   */
  public static void injectDefaultLinkSegmentType(XMLElementLayerConfiguration xmlLayerConfiguration) {
    if (xmlLayerConfiguration.getLinksegmenttypes() == null) {
      /* crete entry */
      xmlLayerConfiguration.setLinksegmenttypes(new XMLElementLinkSegmentTypes());
      /* create default type */
      XMLElementLinkSegmentType xmlLinkSegmentType = new XMLElementLinkSegmentType();
      xmlLinkSegmentType.setName("");
      xmlLinkSegmentType.setId(MacroscopicLinkSegmentType.DEFAULT_XML_ID);
      xmlLayerConfiguration.getLinksegmenttypes().getLinksegmenttype().add(xmlLinkSegmentType);
    }
  }
    
  

}
