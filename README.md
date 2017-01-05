## Overview

## Project structure

## Building

1. Install Scala 2.11.8 or later (http://scala-lang.org)
2. Install SBT (http://scala-sbt.org)
3. To use Kyoto Cabinet databases, install Kyoto Cabinet (http://fallabs.com/kyotocabinet/). Currently, this is the only matrix database backend, though in the future, other backends may be available. Note that it is necessary to install the native libraries (libkyotocabinet and libjkyotocabinet).
4. Run `sbt compile` to compile Java and Scala classes.
5. Run `sbt gwt-compile` to compile client side Java classes to Javascript.

Note that the gwt plugin for SBT has limitations at the moment. If you experience any errors during the gwt-compile phase, you may not see the error messages properly. In that case, please try compiling with Eclipse as below.

## Testing

In order to test (or otherwise use) the Kyoto cabinet database, libkyotocabinet and libjkyotocabinet must be accessible by the JVM.
Edit Toxygates/build.sbt and OTGTool/build.sbt and change lines such as
`
javaOptions += "-Djava.library.path=/usr/local/lib"
`
accordingly.

Run `sbt test` in the root directory.

## Running development mode

If using Kyoto cabinet, see the note in the Testing section above. That also applies here.

Run `sbt gwt-devmode`. A GUI will open, from which you can launch the web application in a browser.

For the application to work, the various settings in war/WEB-INF/web.xml must be properly configured. In particular, you must set repositoryURL, updateURL and dataDir correctly.

To be written: instructions for deploying R and Rserve

## Deploying

Run `sbt packageWar`. This will generate a file such as Toxygates/target/scala-2.11/toxygates.war.
Copy it to a suitable location (e.g. your Tomcat webapps/ directory) to deploy.

Note that if you are using the kyoto cabinet database, libkyotocabinet and libjkyotocabinet must be accessible, as above.
This can be achieved by supplying a JVM option such as `-Djava.library.path=/usr/local/lib` to the JVM running the application server.

To be written: instructions for deploying R and Rserve

## Creating an Eclipse project

SBT can automatically generate Eclipse projects with dependencies configured properly.

1. Install Eclipse and the Scala plugin (but it may be easier to install the Scala IDE, which is Eclipse pre-bundled with Scala: http://scala-ide.org/)
1. Inside Eclipse, install the GWT plugin from Google: http://www.gwtproject.org/usingeclipse.html
1. Run `sbt eclipse`
1. Inside Eclipse, import existing projects. First import the OTGTool project, then the Toxygates one (since the latter depends on the former).
1. Set the output directory for compiled java classes to war/WEB-INF/classes (accessible from the build path settings, source directory).
1. After having run sbt compile, copy the jar dependencies into war/WEB-INF/lib, e.g. 

    find lib_managed -name "*.jar" -exec cp \{\} Toxygates/war/WEB-INF/lib \;

Unfortunately, you will need to remove the old jars and repeat this step if the dependencies change.

1. Right click on the Toxygates project, access the GWT settings, and enable the GWT plugin for that project. The Google App Engine does not need to be enabled.
1. Test your setup by invoking GWT compile on the Toxygates project.
1. To run the development mode, use the "GWT application - super dev mode" configuration tenplate. You will need to set the JVM arguments, e.g. -Djava.library.path, as above.

From now on, you can develop and compile entirely inside Eclipse, but you probably still want to use sbt for deployment.


