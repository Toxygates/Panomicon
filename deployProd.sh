#!/bin/bash
#run deploy.sh first to generate toxygates.war
scp war/toxygates.war   root@202.241.38.91:/opt/apache-tomcat-6.0.36/webapps

