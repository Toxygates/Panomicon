#!/bin/bash

WAR=war
TGCP=$WAR/WEB-INF/classes

function makeWar {
    OUTPUT=toxygates-template.war
    pushd $WAR
    rm $OUTPUT
    rm WEB-INF/web.xml
    [ ! -d csv ] && mkdir csv
    rm csv/*.csv
    jar cf $OUTPUT toxygates images csv *.pdf *.css *.html.template *.zip
    #Exclude classes in some packages
    jar uf $OUTPUT $(find WEB-INF \( -path WEB-INF/classes/t/admin -o \
      -path WEB-INF/classes/t/global -o \
      -path WEB-INF/classes/t/tomcat \) -prune -o \( -type f -print \) )
    popd
}

function makeAdminWar {
    pushd $WAR
    cp WEB-INF/web.xml.admin WEB-INF/web.xml
    rm admin.war
    jar cf admin.war AdminConsole admin.html *.css images
    jar uf admin.war $(find WEB-INF -path WEB-INF/classes/t/global -prune -o \
      -path WEB-INF/classes/t/tomcat -o \
      \( -type f -print \) )
    popd
}

WARLIB=$WAR/WEB-INF/lib

cp $WAR/WEB-INF/web.xml $WAR/WEB-INF/web.xml.bak

makeWar
makeAdminWar

#Restore
mv $WAR/WEB-INF/web.xml.bak $WAR/WEB-INF/web.xml

jar cf gwtTomcatFilter.jar -C $TGCP t/tomcat
jar cf tglobal.jar -C $TGCP t/global
