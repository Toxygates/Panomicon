#!/bin/bash

function makeWar {
    VERSION=$1
    OUTPUT=toxygates-$VERSION.war
    cp -r ../OTGTool/bin/friedrich war/WEB-INF/classes
    cp -r ../OTGTool/bin/otg war/WEB-INF/classes
    cp -r ../OTGTool/bin/t war/WEB-INF/classes
    cd war
    rm $OUTPUT
    jar cf $OUTPUT toxygates otggui csv *.pdf *.css images *.html.template
    jar uf $OUTPUT $(find WEB-INF \( -path WEB-INF/classes/t/admin -o \
      -path WEB-INF/classes/t/global \) -prune -o \( -type f -print \) )
    cd ..
    rm -r war/WEB-INF/classes/otg
    rm -r war/WEB-INF/classes/friedrich
}

function makeAdminWar {
    cp -r ../OTGTool/bin/friedrich war/WEB-INF/classes
    cp -r ../OTGTool/bin/otg war/WEB-INF/classes
    cp -r ../OTGTool/bin/t war/WEB-INF/classes
    cd war
    cp WEB-INF/web.xml.admin WEB-INF/web.xml
    rm admin.war
    jar cf admin.war AdminConsole admin.html *.css images
    jar uf admin.war $(find WEB-INF -path WEB-INF/classes/t/global -prune -o \
      \( -type f -print \) )
    cd ..
    rm -r war/WEB-INF/classes/otg
    rm -r war/WEB-INF/classes/friedrich
}

WARLIB=war/WEB-INF/lib
ivy.sh -retrieve lib/[type]/[artifact]-[revision].[ext] 
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

cp war/toxygates.html war/toxygates.html.bak
cp war/news.html war/news.html.bak
cp war/WEB-INF/web.xml war/WEB-INF/web.xml.bak

makeWar template
makeAdminWar

mv war/toxygates.html.bak war/toxygates.html
mv war/news.html.bak war/news.html
mv war/WEB-INF/web.xml.bak war/WEB-INF/web.xml

