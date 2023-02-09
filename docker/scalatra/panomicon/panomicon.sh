#!/bin/bash

ROOT=/panomicon
exec java -classpath "$ROOT/lib/*:$ROOT/classes" $JVM_ARGS panomicon.ScalatraLauncher "$*"
