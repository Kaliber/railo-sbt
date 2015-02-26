package nl.rhinofly.railo.compiler

import lucee.runtime.util.Creation
import lucee.runtime.PageContext
import lucee.runtime.config.ConfigWeb

case class RailoContext(
  config: ConfigWeb,
  pageContext: PageContext,
  creation: Creation)