package org.planit.planitio.xml.demands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.planit.demands.Demands;
import org.planit.exceptions.PlanItException;
import org.planit.generated.XMLElementOdCellByCellMatrix;
import org.planit.generated.XMLElementOdMatrix;
import org.planit.generated.XMLElementOdRawMatrix;
import org.planit.generated.XMLElementOdRawMatrix.Values;
import org.planit.generated.XMLElementOdRowMatrix;
import org.planit.network.virtual.Zoning.Zones;
import org.planit.od.odmatrix.demand.ODDemandMatrix;
import org.planit.planitio.input.PlanItInputBuilder;
import org.planit.time.TimePeriod;
import org.planit.userclass.UserClass;
import org.planit.utils.network.physical.Mode;
import org.planit.utils.network.virtual.Zone;

/**
 * This class contains methods to update the Demands object using input values
 * from the XML demands input file.
 *
 * @author gman6028
 *
 */
public class UpdateDemands {

	private static List<String> RESERVED_CHARACTERS;

	static {
		final String[] reservedCharacters = { "+", "*", "^" };
		RESERVED_CHARACTERS = Arrays.asList(reservedCharacters);
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
	 * Create the Map of demands for each time period and mode
	 *
	 * @param timePeriodMap Map of time periods
	 * @param modesByExternalIdMap map of available modes by external id
	 * @param noCentroids   the number of centroids in the current zoning
	 * @return empty Map of demands for each time period
	 * @throws PlanItException thrown if there is an error
	 */
	private static Map<Mode, Map<TimePeriod, ODDemandMatrix>> initializeDemandsPerTimePeriodAndMode(
			final Map<Integer, TimePeriod> timePeriodMap, final Zones zones, final Map<Long, Mode> modesByExternalIdMap) 	throws PlanItException {
		final Map<Mode, Map<TimePeriod, ODDemandMatrix>> demandsPerTimePeriodAndMode = new HashMap<Mode, Map<TimePeriod, ODDemandMatrix>>();
		for (final Mode mode : modesByExternalIdMap.values()) {
			final Map<TimePeriod, ODDemandMatrix> demandsPerTimePeriod = new HashMap<TimePeriod, ODDemandMatrix>();
			for (final TimePeriod timePeriod : timePeriodMap.values()) {
				demandsPerTimePeriod.put(timePeriod, new ODDemandMatrix(zones));
			}
			demandsPerTimePeriodAndMode.put(mode, demandsPerTimePeriod);
		}
		return demandsPerTimePeriodAndMode;
	}

	/**
	 * Update the demand matrix object from a generated OD matrix
	 *
	 * @param odmatrix    XMLElementOdMatrix object generated from the input XML
	 * @param pcu          number of PCUs for current mode of travel
	 * @param odDemandMatrix ODDemandMatrix object to be updated
	 * @param zones        zones in the current network
	 * @throws Exception thrown if there is an error during processing
	 */
	private static void updateDemandMatrixFromOdMatrix(final XMLElementOdMatrix odmatrix, final double pcu,
			final ODDemandMatrix odDemandMatrix, final Zones zones) throws Exception {
		if (odmatrix instanceof XMLElementOdCellByCellMatrix) {
			updateDemandMatrixFromCellByCellMatrix((XMLElementOdCellByCellMatrix) odmatrix, pcu, odDemandMatrix, zones);
		} else if (odmatrix instanceof XMLElementOdRowMatrix) {
			updateDemandMatrixFromOdRowMatrix((XMLElementOdRowMatrix) odmatrix, pcu, odDemandMatrix, zones);
		} else if (odmatrix instanceof XMLElementOdRawMatrix) {
			updateDemandMatrixFromOdRawMatrix((XMLElementOdRawMatrix) odmatrix, pcu, odDemandMatrix, zones);
		}
	}

	/**
	 * Update the demand matrix object from a generated cell by cell matrix
	 *
	 * @param odcellbycellmatrix XMLElementOdCellByCellMatrix object generated from the input
	 *                           XML
	 * @param pcu                number of PCUs for current mode of travel
	 * @param odDemandMatrix       ODDemandMatrix object to be updated
	 * @param zones              zones in the current network
	 */
	private static void updateDemandMatrixFromCellByCellMatrix(final XMLElementOdCellByCellMatrix odcellbycellmatrix,
			final double pcu, final ODDemandMatrix odDemandMatrix, final Zones zones) {
		final List<XMLElementOdCellByCellMatrix.O> o = odcellbycellmatrix.getO();
		for (final XMLElementOdCellByCellMatrix.O originZone : o) {
			final List<XMLElementOdCellByCellMatrix.O.D> d = originZone.getD();
			for (final XMLElementOdCellByCellMatrix.O.D demandZone : d) {
				updateDemandMatrix(odDemandMatrix, originZone.getRef().intValue(), demandZone.getRef().intValue(), pcu,
						demandZone.getValue(), zones);
			}
		}
	}

	/**
	 * Update the demand matrix object from a generated OD row matrix
	 *
	 * @param odrowmatrix  XMLElementOdRowMatrix object generated from the input XML
	 * @param pcu          number of PCUs for current mode of travel
	 * @param odDemandMatrix ODDemandMatrix object to be updated
	 * @param zones        zones in the current network
	 */
	private static void updateDemandMatrixFromOdRowMatrix(final XMLElementOdRowMatrix odrowmatrix, final double pcu,
			final ODDemandMatrix odDemandMatrix, final Zones zones) {
		String separator = (odrowmatrix.getDs() == null) ? PlanItInputBuilder.DEFAULT_SEPARATOR
				: odrowmatrix.getDs();
		separator = escapeSeparator(separator);
		final List<XMLElementOdRowMatrix.Odrow> odrow = odrowmatrix.getOdrow();
		for (final XMLElementOdRowMatrix.Odrow originZone : odrow) {
			final String[] rowValuesAsString = originZone.getValue().split(separator);
			for (int i = 0; i < rowValuesAsString.length; i++) {
				updateDemandMatrix(odDemandMatrix, originZone.getRef().intValue(), i + 1, pcu,
						Double.parseDouble(rowValuesAsString[i]), zones);
			}
		}
	}

	/**
	 * Update the demand matrix object from a generated OD raw matrix
	 *
	 * @param odrawmatrix  XMLElementOdRawMatrix object generated from the input XML
	 * @param pcu          number of PCUs for current mode of travel
	 * @param odDemandMatrix ODDemandMatrix object to be updated
	 * @param zones        zones in the current network
	 * @throws Exception thrown if the Odrawmatrix cannot be parsed into a square
	 *                   matrix
	 */
	private static void updateDemandMatrixFromOdRawMatrix(final XMLElementOdRawMatrix odrawmatrix, final double pcu,
			final ODDemandMatrix odDemandMatrix, final Zones zones) throws Exception {
		final Values values = odrawmatrix.getValues();
		String originSeparator = (values.getOs() == null) ? PlanItInputBuilder.DEFAULT_SEPARATOR : values.getOs();
		originSeparator = escapeSeparator(originSeparator);
		String destinationSeparator = (values.getDs() == null) ? PlanItInputBuilder.DEFAULT_SEPARATOR
				: values.getDs();
		destinationSeparator = escapeSeparator(destinationSeparator);
		if (originSeparator.equals(destinationSeparator)) {
			updateDemandMatrixForEqualSeparators(values, originSeparator, pcu, odDemandMatrix, zones);
		} else {
			updateDemandMatrixForDifferentSeparators(values, originSeparator, destinationSeparator, pcu, odDemandMatrix,
					zones);
		}
	}

	/**
	 * Update the demand matrix object from a generated OD raw matrix when origin
	 * and destination separators are different
	 *
	 * @param values               Values object generated from the input XML
	 * @param originSeparator      origin separator character
	 * @param destinationSeparator destination separator character
	 * @param pcu                  number of PCUs for current mode of travel
	 * @param odDemandMatrix         ODDemandMatrix object to be updated
	 * @param zones                zones in the current network
	 * @throws Exception thrown if the Odrawmatrix cannot be parsed into a square
	 *                   matrix
	 */
	private static void updateDemandMatrixForDifferentSeparators(final Values values, final String originSeparator,
			final String destinationSeparator, final double pcu, final ODDemandMatrix odDemandMatrix, final Zones zones) throws Exception {
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
						zones);
			}
		}
	}

	/**
	 * Update the demand matrix object from a generated OD raw matrix when origin
	 * and destination separators are equal
	 *
	 * @param values       Values object generated from the input XML
	 * @param separator    separator character
	 * @param pcu          number of PCUs for current mode of travel
	 * @param odDemandMatrix ODDemandMatrix object to be updated
	 * @param zones        zones in the current network
	 * @throws Exception thrown if the Odrawmatrix cannot be parsed into a square
	 *                   matrix
	 */
	private static void updateDemandMatrixForEqualSeparators(final Values values, final String separator, final double pcu,
			final ODDemandMatrix odDemandMatrix, final Zones zones) throws Exception {
		final String[] allValuesAsString = values.getValue().split(separator);
		final int size = allValuesAsString.length;
		final int noRows = (int) Math.round(Math.sqrt(size));
		if ((noRows * noRows) != size) {
			throw new Exception("Element <odrawmatrix> contains a string of " + size + " values, which is not an exact square");
		}
		final int noCols = noRows;
		for (int i = 0; i < noRows; i++) {
			final int row = i * noRows;
			for (int col = 0; col < noCols; col++) {
				updateDemandMatrix(odDemandMatrix, i + 1, col + 1, pcu, Double.parseDouble(allValuesAsString[row + col]),
						zones);
			}
		}
	}

	/**
	 * Update the demand matrix object with the input value for the current row and
	 * column
	 *
	 * @param odDemandMatrix the ODDemandMatrix object to be updated
	 * @param rowRef       reference to the row (origin) for the current demand
	 *                     value
	 * @param colRef       reference to the column (destination) for the current
	 *                     demand value
	 * @param pcu          number of PCUs for current mode of travel
	 * @param demandValue  current demand value (in PCU)
	 * @param zones        zones in the current network
	 */
	private static void updateDemandMatrix(final ODDemandMatrix odDemandMatrix, final int rowRef, final int colRef, final double pcu,
			final double demandValue, final Zones zones) {
		final Zone originZone = zones.getZoneByExternalId(rowRef);
		final Zone destinationZone = zones.getZoneByExternalId(colRef);
		final double demand = demandValue * pcu;
		odDemandMatrix.setValue(originZone, destinationZone, demand);
	}

	/**
	 * Creates a ODDemandMatrix object from a List of Odmatrix objects read in from
	 * the XML input file and registers it in the Demands object
	 *
	 * @param demands       the PlanIt Demands object to be populated
	 * @param oddemands     List of generated XMLElementOdMatrix objects with data from the XML input file
	 * @param timePeriodMap Map of TimePeriod objects
	 * @param zones         zones in the current network
	 * @param modesByExternalIdMap available modes by external id
	 * @throws Exception thrown if there is an error during processing
	 */
	public static void createAndRegisterDemandMatrix(
			final Demands demands,
			final List<XMLElementOdMatrix> oddemands,
			final Map<Integer, TimePeriod> timePeriodMap,
			final Zones zones,
			final Map<Long, Mode> modesByExternalIdMap) 	throws Exception {

		final Map<Mode, Map<TimePeriod, ODDemandMatrix>> demandsPerTimePeriodAndMode =
				initializeDemandsPerTimePeriodAndMode(timePeriodMap, zones, modesByExternalIdMap);
		for (final XMLElementOdMatrix odmatrix : oddemands) {
			final int timePeriodId = odmatrix.getTimeperiodref().intValue();
			final int userClassExternalId = (odmatrix.getUserclassref() == null) ? UserClass.DEFAULT_EXTERNAL_ID
					: odmatrix.getUserclassref().intValue();
			final Mode mode = UserClass.getById(userClassExternalId).getMode();
			final TimePeriod timePeriod = timePeriodMap.get(timePeriodId);
			final ODDemandMatrix odDemandMatrix = demandsPerTimePeriodAndMode.get(mode).get(timePeriod);
			updateDemandMatrixFromOdMatrix(odmatrix, mode.getPcu(), odDemandMatrix, zones);
			demands.registerODDemand(timePeriod, mode, odDemandMatrix);
		}
	}

}
