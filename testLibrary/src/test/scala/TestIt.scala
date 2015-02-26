import nl.rhinofly.railosbt.RailoRunner
import org.qirx.littlespec.Specification
import lucee.runtime.PageContext
import nl.rhinofly.railosbt.testutils.Cfc
import nl.rhinofly.railo.compiler.RailoContext

object TestIt extends Specification {

  "RailoRunner should be able to call CFC's from Scala, code:" - example {
    class Test() extends Cfc("testLibrary.Test")
    
    RailoRunner.withRailo(9191) { implicit context =>
      val testComponent = new Test()
      testComponent.test() is "tests-init-fromScala"
    }
  }
  
  "Fancy wrapper for railo interaction, code:" - example {
    def withRailo[T](code: RailoContext => T):T = 
      RailoRunner.withRailo(9191)(code)
      
    withRailo { implicit context =>
      val testComponent = Cfc("testLibrary.Test")
      val result = testComponent.test()
      result is "tests-fromScala"
    }
  }
}