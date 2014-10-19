package nl.rhinofly.railosbt

import AutoImport.Railo
import nl.rhinofly.railosbt.configuration.CompileSettings
import nl.rhinofly.railosbt.configuration.ProjectSettings
import nl.rhinofly.railosbt.configuration.RailoSettings
import sbt.AutoPlugin
import sbt.Compile
import sbt.inConfig
import sbt.plugins.JvmPlugin

object RailoPlugin extends AutoPlugin {

  // ensures correct ordering, otherwise settings might get overridden
  override val requires = JvmPlugin

  val autoImport = AutoImport

  override lazy val projectConfigurations =
    super.projectConfigurations :+ Railo

  override def projectSettings =
    super.projectSettings ++
      ProjectSettings.all ++
      inConfig(Compile)(CompileSettings.all) ++
      inConfig(Railo)(RailoSettings.all)
}