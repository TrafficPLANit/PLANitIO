# PLANitIO

PLANit IO project which uses XML input and generates XML and CSV output. this reflects the default input and output format of PLANit.

For more information on PLANit such as the user the manual, licensing, installation, getting started, reference documentation, and more, please visit [www.goPLANit.org](http://www.goplanit.org)

## Development

### Maven build

PLANit IO has the following PLANit specific dependencies (See pom.xml):

* planit-parentpom
* planit-core
* planit-utils
* planit-xml

Dependencies will be automatically downloaded from the PLANit website, (www.repository.goplanit.org)[http://repository.goplanit.org], or alternatively can be checked-out locally for local development. The shared PLANit Maven configuration can be found in planit-parent-pom which is defined as the parent pom of each PLANit repository.

> When developing on multiple PLANit projects locally, including the parent-pom; make sure you install the PLANitParentPom pom.xml before conducting a Maven build (in for example Eclipse), otherwise it resorts to the online repository rather then the local one.

### Maven deploy

Distribution management is setup via the parent pom such that Maven deploys this project to the PLANit online repository (also specified in the parent pom). To enable deployment ensure that you setup your credentials correctly in your settings.xml as otherwise the deployment will fail.

### PLANitXMLGenerator

This project relies on PLANitXMLGenerator to provide the Java classes that are compatible with the underlying XML schemas and inputs that are parsed. Whenever the schema changes re-generate the classes via this separate project. If you do not, a mismatch between the used classes and the underlying XML input occurs causing failure in parsing PLANit inputs.
 
### Testing Documentation

The "docs" directory contains the documents:- 

- *Unit Tests and Integration Tests.md*, which describes the test suite which has been written to PLANitIO;
- *Generating JavaDoc for a Java Project in Eclipse.md*, which explains how to create the documentation for the Java code.

### Git Branching model

We adopt GitFlow as per https://nvie.com/posts/a-successful-git-branching-model/
