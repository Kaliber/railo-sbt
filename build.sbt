
organization := "nl.rhinofly"

val railoResolver = settingKey[Seq[String]]("The railo resolver name and url")

val railoDependencyBase = settingKey[Seq[String]]("The railo dependency organization and artifact")

val runnerDependency = taskKey[Seq[String]]("The runner dependency organization, artifact and version")

val compilerDependency = taskKey[Seq[String]]("The compiler dependency organization, artifact and version")


railoResolver in Global := Seq("http://cfmlprojects.org/artifacts/", "http://cfmlprojects.org/artifacts/")

railoDependencyBase in Global := Seq("org.getrailo", "railo")

runnerDependency in Global := {
  val project = (projectID in `jetty-runner`).value
  Seq(project.organization, project.name, project.revision)
}

compilerDependency in Global := {
  val project = (projectID in `railo-compiler`).value
  Seq(project.organization, project.name, project.revision)
}

val servletApiDependency = "javax.servlet" % "javax.servlet-api" % "3.1.0"

val servletJspApiDependency = "javax.servlet.jsp" % "javax.servlet.jsp-api" % "2.2.1"

val elApiDependency = "javax.el" % "javax.el-api" % "2.2.1"


scalaVersion := "2.11.2"

crossScalaVersions := Seq("2.10.4", scalaVersion.value)

lazy val root = project.in( file(".") )
  .aggregate(`railo-sbt`, `jetty-runner`, `railo-compiler`, `runner-interface`, `compiler-interface`)
  
lazy val `railo-sbt` = project.in( file("plugin") )
  .settings(buildInfoSettings: _*)
  .settings(
    name := "railo-sbt",
    scalaVersion := "2.10.4",
    crossScalaVersions := Seq("2.10.4"),
    sbtPlugin := true,
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := Seq[BuildInfoKey](
      railoResolver in Global,
      railoDependencyBase in Global,
      runnerDependency in Global, // in plugin, add .cross(CrossVersion.binary)
      compilerDependency in Global // in plugin, add .cross(CrossVersion.binary)
    ),
    buildInfoPackage := "nl.rhinofly.build",
    buildInfoObject := "BuildInfo"
  )
  .dependsOn(`compiler-interface`, `runner-interface`)

lazy val `railo-compiler` = project.in( file("compiler") )
  .settings(
    name := "railo-compiler",
    resolvers += {
      val Seq(name, url) = (railoResolver in ThisBuild).value
      name at url
    },
    libraryDependencies ++= {
      val Seq(organization, name) = (railoDependencyBase in ThisBuild).value
      val railoDependency = organization % name
      Seq(
        // matches all versions greater or equal to 4.3 and lower than 4.4
        railoDependency % "[4.3.0.000,4.4.0.000[" % "provided",
        servletApiDependency % "provided",
        servletJspApiDependency % "provided",
        elApiDependency % "provided"
      )
    }
  )
  .dependsOn(`compiler-interface`)

lazy val `compiler-interface` = project.in( file("compiler-interface") )
  .settings(
    name := "railo-compiler-interface"
  )
  .dependsOn(`runner-interface`)

lazy val `jetty-runner` = project.in( file("runner") )
  .settings(
    name := "jetty-runner",
    libraryDependencies ++= Seq(
      "org.eclipse.jetty" % "jetty-webapp" % "9.2.3.v20140905",
      servletApiDependency,
      servletJspApiDependency,
      elApiDependency,
      "org.fusesource.jansi" % "jansi" % "1.11",
      "jline" % "jline" % "2.11"
    )
  )
  .dependsOn(`runner-interface`)
  
lazy val `runner-interface` = project.in( file("runner-interface") )
  .settings(
    name := "jetty-runner-interface",
    libraryDependencies ++= Seq(
      servletApiDependency % "provided",
      // matches all versions greater or equal to 9.2 and lower than 9.3
      "org.eclipse.jetty" % "jetty-webapp" % "[9.2.3.v00000000,9.3.0.v00000000[" % "provided"
    )
  )