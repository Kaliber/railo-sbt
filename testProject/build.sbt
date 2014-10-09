name := "testProject"

lazy val root = project.in( file(".") ).enablePlugins(RailoSbtPlugin)

libraryDependencies ++= Seq( 
  "nl.rhinofly" % "test-library" % "0.1-SNAPSHOT" % "railo",
  "org.qirx" %% "little-spec" % "0.3"
)
