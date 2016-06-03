#!/bin/bash

BACKUPDIR=/home/nibioadmin/toxygates_backup

function rdf_backup {
 cd $BACKUPDIR

 REPO=$1
 [ -f $REPO.trig.1.gz ] && rm -f $REPO.trig.1.gz
 [ -f $REPO.trig.gz ] && mv $REPO.trig.gz $REPO.trig.1.gz

 curl -u user:pass -X GET -H "Accept:application/x-trig"   "http://sontaran:8081/owlim-workbench-webapp-5.3.1/repositories/$REPO/rdf-graphs/service?graph=http://www.ontotext.com/explicit" | gzip -f > $REPO.trig.gz
}

#RDF backup
#rdf_backup otg
rdf_backup ttest

#KC backup

function backup {
 SOURCE=$1
 DEST=$2
 [ -d $DEST.1 ] && rm -rf $DEST.1
 [ -d $DEST ] && mv $DEST $DEST.1

 mkdir -p $DEST
 scp nibioadmin@toxygates:$SOURCE/*.kct $DEST
 scp nibioadmin@toxygates:$SOURCE/*.kch $DEST
 gzip -f $DEST/*.kct
 gzip -f $DEST/*.kch
}

#backup /opt/toxygates $BACKUPDIR/data-main
#backup /opt/toxygates-dev $BACKUPDIR/data-dev
backup /opt/toxygates-chunk $BACKUPDIR/data-chunk

