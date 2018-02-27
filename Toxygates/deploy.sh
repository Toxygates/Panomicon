#!/bin/bash

TGCP=war/WEB-INF/classes
TOOLCP=../OTGTool/classes

function makeWar {
    OUTPUT=toxygates-template.war
    cp -r $TOOLCP/friedrich $TGCP
    cp -r $TOOLCP/otg $TGCP
    cp -r $TOOLCP/t $TGCP
    cd war
    rm $OUTPUT
    rm WEB-INF/web.xml
    [ ! -d csv ] && mkdir csv
    rm csv/*.csv
    jar cf $OUTPUT toxygates images csv *.pdf *.css *.html.template *.zip
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
cp lib/jar/*.jar $WARLIB
cp lib/bundle/*.jar $WARLIB
cp mlib/*.jar $WARLIB
cp ../OTGTool/lib/jar/*.jar $WARLIB
cp ../OTGTool/lib/bundle/*.jar $WARLIB
cp ../OTGTool/mlib/*.jar $WARLIB


#These should be in the shared tomcat lib dir (tglobal.jar)
rm $WARLIB/kyotocabinet*jar
rm $WARLIB/scala-library.jar
#These should not be deployed in a servlet context
rm $WARLIB/servlet-api*.jar
rm $WARLIB/javax.servlet-api*.jar
rm $WARLIB/javaee-api*jar
rm $WARLIB/scalatest*jar
rm $WARLIB/gwt-user.jar
rm $WARLIB/scala-xml*.jar

cp war/WEB-INF/web.xml war/WEB-INF/web.xml.bak

makeWar
makeAdminWar

mv war/WEB-INF/web.xml.bak war/WEB-INF/web.xml

