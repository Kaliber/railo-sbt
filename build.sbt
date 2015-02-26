// Note that some of the structure in here is a bit mind-boggling. The plugin helps in 
// using Railo from the project that has this plugin defined. Although this plugin interacts
// with Railo, it does not have a directly dependency on it.
//
// The `railo-compiler` and `jetty-runner` are added as libraryDependencies to the project
// so code in those libraries can be accessed in the given project.

// https://github.com/sbt/sbt/issues/1448
// https://github.com/sbt/sbt-doge
// instead of `+command` you can do `fixed+ command`
commands += sbtdoge.Doge.crossBuildCommand("fixed+")

// Slow stop on run: https://issues.jboss.org/browse/RAILO-3231

organization := "nl.rhinofly"

//  The values in these keys are made available to the `railo-sbt` plugin using the 
//  buildinfo plugin. They build info only accepts certain types of values, that's 
//  the reason these are all strings or sequences of strings.
val railoResolver = settingKey[Seq[String]]("The railo resolver name and url")

val railoDependencyBase = settingKey[Seq[String]]("The railo dependency organization and artifact")

val runnerDependency = taskKey[Seq[String]]("The runner dependency organization, artifact and version")

val compilerDependency = taskKey[Seq[String]]("The compiler dependency organization, artifact and version")

val testUtilitiesDependency = taskKey[Seq[String]]("The test utilities dependency organization, artifact and version")

val jettyServerFactoryClassName = settingKey[String]("Name of the Jetty Server Factory implementation")

val railoCompilerClassName = settingKey[String]("Name of the Railo Compiler implementation")

val servletJspApiDependency = settingKey[Seq[String]]("The servlet jsp api dependency organization, artifact and version")

val railoDependencies = settingKey[Seq[ModuleID]]("All railo related dependencies")

railoResolver in Global := Seq("http://cfmlprojects.org/artifacts/", "http://cfmlprojects.org/artifacts/")

railoDependencyBase in Global := Seq("org.lucee", "lucee")

runnerDependency in Global := {
  val project = (projectID in `jetty-runner`).value
  Seq(project.organization, project.name, project.revision)
}

compilerDependency in Global := {
  val project = (projectID in `railo-compiler`).value
  Seq(project.organization, project.name, project.revision)
}

testUtilitiesDependency in Global := {
  val project = (projectID in `test-utilities`).value
  Seq(project.organization, project.name, project.revision)
}

jettyServerFactoryClassName in Global := "nl.rhinofly.jetty.runner.JettyServerFactory"

railoCompilerClassName in Global := "nl.rhinofly.railo.compiler.Compiler"

servletJspApiDependency in Global := Seq("javax.servlet.jsp", "javax.servlet.jsp-api", "2.2.1")

railoDependencies in Global  := {
  val railoDependency = {
    val Seq(organization, name) = railoDependencyBase.value
    organization % name
  }
  val actualServletJspApiDependency = {
    val Seq(organization, name, version) = servletJspApiDependency.value
    organization % name % version % "provided"
  }
  Seq(
    // matches all versions greater or equal to 4.5 and lower than 4.6
    railoDependency % "[4.5.0.000,4.6.0.000[" % "provided",
    servletApiDependency % "provided",
    actualServletJspApiDependency,
    elApiDependency
  )
}

val servletApiDependency = "javax.servlet" % "javax.servlet-api" % "3.1.0"

val elApiDependency = "javax.el" % "javax.el-api" % "2.2.1" % "provided"

val jettyDependency = "org.eclipse.jetty" % "jetty-webapp" % "9.2.3.v20140905"

// The root project does not contain code, it just exists to aggregate the other projects
lazy val root = project.in( file(".") )
  .aggregate(
    `railo-sbt`, 
    `jetty-runner`, 
    `railo-compiler`, 
    `runner-interface`, 
    `compiler-interface`,
    `test-utilities`
  )
  
//  This is the actual plugin. Note that it has no direct dependency on railo or jetty. It 
//  loads the actual implementation of `compiler-interface` from the project classpath. The
//  same happens for the actual implementation of `runner-interface`.
//  This is done so you can freely change the Railo version in your project without requiring
//  an update to the plugin. Controlling the classloaders used for compiling and running helps
//  us deal with the static (global) variables Railo uses.
lazy val `railo-sbt` = project.in( file("plugin") )
  .settings(buildInfoSettings: _*)
  .settings(
    name := "railo-sbt",
    organization := "nl.rhinofly",
    scalaVersion := "2.10.4",
    crossScalaVersions := Seq("2.10.4"),
    sbtPlugin := true,
    buildInfoPackage := "nl.rhinofly.build",
    buildInfoObject := "BuildInfo",
    buildInfoKeys := Seq[BuildInfoKey](
      railoResolver,
      railoDependencyBase,
      runnerDependency,
      compilerDependency,
      testUtilitiesDependency,
      jettyServerFactoryClassName,
      railoCompilerClassName,
      servletJspApiDependency
    ),
    sourceGenerators in Compile <+= buildInfo,
    libraryDependencies += Defaults.sbtPluginExtra(
      "com.typesafe.sbt" % "sbt-native-packager" % "0.8.0-M2", 
      (sbtBinaryVersion in update).value, 
      (scalaBinaryVersion in update).value
    )
  )
  .dependsOn(`compiler-interface`, `runner-interface`)

// TODO rename compiler-interface and railo-compiler to something more general
// it will also include getting access to railo stuff

val defaultSettings = Seq(
  scalaVersion := "2.11.5",
  crossScalaVersions := Seq("2.10.4", scalaVersion.value),
  organization := "nl.rhinofly"
)
  
//  This is the implementation of the `compiler-interface`. It does not make hard
//  assumptions about the railo version that is used.
lazy val `railo-compiler` = project.in( file("compiler") )
  .settings(defaultSettings: _*)
  .settings(
    name := "railo-compiler",
    resolvers += {
      val Seq(name, url) = railoResolver.value
      name at url
    },
    libraryDependencies ++= railoDependencies.value
  )
  .dependsOn(`compiler-interface`)

lazy val `compiler-interface` = project.in( file("compiler-interface") )
  .settings(defaultSettings: _*)
  .settings(
    name := "railo-compiler-interface"
  )
  .dependsOn(`runner-interface`)

//  This is the implementation of the `runner-interface`.
lazy val `jetty-runner` = project.in( file("runner") )
  .settings(defaultSettings: _*)
  .settings(
    name := "jetty-runner",
    libraryDependencies ++= Seq(
      jettyDependency,
      "org.fusesource.jansi" % "jansi" % "1.11",
      "jline" % "jline" % "2.11"
    )
  )
  .dependsOn(`runner-interface`)
  
lazy val `runner-interface` = project.in( file("runner-interface") )
  .settings(defaultSettings: _*)
  .settings(
    name := "jetty-runner-interface",
    libraryDependencies ++= Seq(
      servletApiDependency,
      jettyDependency % "provided"
    )
  )
  
lazy val `test-utilities` = project.in( file("test-utilities") )
  .settings(defaultSettings: _*)
  .settings(
    name := "railo-test-utilities",
    resolvers += {
      val Seq(name, url) = railoResolver.value
      name at url
    },
    libraryDependencies ++= railoDependencies.value
  )
  .dependsOn(`railo-compiler`)
