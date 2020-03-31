package org.planit.io.xml.demands;

import java.math.BigInteger;

import javax.xml.datatype.XMLGregorianCalendar;

import org.planit.exceptions.PlanItException;
import org.planit.generated.Durationunit;
import org.planit.generated.XMLElementDemandConfiguration;
import org.planit.generated.XMLElementTimePeriods;
import org.planit.generated.XMLElementTravellerTypes;
import org.planit.generated.XMLElementUserClasses;
import org.planit.input.InputBuilderListener;
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
   * Generate TravelerType objects from generated configuration object
   * and store them
   * 
   * @param demandconfiguration generated XMLElementDemandConfiguration object from
   *          demand XML input
   * @param inputBuilderListener parser to be updated
   * @throws PlanItException
   */
  private static void generateAndStoreTravelerTypes(XMLElementDemandConfiguration demandconfiguration,
      InputBuilderListener inputBuilderListener) throws PlanItException {
    XMLElementTravellerTypes travellertypes = (demandconfiguration.getTravellertypes() == null)
        ? new XMLElementTravellerTypes()
        : demandconfiguration.getTravellertypes();
    if (travellertypes.getTravellertype().isEmpty()) {
      travellertypes.getTravellertype().add(generateDefaultTravellerType());
      demandconfiguration.setTravellertypes(travellertypes);
    }
    for (XMLElementTravellerTypes.Travellertype travellertype : travellertypes.getTravellertype()) {
      TravelerType travelerType = new TravelerType(travellertype.getId().longValue(), travellertype.getName());
      final boolean duplicateTravelerTypeExternalId = inputBuilderListener.addTravelerTypeToExternalIdMap(travelerType.getExternalId(), travelerType);
      if (duplicateTravelerTypeExternalId && inputBuilderListener.isErrorIfDuplicateExternalId()) {
        throw new PlanItException("Duplicate traveler type external id " + travelerType.getExternalId() + " found in network file.");
      }
    }
  }

  /**
   * Generate XMLElementUserClasses objects from generated configuration object
   * and store them
   * 
   * @param demandconfiguration generated XMLElementDemandConfiguration object from demand XML input
   * @param inputBuilderListener parser to be updated
   * @throws PlanItException thrown if a duplicate external Id key is found
   */
  private static void generateAndStoreUserClasses(
      XMLElementDemandConfiguration demandconfiguration,
      InputBuilderListener inputBuilderListener) throws PlanItException {
    XMLElementUserClasses userclasses = demandconfiguration.getUserclasses();
    if (userclasses.getUserclass().isEmpty()) {
      userclasses.getUserclass().add(generateDefaultUserClass());
    }
    for (XMLElementUserClasses.Userclass userclass : userclasses.getUserclass()) {
      Long externalModeId = userclass.getModeref().longValue();
      Mode userClassMode = inputBuilderListener.getModeByExternalId(externalModeId);
      long travellerTypeId =
          (userclass.getTravellertyperef() == null) ? TravelerType.DEFAULT_EXTERNAL_ID : userclass.getTravellertyperef()
              .longValue();
      userclass.setTravellertyperef(BigInteger.valueOf(travellerTypeId));
      TravelerType travellerType = inputBuilderListener.getTravelerTypeByExternalId(travellerTypeId);
      UserClass userClass = new UserClass(
          userclass.getId().longValue(),
          userclass.getName(),
          userClassMode,
          travellerType);
      final boolean duplicateUserClassExternalId = inputBuilderListener.addUserClassToExternalIdMap(userClass.getExternalId(), userClass);
      if (duplicateUserClassExternalId && inputBuilderListener.isErrorIfDuplicateExternalId()) {
        throw new PlanItException("Duplicate user class external id " + userClass.getExternalId() + " found in network file.");
      }
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
   *          demand XML input
   * @param inputBuilderListener parser to be updated
   * @return Map of TimePeriod objects, using the id of the TimePeriod as its key
   * @throws PlanItException thrown if a duplicate external Id is found
   */
  private static void generateTimePeriodMap(XMLElementDemandConfiguration demandconfiguration,
      InputBuilderListener inputBuilderListener) throws PlanItException {
    TimePeriod.reset();
    XMLElementTimePeriods timeperiods = demandconfiguration.getTimeperiods();
    for (XMLElementTimePeriods.Timeperiod timePeriodGenerated : timeperiods.getTimeperiod()) {
      long timePeriodId = timePeriodGenerated.getId().longValue();
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
      final boolean duplicateTimePeriodExternalId = inputBuilderListener.addTimePeriodToExternalIdMap(timePeriod.getExternalId(), timePeriod);
      if (duplicateTimePeriodExternalId && inputBuilderListener.isErrorIfDuplicateExternalId()) {
        throw new PlanItException("Duplicate time period external id " + timePeriod.getExternalId() + " found in network file.");
      }
    }
  }

  /**
   * Sets up all the configuration data from the XML demands file
   * 
   * @param demandconfiguration the generated XMLElementDemandConfiguration object
   *          containing the data from the XML input file
   * @param inputBuilderListener parser to be updated
   * @return Map of TimePeriod objects, using the id of the TimePeriod as its key
   * @throws PlanItException thrown if there is a duplicate external Id found for any component
   */
  public static void generateAndStoreConfigurationData(
      XMLElementDemandConfiguration demandconfiguration,
      InputBuilderListener inputBuilderListener) throws PlanItException {
    ProcessConfiguration.generateAndStoreTravelerTypes(demandconfiguration, inputBuilderListener);
    ProcessConfiguration.generateAndStoreUserClasses(demandconfiguration, inputBuilderListener);
    ProcessConfiguration.generateTimePeriodMap(demandconfiguration, inputBuilderListener);
  }

}
