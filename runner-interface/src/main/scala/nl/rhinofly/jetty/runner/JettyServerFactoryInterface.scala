package nl.rhinofly.jetty.runner

import java.io.File

trait JettyServerFactoryInterface {

  def newServer(port: Int, resourceBase: File, webXmlFile: File):JettyServerInterface
}