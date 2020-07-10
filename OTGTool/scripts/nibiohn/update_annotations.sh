#!/bin/bash

#Script for updating annotations on the NIBIOHN Toxygates/Panomicon server.
#Updates GO terms and KEGG pathways.

#Note, /home/nibioadmin on toxygates is separate from the same directory on the cluster,
#as the former is not a cluster machine.
#We assume that this script is run from a cluster machine.

TGHOME=/home/nibioadmin/toxygates
AHOME=$TGHOME/annotation
REPO=Toxygates

ssh toxygates mkdir -p $AHOME
mkdir -p $AHOME

MAINTENANCE_FILE=/opt/toxygates-chunk/MAINTENANCE_MODE

(ssh toxygates test -f $MAINTENANCE_FILE) && {
  echo Maintenance already in progress. Quitting.
  exit 1
}

ssh toxygates touch $MAINTENANCE_FILE

ssh toxygates curl -L -o $AHOME/go.owl  http://current.geneontology.org/ontology/go.owl
ssh toxygates $TGHOME/replace.sh $AHOME/go.owl $REPO http://level-five.jp/t/annotation/go \
        "\"GO terms\"" "\"Updated $(date) from go.owl\"" &

$TGHOME/kegg_rdf/build_kegg.sh && \
	scp $TGHOME/kegg_rdf/rdf/kegg-pathways-genes.f.nt nibioadmin@toxygates:${AHOME} &

wait

ssh toxygates $TGHOME/replace.sh $AHOME/kegg-pathways-genes.f.nt $REPO http://level-five.jp/t/annotation/kegg \
	"\"KEGG pathways\"" "\"Updated $(date) from ftp.bioinformatics.jp\""
		
ssh toxygates rm $MAINTENANCE_FILE

