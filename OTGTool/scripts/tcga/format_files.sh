#!/bin/bash

#Pipe input from jq_md_extract into this to format data
#e.g. cat xyz.json | jq_md_extract.sh | grep miRNA | cut -f2,3 | format_files.sh

while read dir file
do
  files="$files $dir/$file"
done

echo $files


