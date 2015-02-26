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

  lazy val versionSettings = Seq(
    version := "4.5.0.042"
  )

  lazy val passwordSettings = Seq(
    salt := createSaltTask.value,
    hashPassword := { (clearTextPassword, salt) =>
      val projectDependencies = (dependencyClasspath in Compile).value
      RailoConfiguration.hashPassword(classLoaderWithRailo(projectDependencies), clearTextPassword, salt)
    },
    password in webConfiguration := defaultPassword,
    password in serverConfiguration := defaultPassword
  )

  lazy val directorySettings = Seq(
    target := target.value / Railo.name,
    managedClasspath := jarsInRailoConfiguration(update.value),
    fullClasspath := (fullClasspath in Compile).value,

    // These are primarily used by watchSources
    unmanagedSourceDirectories := Seq((railoSource in Test).value, (railoSource in Compile).value),
    unmanagedSources <<= Defaults.collectFiles(unmanagedSourceDirectories, includeFilter in unmanagedSources, excludeFilter in unmanagedSources),
    includeFilter in unmanagedSources := "*.cfc" | "*.cfm"
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
          val webAppDirectory = (railoSource in Compile).value.getAbsolutePath
          val webXmlFile = (webXml in Compile).value.getAbsolutePath
          val main = (mainClass in run).value.getOrElse("No main class found")
          val classpath = fullClasspath.value.files
          val javaRunner = (runner in run).value

          val arguments = Seq(tcpPort, webAppDirectory, webXmlFile, "stopOnKey")

          toError(javaRunner.run(main, classpath, arguments, streams.value.log))
        }
      )

  lazy val compileSettings = Seq(
    port in compile := 9191,

    sourceDirectory := (railoSource in Compile).value,

    classDirectory := target.value / "classes",

    compile := {
      val serverPassword = (password in serverConfiguration in Compile).value
      val sourceDir = sourceDirectory.value
      val classpath = fullClasspath.value
      val targetDir = classDirectory.value
      val serverPort = (port in compile).value
      val webXmlFile = (webXml in Compile).value
      val logger = streams.value.log

      compileTask(classpath, serverPassword, sourceDir, targetDir, serverPort, webXmlFile, logger)
    },

    products := {
      val compiled = compile.value
      classDirectory.value :: Nil
    }
  )

  lazy val packageRailoSettings = Seq(
    packageOptions :=
      Package.addSpecManifestAttributes(name.value, version.value, organizationName.value) +:
      Package.ManifestAttributes(
        MAPPING_TYPE -> "cfc",
        MAPPING_NAME -> (name in Railo).value
      ) +: packageOptions.value
  )

  lazy val nativePackagerSettings = {
    import Keys.`package`
    Seq(
      port in `package` := (port in run).value,
      mode in `package` := RailoRunMode.Join
    )
  }

  def createSaltTask = {
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