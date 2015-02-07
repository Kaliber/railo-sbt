package nl.rhinofly.jetty.runner

import java.io.File
import scala.Console.GREEN
import scala.Console.RESET
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.component.LifeCycle
import org.eclipse.jetty.webapp.WebAppContext
import org.fusesource.jansi.AnsiConsole
import jline.console.ConsoleReader
import javax.servlet.Servlet

class JettyServer(port: Int, val context:WebAppContext) extends JettyServerInterface {

  lazy val server = {
    val server = new Server(port)
    server.setStopAtShutdown(true)
    server.addLifeCycleListener(LifeCycleListener)
    server
  }

  def getServlet(servletName:String):Servlet = 
    context.getServletHandler.getServlet(servletName).getServlet
  
  private var failure: Option[Throwable] = None

  def start() = {
    server.setHandler(context)
    server.start()
    if (failure.nonEmpty) stop()
  }

  def stopOnKey() = {
    if (failure.isEmpty) waitForKey()
    server.stop()
    server.join()
    failure foreach (throw _)
  }

  def join() = {
    if (failure.isEmpty) server.join()
    failure foreach (throw _)
  }

  def stop() = {
    server.stop()
    server.join()
    failure foreach (throw _)
  }

  private object LifeCycleListener extends LifeCycle.Listener {
    def lifeCycleStarting(event: LifeCycle) = println("STARTING...")

    def lifeCycleStarted(event: LifeCycle) = {
      println("STARTED")
      failure = Option(context.getUnavailableException)
      if (failure.isEmpty) {
        reportServerStarted()
      }
    }

    def lifeCycleFailure(event: LifeCycle, cause: Throwable) = {
      println("FAILURE")
      failure = Option(cause)
    }

    def lifeCycleStopping(event: LifeCycle) = println("STOPPING...")

    def lifeCycleStopped(event: LifeCycle) = println("STOPPED")
  }

  private def reportServerStarted() = {
    AnsiConsole.systemInstall()
    val ANSI_NORMAL = "\u001b[0m"
    val ANSI_WHITEONBLUE = "\u001b[37;44m"
    AnsiConsole.out.println(ANSI_WHITEONBLUE + "Jetty running on port " + port + ANSI_NORMAL)
  }

  private def waitForKey(): Unit = {
    AnsiConsole.out.println(GREEN + "(Server started, use Ctrl+D to stop...)" + RESET)
    withConsoleReader { consoleReader =>
      def waitEOF(): Unit = {
        consoleReader.readCharacter() match {
          case 4 | -1 =>
          // Note: we have to listen to -1 for jline2, for some reason...
          // STOP on Ctrl-D or EOF.
          case 11 =>
            consoleReader.clearScreen(); waitEOF()
          case 10 =>
            println(); waitEOF()
          case 13 =>
            println(); waitEOF()
          case x =>
            print(String valueOf x.toChar)
            waitEOF()
        }
      }
      doWithoutEcho(waitEOF())
    }
  }

  private def withConsoleReader[T](f: ConsoleReader => T): T = {
    val consoleReader = new ConsoleReader
    try f(consoleReader) finally consoleReader.shutdown()
  }

  private def doWithoutEcho(f: => Unit): Unit = {
    withConsoleReader { consoleReader =>
      val terminal = consoleReader.getTerminal
      terminal.setEchoEnabled(false)
      try f finally terminal.restore()
    }
  }
}
