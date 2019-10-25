package org.planit.planitio.xml.util;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.opengis.geometry.DirectPosition;
import org.planit.exceptions.PlanItException;
import org.planit.geo.PlanitGeoUtils;
import org.planit.logging.PlanItLogger;

import net.opengis.gml.PointType;

/**
 * Utility methods for parsing XML data
 * 
 * @author gman6028
 *
 */
public interface XmlUtils {

	/**
	 * Create GML position from generated PointType object
	 * 
	 * @param pointType PointType object storing the location, read in from an XML
	 *                  input file
	 * @return DirectPosition object storing the location
	 * @throws PlanItException thrown if there is an error during processing
	 */
	public static DirectPosition getDirectPositionFromPointType(PlanitGeoUtils planitGeoUtils, PointType pointType)
			throws PlanItException {
		List<Double> value = pointType.getPos().getValue();
		return planitGeoUtils.getDirectPositionFromValues(value.get(0), value.get(1));
	}

	/**
	 * Method to validate an XML input file against an XSD schema using Java XML
	 * Validator
	 * 
	 * @param xmlFileLocation    location of the XML input file
	 * @param schemaFileLocation location of the XSD schema file to validate the XML
	 *                           against
	 * @throws Exception thrown if the input file fails the validation
	 */
	public static void validateXml(String xmlFileLocation, String schemaFileLocation) throws Exception {
		PlanItLogger.info("Validating " + xmlFileLocation + " against " + schemaFileLocation);
		String schemaLang = "http://www.w3.org/2001/XMLSchema";
		SchemaFactory factory = SchemaFactory.newInstance(schemaLang);
		Schema schema = factory.newSchema(new StreamSource(schemaFileLocation));
		Validator validator = schema.newValidator();
		validator.validate(new StreamSource(xmlFileLocation));
	}

	/**
	 * Generates a Java object populated with the data from an XML input file.
	 * 
	 * This method creates a JAXB Unmarshaller object which it uses to populate the
	 * Java class.
	 * 
	 * The output object will be of a generated class, created from the same XSD
	 * file which is used to validate the input XML file.
	 * 
	 * @param clazz           Class of the object to be populated
	 * @param xmlFileLocation location of the input XML file
	 * @return an instance of the output class, populated with the data from the XML
	 *         file.
	 * @throws Exception thrown if the XML file is invalid or cannot be opened
	 */
	public static Object generateObjectFromXml(Class<?> clazz, String xmlFileLocation) throws Exception {
		FileReader fileReader = new FileReader(xmlFileLocation);
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(fileReader);
		JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		Object obj = unmarshaller.unmarshal(xmlStreamReader);
		xmlStreamReader.close();
		fileReader.close();
		return obj;
	}

	/**
	 * Creates an XML output file populated with data from an Object
	 * 
	 * @param object          input object containing the data to be written to the
	 *                        XML file
	 * @param clazz           Class of the object containing the data
	 * @param xmlFileLocation location of the output XML file
	 * @throws Exception thrown if the object is not of the correct class, or the
	 *                   output file cannot be opened
	 */
	public static void generateXmlFileFromObject(Object object, Class<?> clazz, String xmlFileLocation)
			throws Exception {
		FileWriter fileWriter = new FileWriter(xmlFileLocation);
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
		XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(fileWriter);
		generateXmlFileFromObject(object, clazz, xmlStreamWriter);
		xmlStreamWriter.close();
		fileWriter.close();
	}

	/**
	 * Creates an XML stream writer populated with data from an Object
	 * 
	 * @param object          input object containing the data to be written to the
	 *                        XML file
	 * @param clazz           Class of the object containing the data
	 * @param xmlStreamWriter XMLStreamWriter populated with data
	 * @throws Exception thrown if the object is not of the correct class, or the
	 *                   output file cannot be opened
	 */
	public static void generateXmlFileFromObject(Object object, Class<?> clazz, XMLStreamWriter xmlStreamWriter)
			throws Exception {
		if (!clazz.isInstance(object)) {
			throw new Exception("Trying to convert an object to XML which is not of class " + clazz.getName());
		}
		JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.marshal(object, xmlStreamWriter);
	}

}