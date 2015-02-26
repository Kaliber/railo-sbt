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

object RailoConfigurationSettings {

  import AutoImport._
  import RailoKeys._

  lazy val webXmlAndRailoConfigurationSettings =
    webXmlSettings ++
      webConfigurationSettings ++
      serverConfigurationSettings

  lazy val webXmlSettings = Seq(
    content in webXml := {
      serverConfiguration.value
      webConfiguration.value
      val webConfigurationDirectory = (target in webConfiguration).value.getAbsolutePath
      val serverConfigurationDirectory = (target in serverConfiguration).value.getAbsolutePath

      ServletContainer.webXml(webConfigurationDirectory, serverConfigurationDirectory)
    },

    target in webXml := (target in Railo).value / "web.xml",

    webXml := {
      val file = (target in webXml).value
      val contents = (content in webXml).value
      IO.write(file, contents)
      file
    }
  )

  lazy val webConfigurationSettings = Seq(
    password in webConfiguration <<= password in webConfiguration in Railo,
    salt in webConfiguration <<= salt in Railo,

    target in webConfiguration := (target in Railo).value / "web",

    libraryMappings in webConfiguration := railoMappingsInConfiguration(
      updateReport = update.value,
      base = (target in webConfiguration).value,
      target = (target in libraryMappings).value
    ),

    target in libraryMappings := (target in webConfiguration).value / "mappingArchives",

    content in webConfiguration := {
      streams.value.log.warn("Add protection that prevents publishing or packaging with LocalDirectoryMappings that are outside of the project root")
      val clearTextPassword = (password in webConfiguration).value
      val passwordSalt = (salt in webConfiguration).value
      val hashedPassword = (hashPassword in Railo).value(clearTextPassword, passwordSalt)
      val settings = RailoServerSettings((libraryMappings in webConfiguration).value)

      RailoConfiguration.webConfiguration(hashedPassword, passwordSalt, settings)
    },

    artifactPath in webConfiguration :=
      (target in webConfiguration).value / "lucee-web.xml.cfm",

    webConfiguration := {
      val contents = (content in webConfiguration).value
      val file = (artifactPath in webConfiguration).value
      IO.write(file, contents)
    }
  )

  lazy val serverConfigurationSettings = Seq(
    password in serverConfiguration <<= password in serverConfiguration in Railo,
    salt in serverConfiguration <<= salt in Railo,

    target in serverConfiguration := (target in Railo).value / "server",

    content in serverConfiguration := {
      val clearTextPassword = (password in serverConfiguration).value
      val passwordSalt = (salt in serverConfiguration).value
      val hashedPassword = (hashPassword in Railo).value(clearTextPassword, passwordSalt)
      RailoConfiguration.serverConfiguration(hashedPassword, passwordSalt)
    },

    artifactPath in serverConfiguration :=
      (target in serverConfiguration).value / "railo-server" / "context" / "railo-server.xml",

    serverConfiguration := {
      val contents = (content in serverConfiguration).value
      val file = (artifactPath in serverConfiguration).value
      IO.write(file, contents)
    }
  )

  lazy val mappingForCurrentProjectSettings = Seq(
    libraryMappings in webConfiguration += createMapping(
      base = (target in webConfiguration).value,
      file = (packageRailo in Compile).value,
      target = (target in libraryMappings).value,
      mappingName = (name in Railo).value
    )
  )

  lazy val mappingForTestClasses = Seq(
    libraryMappings in webConfiguration += {
      val testSources = (railoSource in Test).value
      RailoServerSettings.LocalDirectoryMapping("test", testSources.getAbsolutePath)
    }
  )

  def railoMappingsInConfiguration(updateReport: UpdateReport, base: File, target: File) = {
    val filter = configurationFilter(Railo.name) && artifactFilter(classifier = Railo.name)
    updateReport.filter(filter).toSeq.map {
      case (configuration, moduleId, artifact, file) =>

        val mappingName =
          for {
            manifest <- new Jar(file).manifest
            mappingName <- Option(manifest.getMainAttributes.getValue(MAPPING_NAME))
          } yield mappingName

        createMapping(base, file, target, mappingName.getOrElse(artifact.name))
    }
  }

  def createMapping(base: File, file: File, target: File, mappingName: String) = {
    val targetFile = target / file.getName
    IO.copyFile(file, targetFile)
    val relativeFileName = IO.relativize(base, targetFile).getOrElse(sys.error(s"Programming error, $targetFile is not relative to $target"))

    RailoServerSettings.ArchiveMapping(
      mappingName,
      "{railo-web}/" + relativeFileName
    )
  }

}