#!/bin/bash
OTGTOOL_ROOT=/home/johan/workspace/OTGTool
#OTGTOOL_ROOT=/Users/johan/Documents/workspace/OTGTool

#Path to scala (version 2.10.x)
SCALA_ROOT=/usr/local/scala-2.10.2
#SCALA_ROOT=/Users/johan/scala-2.10.3

#This directory needs to contain libkyotocabinet and libjkyotocabinet
KC_LIBDIR=/usr/local/lib
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$KC_LIBDIR"

LR=$OTGTOOL_ROOT/lib
CLASSPATH="$LR/jar/*:$LR/bundle/*"

export T_TS_URL=http://sontaran:8081/owlim-workbench-webapp-5.3.1
#export T_TS_URL=http://localhost:3030/data/query
#export T_TS_UPDATE_URL=http://localhost:3030/data/update
export T_TS_UPDATE_URL=
export T_TS_USER=ttest
export T_TS_PASS=ttest
export T_TS_REPO=ttest

export T_DATA_DIR=/home/johan/data_dev
export T_DATA_MATDBCONFIG="#bnum=80000000#pccap=1073741824#apow=1"

exec $SCALA_ROOT/bin/scala -J-Xmx8g -classpath $OTGTOOL_ROOT/bin:$CLASSPATH otg.OTGManager $@


