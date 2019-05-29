package org.planit.xml.constants;

import java.math.BigInteger;

/**
 * Default values used for the population of Java classes when a value is not set in the XML input file
 * 
 * @author gman6028
 *
 */
public interface Default {
	
	public static final double CONNECTOID_LENGTH = 1.0;
	
	public static final String USER_CLASS_NAME = "Default";
	public static final BigInteger USER_CLASS_ID = new BigInteger("1");
	public static final BigInteger USER_CLASS_MODE_REF = new BigInteger("1");
	public static final BigInteger USER_CLASS_TRAVELLER_TYPE = new BigInteger("1"); 
	
	public static final String TRAVELLER_TYPE_NAME = "Default";
	public static final BigInteger TRAVELLER_TYPE_ID = new BigInteger("1");
	public static final String SEPARATOR = ",";

}
