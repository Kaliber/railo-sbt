package nl.rhinofly.railosbt.testutils

import nl.rhinofly.railo.compiler.RailoContext

case class NoWrapperType(value:AnyRef)

trait LowerPriorityImplicits {
  implicit def anyRefToNoWrapperType[T](a:T)(implicit asAnyRef: T => AnyRef) = 
    NoWrapperType(asAnyRef(a))
}

object NoWrapperType extends LowerPriorityImplicits {
  implicit def cfcDefinition[T](cfc:T)(implicit toCfcDefinition: T => CfcDefinition) = 
    NoWrapperType(cfc.instance)
}
