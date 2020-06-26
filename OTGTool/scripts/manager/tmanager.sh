#!/bin/bash
source $(dirname $0)/../config.sh

#Forces read-only mode for triplestore when unset
#unset T_TS_UPDATE_URL

#export T_DATA_DIR=kcchunk:/home/johan/data_dev
export T_DATA_DIR=kcchunk:/shiba/scratch/toxygates/rebuild_test
export T_DATA_MATDBCONFIG="#msiz=4294967296"

echo $FULL_CLASSPATH
runfull -Djava.library.path=$KC_LIBDIR -J-Xmx4g otg.Manager "$@"


