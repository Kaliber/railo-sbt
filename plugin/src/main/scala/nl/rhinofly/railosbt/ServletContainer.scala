package nl.rhinofly.railosbt

object ServletContainer {

  val SERVLET_NAME = "CFMLServlet"

  def webXml(webConfigDirectory: String, serverConfigDirectory: String) = s"""|<?xml version="1.0" encoding="UTF-8"?>
    |<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://java.sun.com/xml/ns/javaee" xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" version="2.5">
    |  <welcome-file-list>
    |    <welcome-file>index.cfm</welcome-file>
    |    <welcome-file>index.html</welcome-file>
    |  </welcome-file-list>
    |  <servlet>
    |    <servlet-name>$SERVLET_NAME</servlet-name>
    |    <servlet-class>railo.loader.servlet.CFMLServlet</servlet-class>
    |    <init-param>
    |      <param-name>railo-web-directory</param-name>
    |      <param-value>$webConfigDirectory</param-value>
    |    </init-param>
    |    <init-param>
    |      <param-name>railo-server-dir</param-name>
    |      <param-value>$serverConfigDirectory</param-value>
    |    </init-param>
    |    <load-on-startup>1</load-on-startup>
    |  </servlet>
    |  <servlet-mapping>
    |    <servlet-name>CFMLServlet</servlet-name>
    |    <url-pattern>*.cfm</url-pattern>
    |    <url-pattern>*.cfc</url-pattern>
    |  </servlet-mapping>
    |</web-app>""".stripMargin
}