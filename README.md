# PLANitXML
XML data I/O format parsers for PLANit projects

## Maven JAXB2 Plugin

This project uses JAXB to generate Java classes from XSD files.  JAXB is run using the Maven JAXB2 Plugin (https://github.com/highsource/maven-jaxb2-plugin).

The Maven JAXB2 Plugin is run by running Maven with goals "clean install" on the code.

## XSD and XJB Files

XJB files are used by JAXB to configure how JAXB creates Java files and to resolve naming clashes.

The src/main/resources directory contains the following files which are used by JAXB.  XSD files are in subdirectory "xsd" and XJB files are in subdirectory "xjb":

|File|Purpose|
|---| ---|
|planitlibrary.xsd|Common XML definitions used by the other XSD files|
|macroscopicinput.xsd|XSD schema defining the top-level <PLANit> element which contains the <macroscopicnetwork>, <macroscopicdemand> and <macroscopiczoning> elements
|macroscopicdemandinput.xsd|XSD schema for demand input XML, defines the <macroscopicdemand> element|
|macroscopiczoninginput.xsd|XSD schema for zoning input XML, defines the <macroscopiczoning> element|
|macroscopicnetworkinput.xsd|XSD schema for network input XML, defines the <macroscopicnetwork> element|
|linkmetadata.xsd|XSD schema used for XML output|
|configuration.xjb|XJB file to define the package name for the generated Java classes which correspond to the definitions in the project's own XSD files|
|macroscopicdemandinput.xjb|XJB file to define the names of the generated Java classes related to demand input|
|macroscopiczoninginput.xjb|XJB file to define the names of the generated Java classes related to zoning input|
|macroscopicnetworkinput.xjb|XJB file to define the names of the generated Java classes related to network input|
|linkmetadata.xjb|XJB file to define the names of the generated Java classes related to output|    
|gml-v_3_1_1.xjb|XJB file to resolve name clashes which appear when XJC is run on GML files|
|xlink-v_1_0.xjb|XJB file to resolve name clashes which appear when XJC is run on GML files|                

The last two files are taken from the OCG Schemas project (https://github.com/highsource/ogc-schemas).  This project was created to address known problems which arise when running the XJC 
compiler on schemas defined by the Open Geospatial Consortium (OGC).  PLANit uses GML, which is one of the OGC schemas.  If these XJB files were not present, the Maven-JAXB2-Plugin would 
not generate Java classes.

The XJB files had the following additions for this project:

gml-v_3_1_1.xjb:
```xjb

		<jaxb:bindings node="xs:element[@name='Arc']">
			<jaxb:factoryMethod name="GmlArc"/>
		</jaxb:bindings>
		
		<jaxb:bindings node="xs:element[@name='animate']">
			<jaxb:factoryMethod name="SmilAnimate"/>
		</jaxb:bindings>
		<jaxb:bindings node="xs:element[@name='animateMotion']">
			<jaxb:factoryMethod name="SmilAnimateMotion"/>
		</jaxb:bindings>
		<jaxb:bindings node="xs:element[@name='animateColor']">
			<jaxb:factoryMethod name="SmilAnimateColor"/>
		</jaxb:bindings>
		<jaxb:bindings node="xs:element[@name='set']">
			<jaxb:factoryMethod name="SmilSet"/>
		</jaxb:bindings>
```

## Generated Java classes

All Java classes generated from XSD code are placed in the "org.planit.generated" package and given names which begin with "XMLElement".  These names are set in the XJB files.

The build also creates several enumerations for input values, and a class "ObjectFactory" which JAXB uses internally.

Do not edit the content or location of the generated Java classes.  These classes get rewritten every time you do a Maven build of the PLANitXML project.  They reflect the state of the XSD files.  If you need to make changes to the format of the XML input or output, change the relevant XSD and XJB files accordingly and re-run the Maven build.

Due to the OCG Schemas project mentioned above, JAXB also creates several "extra" packages which you do not need, namely:

* net.opengis.gml;
* org.w3._1999.xlink;
* org.w3._2001.smil20;
* org.w3._2001.smil20.language.

You can ignore the contents of all these packages, but do not delete them.  You will get compilation error if you delete any of their contents.

The XmlUtils class contains the following methods for reading and writing XML files:-

* generateObjectFromXml()  - used to populate a generated Java class from its corresponding XML input file;
* generateXmlFileFromObject()  - used to generate an XML output file from the data contained in the generated Java class. 

The generated Java classes can be accessed in the code like any Java classes.  They only contain getter and setter methods which are used to hold data, they contain no business logic.  

It is recommended that the generated Java classes only be used to populate PLANit's own business  objects (network, zoning etc) and they not be passed directly to any business logic.  Developers familiar with the concept of Data Transfer Objects will recognize how the generated classes fit this pattern.  The PlanItXMLInputBuilder performs this data transfer.

## Input File Format

The PLANitXML input file consists of the following elements:

```
<PLANit>
	<macroscopicnetwork>
		<linkconfiguration>
		</linkconfiguration>
		<infrastructure>
		</infrastructure>
	</macroscopicnetwork>
	<macroscopicdemand>
		<demandconfiguration>
		</demandconfiguration>
		<oddemands>
		</oddemands>
	</macroscopicdemand>
	<macroscopiczoning>
		<zones>
		</zones>
	</macroscopiczoning>
</PLANit>
```

