scalaVersion := "2.11.7"

libraryDependencies += "org.apache.commons" % "commons-math3" % "latest.integration"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "latest.integration"

libraryDependencies += "org.openrdf.sesame" % "sesame-repository-manager" % "2.7.16"

libraryDependencies += "org.openrdf.sesame" % "sesame-repository-sparql" % "2.7.16"

libraryDependencies += "com.fallabs" % "kyotocabinet-java" % "latest.integration"

//For release - suppress SLF4J NOP warnings
//libraryDependencies += "org.slf4j" % "slf4j-nop" % "latest.integration"

//Download source attachments

//EclipseKeys.withSource := true
