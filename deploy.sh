#!/bin/bash

function makeWar {
    VERSION=$1
    cp -r ../OTGTool/bin/friedrich war/WEB-INF/classes
    cp -r ../OTGTool/bin/otg war/WEB-INF/classes
    cp -r ../OTGTool/bin/t war/WEB-INF/classes
    cd war
    cp toxygates.html.$VERSION toxygates.html
    cp news.html.$VERSION news.html
    cp WEB-INF/web.xml.$VERSION WEB-INF/web.xml
    rm toxygates-$VERSION.war
    jar cf toxygates-$VERSION.war toxygates otggui csv *.html *.pdf *.css images *.html.template
    jar uf toxygates-$VERSION.war  $(find WEB-INF \( -path WEB-INF/classes/t/admin -o \
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
cp lib/*.jar $WARLIB
cp lib/jar/* $WARLIB
cp lib/bundle/*.jar $WARLIB
#This should be in the shared tomcat lib dir (tglobal.jar)
rm $WARLIB/kyotocabinet*jar
rm $WARLIB/scala-library.jar

cp war/toxygates.html war/toxygates.html.bak
cp war/news.html war/news.html.bak
cp war/WEB-INF/web.xml war/WEB-INF/web.xml.bak

makeWar test
makeAdminWar

cp war/toxygates.html.bak war/toxygates.html
cp war/news.html.bak war/news.html
cp war/WEB-INF/web.xml.bak war/WEB-INF/web.xml

