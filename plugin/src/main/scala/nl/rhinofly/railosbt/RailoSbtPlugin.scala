package nl.rhinofly.railosbt

import sbt._
import sbt.Keys._
import railo.runtime.CFMLFactory
import org.eclipse.jetty.webapp.WebAppContext
import scala.Console.{ GREEN, RESET }
import jline.console.ConsoleReader
import railo.loader.engine.CFMLEngineFactory
import nl.rhinofly.railosbt.fakes.FakeHttpServletRequest
import javax.servlet.http.HttpServlet
import nl.rhinofly.railosbt.fakes.FakeHttpServletResponse
import railo.loader.engine.CFMLEngine

object RailoSbtPlugin extends AutoPlugin {

  object RailoSbtKeys {
    val port = settingKey[Int]("Port to start Railo server")
    val servletName = settingKey[String]("Name of the CMFL servlet")
    val cfmlFactory = taskKey[CFMLFactory]("The cfml factory")
    val servletConfiguration = taskKey[File]("location of the web.xml")
    val railoConfiguration = taskKey[File]("location of the railo config dir containing railo-web.xml.cfm")
    val railoLibraryDependencies = taskKey[Map[String, File]]("The dependencies, the pair is the name of the mapping with the location of the library")
  }

  override lazy val projectConfigurations = super.projectConfigurations :+ Railo

  import RailoSbtKeys._

  val Railo = config("railo").describedAs("Configuration for Railo utilities")

  override lazy val projectSettings =
    //inConfig(Railo)(Defaults.buildCore ++ Classpaths.ivyBaseSettings ++ Classpaths.jvmBaseSettings) ++
    Seq(
      port in Railo := 8181,

      servletName in Railo := "CFMLServlet",

      sourceDirectory in Railo := (sourceDirectory in Compile).value / "webapp",

      classDirectory in Railo := target.value / "webappClasses",

      servletConfiguration in Railo := {
        val railoConfigDir = (railoConfiguration in Railo).value
        val targetDir = streams.value.cacheDirectory
        val servlet = (servletName in Railo).value
        generateWebXml(railoConfigDir, targetDir, servlet)
      },

      railoConfiguration in Railo := {
        val railoConfigDir = streams.value.cacheDirectory
        val libraries = (railoLibraryDependencies in Railo).value
        generateRailoConfiguration(railoConfigDir, libraries)
        generateRailoServerConfiguration(railoConfigDir)
        railoConfigDir
      },

      run in Railo := {
        val railoPort = (port in Railo).value
        val webAppDirectory = (sourceDirectory in Railo).value.getAbsolutePath
        val webXml = (servletConfiguration in Railo).value.getAbsolutePath
        executeWithServer(railoPort, webAppDirectory, webXml) { context =>
          println(GREEN + "(Server started, use Ctrl+D to stop and go back to the console...)" + RESET)
          waitForKey()
        }
      },

      compile in Railo := {
        val railoPort = (port in Railo).value
        val webAppDirectory = (sourceDirectory in Railo).value.getAbsolutePath
        val webXml = (servletConfiguration in Railo).value.getAbsolutePath
        val railoServletName = (servletName in Railo).value
        val sourceDir = (sourceDirectory in Railo).value
        val result =
          executeWithServer(railoPort, webAppDirectory, webXml) {
            compileWithRailo(railoServletName, sourceDir)
          }
        IO.copyDirectory(result, (classDirectory in Railo).value, overwrite = true)
        inc.Analysis.Empty
      },

      autoScalaLibrary in Railo := false,
      
      railoLibraryDependencies in Railo :=
        update.value.toSeq.collect {
          case (configuration, _, artifact, file) if configuration == Railo.name =>
            artifact.name -> file
        }.toMap
  )
      
  def packaging = Seq(
    autoScalaLibrary := false,

    mappings in (Compile, packageBin) ++= {
      (compile in Railo).value
      val classDir = (classDirectory in Railo).value
      val classes = classDir.***
      val classMappings = (classes --- classDir) pair (relativeTo(classDir) | flat)

      val sourceDir = (sourceDirectory in Railo).value
      val sources = sourceDir.***
      val sourceMappings = (sources --- sourceDir) pair (relativeTo(sourceDir) | flat)

      sourceMappings.toSeq ++ classMappings.toSeq
    },

    packageOptions in (Compile, packageBin) +=
      Package.ManifestAttributes("mapping-type" -> "cfc"))

  def compileWithRailo(railoServletName: String, sourceDir: File) = { context: WebAppContext =>
    val handler = context.getServletHandler
    val servlet = handler.getServlet(railoServletName).getServlet
    val servletConfig = servlet.getServletConfig

    val startedBefore =
      try {
        CFMLEngineFactory.getInstance
        true
      } catch {
        case _: Throwable => false
      }
    val engine = CFMLEngineFactory.getInstance(servletConfig)

    val factory = engine
      .getCFMLFactory(handler.getServletContext, servletConfig, new FakeHttpServletRequest)

    val pageContext = factory.getRailoPageContext(
      servlet.asInstanceOf[HttpServlet],
      new FakeHttpServletRequest,
      new FakeHttpServletResponse,
      "error-page-url",
      false,
      -1,
      false)

    // we need a page context to be able to restart (which really we need to do)
    if (startedBefore) engine.getCFMLEngineFactory().restart("asdasd")

    val config = factory.getConfig

    val rootMapping = config.getMappings.toSeq
      .find(_.getVirtual == "/")
      .getOrElse(sys.error("Could not find root mapping"))

    val files = (sourceDir ** "*.cfc").get

    val relativeFiles = files
      .flatMap(IO.relativize(sourceDir, _: File))
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

  def executeWithServer[T](port: Int, webAppDirectory: String, webXml: String): (WebAppContext => T) => T =
    code => {
      val (context, server) = startRailoServer(port, webAppDirectory, webXml)
      try code(context)
      finally server.stop
    }

  def startRailoServer(webPort: Int, webappDirLocation: String, webXml: String) = {
    import org.eclipse.jetty.server.Server
    import org.eclipse.jetty.webapp.WebAppContext
    import org.fusesource.jansi.AnsiConsole

    val server = new Server(webPort)
    val root = new WebAppContext()
    root.setContextPath("/")
    root.setDescriptor(webXml)
    root.setResourceBase(webappDirLocation)

    //Parent loader priority is a class loader setting that Jetty accepts.
    //By default Jetty will behave like most web containers in that it will
    //allow your application to replace non-server libraries that are part of the
    //container. Setting parent loader priority to true changes this behavior.
    //Read more here: http://wiki.eclipse.org/Jetty/Reference/Jetty_Classloading
    root.setParentLoaderPriority(true)

    server.setHandler(root)
    server.start()

    AnsiConsole.systemInstall()
    val ANSI_NORMAL = "\u001b[0m"
    val ANSI_WHITEONBLUE = "\u001b[37;44m"
    AnsiConsole.out.println(ANSI_WHITEONBLUE + "Railo running on port " + webPort + ANSI_NORMAL)

    (root, server)
  }

  def generateWebXml(railoConfigDir: File, targetDir: File, servletName: String) = {
    val webXml = s"""|<?xml version="1.0" encoding="UTF-8"?>
    |<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://java.sun.com/xml/ns/javaee" xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" version="2.5">
    |  <welcome-file-list>
    |    <welcome-file>index.cfm</welcome-file>
    |    <welcome-file>index.html</welcome-file>
    |  </welcome-file-list>
    |  <servlet>
    |    <servlet-name>$servletName</servlet-name>
    |    <servlet-class>railo.loader.servlet.CFMLServlet</servlet-class>
    |    <init-param>
    |      <param-name>railo-web-directory</param-name>
    |      <param-value>${railoConfigDir.getAbsolutePath}</param-value>
    |    </init-param>
    |    <init-param>
    |      <param-name>railo-server-dir</param-name>
    |      <param-value>${railoConfigDir.getAbsolutePath}</param-value>
    |    </init-param>
    |    <!--
    |    <init-param>
    |      <param-name>railo-server-root</param-name>
    |      <param-value>lib/railo</param-value>
    |    </init-param>
    |    -->
    |    <load-on-startup>1</load-on-startup>
    |  </servlet>
    |  <servlet-mapping>
    |    <servlet-name>CFMLServlet</servlet-name>
    |    <url-pattern>*.cfm</url-pattern>
    |    <url-pattern>*.cfc</url-pattern>
    |  </servlet-mapping>
    |</web-app>""".stripMargin
    val webXmlFile = targetDir / "web.xml"
    IO.write(webXmlFile, webXml)
    webXmlFile
  }

  def generateRailoConfiguration(railoConfigDir: File, libraries:Map[String, File]) = {
    val railoConfig = s"""|<?xml version="1.0" encoding="UTF-8"?><railo-configuration pw="d6c8871b5f20282faaf3a0abd4e037ccb3b738d4568990d8f578f1e876f245a7" version="4.3"><cfabort/>
    |  <setting/>
    |  <data-sources>
    |  </data-sources>
    |  <resources>
    |    <resource-provider arguments="lock-timeout:10000;" class="railo.commons.io.res.type.s3.S3ResourceProvider" scheme="s3"/>
    |  </resources>
    |  <remote-clients directory="{railo-web}remote-client/"/>
    |  <file-system deploy-directory="{railo-web}/cfclasses/" fld-directory="{railo-web}/library/fld/" temp-directory="{railo-web}/temp/" tld-directory="{railo-web}/library/tld/">
    |  </file-system>
    |  <scope client-directory="{railo-web}/client-scope/" client-directory-max-size="100mb"/>
    |  <mail>
    |  </mail>
    |  <search directory="{railo-web}/search/" engine-class="railo.runtime.search.lucene.LuceneSearchEngine"/>
    |  <scheduler directory="{railo-web}/scheduler/"/>
    |  <mappings>
    |    ${libraries.map {case (mappingName, file) => s"""<mapping archive="$file" readonly="yes" toplevel="no" trusted="true" primary="archive" virtual="/${mappingName.replaceAll("[\\-\\.]", "_")}" />""" }.mkString("\n")}         
    |    <mapping archive="{railo-web}/context/railo-context.ra" physical="{railo-web}/context/" primary="physical" readonly="yes" toplevel="yes" trusted="true" virtual="/railo-context/"/>
    |  </mappings>
    |  <custom-tag>
    |    <mapping physical="{railo-web}/customtags/" trusted="yes"/>
    |  </custom-tag>
    |  <ext-tags>
    |    <ext-tag class="railo.cfx.example.HelloWorld" name="HelloWorld" type="java"/>
    |  </ext-tags>
    |  <component base="/railo-context/Component.cfc" data-member-default-access="public" use-shadow="yes">
    |  <mapping archive="{web-root-directory}/../archives/shared-1.1-SNAPSHOT.ra" inspect-template="" primary="archive" virtual="/shared"/><mapping archive="{web-root-directory}/../archives/libraries-1.0-SNAPSHOT.ra" inspect-template="" primary="archive" virtual="/libraries"/></component>
    |  <regional/>
    |  <debugging template="/railo-context/templates/debugging/debugging.cfm"/>
    |  <application cache-directory="{railo-web}/cache/" cache-directory-max-size="100mb"/>
    |  <logging>
    |    <logger appender="resource" appender-arguments="path:{railo-config}/logs/remoteclient.log" layout="classic" level="info" name="remoteclient"/>
    |    <logger appender="resource" appender-arguments="path:{railo-config}/logs/requesttimeout.log" layout="classic" name="requesttimeout"/>
    |    <logger appender="resource" appender-arguments="path:{railo-config}/logs/mail.log" layout="classic" name="mail"/>
    |    <logger appender="resource" appender-arguments="path:{railo-config}/logs/scheduler.log" layout="classic" name="scheduler"/>
    |    <logger appender="resource" appender-arguments="path:{railo-config}/logs/trace.log" layout="classic" name="trace"/>
    |    <logger appender="resource" appender-arguments="path:{railo-config}/logs/application.log" layout="classic" level="info" name="application"/>
    |    <logger appender="resource" appender-arguments="path:{railo-config}/logs/exception.log" layout="classic" level="info" name="exception"/>
    |  </logging>
    |  <rest/>
    |  <gateways/>
    |  <orm/>
    |  <cache default-object="defObj">
    |    <connection class="railo.runtime.cache.ram.RamCache" custom="timeToIdleSeconds=0&amp;timeToLiveSeconds=0" name="defObj" read-only="false" storage="false"/>
    |  </cache>
    |</railo-configuration>""".stripMargin
    val railoConfigFile = railoConfigDir / "railo-web.xml.cfm"
    IO.write(railoConfigFile, railoConfig)
    railoConfigFile
  }

  def generateRailoServerConfiguration(railoConfigDir: File) = {
    val railoConfig = """|<?xml version="1.0" encoding="UTF-8"?><railo-configuration pw="d6c8871b5f20282faaf3a0abd4e037ccb3b738d4568990d8f578f1e876f245a7" version="4.2">
	|	<system err="default" out="null"/>
	|	<data-sources psq="false"></data-sources>
	|	<file-system fld-directory="{railo-config}/library/fld/" function-directory="{railo-config}/library/function/" tag-directory="{railo-config}/library/tag/" temp-directory="{railo-config}/temp/" tld-directory="{railo-config}/library/tld/"></file-system>
	|	<dump-writers>
	|		<dump-writer class="railo.runtime.dump.HTMLDumpWriter" default="browser" name="html"/>
	|		<dump-writer class="railo.runtime.dump.TextDumpWriter" default="console" name="text"/>
	|		<dump-writer class="railo.runtime.dump.ClassicHTMLDumpWriter" name="classic"/>
	|		<dump-writer class="railo.runtime.dump.SimpleHTMLDumpWriter" name="simple"/>
	|	</dump-writers>
	|	<remote-clients directory="{railo-config}remote-client/"/>
	|    <resources>
	|    	<default-resource-provider arguments="lock-timeout:1000;" class="railo.commons.io.res.type.file.FileResourceProvider"/>
	|    	<resource-provider arguments="lock-timeout:20000;socket-timeout:-1;client-timeout:60000" class="railo.commons.io.res.type.ftp.FTPResourceProvider" scheme="ftp"/>
	|    	<resource-provider arguments="lock-timeout:1000;case-sensitive:true;" class="railo.commons.io.res.type.zip.ZipResourceProvider" scheme="zip"/>	
	|    	<resource-provider arguments="lock-timeout:1000;case-sensitive:true;" class="railo.commons.io.res.type.tar.TarResourceProvider" scheme="tar"/>
	|    	<resource-provider arguments="lock-timeout:1000;case-sensitive:true;" class="railo.commons.io.res.type.tgz.TGZResourceProvider" scheme="tgz"/>
	|    	<resource-provider arguments="lock-timeout:10000;case-sensitive:false;" class="railo.commons.io.res.type.http.HTTPResourceProvider" scheme="http"/>
	|    	<resource-provider arguments="lock-timeout:10000;case-sensitive:false;" class="railo.commons.io.res.type.http.HTTPSResourceProvider" scheme="https"/>
	|    	<resource-provider arguments="lock-timeout:10000;" class="railo.commons.io.res.type.s3.S3ResourceProvider" scheme="s3"/>
	|    </resources>
	|	<scope applicationtimeout="1,0,0,0" cascade-to-resultset="yes" cascading="standard" client-directory-max-size="10mb" client-max-age="90" clientmanagement="no" merge-url-form="no" requesttimeout="0,0,0,50" sessionmanagement="yes" sessiontimeout="0,0,30,0" setclientcookies="yes" setdomaincookies="no"/>
	|	<mail spool-enable="yes" spool-interval="5" timeout="30">
	|	</mail>
	|	<mappings>
	|		<mapping archive="" inspect-template="once" listener-mode="modern" listener-type="curr2root" physical="{railo-server}/context/" primary="physical" readonly="yes" virtual="/railo-server-context/"/>
	|		<mapping archive="{railo-config}/context/railo-context.ra" inspect-template="once" listener-mode="modern" listener-type="curr2root" physical="{railo-config}/context/" primary="physical" readonly="yes" virtual="/railo-context/"/>
	|	</mappings>	
	|	<custom-tag>
	|		<mapping inspect-template="never" physical="{railo-config}/customtags/"/>
	|	</custom-tag>
	|	<ext-tags>
	|		<ext-tag class="railo.cfx.example.HelloWorld" name="HelloWorld" type="java"/>
	|	</ext-tags>
	|	<component base="/railo-context/Component.cfc" data-member-default-access="public" dump-template="/railo-context/component-dump.cfm">
	|  		<mapping inspect-template="never" physical="{railo-web}/components/" primary="physical" virtual="/default"/>
	|	</component>
	|	<regional timeserver="pool.ntp.org"/>
	|	<orm engine-class="railo.runtime.orm.hibernate.HibernateORMEngine"/>
	|	<debugging debug="no" log-memory-usage="no" show-query-usage="no" template="/railo-context/templates/debugging/debugging.cfm"/>
	|	<application listener-mode="curr2root" listener-type="mixed"/>
	|	<update location="http://www.getrailo.org" type="manual"/>
	|	<flex configuration="manual"/>
	|	<logging>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/mapping.log" layout="classic" name="mapping"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/rest.log" layout="classic" name="rest"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/gateway.log" layout="classic" name="gateway"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/remoteclient.log" layout="classic" level="info" name="remoteclient"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/orm.log" layout="classic" name="orm"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/mail.log" layout="classic" name="mail"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/search.log" layout="classic" name="search"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/scheduler.log" layout="classic" name="scheduler"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/scope.log" layout="classic" name="scope"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/application.log" layout="classic" name="application"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/exception.log" layout="classic" name="exception"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/trace.log" layout="classic" name="trace"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/thread.log" layout="classic" name="thread"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/deploy.log" layout="classic" name="deploy"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/requesttimeout.log" layout="classic" name="requesttimeout"/>
	|		<logger appender="resource" appender-arguments="path:{railo-config}/logs/memory.log" layout="classic" name="memory"/>
	|	</logging>
	|</railo-configuration>""".stripMargin

    val railoConfigFile = railoConfigDir / "railo-server" / "context" / "railo-server.xml"
    IO.write(railoConfigFile, railoConfig)
    railoConfigFile
  }

  def withConsoleReader[T](f: ConsoleReader => T): T = {
    val consoleReader = new ConsoleReader
    try f(consoleReader) finally consoleReader.shutdown()
  }

  def waitForKey(): Unit = {
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

  def doWithoutEcho(f: => Unit): Unit = {
    withConsoleReader { consoleReader =>
      val terminal = consoleReader.getTerminal
      terminal.setEchoEnabled(false)
      try f finally terminal.restore()
    }
  }
}