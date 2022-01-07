#!/bin/bash

WAR=war
TGCP=$WAR/WEB-INF/classes
TOOLCP=../OTGTool/classes

function makeWar {
    OUTPUT=toxygates-template.war
    cp -r $TOOLCP/friedrich $TGCP
    cp -r $TOOLCP/t $TGCP
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
    cp -r $TOOLCP/friedrich $TGCP
    cp -r $TOOLCP/t $TGCP
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
[ ! -d $WARLIB ] && mkdir -p $WARLIB
#ivy.sh -retrieve lib/[type]/[artifact]-[revision].[ext] 
rm $WARLIB/*jar
cp lib/{jar,bundle}/*.jar $WARLIB
cp mlib/*.jar $WARLIB
cp ../OTGTool/lib/{jar,bundle}/*.jar $WARLIB
cp ../OTGTool/mlib/*.jar $WARLIB
cp ${GWT_SDK}/gwt-servlet.jar $WARLIB

#These should be in the shared tomcat lib dir (tglobal.jar)
rm $WARLIB/kyotocabinet*jar
rm $WARLIB/scala-library*.jar
rm $WARLIB/scala-parser-combinators*jar
rm $WARLIB/scala-xml*.jar

cp $WAR/WEB-INF/web.xml $WAR/WEB-INF/web.xml.bak

makeWar
makeAdminWar

#Restore
mv $WAR/WEB-INF/web.xml.bak $WAR/WEB-INF/web.xml

jar cf gwtTomcatFilter.jar -C $TGCP t/tomcat
