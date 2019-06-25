package org.planit.xml.demands;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.planit.generated.Demandconfiguration;
import org.planit.generated.Durationunit;
import org.planit.generated.Timeperiods;
import org.planit.generated.Travellertypes;
import org.planit.generated.Userclasses;
import org.planit.time.TimePeriod;
import org.planit.userclass.TravelerType;
import org.planit.userclass.UserClass;
import org.planit.input.PlanItXMLInputBuilder;

/**
 * Methods to generate and store PLANit configuration objects from the
 * configuration object in the XML demand input file.
 * 
 * @author gman6028
 *
 */
public class ProcessConfiguration {

	/**
	 * Sets up all the configuration data from the XML demands file
	 * 
	 * @param demandconfiguration the generated Demandconfiguration object containing the data
	 *                      from the XML input file
	 * @return Map of TimePeriod objects, using the id of the TimePeriod as its key
	 */
	public static Map<Integer, TimePeriod> generateAndStoreConfigurationData(Demandconfiguration demandconfiguration) {
		ProcessConfiguration.generateAndStoreTravellerTypes(demandconfiguration);
		ProcessConfiguration.generateAndStoreUserClasses(demandconfiguration);
		return ProcessConfiguration.generateTimePeriodMap(demandconfiguration);
	}

	/**
	 * Generate TravellerType objects from generated configuration object and store
	 * them
	 * 
	 * @param configuration generated Demandconfiguration object from demand XML input
	 */
	private static void generateAndStoreTravellerTypes(Demandconfiguration demandconfiguration) {
		Travellertypes travellertypes = (demandconfiguration.getTravellertypes() == null) ? new Travellertypes()
				: demandconfiguration.getTravellertypes();
		if (travellertypes.getTravellertype().isEmpty()) {
			travellertypes.getTravellertype().add(generateDefaultTravellerType());
			demandconfiguration.setTravellertypes(travellertypes);
		}
		for (Travellertypes.Travellertype travellertype : travellertypes.getTravellertype()) {
			TravelerType travellerType = new TravelerType(travellertype.getId().longValue(), travellertype.getName());
		}
	}

	/**
	 * Generate default traveller type if none defined in XML files
	 * 
	 * @return default Travellertype object
	 */
	private static Travellertypes.Travellertype generateDefaultTravellerType() {
		Travellertypes.Travellertype travellerType = new Travellertypes.Travellertype();
		//travellerType.setId(BigInteger.valueOf(PlanItXml.DEFAULT_TRAVELER_TYPE_EXTERNAL_ID));
		travellerType.setId(BigInteger.valueOf(TravelerType.DEFAULT_EXTERNAL_ID));
		travellerType.setName(TravelerType.DEFAULT_NAME);
		return travellerType;
	}

	/**
	 * Generate UserClass objects from generated configuration object and store them
	 * 
	 * @param configuration generated Demandconfiguration object from demand XML input
	 */
	private static void generateAndStoreUserClasses(Demandconfiguration demandconfiguration) {
		Userclasses userclasses = demandconfiguration.getUserclasses();
		if (userclasses.getUserclass().isEmpty()) {
			userclasses.getUserclass().add(generateDefaultUserClass());
		}
		for (Userclasses.Userclass userclass : userclasses.getUserclass()) {
			int modeId = userclass.getModeref().intValue();
			long travellerTypeId = (userclass.getTravellertyperef() == null)
					//? PlanItXml.DEFAULT_TRAVELER_TYPE_EXTERNAL_ID
					? TravelerType.DEFAULT_EXTERNAL_ID
					: userclass.getTravellertyperef().longValue();
			userclass.setTravellertyperef(BigInteger.valueOf(travellerTypeId));
			TravelerType travellerType = TravelerType.getByExternalId(travellerTypeId);
			UserClass userClass = new UserClass(userclass.getId().longValue(), userclass.getName(), modeId,
					travellerType.getExternalId());
		}
	}

	/**
	 * Generate default user class if none defined in XML files
	 * 
	 * @return default Userclass object
	 */
	private static Userclasses.Userclass generateDefaultUserClass() {
		Userclasses.Userclass userclass = new Userclasses.Userclass();
		userclass.setName(UserClass.DEFAULT_NAME);
		userclass.setId(BigInteger.valueOf(UserClass.DEFAULT_EXTERNAL_ID));
		userclass.setModeref(BigInteger.valueOf(UserClass.DEFAULT_MODE_REF));
		userclass.setTravellertyperef(BigInteger.valueOf(UserClass.DEFAULT_TRAVELLER_TYPE));
		return userclass;
	}

	/**
	 * Generate a Map of TimePeriod objects from generated configuration object
	 * 
	 * @param configuration generated Demandconfiguration object from demand XML input
	 * @return Map of TimePeriod objects, using the id of the TimePeriod as its key
	 */
	private static Map<Integer, TimePeriod> generateTimePeriodMap(Demandconfiguration demandconfiguration) {
		Timeperiods timeperiods = demandconfiguration.getTimeperiods();
		Map<Integer, TimePeriod> timePeriodMap = new HashMap<Integer, TimePeriod>();
		for (Timeperiods.Timeperiod timePeriodGenerated : timeperiods.getTimeperiod()) {
			int timePeriodId = timePeriodGenerated.getId().intValue();
			XMLGregorianCalendar time = timePeriodGenerated.getStarttime();
			int startTime = 3600 * time.getHour() + 60 * time.getMinute() + time.getSecond();
			int duration = timePeriodGenerated.getDuration().getValue().intValue();
			Durationunit durationUnit = timePeriodGenerated.getDuration().getUnit();
			switch (durationUnit) {
			case H:
				duration *= 3600;
				break;
			case M:
				duration *= 60;
				break;
			case S:
				break;
			}
			//TimePeriod timePeriod = new TimePeriod("" + timePeriodId, startTime, duration);
			TimePeriod timePeriod = new TimePeriod(timePeriodGenerated.getName(), startTime, duration);
			timePeriodMap.put(timePeriodId, timePeriod);
		}
		return timePeriodMap;
	}

}
