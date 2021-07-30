package org.planit.io.xml.util;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.MotorisationModeType;
import org.planit.utils.mode.TrackModeType;
import org.planit.utils.mode.UseOfModeType;
import org.planit.utils.mode.VehicularModeType;
import org.planit.utils.unit.Units;
import org.planit.xml.generated.MotorisationType;
import org.planit.xml.generated.TimeUnit;
import org.planit.xml.generated.TrackType;
import org.planit.xml.generated.UsedToType;
import org.planit.xml.generated.VehicularType;

/**
 * Some methods to convert the XML schema enums to PLANit memory model enums
 * 
 * @author markr
 *
 */
public class EnumConversionUtil {
  
  /** convert motorisation type from xml to PLANit
   * @param xmlMotorisationType to convert
   * @return result
   * @throws PlanItException thrown if error
   */
  public static MotorisationModeType xmlToPlanit(final MotorisationType xmlMotorisationType) throws PlanItException {
    
    switch (xmlMotorisationType) {
    case MOTORISED:
      return MotorisationModeType.MOTORISED;
    case NON_MOTORISED:
      return MotorisationModeType.NON_MOTORISED;      
    default:
      throw new PlanItException(String.format("mapping from xml motorisation type %s to PLANit motorisation type unavailable",xmlMotorisationType.toString()));
    }
  }
  
  /** convert motorisation type from PLANit to XML
   * @param planitMotorisationType to convert
   * @return result
   * @throws PlanItException thrown if error
   */
  public static MotorisationType planitToXml(final MotorisationModeType planitMotorisationType) throws PlanItException {
    
    switch (planitMotorisationType) {
    case MOTORISED:
      return MotorisationType.MOTORISED;
    case NON_MOTORISED:
      return MotorisationType.NON_MOTORISED;      
    default:
      throw new PlanItException(String.format("mapping from planit motorisation type %s to xml motorisation type unavailable",planitMotorisationType.toString()));
    }
  }  
  
  /** convert vehicle type from xml to PLANit
   * @param xmlVehicularType to convert
   * @return result
   * @throws PlanItException thrown if error
   */
  public static VehicularModeType xmlToPlanit(final VehicularType xmlVehicularType) throws PlanItException {
    
    switch (xmlVehicularType) {
    case VEHICLE:
      return VehicularModeType.VEHICLE;
    case NO_VEHICLE:
      return VehicularModeType.NO_VEHICLE;      
    default:
      throw new PlanItException(String.format("mapping from xml vehicular type %s to PLANit vehicular type unavailable",xmlVehicularType.toString()));
    }
  }  
  
  /** convert motorisation type from PLANit to XML
   * @param planitVehicularType to convert
   * @return result
   * @throws PlanItException thrown if error
   */
  public static VehicularType planitToXml(final VehicularModeType planitVehicularType) throws PlanItException {
    
    switch (planitVehicularType) {
    case VEHICLE:
      return VehicularType.VEHICLE;
    case NO_VEHICLE:
      return VehicularType.NO_VEHICLE;      
    default:
      throw new PlanItException(String.format("mapping from planit vehicular type %s to xml vehicular type unavailable",planitVehicularType.toString()));
    }
  }   
  
  /** convert track type from xml to PLANit
   * @param xmlTrackType to convert
   * @return result
   * @throws PlanItException thrown if error
   */
  public static TrackModeType xmlToPlanit(final TrackType xmlTrackType) throws PlanItException {
    
    switch (xmlTrackType) {
    case RAIL:
      return TrackModeType.RAIL;
    case ROAD:
      return TrackModeType.ROAD;      
    default:
      throw new PlanItException(String.format("mapping from xml track type %s to PLANit track type unavailable",xmlTrackType.toString()));
    }
  }  
  
  /** convert track type from PLANit to XML
   * @param xmlTrackType to convert
   * @return result
   * @throws PlanItException thrown if error
   */
  public static TrackType planitToXml(final TrackModeType xmlTrackType) throws PlanItException {
    
    switch (xmlTrackType) {
    case RAIL:
      return TrackType.RAIL;
    case ROAD:
      return TrackType.ROAD;      
    default:
      throw new PlanItException(String.format("mapping from planit track type %s to xml track type unavailable",xmlTrackType.toString()));
    }
  }
  
  /** convert used-to type from xml to PLANit
   * @param xmlUseOfType to convert
   * @return result
   * @throws PlanItException thrown if error
   */
  public static UseOfModeType xmlToPlanit(final UsedToType xmlUseOfType) throws PlanItException {
    
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
      throw new PlanItException(String.format("mapping from xml track type %s to PLANit track type unavailable",xmlUseOfType.toString()));
    }
  }   

  /** convert used-to type from PLANit to XML
   * @param useOfType to convert
   * @return result
   * @throws PlanItException thrown if error
   */  
  public static UsedToType planitToXml(UseOfModeType useOfType) throws PlanItException {
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
      throw new PlanItException(String.format("mapping from planit used-to type %s to xml used-to type unavailable",useOfType.toString()));
    }
  } 
  
  /** Convert TimeUnit type from XML to PLANit Unit
   * 
   * @param xmlTimeUnitType to convert
   * @return result
   * @throws PlanItException thrown if error
   */
  public static Units xmlToPlanit(final TimeUnit xmlTimeUnitType) throws PlanItException {
    
    switch (xmlTimeUnitType) {
    case H:
      return Units.HOUR;
    case MIN:
      return Units.MINUTE;
    case S:
      return Units.SECOND;       
    default:
      throw new PlanItException(String.format("mapping from XML TimeUnit %s to PLANit Units type unavailable",xmlTimeUnitType.toString()));
    }
  }   

  /** Convert time unit from PLANit to XML TimeUnit
   * 
   * @param planitTimeUnit to convert
   * @return result
   * @throws PlanItException thrown if error
   */  
  public static TimeUnit planitToXml(Units planitTimeUnit) throws PlanItException {
    switch (planitTimeUnit) {
    case HOUR:
      return TimeUnit.H;
    case MINUTE:
      return TimeUnit.MIN;
    case SECOND:
      return TimeUnit.S;    
    default:
      throw new PlanItException(String.format("mapping from PLANit time unit (Units) %s to XML TimeUnit unavailable",planitTimeUnit.toString()));
    }
  }

}
