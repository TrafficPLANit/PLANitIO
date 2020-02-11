package org.planit.planitio.xml.demands;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.planit.generated.Durationunit;
import org.planit.generated.XMLElementDemandConfiguration;
import org.planit.generated.XMLElementTimePeriods;
import org.planit.generated.XMLElementTravellerTypes;
import org.planit.generated.XMLElementUserClasses;
import org.planit.time.TimePeriod;
import org.planit.userclass.TravelerType;
import org.planit.userclass.UserClass;
import org.planit.utils.network.physical.Mode;

/**
 * Methods to generate and store PLANit configuration objects from the
 * configuration object in the XML demand input file.
 * 
 * @author gman6028
 *
 */
public class ProcessConfiguration {

	/**
	 * Generate XMLElementTravellerTypes objects from generated configuration object
	 * and store them
	 * 
	 * @param demandconfiguration generated XMLElementDemandConfiguration object from
	 *                      demand XML input
	 */
	private static void generateAndStoreTravellerTypes(XMLElementDemandConfiguration demandconfiguration) {
		XMLElementTravellerTypes travellertypes = (demandconfiguration.getTravellertypes() == null)
				? new XMLElementTravellerTypes()
				: demandconfiguration.getTravellertypes();
		if (travellertypes.getTravellertype().isEmpty()) {
			travellertypes.getTravellertype().add(generateDefaultTravellerType());
			demandconfiguration.setTravellertypes(travellertypes);
		}
		for (XMLElementTravellerTypes.Travellertype travellertype : travellertypes.getTravellertype()) {
			new TravelerType(travellertype.getId().longValue(), travellertype.getName());
		}
	}

	/**
	 * Generate XMLElementUserClasses objects from generated configuration object
	 * and store them
	 * 
	 * @param demandconfiguration generated XMLElementDemandConfiguration object from
	 *                      demand XML input
	 * @param modesByExternalIdMap map with modes by their external ids
	 */
	private static void generateAndStoreUserClasses(
			XMLElementDemandConfiguration demandconfiguration, 
			Map<Long, Mode> modesByExternalIdMap) {
		XMLElementUserClasses userclasses = demandconfiguration.getUserclasses();
		if (userclasses.getUserclass().isEmpty()) {
			userclasses.getUserclass().add(generateDefaultUserClass());
		}
		for (XMLElementUserClasses.Userclass userclass : userclasses.getUserclass()) {
			Long externalModeId = userclass.getModeref().longValue();
			Mode userClassMode = modesByExternalIdMap.get(externalModeId);
			long travellerTypeId = 
					(userclass.getTravellertyperef() == null) ? TravelerType.DEFAULT_EXTERNAL_ID : userclass.getTravellertyperef().longValue();
			userclass.setTravellertyperef(BigInteger.valueOf(travellerTypeId));
			TravelerType travellerType = TravelerType.getByExternalId(travellerTypeId);
			UserClass userClass = new UserClass(
					userclass.getId().longValue(), 
					userclass.getName(), 
					userClassMode,
					travellerType);
		}
	}

	/**
	 * Generate default traveller type if none defined in XML files
	 * 
	 * @return default XMLElementTravellerTypes object
	 */
	private static XMLElementTravellerTypes.Travellertype generateDefaultTravellerType() {
		XMLElementTravellerTypes.Travellertype travellerType = new XMLElementTravellerTypes.Travellertype();
		travellerType.setId(BigInteger.valueOf(TravelerType.DEFAULT_EXTERNAL_ID));
		travellerType.setName(TravelerType.DEFAULT_NAME);
		return travellerType;
	}

	/**
	 * Generate default user class if none defined in XML files
	 * 
	 * @return default XMLElementUserClasses object
	 */
	private static XMLElementUserClasses.Userclass generateDefaultUserClass() {
		XMLElementUserClasses.Userclass userclass = new XMLElementUserClasses.Userclass();
		userclass.setName(UserClass.DEFAULT_NAME);
		userclass.setId(BigInteger.valueOf(UserClass.DEFAULT_EXTERNAL_ID));
		userclass.setModeref(BigInteger.valueOf(UserClass.DEFAULT_MODE_REF));
		userclass.setTravellertyperef(BigInteger.valueOf(UserClass.DEFAULT_TRAVELLER_TYPE));
		return userclass;
	}

	/**
	 * Generate a Map of TimePeriod objects from generated configuration object
	 * 
	 * @param demandconfiguration generated XMLElementDemandConfiguration object from
	 *                      demand XML input
	 * @return Map of TimePeriod objects, using the id of the TimePeriod as its key
	 */
	private static Map<Integer, TimePeriod> generateTimePeriodMap(XMLElementDemandConfiguration demandconfiguration) {
		TimePeriod.reset();
		XMLElementTimePeriods timeperiods = demandconfiguration.getTimeperiods();
		Map<Integer, TimePeriod> timePeriodMap = new HashMap<Integer, TimePeriod>();
		for (XMLElementTimePeriods.Timeperiod timePeriodGenerated : timeperiods.getTimeperiod()) {
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
			TimePeriod timePeriod = new TimePeriod(timePeriodId, timePeriodGenerated.getName(), startTime, duration);
			timePeriodMap.put(timePeriodId, timePeriod);
		}
		return timePeriodMap;
	}

	/**
	 * Sets up all the configuration data from the XML demands file
	 * 
	 * @param demandconfiguration the generated XMLElementDemandConfiguration object
	 *                            containing the data from the XML input file
	 * @param modesByExternalIdMap map with parsed modes by their external ids
	 * @return Map of TimePeriod objects, using the id of the TimePeriod as its key
	 */
	public static Map<Integer, TimePeriod> generateAndStoreConfigurationData(
			XMLElementDemandConfiguration demandconfiguration, Map<Long, Mode> modesByExternalIdMap) {
		ProcessConfiguration.generateAndStoreTravellerTypes(demandconfiguration);
		ProcessConfiguration.generateAndStoreUserClasses(demandconfiguration, modesByExternalIdMap);
		return ProcessConfiguration.generateTimePeriodMap(demandconfiguration);
	}

}