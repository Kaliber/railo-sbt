package nl.rhinofly.railo.compiler

trait Logger {
  def info(message:String):Unit
  def debug(message: String):Unit
}