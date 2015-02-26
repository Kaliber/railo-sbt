package nl.rhinofly.railosbt.configuration

import sbt._
import sbt.Keys._
import sbt.ConfigKey.configurationToKey
import sbt.ScopeAxis.scopeAxisToScope
import nl.rhinofly.railosbt.AutoImport
import nl.rhinofly.railosbt.ServletContainer
import nl.rhinofly.build.BuildInfo

object CompileSettings {

  import AutoImport._
  import RailoKeys._

  lazy val directorySettings = Seq(
    railoSource := sourceDirectory.value / Railo.name
  )

  lazy val runSettings = Seq(
    mainClass in run := (mainClass in run in Railo).value
  )
  
  lazy val packageSettings =
    Defaults.packageTaskSettings(packageRailo, packageRailoMappings) ++
      inTask(packageRailo)(Seq(
        artifactClassifier := Some(Railo.name),
        unmanagedSourceDirectories <<= unmanagedSourceDirectories in Railo,
        unmanagedSources <<= unmanagedSources in Railo,
        products <<= products in Railo,
        packageOptions <<= packageOptions in Railo
      ))

  def packageRailoMappings =
    Defaults.concatMappings(
      products.map(_ flatMap Path.allSubpaths),
      Defaults.sourceMappings
    )

}