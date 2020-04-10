#!/usr/bin/env bash

#Script to filter ensembl annotations (RDF) to generate a platform in Panomicon - work in progress

egrep "prefix|refseq|uniprot|ncbigene|identifiers/go" homo_sapiens_xrefs.ttl > homo_sapiens_xrefs_filtered.ttl
zcat homo_sapiens.ttl.gz | grep SO_transcribed_from >> homo_sapiens_xrefs_filtered.ttl
zcat homo_sapiens.ttl.gz | grep SO_translates_to >> homo_sapiens_xrefs_filtered.ttl


