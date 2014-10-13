name := "railo-sbt"

organization := "nl.rhinofly"

scalaVersion := "2.10.4"

sbtPlugin := true

resolvers += "http://cfmlprojects.org/artifacts/" at "http://cfmlprojects.org/artifacts/"
 
libraryDependencies ++= Seq(
  // Waiting for an answer to:
  // http://stackoverflow.com/questions/26328744/starting-a-webappcontext-in-isolation
  // we should be able to retrieve rail intransitive
  "org.getrailo" % "railo" % "4.3.0.001",// intransitive(),
  "nl.rhinofly" %% "jetty-runner" % "0.1-SNAPSHOT"
)
