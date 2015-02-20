package nl.rhinofly.railo.compiler

import java.io.File
import lucee.loader.engine.CFMLEngineWrapper
import lucee.loader.engine.CFMLEngineFactory
import lucee.runtime.exp.PageException
import nl.rhinofly.jetty.runner.JettyServerInterface
import nl.rhinofly.railo.compiler.fakes.FakeHttpServletRequest
import nl.rhinofly.railo.compiler.fakes.FakeHttpServletResponse
import javax.servlet.http.HttpServlet
import javax.servlet.Servlet
import nl.rhinofly.jetty.runner.JettyServerInterface
import java.io.FilenameFilter
import scala.collection.JavaConverters._
import lucee.runtime.config.ConfigWeb
import scala.util.Try
import lucee.runtime.PageContext

object Compiler extends CompilerInterface {

  // add logger
  def compile(
    jettyServer: JettyServerInterface,
    password: String,
    sourceDir: File,
    railoServletName: String,
    logger: Logger) = {
    val runner = new RailoRunner(jettyServer, railoServletName)
    runner withRailo compileWithServlet(password, sourceDir, logger)
  }

  private def compileWithServlet(
    password: String, sourceDir: File, logger: Logger)(
      config: ConfigWeb, pageContext: PageContext): Try[File] = Try {

    val rootMapping = config.getMappings.toSeq
      .find(_.getVirtual == "/")
      .getOrElse(sys.error("Could not find root mapping"))

    def cfcsIn(dir: File): Seq[File] =
      Option(dir.listFiles())
        .getOrElse(sys.error(s"The given direction is not a directory or an IO error occurred: $dir"))
        .flatMap { file =>
          if (file.isDirectory) cfcsIn(file)
          else Seq(file)
        }
        .filter { file =>
          val name = file.getName
          (name endsWith ".cfc") || (name endsWith ".cfm")
        }

    val files = cfcsIn(sourceDir)

    val relativeFiles = files
      .flatMap(relativize(sourceDir, _))
      .map("/" + _)

    logger.info("Compiling " + files.size + " files.")

    val exceptions =
      relativeFiles.flatMap { path =>
        logger.debug("Compiling " + path)
        try {
          val pageSource = rootMapping.getPageSource(path)
          pageSource.loadPage(pageContext)
          None
        } catch {
          case e: PageException => Some(getFriendlyError(e, config))
        }
      }

    val classRootDirectory = rootMapping.getClassRootDirectory.getAbsolutePath

    logger.info("Compiled to " + classRootDirectory)

    exceptions
      .foldLeft(None: Option[String]) { (messages, e) =>
        messages.map(_ + "\n" + e.toString) orElse Some(e.toString)
      }
      .foreach(sys.error)

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

  private def getFriendlyError(e: PageException, config: ConfigWeb) = {
    val catchBlock = e.getCatchBlock(config).asScala.asInstanceOf[scala.collection.mutable.Map[String, Any]]
    val tagContext = catchBlock.get("TagContext")
      .map(_.asInstanceOf[java.util.List[java.util.Map[String, Any]]])
      .flatMap(_.asScala.headOption.map(_.asScala))
      .getOrElse(Map.empty[String, Any])

    val template = tagContext.get("template").getOrElse("[[template unknown]]")
    val line = tagContext.get("line").getOrElse("[[line unknown]]")
    val column = tagContext.get("column").map(_.asInstanceOf[Double].toInt).getOrElse(0)

    val code = tagContext.get("codePrintPlain")
      .map { s =>
        val r =
          s.asInstanceOf[String].split("\n") match {
            case Array(a, b, c, d, e) => Array(" " + a, " " + b, ">" + c, " " + d, " " + e)
            case x                    => x
          }
        r.mkString("\n")
      }
      .getOrElse("[[no code available]]")

    s"""|${e.getMessage}
        |
        |$template:$line
        |
        |$code
        |""".stripMargin
  }
}