// depend on the plugin itself
//unmanagedSourceDirectories in Compile += {
//  val root = baseDirectory.value.getParentFile
//  root / "src/main/scala"
//}

resolvers += "http://cfmlprojects.org/artifacts/" at "http://cfmlprojects.org/artifacts/"

lazy val pluginRoot = project.in( file(".") ) dependsOn plugin

lazy val plugin = RootProject(file("../../plugin") )
