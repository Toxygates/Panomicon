#!/bin/bash

THOME=/opt/apache-tomcat-xx/webapps

if [ $# -lt 1 ]
then
	echo "Usage: $0 (app name)"
	exit 1
fi

APPNAME=$1
shift

rm -fr $THOME/$APPNAME
