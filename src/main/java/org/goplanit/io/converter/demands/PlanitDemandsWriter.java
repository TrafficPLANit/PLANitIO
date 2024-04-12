package org.goplanit.io.converter.demands;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.logging.Logger;

import org.goplanit.converter.idmapping.DemandsIdMapper;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.converter.demands.DemandsWriter;
import org.goplanit.demands.Demands;
import org.goplanit.io.converter.PlanitWriterImpl;
import org.goplanit.io.xml.util.PlanitSchema;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.userclass.UserClass;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.xml.generated.*;
import org.goplanit.zoning.Zoning;

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

  /** the reference zoning to use */
  private Zoning referenceZoning;
  
  /** the XML to populate */
  private final XMLElementMacroscopicDemand xmlRawDemands;
  
  /** track user classes per mode as this is not yet supported 100%, so we need to identify if an unsupported situation is provided */
  private final Map<Mode,Set<UserClass>> userClassesPerMode;

  /** Populate the demands configuration's time periods
   * 
   * @param demands to populate XML with
   * @param xmlDemandConfiguration to use
   */  
  private void populateXmlTimePeriods(Demands demands, final XMLElementDemandConfiguration xmlDemandConfiguration) {
    if(demands.timePeriods== null || demands.timePeriods.isEmpty()) {
      LOGGER.severe("No time periods available on demands, this shouldn't happen");
      return;
    }
        
    var xmlTimePeriods = new XMLElementTimePeriods();
    xmlDemandConfiguration.setTimeperiods(xmlTimePeriods );
    demands.timePeriods.streamSortedBy(getPrimaryIdMapper().getTimePeriodIdMapper()).forEach(timePeriod -> {
      var xmlTimePeriod = new XMLElementTimePeriods.Timeperiod();
      xmlTimePeriods.getTimeperiod().add(xmlTimePeriod);
      
      /* XML id */
      xmlTimePeriod.setId(getPrimaryIdMapper().getTimePeriodIdMapper().apply(timePeriod));
      
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
          xmlTimePeriod.setStarttime(LocalTime.ofSecondOfDay(timePeriod.getStartTimeSeconds()));
        } catch (Exception e) {
          LOGGER.severe(e.getMessage());
          throw new PlanItRunTimeException("Error when generating start time of time period "+ timePeriod.getXmlId()+" when persisting demand configuration",e);
        }  
      }      
                
      /* duration */
      if(timePeriod.getDurationSeconds()<=0) {
        throw new PlanItRunTimeException("Error duration of time period %s  is not positive, this is not allowed", timePeriod.getXmlId());
      }
      var xmlDuration = new XMLElementDuration();
      xmlDuration.setUnit(Durationunit.S); // TODO: ideally we keep the original unit so input and output files are consistent
      xmlDuration.setValue(BigInteger.valueOf(timePeriod.getDurationSeconds()));
      xmlTimePeriod.setDuration(xmlDuration);           
    });
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
    demands.userClasses.streamSortedBy(getPrimaryIdMapper().getUserClassIdMapper()).forEach( userClass -> {
      var xmlUserClass = new XMLElementUserClasses.Userclass();
      xmlUserClasses.getUserclass().add(xmlUserClass);
      
      /* XML id */
      xmlUserClass.setId(getPrimaryIdMapper().getUserClassIdMapper().apply(userClass));
      
      /* external id */
      if(userClass.hasExternalId()) {
        xmlUserClass.setExternalid(userClass.getExternalId());
      }
      
      /* mode ref */
      if(userClass.getMode()==null) {
        LOGGER.warning(String.format("User class %s has no referenced mode", userClass.getXmlId()));
      }else {
        xmlUserClass.setModeref(getXmlModeReference(userClass.getMode(), getComponentIdMappers().getNetworkIdMappers().getModeIdMapper()));
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
        xmlUserClass.setTravellertyperef(getPrimaryIdMapper().getTravellerTypeIdMapper().apply(userClass.getTravelerType()));
      }      
      
      /* name */
      if(userClass.hasName()) {
        xmlUserClass.setName(userClass.getName());
      }      
    });
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
    demands.travelerTypes.streamSortedBy(getPrimaryIdMapper().getTravellerTypeIdMapper()).forEach(travellerType -> {
      var xmlTravellerType = new XMLElementTravellerTypes.Travellertype();
      xmlTravellerTypes.getTravellertype().add(xmlTravellerType);
      
      /* XML id */
      xmlTravellerType.setId(getPrimaryIdMapper().getTravellerTypeIdMapper().apply(travellerType));
      
      /* external id */
      if(travellerType.hasExternalId()) {
        xmlTravellerType.setExternalid(travellerType.getExternalId());
      }
      
      /* name */
      if(travellerType.hasName()) {
        xmlTravellerType.setName(travellerType.getName());
      }      
    });
    
  }

  /** Populate a single OdDemands entry for a given mode and time period in the XML odrow format
   * 
   * @param odDemandsEntry to populate XML with
   * @param timePeriod  used
   * @param userClass used
   * @param xmlOdDemandsEntry to populate
   * @return totalTrips in veh/h in this od demand entry
   */
  private double populateXmlOdRowMatrix(final OdDemands odDemandsEntry, TimePeriod timePeriod, UserClass userClass, final XMLElementOdRowMatrix xmlOdDemandsEntry) {
    
    /* time period ref */
    String timePeriodRef = getPrimaryIdMapper().getTimePeriodIdMapper().apply(timePeriod);
    xmlOdDemandsEntry.setTimeperiodref(timePeriodRef);
    
    /* user class ref */
    String userClassRef = getPrimaryIdMapper().getUserClassIdMapper().apply(userClass);
    xmlOdDemandsEntry.setUserclassref(userClassRef);

    /* destination separator */
    xmlOdDemandsEntry.setDs(settings.getDestinationSeparator());

    var xmlOdRowsList = xmlOdDemandsEntry.getOdrow();

    final DoubleAdder totalTripDemandVehH = new DoubleAdder();
    final var zoneIdMapper = getComponentIdMappers().getZoningIdMappers().getZoneIdMapper();
    /* stream sorted by the ordering used for persisting zones */
    this.referenceZoning.getOdZones().streamSortedBy(zoneIdMapper).forEach( originZone -> {

      /* odrow */
      var xmlOdRow = new XMLElementOdRowMatrix.Odrow();

      /* (origin zoning) ref */
      xmlOdRow.setRef(zoneIdMapper.apply(originZone));

      final var sb = new StringBuilder();
      this.referenceZoning.getOdZones().streamSortedBy(zoneIdMapper).forEach( destinationZone -> {

        /* convert back to veh/h from PcuH */
        double valueVehH = odDemandsEntry.getValue(originZone, destinationZone)/userClass.getMode().getPcu();
        totalTripDemandVehH.add(valueVehH);
        sb.append(settings.getDecimalFormat().format(valueVehH));
        sb.append(settings.getDestinationSeparator());
      });

      xmlOdRow.setValue(sb.toString());
      xmlOdRowsList.add(xmlOdRow);

    });

    return totalTripDemandVehH.doubleValue();
  }

  /** Populate the actual OD Demands
   * 
   * @param demands to extract from
   */
  private void populateXmlOdDemands(Demands demands) {
    var xmlOdDemands = new XMLElementOdDemands();
    xmlRawDemands.setOddemands(xmlOdDemands);
    
    demands.timePeriods.streamSortedBy(getPrimaryIdMapper().getTimePeriodIdMapper()).forEach( timePeriod -> {
      final var modes = demands.getRegisteredModesForTimePeriod(timePeriod);
      modes.stream().sorted(Comparator.comparing(getComponentIdMappers().getNetworkIdMappers().getModeIdMapper())).forEach( mode -> {
        var odDemandsEntry = demands.get(mode, timePeriod);
        if(odDemandsEntry != null) {
          //TODO: we do not yet preserve the type of matrix used in input, so we use row by row because the raw matrix currently has problems given
          //      that it relies on internal ids, which may or may not be mapped to the XML id causing the user to not be able to determine
          //      what cell the values correspond to. By using the row matrix, the order of the rows and the ref value, allows inferring the col refs even
          //      though they are not explicitly provided as the amtrix is symmetrical
          var xmlOdDemandEntryMatrix = new XMLElementOdRowMatrix();
          xmlOdDemands.getOdcellbycellmatrixOrOdrowmatrixOrOdrawmatrix().add(xmlOdDemandEntryMatrix);
          
          if(userClassesPerMode.containsKey(mode) && userClassesPerMode.get(mode).size()>1) {
            //TODO: od matrices are stored per mode, not per user class (in memory), but XML format defines them per user class, so unless they are all defined
            // 1:1 we do not properly support this yet
            throw new PlanItRunTimeException("PLANit demands writer does not yet support multiple user-classes per mode");
          }
          
          var userClass = userClassesPerMode.get(mode).iterator().next();
          double odDemandVehH = populateXmlOdRowMatrix(odDemandsEntry, timePeriod, userClass, xmlOdDemandEntryMatrix);
          LOGGER.info(String.format("OD demands matrix: total trips %.2f (veh/h)  %.2f pcu factor , timePeriod: %s, user-class %s",odDemandVehH, userClass.getMode().getPcu(), timePeriod.toString(), userClass.toString()));
        }
      });
    });
  }

  /** Populate the demands configuration
   * 
   * @param demands to populate XML with
   */
  private void populateXmlDemandConfiguration(Demands demands) {
    
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
   * @param settings to use
   * @param xmlRawDemands to populate and persist
   */
  protected PlanitDemandsWriter(final PlanitDemandsWriterSettings settings, final XMLElementMacroscopicDemand xmlRawDemands) {
    this(settings, null, xmlRawDemands);
  }

  /** Constructor 
   * 
   * @param settings to use
   * @param referenceZoning to use
   * @param xmlRawDemands to populate and persist
   */
  protected PlanitDemandsWriter(final PlanitDemandsWriterSettings settings, final Zoning referenceZoning, final XMLElementMacroscopicDemand xmlRawDemands) {
    super(IdMapperType.XML);
    this.settings = settings;
    this.referenceZoning = referenceZoning;
    this.xmlRawDemands = xmlRawDemands;
    this.userClassesPerMode = new HashMap<>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DemandsIdMapper getPrimaryIdMapper() {
    return getComponentIdMappers().getDemandsIdMapperIdMapper();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final Demands demands) throws PlanItException {    
    PlanItException.throwIfNull(demands, "Demands is null cannot write to PLANit native format");

    if(!getSettings().validate()){
      LOGGER.severe("Unable to continue PLANit writing of demands, settings invalid");
      return;
    }
    
    /* initialise */
    {
      getComponentIdMappers().populateMissingIdMappers(getIdMapperType());
      LOGGER.info(String.format("Persisting PLANit demands to: %s", Paths.get(getSettings().getOutputDirectory(), getSettings().getFileName()).toString()));
    }
    
    getSettings().logSettings();
    
    /* xml id */
    populateXmlId(demands);
        
    /* configuration */
    populateXmlDemandConfiguration(demands);
    
    /* oddemands */
    populateXmlOdDemands(demands);
        
    /* persist */
    super.persist(xmlRawDemands, XMLElementMacroscopicDemand.class, PlanitSchema.MACROSCOPIC_ZONING_XSD);
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


  /**
   * {@inheritDoc}
   */
  @Override
  public void setReferenceZoning(Zoning referenceZoning) {
    this.referenceZoning = referenceZoning;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Zoning getReferenceZoning() {
    return this.referenceZoning;
  }
}
