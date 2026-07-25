package org.goplanit.io.xml.util;

import org.goplanit.demands.discrete.util.DirectionBound;
import org.goplanit.output.enums.DataType;
import org.goplanit.output.property.OutputProperty;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.mode.MotorisationModeType;
import org.goplanit.utils.mode.TrackModeType;
import org.goplanit.utils.mode.UseOfModeType;
import org.goplanit.utils.mode.VehicularModeType;
import org.goplanit.utils.unit.Unit;
import org.goplanit.xml.generated.v2.*;

/**
 * Some methods to convert the XML schema enums to PLANit memory model enums
 * 
 * @author markr
 *
 */
public class XmlEnumConversionUtil {

  /**
   * Dummy constructor as never instantiated
   */
  private XmlEnumConversionUtil() {
    // compliance to avoid javadoc warnings
  }
  
  /** convert motorisation type from xml to PLANit
   * @param xmlMotorisationType to convert
   * @return result
   */
  public static MotorisationModeType xmlToPlanit(final MotorisationType xmlMotorisationType) {
    
    switch (xmlMotorisationType) {
    case MOTORISED:
      return MotorisationModeType.MOTORISED;
    case NON_MOTORISED:
      return MotorisationModeType.NON_MOTORISED;      
    default:
      throw new PlanItRunTimeException(String.format("mapping from xml motorisation type %s to PLANit motorisation " +
              "type unavailable",xmlMotorisationType.toString()));
    }
  }
  
  /** convert motorisation type from PLANit to XML
   * @param planitMotorisationType to convert
   * @return result
   */
  public static MotorisationType planitToXml(final MotorisationModeType planitMotorisationType) {
    
    switch (planitMotorisationType) {
    case MOTORISED:
      return MotorisationType.MOTORISED;
    case NON_MOTORISED:
      return MotorisationType.NON_MOTORISED;      
    default:
      throw new PlanItRunTimeException(String.format("mapping from PLANit motorisation type %s to XML motorisation " +
              "type unavailable",planitMotorisationType.toString()));
    }
  }  
  
  /** convert vehicle type from xml to PLANit
   * @param xmlVehicularType to convert
   * @return result
   */
  public static VehicularModeType xmlToPlanit(final VehicularType xmlVehicularType) {
    
    switch (xmlVehicularType) {
    case VEHICLE:
      return VehicularModeType.VEHICLE;
    case NO_VEHICLE:
      return VehicularModeType.NO_VEHICLE;      
    default:
      throw new PlanItRunTimeException(String.format("mapping from XML vehicular type %s to PLANit vehicular type " +
              "unavailable",xmlVehicularType.toString()));
    }
  }  
  
  /** convert motorisation type from PLANit to XML
   * @param planitVehicularType to convert
   * @return result
   */
  public static VehicularType planitToXml(final VehicularModeType planitVehicularType) {
    
    switch (planitVehicularType) {
    case VEHICLE:
      return VehicularType.VEHICLE;
    case NO_VEHICLE:
      return VehicularType.NO_VEHICLE;      
    default:
      throw new PlanItRunTimeException(String.format("Mapping from PLANit vehicular type %s to XML vehicular type " +
              "unavailable",planitVehicularType.toString()));
    }
  }   
  
  /** convert track type from XML to PLANit
   *
   * @param xmlTrackType to convert
   * @return result
   */
  public static TrackModeType xmlToPlanit(final  org.goplanit.xml.generated.v2.TrackType xmlTrackType) {
    
    switch (xmlTrackType) {
    case RAIL:
      return TrackModeType.RAIL;
    case ROAD:
      return TrackModeType.ROAD;
    case WATER:
      return TrackModeType.WATER;
    default:
      throw new PlanItRunTimeException(String.format("Mapping from XML track type %s to PLANit track type unavailable",
              xmlTrackType.toString()));
    }
  }  
  
  /** convert track type from PLANit to XML
   * @param xmlTrackType to convert
   * @return result
   */
  public static  org.goplanit.xml.generated.v2.TrackType planitToXml(final TrackModeType xmlTrackType) {
    
    switch (xmlTrackType) {
    case RAIL:
      return  org.goplanit.xml.generated.v2.TrackType.RAIL;
    case ROAD:
      return  org.goplanit.xml.generated.v2.TrackType.ROAD;
    case WATER:
      return  org.goplanit.xml.generated.v2.TrackType.WATER;
    default:
      throw new PlanItRunTimeException(String.format("Mapping from PLANit track type %s to XML track type unavailable",
              xmlTrackType));
    }
  }
  
  /** convert used-to type from XML to PLANit
   *
   * @param xmlUseOfType to convert
   * @return result
   */
  public static UseOfModeType xmlToPlanit(final  org.goplanit.xml.generated.v2.UsedToType xmlUseOfType) {
    
    switch (xmlUseOfType) {
      case GOODS:
        return UseOfModeType.GOODS;
      case TAXI:
        return UseOfModeType.TAXI;
      case ACTIVE:
        return UseOfModeType.ACTIVE;
      case PRIVATE:
        return UseOfModeType.PRIVATE;
      case PUBLIC:
        return UseOfModeType.PUBLIC;
      case RIDE_SHARE:
        return UseOfModeType.RIDE_SHARE;
      case EMERGENCY:
        return UseOfModeType.EMERGENCY;
      default:
        throw new PlanItRunTimeException(
            String.format("Mapping from XML track type %s to PLANit track type unavailable",xmlUseOfType));
    }
  }   

  /** convert used-to type from PLANit to XML
   * @param useOfType to convert
   * @return result
   */
  public static  org.goplanit.xml.generated.v2.UsedToType planitToXml(UseOfModeType useOfType) {
    switch (useOfType) {
      case GOODS:
        return  org.goplanit.xml.generated.v2.UsedToType.GOODS;
      case TAXI:
        return  org.goplanit.xml.generated.v2.UsedToType.TAXI;
      case ACTIVE:
        return  org.goplanit.xml.generated.v2.UsedToType.ACTIVE;
      case PRIVATE:
        return  org.goplanit.xml.generated.v2.UsedToType.PRIVATE;
      case PUBLIC:
        return org.goplanit.xml.generated.v2.UsedToType.PUBLIC;
      case RIDE_SHARE:
        return  org.goplanit.xml.generated.v2.UsedToType.RIDE_SHARE;
      case EMERGENCY:
        return  org.goplanit.xml.generated.v2.UsedToType.EMERGENCY;
      default:
        throw new PlanItRunTimeException(String.format("Mapping from PLANit used-to type %s to XML used-to type " +
                "unavailable",useOfType.toString()));
    }
  } 
  
  /** Convert TimeUnit type from XML to PLANit Unit
   * 
   * @param xmlTimeUnitType to convert
   * @return result
   */
  public static org.goplanit.utils.unit.TimeUnit xmlToPlanit(
          final org.goplanit.xml.generated.v2.TimeUnit xmlTimeUnitType) {
    
    switch (xmlTimeUnitType) {
    case H:
      return Unit.HOUR;
    case MIN:
      return Unit.MINUTE;
    case S:
      return Unit.SECOND;
    case MS:
      return Unit.MILLISECOND;
    default:
      throw new PlanItRunTimeException(String.format("Mapping from XML TimeUnit %s to PLANit Units type " +
              "unavailable",xmlTimeUnitType.toString()));
    }
  }   

  /** Convert time unit from PLANit to XML TimeUnit
   * 
   * @param planitTimeUnit to convert
   * @return result
   */
  public static org.goplanit.xml.generated.v2.TimeUnit planitToXml(Unit planitTimeUnit) {
    if(planitTimeUnit.equals(Unit.HOUR)) {      
      return org.goplanit.xml.generated.v2.TimeUnit.H;
    }else if(planitTimeUnit.equals(Unit.MINUTE)){
      return org.goplanit.xml.generated.v2.TimeUnit.MIN;
    }else if(planitTimeUnit.equals(Unit.SECOND)) {
      return org.goplanit.xml.generated.v2.TimeUnit.S;
    }else if(planitTimeUnit.equals(Unit.MILLISECOND)) {
      return org.goplanit.xml.generated.v2.TimeUnit.MS;
    }else{
      throw new PlanItRunTimeException(String.format(
          "Mapping from PLANit time unit (Units) %s to XML TimeUnit unavailable", planitTimeUnit));
    }
  }

  /**
   * Convert JAXB TripDirectionType to native PLANit DirectionBound exactly
   *
   * @param xmlDirection direction of trip in XML format
   * @return PLANit direction bound
   */
  public static DirectionBound xmlToPlanit(TripDirectionType xmlDirection) {
    if (xmlDirection == null) {
      return null;
    }

    switch (xmlDirection) {
      case OUTBOUND:
        return DirectionBound.OUTBOUND;
      case INBOUND:
        return DirectionBound.INBOUND;
      default:
        throw new IllegalArgumentException("Unsupported XML direction enum constant: " + xmlDirection);
    }
  }

  /**
   * Convert values from Type enumeration in PLANit project to generated Typevalues enumeration
   *
   * @param type value of Type enumeration
   * @return value of generated Typevalues enumeration
   */
  public static Typevalues planitToXml(DataType type) {
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
        throw new PlanItRunTimeException("Data type " + type.value() + " has not been defined in the type values" +
            " simple type in the output XSD file");
    }
  }

  /**
   * Convert values from Units enumeration in PLANit project to generated Unitsvalues enumeration
   *
   * @param outputProperty value of Units enumeration
   * @return value of generated Unitsvalues enumeration
   */
  public static Unitsvalues planitToXml(OutputProperty outputProperty) {
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
    }else if(outputPropertyUnit.equals(Unit.MILLISECOND)) {
      return Unitsvalues.MS;
    }else{
      throw new PlanItRunTimeException("Units type " + outputProperty + " has not been defined in the units " +
          "values simple type in the output XSD file.");
    }
  }

}
