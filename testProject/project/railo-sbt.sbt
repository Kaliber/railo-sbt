lazy val pluginRoot = project.in( file(".") ) dependsOn plugin

lazy val plugin = ProjectRef(file("../../"), "railo-sbt")
