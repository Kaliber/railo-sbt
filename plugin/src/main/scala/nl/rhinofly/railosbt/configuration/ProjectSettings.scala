package nl.rhinofly.railosbt.configuration

import sbt._
import sbt.Keys._
import nl.rhinofly.railosbt.AutoImport
import nl.rhinofly.build.BuildInfo
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.SbtNativePackager.NativePackagerKeys._
import nl.rhinofly.railosbt.Constants._
import nl.rhinofly.railosbt.ServletContainer

object ProjectSettings {

  // A configuration that can be used for dependencies that should not be 
  // added to the Maven or Ivy description (when using the library)
  val compileAndRuntimeAndTestOnly = config("compileAndRuntimeAndTestOnly").hide

  import AutoImport._
  import RailoKeys._

  lazy val nonPluginProjectSettings = Seq(
    // https://github.com/sbt/sbt/issues/1670
    discoveredSbtPlugins := PluginDiscovery.emptyDiscoveredNames
  )

  lazy val directorySettings = Seq[Setting[_]](
    watchSources <++= unmanagedSources in Railo,
    addRailoManagedClasspathTo(Compile),
    addRailoManagedClasspathTo(Runtime)
  )

  lazy val dependencySettings = Seq(
    resolvers += RAILO_RESOLVER,
    libraryDependencies ++= {
      import BuildInfo._
      val railoDependency = railoDependencyBase :+ (version in Railo).value
      Seq(
        scalaDependency(runnerDependency) % compileAndRuntimeAndTestOnly,
        scalaDependency(compilerDependency) % compileAndRuntimeAndTestOnly,
        scalaDependency(testUtilitiesDependency) % compileAndRuntimeAndTestOnly,
        javaDependency(railoDependency) % compileAndRuntimeAndTestOnly,
        // railo does not have this listed as dependency while it depends on it
        javaDependency(servletJspApiDependency) % compileAndRuntimeAndTestOnly
      )
    },
    ivyConfigurations += compileAndRuntimeAndTestOnly,
    unmanagedClasspath in Compile ++= update.value.select(configurationFilter(compileAndRuntimeAndTestOnly.name)),
    unmanagedClasspath in Runtime ++= (unmanagedClasspath in Compile).value,
    unmanagedClasspath in Test ++= (unmanagedClasspath in Compile).value,
    moduleSettings := addRailoArtifacts(moduleSettings.value, streams.value.log)
  )

  lazy val publishSettings = Seq(
    artifacts <++= Classpaths.artifactDefs(packageArtifactTask),
    packagedArtifacts <++= Classpaths.packaged(packageArtifactTask)
  )

  lazy val nativePackagerSettings =
    packageArchetype.java_application ++ Seq(

      bashScriptExtraDefines ++= {
        import Keys.`package`

        val webXmlLocation = IO.relativize(
          base = target.value,
          file = (webXml in Compile).value
        ).getOrElse(sys.error("Expected web.xml to be in the target directory"))

        val serverPort = (port in `package` in Railo).value
        val src = s"../$WEB_APP_DIR_NAME/src"
        val webXmlFile = s"../$WEB_APP_DIR_NAME/$webXmlLocation"
        val runMode = (mode in `package` in Railo).value

        Seq(
          s"addApp $serverPort",
          s"addApp $src",
          s"addApp $webXmlFile",
          s"addApp $runMode"
        )
      },

      mappings in Universal ++= {
        def targetMapping(file: File) = webAppDirTargetMapping(target.value, file)

        sourceMappings((sourceDirectory in Railo).value) ++
          targetMapping((webXml in Compile).value) ++
          targetMapping((artifactPath in webConfiguration in Compile).value) ++
          targetMapping((artifactPath in serverConfiguration in Compile).value) ++
          railoLibraryMappings(target.value, (target in libraryMappings in Compile).value)
      }
    )

  val relativeToWebAppDir: Option[String] => Option[String] = _.map(WEB_APP_DIR_NAME + "/" + _)

  def sourceMappings(sourceDir: File) = {

    val relativeToSrc: Option[String] => Option[String] = _.map("src/" + _)

    val relativize = relativeTo(sourceDir) andThen relativeToSrc andThen relativeToWebAppDir

    (sourceDir.*** --- sourceDir) pair relativize
  }

  def railoLibraryMappings(targetDir: File, mappingsDir: File) = {
    val relativize = relativeTo(targetDir) andThen relativeToWebAppDir

    mappingsDir.*** pair relativize
  }

  def webAppDirTargetMapping(base: File, file: File) =
    relativeToWebAppDir(IO.relativize(base, file)).map(file -> _).toSeq

  lazy val packageArtifactTask = Seq(packageRailo in Compile)

  def scalaDependency(parts: Seq[String]) = {
    val Seq(organization, name, version) = parts
    organization %% name % version
  }

  def javaDependency(parts: Seq[String]) = {
    val Seq(organization, name, version) = parts
    organization % name % version
  }

  implicit class ModuleIDEnhancements(moduleID: ModuleID) {
    def railo() =
      moduleID.artifacts(Artifact.classified(moduleID.name, Railo.name))

    def withRailo() = jarIfEmpty.railo()

    private def jarIfEmpty = if (moduleID.explicitArtifacts.isEmpty) moduleID.jar() else moduleID
  }

  def addRailoArtifacts(moduleSettings: ModuleSettings, logger:Logger): ModuleSettings =
    moduleSettings match {
      case ec: InlineConfiguration if ec.configurations contains Railo =>
        ec.copy(
          dependencies = ec.dependencies.map(addRailoArtifact),
          overrides = ec.overrides.map(addRailoArtifact))
      case unknown => 
        logger.warn("Unknown configuration type: ${unknown.getClass.getName}")
        logger.warn(unknown.toString)
        unknown
    }

  def addRailoArtifact(m: ModuleID): ModuleID =
    if (m.configurations exists (_ contains Railo.name)) m.withRailo()
    else m

  def addRailoManagedClasspathTo(configuration: Configuration) =
    managedClasspath in configuration := {
      val oldClasspath = (managedClasspath in configuration).value
      val railoClasspath = (managedClasspath in Railo).value
      val newClasspath = oldClasspath ++ railoClasspath
      newClasspath.distinct
    }
}