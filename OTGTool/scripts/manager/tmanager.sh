#!/bin/bash
MAIN_ROOT=/home/johan/ws/l5/Toxygates
OTGTOOL_ROOT=$MAIN_ROOT/OTGTool
TOXY_ROOT=$MAIN_ROOT/Toxygates

#This directory needs to contain libkyotocabinet and libjkyotocabinet
KC_LIBDIR=/usr/local/lib
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$KC_LIBDIR"

CLASSPATH="${OTGTOOL_ROOT}/lib/jar/*:${TOXY_ROOT}/war/WEB-INF/lib/*:${OTGTOOL_ROOT}/mlib/*:${TOXY_ROOT}/mlib/*"

export T_TS_URL=http://localhost:3030/Toxygates/query
export T_TS_UPDATE_URL=http://localhost:3030/Toxygates/update

#Forces read-only mode for triplestore when unset
#unset T_TS_UPDATE_URL

export T_TS_USER=x
export T_TS_PASS=y
#export T_TS_REPO=ttest

#export T_DATA_DIR=kcchunk:/home/johan/data_dev
export T_DATA_DIR=kcchunk:/shiba/scratch/toxygates/rebuild_test
export T_DATA_MATDBCONFIG="#pccap=1073741824#msiz=4294967296"

echo $CLASSPATH
exec /usr/local/bin/scala -Djava.library.path=$KC_LIBDIR -J-Xmx4g -classpath $OTGTOOL_ROOT/bin:$CLASSPATH otg.Manager $@


