package org.planit.xml.process;

import java.io.FileReader;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * Interface containing methods which process XML files using JAXB or Java XML Validator APIs
 * 
 * @author gman6028
 *
 */
public interface XmlProcessor {

    static final Logger LOGGER = Logger.getLogger(XmlProcessor.class.getName());
	
/**
 * Method to validate an XML input file against an XSD schema using Java XML Validator
 *  
 * @param xmlFileLocation          location of the XML input file
 * @param schemaFileLocation   location of the XSD schema file to validate the XML against
 * @throws Exception                   thrown if the input file fails the validation
 */
	public static void validateXml(String xmlFileLocation, String schemaFileLocation) throws Exception {
        LOGGER.info("Validating " + xmlFileLocation + " against " + schemaFileLocation);  
		String schemaLang = "http://www.w3.org/2001/XMLSchema";
		SchemaFactory factory = SchemaFactory.newInstance(schemaLang);
		Schema schema = factory.newSchema(new StreamSource(schemaFileLocation));
		Validator validator = schema.newValidator();
        validator.validate(new StreamSource(xmlFileLocation));
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
	public static Object generateObjectFromXml(Class<?> clazz, String xmlFileLocation) throws Exception {
	   	JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
	   	Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
	   	XMLInputFactory xmlinputFactory = XMLInputFactory.newInstance();
	   	XMLStreamReader xmlStreamReader = xmlinputFactory.createXMLStreamReader(new FileReader(xmlFileLocation));
	   	Object obj = unmarshaller.unmarshal(xmlStreamReader);
	   	xmlStreamReader.close();
	    return obj;
	} 
	    

}
