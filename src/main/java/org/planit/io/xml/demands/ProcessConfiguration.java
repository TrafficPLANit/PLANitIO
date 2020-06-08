package org.planit.io.xml.demands;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.generated.Durationunit;
import org.planit.generated.XMLElementDemandConfiguration;
import org.planit.generated.XMLElementTimePeriods;
import org.planit.generated.XMLElementTravellerTypes;
import org.planit.generated.XMLElementUserClasses;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
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

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ProcessConfiguration.class.getCanonicalName());

  /**
   * Generate TravelerType objects from generated configuration object
   * and store them
   * 
   * @param demandconfiguration generated XMLElementDemandConfiguration object from
   *          demand XML input
   * @param inputBuilderListener parser to be updated
   * @return Set of traveller type id values
   * @throws PlanItException
   */
  private static Set<BigInteger> generateAndStoreTravelerTypes(XMLElementDemandConfiguration demandconfiguration,
      InputBuilderListener inputBuilderListener) throws PlanItException {
    XMLElementTravellerTypes travellertypes = (demandconfiguration.getTravellertypes() == null)
        ? new XMLElementTravellerTypes() : demandconfiguration.getTravellertypes();
    Set<BigInteger> travellerTypeIdSet = new HashSet<BigInteger>();
    if (travellertypes.getTravellertype().isEmpty()) {
      travellertypes.getTravellertype().add(generateDefaultTravellerType());
    }

    for (XMLElementTravellerTypes.Travellertype travellertype : travellertypes.getTravellertype()) {
      TravelerType travelerType = new TravelerType(travellertype.getId().longValue(), travellertype.getName());
      final boolean duplicateTravelerTypeExternalId = inputBuilderListener.addTravelerTypeToExternalIdMap(travelerType
          .getExternalId(), travelerType);
      if (duplicateTravelerTypeExternalId && inputBuilderListener.isErrorIfDuplicateExternalId()) {
        String errorMessage = "Duplicate traveler type external id " + travelerType.getExternalId()
            + " found in network file.";
        LOGGER.severe(errorMessage);
        throw new PlanItException(errorMessage);
      }
      travellerTypeIdSet.add(travellertype.getId());

    }
    return travellerTypeIdSet;
  }

  /**
   * Generate XMLElementUserClasses objects from generated configuration object
   * and store them
   * 
   * @param demandconfiguration generated XMLElementDemandConfiguration object from demand XML input
   * @param travellerTypeIdSet Set of id values of traveller types
   * @param physicalNetwork the physical network
   * @param inputBuilderListener parser to be updated
   * @return the number of user classes
   * @throws PlanItException thrown if a duplicate external Id key is found
   */
  private static int generateAndStoreUserClasses(
      XMLElementDemandConfiguration demandconfiguration,
      Set<BigInteger> travellerTypeIdSet,
      PhysicalNetwork physicalNetwork,
      InputBuilderListener inputBuilderListener) throws PlanItException {

    XMLElementUserClasses userclasses = 
        (demandconfiguration.getUserclasses() == null) ? new XMLElementUserClasses() : demandconfiguration.getUserclasses();
    
    if (userclasses.getUserclass().isEmpty()) {
      if (travellerTypeIdSet.size() > 1) {
        String errorMessage = "No user classes defined but more than 1 traveller type defined";
        throw new PlanItException(errorMessage);
      }
      XMLElementUserClasses.Userclass userClass = generateDefaultUserClass();
      userClass.setTravellertyperef(travellerTypeIdSet.iterator().next());
      userclasses.getUserclass().add(userClass);
    }
    
    for (XMLElementUserClasses.Userclass userclass : userclasses.getUserclass()) {
      
      if ((userclass.getTravellertyperef() != null) && (!travellerTypeIdSet.contains(userclass.getTravellertyperef()))) {
        String errorMessage = "travellertyperef value of " + userclass.getTravellertyperef().longValueExact()+ " referenced by user class " + userclass.getName() + " but not defined";
        throw new PlanItException(errorMessage);
      }
      
      if ((userclass.getTravellertyperef() == null) && (travellerTypeIdSet.size() > 1)) {
        String errorMessage = "User class " + userclass.getId() + " has no traveller type specified, but more than one traveller type possible";
        throw new PlanItException(errorMessage);
      }
      
      if (userclass.getModeref() == null) {
        if (physicalNetwork.modes.getNumberOfModes() > 1) {
          String errorMessage = "User class " + userclass.getId() + " has no mode specified, but more than one mode possible.";
          throw new PlanItException(errorMessage);
        }        
        for(Mode mode : physicalNetwork.modes) {
          long modeExternalId = (long) mode.getExternalId();
          userclass.setModeref(BigInteger.valueOf(modeExternalId));          
        }
      }
      Long externalModeId = userclass.getModeref().longValue();
      Mode userClassMode = inputBuilderListener.getModeByExternalId(externalModeId);
      if (userClassMode == null) {
        String errorMessage = "User class " + userclass.getId() + " refers to mode " + externalModeId + " which has not been defined";
        throw new PlanItException(errorMessage);
      }
      
      long travellerTypeId = (userclass.getTravellertyperef() == null) ? TravelerType.DEFAULT_EXTERNAL_ID : userclass.getTravellertyperef().longValue();
      userclass.setTravellertyperef(BigInteger.valueOf(travellerTypeId));
      TravelerType travellerType = inputBuilderListener.getTravelerTypeByExternalId(travellerTypeId);
      
      UserClass userClass = new UserClass(
          userclass.getId().longValue(),
          userclass.getName(),
          userClassMode,
          travellerType);
      
      final boolean duplicateUserClassExternalId = inputBuilderListener.addUserClassToExternalIdMap(userClass.getExternalId(), userClass);
      if (duplicateUserClassExternalId && inputBuilderListener.isErrorIfDuplicateExternalId()) {
        String errorMessage = "Duplicate user class external id " + userClass.getExternalId() + " found in network file.";
        throw new PlanItException(errorMessage);
      }
    }
    return userclasses.getUserclass().size();
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
   * @param demands the Demands object
   * @param demandconfiguration generated XMLElementDemandConfiguration object from
   *          demand XML input
   * @param inputBuilderListener parser to be updated
   * @return Map of TimePeriod objects, using the id of the TimePeriod as its key
   * @throws PlanItException thrown if a duplicate external Id is found, or if there is an
   */
  private static void generateTimePeriodMap(
      Demands demands,
      XMLElementDemandConfiguration demandconfiguration,
      InputBuilderListener inputBuilderListener) throws PlanItException {
    XMLElementTimePeriods timeperiods = demandconfiguration.getTimeperiods();

    XMLGregorianCalendar defaultStartTime;

    try {
      LocalDateTime localDateTime = LocalDate.now().atStartOfDay();
      defaultStartTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(localDateTime.format(
          DateTimeFormatter.ISO_DATE_TIME));
    } catch (DatatypeConfigurationException e) {
      throw new PlanItException(e);
    }
    for (XMLElementTimePeriods.Timeperiod timePeriodGenerated : timeperiods.getTimeperiod()) {
      long timePeriodId = timePeriodGenerated.getId().longValue();
      XMLGregorianCalendar time = (timePeriodGenerated.getStarttime() == null) ? defaultStartTime : timePeriodGenerated
          .getStarttime();
      int startTime = 3600 * time.getHour() + 60 * time.getMinute() + time.getSecond();
      int duration = timePeriodGenerated.getDuration().getValue().intValue();
      Durationunit durationUnit = timePeriodGenerated.getDuration().getUnit();
      if (timePeriodGenerated.getName() == null) {
        timePeriodGenerated.setName("");
      }
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
      final boolean duplicateTimePeriodExternalId = inputBuilderListener.addTimePeriodToExternalIdMap(timePeriod
          .getExternalId(), timePeriod);
      if (duplicateTimePeriodExternalId && inputBuilderListener.isErrorIfDuplicateExternalId()) {
        String errorMessage = "Duplicate time period external id " + timePeriod.getExternalId()
            + " found in network file.";
        LOGGER.severe(errorMessage);
        throw new PlanItException(errorMessage);
      }
      demands.timePeriods.registerTimePeriod(timePeriod);
    }
  }

  /**
   * Sets up all the configuration data from the XML demands file
   * 
   * @param demands the Demands object
   * @param demandconfiguration the generated XMLElementDemandConfiguration object
   *          containing the data from the XML input file
   * @param physicalNetwork the physical network
   * @param inputBuilderListener parser to be updated
   * @return Map of TimePeriod objects, using the id of the TimePeriod as its key
   * @throws PlanItException thrown if there is a duplicate external Id found for any component
   */
  public static void generateAndStoreConfigurationData(
      Demands demands,
      XMLElementDemandConfiguration demandconfiguration,
      PhysicalNetwork physicalNetwork,
      InputBuilderListener inputBuilderListener) throws PlanItException {
    Set<BigInteger> travellerTypes = ProcessConfiguration.generateAndStoreTravelerTypes(demandconfiguration,
        inputBuilderListener);
    ProcessConfiguration.generateAndStoreUserClasses(demandconfiguration, travellerTypes, physicalNetwork,
        inputBuilderListener);
    ProcessConfiguration.generateTimePeriodMap(demands, demandconfiguration, inputBuilderListener);
  }

}
