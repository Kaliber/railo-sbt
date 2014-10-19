package nl.rhinofly.railosbt

import java.io.File

import RailoServerSettings._

case class RailoServerSettings(mappings: Seq[Mapping])

object RailoServerSettings {
  case class Mapping(virtual: String, archive: String)
}