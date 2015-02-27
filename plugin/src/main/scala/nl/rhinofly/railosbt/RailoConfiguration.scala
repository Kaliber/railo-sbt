package nl.rhinofly.railosbt

import java.io.InputStream

object RailoConfiguration {

  import RailoServerSettings._

  def salt(classLoader: ClassLoader): String =
    withRailoClassLoaderFrom(classLoader) { railoClassLoader =>

      ReflectionWrapper(railoClassLoader)
        .findClass("lucee.runtime.functions.other.CreateUUID")
        .salt
    }

  def hashPassword(classLoader: ClassLoader, password: String, salt: String) =
    withRailoClassLoaderFrom(classLoader) { railoClassLoader =>
      
      ReflectionWrapper(railoClassLoader)
        .findClass("lucee.runtime.config.Password")
        .getInstanceFromRawPassword(password, salt)
        .password
    }

  private def withRailoClassLoaderFrom[T](classLoader: ClassLoader)(code: Any => T): T = {
    val tpClass = classLoader.loadClass("lucee.loader.TP")
    val is = tpClass.getResourceAsStream("/core/core.lco")
    assert(is != null, "Could not find /core/core.rc in jar containing TP class")

    try {
      val railoClassLoader = newRailoClassLoader(classLoader, tpClass, is)
      code(railoClassLoader)
    } finally is.close()
  }

  private def newRailoClassLoader(classLoader: ClassLoader, tpClass: Class[_], is: InputStream) = {
    val RailoClassLoader = classLoader
      .loadClass("lucee.loader.classloader.LuceeClassLoader")
      .getConstructor(classOf[InputStream], classOf[ClassLoader], classOf[Boolean])

    RailoClassLoader.newInstance(is, tpClass.getClassLoader, Boolean.box(false))
  }

  private case class ReflectionWrapper(obj: Any) {
    def findClass(className: String) = {
      val result =
        obj.getClass
          .getDeclaredMethod("findClass", classOf[String])
          .invoke(obj, className)
          .asInstanceOf[Class[Any]]

      new ClassEnhancements(result)
    }

    class ClassEnhancements(clazz: Class[Any]) {
      def getInstanceFromRawPassword(password: String, salt: String) = {
        val result =
          clazz.getDeclaredMethod("getInstanceFromRawPassword", classOf[String], classOf[String])
            .invoke(null, password, salt)

        new InstanceEnhancement(clazz, result)
      }

      def salt: String = clazz.getDeclaredMethod("invoke").invoke(null).asInstanceOf[String]
    }

    class InstanceEnhancement(clazz: Class[Any], o: Any) {
      def password =
        clazz.getDeclaredField("password")
          .get(o)
          .asInstanceOf[String]
    }
  }

  def webConfiguration(hashedPassword: String, salt: String, settings: RailoServerSettings) =
    s"""|<?xml version="1.0" encoding="UTF-8"?><cfLuceeConfiguration hspw="$hashedPassword" salt="$salt" version="4.5"><cfabort/>
        |  <setting/>
        |  <data-sources>
        |  </data-sources>
        |  <resources>
        |    <resource-provider arguments="case-sensitive:true;lock-timeout:1000;" class="lucee.commons.io.res.type.ram.RamResourceProvider" scheme="ram"/>
        |    <resource-provider arguments="lock-timeout:10000;" class="lucee.commons.io.res.type.s3.S3ResourceProvider" scheme="s3"/>
        |  </resources>
        |  <remote-clients directory="{lucee-web}remote-client/"/>
        |  <file-system deploy-directory="{lucee-web}/cfclasses/" fld-directory="{lucee-web}/library/fld/" temp-directory="{lucee-web}/temp/" tld-directory="{lucee-web}/library/tld/">
        |  </file-system>
        |  <scope client-directory="{lucee-web}/client-scope/" client-directory-max-size="100mb"/>
        |  <mail>
        |  </mail>
        |  <search directory="{lucee-web}/search/" engine-class="lucee.runtime.search.lucene.LuceneSearchEngine"/>
        |  <scheduler directory="{lucee-web}/scheduler/"/>
        |  <mappings>
             ${settings.mappings.map {
                case ArchiveMapping(virtual, archive) =>
                  s"""|    <mapping virtual="/$virtual" archive="$archive" toplevel="no" trusted="true" primary="archive" />"""
                case LocalDirectoryMapping(virtual, directory) =>
                  s"""|    <mapping virtual="/$virtual" physical="$directory" toplevel="no" trusted="true" primary="physical" />"""
             }.mkString("\n")}
        |    <mapping archive="{lucee-web}/context/lucee-context.lar" physical="{lucee-web}/context/" primary="physical" readonly="yes" toplevel="yes" trusted="true" virtual="/lucee/"/>
        |  </mappings>
        |  <custom-tag>
        |    <mapping physical="{lucee-web}/customtags/" trusted="yes"/>
        |  </custom-tag>
        |  <ext-tags>
        |    <ext-tag class="lucee.cfx.example.HelloWorld" name="HelloWorld" type="java"/>
        |  </ext-tags>
        |  <component base="/lucee/Component.cfc" data-member-default-access="public" use-shadow="yes"> 
        |  </component>
        |  <regional/>
        |  <debugging template="/lucee/templates/debugging/debugging.cfm"/>
        |  <application cache-directory="{lucee-web}/cache/" cache-directory-max-size="100mb"/>
        |  <logging>
        |    <logger appender="resource" appender-arguments="path:{lucee-config}/logs/remoteclient.log" layout="classic" level="info" name="remoteclient"/>
        |    <logger appender="resource" appender-arguments="path:{lucee-config}/logs/requesttimeout.log" layout="classic" name="requesttimeout"/>
        |    <logger appender="resource" appender-arguments="path:{lucee-config}/logs/mail.log" layout="classic" name="mail"/>
        |    <logger appender="resource" appender-arguments="path:{lucee-config}/logs/scheduler.log" layout="classic" name="scheduler"/>
        |    <logger appender="resource" appender-arguments="path:{lucee-config}/logs/trace.log" layout="classic" name="trace"/>
        |    <logger appender="resource" appender-arguments="path:{lucee-config}/logs/application.log" layout="classic" level="info" name="application"/>
        |    <logger appender="resource" appender-arguments="path:{lucee-config}/logs/exception.log" layout="classic" level="info" name="exception"/>  
        |  </logging>    
        |  <rest/>
        |  <gateways/>
        |  <orm/>
        |  <cache default-function="" default-include="" default-object="default" default-query="" default-resource="" default-template="">
        |    <connection class="lucee.runtime.cache.ram.RamCache" custom="timeToIdleSeconds=0&amp;timeToLiveSeconds=0" name="default" read-only="false" storage="false"/>
        |  </cache>
        |</cfLuceeConfiguration>""".stripMargin

  def serverConfiguration(hashedPassword: String, salt: String) = {
    s"""|<?xml version="1.0" encoding="UTF-8"?><railo-configuration pw="$hashedPassword" salt="$salt" version="4.2">
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