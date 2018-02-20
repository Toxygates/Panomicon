#!/bin/bash

#Script to generate and insert auxiliary RDF data into the triplestore.
#Edit $BASE/triplestore/replace.sh to set the correct URL and credentials.
#This script may be slow, depending on the performance and configuration of your triplestore.

BASE=$(dirname $0)/..
source $BASE/functions.sh

REPO=TestRepo

INPUTS=/shiba/scratch/toxygates/inputs
GENERATED=/shiba/scratch/toxygates/generated

#TODO implement and test
#runfull otg.Manager -output $GENERATED/ssorth.ttl -inputs $INPUTS/hsa2mmu.bb $INPUTS/mmu2rno.bb $INPUTS/rno2hsa.bb

#$BASE/triplestore/replace.sh $GENERATED/ssorth.ttl $REPO http://level-five.jp/t/ssorth.ttl
