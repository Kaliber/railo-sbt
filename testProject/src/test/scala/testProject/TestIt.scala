package testProject

import org.qirx.littlespec.Specification
import nl.rhinofly.railosbt.RailoRunner
import nl.rhinofly.railosbt.testutils.Cfc

object TestIt extends Specification {

  """|The context of the RailoRunner should now have test resources
     |under the /test mapping""".stripMargin - {

    RailoRunner.withRailo(9191) { implicit context =>
      val projectComponent = Cfc("project.Test")
      val testComponent = Cfc("test.Test")
      val result = projectComponent.test(testComponent)
      result is "from-test-component"
    }
  }
}