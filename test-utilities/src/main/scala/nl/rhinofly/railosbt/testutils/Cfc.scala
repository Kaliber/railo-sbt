package nl.rhinofly.railosbt.testutils

import scala.language.dynamics
import lucee.runtime.Component
import nl.rhinofly.railo.compiler.RailoContext

class CfcDefinition(val instance: Component)(implicit c: RailoContext) extends Dynamic {

  def applyDynamic(method: String)(arguments: NoWrapperType*): AnyRef = {
    val normalizedArguments = arguments.map(_.value).toArray
    instance.call(c.pageContext, method, normalizedArguments)
  }

  def applyDynamicNamed(method: String)(arguments: (String, NoWrapperType)*): AnyRef =
    instance.callWithNamedValues(c.pageContext, method, Struct(arguments: _*))
}

class Cfc(name: String, arguments: AnyRef*)(implicit c: RailoContext)
  extends CfcDefinition(Cfc.instantiate(name, arguments.toArray))

object Cfc {
  def apply(name: String)(implicit c: RailoContext): CfcDefinition =
    new CfcDefinition(load(name))

  def load(name:String)(implicit c: RailoContext): Component =
    c.pageContext.loadComponent(name)

  def instantiate(name: String, arguments: Array[AnyRef])( implicit c: RailoContext): Component = 
    load(name).call(c.pageContext, "init", arguments).as[Component]
}
