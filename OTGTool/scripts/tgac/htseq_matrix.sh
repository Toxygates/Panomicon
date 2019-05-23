#!/bin/bash

#Script to construct a htseq matrix file from multiple files (one per sample)

i=0
files="ids.txt"

echo "Gene ID" > ids.txt
#Remove .01 etc endings from ENS ids
zcat $1 | cut -f1 | sed 's/\..*$//' >> ids.txt

for f in $*
do
  i=$((i+1))
  out="data_$i.txt"
  echo ${f%/*} > $out
  zcat $f | cut -f2 >> $out
  files="$files $out"
done

paste -d, $files > htseq_matrix.csv
