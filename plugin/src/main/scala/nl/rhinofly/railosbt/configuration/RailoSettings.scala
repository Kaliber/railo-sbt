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
import nl.rhinofly.railosbt.Constants._
import java.util.UUID
import nl.rhinofly.railo.compiler.{ Logger => CompilerLogger }

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
      packageRailoSettings ++
      packageSettings

  def createSalt = {
    var salt: Option[String] = None
    Def.task {
      salt match {
        case Some(salt) => salt
        case None =>
          val projectDependencies = (dependencyClasspath in Compile).value
          val newSalt = RailoConfiguration.salt(classLoaderWithRailo(projectDependencies))
          salt = Some(newSalt)
          newSalt
      }
    }
  }

  lazy val defaults = Seq(
    version := "4.3.0.001",
    salt := createSalt.value,
    hashPassword := { (clearTextPassword, salt) =>
      val projectDependencies = (dependencyClasspath in Compile).value
      RailoConfiguration.hashPassword(classLoaderWithRailo(projectDependencies), clearTextPassword, salt)
    }
  )

  lazy val directorySettings = Seq(
    target := target.value / Railo.name,
    classDirectory := target.value / "classes",
    copyResources := (copyResources in Compile).value,
    sourceDirectory := (railoSource in Compile).value,
    managedClasspath := jarsInRailoConfiguration(update.value),
    fullClasspath := (fullClasspath in Compile).value,
    unmanagedSourceDirectories := Seq(sourceDirectory.value),
    unmanagedSources <<= Defaults.collectFiles(unmanagedSourceDirectories, includeFilter in unmanagedSources, excludeFilter in unmanagedSources),
    includeFilter in unmanagedSources := "*.cfc"
  )

  lazy val webXmlSettings = Seq(
    content in webXml := {
      serverConfiguration.value
      webConfiguration.value
      val webConfigurationDirectory = (target in webConfiguration).value.getAbsolutePath
      val serverConfigurationDirectory = (target in serverConfiguration).value.getAbsolutePath

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
      val passwordSalt = (salt in webConfiguration).value
      val hashedPassword = hashPassword.value(clearTextPassword, passwordSalt)
      val settings = RailoServerSettings(libraryMappings.value)

      RailoConfiguration.webConfiguration(hashedPassword, passwordSalt, settings)
    },

    artifactPath in webConfiguration :=
      (target in webConfiguration).value / "lucee-web.xml.cfm",

    webConfiguration := {
      val contents = (content in webConfiguration).value
      val file = (artifactPath in webConfiguration).value
      IO.write(file, contents)
    },

    target in libraryMappings := (target in webConfiguration).value / "mappingArchives",

    libraryMappings := railoMappingsInConfiguration(
      updateReport = update.value,
      base = (target in webConfiguration).value,
      target = (target in libraryMappings).value
    )
  )

  lazy val serverConfigurationSettings = Seq(
    password in serverConfiguration := defaultPassword,

    target in serverConfiguration := target.value / "server",

    content in serverConfiguration := {
      val clearTextPassword = (password in serverConfiguration).value
      val passwordSalt = (salt in serverConfiguration).value
      val hashedPassword = hashPassword.value(clearTextPassword, passwordSalt)
      RailoConfiguration.serverConfiguration(hashedPassword, passwordSalt)
    },

    artifactPath in serverConfiguration :=
      (target in serverConfiguration).value / "railo-server" / "context" / "railo-server.xml",

    serverConfiguration := {
      val contents = (content in serverConfiguration).value
      val file = (artifactPath in serverConfiguration).value
      IO.write(file, contents)
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
      val logger = streams.value.log

      compileTask(classpath, serverPassword, sourceDir, targetDir, serverPort, webXmlFile, logger)
    },

    products <<= Classpaths.makeProducts
  )

  lazy val packageRailoSettings = Seq(
    packageOptions :=
      Package.addSpecManifestAttributes(name.value, version.value, organizationName.value) +:
      Package.ManifestAttributes(
        MAPPING_TYPE -> "cfc",
        MAPPING_NAME -> (name in Railo).value
      ) +: packageOptions.value
  )

  lazy val packageSettings = {
    import Keys.`package`
    Seq(
      port in `package` := (port in run).value,
      mode in `package` := RailoRunMode.Join
    )
  }

  def classLoaderWithRailo(projectDependencies: Keys.Classpath) = {
    val classLoaderWithRailo = ClasspathUtilities.toLoader(projectDependencies.files)
    classLoaderWithRailo
  }

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

  def railoMappingsInConfiguration(updateReport: UpdateReport, base: File, target: File) = {
    val filter = configurationFilter(Railo.name) && artifactFilter(classifier = Railo.name)
    updateReport.filter(filter).toSeq.map {
      case (configuration, moduleId, artifact, file) =>

        val mappingName =
          for {
            manifest <- new Jar(file).manifest
            mappingName <- Option(manifest.getMainAttributes.getValue(MAPPING_NAME))
          } yield mappingName

        val targetFile = target / file.getName
        IO.copyFile(file, targetFile)
        val relativeFileName = IO.relativize(base, targetFile).getOrElse(sys.error(s"Programming error, $targetFile is not relative to $target"))

        RailoServerSettings.Mapping(
          mappingName.getOrElse(artifact.name),
          "{railo-web}/" + relativeFileName
        )
    }
  }

  def compileTask(classpath: Classpath, serverPassword: String, sourceDir: File, targetDir: File, port: Int, webXmlFile: File, logger: Logger) = {
    val classLoader = ClasspathUtilities.toLoader(classpath.files, getClass.getClassLoader)
    val mirror = universe.runtimeMirror(classLoader)

    val jettyServer = createJettyServer(mirror, port, sourceDir, webXmlFile)
    val result = compileWithRailo(mirror, jettyServer, serverPassword, sourceDir, logger)

    val compiledDirectory = result.get // It's ok when this throws an error, sbt will pick it up
    IO.copyDirectory(compiledDirectory, targetDir, overwrite = true)
    inc.Analysis.Empty
  }

  def createJettyServer(mirror: universe.Mirror, port: Int, resourceBase: File, webXmlFile: File) = {
    val obj = mirror.getObject[JettyServerFactoryInterface](BuildInfo.jettyServerFactoryClassName)
    obj.newServer(port, resourceBase, webXmlFile)
  }

  def compileWithRailo(mirror: universe.Mirror, jettyServer: JettyServerInterface, password: String, sourceDir: File, logger: Logger) = {
    val obj = mirror.getObject[CompilerInterface](BuildInfo.railoCompilerClassName)
    val compilerLogger = new CompilerLogger {
      def info(message: String) = logger.info(message)
      def debug(message: String) = logger.debug(message)
    }
    obj.compile(jettyServer, password, sourceDir, ServletContainer.SERVLET_NAME, compilerLogger)
  }

  implicit class MirrorEnhancement(mirror: universe.Mirror) {
    def getObject[T](name: String): T = {
      val module = mirror.staticModule(name)
      mirror.reflectModule(module).instance.asInstanceOf[T]
    }
  }
}