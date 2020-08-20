#!/bin/bash

BASE=$(dirname $0)/..
source $BASE/config.sh

#Script to download and insert Gene Ontology (GO) terms.

DIR=$BASE/downloads
mkdir -p $DIR

pushd $DIR
curl -L -O http://current.geneontology.org/ontology/go.owl
popd

cd $BASE
./triplestore/replace.sh $DIR/go.owl $REPO http://level-five.jp/t/annotation/go \
  "\"GO terms\"" "\"Updated $(date) from go.owl\"" 

