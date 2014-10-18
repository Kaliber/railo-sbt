package nl.rhinofly.jetty.runner

import org.eclipse.jetty.server.Server
import javax.servlet.Servlet

trait JettyServerInterface {

  def getServlet(servletName:String):Servlet
  
  def start(): Unit
  def stopOnKey(): Unit
  def join(): Unit
  def stop(): Unit
}