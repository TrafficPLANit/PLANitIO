package org.planit.xml.util;

import java.util.List;
import java.util.logging.Logger;

import org.opengis.geometry.DirectPosition;
import org.planit.exceptions.PlanItException;
import org.planit.geo.PlanitGeoUtils;

import net.opengis.gml.PointType;

public interface XmlUtils {

    public static final Logger LOGGER = Logger.getLogger(XmlUtils.class.getName());
    
	/**
	 * Create GML position from generated PointType object
	 * 
	 * @param pointType PointType object storing the location, read in from an XML
	 *                  input file
	 * @return DirectPosition object storing the location
	 * @throws PlanItException thrown if there is an error during processing
	 */
	public static DirectPosition getDirectPositionFromPointType(PlanitGeoUtils planitGeoUtils, PointType pointType) throws PlanItException {
		List<Double> value = pointType.getPos().getValue();
		return planitGeoUtils.getDirectPositionFromValues(value.get(0), value.get(1));
	}
}
