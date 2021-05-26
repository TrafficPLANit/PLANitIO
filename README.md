# PLANitIO

PLANit IO project which uses XML input and generates XML and CSV output. this reflects the default input and output format of PLANit.

For more information on PLANit such as the user the manual, licensing, installation, getting started, reference documentation, and more, please visit [https://trafficplanit.github.io/PLANitManual/](https://trafficplanit.github.io/PLANitManual/)

## Maven parent

Projects need to be built from Maven before they can be run. The common maven configuration can be found in the PLANitParentPom project which acts as the parent for this project's pom.xml.

> Make sure you install the PLANitParentPom pom.xml before conducting a maven build (in Eclipse) on this project, otherwise it cannot find the references dependencies, plugins, and other resources.

## PLANitXMLGenerator

This project relies on PLANitXMLGenerator to provide the Java classes that are compatible with the underlying XML schemas and inputs that are parsed. Whenever the schema changes re-generate the classes via this separate project. If you do not, a mismatch between the used classes and the underlying xml input occurs causing failure in parsing PLANit inputs.
 
## Testing Documentation ##

The "docs" directory contains the documents:- 

- *Unit Tests and Integration Tests.md*, which describes the test suite which has been written to PLANitIO;
- *Generating JavaDoc for a Java Project in Eclipse.md*, which explains how to create the documentation for the Java code.

## Git Branching model

We adopt GitFlow as per https://nvie.com/posts/a-successful-git-branching-model/
