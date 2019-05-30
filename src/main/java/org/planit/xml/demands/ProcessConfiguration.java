package org.planit.xml.demands;

import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.planit.generated.Configuration;
import org.planit.generated.Durationunit;
import org.planit.generated.Timeperiods;
import org.planit.generated.Travellertypes;
import org.planit.generated.Userclasses;
import org.planit.time.TimePeriod;
import org.planit.userclass.TravellerType;
import org.planit.userclass.UserClass;
import org.planit.xml.constants.Default;

/**
 * Methods to generate and store PLANit configuration objects from the configuration object in the XML demand input file.
 * 
 * @author gman6028
 *
 */
public class ProcessConfiguration {
	
/**
 * Sets up all the configuration data from the XML demands file
 * 
 * @param configuration		  the generated Configuration object containing the data from the XML input file
 * @return                               Map of TimePeriod objects, using the id of the TimePeriod as its key
 */
	public static Map<Integer, TimePeriod> generateAndStoreConfigurationData(Configuration configuration) {
   	    ProcessConfiguration.generateAndStoreTravellerTypes(configuration);
    	ProcessConfiguration.generateAndStoreUserClasses(configuration);
    	return ProcessConfiguration.generateTimePeriodMap(configuration);
	}

/**
 * Generate TravellerType objects from generated configuration object and store them
 * 
 * @param configuration       generated Configuration object from demand XML input
 */
    private static void generateAndStoreTravellerTypes(Configuration configuration) {
	     Travellertypes travellertypes = (configuration.getTravellertypes() == null) ?  new Travellertypes() : configuration.getTravellertypes();
    	if (travellertypes.getTravellertype().isEmpty()) {
    		travellertypes.getTravellertype().add(generateDefaultTravellerType());
    	}
    	for (Travellertypes.Travellertype travellertype : travellertypes.getTravellertype()) {
    		TravellerType travellerType = new TravellerType(travellertype.getId().longValue(), travellertype.getName());
    	}
    }

/**
 * Generate default traveller type if none defined in XML files 
 * 
 * @return     default Travellertype object
 */
    private static Travellertypes.Travellertype generateDefaultTravellerType() {
		Travellertypes.Travellertype travellerType = new Travellertypes.Travellertype();
   		travellerType.setId(Default.TRAVELLER_TYPE_ID);
   		travellerType.setName(Default.TRAVELLER_TYPE_NAME);
   		return travellerType;
    }
        
/**
 * Generate UserClass objects from generated configuration object and store them
 * 
 * @param configuration       generated Configuration object from demand XML input
 */
    private static void generateAndStoreUserClasses(Configuration configuration) {
    	Userclasses userclasses = configuration.getUserclasses();
       	if (userclasses.getUserclass().isEmpty()) {
       		userclasses.getUserclass().add(generateDefaultUserClass());
       	}
       	for (Userclasses.Userclass userclass : userclasses.getUserclass()) {
       		int modeId = userclass.getModeref().intValue();
       		long travellerTypeId = (userclass.getTravellertyperef() == null) ? Default.TRAVELLER_TYPE_ID.longValue() : userclass.getTravellertyperef().longValue();
       		TravellerType travellerType = TravellerType.getById(travellerTypeId);
       		UserClass userClass = new UserClass(userclass.getId().longValue(), userclass.getName(), modeId, travellerType.getId());
       	}
    }
       
/**
 * Generate default user class if none defined in XML files 
 * 
 * @return     default Userclass object
 */
   private static Userclasses.Userclass generateDefaultUserClass() {
   		Userclasses.Userclass userclass = new Userclasses.Userclass();
   		userclass.setName(Default.USER_CLASS_NAME);
   		userclass.setId(Default.USER_CLASS_ID);
   		userclass.setModeref(Default.USER_CLASS_MODE_REF);
   		userclass.setTravellertyperef(Default.USER_CLASS_TRAVELLER_TYPE);
   		return userclass;
   }
       
/**
 * Generate a Map of TimePeriod objects from generated configuration object
 * 
 * @param configuration       generated Configuration object from demand XML input
 * @return                              Map of TimePeriod objects, using the id of the TimePeriod as its key
 */
    private static  Map<Integer, TimePeriod> generateTimePeriodMap(Configuration configuration) {
       	Timeperiods timeperiods = configuration.getTimeperiods();
       	Map<Integer, TimePeriod> timePeriodMap = new HashMap<Integer, TimePeriod>();
        for (Timeperiods.Timeperiod timePeriodGenerated : timeperiods.getTimeperiod()) {
            int timePeriodId = timePeriodGenerated.getId().intValue();
            XMLGregorianCalendar time = timePeriodGenerated.getStarttime();
            int startTime = 3600 * time.getHour() + 60 * time.getMinute() + time.getSecond();
            int duration = timePeriodGenerated.getDuration().getValue().intValue();
            Durationunit durationUnit = timePeriodGenerated.getDuration().getUnit();
            switch (durationUnit) {
          	case H:  duration *= 3600;
           				   break;
           	case M: duration *= 60;
   			   			   break;
           	case S:   break;
            }
            TimePeriod timePeriod = new TimePeriod("" + timePeriodId, startTime, duration);
            timePeriodMap.put(timePeriodId, timePeriod);
        }
        return timePeriodMap;
    }
       
}
