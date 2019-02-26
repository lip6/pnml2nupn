#! /bin/sh
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
# Script to launch PNML 2 NuPN (1-Safe P/T Net) model transformation.                       #
# Version: 2019-02-27       (since v1.5.2)                                                  #
# Contributors: Lom M. Hillah                                                               #
# Institutions: Sorbonne Université, and Univ. Paris Nanterre, CNRS LIP6                    #
# Example: ./pnml2nupn.sh pathToModelsFolder [pathToASingleFile] [pathToOtherFolder] [...]  #
#############################################################################################

# Path to executable Jar file
JAR_FILE=pnml2nupn-2.3.0.jar

# Constants
NBPARAM=1
E_NOFILE=66
E_ERROR=-1
E_SUCCESS=0

# Set of advanced arguments for JVM. Increase or decrease memory for the heap if needed by modifying the value of -Xmx
JVM_ARGS="-d64 -server -Xmx7g -Xmn128m -XX:NewSize=2g -XX:MaxNewSize=2g -XX:+UseNUMA -XX:+UseConcMarkSweepGC -XX:+UseParNewGC"

# Should the program keep temporary Cami file? Set to true if you want to keep them. Default is false.
CAMI_TMP_KEEP="-Dcami.tmp.keep=false"

# Should the NuPN generated, even if the net is not 1-safe? Default is false.
FORCE_NUPN_GEN="-Dforce.nupn.generation=true"

# Enable or disable unit safeness checking of the PNML model? Default is false.
# This option would properly work only on *nix systems since it relies on 
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

# Activate debug mode (print stack traces in case of error)? Uncomment the following if you wish so.
export PNML2NUPN_DEBUG=true

if [ $# -lt "$NBPARAM" ] 
	then
	 echo "At least one argument is expected, either a path to a PNML file or directory containing them."
	 echo "Usage: ./`basename $0` pathToModelsFolder [pathToASingleFile] [pathToOtherFolder] [...]"
	 exit "$E_NOFILE" 
fi

echo "Launching PNML2NUPN program"

PNML2NUPN="java $HAS_UNSAFE_ARCS $CAMI_TMP_KEEP $UNIT_SAFENESS_CHECKING $UNIT_SAFENESS_CHECKING_ONLY $UNSAFE_PLACES_NB_REPORT $FORCE_NUPN_GEN $PRESERVE_NUPN_MIX $PRESERVE_NUPN_NATIVE $JVM_ARGS -jar $JAR_FILE"

for file in $1/*.pnml
do
	$PNML2NUPN $file &> ${file%.*}.log
	
done

exit "$E_SUCCESS"
