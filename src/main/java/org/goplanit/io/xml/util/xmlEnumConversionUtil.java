package org.goplanit.io.xml.util;

import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.mode.MotorisationModeType;
import org.goplanit.utils.mode.TrackModeType;
import org.goplanit.utils.mode.UseOfModeType;
import org.goplanit.utils.mode.VehicularModeType;
import org.goplanit.utils.unit.Unit;
import org.goplanit.xml.generated.MotorisationType;
import org.goplanit.xml.generated.TimeUnit;
import org.goplanit.xml.generated.TrackType;
import org.goplanit.xml.generated.UsedToType;
import org.goplanit.xml.generated.VehicularType;

/**
 * Some methods to convert the XML schema enums to PLANit memory model enums
 * 
 * @author markr
 *
 */
public class xmlEnumConversionUtil {
  
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
      throw new PlanItRunTimeException(String.format("mapping from xml motorisation type %s to PLANit motorisation type unavailable",xmlMotorisationType.toString()));
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
      throw new PlanItRunTimeException(String.format("mapping from planit motorisation type %s to xml motorisation type unavailable",planitMotorisationType.toString()));
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
      throw new PlanItRunTimeException(String.format("mapping from xml vehicular type %s to PLANit vehicular type unavailable",xmlVehicularType.toString()));
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
      throw new PlanItRunTimeException(String.format("mapping from planit vehicular type %s to xml vehicular type unavailable",planitVehicularType.toString()));
    }
  }   
  
  /** convert track type from xml to PLANit
   * @param xmlTrackType to convert
   * @return result
   */
  public static TrackModeType xmlToPlanit(final TrackType xmlTrackType) {
    
    switch (xmlTrackType) {
    case RAIL:
      return TrackModeType.RAIL;
    case ROAD:
      return TrackModeType.ROAD;      
    default:
      throw new PlanItRunTimeException(String.format("mapping from xml track type %s to PLANit track type unavailable",xmlTrackType.toString()));
    }
  }  
  
  /** convert track type from PLANit to XML
   * @param xmlTrackType to convert
   * @return result
   */
  public static TrackType planitToXml(final TrackModeType xmlTrackType) {
    
    switch (xmlTrackType) {
    case RAIL:
      return TrackType.RAIL;
    case ROAD:
      return TrackType.ROAD;
    case WATER:
      return TrackType.WATER;
    default:
      throw new PlanItRunTimeException(String.format("Mapping from PLANit track type %s to XML track type unavailable", xmlTrackType));
    }
  }
  
  /** convert used-to type from xml to PLANit
   * @param xmlUseOfType to convert
   * @return result
   */
  public static UseOfModeType xmlToPlanit(final UsedToType xmlUseOfType) {
    
    switch (xmlUseOfType) {
    case GOODS:
      return UseOfModeType.GOODS;
    case HIGH_OCCUPANCY:
      return UseOfModeType.HIGH_OCCUPANCY;
    case PRIVATE:
      return UseOfModeType.PRIVATE;
    case PUBLIC:
      return UseOfModeType.PUBLIC;
    case RIDE_SHARE:
      return UseOfModeType.RIDE_SHARE;    
    default:
      throw new PlanItRunTimeException(String.format("mapping from xml track type %s to PLANit track type unavailable", xmlUseOfType));
    }
  }   

  /** convert used-to type from PLANit to XML
   * @param useOfType to convert
   * @return result
   */
  public static UsedToType planitToXml(UseOfModeType useOfType) {
    switch (useOfType) {
    case GOODS:
      return UsedToType.GOODS;
    case HIGH_OCCUPANCY:
      return UsedToType.HIGH_OCCUPANCY;
    case PRIVATE:
      return UsedToType.PRIVATE;
    case PUBLIC:
      return UsedToType.PUBLIC;
    case RIDE_SHARE:
      return UsedToType.RIDE_SHARE;
    default:
      throw new PlanItRunTimeException(String.format("mapping from planit used-to type %s to xml used-to type unavailable",useOfType.toString()));
    }
  } 
  
  /** Convert TimeUnit type from XML to PLANit Unit
   * 
   * @param xmlTimeUnitType to convert
   * @return result
   */
  public static org.goplanit.utils.unit.TimeUnit xmlToPlanit(final TimeUnit xmlTimeUnitType) {
    
    switch (xmlTimeUnitType) {
    case H:
      return Unit.HOUR;
    case MIN:
      return Unit.MINUTE;
    case S:
      return Unit.SECOND;       
    default:
      throw new PlanItRunTimeException(String.format("mapping from XML TimeUnit %s to PLANit Units type unavailable",xmlTimeUnitType.toString()));
    }
  }   

  /** Convert time unit from PLANit to XML TimeUnit
   * 
   * @param planitTimeUnit to convert
   * @return result
   */
  public static TimeUnit planitToXml(Unit planitTimeUnit) {
    if(planitTimeUnit.equals(Unit.HOUR)) {      
      return TimeUnit.H;
    }else if(planitTimeUnit.equals(Unit.MINUTE)){
      return TimeUnit.MIN;
    }else if(planitTimeUnit.equals(Unit.SECOND)) {
      return TimeUnit.S;    
    }else{
      throw new PlanItRunTimeException(String.format("mapping from PLANit time unit (Units) %s to XML TimeUnit unavailable",planitTimeUnit.toString()));
    }
  }

}
