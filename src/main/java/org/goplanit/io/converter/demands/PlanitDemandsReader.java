package org.goplanit.io.converter.demands;

import org.goplanit.converter.BaseReaderImpl;
import org.goplanit.converter.demands.DemandsReader;
import org.goplanit.demands.Demands;
import org.goplanit.demands.DemandsModifierUtils;
import org.goplanit.io.input.PlanItInputBuilder;
import org.goplanit.io.xml.util.PlanitXmlJaxbParser;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.od.demand.OdDemandMatrix;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.userclass.TravellerType;
import org.goplanit.userclass.UserClass;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.misc.LoggingUtils;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.time.TimePeriod;
import org.goplanit.utils.wrapper.MapWrapper;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.utils.zoning.Zones;
import org.goplanit.xml.generated.*;
import org.goplanit.xml.generated.XMLElementOdRawMatrix.Values;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.ZoningModifierUtils;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Reader to parse PLANit demands from native XML format
 * 
 * @author markr
 *
 */
public class PlanitDemandsReader extends BaseReaderImpl<Demands> implements DemandsReader {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitDemandsReader.class.getCanonicalName());
  
  /** list of reserved characters used */
  private static final List<String> RESERVED_CHARACTERS = Arrays.asList(new String[]{"+", "*", "^"});
  
  /** parses the xml content in JAXB memory format */
  private final PlanitXmlJaxbParser<XMLElementMacroscopicDemand> xmlParser;

  /**
   * Initialise event listeners in case we want to make changes to the XML ids after parsing is complete, e.g., if the parsed
   * demands is going to be modified and saved to disk afterwards, then it is advisable to sync all XML ids to the internal ids upon parsing
   * because this avoids the risk of generating duplicate XML ids during editing of the network (when XML ids are chosen to be synced to internal ids)
   */
  private void syncXmlIdsToIds() {
    LOGGER.info("Syncing PLANit demands XML ids to internally generated ids, overwriting original XML ids");
    DemandsModifierUtils.syncManagedIdEntitiesContainerXmlIdsToIds(demands);
  }
  
  /**
   * initialise the XML id trackers and populate them for the network and or zoning references, 
   * so we can lay indices on the XML id as well for quick lookups
   * 
   * @param network to use
   * @param zoning to use
   */
  private void initialiseParentXmlIdTrackers(MacroscopicNetwork network, Zoning zoning) {    
    initialiseSourceIdMap(Mode.class, Mode::getXmlId, network.getModes());
    
    initialiseSourceIdMap(Zone.class, Zone::getXmlId);
    getSourceIdContainer(Zone.class).addAll(zoning.getOdZones());
    getSourceIdContainer(Zone.class).addAll(zoning.getTransferZones());
  } 
  
  /**
   * initialise the XML id trackers of generated PLANit entit types so we can lay indices on the XML id as well for quick lookups
   * 
   */
  private void initialiseXmlIdTrackers() {    
    initialiseSourceIdMap(UserClass.class, UserClass::getXmlId);   
    initialiseSourceIdMap(TravellerType.class, TravellerType::getXmlId);
    initialiseSourceIdMap(TimePeriod.class, TimePeriod::getXmlId);
  }   
  
  /**
   * Generate default traveller type if none defined in XML files
   * 
   * @return default XMLElementTravellerTypes instance
   */
  private static XMLElementTravellerTypes.Travellertype generateDefaultXMLTravellerType() {
    XMLElementTravellerTypes.Travellertype xmlTravellerType = new XMLElementTravellerTypes.Travellertype();
    xmlTravellerType.setId(TravellerType.DEFAULT_XML_ID);
    xmlTravellerType.setName(TravellerType.DEFAULT_NAME);
    return xmlTravellerType;
  }  
  

  /**
   * Generate default user class if none defined in XML files
   * 
   * @return default XMLElementUserClasses object
   */
  private static XMLElementUserClasses.Userclass generateDefaultUserClass() {
    XMLElementUserClasses.Userclass xmlUserclass = new XMLElementUserClasses.Userclass();
    xmlUserclass.setName(UserClass.DEFAULT_NAME);
    xmlUserclass.setId(UserClass.DEFAULT_XML_ID);
    xmlUserclass.setModeref(Mode.DEFAULT_XML_ID);
    xmlUserclass.setTravellertyperef(TravellerType.DEFAULT_XML_ID);
    return xmlUserclass;
  }  
  
  /**
   * Convert regular expression special characters to act like simple strings
   * during String.split() calls
   *
   * @param separator raw String separator
   * @return String separator with escape characters added if appropriate
   */
  private static String escapeSeparator(final String separator) {
    if (RESERVED_CHARACTERS.contains(separator)) {
      return "\\" + separator;
    }
    return separator;
  }  
  
  /**
   * Update the demand matrix object from a generated OD raw matrix when origin
   * and destination separators are equal
   *
   * @param values Values object generated from the input XML
   * @param separator separator character
   * @param pcu number of PCUs for current mode of travel
   * @param odDemandMatrix ODDemandMatrix object to be updated
   * @param zones zones to collect by id (index position)
   * @throws Exception thrown if the Odrawmatrix cannot be parsed into a square matrix
   */
  private static void populateDemandMatrixRawForEqualSeparators(final Values values, final String separator,
      final double pcu, OdDemandMatrix odDemandMatrix, final Zones<?> zones) throws PlanItException {
    
    final String[] allValuesAsString = values.getValue().split(separator);
    final int size = allValuesAsString.length;
    final int noRows = (int) Math.round(Math.sqrt(size));
    if ((noRows * noRows) != size) {
      throw new PlanItException("Element <odrawmatrix> contains a string of " + size + " values, which is not an exact square");
    }
    final int noCols = noRows;
    for (int i = 0; i < noRows; i++) {
      final int rowOffset = i * noRows;
      final Zone originZone = zones.get(i);
      for (int col = 0; col < noCols; col++) {
        final Zone destinationZone = zones.get(col);
        final double rawDemand = Double.parseDouble(allValuesAsString[rowOffset + col]);
        final double demand = rawDemand * pcu;
        odDemandMatrix.setValue(originZone, destinationZone, demand);
      }
    }
    
  }  
  
  /**
   * Update the demand matrix object from a generated OD raw matrix when origin
   * and destination separators are different
   *
   * @param values Values object generated from the input XML
   * @param originSeparator origin separator character
   * @param destinationSeparator destination separator character
   * @param pcu number of PCUs for current mode of travel
   * @param odDemandMatrix ODDemandMatrix object to be updated
   * @param zones containing zones by Id (index)
   * @throws Exception thrown if the Odrawmatrix cannot be parsed into a square matrix
   */
  private static void populateDemandMatrixRawDifferentSeparators(final Values values, final String originSeparator,
      final String destinationSeparator, final double pcu, OdDemandMatrix odDemandMatrix, final Zones<OdZone> zones) throws PlanItException {
    
    final String[] originRows = values.getValue().split(originSeparator);
    final int noRows = originRows.length;
    for (int i = 0; i < noRows; i++) {
      final Zone originZone = zones.get(i);
      final String[] destinationValuesByOrigin = originRows[i].split(destinationSeparator);
      final int noCols = destinationValuesByOrigin.length;
      if (noRows != noCols) {
        throw new PlanItException("Element <odrawmatrix> does not parse to a square matrix: Row " + (i + 1) + " has " + noCols + " values.");
      }
      for (int col = 0; col < noCols; col++) {
        final Zone destinationZone = zones.get(col);
        final double rawDemand = Double.parseDouble(destinationValuesByOrigin[col]);
        final double demand = rawDemand * pcu;
        odDemandMatrix.setValue(originZone, destinationZone, demand);        
      }
    }
    
  }
  
  /**
   * Check if all required settings are indeed set by the user
   * 
   * @throws PlanItException thrown if error
   */
  private void validateSettings() throws PlanItException {
    PlanItException.throwIfNull(getSettings().getReferenceNetwork(),"Reference network is null for Planit demands reader");
    PlanItException.throwIfNull(getSettings().getReferenceZoning(),"Reference zoning is null for Planit demands reader");
  }  
  
  /**
   * Generate TravelerType objects from generated configuration object and store them
   * 
   * @param demandconfiguration to extract from
   * @throws PlanItException thrown if error
   */
  private void generateAndStoreTravelerTypes(final XMLElementDemandConfiguration demandconfiguration) throws PlanItException {
    
    /* traveller types */
    XMLElementTravellerTypes xmlTravellertypes = 
        (demandconfiguration.getTravellertypes() == null) ? new XMLElementTravellerTypes() : demandconfiguration.getTravellertypes();
    if (xmlTravellertypes.getTravellertype().isEmpty()) {
      xmlTravellertypes.getTravellertype().add(generateDefaultXMLTravellerType());
    }
    
    /* for each traveller type */
    for (XMLElementTravellerTypes.Travellertype xmlTravellertype : xmlTravellertypes.getTravellertype()) {
            
      /* PLANit traveller type */
      TravellerType travelerType = demands.travelerTypes.getFactory().registerNew(xmlTravellertype.getName());
      
      /* xml id */
      if(xmlTravellertype.getId() != null && !xmlTravellertype.getId().isBlank()) {
        travelerType.setXmlId(xmlTravellertype.getId());
      }            
      
      /* external id */
      if(xmlTravellertype.getExternalid() != null && !xmlTravellertype.getExternalid().isBlank()) {
        travelerType.setExternalId(xmlTravellertype.getExternalid());
      }      
            
      registerBySourceId(TravellerType.class, travelerType);      
    }
  }
  
  /**
   * Generate XMLElementUserClasses objects from generated configuration object and store them
   * 
   * @param demandconfiguration generated XMLElementDemandConfiguration object from demand XML input
   * @return the number of user classes
   * @throws PlanItException thrown if a duplicate external Id key is found
   */
  private int generateAndStoreUserClasses(final XMLElementDemandConfiguration demandconfiguration) throws PlanItException {

    /* user classes */
    XMLElementUserClasses xmlUserclasses = (demandconfiguration.getUserclasses() == null) ? new XMLElementUserClasses() : demandconfiguration.getUserclasses();
    
    /* generate default if absent (and no more than one mode is used) */
    if (xmlUserclasses.getUserclass().isEmpty()) {
      PlanItException.throwIf(getSettings().getReferenceNetwork().getModes().size() > 1,"user classes must be explicitly defined when more than one mode is defined");
      PlanItException.throwIf(demands.travelerTypes.size() > 1, "user classes must be explicitly defined when more than one traveller type is defined");
      
      XMLElementUserClasses.Userclass xmlUserClass = generateDefaultUserClass();
      xmlUserClass.setTravellertyperef(demands.travelerTypes.getFirst().getXmlId());
      xmlUserclasses.getUserclass().add(xmlUserClass);
    }
    
    /* USER CLASS */
    for (XMLElementUserClasses.Userclass xmlUserclass : xmlUserclasses.getUserclass()) {
      if(xmlUserclass.getTravellertyperef()==null) {
        PlanItException.throwIf(demands.travelerTypes.size() > 1,
            String.format("User class %s has no traveller type specified, but more than one traveller type possible",xmlUserclass.getId()));                
      }else {
        PlanItException.throwIf(getBySourceId(TravellerType.class, xmlUserclass.getTravellertyperef()) == null, 
            "travellertyperef value of " + xmlUserclass.getTravellertyperef() + " referenced by user class " + xmlUserclass.getName() + " but not defined");
      }
      PlanItException.throwIf(xmlUserclass.getModeref() == null, "User class %s has no mode specified, but more than one mode possible", xmlUserclass.getId() );
      
      /* mode ref */
      MapWrapper<?, Mode> modesByXmlId = getSourceIdContainer(Mode.class);      
      if (xmlUserclass.getModeref() == null) {
        PlanItException.throwIf(getSettings().getReferenceNetwork().getModes().size() > 1, "User class " + xmlUserclass.getId() + " has no mode specified, but more than one mode possible");                
        xmlUserclass.setModeref((String)modesByXmlId.getKeyByValue(modesByXmlId.getFirst()));          
      }
      String xmlModeIdRef = xmlUserclass.getModeref();
      Mode userClassMode = getBySourceId(Mode.class, xmlModeIdRef);
      PlanItException.throwIf(userClassMode == null,"User class %s refers to mode %s which has not been defined", xmlUserclass.getId(), xmlModeIdRef );
           
      /* traveller type ref */
      String travellerTypeXmlIdRef = (xmlUserclass.getTravellertyperef() == null) ? TravellerType.DEFAULT_XML_ID : xmlUserclass.getTravellertyperef();
      xmlUserclass.setTravellertyperef(travellerTypeXmlIdRef);
      TravellerType travellerType = getBySourceId(TravellerType.class, travellerTypeXmlIdRef);
                 
      UserClass userClass = demands.userClasses.getFactory().registerNew(xmlUserclass.getName(), userClassMode, travellerType);
      
      /* xml id */
      if(xmlUserclass.getId() != null && !xmlUserclass.getId().isBlank()) {
        userClass.setXmlId(xmlUserclass.getId());
      }              
      
      /* external id */
      if(xmlUserclass.getExternalid() != null && !xmlUserclass.getExternalid().isBlank()) {
        userClass.setExternalId(xmlUserclass.getExternalid());
      }        
      
      registerBySourceId(UserClass.class, userClass);
    }
    return xmlUserclasses.getUserclass().size();
  }  
  
  /**
   * Generate a Map of TimePeriod objects from generated configuration object
   * 
   * @param demandconfiguration generated XMLElementDemandConfiguration object from demand XML input
   * @throws PlanItException thrown if a duplicate external Id is found, or if there is an
   */
  private void generateTimePeriodMap(final XMLElementDemandConfiguration demandconfiguration) throws PlanItException {
    
    /* time periods */
    XMLElementTimePeriods xmlTimeperiods = demandconfiguration.getTimeperiods();

    LocalTime defaultStartTime = LocalTime.MIN;
    
    /* time period */
    for (XMLElementTimePeriods.Timeperiod xmlTimePeriod : xmlTimeperiods.getTimeperiod()) {

      /* starttime, duration */
      int startTimeSeconds = (xmlTimePeriod.getStarttime() == null) ? defaultStartTime.toSecondOfDay() : xmlTimePeriod.getStarttime().toSecondOfDay();
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
      
      /* PLANit time period */
      TimePeriod timePeriod = demands.timePeriods.getFactory().registerNew(xmlTimePeriod.getName(), startTimeSeconds, duration /*converted to seconds*/);
      
      /* xml id */
      if(xmlTimePeriod.getId() != null && !xmlTimePeriod.getId().isBlank()) {
        timePeriod.setXmlId(xmlTimePeriod.getId());
      }      
      
      /* external id */
      if(xmlTimePeriod.getExternalid() != null && !xmlTimePeriod.getExternalid().isBlank()) {
        timePeriod.setExternalId(xmlTimePeriod.getExternalid());
      }         
      
      registerBySourceId(TimePeriod.class, timePeriod);
    }
  }  
  
  
  /**
   * Update the demand matrix object from a generated OD matrix
   *
   * @param xmlOdMatrix XMLElementOdMatrix object generated from the input XML
   * @param pcu number of PCUs for current mode of travel
   * @param odDemandMatrix ODDemandMatrix object to be updated
   * @param zones to collect zone instances from when needed
   * @throws PlanItException thrown if there is an error during processing
   */
  private void populateDemandMatrix(
      final XMLElementOdMatrix xmlOdMatrix, final double pcu, OdDemandMatrix odDemandMatrix, Zones<OdZone> zones) throws PlanItException {
    
    @SuppressWarnings("unchecked")
    MapWrapper<String, Zone> xmlIdZoneMap = (MapWrapper<String,Zone>)getSourceIdContainer(Zone.class);
    if (xmlOdMatrix instanceof XMLElementOdCellByCellMatrix) {
      
      /* cell-by-cell matrix */
      final List<XMLElementOdCellByCellMatrix.O> o = ((XMLElementOdCellByCellMatrix) xmlOdMatrix).getO();
      for (final XMLElementOdCellByCellMatrix.O xmlOriginZone : o) {
        final Zone originZone = xmlIdZoneMap.get(xmlOriginZone.getRef());        
        for (final XMLElementOdCellByCellMatrix.O.D xmlDestinationZone : xmlOriginZone.getD()) {
          final Zone destinationZone = xmlIdZoneMap.get(xmlDestinationZone.getRef());
          final double demand = xmlDestinationZone.getValue() * pcu;
          odDemandMatrix.setValue(originZone, destinationZone, demand);                    
        }        
      }      
    } else if (xmlOdMatrix instanceof XMLElementOdRowMatrix) {
      
      /* od row matrix */
      XMLElementOdRowMatrix xmlOdRowMatrix = ((XMLElementOdRowMatrix) xmlOdMatrix);      
      String separator = (xmlOdRowMatrix.getDs() == null) ? PlanItInputBuilder.DEFAULT_SEPARATOR: xmlOdRowMatrix.getDs();
      separator = escapeSeparator(separator);
      final List<XMLElementOdRowMatrix.Odrow> xmlOdRow = xmlOdRowMatrix.getOdrow();
      for (final XMLElementOdRowMatrix.Odrow xmlOriginZone : xmlOdRow) {
        final Zone originZone = xmlIdZoneMap.get(xmlOriginZone.getRef());
        final String[] rowValuesAsString = xmlOriginZone.getValue().split(separator);
        for (int i = 0; i < rowValuesAsString.length; i++) {
          /* use internal id's, i.e. order of appearance of the zone elements in XML is used */ 
          final Zone destinationZone = zones.get(i);
          final double demand = Double.parseDouble(rowValuesAsString[i]) * pcu;
          odDemandMatrix.setValue(originZone, destinationZone, demand);          
        }
      }      
      
    } else if (xmlOdMatrix instanceof XMLElementOdRawMatrix) {
      
      /* raw matrix */
      final Values xmlValues = ((XMLElementOdRawMatrix) xmlOdMatrix).getValues();
      String originSeparator = (xmlValues.getOs() == null) ? PlanItInputBuilder.DEFAULT_SEPARATOR : xmlValues.getOs();
      originSeparator = escapeSeparator(originSeparator);
      String destinationSeparator = (xmlValues.getDs() == null) ? PlanItInputBuilder.DEFAULT_SEPARATOR: xmlValues.getDs();
      destinationSeparator = escapeSeparator(destinationSeparator);             
      
      if (originSeparator.equals(destinationSeparator)) {
        populateDemandMatrixRawForEqualSeparators(xmlValues, originSeparator, pcu, odDemandMatrix, zones);
      } else {
        populateDemandMatrixRawDifferentSeparators(xmlValues, originSeparator, destinationSeparator, pcu, odDemandMatrix, zones);
      }            
    }       
  }
  
  /** settings for the zoning reader */
  protected final PlanitDemandsReaderSettings settings = new PlanitDemandsReaderSettings();
  
  /** the demands to populate */
  protected Demands demands;
  
  /** Set the demands to populate
   * 
   * @param demands to populate
   */
  protected void setDemands(final Demands demands) {
    this.demands = demands;
  }
  
  /**
   * Sets up all the configuration data from the XML demands file
   * 
   * @throws PlanItException thrown if there is a duplicate XML Id found for any component
   */
  protected void populateDemandConfiguration() throws PlanItException {
    
    /* configuration element */
    final XMLElementDemandConfiguration demandconfiguration = xmlParser.getXmlRootElement().getDemandconfiguration();
    
    generateAndStoreTravelerTypes(demandconfiguration);
    generateAndStoreUserClasses(demandconfiguration);
    generateTimePeriodMap(demandconfiguration);
  }
  
  /**
   * Parses the demand contents of the XML
   * 
   * @throws PlanItException thrown if error 
   */
  protected void populateDemandContents() throws PlanItException {
    final List<XMLElementOdMatrix> oddemands = xmlParser.getXmlRootElement().getOddemands().getOdcellbycellmatrixOrOdrowmatrixOrOdrawmatrix();
        
    /* od matrix */
    for (final XMLElementOdMatrix xmlOdMatrix : oddemands) {
      
      /* user class ref */
      UserClass userClass = null;  
      if(xmlOdMatrix.getUserclassref() == null) {
        PlanItException.throwIf(demands.userClasses.size()>1,"user class must be explicitly set on od matrix when more than one user class exists");
        userClass = demands.userClasses.getFirst();
      }else {
        final String userClassXmlIdRef = xmlOdMatrix.getUserclassref();
        userClass = getBySourceId(UserClass.class, userClassXmlIdRef);        
      }
      PlanItException.throwIf(userClass==null, "referenced user class on od matrix not available");
      final Mode mode = userClass.getMode();
      
      /* time period ref */
      final String timePeriodXmlIdRef = xmlOdMatrix.getTimeperiodref();
      PlanItException.throwIf(timePeriodXmlIdRef==null, "time period must always be referenced on od matrix");
      final TimePeriod timePeriod = getBySourceId(TimePeriod.class, timePeriodXmlIdRef);
      PlanItException.throwIf(timePeriod==null, "referenced time period on od matrix not available");
      
      /* create od matrix instance */
      var odZones = getSettings().getReferenceZoning().getOdZones();
      OdDemandMatrix odDemandMatrix = new OdDemandMatrix(odZones);
      /* populate */
      populateDemandMatrix(xmlOdMatrix, mode.getPcu(), odDemandMatrix, odZones);
      /* register */
      OdDemands duplicate = demands.registerOdDemandPcuHour(timePeriod, mode, odDemandMatrix);
      if(duplicate != null) {
        throw new PlanItException(String.format("Multiple OD demand matrix encountered for mode-time period combination %s:%s this is not allowed",mode.getXmlId(), timePeriod.getXmlId()));
      }
    }
  }  
  
  /** Reference to demand schema location TODO: move to properties file*/
  public static final String DEMAND_XSD_FILE = "https://trafficplanit.github.io/PLANitManual/xsd/macroscopicdemandinput.xsd";  

  /** Constructor
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param demands to populate
   * @throws PlanItException  thrown if error
   */
  public PlanitDemandsReader(final String pathDirectory, final String xmlFileExtension, final Demands demands) throws PlanItException{
    this.xmlParser = new PlanitXmlJaxbParser<XMLElementMacroscopicDemand>(XMLElementMacroscopicDemand.class);
    getSettings().setInputDirectory(pathDirectory);
    getSettings().setXmlFileExtension(xmlFileExtension);
    setDemands(demands);
  }
  
  /** Constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlMacroscopicDemands to extract from
   * @param network reference network for the demands to read
   * @param zoning reference zoning for the demands to read 
   * @param demandsToPopulate to populate
   * @throws PlanItException  thrown if error
   */
  public PlanitDemandsReader(
      final XMLElementMacroscopicDemand xmlMacroscopicDemands, final MacroscopicNetwork network, final Zoning zoning, final Demands demandsToPopulate) throws PlanItException{
    this.xmlParser = new PlanitXmlJaxbParser<>(xmlMacroscopicDemands);
    setDemands(demandsToPopulate);
    getSettings().setReferenceNetwork(network); 
    getSettings().setReferenceZoning(zoning);
  }

  /** Parse the XMLand populate the demands memory model
   * 
   * @throws PlanItException thrown if error
   */
  @Override
  public Demands read() throws PlanItException {
    
    try {
      
      /* verify completeness of inputs */
      validateSettings();
            
      initialiseParentXmlIdTrackers(settings.getReferenceNetwork(), settings.getReferenceZoning());
      initialiseXmlIdTrackers();
      
      xmlParser.initialiseAndParseXmlRootElement(settings.getInputDirectory(), settings.getXmlFileExtension());
      
      /* xml id */
      String demandsXmlId = xmlParser.getXmlRootElement().getId();
      if(StringUtils.isNullOrBlank(demandsXmlId)) {
        LOGGER.warning(String.format("Demands has no XML id defined, adopting internally generated id %d instead",demands.getId()));
        demandsXmlId = String.valueOf(demands.getId());
      }
      demands.setXmlId(demandsXmlId);        
      
      /* configuration */
      populateDemandConfiguration();
      
      /* demands */
      populateDemandContents();

      if(getSettings().isSyncXmlIdsToIds()){
        syncXmlIdsToIds();
      }

      /* log stats */
      demands.logInfo(LoggingUtils.demandsPrefix(demands.getId()));
      
      /* free */
      xmlParser.clearXmlContent();           

    } catch (final Exception e) {
      e.printStackTrace();
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when populating demands in PLANitIO",e);
    }
    
    return demands;
  } 
  

  /**
   * {@inheritDoc}
   */
  @Override
  public PlanitDemandsReaderSettings getSettings() {
    return settings;
  }

  /**
   * {@inheritDoc}
   */  
  @Override
  public void reset() {
    settings.reset();
  }

}
