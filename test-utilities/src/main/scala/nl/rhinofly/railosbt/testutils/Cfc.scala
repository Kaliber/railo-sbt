package nl.rhinofly.railosbt.testutils

import scala.language.dynamics
import lucee.runtime.Component
import nl.rhinofly.railo.compiler.RailoContext

class CfcDefinition(val underlying: RailoContext => Component) extends Dynamic {

  def this(name: String) = this(Cfc.load(name))

  def checkUnderlying(implicit c:RailoContext):CfcDefinition = {
    underlying(c)
    this
  } 
  
  def applyDynamic(method: String)(arguments: NoWrapperType*)(implicit c: RailoContext): AnyRef = {
    val normalizedArguments = arguments.map(_.value).toArray
    underlying(c).call(c.pageContext, method, normalizedArguments)
  }
    
  def applyDynamicNamed(method:String)(arguments: (String, NoWrapperType) *)(implicit c: RailoContext): AnyRef = {
    val normalizedArguments = arguments.toMap.mapValues(_.value).toSeq
    underlying(c).callWithNamedValues(c.pageContext, method, Struct(normalizedArguments: _*))
  } 
}

class Cfc(name: String, arguments: AnyRef*)
  extends CfcDefinition(
    underlying = Cfc.instantiate(name, arguments.toArray)
  )

object Cfc {
  def apply(name: String)(implicit c:RailoContext): CfcDefinition = 
    new CfcDefinition(name).checkUnderlying

  def load(name: String): RailoContext => Component =
    _.pageContext.loadComponent(name)

  def instantiate(name: String, arguments: Array[AnyRef]): RailoContext => Component = { c =>
    load(name)(c).call(c.pageContext, "init", arguments).as[Component]
  }
}
