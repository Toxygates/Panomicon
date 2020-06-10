## Overview

Panomicon (formerly Toxygates) is a web application for user-friendly analysis of multi-omics data (presently focusing on mRNA and mRNA-miRNA interaction data). It was originally developed for Open TG-GATEs dataset but is now being used for more general purposes.

The public version of Panomicon is available at http://toxygates.nibiohn.go.jp/panomicon. The latest public version of Toxygates is available at http://toxygates.nibiohn.go.jp.

The main technologies used in this project are Java (frontend and backend), Scala (backend) and GWT (Google Web Toolkit). Numerical data is stored in Kyoto Cabinet; other kinds of data are stored as RDF (we currently use Fuseki).

The contents of this directory are:

OTGTool/ 
Back-end and command line tools

Toxygates/
GWT-based web frontend

## Compiling

In order to compile Panomicon, currently the following dependencies are required:

* Java version 8 (compiling on higher versions is not recommended).

* GWT SDK 2.8.2 or higher, available from http://www.gwtproject.org/download.html

* Scala SDK (any 2.12 version)

* The ant build tool.

* The kyoto cabinet native libraries as well as JNI libraries for Java interop, available from https://fallabs.com/kyotocabinet/

First, export the following environment libraries:

* KC_LIB_DIR - path to Kyoto Cabinet libraries

* SCALA_HOME - path to Scala SDK

* GWT_SDK - path to GWT SDK

Then, build the back-end by going into the OTGTool directory and running:
`
ant compile
`
After the back-end has been built, the front-end can be compiled in the Toxygates directory by running:
`
ant compile
`
The development mode (for testing) may be run using
`
ant devmode
`
It may be necessary to configure Toxygates/war/WEB-INF/web.xml first. The file web.xml.template may be used as a guide.

## Releasing

In the Toxygates directory, deploy.sh (or deploy.ps1 on Windows) may be run to produce toxygates-template.war, which can be deployed in a servlet application container such as Tomcat.



