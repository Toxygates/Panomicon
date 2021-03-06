#!/bin/bash

BASE=$(dirname $0)/..
source $BASE/config.sh

INPUTS=$TOXY_SCRATCH/inputs
GENERATED=$TOXY_SCRATCH/generated

mkdir -p $INPUTS
mkdir -p $GENERATED

echo SSorth
$BASE/manager/tmanager.sh orthologs -output $GENERATED/ssorth.ttl -intermineURL https://targetmine.mizuguchilab.org/targetmine/service -intermineAppName targetmine
ORTH_GRAPH=http://level-five.jp/t/ssorth.ttl
$BASE/triplestore/replace.sh $GENERATED/ssorth.ttl $REPO $ORTH_GRAPH

cat > $GENERATED/orth_temp.trig <<EOF
@prefix t:<http://level-five.jp/t/>. 

<$ORTH_GRAPH> a t:ortholog_mapping .
EOF

curl -u $T_TS_USER:$T_TS_PASS -H "Content-type:application/x-trig" -X POST $T_TS_BASE/ --data-binary @$GENERATED/orth_temp.trig
