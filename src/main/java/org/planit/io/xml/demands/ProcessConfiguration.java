package org.planit.io.xml.demands;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.planit.demands.Demands;
import org.planit.xml.generated.Durationunit;
import org.planit.xml.generated.XMLElementDemandConfiguration;
import org.planit.xml.generated.XMLElementTimePeriods;
import org.planit.xml.generated.XMLElementTravellerTypes;
import org.planit.xml.generated.XMLElementUserClasses;
import org.planit.input.InputBuilderListener;
import org.planit.network.physical.PhysicalNetwork;
import org.planit.time.TimePeriod;
import org.planit.userclass.TravelerType;
import org.planit.userclass.UserClass;
import org.planit.utils.exceptions.PlanItException;
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
   * @param demands to register travel types on 
   * @param demandconfiguration generated XMLElementDemandConfiguration object from demand XML input
   * @param inputBuilderListener parser to be updated
   * @throws PlanItException
   */
  private static void generateAndStoreTravelerTypes(Demands demands, XMLElementDemandConfiguration demandconfiguration,
      InputBuilderListener inputBuilderListener) throws PlanItException {
    
    XMLElementTravellerTypes travellertypes = 
        (demandconfiguration.getTravellertypes() == null) ? new XMLElementTravellerTypes() : demandconfiguration.getTravellertypes();
    if (travellertypes.getTravellertype().isEmpty()) {
      travellertypes.getTravellertype().add(generateDefaultXMLTravellerType());
    }
    
    for (XMLElementTravellerTypes.Travellertype travellertype : travellertypes.getTravellertype()) {
      TravelerType travelerType = demands.travelerTypes.createAndRegisterNewTravelerType(travellertype.getId().longValue(), travellertype.getName());
      final boolean duplicateTravelerTypeExternalId = 
          inputBuilderListener.addTravelerTypeToExternalIdMap(travelerType.getExternalId(), travelerType);
      PlanItException.throwIf(duplicateTravelerTypeExternalId && inputBuilderListener.isErrorIfDuplicateExternalId(), 
          "Duplicate traveler type external id " + travelerType.getExternalId() + " found in network file");
    }
  }

  /**
   * Generate XMLElementUserClasses objects from generated configuration object
   * and store them
   * 
   * @param demands the demands to register the user classes on
   * @param demandconfiguration generated XMLElementDemandConfiguration object from demand XML input
   * @param physicalNetwork the physical network
   * @param inputBuilderListener parser to be updated
   * @return the number of user classes
   * @throws PlanItException thrown if a duplicate external Id key is found
   */
  private static int generateAndStoreUserClasses(
      Demands demands,
      XMLElementDemandConfiguration demandconfiguration,
      PhysicalNetwork<?,?,?> physicalNetwork,
      InputBuilderListener inputBuilderListener) throws PlanItException {

    XMLElementUserClasses userclasses = 
        (demandconfiguration.getUserclasses() == null) ? new XMLElementUserClasses() : demandconfiguration.getUserclasses();
    
    if (userclasses.getUserclass().isEmpty()) {
      PlanItException.throwIf(demands.travelerTypes.getNumberOfTravelerTypes() > 1, "No user classes defined but more than 1 traveller type defined");
      
      XMLElementUserClasses.Userclass userClass = generateDefaultUserClass();
      userClass.setTravellertyperef(BigInteger.valueOf((long)demands.travelerTypes.getFirst().getExternalId()));
      userclasses.getUserclass().add(userClass);
    }
    
    for (XMLElementUserClasses.Userclass userclass : userclasses.getUserclass()) {
      if(userclass.getTravellertyperef()==null) {
        PlanItException.throwIf(demands.travelerTypes.getNumberOfTravelerTypes() > 1,
            "User class " + userclass.getId() + " has no traveller type specified, but more than one traveller type possible");                
      }else {
        PlanItException.throwIf(demands.travelerTypes.getTravelerTypeByExternalId(userclass.getTravellertyperef().longValue()) == null, 
            "travellertyperef value of " + userclass.getTravellertyperef().longValueExact()+ " referenced by user class " + userclass.getName() + " but not defined");
      }
      PlanItException.throwIf(userclass.getModeref() == null, "User class " + userclass.getId() + " has no mode specified, but more than one mode possible");
      
      if (userclass.getModeref() == null) {
        PlanItException.throwIf(physicalNetwork.modes.size() > 1, 
            "User class " + userclass.getId() + " has no mode specified, but more than one mode possible");
                
        for(Mode mode : physicalNetwork.modes) {
          long modeExternalId = (long) mode.getExternalId();
          userclass.setModeref(BigInteger.valueOf(modeExternalId));          
        }
      }
      Long externalModeId = userclass.getModeref().longValue();
      Mode userClassMode = inputBuilderListener.getModeByExternalId(externalModeId);
      PlanItException.throwIf(userClassMode == null,"User class " + userclass.getId() + " refers to mode " + externalModeId + " which has not been defined");
           
      long travellerTypeId = (userclass.getTravellertyperef() == null) ? TravelerType.DEFAULT_EXTERNAL_ID : userclass.getTravellertyperef().longValue();
      userclass.setTravellertyperef(BigInteger.valueOf(travellerTypeId));
      TravelerType travellerType = inputBuilderListener.getTravelerTypeByExternalId(travellerTypeId);
      
      UserClass userClass = demands.userClasses.createAndRegisterNewUserClass(
          userclass.getId().longValue(),
          userclass.getName(),
          userClassMode,
          travellerType);
      
      final boolean duplicateUserClassExternalId = inputBuilderListener.addUserClassToExternalIdMap(userClass.getExternalId(), userClass);
      PlanItException.throwIf(duplicateUserClassExternalId && inputBuilderListener.isErrorIfDuplicateExternalId(),
          "Duplicate user class external id " + userClass.getExternalId() + " found in network file");
    }
    return userclasses.getUserclass().size();
  }

  /**
   * Generate default traveller type if none defined in XML files
   * 
   * @return default XMLElementTravellerTypes object
   */
  private static XMLElementTravellerTypes.Travellertype generateDefaultXMLTravellerType() {
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
    userclass.setModeref(BigInteger.valueOf(Mode.DEFAULT_EXTERNAL_ID));
    userclass.setTravellertyperef(BigInteger.valueOf(TravelerType.DEFAULT_EXTERNAL_ID));
    return userclass;
  }

  /**
   * Generate a Map of TimePeriod objects from generated configuration object
   * 
   * @param demands the Demands object
   * @param demandconfiguration generated XMLElementDemandConfiguration object from
   *          demand XML input
   * @param inputBuilderListener parser to be updated
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
      defaultStartTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(localDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
    } catch (DatatypeConfigurationException e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when generating time period map when processing demand configuration",e);
    }
    for (XMLElementTimePeriods.Timeperiod timePeriodGenerated : timeperiods.getTimeperiod()) {
      long timePeriodExternalId = timePeriodGenerated.getId().longValue();
      XMLGregorianCalendar time = (timePeriodGenerated.getStarttime() == null) ? defaultStartTime : timePeriodGenerated.getStarttime();
      int startTimeSeconds = 3600 * time.getHour() + 60 * time.getMinute() + time.getSecond();
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
      TimePeriod timePeriod = demands.timePeriods.createAndRegisterNewTimePeriod(
          timePeriodExternalId, timePeriodGenerated.getName(), startTimeSeconds, duration /*converted to seconds*/);
      final boolean duplicateTimePeriodExternalId = inputBuilderListener.addTimePeriodToExternalIdMap(timePeriod.getExternalId(), timePeriod);
      PlanItException.throwIf(duplicateTimePeriodExternalId && inputBuilderListener.isErrorIfDuplicateExternalId(), 
          "Duplicate time period external id " + timePeriod.getExternalId() + " found in network file.");
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
   * @throws PlanItException thrown if there is a duplicate external Id found for any component
   */
  public static void generateAndStoreConfigurationData(
      Demands demands,
      XMLElementDemandConfiguration demandconfiguration,
      PhysicalNetwork<?,?,?> physicalNetwork,
      InputBuilderListener inputBuilderListener) throws PlanItException {
    ProcessConfiguration.generateAndStoreTravelerTypes(demands, demandconfiguration, inputBuilderListener);
    ProcessConfiguration.generateAndStoreUserClasses(demands, demandconfiguration, physicalNetwork, inputBuilderListener);
    ProcessConfiguration.generateTimePeriodMap(demands, demandconfiguration, inputBuilderListener);
  }

}
