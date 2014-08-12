#!/bin/bash

THOME=/opt/apache-tomcat-xx/webapps

if [ $# -lt 2 ]
then
	echo "Usage: $0 (app name) (instance name)"
	exit 1
fi


APPNAME=$1
shift
INSTANCE=$1
shift

cp -r $THOME/t_viewer_template $THOME/$APPNAME

cat $THOME/t_viewer_template/web.xml | sed "s/##instanceName##/$INSTANCE/" > $THOME/$APPNAME/web.xml
touch $THOME/$APPNAME

