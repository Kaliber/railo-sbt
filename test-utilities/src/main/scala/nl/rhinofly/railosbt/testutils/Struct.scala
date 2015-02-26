package nl.rhinofly.railosbt.testutils

import lucee.runtime.`type`.{Struct => RailoStruct}
import nl.rhinofly.railo.compiler.RailoContext

object Struct {
  def apply(entries: (String, NoWrapperType)*)(implicit c: RailoContext): RailoStruct = {
    import c.creation._
    val s = createStruct
    entries.foreach {
      case (k, v) => s.set(createKey(k), v.value)
    }
    s
  }
}