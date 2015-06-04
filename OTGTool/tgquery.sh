#!/bin/bash
OTGTOOL_ROOT=/home/johan/workspace/OTGTool

#Path to scala (version 2.10.x)
SCALA_ROOT=/usr/local/scala-2.10.2

#This directory needs to contain libkyotocabinet and libjkyotocabinet
KC_LIBDIR=/usr/local/lib
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$KC_LIBDIR"

#Adjust max memory if necessary
JAVA_OPTS="-J-Xmx4g -J-Djava.library.path=$KC_LIBDIR -Dotg.home=/ext/toxygates"

LR=$OTGTOOL_ROOT/lib
CLASSPATH="$LR/commons-codec-1.4.jar:$LR/commons-httpclient-3.1.jar:$LR/commons-logging-1.1.1.jar:$LR/kyotocabinet.jar:$LR/log4j.jar:$LR/lubm.jar:$LR/openrdf-sesame-2.6.10-onejar.jar:$LR/slf4j-api-1.5.0.jar:$LR/slf4j-jdk14-1.5.0.jar"

exec $SCALA_ROOT/bin/scala $JAVA_OPTS -classpath $OTGTOOL_ROOT/bin:$CLASSPATH otg.OTGQuery "$@"

