#!/bin/bash
source $(dirname $0)/../functions.sh


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

echo $FULL_CLASSPATH
runfull -Djava.library.path=$KC_LIBDIR -J-Xmx4g otg.Manager $@


