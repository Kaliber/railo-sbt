package nl.rhinofly.railosbt.configuration

import sbt._
import sbt.Keys._
import sbt.ConfigKey.configurationToKey
import sbt.ScopeAxis.scopeAxisToScope
import nl.rhinofly.railosbt.AutoImport

object CompileSettings {

  import AutoImport._
  import RailoKeys._

  lazy val all = runSettings ++ directorySettings ++ packageSettings

  lazy val runSettings = Seq(
    mainClass := (mainClass in run in Railo).value
  )
  
  lazy val directorySettings = Seq(
    railoSource := sourceDirectory.value / Railo.name
  )

  lazy val packageSettings =
    Defaults.packageTaskSettings(packageRailo, packageRailoMappings) ++
      inTask(packageRailo)(Seq(
        artifactClassifier := Some(Railo.name),
        unmanagedSourceDirectories := (unmanagedSourceDirectories in Railo).value,
        unmanagedSources <<= unmanagedSources in Railo,
        products := (products in Railo).value,
        packageOptions := (packageOptions in Railo).value
      ))

  def packageRailoMappings =
    Defaults.concatMappings(
      products.map(_ flatMap Path.allSubpaths),
      Defaults.sourceMappings
    )

}