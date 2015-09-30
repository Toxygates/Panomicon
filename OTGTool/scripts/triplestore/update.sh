#!/bin/bash

# Script to perform periodic update of a Toxygates triplestore.

source common.sh

if [ $# -lt 1 ]
then
	echo "Usage: $0 (repository name)"
	exit 1
fi


