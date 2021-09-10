package org.planit.io.xml.converter;

import java.util.logging.Logger;

import org.planit.xml.generated.Typevalues;
import org.planit.xml.generated.Unitsvalues;
import org.planit.output.enums.DataType;
import org.planit.output.property.OutputProperty;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.unit.Unit;

/**
 * Utility methods to convert enumerations from the PLANit org.planit.output.enums package into enumerations generated from the output XSD file
 * 
 * @author gman6028
 *
 */
public interface EnumConverter {
  
  
  /** the logger */
  public static final Logger LOGGER = Logger.getLogger(EnumConverter.class.getCanonicalName());   

	/**
	 * Convert values from Type enumeration in PLANit project to generated Typevalues enumeration
	 * 
	 * @param type value of Type enumeration
	 * @return value of generated Typevalues enumeration
	 * @throws PlanItException thrown if a value of Type enumeration is not included in the XSD enumeration definition
	 */
	public static Typevalues convertFromPlanItToXmlGeneratedType(DataType type) throws PlanItException {
		switch (type) {
		case DOUBLE:
			return Typevalues.DOUBLE;
		case FLOAT:
			return Typevalues.FLOAT;
		case INTEGER:
			return Typevalues.INTEGER;
		case LONG:
			return Typevalues.INTEGER;
		case BOOLEAN:
			return Typevalues.BOOLEAN;
		case SRSNAME:
			return Typevalues.SRSNAME;
		case STRING:
			return Typevalues.STRING;
		default:
      throw new PlanItException("Data type " + type.value() + " has not been defined in the typevalues simple type in the output XSD file");
		}
	}

	/**
	 * Convert values from Units enumeration in PLANit project to generated Unitsvalues enumeration
	 * 
	 * @param outputProperty value of Units enumeration
	 * @return value of generated Unitsvalues enumeration
	 * @throws PlanItException thrown if a value of Units enumeration is not included in the XSD enumeration definition
	 */
	public static Unitsvalues convertFromPlanItToXmlGeneratedUnits(OutputProperty outputProperty) throws PlanItException {
		Unit outputPropertyUnit = outputProperty.getDefaultUnit();
	  if(outputProperty.supportsUnitOverride() && outputProperty.isUnitOverride()) {
	    outputPropertyUnit = outputProperty.getOverrideUnit();
		}
	  
	  if(outputPropertyUnit.equals(Unit.VEH_KM)){
			return Unitsvalues.VEH_KM;
		}else if(outputPropertyUnit.equals(Unit.PCU_KM)){
      return Unitsvalues.PCU_KM;	
    }else if(outputPropertyUnit.equals(Unit.NONE)){
			return Unitsvalues.NONE;
    }else if(outputPropertyUnit.equals(Unit.VEH_HOUR)){
			return Unitsvalues.VEH_H;
    }else if(outputPropertyUnit.equals(Unit.PCU_HOUR)) {
      return Unitsvalues.PCU_H;			
    }else if(outputPropertyUnit.equals(Unit.KM_HOUR)) {
			return Unitsvalues.KM_H;
    }else if(outputPropertyUnit.equals(Unit.HOUR)) {
			return Unitsvalues.H;
    }else if(outputPropertyUnit.equals(Unit.KM)) {
			return Unitsvalues.KM;
    }else if(outputPropertyUnit.equals(Unit.SRS)) {
			return Unitsvalues.SRS;
    }else{
      throw new PlanItException("Units type " + outputProperty.toString() + " has not been defined in the unitsvalues simple type in the output XSD file.");
		}
	}

}
