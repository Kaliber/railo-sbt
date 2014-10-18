package nl.rhinofly.railosbt

import java.util.UUID
import scala.Option.option2Iterable
import scala.tools.nsc.io.Jar
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import java.net.URLClassLoader
import sbt.classpath.ClasspathUtilities
import scala.reflect.runtime.universe
import nl.rhinofly.jetty.runner.JettyServerFactoryInterface
import scala.reflect.api.Mirror
import nl.rhinofly.railo.compiler.CompilerInterface
import nl.rhinofly.jetty.runner.JettyServerInterface
import nl.rhinofly.build.BuildInfo

object RailoPlugin extends AutoPlugin {

  import Import.Railo

  val autoImport = Import

  // ensures correct ordering, we override certain settings of the JvmPlugin
  override val requires = JvmPlugin

  override lazy val projectConfigurations =
    super.projectConfigurations :+ Railo

  override def projectSettings =
    super.projectSettings ++
      railoProjectSettings ++
      inConfig(Compile)(railoCompileSettings)

  import Import.RailoKeys._

  val MAPPING_NAME = "mapping-name"
  val MAPPING_TYPE = "mapping-type"

  lazy val defaultPassword = UUID.randomUUID.toString

  val compileOnly = config("compileOnly").hide

  def railoDependency(version: String) = {
    val Seq(organization, name) = BuildInfo.railoDependencyBase
    organization % name % version
  }

  def runnerDependency = {
    val Seq(organization, name, version) = BuildInfo.runnerDependency
    organization %% name % version
  }

  def compilerDependency = {
    val Seq(organization, name, version) = BuildInfo.compilerDependency
    organization %% name % version
  }

  lazy val railoProjectSettings = Seq(
    resolvers += "http://cfmlprojects.org/artifacts/" at "http://cfmlprojects.org/artifacts/",
    version in Railo := "4.3.0.001",
    ivyConfigurations += compileOnly,
    libraryDependencies ++= Seq(
      railoDependency((version in Railo).value) % compileOnly,
      runnerDependency % compileOnly,
      compilerDependency % compileOnly,
      "com.typesafe" % "config" % "1.2.1" % compileOnly
    ),
    unmanagedClasspath in Compile ++= update.value.select(configurationFilter(compileOnly.name)),
    autoScalaLibrary := false,
    crossPaths := false
  )

  lazy val railoCompileSettings =
    directorySettings ++
      serverConfigurationSettings ++
      webConfigurationSettings ++
      webXmlSettings ++
      runSettings ++
      compileSettings ++
      packageSettings

  lazy val directorySettings = Seq(
    sourceDirectory := baseDirectory.value / "src",
    resourceDirectory := baseDirectory.value / "conf",

    unmanagedResourceDirectories := Seq(resourceDirectory.value),
    unmanagedSourceDirectories := Seq(sourceDirectory.value),

    target := target.value / Railo.name,

    classDirectory := target.value / "classes"
  )

  lazy val webXmlSettings = Seq(
    content in webXml := {
      val webConfigurationDirectory = webConfiguration.value.getAbsolutePath
      val serverConfigurationDirectory = serverConfiguration.value.getAbsolutePath
      RailoServer.webXml(webConfigurationDirectory, serverConfigurationDirectory)
    },

    webXml := {
      val file = target.value / "web.xml"
      val contents = (content in webXml).value
      IO.write(file, contents)
      file
    }
  )

  lazy val webConfigurationSettings = Seq(
    password in webConfiguration := defaultPassword,

    target in webConfiguration := target.value / "web",

    content in webConfiguration := {
      val clearTextPassword = (password in webConfiguration).value
      val cl = ClasspathUtilities.toLoader((dependencyClasspath in Compile).value.files)
      val hashedPassword = RailoConfiguration.hashPassword(cl, clearTextPassword)
      val settings = RailoSettings(libraryMappings.value)
      RailoConfiguration.webConfiguration(hashedPassword, settings)
    },

    webConfiguration := {
      val contents = (content in webConfiguration).value
      val directory = (target in webConfiguration).value
      val file = directory / "railo-web.xml.cfm"
      IO.write(file, contents)
      directory
    },

    libraryMappings :=
      update.value.toSeq.collect {
        case (configuration, moduleId, artifact, file) if configuration == Railo.name =>
          val defaultMappingName =
            new Jar(file).manifest.flatMap(m => Option(m.getMainAttributes.getValue(MAPPING_NAME)))

          val cleanMappingName = defaultMappingName.getOrElse(artifact.name)

          Mapping(cleanMappingName, file.getAbsolutePath)
      }
  )

  lazy val serverConfigurationSettings = Seq(
    password in serverConfiguration := defaultPassword,

    target in serverConfiguration := target.value / "server",

    content in serverConfiguration := {
      val clearTextPassword = (password in serverConfiguration).value
      val cl = ClasspathUtilities.toLoader((dependencyClasspath in Compile).value.files)
      val hashedPassword = RailoConfiguration.hashPassword(cl, clearTextPassword)
      RailoConfiguration.serverConfiguration(hashedPassword)
    },

    serverConfiguration := {
      val contents = (content in serverConfiguration).value
      val directory = (target in serverConfiguration).value
      val file = directory / "railo-server" / "context" / "railo-server.xml"
      IO.write(file, contents)
      directory
    }
  )

  lazy val runSettings =
    inTask(run)(
      Seq(
        port := 8181,
        fork := true,
        mainClass := Some("nl.rhinofly.jetty.runner.Main"),
        connectInput := true,
        outputStrategy := Some(StdoutOutput),
        javaOptions += "-Djansi.passthrough=true"
      )
    ) ++ Seq(
        run := {
          val tcpPort = (port in run).value.toString
          val webAppDirectory = sourceDirectory.value.getAbsolutePath
          val webXmlFile = webXml.value.getAbsolutePath

          val main = (mainClass in run).value.getOrElse("No main class found")
          val resourcesCopied = copyResources.value
          val classpath = classDirectory.value +: dependencyClasspath.value.files

          val arguments = Seq(tcpPort, webAppDirectory, webXmlFile, "stopOnKey")

          val javaRunner = (runner in run).value

          toError(javaRunner.run(main, classpath, arguments, streams.value.log))
        }
      )

  lazy val compileSettings = Seq(
    // https://github.com/sbt/sbt/issues/1670
    discoveredSbtPlugins := PluginDiscovery.emptyDiscoveredNames,

    port := 9191,

    compile := {
      val serverPassword = (password in serverConfiguration).value
      val sourceDir = sourceDirectory.value
      val classpath = externalDependencyClasspath.value.files

      val classLoader = ClasspathUtilities.toLoader(classpath, getClass.getClassLoader)

      classLoader.loadClass("nl.rhinofly.jetty.runner.JettyServerFactory$")
      
      val jettyServer = createJettyServer(classLoader, port.value, sourceDirectory.value, webXml.value)
      val result = compileWithRailo(classLoader, jettyServer, serverPassword, sourceDir)
      IO.copyDirectory(result, classDirectory.value, overwrite = true)
      inc.Analysis.Empty
    }
  )

  lazy val packageSettings = Seq(
    mappings in packageBin ++= {
      val classDir = classDirectory.value
      val classes = classDir.***
      val classMappings = (classes --- classDir) pair (relativeTo(classDir) | flat)

      val sourceDir = sourceDirectory.value
      val sources = sourceDir.***
      val sourceMappings = (sources --- sourceDir) pair (relativeTo(sourceDir) | flat)

      sourceMappings.toSeq ++ classMappings.toSeq
    },

    packageOptions in packageBin +=
      Package.ManifestAttributes(
        MAPPING_TYPE -> "cfc",
        MAPPING_NAME -> (name in Railo).value
      )
  )

  def createJettyServer(classLoader: ClassLoader, port: Int, resourceBase: File, webXmlFile: File) = {
    val mirror = universe.runtimeMirror(classLoader)
    // get from configuration
    val module = mirror.staticModule("nl.rhinofly.jetty.runner.JettyServerFactory")

    val obj = mirror.reflectModule(module).instance.asInstanceOf[JettyServerFactoryInterface]

    obj.newServer(port, resourceBase, webXmlFile)
  }

  def compileWithRailo(classLoader: ClassLoader, jettyServer: JettyServerInterface, password: String, sourceDir: File) = {
    val mirror = universe.runtimeMirror(classLoader)
    // get from configuration
    val module = mirror.staticModule("nl.rhinofly.railo.compiler.Compiler")

    val obj = mirror.reflectModule(module).instance.asInstanceOf[CompilerInterface]

    obj.compile(jettyServer, password, sourceDir, RailoServer.SERVLET_NAME)
  }
}