#!/bin/bash

IPS=$(cat $* | grep -a matrix | 
  grep -av 121.101.94.167 | 
  egrep -va "^10.100." | 
  grep -av "153.150.74.72" |
  grep -av "202.241.38.13" |
  cut -f1 -d' ' | sort | uniq -c)

echo "$IPS"

echo "$IPS" | cut -c9-

