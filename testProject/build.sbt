import RailoKeys.{password, serverConfiguration, webConfiguration}

version in Railo := "4.5.0.042"

password in serverConfiguration in Railo := "asdasd"

password in webConfiguration in Railo := "zxczxc"

javaOptions in run in Railo += "-Dconfig.resource=test.conf"

lazy val root = project.in( file(".") ).enablePlugins(RailoApplication)

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.2.1",
  "nl.rhinofly" %% "test-library" % "0.1-SNAPSHOT" % "railo",
  "org.qirx" %% "little-spec" % "0.3"
)

testFrameworks += new TestFramework("org.qirx.littlespec.sbt.TestFramework")
