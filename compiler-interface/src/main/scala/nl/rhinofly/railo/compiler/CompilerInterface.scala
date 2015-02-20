package nl.rhinofly.railo.compiler

import nl.rhinofly.jetty.runner.JettyServerInterface
import java.io.File
import scala.util.Try

trait CompilerInterface {
  def compile(
    jettyServer: JettyServerInterface, 
    password: String, 
    sourceDir: File, 
    railoServletName: String,
    logger:Logger):Try[File]
}