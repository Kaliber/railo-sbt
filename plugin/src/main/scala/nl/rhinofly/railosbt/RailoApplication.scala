package nl.rhinofly.railosbt

import sbt.AutoPlugin
import nl.rhinofly.railosbt.configuration.CompileSettings
import nl.rhinofly.railosbt.configuration.RailoSettings
import sbt.plugins.JvmPlugin
import nl.rhinofly.railosbt.configuration.ProjectSettings
import nl.rhinofly.railosbt.configuration.RailoConfigurationSettings
import nl.rhinofly.railosbt.configuration.TestSettings
import sbt.inConfig
import sbt.Compile
import sbt.Test

object RailoApplication extends AutoPlugin {

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
      nativePackagerSettings
  }

  def railoSettings = {
    import RailoSettings._

    versionSettings ++
      passwordSettings ++
      directorySettings ++
      runSettings ++
      nativePackagerSettings
  }

  def railoCompileSettings = {
    import CompileSettings._
    import RailoConfigurationSettings._

    directorySettings ++
      runSettings ++
      webXmlAndRailoConfigurationSettings
  }

  def railoTestSettings = {
    import TestSettings._
    import RailoConfigurationSettings._

    directorySettings ++
      testUtilitySettings(railoSource in Compile) ++
      webXmlAndRailoConfigurationSettings ++
      mappingForTestClasses
  }
}