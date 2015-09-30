#!/bin/bash
# Given a GO term snapshot, this script generates a set of n-triples that 
# set up the equivalence between bio2rdf GO-terms and geneontology.org GO-terms.

grep -o -E "[0-9]{7}" go_daily-termdb.rdf-xml | while read r; do echo "<http://www.geneontology.org/go#GO:$r> owl:sameAs <http://bio2rdf.org/go:$r> ."; done >sameAs.n3
