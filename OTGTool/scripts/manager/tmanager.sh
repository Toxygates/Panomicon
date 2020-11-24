#!/bin/bash
source $(dirname $0)/../config.sh

#Forces read-only mode for triplestore when unset
#unset T_TS_UPDATE_URL

runfull t.manager.Manager "$@"
