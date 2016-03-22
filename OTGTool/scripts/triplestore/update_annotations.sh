#!/bin/bash

TGHOME=/home/nibioadmin/toxygates
AHOME=$TGHOME/annotation
REPO=ttest
mkdir -p $AHOME
cd $AHOME

curl -O http://purl.obolibrary.org/obo/go.owl
$TGHOME/replace.sh $AHOME/go.owl $REPO http://level-five.jp/t/annotation/go
