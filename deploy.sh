#!/bin/bash

rm otgviewer.war
cp -r ../Friedrich/bin/friedrich/util war/WEB-INF/classes/friedrich
cp -r ../Friedrich/bin/friedrich/statistics war/WEB-INF/classes/friedrich
cp -r ../OTGTool/bin/otg war/WEB-INF/classes
cd war
zip -r otgviewer.war *
scp otgviewer.war johan@sontaran:/opt/apache-tomcat-6.0.35/webapps
#scp otgviewer.war johan@sontaran:~
cd ..
rm -r war/WEB-INF/classes/otg

