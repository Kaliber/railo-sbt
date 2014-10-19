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
import scala.reflect.runtime.universe.Mirror
import nl.rhinofly.railo.compiler.CompilerInterface
import nl.rhinofly.jetty.runner.JettyServerInterface
import nl.rhinofly.build.BuildInfo

object RailoPlugin extends AutoPlugin {

  import Import.Railo
  import Import.RailoKeys._

  // ensures correct ordering, otherwise settings might get overridden
  override val requires = JvmPlugin

  val autoImport = Import

  override lazy val projectConfigurations =
    super.projectConfigurations :+ Railo

  override def projectSettings =
    super.projectSettings ++
      compileOnlySettings ++
      inConfig(Compile)(Seq(
        railoSource := sourceDirectory.value / Railo.name
      )) ++
      railoProjectSettings ++
      inConfig(Railo)(baseRailoSettings) ++
      inConfig(Compile)(packageSettings) ++
      publishSettings

  val compileOnly = config("compileOnly").hide

  val MAPPING_NAME = "mapping-name"
  val MAPPING_TYPE = "mapping-type"

  lazy val defaultPassword = UUID.randomUUID.toString

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

  def servletJspApiDependency = {
    val Seq(organization, name, version) = BuildInfo.servletJspApiDependency
    organization % name % version
  }

  val RailoClassifier = Railo.name

  implicit class ModuleIDEnhancements(moduleID: ModuleID) {
    def railo() =
      moduleID.artifacts(Artifact.classified(moduleID.name, RailoClassifier))

    def withRailo() = jarIfEmpty.railo()

    private def jarIfEmpty = if (moduleID.explicitArtifacts.isEmpty) moduleID.jar() else moduleID
  }

  lazy val railoProjectSettings = Seq(
    resolvers += "http://cfmlprojects.org/artifacts/" at "http://cfmlprojects.org/artifacts/",
    version in Railo := "4.3.0.001",
    libraryDependencies ++= Seq(
      railoDependency((version in Railo).value) % compileOnly,
      runnerDependency % compileOnly,
      compilerDependency % compileOnly,
      // railo does not have this listed as dependency while it 
      // depends on it
      servletJspApiDependency % compileOnly
    ),
    moduleSettings := addRailoArtifacts(moduleSettings.value)
  )

  def addRailoArtifacts(moduleSettings: ModuleSettings): ModuleSettings =
    moduleSettings match {
      case ec: InlineConfiguration if ec.configurations contains Railo =>
        ec.copy(
          dependencies = ec.dependencies.map(addRailoDependency),
          overrides = ec.overrides.map(addRailoDependency))
      case unknown => unknown
    }

  def addRailoDependency(m: ModuleID): ModuleID =
    if (m.configurations exists (_ contains Railo.name)) m.withRailo()
    else m

  lazy val compileOnlySettings = Seq(
    ivyConfigurations += compileOnly,
    unmanagedClasspath in Compile ++= update.value.select(configurationFilter(compileOnly.name))
  )

  lazy val baseRailoSettings =
    directorySettings ++
      serverConfigurationSettings ++
      webConfigurationSettings ++
      webXmlSettings ++
      runSettings ++
      compileSettings

  lazy val directorySettings = Seq(
    target := target.value / Railo.name,

    classDirectory := target.value / "classes",

    fullClasspath := (fullClasspath in Compile).value ++ managedClasspath.value,

    copyResources := (copyResources in Compile).value,

    sourceDirectory := (railoSource in Compile).value,

    managedClasspath := {
      val filter = configurationFilter(Railo.name) && artifactFilter(classifier = "")
      update.value.filter(filter).toSeq.map {
        case (config, module, art, file) =>
          Attributed(file)(AttributeMap.empty
            .put(artifact.key, art)
            .put(moduleID.key, module)
            .put(configuration.key, Railo)
          )
      }.distinct
    },

    unmanagedSourceDirectories := Seq(sourceDirectory.value),
    unmanagedSources <<= Defaults.collectFiles(unmanagedSourceDirectories, includeFilter in unmanagedSources, excludeFilter in unmanagedSources),
    includeFilter in unmanagedSources := "*.cfc"

  ) ++ Defaults.resourceConfigPaths

  lazy val webXmlSettings = Seq(
    content in webXml := {
      val webConfigurationDirectory = webConfiguration.value.getAbsolutePath
      val serverConfigurationDirectory = serverConfiguration.value.getAbsolutePath
      RailoServer.webXml(webConfigurationDirectory, serverConfigurationDirectory)
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

    libraryMappings := {
      val filter = configurationFilter(Railo.name) && artifactFilter(classifier = RailoClassifier)
      update.value.filter(filter).toSeq.map {
        case (configuration, moduleId, artifact, file) =>

          val defaultMappingName =
            new Jar(file).manifest.flatMap(m => Option(m.getMainAttributes.getValue(MAPPING_NAME)))

          val cleanMappingName = defaultMappingName.getOrElse(artifact.name)

          Mapping(cleanMappingName, file.getAbsolutePath)
      }
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
          //val resourcesCopied = copyResources.value
          //val classpath = classDirectory.value +: dependencyClasspath.value.files
          val classpath = fullClasspath.value.files

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
      val classpath = fullClasspath.value.files

      val classLoader = ClasspathUtilities.toLoader(classpath, getClass.getClassLoader)
      val mirror = universe.runtimeMirror(classLoader)
      val jettyServer = createJettyServer(mirror, port.value, sourceDirectory.value, webXml.value)
      val result = compileWithRailo(mirror, jettyServer, serverPassword, sourceDir)
      IO.copyDirectory(result, classDirectory.value, overwrite = true)
      inc.Analysis.Empty
    },

    products <<= Classpaths.makeProducts
  )

  lazy val packageSettings =
    Defaults.packageTaskSettings(packageRailo, packageRailoMappings) ++
      Seq(
        watchSources in Global <++= unmanagedSources in Railo
      ) ++
        inTask(packageRailo)(Seq(
          artifactClassifier := Some(RailoClassifier),

          unmanagedSourceDirectories := (unmanagedSourceDirectories in Railo).value,
          unmanagedSources <<= unmanagedSources in Railo,

          products := (products in Railo).value,

          packageOptions :=
            Package.addSpecManifestAttributes(name.value, version.value, organizationName.value) +:
            Package.ManifestAttributes(
              MAPPING_TYPE -> "cfc",
              MAPPING_NAME -> (name in Railo).value
            ) +: packageOptions.value
        ))

  def packageRailoMappings =
    Defaults.concatMappings(
      products.map(_ flatMap Path.allSubpaths),
      Defaults.sourceMappings
    )

  lazy val artifactTasks = Seq(packageRailo in Compile)

  lazy val publishSettings = Seq(
    artifacts <++= Classpaths.artifactDefs(artifactTasks),
    packagedArtifacts <++= Classpaths.packaged(artifactTasks)
  )

  def createJettyServer(mirror: Mirror, port: Int, resourceBase: File, webXmlFile: File) = {
    val obj = mirror.getObject[JettyServerFactoryInterface](BuildInfo.jettyServerFactoryClassName)
    obj.newServer(port, resourceBase, webXmlFile)
  }

  def compileWithRailo(mirror: Mirror, jettyServer: JettyServerInterface, password: String, sourceDir: File) = {
    val obj = mirror.getObject[CompilerInterface](BuildInfo.railoCompilerClassName)
    obj.compile(jettyServer, password, sourceDir, RailoServer.SERVLET_NAME)
  }

  implicit class MirrorEnhancement(mirror: Mirror) {
    def getObject[T](name: String): T = {
      val module = mirror.staticModule(name)
      mirror.reflectModule(module).instance.asInstanceOf[T]
    }
  }
}