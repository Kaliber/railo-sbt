package nl.rhinofly.railosbt

import sbt._

object Import {

  val Railo = config("railo").describedAs("Configuration for Railo libraries")

  object RailoKeys {
    val port = settingKey[Int]("Port to start Railo server")
    val password = settingKey[String]("The password to use for Railo")
    val content = taskKey[String]("The contents of the configuration file")
    val webConfiguration = taskKey[File]("Generates the configuration and returns the base directory")
    val serverConfiguration = taskKey[File]("Generates the configuration and returns the base directory")
    val libraryMappings = taskKey[Seq[Mapping]]("The mappings extracted from the dependencies")
    val webXml = taskKey[File]("The web xml file used by Jetty")
  }
}
