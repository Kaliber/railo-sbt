package nl.rhinofly.railosbt.testutils

import nl.rhinofly.railo.compiler.RailoContext

case class NoWrapperType(value:AnyRef)

trait LowerPriorityImplicits {
	implicit def anyRefToNoWrapperType(a:AnyRef) = NoWrapperType(a)
}

object NoWrapperType extends LowerPriorityImplicits {
  implicit def cfcDefinition(cfc:CfcDefinition)(implicit c:RailoContext) = 
    NoWrapperType(cfc.underlying(c))
} 
