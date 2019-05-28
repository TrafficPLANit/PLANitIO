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
//import org.planit.generated.Odmatrix;
import org.planit.generated.Odcellbycellmatrix;
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
    		e.printStackTrace();
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
  * Generate default user class if none defined in XML files 
  * 
  * @return     default Userclass object
  */
    private Userclasses.Userclass generateDefaultUserClass() {
		Userclasses.Userclass userclass = new Userclasses.Userclass();
		userclass.setName(Default.USER_CLASS_NAME);
		userclass.setId(Default.USER_CLASS_ID);
		userclass.setModeref(Default.USER_CLASS_MODE_REF);
		userclass.setTravellertyperef(Default.USER_CLASS_TRAVELLER_TYPE);
		return userclass;
    }
    
/**
 * Generate default traveller type if none defined in XML files 
 * 
 * @return     default Travellertype object
 */
    private Travellertypes.Travellertype generateDefaultTravellerType() {
		Travellertypes.Travellertype travellerType = new Travellertypes.Travellertype();
		travellerType.setId(Default.TRAVELLER_TYPE_ID);
		travellerType.setName(Default.TRAVELLER_TYPE_NAME);
		return travellerType;
    }
    
/**
 * Generate TravellerType objects from generated configuration object and store them
 * 
 * @param configuration       generated Configuration object from demand XML input
 */
    private void generateAndStoreTravellerTypes(Configuration configuration) {
	     Travellertypes travellertypes = (configuration.getTravellertypes() == null) ?  new Travellertypes() : configuration.getTravellertypes();
    	if (travellertypes.getTravellertype().isEmpty()) {
    		travellertypes.getTravellertype().add(generateDefaultTravellerType());
    	}
    	for (Travellertypes.Travellertype travellertype : travellertypes.getTravellertype()) {
    		TravellerType travellerType = new TravellerType(travellertype.getId().longValue(), travellertype.getName());
    	}
    }
    
/**
  * Generate UserClass objects from generated configuration object and store them
  * 
  * @param configuration       generated Configuration object from demand XML input
  */
    private void generateAndStoreUserClasses(Configuration configuration) {
    	Userclasses userclasses = configuration.getUserclasses();
    	if (userclasses.getUserclass().isEmpty()) {
    		userclasses.getUserclass().add(generateDefaultUserClass());
    	}
    	for (Userclasses.Userclass userclass : userclasses.getUserclass()) {
    		int modeId = userclass.getModeref().intValue();
    		long travellerTypeId = (userclass.getTravellertyperef() == null) ? Default.TRAVELLER_TYPE_ID.longValue() : userclass.getTravellertyperef().longValue();
    		TravellerType travellerType = TravellerType.getById(travellerTypeId);
    		UserClass userClass = new UserClass(userclass.getId().longValue(), userclass.getName(), modeId, travellerType.getId());
    	}
    }
    
/**
 * Generate a Map of TimePeriod objects from generated configuration object
 * 
 * @param configuration       generated Configuration object from demand XML input
 * @return                              Map of TimePeriod objects, using the id of the TimePeriod as its key
 */
    private Map<Integer, TimePeriod> generateTimePeriodMap(Configuration configuration) {
    	Timeperiods timeperiods = configuration.getTimeperiods();
    	Map<Integer, TimePeriod> timePeriodMap = new HashMap<Integer, TimePeriod>();
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
        return timePeriodMap;
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
     	try {
    		Macroscopicdemand macroscopicdemand = (Macroscopicdemand) generateObjectFromXml(Macroscopicdemand.class, demandXmlFileLocation);
	    	Configuration configuration = macroscopicdemand.getConfiguration();
	    	generateAndStoreTravellerTypes(configuration);
	    	generateAndStoreUserClasses(configuration);
	    	Map<Integer, TimePeriod> timePeriodMap = generateTimePeriodMap(configuration);
	        Map<Mode, Map<TimePeriod, MatrixDemand>> demandsPerTimePeriodAndMode = initializeDemandsPerTimePeriodAndMode(timePeriodMap);       
	        List<Odmatrix> oddemands = macroscopicdemand.getOddemands().getOdcellbycellmatrixOrOdrowmatrixOrOdrawmatrix();
	        for (Odmatrix odmatrix : oddemands) {
	    		int timePeriodId =  odmatrix.getTimeperiodref().intValue();
	    		int userClassId = (odmatrix.getUserclassref() == null) ? Default.USER_CLASS_ID.intValue() :  odmatrix.getUserclassref().intValue();
	    		int modeId = (int) UserClass.getById(userClassId).getModeId();
                Mode mode = modeMap.get(modeId);
                TimePeriod timePeriod = timePeriodMap.get(timePeriodId);
                MatrixDemand matrixDemand = demandsPerTimePeriodAndMode.get(mode).get(timePeriod);  
                updateDemandMatrixFromOdMatrix(odmatrix, mode, matrixDemand);
		    	demands.registerODDemand(timePeriod, mode, matrixDemand);
	        }
    	} catch (Exception e) {
            e.printStackTrace();
    		throw new PlanItException(e);
    	}
    }
    
/**
 * Update the demand matrix object from a generated OD matrix
 * 
 * @param odmatrix                Odmatrix object generated from the input XML
 * @param mode                     Mode of travel
 * @param matrixDemand      MatrixDemand object to be updated
 * @throws Exception              thrown if there is an error during processing
 */
    private void updateDemandMatrixFromOdMatrix(Odmatrix odmatrix, Mode mode, MatrixDemand matrixDemand) throws Exception {
    	if (odmatrix instanceof Odcellbycellmatrix) {
    		updateDemandMatrixFromCellByCellMatrix((Odcellbycellmatrix) odmatrix, mode, matrixDemand);
    	} else if (odmatrix instanceof Odrowmatrix) {
    		updateDemandMatrixFromOdRowMatrix((Odrowmatrix) odmatrix, mode, matrixDemand);
    	} else if (odmatrix instanceof Odrawmatrix) {
    		updateDemandMatrixFromOdRawMatrix((Odrawmatrix) odmatrix, mode, matrixDemand);
    	}
    }
    
/**
  * Update the demand matrix object from a generated OD raw matrix
  * 
  * @param odrowmatrix        Odrawmatrix object generated from the input XML
  * @param mode                    Mode of travel
  * @param matrixDemand     MatrixDemand object to be updated
  * @throws Exception             thrown if the Odrawmatrix cannot be parsed into a square matrix
  */
    private void updateDemandMatrixFromOdRawMatrix(Odrawmatrix odrawmatrix, Mode mode, MatrixDemand matrixDemand) throws Exception {
        Values values = odrawmatrix.getValues();
        String originSeparator = (values.getOs() == null) ? Default.SEPARATOR : values.getOs();
        originSeparator = escapeSeparator(originSeparator);
        String destinationSeparator = (values.getDs() == null) ? Default.SEPARATOR : values.getDs();
        destinationSeparator = escapeSeparator(destinationSeparator);
        if (originSeparator.equals(destinationSeparator)) {
        	 updateDemandMatrixForEqualSeparators(values, originSeparator, mode, matrixDemand);
        } else {
        	 updateDemandMatrixForDifferentSeparators(values, originSeparator, destinationSeparator, mode, matrixDemand);
        }
    }
    
 /**
  * Update the demand matrix object from a generated OD raw matrix when origin and destination separators are different
  * 
  * @param values                          Values object generated from the input XML
  * @param originSeparator           origin separator character
  * @param destinationSeparator  destination separator character    
  * @param mode                           Mode of travel
  * @param matrixDemand            MatrixDemand object to be updated
  * @throws Exception                    thrown if the Odrawmatrix cannot be parsed into a square matrix
  */
    private void updateDemandMatrixForDifferentSeparators(Values values, String originSeparator, String destinationSeparator, Mode mode, MatrixDemand matrixDemand) throws Exception {
    	String [] originRows = values.getValue().split(originSeparator);
    	int noRows = originRows.length;
    	for (int i=0; i <noRows; i++) {	
    		String [] allValuesAsString = originRows[i].split(destinationSeparator);
    		int noCols = allValuesAsString.length;
    		if (noRows != noCols) {
    			throw new Exception("Element <odrawmatrix> does not parse to a square matrix: Row " + (i+1) + " has " + noCols + " values.");
    		}
    		for (int col=0; col < noCols; col++) {
    			updateMatrixDemand(matrixDemand, i+1, col+1, mode, Double.parseDouble(allValuesAsString[col]));
    		}
    	}
    }
    
/**
 * Update the demand matrix object from a generated OD raw matrix when origin and destination separators are equal
 * 
 * @param values                          Values object generated from the input XML
 * @param separator                    separator character
 * @param mode                           Mode of travel
 * @param matrixDemand            MatrixDemand object to be updated
 * @throws Exception                    thrown if the Odrawmatrix cannot be parsed into a square matrix
 */
    private void updateDemandMatrixForEqualSeparators(Values values, String separator, Mode mode, MatrixDemand matrixDemand) throws Exception {
    	String [] allValuesAsString = values.getValue().split(separator);
    	int size = allValuesAsString.length;
    	int noRows = (int) Math.round(Math.sqrt(size));
    	if ((noRows * noRows) != size) {
    		throw new Exception("Element <odrawmatrix> contains a string of " + size + " values, which is not an exact square");
    	}
    	int noCols = noRows;
    	for (int i=0; i <noRows; i++) {
    		int row = i * noRows;
    		for (int col=0; col < noCols; col++) {
    			updateMatrixDemand(matrixDemand, i+1, col+1, mode, Double.parseDouble(allValuesAsString[row + col]));
    		}
    	}
    }
 
/**
 * Update the demand matrix object from a generated OD row matrix
 * 
 * @param odrowmatrix        Odrowmatrix object generated from the input XML
 * @param mode                    Mode of travel
 * @param matrixDemand     MatrixDemand object to be updated
 */
    private void updateDemandMatrixFromOdRowMatrix(Odrowmatrix odrowmatrix, Mode mode, MatrixDemand matrixDemand) {
        String separator = (odrowmatrix.getDs() == null) ? Default.SEPARATOR : odrowmatrix.getDs();
        separator = escapeSeparator(separator);
        List<Odrowmatrix.Odrow> odrow = odrowmatrix.getOdrow();
        for (Odrowmatrix.Odrow originZone : odrow) {
         	String[] rowValuesAsString = originZone.getValue().split(separator);
         	for (int i=0; i<rowValuesAsString.length ; i++) {
         		updateMatrixDemand(matrixDemand, originZone.getRef().intValue(), i+1, mode, Double.parseDouble(rowValuesAsString[i]));
         	}
        }
    }
    
 /**
  * Update the demand matrix object from a generated cell by cell matrix 
  * 
  * @param odcellbycellmatrix      Odcellbycellmatrix object generated from the input XML
  * @param mode                           Mode of travel
  * @param matrixDemand            MatrixDemand object to be updated
  */
    private void updateDemandMatrixFromCellByCellMatrix(Odcellbycellmatrix odcellbycellmatrix, Mode mode, MatrixDemand matrixDemand) {
        List<Odcellbycellmatrix.O> o = odcellbycellmatrix.getO();
        for (Odcellbycellmatrix.O originZone : o) {
        	List<Odcellbycellmatrix.O.D> d = originZone.getD();
        	for (Odcellbycellmatrix.O.D demandZone : d) {
        		updateMatrixDemand(matrixDemand, originZone.getRef().intValue(), demandZone.getRef().intValue(), mode, demandZone.getValue());
        	}
        }
    }
    
/**
 * Update the demand matrix object with the input value for the current row and column
 * 
 * @param matrixDemand      the MatrixDemand object to be updated
 * @param rowRef                   reference to the row (origin) for the current demand value
 * @param colRef                    reference to the column (destination) for the current demand value
 * @param mode                     Mode of travel
 * @param demandValue        current demand value (in PCU)
 */
    private void updateMatrixDemand(MatrixDemand matrixDemand, int rowRef, int colRef, Mode mode, double demandValue) {
		long originZoneId = zones.getZoneByExternalId(rowRef).getId();
		long destinationZoneId = zones.getZoneByExternalId(colRef).getId();
		double demand = demandValue * mode.getPcu();
		matrixDemand.set(originZoneId, destinationZoneId, demand);
    }
    
/**
 * Create the Map of demands for each time period and mode
 * 
 * @param timePeriodMap                     Map of time periods
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
