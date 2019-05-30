package org.planit.xml.input;

import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.planit.basiccsv.network.physical.macroscopic.BasicCsvMacroscopicLinkSegmentType;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.demand.Demands;
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
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.xml.demands.ProcessConfiguration;
import org.planit.xml.demands.UpdateDemands;
import org.planit.xml.input.validation.XmlValidator;
import org.planit.xml.network.ProcessLinkConfiguration;
import org.planit.xml.zoning.UpdateZoning;
import org.planit.zoning.Zoning;

import org.planit.generated.Configuration;
import org.planit.generated.Infrastructure;
import org.planit.generated.Linkconfiguration;
import org.planit.generated.Macroscopicdemand;
import org.planit.generated.Macroscopicnetwork;
import org.planit.generated.Macroscopiczoning;
import org.planit.generated.Odmatrix;
import org.planit.generated.Zones.Zone;

/**
 * Class which reads inputs from XML input files
 * 
 * @author gman6028
 *
 */
public class PlanItXml implements InputBuilderListener  {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(PlanItXml.class.getName());
        
    private static final int ONE_WAY_AB =  1;
    private static final int ONE_WAY_BA =  2;
    private static final int TWO_WAY = 3;
     
    private String networkFileLocation;
    private String linkTypesFileLocation;
    private String modeFileLocation;
    private String zoningXmlFileLocation;
    private String demandXmlFileLocation;
    private String networkXmlFileLocation;
    
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
  * Constructor which reads in the XML input files 
  * 
  * @param zoningXmlFileLocation      location of XML zones input file
  * @param demandXmlFileLocation   location of XML demands input file
  * @param networkXmlFileLocation   location of XML network inputs file
  */
    public PlanItXml(String networkFileLocation, 
						         String linkTypesFileLocation, 
							     String modeFileLocation,
							     String zoningXmlFileLocation,
							     String demandXmlFileLocation,
							     String networkXmlFileLocation) {
        this.networkFileLocation = networkFileLocation;
        this.linkTypesFileLocation = linkTypesFileLocation;
        this.modeFileLocation = modeFileLocation;
    	this.zoningXmlFileLocation = zoningXmlFileLocation;
    	this.demandXmlFileLocation = demandXmlFileLocation;
    	this.networkXmlFileLocation = networkXmlFileLocation;
    }

/**
 * Constructor which reads in the XML input files and XSD files to validate them against
 * 
 * If a null value is entered for the location of an XSD file, no validation is carried out on the corresponding input XML file.
 * 
 * @param zoningXmlFileLocation      location of XML zones input file
 * @param demandXmlFileLocation   location of XML demands input file
 * @param networkXmlFileLocation   location of XML network inputs file
 * @param zoningXsdFileLocation       location of XSD schema file for zones
 * @param demandXsdFileLocation    location of XSD schema file for demands
 * @param networkXsdFileLocation    location of XSD schema file for network
 * @throws PlanItException                 thrown if one of the input XML files is invalid
 */
    public PlanItXml(String networkFileLocation, 
					             String linkTypesFileLocation, 
						         String modeFileLocation,
						         String zoningXmlFileLocation,
						         String demandXmlFileLocation,
						         String networkXmlFileLocation,
						         String zoningXsdFileLocation,
						         String demandXsdFileLocation,
						         String networkXsdFileLocation) throws PlanItException {
    	this(networkFileLocation, linkTypesFileLocation, modeFileLocation, zoningXmlFileLocation, demandXmlFileLocation, networkXmlFileLocation);
    	try {
    		if (zoningXsdFileLocation != null) {
    			XmlValidator.validateXml(zoningXmlFileLocation, zoningXsdFileLocation);
    		}
    		if (demandXsdFileLocation != null) {
    			XmlValidator.validateXml(demandXmlFileLocation, demandXsdFileLocation);
    		}
    		if (networkXsdFileLocation != null) {
    			XmlValidator.validateXml(networkXmlFileLocation, networkXsdFileLocation);
    		}
    	} catch (Exception e) {
    		throw new PlanItException(e);
    	}
    }
    
/**
 * Handles creation events for network, zones, demands and travel time parameters   
 * 
 * We only handle BPR travel time functions at this stage.
 * 
 * @param event                      object creation event
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
 * @param network                       the physical network object
 * @param record                          the record in the network input file which refers to this node
 * @param columnName               the column in the record which refers to this node
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
 * @param network                      the physical network object
 * @param link                             the link from which the link segment will be created
 * @param abDirection                direction of travel
 * @param linkSegmentType        object storing the input values for this link
 * @param noLanes                      the number of lanes in this link
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
 * @return                                    Map containing link type values
 * @throws PlanItException         thrown if there is an error reading the input file
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
       
        try {
        	Macroscopicnetwork macroscopicnetwork = (Macroscopicnetwork) generateObjectFromXml(Macroscopicnetwork.class, networkXmlFileLocation);
        	Linkconfiguration linkconfiguration = macroscopicnetwork.getLinkconfiguration();
        	Infrastructure infrastructure = macroscopicnetwork.getInfrastructure();
        	modeMap = ProcessLinkConfiguration.getModeMap(linkconfiguration);
        	//TODO - finish ProcessLinkConfiguration.createLinkSegmentTypeMap() which goes here
        } catch (Exception ex) {
            throw new PlanItException(ex);
        }

        //modeMap = getModes();       
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
        
        //create and register zones, centroids and connectoids
    	try {
    		Macroscopiczoning macroscopiczoning = (Macroscopiczoning) generateObjectFromXml(Macroscopiczoning.class, zoningXmlFileLocation);
	    	for (Zone zone : macroscopiczoning.getZones().getZone()) {
                Centroid  centroid = UpdateZoning.createAndRegisterZoneAndCentroid(zoning, zone);
                UpdateZoning.registerNewConnectoid(zoning, nodes, zone, centroid);
                noCentroids++;
 	    	}
            zones = zoning.zones;
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw new PlanItException(e);
    	}        
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
    		Map<Integer, TimePeriod> timePeriodMap = ProcessConfiguration.generateAndStoreConfigurationData(configuration);
	        List<Odmatrix> oddemands = macroscopicdemand.getOddemands().getOdcellbycellmatrixOrOdrowmatrixOrOdrawmatrix();
	        UpdateDemands.createAndRegisterDemandMatrix(demands, oddemands, modeMap, timePeriodMap, noCentroids, zones);
   	    } catch (Exception e) {
            e.printStackTrace();
    		throw new PlanItException(e);
    	}
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
