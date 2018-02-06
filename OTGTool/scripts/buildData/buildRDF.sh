#!/bin/bash

#Script to generate and insert auxiliary RDF data into the triplestore.
#Edit $BASE/triplestore/replace.sh to set the correct URL and credentials.
#This script may be slow, depending on the performance and configuration of your triplestore.

BASE=$(dirname $0)/..
source $BASE/functions.sh

REPO=TestRepo

INPUTS=/shiba/scratch/toxygates/inputs
GENERATED=/shiba/scratch/toxygates/generated

#Download from http://www.mirdb.org/download.html
echo mirDB
runfull t.platform.mirna.MiRDBConverter $INPUTS/miRDB_5.0_filter.txt \
	$GENERATED/mirdb.trig

$BASE/triplestore/replace.sh $GENERATED/mirdb.trig $REPO http://level-five.jp/t/mapping/mirdb	