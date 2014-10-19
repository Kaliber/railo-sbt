package nl.rhinofly.railosbt.configuration

import sbt._
import sbt.Keys._
import nl.rhinofly.railosbt.AutoImport
import nl.rhinofly.build.BuildInfo

object ProjectSettings {

  lazy val all =
    nonPluginProjectSettings ++
    directorySettings ++
      compileOnlySettings ++
      dependencySettings ++
      publishSettings

  // A configuration that can be used for dependencies that should not be 
  // added to the Maven or Ivy description
  val compileOnly = config("compileOnly").hide

  import AutoImport._
  import RailoKeys._

  lazy val nonPluginProjectSettings = Seq(
    // https://github.com/sbt/sbt/issues/1670
    discoveredSbtPlugins := PluginDiscovery.emptyDiscoveredNames
  )

  lazy val directorySettings = Seq(
    watchSources <++= unmanagedSources in Railo
  )

  lazy val compileOnlySettings = Seq(
    ivyConfigurations += compileOnly,
    unmanagedClasspath in Compile ++= update.value.select(configurationFilter(compileOnly.name))
  )

  lazy val dependencySettings = Seq(
    resolvers += "http://cfmlprojects.org/artifacts/" at "http://cfmlprojects.org/artifacts/",
    libraryDependencies ++= {
      import BuildInfo._
      val railoDependency = railoDependencyBase :+ (version in Railo).value
      Seq(
        javaDependency(railoDependency) % compileOnly,
        // railo does not have this listed as dependency while it depends on it
        javaDependency(servletJspApiDependency) % compileOnly,
        scalaDependency(runnerDependency) % compileOnly,
        scalaDependency(compilerDependency) % compileOnly
      )
    },
    moduleSettings := addRailoArtifacts(moduleSettings.value)
  )

  lazy val publishSettings = {
    val packageArtifactTask = Seq(packageRailo in Compile)

    Seq(
      artifacts <++= Classpaths.artifactDefs(packageArtifactTask),
      packagedArtifacts <++= Classpaths.packaged(packageArtifactTask)
    )
  }

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

  def addRailoArtifacts(moduleSettings: ModuleSettings): ModuleSettings =
    moduleSettings match {
      case ec: InlineConfiguration if ec.configurations contains Railo =>
        ec.copy(
          dependencies = ec.dependencies.map(addRailoArtifact),
          overrides = ec.overrides.map(addRailoArtifact))
      case unknown => unknown
    }

  def addRailoArtifact(m: ModuleID): ModuleID =
    if (m.configurations exists (_ contains Railo.name)) m.withRailo()
    else m
}