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
 * @author gman6028
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
	  Demands demands,
      final Zones zones, 
      final InputBuilderListener inputBuilderListener) throws PlanItException {
    Map<Mode, Map<TimePeriod, ODDemandMatrix>> demandsPerTimePeriodAndMode =
        new HashMap<Mode, Map<TimePeriod, ODDemandMatrix>>();
    for (final Entry<String, Mode> modeEntry : inputBuilderListener.getAllModesByXmlId().entrySet()) {
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
        final Zone originZone = inputBuilderListener.getZoneByXmlId(xmlOriginZone.getRef());        
        for (final XMLElementOdCellByCellMatrix.O.D xmlDestinationZone : xmlOriginZone.getD()) {
          final Zone destinationZone = inputBuilderListener.getZoneByXmlId(xmlDestinationZone.getRef());
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
        final Zone originZone = inputBuilderListener.getZoneByXmlId(xmlOriginZone.getRef());
        final String[] rowValuesAsString = xmlOriginZone.getValue().split(separator);
        for (int i = 0; i < rowValuesAsString.length; i++) {
          /* use internal id's, i.e. order of appearance of the zone elements in XML is used */ 
          final Zone destinationZone = zones.getZoneById(i);
          final double demand = Double.parseDouble(rowValuesAsString[i]) * pcu;
          odDemandMatrix.setValue(originZone, destinationZone, demand);          
        }
      }      
      
    } else if (xmlOdMatrix instanceof XMLElementOdRawMatrix) {
      
      //TODO: continue here, remove methods called and instead put in this method similar to above
      //      remove the methods called afterwards
      updateDemandMatrixFromOdRawMatrix((XMLElementOdRawMatrix) xmlOdMatrix, pcu, odDemandMatrix, inputBuilderListener);
    }
    
    
  }

  /**
   * Update the demand matrix object from a generated OD raw matrix
   *
   * @param odrawmatrix XMLElementOdRawMatrix object generated from the input XML
   * @param pcu number of PCUs for current mode of travel
   * @param odDemandMatrix ODDemandMatrix object to be updated
   * @param inputBuilderListener InputBuilderListener containing zones by external Id
   * @throws Exception thrown if the Odrawmatrix cannot be parsed into a square matrix
   */
  private static void updateDemandMatrixFromOdRawMatrix(final XMLElementOdRawMatrix odrawmatrix, final double pcu,
      ODDemandMatrix odDemandMatrix, final InputBuilderListener inputBuilderListener) throws Exception {
    final Values values = odrawmatrix.getValues();
    String originSeparator = (values.getOs() == null) ? PlanItInputBuilder.DEFAULT_SEPARATOR : values.getOs();
    originSeparator = escapeSeparator(originSeparator);
    String destinationSeparator = (values.getDs() == null) ? PlanItInputBuilder.DEFAULT_SEPARATOR
        : values.getDs();
    destinationSeparator = escapeSeparator(destinationSeparator);
    if (originSeparator.equals(destinationSeparator)) {
      updateDemandMatrixForEqualSeparators(values, originSeparator, pcu, odDemandMatrix, inputBuilderListener);
    } else {
      updateDemandMatrixForDifferentSeparators(values, originSeparator, destinationSeparator, pcu, odDemandMatrix,
          inputBuilderListener);
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
   * @param inputBuilderListener InputBuilderListener containing zones by external Id
   * @throws Exception thrown if the Odrawmatrix cannot be parsed into a square
   *           matrix
   */
  private static void updateDemandMatrixForDifferentSeparators(final Values values, final String originSeparator,
      final String destinationSeparator, final double pcu, ODDemandMatrix odDemandMatrix,
      final InputBuilderListener inputBuilderListener) throws Exception {
    final String[] originRows = values.getValue().split(originSeparator);
    final int noRows = originRows.length;
    for (int i = 0; i < noRows; i++) {
      final String[] allValuesAsString = originRows[i].split(destinationSeparator);
      final int noCols = allValuesAsString.length;
      if (noRows != noCols) {
        throw new Exception("Element <odrawmatrix> does not parse to a square matrix: Row " + (i + 1) + " has "
            + noCols + " values.");
      }
      for (int col = 0; col < noCols; col++) {
        updateDemandMatrix(odDemandMatrix, i + 1, col + 1, pcu, Double.parseDouble(allValuesAsString[col]),
            inputBuilderListener);
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
   * @param inputBuilderListener InputBuilderListener containing zones by external Id
   * @throws Exception thrown if the Odrawmatrix cannot be parsed into a square
   *           matrix
   */
  private static void updateDemandMatrixForEqualSeparators(final Values values, final String separator,
      final double pcu,
      ODDemandMatrix odDemandMatrix, final InputBuilderListener inputBuilderListener) throws Exception {
    final String[] allValuesAsString = values.getValue().split(separator);
    final int size = allValuesAsString.length;
    final int noRows = (int) Math.round(Math.sqrt(size));
    if ((noRows * noRows) != size) {
      throw new Exception("Element <odrawmatrix> contains a string of " + size
          + " values, which is not an exact square");
    }
    final int noCols = noRows;
    for (int i = 0; i < noRows; i++) {
      final int row = i * noRows;
      for (int col = 0; col < noCols; col++) {
        updateDemandMatrix(odDemandMatrix, i + 1, col + 1, pcu, Double.parseDouble(allValuesAsString[row + col]),
            inputBuilderListener);
      }
    }
  }

  /**
   * Update the demand matrix object with the input value for the current row and
   * column
   *
   * @param odDemandMatrix the ODDemandMatrix object to be updated
   * @param rowRef reference to the row (origin) for the current demand value by the xml id
   * @param colRef reference to the column (destination) for the current demand value by the xml id
   * @param pcu number of PCUs for current mode of travel 
   * @param demandValue current demand value (in PCU)
   * @param inputBuilderListener InputBuilderListener containing zones by external Id
   */
  private static void updateDemandMatrix(ODDemandMatrix odDemandMatrix, final String rowRef, final String colRef,
      final double pcu,
      final double demandValue, 
      final InputBuilderListener inputBuilderListener) {
    final Zone originZone = inputBuilderListener.getZoneByXmlId(rowRef);
    final Zone destinationZone = inputBuilderListener.getZoneByXmlId(colRef);
    final double demand = demandValue * pcu;
    odDemandMatrix.setValue(originZone, destinationZone, demand);
  }  

  /**
   * Creates a ODDemandMatrix object from a List of Odmatrix objects read in from
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
 
    final Map<Mode, Map<TimePeriod, ODDemandMatrix>> demandsPerTimePeriodAndMode = 
    		initializeDemandsPerTimePeriodAndMode(demands, zones, inputBuilderListener);
    for (final XMLElementOdMatrix odmatrix : oddemands) {
      final long timePeriodId = odmatrix.getTimeperiodref().longValue();
      final long userClassXmlId = (odmatrix.getUserclassref() == null) ? UserClass.DEFAULT_XML_ID
          : odmatrix.getUserclassref().longValue();
      final UserClass userClass = inputBuilderListener.getUserClassByXmlId((long) userClassXmlId);
      final Mode mode = userClass.getMode();
      final TimePeriod timePeriod = inputBuilderListener.getTimePeriodByXmlId(timePeriodId);
      ODDemandMatrix odDemandMatrix = demandsPerTimePeriodAndMode.get(mode).get(timePeriod);
      populateDemandMatrix(odmatrix, mode.getPcu(), odDemandMatrix, zones, inputBuilderListener);
      demands.registerODDemand(timePeriod, mode, odDemandMatrix);
    }
  }

}