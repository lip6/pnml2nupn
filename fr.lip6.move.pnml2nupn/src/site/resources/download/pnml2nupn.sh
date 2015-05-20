#! /bin/sh
#
#  Copyright 2014-2015 Université Paris Ouest and Sorbonne Universités,
# 							Univ. Paris 06 - CNRS UMR
# 							7606 (LIP6)
#
#  All rights reserved.   This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
#  Project leader / Initial Contributor:
#    Lom Messan Hillah - <lom-messan.hillah@lip6.fr>
#
#  Contributors:
#    ${ocontributors} - <$oemails}>
#
#  Mailing list:
#    lom-messan.hillah@lip6.fr
#


#################################################################################################
# Script to launch PNML 2 NuPN (1-Safe P/T Net) model transformation.                           #
# Version: 2015-05-20       (since v1.4.0)                                                      #
# Contributors: Lom M. Hillah                                                                   #
# Institutions: Sorbonne Universités, Univ. Paris 06 and Univ. Paris Ouest, CNRS UMR 7606 (LIP6)#
# Example: ./pnml2nupn.sh pathToModelsFolder [pathToASingleFile] [pathToOtherFolder] [...]      #
#################################################################################################

# Path to executable Jar file
JAR_FILE=pnml2nupn-1.4.0.jar

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
FORCE_NUPN_GEN="-Dforce.nupn.generation=false"

# Enable or disable unit safeness checking of the PNML model? Default is false.
UNIT_SAFENESS_CHECKING="-Dunit.safeness.checking=false"

# Enable or disable unit safeness checking only? Default is false.
UNIT_SAFENESS_CHECKING_ONLY="-Dunit.safeness.checking.only=false"

# Check if the net has unsafe arcs. Default is false. 
# This option is exlusive of all the others.When it is set, it deactivates the others. 
HAS_UNSAFE_ARCS="-Dhas.unsafe.arcs=false"

# Activate debug mode (print stack traces in case of error)? Uncomment the following if you wish so.
export PNML2NUPN_DEBUG=true

if [ $# -lt "$NBPARAM" ] 
	then
	 echo "At least one argument is expected, either a path to a PNML file or directory containing them."
	 echo "Usage: ./`basename $0` pathToModelsFolder [pathToASingleFile] [pathToOtherFolder] [...]"
	 exit "$E_NOFILE" 
fi

echo "Launching PNML2NUPN program"

TRADUCTEUR_PNML2NUPN="java $HAS_UNSAFE_ARCS $CAMI_TMP_KEEP $UNIT_SAFENESS_CHECKING $UNIT_SAFENESS_CHECKING_ONLY $FORCE_NUPN_GEN $JVM_ARGS -jar $JAR_FILE"

for file in $1/*.pnml
do
	$TRADUCTEUR_PNML2NUPN $file &> ${file%.*}.log
	
done

exit "$E_SUCCESS"
