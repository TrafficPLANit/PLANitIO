package org.planit.xml.converter;

import org.planit.exceptions.PlanItException;
import org.planit.generated.Typevalues;
import org.planit.generated.Unitsvalues;
import org.planit.output.enums.Type;
import org.planit.output.enums.Units;

/**
 * Utility methods to convert enumerations from the PLANit org.planit.output.enums package into enumerations generated from the output XSD file
 * 
 * @author gman6028
 *
 */
public interface EnumConverter {

	/**
	 * Convert values from Type enumeration in PLANit project to generated Typevalues enumeration
	 * 
	 * @param type value of Type enumeration
	 * @return value of generated Typevalues enumeration
	 * @throws PlanItException thrown if a value of Type enumeration is not included in the XSD enumeration definition
	 */
	public static Typevalues convertFromPlanItToXmlGeneratedType(Type type) throws PlanItException {
		switch (type) {
		case DOUBLE:
			return Typevalues.DOUBLE;
		case FLOAT:
			return Typevalues.FLOAT;
		case INTEGER:
			return Typevalues.INTEGER;
		case BOOLEAN:
			return Typevalues.BOOLEAN;
		default:
			throw new PlanItException("Data type " + type.value()
					+ " has not been defined in the typevalues simple type in the output XSD file.");
		}
	}

	/**
	 * Convert values from Units enumeration in PLANit project to generated Unitsvalues enumeration
	 * 
	 * @param type value of Units enumeration
	 * @return value of generated Unitsvalues enumeration
	 * @throws PlanItException thrown if a value of Units enumeration is not included in the XSD enumeration definition
	 */
	public static Unitsvalues convertFromPlanItToXmlGeneratedUnits(Units units) throws PlanItException {
		switch (units) {
		case VEH_KM:
			return Unitsvalues.VEH_KM;
		case NONE:
			return Unitsvalues.NONE;
		case VEH_H:
			return Unitsvalues.VEH_H;
		case KM_H:
			return Unitsvalues.KM_H;
		case H:
			return Unitsvalues.H;
		case KM:
			return Unitsvalues.KM;
		default:
			throw new PlanItException("Units type " + units.value()
					+ " has not been defined in the unitsvalues simple type in the output XSD file.");
		}
	}

}
