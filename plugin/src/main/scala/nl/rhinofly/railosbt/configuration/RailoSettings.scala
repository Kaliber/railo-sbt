package nl.rhinofly.railosbt.configuration

import sbt._
import sbt.Keys._
import nl.rhinofly.railosbt.RailoConfiguration
import nl.rhinofly.railosbt.ServletContainer
import sbt.ConfigKey.configurationToKey
import sbt.UpdateReport.richUpdateReport
import nl.rhinofly.railosbt.AutoImport
import sbt.classpath.ClasspathUtilities
import scala.reflect.runtime.universe
import java.util.UUID
import nl.rhinofly.railosbt.RailoServerSettings
import nl.rhinofly.jetty.runner.JettyServerFactoryInterface
import nl.rhinofly.railo.compiler.CompilerInterface
import nl.rhinofly.build.BuildInfo
import nl.rhinofly.jetty.runner.JettyServerInterface
import scala.tools.nsc.io.Jar

object RailoSettings {

  lazy val defaultPassword = UUID.randomUUID.toString

  import AutoImport._
  import RailoKeys._

  lazy val all =
    defaults ++
      directorySettings ++
      serverConfigurationSettings ++
      webConfigurationSettings ++
      webXmlSettings ++
      runSettings ++
      compileSettings ++
      packageSettings

  lazy val defaults = Seq(
    version := "4.3.0.001",
    hashPassword := { clearTextPassword =>
      val projectDependencies = (dependencyClasspath in Compile).value
      val classLoaderWithRailo = ClasspathUtilities.toLoader(projectDependencies.files)

      RailoConfiguration.hashPassword(classLoaderWithRailo, clearTextPassword)
    }
  )

  lazy val directorySettings = Seq(
    target := target.value / Railo.name,
    classDirectory := target.value / "classes",
    fullClasspath := (fullClasspath in Compile).value ++ managedClasspath.value,
    copyResources := (copyResources in Compile).value,
    sourceDirectory := (railoSource in Compile).value,
    managedClasspath := jarsInRailoConfiguration(update.value),
    unmanagedSourceDirectories := Seq(sourceDirectory.value),
    unmanagedSources <<= Defaults.collectFiles(unmanagedSourceDirectories, includeFilter in unmanagedSources, excludeFilter in unmanagedSources),
    includeFilter in unmanagedSources := "*.cfc"
  )

  lazy val webXmlSettings = Seq(
    content in webXml := {
      val webConfigurationDirectory = webConfiguration.value.getAbsolutePath
      val serverConfigurationDirectory = serverConfiguration.value.getAbsolutePath
      ServletContainer.webXml(webConfigurationDirectory, serverConfigurationDirectory)
    },

    target in webXml := target.value / "web.xml",

    webXml := {
      val file = (target in webXml).value
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
      val hashedPassword = hashPassword.value(clearTextPassword)
      val settings = RailoServerSettings(libraryMappings.value)

      RailoConfiguration.webConfiguration(hashedPassword, settings)
    },

    webConfiguration := {
      val contents = (content in webConfiguration).value
      val directory = (target in webConfiguration).value
      val file = directory / "railo-web.xml.cfm"
      IO.write(file, contents)
      directory
    },

    libraryMappings := railoMappingsInConfiguration(update.value)
  )

  lazy val serverConfigurationSettings = Seq(
    password in serverConfiguration := defaultPassword,

    target in serverConfiguration := target.value / "server",

    content in serverConfiguration := {
      val clearTextPassword = (password in serverConfiguration).value
      val hashedPassword = hashPassword.value(clearTextPassword)
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
          val classpath = fullClasspath.value.files
          val javaRunner = (runner in run).value

          val arguments = Seq(tcpPort, webAppDirectory, webXmlFile, "stopOnKey")

          toError(javaRunner.run(main, classpath, arguments, streams.value.log))
        }
      )

  lazy val compileSettings = Seq(
    port in compile := 9191,

    compile := {
      val serverPassword = (password in serverConfiguration).value
      val sourceDir = sourceDirectory.value
      val classpath = fullClasspath.value
      val targetDir = classDirectory.value
      val serverPort = (port in compile).value
      val webXmlFile = webXml.value

      compileTask(classpath, serverPassword, sourceDir, targetDir, serverPort, webXmlFile)
    },

    products <<= Classpaths.makeProducts
  )

  lazy val packageSettings = Seq(
    packageOptions :=
      Package.addSpecManifestAttributes(name.value, version.value, organizationName.value) +:
      Package.ManifestAttributes(
        MAPPING_TYPE -> "cfc",
        MAPPING_NAME -> (name in Railo).value
      ) +: packageOptions.value
  )

  def jarsInRailoConfiguration(updateReport: UpdateReport) = {
    val filter = configurationFilter(Railo.name) && artifactFilter(classifier = "")
    updateReport.filter(filter).toSeq.map {
      case (config, module, art, file) =>
        Attributed(file)(AttributeMap.empty
          .put(artifact.key, art)
          .put(moduleID.key, module)
          .put(configuration.key, Railo)
        )
    }.distinct
  }

  def railoMappingsInConfiguration(updateReport: UpdateReport) = {
    val filter = configurationFilter(Railo.name) && artifactFilter(classifier = Railo.name)
    updateReport.filter(filter).toSeq.map {
      case (configuration, moduleId, artifact, file) =>

        val mappingName =
          for {
            manifest <- new Jar(file).manifest
            mappingName <- Option(manifest.getMainAttributes.getValue(MAPPING_NAME))
          } yield mappingName

        RailoServerSettings.Mapping(mappingName.getOrElse(artifact.name), file.getAbsolutePath)
    }
  }

  def compileTask(classpath: Classpath, serverPassword: String, sourceDir: File, targetDir: File, port: Int, webXmlFile: File) = {
    val classLoader = ClasspathUtilities.toLoader(classpath.files, getClass.getClassLoader)
    val mirror = universe.runtimeMirror(classLoader)

    val jettyServer = createJettyServer(mirror, port, sourceDir, webXmlFile)
    val result = compileWithRailo(mirror, jettyServer, serverPassword, sourceDir)

    IO.copyDirectory(result, targetDir, overwrite = true)
    inc.Analysis.Empty
  }

  def createJettyServer(mirror: universe.Mirror, port: Int, resourceBase: File, webXmlFile: File) = {
    val obj = mirror.getObject[JettyServerFactoryInterface](BuildInfo.jettyServerFactoryClassName)
    obj.newServer(port, resourceBase, webXmlFile)
  }

  def compileWithRailo(mirror: universe.Mirror, jettyServer: JettyServerInterface, password: String, sourceDir: File) = {
    val obj = mirror.getObject[CompilerInterface](BuildInfo.railoCompilerClassName)
    obj.compile(jettyServer, password, sourceDir, ServletContainer.SERVLET_NAME)
  }

  implicit class MirrorEnhancement(mirror: universe.Mirror) {
    def getObject[T](name: String): T = {
      val module = mirror.staticModule(name)
      mirror.reflectModule(module).instance.asInstanceOf[T]
    }
  }

  val MAPPING_NAME = "mapping-name"
  val MAPPING_TYPE = "mapping-type"
}