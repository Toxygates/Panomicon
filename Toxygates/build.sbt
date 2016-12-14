import net.thunderklaus.GwtPlugin._

scalaVersion := "2.11.8"

seq(gwtSettings :_*)

scalaSource in Compile := baseDirectory.value / "src"

javaSource in Compile := baseDirectory.value / "src"

scalaSource in Test := baseDirectory.value / "test"

javaSource in Test := baseDirectory.value / "test"

unmanagedBase := baseDirectory.value / "minlib"

libraryDependencies += "org.mortbay.jetty" % "jetty" % "latest.integration" % "container"

libraryDependencies += "org.apache.commons" % "commons-math3" % "latest.integration"

libraryDependencies += "org.openrdf.sesame" % "sesame-repository-manager" % "2.7.16"

libraryDependencies += "org.openrdf.sesame" % "sesame-repository-sparql" % "2.7.16"

libraryDependencies += "commons-fileupload" % "commons-fileupload" % "latest.integration"

libraryDependencies += "commons-io" % "commons-io" % "latest.integration"

libraryDependencies += "javax.mail" % "mail" % "latest.integration"

libraryDependencies += "com.googlecode.gwtupload" % "gwtupload" % "latest.integration"

//libraryDependencies += "com.google.gwt.google-apis" % "gwt-visualization" % "latest.integration"

libraryDependencies += "org.rosuda.REngine" % "Rserve" % "latest.integration"

//Dependencies for intermine.
//Can change json to latest.integration when we have JRE 8

libraryDependencies += "org.json" % "json" % "20090211"

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

libraryDependencies += "org.apache.httpcomponents" % "fluent-hc" % "latest.integration"

libraryDependencies += "log4j" % "log4j" % "latest.integration" 

libraryDependencies += "org.antlr" % "antlr" % "latest.integration" 

gwtVersion := "2.7.0"

gwtWebappPath := baseDirectory.value / "war"

//gwtModules := Seq("toxygates", "otgadmin")

javaOptions in Gwt ++= Seq("-Xmx2g")

fork := true

javaOptions += "-Djava.library.path=/usr/local/lib"

