#!/bin/bash 
 
INSTANCE=toxygates-pubdata
URL=http://toxygates.nibiohn.go.jp:8080/$INSTANCE/toxygates/ 
 
#Example: getMatrix.sh f 0:10 003017646005 003017646004 
# Obtains two columns, one sample in each. 
# f: fold value (Can be f/a) 
# 0:10: range to get (can be x:y or full) 
 
#Example: getMatrix.sh a full test=003017646005,003017646004 
#To get all absolute value rows in a single averaged column named "test" 

java -classpath lib/scala-library.jar:lib/gwt-user.jar:lib/SyncProxy-0.5.jar:lib/httpclient-4.4-beta1.jar:lib/httpcore-4.4-beta1.jar:classes otg.GetMatrix $URL "$@" 
 
