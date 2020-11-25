## Overview

Panomicon (formerly Toxygates) is a web application for user-friendly analysis of multi-omics data (presently focusing on mRNA and mRNA-miRNA interaction data). It was originally developed for the Open TG-GATEs dataset but is now being used for more general purposes.

The public version of Panomicon is available at http://toxygates.nibiohn.go.jp/panomicon.

The main technologies used in this project are Java (front end and back end), Scala (back end) and GWT (Google Web Toolkit). Numerical data is stored in Kyoto Cabinet; other kinds of data are stored as RDF (we currently use Fuseki).

The contents of this directory are:

OTGTool/  
Back-end and command line tools

Toxygates/  
GWT-based web frontend

## Compiling

In order to compile Panomicon, the following dependencies are required:

* Java version 8 (compiling on higher versions is not recommended).

* GWT SDK 2.8.2 or higher, available from http://www.gwtproject.org/download.html

* Scala SDK (any 2.12 version)

* The ant build tool.

In order to run Panomicon, it is also necessary to run a RDF triplestore supporting SPARQL 1.1. We use Apache Jena Fuseki.

First, export the following environment variables:

* SCALA_HOME - path to Scala SDK

* GWT_SDK - path to GWT SDK

Then, build the back end by going into the OTGTool directory and running:
`
ant compile
`
After the back end has been built, the front end can be compiled in the Toxygates directory by running:
`
ant compile
`

## Configuration

In order to run Panomicon, it is necessary to create the configuration file Toxygates/war/WEB-INF/web.xml. This can be done by copying web.xml.template and making changes as necessary.

In particular, the following must be configured:
* URLs for an RDF triplestore (SPARQL 1.1) providing query and update access
* A location where Kyoto Cabinet database files can be created and stored

## Populating the database

In addition to the above configuration, Panomicon also requires that the database be initialized and populated with some necessary data. This can be done by running some of the scripts in the OTGTool/scripts directory. In order to run these scripts, you will need access to the command-line tools for Kyoto Cabinet, which can be obtained from https://fallabs.com/kyotocabinet/ (either download and compile the C/C++ libraries, which include the command-line tools, or download the binaries if you are running Windows. Alternatively, if you are using a Linux distribution which has a Kyoto Cabinet package, that can be installed instead.)

Once the Kyoto Cabinet command line tools, such as `kctreemgr` and `kchashmgr`, have been placed in your PATH along with scala, edit the file `config.sh` and set variables appropriately. 
In particular, the data directory and triplestore access must be configured in the same way as in web.xml. OTGTool must also have been successfully compiled as above. This script will be sourced by other scripts to provide configuration.

Then, from the scripts directory, run: `buildData/newDB.sh`
This will initialise omics expression value databases.
After this has been done, run: `buildData/populate.sh`
This will download, filter and populate the following data: 
Ensembl platforms for Human, Rat and Mouse, miRBase miRNA sets, miRDB mRNA-miRNA interactions, and Gene Ontology (GO) terms.

## Testing

Once compilation, configuration, and database initialization are complete, Panomicon can be run in development mode for local testing by running
`
ant devmode
`
in the Toxygates directory.

This will start a web server on port 8888 where the main Panomicon interface can be accessed through /toxygates.html. The admin interface can be accessed through /admin.html.

## Deployment

To deploy Panomicon for networked use, a servlet application container such as Tomcat is required. 

To produce a WAR file that can be deployed in such a container, first compile and configure Panomicon as described above, then run `deploy.sh` (or `deploy.ps1` on Windows) in the Toxygates directory to  produce toxygates-template.war. The script will also produce admin.war, which can be used to deploy the admin UI in a similar fashion.

Be sure to comment out or remove maintenanceServlet (used by the admin UI) from web.xml if you are releasing to the public, since it allows anybody to edit or delete data.
