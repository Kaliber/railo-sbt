name := "railo-sbt"

organization := "nl.rhinofly"

scalaVersion := "2.10.4"

sbtPlugin := true

resolvers += "http://cfmlprojects.org/artifacts/" at "http://cfmlprojects.org/artifacts/"
 
libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-servlet" % "9.0.3.v20130506",
  "org.eclipse.jetty" % "jetty-webapp" % "9.0.3.v20130506",
  "org.getrailo" % "railo" % "4.3.0.001",
  "org.getrailo" % "railo-rc" % "4.3.0.001",
  "org.mortbay.jetty" % "jsp-2.1-glassfish" % "2.1.v20100127",
  "org.fusesource.jansi" % "jansi" % "1.11"
)
