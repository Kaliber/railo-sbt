package nl.rhinofly.railosbt.fakes

import javax.servlet.http.HttpServletResponse
import scala.beans.BeanProperty

class FakeHttpServletResponse extends HttpServletResponse {

  @BeanProperty
  var contentType: String = _

  def addCookie(x$1: javax.servlet.http.Cookie): Unit = ???
  def addDateHeader(x$1: String, x$2: Long): Unit = ???
  def addHeader(x$1: String, x$2: String): Unit = ???
  def addIntHeader(x$1: String, x$2: Int): Unit = ???
  def containsHeader(x$1: String): Boolean = ???
  def encodeRedirectURL(x$1: String): String = ???
  def encodeRedirectUrl(x$1: String): String = ???
  def encodeURL(x$1: String): String = ???
  def encodeUrl(x$1: String): String = ???
  def getHeader(x$1: String): String = ???
  def getHeaderNames(): java.util.Collection[String] = ???
  def getHeaders(x$1: String): java.util.Collection[String] = ???
  def getStatus(): Int = ???
  def sendError(x$1: Int): Unit = ???
  def sendError(x$1: Int, x$2: String): Unit = ???
  def sendRedirect(x$1: String): Unit = ???
  def setDateHeader(x$1: String, x$2: Long): Unit = ???
  def setHeader(x$1: String, x$2: String): Unit = ???
  def setIntHeader(x$1: String, x$2: Int): Unit = ???
  def setStatus(x$1: Int, x$2: String): Unit = ???
  def setStatus(x$1: Int): Unit = ???

  // Members declared in javax.servlet.ServletResponse
  def flushBuffer(): Unit = ???
  def getBufferSize(): Int = ???
  def getCharacterEncoding(): String = ???
  def getLocale(): java.util.Locale = ???
  def getOutputStream(): javax.servlet.ServletOutputStream = ???
  def getWriter(): java.io.PrintWriter = ???
  def isCommitted(): Boolean = ???
  def reset(): Unit = ???
  def resetBuffer(): Unit = ???
  def setBufferSize(x$1: Int): Unit = ???
  def setCharacterEncoding(x$1: String): Unit = ???
  def setContentLength(x$1: Int): Unit = ???
  def setContentLengthLong(x$1: Long): Unit = ???
  def setLocale(x$1: java.util.Locale): Unit = ???

}