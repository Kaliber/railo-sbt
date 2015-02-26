package nl.rhinofly.railosbt

import sbt._

object AutoImport {

  val Railo = config("railo").describedAs("Configuration for Railo")

  object RailoKeys {
    val port = settingKey[Int]("Port to start Railo server")
    val password = settingKey[String]("The password to use for Railo")
    val content = taskKey[String]("The contents of the configuration file")
    val webConfiguration = taskKey[Unit]("Generates the configuration")
    val serverConfiguration = taskKey[Unit]("Generates the configuration")
    val libraryMappings = taskKey[Seq[RailoServerSettings.Mapping]]("The mappings extracted from the dependencies")
    val webXml = taskKey[File]("The web xml file used by Jetty")
    val packageRailo = taskKey[File]("Produces a railo artifact, containing compiled cfml files")
    val railoSource = settingKey[File]("Railo source directory")
    val hashPassword = taskKey[(String, String) => String]("Hashes the given password and salt using Railo")
    val salt = taskKey[String]("Salt used to hash the password. Default is a UUID")
    val mode = taskKey[RailoRunMode]("The mode in which the server should run")
    val generateTestUtilities = taskKey[Seq[File]]("Generates files useful for testing")
  }
  
  sealed abstract class RailoRunMode(value:String) {
    override def toString = value
  }
  object RailoRunMode {
    object Join extends RailoRunMode("join")
    object StopOnKey extends RailoRunMode("stopOnKey")
  }
}
