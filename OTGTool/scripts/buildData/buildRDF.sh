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

ORTH_GRAPH=http://level-five.jp/t/ssorth.ttl
$BASE/triplestore/replace.sh $GENERATED/ssorth.ttl $REPO $ORTH_GRAPH

cat > temp.trig <<EOF
@prefix t:<http://level-five.jp/t/>. 

<$ORTH_GRAPH> a t:ortholog_mapping .
EOF
curl -u $T_TS_USER:$T_TS_PASS -H "Content-type:application/x-trig" -X POST $T_TS_BASE/ --data-binary @temp.trig
