name := "test-library"

organization := "nl.rhinofly"

version := "0.1-SNAPSHOT"

lazy val root = project.in( file(".") ).enablePlugins(RailoSbtPlugin)

RailoSbtKeys.mappingName in Railo := "test"

RailoSbtPlugin.packaging
