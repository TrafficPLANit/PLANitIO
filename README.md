# PLANitIO
Default PLANit IO project which uses XML input and generates XML and CSV output

## Maven JAXB2 Plugin

This project uses JAXB to generate Java classes from XSD files.  JAXB is run using the Maven JAXB2 Plugin (https://github.com/highsource/maven-jaxb2-plugin).

The Maven JAXB2 Plugin is run by running Maven with goals "clean install" on the code.

## Testing Documentation ##

The "docs" directory contains the documents:- 

- *Unit Tests and Integration Tests.md*, which describes the test suite which has been written to PLANitIO;
- *Generating JavaDoc for a Java Project in Eclipse.md*, which explains how to create the documentation for the Java code.

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

The Maven build uses JAXB to generate Java classes from the XSD schema files described in the previous section.  These classes generated  are placed in package named "org.planit.generated".  The Java classes are all given names which begin with "XMLElement".  These names are set in the XJB files.

The generated Java classes and org.planit.generated package are not stored in Git.  Do not directly edit their content or location.  These classes get rewritten every time you do a Maven build of the PLANitXML project.  They reflect the state of the XSD files.  If you need to make changes to the format of the XML input or output, change the relevant XSD and XJB files accordingly and re-run the Maven build.

The build also creates several enumerations for input values, and a class "ObjectFactory" which JAXB uses internally.

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

It is recommended that the generated Java classes only be used to populate PLANit's own business objects (network, zoning etc) and not be passed directly to any business logic.  Developers familiar with the concept of Data Transfer Objects will recognize how the generated classes fit this pattern.  The PlanItXMLInputBuilder performs this data transfer.

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

|Link Segment Id|Mode External Id|Mode Id|Node Downstream External Id|Node Upstream External Id|Capacity per Lane|Length|Number of Lanes|Cost|Density|Flow|Speed|
|---|---|---|---|---|---|---|---|---|---|---|---|
|0	|1|0|2|1|2000|10|1|10|180|1|1|

## More Complicated PLANitXML Input File

The following input file defines a more complicated test case:

```
<PLANit xmlns:gml="http://www.opengis.net/gml"
	xmlns:xml="http://www.w3.org/XML/1998/namespace"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:noNamespaceSchemaLocation="../../../../../main/resources/macroscopicinput.xsd">

	<!-- macroscopicdemand element defined macroscopicdemandinput.xsd file -->

	<macroscopicdemand>

		<demandconfiguration>
			<userclasses>
				<userclass id="1" moderef="1">  <!-- moderef corresponds to id of <mode> element below -->
					<name>1</name>
				</userclass>
			</userclasses>
			<timeperiods>
				<timeperiod id="0">
					<name>Time Period 1</name>
					<starttime>00:00:01</starttime>
					<duration>43200</duration>
				</timeperiod>
				<timeperiod id="1">
					<name>Time Period 2</name>
					<starttime>12:00:01</starttime>
					<duration>43200</duration>
				</timeperiod>
			</timeperiods>
		</demandconfiguration>

		<oddemands>
			<odrowmatrix timeperiodref="0" userclassref="1">  <!-- timeperiodref and userclassref refer to id numbers set in <demandconfiguration> element above  -->
				<odrow ref="1">100,200,300,400</odrow>
				<odrow ref="2">200,400,600,800</odrow>
				<odrow ref="3">300,600,900,1200</odrow>
				<odrow ref="4">400,800,1200,1600</odrow>
			</odrowmatrix>
			<odrowmatrix timeperiodref="1" userclassref="1">
				<odrow ref="1">100,200,300,400</odrow>
				<odrow ref="2">200,400,600,800</odrow>
				<odrow ref="3">300,600,900,1200</odrow>
				<odrow ref="4">400,800,1200,1600</odrow>
			</odrowmatrix>
		</oddemands>
	</macroscopicdemand>
	
<!-- macroscopicnetwork element defined in the macroscopicnetworkinput.xsd file -->
		
	<macroscopicnetwork>
		<linkconfiguration>
			<modes>
				<mode id="1">																<!-- mandatory attributes, for referencing -->
					<name>Basic</name>														<!-- optional -->
					<pcu>1</pcu>															<!-- optional, default available -->
				</mode>
			</modes>
			<linksegmenttypes>
				<linksegmenttype id="1">
					<name>Regular</name>													<!-- optional -->
					<capacitylane>1500</capacitylane>										<!-- optional, has default -->
					<modes>
						<mode ref="1">															<!-- optional, when 0 it refers to all modes, when a mode is absent the mode is assumed to be banned from the link segment -->
							<maxspeed>100</maxspeed>												<!-- mandatory, should be lower or equal than link max speed when present -->
						</mode>
					</modes>
				</linksegmenttype>
				<linksegmenttype id="2">
					<name>Connector</name>													<!-- optional -->
					<capacitylane>1000</capacitylane>										<!-- optional, has default -->
					<modes>
						<mode ref="1">															<!-- optional, when 0 it refers to all modes, when a mode is absent the mode is assumed to be banned from the link segment -->
							<maxspeed>50</maxspeed>												<!-- mandatory, should be lower or equal than link max speed when present -->
						</mode>
					</modes>
				</linksegmenttype>
			</linksegmenttypes>
		</linkconfiguration>
		<infrastructure>
			<nodes>
				<node id="1" />
				<node id="2" />
				<node id="3" />
				<node id="4" />
				<node id="5" />
				<node id="6" />
				<node id="7" />
				<node id="8" />
				<node id="9" />
				<node id="10" />
				<node id="11" />
				<node id="12" />
				<node id="13" />
				<node id="14" />
				<node id="15" />
				<node id="16" />
				<node id="21" />
				<node id="22" />
				<node id="23" />
				<node id="24" />
			</nodes>
			<links>
				<link nodearef="12" nodebref="9">
					<linksegment id="1" dir="a_b" typeref="1">    <!-- typeref values correspond to the id of the <linksegmenttype> elements defined above --->
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="2" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>2.9</length>
				</link>				
				<link nodearef="12" nodebref="11">
					<linksegment id="3" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="4" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>3</length>
				</link>				
				<link nodearef="12" nodebref="16">
					<linksegment id="5" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="6" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>				
				<link nodearef="9" nodebref="10">
					<linksegment id="7" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="8" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>2.9</length>
				</link>			
				<link nodearef="9" nodebref="13">
					<linksegment id="9" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="10" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>			
				<link nodearef="10" nodebref="11">
					<linksegment id="11" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="12" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>3</length>
				</link>			
				<link nodearef="10" nodebref="14">
					<linksegment id="13" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="14" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>				
				<link nodearef="11" nodebref="15">
					<linksegment id="15" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="16" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>			
				<link nodearef="1" nodebref="5">
					<linksegment id="17" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="18" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>				
				<link nodearef="1" nodebref="4">
					<linksegment id="19" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="20" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>0.9</length>
				</link>				
				<link nodearef="1" nodebref="2">
					<linksegment id="21" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="22" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>0.9</length>
				</link>			
				<link nodearef="2" nodebref="6">
					<linksegment id="23" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="24" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>				
				<link nodearef="2" nodebref="3">
					<linksegment id="25" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="26" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>			
				<link nodearef="3" nodebref="7">
					<linksegment id="27" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="28" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>			
				<link nodearef="3" nodebref="4">
					<linksegment id="29" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="30" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>			
				<link nodearef="4" nodebref="8">
					<linksegment id="31" dir="a_b" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<linksegment id="32" dir="b_a" typeref="1">
						<numberoflanes>1</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>			
				<link nodearef="16" nodebref="23">
					<linksegment id="33" dir="a_b" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<linksegment id="34" dir="b_a" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>				
				<link nodearef="23" nodebref="8">
					<linksegment id="35" dir="a_b" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<linksegment id="36" dir="b_a" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>			
				<link nodearef="13" nodebref="21">
					<linksegment id="37" dir="a_b" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<linksegment id="38" dir="b_a" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>				
				<link nodearef="21" nodebref="5">
					<linksegment id="39" dir="a_b" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<linksegment id="40" dir="b_a" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>				
				<link nodearef="14" nodebref="22">
					<linksegment id="41" dir="a_b" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<linksegment id="42" dir="b_a" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>				
				<link nodearef="22" nodebref="6">
					<linksegment id="43" dir="a_b" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<linksegment id="44" dir="b_a" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>			
				<link nodearef="15" nodebref="24">
					<linksegment id="45" dir="a_b" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<linksegment id="46" dir="b_a" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>				
				<link nodearef="24" nodebref="7">
					<linksegment id="47" dir="a_b" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<linksegment id="48" dir="b_a" typeref="2">
						<numberoflanes>10</numberoflanes>
					</linksegment>
					<length>1</length>
				</link>
			</links>
		</infrastructure>
	</macroscopicnetwork>
	
<!-- macroscopiczoning element defined in the macroscopiczoninginput.xsd file -->
	
	<macroscopiczoning>
		<zones>
			<zone id="1">
				<centroid>
					<name>1</name>
					<gml:Point>									<!-- Mandatory Location of the centroid -->
						<gml:pos>45.256 -110.45</gml:pos>
					</gml:Point>
				</centroid>
				<connectoids>
					<connectoid noderef="21">
						<length>1.0</length>
					</connectoid>
				</connectoids>
			</zone>
			<zone id="2">
				<centroid>
					<name>2</name>
					<gml:Point>									<!-- Mandatory Location of the centroid -->
						<gml:pos>45.256 -110.45</gml:pos>
					</gml:Point>
				</centroid>
				<connectoids>
					<connectoid noderef="22">
						<length>1.0</length>
					</connectoid>
				</connectoids>
			</zone>
			<zone id="3">
				<centroid>
					<name>3</name>
					<gml:Point>									<!-- Mandatory Location of the centroid -->
						<gml:pos>45.256 -110.45</gml:pos>
					</gml:Point>
				</centroid>
				<connectoids>
					<connectoid noderef="23">
						<length>1.0</length>
					</connectoid>
				</connectoids>
			</zone>
			<zone id="4">
				<centroid>
					<name>4</name>
					<gml:Point>									<!-- Mandatory Location of the centroid -->
						<gml:pos>45.256 -110.45</gml:pos>
					</gml:Point>
				</centroid>
				<connectoids>
					<connectoid noderef="24">
						<length>1.0</length>
					</connectoid>
				</connectoids>
			</zone>
		</zones>
	</macroscopiczoning>
</PLANit>
```
This example runs to generate the following output:

|Link Segment Id|Mode External Id|Mode Id|Node Downstream External Id|Node Upstream External Id|Capacity per Lane|Length|Number of Lanes|Cost|Density|Flow|Speed|
|---|---|---|---|---|---|---|---|---|---|---|---|
|0|1|0|1|11|1200|1|3|0.0370117|180|3000|27.0184697|
|1|1|0|4|1|1200|1|1|0.0717191|180|1926|13.9432871|
|2|1|0|12|4|1200|1|3|0.0370117|180|3000|27.0184697|
|3	|1	|0	|2	|1|1200|2|1|0.0448544|180|6|44.5887278|
|4	|1|0|4|2|1200|2|1|0.0448544|180|6|44.5887278|
|5|1|0|3|1|1200|1|1|0.0360507|180|1068|27.7387072|
|6|1|0|4|3|1200|1|1|0.0360507|180|1068|27.7387072|
|0|2|1|1|11|1200|1|3|0.0636732|180|1500|15.705194|
|2|2|1|12|4|1200|1|3|0.0636732|180|1500|15.705194|
|3|2|1|2|1|1200|2|1|0.0611216|180|1086|32.721643|
|4|2|1|4|2|1200|2|1|0.0611216|180|1086|32.721643|
|5|2|1|3|1|1200|1|1|0.0610912|180|414|16.3689599|
|6|2|1|4|3|1200|1|1|0.0610912|180|414|16.3689599|
