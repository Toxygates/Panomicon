#!/bin/bash

#Populates a new Panomicon database with publicly available data.
#newDB.sh should be run first if the database directory does not exist and Kyoto Cabinet
#databases need to be generated.
#The triplestore must also have been set up properly.

#Please set up config.sh before running this script
BASE=$(dirname $0)/..
source $BASE/config.sh

cd $BASE

#Insert basic initial data into the triplestore (definitions of a default instance and dataset)
MIME="application/x-turtle"
curl -u $T_TS_USER:$T_TS_PASS -X POST -H "Content-type:$MIME" "$T_TS_ROOT/$REPO/data" --data-binary @buildData/initData.ttl

#Import data from various sources
./ensemblImport.sh
./mirnaImport.sh
rdf/update_goterms.sh


