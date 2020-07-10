#!/usr/bin/env bash

#Script to filter ensembl annotations (RDF) to generate a platform in Panomicon.
#We filter the raw annotation files from ensembl mainly to keep the sizes manageable.

#You must set up config.sh correctly for this to work.
BASE=$(dirname $0)
source $BASE/config.sh

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

  #This way of invoking zcat works on both linux and Mac OS
  zcat < $XREF | egrep "prefix|refseq|uniprot|ncbigene|identifiers/go" > $OUT
  zcat < $FILE | grep SO_transcribed_from >> $OUT
  zcat < $FILE | grep SO_translates_to >> $OUT

  popd

  STITLE=$(species_shortcode $SPECIES).ensembl
  GRAPH=http://level-five.jp/t/platform/$STITLE
  COMMENT="$SPECIES Ensembl latest $(date)"
  ./triplestore/replace.sh $DIR/$OUT $REPO $GRAPH "$COMMENT"
  ./manager/tmanager.sh platform addEnsembl -title "$STITLE" -input $DIR/$OUT
}

mkdir -p $DIR
process_species homo_sapiens
process_species mus_musculus
process_species rattus_norvegicus
