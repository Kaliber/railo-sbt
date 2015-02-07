package nl.rhinofly.jetty.runner

import java.io.File
import org.eclipse.jetty.webapp.WebAppContext

object FileBasedContext {

  def create(resourceBase:File, webXmlFile: File) = {
    val context = new WebAppContext()
    context.setContextPath("/")
    context.setDescriptor(webXmlFile.getAbsolutePath)
    context.setResourceBase(resourceBase.getAbsolutePath)
    context
  }
  
}