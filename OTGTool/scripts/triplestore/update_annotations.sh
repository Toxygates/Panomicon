#!/bin/bash

TGHOME=/home/nibioadmin/toxygates
AHOME=$TGHOME/annotation
REPO=ttest
mkdir -p $AHOME
cd $AHOME

MAINTENANCE_FILE=/opt/toxygates-chunk/MAINTENANCE_MODE

(ssh toxygates test -f $MAINTENANCE_FILE) && {
  echo Maintenance already in progress. Quitting.
  exit 1
}

ssh toxygates touch $MAINTENANCE_FILE

curl -O http://geneontology.org/ontology/go.owl
$TGHOME/replace.sh $AHOME/go.owl $REPO http://level-five.jp/t/annotation/go \
	"GO terms" "Updated $(date) from go.owl"

$TGHOME/kegg_rdf/build_kegg.sh
cp $TGHOME/kegg_rdf/rdf/kegg-pathways-genes.f.nt $AHOME

$TGHOME/replace.sh $AHOME/kegg-pathways-genes.f.nt $REPO http://level-five.jp/t/annotation/kegg \
	"KEGG pathways" "Updated $(date) from ftp.bioinformatics.jp"
		
ssh toxygates rm $MAINTENANCE_FILE
	