name := "test-library"

organization := "nl.rhinofly"

lazy val root = project.in( file(".") ).enablePlugins(RailoSbtPlugin)

RailoSbtPlugin.packaging
