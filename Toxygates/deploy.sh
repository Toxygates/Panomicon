#!/bin/bash

TGCP=war/WEB-INF/classes
TOOLCP=../OTGTool/bin

function makeWar {
    VERSION=$1
    OUTPUT=toxygates-template.war
    cp -r $TOOLCP/friedrich war/WEB-INF/classes
    cp -r $TOOLCP/otg war/WEB-INF/classes
    cp -r $TOOLCP/t war/WEB-INF/classes
    cd war
    rm $OUTPUT
    rm WEB-INF/web.xml
    [ ! -d csv ] && mkdir csv
    jar cf $OUTPUT toxygates csv *.pdf *.css images *.html.template
    #Exclude classes in t/admin and t/global
    jar uf $OUTPUT $(find WEB-INF \( -path WEB-INF/classes/t/admin -o \
      -path WEB-INF/classes/t/global \) -prune -o \( -type f -print \) )
    cd ..
}

function makeAdminWar {
    cp -r $TOOLCP/friedrich war/WEB-INF/classes
    cp -r $TOOLCP/otg war/WEB-INF/classes
    cp -r $TOOLCP/t war/WEB-INF/classes
    cd war
    cp WEB-INF/web.xml.admin WEB-INF/web.xml
    rm admin.war
    jar cf admin.war OTGAdmin admin.html *.css images
    jar uf admin.war $(find WEB-INF -path WEB-INF/classes/t/global -prune -o \
      \( -type f -print \) )
    cd ..
}

WARLIB=war/WEB-INF/lib
#ivy.sh -retrieve lib/[type]/[artifact]-[revision].[ext] 
rm $WARLIB/*jar
cp lib/jar/* $WARLIB
cp lib/bundle/*.jar $WARLIB
cp mlib/*jar $WARLIB

#These should be in the shared tomcat lib dir (tglobal.jar)
rm $WARLIB/kyotocabinet*jar
rm $WARLIB/scala-library.jar
#These should not be deployed in a servlet context
rm $WARLIB/servlet-api*.jar
rm $WARLIB/javaee-api*jar
rm $WARLIB/scalatest*jar

cp war/WEB-INF/web.xml war/WEB-INF/web.xml.bak

makeWar
makeAdminWar

mv war/WEB-INF/web.xml.bak war/WEB-INF/web.xml

