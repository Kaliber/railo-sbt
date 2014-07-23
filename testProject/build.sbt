name := "testProject"

lazy val root = project.in( file(".") ).enablePlugins(RailoSbtPlugin)

libraryDependencies += 
  "nl.rhinofly" %% "test-library" % "0.1-SNAPSHOT" % "railo"

RailoSbtPlugin.libraryDependencies
