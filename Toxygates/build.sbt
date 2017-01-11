import net.thunderklaus.GwtPlugin._

scalaVersion := "2.11.8"

seq(gwtSettings :_*)

scalaSource in Compile := baseDirectory.value / "src"

javaSource in Compile := baseDirectory.value / "src"

scalaSource in Test := baseDirectory.value / "test"

javaSource in Test := baseDirectory.value / "test"

//Testing, running etc will be in a separate JVM. Necessary to pick up the -D arguments.
fork := true

javaOptions += "-Djava.library.path=/usr/local/lib"

//* * DEPENDENCIES

libraryDependencies += "org.mortbay.jetty" % "jetty" % "latest.integration" % "container"

libraryDependencies += "commons-fileupload" % "commons-fileupload" % "latest.integration"

libraryDependencies += "commons-io" % "commons-io" % "2.5"

libraryDependencies += "javax.mail" % "mail" % "latest.integration"

libraryDependencies += "com.googlecode.gwtupload" % "gwtupload" % "latest.integration"

libraryDependencies += "org.rosuda.REngine" % "Rserve" % "latest.integration"

//Dependencies for intermine.

libraryDependencies += "org.json" % "json" % "latest.integration"

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

libraryDependencies += "org.apache.httpcomponents" % "fluent-hc" % "latest.integration"

libraryDependencies += "log4j" % "log4j" % "latest.integration" 

libraryDependencies += "org.antlr" % "antlr" % "latest.integration" 

//** GWT SETTINGS

//For gwt-devmode to set classpath correctly
dependencyClasspath in Gwt += baseDirectory.value / "target/scala-2.11/classes"

webappResources in Compile += baseDirectory.value / "WebContent"

gwtVersion := "2.7.0"

gwtModules := Seq("otgviewer.toxygates", "otgviewer.admin.OTGAdmin")

javaOptions in Gwt ++= Seq("-Xmx2g")

//For renaming the packaged war to simply toxygates.war
artifactName in packageWar := { (scalaVersion, module, artifact) => artifact.name + "." + artifact.extension }

warPostProcess in Compile <<= (target) map {
    (target) => {
      () =>
        val webapp = target / "webapp"
        val webInf = webapp / "WEB-INF"
        val classes = webInf / "classes"
        val lib = webInf / "lib"

        //For admin interface only
        IO.delete(classes / "t" / "admin")

        //These go in the "tglobal" jar
        IO.delete(classes / "t" / "common")
        IO.delete(lib / "kyotocabinet-java-1.24.jar")
        IO.delete(lib / "scala-library-2.11.8.jar")
    }
}

//** ECLIPSE SETTINGS

EclipseKeys.relativizeLibs := false

//For downloading source attachments for Eclipse projects.
//Disable to speed up dependency retrieval.
EclipseKeys.withSource := true

