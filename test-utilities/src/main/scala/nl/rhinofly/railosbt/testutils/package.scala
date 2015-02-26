package nl.rhinofly.railosbt

import lucee.runtime.`type`.{Array => RailoArray}
import nl.rhinofly.railo.compiler.RailoContext

package object testutils {
  
  implicit class ShortCastAlias(o:AnyRef) {
    def as[T] = o.asInstanceOf[T]
  }
  
  implicit def arrayToRailoArray[T](a:Array[T])(implicit context:RailoContext):RailoArray = {
    val ra = context.creation.createArray()
    a.foreach(ra.append)
    ra
  }
}