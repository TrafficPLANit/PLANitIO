package org.planit.xml.network;

import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.planit.basiccsv.network.physical.macroscopic.BasicCsvMacroscopicLinkSegmentType;
import org.planit.exceptions.PlanItException;
import org.planit.generated.Linkconfiguration;
import org.planit.generated.Linksegmenttypes;
import org.planit.generated.Modes;
import org.planit.userclass.Mode;
import org.planit.xml.constants.Default;
import org.planit.xml.network.physical.macroscopic.XmlMacroscopicLinkSegmentType;

public class ProcessLinkConfiguration {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(ProcessLinkConfiguration.class.getName());
	
	public static Map<Integer, Mode> getModeMap(Linkconfiguration linkconfiguration) throws PlanItException {
        Map<Integer, Mode> modeMap = new HashMap<Integer, Mode>();
        for (Modes.Mode generatedMode : linkconfiguration.getModes().getMode()) {
            int modeId = generatedMode.getId().intValue();
            if (modeId == 0) {
                throw new PlanItException("Found a Mode value of 0 in the modes definition file, this is prohibited");
            }
            String name = generatedMode.getName();
            double pcu = generatedMode.getPcu();
            Mode mode = new Mode(modeId, name, pcu);
            modeMap.put(modeId, mode);
        }
        return modeMap;
	}
	
	/**
	 * Reads route type values from input file and stores them in a Map
	 * 
	 * @return                                    Map containing link type values
	 * @throws PlanItException         thrown if there is an error reading the input file
	 */
/*
	 public static Map<Integer, XmlMacroscopicLinkSegmentType> createLinkSegmentTypeMap(Linkconfiguration linkconfiguration, Map<Integer, Mode> modeMap) throws PlanItException {
	     //double maximumDensity = Double.POSITIVE_INFINITY;
	     XmlMacroscopicLinkSegmentType.reset();
	     Map<Integer, XmlMacroscopicLinkSegmentType> linkSegmentMap = new HashMap<Integer, XmlMacroscopicLinkSegmentType>();
	     for (Linksegmenttypes.Linksegmenttype linkSegmentType : linkconfiguration.getLinksegmenttypes().getLinksegmenttype()) {
	    	 int type = linkSegmentType.getId().intValue();
	    	 String name = linkSegmentType.getName();
	    	 Float capacity = (linkSegmentType.getCapacitylane() == null) ? Default.LANE_CAPACITY : linkSegmentType.getCapacitylane();
	    	 Float maximumDensity = (linkSegmentType.getMaxdensitylane() == null) ? Default.MAXIMUM_LANE_DENSITY : linkSegmentType.getMaxdensitylane();
	     }
	     
	     
	     try (Reader in = new FileReader(linkTypesFileLocation)) {
	         Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);
	         for (CSVRecord record : records) {
	             int type = Integer.parseInt(record.get("Type"));
	             String name = record.get("Name");
	             double speed = Double.parseDouble(record.get("Speed"));
	             String capacityString = record.get("Capacity");
	             double capacity;
	             if ((capacityString != null) && (!capacityString.equals(""))) {
	                capacity = Double.parseDouble(capacityString);
	             } else {
	                capacity = 0.0;
	             }
	             int modeId = Integer.parseInt(record.get("Mode"));
	             if  ((modeId != 0) && (!modeMap.containsKey(modeId))) {
	                 throw new PlanItException("Mode Id " + modeId + " found in link types file but not in modes definition file");
	             }
	             XmlMacroscopicLinkSegmentType linkSegmentType = XmlMacroscopicLinkSegmentType.createOrUpdateLinkSegmentType(name, capacity, maximumDensity, speed, modeId,  modeMap, type);
	             linkSegmentMap.put(type, linkSegmentType);
	         }
	         in.close();
	         
	         //If a mode is missing for a link type, set the speed to zero for vehicles of this type in this link type, meaning they are forbidden
	         for (Integer linkType : linkSegmentMap.keySet()) {
	             XmlMacroscopicLinkSegmentType linkSegmentType = linkSegmentMap.get(linkType);
	             for (Mode mode : modeMap.values()) {
	                 long modeId = mode.getId();
	                 if (!linkSegmentType.getSpeedMap().containsKey(modeId)) {
	                     LOGGER.info("Mode " + mode.getName() + " not defined for Link Type " + linkSegmentType.getName() + ".  Will be given a speed zero, meaning vehicles of this type are not allowed in links of this type.");
	                     XmlMacroscopicLinkSegmentType linkSegmentTypeNew = XmlMacroscopicLinkSegmentType.createOrUpdateLinkSegmentType(linkSegmentType.getName(), 0.0, maximumDensity, 0.0, (int) modeId,  modeMap, linkType);
	                     linkSegmentMap.put(linkType, linkSegmentTypeNew);
	                 }
	             }
	         }           
	         return linkSegmentMap;
	     } catch (Exception ex) {
	         throw new PlanItException(ex);
	     }
	 }
*/	    
	/**
	 * Reads route type values from input file and stores them in a Map
	 * 
	 * @return                                    Map containing link type values
	 * @throws PlanItException         thrown if there is an error reading the input file
	 */
/*
	private Map<Integer, BasicCsvMacroscopicLinkSegmentType> createLinkSegmentTypeMap() throws PlanItException {
	    double maximumDensity = Double.POSITIVE_INFINITY;
	    BasicCsvMacroscopicLinkSegmentType.reset();
	    Map<Integer, BasicCsvMacroscopicLinkSegmentType> linkSegmentMap = new HashMap<Integer, BasicCsvMacroscopicLinkSegmentType>();
	    try (Reader in = new FileReader(linkTypesFileLocation)) {
	        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);
	        for (CSVRecord record : records) {
	            int type = Integer.parseInt(record.get("Type"));
	            String name = record.get("Name");
	            double speed = Double.parseDouble(record.get("Speed"));
	            String capacityString = record.get("Capacity");
	            double capacity;
	            if ((capacityString != null) && (!capacityString.equals(""))) {
	               capacity = Double.parseDouble(capacityString);
	            } else {
	               capacity = 0.0;
	            }
	            int modeId = Integer.parseInt(record.get("Mode"));
	            if  ((modeId != 0) && (!modeMap.containsKey(modeId))) {
	                throw new PlanItException("Mode Id " + modeId + " found in link types file but not in modes definition file");
	            }
	            double alpha = Double.parseDouble(record.get("Alpha"));
	            double beta = Double.parseDouble(record.get("Beta"));
	            BasicCsvMacroscopicLinkSegmentType linkSegmentType = BasicCsvMacroscopicLinkSegmentType.createOrUpdateLinkSegmentType(name, capacity, maximumDensity, speed, alpha, beta, modeId,  modeMap, type);
	            linkSegmentMap.put(type, linkSegmentType);
	        }
	        in.close();
	         
	       //If a mode is missing for a link type, set the speed to zero for vehicles of this type in this link type, meaning they are forbidden
	        for (Integer linkType : linkSegmentMap.keySet()) {
	            BasicCsvMacroscopicLinkSegmentType linkSegmentType = linkSegmentMap.get(linkType);
	            for (Mode mode : modeMap.values()) {
	                long modeId = mode.getId();
	                if (!linkSegmentType.getSpeedMap().containsKey(modeId)) {
	                    LOGGER.info("Mode " + mode.getName() + " not defined for Link Type " + linkSegmentType.getName() + ".  Will be given a speed zero, meaning vehicles of this type are not allowed in links of this type.");
	                    BasicCsvMacroscopicLinkSegmentType linkSegmentTypeNew = BasicCsvMacroscopicLinkSegmentType.createOrUpdateLinkSegmentType(linkSegmentType.getName(), 0.0, maximumDensity, 0.0, 0.0, 0.0, (int) modeId,  modeMap, linkType);
	                    linkSegmentMap.put(linkType, linkSegmentTypeNew);
	                }
	            }
	        }           
	        return linkSegmentMap;
	    } catch (Exception ex) {
	        throw new PlanItException(ex);
	    }
	}
*/		    
}
