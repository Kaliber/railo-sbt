version in Railo := "4.5.0.042"

name in Railo := "testLibrary"

name := "test-library"

organization := "nl.rhinofly"

version := "0.1-SNAPSHOT"

lazy val root = project.in( file(".") ).enablePlugins(RailoLibrary)

libraryDependencies ++= Seq(
  "org.qirx" %% "little-spec" % "0.3" % "test"
)

testFrameworks += new TestFramework("org.qirx.littlespec.sbt.TestFramework")
