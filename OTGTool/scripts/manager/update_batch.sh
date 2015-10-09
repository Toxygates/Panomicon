#!/bin/bash

#e.g. RatVitroLiver
BATCH=$1
shift

#e.g. acetaminophen....meta.tsv.gz and others

for MF in $*
do
	BASE=${MF%meta.tsv.gz}
	gzip -d ${BASE}*
	/home/nibioadmin/toxygates/OTGTool/tmanager.sh batch add -append -title $BATCH -metadata ${MF%.gz} -ni ${BASE}med.csv -fold ${BASE}med_fold.csv -calls ${BASE}call.csv -foldCalls ${BASE}call_fold.csv -foldP ${BASE}med_fold_p.csv
	gzip $BASE*
done