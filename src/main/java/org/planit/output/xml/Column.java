package org.planit.output.xml;

import org.planit.generated.Datatypedescription;

public enum Column {
    LINK_ID, 
    MODE_ID, 
    SPEED, 
    DENSITY,
    FLOW,
    LENGTH,
    START_NODE_ID,
    END_NODE_ID,
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
			case LENGTH: outString = "length";
			break;
			case START_NODE_ID: outString = "start node id";
			break;
			case END_NODE_ID: outString = "end node id";
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
			case LENGTH: outString = "km";
			break;
			case START_NODE_ID: outString = "none";
			break;
			case END_NODE_ID: outString = "none";
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
			case LENGTH: outString = Datatypedescription.DOUBLE;
			break;
			case START_NODE_ID:  outString = Datatypedescription.INTEGER;
			break;
			case END_NODE_ID:  outString = Datatypedescription.INTEGER;
			break;
		}
		return outString;
	}
}