package nl.rhinofly.railosbt

import java.io.InputStream

object RailoConfiguration {

  import RailoServerSettings._
  
  def hashPassword(classLoader: ClassLoader, password: String) = {
    val tpClass = classLoader.loadClass("railo.loader.TP")
    val is = tpClass.getResourceAsStream("/core/core.rc")
    try {

      val railoClassLoader = newRailoClassLoader(classLoader, tpClass, is)

      ReflectionWrapper(railoClassLoader)
        .findClass("railo.runtime.config.ConfigWebFactory")
        .hash(password)

    } finally is.close()
  }

  private def newRailoClassLoader(classLoader: ClassLoader, tpClass: Class[_], is: InputStream) = {
    val RailoClassLoader = classLoader
      .loadClass("railo.loader.classloader.RailoClassLoader")
      .getConstructor(classOf[InputStream], classOf[ClassLoader], classOf[Boolean])

    RailoClassLoader.newInstance(is, tpClass.getClassLoader, Boolean.box(false))
  }

  private case class ReflectionWrapper(obj: Any) {
    def findClass(className: String) = {
      val result =
        obj.getClass
          .getDeclaredMethod("findClass", classOf[String])
          .invoke(obj, "railo.runtime.config.ConfigWebFactory")
          .asInstanceOf[Class[Any]]

      ClassEnhancements(result)
    }

    case class ClassEnhancements(clazz: Class[Any]) {
      def hash(password: String) =
        clazz.getDeclaredMethod("hash", classOf[String])
          .invoke(null, password)
          .asInstanceOf[String]
    }
  }

  def webConfiguration(hashedPassword: String, settings: RailoServerSettings) =
    s"""|<?xml version="1.0" encoding="UTF-8"?><railo-configuration pw="$hashedPassword" version="4.3"><cfabort/>
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
        |    ${settings.mappings.map { case Mapping(virtual, archive) => s"""<mapping archive="$archive" readonly="yes" toplevel="no" trusted="true" primary="archive" virtual="/$virtual" />""" }.mkString("\n")}         
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

  def serverConfiguration(hashedPassword: String) = {
    s"""|<?xml version="1.0" encoding="UTF-8"?><railo-configuration pw="$hashedPassword" version="4.2">
       | <system err="default" out="null"/>
       | <data-sources psq="false"></data-sources>
       | <file-system fld-directory="{railo-config}/library/fld/" function-directory="{railo-config}/library/function/" tag-directory="{railo-config}/library/tag/" temp-directory="{railo-config}/temp/" tld-directory="{railo-config}/library/tld/"></file-system>
       | <dump-writers>
       |   <dump-writer class="railo.runtime.dump.HTMLDumpWriter" default="browser" name="html"/>
       |   <dump-writer class="railo.runtime.dump.TextDumpWriter" default="console" name="text"/>
       |   <dump-writer class="railo.runtime.dump.ClassicHTMLDumpWriter" name="classic"/>
       |   <dump-writer class="railo.runtime.dump.SimpleHTMLDumpWriter" name="simple"/>
       | </dump-writers>
       | <remote-clients directory="{railo-config}remote-client/"/>
       |    <resources>
       |     <default-resource-provider arguments="lock-timeout:1000;" class="railo.commons.io.res.type.file.FileResourceProvider"/>
       |     <resource-provider arguments="lock-timeout:20000;socket-timeout:-1;client-timeout:60000" class="railo.commons.io.res.type.ftp.FTPResourceProvider" scheme="ftp"/>
       |     <resource-provider arguments="lock-timeout:1000;case-sensitive:true;" class="railo.commons.io.res.type.zip.ZipResourceProvider" scheme="zip"/>  
       |     <resource-provider arguments="lock-timeout:1000;case-sensitive:true;" class="railo.commons.io.res.type.tar.TarResourceProvider" scheme="tar"/>
       |     <resource-provider arguments="lock-timeout:1000;case-sensitive:true;" class="railo.commons.io.res.type.tgz.TGZResourceProvider" scheme="tgz"/>
       |     <resource-provider arguments="lock-timeout:10000;case-sensitive:false;" class="railo.commons.io.res.type.http.HTTPResourceProvider" scheme="http"/>
       |     <resource-provider arguments="lock-timeout:10000;case-sensitive:false;" class="railo.commons.io.res.type.http.HTTPSResourceProvider" scheme="https"/>
       |     <resource-provider arguments="lock-timeout:10000;" class="railo.commons.io.res.type.s3.S3ResourceProvider" scheme="s3"/>
       |    </resources>
       | <scope applicationtimeout="1,0,0,0" cascade-to-resultset="yes" cascading="standard" client-directory-max-size="10mb" client-max-age="90" clientmanagement="no" merge-url-form="no" requesttimeout="0,0,0,50" sessionmanagement="yes" sessiontimeout="0,0,30,0" setclientcookies="yes" setdomaincookies="no"/>
       | <mail spool-enable="yes" spool-interval="5" timeout="30">
       | </mail>
       | <mappings>
       |   <mapping archive="" inspect-template="once" listener-mode="modern" listener-type="curr2root" physical="{railo-server}/context/" primary="physical" readonly="yes" virtual="/railo-server-context/"/>
       |   <mapping archive="{railo-config}/context/railo-context.ra" inspect-template="once" listener-mode="modern" listener-type="curr2root" physical="{railo-config}/context/" primary="physical" readonly="yes" virtual="/railo-context/"/>
       | </mappings> 
       | <custom-tag>
       |   <mapping inspect-template="never" physical="{railo-config}/customtags/"/>
       | </custom-tag>
       | <ext-tags>
       |   <ext-tag class="railo.cfx.example.HelloWorld" name="HelloWorld" type="java"/>
       | </ext-tags>
       | <component base="/railo-context/Component.cfc" data-member-default-access="public" dump-template="/railo-context/component-dump.cfm">
       |     <mapping inspect-template="never" physical="{railo-web}/components/" primary="physical" virtual="/default"/>
       | </component>
       | <regional timeserver="pool.ntp.org"/>
       | <orm engine-class="railo.runtime.orm.hibernate.HibernateORMEngine"/>
       | <debugging debug="no" log-memory-usage="no" show-query-usage="no" template="/railo-context/templates/debugging/debugging.cfm"/>
       | <application listener-mode="curr2root" listener-type="mixed"/>
       | <update location="http://www.getrailo.org" type="manual"/>
       | <flex configuration="manual"/>
       | <logging>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/mapping.log" layout="classic" name="mapping"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/rest.log" layout="classic" name="rest"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/gateway.log" layout="classic" name="gateway"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/remoteclient.log" layout="classic" level="info" name="remoteclient"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/orm.log" layout="classic" name="orm"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/mail.log" layout="classic" name="mail"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/search.log" layout="classic" name="search"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/scheduler.log" layout="classic" name="scheduler"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/scope.log" layout="classic" name="scope"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/application.log" layout="classic" name="application"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/exception.log" layout="classic" name="exception"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/trace.log" layout="classic" name="trace"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/thread.log" layout="classic" name="thread"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/deploy.log" layout="classic" name="deploy"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/requesttimeout.log" layout="classic" name="requesttimeout"/>
       |   <logger appender="resource" appender-arguments="path:{railo-config}/logs/memory.log" layout="classic" name="memory"/>
       | </logging>
       |</railo-configuration>""".stripMargin
  }
}