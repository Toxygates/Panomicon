#!/bin/bash

TGHOME=/home/nibioadmin/toxygates
AHOME=$TGHOME/annotation
REPO=ttest
mkdir -p $AHOME
cd $AHOME

curl -O http://purl.obolibrary.org/obo/go.owl
$TGHOME/replace.sh $AHOME/go.owl $REPO http://level-five.jp/t/annotation/go \
	"GO terms" "Updated $(date) from go.owl"

#TODO fetch kegg-pathways-genes.nt
$TGHOME/replace.sh $AHOME/kegg-pathways-genes.nt $REPO http://level-five.jp/t/annotation/kegg \
	"KEGG pathways" "Updated $(date) from ftp.bioinformatics.jp"
	
	
