#!/usr/bin/env bash

#Script to filter ensembl annotations (RDF) to generate a platform in Panomicon - work in progress
#We filter the raw annotation files from ensembl mainly to keep the sizes manageable.

#After these files have been generated, they must be inserted into the triplestore manually (for example using curl,
#or the replace.sh script) and then the TManager tool should be run to add the corresponding platform.

function process_species {
  SPECIES=$1
  OUT=${SPECIES}_filtered.ttl
  shift
  zcat ${SPECIES}_xrefs.ttl.gz | egrep "prefix|refseq|uniprot|ncbigene|identifiers/go" > $OUT
  zcat $SPECIES.ttl.gz | grep SO_transcribed_from >> $OUT
  zcat $SPECIES.ttl.gz | grep SO_translates_to >> $OUT
}

process_species homo_sapiens
process_species mus_musculus
process_species rattus_norvegicus
