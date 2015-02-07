package nl.rhinofly.jetty.runner

import java.io.File

object JettyServerFactory extends JettyServerFactoryInterface {
  
  def newServer(port: Int, resourceBase: File, webXmlFile: File):JettyServerInterface = {
    val context = FileBasedContext.create(resourceBase, webXmlFile)
    new JettyServer(port, context)
  } 
}