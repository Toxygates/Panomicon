#!/bin/bash

INSTANCE=dev
URL=http://toxygates.nibiohn.go.jp:8080/$INSTANCE/toxygates/

#Example: sampleSearch.sh organism=Rat compound_name=acetaminophen dose_level=High
#Example: sampleSearch.sh organism=Rat
#Note that all parameter values are case sensitive. Rat is not the same as 'rat'.

CP=classes
for j in lib/*.jar
do
        CP=$CP:$j
done

java -classpath $CP otg.SampleSearch $URL "$@"
