#!/bin/bash

# Backup restore script for RDF data. Use with caution.
# Usage e.g.: restore.sh otg.trig otg-restored
# Where otg-trig is the input file, and otg-restored is the repository to restore to
# (the repository must exist prior to running this command).
# To restore backed up .kct files, manually copy the files once tomcat has been shut down.

INPUT=$1
shift
REPO=$1
shift

curl -u xyzuser:xyzpass -X POST -H "Content-type:application/x-trig" "http://sontaran:8081/owlim-workbench-webapp-5.3.1/repositories/$REPO/statements" --data-binary @$INPUT

