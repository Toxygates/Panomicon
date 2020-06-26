#!/bin/bash

# Graph replacement script for RDF data. Use with caution.
# Usage e.g.: replace.sh otg.trig restore-test http://level-five.jp/t/graph title comment
# Where otg-trig is the input file, and restore-test is the repository to insert into, and the third parameter is the graph to be replaced
# (the repository must exist prior to running this command).
# Annotation comments are automatically inserted.

INPUT=$1
shift
REPO=$1
shift
#e.g. http://level-five.jp/t/annotation/go
GRAPH=$1
shift

BASE=$(dirname $0)
source $BASE/../config.sh

NAMED=no

#Useful reference:
# http://librdf.org/raptor/api/raptor-formats-types-by-parser.html
case $INPUT in
  *owl)
    MIME="application/rdf+xml"
    NAMED=yes
    ;;
  *ttl)
    MIME="application/x-turtle"
    NAMED=yes
    ;;
  *nt)
  	#MIME="application/n-triples"
  	#This is controversial - MIME type of n-triples used to be text/plain
  	#and OWLIM won't accept application/n-triples
  	MIME="text/plain"
  	NAMED=yes
  	;;
  *trig)
    MIME="application/x-trig"
    ;;
  *nq)
    MIME="text/n-quads"
    ;;
  *)
    echo "Unrecognised input file type: $INPUT"
    exit 1
esac

URLBASE="$T_TS_ROOT/$REPO"

QUERY="DROP GRAPH <$GRAPH>"
curl -u $T_TS_USER:$T_TS_PASS "$URLBASE/" --data-urlencode update="$QUERY" 

if [[ "$NAMED" == "yes" ]]
then
  #insert into a named graph
  curl -u $T_TS_USER:$T_TS_PASS -X POST -H "Content-type:$MIME" "$URLBASE/data?graph=$GRAPH" --data-binary @$INPUT
else
  #graph names are already present in the raw data
  curl -u $T_TS_USER:$T_TS_PASS -X POST -H "Content-type:$MIME" "$URLBASE/" --data-binary @$INPUT
fi

if [[ $# -lt 5 ]]
then
  exit 0
fi

#e.g. "GO terms"
TITLE=$1
shift
#e.g. "Updated 2016-01-28 from geneontology.org"
COMMENT=$1

#If the annotation comments are unwanted, uncomment the following and exit.
#exit 0

cat > temp.trig <<EOF
@prefix t:<http://level-five.jp/t/>. 
@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> .

<$GRAPH> { <$GRAPH> a t:annotation; rdfs:label "$TITLE"; t:comment "$COMMENT". }
EOF
curl -u $T_TS_USER:$T_TS_PASS -H "Content-type:application/x-trig" -X POST "$URLBASE/" --data-binary @temp.trig
