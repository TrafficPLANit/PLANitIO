package org.planit.io.xml.demands;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map.Entry;
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
import org.planit.utils.mode.Mode;

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
    
    XMLElementTravellerTypes xmlTravellertypes = 
        (demandconfiguration.getTravellertypes() == null) ? new XMLElementTravellerTypes() : demandconfiguration.getTravellertypes();
    if (xmlTravellertypes.getTravellertype().isEmpty()) {
      xmlTravellertypes.getTravellertype().add(generateDefaultXMLTravellerType());
    }
    
    for (XMLElementTravellerTypes.Travellertype xmlTravellertype : xmlTravellertypes.getTravellertype()) {
      
      /* external id */
      String externalId = String.valueOf(xmlTravellertype.getId());
      if(xmlTravellertype.getExternalid() != null && !xmlTravellertype.getExternalid().isBlank()) {
        externalId = xmlTravellertype.getExternalid();
      }  
      
      TravelerType travelerType = demands.travelerTypes.createAndRegisterNewTravelerType(externalId, xmlTravellertype.getName());
      final boolean duplicateTravelerTypeXmlId = inputBuilderListener.addTravelerTypeToXmlIdMap(xmlTravellertype.getId().longValue(), travelerType);
      PlanItException.throwIf(duplicateTravelerTypeXmlId && inputBuilderListener.isErrorIfDuplicateXmlId(), 
          "Duplicate traveler type xml id " + travelerType.getExternalId() + " found in network file");
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

    XMLElementUserClasses xmlUserclasses = 
        (demandconfiguration.getUserclasses() == null) ? new XMLElementUserClasses() : demandconfiguration.getUserclasses();
    
    if (xmlUserclasses.getUserclass().isEmpty()) {
      PlanItException.throwIf(demands.travelerTypes.getNumberOfTravelerTypes() > 1, "No user classes defined but more than 1 traveller type defined");
      
      XMLElementUserClasses.Userclass xmlUserClass = generateDefaultUserClass();
      xmlUserClass.setTravellertyperef(BigInteger.valueOf((long)demands.travelerTypes.getFirst().getExternalId()));
      xmlUserclasses.getUserclass().add(xmlUserClass);
    }
    
    for (XMLElementUserClasses.Userclass xmlUserclass : xmlUserclasses.getUserclass()) {
      if(xmlUserclass.getTravellertyperef()==null) {
        PlanItException.throwIf(demands.travelerTypes.getNumberOfTravelerTypes() > 1,
            "User class " + xmlUserclass.getId() + " has no traveller type specified, but more than one traveller type possible");                
      }else {
        PlanItException.throwIf(demands.travelerTypes.getTravelerTypeByExternalId(xmlUserclass.getTravellertyperef().longValue()) == null, 
            "travellertyperef value of " + xmlUserclass.getTravellertyperef().longValueExact()+ " referenced by user class " + xmlUserclass.getName() + " but not defined");
      }
      PlanItException.throwIf(xmlUserclass.getModeref() == null, "User class " + xmlUserclass.getId() + " has no mode specified, but more than one mode possible");
      
      /* mode ref */
      if (xmlUserclass.getModeref() == null) {
        PlanItException.throwIf(physicalNetwork.modes.size() > 1, 
            "User class " + xmlUserclass.getId() + " has no mode specified, but more than one mode possible");
                
        
        for(Entry<Long,Mode> entry : inputBuilderListener.getAllModesByXmlId().entrySet()) {
          xmlUserclass.setModeref(BigInteger.valueOf(entry.getKey()));          
        }
      }
      Long xmlModeIdRef = xmlUserclass.getModeref().longValue();
      Mode userClassMode = inputBuilderListener.getModeByXmlId(xmlModeIdRef);
      PlanItException.throwIf(userClassMode == null,"User class " + xmlUserclass.getId() + " refers to mode " + xmlModeIdRef + " which has not been defined");
           
      /* traveller type ref */
      long travellerTypeXmlIdRef = (xmlUserclass.getTravellertyperef() == null) ? TravelerType.DEFAULT_XML_ID : xmlUserclass.getTravellertyperef().longValue();
      xmlUserclass.setTravellertyperef(BigInteger.valueOf(travellerTypeXmlIdRef));
      TravelerType travellerType = inputBuilderListener.getTravelerTypeByXmlId(travellerTypeXmlIdRef);
      
      /* external id */
      String externalId = String.valueOf(xmlUserclass.getId());
      if(xmlUserclass.getExternalid() != null && !xmlUserclass.getExternalid().isBlank()) {
        externalId = xmlUserclass.getExternalid();
      }        
      
      UserClass userClass = demands.userClasses.createAndRegisterNewUserClass(
          externalId, xmlUserclass.getName(), userClassMode, travellerType);
      
      final boolean duplicateUserClassXmlId = inputBuilderListener.addUserClassToXmlIdMap(xmlUserclass.getId().longValue(), userClass);
      PlanItException.throwIf(duplicateUserClassXmlId && inputBuilderListener.isErrorIfDuplicateXmlId(),
          "Duplicate user class xml id " + userClass.getExternalId() + " found in network file");
    }
    return xmlUserclasses.getUserclass().size();
  }

  /**
   * Generate default traveller type if none defined in XML files
   * 
   * @return default XMLElementTravellerTypes object
   */
  private static XMLElementTravellerTypes.Travellertype generateDefaultXMLTravellerType() {
    XMLElementTravellerTypes.Travellertype travellerType = new XMLElementTravellerTypes.Travellertype();
    travellerType.setId(BigInteger.valueOf(TravelerType.DEFAULT_XML_ID));
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
    userclass.setId(BigInteger.valueOf(UserClass.DEFAULT_XML_ID));
    userclass.setModeref(BigInteger.valueOf(Mode.DEFAULT_XML_ID));
    userclass.setTravellertyperef(BigInteger.valueOf(TravelerType.DEFAULT_XML_ID));
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
    
    /* tim periods */
    XMLElementTimePeriods xmlTimeperiods = demandconfiguration.getTimeperiods();

    XMLGregorianCalendar defaultStartTime;
    try {
      LocalDateTime localDateTime = LocalDate.now().atStartOfDay();
      defaultStartTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(localDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
    } catch (DatatypeConfigurationException e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when generating time period map when processing demand configuration",e);
    }
    
    /* time period */
    for (XMLElementTimePeriods.Timeperiod xmlTimePeriod : xmlTimeperiods.getTimeperiod()) {
      
      /* external id */
      String externalId = String.valueOf(xmlTimePeriod.getId());
      if(xmlTimePeriod.getExternalid() != null && !xmlTimePeriod.getExternalid().isBlank()) {
        externalId = xmlTimePeriod.getExternalid();
      }       
      
      /* starttime, duration */
      XMLGregorianCalendar time = (xmlTimePeriod.getStarttime() == null) ? defaultStartTime : xmlTimePeriod.getStarttime();
      int startTimeSeconds = 3600 * time.getHour() + 60 * time.getMinute() + time.getSecond();
      int duration = xmlTimePeriod.getDuration().getValue().intValue();
      Durationunit durationUnit = xmlTimePeriod.getDuration().getUnit();
      if (xmlTimePeriod.getName() == null) {
        xmlTimePeriod.setName("");
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
      
      TimePeriod timePeriod = demands.timePeriods.createAndRegisterNewTimePeriod(externalId, xmlTimePeriod.getName(), startTimeSeconds, duration /*converted to seconds*/);
      final boolean duplicateTimePeriodXmlId = inputBuilderListener.addTimePeriodToXmlIdMap(xmlTimePeriod.getId().longValue(), timePeriod);
      PlanItException.throwIf(duplicateTimePeriodXmlId && inputBuilderListener.isErrorIfDuplicateXmlId(), 
          "Duplicate time period xml id " + xmlTimePeriod.getId() + " found in network file.");
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
