scalaVersion := "2.11.8"

lazy val otgtool = (project in file("OTGTool"))

lazy val toxygates = (project in file("Toxygates")).dependsOn(otgtool % "test->test;compile->compile")

lazy val root = (project in file(".")).aggregate(otgtool,toxygates)
