#!/bin/bash

#Script to construct a mirna data file from multiple files (one per sample)

i=0
files="ids.txt"

cat $1 | cut -f1 > ids.txt

for f in $*
do
  i=$((i+1))
  out="data_$i.txt"
  #column header
  echo ${f%/*} > $out
  cat $f | tail -n +2 | cut -f3 >> $out
  files="$files $out"
done

paste -d, $files > mirna_matrix.csv
