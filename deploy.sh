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
    zip -r toxygates-$VERSION.war toxygates AdminConsole csv *.html *.pdf *.css images WEB-INF
    cd ..
    rm -r war/WEB-INF/classes/otg
    rm -r war/WEB-INF/classes/friedrich
}

cp war/toxygates.html war/toxygates.html.bak
cp war/WEB-INF/web.xml war/WEB-INF/web.xml.bak

makeWar production
makeWar test
makeWar adju

cp war/toxygates.html.bak war/toxygates.html
cp war/WEB-INF/web.xml.bak war/WEB-INF/web.xml

