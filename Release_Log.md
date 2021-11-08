# Release Log

PLANitIO  Releases

## 0.3.0

* refactored tests so we have a runner class that wraps the assignment and a helper class for the csv comparisons #14
* update default mode to car and use factory method in PLANit + update xsd so this can be persisted as well #8
* add support for parsing/writing transferzonegroups #16
* network writer does not persist crs properly. Should store epsg code, but stores name instead. this has been fixed #19
* coordinate reference system on zoning XML element is never parsed nor is content converted to network CRS. this has been fixed #12
* added tests for validating the correctness of reader/writer of intermodal component of zoning #15
* implemented intermodal writer to support direct persistence of both network and (transfer)zoning analogous to intermodal reader #18
* updated artifact id to conform with how this generally is setup, i.e. <application>-<subrepo> #21
* parsing of initial costs is unnecessarily slow. this has been improved #5
* add service network that can be defined on top of a physical network to represent service legs and service nodes (planit/#62)
* update packages to conform to new domain org.goplanit.* #23

## 0.2.0

* add LICENSE.TXT to each repository so it is clearly licensed (planit/#33)
* add support for parsing/persisting predefined modes
* add support for parsing default allowed link segment type modes based on their track type (road/rail) (planitio/#9)
* separated reading of network from inputbuilder into networkreader class such that we can separately parse networks consistent with the converter classes (planitio/#10)  
* allow an option to include XML validation in the Java if the user wishes (planitio/#4)
* allow External Ids (and xml ids) to be of String type (planitio/#1)
* centroid element must become optional, since it is not strictly needed (planitio/#2)
* support multi-layer networks in parser   

## 0.1.0

* moved to new repository (www.github.com/trafficplanit/PLANitIO
* split JAXB code generation off in its own repository (#7) - new repository is PLANitXMLGenerator

## 0.0.4

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