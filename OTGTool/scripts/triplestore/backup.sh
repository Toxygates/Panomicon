#!/bin/bash

BACKUPDIR=/home/nibioadmin/toxygates_backup

function rdf_backup {
 REPO=$1
 curl -u xyzuser:xyzpass -X GET -H "Accept:application/x-trig"   "http://sontaran:8081/owlim-workbench-webapp-5.3.1/repositories/$REPO/rdf-graphs/service?graph=http://www.ontotext.com/explicit" | gzip -f > $BACKUPDIR/$REPO.trig.gz
}

#RDF backup
rdf_backup otg
rdf_backup ttest

#KC backup
#mkdir -p $BACKUPDIR/data-main
#mkdir -p $BACKUPDIR/data-adjuvant
mkdir -p $BACKUPDIR/data-dev

function backup {
 SOURCE=$1
 DEST=$2
 scp nibioadmin@toxygates:$SOURCE/*.kct $DEST
 gzip -f $DEST/*.kct
}

#backup /opt/toxygates $BACKUPDIR/data-main
backup /opt/toxygates-dev $BACKUPDIR/data-dev

