package nl.rhinofly.railosbt.fakes

import javax.servlet.http.HttpServletRequest

class FakeHttpServletRequest extends HttpServletRequest {
  // Members declared in javax.servlet.http.HttpServletRequest
  def authenticate(x$1: javax.servlet.http.HttpServletResponse): Boolean = ???
  def changeSessionId(): String = ???
  def getAuthType(): String = ???
  def getContextPath(): String = "/"
  def getCookies(): Array[javax.servlet.http.Cookie] = Array.empty
  def getDateHeader(x$1: String): Long = ???
  def getHeader(x$1: String): String = ???
  def getHeaderNames(): java.util.Enumeration[String] = ???
  def getHeaders(x$1: String): java.util.Enumeration[String] = ???
  def getIntHeader(x$1: String): Int = ???
  def getMethod(): String = ???
  def getPart(x$1: String): javax.servlet.http.Part = ???
  def getParts(): java.util.Collection[javax.servlet.http.Part] = ???
  def getPathInfo(): String = ""
  def getPathTranslated(): String = ???
  def getQueryString(): String = ""
  def getRemoteUser(): String = ???
  def getRequestURI(): String = "/"
  def getRequestURL(): StringBuffer = ???
  def getRequestedSessionId(): String = ???
  def getServletPath(): String = "/"
  def getSession(): javax.servlet.http.HttpSession = ???
  def getSession(x$1: Boolean): javax.servlet.http.HttpSession = ???
  def getUserPrincipal(): java.security.Principal = ???
  def isRequestedSessionIdFromCookie(): Boolean = ???
  def isRequestedSessionIdFromURL(): Boolean = ???
  def isRequestedSessionIdFromUrl(): Boolean = ???
  def isRequestedSessionIdValid(): Boolean = ???
  def isUserInRole(x$1: String): Boolean = ???
  def login(x$1: String, x$2: String): Unit = ???
  def logout(): Unit = ???
  def upgrade[T <: javax.servlet.http.HttpUpgradeHandler](x$1: Class[T]): T = ???
  // Members declared in javax.servlet.ServletRequest
  def getAsyncContext(): javax.servlet.AsyncContext = ???
  def getAttribute(name: String): Object = null
  def getAttributeNames(): java.util.Enumeration[String] = ???
  def getCharacterEncoding(): String = ???
  def getContentLengthLong(): Long = ???
  def getContentLength(): Int = ???
  def getContentType(): String = null
  def getDispatcherType(): javax.servlet.DispatcherType = ???
  def getInputStream(): javax.servlet.ServletInputStream = ???
  def getLocalAddr(): String = ???
  def getLocalName(): String = ???
  def getLocalPort(): Int = ???
  def getLocale(): java.util.Locale = ???
  def getLocales(): java.util.Enumeration[java.util.Locale] = ???
  def getParameter(x$1: String): String = ???
  def getParameterMap(): java.util.Map[String, Array[String]] = ???
  def getParameterNames(): java.util.Enumeration[String] = ???
  def getParameterValues(x$1: String): Array[String] = ???
  def getProtocol(): String = ???
  def getReader(): java.io.BufferedReader = ???
  def getRealPath(x$1: String): String = ???
  def getRemoteAddr(): String = ???
  def getRemoteHost(): String = ???
  def getRemotePort(): Int = ???
  def getRequestDispatcher(x$1: String): javax.servlet.RequestDispatcher = ???
  def getScheme(): String = "http"
  def getServerName(): String = "sbt-not-a-server"
  def getServerPort(): Int = 9999
  def getServletContext(): javax.servlet.ServletContext = ???
  def isAsyncStarted(): Boolean = ???
  def isAsyncSupported(): Boolean = ???
  def isSecure(): Boolean = ???
  def removeAttribute(x$1: String): Unit = ???
  def setAttribute(x$1: String, x$2: Any): Unit = ???
  def setCharacterEncoding(x$1: String): Unit = ???
  def startAsync(x$1: javax.servlet.ServletRequest, x$2: javax.servlet.ServletResponse): javax.servlet.AsyncContext = ???
  def startAsync(): javax.servlet.AsyncContext = ???
}