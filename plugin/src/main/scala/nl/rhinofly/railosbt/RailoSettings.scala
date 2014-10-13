package nl.rhinofly.railosbt

import java.io.File

case class RailoSettings(mappings:Seq[Mapping])

case class Mapping(virtual:String, archive:String)