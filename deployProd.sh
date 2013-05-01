#!/bin/bash
#Run deploy.sh first to generate toxygates.war
scp war/toxygates.war   nibioadmin@targetmine:/opt/apache-tomcat-6.0.35/webapps

