#!/bin/bash

cp -r ../OTGTool/bin/friedrich war/WEB-INF/classes
cp -r ../OTGTool/bin/otg war/WEB-INF/classes
cd war
zip -r toxygates.war *
#scp toxygates.war johan@sontaran:/opt/apache-tomcat-6.0.35/webapps
cd ..
rm -r war/WEB-INF/classes/otg

