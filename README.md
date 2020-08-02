# PLANitIO

PLANit IO project which uses XML input and generates XML and CSV output. this reflects the default input and output format of PLANit.

For more information on PLANit such as the user the manual, licensing, installation, getting started, reference documentation, and more, please visit [https://trafficplanit.github.io/PLANitManual/](https://trafficplanit.github.io/PLANitManual/)

## Maven parent

Projects need to be built from Maven before they can be run. The common maven configuration can be found in the PLANitAll project which acts as the parent for this project's pom.xml.

> Make sure you install the PLANitAll pom.xml before conducting a maven build (in Eclipse) on this project, otherwise it cannot find the references dependencies, plugins, and other resources.

## PLANitXMLGenerator

This project relies on PLANitXMLGenerator to provide the Java classes that are compatible with the underlying XML schemas and inputs that are parsed. Whenever the schema changes re-generate the classes via this separate project. If you do not, a mismatch between the used classes and the underlying xml input occurs causing failure in parsing PLANit inputs.

### Generated Java classes

The XmlUtils class contains the following methods for reading and writing XML files based on the results of PLANitXMLGenerator:-

* generateObjectFromXml()  - used to populate a generated Java class from its corresponding XML input file;
* generateXmlFileFromObject()  - used to generate an XML output file from the data contained in the generated Java class. 

The generated Java classes can be accessed in the code like any Java classes.  They only contain getter and setter methods which are used to hold data, they contain no business logic.  

It is recommended that the generated Java classes only be used to populate PLANit's own business objects (network, zoning etc) and not be passed directly to any business logic.  Developers familiar with the concept of Data Transfer Objects will recognize how the generated classes fit this pattern.  The PlanItXMLInputBuilder performs this data transfer.
 
## Testing Documentation ##

The "docs" directory contains the documents:- 

- *Unit Tests and Integration Tests.md*, which describes the test suite which has been written to PLANitIO;
- *Generating JavaDoc for a Java Project in Eclipse.md*, which explains how to create the documentation for the Java code.
