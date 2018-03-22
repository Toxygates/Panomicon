#!/bin/bash
export MAIN_ROOT=/home/johan/ws/l5/Toxygates
export OTGTOOL_ROOT=$MAIN_ROOT/OTGTool
export TOXY_ROOT=$MAIN_ROOT/Toxygates

#This directory needs to contain libkyotocabinet and libjkyotocabinet
#for any work with KC databases
KC_LIBDIR=/usr/local/lib
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$KC_LIBDIR"

export OTGTOOL_CLASSPATH="${OTGTOOL_ROOT}/lib/jar/*:${OTGTOOL_ROOT}/lib/bundle/*:${OTGTOOL_ROOT}/mlib/*:${OTGTOOL_ROOT}/classes"

export REPO=Toxygates
export T_TS_BASE=http://localhost:3030/$REPO
export T_TS_URL=$T_TS_BASE/query
export T_TS_UPDATE_URL=$T_TS_BASE/update

#Forces read-only mode for triplestore when unset
#unset T_TS_UPDATE_URL

export T_TS_USER=x
export T_TS_PASS=y

function runfull { 
	scala -Djava.library.path=$KC_LIBDIR -J-Xmx4g -classpath $OTGTOOL_CLASSPATH $*
}
