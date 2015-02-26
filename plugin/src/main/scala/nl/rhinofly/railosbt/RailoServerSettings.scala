package nl.rhinofly.railosbt

import java.io.File

import RailoServerSettings._

case class RailoServerSettings(mappings: Seq[Mapping])

object RailoServerSettings {
  sealed trait Mapping
  case class ArchiveMapping(virtual: String, archive: String) extends Mapping
  case class LocalDirectoryMapping(virtual: String, directory: String) extends Mapping
}