#!/bin/bash

INSTANCE=toxygates-pubdata
URL=http://toxygates.nibio.go.jp:8080/$INSTANCE/toxygates/

#Example: getMatrix.sh 0 10 003017646005 003017646004
#To get the first 10 rows in 2 columns
#Example: getMatrix.sh 10 20 test=003017646005,003017646004
#To get 20 rows starting from row 10 (rows 10-29) in a single average column named test

java -classpath lib/scala-library.jar:lib/gwt-user.jar:lib/SyncProxyAndroid-0.4.2.jar:classes otg.GetMatrix $URL "$@"

