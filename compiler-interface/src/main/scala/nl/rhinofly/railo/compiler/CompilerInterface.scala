package nl.rhinofly.railo.compiler

import nl.rhinofly.jetty.runner.JettyServerInterface
import java.io.File

trait CompilerInterface {
  def compile(jettyServer: JettyServerInterface, password: String, sourceDir: File, railoServletName: String):File
}