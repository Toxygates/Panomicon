#!/bin/bash

# Graph replacementscript for RDF data. Use with caution.
# Usage e.g.: replace.sh otg.trig restore-test http://level-five.jp/t/graph
# Where otg-trig is the input file, and restore-test is the repository to restore to, and the final parameter is the graph to be replaced
# (the repository must exist prior to running this command).

INPUT=$1
shift
REPO=$1
shift
GRAPH=$1
shift

QUERY="DROP GRAPH <$GRAPH>"
curl -u xyzuser:xyzpass "http://sontaran:8081/owlim-workbench-webapp-5.3.1/repositories/$REPO/statements" --data-urlencode update="$QUERY" 
curl -u xyzuser:xyzpass -X POST -H "Content-type:application/x-trig" "http://sontaran:8081/owlim-workbench-webapp-5.3.1/repositories/$REPO/statements" --data-binary @$INPUT

