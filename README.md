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

### Angular application (new)

`docker build -f Toxygates/Dockerfile .`

### GWT application (old)

The GWT application can be compiled with Docker even if it is ultimately run using a servlet container such as Tomcat.

`docker build -f Toxygates/Dockerfile.gwt . --output=.`

The resulting WAR files will be copied to `./tg-build`.

## Configuration

In order to run Panomicon, it is necessary to create the configuration file Toxygates/war/WEB-INF/web.xml. This can be done by copying web.xml.template and making changes as necessary.

In particular, the following must be configured:
* URLs for an RDF triplestore (SPARQL 1.1) providing query and update access
* A location where Kyoto Cabinet database files can be created and stored

## Populating the database

In addition to the above configuration, Panomicon also requires that the database be initialized and populated with some necessary data. This can be done by running some of the scripts in the OTGTool/scripts directory. In order to run these scripts, you will need access to the command-line tools for Kyoto Cabinet, which can be obtained from https://fallabs.com/kyotocabinet/ (either download and compile the C/C++ libraries, which include the command-line tools, or download the binaries if you are running Windows. Alternatively, if you are using a Linux distribution which has a Kyoto Cabinet package, that can be installed instead.)

Once the Kyoto Cabinet command line tools, such as `kctreemgr` and `kchashmgr`, have been placed in your PATH along with scala, edit the file `config.sh` and set variables appropriately.  In particular, the data directory and triplestore access must be configured in the same way as in web.xml. 

Compile the back-end tools by going into the OTGTool directory and running:
`
ant compile
`

Then, from the scripts directory, run: `buildData/newDB.sh`
This will initialise omics expression value databases.
After this has been done, run: `buildData/populate.sh`
This will download, filter and populate the following data: 
Ensembl platforms for Human, Rat and Mouse, miRBase miRNA sets, miRDB mRNA-miRNA interactions, and Gene Ontology (GO) terms.

## Running development mode

Once compilation, configuration, and database initialization are complete, Panomicon can be run in development mode for local testing.

### Angular application (new)

First, start the backend by running

`ant scalatra`

in the Toxygates directory.

Next, go into the angular directory and follow the instructions there.

### GWT application (old)
Since we no longer use the embedded Jetty servlet container in devmode, it is necessary to have a separate servlet container, such as tomcat, for this. The simplest way is to symlink the war/ directory to e.g. /var/lib/tomcat/webapps/panomicon (in the case of tomcat, and depending on the location of your tomcat installation). 

Next, run
`
ant devmode
`
in the Toxygates directory. This will start the dynamic code server that allows client-side code changes to propagate when you reload the application.

With devmode and tomcat (or some other servlet container) running, you should be able to go to e.g. localhost:8080/panomicon/toxygates.html.

If you do not need dynamic code reloading, you can simply compile the application with `ant compile` and use a static version at the same URL without devmode.

## Deployment

To deploy Panomicon for networked use, a servlet application container such as Tomcat is required. 

To produce a WAR file that can be deployed in such a container, first compile and configure Panomicon as described above, then run `deploy.sh` (or `deploy.ps1` on Windows) in the Toxygates directory to  produce toxygates-template.war. The script will also produce admin.war, which can be used to deploy the admin UI in a similar fashion.

Be sure to comment out or remove maintenanceServlet (used by the admin UI) from web.xml if you are releasing to the public, since it allows anybody to edit or delete data.
