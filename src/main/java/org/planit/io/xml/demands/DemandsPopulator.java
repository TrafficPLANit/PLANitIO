package org.planit.io.xml.demands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.planit.demands.Demands;
import org.planit.xml.generated.XMLElementOdCellByCellMatrix;
import org.planit.xml.generated.XMLElementOdMatrix;
import org.planit.xml.generated.XMLElementOdRawMatrix;
import org.planit.xml.generated.XMLElementOdRawMatrix.Values;
import org.planit.xml.generated.XMLElementOdRowMatrix;
import org.planit.input.InputBuilderListener;
import org.planit.io.input.PlanItInputBuilder;
import org.planit.network.virtual.Zoning.Zones;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.time.TimePeriod;
import org.planit.userclass.UserClass;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.virtual.Zone;

/**
 * This class contains methods to populate the Demands object using input values
 * from the XML demands input file.
 *
 * @author gman6028, markr
 *
 */
public class DemandsPopulator {

  private static final List<String> RESERVED_CHARACTERS = Arrays.asList(new String[]{"+", "*", "^"});

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(DemandsPopulator.class.getCanonicalName());   
  
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
   * Create the Map of demands for each time period and mode
   *
   * @param zones store of registered zones 
   * @param inputBuilderListener InputBuilderListener containing list of registered modes
   * @return Map of demands for each time period
   * @throws PlanItException thrown if there is an error
   */
  private static Map<Mode, Map<TimePeriod, ODDemandMatrix>> initializeDemandsPerTimePeriodAndMode(
	  Demands demands, final Zones zones, final InputBuilderListener inputBuilderListener) throws PlanItException {
    
    Map<Mode, Map<TimePeriod, ODDemandMatrix>> demandsPerTimePeriodAndMode = new HashMap<Mode, Map<TimePeriod, ODDemandMatrix>>();
    for (final Entry<String, Mode> modeEntry : inputBuilderListener.getAllModesBySourceId().entrySet()) {
      final Map<TimePeriod, ODDemandMatrix> demandsPerTimePeriod = new HashMap<TimePeriod, ODDemandMatrix>();
      for (final TimePeriod timePeriod : demands.timePeriods.asSortedSetByStartTime()) {
        demandsPerTimePeriod.put(timePeriod, new ODDemandMatrix(zones));
      }
      demandsPerTimePeriodAndMode.put(modeEntry.getValue(), demandsPerTimePeriod);
    }
    return demandsPerTimePeriodAndMode;
  }

  /**
   * Update the demand matrix object from a generated OD matrix
   *
   * @param xmlOdMatrix XMLElementOdMatrix object generated from the input XML
   * @param pcu number of PCUs for current mode of travel
   * @param odDemandMatrix ODDemandMatrix object to be updated
   * @param zones to collect zone instances from when needed
   * @param inputBuilderListener InputBuilderListener containing zones by external Id
   * @throws Exception thrown if there is an error during processing
   */
  private static void populateDemandMatrix(final XMLElementOdMatrix xmlOdMatrix, final double pcu,
      ODDemandMatrix odDemandMatrix, Zones zones, final InputBuilderListener inputBuilderListener) throws Exception {
    
    if (xmlOdMatrix instanceof XMLElementOdCellByCellMatrix) {
      
      /* cell-by-cell matrix */
      final List<XMLElementOdCellByCellMatrix.O> o = ((XMLElementOdCellByCellMatrix) xmlOdMatrix).getO();
      for (final XMLElementOdCellByCellMatrix.O xmlOriginZone : o) {
        final Zone originZone = inputBuilderListener.getZoneBySourceId(xmlOriginZone.getRef());        
        for (final XMLElementOdCellByCellMatrix.O.D xmlDestinationZone : xmlOriginZone.getD()) {
          final Zone destinationZone = inputBuilderListener.getZoneBySourceId(xmlDestinationZone.getRef());
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
        final Zone originZone = inputBuilderListener.getZoneBySourceId(xmlOriginZone.getRef());
        final String[] rowValuesAsString = xmlOriginZone.getValue().split(separator);
        for (int i = 0; i < rowValuesAsString.length; i++) {
          /* use internal id's, i.e. order of appearance of the zone elements in XML is used */ 
          final Zone destinationZone = zones.getZoneById(i);
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
      final String destinationSeparator, final double pcu, ODDemandMatrix odDemandMatrix, final Zones zones) throws PlanItException {
    
    final String[] originRows = values.getValue().split(originSeparator);
    final int noRows = originRows.length;
    for (int i = 0; i < noRows; i++) {
      final Zone originZone = zones.getZoneById(i);
      final String[] destinationValuesByOrigin = originRows[i].split(destinationSeparator);
      final int noCols = destinationValuesByOrigin.length;
      if (noRows != noCols) {
        throw new PlanItException("Element <odrawmatrix> does not parse to a square matrix: Row " + (i + 1) + " has " + noCols + " values.");
      }
      for (int col = 0; col < noCols; col++) {
        final Zone destinationZone = zones.getZoneById(col);
        final double rawDemand = Double.parseDouble(destinationValuesByOrigin[col]);
        final double demand = rawDemand * pcu;
        odDemandMatrix.setValue(originZone, destinationZone, demand);        
      }
    }
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
      final double pcu, ODDemandMatrix odDemandMatrix, final Zones zones) throws PlanItException {
    final String[] allValuesAsString = values.getValue().split(separator);
    final int size = allValuesAsString.length;
    final int noRows = (int) Math.round(Math.sqrt(size));
    if ((noRows * noRows) != size) {
      throw new PlanItException("Element <odrawmatrix> contains a string of " + size + " values, which is not an exact square");
    }
    final int noCols = noRows;
    for (int i = 0; i < noRows; i++) {
      final int rowOffset = i * noRows;
      final Zone originZone = zones.getZoneById(i);
      for (int col = 0; col < noCols; col++) {
        final Zone destinationZone = zones.getZoneById(col);
        final double rawDemand = Double.parseDouble(allValuesAsString[rowOffset + col]);
        final double demand = rawDemand * pcu;
        odDemandMatrix.setValue(originZone, destinationZone, demand);
      }
    }
  }

  /**
   * Creates an ODDemandMatrix object from a List of Odmatrix objects read in from
   * the XML input file and registers it in the Demands object
   *
   * @param demands the PlanIt Demands object to be populated
   * @param oddemands List of generated XMLElementOdMatrix objects with data from the XML input file
   * @param zones zones in the current network
   * @param inputBuilderListener InputBuilderListener containing zones by external Id
   * @throws Exception thrown if there is an error during processing
   */
  public static void createAndRegisterDemandMatrix(
      final Demands demands,
      final List<XMLElementOdMatrix> oddemands,
      final Zones zones,
      final InputBuilderListener inputBuilderListener) throws Exception {
 
    final Map<Mode, Map<TimePeriod, ODDemandMatrix>> demandsPerTimePeriodAndMode = initializeDemandsPerTimePeriodAndMode(demands, zones, inputBuilderListener);
    /* od matrix */
    for (final XMLElementOdMatrix xmlOdMatrix : oddemands) {
      /* refs */
      final String timePeriodXmlIdRef = xmlOdMatrix.getTimeperiodref();
      final String userClassXmlIdRef = (xmlOdMatrix.getUserclassref() == null) ? UserClass.DEFAULT_XML_ID : xmlOdMatrix.getUserclassref();
      final UserClass userClass = inputBuilderListener.getUserClassBySourceId(userClassXmlIdRef);
      final Mode mode = userClass.getMode();
      final TimePeriod timePeriod = inputBuilderListener.getTimePeriodBySourceId(timePeriodXmlIdRef);
      /* populate */
      ODDemandMatrix odDemandMatrix = demandsPerTimePeriodAndMode.get(mode).get(timePeriod);
      populateDemandMatrix(xmlOdMatrix, mode.getPcu(), odDemandMatrix, zones, inputBuilderListener);
      /* register */
      demands.registerODDemand(timePeriod, mode, odDemandMatrix);
    }
  }

}