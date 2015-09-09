#!/bin/bash

INSTANCE=dev
URL=http://toxygates.nibiohn.go.jp:8080/$INSTANCE/toxygates/

#Example: sampleSearch.sh organism=Rat compound_name=acetaminophen dose_level=High
#Example: sampleSearch.sh organism=Rat
#Note that all parameter values are case sensitive. Rat is not the same as 'rat'.

SCRIPT_PATH=${0%/*}
CP=${SCRIPT_PATH}/classes
for j in ${SCRIPT_PATH}/lib/*.jar
do
        CP=$CP:$j
done

java -classpath $CP otg.SampleSearch $URL "$@"
