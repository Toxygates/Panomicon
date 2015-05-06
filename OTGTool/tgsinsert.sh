#!/bin/bash

#Script to insert (add or update) series into a series database.
#Example usage:
#tginsert.sh -fdb otgf.kct -db otgfs.kct -metadata attribute.tsv -repository otg-test
#See OTGTool/src/main/scala/otg/Series.scala

OTGTOOL_ROOT=/home/johan/workspace/OTGTool

#Path to scala (version 2.10.x)
SCALA_ROOT=/usr/local/scala-2.10.2

#This directory needs to contain libkyotocabinet and libjkyotocabinet
KC_LIBDIR=/usr/local/lib
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$KC_LIBDIR

#Directory for read-only access to data files such as compounds.txt and rat.probes.txt
OTGHOME=-J-Dotg.home=/shiba/toxygates

#Adjust max memory if necessary (give it as much as the machine can spare)
JAVA_OPTS="-J-Xmx4g -J-Djava.library.path=$KC_LIBDIR $OTGHOME"

LR=$OTGTOOL_ROOT/lib
CLASSPATH="$LR/commons-codec-1.4.jar:$LR/commons-httpclient-3.1.jar:$LR/commons-logging-1.1.1.jar:$LR/kyotocabinet.jar:$LR/log4j.jar:$LR/lubm.jar:$LR/openrdf-sesame-2.6.10-onejar.jar:$LR/slf4j-api-1.5.0.jar:$LR/slf4j-jdk14-1.5.0.jar"

exec $SCALA_ROOT/bin/scala $JAVA_OPTS -classpath $OTGTOOL_ROOT/bin:$CLASSPATH otg.OTGSeriesInsert "$@"
