package nl.rhinofly.railosbt.configuration

import sbt._
import sbt.Keys._
import nl.rhinofly.railosbt.AutoImport
import nl.rhinofly.build.BuildInfo
import nl.rhinofly.railosbt.ServletContainer

object TestSettings {
  import AutoImport._
  import RailoKeys._
  
  lazy val directorySettings = Seq(
    railoSource := sourceDirectory.value / Railo.name
  )
  
  def testUtilitySettings(source: SettingKey[File]) = Seq(

    sourceGenerators += generateTestUtilities.taskValue,

    generateTestUtilities := {
      val runnerName = "RailoRunner"
      val templateStream = getClass.getClassLoader.getResourceAsStream(runnerName + ".template")
      val template = IO.readStream(templateStream)

      val sourceDir = source.value.getAbsolutePath
      val webXmlFile = webXml.value.getAbsolutePath

      import BuildInfo.{ railoCompilerClassName, jettyServerFactoryClassName }

      val fullTemplate = template
        .replace("$sourceDirectory", s"""new File("$sourceDir")""")
        .replace("$webXmlFile", s"""new File("$webXmlFile")""")
        .replace("$railoCompilerClassName", railoCompilerClassName)
        .replace("$jettyServerFactoryClassName", jettyServerFactoryClassName)
        .replace("$railoServletName", "\"" + ServletContainer.SERVLET_NAME + "\"")

      val file = (sourceManaged in Test).value / (runnerName + ".scala")
      IO.write(file, fullTemplate)
      Seq(file)
    }
  )
}