#!/bin/bash
#Configuration for other scripts. This file cannot be run on its own.

#Path to sources and compiled binaries
export MAIN_ROOT=/path/to/Toxygates
export OTGTOOL_ROOT=$MAIN_ROOT/OTGTool
export TOXY_ROOT=$MAIN_ROOT/Toxygates

#Scratch directory for temporary files
export TOXY_SCRATCH=/path/to/Toxygates/tmp

#This directory needs to contain libkyotocabinet and libjkyotocabinet
#for any work with KC databases
KC_LIBDIR=/usr/local/lib
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$KC_LIBDIR"

export OTGTOOL_CLASSPATH="${OTGTOOL_ROOT}/lib/jar/*:${OTGTOOL_ROOT}/lib/bundle/*:${OTGTOOL_ROOT}/mlib/*:${OTGTOOL_ROOT}/classes"

#Directory where databases are stored
export T_DATA_PATH=/path/to/databases
export T_DATA_DIR=kcchunk:$T_DATA_PATH
export T_DATA_MATDBCONFIG="#msiz=4294967296"

#Triplestore access parameters (e.g. Fuseki)
export REPO=Toxygates
export T_TS_ROOT=http://localhost:3030
export T_TS_BASE=$T_TS_ROOT/$REPO
export T_TS_URL=$T_TS_BASE/query
export T_TS_UPDATE_URL=$T_TS_BASE/update

#Forces read-only mode for triplestore when unset
#unset T_TS_UPDATE_URL

export T_TS_USER=x
export T_TS_PASS=y

function runfull { 
	scala -Djava.library.path=$KC_LIBDIR -J-Xmx4g -classpath $OTGTOOL_CLASSPATH "$@"
}
