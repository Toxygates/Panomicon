#!/bin/bash

SCALA_ROOT=/usr/local/scala-2.10.2

ROOT=/home/johan/workspace/OTGTool
exec $SCALA_ROOT/bin/scala -J-Xmx8g -classpath $ROOT/lib/commons-math3-3.2.jar:$ROOT/bin otg.FoldBuilder $*


