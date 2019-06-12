package org.planit.xml.demands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.planit.demand.Demands;
import org.planit.demand.MatrixDemand;
import org.planit.exceptions.PlanItException;
import org.planit.generated.Odcellbycellmatrix;
import org.planit.generated.Odmatrix;
import org.planit.generated.Odrawmatrix;
import org.planit.generated.Odrowmatrix;
import org.planit.generated.Odrawmatrix.Values;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.userclass.UserClass;
import org.planit.xml.input.PlanItXml;
import org.planit.zoning.Zoning.Zones;

/**
 * This class contains methods to update the Demands object using input values
 * from the XML demands input file.
 * 
 * @author gman6028
 *
 */
public class UpdateDemands {

	private static final Logger LOGGER = Logger.getLogger(UpdateDemands.class.getName());
	private static List<String> RESERVED_CHARACTERS;

	static {
		String[] reservedCharacters = { "+", "*", "^" };
		RESERVED_CHARACTERS = Arrays.asList(reservedCharacters);
	}

	/**
	 * Convert regular expression special characters to act like simple strings
	 * during String.split() calls
	 * 
	 * @param separator raw String separator
	 * @return String separator with escape characters added if appropriate
	 */
	private static String escapeSeparator(String separator) {
		if (RESERVED_CHARACTERS.contains(separator)) {
			return "\\" + separator;
		}
		return separator;
	}

	/**
	 * Creates a MatrixDemand object from a List of Odmatrix objects read in from
	 * the XML input file and registers it in the Demands object
	 * 
	 * @param demands       the PlanIt Demands object to be populated
	 * @param oddemands     List of generated Odmatrix objects with data from the
	 *                      XML input file
	 * @param modeMap       Map of Mode objects
	 * @param timePeriodMap Map of TimePeriod objects
	 * @param noCentroids   the number of centroids in the current zoning
	 * @param zones         zones in the current network
	 * @throws Exception thrown if there is an error during processing
	 */
	public static void createAndRegisterDemandMatrix(Demands demands, List<Odmatrix> oddemands,
			Map<Integer, Mode> modeMap, Map<Integer, TimePeriod> timePeriodMap, int noCentroids, Zones zones)
			throws Exception {
		Map<Mode, Map<TimePeriod, MatrixDemand>> demandsPerTimePeriodAndMode = initializeDemandsPerTimePeriodAndMode(
				timePeriodMap, modeMap, noCentroids);
		for (Odmatrix odmatrix : oddemands) {
			int timePeriodId = odmatrix.getTimeperiodref().intValue();
			int userClassId = (odmatrix.getUserclassref() == null) ? UserClass.DEFAULT_EXTERNAL_ID
					: odmatrix.getUserclassref().intValue();
			int externalId = (int) UserClass.getById(userClassId).getModeExternalId();
			Mode mode = modeMap.get(externalId);
			TimePeriod timePeriod = timePeriodMap.get(timePeriodId);
			MatrixDemand demandMatrix = demandsPerTimePeriodAndMode.get(mode).get(timePeriod);
			updateDemandMatrixFromOdMatrix(odmatrix, mode.getPcu(), demandMatrix, zones);
			demands.registerODDemand(timePeriod, mode, demandMatrix);
		}
	}

	/**
	 * Create the Map of demands for each time period and mode
	 * 
	 * @param timePeriodMap Map of time periods
	 * @param modeMap       Map of Mode objects
	 * @param noCentroids   the number of centroids in the current zoning
	 * @return empty Map of demands for each time period
	 * @throws PlanItException thrown if there is an error
	 */
	private static Map<Mode, Map<TimePeriod, MatrixDemand>> initializeDemandsPerTimePeriodAndMode(
			Map<Integer, TimePeriod> timePeriodMap, Map<Integer, Mode> modeMap, int noCentroids)
			throws PlanItException {
		Map<Mode, Map<TimePeriod, MatrixDemand>> demandsPerTimePeriodAndMode = new HashMap<Mode, Map<TimePeriod, MatrixDemand>>();
		for (Mode mode : modeMap.values()) {
			Map<TimePeriod, MatrixDemand> demandsPerTimePeriod = new HashMap<TimePeriod, MatrixDemand>();
			for (TimePeriod timePeriod : timePeriodMap.values()) {
				demandsPerTimePeriod.put(timePeriod, new MatrixDemand(noCentroids));
			}
			demandsPerTimePeriodAndMode.put(mode, demandsPerTimePeriod);
		}
		return demandsPerTimePeriodAndMode;
	}

	/**
	 * Update the demand matrix object from a generated OD matrix
	 * 
	 * @param odmatrix     Odmatrix object generated from the input XML
	 * @param pcu          number of PCUs for current mode of travel
	 * @param matrixDemand MatrixDemand object to be updated
	 * @param zones        zones in the current network
	 * @throws Exception thrown if there is an error during processing
	 */
	private static void updateDemandMatrixFromOdMatrix(Odmatrix odmatrix, double pcu, MatrixDemand matrixDemand,
			Zones zones) throws Exception {
		if (odmatrix instanceof Odcellbycellmatrix) {
			updateDemandMatrixFromCellByCellMatrix((Odcellbycellmatrix) odmatrix, pcu, matrixDemand, zones);
		} else if (odmatrix instanceof Odrowmatrix) {
			updateDemandMatrixFromOdRowMatrix((Odrowmatrix) odmatrix, pcu, matrixDemand, zones);
		} else if (odmatrix instanceof Odrawmatrix) {
			updateDemandMatrixFromOdRawMatrix((Odrawmatrix) odmatrix, pcu, matrixDemand, zones);
		}
	}

	/**
	 * Update the demand matrix object from a generated cell by cell matrix
	 * 
	 * @param odcellbycellmatrix Odcellbycellmatrix object generated from the input
	 *                           XML
	 * @param pcu                number of PCUs for current mode of travel
	 * @param matrixDemand       MatrixDemand object to be updated
	 * @param zones              zones in the current network
	 */
	private static void updateDemandMatrixFromCellByCellMatrix(Odcellbycellmatrix odcellbycellmatrix, double pcu,
			MatrixDemand matrixDemand, Zones zones) {
		List<Odcellbycellmatrix.O> o = odcellbycellmatrix.getO();
		for (Odcellbycellmatrix.O originZone : o) {
			List<Odcellbycellmatrix.O.D> d = originZone.getD();
			for (Odcellbycellmatrix.O.D demandZone : d) {
				updateMatrixDemand(matrixDemand, originZone.getRef().intValue(), demandZone.getRef().intValue(), pcu,
						demandZone.getValue(), zones);
			}
		}
	}

	/**
	 * Update the demand matrix object from a generated OD row matrix
	 * 
	 * @param odrowmatrix  Odrowmatrix object generated from the input XML
	 * @param pcu          number of PCUs for current mode of travel
	 * @param matrixDemand MatrixDemand object to be updated
	 * @param zones        zones in the current network
	 */
	private static void updateDemandMatrixFromOdRowMatrix(Odrowmatrix odrowmatrix, double pcu,
			MatrixDemand matrixDemand, Zones zones) {
		String separator = (odrowmatrix.getDs() == null) ? PlanItXml.DEFAULT_SEPARATOR : odrowmatrix.getDs();
		separator = escapeSeparator(separator);
		List<Odrowmatrix.Odrow> odrow = odrowmatrix.getOdrow();
		for (Odrowmatrix.Odrow originZone : odrow) {
			String[] rowValuesAsString = originZone.getValue().split(separator);
			for (int i = 0; i < rowValuesAsString.length; i++) {
				updateMatrixDemand(matrixDemand, originZone.getRef().intValue(), i + 1, pcu,
						Double.parseDouble(rowValuesAsString[i]), zones);
			}
		}
	}

	/**
	 * Update the demand matrix object from a generated OD raw matrix
	 * 
	 * @param odrawmatrix  Odrawmatrix object generated from the input XML
	 * @param pcu          number of PCUs for current mode of travel
	 * @param matrixDemand MatrixDemand object to be updated
	 * @param zones        zones in the current network
	 * @throws Exception thrown if the Odrawmatrix cannot be parsed into a square
	 *                   matrix
	 */
	private static void updateDemandMatrixFromOdRawMatrix(Odrawmatrix odrawmatrix, double pcu,
			MatrixDemand matrixDemand, Zones zones) throws Exception {
		Values values = odrawmatrix.getValues();
		String originSeparator = (values.getOs() == null) ? PlanItXml.DEFAULT_SEPARATOR : values.getOs();
		originSeparator = escapeSeparator(originSeparator);
		String destinationSeparator = (values.getDs() == null) ? PlanItXml.DEFAULT_SEPARATOR : values.getDs();
		destinationSeparator = escapeSeparator(destinationSeparator);
		if (originSeparator.equals(destinationSeparator)) {
			updateDemandMatrixForEqualSeparators(values, originSeparator, pcu, matrixDemand, zones);
		} else {
			updateDemandMatrixForDifferentSeparators(values, originSeparator, destinationSeparator, pcu, matrixDemand,
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
	 * @param matrixDemand         MatrixDemand object to be updated
	 * @param zones                zones in the current network
	 * @throws Exception thrown if the Odrawmatrix cannot be parsed into a square
	 *                   matrix
	 */
	private static void updateDemandMatrixForDifferentSeparators(Values values, String originSeparator,
			String destinationSeparator, double pcu, MatrixDemand matrixDemand, Zones zones) throws Exception {
		String[] originRows = values.getValue().split(originSeparator);
		int noRows = originRows.length;
		for (int i = 0; i < noRows; i++) {
			String[] allValuesAsString = originRows[i].split(destinationSeparator);
			int noCols = allValuesAsString.length;
			if (noRows != noCols) {
				throw new Exception("Element <odrawmatrix> does not parse to a square matrix: Row " + (i + 1) + " has "
						+ noCols + " values.");
			}
			for (int col = 0; col < noCols; col++) {
				updateMatrixDemand(matrixDemand, i + 1, col + 1, pcu, Double.parseDouble(allValuesAsString[col]),
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
	 * @param matrixDemand MatrixDemand object to be updated
	 * @param zones        zones in the current network
	 * @throws Exception thrown if the Odrawmatrix cannot be parsed into a square
	 *                   matrix
	 */
	private static void updateDemandMatrixForEqualSeparators(Values values, String separator, double pcu,
			MatrixDemand matrixDemand, Zones zones) throws Exception {
		String[] allValuesAsString = values.getValue().split(separator);
		int size = allValuesAsString.length;
		int noRows = (int) Math.round(Math.sqrt(size));
		if ((noRows * noRows) != size) {
			throw new Exception(
					"Element <odrawmatrix> contains a string of " + size + " values, which is not an exact square");
		}
		int noCols = noRows;
		for (int i = 0; i < noRows; i++) {
			int row = i * noRows;
			for (int col = 0; col < noCols; col++) {
				updateMatrixDemand(matrixDemand, i + 1, col + 1, pcu, Double.parseDouble(allValuesAsString[row + col]),
						zones);
			}
		}
	}

	/**
	 * Update the demand matrix object with the input value for the current row and
	 * column
	 * 
	 * @param matrixDemand the MatrixDemand object to be updated
	 * @param rowRef       reference to the row (origin) for the current demand
	 *                     value
	 * @param colRef       reference to the column (destination) for the current
	 *                     demand value
	 * @param pcu          number of PCUs for current mode of travel
	 * @param demandValue  current demand value (in PCU)
	 * @param zones        zones in the current network
	 */
	private static void updateMatrixDemand(MatrixDemand matrixDemand, int rowRef, int colRef, double pcu,
			double demandValue, Zones zones) {
		long originZoneId = zones.getZoneByExternalId(rowRef).getId();
		long destinationZoneId = zones.getZoneByExternalId(colRef).getId();
		double demand = demandValue * pcu;
		matrixDemand.set(originZoneId, destinationZoneId, demand);
	}

}
