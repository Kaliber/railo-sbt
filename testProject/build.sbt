import RailoKeys.{password, serverConfiguration, webConfiguration}

version in Railo := "4.5.0.042"

password in serverConfiguration in Compile := "asdasd"

password in webConfiguration in Compile := "zxczxc"

password in serverConfiguration in Railo := "asdasd"

password in webConfiguration in Railo := "zxczxc"

javaOptions in run in Compile += "-Dconfig.resource=test.conf"

name := "testProject"

lazy val root = project.in( file(".") ).enablePlugins(RailoPlugin)

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.2.1",
  "nl.rhinofly" %% "test-library" % "0.1-SNAPSHOT" % "railo",
  "org.qirx" %% "little-spec" % "0.3"
)
