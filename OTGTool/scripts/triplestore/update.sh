#!/bin/bash

BASE=$(dirname $0)
source $BASE/../config.sh

#Supply repository name as first argument, e.g. Toxygates

REPO=$1
shift

URLBASE="$T_TS_ROOT/$REPO"

exec curl -u $T_TS_USER:$T_TS_PASS "$URLBASE/update" --data-urlencode update@-
