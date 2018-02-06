#!/bin/bash
export MAIN_ROOT=/home/johan/ws/l5/Toxygates
export OTGTOOL_ROOT=$MAIN_ROOT/OTGTool
export TOXY_ROOT=$MAIN_ROOT/Toxygates

#This directory needs to contain libkyotocabinet and libjkyotocabinet
#for any work with KC databases
KC_LIBDIR=/usr/local/lib
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$KC_LIBDIR"

export OTGTOOL_CLASSPATH="${OTGTOOL_ROOT}/lib/jar/*:${OTGTOOL_ROOT}/mlib/*:${OTGTOOL_ROOT}/classes"
export FULL_CLASSPATH="${OTGTOOL_CLASSPATH}:${TOXY_ROOT}/war/WEB-INF/lib/*:${TOXY_ROOT}/mlib/*"

function runfull {
	scala -classpath $FULL_CLASSPATH $*
}