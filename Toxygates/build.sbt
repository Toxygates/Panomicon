import net.thunderklaus.GwtPlugin._

scalaVersion := "2.11.8"

seq(gwtSettings :_*)

scalaSource in Compile := baseDirectory.value / "src"

javaSource in Compile := baseDirectory.value / "src"

//For gwt-devmode to set classpath correctly
dependencyClasspath in Gwt += baseDirectory.value / "target/scala-2.11/classes"

scalaSource in Test := baseDirectory.value / "test"

javaSource in Test := baseDirectory.value / "test"

libraryDependencies += "org.mortbay.jetty" % "jetty" % "latest.integration" % "container"

//libraryDependencies += "org.apache.commons" % "commons-math3" % "latest.integration"

libraryDependencies += "commons-fileupload" % "commons-fileupload" % "latest.integration"

libraryDependencies += "commons-io" % "commons-io" % "2.5"

libraryDependencies += "javax.mail" % "mail" % "latest.integration"

libraryDependencies += "com.googlecode.gwtupload" % "gwtupload" % "latest.integration"

//libraryDependencies += "com.google.gwt.google-apis" % "gwt-visualization" % "latest.integration"

libraryDependencies += "org.rosuda.REngine" % "Rserve" % "latest.integration"

//Dependencies for intermine.
//Can change json to latest.integration when we have JRE 8

libraryDependencies += "org.json" % "json" % "latest.integration"

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

libraryDependencies += "org.apache.httpcomponents" % "fluent-hc" % "latest.integration"

libraryDependencies += "log4j" % "log4j" % "latest.integration" 

libraryDependencies += "org.antlr" % "antlr" % "latest.integration" 

gwtVersion := "2.7.0"

//gwtWebappPath := baseDirectory.value / "war"

gwtModules := Seq("otgviewer.toxygates", "otgviewer.admin.OTGAdmin")

javaOptions in Gwt ++= Seq("-Xmx2g")

//Testing, running etc will be in a separate JVM
fork := true

javaOptions += "-Djava.library.path=/usr/local/lib"

webappResources in Compile += baseDirectory.value / "WebContent"

EclipseKeys.relativizeLibs := false

//For downloading source attachments for Eclipse projects.
//Disable to speed up dependency retrieval.
EclipseKeys.withSource := true

//For renaming the packaged war to simply toxygates.war
artifactName in packageWar := { (scalaVersion, module, artifact) => artifact.name + "." + artifact.extension }

//retrieveManaged := true
