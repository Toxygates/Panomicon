#!/bin/bash

#e.g. RatVitroLiver
BATCH=$1
shift

#e.g. acetaminophen....meta.tsv and others
#For each meta.tsv, a data.tsv file is also expected, and a call.csv file may 
#optionally be present
BASE=${MF%meta.tsv}
/home/nibioadmin/toxygates/OTGTool/tmanager.sh batch add -append -title $BATCH -multiMetadata "$@"

