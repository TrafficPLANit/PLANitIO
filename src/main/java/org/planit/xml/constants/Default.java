package org.planit.xml.constants;

import java.math.BigInteger;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Default values used for the population of Java classes when a value is not set in the XML input file
 * 
 * @author gman6028
 *
 */
public class Default {
	
	public static final double CONNECTOID_LENGTH = 1.0;
	
	public static final String USER_CLASS_NAME = "Default";
	public static final BigInteger USER_CLASS_ID = new BigInteger("1");
	public static final BigInteger USER_CLASS_MODE_REF = new BigInteger("1");
	public static final BigInteger USER_CLASS_TRAVELLER_TYPE = new BigInteger("1"); 
	
	public static final String TRAVELLER_TYPE_NAME = "Default";
	public static final BigInteger TRAVELLER_TYPE_ID = new BigInteger("1");
	public static final String SEPARATOR = ",";
	
	public static CoordinateReferenceSystem COORDINATE_REFERENCE_SYSTEM;
	public static final float LANE_CAPACITY = 1800.0f;
	public static final float MAXIMUM_LANE_DENSITY = 180.0f;

    static {
    	COORDINATE_REFERENCE_SYSTEM = new DefaultGeographicCRS(DefaultGeographicCRS.WGS84);
    }
}
