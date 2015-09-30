#!/bin/bash

# Script to restore a Toxygates triplestore backup.

source common.sh

if [ $# -lt 2 ]
then
	echo "Usage: $0 (repository name) (backup directory)"
	exit 1
fi


