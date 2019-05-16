#!/bin/bash

#Script to extract important columns from (JSON format) metadata files from TGAC.

exec jq -r '.[]  | [ .associated_entities[0].case_id, .file_id, .file_name, .data_type ] | @tsv' $*
