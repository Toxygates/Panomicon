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

#General treatment IDs
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
     FILTER (?batchGraph NOT IN (<http://level-five.jp/t/batch/rat.1024_1025>, <http://level-five.jp/t/batch/rat.1008_1010>))
  } 
EOF

#Special treatment IDs for adjuvant batches
#Note - this isn't working properly yet

triplestore/update.sh $REPO <<EOF
  PREFIX t:<http://level-five.jp/t/> 
  PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
  INSERT {
    GRAPH ?batchGraph {
    ?x t:treatment ?treatment; t:control_treatment ?control_treatment.
    }
  } WHERE {
      GRAPH ?batchGraph {
        ?x a t:sample; t:compound_name ?compound_name; t:dose_level ?dose_level; t:exposure_time ?exposure_time;
        t:organ_id ?organ_id; t:vehicle ?vehicle; t:vehicle_dose ?vehicle_dose; t:exp_id ?exp_id.
     } ?batchGraph rdfs:label ?batchLabel.
  
     BIND(CONCAT(STR(?compound_name), "|", STR(?organ_id), "|", STR(?exposure_time), "|", STR(?dose_level),
       "|", STR(?vehicle), "|", STR(?vehicle_dose), "|", STR(?exp_id)) AS ?treatment)
     BIND(CONCAT(STR(?compound_name), "|", STR(?organ_id), "|", STR(?exposure_time), "|", "Control",
       "|", STR(?vehicle), "|", STR(?vehicle_dose), "|", STR(?exp_id)) AS ?control_treatment)

     FILTER (?batchGraph IN (<http://level-five.jp/t/batch/rat.1024_1025>, <http://level-five.jp/t/batch/rat.1008_1010>))
  } 
EOF
