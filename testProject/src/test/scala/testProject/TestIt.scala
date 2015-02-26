package testProject

import org.qirx.littlespec.Specification
import nl.rhinofly.railosbt.RailoRunner
import nl.rhinofly.railosbt.testutils.Cfc

object TestIt extends Specification {

  """|The context of the RailoRunner should now have test resources
     |under the /test mapping""".stripMargin - {

    RailoRunner.withRailo(9191) { implicit context =>
      val projectComponent = Cfc("project.Test")
      val testComponent1 = Cfc("test.Test1")
      val testComponent2 = new Cfc("test.Test2")
      
      val result1 = projectComponent.test(testComponent1)
      val result2 = projectComponent.test(testComponent2)
      
      result1 is "from-test1-component"
      result2 is "from-test2-component"
    }
  }
}