package org.planit.io.demands;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.planit.demands.Demands;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.io.xml.util.PlanitXmlReader;
import org.planit.network.macroscopic.MacroscopicNetwork;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.utils.time.TimePeriod;
import org.planit.userclass.TravelerType;
import org.planit.userclass.UserClass;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.Zone;
import org.planit.utils.zoning.Zones;
import org.planit.xml.generated.Durationunit;
import org.planit.xml.generated.XMLElementDemandConfiguration;
import org.planit.xml.generated.XMLElementMacroscopicDemand;
import org.planit.xml.generated.XMLElementOdCellByCellMatrix;
import org.planit.xml.generated.XMLElementOdMatrix;
import org.planit.xml.generated.XMLElementOdRawMatrix;
import org.planit.xml.generated.XMLElementOdRowMatrix;
import org.planit.xml.generated.XMLElementTimePeriods;
import org.planit.xml.generated.XMLElementTravellerTypes;
import org.planit.xml.generated.XMLElementUserClasses;
import org.planit.xml.generated.XMLElementOdRawMatrix.Values;
import org.planit.zoning.Zoning;

public class PlanitDemandsReader extends PlanitXmlReader<XMLElementMacroscopicDemand>{

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(PlanitDemandsReader.class.getCanonicalName());
  
  private static final List<String> RESERVED_CHARACTERS = Arrays.asList(new String[]{"+", "*", "^"});  
  
  /**
   * Generate default traveller type if none defined in XML files
   * 
   * @return default XMLElementTravellerTypes instance
   */
  private static XMLElementTravellerTypes.Travellertype generateDefaultXMLTravellerType() {
    XMLElementTravellerTypes.Travellertype xmlTravellerType = new XMLElementTravellerTypes.Travellertype();
    xmlTravellerType.setId(TravelerType.DEFAULT_XML_ID);
    xmlTravellerType.setName(TravelerType.DEFAULT_NAME);
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
    xmlUserclass.setTravellertyperef(TravelerType.DEFAULT_XML_ID);
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
      final double pcu, ODDemandMatrix odDemandMatrix, final Zones<?> zones) throws PlanItException {
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
      final String destinationSeparator, final double pcu, ODDemandMatrix odDemandMatrix, final Zones<OdZone> zones) throws PlanItException {
    
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
   * Generate TravelerType objects from generated configuration object and store them
   * 
   * @param demandconfiguration to extract from
   * @throws PlanItException
   */
  private void generateAndStoreTravelerTypes(XMLElementDemandConfiguration demandconfiguration) throws PlanItException {
    
    /* traveller types */
    XMLElementTravellerTypes xmlTravellertypes = 
        (demandconfiguration.getTravellertypes() == null) ? new XMLElementTravellerTypes() : demandconfiguration.getTravellertypes();
    if (xmlTravellertypes.getTravellertype().isEmpty()) {
      xmlTravellertypes.getTravellertype().add(generateDefaultXMLTravellerType());
    }
    
    /* for each traveller type */
    for (XMLElementTravellerTypes.Travellertype xmlTravellertype : xmlTravellertypes.getTravellertype()) {
            
      /* PLANit traveller type */
      TravelerType travelerType = demands.travelerTypes.createAndRegisterNewTravelerType(xmlTravellertype.getName());
      
      /* xml id */
      if(xmlTravellertype.getId() != null && !xmlTravellertype.getId().isBlank()) {
        travelerType.setXmlId(xmlTravellertype.getId());
      }            
      
      /* external id */
      if(xmlTravellertype.getExternalid() != null && !xmlTravellertype.getExternalid().isBlank()) {
        travelerType.setExternalId(xmlTravellertype.getExternalid());
      }      
      
      final TravelerType duplicateTravelerType = settings.getMapToIndexTravelerTypeByXmlIds().put(travelerType.getXmlId(), travelerType);
      PlanItException.throwIf(duplicateTravelerType!=null, "duplicate traveler type xml id " + travelerType.getXmlId() + " found in demands");
    }
  }
  
  /**
   * Generate XMLElementUserClasses objects from generated configuration object and store them
   * 
   * @param demandconfiguration generated XMLElementDemandConfiguration object from demand XML input
   * @param network the network
   * @param sourceIdModeMap available modes by XML id
   * @return the number of user classes
   * @throws PlanItException thrown if a duplicate external Id key is found
   */
  private int generateAndStoreUserClasses( XMLElementDemandConfiguration demandconfiguration, MacroscopicNetwork network, Map<String, Mode> sourceIdModeMap) throws PlanItException {

    /* user classes */
    XMLElementUserClasses xmlUserclasses = (demandconfiguration.getUserclasses() == null) ? new XMLElementUserClasses() : demandconfiguration.getUserclasses();
    
    /* generate default if absent (and no more than one mode is used) */
    if (xmlUserclasses.getUserclass().isEmpty()) {
      PlanItException.throwIf(network.modes.size() > 1,"user classes must be explicitly defined when more than one mode is defined");
      PlanItException.throwIf(demands.travelerTypes.getNumberOfTravelerTypes() > 1, "user classes must be explicitly defined when more than one traveller type is defined");
      
      XMLElementUserClasses.Userclass xmlUserClass = generateDefaultUserClass();
      xmlUserClass.setTravellertyperef(demands.travelerTypes.getFirst().getXmlId());
      xmlUserclasses.getUserclass().add(xmlUserClass);
    }
    
    /* USER CLASS */
    for (XMLElementUserClasses.Userclass xmlUserclass : xmlUserclasses.getUserclass()) {
      if(xmlUserclass.getTravellertyperef()==null) {
        PlanItException.throwIf(demands.travelerTypes.getNumberOfTravelerTypes() > 1,
            String.format("User class %s has no traveller type specified, but more than one traveller type possible",xmlUserclass.getId()));                
      }else {
        PlanItException.throwIf(settings.getMapToIndexTravelerTypeByXmlIds().get(xmlUserclass.getTravellertyperef()) == null, 
            "travellertyperef value of " + xmlUserclass.getTravellertyperef() + " referenced by user class " + xmlUserclass.getName() + " but not defined");
      }
      PlanItException.throwIf(xmlUserclass.getModeref() == null, "User class " + xmlUserclass.getId() + " has no mode specified, but more than one mode possible");
      
      /* mode ref */
      if (xmlUserclass.getModeref() == null) {
        PlanItException.throwIf(network.modes.size() > 1, "User class " + xmlUserclass.getId() + " has no mode specified, but more than one mode possible");                
        xmlUserclass.setModeref(sourceIdModeMap.keySet().iterator().next());          
      }
      String xmlModeIdRef = xmlUserclass.getModeref();
      Mode userClassMode = sourceIdModeMap.get(xmlModeIdRef);
      PlanItException.throwIf(userClassMode == null,"User class " + xmlUserclass.getId() + " refers to mode " + xmlModeIdRef + " which has not been defined");
           
      /* traveller type ref */
      String travellerTypeXmlIdRef = (xmlUserclass.getTravellertyperef() == null) ? TravelerType.DEFAULT_XML_ID : xmlUserclass.getTravellertyperef();
      xmlUserclass.setTravellertyperef(travellerTypeXmlIdRef);
      TravelerType travellerType = settings.getMapToIndexTravelerTypeByXmlIds().get(travellerTypeXmlIdRef);
                 
      UserClass userClass = demands.userClasses.createAndRegisterNewUserClass(xmlUserclass.getName(), userClassMode, travellerType);
      
      /* xml id */
      if(xmlUserclass.getId() != null && !xmlUserclass.getId().isBlank()) {
        userClass.setXmlId(xmlUserclass.getId());
      }              
      
      /* external id */
      if(xmlUserclass.getExternalid() != null && !xmlUserclass.getExternalid().isBlank()) {
        userClass.setExternalId(xmlUserclass.getExternalid());
      }        
      
      final UserClass duplicateUserClass = settings.getMapToIndexUserClassByXmlIds().put(userClass.getXmlId(), userClass);
      PlanItException.throwIf(duplicateUserClass!=null, "duplicate user class xml id " + userClass.getXmlId() + " found in demands");
    }
    return xmlUserclasses.getUserclass().size();
  }  
  
  /**
   * Generate a Map of TimePeriod objects from generated configuration object
   * 
   * @param demandconfiguration generated XMLElementDemandConfiguration object from demand XML input
   * @throws PlanItException thrown if a duplicate external Id is found, or if there is an
   */
  private void generateTimePeriodMap(XMLElementDemandConfiguration demandconfiguration) throws PlanItException {
    
    /* time periods */
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
      
      /* PLANit time period */
      TimePeriod timePeriod = demands.timePeriods.createAndRegisterNewTimePeriod(xmlTimePeriod.getName(), startTimeSeconds, duration /*converted to seconds*/);
      
      /* xml id */
      if(xmlTimePeriod.getId() != null && !xmlTimePeriod.getId().isBlank()) {
        timePeriod.setXmlId(xmlTimePeriod.getId());
      }      
      
      /* external id */
      if(xmlTimePeriod.getExternalid() != null && !xmlTimePeriod.getExternalid().isBlank()) {
        timePeriod.setExternalId(xmlTimePeriod.getExternalid());
      }         
      
      final TimePeriod duplicateTimePeriod = settings.getMapToIndexTimePeriodByXmlIds().put(timePeriod.getXmlId(), timePeriod);
      PlanItException.throwIf(duplicateTimePeriod!=null, "duplicate time period xml id " + timePeriod.getXmlId() + " found in demands");
    }
  }  
  
  
  /**
   * Update the demand matrix object from a generated OD matrix
   *
   * @param xmlOdMatrix XMLElementOdMatrix object generated from the input XML
   * @param pcu number of PCUs for current mode of travel
   * @param odDemandMatrix ODDemandMatrix object to be updated
   * @param zones to collect zone instances from when needed
   * @param xmlIdZoneMap to obtain zones by xml id
   * @throws Exception thrown if there is an error during processing
   */
  private static void populateDemandMatrix(final XMLElementOdMatrix xmlOdMatrix, final double pcu, ODDemandMatrix odDemandMatrix, Zones<OdZone> zones, Map<String, Zone> xmlIdZoneMap) throws PlanItException {
    
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
  
  /** set the demands to populate
   * @param demands to populate
   */
  protected void setDemands(Demands demands) {
    this.demands = demands;
  }
  
  /**
   * Sets up all the configuration data from the XML demands file
   * @param network these demands pertain to
   * @param sourceIdModeMap to obtain modes by Xml id
   * 
   * @throws PlanItException thrown if there is a duplicate XML Id found for any component
   */
  protected void populateDemandConfiguration(MacroscopicNetwork network, Map<String, Mode> sourceIdModeMap) throws PlanItException {
    
    /* configuration element */
    final XMLElementDemandConfiguration demandconfiguration = getXmlRootElement().getDemandconfiguration();
    
    generateAndStoreTravelerTypes(demandconfiguration);
    generateAndStoreUserClasses(demandconfiguration, network, sourceIdModeMap);
    generateTimePeriodMap(demandconfiguration);
  }
  
  /**
   * parses the demand contents of the Xml
   * @param zoning to relate the demands to
   * @param xmlIdZoneMap to obtain zones by xml id
   * @throws PlanItException thrown if error 
   */
  protected void populateDemandContents(Zoning zoning, Map<String, Zone> xmlIdZoneMap) throws PlanItException {
    final List<XMLElementOdMatrix> oddemands = getXmlRootElement().getOddemands().getOdcellbycellmatrixOrOdrowmatrixOrOdrawmatrix();
    
    //final Map<Mode, Map<TimePeriod, ODDemandMatrix>> demandsPerTimePeriodAndMode = initializeDemandsPerTimePeriodAndMode(demands, zones, inputBuilderListener);
    
    /* od matrix */
    for (final XMLElementOdMatrix xmlOdMatrix : oddemands) {
      
      /* user class ref */
      UserClass userClass = null;  
      if(xmlOdMatrix.getUserclassref() == null) {
        PlanItException.throwIf(demands.userClasses.size()>1,"user class must be explicitly set on od matrix when more than one user class exists");
        userClass = demands.userClasses.getFirst();
      }else {
        final String userClassXmlIdRef = xmlOdMatrix.getUserclassref();
        userClass = settings.getMapToIndexUserClassByXmlIds().get(userClassXmlIdRef);        
      }
      PlanItException.throwIf(userClass==null, "referenced user class on od matrix not available");
      final Mode mode = userClass.getMode();
      
      /* time period ref */
      final String timePeriodXmlIdRef = xmlOdMatrix.getTimeperiodref();
      PlanItException.throwIf(timePeriodXmlIdRef==null, "time period must always be referenced on od matrix");
      final TimePeriod timePeriod = settings.getMapToIndexTimePeriodByXmlIds().get(timePeriodXmlIdRef);
      PlanItException.throwIf(timePeriod==null, "referenced time period on od matrix not available");
      
      /* create od matrix instance */
      ODDemandMatrix odDemandMatrix = new ODDemandMatrix(zoning.odZones);
      /* populate */
      populateDemandMatrix(xmlOdMatrix, mode.getPcu(), odDemandMatrix, zoning.odZones, xmlIdZoneMap);
      /* register */
      ODDemandMatrix duplicate = demands.registerODDemand(timePeriod, mode, odDemandMatrix);
      if(duplicate != null) {
        throw new PlanItException(String.format("multiple OD demand matrix encountered for mode-time period combination %s:%s this is not allowed",mode.getXmlId(), timePeriod.getXmlId()));
      }
    }
  }  
  

  /** constructor
   * 
   * @param pathDirectory to use
   * @param xmlFileExtension to use
   * @param demands to populate
   * @throws PlanItException  thrown if error
   */
  public PlanitDemandsReader(String pathDirectory, String xmlFileExtension, Demands demands) throws PlanItException{   
    super(XMLElementMacroscopicDemand.class);
    settings.setInputPathDirectory(pathDirectory);
    settings.setXmlFileExtension(xmlFileExtension);
    setDemands(demands);
  }
  
  /** constructor where file has already been parsed and we only need to convert from raw XML objects to PLANit memory model
   * 
   * @param xmlMacroscopicDemands to extract from
   * @param demands to populate
   * @throws PlanItException  thrown if error
   */
  public PlanitDemandsReader(XMLElementMacroscopicDemand xmlMacroscopicDemands, Demands demands) throws PlanItException{
    super(xmlMacroscopicDemands);    
    setDemands(demands);
  }

  /** parse the Xml and populate the demands memory model
   * @param network to utilise
   * @param zoning to utilise
   * @param xmlIdModeMap to obtain available modes by Xml id
   * @param xmlIdZoneMap to obtain zones by Xml id
   * @throws PlanItException thrown if error
   */
  public void read(MacroscopicNetwork network, Zoning zoning, Map<String, Mode> xmlIdModeMap, Map<String, Zone> xmlIdZoneMap) throws PlanItException {
    
    try {
      
      initialiseAndParseXmlRootElement(settings.getInputPathDirectory(), settings.getXmlFileExtension());
      
      /* configuration */
      populateDemandConfiguration(network, xmlIdModeMap);
      
      /* demands */
      populateDemandContents(zoning, xmlIdZoneMap);
      
      /* free */
      clearXmlContent();

    } catch (final Exception e) {
      LOGGER.severe(e.getMessage());
      throw new PlanItException("Error when populating demands in PLANitIO",e);
    }
  } 
  
  /** settings for this reader
   * @return settings
   */
  public PlanitDemandsReaderSettings getSettings() {
    return settings;
  }
}
