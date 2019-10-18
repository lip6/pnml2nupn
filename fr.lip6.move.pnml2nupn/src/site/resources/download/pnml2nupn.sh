#! /bin/bash
#
#  Copyright 2014-2019 Université Paris Nanterre and Sorbonne Universités,
# 							CNRS, LIP6
#
#  All rights reserved.   This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
#  Project leader / Principal Contributor:
#    Lom Messan Hillah - <lom-messan.hillah@lip6.fr>


#############################################################################################
# Script to launch PNML 2 NUPN (1-Safe P/T Net) model transformation.                       #
# Version: 2019-10-18       (since v1.5.2)                                                  #
# Contributors: Lom M. Hillah                                                               #
# Institutions: Sorbonne Université, and Univ. Paris Nanterre, LIP6, CNRS                   #
# Example: ./pnml2nupn.sh pathToModelsFolder [pathToASingleFile] [pathToOtherFolder] [...]  #
#############################################################################################

# Path to executable Jar file
JAR_FILE=pnml2nupn-3.1.0.jar

# Constants
NBPARAM=1
E_NOFILE=66
E_SUCCESS=0

# Setting d64 as arg to JVM depends on usinsg SunOS.
case $(uname -s) in
	SunOS ) D64="-d64 " ;;
	* ) D64="" ;; 
esac

# First, you must set below the path to the java executable in your OS.
# Starting from version 3.0.0, pnml2nupn runs with Java 11+. 
# pnml2nupn versions 3.x.x will not run with Java versions before 11.
JAVA="java"

# Set of advanced arguments for JVM. By default it is empty. See below if you want to use a predefined set.
JVM_ARGS="-Xmx2g"

# Uncomment the JVM_ARGS line below if you want to use the proposed, predefined set of advanced arguments for the JVM.
# In particular, you can increase or decrease max memory to allocate for the heap, if needed, by modifying the value of -Xmx
# JVM_ARGS="$D64 -server -Xmx2g -Xmn128m -XX:NewSize=2g -XX:MaxNewSize=2g -XX:+UseNUMA"

# Should the program keep temporary Cami file? Set to true if you want to keep them. Default is false.
CAMI_TMP_KEEP="-Dcami.tmp.keep=false"

# Should the NuPN generated, even if the net is not 1-safe? Default is false.
FORCE_NUPN_GEN="-Dforce.nupn.generation=true"

# Enable or disable unit safeness checking of the PNML model? Default is false.
# This option would work only on *nix systems since it relies on 
# the Bounds tool (embedded but deployed locally at runtime)
UNIT_SAFENESS_CHECKING="-Dunit.safeness.checking=false"

# Enable or disable unit safeness checking only? Default is false.
# This option also activates UNIT_SAFENESS_CHECKING
UNIT_SAFENESS_CHECKING_ONLY="-Dunit.safeness.checking.only=false"

# How many places (at most) to report when the net is unsafe? 
# (e.g. 0, 1, 2, 4, or -1 to specify 'all')
UNSAFE_PLACES_NB_REPORT="-Dunsafe.places.nb.report=10"

# Check if the net has unsafe arcs. Default is false. 
# This option is exlusive of all the others. When it is set, it deactivates the others. 
HAS_UNSAFE_ARCS="-Dhas.unsafe.arcs=false"

# Use NUPN tool info section (if present) to complement the naive generation strategy of NUPN.
PRESERVE_NUPN_MIX="-Dpreserve.nupn.mix=false"

# Use NUPN tool info section (if present) right from the beginning to generate the NUPN.
# This option preempts preserve.nupn.mix. Therefore, it deactivates preserve.nupn.mix when it is set.
PRESERVE_NUPN_NATIVE="-Dpreserve.nupn.native=false"

# Use PNML place names instead of their ids for the mappings PNML id/ NUPN id 
# in the labels section of the NUPN. 
# This option is ignored when a NUPN tool specific section is found in the PNML.
USE_PLACE_NAMES="-Duse.place.names=false"

# Use PNML transition names instead of their ids for the mappings PNML id/ NUPN id 
# in the labels section of the NUPN.
# This option is ignored when a NUPN tool specific section is found in the PNML.
USE_TRANSITION_NAMES="-Duse.transition.names=false"

# Start numbering of places in NUPN from the specified number
FIRST_PLACE_NUMBER="-Dfirst.place.number=0"

# Start numbering of transitions in NUPN from the specified number
FIRST_TRANSITION_NUMBER="-Dfirst.transition.number=0"

# Group the options to pass over to the pnml2nupn translator.
TRANSLATOR_OPTS="$CAMI_TMP_KEEP
$HAS_UNSAFE_ARCS $UNIT_SAFENESS_CHECKING $UNIT_SAFENESS_CHECKING_ONLY $UNSAFE_PLACES_NB_REPORT
$FORCE_NUPN_GEN
$PRESERVE_NUPN_MIX $PRESERVE_NUPN_NATIVE
$USE_PLACE_NAMES $USE_TRANSITION_NAMES
$FIRST_PLACE_NUMBER $FIRST_TRANSITION_NUMBER"

# Activate debug mode (print stack traces in case of error)? Uncomment the following if you wish so.
export PNML2NUPN_DEBUG=true

if [ $# -lt "$NBPARAM" ] 
	then
	 echo "At least one argument is expected, either a path to a PNML file or directory containing them."
	 echo "Usage: ./$(basename $0) pathToModelsFolder [pathToASingleFile] [pathToOtherFolder] [...]"
	 exit "$E_NOFILE" 
fi

echo "Launching PNML2NUPN program"

PNML2NUPN="$JAVA $TRANSLATOR_OPTS $JVM_ARGS -jar $JAR_FILE"

list=""
for file in "$@" 
do
	if [ -d "$file" ]
	then
		list="$list $(ls $file/*.pnml)"
	else
		file=$(dirname $file)/$(basename $file .pnml).pnml
		list="$list $file" 
	fi
done

for file in $list
do
	$PNML2NUPN $file &> ${file%.*}.log 
done

exit "$E_SUCCESS"
