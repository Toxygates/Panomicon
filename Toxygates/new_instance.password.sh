#!/bin/bash

#The main difference between this script and new_instance.public 
#is that the cat << EOF part at the end is more complicated.

THOME=/shiba/toxygates/webapp_test
#THOME=/opt/apache-tomcat-6.0.36/webapps

if [ $# -lt 3 ]
then
    echo "Usage: $0 (app name) (instance name) (tomcat role)"
    exit 1
fi


APPNAME=$1
shift
INSTANCE=$1
shift
ROLE=$1

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

<security-constraint>
  <web-resource-collection>
     <web-resource-name>Toxygates (Development version)</web-resource-name>
     <url-pattern>/*</url-pattern>
  </web-resource-collection>
  <auth-constraint>
    <role-name>$ROLE</role-name>
  </auth-constraint>
</security-constraint>

<login-config>
  <auth-method>BASIC</auth-method>
  <realm-name>$APPNAME</realm-name>
</login-config>

</web-app>

EOF

touch $TDIR

