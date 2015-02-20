package nl.rhinofly.railo.compiler

import nl.rhinofly.jetty.runner.JettyServerInterface
import javax.servlet.Servlet
import lucee.loader.engine.CFMLEngineFactory
import nl.rhinofly.railo.compiler.fakes.FakeHttpServletRequest
import javax.servlet.http.HttpServlet
import nl.rhinofly.railo.compiler.fakes.FakeHttpServletResponse
import lucee.runtime.PageContext
import lucee.runtime.config.ConfigWeb

class RailoRunner(jettyServer: JettyServerInterface, railoServletName: String) {

  def withRailo[T](code: (ConfigWeb, PageContext) => T): T =
    withRailoServlet { servlet =>
      val servletConfig = servlet.getServletConfig

      val engine = CFMLEngineFactory.getInstance(servletConfig)

      val factory = engine
        .getCFMLFactory(servletConfig.getServletContext, servletConfig, new FakeHttpServletRequest)

      val pageContext = factory.getLuceePageContext(
        servlet.asInstanceOf[HttpServlet],
        new FakeHttpServletRequest,
        new FakeHttpServletResponse,
        /* errorPageURL = */ "error-page-url",
        /* needsSession = */ false,
        /* bufferSize = */ -1,
        /* autoflush = */ false)

      // The section below is commented out because we might need reloading 
      // in the future. It was quite a trip to figure out how to do it.

      // We need a page context to be able to restart. We need to restart
      // Railo because we are running railo in memory and it uses static 
      // members to store things
      // println("Restarting Railo before compilation")
      // engine.getCFMLEngineFactory().restart(password)

      val config = factory.getConfig

      // Railo needs to think it's actually executing a page, so we make
      // it think the current page is the base component (a relatively 
      // easy accessible PageSource instance)
      pageContext.addPageSource(config.getBaseComponentPageSource, true)
      
      code(config, pageContext)
    }

  private def withRailoServlet[T](code: Servlet => T): T =
    try {
      jettyServer.start()
      code(jettyServer.getServlet(railoServletName))
    } finally jettyServer.stop()

}