package nl.rhinofly.railosbt.testutils

import lucee.runtime.`type`.{ Query => RailoQuery }
import lucee.runtime.`type`.Collection
import nl.rhinofly.railo.compiler.RailoContext

object Query {

  def apply(entries: (String, Seq[AnyRef])*)(implicit c: RailoContext): RailoQuery = {
    import c.creation.{ createQuery, createKey }

    val q = createQuery(Array.empty[Collection.Key], 0, "custom-query")

    entries.foreach {
      case (columnName, values) =>
        val key = createKey(columnName)
        q.addColumn(key, values.toArray)
    }

    q
  }
}