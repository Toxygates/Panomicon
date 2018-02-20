#!/bin/bash

#Script to generate and insert auxiliary RDF data into the triplestore.
#Edit $BASE/triplestore/replace.sh to set the correct URL and credentials.
#This script may be slow, depending on the performance and configuration of your triplestore.

BASE=$(dirname $0)/..
source $BASE/functions.sh

REPO=TestRepo

INPUTS=/shiba/scratch/toxygates/inputs
GENERATED=/shiba/scratch/toxygates/generated

$BASE/manager/tmanager.sh orthologs -output $GENERATED/ssorth.ttl -intermineURL http://targetmine.mizuguchilab.org/targetmine/service -intermineAppName targetmine

#TODO test
#$BASE/triplestore/replace.sh $GENERATED/ssorth.ttl $REPO http://level-five.jp/t/ssorth.ttl
