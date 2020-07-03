#!/bin/bash

BASE=$(dirname $0)
source $BASE/config.sh

echo miRDB
pushd downloads
MIRDB=miRDB_v6.0_prediction_result.txt.gz
curl -O http://mirdb.org/download/$MIRDB
popd

OUTDIR=$T_DATA_PATH/mirna
mkdir -p $OUTDIR
zcat < downloads/$MIRDB | egrep "mmu|hsa|rno" > $OUTDIR/mirdb_filter.txt

MIRBASE_VERSION=22
echo MirBase
pushd downloads
curl -O ftp://mirbase.org/pub/mirbase/$MIRBASE_VERSION/miRNA.dat.gz
gunzip miRNA.dat
popd

FULLNAME=mirbase-v$MIRBASE_VERSION

runfull t.platform.mirna.MiRBaseConverter downloads/miRNA.dat tplatform \
        > downloads/${FULLNAME}_platform.tsv

$BASE/manager/tmanager.sh platform add -title "$FULLNAME" -input downloads/${FULLNAME}_platform.tsv
