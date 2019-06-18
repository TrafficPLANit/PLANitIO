package org.planit.output.xml;

import org.planit.generated.Datatypedescription;

public enum Column {
    LINK_ID, 
    MODE_ID, 
    SPEED, 
    DENSITY,
    FLOW,
    TRAVEL_TIME;
	
	public static String getName(Column column) {
		String outString = null;
		switch (column) {
			case LINK_ID: outString = "link id";
            break;
			case MODE_ID: outString = "mode id";
			break;
			case SPEED: outString = "speed";
			break;
			case DENSITY: outString = "density";
			break;
			case FLOW: outString = "flow";
			break;
			case TRAVEL_TIME: outString = "travel time";
			break;
		}
		return outString;
	}
	
	public static String getUnits(Column column) {
		String outString = null;
		switch (column) {
			case LINK_ID: outString = "none";
            break;
			case MODE_ID: outString = "none";
			break;
			case SPEED: outString = "km/h";
			break;
			case DENSITY: outString = "veh/km";
			break;
			case FLOW: outString = "veh/h";
			break;
			case TRAVEL_TIME: outString = "hr";
			break;
		}
		return outString;
	}


	public static Datatypedescription getType(Column column) {
		Datatypedescription outString = null;
		switch (column) {
			case LINK_ID: outString = Datatypedescription.INTEGER;
	        break;
			case MODE_ID: outString = Datatypedescription.INTEGER;
			break;
			case SPEED: outString = Datatypedescription.FLOAT;
			break;
			case DENSITY: outString = Datatypedescription.FLOAT;
			break;
			case FLOW: outString = Datatypedescription.FLOAT;
			break;
			case TRAVEL_TIME: outString = Datatypedescription.FLOAT;
			break;
		}
		return outString;
	}
}