#!/bin/bash

#Script to generate and insert platforms into the triplestore and necessary Kyoto Cabinet databases.

BASE=$(dirname $0)/..
source $BASE/functions.sh

INPUTS=/shiba/scratch/toxygates/inputs
GENERATED=/shiba/scratch/toxygates/generated

echo Affymetrix
runfull -J-Xmx4g t.platform.affy.Converter $INPUTS/HG-U133_Plus_2.na33.annot.csv \
	$GENERATED/HG-U133_Plus_2.na33_platform.csv
runfull -J-Xmx4g t.platform.affy.Converter $INPUTS/Mouse430_2.na34.annot.csv \
	$GENERATED/Mouse430_2.na34_platform.csv 
runfull -J-Xmx4g t.platform.affy.Converter $INPUTS/Rat230_2.na34.annot.csv \
	$GENERATED/Rat230_2.na34_platform.csv

