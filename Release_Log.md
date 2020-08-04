# Release Log

PLANitIO  Releases (formerly PLANitXML)

### 0.1.0

* moved to new repository (www.github.com/trafficplanit/PLANitIO
* split JAXB code generation off in its own repository (#7) - new repository is PLANitXMLGenerator

### 0.0.4

* PLANitIO now reads the value of <maxspeed> from <linksegment> and <linksegmenttype> and uses the smaller if they are both present and different (#12)
* Creation of bulky JAR file now done in PLANitALL project (#25)
* Refactor org.planit.planitio to org.planit.io (#4)
* Fixed bugs in Logging (#5)
* Generated JAR file contains all dependencies in full including PLANit and PLANitUtils (#6)
* PLANitIO now reads <maxspeed> from <linksegment> element (#12)
* When link segment has no link segment type specified, added a check if this is allowed (#14)
* Fixed inconsistency in auto-generated file names for csv and xml (use of underscores and spaces) (#19)
* Changed content of <csvdata> path in xml meta-data output to relative path to XML (#22)
* Demand configuration defaults are now verified and  populated correctly (#16)
* XSD reference added for OD and path output types  (#18)
* Added a description property to the outputconfiguration in Java and recorded in the metadata XML output file (#20)
* Location information on centroid no longer mandatory, default behaviour implemented (#17)
* Default link segment type now created (#11)
* Default mode now created (#10)
* Check that both link segments in a single link are in opposite directions (#13)

## 0.0.3

* Restructure resources directory to create easier to understand hierarchy (#1)

## 0.0.2

* Renamed PLANitXML to PLANitIO
* Refactored package names 
* Added OD OutputType to capture and record Skim matrices
* Added writing of Skim matrices to output formatters
* Common approach to adding and removing output properties from output formatters
* Some code refactored, particularly in BaseOutputFormatter and FileOutputFormatter to reuse common code for managing output between different output formatters
* Updated standard results files in unit tests to match updated output property names

## 0.0.1

First Release