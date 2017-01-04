## BUILDING

1. Install Scala 2.11.8 or later (http://scala-lang.org)
2. Install SBT (http://scala-sbt.org)
3. Run `sbt compile` to compile Java and Scala classes.
4. Run `sbt gwt-compile` to compile client side Java classes to Javascript.

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

## Deploying

Run `sbt packageWar`. This will generate a file such as Toxygates/target/scala-2.11/toxygates.war.
Copy it to a suitable location (e.g. your Tomcat webapps/ directory).

Note that if you are using the kyoto cabinet database, libkyotocabinet and libjkyotocabinet must be accessible by the JVM.
This can be achieved by supplying a JVM option such as `-Djava.library.path=/usr/local/lib` to the JVM running the application server.

## Creating an Eclipse project

SBT can automatically generate Eclipse projects with dependencies configured properly.

1. Install Eclipse and the Scala plugin (but it may be easier to install the Scala IDE, which is Eclipse pre-bundled with Scala: http://scala-ide.org/)
2. Inside Eclipse, install the GWT plugin from Google: http://www.gwtproject.org/usingeclipse.html
3. Run `sbt eclipse`
4. Inside Eclipse, import existing projects. First import the OTGTool, then the Toxygates one (since the latter depends on the former).
5. Set the output directory for .class files to war/WEB-INF/classes.
6. Right click on the Toxygates project, access the GWT settings, and enable the GWT plugin for that project. The Google App Engine does not need to be enabled.
7. Test your setup by invoking GWT compile on the Toxygates project.

From now on, you can develop and compile entirely inside Eclipse, but you probably still want to use sbt for deployment.


