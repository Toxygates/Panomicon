#!/bin/bash

#Please set up config.sh before running this script
BASE=$(dirname $0)/..
source $BASE/config.sh

mkdir -p $T_DATA_PATH
pushd $T_DATA_PATH

#The size parameters here have been found by experiment to be efficient for 
#a database that contains all of Open TG-GATEs and some additional data.
#See https://fallabs.com/kyotocabinet/spex.html for reference.

#Big databases
kchashmgr create -bnum 8000000 expr.kch
kchashmgr create -bnum 8000000 fold.kch

kctreemgr create -bnum 2000000 time_series.kct
kctreemgr create -bnum 2000000 dose_series.kct

#Smaller databases, less sensitive
kctreemgr create -bnum 100000 probe_index.kct
kctreemgr create -bnum 50000 enum_index.kct

popd

