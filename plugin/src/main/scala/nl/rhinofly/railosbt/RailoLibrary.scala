package nl.rhinofly.railosbt

import sbt.AutoPlugin
import nl.rhinofly.railosbt.configuration.CompileSettings
import nl.rhinofly.railosbt.configuration.RailoSettings
import sbt.plugins.JvmPlugin
import nl.rhinofly.railosbt.configuration.ProjectSettings
import nl.rhinofly.railosbt.configuration.TestSettings
import nl.rhinofly.railosbt.configuration.RailoConfigurationSettings
import sbt.inConfig
import sbt.Compile
import sbt.Keys.target
import sbt.Test

object RailoLibrary extends AutoPlugin {

  import AutoImport.Railo
  import AutoImport.RailoKeys.railoSource

  // ensures correct ordering, otherwise settings might get overridden
  override val requires = JvmPlugin

  val autoImport = AutoImport

  override lazy val projectConfigurations =
    super.projectConfigurations :+ Railo

  override def projectSettings =
    super.projectSettings ++
      railoProjectSettings ++
      inConfig(Railo)(railoSettings) ++
      inConfig(Compile)(railoCompileSettings) ++
      inConfig(Test)(railoTestSettings)

  def railoProjectSettings = {
    import ProjectSettings._

    nonPluginProjectSettings ++
      directorySettings ++
      dependencySettings ++
      publishSettings
  }

  def railoSettings = {
    import RailoSettings._

    versionSettings ++
      passwordSettings ++
      directorySettings ++
      mappingSettings ++
      compileSettings ++
      packageRailoSettings
  }

  def railoCompileSettings = {
    import CompileSettings._
    import RailoConfigurationSettings._

    directorySettings ++
      packageSettings ++
      webXmlAndRailoConfigurationSettings
  }

  def railoTestSettings = {
    import TestSettings._
    import RailoConfigurationSettings._

    directorySettings ++
      testUtilitySettings(railoSource in Test) ++
      webXmlAndRailoConfigurationSettings ++
      mappingForCurrentProjectSettings
  }
}