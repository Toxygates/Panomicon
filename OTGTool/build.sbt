import AssemblyKeys._ // put this at the top of the file

scalaVersion := "2.11.8"

sbtVersion := "0.13.13"

assemblySettings

libraryDependencies += "org.apache.commons" % "commons-math3" % "latest.integration"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "latest.integration"

libraryDependencies += "org.openrdf.sesame" % "sesame-repository-manager" % "2.7.16"

libraryDependencies += "org.openrdf.sesame" % "sesame-repository-sparql" % "2.7.16"

libraryDependencies += "com.fallabs" % "kyotocabinet-java" % "latest.integration"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

//For release - suppress SLF4J NOP warnings
//libraryDependencies += "org.slf4j" % "slf4j-nop" % "latest.integration"

//Download source attachments

//EclipseKeys.withSource := true

test in assembly := {}

//assemblyJarName in assembly := "otgtool.jar"

fork := true

javaOptions += "-Djava.library.path=/usr/local/lib"
