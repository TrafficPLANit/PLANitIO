package org.goplanit.io.converter.demands;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.goplanit.converter.IdMapperType;
import org.goplanit.converter.demands.DemandsWriter;
import org.goplanit.demands.Demands;
import org.goplanit.io.converter.PlanitWriterImpl;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.userclass.UserClass;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.xml.generated.Durationunit;
import org.goplanit.xml.generated.XMLElementDemandConfiguration;
import org.goplanit.xml.generated.XMLElementDuration;
import org.goplanit.xml.generated.XMLElementMacroscopicDemand;
import org.goplanit.xml.generated.XMLElementMacroscopicZoning;
import org.goplanit.xml.generated.XMLElementOdDemands;
import org.goplanit.xml.generated.XMLElementOdRawMatrix;
import org.goplanit.xml.generated.XMLElementTimePeriods;
import org.goplanit.xml.generated.XMLElementTravellerTypes;
import org.goplanit.xml.generated.XMLElementUserClasses;

/**
 * A class that takes a PLANit demands and persists it to file in the PLANit native XML format. 
 * 
 * @author markr
 *
 */
public class PlanitDemandsWriter extends PlanitWriterImpl<Demands> implements DemandsWriter {
  
  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitDemandsWriter.class.getCanonicalName());  
          
  /** settings to use */
  private final PlanitDemandsWriterSettings settings;
  
  /** the XML to populate */
  private final XMLElementMacroscopicDemand xmlRawDemands;
  
  /** track user classes per mode as this is not yet supported 100%, so we need to identify if an unsupported situation is provided */
  private final Map<Mode,Set<UserClass>> userClassesPerMode;
           
  /** Populate the demands configuration's time periods
   * 
   * @param demands to populate XML with
   * @param xmlDemandConfiguration to use
   * @throws PlanItException thrown if error
   */  
  private void populateXmlTimePeriods(Demands demands, final XMLElementDemandConfiguration xmlDemandConfiguration) throws PlanItException {
    if(demands.timePeriods== null || demands.timePeriods.isEmpty()) {
      LOGGER.severe("No time periods available on demands, this shouldn't happen");
      return;
    }
        
    var xmlTimePeriods = new XMLElementTimePeriods();
    xmlDemandConfiguration.setTimeperiods(xmlTimePeriods );
    for(var timePeriod : demands.timePeriods) {
      var xmlTimePeriod = new XMLElementTimePeriods.Timeperiod();
      xmlTimePeriods.getTimeperiod().add(xmlTimePeriod);
      
      /* XML id */
      xmlTimePeriod.setId(getTimePeriodIdMapper().apply(timePeriod));
      
      /* external id */
      if(timePeriod.hasExternalId()) {
        xmlTimePeriod.setExternalid(timePeriod.getExternalId());
      }
      
      /* name/description */
      if(timePeriod.hasDescription()) {
        xmlTimePeriod.setName(timePeriod.getDescription());
      }      
      
      /* start time */
      if(timePeriod.getStartTimeSeconds() > 0) {
        try {
          var startTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(LocalDate.now().atStartOfDay().plusSeconds(timePeriod.getStartTimeSeconds()).format(DateTimeFormatter.ISO_DATE_TIME));
          xmlTimePeriod.setStarttime(startTime);
        } catch (Exception e) {
          LOGGER.severe(e.getMessage());
          throw new PlanItException("Error when generating start time of time period "+ timePeriod.getXmlId()+" when persisting demand configuration",e);
        }  
      }      
                
      /* duration */
      if(timePeriod.getDurationSeconds()<=0) {
        throw new PlanItException("Error duration of time period %s  is not positive, this is not allowed", timePeriod.getXmlId());
      }
      var xmlDuration = new XMLElementDuration();
      xmlDuration.setUnit(Durationunit.S); // TODO: ideally we keep the original unit so input and output files are consistent
      xmlDuration.setValue(BigInteger.valueOf(timePeriod.getDurationSeconds()));
      xmlTimePeriod.setDuration(xmlDuration);           
    }
  }

  /** Populate the demands configuration's user class
   * 
   * @param demands to populate XML with
   * @param xmlDemandConfiguration to use
   */  
  private void populateXmlUserClasses(Demands demands, final XMLElementDemandConfiguration xmlDemandConfiguration) {
    if(demands.userClasses == null || demands.userClasses.isEmpty()) {
      LOGGER.severe("No user classes available on demands, this shouldn't happen");
      return;
    }
    
    var xmlUserClasses = new XMLElementUserClasses();
    xmlDemandConfiguration.setUserclasses(xmlUserClasses);
    for(var userClass : demands.userClasses) {
      var xmlUserClass = new XMLElementUserClasses.Userclass();
      xmlUserClasses.getUserclass().add(xmlUserClass);
      
      /* XML id */
      xmlUserClass.setId(getUserClassIdMapper().apply(userClass));
      
      /* external id */
      if(userClass.hasExternalId()) {
        xmlUserClass.setExternalid(userClass.getExternalId());
      }
      
      /* mode ref */
      if(userClass.getMode()==null) {
        LOGGER.warning(String.format("User class %s has no referenced mode", userClass.getXmlId()));
      }else {
        xmlUserClass.setModeref(getXmlModeReference(userClass.getMode()));
        if(!userClassesPerMode.containsKey(userClass.getMode())) {
          var userClassesForMode = new HashSet<UserClass>();
          userClassesPerMode.put(userClass.getMode(), userClassesForMode);
        }
        userClassesPerMode.get(userClass.getMode()).add(userClass);        
      }            
      
      /* traveller type ref */
      if(userClass.getMode()==null) {
        LOGGER.warning(String.format("User class %s has no referenced traveller type", userClass.getXmlId()));
      }else {
        xmlUserClass.setTravellertyperef(getTravellerTypeIdMapper().apply(userClass.getTravelerType()));
      }      
      
      /* name */
      if(userClass.hasName()) {
        xmlUserClass.setName(userClass.getName());
      }      
    }
  }

  /** Populate the demands configuration's traveller types
   * 
   * @param demands to populate XML with
   * @param xmlDemandConfiguration to use
   */
  private void populateXmlTravellerTypes(final Demands demands, final XMLElementDemandConfiguration xmlDemandConfiguration) {
    if(demands.travelerTypes== null || demands.travelerTypes.isEmpty()) {
      LOGGER.severe("No traveller types available on demands, this shouldn't happen");
      return;
    }
    
    var xmlTravellerTypes = new XMLElementTravellerTypes();
    xmlDemandConfiguration.setTravellertypes(xmlTravellerTypes );
    for(var travellerType : demands.travelerTypes) {
      var xmlTravellerType = new XMLElementTravellerTypes.Travellertype();
      xmlTravellerTypes.getTravellertype().add(xmlTravellerType);
      
      /* XML id */
      xmlTravellerType.setId(getTravellerTypeIdMapper().apply(travellerType));
      
      /* external id */
      if(travellerType.hasExternalId()) {
        xmlTravellerType.setExternalid(travellerType.getExternalId());
      }
      
      /* name */
      if(travellerType.hasName()) {
        xmlTravellerType.setName(travellerType.getName());
      }      
    }
    
  }

  /** Populate a single OdDemands entry for a given mode and time period
   * 
   * @param odDemandsEntry to populate XML with
   * @param timePeriod  used
   * @param userClass used
   * @param xmlOdDemandsEntry to populate
   */
  private void populateXmlOdDemandsEntry(final OdDemands odDemandsEntry, TimePeriod timePeriod, UserClass userClass, final XMLElementOdRawMatrix xmlOdDemandsEntry) {
    
    /* time period ref */
    String timePeriodRef = getTimePeriodIdMapper().apply(timePeriod);
    xmlOdDemandsEntry.setTimeperiodref(timePeriodRef);
    
    /* user class ref */
    String userClassRef = getUserClassIdMapper().apply(userClass);
    xmlOdDemandsEntry.setUserclassref(userClassRef);
    
    /* values */
    // CONTINUE HERE
  }

  /** Populate the actual OD Demands
   * 
   * @param demands to extract from
   * @throws PlanItException 
   */
  private void populateXmlOdDemands(Demands demands) throws PlanItException {
    var xmlOdDemands = new XMLElementOdDemands();
    xmlRawDemands.setOddemands(xmlOdDemands);
    
    for(var timePeriod : demands.timePeriods) {
      var modes = demands.getRegisteredModesForTimePeriod(timePeriod);
      for(var mode : modes) {
        var odDemandsEntry = demands.get(mode, timePeriod);
        if(odDemandsEntry != null) {
          //TODO: we do not yet preserve the type of matrix used in input, so we use most compact form which is the raw matrix
          var xmlOdDemandEntryMatrix = new XMLElementOdRawMatrix();
          xmlOdDemands.getOdcellbycellmatrixOrOdrowmatrixOrOdrawmatrix().add(xmlOdDemandEntryMatrix);
          
          if(userClassesPerMode.containsKey(mode) && userClassesPerMode.get(mode).size()>1) {
            //TODO: od matrices are stored per mode, not per user class (in memory), but XML format defines them per user class, so unless they are all defined
            // 1:1 we do not properly support this yet
            throw new PlanItException("PLANit demands writer does not yet support multiple user classes per mode");
          }
          
          populateXmlOdDemandsEntry(odDemandsEntry, timePeriod, userClassesPerMode.get(mode).iterator().next(), xmlOdDemandEntryMatrix);
        }
      }
    }
  }

  /** Populate the demands configuration
   * 
   * @param demands to populate XML with
   * @throws PlanItException thrown if error
   */
  private void populateXmlDemandConfiguration(Demands demands) throws PlanItException {
    
    var demandConfiguration = new XMLElementDemandConfiguration();
    xmlRawDemands.setDemandconfiguration(demandConfiguration);
    
    /* traveller types */
    populateXmlTravellerTypes(demands, demandConfiguration);
    
    /* user classes */
    populateXmlUserClasses(demands, demandConfiguration);
    
    /* time periods */
    populateXmlTimePeriods(demands, demandConfiguration);
  }

  /** Populate the XML id of the XML demands element
   * 
   * @param demands to extract XML id from
   */
  private void populateXmlId(Demands demands) {
    /* XML id */    
    if(!demands.hasXmlId()) {
      LOGGER.warning(String.format("Demands has no XML id defined, adopting internally generated id %d instead",demands.getId()));
      demands.setXmlId(String.valueOf(demands.getId()));
    }
    xmlRawDemands.setId(demands.getXmlId());
  }

  /** Constructor 
   * 
   * @param demandsPath to persist demands on
   * @param xmlRawDemands to populate and persist
   */
  protected PlanitDemandsWriter(final String demandsPath, final XMLElementMacroscopicDemand xmlRawDemands) {
    super(IdMapperType.XML);
    this.settings = new PlanitDemandsWriterSettings(demandsPath);
    this.xmlRawDemands = xmlRawDemands;
    this.userClassesPerMode = new HashMap<>();
  }

  /** default network file name to use */
  public static final String DEFAULT_ZONING_FILE_NAME = "zoning.xml";

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final Demands demands) throws PlanItException {    
    PlanItException.throwIfNull(demands, "Demands is null cannot write to PLANit native format");

    /* initialise */
    {
      LOGGER.info(String.format("Persisting PLANit demands to: %s", Paths.get(getSettings().getOutputPathDirectory(), getSettings().getFileName()).toString()));
    }
    
    getSettings().logSettings();
    
    /* xml id */
    populateXmlId(demands);
        
    /* configuration */
    populateXmlDemandConfiguration(demands);
    
    /* oddemands */
    populateXmlOdDemands(demands);
        
    /* persist */
    super.persist(xmlRawDemands, XMLElementMacroscopicZoning.class, PlanitSchema.MACROSCOPIC_ZONING_XSD);
  }


  /**
   * {@inheritDoc}
   */  
  @Override
  public void reset() {
    xmlRawDemands.setDemandconfiguration(null);
    xmlRawDemands.setId(null);
    xmlRawDemands.setOddemands(null); 
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitDemandsWriterSettings getSettings() {
    return this.settings;
  }
  
}
