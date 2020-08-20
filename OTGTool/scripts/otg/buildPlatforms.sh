#!/bin/bash

#Script to generate and insert platforms into the triplestore and necessary Kyoto Cabinet databases.
#Adjust repositories and data directories in $BASE/manager/tmanager.sh prior to running.
#This script may take a long time to run.

BASE=$(dirname $0)/..
source $BASE/config.sh

INPUTS=$TOXY_SCRATCH/inputs
GENERATED=$TOXY_SCRATCH/generated

mkdir -p $INPUTS
mkdir -p $GENERATED

echo Affymetrix
runfull -J-Xmx4g t.platform.affy.Converter $INPUTS/HG-U133_Plus_2.na33.annot.csv \
	$GENERATED/HG-U133_Plus_2.na33_platform.csv
runfull -J-Xmx4g t.platform.affy.Converter $INPUTS/Mouse430_2.na34.annot.csv \
	$GENERATED/Mouse430_2.na34_platform.csv 
runfull -J-Xmx4g t.platform.affy.Converter $INPUTS/Rat230_2.na34.annot.csv \
	$GENERATED/Rat230_2.na34_platform.csv

$BASE/manager/tmanager.sh platform add -title "HG-U133_Plus_2" -input $GENERATED/HG-U133_Plus_2.na33_platform.csv
$BASE/manager/tmanager.sh platform add -title "Mouse430_2" -input $GENERATED/Mouse430_2.na34_platform.csv
$BASE/manager/tmanager.sh platform add -title "Rat230_2" -input $GENERATED/Rat230_2.na34_platform.csv 

