#!/bin/bash

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

cp -r $THOME/t_viewer_template $THOME/$APPNAME

TARGET=$THOME/$APPNAME/WEB-INF/web.xml
cat $THOME/t_viewer_template/WEB-INF/web.xml.template | sed "s/##instanceName##/$INSTANCE/" > $TARGET

cat >> $TARGET <<EOF

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

touch $THOME/$APPNAME

