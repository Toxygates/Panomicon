#!/bin/bash

#Populates a new Panomicon database with publicly available data.
#newDB.sh should be run first if the database directory does not exist and Kyoto Cabinet
#databases need to be generated.
#The triplestore must also have been set up properly.

#Please set up config.sh before running this script
BASE=$(dirname $0)/..
source $BASE/config.sh

cd $BASE
./ensemblImport.sh
./mirnaImport.sh
rdf/update_goterms.sh


