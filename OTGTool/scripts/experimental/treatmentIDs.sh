#!/bin/bash

#Supply repository ID as first argument, e.g. Toxygates

#Delete all treatment/control treatment values
triplestore/update.sh $REPO <<EOF
  PREFIX t:<http://level-five.jp/t/> 
  DELETE {
    GRAPH ?g {
      ?x t:treatment ?treatment; t:control_treatment ?control_treatment.
    }
  } WHERE {
    GRAPH ?g {
      ?x a t:sample.
      ?x t:treatment ?treatment; t:control_treatment ?control_treatment.
    }
  }
EOF

# Generate general treatment IDs
# Currently, the adjuvant batches (<http://level-five.jp/t/batch/adjuvant.mouse.20180531>,
# <http://level-five.jp/t/batch/adjuvant.rat.20180613>) are the only known ones where this formula doesn't apply.
# For those batches, the treatments are set manually in the metadata.
triplestore/update.sh $REPO <<EOF
  PREFIX t:<http://level-five.jp/t/> 
  PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
  INSERT {
    GRAPH ?batchGraph {
    ?x t:treatment ?treatment; t:control_treatment ?control_treatment.
    }
  } WHERE {
      GRAPH ?batchGraph {
        ?x a t:sample; t:compound_name ?compound_name; t:dose_level ?dose_level; t:exposure_time ?exposure_time .
     } ?batchGraph rdfs:label ?batchLabel.
  
     BIND(CONCAT(STR(?compound_name), "|", STR(?exposure_time), "|", STR(?dose_level)) AS ?treatment)
     BIND(CONCAT(STR(?compound_name), "|", STR(?exposure_time), "|", "Control") AS ?control_treatment)
     FILTER (?batchGraph NOT IN (<http://level-five.jp/t/batch/adjuvant.mouse.20180531>, <http://level-five.jp/t/batch/adjuvant.rat.20180613>))
  } 
EOF

