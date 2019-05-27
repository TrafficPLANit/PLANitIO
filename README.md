# PLANitXML
XML data I/O format parsers for PLANit projects

## Maven JAXB2 Plugin

This project uses JAXB to generate Java classes from XSD files.  JAXB is run using the Maven JAXB2 Plugin (https://github.com/highsource/maven-jaxb2-plugin).

The Maven JAXB2 Plugin is run by running Maven with goals "clean install" on the code.

## XSD and XJB Files

XJB files are used by JAXB to configure how JAXB creates Java files and to resolve naming clashes

The src/main/resources directory contains the following files which are used by JAXB:


|---| ---|
|PLANit.xsd|Common XML definitions used by the other XSD file|
|macroscopicdemandinput.xsd|XSD definition for demand input XML file|
|macroscopiczoninginput.xsd|XSD definition for zoning input XML file|
|macroscopicsupplyinput.xsd|XSD definition for supply input XML file|
|PLANit.xjb|XJB file to define the package name for the generated Java classes which correspond to the definitions in the project's own XSD files|
|gml-v_3_1_1.xjb|XJB file to resolve name clashes which appear when XJC is run on GML files|
| xlink-v_1_0.xjb|XJB file to resolve name clashes which appear when XJC is run on GML files|                        

The last two files are taken from the OCG Schemas project (https://github.com/highsource/ogc-schemas).  This project was created to address known problems which arise when running the XJC 
compiler on schemas defined by the Open Geospatial Consortium (OGC).  PLANit uses GML, which is one of the OGC schemas.  If the XJB files were not present, the Maven-JAXB2-Plugin would 
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

