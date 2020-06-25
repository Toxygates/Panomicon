#!/usr/bin/env bash

#Script to filter ensembl annotations (RDF) to generate a platform in Panomicon - work in progress
#We filter the raw annotation files from ensembl mainly to keep the sizes manageable.

#You must configure scripts/triplestore/replace.sh with the correct URL, username and password for the
#triplestore (e.g. Fuseki) for this to work.
#It is also necessary to configure functions.sh (general settings) and manager/tmanager.sh.

#Triplestore repository
REPO=$1
shift

BASEURL=ftp://ftp.ensembl.org/pub/current_rdf
DIR=downloads

function species_shortcode {
  #If new species are added, also add the code to (OTGTool) t/platform/Species.scala
  case "$1" in
    homo_sapiens      ) echo "hsa" ;;
    mus_musculus      ) echo "mmu" ;; 
    rattus_norvegicus ) echo "rno" ;;
    * ) exit 1 ;;
  esac
}

function process_species {
  SPECIES=$1

  FILE=${SPECIES}.ttl.gz
  XREF=${SPECIES}_xrefs.ttl.gz
  pushd $DIR
  curl -O $BASEURL/$SPECIES/$FILE
  curl -O $BASEURL/$SPECIES/$XREF

  OUT=${SPECIES}_filtered.ttl
  zcat $XREF | egrep "prefix|refseq|uniprot|ncbigene|identifiers/go" > $OUT
  zcat $FILE | grep SO_transcribed_from >> $OUT
  zcat $FILE | grep SO_translates_to >> $OUT

  popd

  STITLE=$(species_shortcode $SPECIES).ensembl
  GRAPH=http://level-five.jp/t/platform/$STITLE
  TITLE="\"$SPECIES Ensembl latest $(date)\""
  ./triplestore/replace.sh $DIR/$OUT $REPO $GRAPH $TITLE
  ./manager/tmanager.sh platform addEnsembl -title $STITLE -input $DIR/$OUT
}

mkdir -p $DIR
process_species homo_sapiens
process_species mus_musculus
process_species rattus_norvegicus
