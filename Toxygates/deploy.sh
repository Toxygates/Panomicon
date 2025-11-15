#!/bin/bash

WAR=war
TGCP=$WAR/WEB-INF/classes

function makeWar {
    OUTPUT=$1 
    shift
    WEBXML=$1
    shift

    pushd $WAR
    rm $OUTPUT
    cp $WEBXML WEB-INF/web.xml 
    [ ! -d csv ] && mkdir csv
    rm csv/*.csv

    #html.template is for the deprecated new_instance/delete_instance system
    #jar cf $OUTPUT toxygates images csv *.pdf *.css *.html.template *.zip 

    jar cf $OUTPUT toxygates images csv *.pdf *.css toxygates.html *.zip 
    #Exclude classes in some packages
    jar uf $OUTPUT $(find WEB-INF \( \
      -path WEB-INF/classes/t/admin -o \
      -path WEB-INF/classes/t/tomcat \) -prune -o \( -type f -print \) )
    popd
}

function makeAdminWar {
    pushd $WAR
    cp WEB-INF/web.xml.admin WEB-INF/web.xml
    rm admin.war
    jar cf admin.war AdminConsole admin.html *.css images
    jar uf admin.war $(find WEB-INF \
      -path WEB-INF/classes/t/tomcat -prune -o \
      \( -type f -print \) )
    popd
}


cp $WAR/WEB-INF/web.xml $WAR/WEB-INF/web.xml.bak

#This template war is intended for the new_instance/delete_instance scripts, now deprecated.
#makeWar toxygates-template.jar WEB-INF/web.xml.template

makeWar toxygates.war WEB-INF/web.xml.docker
makeAdminWar

#Restore
mv $WAR/WEB-INF/web.xml.bak $WAR/WEB-INF/web.xml

jar cf gwtTomcatFilter.jar -C $TGCP t/tomcat

#jar cf tglobal.jar -C $TGCP t/global
