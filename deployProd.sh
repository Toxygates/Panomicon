#!/bin/bash

cp -r ../OTGTool/bin/otg war/WEB-INF/classes
cd war
zip -r otgviewer.war *
scp otgviewer.war nibioadmin@targetmine:/opt/apache-tomcat-6.0.35/webapps
cd ..
rm -r war/WEB-INF/classes/otg

