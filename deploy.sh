#!/bin/bash

cp -r ../Friedrich/bin/friedrich/util war/WEB-INF/classes/friedrich
cp -r ../Friedrich/bin/friedrich/data war/WEB-INF/classes/friedrich
cp -r ../OTGTool/bin/otg war/WEB-INF/classes
cd war
zip -r toxygates.war *
scp toxygates.war johan@sontaran:/opt/apache-tomcat-6.0.35/webapps
#scp toxygates.war johan@sontaran:~
cd ..
rm -r war/WEB-INF/classes/otg

