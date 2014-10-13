name := "jetty-runner"

organization := "nl.rhinofly"

version := "0.1-SNAPSHOT"

crossScalaVersions := Seq("2.10.4", "2.11.2")

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-webapp" % "9.2.3.v20140905",
  "javax.servlet" % "javax.servlet-api" % "3.1.0",
  "javax.servlet.jsp" % "javax.servlet.jsp-api" % "2.2.1",
  "javax.el" % "javax.el-api" % "2.2.1",
  "org.fusesource.jansi" % "jansi" % "1.11",
  "jline" % "jline" % "2.11"
)