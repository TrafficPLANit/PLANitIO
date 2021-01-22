package org.planit.io.xml.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.planit.utils.exceptions.PlanItException;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * Utility methods for parsing XML data
 * 
 * @author gman6028
 *
 */
public class JAXBUtils {
    
  /** the logger */
  public static final Logger LOGGER = Logger.getLogger(JAXBUtils.class.getCanonicalName());   
  
	/**
	 * Method to validate an XML input file against an XSD schema using Java XML
	 * Validator
	 * 
	 * @param xmlFileLocation    location of the XML input file
	 * @param schemaFileLocation location of the XSD schema file to validate the XML
	 *                           against
	 * @throws Exception thrown if the input file fails the validation
	 */
	public static void validateXml(File xmlFileLocation, String schemaFileLocation) throws Exception {
		LOGGER.fine("validating " + xmlFileLocation.getAbsolutePath() + " against " + schemaFileLocation);
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
	public static Object generateObjectFromXml(Class<?> clazz, File xmlFileLocation) throws Exception {
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
	 * @param object input object containing the data to be written to the XML file
	 * @param clazz Class of the object containing the data
	 * @param xmlFileLocation location of the output XML file
	 * @param noNameSpaceUri the namespace uri to use for the default namespace schema (no prefix), not used when null 
	 * @throws Exception thrown if the object is not of the correct class, or the
	 *                   output file cannot be opened
	 */
	public static void generateXmlFileFromObject(final Object object, Class<?> clazz, final Path xmlFileLocation, final String noNameSpaceUri) throws Exception {
    if (!clazz.isInstance(object)) {
      throw new PlanItException("Trying to convert an object to XML which is not of class " + clazz.getName());
    }
    
    OutputStream outputStream = new FileOutputStream(xmlFileLocation.toFile());
    try {          
      JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
      Marshaller marshaller = jaxbContext.createMarshaller();
      
      /* ensure correct namespace prefixes are used when writing */
      setPlanitNamespacePrefixes(marshaller);    
      
      if(noNameSpaceUri != null && !noNameSpaceUri.isBlank()) {
        marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, noNameSpaceUri);
      }

      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(object,outputStream);      
    }catch(Exception e) {
      outputStream.close();
      throw e;
    }
    outputStream.close();    
	}
	
  /**
   * Let any marshaller use the PLANit preferred prefixes for namespaces that it uses. the namespaces used
   * are based on a custom implementation of a NamespacePrefixMapper {@link PlanitNamespacePrefixMapper}
   * 
   * @param marshaller to inject mapper on
   * @throws PropertyException thrown if error setting properties
   */
  public static void setPlanitNamespacePrefixes(Marshaller marshaller) throws PropertyException {
    
    NamespacePrefixMapper mapper = new PlanitNamespacePrefixMapper();
    /* this package depends on the Java implementation, alternatively try 
     * com.sun.xml.internal.bind.namespacePrefixMapper if this fails */
    try {
      marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", mapper);
    }catch(Exception e) {
      marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", mapper);
    }
  }

  /** create populated instance of class based from the first compatible potential files
   * @param <T> raw XML to find
   * @param clazz of type T
   * @param potentialXmlFileNames to search among
   * @return parsed result, null if not found
   */
  public static <T> T generateInstanceFromXml(Class<T> clazz, final File[] potentialXmlFileNames) {
    T result=null;
    for (int i = 0; i < potentialXmlFileNames.length; i++) {
      File currFileName = potentialXmlFileNames[i];
      if (result==null) {
        try {
          result = clazz.cast(JAXBUtils.generateObjectFromXml(clazz, currFileName));
        } catch (final Exception e) {
          /* ok, just try next */   
        }                  
      }else {
        LOGGER.info("parsed file " + currFileName);
        break;
      }
    }    
    return result;
  }	
	
}