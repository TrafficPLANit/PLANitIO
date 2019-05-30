package org.planit.xml.input.validation;

import java.util.logging.Logger;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

/**
 * Interface containing utility functions to validate XSD schema
 * 
 * @author gman6028
 *
 */
public interface XmlValidator {

    static final Logger LOGGER = Logger.getLogger(XmlValidator.class.getName());
	
/**
 * Method to validate an XML input file against an XSD schema
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

}
