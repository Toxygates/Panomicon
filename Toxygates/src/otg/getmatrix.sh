#!/bin/bash 
 
INSTANCE=dev
URL=http://toxygates.nibiohn.go.jp:8080/$INSTANCE/toxygates/ 
 
#Example: getMatrix.sh f 0:10 003017646005 003017646004 
# Obtains two columns, one sample in each. 
# f: fold value (Can be f/a) 
# 0:10: range to get (can be x:y or full) 
 
#Example: getMatrix.sh a full test=003017646005,003017646004 
#To get all absolute value rows in a single averaged column named "test" 

SCRIPT_PATH=${0%/*}
CP=${SCRIPT_PATH}/classes
for j in ${SCRIPT_PATH}/lib/*.jar
do
        CP=$CP:$j
done

java -classpath $CP otg.GetMatrix $URL "$@"

