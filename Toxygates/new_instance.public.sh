#!/bin/bash

THOME=/shiba/toxygates/webapp_test
#THOME=/opt/apache-tomcat-6.0.36/webapps

if [ $# -lt 2 ]
then
	echo "Usage: $0 (app name) (instance name)"
	exit 1
fi

APPNAME=$1
shift
INSTANCE=$1
shift

TDIR=$THOME/$APPNAME
if [ -d $TDIR -o -f $TDIR ]
then
	echo "$TDIR already exists. Cannot create."
	exit 1
fi

cp -r $THOME/t_viewer_template $TDIR
mkdir -p $THOME/shared/$INSTANCE

SDIR=$THOME/t_viewer_template
cat $THOME/t_viewer_template/WEB-INF/web.xml.template | sed "s/##instanceName##/$INSTANCE/" > $TDIR/WEB-INF/web.xml
cat $THOME/t_viewer_template/toxygates.html.template_nibiohn | sed "s/##instanceName##/$INSTANCE/" > $TDIR/toxygates.html

cat >> $TDIR/WEB-INF/web.xml <<EOF
</web-app>
EOF

touch $TDIR
