PNML to Nested-Unit Petri Net (NUPN) Converter
========

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/8798cf9d0cef45349930953ba85906e9)](https://www.codacy.com/app/lip6/pnml2nupn?utm_source=github.com&utm_medium=referral&utm_content=lip6/pnml2nupn&utm_campaign=badger)
[![Build Status](https://travis-ci.org/lip6/pnml2nupn.svg?branch=master)](https://travis-ci.org/lip6/pnml2nupn "Travis Build Status") &nbsp;
[![Coverity Scan Build Status](https://scan.coverity.com/projects/17780/badge.svg)](https://scan.coverity.com/projects/lip6-pnml2nupn "Coverity Scan Build Status")

PNML2NUPN transforms 1-Safe Place/Transition Petri nets from the [Petri Net Markup Language](http://www.pnml.org) into the [Nested-Units Petri Nets](https://cadp.inria.fr/man/nupn.html) format handled by the Caesar.bdd structural analyser tool of the [CADP tool suite](http://cadp.inria.fr).

Download PNML2NUPN executable jar from its [companion web site](http://pnml.lip6.fr/pnml2nupn/), or the [release page](https://github.com/lip6/pnml2nupn/releases) ofs this repository.

## Usage

 pnml2nupn simply runs on the command line. It is expecting either a set of paths to 
 PNML files, or to folders containing them, or a mix of files and folders. It scans folders recursively, looking for PNML files.
 
**Requirement:**
  Since version 3.0.0, pnml2nupn runs on Java 11+. Prior versions 2.x.x run on Java 7 to Java 8. Starting from 3.0.0, pnml2nupn will not run on Java below 11.
 
 
### Command-line invocation

The basic command-line invocation is the following (update the name of the jar file with the  version you are using, e.g., pnml2nupn-3.0.0.jar):

  ***java -jar pnml2nupn-version.jar pathToPNMLFile [pathToFolder pathToAnotherPNMLFile ...]***

To increase the max allocated memory to the heap, in case you are dealing with very large PNML files, pass the -Xmx argument to the JVM. In the following example, the max memory is set to 2 Go:

  ***java -Xmx2g -jar pnml2nupn-version.jar pathToPNMLFile [pathToFolder pathToAnotherPNMLFile ...]***
  
### Invocation using a Shell script

  There is a [Shell script](fr.lip6.move.pnml2nupn/src/site/resources/download/pnml2nupn.sh) to help you increase productivity in using the PNML to NUPN Converter. Feel free to use it and adapt it to your environment.
  
  Before you use it, you must first set in this script the path to the Java executable (i.e., java or java.exe) in your OS.
  
  The Shell script also contains some properties and a debug environment variable for pnml2nupn, that you can enable or disable.
  See belo
 
### Output since version 3.0.0

 Upon successful execution, this tool will output a single file:
 
  * A <<*.nupn>> file that contains the Nested-Units Petri Net obtained from the PNML P/T translation.
  Mappings between PNML places ids and their NUPN counterparts are included at the end of the NUPN file. 
  The same holds for mappings between PNML transition ids and their NUPN counterparts.
 
### Output before version 3.0.0

 Upon successful execution, this tool will output 3 files:
 
  * A <<*.nupn>> file that contains the Nested Unit Petri Net obtained from the PNML P/T translation;
  
  * A <<*.places>> file that contains the mapping between the places ids from PNML and their counterparts in NUPN;
  
  * A <<*.trans>> file that contains the mapping between the transitions ids from PNML and their counterparts in NUPN.