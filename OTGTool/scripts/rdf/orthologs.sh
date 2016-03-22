#!/bin/bash

#This script obtains eggNOG orthologs for rat (9606), mouse (10090) and human (10116) from the UniProt SPARQL endpoint.

TEMPFILE=orthologs.tmp
OUTPUT=orthologs.ttl

if [ -f $TEMPFILE ] || [ -f $OUTPUT ] 
then
	echo "At least one of $OUTPUT and $TEMPFILE exists. Remove and try again."
	exit 1
fi

read -r -d '' QUERY <<'EOF'

PREFIX up:<http://purl.uniprot.org/core/> 
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> 
PREFIX dc:<http://purl.org/dc/terms/> 
PREFIX taxo:<http://purl.uniprot.org/taxonomy/>

select distinct ?p ?ogroup ?org {
  ?p up:organism ?org;
    rdfs:seeAlso ?ogroup.
  ?ogroup up:database <http://purl.uniprot.org/database/eggNOG> .
  FILTER ( ?org IN(taxo:9606, taxo:10116, taxo:10090) )
} 

EOF

curl -g http://sparql.uniprot.org/sparql \
--data-urlencode query="$QUERY" \
--data-urlencode format="csv" | tail -n+2 | sed "s///" > $TEMPFILE

cat $TEMPFILE | sed -E "s/(.*),(.*),(.*)/<\1> up:organism <\3> ; rdfs:seeAlso <\2> ./" >> $OUTPUT
cat $TEMPFILE | sed -E "s/(.*),(.*),(.*)/<\2> up:database <http:\/\/purl.uniprot.org\/database\/eggNOG> ./" >> $OUTPUT
