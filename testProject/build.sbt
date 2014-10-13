import RailoKeys.{password, serverConfiguration, webConfiguration}

version in Railo := "4.3.0.002"

password in serverConfiguration in Compile := "asdasd"

password in webConfiguration in Compile := "zxczxc"

javaOptions in run in Compile += "-Dconfig.resource=test.conf"

name := "testProject"

lazy val root = project.in( file(".") ).enablePlugins(RailoPlugin)

libraryDependencies ++= Seq( 
  "nl.rhinofly" % "test-library" % "0.1-SNAPSHOT" % "railo",
  "org.qirx" %% "little-spec" % "0.3"
)
