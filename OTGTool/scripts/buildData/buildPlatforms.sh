#!/bin/bash

#Script to generate and insert platforms into the triplestore and necessary Kyoto Cabinet databases.
#Adjust repositories and data directories in $BASE/manager/tmanager.sh prior to running.
#This script may take a long time to run.

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

$BASE/manager/tmanager.sh platform add -title "HG-U133_Plus_2" -input $GENERATED/HG-U133_Plus_2.na33_platform.csv
$BASE/manager/tmanager.sh platform add -title "Mouse430_2" -input $GENERATED/Mouse430_2.na34_platform.csv
$BASE/manager/tmanager.sh platform add -title "Rat230_2" -input $GENERATED/Rat230_2.na34_platform.csv 


#Download miRNA.dat from http://www.mirbase.org/ftp.shtml and rename
echo Mirbase
runfull t.platform.mirna.MiRBaseConverter $INPUTS/mirbase-v21.dat tplatform \
	> $GENERATED/mirbase-v21_platform.tsv

$BASE/manager/tmanager.sh platform add -title "mirbase-v21" -input $GENERATED/mirbase-v21_platform.tsv
	