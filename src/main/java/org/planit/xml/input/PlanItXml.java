package org.planit.xml.input;

import java.io.FileReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.planit.basiccsv.network.physical.macroscopic.BasicCsvMacroscopicLinkSegmentType;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.demand.Demands;
import org.planit.demand.MatrixDemand;
import org.planit.event.CreatedProjectComponentEvent;
import org.planit.event.listener.InputBuilderListener;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.Link;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.Node;
import org.planit.network.physical.PhysicalNetwork.LinkSegments;
import org.planit.network.physical.PhysicalNetwork.Nodes;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegmentType;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.network.virtual.Centroid;
import org.planit.network.virtual.VirtualNetwork;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.userclass.TravellerType;
import org.planit.userclass.UserClass;
import org.planit.xml.constants.Default;
import org.planit.zoning.Zone;
import org.planit.zoning.Zoning;

import org.planit.generated.Configuration;
import org.planit.generated.Connectoid;
import org.planit.generated.Durationunit;
import org.planit.generated.Macroscopicdemand;
import org.planit.generated.Macroscopiczoning;
import org.planit.generated.Odmatrix;
import org.planit.generated.Odrawmatrix;
import org.planit.generated.Odrawmatrix.Values;
import org.planit.generated.Odrowmatrix;
import org.planit.generated.Timeperiods;
import org.planit.generated.Travellertypes;
import org.planit.generated.Userclasses;
import org.planit.generated.Zones;

public class PlanItXml implements InputBuilderListener  {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(PlanItXml.class.getName());
        
    private static final int ONE_WAY_AB =  1;
    private static final int ONE_WAY_BA =  2;
    private static final int TWO_WAY = 3;
     
    private String networkFileLocation;
    private String demandFileLocation;
    private String linkTypesFileLocation;
    private String zoneFileLocation;
    private String timePeriodFileLocation;
    private String modeFileLocation;
    private String zoningXmlFileLocation;
    private String demandXmlFileLocation;
    private String supplyXmlFileLocation;
    private List<String> RESERVED_CHARACTERS;
    
    private int noCentroids;
    private Map<Integer, BasicCsvMacroscopicLinkSegmentType> linkSegmentTypeMap;
    private Map<MacroscopicLinkSegment, Map<Long, Double>> alphaMapMap;
    private Map<MacroscopicLinkSegment, Map<Long, Double>> betaMapMap;
    private Map<Integer, Mode> modeMap;
    private LinkSegments linkSegments;
    private Nodes nodes;
    private Zoning.Zones zones;
    
    /**
     * the number of parsed link segments
     */
    private int numberOfLinkSegments;

/**
 * Constructor which reads in input file locations and instantiates the event manager.
 * 
 * @param networkFileLocation                       location of the network definition file
 * @param demandFileLocation                        location of the demands file
 * @param linkTypesFileLocation                 location of the link types file
 * @param zoneFileLocation                          location of the zones file
 * @param timePeriodFileLocation                location of the time periods file
 * @param modeFileLocation                        location of the mode definitions file
 */
    public PlanItXml(String networkFileLocation, 
					               String demandFileLocation, 
					               String linkTypesFileLocation, 
						           String zoneFileLocation, 
						           String timePeriodFileLocation, 
						           String modeFileLocation,
						           String zoningXmlFileLocation,
						           String demandXmlFileLocation,
						           String supplyXmlFileLocation) {
        this.networkFileLocation = networkFileLocation;
        this.demandFileLocation = demandFileLocation;
        this.linkTypesFileLocation = linkTypesFileLocation;
        this.zoneFileLocation = zoneFileLocation;       
        this.timePeriodFileLocation = timePeriodFileLocation;
        this.modeFileLocation = modeFileLocation;
    	this.zoningXmlFileLocation = zoningXmlFileLocation;
    	this.demandXmlFileLocation = demandXmlFileLocation;
    	this.supplyXmlFileLocation = supplyXmlFileLocation;
    	String[] reservedCharacters = {"+", "*", "^"};
    	RESERVED_CHARACTERS = Arrays.asList(reservedCharacters);
    }
    
/**
 * Handles creation events for network, zones, demands and travel time parameters   
 * 
 * We only handle BPR travel time functions at this stage.
 * 
 * @param event                         object creation event
 * @throws PlanItException      captures any exception thrown during creation events
 */
    @Override
    public void onCreateProjectComponent(CreatedProjectComponentEvent<?> event) throws PlanItException {
        Object projectComponent = event.getProjectComponent();
        try {
            if (projectComponent instanceof MacroscopicNetwork) {
                populateNetwork((MacroscopicNetwork) projectComponent); 
            } else if (projectComponent instanceof Zoning) {
                populateZoning((Zoning) projectComponent); 
            } else if (projectComponent instanceof Demands) {
                populateDemands((Demands) projectComponent); 
            } else if (projectComponent instanceof BPRLinkTravelTimeCost) {
                populateBprParameters((BPRLinkTravelTimeCost) projectComponent); //place parameters on the BPR cost component
            } else {
                LOGGER.fine("Event component is " + projectComponent.getClass().getCanonicalName() + " which is not handled by BascCsvScan");
            }
        } catch (PlanItException e) {
            e.printStackTrace();
        }
    }
    
/**
 * Tests whether a node referred to in the network input file already exists, and creates it if it does not 
 * 
 * @param network                           the physical network object
 * @param record                            the record in the network input file which refers to this node
 * @param columnName                the column in the record which refers to this node
 * @return                                      the node which has been created or found
 */
    private Node identifyAndRegisterNode(MacroscopicNetwork network, CSVRecord record, String columnName) {
        long nodeId = Integer.parseInt(record.get(columnName));
        Node node = network.nodes.findNodeByExternalIdentifier(nodeId);
        if  (node == null) {
            node = new Node();
            node.setExternalId(nodeId);
            network.nodes.registerNode(node);
        }
        return node;
    }

/**
 * Registers a new link segment in the physical network and stores the alpha and beta values for the link
 * 
 * @param network                           the physical network object
 * @param link                              the link from which the link segment will be created
 * @param abDirection                   direction of travel
 * @param linkSegmentType           object storing the input values for this link
 * @param noLanes                       the number of lanes in this link
 * @throws PlanItException          thrown if there is an error
 */
    private void generateAndRegisterLinkSegment(MacroscopicNetwork network, Link link, boolean abDirection, BasicCsvMacroscopicLinkSegmentType linkSegmentType, int noLanes) throws PlanItException {
        
        //create the link and store it in the network object
        MacroscopicLinkSegment linkSegment =  (MacroscopicLinkSegment) network.linkSegments.createDirectionalLinkSegment(link, abDirection);
        linkSegment.setMaximumSpeedMap(linkSegmentType.getSpeedMap());
        linkSegment.setNumberOfLanes(noLanes);
        MacroscopicLinkSegmentType macroscopicLinkSegmentType = network.registerNewLinkSegmentType(linkSegmentType.getName(), linkSegmentType.getCapacityPerLane(), linkSegmentType.getMaximumDensityPerLane(), null).getFirst();
        linkSegment.setLinkSegmentType(macroscopicLinkSegmentType);
        network.linkSegments.registerLinkSegment(link, linkSegment, abDirection);
        
        //store the alpha and beta values for the link
        alphaMapMap.put(linkSegment, linkSegmentType.getAlphaMap());
        betaMapMap.put(linkSegment, linkSegmentType.getBetaMap());
    }

/**
 * Reads route type values from input file and stores them in a Map
 * 
 * @return                                      Map containing link type values
 * @throws PlanItException          thrown if there is an error reading the input file
 */
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
    
/**
 * Creates the physical network object from the data in the input file
 *  
 * @param network                      the physical network object to be populated from the input data
 * @throws PlanItException          thrown if there is an error reading the input file
 */
    public void populateNetwork(@Nonnull MacroscopicNetwork network)  throws PlanItException {
        
        LOGGER.info("Populating Network");

        modeMap = getModes();       
        linkSegmentTypeMap = createLinkSegmentTypeMap();
        alphaMapMap = new HashMap<MacroscopicLinkSegment, Map<Long, Double>>();
        betaMapMap = new HashMap<MacroscopicLinkSegment, Map<Long, Double>>();
    
        try (Reader in = new FileReader(networkFileLocation)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);
            for (CSVRecord record : records) {
                Node startNode = identifyAndRegisterNode(network, record, "StartNode");
                Node endNode = identifyAndRegisterNode(network, record, "EndNode");
                int linkDirection = Integer.parseInt(record.get("Direction"));
                double length = Double.parseDouble(record.get("Length"));
                int noLanes = Integer.parseInt(record.get("NoLanes"));
                int linkType = Integer.parseInt(record.get("Type"));
                if (!linkSegmentTypeMap.containsKey(linkType)) {
                    throw new PlanItException("Link type " + linkType + " found in " + networkFileLocation + " but not in " + linkTypesFileLocation);
                }
                BasicCsvMacroscopicLinkSegmentType linkSegmentType = linkSegmentTypeMap.get(linkType);
                Link link = network.links.registerNewLink(startNode, endNode, length);
                if ((linkDirection == ONE_WAY_AB)  ||  (linkDirection == TWO_WAY)) {
                    generateAndRegisterLinkSegment(network, link, true, linkSegmentType, noLanes);
                }
                // Generate B->A direction link segment
                if ((linkDirection == ONE_WAY_BA)  ||  (linkDirection == TWO_WAY)) {
                    generateAndRegisterLinkSegment(network, link, false, linkSegmentType, noLanes);
                }
            }
            in.close();
        } catch (Exception ex) {
            throw new PlanItException(ex);
        }
        linkSegments = network.linkSegments;
        nodes = network.nodes;
        numberOfLinkSegments = network.linkSegments.getNumberOfLinkSegments();
    }
    
 /**
  * Generates a Java object populated with the data from an XML input file.
  * 
  * This method creates a JAXB Unmarshaller object which it uses to populate the Java class.
  * 
  * The output object will be of a generated class, created from the same XSD file which is used to validate the input XML file.
  * 
  * @param clazz                       Class of the object to be populated 
  * @param xmlFileLocation     location of the input XML file
  * @return                                an instance of the output class, populated with the data from the XML file.
  * @throws Exception              thrown if the XML file is invalid or cannot be opened
  */
    private Object generateObjectFromXml(Class<?> clazz, String xmlFileLocation) throws Exception {
       	JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
       	Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
       	XMLInputFactory xmlinputFactory = XMLInputFactory.newInstance();
       	XMLStreamReader xmlStreamReader = xmlinputFactory.createXMLStreamReader(new FileReader(xmlFileLocation));
       	Object obj = unmarshaller.unmarshal(xmlStreamReader);
       	xmlStreamReader.close();
        return obj;
    } 
    
/**
 * Creates the Zoning object and connectoids from the data in the input file
 * 
 * @param zoning                     the Zoning object to be populated from the input data
 * @throws PlanItException      thrown if there is an error reading the input file
 */
    public void populateZoning(Zoning zoning) throws PlanItException { 
        LOGGER.info("Populating Zoning");  
        if (nodes.getNumberOfNodes() == 0)
            throw new PlanItException("Cannot parse zoning input file before the network input file has been parsed.");
        noCentroids = 0;
        
        //create and register zones
    	try {
    		Macroscopiczoning macroscopiczoning = (Macroscopiczoning) generateObjectFromXml(Macroscopiczoning.class, zoningXmlFileLocation);
            VirtualNetwork virtualNetwork = zoning.getVirtualNetwork();
	    	for (Zones.Zone zoneGenerated : macroscopiczoning.getZones().getZone()) {
                long zoneExternalId = zoneGenerated.getId().longValue();
                Connectoid connectoid = zoneGenerated.getConnectoids().getConnectoid().get(0);
                long nodeExternalId = connectoid.getNoderef().longValue();
                double connectoidLength = (connectoid.getLength() == null) ? Default.CONNECTOID_LENGTH : connectoid.getLength();
                Node node = nodes.findNodeByExternalIdentifier(nodeExternalId);
                Zone zone = zoning.zones.createAndRegisterNewZone(zoneExternalId);
                Centroid centroid = zone.getCentroid();
                virtualNetwork.connectoids.registerNewConnectoid(centroid, node, connectoidLength);
                noCentroids++;
 	    	}
            zones = zoning.zones;
    	} catch (Exception e) {
    		throw new PlanItException(e);
    	}
        
    }
    
 /**
  * Convert regular expression special characters to act like simple strings during String.split() calls
  * 
  * @param separator			raw String separator 
  * @return							String separator with escape characters added if appropriate
  */
    private String escapeSeparator(String separator) {
    	if (RESERVED_CHARACTERS.contains(separator)) {
    		return "\\" + separator;
     	}
    	return separator;
    }
    
/**
 * Populates the Demands object from the input file
 * 
 * @param demands               the Demands object to be populated from the input data
 * @throws PlanItException    thrown if there is an error reading the input file
 */
    public void populateDemands(@Nonnull Demands demands) throws PlanItException {
        LOGGER.info("Populating Demands");  
        if (noCentroids == 0)
            throw new PlanItException("Cannot parse demand input file before zones input file has been parsed.");
 
        Map<Integer, TimePeriod> timePeriodMap = new HashMap<Integer, TimePeriod>();
    	try {
    		Macroscopicdemand macroscopicdemand = (Macroscopicdemand) generateObjectFromXml(Macroscopicdemand.class, demandXmlFileLocation);
	    	Configuration configuration = macroscopicdemand.getConfiguration();
	    	Timeperiods timeperiods = configuration.getTimeperiods();
	    	Userclasses userclasses = configuration.getUserclasses();
	    	if (userclasses.getUserclass().isEmpty()) {
	    		Userclasses.Userclass userclass = new Userclasses.Userclass();
	    		userclass.setName("Default");
	    		userclass.setId(new BigInteger("1"));
	    		userclass.setModeref(new BigInteger("1"));
	    		userclass.setTravellertyperef(new BigInteger("1"));
	    		userclasses.getUserclass().add(userclass);
	    	}
	    	Travellertypes travellertypes = configuration.getTravellertypes();
	    	if (travellertypes == null) {
	    		travellertypes = new Travellertypes();
	    	}
	    	if (travellertypes.getTravellertype().isEmpty()) {
	    		Travellertypes.Travellertype travellerType = new Travellertypes.Travellertype();
	    		travellerType.setId(new BigInteger("1"));
	    		travellerType.setName("Default");
	    		travellertypes.getTravellertype().add(travellerType);
	    	}
	    	for (Travellertypes.Travellertype travellertype : travellertypes.getTravellertype()) {
	    		TravellerType travellerType = new TravellerType(travellertype.getId().longValue(), travellertype.getName());
	    	}
	    	for (Userclasses.Userclass userclass : userclasses.getUserclass()) {
	    		int modeId = userclass.getModeref().intValue();
	    		//Mode mode = modeMap.get(modeId);
	    		TravellerType travellerType;
	    		if (userclass.getTravellertyperef() == null) {
	    			travellerType =  TravellerType.getById(1);  			
	    		} else {
	    			travellerType = TravellerType.getById(userclass.getTravellertyperef().longValue());
	    		}
	    		UserClass userClass = new UserClass(userclass.getId().longValue(), userclass.getName(), modeId, travellerType.getId());
	    	}
	        for (Timeperiods.Timeperiod timePeriodGenerated : timeperiods.getTimeperiod()) {
                int timePeriodId = timePeriodGenerated.getId().intValue();
                XMLGregorianCalendar time = timePeriodGenerated.getStarttime();
                int startTime = 3600 * time.getHour() + 60 * time.getMinute() + time.getSecond();
                int duration = timePeriodGenerated.getDuration().getValue().intValue();
                Durationunit durationUnit = timePeriodGenerated.getDuration().getUnit();
                switch (durationUnit) {
                	case H:  duration *= 3600;
                				   break;
                	case M: duration *= 60;
 				   				   break;
                }
                TimePeriod timePeriod = new TimePeriod("" + timePeriodId, startTime, duration);
                timePeriodMap.put(timePeriodId, timePeriod);
	        }

	        Map<Mode, Map<TimePeriod, MatrixDemand>> demandsPerTimePeriodAndMode = initializeDemandsPerTimePeriodAndMode(timePeriodMap);       
	        List<Object> oddemands = macroscopicdemand.getOddemands().getOdmatrixOrOdrowmatrixOrOdrawmatrix();
	        for (Object odmatrixInput : oddemands) {
		    	if (odmatrixInput instanceof Odmatrix) {
		    		Odmatrix odmatrix = (Odmatrix) odmatrixInput;
		    		int timePeriodId = odmatrix.getTimeperiodref().intValue();
		    		int userClassId = (odmatrix.getUserclassref() == null) ? 1 : odmatrix.getUserclassref().intValue();
		    		int modeId = (int) UserClass.getById(userClassId).getModeId();
	                Mode mode = modeMap.get(modeId);
	                TimePeriod timePeriod = timePeriodMap.get(timePeriodId);
	                MatrixDemand matrixDemand = demandsPerTimePeriodAndMode.get(mode).get(timePeriod);     
	                List<Odmatrix.O> o = odmatrix.getO();
	                for (Odmatrix.O originZone : o) {
	                	long originZoneId = zones.getZoneByExternalId(originZone.getRef().longValue()).getId();
	                	List<Odmatrix.O.D> d = originZone.getD();
	                	for (Odmatrix.O.D demandZone : d) {
	                		long destinationZoneId = zones.getZoneByExternalId(demandZone.getRef().longValue()).getId();
	                		double demand = demandZone.getValue() * mode.getPcu();
	                		matrixDemand.set(originZoneId, destinationZoneId, demand);
	                	}
	                }
	                demands.registerODDemand(timePeriod, mode, matrixDemand);
		    	} else if (odmatrixInput instanceof Odrowmatrix) {
		    		Odrowmatrix odrowmatrix = (Odrowmatrix) odmatrixInput;
		    		int timePeriodId = odrowmatrix.getTimeperiodref().intValue();
		    		int userClassId = (odrowmatrix.getUserclassref() == null) ? 1 : odrowmatrix.getUserclassref().intValue();
		    		int modeId = (int) UserClass.getById(userClassId).getModeId();
	                Mode mode = modeMap.get(modeId);
	                TimePeriod timePeriod = timePeriodMap.get(timePeriodId);
	                MatrixDemand matrixDemand = demandsPerTimePeriodAndMode.get(mode).get(timePeriod);  
	                String separator = (odrowmatrix.getDs() == null) ? "," : odrowmatrix.getDs();
	                separator = escapeSeparator(separator);
	                List<Odrowmatrix.Odrow> odrow = odrowmatrix.getOdrow();
	                for (Odrowmatrix.Odrow originRow : odrow) {
	                	long originZoneId = zones.getZoneByExternalId(originRow.getRef().longValue()).getId();
	                	String[] rowValuesAsString = originRow.getValue().split(separator);
	                	for (int i=0; i<rowValuesAsString.length ; i++) {
	                		long destinationZoneId = zones.getZoneByExternalId(i + 1).getId();
	                		double demand = Double.parseDouble(rowValuesAsString[i]) * mode.getPcu();
	    	                matrixDemand.set(originZoneId, destinationZoneId, demand);
	                	}
	                }
	                demands.registerODDemand(timePeriod, mode, matrixDemand);
		    	} else if (odmatrixInput instanceof Odrawmatrix) {
		    		Odrawmatrix odrawmatrix = (Odrawmatrix) odmatrixInput;
		    		int timePeriodId = odrawmatrix.getTimeperiodref().intValue();
		    		int userClassId = (odrawmatrix.getUserclassref() == null) ? 1 : odrawmatrix.getUserclassref().intValue();
		    		int modeId = (int) UserClass.getById(userClassId).getModeId();
	                Mode mode = modeMap.get(modeId);
	                TimePeriod timePeriod = timePeriodMap.get(timePeriodId);
	                MatrixDemand matrixDemand = demandsPerTimePeriodAndMode.get(mode).get(timePeriod);   
	                Values values = odrawmatrix.getValues();
	                String originSeparator = (values.getOs() == null) ? "," : values.getOs();
	                originSeparator = escapeSeparator(originSeparator);
	                String demandSeparator = (values.getDs() == null) ? "," : values.getDs();
	                demandSeparator = escapeSeparator(demandSeparator);
	                if (originSeparator.equals(demandSeparator)) {
	                	String [] allValuesAsString = values.getValue().split(originSeparator);
	                	int size = allValuesAsString.length;
	                	int noRows = (int) Math.round(Math.sqrt(size));
	                	if ((noRows * noRows) != size) {
	                		throw new Exception("Element <odrawmatrix> contains a string of " + size + " values, which is not an exact square");
	                	}
	                	int noCols = noRows;
	                	for (int i=0; i <noRows; i++) {
	                		int row = i * noRows;
	                		for (int col=0; col < noCols; col++) {
	                			long originZoneId = zones.getZoneByExternalId(i + 1).getId();
	                			long destinationZoneId = zones.getZoneByExternalId(col + 1).getId();
	                			double demand = Double.parseDouble(allValuesAsString[row + col]) * mode.getPcu();
	                			matrixDemand.set(originZoneId, destinationZoneId, demand);
	                		}
	                	}
	                } else {
	                	String [] originRows = values.getValue().split(originSeparator);
	                	int noRows = originRows.length;
	                	for (int i=0; i <noRows; i++) {	
	                		String [] allValuesAsString = originRows[i].split(demandSeparator);
	                		int noCols = allValuesAsString.length;
	                		if (noRows != noCols) {
	                			throw new Exception("Element <odrawmatrix> does not parse to a square matrix: Row " + (i+1) + " has " + noCols + " values.");
	                		}
	                		for (int col=0; col < noCols; col++) {
	                			long originZoneId = zones.getZoneByExternalId(i + 1).getId();
	                			long destinationZoneId = zones.getZoneByExternalId(col + 1).getId();
	                			double demand = Double.parseDouble(allValuesAsString[col]) * mode.getPcu();
	                			matrixDemand.set(originZoneId, destinationZoneId, demand);
	                		}
	                	}
	                }
	                demands.registerODDemand(timePeriod, mode, matrixDemand);
		    	}
	        }
    	} catch (Exception e) {
            e.printStackTrace();
    		throw new PlanItException(e);
    	}
    }
    
/**
 * Create the Map of demands for each time period and mode
 * 
 * @param timePeriodMap                       Map of time periods
 * @return                                                empty Map of demands for each time period
 * @throws PlanItException                     thrown if there is an error
 */
    private Map<Mode, Map<TimePeriod, MatrixDemand>> initializeDemandsPerTimePeriodAndMode(Map<Integer, TimePeriod> timePeriodMap) throws PlanItException {
        Map<Mode, Map<TimePeriod, MatrixDemand>> demandsPerTimePeriodAndMode = new HashMap<Mode,  Map<TimePeriod, MatrixDemand>>();
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
 * Read and return the zone Id corresponding to a row and column in the demands input file
 * 
 * @param record                    current record in the demands input CSV file (corresponds to the row in the file)
 * @param columnHeader       the column header (corresponds to the column in the file)
 * @return                               the Id of the zone
 * @throws Exception             thrown if the record cannot be read
 */

    private long getZoneId(CSVRecord record, String columnHeader) throws Exception {
        int externalZoneId = Integer.parseInt(record.get(columnHeader));
        Zone zone = zones.getZoneByExternalId(externalZoneId);
        return zone.getId();
    }
    
/**
 * Read in the modes from the mode definition file
 * 
 * @return                                  Map storing the Mode objects
 * @throws PlanItException       thrown if the mode definition file cannot be opened
 */
    private Map<Integer, Mode> getModes() throws PlanItException {
        Map<Integer, Mode> modeMap = new HashMap<Integer, Mode>();
        try (Reader in = new FileReader(modeFileLocation)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);
            for (CSVRecord record : records) {
                int modeId = Integer.parseInt(record.get("Mode"));
                if (modeId == 0) {
                    throw new PlanItException("Found a Mode value of 0 in the modes definition file, this is prohibited");
                }
                String name = record.get("Name");
                double pcu = Double.parseDouble(record.get("PCU"));
                Mode mode = new Mode(modeId, name, pcu);
                modeMap.put(modeId, mode);
            }
            return modeMap;
        } catch (Exception ex) {
            throw new PlanItException(ex);
        }
    }
    
/**
 * Populate the BPR parameters object from data which has previously been read in from the route types file
 * 
 * @param costComponent         the BPR travel time cost object to be populated
 * @throws PlanItException         thrown if the alpha and beta parameters have not already been stored
 */
    public void populateBprParameters(@Nonnull BPRLinkTravelTimeCost costComponent) throws PlanItException {
        LOGGER.info("Populating BPR Parameters");  
        try {
            if (numberOfLinkSegments <= 0) {
                throw new PlanItException("Cannot populate BPR parameters before physical network has been parsed");
            }
            if (alphaMapMap.keySet().isEmpty()) {
                throw new PlanItException("Alpha parameters have not been set - should have been read in from the route types file");
            }
            if (betaMapMap.keySet().isEmpty()) {
                throw new PlanItException("Beta parameters have not been set - should have been read in from the route types file");
            }
            BPRLinkTravelTimeCost.BPRParameters[] bprLinkSegmentParameters = new BPRLinkTravelTimeCost.BPRParameters[numberOfLinkSegments];
            Iterator<LinkSegment> iterator = linkSegments.iterator();
            while (iterator.hasNext()) {
                MacroscopicLinkSegment macroscopicLinkSegment = (MacroscopicLinkSegment) iterator.next();
                Map<Long, Double> alphaMap = alphaMapMap.get(macroscopicLinkSegment);
                Map<Long, Double> betaMap = betaMapMap.get(macroscopicLinkSegment);
                bprLinkSegmentParameters[(int) macroscopicLinkSegment.getId()] = new BPRLinkTravelTimeCost.BPRParameters(alphaMap, betaMap);
            }           
            costComponent.populate(bprLinkSegmentParameters);
        } catch (PlanItException e) {
            throw new PlanItException(e);
        }
    }
        
}
