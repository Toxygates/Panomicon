import AssemblyKeys._ // put this at the top of the file

scalaVersion := "2.11.8"

sbtVersion := "0.13.13"

assemblySettings

libraryDependencies += "org.apache.commons" % "commons-math3" % "latest.integration"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "latest.integration"

libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-repository-sparql" % "latest.integration"

libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-repository-manager" % "latest.integration"

libraryDependencies += "org.eclipse.rdf4j" % "rdf4j-repository-http" % "latest.integration"

libraryDependencies += "com.fallabs" % "kyotocabinet-java" % "latest.integration"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

//For release - suppress SLF4J NOP warnings
//libraryDependencies += "org.slf4j" % "slf4j-nop" % "latest.integration"

//Download source attachments
EclipseKeys.withSource := true

test in assembly := {}

//assemblyJarName in assembly := "otgtool.jar"

fork := true

javaOptions += "-Djava.library.path=/usr/local/lib"
