#!/bin/bash

INSTANCE=toxygates-pubdata
URL=http://toxygates.nibio.go.jp:8080/$INSTANCE/toxygates/

#Example: sampleSearch.sh organism=Rat compound_name=acetaminophen dose_level=High
#Example: sampleSearch.sh organism=Rat
#Note that all parameter values are case sensitive. Rat is not the same as 'rat'.

java -classpath lib/scala-library.jar:lib/gwt-user.jar:lib/SyncProxyAndroid-0.4.2.jar:classes otg.SampleSearch $URL "$@"

