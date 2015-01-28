#!/bin/bash

mvn -f ./fr.lip6.move.pnml2nupn/pom.xml clean package -Dmaven.javadoc.skip=true -Dmaven.test.skip=true
