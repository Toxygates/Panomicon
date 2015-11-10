#!/bin/bash
OTGTOOL_ROOT=/home/johan/workspace/Toxygates/OTGTool

SCALA_ROOT=/usr/local

#This directory needs to contain libkyotocabinet and libjkyotocabinet
KC_LIBDIR=/usr/local/lib
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$KC_LIBDIR"

LR=$OTGTOOL_ROOT/lib
CLASSPATH="$LR/jar/*:$LR/bundle/*"

export T_TS_URL=http://sontaran:8081/owlim-workbench-webapp-5.3.1
#export T_TS_URL=http://localhost:3030/data/query
#export T_TS_UPDATE_URL=http://localhost:3030/data/update

#Forces read-only mode for triplestore when not set
unset T_TS_UPDATE_URL

export T_TS_USER=ttest
export T_TS_PASS=ttest
export T_TS_REPO=ttest

#export T_DATA_DIR=/home/johan/data_dev
export T_DATA_DIR=/home/johan/data_rebuild
export T_DATA_MATDBCONFIG="#bnum=6250000#pccap=1073741824#msiz=4294967296"

exec $SCALA_ROOT/bin/scala -Djava.library.path=$KC_LIBDIR -J-Xmx4g -classpath $OTGTOOL_ROOT/bin:$CLASSPATH otg.Manager $@


