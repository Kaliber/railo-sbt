package nl.rhinofly.railo.compiler

import java.io.File
import railo.loader.engine.CFMLEngineWrapper
import railo.loader.engine.CFMLEngineFactory
import nl.rhinofly.jetty.runner.JettyServerInterface
import nl.rhinofly.railo.compiler.fakes.FakeHttpServletRequest
import nl.rhinofly.railo.compiler.fakes.FakeHttpServletResponse
import javax.servlet.http.HttpServlet
import javax.servlet.Servlet
import nl.rhinofly.jetty.runner.JettyServerInterface
import java.io.FilenameFilter

object Compiler extends CompilerInterface {

  // add logger
  def compile(jettyServer: JettyServerInterface, password: String, sourceDir: File, railoServletName: String) = {
    try {
      jettyServer.start()
      compileWithServlet(password, sourceDir, railoServletName, jettyServer.getServlet(railoServletName))
    } finally jettyServer.stop()
  }

  def compileWithServlet(password: String, sourceDir: File, railoServletName: String, servlet: Servlet) = {

    val servletConfig = servlet.getServletConfig

    val engine = CFMLEngineFactory.getInstance(servletConfig)

    val factory = engine
      .getCFMLFactory(servletConfig.getServletContext, servletConfig, new FakeHttpServletRequest)

    val pageContext = factory.getRailoPageContext(
      servlet.asInstanceOf[HttpServlet],
      new FakeHttpServletRequest,
      new FakeHttpServletResponse,
      "error-page-url",
      false,
      -1,
      false)

    // The section below is commented out because we might need reloading 
    // in the future. It was quite a trip to figure out how to do it.

    // We need a page context to be able to restart. We need to restart
    // Railo because we are running railo in memory and it uses static 
    // members to store things
    // println("Restarting Railo before compilation")
    // engine.getCFMLEngineFactory().restart(password)

    val config = factory.getConfig

    val rootMapping = config.getMappings.toSeq
      .find(_.getVirtual == "/")
      .getOrElse(sys.error("Could not find root mapping"))

    val files = sourceDir.listFiles(new FilenameFilter {
      def accept(dir: File, name: String) = {
        println(dir + " :: " + name)
        name.endsWith(".cfc")
      }
    })

    val relativeFiles = files
      .flatMap(relativize(sourceDir, _))
      .map("/" + _)

    relativeFiles.foreach { path =>
      println("Compiling " + path)
      val pageSource = rootMapping.getPageSource(path)
      pageSource.loadPage(pageContext)
    }

    val classRootDirectory = rootMapping.getClassRootDirectory.getAbsolutePath

    println("Compiled to " + classRootDirectory)

    new File(classRootDirectory)
  }

  private def relativize(base: File, file: File): Option[String] = {
    val pathString = file.getAbsolutePath
    baseFileString(base) flatMap { baseString =>
      if (pathString.startsWith(baseString))
        Some(pathString.substring(baseString.length))
      else None
    }
  }

  private def baseFileString(baseFile: File): Option[String] =
    if (baseFile.isDirectory) {
      val cp = baseFile.getAbsolutePath
      assert(cp.length > 0)
      val normalized = if (cp.charAt(cp.length - 1) == File.separatorChar) cp else cp + File.separatorChar
      Some(normalized)
    } else None
}