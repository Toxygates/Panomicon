#!/bin/bash
source config.sh

#Run a scala REPL with the configuration and classpath set up.
#This is in principle identical to tmanager.sh, except that no main class is supplied at the end.
exec scala -classpath $OTGTOOL_CLASSPATH -J-Xmx4g

