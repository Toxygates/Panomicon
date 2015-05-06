#!/bin/bash

# Script to create a new Toxygates triplestore.

source common.sh

if [ $# -lt 2 ]
then
	echo "Usage: $0 (repository name) (source file directory)"
	exit 1
fi


