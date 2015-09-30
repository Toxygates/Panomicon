#!/bin/bash

#Script to query a series database.
#See OTGTool/src/main/scala/Series.scala
#Example usage: tgsquery.sh -species Rat -repeat Single -organ Liver -probe 1379190_at 
#This tool can also perform compound ranking. See the source code for details.

OTGTOOL_ROOT=/scratch/johan/Toxygates/OTGTool

#Path to scala (version 2.10.x)
SCALA_ROOT=/scratch/johan/scala-2.10.2

#This directory needs to contain libkyotocabinet and libjkyotocabinet
KC_LIBDIR=/home/johan/lib
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$KC_LIBDIR

#Directory for read-only access to data files such as compounds.txt and rat.probes.txt
OTGHOME=-J-Dotg.home=/work/otg-test

#Adjust max memory if necessary (give it as much as the machine can spare)
JAVA_OPTS="-J-Xmx20g -J-Djava.library.path=$KC_LIBDIR $OTGHOME"

LR=$OTGTOOL_ROOT/lib
CLASSPATH="$LR/commons-codec-1.4.jar:$LR/commons-httpclient-3.1.jar:$LR/commons-logging-1.1.1.jar:$LR/kyotocabinet.jar:$LR/log4j.jar:$LR/lubm.jar:$LR/openrdf-sesame-2.6.10-onejar.jar:$LR/slf4j-api-1.5.0.jar:$LR/slf4j-jdk14-1.5.0.jar"

exec $SCALA_ROOT/bin/scala $JAVA_OPTS -classpath $OTGTOOL_ROOT/bin:$CLASSPATH otg.OTGSeriesQuery "$@"
