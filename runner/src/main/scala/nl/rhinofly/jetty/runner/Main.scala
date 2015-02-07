package nl.rhinofly.jetty.runner

import java.io.File

import scala.util.Try

object Main extends App {

  args match {
    case Parameters(port, resourceBase, webXmlFile, mode) =>
      val context = FileBasedContext.create(resourceBase, webXmlFile)
      val server = new JettyServer(port, context)
      server.start()
      print("Descriptor    : " + webXmlFile)
      print("Resource base : " + resourceBase)
      print("")
      mode.join(server)
    case _ =>
      println("Usage: port resourceBase webXmlFile mode")
      println("Examples:")
      println("  8181 src web.xml stopOnKey")
      println("  8181 src web.xml join")
      System.exit(1)
  }

  sealed case class Mode(join: JettyServer => Unit)
  object Mode {
    object StopOnKey extends Mode(_.stopOnKey())
    object Join extends Mode(_.join())

    val values = Map(
      "stopOnKey" -> StopOnKey,
      "join" -> Join
    )
  }

  object Parameters {
    def unapply(args: Array[String]): Option[(Int, File, File, Mode)] =
      args match {
        case Array(port, resourceBase, webXmlFile, mode) =>
          for {
            port <- Try(port.toInt).toOption
            resourceBase <- toFile(resourceBase)
            webXmlFile <- toFile(webXmlFile)
            mode <- Mode.values.get(mode)
          } yield (port, resourceBase, webXmlFile, mode)
        case _ => None
      }

    private def toFile(location: String): Option[File] =
      Option(new File(location)).filter { file =>
        val exists = file.exists
        if (!exists) println(s"Could not find '$file'")
        exists
      }
  }
}