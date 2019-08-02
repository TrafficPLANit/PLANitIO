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

## Basic PLANitXML Input File

The simplest possible example of a PLANitXML input file.  This example only has one user class and time period and one two-way link, from node 1 to node 2:

```
<PLANit xmlns:gml="http://www.opengis.net/gml"	xmlns:xml="http://www.w3.org/XML/1998/namespace"	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"	xsi:noNamespaceSchemaLocation="../../../../../main/resources/macroscopicinput.xsd">

<!-- macroscopicdemand element defined macroscopicdemandinput.xsd file -->

	<macroscopicdemand>
		<demandconfiguration>        <!-- definition of time periods and user classes -->
			<userclasses>                 
				<userclass id="1" moderef="1">
					<name>1</name>
				</userclass>
			</userclasses>
			<timeperiods>
				<timeperiod id="0">
					<name>Time Period 1</name>
					<starttime>00:00:01</starttime>
					<duration>86400</duration>
				</timeperiod>
			</timeperiods>
		</demandconfiguration>
		<oddemands>                            <!-- definition of origin-demand matrix, here defined cell by cell -->
			<odcellbycellmatrix timeperiodref="0"	userclassref="1">
				<o ref="1">
					<d ref="2">1</d>
				</o>
			</odcellbycellmatrix>
		</oddemands>
	</macroscopicdemand>
	
<!-- macroscopicnetwork element defined in the macroscopicnetworkinput.xsd file -->
	
	<macroscopicnetwork>
		<linkconfiguration>
			<modes>                                                <!-- definition of modes -->
				<mode id="1">																
					<name>Basic</name>														
					<pcu>1</pcu>															
				</mode>
			</modes>
			<linksegmenttypes>                              <!-- definition of types of link segment -->
				<linksegmenttype id="1">
					<name>Standard</name>													
					<capacitylane>2000</capacitylane>										
					<modes>
						<mode ref="0">															
							<maxspeed>1</maxspeed>												
						</mode>
					</modes>
				</linksegmenttype>
			</linksegmenttypes>
		</linkconfiguration>
		<infrastructure>
			<nodes>                                                                          <!-- list of nodes, each must have an id number -->
				<node id="1" />
				<node id="2" />
			</nodes>
			<links>
				<link nodearef="1" nodebref="2">                              <!-- nodearef and nodebref refer to nodes defined in the <nodes> element -->
					<linksegment id="1" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="2" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>10</length>
				</link>
			</links>
		</infrastructure>
	</macroscopicnetwork>
	
<!-- macroscopiczoning element defined in the macroscopiczoninginput.xsd file -->
	
	<macroscopiczoning>
		<zones>                                                                   <!-- definition of demand zones, including centroids and connectoids -->
			<zone id="1"> 
				<centroid>
					<name>1</name>
					<gml:Point>									
						<gml:pos>45.256 -110.45</gml:pos>
					</gml:Point>
				</centroid>
				<connectoids>
					<connectoid noderef="1">
						<length>1.0</length>
					</connectoid>
				</connectoids>
			</zone>
			<zone id="2">
				<centroid>
					<name>2</name>
					<gml:Point>									
						<gml:pos>45.256 -110.45</gml:pos>
					</gml:Point>
				</centroid>
				<connectoids>
					<connectoid noderef="2">
						<length>1.0</length>
					</connectoid>
				</connectoids>
			</zone>
		</zones>
	</macroscopiczoning>
</PLANit>	
```
 This example runs to generate the following output:
 
|Link Segment Id|Mode External Id|Mode Id|Node Downstream External Id|Node Upstream External Id|Capacity per Lane|Downstream Node Location|Length|Number of Lanes|Upstream Node Location|Cost	Density|Flow|Speed|
|0	|1	|0	|2|1|2000|Not Specified|10|	1|	Not Specified|10|180|1|1|

## More Complicated PLANitXML Input File

The following input file defines a more complicated test case
 
