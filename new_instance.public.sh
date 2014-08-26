#!/bin/bash

THOME=/shiba/toxygates/webapp_test

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

TARGET=$THOME/$APPNAME/WEB-INF/web.xml
cat $THOME/t_viewer_template/WEB-INF/web.xml | sed "s/##instanceName##/$INSTANCE/" > $TARGET

cat >> $TARGET <<EOF
</web-app>
EOF

touch $THOME/$APPNAME
