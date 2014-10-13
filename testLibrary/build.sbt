version in Railo := "4.3.0.002"

name in Railo := "test"

name := "test-library"

organization := "nl.rhinofly"

version := "0.1-SNAPSHOT"

lazy val root = project.in( file(".") ).enablePlugins(RailoPlugin)

libraryDependencies ++= Seq(
  "org.qirx" %% "little-spec" % "0.3" % "test"
)
